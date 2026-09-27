package app.vanta.launcher.ui.screens.settings

import app.vanta.launcher.R
import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.domain.model.AnimationStyle
import app.vanta.launcher.domain.model.AppItem
import app.vanta.launcher.domain.model.ClockFormat
import app.vanta.launcher.domain.model.DarkMode
import app.vanta.launcher.domain.model.HomeModule
import app.vanta.launcher.domain.model.IconStyle
import app.vanta.launcher.domain.model.RefreshRateMode
import app.vanta.launcher.domain.model.ThemeId
import app.vanta.launcher.domain.model.WeatherUnit
import app.vanta.launcher.ui.components.BackupRestoreButtons
import app.vanta.launcher.ui.components.DriftingNoiseOverlay
import app.vanta.launcher.ui.components.HeadlineText
import app.vanta.launcher.ui.components.LumiaEasing
import app.vanta.launcher.ui.components.MonoLabel
import app.vanta.launcher.ui.components.TileCaptions
import app.vanta.launcher.ui.components.VantaLogoStyle
import app.vanta.launcher.ui.components.VantaLogoWithWordmark
import app.vanta.launcher.ui.components.WpSwitch
import app.vanta.launcher.ui.components.tilePress
import app.vanta.launcher.ui.nav.StandardAppViewModel
import app.vanta.launcher.ui.theme.AppColors
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.StandardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private val RowHeight = 52.dp
private val BarHeight = 60.dp
private val PagePadding = 16.dp

private val SmoothnessLabels = listOf("SNAPPY", "SMOOTH", "LUXURIOUS", "BOUNCY", "GLASS")

private enum class TileIconStyle(val label: String) {
    ORIGINAL("ORIGINAL"),
    MONOCHROME("MONOCHROME"),
    ACCENT("ACCENT"),
    TEXT_ONLY("TEXT ONLY"),
    CIRCLE("CIRCLE"),
    ROUNDED("ROUNDED")
}

private fun TileIconStyle.next(): TileIconStyle =
    TileIconStyle.entries[(ordinal + 1) % TileIconStyle.entries.size]

