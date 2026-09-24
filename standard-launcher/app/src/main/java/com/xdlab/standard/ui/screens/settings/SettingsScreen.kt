package com.xdlab.standard.ui.screens.settings

import com.xdlab.standard.R
import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.domain.model.AnimationStyle
import com.xdlab.standard.domain.model.AppItem
import com.xdlab.standard.domain.model.ClockFormat
import com.xdlab.standard.domain.model.HomeModule
import com.xdlab.standard.domain.model.IconStyle
import com.xdlab.standard.domain.model.ThemeId
import com.xdlab.standard.domain.model.WeatherUnit
import com.xdlab.standard.ui.components.DriftingNoiseOverlay
import com.xdlab.standard.ui.components.HeadlineText
import com.xdlab.standard.ui.components.LocalHapticsEnabled
import com.xdlab.standard.ui.components.MonoLabel
import com.xdlab.standard.ui.components.TileCaptions
import com.xdlab.standard.ui.nav.StandardAppViewModel
import com.xdlab.standard.ui.theme.LocalAppTheme
import com.xdlab.standard.ui.theme.StandardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val RowHeight = 52.dp

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
    val haptics = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsEnabled.current
    val tick: () -> Unit = { if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    var picker by remember { mutableStateOf<PickerRequest?>(null) }
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
    val hiddenPrefs = remember { context.getSharedPreferences("standard_hidden", Context.MODE_PRIVATE) }
    var hiddenPackages by remember { mutableStateOf<List<String>>(hiddenPrefs.getStringSet("hidden", emptySet())?.toList() ?: emptyList()) }
    var appearanceExpanded by remember { mutableStateOf(true) }
    var homeExpanded by remember { mutableStateOf(true) }
    var motionExpanded by remember { mutableStateOf(true) }
    var systemExpanded by remember { mutableStateOf(true) }
    var aboutExpanded by remember { mutableStateOf(true) }
    var gesturesExpanded by remember { mutableStateOf(true) }

    val gesturesPrefs = remember { context.getSharedPreferences("standard_gestures", Context.MODE_PRIVATE) }
    var swipeUp by remember { mutableStateOf(gesturesPrefs.getString("swipe_up", "SEARCH") ?: "SEARCH") }
    var swipeDown by remember { mutableStateOf(gesturesPrefs.getString("swipe_down", "SEARCH") ?: "SEARCH") }
    var swipeLeft by remember { mutableStateOf(gesturesPrefs.getString("swipe_left", "NEXT PAGE") ?: "NEXT PAGE") }
    var swipeRight by remember { mutableStateOf(gesturesPrefs.getString("swipe_right", "PREV PAGE") ?: "PREV PAGE") }

    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("first_run", false)) }

    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        DriftingNoiseOverlay(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    HeadlineText("SETTINGS", 40.sp)
                    MonoLabel("STANDARD. // V$version", size = 10.sp, color = colors.muted)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(2.dp, colors.ink)
                        .plainClickable { onClose() }
                        .semantics { contentDescription = "Close settings" },
                    contentAlignment = Alignment.Center
                ) {
                    MonoLabel("✕", size = 18.sp, weight = FontWeight.Bold)
                }
            }

            CollapsibleSection(
                title = "APPEARANCE",
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded }
            ) {
                ThemeSwatches(
                    selected = settings.themeId,
                    onSelect = { tick(); viewModel.setThemeId(it) }
                )
                ToggleRow("DARK MODE", settings.darkMode) { tick(); viewModel.setDarkMode(it) }
                ToggleRow("PAPER GRAIN", settings.useTexture) { tick(); viewModel.setUseTexture(it) }
                if (settings.useTexture) {
                    val strengthLabel = when {
                        settings.textureStrength < 0.08f -> "LIGHT"
                        settings.textureStrength < 0.13f -> "MEDIUM"
                        else -> "HEAVY"
                    }
                    ValueRow("GRAIN STRENGTH", strengthLabel) {
                        tick()
                        viewModel.setTextureStrength(
                            when (strengthLabel) {
                                "LIGHT" -> 0.10f
                                "MEDIUM" -> 0.16f
                                else -> 0.06f
                            }
                        )
                    }
                    ToggleRow("GRAIN DRIFT", settings.noiseDrift) { tick(); viewModel.setNoiseDrift(it) }
                }
                ToggleRow("TIME-OF-DAY TINT", settings.timeOfDayTint) { tick(); viewModel.setTimeOfDayTint(it) }
                ToggleRow("WALLPAPER BACKGROUND", wallpaperBg) {
                    tick()
                    wallpaperBg = it
                    prefs.edit().putBoolean("wallpaper_bg", it).apply()
                }
                ValueRow("WALLPAPER", "SET WALLPAPER") {
                    tick()
                    Toast.makeText(context, "OPENING WALLPAPER PICKER", Toast.LENGTH_SHORT).show()
                    context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                MonoLabel("WALLPAPER PRESETS", size = 10.sp, color = colors.muted, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                WallpaperPresetSwatches(
                    selected = wallpaperPreset,
                    onSelect = {
                        tick()
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
                ) { tick(); viewModel.setIconStyle(settings.iconStyle.next()) }
                ValueRow("ICON RENDER", TileIconStyle.entries[iconStyleGlobal].label) {
                    tick()
                    val nextStyle = TileIconStyle.entries[iconStyleGlobal].next()
                    iconStyleGlobal = nextStyle.ordinal
                    prefs.edit().putInt("icon_style_global", nextStyle.ordinal).apply()
                }
                AccentColorPicker(current = customAccent) { color ->
                    tick()
                    customAccent = color
                    prefs.edit().putLong("custom_accent", color).apply()
                }
                SectionHeader("STATUS BAR")
                ToggleRow("SHOW STATUS BAR", statusBarVisible) {
                    tick()
                    statusBarVisible = it
                    prefs.edit().putBoolean("status_bar_visible", it).apply()
                }
                ToggleRow("TRANSPARENT STATUS BAR", statusBarTransparent) {
                    tick()
                    statusBarTransparent = it
                    prefs.edit().putBoolean("status_bar_transparent", it).apply()
                }
                ValueRow("STATUS BAR ICONS", statusBarIcons) {
                    tick()
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
                    tick()
                    liveTiles = it
                    prefs.edit().putBoolean("live_tiles", it).apply()
                }
                ToggleRow("RANDOM FLIP TIMING", randomFlip) {
                    tick()
                    randomFlip = it
                    prefs.edit().putBoolean("random_flip", it).apply()
                }
                ToggleRow("NOTIFICATION PREVIEWS", notifPreviews) {
                    tick()
                    notifPreviews = it
                    prefs.edit().putBoolean("notif_previews", it).apply()
                }
                SectionHeader("LIVE TILE CONTENT")
                ToggleRow("WEATHER UPDATES", liveWeather) {
                    tick()
                    liveWeather = it
                    prefs.edit().putBoolean("live_weather", it).apply()
                }
                ToggleRow("CLOCK LIVE TILE", liveClock) {
                    tick()
                    liveClock = it
                    prefs.edit().putBoolean("live_clock", it).apply()
                }
                ToggleRow("BATTERY LIVE TILE", liveBattery) {
                    tick()
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
                        tick()
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
                        onUp = { tick(); viewModel.movePinned(app.packageName, -1) },
                        onDown = { tick(); viewModel.movePinned(app.packageName, +1) },
                        onUnpin = { tick(); viewModel.unpin(app.packageName) },
                        onAccent = { tick(); viewModel.setAccent(if (app.isAccent) null else app.packageName) },
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
                            tick()
                            val next = if (app.packageName in accentApps) accentApps - app.packageName else accentApps + app.packageName
                            viewModel.setAccentApps(next)
                            prefs.edit().putString("accent_apps", next.joinToString(",")).apply()
                        }
                    )
                }
                AccentModeButtons(selected = accentMode) { mode ->
                    tick()
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
                        onUp = { tick(); viewModel.setFocusApps(focusApps.map { it.packageName }.swap(index, index - 1)) },
                        onDown = { tick(); viewModel.setFocusApps(focusApps.map { it.packageName }.swap(index, index + 1)) },
                        onRemove = { tick(); viewModel.setFocusApps(focusApps.map { it.packageName } - app.packageName) }
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
                        onRemove = { tick(); viewModel.setQuotes(settings.quotes.filterIndexed { i, _ -> i != index }) },
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
                    tick()
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
                    tick()
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
                ) { tick(); viewModel.setAnimationStyle(settings.animationStyle.next()) }
                ValueRow("SMOOTHNESS", SmoothnessLabels[smoothness]) {
                    tick()
                    val next = (smoothness + 1) % SmoothnessLabels.size
                    smoothness = next
                    prefs.edit().putInt("smoothness_level", next).apply()
                }
                ToggleRow("CINEMATIC INTRO", settings.cinematicIntro) { tick(); viewModel.setCinematicIntro(it) }
                var silkyPager by remember { mutableStateOf(settings.silkyPager) }
                ToggleRow("SILKY PAGER", silkyPager) { silkyPager = it; viewModel.setSilkyPager(it) }
                var slideableHome by remember { mutableStateOf(settings.slideableHome) }
                ToggleRow("SLIDEABLE HOME", slideableHome) { slideableHome = it; viewModel.setSlideableHome(it) }
                var motionTouch by remember { mutableStateOf(settings.motionTouch) }
                ToggleRow("MOTION TOUCH", motionTouch) { motionTouch = it; viewModel.setMotionTouch(it) }
                ToggleRow("GLANCE (HOLD A TAB)", settings.glanceEnabled) { tick(); viewModel.setGlanceEnabled(it) }
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
                ActionRow("BACKUP / RESTORE") {
                    Toast.makeText(context, "USE BUTTONS BELOW", Toast.LENGTH_SHORT).show()
                }
                com.xdlab.standard.ui.components.BackupRestoreButtons(modifier = Modifier.fillMaxWidth())
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
                                .border(2.dp, colors.ink)
                                .plainClickable {
                                    tick()
                                    val next = hiddenPackages - app.packageName
                                    hiddenPackages = next
                                    hiddenPrefs.edit().putStringSet("hidden", next.toSet()).apply()
                                }
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
                    tick()
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
                    tick()
                    swipeDown = "SEARCH"
                    gesturesPrefs.edit().putString("swipe_down", "SEARCH").apply()
                    Toast.makeText(context, "SWIPE DOWN: SEARCH", Toast.LENGTH_SHORT).show()
                }
                ValueRow("SWIPE LEFT", swipeLeft) {
                    tick()
                    swipeLeft = "NEXT PAGE"
                    gesturesPrefs.edit().putString("swipe_left", "NEXT PAGE").apply()
                    Toast.makeText(context, "SWIPE LEFT: NEXT PAGE", Toast.LENGTH_SHORT).show()
                }
                ValueRow("SWIPE RIGHT", swipeRight) {
                    tick()
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

        picker?.let { request ->
            AppPicker(
                request = request,
                onDismiss = { picker = null },
                onPick = { app ->
                    tick()
                    request.onPick(app)
                    picker = null
                }
            )
        }

        if (showOnboarding) {
            FirstRunOnboarding {
                showOnboarding = false
                prefs.edit().putBoolean("first_run", true).apply()
            }
        }
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

@Composable
private fun SectionHeader(title: String) {
    val colors = LocalAppTheme.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 6.dp)) {
        MonoLabel(title, size = 11.sp, weight = FontWeight.Bold, color = colors.ink)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(colors.ink))
    }
}

