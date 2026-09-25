package app.vanta.launcher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.vanta.launcher.util.RefreshRate
import kotlinx.coroutines.launch

@Composable
fun LumiaScroll(
    modifier: Modifier = Modifier,
    parallaxFactor: Float = 0.4f,
    content: @Composable (scrollOffset: Float) -> Unit
) {
    val density = LocalDensity.current
    val maxBouncePx = with(density) { 120.dp.toPx() }
    val scrollState = remember { Animatable(0f) }
    val bounce = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val springSpec = RefreshRate.springSpec()

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && bounce.value > 0f) {
                    val consumed = available.y.coerceAtMost(bounce.value)
                    scope.launch { bounce.snapTo((bounce.value - consumed).coerceAtLeast(0f)) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y != 0f) {
                    val newBounce = (bounce.value + available.y).coerceIn(-maxBouncePx, maxBouncePx)
                    scope.launch { bounce.animateTo(newBounce, springSpec) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (bounce.value != 0f) {
                    bounce.animateTo(0f, springSpec)
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .nestedScroll(connection)
    ) {
        content(scrollState.value)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = bounce.value * 0.5f }
        )
    }
}