@Composable
fun SettingsScreen(viewModel: StandardAppViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val settings by viewModel.settings.collectAsState()
    val pinned by viewModel.pinnedApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val quickTools by viewModel.quickTools.collectAsState()
    val focusApps by viewModel.focusApps.collectAsState()

    var picker by remember { mutableStateOf<PickerRequest?>(null) }
    // Keeps the last request alive while the picker sheet slides out.
    val shownPicker = remember { arrayOfNulls<PickerRequest>(1) }
    if (picker != null) shownPicker[0] = picker
    var resetArmed by remember { mutableIntStateOf(0) }

    val prefs = remember { context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE) }
    var smoothness by remember {
        mutableIntStateOf(prefs.getInt("smoothness_level", 0).coerceIn(0, SmoothnessLabels.lastIndex))
    }
    var iconStyleGlobal by remember {
        mutableIntStateOf(prefs.getInt("icon_style_global", 0).coerceIn(0, TileIconStyle.entries.lastIndex))
    }
    var wallpaperBg by remember { mutableStateOf(prefs.getBoolean("wallpaper_bg", false)) }
    var liveTiles by remember { mutableStateOf(prefs.getBoolean("live_tiles", true)) }
    var randomFlip by remember { mutableStateOf(prefs.getBoolean("random_flip", true)) }
    var notifPreviews by remember { mutableStateOf(prefs.getBoolean("notif_previews", true)) }
    var customAccent by remember { mutableStateOf(prefs.getLong("custom_accent", 0L)) }
    var wallpaperPreset by remember { mutableIntStateOf(prefs.getInt("wallpaper_preset", 0).coerceIn(0, 5)) }
    var statusBarVisible by remember { mutableStateOf(prefs.getBoolean("status_bar_visible", true)) }
    var statusBarTransparent by remember { mutableStateOf(prefs.getBoolean("status_bar_transparent", true)) }
    var statusBarIcons by remember { mutableStateOf(prefs.getString("status_bar_icons", "AUTO") ?: "AUTO") }
    var liveWeather by remember { mutableStateOf(prefs.getBoolean("live_weather", true)) }
    var liveClock by remember { mutableStateOf(prefs.getBoolean("live_clock", true)) }
    var liveBattery by remember { mutableStateOf(prefs.getBoolean("live_battery", true)) }
    var darkMode by remember { mutableStateOf(readDarkModePref(prefs)) }
    var refreshRateMode by remember {
        mutableStateOf(
            prefs.getString("refresh_rate_mode", null)?.let {
                runCatching { RefreshRateMode.valueOf(it) }.getOrNull()
            } ?: RefreshRateMode.AUTO
        )
    }
    val hiddenPrefs = remember { context.getSharedPreferences("standard_hidden", Context.MODE_PRIVATE) }
    var hiddenPackages by remember { mutableStateOf<List<String>>(hiddenPrefs.getStringSet("hidden", emptySet())?.toList() ?: emptyList()) }
    var appearanceExpanded by rememberSaveable { mutableStateOf(true) }
    var homeExpanded by rememberSaveable { mutableStateOf(true) }
    var motionExpanded by rememberSaveable { mutableStateOf(true) }
    var systemExpanded by rememberSaveable { mutableStateOf(true) }
    var aboutExpanded by rememberSaveable { mutableStateOf(true) }
    var gesturesExpanded by rememberSaveable { mutableStateOf(true) }

    val gesturesPrefs = remember { context.getSharedPreferences("standard_gestures", Context.MODE_PRIVATE) }
    var swipeUp by remember { mutableStateOf(gesturesPrefs.getString("swipe_up", "SEARCH") ?: "SEARCH") }
    var swipeDown by remember { mutableStateOf(gesturesPrefs.getString("swipe_down", "SEARCH") ?: "SEARCH") }
    var swipeLeft by remember { mutableStateOf(gesturesPrefs.getString("swipe_left", "NEXT PAGE") ?: "NEXT PAGE") }
    var swipeRight by remember { mutableStateOf(gesturesPrefs.getString("swipe_right", "PREV PAGE") ?: "PREV PAGE") }

    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        DriftingNoiseOverlay(Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(top = BarHeight, bottom = 32.dp)
                    .padding(horizontal = PagePadding)
            ) {
            CollapsibleSection(
                title = "APPEARANCE",
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded }
            ) {
                ThemeSwatches(
                    selected = settings.themeId,
                    onSelect = { viewModel.setThemeId(it) }
                )
                ValueRow("DARK MODE", darkMode.displayLabel()) {
                    val next = darkMode.next()
                    darkMode = next
                    prefs.edit().putString("dark_mode", next.name).apply()
                }
                ValueRow("REFRESH RATE", refreshRateMode.displayLabel()) {
                    val next = refreshRateMode.next()
                    refreshRateMode = next
                    prefs.edit().putString("refresh_rate_mode", next.name).apply()
                }
                ToggleRow("PAPER GRAIN", settings.useTexture) { viewModel.setUseTexture(it) }
                if (settings.useTexture) {
                    val strengthLabel = when {
                        settings.textureStrength < 0.08f -> "LIGHT"
                        settings.textureStrength < 0.13f -> "MEDIUM"
                        else -> "HEAVY"
                    }
                    ValueRow("GRAIN STRENGTH", strengthLabel) {
                        viewModel.setTextureStrength(
                            when (strengthLabel) {
                                "LIGHT" -> 0.10f
                                "MEDIUM" -> 0.16f
                                else -> 0.06f
                            }
                        )
                    }
                    ToggleRow("GRAIN DRIFT", settings.noiseDrift) { viewModel.setNoiseDrift(it) }
                }
                ToggleRow("TIME-OF-DAY TINT", settings.timeOfDayTint) { viewModel.setTimeOfDayTint(it) }
                ToggleRow("WALLPAPER BACKGROUND", wallpaperBg) {
                    wallpaperBg = it
                    prefs.edit().putBoolean("wallpaper_bg", it).apply()
                }
                ValueRow("WALLPAPER", "SET WALLPAPER") {
                    Toast.makeText(context, "OPENING WALLPAPER PICKER", Toast.LENGTH_SHORT).show()
                    context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                MonoLabel("WALLPAPER PRESETS", size = 10.sp, color = colors.muted, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
                WallpaperPresetSwatches(
                    selected = wallpaperPreset,
                    onSelect = {
                        wallpaperPreset = it
                        prefs.edit().putInt("wallpaper_preset", it).apply()
                    }
                )
                ValueRow(
                    "ICON STYLE",
                    when (settings.iconStyle) {
                        IconStyle.TEXT_ONLY -> "TEXT ONLY"
                        IconStyle.ICON_ONLY -> "ICON ONLY"
                        IconStyle.ICON_TEXT -> "ICON + TEXT"
                    }
                ) { viewModel.setIconStyle(settings.iconStyle.next()) }
                ValueRow("ICON RENDER", TileIconStyle.entries[iconStyleGlobal].label) {
                    val nextStyle = TileIconStyle.entries[iconStyleGlobal].next()
                    iconStyleGlobal = nextStyle.ordinal
                    prefs.edit().putInt("icon_style_global", nextStyle.ordinal).apply()
                }
                AccentColorPicker(current = customAccent) { color ->
                    customAccent = color
                    prefs.edit().putLong("custom_accent", color).apply()
                }
                SectionHeader("STATUS BAR")
                ToggleRow("SHOW STATUS BAR", statusBarVisible) {
                    statusBarVisible = it
                    prefs.edit().putBoolean("status_bar_visible", it).apply()
                }
                ToggleRow("TRANSPARENT STATUS BAR", statusBarTransparent) {
                    statusBarTransparent = it
                    prefs.edit().putBoolean("status_bar_transparent", it).apply()
                }
                ValueRow("STATUS BAR ICONS", statusBarIcons) {
                    val next = when (statusBarIcons) {
                        "LIGHT" -> "DARK"
                        "DARK" -> "AUTO"
                        else -> "LIGHT"
                    }
                    statusBarIcons = next
                    prefs.edit().putString("status_bar_icons", next).apply()
                }
            }

            CollapsibleSection(
                title = "HOME",
                expanded = homeExpanded,
                onToggle = { homeExpanded = !homeExpanded }
            ) {
                ToggleRow("LIVE TILES", liveTiles) {
                    liveTiles = it
                    prefs.edit().putBoolean("live_tiles", it).apply()
                }
                ToggleRow("RANDOM FLIP TIMING", randomFlip) {
                    randomFlip = it
                    prefs.edit().putBoolean("random_flip", it).apply()
                }
                ToggleRow("NOTIFICATION PREVIEWS", notifPreviews) {
                    notifPreviews = it
                    prefs.edit().putBoolean("notif_previews", it).apply()
                }
                SectionHeader("LIVE TILE CONTENT")
                ToggleRow("WEATHER UPDATES", liveWeather) {
                    liveWeather = it
                    prefs.edit().putBoolean("live_weather", it).apply()
                }
                ToggleRow("CLOCK LIVE TILE", liveClock) {
                    liveClock = it
                    prefs.edit().putBoolean("live_clock", it).apply()
                }
                ToggleRow("BATTERY LIVE TILE", liveBattery) {
                    liveBattery = it
                    prefs.edit().putBoolean("live_battery", it).apply()
                }
                SectionHeader("HOME MODULES")
                HomeModule.entries.forEach { module ->
                    ToggleRow(
                        label = when (module) {
                            HomeModule.SEARCH -> "SEARCH BAR"
                            HomeModule.MEDIA -> "NOW PLAYING"
                            HomeModule.BATTERY -> "BATTERY"
                            HomeModule.PEOPLE -> "PEOPLE HUB"
                            HomeModule.QUICK_SETTINGS -> "QUICK SETTINGS"
                        },
                        checked = module in settings.homeModules
                    ) { on ->
                        viewModel.setHomeModules(if (on) settings.homeModules + module else settings.homeModules - module)
                    }
                }
                SectionHeader("HOME TILES")
                if (pinned.isEmpty()) {
                    MonoLabel("NO PINNED APPS", size = 11.sp, color = colors.muted, modifier = Modifier.padding(vertical = 8.dp))
                }
                pinned.forEachIndexed { index, app ->
                    PinnedAppRow(
                        index = index,
                        app = app,
                        isFirst = index == 0,
                        isLast = index == pinned.lastIndex,
                        onUp = { viewModel.movePinned(app.packageName, -1) },
                        onDown = { viewModel.movePinned(app.packageName, +1) },
                        onUnpin = { viewModel.unpin(app.packageName) },
                        onAccent = { viewModel.setAccent(if (app.isAccent) null else app.packageName) },
                        onCaption = { viewModel.setCaption(app.packageName, it) }
                    )
                }
                ActionRow("+ PIN AN APP") {
                    picker = PickerRequest("PIN AN APP", allApps.filter { !it.pinned }) { app ->
                        if (app != null) viewModel.setPinned(pinned.map { it.packageName } + app.packageName)
                    }
                }
                SectionHeader("ACCENT APPS")
                val accentApps by viewModel.accentApps.collectAsState()
                val accentMode by viewModel.accentMode.collectAsState()
                LaunchedEffect(Unit) {
                    val rawApps = prefs.getString("accent_apps", null)
                    if (rawApps != null) {
                        viewModel.setAccentApps(rawApps.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                    }
                    val rawMode = prefs.getString("accent_mode", null)
                    if (rawMode != null) {
                        viewModel.setAccentMode(rawMode)
                    }
                }
                if (pinned.isEmpty()) {
                    MonoLabel("NO PINNED APPS", size = 11.sp, color = colors.muted, modifier = Modifier.padding(vertical = 8.dp))
                }
                pinned.forEach { app ->
                    AccentAppCheckRow(
                        label = app.label,
                        checked = app.packageName in accentApps,
                        enabled = app.packageName in accentApps || accentApps.size < 5,
                        onToggle = {
                            val next = if (app.packageName in accentApps) accentApps - app.packageName else accentApps + app.packageName
                            viewModel.setAccentApps(next)
                            prefs.edit().putString("accent_apps", next.joinToString(",")).apply()
                        }
                    )
                }
                AccentModeButtons(selected = accentMode) { mode ->
                    viewModel.setAccentMode(mode)
                    prefs.edit().putString("accent_mode", mode).apply()
                }
                SectionHeader("QUICK TOOLS")
                (0 until 4).forEach { slot ->
                    val app = quickTools.getOrNull(slot)
                    ValueRow("SLOT ${slot + 1}", app?.label?.uppercase() ?: "EMPTY") {
                        picker = PickerRequest("QUICK TOOL ${slot + 1}", allApps, allowNone = app != null) { chosen ->
                            val current = quickTools.map { it.packageName }.toMutableList()
                            if (chosen == null) {
                                if (slot < current.size) current.removeAt(slot)
                            } else if (slot < current.size) {
                                current[slot] = chosen.packageName
                            } else {
                                current.add(chosen.packageName)
                            }
                            viewModel.setQuickTools(current.filter { it.isNotBlank() })
                        }
                    }
                }
                SectionHeader("FOCUS APPS")
                focusApps.forEachIndexed { index, app ->
                    ListEditRow(
                        label = "%02d  %s".format(index + 1, app.label.uppercase()),
                        isFirst = index == 0,
                        isLast = index == focusApps.lastIndex,
                        onUp = { viewModel.setFocusApps(focusApps.map { it.packageName }.swap(index, index - 1)) },
                        onDown = { viewModel.setFocusApps(focusApps.map { it.packageName }.swap(index, index + 1)) },
                        onRemove = { viewModel.setFocusApps(focusApps.map { it.packageName } - app.packageName) }
                    )
                }
                if (focusApps.size < 10) {
                    ActionRow("+ ADD TO FOCUS") {
                        picker = PickerRequest("ADD TO FOCUS", allApps.filter { a -> focusApps.none { it.packageName == a.packageName } }) { app ->
                            if (app != null) viewModel.setFocusApps(focusApps.map { it.packageName } + app.packageName)
                        }
                    }
                }
                SectionHeader("QUOTES")
                settings.quotes.forEachIndexed { index, quote ->
                    ListEditRow(
                        label = quote,
                        isFirst = true,
                        isLast = true,
                        onUp = {},
                        onDown = {},
                        onRemove = { viewModel.setQuotes(settings.quotes.filterIndexed { i, _ -> i != index }) },
                        showArrows = false
                    )
                }
                var newQuote by remember { mutableStateOf("") }
                BrutalTextField(
                    value = newQuote,
                    onValueChange = { newQuote = it },
                    placeholder = "NEW QUOTE //",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    onDone = {
                        if (newQuote.isNotBlank()) {
                            viewModel.setQuotes(settings.quotes + newQuote.trim().uppercase())
                            newQuote = ""
                        }
                    },
                    trailingLabel = "ADD"
                )
                SectionHeader("CLOCK & WEATHER")
                LocationEditor(viewModel = viewModel, currentName = settings.weatherLocation.name)
                ValueRow("UNITS", if (settings.weatherUnit == WeatherUnit.CELSIUS) "°C · KM/H" else "°F · MPH") {
                    viewModel.setWeatherUnit(if (settings.weatherUnit == WeatherUnit.CELSIUS) WeatherUnit.FAHRENHEIT else WeatherUnit.CELSIUS)
                }
                ValueRow(
                    "CLOCK",
                    when (settings.clockFormat) {
                        ClockFormat.AUTO -> "AUTO"
                        ClockFormat.H12 -> "12 HOUR"
                        ClockFormat.H24 -> "24 HOUR"
                    }
                ) {
                    viewModel.setClockFormat(
                        when (settings.clockFormat) {
                            ClockFormat.AUTO -> ClockFormat.H12
                            ClockFormat.H12 -> ClockFormat.H24
                            ClockFormat.H24 -> ClockFormat.AUTO
                        }
                    )
                }
            }

            CollapsibleSection(
                title = "MOTION",
                expanded = motionExpanded,
                onToggle = { motionExpanded = !motionExpanded }
            ) {
                ValueRow(
                    "TILE ANIMATION",
                    when (settings.animationStyle) {
                        AnimationStyle.TAP_FLIP -> "TAP FLIP"
                        AnimationStyle.CUBE -> "WINDOWS 8.1 3D ROTATE"
                        AnimationStyle.SMOOTH -> "SMOOTH"
                    }
                ) { viewModel.setAnimationStyle(settings.animationStyle.next()) }
                ValueRow("SMOOTHNESS", SmoothnessLabels[smoothness]) {
                    val next = (smoothness + 1) % SmoothnessLabels.size
                    smoothness = next
                    prefs.edit().putInt("smoothness_level", next).apply()
                }
                ToggleRow("CINEMATIC INTRO", settings.cinematicIntro) { viewModel.setCinematicIntro(it) }
                var silkyPager by remember { mutableStateOf(settings.silkyPager) }
                ToggleRow("SILKY PAGER", silkyPager) { silkyPager = it; viewModel.setSilkyPager(it) }
                var motionTouch by remember { mutableStateOf(settings.motionTouch) }
                ToggleRow("MOTION TOUCH", motionTouch) { motionTouch = it; viewModel.setMotionTouch(it) }
                ToggleRow("GLANCE (HOLD A TAB)", settings.glanceEnabled) { viewModel.setGlanceEnabled(it) }
                ToggleRow("HAPTICS", settings.hapticsEnabled) { viewModel.setHaptics(it) }
            }

            CollapsibleSection(
                title = "SYSTEM",
                expanded = systemExpanded,
                onToggle = { systemExpanded = !systemExpanded }
            ) {
                DefaultLauncherRow()
                ActionRow(if (viewModel.mediaAccessGranted()) "NOTIFICATION ACCESS · GRANTED" else "NOTIFICATION ACCESS FOR NOW PLAYING") {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                BatteryOptimizationRow()
                ActionRow(
                    label = if (resetArmed == 1) "TAP AGAIN TO RESET LAYOUT" else "RESET LAYOUT",
                    destructive = resetArmed == 1
                ) {
                    if (resetArmed != 1) {
                        resetArmed = 1
                    } else {
                        resetArmed = 0
                        viewModel.resetLayout()
                        Toast.makeText(context, "LAYOUT RESET", Toast.LENGTH_SHORT).show()
                    }
                }
                ActionRow(
                    label = if (resetArmed == 2) "TAP AGAIN TO RESET EVERYTHING" else "RESET EVERYTHING",
                    destructive = resetArmed == 2
                ) {
                    if (resetArmed != 2) {
                        resetArmed = 2
                    } else {
                        resetArmed = 0
                        viewModel.resetEverything()
                        Toast.makeText(context, "ALL SETTINGS RESET", Toast.LENGTH_SHORT).show()
                    }
                }
                SectionHeader("BACKUP / RESTORE")
                BackupRestoreButtons(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                SectionHeader("HIDDEN APPS")
                val hiddenApps = allApps.filter { it.packageName in hiddenPackages }
                if (hiddenApps.isEmpty()) {
                    MonoLabel("NO HIDDEN APPS", size = 11.sp, color = colors.muted, modifier = Modifier.padding(vertical = 8.dp))
                }
                hiddenApps.forEach { app ->
                    RowShell {
                        MonoLabel(app.label, size = 12.sp, weight = FontWeight.Bold, color = colors.ink, modifier = Modifier.weight(1f), maxLines = 1)
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .tilePress(
                                    onTap = {
                                        val next = hiddenPackages - app.packageName
                                        hiddenPackages = next
                                        hiddenPrefs.edit().putStringSet("hidden", next.toSet()).apply()
                                    },
                                    tilt = false
                                )
                                .border(2.dp, colors.ink)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            MonoLabel("UNHIDE", size = 10.sp, weight = FontWeight.Bold, color = colors.ink)
                        }
                    }
                }
            }

            CollapsibleSection(
                title = "GESTURES",
                expanded = gesturesExpanded,
                onToggle = { gesturesExpanded = !gesturesExpanded }
            ) {
                ValueRow("SWIPE UP", swipeUp) {
                    val next = when (swipeUp) {
                        "SEARCH" -> "NOTIFICATIONS"
                        "NOTIFICATIONS" -> "FOCUS"
                        else -> "SEARCH"
                    }
                    swipeUp = next
                    gesturesPrefs.edit().putString("swipe_up", next).apply()
                    Toast.makeText(context, "SWIPE UP: $next", Toast.LENGTH_SHORT).show()
                }
                ValueRow("SWIPE DOWN", swipeDown) {
                    swipeDown = "SEARCH"
                    gesturesPrefs.edit().putString("swipe_down", "SEARCH").apply()
                    Toast.makeText(context, "SWIPE DOWN: SEARCH", Toast.LENGTH_SHORT).show()
                }
                ValueRow("SWIPE LEFT", swipeLeft) {
                    swipeLeft = "NEXT PAGE"
                    gesturesPrefs.edit().putString("swipe_left", "NEXT PAGE").apply()
                    Toast.makeText(context, "SWIPE LEFT: NEXT PAGE", Toast.LENGTH_SHORT).show()
                }
                ValueRow("SWIPE RIGHT", swipeRight) {
                    swipeRight = "PREV PAGE"
                    gesturesPrefs.edit().putString("swipe_right", "PREV PAGE").apply()
                    Toast.makeText(context, "SWIPE RIGHT: PREV PAGE", Toast.LENGTH_SHORT).show()
                }
            }

            CollapsibleSection(
                title = "ABOUT",
                expanded = aboutExpanded,
                onToggle = { aboutExpanded = !aboutExpanded }
            ) {
                VantaLogoWithWordmark(
                    style = VantaLogoStyle.MetroTile,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                )
                Spacer(Modifier.height(12.dp))
                AboutRow("VERSION", version)
                AboutRow("FONTS", "SPACE GROTESK + JETBRAINS MONO (OFL)")
                AboutRow("WEATHER", "OPEN-METEO")
                AboutRow("PRIVACY", "NO TRACKING. NO CLOUD SYNC.")
                Spacer(Modifier.height(24.dp))
                MonoLabel("SAME PHONE. HIGHER STANDARDS.", size = 10.sp, color = colors.muted)
                Spacer(Modifier.height(10.dp))
                MonoLabel(stringResource(R.string.developer), size = 10.sp, color = colors.ink, weight = FontWeight.Bold)
            }
            }

            SettingsBar(
                scrollState = scrollState,
                onClose = onClose,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        AnimatedVisibility(
            visible = picker != null,
            enter = slideInVertically(tween(260, easing = LumiaEasing)) { it / 6 } + fadeIn(tween(260, easing = LumiaEasing)),
            exit = slideOutVertically(tween(180, easing = LumiaEasing)) { it / 6 } + fadeOut(tween(160, easing = LumiaEasing))
        ) {
            shownPicker[0]?.let { request ->
                AppPicker(
                    request = request,
                    onDismiss = { picker = null },
                    onPick = { app ->
                        request.onPick(app)
                        picker = null
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Page chrome
// ---------------------------------------------------------------------------------------------

/** "SETTINGS //" bar the list scrolls under; a 1dp ink rule fades in after 8dp of scroll. */
@Composable
private fun SettingsBar(scrollState: ScrollState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppTheme.current
    val threshold = with(LocalDensity.current) { 8.dp.toPx() }
    val scrolled by remember(threshold) { derivedStateOf { scrollState.value > threshold } }
    val rule = animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(160, easing = LumiaEasing),
        label = "barRule"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .background(colors.background)
            .drawBehind {
                val a = rule.value
                if (a > 0f) {
                    val h = 1.dp.toPx()
                    drawRect(colors.ink, topLeft = Offset(0f, size.height - h), size = Size(size.width, h), alpha = a)
                }
            }
            .padding(horizontal = PagePadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeadlineText("SETTINGS //", 28.sp, maxLines = 1)
        Spacer(Modifier.weight(1f))
        CloseButton(onClick = onClose, description = "Close settings")
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit, description: String) {
    val colors = LocalAppTheme.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .tilePress(onTap = onClick, tilt = false)
            .border(2.dp, colors.ink)
            .semantics {
                contentDescription = description
                onClick { onClick(); true }
            },
        contentAlignment = Alignment.Center
    ) {
        MonoLabel("×", size = 20.sp, weight = FontWeight.Bold, color = colors.ink)
    }
}

// ---------------------------------------------------------------------------------------------
// Rows
// ---------------------------------------------------------------------------------------------

private fun Modifier.plainClickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick
    )
)

private fun <T> List<T>.swap(a: Int, b: Int): List<T> {
    if (a !in indices || b !in indices) return this
    val m = toMutableList()
    val t = m[a]
    m[a] = m[b]
    m[b] = t
    return m
}

private fun IconStyle.next(): IconStyle = when (this) {
    IconStyle.TEXT_ONLY -> IconStyle.ICON_ONLY
    IconStyle.ICON_ONLY -> IconStyle.ICON_TEXT
    IconStyle.ICON_TEXT -> IconStyle.TEXT_ONLY
}

private fun AnimationStyle.next(): AnimationStyle = when (this) {
    AnimationStyle.TAP_FLIP -> AnimationStyle.CUBE
    AnimationStyle.CUBE -> AnimationStyle.SMOOTH
    AnimationStyle.SMOOTH -> AnimationStyle.TAP_FLIP
}

private fun DarkMode.next(): DarkMode = DarkMode.entries[(ordinal + 1) % DarkMode.entries.size]

private fun RefreshRateMode.next(): RefreshRateMode =
    RefreshRateMode.entries[(ordinal + 1) % RefreshRateMode.entries.size]

private fun DarkMode.displayLabel(): String = when (this) {
    DarkMode.AUTO_SYSTEM -> "AUTO SYSTEM"
    DarkMode.AUTO_TIME -> "AUTO TIME"
    DarkMode.LIGHT -> "LIGHT"
    DarkMode.DARK -> "DARK"
}

private fun RefreshRateMode.displayLabel(): String = when (this) {
    RefreshRateMode.AUTO -> "AUTO"
    RefreshRateMode.HZ60 -> "60HZ"
    RefreshRateMode.HZ90 -> "90HZ"
    RefreshRateMode.HZ120 -> "120HZ"
    RefreshRateMode.MAX -> "MAX"
}

private fun readDarkModePref(prefs: SharedPreferences): DarkMode {
    if (!prefs.contains("dark_mode")) return DarkMode.AUTO_SYSTEM
    val stored = runCatching { prefs.getString("dark_mode", null) }.getOrNull()
    if (stored != null) {
        return runCatching { DarkMode.valueOf(stored) }.getOrNull() ?: DarkMode.AUTO_SYSTEM
    }
    return runCatching { if (prefs.getBoolean("dark_mode", false)) DarkMode.DARK else DarkMode.LIGHT }
        .getOrNull() ?: DarkMode.AUTO_SYSTEM
}

/** Sub-group header inside a section: mono bold + 2dp ink rule. */
@Composable
private fun SectionHeader(title: String) {
    val colors = LocalAppTheme.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
        MonoLabel(title, size = 11.sp, weight = FontWeight.Bold, color = colors.ink)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(colors.ink))
    }
}

/**
 * Section header with a +/− square (the vertical bar turns 90° into the horizontal one) and a body
 * that expands in 260ms / collapses in 180ms on the Lumia curve.
 */
@Composable
private fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = LocalAppTheme.current
    val turn = animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(260, easing = LumiaEasing),
        label = "sectionPlus"
    )
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .tilePress(onTap = onToggle, tilt = false)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title section, ${if (expanded) "collapse" else "expand"}"
                    onClick { onToggle(); true }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonoLabel(title, size = 11.sp, weight = FontWeight.Bold, color = colors.ink)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, colors.ink)
                    .drawBehind {
                        val bar = 10.dp.toPx()
                        val t = 2.dp.toPx()
                        val topLeft = Offset(size.width / 2f - bar / 2f, size.height / 2f - t / 2f)
                        drawRect(colors.ink, topLeft, Size(bar, t))
                        rotate(90f - turn.value) { drawRect(colors.ink, topLeft, Size(bar, t)) }
                    }
            )
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(colors.ink))
    }
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(tween(260, easing = LumiaEasing)) + fadeIn(tween(260, easing = LumiaEasing)),
        exit = shrinkVertically(tween(180, easing = LumiaEasing)) + fadeOut(tween(180, easing = LumiaEasing))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

