package app.vanta.launcher.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.domain.model.AnimationStyle
import app.vanta.launcher.domain.model.AppItem
import app.vanta.launcher.domain.model.HomeModule
import app.vanta.launcher.domain.model.IconStyle
import app.vanta.launcher.domain.model.SettingsState
import app.vanta.launcher.domain.model.TileSize
import app.vanta.launcher.ui.components.AppTile
import app.vanta.launcher.ui.components.BatteryTile
import app.vanta.launcher.ui.components.FitHeadlineText
import app.vanta.launcher.ui.components.FloatingSearchBar
import app.vanta.launcher.ui.components.HeadlineText
import app.vanta.launcher.ui.components.LiveTile
import app.vanta.launcher.ui.components.LocalHapticsEnabled
import app.vanta.launcher.ui.components.LocalTileColors
import app.vanta.launcher.ui.components.MusicWidget
import app.vanta.launcher.ui.components.NotesWidget
import app.vanta.launcher.ui.components.TaskItem
import app.vanta.launcher.ui.components.TaskStore
import app.vanta.launcher.ui.components.TasksWidget
import app.vanta.launcher.ui.components.MonoLabel
import app.vanta.launcher.ui.components.PeopleHubTile
import app.vanta.launcher.ui.components.PlusCross
import app.vanta.launcher.ui.components.QuickSettingsFab
import app.vanta.launcher.ui.components.SwipeDownSearch
import app.vanta.launcher.ui.components.Tile
import app.vanta.launcher.ui.components.TileDefaults
import app.vanta.launcher.ui.components.TileEntrance
import app.vanta.launcher.ui.components.WeatherTile
import app.vanta.launcher.ui.components.button
import app.vanta.launcher.ui.components.liveTimeFormatted
import app.vanta.launcher.ui.components.tilePress
import app.vanta.launcher.ui.nav.StandardAppViewModel
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.StandardType
import app.vanta.launcher.util.RefreshRate
import app.vanta.launcher.domain.model.MediaInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

private const val COLUMNS = 4
private val RowHeight = 112.dp
private val LargeRowHeight = 234.dp

private fun TileSize.span(): Int = when (this) {
    TileSize.SMALL -> 1
    TileSize.MEDIUM -> 2
    TileSize.WIDE, TileSize.LARGE -> COLUMNS
}

private fun TileSize.prev(): TileSize = when (this) {
    TileSize.SMALL -> TileSize.LARGE
    TileSize.MEDIUM -> TileSize.SMALL
    TileSize.WIDE -> TileSize.MEDIUM
    TileSize.LARGE -> TileSize.WIDE
}

/** Greedy pack into rows of [COLUMNS] spans; a row closes when the next tile does not fit. */
internal fun packRows(apps: List<AppItem>): List<List<AppItem>> {
    val rows = mutableListOf<List<AppItem>>()
    var row = mutableListOf<AppItem>()
    var used = 0
    for (app in apps) {
        val s = app.tileSize.span()
        val full = s >= COLUMNS
        if (full && row.isNotEmpty()) {
            rows += row
            row = mutableListOf()
            used = 0
        }
        if (used + s > COLUMNS) {
            rows += row
            row = mutableListOf()
            used = 0
        }
        row += app
        used += s
        if (full) {
            rows += row
            row = mutableListOf()
            used = 0
        }
    }
    if (row.isNotEmpty()) rows += row
    return rows
}

