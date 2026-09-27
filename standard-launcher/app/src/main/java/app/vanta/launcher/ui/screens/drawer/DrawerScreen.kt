package app.vanta.launcher.ui.screens.drawer

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.vanta.launcher.R
import app.vanta.launcher.domain.model.AppItem
import app.vanta.launcher.ui.components.AppBarAction
import app.vanta.launcher.ui.components.AppIcon
import app.vanta.launcher.ui.components.BarcodeTile
import app.vanta.launcher.ui.components.CalloutTile
import app.vanta.launcher.ui.components.DateTile
import app.vanta.launcher.ui.components.FitHeadlineText
import app.vanta.launcher.ui.components.HeadlineText
import app.vanta.launcher.ui.components.LocalHapticsEnabled
import app.vanta.launcher.ui.components.LocalTileColors
import app.vanta.launcher.ui.components.LumiaEasing
import app.vanta.launcher.ui.components.MonoLabel
import app.vanta.launcher.ui.components.QuoteTile
import app.vanta.launcher.ui.components.SectionLabel
import app.vanta.launcher.ui.components.SemanticZoomGrid
import app.vanta.launcher.ui.components.Tile
import app.vanta.launcher.ui.components.TileCaptions
import app.vanta.launcher.ui.components.TileDefaults
import app.vanta.launcher.ui.components.TileStyle
import app.vanta.launcher.ui.components.WeatherTile
import app.vanta.launcher.ui.components.WpAppBar
import app.vanta.launcher.ui.components.WpGlyph
import app.vanta.launcher.ui.components.tilePress
import app.vanta.launcher.ui.nav.StandardAppViewModel
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.StandardType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PagePadding = 12.dp
private val Gutter = TileDefaults.Gutter
private const val FallbackQuote = "A DISCIPLINED MIND CREATES A FREER LIFE."

/** One row of the ALL APPS list: a letter header or a continuously numbered app. */
private sealed interface Entry {
    val key: String

    data class Letter(val letter: String) : Entry {
        override val key get() = "L:$letter"
    }

    data class App(val app: AppItem, val number: Int) : Entry {
        override val key get() = app.packageName
    }
}

private data class AppRowMenuAction(val label: String, val onClick: () -> Unit)

private data class MostUsedItem(val app: AppItem, val timeText: String)

private fun formatUsageTime(ms: Long): String {
    val totalMinutes = ms / 60_000
    return if (totalMinutes >= 60) {
        val hours = totalMinutes / 60
        val tenths = (totalMinutes % 60) * 10 / 60
        "${hours}.${tenths}h"
    } else {
        "${totalMinutes}m"
    }
}

private fun letterOf(app: AppItem): String =
    app.label.firstOrNull()?.takeIf { it.isLetter() }?.uppercase() ?: "#"