/** Text that swaps with an 8dp upward slide + fade on the Lumia curve. */
@Composable
private fun SlidingValue(text: String, size: TextUnit, color: Color, weight: FontWeight = FontWeight.Medium) {
    val slide = with(LocalDensity.current) { 8.dp.roundToPx() }
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(tween(200, easing = LumiaEasing)) { slide } + fadeIn(tween(200, easing = LumiaEasing)))
                .togetherWith(slideOutVertically(tween(200, easing = LumiaEasing)) { -slide } + fadeOut(tween(200, easing = LumiaEasing)))
                .using(SizeTransform(clip = false) { _, _ -> tween(200, easing = LumiaEasing) })
        },
        label = "slidingValue"
    ) { shown ->
        MonoLabel(shown, size = size, color = color, weight = weight, maxLines = 1)
    }
}

/** Selected-state ring: 3dp inner stroke that scales 0.8 -> 1 and fades in over 160ms. Drawn in the draw phase. */
@Composable
private fun Modifier.selectionRing(selected: Boolean, color: Color, inset: Dp): Modifier {
    val ring = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(160, easing = LumiaEasing),
        label = "selectionRing"
    )
    return drawWithContent {
        drawContent()
        val p = ring.value
        if (p > 0.01f) {
            val i = inset.toPx()
            val stroke = 3.dp.toPx()
            scale(0.8f + 0.2f * p) {
                drawRect(
                    color = color,
                    topLeft = Offset(i + stroke / 2f, i + stroke / 2f),
                    size = Size(size.width - 2f * i - stroke, size.height - 2f * i - stroke),
                    style = Stroke(stroke),
                    alpha = p
                )
            }
        }
    }
}

