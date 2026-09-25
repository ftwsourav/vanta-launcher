package app.vanta.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.data.remote.ForecastDay
import app.vanta.launcher.domain.model.WeatherData
import app.vanta.launcher.domain.model.display
import app.vanta.launcher.domain.model.displayWind
import app.vanta.launcher.ui.theme.LocalSettings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

enum class WeatherKind { SUN, PARTLY, CLOUD, FOG, RAIN, SNOW, STORM }

fun wmoToKind(code: Int): WeatherKind = when (code) {
    0, 1 -> WeatherKind.SUN
    2 -> WeatherKind.PARTLY
    3 -> WeatherKind.CLOUD
    45, 48 -> WeatherKind.FOG
    in 51..67, in 80..82 -> WeatherKind.RAIN
    in 71..77, 85, 86 -> WeatherKind.SNOW
    in 95..99 -> WeatherKind.STORM
    else -> WeatherKind.CLOUD
}

/** Monochrome weather glyph drawn with strokes so it matches the mockups' black-and-white art. */
@Composable
fun WeatherGlyph(kind: WeatherKind, modifier: Modifier = Modifier, color: Color = LocalTileColors.current.content) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.07f
        when (kind) {
            WeatherKind.SUN -> drawSun(color, strokeWidth, center, size.minDimension * 0.42f)
            WeatherKind.PARTLY -> {
                drawSun(color, strokeWidth, Offset(size.width * 0.35f, size.height * 0.35f), size.minDimension * 0.28f)
                drawCloud(color, Offset(size.width * 0.55f, size.height * 0.62f), size.minDimension * 0.42f)
            }
            WeatherKind.CLOUD -> drawCloud(color, center, size.minDimension * 0.5f)
            WeatherKind.FOG -> {
                drawCloud(color, Offset(center.x, size.height * 0.4f), size.minDimension * 0.42f)
                val y1 = size.height * 0.78f
                val y2 = size.height * 0.92f
                drawLine(color, Offset(size.width * 0.15f, y1), Offset(size.width * 0.85f, y1), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.3f, y2), Offset(size.width * 0.7f, y2), strokeWidth, StrokeCap.Round)
            }
            WeatherKind.RAIN -> {
                drawCloud(color, Offset(center.x, size.height * 0.4f), size.minDimension * 0.42f)
                for (i in 0..2) {
                    val x = size.width * (0.3f + 0.2f * i)
                    drawLine(color, Offset(x, size.height * 0.72f), Offset(x - size.width * 0.06f, size.height * 0.95f), strokeWidth, StrokeCap.Round)
                }
            }
            WeatherKind.SNOW -> {
                drawCloud(color, Offset(center.x, size.height * 0.4f), size.minDimension * 0.42f)
                for (i in 0..2) {
                    drawCircle(color, size.minDimension * 0.05f, Offset(size.width * (0.3f + 0.2f * i), size.height * 0.85f))
                }
            }
            WeatherKind.STORM -> {
                drawCloud(color, Offset(center.x, size.height * 0.38f), size.minDimension * 0.42f)
                val bolt = Path().apply {
                    moveTo(size.width * 0.55f, size.height * 0.62f)
                    lineTo(size.width * 0.42f, size.height * 0.8f)
                    lineTo(size.width * 0.52f, size.height * 0.8f)
                    lineTo(size.width * 0.44f, size.height * 0.98f)
                    lineTo(size.width * 0.62f, size.height * 0.74f)
                    lineTo(size.width * 0.52f, size.height * 0.74f)
                    close()
                }
                drawPath(bolt, color)
            }
        }
    }
}

private fun DrawScope.drawSun(color: Color, strokeWidth: Float, c: Offset, r: Float) {
    drawCircle(color, r * 0.55f, c)
    for (i in 0 until 8) {
        val a = i * (Math.PI / 4).toFloat()
        val inner = r * 0.75f
        drawLine(color, Offset(c.x + cos(a) * inner, c.y + sin(a) * inner), Offset(c.x + cos(a) * r, c.y + sin(a) * r), strokeWidth, StrokeCap.Round)
    }
}

