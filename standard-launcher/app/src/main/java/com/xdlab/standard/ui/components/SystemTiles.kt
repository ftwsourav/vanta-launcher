package com.xdlab.standard.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.xdlab.standard.domain.model.ContactItem
import com.xdlab.standard.domain.model.MediaInfo
import com.xdlab.standard.ui.theme.LocalAppTheme

/** Button semantics for tiles and controls that use [tilePress] instead of `clickable`. */
fun Modifier.button(label: String, action: (() -> Unit)? = null) = semantics(mergeDescendants = true) {
    role = Role.Button
    contentDescription = label
    if (action != null) onClick(action = { action(); true })
}

private fun android.content.Context.open(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(this, "NOT AVAILABLE", Toast.LENGTH_SHORT).show()
    }
}

/** Flat ink bar instead of a ring: the mockups have no curves. */
@Composable
fun BatteryTile(percent: Int, isCharging: Boolean, modifier: Modifier = Modifier) {
    val accent = LocalAppTheme.current.accent
    Tile(modifier = modifier.fillMaxWidth().height(96.dp)) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                MonoLabel(if (isCharging) "CHARGING //" else "BATTERY //", size = 11.sp, color = c)
                CountUpText(percent, "%", fontSize = 32.sp, color = c)
            }
            Box(Modifier.fillMaxWidth().height(12.dp).border(TileDefaults.Border, c).padding(3.dp)) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                        .background(if (percent <= 15 && !isCharging) accent else c)
                )
            }
        }
    }
}

/**
 * MEDIA module: asks for notification-listener access when missing, hides itself when
 * nothing is playing, otherwise shows the track. Tap opens the playing app.
 */
@Composable
fun MediaModule(nowPlaying: MediaInfo?, onOpenApp: (String) -> Unit, modifier: Modifier = Modifier, onPlayPause: () -> Unit = {}, onNext: () -> Unit = {}, onPrevious: () -> Unit = {}) {
    val context = LocalContext.current
    val check = { NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
    var granted by remember { mutableStateOf(check()) }
    LifecycleResumeEffect(Unit) {
        granted = check()
        onPauseOrDispose { }
    }
    when {
        !granted -> Tile(
            modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).button("Enable notification access") { context.open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            onClick = { context.open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        ) {
            MonoLabel("NOTIFICATION ACCESS // TAP TO ENABLE", size = 11.sp, color = LocalTileColors.current.content)
        }
        nowPlaying == null -> Unit
        else -> Tile(
            modifier = modifier.fillMaxWidth().height(96.dp).button("Now playing ${nowPlaying.title ?: ""}"),
            onClick = { nowPlaying.packageName?.let(onOpenApp) }
        ) {
            val c = LocalTileColors.current.content
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                HeadlineText((nowPlaying.title ?: "UNKNOWN").uppercase(), 22.sp, color = c, maxLines = 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    MonoLabel(nowPlaying.artist ?: "", size = 10.sp, color = c.copy(alpha = 0.8f), maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("\u23EE", color = c, fontSize = 18.sp, modifier = Modifier.clickable { onPrevious() })
                        Text(if (nowPlaying.isPlaying) "\u23F8" else "\u25B6", color = c, fontSize = 18.sp, modifier = Modifier.clickable { onPlayPause() })
                        Text("\u23ED", color = c, fontSize = 18.sp, modifier = Modifier.clickable { onNext() })
                    }
                }
            }
        }
    }
}

/** PEOPLE module: always rendered; asks for READ_CONTACTS on tap, contact tiles dial. */
@Composable
fun PeopleHubTile(
    contacts: List<ContactItem>,
    permissionGranted: Boolean,
    onPermission: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onPermission)
    when {
        !permissionGranted -> Tile(
            modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).button("Enable contacts") { launcher.launch(android.Manifest.permission.READ_CONTACTS) },
            onClick = { launcher.launch(android.Manifest.permission.READ_CONTACTS) }
        ) {
            MonoLabel("CONTACTS // TAP TO ENABLE", size = 11.sp, color = LocalTileColors.current.content)
        }
        contacts.isEmpty() -> Tile(modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            MonoLabel("NO CONTACTS //", size = 11.sp, color = LocalTileColors.current.content)
        }
        else -> Row(
            modifier = modifier.fillMaxWidth().height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
        ) {
            contacts.take(4).forEach { contact ->
                val number = contact.phoneNumber
                Tile(
                    modifier = Modifier.weight(1f).fillMaxHeight().button("Call ${contact.displayName}"),
                    onClick = number?.let { n -> { context.open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n"))) } }
                ) {
                    val c = LocalTileColors.current.content
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        HeadlineText(contact.initials, 26.sp, color = c, maxLines = 1)
                        MonoLabel(contact.displayName.substringBefore(' '), size = 9.sp, color = c.copy(alpha = 0.8f), maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * QUICK SETTINGS module. Give it the whole page Box: it draws a tap-to-dismiss scrim while
 * open and anchors the button bottom-end, inset from the navigation bar.
 */
@Composable
fun QuickSettingsFab(modifier: Modifier = Modifier, onToggleNight: () -> Unit = {}) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, label = "fabRotation")
    BackHandler(enabled = expanded) { expanded = false }

    Box(modifier = modifier) {
        if (expanded) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { expanded = false } })
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
        ) {
            if (expanded) {
                listOf(
                    "WIFI" to { context.open(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)) },
                    "BT" to { context.open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                    "DND" to {
                        // ACTION_ZEN_MODE_SETTINGS is @hide; the action string resolves on stock and OxygenOS.
                        try {
                            context.startActivity(Intent("android.settings.ZEN_MODE_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (e: Exception) {
                            context.open(Intent(Settings.ACTION_SOUND_SETTINGS))
                        }
                    },
                    "NIGHT" to { onToggleNight(); expanded = false }
                ).forEach { (label, action) ->
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .background(colors.ink)
                            .tilePress(onTap = action, tilt = false)
                            .button(label, action)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MonoLabel(label, size = 12.sp, color = colors.onInk, weight = FontWeight.Bold)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .tilePress(onTap = { expanded = !expanded }, tilt = false)
                    .button(if (expanded) "Close quick settings" else "Quick settings") { expanded = !expanded }
                    .graphicsLayer { rotationZ = rotation },
                contentAlignment = Alignment.Center
            ) {
                HeadlineText("+", 32.sp, color = colors.onAccent)
            }
        }
    }
}
