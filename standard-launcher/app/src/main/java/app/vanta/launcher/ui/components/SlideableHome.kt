package app.vanta.launcher.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.vanta.launcher.ui.theme.LocalAppTheme

class SlideableState(val animated: State<Float>) {
    val offset: Float get() = animated.value
    val isPeeked: Boolean get() = offset < -0.5f
}

@Composable
fun SlideableHome(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalAppTheme.current
    val maxSlideUp = 120.dp
    val maxSlideDown = 60.dp
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedState = animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "slideableHomeOffset"
    )
    val state = remember { SlideableState(animatedState) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val upPx = with(density) { maxSlideUp.toPx() }
    val downPx = with(density) { maxSlideDown.toPx() }
    val gripGrow = (state.offset / downPx).coerceIn(0f, 1f)
    val parallax = (state.offset / upPx).coerceIn(-1f, 1f)

    Box(modifier = modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                dragOffset = (dragOffset + dragAmount).coerceIn(-downPx, upPx)
            },
            onDragEnd = {
                dragOffset = if (dragOffset < 0f) -downPx else 0f
            }
        )
    }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = state.offset
                    scaleY = 1f - parallax * 0.06f
                }
        ) {
            content()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = state.offset * 0.4f
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .drawBehind {
                        val fullWidth = size.width
                        val growWidth = fullWidth * gripGrow
                        drawRoundRect(
                            color = colors.accent,
                            topLeft = Offset((fullWidth - growWidth) / 2f, 0f),
                            size = Size(growWidth, 6.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                    }
            )
        }
    }
}