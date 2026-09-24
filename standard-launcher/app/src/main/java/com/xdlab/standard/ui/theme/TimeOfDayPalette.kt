package com.xdlab.standard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.time.LocalTime
import kotlinx.coroutines.delay

data class TimeOfDayPalette(
    val background: Color,
    val tileFill: Color,
    val accent: Color,
    val name: String
)

val DAWN = TimeOfDayPalette(
    background = Color(0xFFF5F0E8),
    tileFill = Color(0xFFE8DCC8),
    accent = Color(0xFFD97706),
    name = "DAWN"
)

val DAY = TimeOfDayPalette(
    background = Color(0xFFFAFAFA),
    tileFill = Color(0xFFE5E5E5),
    accent = Color(0xFF000000),
    name = "DAY"
)

val DUSK = TimeOfDayPalette(
    background = Color(0xFF2D2418),
    tileFill = Color(0xFF3D3020),
    accent = Color(0xFFE67E22),
    name = "DUSK"
)

val NIGHT = TimeOfDayPalette(
    background = Color(0xFF0F1419),
    tileFill = Color(0xFF1A1F28),
    accent = Color(0xFF60A5FA),
    name = "NIGHT"
)

val MIDNIGHT = TimeOfDayPalette(
    background = Color(0xFF0A0A0A),
    tileFill = Color(0xFF141414),
    accent = Color(0xFF374151),
    name = "MIDNIGHT"
)

fun paletteForHour(hour: Int): TimeOfDayPalette {
    val h = ((hour % 24) + 24) % 24
    return when (h) {
        in 4..6 -> DAWN
        in 7..16 -> DAY
        in 17..19 -> DUSK
        in 20..22 -> NIGHT
        else -> MIDNIGHT
    }
}

fun lerpPalette(a: TimeOfDayPalette, b: TimeOfDayPalette, t: Float): TimeOfDayPalette {
    val clamped = t.coerceIn(0f, 1f)
    return TimeOfDayPalette(
        background = lerp(a.background, b.background, clamped),
        tileFill = lerp(a.tileFill, b.tileFill, clamped),
        accent = lerp(a.accent, b.accent, clamped),
        name = if (clamped < 0.5f) a.name else b.name
    )
}

@Composable
fun rememberTimeOfDayPalette(enabled: Boolean): TimeOfDayPalette {
    var palette by remember {
        val now = LocalTime.now()
        val current = paletteForHour(now.hour)
        val next = paletteForHour((now.hour + 1) % 24)
        val t = now.minute / 60f
        mutableStateOf(lerpPalette(current, next, t))
    }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            val now = LocalTime.now()
            val current = paletteForHour(now.hour)
            val next = paletteForHour((now.hour + 1) % 24)
            val t = now.minute / 60f
            palette = lerpPalette(current, next, t)
            delay(60000)
        }
    }
    return if (enabled) palette else DAY
}

fun darkPaletteForHour(hour: Int): TimeOfDayPalette {
    val h = ((hour % 24) + 24) % 24
    return when (h) {
        in 17..19 -> DUSK
        in 20..22 -> NIGHT
        else -> MIDNIGHT
    }
}
