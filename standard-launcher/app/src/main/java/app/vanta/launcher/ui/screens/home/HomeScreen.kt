package app.vanta.launcher.ui.screens.home

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
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
import app.vanta.launcher.domain.model.BatteryState
import app.vanta.launcher.domain.model.HomeModule
import app.vanta.launcher.domain.model.IconStyle
import app.vanta.launcher.domain.model.MediaInfo
import app.vanta.launcher.domain.model.SettingsState
import app.vanta.launcher.domain.model.TileSize
import app.vanta.launcher.domain.model.WeatherData
import app.vanta.launcher.domain.model.display
import app.vanta.launcher.ui.components.AppTile
import app.vanta.launcher.ui.components.BatteryTile
import app.vanta.launcher.ui.components.BrutalTextDialog
import app.vanta.launcher.ui.components.FitHeadlineText
import app.vanta.launcher.ui.components.FloatingSearchBar
import app.vanta.launcher.ui.components.FolderPanel
import app.vanta.launcher.ui.components.FolderPickerDialog
import app.vanta.launcher.ui.components.FolderStore
import app.vanta.launcher.ui.components.FolderTile
import app.vanta.launcher.ui.components.HeadlineText
import app.vanta.launcher.ui.components.LiveTile
import app.vanta.launcher.ui.components.LocalEntranceTick
import app.vanta.launcher.ui.components.LocalHapticsEnabled
import app.vanta.launcher.ui.components.LocalTileColors
import app.vanta.launcher.ui.components.LumiaEasing
import app.vanta.launcher.ui.components.MonoLabel
import app.vanta.launcher.ui.components.MusicWidget
import app.vanta.launcher.ui.components.NotesWidget
import app.vanta.launcher.ui.components.PeopleHubTile
import app.vanta.launcher.ui.components.PlusCross
import app.vanta.launcher.ui.components.QuickSettingsFab
import app.vanta.launcher.ui.components.SwipeDownSearch
import app.vanta.launcher.ui.components.TaskItem
import app.vanta.launcher.ui.components.TaskStore
import app.vanta.launcher.ui.components.TasksWidget
import app.vanta.launcher.ui.components.Tile
import app.vanta.launcher.ui.components.TileCaptions
import app.vanta.launcher.ui.components.TileDefaults
import app.vanta.launcher.ui.components.TileEntrance
import app.vanta.launcher.ui.components.TileFolder
import app.vanta.launcher.ui.components.TileGroup
import app.vanta.launcher.ui.components.TileStyle
import app.vanta.launcher.ui.components.VantaWidgetHost
import app.vanta.launcher.ui.components.WeatherTile
import app.vanta.launcher.ui.components.WidgetStore
import app.vanta.launcher.ui.components.WidgetTile
import app.vanta.launcher.ui.components.button
import app.vanta.launcher.ui.components.liveTimeFormatted
import app.vanta.launcher.ui.components.tilePress
import app.vanta.launcher.ui.nav.StandardAppViewModel
import app.vanta.launcher.ui.screens.focus.FocusSession
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.StandardType
import app.vanta.launcher.util.RefreshRate
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

/** null -> ink -> accent -> outline -> null, the edit-mode style square. */
private fun nextTileStyle(current: String?): String? = when (current) {
    null -> "ink"
    "ink" -> "accent"
    "accent" -> "outline"
    else -> null
}

private fun isMorningHour(): Boolean = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 5..9

/** Frames of the morning brief, each in the "SAME PHONE. HIGHER STANDARDS." voice. */
private fun morningBriefFrames(context: Context, weather: WeatherData?, settings: SettingsState, battery: BatteryState): List<String> {
    val now = Date()
    val date = SimpleDateFormat("EEEE · d MMM", Locale.ENGLISH).format(now)
    val unit = settings.weatherUnit
    val weatherLine = weather?.let {
        "${unit.display(it.tempC)} ${it.condition} · ${it.location}\nH ${unit.display(it.highC)} · L ${unit.display(it.lowC)}"
    } ?: "WEATHER · NOT LOADED YET"
    val alarmAt = try {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).nextAlarmClock?.triggerTime
    } catch (e: Exception) {
        null
    }
    val alarmLine = alarmAt?.let {
        "NEXT ALARM · " + SimpleDateFormat("EEE", Locale.ENGLISH).format(Date(it)) + " " +
            android.text.format.DateFormat.getTimeFormat(context).format(Date(it))
    } ?: "NO ALARM SET"
    val batteryLine = "BATTERY ${battery.percent}%" + if (battery.isCharging) " · CHARGING" else ""
    val quote = settings.quotes.takeIf { it.isNotEmpty() }?.let { it[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % it.size] }
    return listOfNotNull(date, weatherLine, alarmLine, batteryLine, quote).map { it.uppercase() }
}

