package app.vanta.launcher.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import app.vanta.launcher.ui.theme.LocalAppTheme
import kotlin.math.abs

/** How far in from the screen edge a drag may start to count as an edge swipe. */
internal val EdgeZone = 36.dp
/** Drag distance that completes an edge swipe. */
internal val EdgeThreshold = 88.dp

/**
 * Left-edge pull. The pointer is watched in the Initial pass so a drag that starts at the edge is
 * claimed here (and consumed) before the pager's Main-pass drag detector sees it; drags that start
 * anywhere else are left untouched. A square accent bar grows from the edge as the finger pulls.
 */
@Composable
fun Modifier.edgeSwipeHandler(
    enabled: Boolean,
    onSwipeOpen: () -> Unit,
    onDragProgress: (Float) -> Unit = {}
): Modifier {
    if (!enabled) return this
    val accent = LocalAppTheme.current.accent
    val haptics = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsEnabled.current
    val density = LocalDensity.current
    val slop = LocalViewConfiguration.current.touchSlop
    val edgeWidthPx = with(density) { EdgeZone.toPx() }
    val thresholdPx = with(density) { EdgeThreshold.toPx() }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val currentOpen by rememberUpdatedState(onSwipeOpen)
    val currentProgress by rememberUpdatedState(onDragProgress)

    return this
        .drawBehind { drawEdgePull(dragProgress, accent, fromLeft = true) }
        .pointerInput(edgeWidthPx, thresholdPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.position.x > edgeWidthPx) return@awaitEachGesture
                var travel = 0f
                var claimed = false
                var notched = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    val delta = change.positionChange()
                    travel += delta.x
                    if (!claimed) {
                        // Claim only a rightward drag past slop; a tap or a vertical scroll at the edge stays free.
                        if (travel > slop && abs(delta.y) < abs(delta.x) * 2f) claimed = true
                        else if (travel < -slop || abs(change.position.y - down.position.y) > slop * 2f) return@awaitEachGesture
                    }
                    if (claimed) {
                        change.consume()
                        dragProgress = ((travel - slop) / thresholdPx).coerceIn(0f, 1f)
                        currentProgress(dragProgress)
                        if (dragProgress >= 0.5f && !notched) {
                            if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            notched = true
                        } else if (dragProgress < 0.5f) {
                            notched = false
                        }
                    }
                }
                if (claimed && dragProgress > 0.5f) currentOpen()
                dragProgress = 0f
                currentProgress(0f)
            }
        }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEdgePull(progress: Float, accent: Color, fromLeft: Boolean) {
    if (progress <= 0.01f) return
    val p = progress.coerceIn(0f, 1f)
    val barWidth = 6.dp.toPx()
    val barLeft = if (fromLeft) 0f else size.width - barWidth
    drawRect(color = accent.copy(alpha = 0.35f + 0.65f * p), topLeft = Offset(barLeft, 0f), size = Size(barWidth, size.height))
    val shadeWidth = size.width * 0.35f * p
    if (shadeWidth <= 0f) return
    val start = if (fromLeft) barWidth else size.width - barWidth - shadeWidth
    drawRect(
        brush = Brush.horizontalGradient(
            colors = if (fromLeft) listOf(Color.Black.copy(alpha = 0.22f * p), Color.Transparent)
            else listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f * p)),
            startX = start,
            endX = start + shadeWidth
        ),
        topLeft = Offset(start, 0f),
        size = Size(shadeWidth, size.height)
    )
}
