package com.example.cstore.data.weather

data class WeatherResponse(
    val main: MainInfo,
    val weather: List<WeatherInfo>,
    val name: String? = null
)

data class MainInfo(
    val temp: Double
)

data class WeatherInfo(
    val main: String,
    val description: String
)

data class WeatherData(
    val temperature: Double,
    val condition: String,
    val description: String,
    val cityName: String
)