/** Callbacks the grid needs, bundled so the row renderer stays readable. */
private class GridActions(
    val launch: (String) -> Unit,
    val enterEdit: () -> Unit,
    val contextMenu: (AppItem?) -> Unit,
    val addToFolder: (AppItem) -> Unit,
    val editCaption: (AppItem) -> Unit,
    val addWidget: () -> Unit
)

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
    val focusActive by FocusSession.active.collectAsState()
    val focusApps by viewModel.focusApps.collectAsState()
    val focusPkgs = remember(focusApps) { focusApps.map { it.packageName }.toSet() }
    val modules = settings.homeModules
    var contextMenuTile by remember { mutableStateOf<AppItem?>(null) }
    var showSwipeSearch by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (editMode) 0.7f else 1f,
        animationSpec = tween(200),
        label = "contentAlpha"
    )

    // Folders: apps inside one leave the main grid; the open folder expands inline under its row.
    val folderStore = remember { FolderStore(context) }
    var folders by remember { mutableStateOf(folderStore.load()) }
    val reloadFolders = { folders = folderStore.load() }
    var expandedFolderId by remember { mutableStateOf<String?>(null) }
    var renameFolder by remember { mutableStateOf<TileFolder?>(null) }
    var folderPickerFor by remember { mutableStateOf<AppItem?>(null) }
    var captionFor by remember { mutableStateOf<AppItem?>(null) }
    val widgetStore = remember { WidgetStore(context) }
    val widgets by widgetStore.widgets.collectAsState()
    BackHandler(enabled = expandedFolderId != null) { expandedFolderId = null }

    // Re-checked once a minute; only a flip of the boolean recomposes Home.
    val morningHour by produceState(initialValue = isMorningHour()) {
        while (true) {
            delay(60_000)
            value = isMorningHour()
        }
    }
    val morning = settings.morningBrief && morningHour

    val hapticsOn = LocalHapticsEnabled.current
    val haptics = LocalHapticFeedback.current
    val enterEdit = {
        if (!viewModel.editMode.value) {
            if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.setEditMode(true)
        }
    }
    val launch: (String) -> Unit = { viewModel.launchApp(context, it) }
    val actions = GridActions(
        launch = launch,
        enterEdit = enterEdit,
        contextMenu = { contextMenuTile = it },
        addToFolder = { folderPickerFor = it },
        editCaption = { captionFor = it },
        addWidget = {
            try {
                context.startActivity(Intent().setClassName(context, "app.vanta.launcher.ui.components.WidgetPickerActivity"))
            } catch (e: Exception) {
                Toast.makeText(context, "WIDGET PICKER UNAVAILABLE", Toast.LENGTH_SHORT).show()
            }
        }
    )
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
                .pointerInput(hapticsOn) {
                    detectTapGestures(
                        onLongPress = { enterEdit() },
                        // A tap on bare paper closes the open folder.
                        onTap = { expandedFolderId = null }
                    )
                }
                .verticalScroll(scrollState)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
        ) {
            if (editMode) EditBar(onDone = { viewModel.setEditMode(false) })
            if (editMode) QuickOptions(settings = settings, pinned = pinned, viewModel = viewModel)

            Collapsible(visible = focusActive) { FocusRow() }

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
                Crossfade(targetState = morning, animationSpec = tween(300, easing = LumiaEasing), label = "brief") { brief ->
                    if (brief) {
                        val frames = remember(weather, battery, settings.quotes, settings.weatherUnit) {
                            morningBriefFrames(context, weather, settings, battery)
                        }
                        MorningBriefTile(frames = frames, modifier = Modifier.alpha(contentAlpha))
                    } else {
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
                }
            }

            TileEntrance(index = entrance++) {
                HomeNowClock(
                    nowPlaying = nowPlaying,
                    modifier = Modifier.fillMaxWidth().alpha(contentAlpha)
                )
            }

            val folderPkgs = remember(folders) { folders.flatMap { it.appPackages }.toSet() }
            val gridApps = remember(pinned, folderPkgs) { pinned.filter { it.packageName !in folderPkgs } }
            // While focusing only focus apps stay; folders survive when they hold at least one.
            val shownFolders = if (focusActive) folders.filter { f -> f.appPackages.any { it in focusPkgs } } else folders

            if (gridApps.isEmpty() && shownFolders.isEmpty()) {
                TileEntrance(index = entrance++) {
                    Tile(
                        modifier = Modifier.fillMaxWidth().height(RowHeight * fontScale).button("Pin apps in drawer", onOpenDrawer),
                        onClick = onOpenDrawer
                    ) {
                        val c = LocalTileColors.current.content
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            HeadlineText(if (focusActive) "FOCUS" else "PIN APPS", 32.sp, color = c)
                            MonoLabel(if (focusActive) "NO FOCUS APPS · SET THEM ON THE FOCUS PAGE" else "IN DRAWER →", size = 10.sp, color = c)
                        }
                    }
                }
            } else {
                val rows = remember(gridApps) { packRows(gridApps) }
                PinnedGrid(
                    rows = rows,
                    firstIndex = entrance,
                    pinned = pinned,
                    folders = folders,
                    settings = settings,
                    editMode = editMode,
                    fontScale = fontScale,
                    viewModel = viewModel,
                    actions = actions,
                    contextMenuTile = contextMenuTile,
                    visibleFor = { !focusActive || it.packageName in focusPkgs }
                )
                entrance += rows.size
                if (shownFolders.isNotEmpty()) {
                    val byPkg = remember(allApps, pinned) { allApps.associateBy { it.packageName } + pinned.associateBy { it.packageName } }
                    FolderSection(
                        folders = shownFolders,
                        firstIndex = entrance,
                        resolve = { f -> f.appPackages.mapNotNull { byPkg[it] } },
                        expandedId = expandedFolderId,
                        settings = settings,
                        editMode = editMode,
                        fontScale = fontScale,
                        onExpand = { expandedFolderId = it },
                        onOpenApp = launch,
                        onRename = { renameFolder = it },
                        onDelete = { folderStore.deleteFolder(it.id); reloadFolders() },
                        onRemoveApp = { f, pkg -> folderStore.removeFromFolder(f.id, pkg); reloadFolders() }
                    )
                    entrance += (shownFolders.size + 1) / 2
                }
            }

            if (widgets.isNotEmpty()) {
                TileEntrance(index = entrance++) {
                    TileGroup("WIDGETS", modifier = Modifier.alpha(contentAlpha)) {
                        widgets.forEach { entry ->
                            key(entry.id) {
                                WidgetTile(
                                    entry = entry,
                                    modifier = Modifier.fillMaxWidth().height(entry.heightDp.dp),
                                    onRemove = { widgetStore.remove(entry.id); VantaWidgetHost.deleteId(context, entry.id) },
                                    onResize = { widgetStore.update(it) }
                                )
                            }
                        }
                    }
                }
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

            val notesPrefs = remember { context.getSharedPreferences("standard_notes", Context.MODE_PRIVATE) }
            // Re-read on every foreground return: a finished focus session appends its own line.
            val entranceTick = LocalEntranceTick.current
            var notes by remember(entranceTick) { mutableStateOf(notesPrefs.getString("notes", "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()) }
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
                onDismiss = { showSwipeSearch = false },
                onDarkMode = { viewModel.setDarkMode(it) }
            )
        }

        renameFolder?.let { f ->
            BrutalTextDialog(
                title = "FOLDER NAME",
                initial = f.name,
                onSave = { folderStore.renameFolder(f.id, it.ifBlank { "FOLDER" }.uppercase()); reloadFolders(); renameFolder = null },
                onDismiss = { renameFolder = null }
            )
        }
        folderPickerFor?.let { app ->
            FolderPickerDialog(
                folders = folders,
                onPick = { f -> folderStore.addToFolder(f.id, app.packageName); reloadFolders(); folderPickerFor = null },
                onNew = { expandedFolderId = folderStore.createFolder(app.packageName).id; reloadFolders(); folderPickerFor = null },
                onDismiss = { folderPickerFor = null }
            )
        }
        captionFor?.let { app ->
            BrutalTextDialog(
                title = "CAPTION",
                initial = app.caption ?: TileCaptions.defaultFor(app.packageName, app.label),
                suggestions = remember(app.packageName) { TileCaptions.suggestions(app.packageName, app.label) },
                onSave = { viewModel.setCaption(app.packageName, it.ifBlank { null }?.uppercase()); captionFor = null },
                onReset = { viewModel.setCaption(app.packageName, null); captionFor = null },
                onDismiss = { captionFor = null }
            )
        }
    }
}

