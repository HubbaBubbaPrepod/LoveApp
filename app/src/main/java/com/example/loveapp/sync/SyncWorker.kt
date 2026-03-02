package com.example.loveapp.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.loveapp.data.dao.ActivityLogDao
import com.example.loveapp.data.dao.CustomCalendarDao
import com.example.loveapp.data.dao.CustomCalendarEventDao
import com.example.loveapp.data.dao.MenstrualCycleDao
import com.example.loveapp.data.dao.MoodEntryDao
import com.example.loveapp.data.dao.NoteDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.dao.RelationshipInfoDao
import com.example.loveapp.data.dao.WishDao
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.network.SocketState
import com.example.loveapp.network.WebSocketManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Drains the outbox and flushes all `syncPending=true` entities to the server.
 *
 * Scheduled:
 *  - Periodically every 15 minutes by [SyncManager.schedulePeriodicSync].
 *  - Immediately on network reconnection via [SyncManager].
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val outboxDao: OutboxDao,
    private val webSocketManager: WebSocketManager,
    private val noteDao: NoteDao,
    private val wishDao: WishDao,
    private val moodEntryDao: MoodEntryDao,
    private val activityLogDao: ActivityLogDao,
    private val menstrualCycleDao: MenstrualCycleDao,
    private val customCalendarDao: CustomCalendarDao,
    private val customCalendarEventDao: CustomCalendarEventDao,
    private val relationshipInfoDao: RelationshipInfoDao
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME_PERIODIC = "loveapp_sync_periodic"
        const val WORK_NAME_IMMEDIATE = "loveapp_sync_immediate"

        fun scheduleImmediateSync(context: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(context).enqueue(req)
        }

        fun schedulePeriodicSync(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }
    }

    private val gson = Gson()

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started")

        // Ensure socket is connected before attempting sync
        if (webSocketManager.connectionState.value != SocketState.CONNECTED) {
            webSocketManager.connect()
            // Give socket a moment to connect (non-blocking short delay)
            kotlinx.coroutines.delay(2_000)
        }

        var allSucceeded = true

        // 1. Drain outbox (offline-queued mutations)
        try {
            val pending = outboxDao.getRetryable()
            Log.d(TAG, "Outbox entries to process: ${pending.size}")
            for (entry in pending) {
                val sent = deliverOutboxEntry(entry)
                if (sent) {
                    outboxDao.remove(entry)
                } else {
                    val next = entry.copy(
                        retryCount = entry.retryCount + 1,
                        retryAfter = System.currentTimeMillis() + backoffMs(entry.retryCount + 1)
                    )
                    outboxDao.update(next)
                    allSucceeded = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Outbox drain failed", e)
            allSucceeded = false
        }

        // 2. Push any entities that are syncPending=true but not in the outbox
        //    (e.g., created while the app was open but the socket was down)
        try {
            pushPendingEntities()
        } catch (e: Exception) {
            Log.e(TAG, "pushPendingEntities failed", e)
            allSucceeded = false
        }

        return if (allSucceeded) {
            Log.d(TAG, "SyncWorker completed successfully")
            Result.success()
        } else {
            Log.w(TAG, "SyncWorker completed with some failures – will retry")
            Result.retry()
        }
    }

    private fun deliverOutboxEntry(entry: OutboxEntry): Boolean {
        return try {
            val payload: Map<String, Any?> = gson.fromJson(
                entry.payload,
                object : TypeToken<Map<String, Any?>>() {}.type
            )
            webSocketManager.emitChange(entry.entityType, entry.action, payload)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deliver outbox entry ${entry.id}", e)
            false
        }
    }

    /** Push all local syncPending entities that have no outbox entry yet. */
    private suspend fun pushPendingEntities() {
        noteDao.getPendingSync().forEach { note ->
            val map = mapOf(
                "id" to note.serverId,
                "title" to note.title, "content" to note.content,
                "user_id" to note.userId, "is_private" to note.isPrivate,
                "tags" to note.tags, "due_date" to note.dueDate,
                "image_url" to note.imageUrl
            )
            webSocketManager.emitChange("note", if (note.serverId == null) "create" else "update", map)
        }
        wishDao.getPendingSync().forEach { wish ->
            val map = mapOf(
                "id" to wish.serverId, "title" to wish.title, "description" to wish.description,
                "user_id" to wish.userId, "is_completed" to wish.isCompleted,
                "priority" to wish.priority, "category" to wish.category,
                "due_date" to wish.dueDate, "image_url" to wish.imageUrl
            )
            webSocketManager.emitChange("wish", if (wish.serverId == null) "create" else "update", map)
        }
        moodEntryDao.getPendingSync().forEach { mood ->
            val map = mapOf(
                "id" to mood.serverId, "user_id" to mood.userId,
                "mood_type" to mood.moodType, "date" to mood.date,
                "note" to mood.note, "color" to mood.color
            )
            webSocketManager.emitChange("mood", if (mood.serverId == null) "create" else "update", map)
        }
        activityLogDao.getPendingSync().forEach { act ->
            val map = mapOf(
                "id" to act.serverId, "user_id" to act.userId, "title" to act.title,
                "description" to act.description, "date" to act.date,
                "image_urls" to act.imageUrls, "category" to act.category
            )
            webSocketManager.emitChange("activity", if (act.serverId == null) "create" else "update", map)
        }
        menstrualCycleDao.getPendingSync().forEach { cycle ->
            val map = mapOf(
                "id" to cycle.serverId, "user_id" to cycle.userId,
                "cycle_start_date" to cycle.cycleStartDate,
                "cycle_duration" to cycle.cycleDuration,
                "period_duration" to cycle.periodDuration,
                "symptoms" to cycle.symptoms, "mood" to cycle.mood, "notes" to cycle.notes
            )
            webSocketManager.emitChange("cycle", if (cycle.serverId == null) "create" else "update", map)
        }
        customCalendarDao.getPendingSync().forEach { cal ->
            val map = mapOf(
                "id" to cal.serverId, "user_id" to cal.userId, "name" to cal.name,
                "description" to cal.description, "type" to cal.type,
                "color_hex" to cal.colorHex, "icon" to cal.icon
            )
            webSocketManager.emitChange("calendar", if (cal.serverId == null) "create" else "update", map)
        }
        customCalendarEventDao.getPendingSync().forEach { ev ->
            val map = mapOf(
                "id" to ev.serverId, "calendar_id" to ev.calendarId, "title" to ev.title,
                "description" to ev.description, "event_date" to ev.eventDate,
                "event_type" to ev.eventType, "marked_date" to ev.markedDate,
                "image_url" to ev.imageUrl
            )
            webSocketManager.emitChange("event", if (ev.serverId == null) "create" else "update", map)
        }
        relationshipInfoDao.getPendingSync().forEach { rel ->
            val map = mapOf(
                "id" to rel.serverId, "user_id_1" to rel.userId1, "user_id_2" to rel.userId2,
                "relationship_start_date" to rel.relationshipStartDate,
                "first_kiss_date" to rel.firstKissDate,
                "anniversary_date" to rel.anniversaryDate,
                "nickname1" to rel.nickname1, "nickname2" to rel.nickname2
            )
            webSocketManager.emitChange("relationship", if (rel.serverId == null) "create" else "update", map)
        }
    }

    /** Exponential back-off: 1 min, 2 min, 4 min, 8 min, 16 min (capped). */
    private fun backoffMs(attempt: Int): Long =
        minOf(1L shl (attempt - 1), 16L) * 60_000L
}
