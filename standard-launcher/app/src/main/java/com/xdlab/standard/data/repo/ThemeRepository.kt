package com.xdlab.standard.data.repo

import com.xdlab.standard.domain.model.ThemeConfig

interface ThemeRepository {
    fun themeById(id: String): ThemeConfig
    fun allThemes(): List<ThemeConfig>
}
