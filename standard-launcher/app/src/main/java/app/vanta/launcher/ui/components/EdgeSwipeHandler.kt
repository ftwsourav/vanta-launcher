package app.vanta.launcher.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.vanta.launcher.ui.theme.LocalAppTheme

@Composable
fun Modifier.edgeSwipeHandler(
    enabled: Boolean,
    onSwipeOpen: () -> Unit,
    onDragProgress: (Float) -> Unit = {}
): Modifier {
    if (!enabled) return this
    val accent = LocalAppTheme.current.accent
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val edgeWidthPx = with(density) { 24.dp.toPx() }
    val thresholdPx = with(density) { 80.dp.toPx() }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var startedFromEdge by remember { mutableStateOf(false) }
    var notchFired by remember { mutableStateOf(false) }

    return this
        .drawBehind {
            if (dragProgress > 0.01f) {
                val barWidth = 6.dp.toPx()
                val progress = dragProgress.coerceIn(0f, 1f)
                drawRoundRect(
                    color = accent.copy(alpha = progress),
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
                val shadowWidth = (size.width - barWidth) * progress
                if (shadowWidth > 0f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.35f * progress), Color.Transparent),
                            startX = barWidth,
                            endX = barWidth + shadowWidth
                        ),
                        topLeft = Offset(barWidth, 0f),
                        size = Size(shadowWidth, size.height)
                    )
                }
            }
        }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { offset ->
                    startedFromEdge = offset.x <= edgeWidthPx
                },
                onDragEnd = {
                    if (startedFromEdge && dragProgress > 0.5f) {
                        onSwipeOpen()
                    }
                    dragProgress = 0f
                    startedFromEdge = false
                    notchFired = false
                    onDragProgress(0f)
                },
                onDragCancel = {
                    dragProgress = 0f
                    startedFromEdge = false
                    notchFired = false
                    onDragProgress(0f)
                },
                onHorizontalDrag = { _, dragAmount ->
                    if (startedFromEdge) {
                        dragProgress = (dragProgress + dragAmount / thresholdPx).coerceIn(0f, 1f)
                        onDragProgress(dragProgress)
                        if (dragProgress >= 0.5f && !notchFired) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            notchFired = true
                        } else if (dragProgress < 0.5f) {
                            notchFired = false
                        }
                    }
                }
            )
        }
}