/** Ink or paper, whichever contrasts more with [fill]. */
private fun ringColorFor(fill: Color, colors: AppColors): Color {
    val l = fill.luminance()
    return if (abs(l - colors.ink.luminance()) >= abs(l - colors.onInk.luminance())) colors.ink else colors.onInk
}

/** Turnstile entrance for picker rows: 20ms stagger capped at 240ms, measured from [bornAt] so rows scrolled in later swing in at once. */
@Composable
private fun Modifier.turnstileIn(index: Int, bornAt: Long): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val wait = minOf(index * 20L, 240L) - (System.currentTimeMillis() - bornAt)
        if (wait > 0) delay(wait)
        progress.animateTo(1f, tween(280, easing = LumiaEasing))
    }
    return graphicsLayer {
        val p = progress.value
        if (p < 1f) {
            transformOrigin = TransformOrigin(0f, 0.5f)
            rotationY = 60f * (1f - p)
            alpha = p
            cameraDistance = 14f * density
        }
    }
}

@Composable
private fun RowShell(
    modifier: Modifier = Modifier,
    minHeight: Dp = RowHeight,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalAppTheme.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) { content() }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ink.copy(alpha = 0.15f)))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalAppTheme.current
    RowShell(
        modifier = Modifier
            .tilePress(onTap = { onChange(!checked) })
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                contentDescription = label
                onClick { onChange(!checked); true }
            }
    ) {
        MonoLabel(label, size = 12.sp, weight = FontWeight.Bold, color = colors.ink, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        SlidingValue(if (checked) "ON" else "OFF", size = 9.sp, color = colors.muted)
        Spacer(Modifier.width(8.dp))
        WpSwitch(checked)
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    val colors = LocalAppTheme.current
    RowShell(
        modifier = Modifier
            .tilePress(onTap = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label, $value"
                onClick { onClick(); true }
            }
    ) {
        MonoLabel(label, size = 12.sp, weight = FontWeight.Bold, color = colors.ink, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        SlidingValue(value, size = 11.sp, color = colors.ink.copy(alpha = 0.8f))
        Spacer(Modifier.width(10.dp))
        MonoLabel(">", size = 16.sp, weight = FontWeight.Bold, color = colors.ink)
    }
}

@Composable
private fun ActionRow(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = LocalAppTheme.current
    val tint = if (destructive) colors.accent else colors.ink
    RowShell(
        modifier = Modifier
            .tilePress(onTap = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick { onClick(); true }
            }
    ) {
        MonoLabel(label, size = 12.sp, weight = FontWeight.Bold, color = tint, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        MonoLabel(">", size = 16.sp, weight = FontWeight.Bold, color = tint)
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    val colors = LocalAppTheme.current
    RowShell(minHeight = 40.dp) {
        MonoLabel(label, size = 10.sp, color = colors.muted)
        Spacer(Modifier.width(10.dp))
        MonoLabel(value, size = 10.sp, color = colors.ink, maxLines = 1)
    }
}

@Composable
private fun SquareButton(glyph: String, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = LocalAppTheme.current
    val tint = if (enabled) colors.ink else colors.ink.copy(alpha = 0.25f)
    Box(
        modifier = Modifier
            .size(40.dp)
            .tilePress(onTap = onClick, enabled = enabled, tilt = false)
            .border(2.dp, tint)
            .semantics {
                contentDescription = description
                if (enabled) onClick { onClick(); true }
            },
        contentAlignment = Alignment.Center
    ) {
        MonoLabel(glyph, size = 13.sp, weight = FontWeight.Bold, color = tint)
    }
}

@Composable
private fun ListEditRow(
    label: String,
    isFirst: Boolean,
    isLast: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
    showArrows: Boolean = true
) {
    val colors = LocalAppTheme.current
    RowShell {
        MonoLabel(label, size = 11.sp, weight = FontWeight.Bold, color = colors.ink, modifier = Modifier.weight(1f), maxLines = 2)
        Spacer(Modifier.width(8.dp))
        if (showArrows) {
            SquareButton("↑", "Move up", enabled = !isFirst, onClick = onUp)
            Spacer(Modifier.width(6.dp))
            SquareButton("↓", "Move down", enabled = !isLast, onClick = onDown)
            Spacer(Modifier.width(6.dp))
        }
        SquareButton("×", "Remove", onClick = onRemove)
    }
}

@Composable
private fun AccentAppCheckRow(label: String, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val colors = LocalAppTheme.current
    val fill = animateColorAsState(
        targetValue = if (checked) colors.accent else Color.Transparent,
        animationSpec = tween(160, easing = LumiaEasing),
        label = "accentCheck"
    )
    RowShell(
        modifier = Modifier
            .tilePress(onTap = onToggle, enabled = enabled)
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                toggleableState = ToggleableState(checked)
                contentDescription = label
                if (enabled) onClick { onToggle(); true }
            }
    ) {
        MonoLabel(label, size = 12.sp, weight = FontWeight.Bold, color = if (enabled) colors.ink else colors.muted, modifier = Modifier.weight(1f), maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .drawBehind { drawRect(fill.value) }
                .border(2.dp, if (enabled) colors.ink else colors.ink.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun AccentModeButtons(selected: String, onSelect: (String) -> Unit) {
    val colors = LocalAppTheme.current
    val modes = listOf("MANUAL" to "manual", "RANDOM" to "random", "ROTATE" to "rotate")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modes.forEach { (label, value) ->
            val isSelected = value == selected
            val fill = animateColorAsState(
                targetValue = if (isSelected) colors.accent else Color.Transparent,
                animationSpec = tween(160, easing = LumiaEasing),
                label = "accentMode"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .tilePress(onTap = { onSelect(value) }, tilt = false)
                    .drawBehind { drawRect(fill.value) }
                    .border(2.dp, if (isSelected) colors.accent else colors.ink)
                    .semantics {
                        role = Role.RadioButton
                        this.selected = isSelected
                        contentDescription = label
                    },
                contentAlignment = Alignment.Center
            ) {
                MonoLabel(label, size = 11.sp, weight = FontWeight.Bold, color = if (isSelected) colors.onAccent else colors.ink)
            }
        }
    }
}

@Composable
private fun PinnedAppRow(
    index: Int,
    app: AppItem,
    isFirst: Boolean,
    isLast: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onUnpin: () -> Unit,
    onAccent: () -> Unit,
    onCaption: (String?) -> Unit
) {
    val colors = LocalAppTheme.current
    var editing by remember(app.packageName) { mutableStateOf(false) }
    var draft by remember(app.packageName, app.caption) { mutableStateOf(app.caption ?: "") }
    val accentFill = animateColorAsState(
        targetValue = if (app.isAccent) colors.accent else Color.Transparent,
        animationSpec = tween(160, easing = LumiaEasing),
        label = "pinnedAccent"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = RowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonoLabel("%02d".format(index + 1), size = 10.sp, color = colors.muted)
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .tilePress(onTap = { editing = !editing }, tilt = false)
                    .semantics(mergeDescendants = true) { onClick { editing = !editing; true } }
            ) {
                MonoLabel(app.label, size = 12.sp, weight = FontWeight.Bold, color = colors.ink, maxLines = 1)
                MonoLabel(
                    app.caption ?: TileCaptions.defaultFor(app.packageName, app.label),
                    size = 9.sp,
                    color = colors.muted,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .tilePress(onTap = onAccent, tilt = false)
                    .drawBehind { drawRect(accentFill.value) }
                    .border(2.dp, colors.ink)
                    .semantics {
                        contentDescription = if (app.isAccent) "Clear accent" else "Make accent"
                        onClick { onAccent(); true }
                    }
            )
            Spacer(Modifier.width(6.dp))
            SquareButton("↑", "Move up", enabled = !isFirst, onClick = onUp)
            Spacer(Modifier.width(6.dp))
            SquareButton("↓", "Move down", enabled = !isLast, onClick = onDown)
            Spacer(Modifier.width(6.dp))
            SquareButton("×", "Unpin", onClick = onUnpin)
        }
        AnimatedVisibility(
            visible = editing,
            enter = expandVertically(tween(220, easing = LumiaEasing)) + fadeIn(tween(220, easing = LumiaEasing)),
            exit = shrinkVertically(tween(160, easing = LumiaEasing)) + fadeOut(tween(160, easing = LumiaEasing))
        ) {
            BrutalTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "CAPTION //",
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                onDone = {
                    onCaption(draft.ifBlank { null })
                    editing = false
                },
                trailingLabel = "SAVE"
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ink.copy(alpha = 0.15f)))
    }
}

@Composable
private fun BrutalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
    trailingLabel: String? = null
) {
    val colors = LocalAppTheme.current
    Row(
        modifier = modifier.border(2.dp, colors.ink).heightIn(min = 48.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                MonoLabel(placeholder, size = 12.sp, color = colors.muted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = StandardType.mono(13.sp).copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (trailingLabel != null) {
            Spacer(Modifier.width(10.dp))
            MonoLabel(
                trailingLabel,
                size = 11.sp,
                weight = FontWeight.Bold,
                color = colors.accent,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .tilePress(onTap = onDone, tilt = false)
                    .semantics { role = Role.Button; onClick { onDone(); true } }
                    .padding(vertical = 12.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Theme swatches
// ---------------------------------------------------------------------------------------------

@Composable
private fun ThemeSwatches(selected: String, onSelect: (String) -> Unit) {
    val colors = LocalAppTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeId.ALL.forEach { id ->
            val isSelected = id == selected
            val fill = Color(swatchColor(id))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .tilePress(onTap = { onSelect(id) }, tilt = false)
                    .semantics(mergeDescendants = true) {
                        role = Role.RadioButton
                        this.selected = isSelected
                        contentDescription = id.uppercase()
                        onClick { onSelect(id); true }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(fill)
                        .selectionRing(isSelected, ringColorFor(fill, colors), inset = 6.dp)
                        .border(2.dp, colors.ink)
                )
                Spacer(Modifier.height(4.dp))
                MonoLabel(id, size = 9.sp, color = if (isSelected) colors.ink else colors.muted)
            }
        }
    }
}

private fun swatchColor(id: String): Long = when (id) {
    ThemeId.MONO -> 0xFF111111
    ThemeId.BLUE -> 0xFF1E56B0
    ThemeId.RED -> 0xFFB3241F
    ThemeId.GREEN -> 0xFF1A7A3C
    ThemeId.PURPLE -> 0xFF5B2FB5
    ThemeId.ORANGE -> 0xFFB8541E
    else -> 0xFF111111
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentColorPicker(current: Long, onPick: (Long) -> Unit) {
    val colors = LocalAppTheme.current
    val presets = remember {
        listOf(
            0xFFE53935L, 0xFFFB8C00L, 0xFFFDD835L, 0xFF43A047L,
            0xFF00897BL, 0xFF1E88E5L, 0xFF8E24AAL, 0xFFD81B60L,
            0xFFE65100L, 0xFFFF6F00L, 0xFFAFB42BL, 0xFF2E7D32L,
            0xFF00695CL, 0xFF0277BDL, 0xFF1565C0L, 0xFF4527A0L,
            0xFF6A1B9AL, 0xFFAD1457L, 0xFFC62828L, 0xFFEF6C00L,
            0xFFF9A825L, 0xFF558B2FL, 0xFF00838FL, 0xFF3949ABL,
            0xFF5E35B1L, 0xFF8E24AAL, 0xFFEC407AL, 0xFFFF7043L,
            0xFF26A69AL, 0xFF42A5F5L, 0xFFAB47BCL, 0xFFFFCA28L,
            0xFF66BB6AL, 0xFF26C6DAL, 0xFF7E57C2L, 0xFFFF5252L,
            0xFFD4E157L, 0xFF80DEEAL, 0xFFB388FFL, 0xFFFF8A80L,
            0xFFFFAB40L, 0xFFB9F6CAL, 0xFFA7FFFFL, 0xFF82B1FFL,
            0xFFB388FFL, 0xFFFF80ABL, 0xFFFFFFFFL, 0xFF000000L
        ).distinct()
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
        MonoLabel("ACCENT", size = 10.sp, color = colors.muted, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { color ->
                val isSelected = color == current
                val fill = Color(color)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .tilePress(onTap = { onPick(color) }, tilt = false)
                        .background(fill)
                        .selectionRing(isSelected, ringColorFor(fill, colors), inset = 4.dp)
                        .border(2.dp, colors.ink)
                        .semantics {
                            role = Role.RadioButton
                            this.selected = isSelected
                            contentDescription = "Accent color"
                            onClick { onPick(color); true }
                        }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonoLabel("CUSTOM ACCENT", size = 11.sp, color = colors.muted)
            val isDefault = current == 0L
            Box(
                modifier = Modifier
                    .tilePress(onTap = { onPick(0L) }, tilt = false)
                    .border(2.dp, if (isDefault) colors.accent else colors.ink)
                    .background(if (isDefault) colors.accent else Color.Transparent)
                    .semantics { role = Role.Button; onClick { onPick(0L); true } }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                MonoLabel("DEFAULT", size = 10.sp, weight = FontWeight.Bold, color = if (isDefault) colors.onAccent else colors.ink)
            }
        }
    }
}

private data class WallpaperPreset(val name: String, val colors: List<Long>)

private val WallpaperPresets = listOf(
    WallpaperPreset("OFF", listOf(0xFF222222L)),
    WallpaperPreset("SUNSET", listOf(0xFFFF7043L, 0xFFEC407AL)),
    WallpaperPreset("OCEAN", listOf(0xFF1E88E5L, 0xFF00897BL)),
    WallpaperPreset("FOREST", listOf(0xFF43A047L, 0xFF1B1B1BL)),
    WallpaperPreset("DUSK", listOf(0xFF7B1FA2L, 0xFF1E88E5L)),
    WallpaperPreset("MONO", listOf(0xFF9E9E9EL, 0xFF000000L))
)

@Composable
private fun WallpaperPresetSwatches(selected: Int, onSelect: (Int) -> Unit) {
    val colors = LocalAppTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WallpaperPresets.forEachIndexed { index, preset ->
            val isSelected = index == selected
            val first = Color(preset.colors.first())
            val brush = if (preset.colors.size >= 2) {
                Brush.horizontalGradient(preset.colors.map { Color(it) })
            } else {
                Brush.horizontalGradient(listOf(first, first))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .tilePress(onTap = { onSelect(index) }, tilt = false)
                    .semantics(mergeDescendants = true) {
                        role = Role.RadioButton
                        this.selected = isSelected
                        contentDescription = preset.name
                        onClick { onSelect(index); true }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(brush)
                        .selectionRing(isSelected, ringColorFor(first, colors), inset = 6.dp)
                        .border(2.dp, colors.ink)
                )
                Spacer(Modifier.height(4.dp))
                MonoLabel(preset.name, size = 9.sp, color = if (isSelected) colors.ink else colors.muted)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Location
// ---------------------------------------------------------------------------------------------

@Composable
private fun LocationEditor(viewModel: StandardAppViewModel, currentName: String) {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val scope = rememberCoroutineScope()
    var query by remember(currentName) { mutableStateOf(currentName) }
    var busy by remember { mutableStateOf(false) }

    fun useLocation(lat: Double, lon: Double) {
        scope.launch {
            val name = withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    Geocoder(context).getFromLocation(lat, lon, 1)?.firstOrNull()?.let { a ->
                        listOfNotNull(a.locality ?: a.subAdminArea ?: a.adminArea, a.countryCode).joinToString(", ")
                    }
                }.getOrNull()
            } ?: "HERE"
            viewModel.refreshForLocation(lat, lon, name.uppercase())
            busy = false
        }
    }

    fun locate() {
        busy = true
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (last != null) {
                useLocation(last.latitude, last.longitude)
                return
            }
            val provider = if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
            lm.getCurrentLocation(provider, null, context.mainExecutor) { loc ->
                if (loc != null) {
                    useLocation(loc.latitude, loc.longitude)
                } else {
                    busy = false
                    Toast.makeText(context, "LOCATION UNAVAILABLE", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            busy = false
            Toast.makeText(context, "LOCATION PERMISSION DENIED", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            busy = false
            Toast.makeText(context, "LOCATION UNAVAILABLE", Toast.LENGTH_SHORT).show()
        }
    }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) locate()
        else Toast.makeText(context, "LOCATION DENIED. TYPE A CITY INSTEAD", Toast.LENGTH_SHORT).show()
    }

    MonoLabel("WEATHER LOCATION", size = 10.sp, color = colors.muted, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
    BrutalTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = "CITY //",
        modifier = Modifier.fillMaxWidth(),
        onDone = {
            if (query.isNotBlank()) {
                busy = true
                scope.launch {
                    val ok = viewModel.setWeatherPlace(query)
                    busy = false
                    Toast.makeText(context, if (ok) "WEATHER SET" else "CITY NOT FOUND", Toast.LENGTH_SHORT).show()
                }
            }
        },
        trailingLabel = if (busy) "..." else "SET"
    )
    ActionRow(if (busy) "LOCATING..." else "USE MY LOCATION") {
        val hasAny = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            .any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        if (hasAny) locate()
        else permissions.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }
}

// ---------------------------------------------------------------------------------------------
// Default launcher
// ---------------------------------------------------------------------------------------------

@Composable
private fun DefaultLauncherRow() {
    val context = LocalContext.current
    val roleManager = remember { context.getSystemService(RoleManager::class.java) }
    var isDefault by remember { mutableStateOf(roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
    }
    LaunchedEffect(Unit) { isDefault = roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true }
    if (isDefault) {
        AboutRow("DEFAULT LAUNCHER", "VANTA. OK")
    } else {
        ActionRow("SET AS DEFAULT LAUNCHER") {
            val rm = roleManager
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                launcher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
            } else {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

@Composable
private fun BatteryOptimizationRow() {
    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager }
    var exempted by remember { mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        exempted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }
    LaunchedEffect(Unit) { exempted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true }
    ValueRow("BATTERY OPTIMIZATION", if (exempted) "EXEMPTED" else "OPTIMIZED") {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:${context.packageName}")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            launcher.launch(intent)
        } catch (e: Exception) { }
    }
}

// ---------------------------------------------------------------------------------------------
// App picker sheet
// ---------------------------------------------------------------------------------------------

private class PickerRequest(
    val title: String,
    val apps: List<AppItem>,
    val allowNone: Boolean = false,
    val onPick: (AppItem?) -> Unit
)

/** Full-bleed paper sheet: title bar, mono search field, 52dp rows that turnstile in. */
@Composable
private fun AppPicker(request: PickerRequest, onDismiss: () -> Unit, onPick: (AppItem?) -> Unit) {
    val colors = LocalAppTheme.current
    var query by remember { mutableStateOf("") }
    val bornAt = remember { System.currentTimeMillis() }
    val shown = remember(query, request.apps) {
        if (query.isBlank()) request.apps else request.apps.filter { it.label.contains(query, ignoreCase = true) }
    }
    BackHandler { onDismiss() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .plainClickable { }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(BarHeight).padding(horizontal = PagePadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeadlineText(request.title, 28.sp, modifier = Modifier.weight(1f), maxLines = 1)
            Spacer(Modifier.width(12.dp))
            CloseButton(onClick = onDismiss, description = "Close picker")
        }
        BrutalTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "SEARCH //",
            modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding)
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = PagePadding, end = PagePadding, bottom = 24.dp)
        ) {
            if (request.allowNone) {
                item(key = "__none") {
                    Box(Modifier.animateItem().turnstileIn(0, bornAt)) {
                        ActionRow("NONE") { onPick(null) }
                    }
                }
            }
            itemsIndexed(shown, key = { _, app -> app.packageName }) { index, app ->
                Box(Modifier.animateItem().turnstileIn(index + 1, bornAt)) {
                    RowShell(
                        modifier = Modifier
                            .tilePress(onTap = { onPick(app) })
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                onClick { onPick(app); true }
                            }
                    ) {
                        MonoLabel(app.label, size = 12.sp, weight = FontWeight.Bold, color = colors.ink, modifier = Modifier.weight(1f))
                        if (app.pinned) Box(Modifier.size(8.dp).background(colors.accent))
                        Spacer(Modifier.width(10.dp))
                        MonoLabel(">", size = 16.sp, weight = FontWeight.Bold, color = colors.ink)
                    }
                }
            }
        }
    }
}
