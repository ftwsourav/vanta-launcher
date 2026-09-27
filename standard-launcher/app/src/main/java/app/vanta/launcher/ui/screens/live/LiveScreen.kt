package app.vanta.launcher.ui.screens.live

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.vanta.launcher.StandardApplication
import app.vanta.launcher.data.remote.StandardMediaListener
import app.vanta.launcher.domain.model.MediaInfo
import app.vanta.launcher.ui.components.AppIcon
import app.vanta.launcher.ui.components.HeadlineText
import app.vanta.launcher.ui.components.LocalHapticsEnabled
import app.vanta.launcher.ui.components.LocalTileColors
import app.vanta.launcher.ui.components.LumiaEasing
import app.vanta.launcher.ui.components.MonoLabel
import app.vanta.launcher.ui.components.Tile
import app.vanta.launcher.ui.components.TileDefaults
import app.vanta.launcher.ui.components.TileEntrance
import app.vanta.launcher.ui.components.liveClockFormatted
import app.vanta.launcher.ui.components.liveTimeFormatted
import app.vanta.launcher.ui.components.rememberBatteryState
import app.vanta.launcher.ui.components.tilePress
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.SpaceGrotesk
import app.vanta.launcher.ui.theme.StandardType
import app.vanta.launcher.util.RefreshRate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sign

/** Set once the user taps GOT IT on the re-grant explainer; cleared again when the listener reconnects. */
private const val RegrantKey = "notif_regrant_prompted"

data class NotifItem(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isActionable: Boolean
) {
    val key: String get() = packageName + timestamp
}

