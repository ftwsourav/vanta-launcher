package app.vanta.launcher.data.repo

import app.vanta.launcher.data.remote.ForecastDay
import app.vanta.launcher.domain.model.WeatherData
import app.vanta.launcher.domain.model.WeatherLocation
import kotlinx.coroutines.flow.StateFlow

interface WeatherRepository {
    val weather: StateFlow<WeatherData?>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>
    val forecast: StateFlow<List<ForecastDay>>
    /** Fetches for the saved location. Returns false when the fetch failed (error is set). */
    suspend fun refresh(): Boolean
    suspend fun refreshForLocation(lat: Double, lon: Double, name: String)
    /** Resolves a typed place name to coordinates via Open-Meteo geocoding; null when nothing matches. */
    suspend fun geocode(query: String): WeatherLocation?
}
