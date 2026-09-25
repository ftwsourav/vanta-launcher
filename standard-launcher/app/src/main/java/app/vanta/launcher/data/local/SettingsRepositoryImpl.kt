package app.vanta.launcher.data.local

import android.content.Context
import android.content.SharedPreferences
import app.vanta.launcher.data.repo.SettingsRepository
import app.vanta.launcher.domain.model.AnimationStyle
import app.vanta.launcher.domain.model.ClockFormat
import app.vanta.launcher.domain.model.DarkMode
import app.vanta.launcher.domain.model.DefaultQuotes
import app.vanta.launcher.domain.model.HomeModule
import app.vanta.launcher.domain.model.IconStyle
import app.vanta.launcher.domain.model.RefreshRateMode
import app.vanta.launcher.domain.model.WeatherUnit
import app.vanta.launcher.domain.model.SettingsState
import app.vanta.launcher.domain.model.WeatherLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    override val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_DARK_MODE || key == KEY_REFRESH_RATE_MODE) {
            _settings.value = loadSettings()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun loadSettings(): SettingsState {
        val defaults = SettingsState()
        val name = prefs.getString(KEY_WEATHER_NAME, defaults.weatherLocation.name) ?: defaults.weatherLocation.name
        val latLon = prefs.getString(KEY_WEATHER_LATLON, "") ?: ""
        val parts = latLon.split(",")
        val lat = parts.getOrNull(0)?.toDoubleOrNull() ?: defaults.weatherLocation.lat
        val lon = parts.getOrNull(1)?.toDoubleOrNull() ?: defaults.weatherLocation.lon
        return SettingsState(
            themeId = prefs.getString(KEY_THEME_ID, defaults.themeId) ?: defaults.themeId,
            animationStyle = AnimationStyle.entries
                .getOrNull(prefs.getInt(KEY_ANIMATION_STYLE, defaults.animationStyle.ordinal))
                ?: defaults.animationStyle,
            iconStyle = IconStyle.entries
                .getOrNull(prefs.getInt(KEY_ICON_STYLE, defaults.iconStyle.ordinal))
                ?: defaults.iconStyle,
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, defaults.hapticsEnabled),
            darkMode = readDarkMode(),
            refreshRateMode = readRefreshRateMode(),
            useTexture = prefs.getBoolean(KEY_USE_TEXTURE, defaults.useTexture),
            textureStrength = prefs.getFloat(KEY_TEXTURE_STRENGTH, defaults.textureStrength),
            noiseDrift = prefs.getBoolean(KEY_NOISE_DRIFT, defaults.noiseDrift),
            silkyPager = prefs.getBoolean(KEY_SILKY_PAGER, defaults.silkyPager),
            slideableHome = prefs.getBoolean(KEY_SLIDEABLE_HOME, defaults.slideableHome),
            motionTouch = prefs.getBoolean(KEY_MOTION_TOUCH, defaults.motionTouch),
            timeOfDayTint = prefs.getBoolean(KEY_TOD_TINT, defaults.timeOfDayTint),
            quotes = prefs.getString(KEY_QUOTES, null)
                ?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() } ?: defaults.quotes,
            homeModules = prefs.getString(KEY_HOME_MODULES, null)
                ?.split(",")?.mapNotNull { n -> HomeModule.entries.firstOrNull { it.name == n.trim() } }?.toSet()
                ?: defaults.homeModules,
            weatherUnit = prefs.getString(KEY_WEATHER_UNIT, null)?.let { n -> WeatherUnit.entries.firstOrNull { it.name == n } }
                ?: defaults.weatherUnit,
            clockFormat = prefs.getString(KEY_CLOCK_FORMAT, null)?.let { n -> ClockFormat.entries.firstOrNull { it.name == n } }
                ?: defaults.clockFormat,
            cinematicIntro = prefs.getBoolean(KEY_CINEMATIC_INTRO, defaults.cinematicIntro),
            glanceEnabled = prefs.getBoolean(KEY_GLANCE_ENABLED, defaults.glanceEnabled),
            weatherLocation = WeatherLocation(name, lat, lon)
        )
    }

    private fun readDarkMode(): DarkMode {
        if (!prefs.contains(KEY_DARK_MODE)) return DarkMode.AUTO_SYSTEM
        val stored = runCatching { prefs.getString(KEY_DARK_MODE, null) }.getOrNull()
        if (stored != null) {
            return runCatching { DarkMode.valueOf(stored) }.getOrNull() ?: DarkMode.AUTO_SYSTEM
        }
        return runCatching { if (prefs.getBoolean(KEY_DARK_MODE, false)) DarkMode.DARK else DarkMode.LIGHT }
            .getOrNull() ?: DarkMode.AUTO_SYSTEM
    }

    private fun readRefreshRateMode(): RefreshRateMode {
        return prefs.getString(KEY_REFRESH_RATE_MODE, null)?.let {
            runCatching { RefreshRateMode.valueOf(it) }.getOrNull()
        } ?: RefreshRateMode.AUTO
    }

    override suspend fun setWeatherUnit(unit: WeatherUnit) {
        prefs.edit().putString(KEY_WEATHER_UNIT, unit.name).apply()
        mutate { it.copy(weatherUnit = unit) }
    }

    override suspend fun setClockFormat(format: ClockFormat) {
        prefs.edit().putString(KEY_CLOCK_FORMAT, format.name).apply()
        mutate { it.copy(clockFormat = format) }
    }

    override suspend fun setCinematicIntro(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CINEMATIC_INTRO, enabled).apply()
        mutate { it.copy(cinematicIntro = enabled) }
    }

    override suspend fun setGlanceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GLANCE_ENABLED, enabled).apply()
        mutate { it.copy(glanceEnabled = enabled) }
    }

    override suspend fun setFirstRunDone() {
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }

    override suspend fun resetToDefaults() {
        prefs.edit().clear().putBoolean(KEY_FIRST_RUN, false).apply()
        _settings.value = loadSettings()
    }

    override suspend fun setQuotes(quotes: List<String>) {
        val cleaned = quotes.map { it.trim() }.filter { it.isNotEmpty() }
        prefs.edit().putString(KEY_QUOTES, cleaned.joinToString("\n")).apply()
        mutate { it.copy(quotes = cleaned.ifEmpty { DefaultQuotes }) }
    }

    override suspend fun setHomeModules(modules: Set<HomeModule>) {
        prefs.edit().putString(KEY_HOME_MODULES, modules.joinToString(",") { it.name }).apply()
        mutate { it.copy(homeModules = modules) }
    }

    private fun mutate(block: (SettingsState) -> SettingsState) {
        _settings.value = block(_settings.value)
    }

    override suspend fun setThemeId(id: String) {
        prefs.edit().putString(KEY_THEME_ID, id).apply()
        mutate { it.copy(themeId = id) }
    }

    override suspend fun setAnimationStyle(style: AnimationStyle) {
        prefs.edit().putInt(KEY_ANIMATION_STYLE, style.ordinal).apply()
        mutate { it.copy(animationStyle = style) }
    }

    override suspend fun setIconStyle(style: IconStyle) {
        prefs.edit().putInt(KEY_ICON_STYLE, style.ordinal).apply()
        mutate { it.copy(iconStyle = style) }
    }

    override suspend fun setHaptics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        mutate { it.copy(hapticsEnabled = enabled) }
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        val mode = if (enabled) DarkMode.DARK else DarkMode.LIGHT
        prefs.edit().putString(KEY_DARK_MODE, mode.name).apply()
        mutate { it.copy(darkMode = mode) }
    }

    suspend fun setRefreshRateMode(mode: RefreshRateMode) {
        prefs.edit().putString(KEY_REFRESH_RATE_MODE, mode.name).apply()
        mutate { it.copy(refreshRateMode = mode) }
    }

    override suspend fun setUseTexture(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_TEXTURE, enabled).apply()
        mutate { it.copy(useTexture = enabled) }
    }

    override suspend fun setTextureStrength(strength: Float) {
        val clamped = strength.coerceIn(0.02f, 0.30f)
        prefs.edit().putFloat(KEY_TEXTURE_STRENGTH, clamped).apply()
        mutate { it.copy(textureStrength = clamped) }
    }

    override suspend fun setNoiseDrift(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOISE_DRIFT, enabled).apply()
        mutate { it.copy(noiseDrift = enabled) }
    }

    override suspend fun setTimeOfDayTint(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TOD_TINT, enabled).apply()
        mutate { it.copy(timeOfDayTint = enabled) }
    }

    suspend fun setSilkyPager(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SILKY_PAGER, enabled).apply()
        mutate { it.copy(silkyPager = enabled) }
    }

    suspend fun setSlideableHome(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SLIDEABLE_HOME, enabled).apply()
        mutate { it.copy(slideableHome = enabled) }
    }

    suspend fun setMotionTouch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MOTION_TOUCH, enabled).apply()
        mutate { it.copy(motionTouch = enabled) }
    }

    override suspend fun setWeatherLocation(location: WeatherLocation) {
        prefs.edit()
            .putString(KEY_WEATHER_NAME, location.name)
            .putString(KEY_WEATHER_LATLON, "${location.lat},${location.lon}")
            .apply()
        mutate { it.copy(weatherLocation = location) }
    }

    private companion object {
        const val PREFS_NAME = "standard_settings"
        const val KEY_THEME_ID = "theme_id"
        const val KEY_ANIMATION_STYLE = "animation_style"
        const val KEY_ICON_STYLE = "icon_style"
        const val KEY_HAPTICS = "haptics_enabled"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_REFRESH_RATE_MODE = "refresh_rate_mode"
        const val KEY_USE_TEXTURE = "use_texture"
        const val KEY_TEXTURE_STRENGTH = "texture_strength"
        const val KEY_NOISE_DRIFT = "noise_drift"
        const val KEY_SILKY_PAGER = "silky_pager"
        const val KEY_SLIDEABLE_HOME = "slideable_home"
        const val KEY_MOTION_TOUCH = "motion_touch"
        const val KEY_TOD_TINT = "tod_tint"
        const val KEY_QUOTES = "quotes"
        const val KEY_HOME_MODULES = "home_modules"
        const val KEY_WEATHER_UNIT = "weather_unit"
        const val KEY_CLOCK_FORMAT = "clock_format"
        const val KEY_CINEMATIC_INTRO = "cinematic_intro"
        const val KEY_GLANCE_ENABLED = "glance_enabled"
        const val KEY_FIRST_RUN = "first_run"
        const val KEY_WEATHER_NAME = "weather_name"
        const val KEY_WEATHER_LATLON = "weather_latlon"
    }
}
