package app.vanta.launcher.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.vanta.launcher.domain.model.AppItem
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.StandardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SheetInMs = 260
private const val SheetOutMs = 200
private const val ContactsDebounceMs = 150L

/** One row of the result list. Labels carry no number; everything else is numbered from 01. */
private sealed interface Hit { val key: String }
private class Label(val text: String) : Hit { override val key = "label:$text" }
private class AnswerHit(val text: String) : Hit { override val key = "answer" }
private class AppHit(val app: AppItem) : Hit { override val key = "app:${app.packageName}" }
private class ContactRow(val c: ContactHit) : Hit { override val key = "contact:${c.id}" }
private object AllowContacts : Hit { override val key = "contacts-allow" }
private class SettingRow(val s: SettingsHit) : Hit { override val key = "setting" }
private object WebHit : Hit { override val key = "web" }
private class Entry(val hit: Hit, val n: Int)

@Composable
fun SwipeDownSearch(
    modifier: Modifier = Modifier,
    apps: List<AppItem>,
    onLaunch: (String) -> Unit,
    onDismiss: () -> Unit,
    onDarkMode: (Boolean) -> Unit = {}
) {
    val colors = LocalAppTheme.current
    val keyboard = LocalSoftwareKeyboardController.current
    val clipboard = LocalClipboardManager.current
    val focus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val historyPrefs = remember {
        context.getSharedPreferences("standard_search_history", Context.MODE_PRIVATE)
    }
    var history by remember { mutableStateOf(historyPrefs.getString("history", "") ?: "") }
    val historyPackages = remember(history) {
        if (history.isBlank()) emptyList()
        else history.split(",").filter { it.isNotBlank() }
    }
    val recentApps = remember(apps, historyPackages) {
        historyPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }.take(6)
    }
    val results = remember(apps, query) {
        if (query.isBlank()) emptyList()
        else apps.filter { it.label.contains(query, ignoreCase = true) }.take(8)
    }
    val answer = remember(query) { calcAnswer(query) ?: convertAnswer(query) }
    val setting = remember(query) { settingsHit(query) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnDarkMode by rememberUpdatedState(onDarkMode)

    // Contacts: permission gate + 150ms debounced name query off the main thread.
    var contactsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val requestContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        contactsGranted = granted
    }
    var contacts by remember { mutableStateOf(emptyList<ContactHit>()) }
    val nameLike = query.trim().length >= 2 && answer == null && query.any { it.isLetter() }
    LaunchedEffect(query, contactsGranted) {
        if (!contactsGranted || !nameLike) { contacts = emptyList(); return@LaunchedEffect }
        delay(ContactsDebounceMs)
        contacts = withContext(Dispatchers.IO) { queryContacts(context, query.trim()) }
    }

    // Answers, apps, contacts, settings, web: numbered continuously, section labels between.
    val entries = remember(query, results, answer, contacts, contactsGranted, nameLike, setting) {
        if (query.isBlank()) emptyList() else buildList {
            var n = 0
            fun section(label: String?, hits: List<Hit>) {
                if (hits.isEmpty()) return
                if (label != null) add(Entry(Label(label), -1))
                hits.forEach { add(Entry(it, n++)) }
            }
            section("ANSWER", listOfNotNull(answer?.let { AnswerHit(it) }))
            section("APPS", results.map { AppHit(it) })
            section(
                "CONTACTS",
                when {
                    contactsGranted -> contacts.map { ContactRow(it) }
                    nameLike -> listOf(AllowContacts)
                    else -> emptyList()
                }
            )
            section("SETTINGS", listOfNotNull(setting?.let { SettingRow(it) }))
            section(null, listOf(WebHit))
        }
    }

    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(900); copied = false } }
    LaunchedEffect(query) { copied = false }

    val sheet = remember { Animatable(0f) }
    val dim = remember { Animatable(0f) }
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { dim.animateTo(1f, tween(200)) }
        sheet.animateTo(1f, tween(SheetInMs, easing = LumiaEasing))
    }
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }

    val dismiss: () -> Unit = {
        if (!closing) {
            closing = true
            keyboard?.hide()
            scope.launch {
                launch { dim.animateTo(0f, tween(SheetOutMs)) }
                sheet.animateTo(0f, tween(SheetOutMs, easing = LumiaEasing))
                currentOnDismiss()
            }
        }
    }
    BackHandler { dismiss() }

    val launchApp: (String) -> Unit = { pkg ->
        val updated = (listOf(pkg) + historyPackages.filter { it != pkg }).take(8)
        val joined = updated.joinToString(",")
        historyPrefs.edit().putString("history", joined).apply()
        history = joined
        onLaunch(pkg)
        dismiss()
    }
    val open: (Intent) -> Unit = { intent ->
        try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { }
        dismiss()
    }
    val launchWeb: () -> Unit = {
        val q = query.trim()
        if (q.isNotBlank()) {
            open(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))))
        }
    }
    val dial: (String?) -> Unit = { n -> if (n != null) open(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", n, null))) }
    val message: (String?) -> Unit = { n -> if (n != null) open(Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", n, null))) }
    val act: (Hit) -> Unit = { hit ->
        when (hit) {
            is AnswerHit -> {
                clipboard.setText(AnnotatedString(hit.text.substringAfter("= ")))
                copied = true
            }
            is AppHit -> launchApp(hit.app.packageName)
            is ContactRow -> dial(hit.c.number)
            AllowContacts -> requestContacts.launch(Manifest.permission.READ_CONTACTS)
            is SettingRow -> { hit.s.run(context, currentOnDarkMode); dismiss() }
            WebHit -> launchWeb()
            is Label -> {}
        }
    }
    val launchTop: () -> Unit = {
        val top = entries.firstOrNull { it.n == 0 }
        if (top != null) act(top.hit) else launchWeb()
    }
    val clearHistory: () -> Unit = {
        historyPrefs.edit().remove("history").apply()
        history = ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(colors.ink.copy(alpha = 0.5f * dim.value)) }
                .pointerInput(Unit) { detectTapGestures { dismiss() } }
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    val h = sheetHeightPx
                    translationY = -h * (1f - sheet.value)
                    alpha = if (h == 0f) 0f else 1f
                }
                .onSizeChanged { sheetHeightPx = it.height.toFloat() }
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            total += amount
                            if (total < -140f) {
                                dismiss()
                                total = 0f
                            }
                        }
                    )
                }
                .background(colors.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(colors.tile)
                    .border(TileDefaults.Border, colors.ink)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        MonoLabel("SEARCH //", size = 14.sp, color = colors.muted, weight = FontWeight.Bold)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                        textStyle = StandardType.mono(14.sp, FontWeight.Bold).copy(color = colors.onTile),
                        cursorBrush = SolidColor(colors.ink),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = { launchTop() })
                    )
                }
                if (query.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    MonoLabel(
                        "CLEAR",
                        size = 9.sp,
                        color = colors.accent,
                        weight = FontWeight.Bold,
                        modifier = Modifier
                            .tilePress(onTap = { query = "" }, tilt = false)
                            .button("CLEAR QUERY") { query = "" }
                            .padding(4.dp)
                    )
                }
            }
            Spacer(Modifier.height(TileDefaults.Gutter))
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
            ) {
                if (query.isNotBlank()) {
                    itemsIndexed(entries, key = { _, e -> e.hit.key }) { pos, e ->
                        val enter = Modifier.metroTurnstileIn(pos, key = query, staggerMs = 20, capMs = 240)
                        val top = e.n == 0
                        when (val hit = e.hit) {
                            is Label -> MonoLabel(
                                "${hit.text} //",
                                size = 10.sp,
                                color = colors.muted,
                                modifier = enter.padding(vertical = 2.dp)
                            )
                            is AnswerHit -> SearchRow(
                                index = e.n,
                                label = hit.text,
                                packageName = null,
                                top = top,
                                onTap = { act(hit) },
                                modifier = enter,
                                trailing = if (copied) {
                                    {
                                        MonoLabel(
                                            "COPIED",
                                            size = 9.sp,
                                            color = colors.accent,
                                            weight = FontWeight.Bold,
                                            modifier = Modifier.metroTurnstileIn(0, key = copied, durationMs = 180)
                                        )
                                    }
                                } else null
                            )
                            is AppHit -> SearchRow(
                                index = e.n,
                                label = hit.app.label,
                                packageName = hit.app.packageName,
                                top = top,
                                onTap = { act(hit) },
                                modifier = enter
                            )
                            is ContactRow -> SearchRow(
                                index = e.n,
                                label = hit.c.name,
                                packageName = null,
                                top = top,
                                onTap = { act(hit) },
                                modifier = enter,
                                trailing = {
                                    SquareAction("CALL", top) { dial(hit.c.number) }
                                    Spacer(Modifier.width(6.dp))
                                    SquareAction("MSG", top) { message(hit.c.number) }
                                }
                            )
                            AllowContacts -> SearchRow(
                                index = e.n,
                                label = "ALLOW CONTACTS →",
                                packageName = null,
                                top = top,
                                onTap = { act(hit) },
                                modifier = enter
                            )
                            is SettingRow -> SearchRow(
                                index = e.n,
                                label = "${hit.s.label} →",
                                packageName = null,
                                top = top,
                                onTap = { act(hit) },
                                modifier = enter
                            )
                            WebHit -> SearchRow(
                                index = e.n,
                                label = "SEARCH THE WEB →",
                                packageName = null,
                                top = top,
                                onTap = { act(hit) },
                                modifier = enter
                            )
                        }
                    }
                } else if (recentApps.isNotEmpty()) {
                    item(key = "recent-header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MonoLabel("RECENT //", size = 10.sp, color = colors.muted)
                            Spacer(Modifier.weight(1f))
                            MonoLabel(
                                "CLEAR",
                                size = 10.sp,
                                color = colors.accent,
                                weight = FontWeight.Bold,
                                modifier = Modifier
                                    .tilePress(onTap = clearHistory, tilt = false)
                                    .button("CLEAR HISTORY") { clearHistory() }
                                    .padding(4.dp)
                            )
                        }
                    }
                    itemsIndexed(recentApps, key = { _, app -> app.packageName }) { i, app ->
                        SearchRow(
                            index = i,
                            label = app.label,
                            packageName = app.packageName,
                            top = false,
                            onTap = { launchApp(app.packageName) },
                            modifier = Modifier.metroTurnstileIn(i, key = "recent", staggerMs = 20, capMs = 240)
                        )
                    }
                }
            }
        }
    }
}

