package com.xdlab.standard.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.xdlab.standard.domain.model.ThemeSurface

@Immutable
data class AppColors(
    val background: Color,
    /** Outlines, solid "ink" tiles and primary text on the page. */
    val ink: Color,
    val onInk: Color,
    /** Fill of a standard tile (paper on light themes, a colour block on Metro themes). */
    val tile: Color,
    val onTile: Color,
    val accent: Color,
    val onAccent: Color,
    /** Tile edge colour on Metro themes; paper themes use [ink]. */
    val outline: Color,
    val filledTiles: Boolean,
    val isDark: Boolean,
    val useTexture: Boolean,
    val textureStrength: Float,
    val noiseDrift: Boolean
) {
    /** Primary text colour on the page background. */
    val text: Color get() = ink

    /** Legacy alias for [tile]. */
    val tileFill: Color get() = tile

    val muted: Color get() = ink.copy(alpha = 0.55f)
}

@Composable
fun AppColors(
    surface: ThemeSurface,
    dark: Boolean,
    useTexture: Boolean,
    textureStrength: Float,
    noiseDrift: Boolean
): AppColors {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE)
    val customAccent = prefs.getLong("custom_accent", 0L)
    val dynamicColor = prefs.getBoolean("dynamic_color", false)

    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        val accent: Color
        val onAccent: Color
        if (customAccent != 0L) {
            accent = Color(customAccent)
            onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
        } else {
            accent = scheme.primary
            onAccent = scheme.onPrimary
        }
        return AppColors(
            background = scheme.background,
            ink = scheme.onBackground,
            onInk = scheme.background,
            tile = scheme.surface,
            onTile = scheme.onSurface,
            accent = accent,
            onAccent = onAccent,
            outline = scheme.outline,
            filledTiles = surface.filledTiles,
            isDark = dark,
            useTexture = useTexture,
            textureStrength = textureStrength,
            noiseDrift = noiseDrift
        )
    }

    val resolvedAccent: Color
    val resolvedOnAccent: Color

    if (customAccent != 0L) {
        resolvedAccent = Color(customAccent)
        resolvedOnAccent = if (resolvedAccent.luminance() > 0.5f) Color.Black else Color.White
    } else {
        resolvedAccent = Color(surface.accent)
        resolvedOnAccent = Color(surface.onAccent)
    }

    return AppColors(
        background = Color(surface.background),
        ink = Color(surface.ink),
        onInk = Color(surface.onInk),
        tile = Color(surface.tile),
        onTile = Color(surface.onTile),
        accent = resolvedAccent,
        onAccent = resolvedOnAccent,
        outline = Color(surface.outline),
        filledTiles = surface.filledTiles,
        isDark = dark,
        useTexture = useTexture,
        textureStrength = textureStrength,
        noiseDrift = noiseDrift
    )
}

val LocalAppTheme = compositionLocalOf<AppColors> {
    error("LocalAppTheme not provided")
}
