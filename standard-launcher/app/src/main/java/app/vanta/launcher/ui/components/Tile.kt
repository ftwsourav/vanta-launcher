package app.vanta.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.data.remote.AppNotifSummary
import app.vanta.launcher.data.remote.StandardMediaListener
import app.vanta.launcher.domain.model.AnimationStyle
import app.vanta.launcher.domain.model.AppItem
import app.vanta.launcher.domain.model.IconStyle
import app.vanta.launcher.domain.model.LiveTileMode
import app.vanta.launcher.ui.theme.AppColors
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.LocalSettings
import app.vanta.launcher.ui.theme.StandardType
import app.vanta.launcher.util.RefreshRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

/** Outline = paper tile with an ink border; Ink = solid ink block; Accent = the one red tile. */
enum class TileStyle { Outline, Ink, Accent }

/**
 * One-shot origin of the tile that was just tapped, so the app window can reveal out of it
 * (WP tile-to-app). Set by [AppTile] right before it launches, consumed by the app repository.
 * ponytail: process-global hand-off; thread it through launch() if a second launcher path appears.
 */
object LaunchOrigin {
    @Volatile private var pending: Pair<android.view.View, android.graphics.Rect>? = null
    fun set(view: android.view.View, rect: android.graphics.Rect) { pending = view to rect }
    fun consume(): Pair<android.view.View, android.graphics.Rect>? = pending.also { pending = null }
}

@Immutable
data class TileColors(val fill: Color, val content: Color, val outline: Color)

/** Colours of the nearest enclosing [Tile]; text inside tiles reads its colour from here. */
val LocalTileColors = compositionLocalOf { TileColors(Color.Transparent, Color(0xFF111111), Color(0xFF111111)) }

fun tileColors(style: TileStyle, colors: AppColors, fillOverride: Color? = null): TileColors {
    val outline = if (colors.filledTiles) colors.outline else colors.ink
    if (fillOverride != null) {
        val content = if (fillOverride.luminance() > 0.5f) Color(0xFF111111) else Color(0xFFF4F1EA)
        return TileColors(fillOverride, content, outline)
    }
    return when (style) {
        TileStyle.Outline -> TileColors(colors.tile, colors.onTile, outline)
        TileStyle.Ink -> TileColors(colors.ink, colors.onInk, outline)
        TileStyle.Accent -> TileColors(colors.accent, colors.onAccent, outline)
    }
}

object TileDefaults {
    val Border = 2.dp
    val Padding = 14.dp
    val Gutter = 10.dp
}

@Composable
fun Tile(
    modifier: Modifier = Modifier,
    style: TileStyle = TileStyle.Outline,
    fill: Color? = null,
    contentPadding: Dp = TileDefaults.Padding,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val tc = tileColors(style, LocalAppTheme.current, fill)
    val press = if (onClick != null || onLongClick != null) {
        Modifier.tilePress(onTap = onClick ?: {}, onLongPress = onLongClick)
    } else {
        Modifier
    }
    CompositionLocalProvider(LocalTileColors provides tc) {
        Box(
            modifier = modifier
                .then(press)
                .background(tc.fill)
                .border(TileDefaults.Border, tc.outline)
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

private val PressSpring = spring<Float>(dampingRatio = 0.75f, stiffness = 700f)

/**
 * Metro press: the tile sinks slightly and tilts toward the finger, with a light tick.
 * Haptics follow [LocalHapticsEnabled].
 */
@Composable
fun Modifier.tilePress(
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    enabled: Boolean = true,
    tilt: Boolean = true
): Modifier {
    val hapticsOn = LocalHapticsEnabled.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }
    val currentTap by rememberUpdatedState(onTap)
    val currentLong by rememberUpdatedState(onLongPress)
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    return this
        .pointerInput(enabled, onLongPress != null) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = { pos ->
                    tiltX = (pos.y / size.height) * 2f - 1f
                    tiltY = (pos.x / size.width) * 2f - 1f
                    if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch { press.animateTo(1f, PressSpring) }
                    tryAwaitRelease()
                    scope.launch { press.animateTo(0f, PressSpring) }
                },
                onTap = { currentTap() },
                onLongPress = if (onLongPress != null) { _ -> currentLong?.invoke() } else null
            )
        }
        .graphicsLayer {
            val p = press.value
            scaleX = 1f - 0.035f * p
            scaleY = 1f - 0.035f * p
            if (tilt) {
                rotationX = -tiltX * 6f * p
                rotationY = tiltY * 6f * p
                cameraDistance = 18f * density
            }
        }
}

