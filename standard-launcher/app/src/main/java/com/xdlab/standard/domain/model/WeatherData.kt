package com.xdlab.standard.domain.model

data class WeatherData(
    val location: String,
    val tempC: Int,
    val condition: String,
    val conditionCode: String,
    val highC: Int,
    val lowC: Int,
    val humidity: Int,
    val windKph: Int,
    val lastUpdatedEpoch: Long
)
