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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import app.vanta.launcher.ui.theme.LocalAppTheme
import kotlin.math.abs

/**
 * Right-edge pull, the mirror of [edgeSwipeHandler]: watched in the Initial pass so it wins over the
 * pager when the drag starts at the right edge, otherwise inert.
 */
@Composable
fun Modifier.backSwipeHandler(
    enabled: Boolean,
    onSwipeBack: () -> Unit
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
    val currentBack by rememberUpdatedState(onSwipeBack)

    return this
        .drawBehind { drawEdgePull(dragProgress, accent, fromLeft = false) }
        .pointerInput(edgeWidthPx, thresholdPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.position.x < size.width - edgeWidthPx) return@awaitEachGesture
                var travel = 0f
                var claimed = false
                var notched = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    val delta = change.positionChange()
                    travel -= delta.x
                    if (!claimed) {
                        if (travel > slop && abs(delta.y) < abs(delta.x) * 2f) claimed = true
                        else if (travel < -slop || abs(change.position.y - down.position.y) > slop * 2f) return@awaitEachGesture
                    }
                    if (claimed) {
                        change.consume()
                        dragProgress = ((travel - slop) / thresholdPx).coerceIn(0f, 1f)
                        if (dragProgress >= 0.5f && !notched) {
                            if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            notched = true
                        } else if (dragProgress < 0.5f) {
                            notched = false
                        }
                    }
                }
                if (claimed && dragProgress > 0.5f) currentBack()
                dragProgress = 0f
            }
        }
}