@Composable
fun HomeScreen(
    viewModel: StandardAppViewModel,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFocus: () -> Unit = {}
) {
    val context = LocalContext.current
    val pinned by viewModel.pinnedApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val weatherLoading by viewModel.weatherLoading.collectAsState()
    val weatherError by viewModel.weatherError.collectAsState()
    val forecast by viewModel.forecast.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val contactsPermissionGranted by viewModel.contactsPermissionGranted.collectAsState()
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val battery by viewModel.battery.collectAsState()
    val editMode by viewModel.editMode.collectAsState()
    val modules = settings.homeModules
    var contextMenuTile by remember { mutableStateOf<AppItem?>(null) }
    var showSwipeSearch by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (editMode) 0.7f else 1f,
        animationSpec = tween(200),
        label = "contentAlpha"
    )

    val hapticsOn = LocalHapticsEnabled.current
    val haptics = LocalHapticFeedback.current
    val enterEdit = {
        if (!viewModel.editMode.value) {
            if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.setEditMode(true)
        }
    }
    val launch: (String) -> Unit = { viewModel.launchApp(context, it) }
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.3f)
    val scrollState = rememberScrollState()

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectVerticalDragGestures(onVerticalDrag = { change, amount ->
                if (change.position.y < 50f && amount > 5f) showSwipeSearch = true
            })
        }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(hapticsOn) { detectTapGestures(onLongPress = { enterEdit() }) }
                .verticalScroll(scrollState)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
        ) {
            if (editMode) EditBar(onDone = { viewModel.setEditMode(false) })
            if (editMode) QuickOptions(settings = settings, pinned = pinned, viewModel = viewModel)

            // Every top-level block turnstiles in with a continuous stagger index.
            var entrance = 0

            if (HomeModule.SEARCH in modules) {
                TileEntrance(index = entrance++) {
                    Box(modifier = Modifier.alpha(contentAlpha)) {
                        FloatingSearchBar(apps = allApps, onLaunch = launch, onPin = { viewModel.togglePin(it) })
                    }
                }
            }

            TileEntrance(index = entrance++) {
                Row(
                    modifier = Modifier.fillMaxWidth().alpha(contentAlpha).heightIn(min = 190.dp).height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
                ) {
                    DayTile(modifier = Modifier.weight(1f).fillMaxHeight())
                    WeatherTile(
                        weather = weather,
                        isLoading = weatherLoading,
                        error = weatherError,
                        forecast = forecast,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onRefresh = { viewModel.refreshWeather() },
                        onSetLocation = onOpenSettings
                    )
                }
            }

            TileEntrance(index = entrance++) {
                HomeNowClock(
                    nowPlaying = nowPlaying,
                    modifier = Modifier.fillMaxWidth().alpha(contentAlpha)
                )
            }

            if (pinned.isEmpty()) {
                TileEntrance(index = entrance++) {
                    Tile(
                        modifier = Modifier.fillMaxWidth().height(RowHeight * fontScale).button("Pin apps in drawer", onOpenDrawer),
                        onClick = onOpenDrawer
                    ) {
                        val c = LocalTileColors.current.content
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            HeadlineText("PIN APPS", 32.sp, color = c)
                            MonoLabel("IN DRAWER →", size = 10.sp, color = c)
                        }
                    }
                }
            } else {
                val rows = remember(pinned) { packRows(pinned) }
                PinnedGrid(
                    rows = rows,
                    firstIndex = entrance,
                    pinned = pinned,
                    settings = settings,
                    editMode = editMode,
                    fontScale = fontScale,
                    viewModel = viewModel,
                    launch = launch,
                    enterEdit = enterEdit,
                    contextMenuTile = contextMenuTile,
                    onContextMenuTile = { contextMenuTile = it }
                )
                entrance += rows.size
            }

            val taskStore = remember { TaskStore(context) }
            var tasks by remember { mutableStateOf(taskStore.load()) }
            TileEntrance(index = entrance++) {
                TasksWidget(
                    tasks = tasks,
                    onToggle = { id -> tasks = tasks.map { if (it.id == id) it.copy(completed = !it.completed) else it }; taskStore.save(tasks) },
                    // ids must be unique for the keyed list: next = max + 1, not size (size repeats after a removal).
                    onAdd = { text -> tasks = tasks + TaskItem((tasks.maxOfOrNull { it.id } ?: -1) + 1, text, false); taskStore.save(tasks) },
                    onRemove = { id -> tasks = tasks.filterNot { it.id == id }; taskStore.save(tasks) },
                    modifier = Modifier.fillMaxWidth().alpha(contentAlpha)
                )
            }

            val notesPrefs = remember { context.getSharedPreferences("standard_notes", android.content.Context.MODE_PRIVATE) }
            var notes by remember { mutableStateOf(notesPrefs.getString("notes", "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()) }
            TileEntrance(index = entrance++) {
                NotesWidget(
                    notes = notes,
                    onAdd = { text -> notes = notes + text; notesPrefs.edit().putString("notes", notes.joinToString("\n")).apply() },
                    onRemove = { idx -> notes = notes.filterIndexed { i, _ -> i != idx }; notesPrefs.edit().putString("notes", notes.joinToString("\n")).apply() },
                    modifier = Modifier.fillMaxWidth().alpha(contentAlpha)
                )
            }

            if (HomeModule.MEDIA in modules) {
                val albumArt by viewModel.albumArt.collectAsState()
                TileEntrance(index = entrance++) {
                    MusicWidget(
                        title = nowPlaying?.title,
                        artist = nowPlaying?.artist,
                        isPlaying = nowPlaying?.isPlaying ?: false,
                        albumArt = albumArt,
                        onPlayPause = { viewModel.mediaPlayPause() },
                        onNext = { viewModel.mediaNext() },
                        onPrevious = { viewModel.mediaPrevious() },
                        modifier = Modifier.fillMaxWidth().alpha(contentAlpha)
                    )
                }
            }
            if (HomeModule.BATTERY in modules) {
                TileEntrance(index = entrance++) {
                    Box(modifier = Modifier.alpha(contentAlpha)) {
                        BatteryTile(percent = battery.percent, isCharging = battery.isCharging)
                    }
                }
            }
            if (HomeModule.PEOPLE in modules) {
                TileEntrance(index = entrance++) {
                    Box(modifier = Modifier.alpha(contentAlpha)) {
                        PeopleHubTile(
                            contacts = contacts,
                            permissionGranted = contactsPermissionGranted,
                            onPermission = { viewModel.setContactsPermission(it) }
                        )
                    }
                }
            }

            TileEntrance(index = entrance++) {
                Row(
                    modifier = Modifier.fillMaxWidth().alpha(contentAlpha).heightIn(min = 96.dp * fontScale).height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
                ) {
                    Tile(
                        modifier = Modifier.weight(1f).fillMaxHeight().button("Open apps", onOpenDrawer),
                        onClick = onOpenDrawer
                    ) {
                        val accent = LocalAppTheme.current.accent
                        val content = LocalTileColors.current.content
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PlusCross(color = accent, size = 18.dp)
                            Spacer(Modifier.height(8.dp))
                            MonoLabel("APPS", size = 10.sp, color = content)
                        }
                    }
                    QuoteLine(quotes = settings.quotes, modifier = Modifier.weight(2f).padding(horizontal = 6.dp))
                    SettingsCaption(modifier = Modifier.weight(1f).fillMaxHeight(), onClick = onOpenSettings)
                }
            }
            // Room for the fixed quick-settings square so it never sits on the last row.
            if (HomeModule.QUICK_SETTINGS in modules) Spacer(Modifier.height(56.dp))
        }

        if (HomeModule.QUICK_SETTINGS in modules) {
            QuickSettingsFab(
                modifier = Modifier.fillMaxSize(),
                onToggleNight = {
                    val next = when (settings.darkMode) {
                        app.vanta.launcher.domain.model.DarkMode.AUTO_SYSTEM -> app.vanta.launcher.domain.model.DarkMode.DARK
                        app.vanta.launcher.domain.model.DarkMode.DARK -> app.vanta.launcher.domain.model.DarkMode.LIGHT
                        app.vanta.launcher.domain.model.DarkMode.LIGHT -> app.vanta.launcher.domain.model.DarkMode.AUTO_SYSTEM
                        app.vanta.launcher.domain.model.DarkMode.AUTO_TIME -> app.vanta.launcher.domain.model.DarkMode.DARK
                    }
                    viewModel.setDarkMode(next == app.vanta.launcher.domain.model.DarkMode.DARK)
                }
            )
        }

        if (showSwipeSearch) {
            SwipeDownSearch(
                apps = allApps,
                onLaunch = { pkg -> viewModel.launchApp(context, pkg) },
                onDismiss = { showSwipeSearch = false }
            )
        }
    }
}