@Composable
fun LiveScreen(
    modifier: Modifier = Modifier,
    onOpenFocus: () -> Unit = {},
    onOpenSearch: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val checkGranted = { NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
    var granted by remember { mutableStateOf(checkGranted()) }
    var connected by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf<List<NotifItem>>(emptyList()) }
    var dismissed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var mutedApps by remember { mutableStateOf(loadMutedApps(context)) }
    val prefs = remember { context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE) }
    var regrantDismissed by remember { mutableStateOf(prefs.getBoolean(RegrantKey, false)) }
    val toggleMute = { pkg: String ->
        val updated = if (pkg in mutedApps) mutedApps - pkg else mutedApps + pkg
        saveMutedApps(context, updated)
        mutedApps = updated
    }
    val refreshAccess = {
        granted = checkGranted()
        connected = !granted || StandardMediaListener.connected.value || listenerConnected(context)
    }
    // New or removed notifications land immediately instead of on the next poll.
    val listenerFeed by StandardMediaListener.active.collectAsState()
    LaunchedEffect(listenerFeed) {
        if (granted) notifications = try { loadActiveNotifications(context) } catch (e: Exception) { emptyList() }
    }

    LifecycleResumeEffect(Unit) {
        refreshAccess()
        onPauseOrDispose { }
    }

    LaunchedEffect(granted, connected) {
        if (granted && connected && regrantDismissed) {
            prefs.edit().putBoolean(RegrantKey, false).apply()
            regrantDismissed = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshAccess()
            notifications = if (granted) {
                try {
                    loadActiveNotifications(context)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            delay(2000)
        }
    }

    val visible = notifications.filter { it.key !in dismissed }
    val dismiss = { n: NotifItem -> dismissed = dismissed + n.key }
    val openNotif = { n: NotifItem ->
        dismiss(n)
        launchApp(context, n.packageName)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
    ) {
        Text(
            text = "live",
            style = StandardType.display(40.sp),
            color = colors.ink.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 2.dp)
        )

        NowClock()

        TileEntrance(0) { NowPlayingRow() }
        TileEntrance(1) { BatteryRow() }
        TileEntrance(2) { AlarmRow() }
        TileEntrance(3) { WeatherRow() }

        when {
            !granted -> TileEntrance(4) {
                GrantAccessTile { openNotificationListenerSettings(context) }
            }
            !connected && !regrantDismissed -> TileEntrance(4) {
                RegrantExplainerTile(
                    onOpenSettings = { openNotificationListenerSettings(context) },
                    onDismiss = {
                        prefs.edit().putBoolean(RegrantKey, true).apply()
                        regrantDismissed = true
                    }
                )
            }
        }

        if (granted) {
            var index = 5
            if (visible.isEmpty()) {
                EmptyState(onOpenFocus, onOpenSearch)
            } else {
                val active = visible.filter { it.packageName !in mutedApps }
                val muted = visible.filter { it.packageName in mutedApps }
                val actionable = active.filter { it.isActionable }
                val regular = active.filter { !it.isActionable }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    MonoLabel(
                        "CLEAR ALL",
                        modifier = Modifier
                            .tilePress(onTap = { dismissed = dismissed + visible.map { it.key } }, tilt = false)
                            .padding(4.dp),
                        size = 11.sp,
                        color = colors.accent,
                        weight = FontWeight.Bold
                    )
                }

                if (actionable.isNotEmpty()) {
                    SectionRule("ACTION REQUIRED", colors.accent)
                    actionable.forEach { n ->
                        key(n.key) {
                            TileEntrance(index++) {
                                NotifCard(
                                    notif = n,
                                    isActionable = true,
                                    onOpen = { openNotif(n) },
                                    onDismiss = { dismiss(n) },
                                    onToggleMute = { toggleMute(n.packageName) }
                                )
                            }
                        }
                    }
                }

                if (regular.isNotEmpty()) {
                    SectionRule("NOTIFICATIONS", colors.ink)
                    regular.groupBy { it.packageName }.forEach { (pkg, group) ->
                        key(pkg) {
                            TileEntrance(index++) {
                                NotifGroup(group, isMuted = false, onOpen = openNotif, onDismiss = dismiss, onToggleMute = toggleMute)
                            }
                        }
                    }
                }

                if (muted.isNotEmpty()) {
                    SectionRule("MUTED", colors.ink.copy(alpha = 0.4f))
                    muted.groupBy { it.packageName }.forEach { (pkg, group) ->
                        key("muted:$pkg") {
                            TileEntrance(index++) {
                                NotifGroup(group, isMuted = true, onOpen = openNotif, onDismiss = dismiss, onToggleMute = toggleMute)
                            }
                        }
                    }
                }
            }

            if (mutedApps.isNotEmpty()) {
                TileEntrance(index) { MutedAppsTile(mutedApps, onUnmute = toggleMute) }
            }
        }
    }
}

/** Big tabular clock; each minute change slides the digits up 6dp with a fade. */
@Composable
private fun NowClock() {
    val colors = LocalAppTheme.current
    val density = LocalDensity.current
    val full = liveClockFormatted()
    val digits = full.substringBefore(' ')
    val suffix = full.substringAfter(' ', "")
    val slide = with(density) { 6.dp.roundToPx() }
    val style = remember { StandardType.display(72.sp).copy(fontFeatureSettings = "tnum") }
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedContent(
                targetState = digits,
                transitionSpec = {
                    (slideInVertically(tween(220, easing = LumiaEasing)) { slide } + fadeIn(tween(220, easing = LumiaEasing)))
                        .togetherWith(slideOutVertically(tween(160, easing = LumiaEasing)) { -slide } + fadeOut(tween(160, easing = LumiaEasing)))
                        .using(SizeTransform(clip = false))
                },
                label = "clock"
            ) { t ->
                Text(text = t, style = style, color = colors.ink)
            }
            if (suffix.isNotEmpty()) {
                MonoLabel(
                    suffix,
                    modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
                    size = 12.sp,
                    color = colors.ink.copy(alpha = 0.6f),
                    weight = FontWeight.Bold
                )
            }
        }
        Row(modifier = Modifier.padding(top = 2.dp)) {
            MonoLabel(liveTimeFormatted("EEEE"), size = 12.sp, color = colors.accent, weight = FontWeight.Bold)
            MonoLabel(" · " + liveTimeFormatted("MMM d"), size = 12.sp, color = colors.ink.copy(alpha = 0.6f), weight = FontWeight.Bold)
        }
    }
}