private data class OnboardingSlide(val title: String, val description: String)

private val OnboardingSlides = listOf(
    OnboardingSlide("SWIPE TO NAVIGATE", "SWIPE LEFT AND RIGHT TO MOVE BETWEEN PAGES."),
    OnboardingSlide("TILES ARE LIVE", "TILES UPDATE IN REAL TIME WITH LIVE DATA."),
    OnboardingSlide("CHECK THE LIVE PAGE", "OPEN THE LIVE PAGE TO SEE CURRENT SESSIONS AND TRACKS.")
)

@Composable
private fun FirstRunOnboarding(onFinish: () -> Unit) {
    val colors = LocalAppTheme.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 3 }
    BackHandler { onFinish() }
    Box(
        modifier = Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.97f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)
        ) {
            Spacer(Modifier.weight(1f))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val slide = OnboardingSlides[page]
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeadlineText(slide.title, 32.sp, modifier = Modifier.weight(1f))
                    MonoLabel(slide.description, size = 12.sp, color = colors.muted, modifier = Modifier.weight(1f), maxLines = 3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).border(2.dp, colors.ink).plainClickable(onFinish),
                            contentAlignment = Alignment.Center
                        ) { MonoLabel("SKIP", size = 12.sp, weight = FontWeight.Bold, color = colors.ink) }
                        Box(
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).border(2.dp, colors.accent).background(colors.accent).plainClickable {
                                if (page < OnboardingSlides.lastIndex) {
                                    scope.launch { pagerState.animateScrollToPage(page + 1) }
                                } else {
                                    onFinish()
                                }
                            },
                            contentAlignment = Alignment.Center
                        ) { MonoLabel(if (page == OnboardingSlides.lastIndex) "FINISH" else "NEXT", size = 12.sp, weight = FontWeight.Bold, color = colors.onAccent) }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = LocalAppTheme.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .plainClickable(onToggle)
                .semantics { contentDescription = "$title section, ${if (expanded) "collapse" else "expand"}" },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonoLabel(title, size = 11.sp, weight = FontWeight.Bold, color = colors.ink)
            MonoLabel(if (expanded) "–" else "+", size = 16.sp, weight = FontWeight.Bold, color = colors.ink)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(colors.ink))
    }
    if (expanded) {
        content()
    }
}

