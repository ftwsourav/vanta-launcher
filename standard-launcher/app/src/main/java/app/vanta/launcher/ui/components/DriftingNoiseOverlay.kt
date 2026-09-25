package app.vanta.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ShaderBrush
import app.vanta.launcher.ui.theme.LocalAppTheme
import java.util.Random

/**
 * Paper grain. A tiling alpha mask of specks, tinted ink on light surfaces and paper on dark
 * ones, slowly drifting so the page never feels like a flat bitmap.
 *
 * @param alpha overrides the theme's texture strength (and ignores the texture toggle).
 * @param tint overrides the speck colour (Glance uses white on black).
 */
@Composable
fun DriftingNoiseOverlay(modifier: Modifier = Modifier, alpha: Float? = null, tint: Color? = null) {
    val colors = LocalAppTheme.current
    if (!colors.useTexture && alpha == null) return

    val strength = alpha ?: colors.textureStrength
    val speck = tint ?: if (colors.isDark) Color.White else Color.Black
    val offset = rememberDriftOffset(enabled = colors.noiseDrift)
    val bitmap = remember { createGrainMask() }
    val shader = remember(bitmap) { BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
    val matrix = remember { Matrix() }
    val brush = remember(shader) { ShaderBrush(shader) }
    val filter = remember(speck) { ColorFilter.tint(speck, BlendMode.SrcIn) }

    Box(
        modifier = modifier.graphicsLayer().drawWithContent {
            drawContent()
            val o = offset.value
            matrix.setTranslate(o, o * 0.37f)
            shader.setLocalMatrix(matrix)
            drawRect(brush = brush, alpha = strength, colorFilter = filter)
        }
    )
}

@Composable
fun rememberDriftOffset(enabled: Boolean = true): Animatable<Float, AnimationVector1D> {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        // ponytail: stepped 5 fps drift instead of a per-vsync tween; visually identical, lets the LTPO panel idle.
        while (true) {
            kotlinx.coroutines.delay(200)
            animatable.snapTo((animatable.value + 1.5f) % GRAIN_SIZE)
        }
    }
    return animatable
}

private const val GRAIN_SIZE = 256

// ponytail: per-pixel grain only; the mockups' low-frequency blotches would need a second, coarser layer.
private fun createGrainMask(): Bitmap {
    val size = GRAIN_SIZE
    val random = Random(0x5EEDL)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(size * size)
    for (i in pixels.indices) {
        val n = random.nextInt(256)
        // Skew toward faint specks with occasional strong ones.
        val a = (n * n) / 255
        pixels[i] = (a shl 24)
    }
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap
}
