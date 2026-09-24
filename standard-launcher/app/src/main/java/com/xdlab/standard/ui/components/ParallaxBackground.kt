package com.xdlab.standard.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.xdlab.standard.ui.theme.LocalAppTheme

@Composable
fun ParallaxBackground(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    parallaxFactor: Float = 0.5f
) {
    val colors = LocalAppTheme.current
    val scrollFraction = if (scrollState.maxValue > 0) {
        (scrollState.value.toFloat() / scrollState.maxValue).coerceIn(0f, 1f)
    } else {
        0f
    }
    val glowAlpha = 0.08f * (1f - scrollFraction)
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationY = scrollState.value * parallaxFactor }
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(colors.background, colors.tileFill)
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = glowAlpha),
                            colors.ink.copy(alpha = glowAlpha * 0.5f),
                            colors.accent.copy(alpha = 0f)
                        ),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.6f
                    )
                )
            }
    )
}
