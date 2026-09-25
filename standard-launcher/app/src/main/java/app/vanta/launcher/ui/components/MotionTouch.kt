package app.vanta.launcher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.vanta.launcher.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch

@Composable
fun Modifier.pressureTilt(enabled: Boolean, tilt: Float = 0.15f): Modifier {
    if (!enabled) return this
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val theme = LocalAppTheme.current
    return this
        .pointerInput(enabled, tilt) {
            detectTapGestures(
                onPress = {
                    press.snapTo(1f)
                    tryAwaitRelease()
                    scope.launch { press.animateTo(0f, animationSpec = TileSpring) }
                }
            )
        }
        .graphicsLayer {
            val p = press.value
            val s = 1f - tilt * 0.8f * p
            scaleX = s
            scaleY = s
            rotationZ = tilt * p * 8f
            ambientShadowColor = theme.accent
        }
}

@Composable
fun Modifier.dragReorder(enabled: Boolean, onMove: (Int) -> Unit): Modifier {
    if (!enabled) return this
    val drag = remember { Animatable(0f) }
    var accumulated by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val theme = LocalAppTheme.current
    return this
        .pointerInput(enabled) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    scope.launch {
                        drag.snapTo(1f)
                        accumulated = 0f
                    }
                },
                onDrag = { change, amount ->
                    change.consume()
                    accumulated += amount.y
                    val slot = 48.dp.toPx()
                    val moved = (accumulated / slot).toInt()
                    if (moved != 0) {
                        onMove(moved)
                        accumulated -= moved * slot
                        scope.launch {
                            drag.animateTo(0f, animationSpec = BounceSpring)
                            drag.snapTo(1f)
                        }
                    }
                },
                onDragEnd = {
                    scope.launch { drag.animateTo(0f, animationSpec = TileSpring) }
                },
                onDragCancel = {
                    scope.launch { drag.animateTo(0f, animationSpec = TileSpring) }
                }
            )
        }
        .graphicsLayer {
            val d = drag.value
            val lift = 1f + 0.10f * d
            scaleX = lift
            scaleY = lift
            translationY = 0f
            shadowElevation = 12f * d
            ambientShadowColor = theme.accent
            spotShadowColor = theme.ink
        }
}

@Composable
fun Modifier.springRipple(enabled: Boolean, ripple: Float = 1f): Modifier {
    if (!enabled) return this
    val progress = remember { Animatable(0f) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val theme = LocalAppTheme.current
    return this
        .pointerInput(enabled, ripple) {
            detectTapGestures(
                onPress = {
                    origin = it
                    tryAwaitRelease()
                    scope.launch {
                        progress.animateTo(
                            1f,
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                        )
                    }
                }
            )
        }
        .drawBehind {
            val p = progress.value
            val maxR = maxOf(size.width, size.height) * ripple
            val r = (p * maxR).coerceAtLeast(0.001f)
            if (p > 0.001f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.accent.copy(alpha = 0.45f * (1f - p)),
                            theme.ink.copy(alpha = 0.20f * (1f - p)),
                            theme.tileFill.copy(alpha = 0.0f)
                        ),
                    center = origin,
                    radius = r
                )
                )
            }
        }
}

@Composable
fun Modifier.silkyLift(lift: Float = 0.12f): Modifier {
    val theme = LocalAppTheme.current
    return this.graphicsLayer {
        val l = 1f + lift
        translationY = -lift * 2f
        scaleX = l
        scaleY = l
        shadowElevation = 10f * lift
        ambientShadowColor = theme.accent
        spotShadowColor = theme.ink
    }
}

class ReorderState(
    val isDragging: State<Boolean>,
    val dragOffset: State<Float>
) {
    val dragging: Boolean get() = isDragging.value
    val offset: Float get() = dragOffset.value
}

@Composable
fun rememberReorderState(): ReorderState {
    val isDragging = remember { mutableStateOf(false) }
    val dragOffset = remember { mutableFloatStateOf(0f) }
    return remember(isDragging, dragOffset) {
        ReorderState(isDragging, dragOffset)
    }
}