@Composable
private fun AccentColorPicker(current: Long, onPick: (Long) -> Unit) {
    val colors = LocalAppTheme.current
    val presets = remember {
        listOf(
            0xFFE53935L, 0xFFFB8C00L, 0xFFFDD835L, 0xFF43A047L,
            0xFF00897BL, 0xFF1E88E5L, 0xFF8E24AAL, 0xFFD81B60L
        )
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { color ->
                val isSelected = color == current
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(color))
                        .border(if (isSelected) 3.dp else 2.dp, if (isSelected) colors.ink else colors.ink.copy(alpha = 0.35f))
                        .plainClickable { onPick(color) }
                        .semantics { contentDescription = "Accent color" }
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
                    .border(2.dp, if (isDefault) colors.accent else colors.ink)
                    .background(if (isDefault) colors.accent else Color.Transparent)
                    .plainClickable { onPick(0L) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                MonoLabel("DEFAULT", size = 10.sp, weight = FontWeight.Bold, color = if (isDefault) colors.onAccent else colors.ink)
            }
        }
    }
}

@Composable
private fun RowShell(
    modifier: Modifier = Modifier,
    minHeight: Dp = RowHeight,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val colors = LocalAppTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) { content() }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ink.copy(alpha = 0.22f)))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalAppTheme.current
    RowShell(
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onValueChange = onChange
        )
    ) {
        MonoLabel(label, size = 12.sp, weight = FontWeight.Bold, color = colors.ink)
        val knobX by animateDpAsState(if (checked) 22.dp else 2.dp, label = "knob")
        Box(modifier = Modifier.size(width = 40.dp, height = 22.dp).border(2.dp, colors.ink)) {
            Box(
                modifier = Modifier
                    .offset(x = knobX, y = 2.dp)
                    .size(14.dp)
                    .background(if (checked) colors.accent else colors.ink.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    val colors = LocalAppTheme.current
    RowShell(modifier = Modifier.plainClickable(onClick).semantics { contentDescription = "$label, $value" }) {
        MonoLabel(label, size = 12.sp, weight = FontWeight.Bold, color = colors.ink, modifier = Modifier.weight(1f))
        MonoLabel(value, size = 11.sp, color = colors.ink.copy(alpha = 0.8f), maxLines = 1)
        Spacer(Modifier.width(10.dp))
        MonoLabel("›", size = 16.sp, weight = FontWeight.Bold, color = colors.ink)
    }
}

@Composable
private fun ActionRow(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = LocalAppTheme.current
    RowShell(modifier = Modifier.plainClickable(onClick)) {
        MonoLabel(label, size = 12.sp, weight = FontWeight.Bold, color = if (destructive) colors.accent else colors.ink)
        MonoLabel("→", size = 16.sp, weight = FontWeight.Bold, color = if (destructive) colors.accent else colors.ink)
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    val colors = LocalAppTheme.current
    RowShell(minHeight = 40.dp) {
        MonoLabel(label, size = 10.sp, color = colors.muted)
        MonoLabel(value, size = 10.sp, color = colors.ink, maxLines = 1)
    }
}

@Composable
private fun SquareButton(glyph: String, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = LocalAppTheme.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(2.dp, if (enabled) colors.ink else colors.ink.copy(alpha = 0.25f))
            .then(if (enabled) Modifier.plainClickable(onClick) else Modifier)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        MonoLabel(glyph, size = 13.sp, weight = FontWeight.Bold, color = if (enabled) colors.ink else colors.ink.copy(alpha = 0.25f))
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
            SquareButton("▲", "Move up", enabled = !isFirst, onClick = onUp)
            Spacer(Modifier.width(6.dp))
            SquareButton("▼", "Move down", enabled = !isLast, onClick = onDown)
            Spacer(Modifier.width(6.dp))
        }
        SquareButton("✕", "Remove", onClick = onRemove)
    }
}

@Composable
private fun AccentAppCheckRow(label: String, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val colors = LocalAppTheme.current
    val mod = if (enabled) Modifier.plainClickable(onToggle) else Modifier
    RowShell(modifier = mod) {
        MonoLabel(label, size = 12.sp, weight = FontWeight.Bold, color = if (enabled) colors.ink else colors.muted, modifier = Modifier.weight(1f), maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, if (enabled) colors.ink else colors.ink.copy(alpha = 0.35f))
                .background(if (checked) colors.accent else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (checked) MonoLabel("✕", size = 12.sp, weight = FontWeight.Bold, color = colors.onAccent)
        }
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .border(2.dp, if (isSelected) colors.accent else colors.ink)
                    .background(if (isSelected) colors.accent else Color.Transparent)
                    .plainClickable { onSelect(value) }
                    .semantics { contentDescription = label },
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = RowHeight).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonoLabel("%02d".format(index + 1), size = 10.sp, color = colors.muted)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f).plainClickable { editing = !editing }) {
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
                    .border(2.dp, colors.ink)
                    .background(if (app.isAccent) colors.accent else Color.Transparent)
                    .plainClickable(onAccent)
                    .semantics { contentDescription = if (app.isAccent) "Clear accent" else "Make accent" }
            )
            Spacer(Modifier.width(6.dp))
            SquareButton("▲", "Move up", enabled = !isFirst, onClick = onUp)
            Spacer(Modifier.width(6.dp))
            SquareButton("▼", "Move down", enabled = !isLast, onClick = onDown)
            Spacer(Modifier.width(6.dp))
            SquareButton("✕", "Unpin", onClick = onUnpin)
        }
        if (editing) {
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
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ink.copy(alpha = 0.22f)))
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
                modifier = Modifier.heightIn(min = 44.dp).plainClickable(onDone).padding(vertical = 12.dp)
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
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(id) }
                    .semantics { contentDescription = id.uppercase() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(fill)
                        .border(if (isSelected) 3.dp else 2.dp, if (isSelected) colors.ink else colors.ink.copy(alpha = 0.35f))
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
            val brush = if (preset.colors.size >= 2) {
                Brush.horizontalGradient(preset.colors.map { Color(it) })
            } else {
                Brush.verticalGradient(preset.colors.map { Color(it) })
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .plainClickable { onSelect(index) }
                    .semantics { contentDescription = preset.name },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(brush)
                        .border(if (isSelected) 3.dp else 2.dp, if (isSelected) colors.accent else colors.ink.copy(alpha = 0.35f))
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
        trailingLabel = if (busy) "…" else "SET"
    )
    ActionRow(if (busy) "LOCATING…" else "USE MY LOCATION") {
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
        AboutRow("DEFAULT LAUNCHER", "STANDARD. ✓")
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

// ---------------------------------------------------------------------------------------------
// App picker overlay
// ---------------------------------------------------------------------------------------------

private class PickerRequest(
    val title: String,
    val apps: List<AppItem>,
    val allowNone: Boolean = false,
    val onPick: (AppItem?) -> Unit
)

@Composable
private fun AppPicker(request: PickerRequest, onDismiss: () -> Unit, onPick: (AppItem?) -> Unit) {
    val colors = LocalAppTheme.current
    var query by remember { mutableStateOf("") }
    val shown = remember(query, request.apps) {
        if (query.isBlank()) request.apps else request.apps.filter { it.label.contains(query, ignoreCase = true) }
    }
    BackHandler { onDismiss() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.97f))
            .plainClickable { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeadlineText(request.title, 28.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(48.dp).border(2.dp, colors.ink).plainClickable(onDismiss),
                    contentAlignment = Alignment.Center
                ) { MonoLabel("✕", size = 18.sp, weight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(12.dp))
            BrutalTextField(value = query, onValueChange = { query = it }, placeholder = "SEARCH //", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (request.allowNone) {
                    item(key = "__none") { ActionRow("NONE") { onPick(null) } }
                }
                items(shown, key = { it.packageName }) { app ->
                    RowShell(modifier = Modifier.plainClickable { onPick(app) }) {
                        MonoLabel(app.label, size = 12.sp, weight = FontWeight.Bold, color = colors.ink, modifier = Modifier.weight(1f))
                        if (app.pinned) Box(Modifier.size(8.dp).background(colors.accent))
                        Spacer(Modifier.width(10.dp))
                        MonoLabel("→", size = 16.sp, weight = FontWeight.Bold, color = colors.ink)
                    }
                }
            }
        }
    }
}
