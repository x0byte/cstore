package com.example.cstore.data.events

import android.content.Context
import android.net.Uri
import com.example.cstore.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object EventRepository {
    private val _events = MutableStateFlow<List<CityEvent>>(emptyList())
    val events: StateFlow<List<CityEvent>> = _events

    suspend fun ingestCsvFromUri(
        context: Context,
        uri: Uri,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Result<Int> = withContext(ioDispatcher) {
        try {
            context.contentResolver.openInputStream(uri).use { stream ->
                if (stream == null) return@withContext Result.failure(IllegalArgumentException("Cannot open stream for URI"))
                parseCsv(stream).fold(
                    onSuccess = { list ->
                        _events.value = list
                        Result.success(list.size)
                    },
                    onFailure = { e -> Result.failure(e) }
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ingestCsvFromStream(
        inputStream: InputStream,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Result<Int> = withContext(ioDispatcher) {
        parseCsv(inputStream).fold(
            onSuccess = { list ->
                _events.value = list
                Result.success(list.size)
            },
            onFailure = { e -> Result.failure(e) }
        )
    }

    fun clear() { _events.value = emptyList() }

    suspend fun autoLoadBundled(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val res = context.resources
            val input = res.openRawResource(R.raw.events_sample)
            ingestCsvFromStream(input)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCsv(inputStream: InputStream): Result<List<CityEvent>> {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            if (lines.isEmpty()) return Result.success(emptyList())

            val header = lines.first().split(',').map { it.trim().lowercase() }
            val indexMap = buildIndexMap(header)

            val parsed = lines.drop(1).mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                val cols = splitCsvLine(line)
                try {
                    val id = getCol(cols, indexMap["id"]).ifBlank { null } ?: generateId(cols)
                    val title = getCol(cols, indexMap["title"]).ifBlank { "Untitled Event" }
                    val category = getCol(cols, indexMap["category"]).ifBlank { null }
                    val venue = getCol(cols, indexMap["venue"]).ifBlank { null }
                    val startTime = parseInstant(getCol(cols, indexMap["start_time"])) ?: return@mapNotNull null
                    val endTime = parseInstant(getCol(cols, indexMap["end_time"]))
                    val lat = getCol(cols, indexMap["latitude"]).toDoubleOrNull()
                    val lon = getCol(cols, indexMap["longitude"]).toDoubleOrNull()
                    CityEvent(
                        id = id,
                        title = title,
                        category = category,
                        venue = venue,
                        startTime = startTime,
                        endTime = endTime,
                        latitude = lat,
                        longitude = lon
                    )
                } catch (_: Exception) {
                    null
                }
            }
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildIndexMap(header: List<String>): Map<String, Int> {
        val indices = mutableMapOf<String, Int>()
        for ((key, aliases) in EventCsvSchema.headerAliases) {
            val idx = header.indexOfFirst { h -> aliases.any { it.equals(h, ignoreCase = true) } }
            if (idx >= 0) indices[key] = idx
        }
        return indices
    }

    private fun getCol(cols: List<String>, idx: Int?): String {
        if (idx == null) return ""
        return if (idx in cols.indices) cols[idx].trim().trim('"') else ""
    }

    private fun parseInstant(value: String?): Instant? {
        if (value == null || value.isBlank()) return null
        val v = value.trim().trim('"')
        for (fmt in EventCsvSchema.dtFormats) {
            try {
                // Ensure the formatter has a zone
                val zonedFmt = if (fmt.zone == null) fmt.withZone(ZoneId.of("UTC")) else fmt
                return zonedFmt.parse(v, Instant::from)
            } catch (_: Exception) { }
        }
        // Fallback: try to parse epoch seconds/millis
        v.toLongOrNull()?.let { raw ->
            return if (raw > 1_000_000_000_000L) Instant.ofEpochMilli(raw) else Instant.ofEpochSecond(raw)
        }
        return null
    }

    private fun splitCsvLine(line: String): List<String> {
        // Minimal CSV splitter handling quoted commas
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when (c) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (inQuotes) sb.append(c) else {
                        result.add(sb.toString())
                        sb.clear()
                    }
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    private fun generateId(cols: List<String>): String = cols.joinToString("|").hashCode().toString()
}


