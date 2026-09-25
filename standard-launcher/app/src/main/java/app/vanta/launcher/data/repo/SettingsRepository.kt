package app.vanta.launcher.data.repo

import app.vanta.launcher.domain.model.AnimationStyle
import app.vanta.launcher.domain.model.ClockFormat
import app.vanta.launcher.domain.model.HomeModule
import app.vanta.launcher.domain.model.IconStyle
import app.vanta.launcher.domain.model.WeatherUnit
import app.vanta.launcher.domain.model.SettingsState
import app.vanta.launcher.domain.model.WeatherLocation
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
