package app.vanta.launcher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import app.vanta.launcher.ui.theme.LocalAppTheme
import kotlin.random.Random

private const val SplashMs = 650f
private const val InMs = 220f
private const val OutStartMs = 430f

/**
 * 650ms boot: ink page with paper grain, the Vanta tile logo arrives on the Lumia curve, then scales
 * 1 -> 1.6 and fades while the page lerps ink -> background so it wipes straight into the home page.
 * Every animated value is read in a draw or graphicsLayer lambda; nothing recomposes per frame.
 */
@Composable
fun CinematicSplash(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val progress = remember { Animatable(0f) }
    val currentDone by rememberUpdatedState(onAnimationComplete)

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = SplashMs.toInt(), easing = LinearEasing))
        currentDone()
    }

    val grains = remember {
        List(220) { index ->
            val r = Random(index.toLong())
            GrainDot(x = r.nextFloat(), y = r.nextFloat(), sizePx = 1f + r.nextFloat() * 2.5f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val t = progress.value * SplashMs
                val out = LumiaEasing.transform(((t - OutStartMs) / (SplashMs - OutStartMs)).coerceIn(0f, 1f))
                drawRect(lerp(colors.ink, colors.background, out))
                val inF = (t / InMs).coerceIn(0f, 1f)
                val grainAlpha = (if (inF < 0.5f) inF * 2f else 1f) * (1f - out) * 0.7f
                if (grainAlpha > 0.01f) {
                    val speck = colors.onInk.copy(alpha = grainAlpha)
                    grains.forEach { g ->
                        drawRect(
                            color = speck,
                            topLeft = Offset(g.x * size.width, g.y * size.height),
                            size = Size(g.sizePx, g.sizePx)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                val t = progress.value * SplashMs
                val inF = LumiaEasing.transform((t / InMs).coerceIn(0f, 1f))
                val out = LumiaEasing.transform(((t - OutStartMs) / (SplashMs - OutStartMs)).coerceIn(0f, 1f))
                val s = 0.86f + 0.14f * inF + 0.6f * out
                scaleX = s
                scaleY = s
                alpha = inF * (1f - out)
            }
        ) {
            VantaLogo(style = VantaLogoStyle.MetroTile, size = 120.dp)
        }
    }
}

private data class GrainDot(
    val x: Float,
    val y: Float,
    val sizePx: Float
)