@Composable
private fun Modifier.editJiggle(enabled: Boolean): Modifier {
    if (!enabled) return this
    val transition = rememberInfiniteTransition(label = "jiggle")
    val rot by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(150), RepeatMode.Reverse),
        label = "jiggleRot"
    )
    return this.graphicsLayer {
        rotationZ = rot
        scaleX = 0.965f
        scaleY = 0.965f
    }
}

private fun Modifier.resizeCorner(enabled: Boolean, color: Color): Modifier {
    if (!enabled) return this
    return this.drawWithContent {
        drawContent()
        val leg = 22.dp.toPx()
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w, h)
            lineTo(w, h - leg)
            lineTo(w - leg, h)
            close()
        }
        drawPath(path, color)
    }
}

private enum class LiveTileKind { NONE, CLOCK, WEATHER, BATTERY }

private fun detectLiveTileKind(packageName: String, context: Context): LiveTileKind {
    val prefs = context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE)
    val clockPackages = setOf(
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.samsung.android.app.clockpackage"
    )
    val clockPkg = prefs.getString("live_clock_pkg", null)
    val weatherPkg = prefs.getString("live_weather_pkg", null)
    val batteryPkg = prefs.getString("live_battery_pkg", null)
    return when {
        packageName == clockPkg || packageName in clockPackages -> LiveTileKind.CLOCK
        packageName == weatherPkg -> LiveTileKind.WEATHER
        packageName == batteryPkg -> LiveTileKind.BATTERY
        else -> LiveTileKind.NONE
    }
}

/**
 * An app tile: heavy uppercase title top-left, mono caption bottom-left, optional trailing glyph.
 * In edit mode it jiggles and a tap cycles the size.
 */
