package com.example.cstore.data.sensor

import com.example.cstore.data.events.CityEvent
import com.example.cstore.data.events.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.math.exp

data class EventSensorReading(
    val timestamp: Instant,
    val activeEvents: List<CityEvent>,
    val globalIntensity: Double // 0.0..1.0, based on number and proximity of active events to now
)

object EventSensor {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _reading = MutableStateFlow(EventSensorReading(Instant.EPOCH, emptyList(), 0.0))
    val reading: StateFlow<EventSensorReading> = _reading

    init {
        scope.launch {
            while (isActive) {
                val now = Instant.now()
                val events = EventRepository.events.value
                val active = events.filter { ev ->
                    val starts = ev.startTime
                    val ends = ev.endTime
                    when {
                        ends != null -> now.isAfter(starts) && now.isBefore(ends)
                        else -> now.isAfter(starts) && now.isBefore(starts.plusSeconds(3 * 60 * 60)) // default 3h window
                    }
                }
                val intensity = normalizeIntensity(active.size)
                _reading.value = EventSensorReading(now, active, intensity)
                delay(10_000) // update every 10s
            }
        }
    }

    private fun normalizeIntensity(activeCount: Int): Double {
        // Smooth logistic-like curve for counts 0..20
        val x = activeCount.toDouble()
        val k = 0.35
        val x0 = 6.0
        val y = 1.0 / (1 + exp(-k * (x - x0)))
        return y.coerceIn(0.0, 1.0)
    }
}


