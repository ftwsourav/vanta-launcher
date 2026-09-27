package app.vanta.launcher.ui.components

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.vanta.launcher.domain.model.ContactItem
import app.vanta.launcher.domain.model.MediaInfo
import app.vanta.launcher.ui.theme.LocalAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Button semantics for tiles and controls that use [tilePress] instead of `clickable`. */
fun Modifier.button(label: String, action: (() -> Unit)? = null) = semantics(mergeDescendants = true) {
    role = Role.Button
    contentDescription = label
    if (action != null) onClick(action = { action(); true })
}

/** Drawn cross for "+" buttons: two [stroke] bars, no font glyph (the CJK ＋ renders full-width). */
@Composable
fun PlusCross(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp, stroke: Dp = 2.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = stroke.toPx()
        val full = this.size
        drawRect(color, topLeft = Offset((full.width - w) / 2f, 0f), size = Size(w, full.height))
        drawRect(color, topLeft = Offset(0f, (full.height - w) / 2f), size = Size(full.width, w))
    }
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
    val context = LocalContext.current
    val (liveLevel, liveCharging) = rememberBatteryState()
    val frames = batteryFrames(context)
    val displayLevel = if (percent in 0..100) percent else liveLevel
    val displayCharging = if (percent in 0..100) isCharging else liveCharging
    val accent = LocalAppTheme.current.accent
    Tile(modifier = modifier.fillMaxWidth().height(96.dp)) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                MonoLabel(frames[1] + " //", size = 11.sp, color = c)
                HeadlineText(frames[0], 32.sp, color = c)
            }
            Box(Modifier.fillMaxWidth().height(12.dp).border(TileDefaults.Border, c).padding(3.dp)) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((displayLevel / 100f).coerceIn(0f, 1f))
                        .background(if (displayLevel <= 15 && !displayCharging) accent else c)
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

/** PEOPLE module: live rotating contact grid with photos and call/SMS actions. */
@Composable
fun PeopleHubTile(
    contacts: List<ContactItem>,
    permissionGranted: Boolean,
    onPermission: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    widgetSize: Int = 1
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onPermission)
    var pending by remember { mutableStateOf<ContactItem?>(null) }
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
        else -> {
            val groupSize = when (widgetSize) { 0 -> 2; 1 -> 4; else -> 6 }
            val tileHeight = when (widgetSize) { 0 -> 96.dp; 1 -> 120.dp; else -> 144.dp }
            val groups = contacts.chunked(groupSize)
            val onContactTap: (ContactItem) -> Unit = { contact -> pending = contact }
            val frames: List<@Composable () -> Unit> = groups.map { group ->
                { PeopleFrame(group, groupSize, onContactTap) }
            }
            Column(modifier = modifier.fillMaxWidth().animateContentSize()) {
                PeopleHeader(count = contacts.size, rotating = groups.size > 1)
                LiveTile(
                    modifier = Modifier.fillMaxWidth().height(tileHeight),
                    frames = frames,
                    intervalMs = 5000L,
                    flipEnabled = groups.size > 1
                )
            }
        }
    }
    pending?.let { contact ->
        ContactActionDialog(
            contact = contact,
            onCall = {
                contact.phoneNumber?.let { n -> context.open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n"))) }
                pending = null
            },
            onMessage = {
                contact.phoneNumber?.let { n -> context.open(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$n"))) }
                pending = null
            },
            onDismiss = { pending = null }
        )
    }
}

@Composable
private fun PeopleHeader(count: Int, rotating: Boolean) {
    val theme = LocalAppTheme.current
    val pulse = rememberInfiniteTransition(label = "peoplePulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(6.dp).background(theme.accent.copy(alpha = pulseAlpha)))
            MonoLabel("PEOPLE", size = 11.sp, color = theme.ink)
        }
        MonoLabel(if (rotating) "$count //LIVE" else "$count", size = 11.sp, color = theme.ink.copy(alpha = 0.7f))
    }
}

@Composable
private fun PeopleFrame(
    group: List<ContactItem>,
    slots: Int,
    onTap: (ContactItem) -> Unit
) {
    val outline = LocalTileColors.current.outline
    val c = LocalTileColors.current.content
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
    ) {
        val padded = group + List<ContactItem?>((slots - group.size).coerceAtLeast(0)) { null }
        padded.forEach { contact ->
            if (contact != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(TileDefaults.Border, outline)
                        .tilePress(onTap = { onTap(contact) }, tilt = false)
                        .button("Contact ${contact.displayName}") { onTap(contact) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            ContactAvatar(contact)
                        }
                        MonoLabel(
                            contact.displayName.substringBefore(' '),
                            size = 9.sp,
                            color = c.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(TileDefaults.Border, outline.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
private fun ContactAvatar(contact: ContactItem) {
    val c = LocalTileColors.current.content
    val photo = rememberContactPhoto(contact.id)
    if (photo != null) {
        Image(
            painter = BitmapPainter(photo),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        HeadlineText(contact.initials, 26.sp, color = c, maxLines = 1)
    }
}

@Composable
private fun rememberContactPhoto(contactId: String): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, contactId, context) {
        value = withContext(Dispatchers.IO) {
            ContactPhotoCache.getOrLoad(contactId) { loadContactPhoto(context, contactId) }
        }
    }.value
}

private object ContactPhotoCache {
    private val cache = android.util.LruCache<String, ImageBitmap>(32)
    fun getOrLoad(key: String, loader: () -> ImageBitmap?): ImageBitmap? {
        cache.get(key)?.let { return it }
        val loaded = loader() ?: return null
        cache.put(key, loaded)
        return loaded
    }
}

private fun loadContactPhoto(context: android.content.Context, contactId: String): ImageBitmap? {
    return try {
        val contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
        val photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
        context.contentResolver.openInputStream(photoUri)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun ContactActionDialog(
    contact: ContactItem,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.tile)
                .border(TileDefaults.Border, theme.ink)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MonoLabel("CONTACT //", size = 11.sp, color = theme.ink.copy(alpha = 0.7f))
            HeadlineText(contact.displayName.uppercase(), 24.sp, color = theme.ink, maxLines = 1)
            contact.phoneNumber?.let { number ->
                MonoLabel(number, size = 11.sp, color = theme.ink.copy(alpha = 0.85f), maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContactActionChip("CALL", Modifier.weight(1f), onCall)
                ContactActionChip("MESSAGE", Modifier.weight(1f), onMessage)
            }
        }
    }
}

@Composable
private fun ContactActionChip(label: String, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val theme = LocalAppTheme.current
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .border(TileDefaults.Border, theme.ink)
            .tilePress(onTap = onTap, tilt = false)
            .button(label, onTap),
        contentAlignment = Alignment.Center
    ) {
        MonoLabel(label, size = 13.sp, color = theme.ink, weight = FontWeight.Bold)
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
            // 48dp accent square, Metro style; the glyph crossfades between // and ✕.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.accent)
                    .tilePress(onTap = { expanded = !expanded }, tilt = false)
                    .button(if (expanded) "Close quick settings" else "Quick settings") { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = expanded, animationSpec = tween(150, easing = LumiaEasing), label = "fabGlyph") { open ->
                    MonoLabel(if (open) "✕" else "//", size = 16.sp, color = colors.onAccent, weight = FontWeight.Bold)
                }
            }
        }
    }
}