@Composable
fun AppTile(
    app: AppItem,
    iconStyle: IconStyle,
    animationStyle: AnimationStyle,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    titleSize: TextUnit = 22.sp,
    trailing: String? = null,
    editMode: Boolean = false,
    onCycleSize: () -> Unit = {},
    style: TileStyle = when (app.tileStyle) {
        "ink" -> TileStyle.Ink
        "accent" -> TileStyle.Accent
        "outline" -> TileStyle.Outline
        else -> if (app.isAccent) TileStyle.Accent else TileStyle.Outline
    },
    onLongPress: (() -> Unit)? = null,
    /** Edit mode: cycles Outline -> Ink -> Accent for this tile (wired by Home). */
    onCycleStyle: (() -> Unit)? = null,
    /** Edit mode: tapping the caption opens the caption editor (wired by Home). */
    onEditCaption: (() -> Unit)? = null,
    liveEnabled: Boolean = true,
    liveContent: List<String>? = null
) {
    val colors = LocalAppTheme.current
    val captionText = caption ?: TileCaptions.defaultFor(app.packageName, app.label)
    val flip = remember { Animatable(0f) }
    val turnstileRotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hapticsOn = LocalHapticsEnabled.current
    val haptics = LocalHapticFeedback.current
    // This app's notification summary only, so a tile recomposes when its own count/text changes.
    val summary by remember(app.packageName) {
        StandardMediaListener.summaries.map { it[app.packageName] }.distinctUntilChanged()
    }.collectAsState(initial = StandardMediaListener.summaries.value[app.packageName])
    val notifCount = summary?.count ?: 0
    var menuExpanded by remember { mutableStateOf(false) }
    var shortcuts by remember(app.packageName) { mutableStateOf<List<ShortcutInfo>>(emptyList()) }
    LaunchedEffect(menuExpanded) {
        if (menuExpanded) {
            shortcuts = try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val query = LauncherApps.ShortcutQuery().apply {
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    )
                    setPackage(app.packageName)
                }
                launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
            } catch (e: Exception) {
                Toast.makeText(context, "Shortcuts unavailable", Toast.LENGTH_SHORT).show()
                emptyList()
            }
        }
    }
    val longHandler: (() -> Unit)? = if (editMode) onLongPress else { { menuExpanded = true } }

    // Where this tile sits in the window, handed to the launch so the app reveals out of the tile.
    val rootView = LocalView.current
    var bounds by remember { mutableStateOf<android.graphics.Rect?>(null) }
    val launchFromTile: () -> Unit = {
        bounds?.let { LaunchOrigin.set(rootView, it) }
        onTap()
    }

    val editCaption: (() -> Unit)? = if (editMode && onEditCaption != null) {
        {
            if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onEditCaption()
        }
    } else null
    val front: @Composable () -> Unit = {
        AppTileFace(app, iconStyle, captionText, titleSize, trailing, editCaption)
    }
    val back: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = captionText,
                style = StandardType.headline(18.sp),
                color = LocalTileColors.current.content,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    val tap: () -> Unit = {
        when {
            editMode -> onCycleSize()
            animationStyle == AnimationStyle.TAP_FLIP -> scope.launch {
                flip.animateTo(90f, RefreshRate.Snappy)
                launchFromTile()
                flip.animateTo(180f, RefreshRate.springSpec())
                delay(120)
                flip.snapTo(0f)
            }
            else -> scope.launch {
                turnstileRotation.animateTo(70f, tween(160, easing = TurnstileOutEasing))
                launchFromTile()
                delay(220)
                turnstileRotation.animateTo(0f, tween(240, easing = TurnstileInEasing))
            }
        }
    }

    val liveActive = liveEnabled && !editMode
    // Per-package start delay (0-3s) and period (7-11s) so the grid never moves in unison.
    val staggerDelay = remember(app.packageName) {
        app.packageName.hashCode().absoluteValue.toLong() % 3000L
    }
    val liveIntervalMs = remember(app.packageName) {
        7000L + app.packageName.hashCode().absoluteValue.toLong() % 4001L
    }
    val flipped by remember { derivedStateOf { flip.value > 90f } }
    val liveStarted by produceState(initialValue = false, app.packageName, liveActive) {
        if (liveActive) {
            delay(staggerDelay)
            value = true
        } else {
            value = false
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { c ->
                val pos = c.positionInWindow()
                bounds = android.graphics.Rect(pos.x.toInt(), pos.y.toInt(), (pos.x + c.size.width).toInt(), (pos.y + c.size.height).toInt())
            }
            .editJiggle(editMode)
            .tilePress(onTap = tap, onLongPress = longHandler, tilt = !editMode)
            .graphicsLayer {
                rotationY = flip.value
                cameraDistance = 16f * density
            }
            .graphicsLayer {
                // Launch: the tile swings away around its left edge (WP turnstile out).
                val t = turnstileRotation.value
                transformOrigin = TransformOrigin(0f, 0.5f)
                rotationY = -t
                alpha = 1f - (t / 90f) * 0.6f
                cameraDistance = 14f * density
            }
            .resizeCorner(editMode, colors.accent)
            .semantics(mergeDescendants = true) {
                contentDescription = if (notifCount > 0) "${app.label}, $notifCount notifications" else app.label
                if (editMode) stateDescription = "Edit mode, long press to drag"
            }
    ) {
        val notif = summary
        when {
            flipped -> Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                Tile(modifier = Modifier.fillMaxSize(), style = style, content = back)
            }
            liveActive && liveStarted -> {
                val liveKind = remember(app.packageName, context) { detectLiveTileKind(app.packageName, context) }
                val liveStrings: List<String>? = liveContent ?: when (liveKind) {
                    LiveTileKind.CLOCK -> clockFrames()
                    LiveTileKind.WEATHER -> weatherFrames(context)
                    LiveTileKind.BATTERY -> batteryFrames(context)
                    LiveTileKind.NONE -> null
                }
                val frames: List<@Composable () -> Unit> = when {
                    // WP: while something is waiting, the tile only alternates name <-> notification.
                    notif != null -> listOf(front, { NotifFace(notif, titleSize, colors.accent) })
                    liveStrings != null -> liveStrings.map { text ->
                        @Composable {
                            Box(modifier = Modifier.fillMaxSize()) {
                                FitHeadlineText(
                                    text = text,
                                    maxSize = (titleSize.value * 1.6f).sp,
                                    minSize = 14.sp,
                                    stacked = text.length > 12 && ' ' in text,
                                    color = LocalTileColors.current.content,
                                    modifier = Modifier.align(Alignment.BottomStart)
                                )
                            }
                        }
                    }
                    else -> listOf(
                        front,
                        {
                            // Metro "name" frame: the label as big as the tile allows, never broken mid-word.
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(colors.accent))
                                FitHeadlineText(
                                    text = app.label.uppercase(),
                                    maxSize = (titleSize.value * 1.6f).sp,
                                    minSize = 14.sp,
                                    color = LocalTileColors.current.content,
                                    modifier = Modifier.align(Alignment.BottomStart)
                                )
                            }
                        },
                        {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AppIcon(packageName = app.packageName, size = 56.dp)
                            }
                        }
                    )
                }
                LiveFrames(
                    frames = frames,
                    mode = LocalSettings.current.liveTileMode,
                    intervalMs = liveIntervalMs,
                    modifier = Modifier.fillMaxSize(),
                    style = style
                )
            }
            else -> Tile(modifier = Modifier.fillMaxSize(), style = style, content = front)
        }

        if (editMode && onCycleStyle != null) {
            // Style cycle affordance: a 22dp square in the corner filled with the NEXT style's colour.
            val nextFill = when (style) {
                TileStyle.Outline -> colors.ink
                TileStyle.Ink -> colors.accent
                TileStyle.Accent -> colors.tile
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(22.dp)
                    .background(nextFill)
                    .border(TileDefaults.Border, colors.ink)
                    .pointerInput(onCycleStyle) {
                        detectTapGestures {
                            if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCycleStyle()
                        }
                    }
                    .semantics { contentDescription = "Tile style" }
            )
        }

        if (notifCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .background(colors.accent)
                    .semantics { invisibleToUser() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (notifCount <= 9) notifCount.toString() else "9+",
                    style = StandardType.mono(9.sp, FontWeight.Bold),
                    color = colors.onAccent,
                    textAlign = TextAlign.Center
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            shortcuts.take(4).forEach { shortcut ->
                DropdownMenuItem(
                    text = { Text(shortcut.shortLabel?.toString() ?: "", style = StandardType.mono(13.sp)) },
                    onClick = {
                        menuExpanded = false
                        try {
                            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                            launcherApps.startShortcut(shortcut, null, null)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Shortcut unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("APP INFO", style = StandardType.mono(13.sp)) },
                onClick = {
                    menuExpanded = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", app.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(intent) } catch (e: Exception) {}
                }
            )
            DropdownMenuItem(
                text = { Text("UNINSTALL", style = StandardType.mono(13.sp)) },
                onClick = {
                    menuExpanded = false
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.fromParts("package", app.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(intent) } catch (e: Exception) {}
                }
            )
        }
    }
}

/** WP notification face: count big top-left, newest title and text at the foot, accent square top-right. */
@Composable
private fun NotifFace(summary: AppNotifSummary, titleSize: TextUnit, accent: Color) {
    val c = LocalTileColors.current.content
    val small = titleSize.value <= 22f
    val title = summary.title.ifBlank { summary.text }
    val text = if (summary.title.isBlank()) "" else summary.text
    Box(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(accent))
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            HeadlineText(summary.count.toString(), (titleSize.value * 1.6f).sp, color = c, maxLines = 1)
            Column {
                MonoLabel(title, size = 11.sp, color = c, weight = FontWeight.Bold, maxLines = 1)
                if (!small && text.isNotBlank()) {
                    MonoLabel(text, size = 10.sp, color = c.copy(alpha = 0.85f), maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun AppTileFace(
    app: AppItem,
    iconStyle: IconStyle,
    caption: String,
    titleSize: TextUnit,
    trailing: String?,
    onEditCaption: (() -> Unit)? = null
) {
    val content = LocalTileColors.current.content
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            if (iconStyle == IconStyle.ICON_ONLY) {
                AppIcon(packageName = app.packageName, size = 40.dp)
            } else {
                // Never break a word: multi-word names stack one word per line on small tiles,
                // single words shrink until they fit.
                val label = app.label.uppercase()
                FitHeadlineText(
                    text = label,
                    maxSize = titleSize,
                    minSize = 13.sp,
                    stacked = ' ' in label && titleSize.value <= 32f,
                    color = content,
                    modifier = Modifier.weight(1f)
                )
            }
            if (iconStyle == IconStyle.ICON_TEXT) {
                Spacer(Modifier.width(8.dp))
                AppIcon(packageName = app.packageName, size = 22.dp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = caption.uppercase(),
                style = StandardType.mono(10.sp),
                color = content.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .then(
                        if (onEditCaption != null) {
                            Modifier.pointerInput(onEditCaption) { detectTapGestures { onEditCaption() } }
                        } else Modifier
                    )
            )
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = trailing,
                    style = StandardType.mono(13.sp, FontWeight.Bold),
                    color = content
                )
            }
        }
    }
}

@Composable
fun AppIcon(packageName: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val maskShape = remember(context) {
        val prefs = context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE)
        when (prefs.getString("icon_mask", "round")) {
            "square" -> RoundedCornerShape(12.dp)
            "squircle" -> RoundedCornerShape(20.dp)
            else -> CircleShape
        }
    }
    val state = produceState<IconLoadState>(initialValue = IconLoadState.Loading, packageName, context) {
        value = withContext(Dispatchers.IO) {
            IconCache.getOrLoad(packageName) { loadAppIcon(context, packageName) }
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(maskShape)
            .background(colors.tile)
            .border(1.dp, colors.outline.copy(alpha = 0.30f), maskShape),
        contentAlignment = Alignment.Center
    ) {
        when (val s = state.value) {
            is IconLoadState.Success -> {
                Image(
                    painter = BitmapPainter(s.bitmap),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            is IconLoadState.Failed -> {
                val letter = s.label.firstOrNull()?.uppercase() ?: "?"
                Text(
                    text = letter,
                    style = StandardType.mono((size.value * 0.5f).sp, FontWeight.Bold),
                    color = colors.onTile
                )
            }
            IconLoadState.Loading -> Unit
        }
    }
}

private sealed interface IconLoadState {
    object Loading : IconLoadState
    data class Success(val bitmap: ImageBitmap) : IconLoadState
    data class Failed(val label: String) : IconLoadState
}

private object IconCache {
    private val cache = android.util.LruCache<String, IconLoadState>(64)
    fun getOrLoad(key: String, loader: () -> IconLoadState): IconLoadState {
        cache.get(key)?.let { return it }
        val loaded = loader()
        cache.put(key, loaded)
        return loaded
    }
}

private fun loadAppIcon(context: Context, packageName: String): IconLoadState {
    return try {
        val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
        IconLoadState.Success(drawableToBitmap(drawable, 144).asImageBitmap())
    } catch (e: Exception) {
        IconLoadState.Failed(resolveAppLabel(context, packageName))
    }
}

private fun resolveAppLabel(context: Context, packageName: String): String {
    return try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (e: Exception) {
        packageName
    }
}

private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

/**
 * The one frame switcher behind every live tile. FLIP: the tile turns 180° on Y with the next frame
 * riding the mirrored back face, snapping to 0 as it lands. PEEK (WP8): the current face slides up
 * and out while the next slides in from below, 420ms Lumia, clipped inside the border, then rests.
 * Frames only change once a move has landed, so nothing recomposes per animation frame.
 */
@Composable
fun LiveFrames(
    frames: List<@Composable () -> Unit>,
    mode: LiveTileMode = LocalSettings.current.liveTileMode,
    intervalMs: Long = 7000L,
    modifier: Modifier = Modifier,
    style: TileStyle = TileStyle.Outline
) {
    if (frames.isEmpty()) return
    val count = frames.size
    var frameIndex by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(count, intervalMs, mode) {
        progress.snapTo(0f)
        if (count <= 1) return@LaunchedEffect
        while (true) {
            delay(intervalMs)
            if (mode == LiveTileMode.PEEK) {
                progress.animateTo(1f, tween(420, easing = LumiaEasing))
            } else {
                progress.animateTo(1f, RefreshRate.springSpec())
            }
            frameIndex = (frameIndex + 1) % count
            progress.snapTo(0f)
        }
    }

    val current = frameIndex % count
    val next = (current + 1) % count
    if (mode == LiveTileMode.PEEK) {
        val moving by remember { derivedStateOf { progress.value > 0f } }
        val inner = TileDefaults.Padding - TileDefaults.Border
        Tile(modifier = modifier, style = style, contentPadding = TileDefaults.Border) {
            Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = -size.height * progress.value }
                        .padding(inner)
                ) { frames[current]() }
                if (moving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationY = size.height * (1f - progress.value) }
                            .padding(inner)
                    ) { frames[next]() }
                }
            }
        }
    } else {
        val showBack by remember { derivedStateOf { progress.value > 0.5f } }
        Box(
            modifier = modifier.graphicsLayer {
                rotationY = 180f * progress.value
                cameraDistance = 16f * density
            }
        ) {
            if (showBack) {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                    Tile(modifier = Modifier.fillMaxSize(), style = style) { frames[next]() }
                }
            } else {
                Tile(modifier = Modifier.fillMaxSize(), style = style) { frames[current]() }
            }
        }
    }
}