/** Vertical expand/collapse that leaves the tree (and the column gutter) once fully hidden. */
@Composable
private fun Collapsible(visible: Boolean, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(visible) }
    state.targetState = visible
    if (state.currentState || state.targetState) {
        AnimatedVisibility(
            visibleState = state,
            enter = expandVertically(tween(220, easing = LumiaEasing)) + fadeIn(tween(220)),
            exit = shrinkVertically(tween(200, easing = LumiaEasing)) + fadeOut(tween(150))
        ) { content() }
    }
}

/** Slim accent strip while a focus session runs: FOCUS · 24:31 LEFT, tap ends it. Ticks once a second. */
@Composable
private fun FocusRow(modifier: Modifier = Modifier) {
    val colors = LocalAppTheme.current
    val remaining by produceState(initialValue = FocusSession.remainingMillis()) {
        while (true) {
            value = FocusSession.remainingMillis()
            if (value <= 0L && FocusSession.active.value) FocusSession.stop()
            delay(1000)
        }
    }
    val secs = remaining / 1000
    val end = { FocusSession.stop() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.accent)
            .tilePress(onTap = end, tilt = false)
            .button("End focus session", end)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(colors.onAccent))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "FOCUS · %d:%02d LEFT".format(secs / 60, secs % 60),
            style = StandardType.mono(11.sp, FontWeight.Bold).copy(fontFeatureSettings = "tnum"),
            color = colors.onAccent,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        MonoLabel("TAP TO END", size = 10.sp, color = colors.onAccent, weight = FontWeight.Bold)
    }
}

