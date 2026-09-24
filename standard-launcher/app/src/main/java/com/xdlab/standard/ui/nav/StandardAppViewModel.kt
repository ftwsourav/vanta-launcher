package com.xdlab.standard.ui.nav

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xdlab.standard.data.remote.ForecastDay
import com.xdlab.standard.di.AppContainer
import com.xdlab.standard.domain.model.AnimationStyle
import com.xdlab.standard.domain.model.ClockFormat
import com.xdlab.standard.domain.model.WeatherUnit
import com.xdlab.standard.domain.model.AppItem
import com.xdlab.standard.domain.model.BatteryState
import com.xdlab.standard.domain.model.ContactItem
import com.xdlab.standard.domain.model.HomeModule
import com.xdlab.standard.domain.model.IconStyle
import com.xdlab.standard.domain.model.MediaInfo
import com.xdlab.standard.domain.model.SettingsState
import com.xdlab.standard.domain.model.ThemeConfig
import com.xdlab.standard.domain.model.TileSize
import com.xdlab.standard.domain.model.WeatherData
import com.xdlab.standard.domain.model.WeatherLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class StandardAppViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<SettingsState> = container.settingsRepository.settings
    val pinnedApps: StateFlow<List<AppItem>> = container.appRepository.pinnedApps
    val allApps: StateFlow<List<AppItem>> = container.appRepository.allApps
    val quickTools: StateFlow<List<AppItem>> = container.appRepository.quickTools
    val focusApps: StateFlow<List<AppItem>> = container.appRepository.focusApps
    val weather: StateFlow<WeatherData?> = container.weatherRepository.weather
    val weatherLoading: StateFlow<Boolean> = container.weatherRepository.isLoading
    val weatherError: StateFlow<String?> = container.weatherRepository.error
    val forecast: StateFlow<List<ForecastDay>> = container.weatherRepository.forecast
    val contacts: StateFlow<List<ContactItem>> = container.contactsRepository.contacts
    val contactsPermissionGranted: StateFlow<Boolean> = container.contactsRepository.permissionGranted
    val nowPlaying: StateFlow<MediaInfo?> = container.mediaRepository.nowPlaying
    val albumArt: StateFlow<android.graphics.Bitmap?> = container.mediaRepository.albumArt
    val battery: StateFlow<BatteryState> = container.batteryRepository.battery
    val themes: List<ThemeConfig> = container.themeRepository.allThemes()

    /** True once the cinematic intro has played in this process (survives activity recreation). */
    var splashShown: Boolean = false

    /** Incremented when HOME is pressed while the launcher is already showing. */
    val homeTick = MutableStateFlow(0)

    /** Home edit mode lives here so HOME / back / settings can all leave it. */
    val editMode = MutableStateFlow(false)

    fun onHomePressed() {
        editMode.value = false
        homeTick.value = homeTick.value + 1
    }

    fun setEditMode(enabled: Boolean) {
        editMode.value = enabled
    }

    fun mediaAccessGranted(): Boolean = container.mediaRepository.hasAccess()
    fun refreshMedia() = container.mediaRepository.refresh()
    fun mediaPlayPause() = container.mediaRepository.playPause()
    fun mediaNext() = container.mediaRepository.next()
    fun mediaPrevious() = container.mediaRepository.previous()

    fun setContactsPermission(granted: Boolean) {
        (container.contactsRepository as? com.xdlab.standard.data.local.ContactsRepositoryImpl)
            ?.setPermissionGranted(granted)
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        val ok = container.appRepository.launch(context, packageName)
        if (!ok) {
            Toast.makeText(context, "CAN'T OPEN", Toast.LENGTH_SHORT).show()
            container.appRepository.refresh()
        }
        return ok
    }

    fun setPinned(packageNames: List<String>) {
        viewModelScope.launch { container.appRepository.setPinned(packageNames) }
    }

    fun movePinned(packageName: String, delta: Int) {
        viewModelScope.launch { container.appRepository.movePinned(packageName, delta) }
    }

    fun unpin(packageName: String) {
        viewModelScope.launch { container.appRepository.unpin(packageName) }
    }

    fun togglePin(app: AppItem) {
        val current = pinnedApps.value.map { it.packageName }
        setPinned(if (app.pinned) current - app.packageName else current + app.packageName)
    }

    fun setAccent(packageName: String?) {
        viewModelScope.launch { container.appRepository.setAccent(packageName) }
    }

    private val _accentApps = MutableStateFlow<List<String>>(emptyList())
    private val _accentMode = MutableStateFlow("manual")
    val accentApps: StateFlow<List<String>> = _accentApps
    val accentMode: StateFlow<String> = _accentMode

    private var cachedRandomIndex = 0
    private var cachedRandomTick = -1

    fun setAccentApps(packages: List<String>) {
        val cleaned = packages.filter { it.isNotBlank() }.distinct().take(5)
        _accentApps.value = cleaned
        viewModelScope.launch {
            val active = getActiveAccentApp()
            container.appRepository.setAccent(active)
        }
    }

    fun setAccentMode(mode: String) {
        val normalized = if (mode == "random" || mode == "rotate") mode else "manual"
        _accentMode.value = normalized
        viewModelScope.launch {
            val active = getActiveAccentApp()
            if (active != null) container.appRepository.setAccent(active)
        }
    }

    fun getActiveAccentApp(): String? {
        val apps = _accentApps.value
        val mode = _accentMode.value
        if (apps.isEmpty()) return null
        return when (mode) {
            "random" -> {
                val tick = homeTick.value
                if (tick != cachedRandomTick || cachedRandomIndex >= apps.size) {
                    cachedRandomTick = tick
                    cachedRandomIndex = Random.nextInt(apps.size)
                }
                apps[cachedRandomIndex]
            }
            "rotate" -> {
                val idx = ((System.currentTimeMillis() / 30000L) % apps.size).toInt()
                apps[idx]
            }
            else -> apps.firstOrNull()
        }
    }

    fun setTileSize(packageName: String, size: TileSize) {
        viewModelScope.launch { container.appRepository.setTileSize(packageName, size) }
    }

    fun setCaption(packageName: String, caption: String?) {
        viewModelScope.launch { container.appRepository.setCaption(packageName, caption) }
    }

    fun setQuickTools(packageNames: List<String>) {
        viewModelScope.launch { container.appRepository.setQuickTools(packageNames) }
    }

    fun setFocusApps(packageNames: List<String>) {
        viewModelScope.launch { container.appRepository.setFocusApps(packageNames) }
    }

    fun resetLayout() {
        viewModelScope.launch { container.appRepository.resetLayout() }
    }

    fun refreshWeather() {
        viewModelScope.launch { container.weatherRepository.refresh() }
    }

    fun refreshForLocation(lat: Double, lon: Double, name: String) {
        viewModelScope.launch { container.weatherRepository.refreshForLocation(lat, lon, name) }
    }

    fun setThemeId(id: String) {
        viewModelScope.launch { container.settingsRepository.setThemeId(id) }
    }

    fun setAnimationStyle(style: AnimationStyle) {
        viewModelScope.launch { container.settingsRepository.setAnimationStyle(style) }
    }

    fun setIconStyle(style: IconStyle) {
        viewModelScope.launch { container.settingsRepository.setIconStyle(style) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setHaptics(enabled) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setDarkMode(enabled) }
    }

    fun setUseTexture(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setUseTexture(enabled) }
    }

    fun setTextureStrength(strength: Float) {
        viewModelScope.launch { container.settingsRepository.setTextureStrength(strength) }
    }

    fun setNoiseDrift(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setNoiseDrift(enabled) }
    }

    fun setTimeOfDayTint(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setTimeOfDayTint(enabled) }
    }

    fun setSilkyPager(enabled: Boolean) {
        viewModelScope.launch { (container.settingsRepository as? com.xdlab.standard.data.local.SettingsRepositoryImpl)?.setSilkyPager(enabled) }
    }

    fun setSlideableHome(enabled: Boolean) {
        viewModelScope.launch { (container.settingsRepository as? com.xdlab.standard.data.local.SettingsRepositoryImpl)?.setSlideableHome(enabled) }
    }

    fun setMotionTouch(enabled: Boolean) {
        viewModelScope.launch { (container.settingsRepository as? com.xdlab.standard.data.local.SettingsRepositoryImpl)?.setMotionTouch(enabled) }
    }

    fun setQuotes(quotes: List<String>) {
        viewModelScope.launch { container.settingsRepository.setQuotes(quotes) }
    }

    fun setHomeModules(modules: Set<HomeModule>) {
        viewModelScope.launch { container.settingsRepository.setHomeModules(modules) }
    }

    /** Geocodes a typed place and, when found, saves it and refreshes. Returns false when nothing matched. */
    suspend fun setWeatherPlace(query: String): Boolean {
        val loc = container.weatherRepository.geocode(query) ?: return false
        container.weatherRepository.refreshForLocation(loc.lat, loc.lon, loc.name)
        return true
    }

    fun setWeatherUnit(unit: WeatherUnit) {
        viewModelScope.launch { container.settingsRepository.setWeatherUnit(unit) }
    }

    fun setClockFormat(format: ClockFormat) {
        viewModelScope.launch { container.settingsRepository.setClockFormat(format) }
    }

    fun setCinematicIntro(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setCinematicIntro(enabled) }
    }

    fun setGlanceEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setGlanceEnabled(enabled) }
    }

    fun setFirstRunDone() {
        viewModelScope.launch { container.settingsRepository.setFirstRunDone() }
    }

    fun resetEverything() {
        viewModelScope.launch {
            container.settingsRepository.resetToDefaults()
            container.appRepository.resetLayout()
        }
    }

    fun setWeatherLocation(location: WeatherLocation) {
        viewModelScope.launch { container.settingsRepository.setWeatherLocation(location) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return StandardAppViewModel(container) as T
                }
            }
    }
}