/** Bumped by the app shell every time the launcher comes back to the foreground; tiles turnstile in again. */
val LocalEntranceTick = staticCompositionLocalOf { 0 }

/** WP turnstile-out: fast at first, settles as the tile leaves. */
val TurnstileOutEasing: Easing = CubicBezierEasing(0.4f, 0f, 0.9f, 0.5f)
/** WP turnstile-in: the Lumia curve, quick arrival with a long soft settle. */
val TurnstileInEasing: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

private const val EntranceStaggerMs = 32L
private const val EntranceCapMs = 320L

/**
 * Windows Phone turnstile entrance: each tile swings in around the screen's left edge, staggered
 * by [index], on first composition and again whenever [LocalEntranceTick] changes (the launcher
 * coming back to the foreground). The transform is read in the draw phase only.
 */
@Composable
fun TileEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tick = LocalEntranceTick.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(tick) {
        progress.snapTo(0f)
        delay((index * EntranceStaggerMs).coerceAtMost(EntranceCapMs))
        progress.animateTo(1f, tween(durationMillis = 420, easing = TurnstileInEasing))
    }
    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            if (p < 1f) {
                transformOrigin = TransformOrigin(0f, 0.5f)
                rotationY = 55f * (1f - p)
                translationX = size.width * 0.12f * (1f - p)
                alpha = p
                cameraDistance = 14f * density
            }
        }
    ) {
        content()
    }
}
