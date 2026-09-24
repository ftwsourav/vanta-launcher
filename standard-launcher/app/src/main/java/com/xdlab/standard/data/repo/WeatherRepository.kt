package com.xdlab.standard.data.repo

import com.xdlab.standard.data.remote.ForecastDay
import com.xdlab.standard.domain.model.WeatherData
import com.xdlab.standard.domain.model.WeatherLocation
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