private fun buildEntries(apps: List<AppItem>, query: String, sortMode: String, usageCount: Map<String, Int> = emptyMap()): List<Entry> {
    val q = query.trim()
    val filtered = if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    var n = 0
    return if (sortMode == "mostUsed") {
        buildList {
            filtered.sortedWith(compareByDescending<AppItem> { usageCount[it.packageName] ?: 0 }.thenBy { it.label.lowercase() })
                .forEach { add(Entry.App(it, ++n)) }
        }
    } else {
        buildList {
            filtered.groupBy(::letterOf).toSortedMap().forEach { (letter, group) ->
                add(Entry.Letter(letter))
                group.forEach { add(Entry.App(it, ++n)) }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DrawerScreen(viewModel: StandardAppViewModel, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val hapticsOn = LocalHapticsEnabled.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val pinned by viewModel.pinnedApps.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf("alpha") }
    val listState = rememberLazyListState()
    var zoomOpen by remember { mutableStateOf(false) }
    // Rows turnstile in on first show and again whenever search or sort rebuilds the list.
    val listEpoch = remember(searchQuery, sortMode) { SystemClock.uptimeMillis() }
    var pinNotice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pinNotice) {
        if (pinNotice != null) {
            delay(1500)
            pinNotice = null
        }
    }

    val quotes = settings.quotes
    var quoteIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(quotes.size) {
        while (quotes.size > 1) {
            delay(8_000)
            quoteIndex++
        }
    }
    val quote = if (quotes.isEmpty()) FallbackQuote else quotes[quoteIndex % quotes.size]

    val recentsPrefs = context.getSharedPreferences("standard_recents", Context.MODE_PRIVATE)
    val hiddenPrefs = context.getSharedPreferences("standard_hidden", Context.MODE_PRIVATE)
    val recentsRaw = remember { recentsPrefs.getString("recents", "") ?: "" }
    var hiddenRaw by remember { mutableStateOf(hiddenPrefs.getString("hidden", "") ?: "") }
    val hiddenApps = remember(hiddenRaw) { hiddenRaw.split(",").filter { it.isNotBlank() }.toSet() }

    val usageCountPrefs = context.getSharedPreferences("standard_app_usage", Context.MODE_PRIVATE)
    val usageLastPrefs = context.getSharedPreferences("standard_app_last_open", Context.MODE_PRIVATE)
    var usageVersion by remember { mutableIntStateOf(0) }
    val usageCountMap = remember(usageVersion) {
        usageCountPrefs.all.mapValues { (_, v) -> (v as? Int) ?: 0 }
    }

    val showHeader = searchQuery.isBlank()
    val visibleApps = remember(allApps, hiddenApps) { allApps.filter { it.packageName !in hiddenApps } }
    val byPkg = remember(visibleApps) { visibleApps.associateBy { it.packageName } }
    val recents: List<AppItem> = remember(recentsRaw, byPkg, pinned, hiddenApps) {
        if (recentsRaw.isNotBlank()) recentsRaw.split(",").mapNotNull { byPkg[it.trim()] }.take(4)
        else pinned.filter { it.packageName !in hiddenApps }.take(4)
    }
    val recentsVisible = showHeader && recents.isNotEmpty()
    val hasUsagePermission = remember {
        try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }
    val mostUsedItems: List<MostUsedItem> = remember(hasUsagePermission, usageCountMap, byPkg) {
        if (hasUsagePermission) {
            try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val now = System.currentTimeMillis()
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 30L * 24 * 60 * 60 * 1000, now) ?: emptyList()
                stats.filter { it.totalTimeInForeground > 0 }
                    .groupBy { it.packageName }
                    .mapValues { (_, list) -> list.sumOf { it.totalTimeInForeground } }
                    .toList()
                    .sortedByDescending { it.second }
                    .mapNotNull { (pkg, time) -> byPkg[pkg]?.let { MostUsedItem(it, formatUsageTime(time)) } }
                    .take(4)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            visibleApps.mapNotNull { app ->
                val count = usageCountMap[app.packageName] ?: 0
                if (count > 0) MostUsedItem(app, "${count}-") else null
            }
                .sortedWith(compareByDescending<MostUsedItem> { usageCountMap[it.app.packageName] ?: 0 }.thenBy { it.app.label.lowercase() })
                .take(4)
        }
    }
    val mostUsedVisible = showHeader && mostUsedItems.isNotEmpty()
    val entries = remember(searchQuery, visibleApps, sortMode, usageCountMap) { buildEntries(visibleApps, searchQuery, sortMode, usageCountMap) }
    val prefix = (if (showHeader) 1 else 0) + (if (mostUsedVisible) 1 else 0) + (if (recentsVisible) 1 else 0) + 1
    val letterIndex = remember(entries, prefix) {
        buildMap { entries.forEachIndexed { i, e -> if (e is Entry.Letter) put(e.letter, prefix + i) } }
    }
    val letters = remember(letterIndex) { letterIndex.keys.toList() }
    val launch: (AppItem) -> Unit = {
        val current = usageCountPrefs.getInt(it.packageName, 0)
        usageCountPrefs.edit().putInt(it.packageName, current + 1).apply()
        usageLastPrefs.edit().putLong(it.packageName, System.currentTimeMillis()).apply()
        usageVersion++
        viewModel.launchApp(context, it.packageName)
    }
    val togglePin: (AppItem) -> Unit = { app ->
        viewModel.togglePin(app)
        if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        pinNotice = (if (app.pinned) "UNPINNED " else "PINNED ") + app.label.uppercase()
    }
    val hideApp: (AppItem) -> Unit = { app ->
        val list = hiddenRaw.split(",").filter { it.isNotBlank() }.toMutableList()
        if (app.packageName !in list) list.add(app.packageName)
        hiddenPrefs.edit().putString("hidden", list.joinToString(",")).apply()
        hiddenRaw = list.joinToString(",")
        if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        pinNotice = "HIDDEN " + app.label.uppercase()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(hapticsOn) {
                // Two-finger pinch only. Single-finger gestures are never consumed here, so the
                // list scrolls and the pager still swipes between Apps and Focus.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var zoom = 1f
                    var fired = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.count { it.pressed }
                        if (pressed == 0) break
                        if (pressed < 2) continue
                        zoom *= event.calculateZoom()
                        event.changes.forEach { it.consume() }
                        if (fired) continue
                        val open = when {
                            zoom < 0.8f -> true
                            zoom > 1.25f -> false
                            else -> continue
                        }
                        fired = true
                        zoomOpen = open
                        if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
    ) {
        val gridColumns = when {
            maxWidth < 600.dp -> 1
            maxWidth < 900.dp -> 2
            maxWidth < 1200.dp -> 3
            else -> 4
        }
        val gridState = rememberLazyGridState()
        val currentLetter by remember(letterIndex, gridColumns) {
            derivedStateOf {
                val first = if (gridColumns >= 2) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
                letterIndex.entries.lastOrNull { it.value <= first }?.key
            }
        }
        val jumpTo: (String, Boolean) -> Unit = { letter, animate ->
            letterIndex[letter]?.let { index ->
                scope.launch {
                    when {
                        gridColumns >= 2 && animate -> gridState.animateScrollToItem(index)
                        gridColumns >= 2 -> gridState.scrollToItem(index)
                        animate -> listState.animateScrollToItem(index)
                        else -> listState.scrollToItem(index)
                    }
                }
            }
        }
        val firstVisible: () -> Int = {
            if (gridColumns >= 2) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
        }
        val launchFirstResult: () -> Unit = {
            entries.firstNotNullOfOrNull { (it as? Entry.App)?.app }?.let(launch)
        }
        Column(modifier = Modifier.fillMaxSize()) {
            SearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = launchFirstResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PagePadding, vertical = 4.dp)
            )
            if (gridColumns >= 2) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    state = gridState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showHeader) {
                        item(key = "top", contentType = "top", span = { GridItemSpan(maxLineSpan) }) {
                            TopSection(
                                viewModel = viewModel,
                                quote = quote,
                                onLaunch = launch,
                                onTogglePin = togglePin,
                                onOpenSettings = onOpenSettings,
                                modifier = Modifier.padding(horizontal = PagePadding, vertical = 8.dp)
                            )
                        }
                    }
                    if (mostUsedVisible) {
                        item(key = "mostUsed", contentType = "mostUsed", span = { GridItemSpan(maxLineSpan) }) {
                            MostUsedRow(
                                apps = mostUsedItems,
                                onLaunch = launch,
                                onTogglePin = togglePin,
                                modifier = Modifier.padding(horizontal = PagePadding, vertical = 6.dp)
                            )
                        }
                    }
                    if (recentsVisible) {
                        item(key = "recents", contentType = "recents", span = { GridItemSpan(maxLineSpan) }) {
                            RecentlyUsedRow(
                                apps = recents,
                                onLaunch = launch,
                                onTogglePin = togglePin,
                                modifier = Modifier.padding(horizontal = PagePadding, vertical = 6.dp)
                            )
                        }
                    }
                    item(key = "all", contentType = "label", span = { GridItemSpan(maxLineSpan) }) {
                        SectionLabel(
                            when {
                                !showHeader -> "RESULTS"
                                sortMode == "mostUsed" -> "MOST USED"
                                else -> "ALL APPS"
                            },
                            modifier = Modifier.padding(horizontal = PagePadding, vertical = 6.dp)
                        )
                    }
                    if (entries.isEmpty()) {
                        item(key = "empty", contentType = "label", span = { GridItemSpan(maxLineSpan) }) {
                            HeadlineText(
                                if (showHeader) "NO APPS" else "NO RESULTS",
                                28.sp,
                                color = colors.muted,
                                modifier = Modifier.padding(PagePadding)
                            )
                        }
                    }
                    entries.forEachIndexed { i, entry ->
                        when (entry) {
                            is Entry.Letter -> item(key = entry.key, contentType = Entry.Letter::class, span = { GridItemSpan(maxLineSpan) }) {
                                LetterHeader(
                                    letter = entry.letter,
                                    onTap = { zoomOpen = true },
                                    modifier = Modifier.turnstileIn(prefix + i, listEpoch, firstVisible)
                                )
                            }
                            is Entry.App -> item(key = entry.key, contentType = Entry.App::class) {
                                AppEntryRow(
                                    entry = entry,
                                    onLaunch = launch,
                                    onTogglePin = togglePin,
                                    onHide = hideApp,
                                    modifier = Modifier.turnstileIn(prefix + i, listEpoch, firstVisible)
                                )
                            }
                        }
                    }
                    item(key = "settings", contentType = "settings", span = { GridItemSpan(maxLineSpan) }) {
                        SettingsRow(onOpenSettings, Modifier.padding(horizontal = PagePadding).padding(top = 18.dp))
                    }
                    item(key = "footer", contentType = "footer", span = { GridItemSpan(maxLineSpan) }) {
                        Footer(Modifier.padding(horizontal = PagePadding).padding(bottom = 16.dp))
                    }
                    item(key = "appBarSpacer", contentType = "spacer", span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(Modifier.height(72.dp))
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showHeader) {
                        item(key = "top", contentType = "top") {
                            TopSection(
                                viewModel = viewModel,
                                quote = quote,
                                onLaunch = launch,
                                onTogglePin = togglePin,
                                onOpenSettings = onOpenSettings,
                                modifier = Modifier.padding(horizontal = PagePadding, vertical = 8.dp)
                            )
                        }
                    }
                    if (mostUsedVisible) {
                        item(key = "mostUsed", contentType = "mostUsed") {
                            MostUsedRow(
                                apps = mostUsedItems,
                                onLaunch = launch,
                                onTogglePin = togglePin,
                                modifier = Modifier.padding(horizontal = PagePadding, vertical = 6.dp)
                            )
                        }
                    }
                    if (recentsVisible) {
                        item(key = "recents", contentType = "recents") {
                            RecentlyUsedRow(
                                apps = recents,
                                onLaunch = launch,
                                onTogglePin = togglePin,
                                modifier = Modifier.padding(horizontal = PagePadding, vertical = 6.dp)
                            )
                        }
                    }
                    item(key = "all", contentType = "label") {
                        SectionLabel(
                            when {
                                !showHeader -> "RESULTS"
                                sortMode == "mostUsed" -> "MOST USED"
                                else -> "ALL APPS"
                            },
                            modifier = Modifier.padding(horizontal = PagePadding, vertical = 6.dp)
                        )
                    }
                    if (entries.isEmpty()) {
                        item(key = "empty", contentType = "label") {
                            HeadlineText(
                                if (showHeader) "NO APPS" else "NO RESULTS",
                                28.sp,
                                color = colors.muted,
                                modifier = Modifier.padding(PagePadding)
                            )
                        }
                    }
                    entries.forEachIndexed { i, entry ->
                        when (entry) {
                            is Entry.Letter -> item(key = entry.key, contentType = Entry.Letter::class) {
                                LetterHeader(
                                    letter = entry.letter,
                                    onTap = { zoomOpen = true },
                                    modifier = Modifier.turnstileIn(prefix + i, listEpoch, firstVisible)
                                )
                            }
                            is Entry.App -> item(key = entry.key, contentType = Entry.App::class) {
                                AppEntryRow(
                                    entry = entry,
                                    onLaunch = launch,
                                    onTogglePin = togglePin,
                                    onHide = hideApp,
                                    modifier = Modifier.turnstileIn(prefix + i, listEpoch, firstVisible)
                                )
                            }
                        }
                    }
                    item(key = "settings", contentType = "settings") {
                        SettingsRow(onOpenSettings, Modifier.padding(horizontal = PagePadding).padding(top = 18.dp))
                    }
                    item(key = "footer", contentType = "footer") {
                        Footer(Modifier.padding(horizontal = PagePadding).padding(bottom = 16.dp))
                    }
                    item(key = "appBarSpacer", contentType = "spacer") {
                        Spacer(Modifier.height(72.dp))
                    }
                }
            }
        }

        WpAppBar(
            actions = listOf(
                AppBarAction("SORT", WpGlyph.Sort) {
                    sortMode = if (sortMode == "alpha") "mostUsed" else "alpha"
                    pinNotice = if (sortMode == "alpha") "SORTED A-Z" else "SORTED BY USE"
                },
                AppBarAction("REFRESH", WpGlyph.Refresh) {
                    viewModel.refreshWeather()
                    pinNotice = "REFRESHING WEATHER"
                },
                AppBarAction("SETTINGS", WpGlyph.Settings) { onOpenSettings() }
            ),
            menuItems = listOf("SCROLL TO TOP", "LETTER GRID"),
            onMenuItemClick = { item ->
                when (item) {
                    "SCROLL TO TOP" -> scope.launch {
                        if (gridColumns >= 2) gridState.animateScrollToItem(0) else listState.animateScrollToItem(0)
                    }
                    "LETTER GRID" -> zoomOpen = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )

        AlphabetScrubber(
            letters = letters,
            current = currentLetter,
            onLetter = { jumpTo(it, false) },
            onCurrentTap = { zoomOpen = true },
            scrolling = if (gridColumns >= 2) gridState.isScrollInProgress else listState.isScrollInProgress,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        pinNotice?.let { text ->
            Tile(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                style = TileStyle.Ink,
                contentPadding = 10.dp
            ) {
                MonoLabel(text, size = 11.sp, color = LocalTileColors.current.content, maxLines = 1)
            }
        }

        SemanticZoomGrid(
            visible = zoomOpen,
            available = letterIndex.keys,
            onLetterSelected = { letter ->
                zoomOpen = false
                jumpTo(letter, true)
            },
            onDismiss = { zoomOpen = false }
        )
    }
}

/**
 * Lumia list entrance: each row swings in around its left edge, staggered 25ms by its distance
 * from the first visible row (cap 300ms). Re-runs when [epoch] changes; rows composed later by
 * scrolling snap straight in so the list never flickers mid-scroll.
 */
@Composable
private fun Modifier.turnstileIn(index: Int, epoch: Long, firstVisible: () -> Int): Modifier {
    val enter = remember { Animatable(if (SystemClock.uptimeMillis() - epoch > 600L) 1f else 0f) }
    LaunchedEffect(epoch) {
        if (SystemClock.uptimeMillis() - epoch > 600L) {
            enter.snapTo(1f)
            return@LaunchedEffect
        }
        enter.snapTo(0f)
        delay(((index - firstVisible()).coerceAtLeast(0) * 25L).coerceAtMost(300L))
        enter.animateTo(1f, tween(320, easing = LumiaEasing))
    }
    return graphicsLayer {
        val p = enter.value
        if (p < 1f) {
            transformOrigin = TransformOrigin(0f, 0.5f)
            rotationY = 60f * (1f - p)
            alpha = p
            cameraDistance = 14f * density
        }
    }
}

/** Letter section header; tapping it opens the semantic zoom grid, like Windows Phone. */
@Composable
private fun LetterHeader(letter: String, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppTheme.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background.copy(alpha = 0.92f))
            .tilePress(onTap = onTap, tilt = false)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = "Show letters") { onTap(); true }
            }
            .padding(start = PagePadding, top = 12.dp, bottom = 2.dp)
    ) {
        HeadlineText(letter, 28.sp)
    }
}