private fun DrawScope.drawCloud(color: Color, c: Offset, w: Float) {
    val r = w * 0.32f
    drawCircle(color, r, Offset(c.x - w * 0.28f, c.y + r * 0.2f))
    drawCircle(color, r * 1.25f, Offset(c.x + w * 0.02f, c.y - r * 0.3f))
    drawCircle(color, r * 0.95f, Offset(c.x + w * 0.36f, c.y + r * 0.25f))
    drawRect(color, Offset(c.x - w * 0.28f, c.y + r * 0.2f), Size(w * 0.64f, r))
}

/**
 * The mockups' black weather tile: city + LIVE, huge temperature with a glyph, condition,
 * H/L, HUMIDITY, WIND rows and a "// HH:MM" stamp. Tap toggles the 3-day strip and refreshes when stale.
 *
 * @param compact smaller variant for Focus (temperature + condition + optional [quote]).
 */
@Composable
fun WeatherTile(
    weather: WeatherData?,
    isLoading: Boolean,
    error: String?,
    forecast: List<ForecastDay>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    quote: String? = null,
    onRefresh: () -> Unit = {},
    onSetLocation: () -> Unit = {}
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val now = liveClockFormatted()
    val unit = LocalSettings.current.weatherUnit
    val stale = weather == null || (System.currentTimeMillis() / 1000 - weather.lastUpdatedEpoch) > 600
    val updated = weather?.lastUpdatedEpoch?.let {
        Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    Tile(
        modifier = modifier,
        style = TileStyle.Ink,
        onClick = {
            if (weather == null && !isLoading) {
                onSetLocation()
            } else {
                if (!compact) expanded = !expanded
                if (stale) onRefresh()
            }
        }
    ) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                MonoLabel(
                    weather?.location ?: if (isLoading) "LOADING" else "NO LOCATION",
                    size = 12.sp, color = c, maxLines = 1, modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                MonoLabel(
                    when {
                        isLoading -> "SYNC"
                        error != null -> "OFFLINE"
                        weather != null -> "LIVE"
                        else -> "//"
                    },
                    size = 11.sp, color = c.copy(alpha = 0.85f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeadlineText(
                    text = weather?.let { unit.display(it.tempC) } ?: "â€”",
                    size = if (compact) 44.sp else 64.sp,
                    color = c
                )
                if (weather != null) {
                    WeatherGlyph(
                        kind = wmoToKind(weather.conditionCode.toIntOrNull() ?: 3),
                        modifier = Modifier.size(if (compact) 40.dp else 64.dp),
                        color = c
                    )
                }
            }
            MonoLabel(
                weather?.condition ?: if (weather == null && !isLoading) "TAP TO SET LOCATION" else "",
                size = if (compact) 11.sp else 13.sp, color = c, maxLines = 1
            )
            if (!compact && weather != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MonoLabel("H: ${unit.display(weather.highC)}  L: ${unit.display(weather.lowC)}", size = 11.sp, color = c.copy(alpha = 0.9f))
                    MonoLabel("HUMIDITY: ${weather.humidity}%", size = 11.sp, color = c.copy(alpha = 0.9f))
                    MonoLabel("WIND: ${unit.displayWind(weather.windKph)}", size = 11.sp, color = c.copy(alpha = 0.9f))
                }
            }
            if (compact && quote != null) {
                Spacer(Modifier.height(4.dp))
                MonoLabel(quote, size = 10.sp, color = c.copy(alpha = 0.9f), maxLines = 3)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                MonoLabel(
                    if (error != null && updated != null) "UPD $updated" else "",
                    size = 10.sp, color = c.copy(alpha = 0.7f)
                )
                MonoLabel("// $now", size = 11.sp, color = c)
            }
            if (!compact) {
                AnimatedVisibility(visible = expanded && forecast.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        forecast.take(3).forEach { day ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                WeatherGlyph(kind = wmoToKind(day.condition.toWmoGuess()), modifier = Modifier.size(24.dp), color = c)
                                Spacer(Modifier.height(4.dp))
                                MonoLabel("${unit.display(day.highC)}/${unit.display(day.lowC)}", size = 10.sp, color = c)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** ForecastDay carries a condition string; map it back to a kind without touching the data layer. */
private fun String.toWmoGuess(): Int = when {
    contains("thunder", true) -> 95
    contains("snow", true) -> 71
    contains("shower", true) -> 80
    contains("rain", true) || contains("drizzle", true) -> 61
    contains("fog", true) -> 45
    contains("overcast", true) -> 3
    contains("partly", true) -> 2
    contains("clear", true) -> 0
    else -> 3
}
