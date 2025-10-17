package com.example.cstore.domain.aggregation

import com.example.cstore.data.listing.Listing
import com.example.cstore.data.sensor.EventSensor
import com.example.cstore.data.weather.WeatherData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max

data class ListingContext(
    val listingId: String,
    val relevanceScore: Double,
    val badges: List<String>
)

data class ContextState(
    val climateScore: Double,
    val eventIntensity: Double,
    val listingContexts: Map<String, ListingContext>
)

object ContextAggregator {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _weather = MutableStateFlow<WeatherData?>(null)
    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    private val _context = MutableStateFlow(
        ContextState(
            climateScore = 0.0,
            eventIntensity = 0.0,
            listingContexts = emptyMap()
        )
    )
    val context: StateFlow<ContextState> = _context

    init {
        // Combine event sensor + weather + listings
        combine(EventSensor.reading, _weather, _listings) { eventReading, weather, listings ->
            val climate = weatherToScore(weather)
            val intensity = eventReading.globalIntensity
            val listingContexts = listings.associate { listing ->
                val score = computeRelevance(listing, intensity, climate)
                val badges = buildBadges(score, intensity, climate)
                listing.id to ListingContext(listing.id, score, badges)
            }
            ContextState(climate, intensity, listingContexts)
        }.stateIn(scope, SharingStarted.Eagerly, _context.value).also { flow ->
            // Keep updating local state
            scope.launch {
                flow.collect { state -> _context.value = state }
            }
        }
    }

    fun updateWeather(weather: WeatherData?) { _weather.value = weather }
    fun updateListings(list: List<Listing>) { _listings.value = list }

    private fun weatherToScore(weather: WeatherData?): Double {
        if (weather == null) return 0.0
        val temp = weather.temperature
        // Peak comfort around 22C (score 1.0), taper off otherwise
        val comfort = 1.0 - (kotlin.math.abs(temp - 22.0) / 22.0)
        return comfort.coerceIn(0.0, 1.0)
    }

    private fun computeRelevance(listing: Listing, eventIntensity: Double, climateScore: Double): Double {
        // Simple weight: events 0.6, climate 0.2, category boost 0.2
        val catBoost = categoryBoost(listing.category, climateScore)
        val raw = 0.6 * eventIntensity + 0.2 * climateScore + 0.2 * catBoost
        return raw.coerceIn(0.0, 1.0)
    }

    private fun categoryBoost(category: String?, climate: Double): Double {
        return when (category) {
            "Clothing" -> if (climate < 0.5) 1.0 else 0.3
            "Electronics" -> 0.6
            "Home" -> 0.5
            "Outdoor" -> if (climate > 0.6) 0.9 else 0.4
            else -> 0.4
        }
    }

    private fun buildBadges(score: Double, eventIntensity: Double, climate: Double): List<String> {
        val badges = mutableListOf<String>()
        if (eventIntensity > 0.6) badges.add("Event nearby")
        if (score > 0.75) badges.add("Trending")
        return badges
    }
}