/** 56dp outline status row: label on the left, trailing meta pinned to the right edge. */
@Composable
private fun LiveRow(
    label: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    trailingAccent: Boolean = false,
    dim: Boolean = false,
    dot: Color? = null,
    onTap: (() -> Unit)? = null
) {
    val accent = LocalAppTheme.current.accent
    Tile(modifier = modifier.fillMaxWidth().height(56.dp), contentPadding = 0.dp, onClick = onTap) {
        val c = LocalTileColors.current.content
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dot != null) {
                Box(Modifier.size(8.dp).background(dot))
                Spacer(Modifier.width(10.dp))
            }
            MonoLabel(
                label,
                modifier = Modifier.weight(1f),
                size = 12.sp,
                color = c.copy(alpha = if (dim) 0.5f else 1f),
                weight = FontWeight.Bold,
                maxLines = 1
            )
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                MonoLabel(
                    trailing,
                    size = 11.sp,
                    color = if (trailingAccent) accent else c.copy(alpha = 0.5f),
                    weight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun NowPlayingRow() {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val repo = remember { (context.applicationContext as? StandardApplication)?.container?.mediaRepository }
    val info by repo?.nowPlaying?.collectAsState() ?: remember { mutableStateOf<MediaInfo?>(null) }
    val playing = info
    LiveRow(
        label = if (playing != null) "NOW PLAYING · ${playing.title ?: "—"}" else "NOTHING PLAYING",
        trailing = when {
            playing == null -> null
            playing.isPlaying -> "PAUSE"
            else -> "PLAY"
        },
        trailingAccent = true,
        dim = playing == null,
        dot = if (playing != null) colors.accent else colors.ink.copy(alpha = 0.4f),
        onTap = if (playing != null) ({ repo?.playPause() }) else null
    )
}

@Composable
private fun BatteryRow() {
    val context = LocalContext.current
    val (level, charging) = rememberBatteryState()
    val hz = remember { RefreshRate.readHz(context).toInt() }
    LiveRow(
        label = (if (charging) "CHARGING" else "BATTERY") + " $level%",
        trailing = "$hz HZ",
        onTap = { open(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) }
    )
}

@Composable
private fun AlarmRow() {
    val context = LocalContext.current
    val fmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val next by produceState(-1L) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        while (true) {
            value = am?.nextAlarmClock?.triggerTime ?: -1L
            delay(30_000)
        }
    }
    LiveRow(
        label = if (next > 0) "NEXT ALARM" else "NO ALARM SET",
        trailing = if (next > 0) fmt.format(Date(next)) else null,
        trailingAccent = true,
        dim = next <= 0,
        onTap = { open(context, Intent(AlarmClock.ACTION_SHOW_ALARMS)) }
    )
}

@Composable
private fun WeatherRow() {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val label = remember {
        val p = context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE)
        val temp = p.getString("weather_temp", null) ?: "18°"
        val cond = p.getString("weather_condition", null) ?: "PARTLY CLOUDY"
        val city = p.getString("weather_city", null) ?: "LONDON"
        "$temp $cond · $city"
    }
    LiveRow(label = label, trailing = "//", dot = colors.accent)
}

@Composable
private fun EmptyState(onOpenFocus: () -> Unit, onOpenSearch: () -> Unit) {
    val colors = LocalAppTheme.current
    val pulse = rememberInfiniteTransition(label = "emptyPulse").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
    ) {
        Box(Modifier.size(10.dp).graphicsLayer { alpha = pulse.value }.background(colors.accent))
        HeadlineText("YOUR PIVOTS ARE QUIET", 22.sp, color = colors.ink.copy(alpha = 0.5f))
        Spacer(Modifier.height(4.dp))
        LiveRow("VIEW FOCUS MODE", trailing = ">", trailingAccent = true, onTap = onOpenFocus)
        LiveRow("SEARCH APPS", trailing = ">", trailingAccent = true, onTap = onOpenSearch)
    }
}

