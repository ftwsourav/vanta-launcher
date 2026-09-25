package app.vanta.launcher.ui.theme

import app.vanta.launcher.data.repo.ThemeRepository
import app.vanta.launcher.domain.model.ThemeConfig
import app.vanta.launcher.domain.model.ThemeId
import app.vanta.launcher.domain.model.ThemeSurface

/**
 * Six themes, each with a light "paper" surface (outlined tiles) and a dark surface.
 * Mono dark is charcoal paper; the colour themes go dark as filled Metro tiles, matching the blue mockup.
 */
class ThemeRepositoryImpl : ThemeRepository {

    private fun paper(ink: Long, onInk: Long, background: Long, tile: Long, accent: Long, onAccent: Long) = ThemeSurface(
        background = background, ink = ink, onInk = onInk, tile = tile, onTile = ink,
        accent = accent, onAccent = onAccent, outline = ink, filledTiles = false
    )

    private fun metro(background: Long, ink: Long, tile: Long, accent: Long, onAccent: Long, outline: Long) = ThemeSurface(
        background = background, ink = ink, onInk = 0xFFFFFFFF, tile = tile, onTile = 0xFFFFFFFF,
        accent = accent, onAccent = onAccent, outline = outline, filledTiles = true
    )

    private val themes: List<ThemeConfig> = listOf(
        ThemeConfig(
            id = ThemeId.MONO, displayName = "Mono",
            light = paper(ink = 0xFF111111, onInk = 0xFFEDE9E0, background = 0xFFE7E3DA, tile = 0xFFECE8DF, accent = 0xFFD93A2B, onAccent = 0xFF111111),
            dark = ThemeSurface(
                background = 0xFF111111, ink = 0xFFEDE9E0, onInk = 0xFF111111, tile = 0xFF181818, onTile = 0xFFEDE9E0,
                accent = 0xFFD93A2B, onAccent = 0xFF111111, outline = 0xFFEDE9E0, filledTiles = false
            )
        ),
        ThemeConfig(
            id = ThemeId.BLUE, displayName = "Blue",
            light = paper(ink = 0xFF0E2A57, onInk = 0xFFEEF3FA, background = 0xFFE6ECF5, tile = 0xFFEDF1F8, accent = 0xFF1D6FE0, onAccent = 0xFFFFFFFF),
            dark = metro(background = 0xFF0A1426, ink = 0xFF12305F, tile = 0xFF1E56B0, accent = 0xFF2F8CFF, onAccent = 0xFFFFFFFF, outline = 0xFF081A3A)
        ),
        ThemeConfig(
            id = ThemeId.RED, displayName = "Red",
            light = paper(ink = 0xFF4A100C, onInk = 0xFFF6EFED, background = 0xFFF1EAE7, tile = 0xFFF6F0EE, accent = 0xFFD93A2B, onAccent = 0xFF111111),
            dark = metro(background = 0xFF1A0A0A, ink = 0xFF5A1410, tile = 0xFFA8261F, accent = 0xFFE8503C, onAccent = 0xFFFFFFFF, outline = 0xFF300C0A)
        ),
        ThemeConfig(
            id = ThemeId.GREEN, displayName = "Green",
            light = paper(ink = 0xFF0F3B20, onInk = 0xFFEEF5EF, background = 0xFFE8EFE9, tile = 0xFFEFF5F0, accent = 0xFF1DA24A, onAccent = 0xFF06130A),
            dark = metro(background = 0xFF07160D, ink = 0xFF0F4A26, tile = 0xFF1A7A3C, accent = 0xFF22C55E, onAccent = 0xFF06130A, outline = 0xFF0A2A17)
        ),
        ThemeConfig(
            id = ThemeId.PURPLE, displayName = "Purple",
            light = paper(ink = 0xFF2B1650, onInk = 0xFFF3EFF9, background = 0xFFEDE9F3, tile = 0xFFF3F0F8, accent = 0xFF7A3AF0, onAccent = 0xFFFFFFFF),
            dark = metro(background = 0xFF120A22, ink = 0xFF3A1E6E, tile = 0xFF5B2FB5, accent = 0xFF9B5CFF, onAccent = 0xFFFFFFFF, outline = 0xFF1F1040)
        ),
        ThemeConfig(
            id = ThemeId.ORANGE, displayName = "Orange",
            light = paper(ink = 0xFF4A2A10, onInk = 0xFFF8F2EB, background = 0xFFF3EDE5, tile = 0xFFF8F3ED, accent = 0xFFF0731A, onAccent = 0xFF111111),
            dark = metro(background = 0xFF1A0F06, ink = 0xFF6B3410, tile = 0xFFB8541E, accent = 0xFFFF8A2A, onAccent = 0xFF1A0F06, outline = 0xFF381D0B)
        )
    )

    private val byId: Map<String, ThemeConfig> = themes.associateBy { it.id }

    override fun themeById(id: String): ThemeConfig = byId[id] ?: byId.getValue(ThemeId.MONO)

    override fun allThemes(): List<ThemeConfig> = themes
}