/** Full-width ink tile between 5 and 9: one mono line at a time, each peeking up (WP8 slide) every 6 s. */
@Composable
private fun MorningBriefTile(frames: List<String>, modifier: Modifier = Modifier) {
    if (frames.isEmpty()) return
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(frames.size) {
        while (frames.size > 1) {
            delay(6000)
            index = (index + 1) % frames.size
        }
    }
    val accent = LocalAppTheme.current.accent
    Tile(modifier = modifier.fillMaxWidth().height(136.dp), style = TileStyle.Ink) {
        val c = LocalTileColors.current.content
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(accent))
                Spacer(Modifier.width(8.dp))
                MonoLabel("MORNING BRIEF", size = 10.sp, color = c, weight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${index % frames.size + 1}/${frames.size}",
                    style = StandardType.mono(10.sp).copy(fontFeatureSettings = "tnum"),
                    color = c
                )
            }
            Box(Modifier.fillMaxWidth().clipToBounds()) {
                AnimatedContent(
                    targetState = index % frames.size,
                    transitionSpec = {
                        (slideInVertically(tween(320, easing = LumiaEasing)) { it } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically(tween(200, easing = LumiaEasing)) { -it } + fadeOut(tween(120)))
                    },
                    label = "briefFrame"
                ) { i ->
                    MonoLabel(frames[i], size = 14.sp, color = c, weight = FontWeight.Bold, maxLines = 3, modifier = Modifier.fillMaxWidth())
                }
            }
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
            MonoLabel("// TAP: SIZE  HOLD: MENU  ◀ ▶: MOVE  ■: STYLE", size = 10.sp, color = colors.ink, maxLines = 1, modifier = Modifier.weight(1f))
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

/**
 * Folder rows (two 2-span tiles per row) with the open folder's panel inline under its row.
 * The section's height tweens so the rows below glide down, no bounce.
 */