/** Mono caption over a 2dp rule. */
@Composable
private fun SectionRule(text: String, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        MonoLabel(text, size = 11.sp, color = color, weight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(color))
    }
}

/** Inset stroke whose colour is read at draw time, so a pulsing border never recomposes. */
private fun Modifier.strokeRect(width: Dp, color: () -> Color): Modifier = drawWithCache {
    val w = width.toPx()
    val stroke = Stroke(w)
    val inset = Offset(w / 2f, w / 2f)
    val rect = Size(size.width - w, size.height - w)
    onDrawBehind { drawRect(color(), topLeft = inset, size = rect, style = stroke) }
}

@Composable
private fun GrantAccessTile(onTap: () -> Unit) {
    val colors = LocalAppTheme.current
    val pulse = rememberInfiniteTransition(label = "grantPulse").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "grantBorder"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tilePress(onTap = onTap)
            .background(colors.tile)
            .strokeRect(2.dp) { colors.accent.copy(alpha = pulse.value) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(colors.accent))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            MonoLabel("GRANT NOTIFICATION ACCESS", size = 12.sp, color = colors.accent, weight = FontWeight.Bold)
            MonoLabel("TAP TO OPEN SETTINGS · NEEDED FOR LIVE", size = 9.sp, color = colors.onTile.copy(alpha = 0.6f))
        }
        Spacer(Modifier.width(12.dp))
        Text(text = ">", style = StandardType.display(20.sp), color = colors.accent)
    }
}

@Composable
private fun RegrantExplainerTile(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppTheme.current
    val pulse = rememberInfiniteTransition(label = "regrantPulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "regrantBorder"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.tile)
            .strokeRect(2.dp) { colors.accent.copy(alpha = pulse.value) }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(colors.accent))
            Spacer(Modifier.width(10.dp))
            HeadlineText("NOTIFICATIONS NEED RE-ENABLING", 16.sp, color = colors.accent, modifier = Modifier.weight(1f))
        }
        Text(
            text = "Vanta was just updated. Android requires you to re-grant notification access for the Live page to work. This is a one-time step.",
            style = StandardType.mono(11.sp),
            color = colors.onTile.copy(alpha = 0.8f)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("OPEN SETTINGS", colors.accent, Modifier.weight(1f), onOpenSettings)
            ActionButton("GOT IT", colors.onTile.copy(alpha = 0.6f), Modifier, onDismiss)
        }
    }
}

@Composable
private fun ActionButton(label: String, color: Color, modifier: Modifier, onTap: () -> Unit) {
    Box(
        modifier = modifier
            .tilePress(onTap = onTap, tilt = false)
            .strokeRect(2.dp) { color }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        MonoLabel(label, size = 12.sp, color = color, weight = FontWeight.Bold)
    }
}

