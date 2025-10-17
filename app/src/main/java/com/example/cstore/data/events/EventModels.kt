package com.example.cstore.data.events

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CityEvent(
    val id: String,
    val title: String,
    val category: String?,
    val venue: String?,
    val startTime: Instant,
    val endTime: Instant?,
    val latitude: Double?,
    val longitude: Double?
)

object EventCsvSchema {
    // Expected headers (case-insensitive, extra columns ignored):
    // id,title,start_time,end_time,latitude,longitude,category,venue
    val headerAliases = mapOf(
        "id" to listOf("id", "event_id"),
        "title" to listOf("title", "event_title", "name"),
        "start_time" to listOf("start_time", "start", "start_datetime", "startdate", "date_start"),
        "end_time" to listOf("end_time", "end", "end_datetime", "enddate", "date_end"),
        "latitude" to listOf("latitude", "lat"),
        "longitude" to listOf("longitude", "lon", "lng", "long"),
        "category" to listOf("category", "type", "tags"),
        "venue" to listOf("venue", "location", "place")
    )

    val dtFormats: List<DateTimeFormatter> = listOf(
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC")),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("UTC")),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm").withZone(ZoneId.of("UTC"))
    )
}


