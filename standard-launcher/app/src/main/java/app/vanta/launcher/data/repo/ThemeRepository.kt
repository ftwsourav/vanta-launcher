package app.vanta.launcher.data.repo

import app.vanta.launcher.domain.model.ThemeConfig

interface ThemeRepository {
    fun themeById(id: String): ThemeConfig
    fun allThemes(): List<ThemeConfig>
}
