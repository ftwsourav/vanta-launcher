package com.xdlab.standard.domain.model

data class SettingsState(
    val themeId: String = ThemeId.MONO,
    val animationStyle: AnimationStyle = AnimationStyle.CUBE,
    val iconStyle: IconStyle = IconStyle.TEXT_ONLY,
    val hapticsEnabled: Boolean = true,
    val darkMode: DarkMode = DarkMode.AUTO_SYSTEM,
    val refreshRateMode: RefreshRateMode = RefreshRateMode.AUTO,
    val useTexture: Boolean = true,
    val textureStrength: Float = 0.10f,
    val noiseDrift: Boolean = true,
    val silkyPager: Boolean = true,
    val slideableHome: Boolean = true,
    val motionTouch: Boolean = true,
    val timeOfDayTint: Boolean = false,
    val quotes: List<String> = DefaultQuotes,
    val homeModules: Set<HomeModule> = setOf(HomeModule.BATTERY, HomeModule.MEDIA, HomeModule.PEOPLE, HomeModule.QUICK_SETTINGS),
    val weatherUnit: WeatherUnit = WeatherUnit.CELSIUS,
    val clockFormat: ClockFormat = ClockFormat.AUTO,
    val cinematicIntro: Boolean = true,
    val glanceEnabled: Boolean = true,
    val weatherLocation: WeatherLocation = WeatherLocation("London", 51.5074, -0.1278),
    val accentApps: List<String> = emptyList(),
    val accentMode: String = "manual",
    val customAccent: Long = 0L
)

object ThemeId {
    const val MONO = "mono"
    const val BLUE = "blue"
    const val RED = "red"
    const val GREEN = "green"
    const val PURPLE = "purple"
    const val ORANGE = "orange"
    val ALL = listOf(MONO, BLUE, RED, GREEN, PURPLE, ORANGE)
}

enum class DarkMode { AUTO_SYSTEM, AUTO_TIME, LIGHT, DARK }

enum class RefreshRateMode { AUTO, HZ60, HZ90, HZ120, MAX }
