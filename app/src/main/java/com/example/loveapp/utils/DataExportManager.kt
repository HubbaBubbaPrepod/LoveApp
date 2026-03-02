package com.example.loveapp.utils

import android.content.Context
import android.net.Uri
import com.example.loveapp.data.dao.*
import com.example.loveapp.data.entity.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full JSON export and import of all local Room data.
 *
 * Export: dump all non-deleted entities into a single JSON file.
 * Import: read the JSON and upsert all entities into Room (merge strategy).
 */
@Singleton
class DataExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteDao: NoteDao,
    private val wishDao: WishDao,
    private val moodEntryDao: MoodEntryDao,
    private val activityLogDao: ActivityLogDao,
    private val cycleDao: MenstrualCycleDao,
    private val calendarDao: CustomCalendarDao,
    private val relationshipDao: RelationshipInfoDao
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)

    data class ExportPayload(
        val exportedAt: Long = System.currentTimeMillis(),
        val version: Int = 1,
        val notes: List<Note> = emptyList(),
        val wishes: List<Wish> = emptyList(),
        val moods: List<MoodEntry> = emptyList(),
        val activities: List<ActivityLog> = emptyList(),
        val cycles: List<MenstrualCycleEntry> = emptyList(),
        val calendars: List<CustomCalendar> = emptyList(),
        val relationships: List<RelationshipInfo> = emptyList()
    )

    /** Writes all data as JSON to [uri]. Returns the number of records exported. */
    suspend fun exportToUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val payload = ExportPayload(
                notes         = noteDao.getAllNotes(),
                wishes        = wishDao.getAllWishes(),
                moods         = moodEntryDao.getAllMoods(),
                activities    = activityLogDao.getAllActivities(),
                cycles        = cycleDao.getAllCycleEntries(),
                calendars     = calendarDao.getAllCalendars(),
                relationships = relationshipDao.getAllRelationshipInfo()
            )
            val total = payload.notes.size + payload.wishes.size + payload.moods.size +
                    payload.activities.size + payload.cycles.size +
                    payload.calendars.size + payload.relationships.size

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.write(gson.toJson(payload))
                }
            } ?: return@withContext Result.failure(Exception("Cannot open URI for writing"))

            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Reads a JSON export file from [uri] and upserts all entities into Room. */
    suspend fun importFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            } ?: return@withContext Result.failure(Exception("Cannot open URI for reading"))

            val payload = gson.fromJson(json, ExportPayload::class.java)

            // Upsert all entities — server IDs and local IDs are preserved
            payload.notes.forEach         { noteDao.upsert(it.copy(id = 0)) }
            payload.wishes.forEach        { wishDao.upsert(it.copy(id = 0)) }
            payload.moods.forEach         { moodEntryDao.upsert(it.copy(id = 0)) }
            payload.activities.forEach    { activityLogDao.upsert(it.copy(id = 0)) }
            payload.cycles.forEach        { cycleDao.upsert(it.copy(id = 0)) }
            payload.calendars.forEach     { calendarDao.upsert(it.copy(id = 0)) }
            payload.relationships.forEach { relationshipDao.upsert(it.copy(id = 0)) }

            val total = payload.notes.size + payload.wishes.size + payload.moods.size +
                    payload.activities.size + payload.cycles.size +
                    payload.calendars.size + payload.relationships.size
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Returns a suggested filename for today's export. */
    fun suggestFileName(): String = "loveapp_export_${dateFmt.format(Date())}.json"
}