@Composable
private fun EditBar(onDone: () -> Unit) {
    val colors = LocalAppTheme.current
    val pulseTransition = rememberInfiniteTransition(label = "editPulse")
    // Read in graphicsLayer only: the pulse must not recompose the bar every frame.
    val pulse = pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "editPulseAlpha"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(Modifier.size(8.dp).graphicsLayer { alpha = pulse.value }.background(colors.accent))
            MonoLabel("EDITING", size = 10.sp, color = colors.ink, weight = FontWeight.Bold, modifier = Modifier.graphicsLayer { alpha = pulse.value })
            MonoLabel("// TAP: SIZE  ◀ ▶: MOVE  ✕: UNPIN  ■: ACCENT", size = 10.sp, color = colors.ink, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.width(TileDefaults.Gutter))
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 64.dp, minHeight = 40.dp)
                .background(colors.accent)
                .tilePress(onTap = onDone, tilt = false)
                .button("Done", onDone)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            MonoLabel("DONE", size = 12.sp, color = colors.onAccent, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickOptions(
    settings: SettingsState,
    pinned: List<AppItem>,
    viewModel: StandardAppViewModel
) {
    val compact = pinned.isNotEmpty() && pinned.all { it.tileSize == TileSize.SMALL }
    var savedSizes by remember { mutableStateOf<Map<String, TileSize>>(emptyMap()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
    ) {
        QuickOptionTile("ANIM", Modifier.weight(1f)) {
            viewModel.setAnimationStyle(
                when (settings.animationStyle) {
                    AnimationStyle.TAP_FLIP -> AnimationStyle.CUBE
                    AnimationStyle.CUBE -> AnimationStyle.SMOOTH
                    AnimationStyle.SMOOTH -> AnimationStyle.TAP_FLIP
                }
            )
        }
        QuickOptionTile("ICON", Modifier.weight(1f)) {
            viewModel.setIconStyle(
                when (settings.iconStyle) {
                    IconStyle.TEXT_ONLY -> IconStyle.ICON_ONLY
                    IconStyle.ICON_ONLY -> IconStyle.ICON_TEXT
                    IconStyle.ICON_TEXT -> IconStyle.TEXT_ONLY
                }
            )
        }
        QuickOptionTile("SIZE", Modifier.weight(1f)) {
            if (!compact) {
                savedSizes = pinned.associate { it.packageName to it.tileSize }
                pinned.forEach { viewModel.setTileSize(it.packageName, TileSize.SMALL) }
            } else {
                val restore = savedSizes.ifEmpty { pinned.associate { it.packageName to TileSize.MEDIUM } }
                restore.forEach { (pkg, size) -> viewModel.setTileSize(pkg, size) }
            }
        }
    }
}

@Composable
private fun QuickOptionTile(label: String, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val colors = LocalAppTheme.current
    Box(
        modifier = modifier
            .sizeIn(minHeight = 40.dp)
            .tilePress(onTap = onTap, tilt = false)
            .button(label, onTap)
            .background(colors.tile)
            .border(TileDefaults.Border, colors.ink)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        MonoLabel(label, size = 10.sp, color = colors.ink, weight = FontWeight.Bold)
    }
}

/** FRI / SEP 06, 2026 on the front; the time and STANDARD. on the back, alternating. */
@Composable
private fun DayTile(modifier: Modifier = Modifier) {
    val day = liveTimeFormatted("EEE")
    val date = liveTimeFormatted("MMM dd, yyyy")
    val time = liveTimeFormatted("HH:mm")
    LiveTile(
        modifier = modifier,
        intervalMs = 8000L,
        frames = listOf<@Composable () -> Unit>(
            {
                val c = LocalTileColors.current.content
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    FitHeadlineText(day, 96.sp, color = c)
                    MonoLabel(date, size = 13.sp, color = c, weight = FontWeight.Bold)
                }
            },
            {
                val c = LocalTileColors.current.content
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    HeadlineText(time, 64.sp, color = c, maxLines = 1)
                    MonoLabel("STANDARD.", size = 13.sp, color = c, weight = FontWeight.Bold)
                }
            }
        )
    )
}

