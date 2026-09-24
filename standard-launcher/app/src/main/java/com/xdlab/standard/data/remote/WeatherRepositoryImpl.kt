package com.xdlab.standard.data.remote

import android.content.Context
import android.util.Log
import com.xdlab.standard.data.repo.SettingsRepository
import com.xdlab.standard.data.repo.WeatherRepository
import com.xdlab.standard.domain.model.WeatherData
import com.xdlab.standard.domain.model.WeatherLocation
import kotlinx.coroutines.Dispatchers
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
                _weather.value = parsed.toDomain(name, now)
                _forecast.value = parsed.toForecastDays()
                cacheWeather(parsed, name, now)
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
        const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
        const val GEO_URL = "https://geocoding-api.open-meteo.com/v1/search"
    }
}
