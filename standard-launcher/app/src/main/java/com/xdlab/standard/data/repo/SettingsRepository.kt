package com.xdlab.standard.data.repo

import com.xdlab.standard.domain.model.AnimationStyle
import com.xdlab.standard.domain.model.ClockFormat
import com.xdlab.standard.domain.model.HomeModule
import com.xdlab.standard.domain.model.IconStyle
import com.xdlab.standard.domain.model.WeatherUnit
import com.xdlab.standard.domain.model.SettingsState
import com.xdlab.standard.domain.model.WeatherLocation
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<SettingsState>
    suspend fun setThemeId(id: String)
    suspend fun setAnimationStyle(style: AnimationStyle)
    suspend fun setIconStyle(style: IconStyle)
    suspend fun setHaptics(enabled: Boolean)
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setUseTexture(enabled: Boolean)
    suspend fun setTextureStrength(strength: Float)
    suspend fun setNoiseDrift(enabled: Boolean)
    suspend fun setTimeOfDayTint(enabled: Boolean)
    suspend fun setQuotes(quotes: List<String>)
    suspend fun setHomeModules(modules: Set<HomeModule>)
    suspend fun setWeatherUnit(unit: WeatherUnit)
    suspend fun setClockFormat(format: ClockFormat)
    suspend fun setCinematicIntro(enabled: Boolean)
    suspend fun setGlanceEnabled(enabled: Boolean)
    suspend fun setWeatherLocation(location: WeatherLocation)
    suspend fun setFirstRunDone()
    suspend fun resetToDefaults()
}
