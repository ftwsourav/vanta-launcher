package com.xdlab.standard.ui.screens.live

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.ui.components.AppIcon
import com.xdlab.standard.ui.components.Tile
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
    var notifications by remember { mutableStateOf<List<NotifItem>>(emptyList()) }
    var dismissed by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        while (true) {
            val loaded = try {
                loadActiveNotifications(context)
            } catch (e: Exception) {
                emptyList()
            }
            notifications = if (loaded.isEmpty()) {
                listOf(
                    NotifItem(
                        packageName = "com.xdlab.standard",
                        appName = "Vanta",
                        title = "WELCOME TO LIVE",
                        text = "Grant notification access to see your notifications here. Tap to open settings.",
                        timestamp = System.currentTimeMillis(),
                        isActionable = true
                    )
                )
            } else {
                loaded
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
                TileEntrance(0) {
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
                    TileEntrance(i + 1) {
                        NotifCard(
                            notif = notif,
                            isActionable = true,
                            onOpen = {
                                dismissed = dismissed + (notif.packageName + notif.timestamp)
                                if (notif.packageName == "com.xdlab.standard") {
                                    try {
                                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    } catch (e: Exception) { }
                                } else {
                                    launchApp(context, notif.packageName)
                                }
                            },
                            onDismiss = {
                                dismissed = dismissed + (notif.packageName + notif.timestamp)
                            }
                        )
                    }
                }
            }

            TileEntrance(actionable.size + 1) {
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
                TileEntrance(actionable.size + i + 2) {
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

@Composable
private fun NotifCard(
    notif: NotifItem,
    isActionable: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppTheme.current
    val pulse = rememberInfiniteTransition(label = "cardPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = if (isActionable) 0.3f else 0.1f,
        targetValue = if (isActionable) 1f else 0.3f,
        animationSpec = infiniteRepeatable(tween(if (isActionable) 800 else 1500), RepeatMode.Reverse),
        label = "borderPulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActionable) 2.dp else 1.dp,
                color = if (isActionable) colors.accent.copy(alpha = pulseAlpha) else colors.ink.copy(alpha = 0.3f)
            )
            .background(colors.tile.copy(alpha = 0.9f))
            .clickable { onOpen() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        if (amount > 100f) onDismiss()
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
        if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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