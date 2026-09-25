package app.vanta.launcher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import app.vanta.launcher.ui.theme.LocalAppTheme
import kotlin.random.Random

@Composable
fun CinematicSplash(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = LinearEasing)
        )
        onAnimationComplete()
    }

    val timeMs = progress.value * 800f
    val phase1Frac = (timeMs / 250f).coerceIn(0f, 1f)
    val phase3Frac = ((timeMs - 550f) / 250f).coerceIn(0f, 1f)

    val sScaleIn by animateFloatAsState(
        targetValue = if (progress.value > 0f) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "sScaleIn"
    )
    val sScale = when {
        timeMs < 250f -> sScaleIn
        timeMs < 550f -> 1f
        else -> 1f + phase3Frac
    }
    val sAlpha = when {
        timeMs < 250f -> phase1Frac
        timeMs < 550f -> 1f
        else -> 1f - phase3Frac
    }

    val bgColor = lerp(Color.Black, colors.tileFill, phase3Frac)

    val grainAlpha = if (phase1Frac <= 0.5f) phase1Frac * 2f else (1f - phase1Frac) * 2f
    val grains = remember {
        List(200) { index ->
            val r = Random(index.toLong())
            GrainDot(
                x = r.nextFloat(),
                y = r.nextFloat(),
                sizePx = 1f + r.nextFloat() * 3f
            )
        }
    }

    val shardStarts = remember {
        List(12) { index ->
            val r = Random(index.toLong() + 100L)
            val x = r.nextFloat() * 1.6f - 0.3f
            val y = if (r.nextBoolean()) -(r.nextFloat() * 0.5f + 0.2f) else (1f + r.nextFloat() * 0.5f + 0.2f)
            Offset(x, y)
        }
    }

    val shardFracs = List(12) { index ->
        val target = if (timeMs >= 250f + 15f * index) 1f else 0f
        animateFloatAsState(
            targetValue = target,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
            label = "shard$index"
        ).value
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            grains.forEach { g ->
                drawCircle(
                    color = Color.White.copy(alpha = grainAlpha),
                    radius = g.sizePx / 2f,
                    center = Offset(g.x * size.width, g.y * size.height)
                )
            }
            val cellW = size.width / 4f
            val cellH = size.height / 3f
            val shardW = 28.dp.toPx()
            val shardH = 10.dp.toPx()
            shardStarts.forEachIndexed { index, start ->
                val frac = shardFracs[index]
                val col = index % 4
                val row = index / 4
                val gridX = col * cellW + cellW / 2f
                val gridY = row * cellH + cellH / 2f
                val startX = start.x * size.width
                val startY = start.y * size.height
                val cx = startX + (gridX - startX) * frac
                val cy = startY + (gridY - startY) * frac
                val scale = 1f + phase3Frac
                val shardAlpha = when {
                    timeMs < 550f -> frac
                    else -> 1f - phase3Frac
                }
                drawRect(
                    color = colors.accent.copy(alpha = shardAlpha.coerceIn(0f, 1f)),
                    topLeft = Offset(cx - shardW * scale / 2f, cy - shardH * scale / 2f),
                    size = Size(shardW * scale, shardH * scale)
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = sAlpha
                    scaleX = sScale
                    scaleY = sScale
                }
        ) {
            val vPath = Path().apply {
                moveTo(size.width * 0.35f, size.height * 0.35f)
                lineTo(size.width * 0.5f, size.height * 0.65f)
                lineTo(size.width * 0.65f, size.height * 0.35f)
                lineTo(size.width * 0.58f, size.height * 0.35f)
                lineTo(size.width * 0.5f, size.height * 0.49f)
                lineTo(size.width * 0.42f, size.height * 0.35f)
                close()
            }
            drawPath(vPath, color = colors.ink, style = Fill)
        }
    }
}

private data class GrainDot(
    val x: Float,
    val y: Float,
    val sizePx: Float
)
