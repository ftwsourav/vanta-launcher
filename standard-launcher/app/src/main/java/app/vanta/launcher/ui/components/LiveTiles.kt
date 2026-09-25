package app.vanta.launcher.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.util.RefreshRate
import kotlinx.coroutines.delay

@Composable
fun LiveTile(
    modifier: Modifier = Modifier,
    frames: List<@Composable () -> Unit>,
    intervalMs: Long = 5000L,
    flipEnabled: Boolean = true
) {
    if (frames.isEmpty()) return
    val rotation = remember { Animatable(0f) }
    var frameIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(frames.size, intervalMs, flipEnabled) {
        if (frames.size <= 1) return@LaunchedEffect
        while (true) {
            delay(intervalMs)
            if (flipEnabled) {
                rotation.animateTo(90f, RefreshRate.Snappy)
                frameIndex = (frameIndex + 1) % frames.size
                rotation.animateTo(360f, RefreshRate.springSpec())
                rotation.snapTo(0f)
            } else {
                frameIndex = (frameIndex + 1) % frames.size
            }
        }
    }

    if (flipEnabled && frames.size > 1) {
        Box(
            modifier = modifier.graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 16f * density
            }
        ) {
            val r = rotation.value
            val idx = frameIndex % frames.size
            if (r > 90f && r < 270f) {
                Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                    Tile(modifier = Modifier.fillMaxSize(), style = TileStyle.Outline) {
                        frames[idx]()
                    }
                }
            } else {
                Tile(modifier = Modifier.fillMaxSize(), style = TileStyle.Outline) {
                    frames[idx]()
                }
            }
        }
    } else {
        Tile(modifier = modifier, style = TileStyle.Outline) {
            Crossfade(targetState = frameIndex, animationSpec = tween(500)) { idx ->
                frames[idx % frames.size]()
            }
        }
    }
}

@Composable
fun MusicLiveTile(
    title: String?,
    artist: String?,
    isPlaying: Boolean,
    albumArt: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val frames = listOf<@Composable () -> Unit>(
        {
            val c = LocalTileColors.current.content
            val titleColor = if (albumArt != null) colors.onInk else c
            Box(Modifier.fillMaxSize()) {
                if (albumArt != null) {
                    Image(
                        painter = BitmapPainter(albumArt.asImageBitmap()),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(colors.ink.copy(alpha = 0.45f)))
                }
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    MonoLabel("NOW PLAYING //", size = 11.sp, color = titleColor)
                    HeadlineText((title ?: "â€”").uppercase(), 26.sp, color = titleColor, maxLines = 2)
                }
            }
        },
        {
            val c = LocalTileColors.current.content
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                MonoLabel("ARTIST //", size = 11.sp, color = c.copy(alpha = 0.85f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    HeadlineText(
                        (artist ?: "UNKNOWN").uppercase(),
                        22.sp,
                        color = c,
                        maxLines = 2,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    MonoLabel(
                        if (isPlaying) "\u23F8" else "\u25B6",
                        size = 22.sp,
                        color = c,
                        weight = FontWeight.Bold
                    )
                }
            }
        },
        {
            val c = LocalTileColors.current.content
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                MonoLabel("\u266A", size = 40.sp, color = c, weight = FontWeight.Bold)
                MonoLabel(if (isPlaying) "PLAYING //" else "PAUSED //", size = 11.sp, color = c)
            }
        }
    )
    LiveTile(modifier = modifier, frames = frames, intervalMs = 4000L)
}

@Composable
fun WeatherLiveTile(
    tempC: Int,
    condition: String,
    city: String,
    highC: Int,
    lowC: Int,
    modifier: Modifier = Modifier
) {
    val frames = listOf<@Composable () -> Unit>(
        {
            val c = LocalTileColors.current.content
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                MonoLabel("WEATHER //", size = 11.sp, color = c.copy(alpha = 0.85f))
                HeadlineText("$tempC\u00B0", 56.sp, color = c)
                MonoLabel(condition.uppercase(), size = 12.sp, color = c, maxLines = 1)
            }
        },
        {
            val c = LocalTileColors.current.content
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                MonoLabel("CITY //", size = 11.sp, color = c.copy(alpha = 0.85f))
                HeadlineText(city.uppercase(), 28.sp, color = c, maxLines = 1)
                MonoLabel("H: ${highC}\u00B0  L: ${lowC}\u00B0", size = 12.sp, color = c)
            }
        },
        {
            val c = LocalTileColors.current.content
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeatherGlyph(
                    kind = conditionToKind(condition),
                    modifier = Modifier.size(56.dp),
                    color = c
                )
                HeadlineText("$tempC\u00B0", 40.sp, color = c)
            }
        }
    )
    LiveTile(modifier = modifier, frames = frames, intervalMs = 5000L)
}

@Composable
fun BatteryLiveTile(
    percent: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val frames = listOf<@Composable () -> Unit>(
        {
            val c = LocalTileColors.current.content
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                MonoLabel(if (isCharging) "CHARGING //" else "BATTERY //", size = 11.sp, color = c)
                HeadlineText("$percent%", 48.sp, color = c)
            }
        },
        {
            val c = LocalTileColors.current.content
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                MonoLabel("BATTERY //", size = 11.sp, color = c)
                Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
                    val pct = (percent / 100f).coerceIn(0f, 1f)
                    val sw = 2.dp.toPx()
                    drawRect(color = c, topLeft = Offset.Zero, size = size, style = Stroke(width = sw))
                    drawRect(
                        color = if (percent <= 15 && !isCharging) colors.accent else c,
                        topLeft = Offset(sw, sw),
                        size = Size((size.width - 2 * sw) * pct, size.height - 2 * sw)
                    )
                    if (isCharging) {
                        val bolt = Path().apply {
                            moveTo(size.width * 0.52f, size.height * 0.1f)
                            lineTo(size.width * 0.38f, size.height * 0.55f)
                            lineTo(size.width * 0.49f, size.height * 0.55f)
                            lineTo(size.width * 0.43f, size.height * 0.9f)
                            lineTo(size.width * 0.62f, size.height * 0.4f)
                            lineTo(size.width * 0.5f, size.height * 0.4f)
                            close()
                        }
                        drawPath(bolt, colors.accent)
                    }
                }
                MonoLabel("$percent%", size = 11.sp, color = c.copy(alpha = 0.85f))
            }
        }
    )
    LiveTile(modifier = modifier, frames = frames, intervalMs = 4000L)
}

private fun conditionToKind(condition: String): WeatherKind = when {
    condition.contains("clear", true) || condition.contains("sun", true) -> WeatherKind.SUN
    condition.contains("partly", true) -> WeatherKind.PARTLY
    condition.contains("fog", true) || condition.contains("mist", true) || condition.contains("haze", true) -> WeatherKind.FOG
    condition.contains("rain", true) || condition.contains("drizzle", true) || condition.contains("shower", true) -> WeatherKind.RAIN
    condition.contains("snow", true) || condition.contains("sleet", true) -> WeatherKind.SNOW
    condition.contains("thunder", true) || condition.contains("storm", true) -> WeatherKind.STORM
    else -> WeatherKind.CLOUD
}
