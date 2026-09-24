package com.xdlab.standard.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.domain.model.AnimationStyle
import com.xdlab.standard.domain.model.AppItem
import com.xdlab.standard.domain.model.IconStyle
import com.xdlab.standard.ui.theme.AppColors
import com.xdlab.standard.ui.theme.LocalAppTheme
import com.xdlab.standard.ui.theme.StandardType
import com.xdlab.standard.util.RefreshRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

/** Outline = paper tile with an ink border; Ink = solid ink block; Accent = the one red tile. */
enum class TileStyle { Outline, Ink, Accent }

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
    style: TileStyle = if (app.isAccent) TileStyle.Accent else TileStyle.Outline,
    onLongPress: (() -> Unit)? = null,
    liveEnabled: Boolean = true
) {
    val colors = LocalAppTheme.current
    val captionText = caption ?: TileCaptions.defaultFor(app.packageName, app.label)
    val flip = remember { Animatable(0f) }
    val turnstileRotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val front: @Composable () -> Unit = {
        AppTileFace(app, iconStyle, captionText, titleSize, trailing)
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
                onTap()
                flip.animateTo(180f, RefreshRate.springSpec())
                delay(120)
                flip.snapTo(0f)
            }
            else -> scope.launch {
                turnstileRotation.animateTo(90f, spring(dampingRatio = 0.7f, stiffness = 800f))
                onTap()
                delay(100)
                turnstileRotation.snapTo(0f)
            }
        }
    }

    val liveActive = liveEnabled && !editMode
    val staggerDelay = remember(app.packageName) {
        app.packageName.hashCode().absoluteValue.toLong() % 3000L
    }
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
            .editJiggle(editMode)
            .tilePress(onTap = tap, onLongPress = onLongPress, tilt = !editMode)
            .graphicsLayer {
                rotationY = flip.value
                cameraDistance = 16f * density
            }
            .graphicsLayer {
                rotationY = turnstileRotation.value
                scaleX = 1f + turnstileRotation.value / 180f * 0.2f
            }
            .resizeCorner(editMode, colors.accent)
    ) {
        if (liveActive && liveStarted && flip.value <= 90f) {
            LiveTile(
                modifier = Modifier.fillMaxSize(),
                frames = listOf(
                    front,
                    {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = app.label.uppercase(),
                                style = StandardType.headline((titleSize.value * 1.5f).sp),
                                color = colors.accent,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AppIcon(packageName = app.packageName, size = 56.dp)
                        }
                    }
                ),
                intervalMs = 7000L,
                flipEnabled = true
            )
        } else if (flip.value <= 90f) {
            Tile(modifier = Modifier.fillMaxSize(), style = style, content = front)
        } else {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                Tile(modifier = Modifier.fillMaxSize(), style = style, content = back)
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
    trailing: String?
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
                HeadlineText(
                    text = app.label.uppercase(),
                    size = titleSize,
                    color = content,
                    maxLines = 2,
                    modifier = Modifier.weight(1f, fill = false)
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
                modifier = Modifier.weight(1f, fill = false)
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
fun AppIcon(packageName: String, size: Dp) {
    val context = LocalContext.current
    val bitmapState = produceState<ImageBitmap?>(initialValue = null, packageName, context) {
        value = withContext(Dispatchers.IO) {
            IconCache.getOrLoad(packageName) { loadAppIconBitmap(context, packageName) }
        }
    }
    val bmp = bitmapState.value
    if (bmp != null) {
        Image(
            painter = BitmapPainter(bmp),
            contentDescription = null,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit
        )
    }
}

private object IconCache {
    private val cache = android.util.LruCache<String, ImageBitmap>(64)
    fun getOrLoad(key: String, loader: () -> ImageBitmap?): ImageBitmap? {
        cache.get(key)?.let { return it }
        val loaded = loader() ?: return null
        cache.put(key, loaded)
        return loaded
    }
}

private fun loadAppIconBitmap(context: Context, packageName: String): ImageBitmap? {
    return try {
        val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
        drawableToBitmap(drawable, 144).asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

/** A tile that flips between N content frames on a timer. */
@Composable
fun LiveTileCycling(
    modifier: Modifier = Modifier,
    style: TileStyle = TileStyle.Outline,
    intervalMs: Long = 3500,
    frames: List<@Composable () -> Unit>
) {
    if (frames.isEmpty()) return
    val rotation = remember { Animatable(0f) }
    var frameIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(frames.size, intervalMs) {
        while (true) {
            delay(intervalMs)
            rotation.animateTo(90f, RefreshRate.Snappy)
            frameIndex = (frameIndex + 1) % frames.size
            rotation.snapTo(270f)
            rotation.animateTo(360f, RefreshRate.springSpec())
            rotation.snapTo(0f)
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotation.value
            cameraDistance = 16f * density
        }
    ) {
        Tile(modifier = Modifier.fillMaxSize(), style = style) { frames[frameIndex]() }
    }
}

/** A two-faced tile that flips on a timer. */
@Composable
fun LiveTile(
    modifier: Modifier = Modifier,
    style: TileStyle = TileStyle.Outline,
    intervalMs: Long = 4000,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(intervalMs) {
        while (true) {
            delay(intervalMs)
            rotation.animateTo(180f, RefreshRate.springSpec())
            rotation.snapTo(0f)
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotation.value
            cameraDistance = 16f * density
        }
    ) {
        if (rotation.value <= 90f) {
            Tile(modifier = Modifier.fillMaxSize(), style = style, content = front)
        } else {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                Tile(modifier = Modifier.fillMaxSize(), style = style, content = back)
            }
        }
    }
}

/** Staggered fade + rise on first composition. */
@Composable
fun TileEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val translation = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        delay(index * 40L)
        kotlinx.coroutines.coroutineScope {
            launch { alpha.animateTo(1f, RefreshRate.springSpec()) }
            launch { translation.animateTo(0f, RefreshRate.springSpec()) }
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            this.translationY = translation.value
        }
    ) {
        content()
    }
}
