package app.vanta.launcher.data.remote

import app.vanta.launcher.domain.model.WeatherData
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class WeatherResponse(
    val current: CurrentWeather,
    val daily: DailyWeather
)

@Serializable
data class CurrentWeather(
    val time: String,
    val temperature_2m: Double,
    val relative_humidity_2m: Int,
    val weather_code: Int,
    val wind_speed_10m: Double
)

@Serializable
data class DailyWeather(
    val time: List<String>,
    val weather_code: List<Int> = emptyList(),
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>
)

fun wmoToCondition(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45 -> "Fog"
    48 -> "Depositing rime fog"
    in 51..57 -> "Drizzle"
    in 61..67 -> "Rain"
    in 71..77 -> "Snow"
    in 80..82 -> "Showers"
    in 85..86 -> "Snow showers"
    in 95..99 -> "Thunderstorm"
    else -> "Unknown"
}

fun wmoToIcon(code: Int): String = when (code) {
    0, 1 -> "\u2600"
    2 -> "\u26C5"
    3 -> "\u2601"
    45, 48 -> "\uF76F"
    in 51..57 -> "\uF749"
    in 61..67 -> "\uF740"
    in 71..77 -> "\u2744"
    in 80..82 -> "\uF748"
    in 85..86 -> "\u2744"
    in 95..99 -> "\u26C8"
    else -> "\u2601"
}

data class ForecastDay(
    val date: String,
    val icon: String,
    val condition: String,
    val highC: Int,
    val lowC: Int
)

fun WeatherResponse.toDomain(name: String, updatedEpoch: Long = System.currentTimeMillis() / 1000): WeatherData {
    val current = this.current
    val daily = this.daily
    return WeatherData(
        location = name,
        tempC = current.temperature_2m.roundToInt(),
        condition = wmoToCondition(current.weather_code),
        conditionCode = current.weather_code.toString(),
        highC = daily.temperature_2m_max.firstOrNull()?.roundToInt()
            ?: current.temperature_2m.roundToInt(),
        lowC = daily.temperature_2m_min.firstOrNull()?.roundToInt()
            ?: current.temperature_2m.roundToInt(),
        humidity = current.relative_humidity_2m,
        windKph = current.wind_speed_10m.roundToInt(),
        lastUpdatedEpoch = updatedEpoch
    )
}

fun WeatherResponse.toForecastDays(): List<ForecastDay> {
    val d = this.daily
    val codes = d.weather_code
    return d.time.indices.map { i ->
        ForecastDay(
            date = d.time[i],
            icon = wmoToIcon(codes.getOrNull(i) ?: 0),
            condition = wmoToCondition(codes.getOrNull(i) ?: 0),
            highC = d.temperature_2m_max.getOrNull(i)?.roundToInt() ?: 0,
            lowC = d.temperature_2m_min.getOrNull(i)?.roundToInt() ?: 0
        )
    }
}
