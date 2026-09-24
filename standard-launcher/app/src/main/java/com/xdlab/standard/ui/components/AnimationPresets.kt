package com.xdlab.standard.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.ui.theme.SpaceGrotesk

val LumiaEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
val SharpEasing = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)
val GlideEasing = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)

val TileSpring = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
val BounceSpring = spring<Float>(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)
val GentleSpring = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)

fun staggerDelay(index: Int, baseDelay: Int = 40, maxDelay: Int = 400): Int =
    minOf(index * baseDelay, maxDelay)

@Composable
fun Modifier.pressScale(pressed: Boolean, scaleDown: Float = 0.92f): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = TileSpring,
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Modifier.tileEntrance(index: Int, visible: Boolean = true): Modifier {
    val delayMs = staggerDelay(index)
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = LumiaEasing, delayMillis = delayMs),
        label = "tileEntranceAlpha"
    )
    val translation by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = tween(durationMillis = 500, easing = LumiaEasing, delayMillis = delayMs),
        label = "tileEntranceTranslation"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translation.dp.toPx()
    }
}

@Composable
fun Modifier.shimmer(active: Boolean): Modifier {
    if (!active) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }
    return this.drawWithCache {
        val width = size.width
        val p = progress.value
        val bandLeft = p * 2f * width - width
        val brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.15f),
                Color.Transparent
            ),
            start = Offset(bandLeft, 0f),
            end = Offset(bandLeft + width, 0f)
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush = brush)
        }
    }
}

@Composable
fun Modifier.pulseGlow(active: Boolean, color: Color, radius: Float = 8f): Modifier {
    if (!active) return this
    val elevation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        elevation.animateTo(
            targetValue = radius,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }
    return this.graphicsLayer {
        shadowElevation = elevation.value
        ambientShadowColor = color
        spotShadowColor = color
    }
}

@Composable
fun CountUpText(
    targetValue: Int,
    suffix: String = "",
    modifier: Modifier = Modifier,
    durationMs: Int = 800,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    color: Color = Color.White
) {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(targetValue) {
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis = durationMs, easing = LumiaEasing)
        )
    }
    val display = animatable.value.toInt()
    Text(
        text = "$display$suffix",
        color = color,
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Black,
        fontSize = fontSize,
        modifier = modifier
    )
}

fun pageSlideTransition(): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(durationMillis = 300, easing = LumiaEasing),
        initialOffsetX = { it / 4 }
    ) + fadeIn(animationSpec = tween(durationMillis = 300, easing = LumiaEasing))

fun pageSlideExitTransition(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(durationMillis = 300, easing = LumiaEasing),
        targetOffsetX = { -it / 4 }
    ) + fadeOut(animationSpec = tween(durationMillis = 300, easing = LumiaEasing))

enum class HapticType {
    TAP,
    LONG_PRESS,
    TEXT_HANDLE_MOVE
}

@Composable
fun rememberHaptic(enabled: Boolean): (HapticType) -> Unit {
    val haptics = LocalHapticFeedback.current
    return { type: HapticType ->
        if (enabled) {
            val mapped = when (type) {
                HapticType.TAP -> HapticFeedbackType.TextHandleMove
                HapticType.LONG_PRESS -> HapticFeedbackType.LongPress
                HapticType.TEXT_HANDLE_MOVE -> HapticFeedbackType.TextHandleMove
            }
            haptics.performHapticFeedback(mapped)
        }
    }
}

@Composable
fun Modifier.scrollFade(scrollState: ScrollState, threshold: Float = 200f): Modifier =
    this.graphicsLayer {
        this.alpha = (1f - scrollState.value / threshold).coerceIn(0f, 1f)
    }

/** Whether tile presses and settings rows should vibrate. Provided from settings at the root. */
val LocalHapticsEnabled = androidx.compose.runtime.compositionLocalOf { true }