/** Notification card: tap opens, long-press shows an inline action strip, swipe past 40% dismisses. */
@Composable
private fun NotifCard(
    notif: NotifItem,
    isActionable: Boolean,
    isMuted: Boolean = false,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onToggleMute: () -> Unit = {}
) {
    val colors = LocalAppTheme.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsEnabled.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    val offsetX = remember { Animatable(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    var pastThreshold by remember { mutableStateOf(false) }
    val hot = isActionable && !isMuted
    val ink = colors.onTile
    val pulse = rememberInfiniteTransition(label = "cardPulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "borderPulse"
    )
    val dragState = rememberDraggableState { delta ->
        val next = offsetX.value + delta
        scope.launch { offsetX.snapTo(next) }
        val over = widthPx > 0 && abs(next) > widthPx * 0.4f
        if (over != pastThreshold) {
            pastThreshold = over
            if (over && hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width }
            .graphicsLayer {
                val x = offsetX.value
                translationX = x
                alpha = (if (isMuted) 0.55f else 1f) * (1f - 0.5f * abs(x) / widthPx.coerceAtLeast(1))
            }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    val past = pastThreshold
                    pastThreshold = false
                    if (past) {
                        offsetX.animateTo(sign(offsetX.value) * widthPx, tween(160, easing = LumiaEasing))
                        onDismiss()
                    } else {
                        offsetX.animateTo(0f, RefreshRate.springSpec())
                    }
                }
            )
            .tilePress(
                onTap = onOpen,
                onLongPress = {
                    if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    menu = !menu
                }
            )
            .background(colors.tile)
            .strokeRect(2.dp) {
                when {
                    hot -> colors.accent.copy(alpha = pulse.value)
                    isMuted -> ink.copy(alpha = 0.3f)
                    else -> ink
                }
            }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            AppIcon(packageName = notif.packageName, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoLabel(
                        if (isMuted) "${notif.appName} · MUTED" else notif.appName,
                        modifier = Modifier.weight(1f),
                        size = 11.sp,
                        color = ink.copy(alpha = 0.6f),
                        weight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(12.dp))
                    MonoLabel(timeAgo(notif.timestamp), size = 10.sp, color = ink.copy(alpha = 0.45f))
                }
                if (notif.title.isNotBlank()) {
                    Text(
                        text = notif.title,
                        style = StandardType.headline(15.sp),
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                if (notif.text.isNotBlank()) {
                    Text(
                        text = notif.text,
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = ink.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (hot) {
                Spacer(Modifier.width(8.dp))
                Text(text = ">", style = StandardType.display(20.sp), color = colors.accent)
            }
        }
        AnimatedVisibility(
            visible = menu,
            enter = expandVertically(tween(220, easing = LumiaEasing)) + fadeIn(tween(220, easing = LumiaEasing)),
            exit = shrinkVertically(tween(160, easing = LumiaEasing)) + fadeOut(tween(160, easing = LumiaEasing))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuAction("OPEN") { menu = false; onOpen() }
                MenuAction("SETTINGS") { menu = false; openAppNotificationSettings(context, notif.packageName) }
                MenuAction(if (isMuted) "UNMUTE" else "MUTE") { menu = false; onToggleMute() }
                Spacer(Modifier.weight(1f))
                MenuAction("DISMISS", color = ink.copy(alpha = 0.5f)) { onDismiss() }
            }
        }
    }
}

@Composable
private fun MenuAction(label: String, color: Color = LocalAppTheme.current.accent, onTap: () -> Unit) {
    MonoLabel(
        label,
        modifier = Modifier.tilePress(onTap = onTap, tilt = false).padding(vertical = 4.dp),
        size = 11.sp,
        color = color,
        weight = FontWeight.Bold
    )
}

@Composable
private fun GroupHeader(packageName: String, label: String, trailing: String, dim: Boolean, onToggle: () -> Unit) {
    val colors = LocalAppTheme.current
    val c = colors.ink.copy(alpha = if (dim) 0.4f else 1f)
    Column(modifier = Modifier.fillMaxWidth().tilePress(onTap = onToggle, tilt = false)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = packageName, size = 20.dp)
            Spacer(Modifier.width(8.dp))
            MonoLabel(label, modifier = Modifier.weight(1f), size = 11.sp, color = c, weight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.width(12.dp))
            MonoLabel(trailing, size = 11.sp, color = if (dim) c else colors.accent, weight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(c))
    }
}

@Composable
private fun NotifGroup(
    notifs: List<NotifItem>,
    isMuted: Boolean,
    onOpen: (NotifItem) -> Unit,
    onDismiss: (NotifItem) -> Unit,
    onToggleMute: (String) -> Unit
) {
    val first = notifs.first()
    var expanded by remember(first.packageName) { mutableStateOf(false) }
    val shown = if (expanded || notifs.size == 1) notifs else notifs.take(1)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (notifs.size > 1) {
            GroupHeader(
                packageName = first.packageName,
                label = "${first.appName} (${notifs.size})",
                trailing = if (expanded) "− ${notifs.size - 1} LESS" else "+ ${notifs.size - 1} MORE",
                dim = isMuted,
                onToggle = { expanded = !expanded }
            )
        }
        shown.forEachIndexed { i, n ->
            key(n.key) {
                val card: @Composable () -> Unit = {
                    NotifCard(
                        notif = n,
                        isActionable = false,
                        isMuted = isMuted,
                        onOpen = { onOpen(n) },
                        onDismiss = { onDismiss(n) },
                        onToggleMute = { onToggleMute(n.packageName) }
                    )
                }
                if (i == 0) card() else TileEntrance(i, content = card)
            }
        }
    }
}

@Composable
private fun MutedAppsTile(mutedApps: Set<String>, onUnmute: (String) -> Unit) {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    Tile(modifier = Modifier.fillMaxWidth()) {
        val c = LocalTileColors.current.content
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MonoLabel("MUTED APPS", size = 11.sp, color = c.copy(alpha = 0.4f), weight = FontWeight.Bold)
            mutedApps.forEach { pkg ->
                key(pkg) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel(
                            resolveAppName(context, pkg),
                            modifier = Modifier.weight(1f),
                            size = 11.sp,
                            color = c.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                        Spacer(Modifier.width(12.dp))
                        MonoLabel(
                            "UNMUTE",
                            modifier = Modifier.tilePress(onTap = { onUnmute(pkg) }, tilt = false),
                            size = 10.sp,
                            color = colors.accent,
                            weight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun timeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "now"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        else -> "${diff / 86400000}d"
    }
}

/**
 * True when the system will actually talk to our listener. After an update Android can leave the
 * package in the enabled list but never rebind; session lookups then throw SecurityException.
 */
private fun listenerConnected(context: Context): Boolean = try {
    val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    msm?.getActiveSessions(ComponentName(context, StandardMediaListener::class.java))
    true
} catch (e: SecurityException) {
    false
} catch (e: Exception) {
    true
}

private fun loadActiveNotifications(context: Context): List<NotifItem> {
    return try {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // The bound listener sees every app; NotificationManager only ever returns our own.
            val active = StandardMediaListener.active.value.takeIf { StandardMediaListener.connected.value }
                ?: nm.activeNotifications.toList()
            active.mapNotNull { sbn ->
                try {
                    val n = sbn.notification
                    val pkg = sbn.packageName
                    val pm = context.packageManager
                    val appName = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    } catch (e: Exception) {
                        pkg.substringAfterLast('.')
                    }.uppercase()
                    val extras = n.extras
                    val title = extras.getString("android.title") ?: ""
                    val text = extras.getString("android.text") ?: extras.getString("android.bigText") ?: ""
                    val isActionable = n.actions != null && n.actions.isNotEmpty()
                    NotifItem(pkg, appName, title, text, sbn.postTime, isActionable)
                } catch (e: Exception) { null }
            }.filter { it.title.isNotBlank() || it.text.isNotBlank() }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) { }
}

private fun launchApp(context: Context, packageName: String) {
    context.packageManager.getLaunchIntentForPackage(packageName)?.let { open(context, it) }
}

private fun openNotificationListenerSettings(context: Context) =
    open(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

private fun openAppNotificationSettings(context: Context, packageName: String) =
    open(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))

private fun loadMutedApps(context: Context): Set<String> {
    val prefs = context.getSharedPreferences("standard_muted_apps", Context.MODE_PRIVATE)
    return prefs.getString("apps", "")?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
}

private fun saveMutedApps(context: Context, apps: Set<String>) {
    val prefs = context.getSharedPreferences("standard_muted_apps", Context.MODE_PRIVATE)
    prefs.edit().putString("apps", apps.joinToString(",")).apply()
}

private fun resolveAppName(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString().uppercase()
    } catch (e: Exception) {
        packageName.substringAfterLast('.').uppercase()
    }
}