@Composable
private fun FolderSection(
    folders: List<TileFolder>,
    firstIndex: Int,
    resolve: (TileFolder) -> List<AppItem>,
    expandedId: String?,
    settings: SettingsState,
    editMode: Boolean,
    fontScale: Float,
    onExpand: (String?) -> Unit,
    onOpenApp: (String) -> Unit,
    onRename: (TileFolder) -> Unit,
    onDelete: (TileFolder) -> Unit,
    onRemoveApp: (TileFolder, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .animateContentSize(animationSpec = tween(260, easing = LumiaEasing)),
        verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
    ) {
        folders.chunked(2).forEachIndexed { r, pair ->
            TileEntrance(index = firstIndex + r) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(RowHeight * fontScale),
                    horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
                ) {
                    pair.forEach { f ->
                        key(f.id) {
                            FolderTile(
                                folder = f,
                                apps = resolve(f),
                                expanded = expandedId == f.id,
                                editMode = editMode,
                                onTap = { onExpand(if (expandedId == f.id) null else f.id) },
                                onLongPress = { onRename(f) },
                                onDelete = { onDelete(f) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            val open = pair.firstOrNull { it.id == expandedId }
            if (open != null) {
                key(open.id) {
                    FolderPanel(
                        folder = open,
                        apps = resolve(open),
                        iconStyle = settings.iconStyle,
                        animationStyle = settings.animationStyle,
                        editMode = editMode,
                        onOpenApp = onOpenApp,
                        onRename = { onRename(open) },
                        onRemoveApp = { onRemoveApp(open, it) },
                        onClose = { onExpand(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedGrid(
    rows: List<List<AppItem>>,
    firstIndex: Int,
    pinned: List<AppItem>,
    folders: List<TileFolder>,
    settings: SettingsState,
    editMode: Boolean,
    fontScale: Float,
    viewModel: StandardAppViewModel,
    actions: GridActions,
    contextMenuTile: AppItem?,
    visibleFor: (AppItem) -> Boolean
) {
    val context = LocalContext.current
    var draggedIndex by remember { mutableStateOf(-1) }
    rows.forEachIndexed { r, row ->
        renderRow(firstIndex + r, row, context, settings, editMode, fontScale, viewModel, actions, contextMenuTile, pinned, folders.isNotEmpty(), visibleFor, draggedIndex) { draggedIndex = it }
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
    actions: GridActions,
    contextMenuTile: AppItem?,
    pinned: List<AppItem>,
    hasFolders: Boolean,
    visibleFor: (AppItem) -> Boolean,
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
    // Focus: a row with nothing left to show shrinks away; a partly hidden row lets its survivors grow.
    Collapsible(visible = row.any(visibleFor)) {
    TileEntrance(index = r) {
        Row(
            modifier = Modifier.fillMaxWidth().height(animatedHeight.dp),
            horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
        ) {
                row.forEach { app ->
                    key(app.packageName) {
                        val span = app.tileSize.span()
                        val visible = visibleFor(app)
                        val animatedSpan by animateFloatAsState(
                            targetValue = if (visible) span.toFloat() else 0.001f,
                            animationSpec = if (visible) RefreshRate.springSpec() else tween(200, easing = LumiaEasing),
                            label = "span"
                        )
                        val tileAlpha by animateFloatAsState(
                            targetValue = if (visible) 1f else 0f,
                            animationSpec = tween(200, easing = LumiaEasing),
                            label = "tileAlpha"
                        )
                        val cycle = { viewModel.setTileSize(app.packageName, app.tileSize.next()) }
                        val open = { actions.launch(app.packageName) }
                        val index = pinned.indexOf(app)
                        val currentIndex by rememberUpdatedState(index)
                        Box(
                            modifier = Modifier
                                .weight(animatedSpan)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    alpha = tileAlpha
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
                                caption = app.caption,
                                titleSize = when (span) {
                                    1 -> 22.sp
                                    2 -> 32.sp
                                    else -> 40.sp
                                },
                                trailing = if (app.isAccent) "→" else null,
                                editMode = editMode,
                                onCycleSize = cycle,
                                // AppTile only forwards this in edit mode; plain long-press is its shortcuts menu.
                                onLongPress = { actions.contextMenu(app) },
                                onCycleStyle = { viewModel.setTileStyle(app.packageName, nextTileStyle(app.tileStyle)) },
                                onEditCaption = { actions.editCaption(app) }
                            )
                            if (editMode) EditControls(app = app, viewModel = viewModel)
                            DropdownMenu(
                                expanded = contextMenuTile == app,
                                onDismissRequest = { actions.contextMenu(null) }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("RESIZE") },
                                    onClick = {
                                        actions.contextMenu(null)
                                        actions.enterEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("MOVE") },
                                    onClick = {
                                        actions.contextMenu(null)
                                        actions.enterEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("EDIT CAPTION") },
                                    onClick = {
                                        actions.contextMenu(null)
                                        actions.editCaption(app)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (hasFolders) "ADD TO FOLDER…" else "NEW FOLDER") },
                                    onClick = {
                                        actions.contextMenu(null)
                                        actions.addToFolder(app)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("PIN/UNPIN") },
                                    onClick = {
                                        actions.contextMenu(null)
                                        viewModel.togglePin(app)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("REMOVE") },
                                    onClick = {
                                        actions.contextMenu(null)
                                        viewModel.unpin(app.packageName)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("APP INFO") },
                                    onClick = {
                                        actions.contextMenu(null)
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
                                        actions.contextMenu(null)
                                        actions.addWidget()
                                    }
                                )
                            }
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
    // Top-left is the tile's own style square (Tile.kt); Home only adds unpin, move and resize.
    Box(modifier = Modifier.fillMaxSize()) {
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
