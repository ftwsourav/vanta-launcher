package app.vanta.launcher.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.vanta.launcher.ui.theme.LocalAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** Fraction of a page width the panorama moves per page (WP7 hub feel). */
private const val PanoramaRate = 1f / 3f

/**
 * One wide mono photo behind all four pages, moving at a third of the swipe. The bitmap is decoded
 * once per URI, downsampled to about twice the screen width, desaturated and lightly crushed so the
 * paper tiles and headlines stay readable on top. The translation is read in the draw phase only.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PanoramaBackground(
    uri: String?,
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    if (uri.isNullOrBlank()) return
    val colors = LocalAppTheme.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val screenW = context.resources.displayMetrics.widthPixels
    val image by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { decodePanorama(context, uri, targetWidth = screenW * 2) }
    }
    val bmp = image ?: return
    // Mono print: no saturation, a touch more contrast, then the page colour laid over it.
    val mono = ColorFilter.colorMatrix(
        ColorMatrix().apply {
            setToSaturation(0f)
            timesAssign(ColorMatrix(floatArrayOf(
                1.15f, 0f, 0f, 0f, -16f,
                0f, 1.15f, 0f, 0f, -16f,
                0f, 0f, 1.15f, 0f, -16f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
    )
    val paint = Paint().apply { colorFilter = mono }
    val travel = (pageCount - 1).coerceAtLeast(1)
    Canvas(
        modifier = modifier.graphicsLayer {
            // Move a third of a page per page, so four pages sweep one screen-width of photo.
            translationX = -(pagerState.currentPage + pagerState.currentPageOffsetFraction) * size.width * PanoramaRate
        }
    ) {
        val needW = size.width * (1f + travel * PanoramaRate)
        val scale = max(needW / bmp.width, size.height / bmp.height)
        val dstW = ceil(bmp.width * scale).toInt()
        val dstH = ceil(bmp.height * scale).toInt()
        val top = ((size.height - dstH) / 2f).roundToInt()
        drawIntoCanvas { c ->
            c.drawImageRect(
                image = bmp,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bmp.width, bmp.height),
                dstOffset = IntOffset(0, top),
                dstSize = IntSize(dstW, dstH),
                paint = paint
            )
        }
        // Paper wash so ink headlines keep their contrast.
        drawRect(color = colors.background.copy(alpha = if (colors.isDark) 0.62f else 0.70f), size = Size(dstW.toFloat(), size.height), topLeft = Offset.Zero)
    }
}

private fun decodePanorama(context: Context, uri: String, targetWidth: Int): ImageBitmap? = runCatching {
    val resolver = context.contentResolver
    val parsed = Uri.parse(uri)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
}.getOrNull()
