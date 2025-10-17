package com.example.cstore.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cstore.data.listing.Listing
import com.example.cstore.data.listing.ListingRepository
import com.example.cstore.data.weather.WeatherData
import com.example.cstore.domain.aggregation.ContextAggregator
import com.example.cstore.domain.aggregation.ContextState
import com.example.cstore.data.weather.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val listings: List<Listing>, val weather: WeatherData? = null) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data class Success(val weather: WeatherData) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class HomeViewModel(
    private val repository: ListingRepository = ListingRepository(),
    private val weatherRepository: WeatherRepository = WeatherRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState: StateFlow<WeatherUiState> = _weatherState

    val contextState: StateFlow<ContextState> = ContextAggregator.context

    // Don't auto-load on init to prevent crashes before auth

    fun loadListings() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                val result = withTimeout(5000) { repository.getAllListings() }
                _uiState.value = result.fold(
                    onSuccess = { list -> 
                        // Filter out any invalid listings
                        val validListings = list.filter { it.title.isNotBlank() }
                        // Update aggregator and sort by relevance if available
                        ContextAggregator.updateListings(validListings)
                        val ctx = ContextAggregator.context.value
                        val sorted = if (ctx.listingContexts.isNotEmpty()) {
                            validListings.sortedByDescending { l -> ctx.listingContexts[l.id]?.relevanceScore ?: 0.0 }
                        } else validListings
                        HomeUiState.Success(sorted)
                    },
                    onFailure = { e -> 
                        HomeUiState.Error("Failed to load listings: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Request failed: ${e.message}")
            }
        }
    }

    fun loadWeather(lat: Double, lon: Double, apiKey: String) {
        _weatherState.value = WeatherUiState.Loading
        viewModelScope.launch {
            try {
                val result = weatherRepository.getCurrentWeather(lat, lon, apiKey)
                result.fold(
                    onSuccess = { response ->
                        val weatherData = WeatherData(
                            temperature = response.main.temp,
                            condition = response.weather.firstOrNull()?.main ?: "Unknown",
                            description = response.weather.firstOrNull()?.description ?: "Unknown",
                            cityName = response.name ?: "Unknown"
                        )
                        _weatherState.value = WeatherUiState.Success(weatherData)
                        ContextAggregator.updateWeather(weatherData)
                    },
                    onFailure = { e ->
                        _weatherState.value = WeatherUiState.Error("Weather unavailable: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _weatherState.value = WeatherUiState.Error("Weather request failed: ${e.message}")
            }
        }
    }

    fun getRecommendation(temp: Double): String {
        return when {
            temp < 15 -> "It's chilly! Check out jackets and warm clothing."
            temp in 15.0..28.0 -> "Perfect weather — browse all categories!"
            else -> "Feeling the heat? Maybe grab a fan or summer wear!"
        }
    }
}