/** Accent when [top], otherwise the tile outline the theme uses. */
@Composable
private fun rowOutline(top: Boolean): Color {
    val colors = LocalAppTheme.current
    return when {
        top -> colors.accent
        colors.filledTiles -> colors.outline
        else -> colors.ink
    }
}

/**
 * 52dp outline tile row: index, icon, headline label; the top result is outlined in accent and marked for Enter.
 * [trailing] replaces the Enter mark (contact actions, COPIED flash).
 */
@Composable
private fun SearchRow(
    index: Int,
    label: String,
    packageName: String?,
    top: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = LocalAppTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .tilePress(onTap = onTap, tilt = false)
            .background(colors.tile)
            .border(TileDefaults.Border, rowOutline(top))
            .button(label) { onTap() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonoLabel("%02d".format(index + 1), size = 9.sp, color = colors.onTile.copy(alpha = 0.7f))
        Spacer(Modifier.width(10.dp))
        if (packageName != null) {
            AppIcon(packageName = packageName, size = 26.dp)
            Spacer(Modifier.width(10.dp))
        }
        HeadlineText(
            label.uppercase(),
            18.sp,
            color = colors.onTile,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        if (trailing != null) {
            trailing()
        } else if (top) {
            MonoLabel("↵", size = 14.sp, color = colors.accent, weight = FontWeight.Bold)
        }
    }
}

/** 32dp square action inside a row ("CALL", "MSG"); consumes its own press so the row does not fire. */
@Composable
private fun SquareAction(label: String, top: Boolean, onTap: () -> Unit) {
    val colors = LocalAppTheme.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .tilePress(onTap = onTap, tilt = false)
            .border(TileDefaults.Border, rowOutline(top))
            .button(label) { onTap() },
        contentAlignment = Alignment.Center
    ) {
        MonoLabel(label, size = 8.sp, color = colors.onTile, weight = FontWeight.Bold)
    }
}
