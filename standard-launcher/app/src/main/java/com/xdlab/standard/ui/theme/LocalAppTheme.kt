package com.xdlab.standard.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
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

fun AppColors(
    surface: ThemeSurface,
    dark: Boolean,
    useTexture: Boolean,
    textureStrength: Float,
    noiseDrift: Boolean
): AppColors = AppColors(
    background = Color(surface.background),
    ink = Color(surface.ink),
    onInk = Color(surface.onInk),
    tile = Color(surface.tile),
    onTile = Color(surface.onTile),
    accent = Color(surface.accent),
    onAccent = Color(surface.onAccent),
    outline = Color(surface.outline),
    filledTiles = surface.filledTiles,
    isDark = dark,
    useTexture = useTexture,
    textureStrength = textureStrength,
    noiseDrift = noiseDrift
)

val LocalAppTheme = compositionLocalOf<AppColors> {
    error("LocalAppTheme not provided")
}