@Composable
private fun AppEntryRow(
    entry: Entry.App,
    onLaunch: (AppItem) -> Unit,
    onTogglePin: (AppItem) -> Unit,
    onHide: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = entry.app
    NumberedAppRowWithIcon(
        index = entry.number,
        app = app,
        onLaunch = { onLaunch(app) },
        onLongPress = { onTogglePin(app) },
        pinned = app.pinned,
        menuActions = listOf(
            AppRowMenuAction(if (app.pinned) "UNPIN FROM HOME" else "PIN TO HOME") { onTogglePin(app) },
            AppRowMenuAction("HIDE APP") { onHide(app) },
            AppRowMenuAction("APP INFO") {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${app.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { context.startActivity(intent) } catch (_: Exception) {}
            }
        ),
        modifier = modifier
            .padding(horizontal = PagePadding)
            .appRowSemantics(app, onLaunch, onTogglePin)
    )
}

/** TalkBack: name the row, and expose Open / Pin-Unpin as actions since the press is gesture-driven. */
private fun Modifier.appRowSemantics(
    app: AppItem,
    onLaunch: (AppItem) -> Unit,
    onTogglePin: (AppItem) -> Unit
): Modifier = semantics(mergeDescendants = true) {
    role = Role.Button
    contentDescription = if (app.pinned) "${app.label}, pinned" else app.label
    onClick(label = "Open") { onLaunch(app); true }
    onLongClick(label = if (app.pinned) "Unpin" else "Pin") { onTogglePin(app); true }
}

@Composable
private fun NumberedAppRowWithIcon(
    index: Int,
    app: AppItem,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    pinned: Boolean = false,
    labelSize: TextUnit = 24.sp,
    minHeight: Dp = 56.dp,
    menuActions: List<AppRowMenuAction> = emptyList()
) {
    val colors = LocalAppTheme.current
    var menuExpanded by remember { mutableStateOf(false) }
    Tile(
        modifier = modifier.fillMaxWidth(),
        style = TileStyle.Outline,
        contentPadding = 0.dp,
        onClick = onLaunch,
        onLongClick = {
            if (menuActions.isNotEmpty()) menuExpanded = true else onLongPress?.invoke()
        }
    ) {
        val c = LocalTileColors.current.content
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonoLabel("%02d".format(index), size = 11.sp, color = c, modifier = Modifier.padding(horizontal = 12.dp))
            AppIcon(packageName = app.packageName, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            // Shrinks to fit its slot so a label never ellipsizes to "YOU...".
            FitHeadlineText(
                app.label.uppercase(),
                maxSize = labelSize,
                minSize = 14.sp,
                color = c,
                modifier = Modifier.weight(1f).padding(end = 12.dp, top = 8.dp, bottom = 8.dp)
            )
            if (pinned) {
                Box(Modifier.size(8.dp).background(colors.accent))
                Spacer(Modifier.width(10.dp))
            }
            MonoLabel("→", size = 18.sp, color = c, weight = FontWeight.Bold, modifier = Modifier.padding(end = 14.dp))
        }
        if (menuExpanded) RowMenu(menuActions, onDismiss = { menuExpanded = false })
    }
}

/** Long-press menu for an app row: an outline tile of mono actions that scales in beside the row. */
@Composable
private fun RowMenu(actions: List<AppRowMenuAction>, onDismiss: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(180, easing = LumiaEasing)) }
    val offsetPx = with(LocalDensity.current) { 44.dp.roundToPx() }
    Popup(
        offset = IntOffset(offsetPx, offsetPx),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Tile(
            modifier = Modifier
                .width(220.dp)
                .graphicsLayer {
                    val p = progress.value
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = 0.92f + 0.08f * p
                    scaleY = 0.92f + 0.08f * p
                    alpha = p
                },
            style = TileStyle.Outline,
            contentPadding = 0.dp
        ) {
            val c = LocalTileColors.current.content
            Column {
                actions.forEach { action ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .tilePress(onTap = { onDismiss(); action.onClick() }, tilt = false)
                            .semantics {
                                role = Role.Button
                                onClick { onDismiss(); action.onClick(); true }
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        MonoLabel(action.label, size = 12.sp, color = c)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyUsedRow(
    apps: List<AppItem>,
    onLaunch: (AppItem) -> Unit,
    onTogglePin: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalTileColors.current.content
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel("RECENTLY USED", modifier = Modifier.padding(bottom = 6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Gutter)
        ) {
            apps.forEach { app ->
                Tile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    style = TileStyle.Outline,
                    contentPadding = 6.dp,
                    onClick = { onLaunch(app) },
                    onLongClick = { onTogglePin(app) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppIcon(packageName = app.packageName, size = 32.dp)
                        // ponytail: 4-across tiles are ~70dp wide, so the fit range is 14-9sp with a 2-line fallback.
                        FitHeadlineText(app.label.uppercase(), maxSize = 14.sp, minSize = 9.sp, color = c, maxLines = 2, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun MostUsedRow(
    apps: List<MostUsedItem>,
    onLaunch: (AppItem) -> Unit,
    onTogglePin: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalTileColors.current.content
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel("MOST USED", modifier = Modifier.padding(bottom = 6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Gutter)
        ) {
            apps.forEach { item ->
                Tile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    style = TileStyle.Outline,
                    contentPadding = 6.dp,
                    onClick = { onLaunch(item.app) },
                    onLongClick = { onTogglePin(item.app) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppIcon(packageName = item.app.packageName, size = 32.dp)
                        FitHeadlineText(item.app.label.uppercase(), maxSize = 14.sp, minSize = 9.sp, color = c, maxLines = 2, textAlign = TextAlign.Center)
                        MonoLabel(item.timeText, size = 8.sp, color = c, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * The mockup's two-column top: headline + numbered pinned rows on the left; weather, callout,
 * quote, date and QUICK TOOLS on the right. IntrinsicSize.Max lets the last right tile fill.
 */
@Composable
private fun TopSection(
    viewModel: StandardAppViewModel,
    quote: String,
    onLaunch: (AppItem) -> Unit,
    onTogglePin: (AppItem) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pinned by viewModel.pinnedApps.collectAsState()
    val quickTools by viewModel.quickTools.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val weatherLoading by viewModel.weatherLoading.collectAsState()
    val weatherError by viewModel.weatherError.collectAsState()
    val forecast by viewModel.forecast.collectAsState()

    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(Gutter)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // One word per line; shrinks until BETTER fits the column so a word never breaks.
            FitHeadlineText("GOOD APPS BETTER DAYS.", maxSize = 64.sp, stacked = true)
            SectionLabel(
                "UTILITIES / CREATIVITY / LIFE",
                modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
                size = 9.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pinned.forEachIndexed { i, app ->
                    NumberedAppRowWithIcon(
                        index = i + 1,
                        app = app,
                        onLaunch = { onLaunch(app) },
                        onLongPress = { onTogglePin(app) },
                        labelSize = 18.sp,
                        minHeight = 60.dp,
                        modifier = Modifier.appRowSemantics(app, onLaunch, onTogglePin)
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Gutter)) {
            WeatherTile(
                weather = weather,
                isLoading = weatherLoading,
                error = weatherError,
                forecast = forecast,
                modifier = Modifier.fillMaxWidth().height(250.dp),
                onRefresh = viewModel::refreshWeather,
                onSetLocation = onOpenSettings
            )
            CalloutTile(
                headline = stringResource(R.string.callout_make_shit),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        onClick(label = "Open settings") { onOpenSettings(); true }
                    },
                bottomCaption = "PLANS / PROGRESS / RESULTS",
                headlineSize = 38.sp,
                onClick = onOpenSettings
            )
            // ponytail: quote and date stack instead of sitting side by side; at half of a 188dp
            // column DateTile's "SEP 2026" and any 11-letter word in the quote ellipsize.
            QuoteTile(quote, modifier = Modifier.fillMaxWidth().height(110.dp), size = 10.sp)
            DateTile(modifier = Modifier.fillMaxWidth().height(150.dp), daySize = 44.sp)
            QuickToolsTile(quickTools, onLaunch, modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }
}

/** QUICK TOOLS // header and a 2x2 grid of the user's quick tools with hairline dividers. */
@Composable
private fun QuickToolsTile(tools: List<AppItem>, onLaunch: (AppItem) -> Unit, modifier: Modifier = Modifier) {
    Tile(modifier = modifier, contentPadding = 0.dp) {
        val c = LocalTileColors.current.content
        val hairline = c.copy(alpha = 0.35f)
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MonoLabel("QUICK TOOLS", size = 11.sp, color = c)
                MonoLabel("//", size = 11.sp, color = c)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .drawBehind {
                        val w = 1.dp.toPx()
                        drawLine(hairline, Offset(0f, 0f), Offset(size.width, 0f), w)
                        drawLine(hairline, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), w)
                        drawLine(hairline, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), w)
                    }
            ) {
                repeat(2) { r ->
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        repeat(2) { col ->
                            val app = tools.getOrNull(r * 2 + col)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .tilePress(onTap = { app?.let(onLaunch) }, enabled = app != null, tilt = false)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (app != null) {
                                    FitHeadlineText(app.label.uppercase(), maxSize = 16.sp, minSize = 11.sp, color = c)
                                    MonoLabel(
                                        app.caption ?: TileCaptions.defaultFor(app.packageName, app.label),
                                        size = 9.sp,
                                        color = c.copy(alpha = 0.8f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Square 2dp search box: mono text, "SEARCH" placeholder, a "//" caret marker that turns accent
 * with the border on focus, and an x square to clear. IME Search opens the first result.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val outlineColor by animateColorAsState(
        targetValue = if (focused) colors.accent else colors.ink,
        animationSpec = tween(180, easing = LumiaEasing),
        label = "searchOutline"
    )
    val markerColor by animateColorAsState(
        targetValue = if (focused) colors.accent else colors.muted,
        animationSpec = tween(180, easing = LumiaEasing),
        label = "searchMarker"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val w = TileDefaults.Border.toPx()
                drawRect(
                    color = outlineColor,
                    topLeft = Offset(w / 2f, w / 2f),
                    size = Size(size.width - w, size.height - w),
                    style = Stroke(w)
                )
            }
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 14.dp)
                .onFocusChanged { focused = it.isFocused },
            singleLine = true,
            textStyle = StandardType.mono(13.sp).copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = {
                focus.clearFocus()
                onSearch()
            }),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) MonoLabel("SEARCH", size = 13.sp, color = colors.muted)
                    inner()
                }
            }
        )
        MonoLabel("//", size = 13.sp, color = markerColor, weight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(tween(180, easing = LumiaEasing)) + scaleIn(tween(180, easing = LumiaEasing), initialScale = 0.8f),
            exit = fadeOut(tween(120, easing = LumiaEasing))
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .tilePress(onTap = { onQueryChange(""); focus.clearFocus() }, tilt = false)
                    .semantics {
                        role = Role.Button
                        onClick(label = "Clear search") { onQueryChange(""); true }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(28.dp).border(TileDefaults.Border, colors.ink), contentAlignment = Alignment.Center) {
                    MonoLabel("×", size = 14.sp, color = colors.ink, weight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Tile(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = "Open settings") { onOpenSettings(); true }
            },
        contentPadding = 12.dp,
        onClick = onOpenSettings
    ) {
        val c = LocalTileColors.current.content
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonoLabel("SETTINGS", size = 12.sp, color = c)
            MonoLabel("→", size = 16.sp, color = c, weight = FontWeight.Bold)
        }
    }
}

/** Outline arrow tile beside the outlined mantra + barcode tile. */
@Composable
private fun Footer(modifier: Modifier = Modifier) {
    val colors = LocalAppTheme.current
    Row(
        modifier = modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(Gutter)
    ) {
        Tile(modifier = Modifier.weight(1f).fillMaxHeight(), style = TileStyle.Outline) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                MonoLabel("→", size = 36.sp, color = colors.accent, weight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                MonoLabel("SAME\nPHONE.\nHIGHER\nSTANDARDS.", size = 11.sp, color = LocalTileColors.current.content)
            }
        }
        BarcodeTile(
            mantra = listOf("FOCUS", "CREATE", "LEARN", "BUILD", "REPEAT"),
            caption = "STANDARD.//2026",
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

/**
 * Translucent letter rail on the right edge. Tap a letter or drag along the rail to scrub;
 * the section under the viewport top is drawn in the accent.
 */
@Composable
private fun AlphabetScrubber(
    letters: List<String>,
    current: String?,
    onLetter: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCurrentTap: () -> Unit = {},
    scrolling: Boolean = false
) {
    if (letters.isEmpty()) return
    val colors = LocalAppTheme.current
    val hapticsOn = LocalHapticsEnabled.current
    val haptics = LocalHapticFeedback.current
    val rowHeight = 20.dp
    val railPadding = 4.dp
    val currentOnLetter by rememberUpdatedState(onLetter)
    // Rail shows while the list moves or a finger is on it, then fades so it never sits on the tiles.
    var dragging by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) shown = true
        else { kotlinx.coroutines.delay(1400); shown = false }
    }
    val railAlpha by androidx.compose.animation.core.animateFloatAsState(if (shown) 1f else 0f, label = "rail")
    Column(
        modifier = modifier
            .graphicsLayer { alpha = railAlpha }
            .background(colors.tile.copy(alpha = 0.8f))
            .pointerInput(letters, hapticsOn) {
                val rowPx = rowHeight.toPx()
                val padPx = railPadding.toPx()
                var last: String? = null
                fun scrubTo(y: Float) {
                    val letter = letters[((y - padPx) / rowPx).toInt().coerceIn(0, letters.lastIndex)]
                    if (letter == last) return
                    last = letter
                    if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnLetter(letter)
                }
                detectVerticalDragGestures(
                    onDragStart = { start ->
                        last = null
                        dragging = true
                        scrubTo(start.y)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        scrubTo(change.position.y)
                    }
                )
            }
            .padding(vertical = railPadding, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            val active = letter == current
            Box(
                modifier = Modifier
                    .height(rowHeight)
                    .width(24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        role = Role.Button
                    ) {
                        if (!shown) return@clickable
                        if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (letter == current) onCurrentTap() else onLetter(letter)
                    }
                    .semantics { contentDescription = if (letter == current) "$letter, show letters" else "Jump to $letter" },
                contentAlignment = Alignment.Center
            ) {
                MonoLabel(
                    letter,
                    size = 10.sp,
                    color = if (active) colors.accent else colors.ink,
                    weight = if (active) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
