package com.xdlab.standard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/** Maps the launcher palette onto Material 3 so any Material widget still in use follows the theme. */
fun standardColorScheme(c: AppColors): ColorScheme {
    val base = if (c.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.ink,
        onPrimary = c.onInk,
        secondary = c.accent,
        onSecondary = c.onAccent,
        background = c.background,
        onBackground = c.ink,
        surface = c.background,
        onSurface = c.ink,
        surfaceVariant = c.tile,
        onSurfaceVariant = c.onTile,
        outline = c.outline,
        error = c.accent,
        onError = c.onAccent
    )
}
