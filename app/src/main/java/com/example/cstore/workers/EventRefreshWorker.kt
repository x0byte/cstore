package com.example.cstore.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cstore.data.events.EventRepository
import com.example.cstore.R

/**
 * Background worker that periodically refreshes event data from bundled CSV.
 * Ensures the event sensor and aggregator stay up-to-date even when app is backgrounded.
 */
class EventRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Reload bundled events CSV
            val result = EventRepository.autoLoadBundled(applicationContext)
            
            result.fold(
                onSuccess = { count ->
                    // Successfully refreshed events
                    Result.success()
                },
                onFailure = { error ->
                    // Failed to refresh, retry
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            // Unexpected error, retry
            Result.retry()
        }
    }
}

