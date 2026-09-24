package com.xdlab.standard.ui.screens.live

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.xdlab.standard.ui.components.AppIcon
import com.xdlab.standard.ui.components.TileEntrance
import com.xdlab.standard.ui.theme.JetBrainsMono
import com.xdlab.standard.ui.theme.LocalAppTheme
import com.xdlab.standard.ui.theme.SpaceGrotesk
import kotlinx.coroutines.delay

data class NotifItem(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isActionable: Boolean
)

@Composable
fun LiveScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val haptics = LocalHapticFeedback.current
    val checkGranted = { NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
    var granted by remember { mutableStateOf(checkGranted()) }
    var notifications by remember { mutableStateOf<List<NotifItem>>(emptyList()) }
    var dismissed by remember { mutableStateOf<Set<String>>(emptySet()) }

    LifecycleResumeEffect(Unit) {
        granted = checkGranted()
        onPauseOrDispose { }
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
            WeatherMini()
        }

        TileEntrance(0) {
            com.xdlab.standard.ui.components.CalendarNextEvent()
        }

        if (!granted) {
            TileEntrance(1) {
                GrantAccessTile { openNotificationListenerSettings(context) }
            }
        } else {
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
                    modifier = Modifier.fillMaxWidth().padding(top = 120.dp),
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
                            text = "NOTHING NEEDS\nYOUR ATTENTION",
                            color = colors.ink.copy(alpha = 0.5f),
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            lineHeight = 24.sp
                        )
                    }
                }
            } else {
                val actionable = visible.filter { it.isActionable }
                val regular = visible.filter { !it.isActionable }

                if (actionable.isNotEmpty()) {
                    TileEntrance(1) {
                        Text(
                            text = "ACTION REQUIRED",
                            color = colors.accent,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    actionable.forEachIndexed { i, notif ->
                        TileEntrance(i + 2) {
                            NotifCard(
                                notif = notif,
                                isActionable = true,
                                onOpen = {
                                    dismissed = dismissed + (notif.packageName + notif.timestamp)
                                    launchApp(context, notif.packageName)
                                },
                                onDismiss = {
                                    dismissed = dismissed + (notif.packageName + notif.timestamp)
                                }
                            )
                        }
                    }
                }

                TileEntrance(actionable.size + 2) {
                    Text(
                        text = "NOTIFICATIONS",
                        color = colors.ink,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
                regular.forEachIndexed { i, notif ->
                    TileEntrance(actionable.size + i + 3) {
                        NotifCard(
                            notif = notif,
                            isActionable = false,
                            onOpen = {
                                dismissed = dismissed + (notif.packageName + notif.timestamp)
                                launchApp(context, notif.packageName)
                            },
                            onDismiss = {
                                dismissed = dismissed + (notif.packageName + notif.timestamp)
                            }
                        )
                    }
                }
            }
        }
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
            text = "→",
            color = colors.accent,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NotifCard(
    notif: NotifItem,
    isActionable: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppTheme.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "cardPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = if (isActionable) 0.3f else 0.1f,
        targetValue = if (isActionable) 1f else 0.3f,
        animationSpec = infiniteRepeatable(tween(if (isActionable) 800 else 1500), RepeatMode.Reverse),
        label = "borderPulse"
    )

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isActionable) 2.dp else 1.dp,
                    color = if (isActionable) colors.accent.copy(alpha = pulseAlpha) else colors.ink.copy(alpha = 0.3f)
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
                Text(
                    text = notif.appName.uppercase(),
                    color = colors.ink.copy(alpha = 0.6f),
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp
                )
                Text(
                    text = notif.title,
                    color = colors.ink,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                if (notif.text.isNotBlank()) {
                    Text(
                        text = notif.text,
                        color = colors.ink.copy(alpha = 0.7f),
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
            if (isActionable) {
                Text(
                    text = "→",
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
                text = { Text("APP SETTINGS") },
                onClick = {
                    menuExpanded = false
                    Toast.makeText(context, "APP SETTINGS", Toast.LENGTH_SHORT).show()
                }
            )
            DropdownMenuItem(
                text = { Text("BLOCK NOTIFICATIONS") },
                onClick = {
                    menuExpanded = false
                    Toast.makeText(context, "BLOCK NOTIFICATIONS", Toast.LENGTH_SHORT).show()
                }
            )
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
