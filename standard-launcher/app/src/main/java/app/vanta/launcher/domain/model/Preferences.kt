package app.vanta.launcher.domain.model

enum class WeatherUnit { CELSIUS, FAHRENHEIT }

enum class ClockFormat { AUTO, H12, H24 }

fun WeatherUnit.display(celsius: Int): String = when (this) {
    WeatherUnit.CELSIUS -> "$celsius°"
    WeatherUnit.FAHRENHEIT -> "${Math.round(celsius * 9f / 5f + 32f)}°"
}

fun WeatherUnit.displayWind(kph: Int): String = when (this) {
    WeatherUnit.CELSIUS -> "$kph KM/H"
    WeatherUnit.FAHRENHEIT -> "${Math.round(kph * 0.621371f)} MPH"
}
