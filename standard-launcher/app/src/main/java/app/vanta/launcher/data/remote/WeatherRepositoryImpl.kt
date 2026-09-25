package app.vanta.launcher.data.remote

import android.content.Context
import android.util.Log
import app.vanta.launcher.data.repo.SettingsRepository
import app.vanta.launcher.data.repo.WeatherRepository
import app.vanta.launcher.domain.model.WeatherData
import app.vanta.launcher.domain.model.WeatherLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class WeatherRepositoryImpl(
    context: Context,
    private val settings: SettingsRepository
) : WeatherRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val tilePrefs = context.getSharedPreferences(TILE_PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = OkHttpClient.Builder()
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    private val _weather = MutableStateFlow<WeatherData?>(null)
    override val weather: StateFlow<WeatherData?> = _weather.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val _forecast = MutableStateFlow<List<ForecastDay>>(emptyList())
    override val forecast: StateFlow<List<ForecastDay>> = _forecast.asStateFlow()

    init {
        loadCachedWeather()
        startAutoRefresh()
    }

    private fun loadCachedWeather() {
        val cached = prefs.getString(KEY_CACHE, null) ?: return
        val name = prefs.getString(KEY_NAME, null) ?: settings.settings.value.weatherLocation.name
        val fetchedAt = prefs.getLong(KEY_FETCHED_AT, System.currentTimeMillis() / 1000)
        try {
            val response = json.decodeFromString<WeatherResponse>(cached)
            _weather.value = response.toDomain(name, fetchedAt)
            _forecast.value = response.toForecastDays()
        } catch (e: Exception) {
            Log.w(TAG, "cached weather unreadable", e)
        }
    }

    private fun cacheWeather(response: WeatherResponse, name: String, fetchedAt: Long) {
        try {
            prefs.edit()
                .putString(KEY_CACHE, json.encodeToString(response))
                .putString(KEY_NAME, name)
                .putLong(KEY_FETCHED_AT, fetchedAt)
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "weather cache write failed", e)
        }
    }

    private fun startAutoRefresh() {
        scope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                val loc = settings.settings.value.weatherLocation
                if (loc.lat.isFinite() && loc.lon.isFinite() && !(loc.lat == 0.0 && loc.lon == 0.0)) {
                    refresh()
                }
            }
        }
    }

    private fun persistTile(data: WeatherData, iconCode: String) {
        try {
            tilePrefs.edit()
                .putString(KEY_TILE_TEMP, "${data.tempC}\u00B0")
                .putString(KEY_TILE_CONDITION, data.condition.uppercase())
                .putString(KEY_TILE_CITY, data.location.uppercase())
                .putString(KEY_TILE_ICON, iconCode)
                .putLong(KEY_TILE_UPDATED, data.lastUpdatedEpoch)
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "tile prefs write failed", e)
        }
    }

    private fun wmoToOwmIcon(code: Int, isDay: Boolean): String {
        val base = when (code) {
            0, 1 -> "01"
            2 -> "02"
            3 -> "04"
            45, 48 -> "50"
            in 51..57 -> "09"
            in 61..67 -> "10"
            in 71..77 -> "13"
            in 80..82 -> "09"
            in 85..86 -> "13"
            in 95..99 -> "11"
            else -> "03"
        }
        return base + if (isDay) "d" else "n"
    }

    private fun isDayTime(timeIso: String): Boolean {
        val hour = timeIso.substringAfter("T", "").take(2).toIntOrNull() ?: return true
        return hour in 6..18
    }

    override suspend fun refresh(): Boolean {
        val location = settings.settings.value.weatherLocation
        return fetchWeather(location.lat, location.lon, location.name)
    }

    override suspend fun refreshForLocation(lat: Double, lon: Double, name: String) {
        settings.setWeatherLocation(WeatherLocation(name, lat, lon))
        fetchWeather(lat, lon, name)
    }

    /** OkHttp is blocking: always off the main thread. */
    private suspend fun fetchWeather(lat: Double, lon: Double, name: String): Boolean = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _error.value = null
        try {
            val url = BASE_URL.toHttpUrl().newBuilder()
                .addQueryParameter("latitude", lat.toString())
                .addQueryParameter("longitude", lon.toString())
                .addQueryParameter("current", "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m")
                .addQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min")
                .addQueryParameter("timezone", "auto")
                .addQueryParameter("forecast_days", "3")
                .build()
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    _error.value = "HTTP ${response.code}"
                    return@withContext false
                }
                val body = response.body?.string()
                if (body.isNullOrEmpty()) {
                    _error.value = "EMPTY"
                    return@withContext false
                }
                val parsed = json.decodeFromString<WeatherResponse>(body)
                val now = System.currentTimeMillis() / 1000
                val domain = parsed.toDomain(name, now)
                _weather.value = domain
                _forecast.value = parsed.toForecastDays()
                cacheWeather(parsed, name, now)
                persistTile(domain, wmoToOwmIcon(parsed.current.weather_code, isDayTime(parsed.current.time)))
                true
            }
        } catch (e: IOException) {
            _error.value = "OFFLINE"
            false
        } catch (e: Exception) {
            Log.w(TAG, "weather fetch failed", e)
            _error.value = "ERROR"
            false
        } finally {
            _isLoading.value = false
        }
    }

    @Serializable
    private data class GeoResponse(val results: List<GeoResult> = emptyList())

    @Serializable
    private data class GeoResult(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val country_code: String? = null,
        val admin1: String? = null
    )

    override suspend fun geocode(query: String): WeatherLocation? = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext null
        try {
            val url = GEO_URL.toHttpUrl().newBuilder()
                .addQueryParameter("name", q)
                .addQueryParameter("count", "1")
                .addQueryParameter("language", "en")
                .addQueryParameter("format", "json")
                .build()
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val hit = json.decodeFromString<GeoResponse>(body).results.firstOrNull() ?: return@withContext null
                val label = listOfNotNull(hit.name, hit.country_code).joinToString(", ").uppercase()
                WeatherLocation(label, hit.latitude, hit.longitude)
            }
        } catch (e: Exception) {
            Log.w(TAG, "geocode failed", e)
            null
        }
    }

    private companion object {
        const val TAG = "StandardWeather"
        const val PREFS_NAME = "weather_cache"
        const val KEY_CACHE = "weather_response_json"
        const val KEY_NAME = "weather_location_name"
        const val KEY_FETCHED_AT = "weather_fetched_at"
        const val TILE_PREFS_NAME = "standard_settings"
        const val KEY_TILE_TEMP = "weather_temp"
        const val KEY_TILE_CONDITION = "weather_condition"
        const val KEY_TILE_CITY = "weather_city"
        const val KEY_TILE_ICON = "weather_icon"
        const val KEY_TILE_UPDATED = "weather_updated"
        const val REFRESH_INTERVAL_MS = 30L * 60L * 1000L
        const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
        const val GEO_URL = "https://geocoding-api.open-meteo.com/v1/search"
    }
}

fun weatherIconToGlyph(iconCode: String): String = when (iconCode.take(2)) {
    "01" -> "\u2600"
    "02" -> "\u26C5"
    "03" -> "\u2601"
    "04" -> "\u2601"
    "09" -> "\u2601"
    "10" -> "\u2601"
    "11" -> "\u26C8"
    "13" -> "\u2744"
    "50" -> "\u2601"
    else -> "\u2601"
}