@Composable
private fun HomeNowClock(nowPlaying: MediaInfo?, modifier: Modifier = Modifier) {
    val colors = LocalAppTheme.current
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val locale = Locale.getDefault()
    val time = SimpleDateFormat("h:mm", locale).format(Date(now))
    val weekday = SimpleDateFormat("EEEE", locale).format(Date(now))
    val date = SimpleDateFormat("MMM d", locale).format(Date(now))
    Column(modifier = modifier) {
        Text(
            text = time,
            style = StandardType.display(64.sp).copy(fontFeatureSettings = "tnum"),
            color = colors.ink,
            maxLines = 1,
            softWrap = false
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel(weekday, size = 11.sp, color = colors.accent, weight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            MonoLabel("// $date", size = 11.sp, color = colors.ink, weight = FontWeight.Bold)
        }
        NowPlayingMini(nowPlaying = nowPlaying)
    }
}

/** Accent-ruled strip under the clock; composes nothing at all when no media session exists. */
@Composable
private fun NowPlayingMini(nowPlaying: MediaInfo?, modifier: Modifier = Modifier) {
    if (nowPlaying == null) return
    val colors = LocalAppTheme.current
    val title = nowPlaying.title ?: "UNKNOWN"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .border(TileDefaults.Border, colors.accent)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(10.dp).background(colors.accent))
        MonoLabel(
            (if (nowPlaying.isPlaying) "NOW PLAYING // " else "PAUSED // ") + title,
            size = 11.sp,
            color = colors.ink,
            weight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PinnedGrid(
    rows: List<List<AppItem>>,
    firstIndex: Int,
    pinned: List<AppItem>,
    settings: SettingsState,
    editMode: Boolean,
    fontScale: Float,
    viewModel: StandardAppViewModel,
    launch: (String) -> Unit,
    enterEdit: () -> Unit,
    contextMenuTile: AppItem?,
    onContextMenuTile: (AppItem?) -> Unit
) {
    val context = LocalContext.current
    var draggedIndex by remember { mutableStateOf(-1) }
    rows.forEachIndexed { r, row ->
        renderRow(firstIndex + r, row, context, settings, editMode, fontScale, viewModel, launch, enterEdit, contextMenuTile, onContextMenuTile, pinned, draggedIndex) { draggedIndex = it }
    }
}

@Composable
private fun renderRow(
    r: Int,
    row: List<AppItem>,
    context: android.content.Context,
    settings: SettingsState,
    editMode: Boolean,
    fontScale: Float,
    viewModel: StandardAppViewModel,
    launch: (String) -> Unit,
    enterEdit: () -> Unit,
    contextMenuTile: AppItem?,
    onContextMenuTile: (AppItem?) -> Unit,
    pinned: List<AppItem>,
    draggedIndex: Int,
    onDraggedIndex: (Int) -> Unit
) {
    val large = row.any { it.tileSize == TileSize.LARGE }
    val targetHeight = (if (large) LargeRowHeight else RowHeight) * fontScale
    // Metro: no bounce on a size change, just a quick settle.
    val animatedHeight by animateFloatAsState(
        targetValue = targetHeight.value,
        animationSpec = RefreshRate.springSpec(),
        label = "rowHeight"
    )
    val tileHeightPx = with(LocalDensity.current) { targetHeight.toPx() }
    val currentTileHeightPx by rememberUpdatedState(tileHeightPx)
    TileEntrance(index = r) {
        Row(
            modifier = Modifier.fillMaxWidth().height(animatedHeight.dp),
            horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
        ) {
                row.forEach { app ->
                    key(app.packageName) {
                        val span = app.tileSize.span()
                        val animatedSpan by animateFloatAsState(
                            targetValue = span.toFloat(),
                            animationSpec = RefreshRate.springSpec(),
                            label = "span"
                        )
                        val cycle = { viewModel.setTileSize(app.packageName, app.tileSize.next()) }
                        val open = { launch(app.packageName) }
                        val index = pinned.indexOf(app)
                        val currentIndex by rememberUpdatedState(index)
                        Box(
                            modifier = Modifier
                                .weight(animatedSpan)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    if (draggedIndex == index) {
                                        scaleX = 1.1f
                                        scaleY = 1.1f
                                        shadowElevation = 16f
                                    }
                                }
                                .button(app.label) { if (editMode) cycle() else open() }
                                .pointerInput(editMode) {
                                    if (editMode) {
                                        var accumulated = 0f
                                        var localIdx = currentIndex
                                        detectDragGestures(
                                            onDragStart = {
                                                localIdx = currentIndex
                                                onDraggedIndex(localIdx)
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                accumulated += amount.y
                                                val moved = (accumulated / currentTileHeightPx).toInt()
                                                if (moved != 0) {
                                                    viewModel.movePinned(app.packageName, moved)
                                                    localIdx += moved
                                                    onDraggedIndex(localIdx)
                                                    accumulated -= moved * currentTileHeightPx
                                                }
                                            },
                                            onDragEnd = { onDraggedIndex(-1) },
                                            onDragCancel = { onDraggedIndex(-1) }
                                        )
                                    }
                                }
                        ) {
                            AppTile(
                                app = app,
                                iconStyle = settings.iconStyle,
                                animationStyle = settings.animationStyle,
                                onTap = open,
                                modifier = Modifier.fillMaxSize(),
                                titleSize = when (span) {
                                    1 -> 22.sp
                                    2 -> 32.sp
                                    else -> 40.sp
                                },
                                trailing = if (app.isAccent) "→" else null,
                                editMode = editMode,
                                onCycleSize = cycle,
                                onLongPress = { if (editMode) enterEdit() else onContextMenuTile(app) }
                            )
                            if (editMode) EditControls(app = app, viewModel = viewModel)
                            DropdownMenu(
                                expanded = contextMenuTile == app,
                                onDismissRequest = { onContextMenuTile(null) }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("RESIZE") },
                                    onClick = {
                                        onContextMenuTile(null)
                                        enterEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("MOVE") },
                                    onClick = {
                                        onContextMenuTile(null)
                                        enterEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("PIN/UNPIN") },
                                    onClick = {
                                        onContextMenuTile(null)
                                        viewModel.togglePin(app)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("REMOVE") },
                                    onClick = {
                                        onContextMenuTile(null)
                                        viewModel.unpin(app.packageName)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("APP INFO") },
                                    onClick = {
                                        onContextMenuTile(null)
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.parse("package:${app.packageName}")
                                            )
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ADD WIDGET") },
                                    onClick = {
                                        onContextMenuTile(null)
                                        Toast.makeText(context, "WIDGET PICKER — COMING SOON", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("EDIT DOCK") },
                                    onClick = {
                                        onContextMenuTile(null)
                                        Toast.makeText(context, "DRAG APPS TO DOCK", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
}

@Composable
private fun EditControls(app: AppItem, viewModel: StandardAppViewModel) {
    val colors = LocalAppTheme.current
    val resize = { delta: Int ->
        val target = if (delta > 0) app.tileSize.next() else app.tileSize.prev()
        viewModel.setTileSize(app.packageName, target)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        EditButton(
            label = if (app.isAccent) "Clear accent" else "Set accent",
            modifier = Modifier.align(Alignment.TopStart),
            onTap = { viewModel.setAccent(if (app.isAccent) null else app.packageName) }
        ) {
            Box(Modifier.size(10.dp).background(if (app.isAccent) colors.accent else colors.onInk))
        }
        EditButton("Unpin", Modifier.align(Alignment.TopEnd), onTap = { viewModel.unpin(app.packageName) }) {
            MonoLabel("✕", size = 12.sp, color = colors.onInk, weight = FontWeight.Bold)
        }
        EditButton("Move earlier", Modifier.align(Alignment.BottomStart), onTap = { viewModel.movePinned(app.packageName, -1) }) {
            MonoLabel("◀", size = 11.sp, color = colors.onInk, weight = FontWeight.Bold)
        }
        EditButton("Move later", Modifier.align(Alignment.BottomEnd), onTap = { viewModel.movePinned(app.packageName, 1) }) {
            MonoLabel("▶", size = 11.sp, color = colors.onInk, weight = FontWeight.Bold)
        }
        if (app.tileSize != TileSize.SMALL) {
            ResizeHandle(onResize = resize, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun ResizeHandle(onResize: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ink = LocalAppTheme.current.ink
    var accumulated by remember { mutableFloatStateOf(0f) }
    val thresholdPx = with(LocalDensity.current) { 36.dp.toPx() }
    Box(
        modifier = modifier
            .size(36.dp)
            .tilePress(onTap = { onResize(1) }, tilt = false)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { accumulated = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        accumulated += amount.x + amount.y
                        while (accumulated > thresholdPx) {
                            onResize(1)
                            accumulated -= thresholdPx
                        }
                        while (accumulated < -thresholdPx) {
                            onResize(-1)
                            accumulated += thresholdPx
                        }
                    }
                )
            }
            .button("Resize", { onResize(1) }),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w, h)
                lineTo(w, h * 0.45f)
                lineTo(w * 0.45f, h)
                close()
            }
            drawPath(path = path, color = ink)
        }
    }
}

/** 40dp hit area around a 24dp ink square. */
@Composable
private fun EditButton(
    label: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    content: @Composable () -> Unit
) {
    val ink = LocalAppTheme.current.ink
    Box(
        modifier = modifier
            .size(40.dp)
            .tilePress(onTap = onTap, tilt = false)
            .button(label, onTap),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(24.dp).background(ink), contentAlignment = Alignment.Center) { content() }
    }
}

/** Mono quote on the paper, cycling through [quotes] with a fade. */
@Composable
private fun QuoteLine(quotes: List<String>, modifier: Modifier = Modifier) {
    if (quotes.isEmpty()) {
        Spacer(modifier)
        return
    }
    val ink = LocalAppTheme.current.ink
    var index by remember(quotes.size) { mutableIntStateOf(0) }
    LaunchedEffect(quotes.size) {
        while (quotes.size > 1) {
            delay(8000)
            index = (index + 1) % quotes.size
        }
    }
    Crossfade(targetState = index, animationSpec = tween(600), label = "quote", modifier = modifier) { i ->
        Column {
            MonoLabel("“" + quotes[i % quotes.size] + "”", size = 14.sp, color = ink, maxLines = 4)
            MonoLabel("—", size = 14.sp, color = ink)
        }
    }
}

/** DISCIPLINE / BUILDS / FREEDOM with an ink rule; this is the way into launcher settings. */
@Composable
private fun SettingsCaption(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalAppTheme.current
    Column(
        modifier = modifier
            .sizeIn(minHeight = 40.dp)
            .tilePress(onTap = onClick, tilt = false)
            .button("Settings", onClick),
        verticalArrangement = Arrangement.Top
    ) {
        MonoLabel("DISCIPLINE\nBUILDS\nFREEDOM", size = 11.sp, color = colors.ink)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ink))
        Spacer(Modifier.height(4.dp))
        MonoLabel("SETTINGS →", size = 9.sp, color = colors.muted)
    }
}
