package app.vanta.launcher.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@Composable
fun StandardTheme(
    colors: AppColors = LocalAppTheme.current,
    @Suppress("UNUSED_PARAMETER") dark: Boolean = colors.isDark,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = standardColorScheme(colors),
        typography = StandardTypography,
        content = content
    )
}

/** Cross-fades every palette colour when the theme or dark mode changes. */
@Composable
fun animateAppColors(target: AppColors, durationMs: Int = 450): AppColors {
    val spec = tween<Color>(durationMs)
    val background by animateColorAsState(target.background, spec, label = "bg")
    val ink by animateColorAsState(target.ink, spec, label = "ink")
    val onInk by animateColorAsState(target.onInk, spec, label = "onInk")
    val tile by animateColorAsState(target.tile, spec, label = "tile")
    val onTile by animateColorAsState(target.onTile, spec, label = "onTile")
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    val onAccent by animateColorAsState(target.onAccent, spec, label = "onAccent")
    val outline by animateColorAsState(target.outline, spec, label = "outline")
    return target.copy(
        background = background,
        ink = ink,
        onInk = onInk,
        tile = tile,
        onTile = onTile,
        accent = accent,
        onAccent = onAccent,
        outline = outline
    )
}
