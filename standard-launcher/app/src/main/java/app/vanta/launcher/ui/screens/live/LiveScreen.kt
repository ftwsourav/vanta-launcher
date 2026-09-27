package app.vanta.launcher.ui.screens.live

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.vanta.launcher.StandardApplication
import app.vanta.launcher.domain.model.MediaInfo
import app.vanta.launcher.ui.components.AppIcon
import app.vanta.launcher.ui.components.TileEntrance
import app.vanta.launcher.ui.theme.JetBrainsMono
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.SpaceGrotesk
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotifItem(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isActionable: Boolean
)

@Composable
fun LiveScreen(
    modifier: Modifier = Modifier,
    onOpenFocus: () -> Unit = {},
    onOpenSearch: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val haptics = LocalHapticFeedback.current
    val checkGranted = { NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
    var granted by remember { mutableStateOf(checkGranted()) }
    var notifications by remember { mutableStateOf<List<NotifItem>>(emptyList()) }
    var dismissed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var mutedApps by remember { mutableStateOf(loadMutedApps(context)) }
    val regrantPrefs = remember { context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE) }
    var regrantPrompted by remember { mutableStateOf(regrantPrefs.getBoolean("notif_regrant_prompted", false)) }
    val toggleMute = { pkg: String ->
        val updated = if (pkg in mutedApps) mutedApps - pkg else mutedApps + pkg
        saveMutedApps(context, updated)
        mutedApps = updated
    }

    LifecycleResumeEffect(Unit) {
        granted = checkGranted()
        onPauseOrDispose { }
    }

    LaunchedEffect(granted) {
        if (granted && !regrantPrompted) {
            regrantPrefs.edit().putBoolean("notif_regrant_prompted", true).apply()
            regrantPrompted = true
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            granted = checkGranted()
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

    val visible = notifications.filter { !dismissed.contains(it.packageName + it.timestamp) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "live",
            color = colors.ink,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        TileEntrance(0) {
            NowClock()
        }

        TileEntrance(0) {
            NowPlaying()
        }

        TileEntrance(0) {
            StatusBar()
        }

        TileEntrance(0) {
            NextAlarm()
        }

        TileEntrance(0) {
            WeatherMini()
        }

        TileEntrance(0) {
            app.vanta.launcher.ui.components.CalendarNextEvent()
        }

        if (!granted) {
            if (!regrantPrompted) {
                TileEntrance(1) {
                    RegrantExplainerTile(
                        onOpenSettings = { openNotificationListenerSettings(context) },
                        onDismiss = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            regrantPrefs.edit().putBoolean("notif_regrant_prompted", true).apply()
                            regrantPrompted = true
                        }
                    )
                }
            }
            TileEntrance(1) {
                GrantAccessTile { openNotificationListenerSettings(context) }
            }
        } else {
            var tileIndex = 0

            if (visible.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "CLEAR ALL",
                        color = colors.accent,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            dismissed = visible.map { it.packageName + it.timestamp }.toSet() + dismissed
                        }
                    )
                }
            }

            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val pulse = rememberInfiniteTransition(label = "emptyPulse")
                        val pulseAlpha by pulse.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                            label = "pulse"
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .alpha(pulseAlpha)
                                .background(colors.accent)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "YOUR PIVOTS ARE QUIET",
                            color = colors.ink.copy(alpha = 0.5f),
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            lineHeight = 24.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        SuggestTile("VIEW FOCUS MODE", ">", onTap = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onOpenFocus() })
                        SuggestTile("SEARCH APPS", ">", onTap = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onOpenSearch() })
                        SuggestTile("REFRESH WEATHER", ">", onTap = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); Toast.makeText(context, "WEATHER REFRESHED", Toast.LENGTH_SHORT).show() })
                    }
                }
            } else {
                val mutedVisible = visible.filter { it.packageName in mutedApps }
                val activeVisible = visible.filter { it.packageName !in mutedApps }
                val actionable = activeVisible.filter { it.isActionable }
                val regular = activeVisible.filter { !it.isActionable }
                val regularGrouped = regular.groupBy { it.packageName }
                val mutedGrouped = mutedVisible.groupBy { it.packageName }

                if (actionable.isNotEmpty()) {
                    TileEntrance(tileIndex++) {
                        Text(
                            text = "ACTION REQUIRED",
                            color = colors.accent,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    actionable.forEach { notif ->
                        TileEntrance(tileIndex++) {
                            NotifCard(
                                notif = notif,
                                isActionable = true,
                                isMuted = false,
                                onOpen = {
                                    dismissed = dismissed + (notif.packageName + notif.timestamp)
                                    launchApp(context, notif.packageName)
                                },
                                onDismiss = {
                                    dismissed = dismissed + (notif.packageName + notif.timestamp)
                                },
                                onToggleMute = { toggleMute(notif.packageName) }
                            )
                        }
                    }
                }

                if (regular.isNotEmpty()) {
                    TileEntrance(tileIndex++) {
                        Text(
                            text = "NOTIFICATIONS",
                            color = colors.ink,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    regularGrouped.values.forEach { group ->
                        TileEntrance(tileIndex++) {
                            NotifGroup(
                                notifs = group,
                                isMuted = false,
                                onOpen = {
                                    dismissed = dismissed + (it.packageName + it.timestamp)
                                    launchApp(context, it.packageName)
                                },
                                onDismiss = {
                                    dismissed = dismissed + (it.packageName + it.timestamp)
                                },
                                onToggleMute = { pkg -> toggleMute(pkg) }
                            )
                        }
                    }
                }

                if (mutedVisible.isNotEmpty()) {
                    TileEntrance(tileIndex++) {
                        Text(
                            text = "MUTED",
                            color = colors.ink.copy(alpha = 0.4f),
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    mutedGrouped.values.forEach { group ->
                        TileEntrance(tileIndex++) {
                            NotifGroup(
                                notifs = group,
                                isMuted = true,
                                onOpen = {
                                    dismissed = dismissed + (it.packageName + it.timestamp)
                                    launchApp(context, it.packageName)
                                },
                                onDismiss = {
                                    dismissed = dismissed + (it.packageName + it.timestamp)
                                },
                                onToggleMute = { pkg -> toggleMute(pkg) }
                            )
                        }
                    }
                }
            }

            if (mutedApps.isNotEmpty()) {
                TileEntrance(tileIndex++) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.ink.copy(alpha = 0.2f))
                            .background(colors.tile)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "MUTED APPS",
                            color = colors.ink.copy(alpha = 0.4f),
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        mutedApps.forEach { pkg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = resolveAppName(context, pkg),
                                    color = colors.ink.copy(alpha = 0.5f),
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "UNMUTE",
                                    color = colors.accent,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        toggleMute(pkg)
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
private fun NowClock() {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val timeFmt = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE · MMM d", Locale.getDefault()) }
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }
    Column {
        Text(
            text = timeFmt.format(now).uppercase(),
            color = colors.ink,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Black,
            fontSize = 72.sp,
            lineHeight = 72.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = dateFmt.format(now).uppercase(),
            color = colors.accent,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun NowPlaying() {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val repo = (context.applicationContext as? StandardApplication)?.container?.mediaRepository
    val flow = repo?.nowPlaying
    val info by flow?.collectAsState() ?: androidx.compose.runtime.remember { mutableStateOf<MediaInfo?>(null) }
    val active = info != null
    val label = if (active) {
        "NOW PLAYING · ${info?.title?.uppercase()}"
    } else {
        "NOTHING PLAYING"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (active) colors.accent else colors.ink.copy(alpha = 0.3f))
            .background(colors.tile)
            .clickable { if (active) repo?.playPause() else Toast.makeText(context, "NOTHING PLAYING", Toast.LENGTH_SHORT).show() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (active) 8.dp else 6.dp)
                .background(if (active) colors.accent else colors.ink.copy(alpha = 0.4f))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = if (active) colors.ink else colors.ink.copy(alpha = 0.5f),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun StatusBar() {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    val live = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.ink.copy(alpha = 0.3f))
            .background(colors.tile)
            .clickable { Toast.makeText(context, "STATUS", Toast.LENGTH_SHORT).show() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (level >= 0) "BATTERY $level%" else "BATTERY --",
            color = colors.ink,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            text = if (live) "LIVE" else "120HZ",
            color = if (live) colors.accent else colors.ink.copy(alpha = 0.5f),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun NextAlarm() {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    val next = am?.nextAlarmClock
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val label = if (next != null && next.triggerTime > 0) {
        "NEXT ALARM · ${timeFmt.format(Date(next.triggerTime)).uppercase()}"
    } else {
        "NO ALARM SET"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.ink.copy(alpha = 0.3f))
            .background(colors.tile)
            .clickable { Toast.makeText(context, "ALARM", Toast.LENGTH_SHORT).show() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.ink,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            text = if (next != null && next.triggerTime > 0) "⏰" else "X",
            color = if (next != null && next.triggerTime > 0) colors.accent else colors.ink.copy(alpha = 0.5f),
            fontFamily = JetBrainsMono,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SuggestTile(label: String, glyph: String, onTap: () -> Unit) {
    val colors = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.ink.copy(alpha = 0.3f))
            .background(colors.tile)
            .clickable { onTap() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = glyph,
            color = colors.accent,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = colors.ink,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ">",
            color = colors.accent,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun WeatherMini() {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val prefs = remember { context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE) }
    val temp = remember { prefs.getString("weather_temp", null) ?: "18°" }
    val condition = remember { prefs.getString("weather_condition", null) ?: "PARTLY CLOUDY" }
    val city = remember { prefs.getString("weather_city", null) ?: "LONDON" }
    val label = "$temp $condition · $city"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.ink.copy(alpha = 0.3f))
            .background(colors.tile)
            .clickable { Toast.makeText(context, "WEATHER", Toast.LENGTH_SHORT).show() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(colors.accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = colors.ink,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            text = "//",
            color = colors.ink.copy(alpha = 0.4f),
            fontFamily = JetBrainsMono,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun GrantAccessTile(onTap: () -> Unit) {
    val colors = LocalAppTheme.current
    val pulse = rememberInfiniteTransition(label = "grantPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "grantBorder"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, colors.accent.copy(alpha = pulseAlpha))
            .background(colors.tile)
            .clickable { onTap() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(colors.accent)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "GRANT NOTIFICATION ACCESS",
                color = colors.accent,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "TAP TO OPEN SETTINGS · REQUIRED FOR LIVE NOTIFICATIONS",
                color = colors.ink.copy(alpha = 0.6f),
                fontFamily = JetBrainsMono,
                fontSize = 9.sp
            )
        }
        Text(
            text = ">",
            color = colors.accent,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun RegrantExplainerTile(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppTheme.current
    val pulse = rememberInfiniteTransition(label = "regrantPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "regrantBorder"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, colors.accent.copy(alpha = pulseAlpha))
            .background(colors.tile)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colors.accent)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "NOTIFICATIONS NEED RE-ENABLING",
                color = colors.accent,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = "Vanta was just updated. Android requires you to re-grant notification access for the Live page to work. This is a one-time step.",
            color = colors.ink.copy(alpha = 0.8f),
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, colors.accent)
                    .clickable { onOpenSettings() }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OPEN SETTINGS",
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
            }
            Box(
                modifier = Modifier
                    .border(1.dp, colors.ink.copy(alpha = 0.3f))
                    .clickable { onDismiss() }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GOT IT",
                    color = colors.ink.copy(alpha = 0.6f),
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    var menuExpanded by remember { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "cardPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = if (isActionable && !isMuted) 0.3f else 0.1f,
        targetValue = if (isActionable && !isMuted) 1f else 0.3f,
        animationSpec = infiniteRepeatable(tween(if (isActionable && !isMuted) 800 else 1500), RepeatMode.Reverse),
        label = "borderPulse"
    )

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isActionable && !isMuted) 2.dp else 1.dp,
                    color = when {
                        isMuted -> colors.ink.copy(alpha = 0.2f)
                        isActionable -> colors.accent.copy(alpha = pulseAlpha)
                        else -> colors.ink.copy(alpha = 0.3f)
                    }
                )
                .background(colors.tile.copy(alpha = 0.9f))
                .combinedClickable(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpen()
                    },
                    onLongClick = { menuExpanded = true }
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount ->
                            if (amount > 100f) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                            }
                        }
                    )
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = notif.packageName, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notif.appName.uppercase(),
                        color = if (isMuted) colors.ink.copy(alpha = 0.4f) else colors.ink.copy(alpha = 0.6f),
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp
                    )
                    if (isMuted) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "MUTED",
                            color = colors.ink.copy(alpha = 0.4f),
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }
                Text(
                    text = notif.title,
                    color = if (isMuted) colors.ink.copy(alpha = 0.4f) else colors.ink,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                if (notif.text.isNotBlank()) {
                    Text(
                        text = notif.text,
                        color = if (isMuted) colors.ink.copy(alpha = 0.4f) else colors.ink.copy(alpha = 0.7f),
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }
                Text(
                    text = timeAgo(notif.timestamp),
                    color = colors.ink.copy(alpha = 0.4f),
                    fontFamily = JetBrainsMono,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (isActionable && !isMuted) {
                Text(
                    text = ">",
                    color = colors.accent,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("OPEN APP") },
                onClick = {
                    menuExpanded = false
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpen()
                }
            )
            DropdownMenuItem(
                text = { Text("APP NOTIFICATION SETTINGS") },
                onClick = {
                    menuExpanded = false
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    openAppNotificationSettings(context, notif.packageName)
                }
            )
            DropdownMenuItem(
                text = { Text("MANAGE NOTIFICATIONS") },
                onClick = {
                    menuExpanded = false
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    openAppNotificationSettings(context, notif.packageName)
                }
            )
            DropdownMenuItem(
                text = { Text(if (isMuted) "UNMUTE APP" else "MUTE APP") },
                onClick = {
                    menuExpanded = false
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleMute()
                }
            )
        }
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
    val colors = LocalAppTheme.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val first = notifs.first()
    var expanded by remember(first.packageName) { mutableStateOf(false) }

    if (notifs.size == 1) {
        NotifCard(
            notif = first,
            isActionable = false,
            isMuted = isMuted,
            onOpen = { onOpen(first) },
            onDismiss = { onDismiss(first) },
            onToggleMute = { onToggleMute(first.packageName) }
        )
    } else {
        val rest = notifs.drop(1)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isMuted) colors.ink.copy(alpha = 0.2f) else colors.ink.copy(alpha = 0.5f))
                    .background(colors.tile)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        launchApp(context, first.packageName)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(packageName = first.packageName, size = 24.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${first.appName} (${notifs.size})",
                    color = if (isMuted) colors.ink.copy(alpha = 0.4f) else colors.ink,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "-${rest.size} LESS" else "+${rest.size} MORE",
                    color = if (isMuted) colors.ink.copy(alpha = 0.4f) else colors.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        expanded = !expanded
                    }
                )
            }
            NotifCard(
                notif = first,
                isActionable = false,
                isMuted = isMuted,
                onOpen = { onOpen(first) },
                onDismiss = { onDismiss(first) },
                onToggleMute = { onToggleMute(first.packageName) }
            )
            if (expanded) {
                rest.forEach { n ->
                    NotifCard(
                        notif = n,
                        isActionable = false,
                        isMuted = isMuted,
                        onOpen = { onOpen(n) },
                        onDismiss = { onDismiss(n) },
                        onToggleMute = { onToggleMute(n.packageName) }
                    )
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

private fun loadActiveNotifications(context: Context): List<NotifItem> {
    return try {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val active = nm.activeNotifications
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

private fun launchApp(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    } catch (e: Exception) { }
}

private fun openNotificationListenerSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) { }
}

private fun loadMutedApps(context: Context): Set<String> {
    val prefs = context.getSharedPreferences("standard_muted_apps", Context.MODE_PRIVATE)
    return prefs.getString("apps", "")?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
}

private fun saveMutedApps(context: Context, apps: Set<String>) {
    val prefs = context.getSharedPreferences("standard_muted_apps", Context.MODE_PRIVATE)
    prefs.edit().putString("apps", apps.joinToString(",")).apply()
}

private fun openAppNotificationSettings(context: Context, packageName: String) {
    try {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) { }
}

private fun resolveAppName(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString().uppercase()
    } catch (e: Exception) {
        packageName.substringAfterLast('.').uppercase()
    }
}