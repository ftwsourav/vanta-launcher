package app.vanta.launcher.domain.model

import kotlinx.serialization.Serializable

/**
 * One surface of a theme. Paper themes outline their tiles in [ink]; Metro themes
 * ([filledTiles]) fill them with [tile] and use [outline] only as a thin edge.
 */
@Serializable
data class ThemeSurface(
    val background: Long,
    val ink: Long,
    val onInk: Long,
    val tile: Long,
    val onTile: Long,
    val accent: Long,
    val onAccent: Long,
    val outline: Long,
    val filledTiles: Boolean
)

@Serializable
data class ThemeConfig(
    val id: String,
    val displayName: String,
    val light: ThemeSurface,
    val dark: ThemeSurface
) {
    fun surface(dark: Boolean): ThemeSurface = if (dark) this.dark else light
}
