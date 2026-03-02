package com.example.loveapp.utils

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges LoveApp's custom calendars/events with the Android system CalendarContract.
 *
 * Requires READ_CALENDAR + WRITE_CALENDAR permissions (declared in manifest).
 */
@Singleton
class CalendarExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val LOVEAPP_CALENDAR_NAME = "LoveApp"
        private const val LOVEAPP_CALENDAR_DISPLAY_NAME = "💕 LoveApp"
        private const val LOVEAPP_ACCOUNT_TYPE = "com.example.loveapp"
        private const val LOVEAPP_ACCOUNT_NAME = "LoveApp Events"
    }

    data class SystemCalendarEvent(
        val id: Long,
        val title: String,
        val startMillis: Long,
        val endMillis: Long,
        val description: String = "",
        val allDay: Boolean = true
    )

    // ─── Calendar management ──────────────────────────────────────────────

    /** Finds or creates the LoveApp calendar in the system CalendarContract. */
    @RequiresPermission(allOf = [Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR])
    suspend fun getOrCreateLoveAppCalendarId(): Long = withContext(Dispatchers.IO) {
        findLoveAppCalendarId() ?: createLoveAppCalendar()
    }

    private fun findLoveAppCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND ${CalendarContract.Calendars.ACCOUNT_NAME} = ?"
        val selArgs = arrayOf(LOVEAPP_ACCOUNT_TYPE, LOVEAPP_ACCOUNT_NAME)
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI, projection, selection, selArgs, null
            )
            if (cursor != null && cursor.moveToFirst()) cursor.getLong(0) else null
        } finally {
            cursor?.close()
        }
    }

    private fun createLoveAppCalendar(): Long {
        val cv = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, LOVEAPP_ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, LOVEAPP_ACCOUNT_TYPE)
            put(CalendarContract.Calendars.NAME, LOVEAPP_CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, LOVEAPP_CALENDAR_DISPLAY_NAME)
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFFFC6C9D.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, LOVEAPP_ACCOUNT_NAME)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, LOVEAPP_ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, LOVEAPP_ACCOUNT_TYPE)
            .build()
        val result = context.contentResolver.insert(uri, cv)
        return result?.lastPathSegment?.toLongOrNull() ?: -1L
    }

    // ─── Event insertion / deletion ───────────────────────────────────────

    /**
     * Inserts a single all-day event on [dateMillis] into the LoveApp system calendar.
     * Returns the new system event ID.
     */
    @RequiresPermission(allOf = [Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR])
    suspend fun insertEvent(
        calendarId: Long,
        title: String,
        dateMillis: Long,
        description: String = "",
        allDay: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, dateMillis)
            put(CalendarContract.Events.DTEND, dateMillis + if (allDay) 86_400_000L else 3_600_000L)
            put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, cv)
        uri?.lastPathSegment?.toLongOrNull() ?: -1L
    }

    /** Deletes a system calendar event by its id. */
    @RequiresPermission(Manifest.permission.WRITE_CALENDAR)
    suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        val uri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.delete(uri, null, null) > 0
    }

    /** Returns all events in the LoveApp system calendar. */
    @RequiresPermission(Manifest.permission.READ_CALENDAR)
    suspend fun getEvents(calendarId: Long): List<SystemCalendarEvent> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.ALL_DAY
        )
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DELETED} = 0"
        val events = mutableListOf<SystemCalendarEvent>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI, projection, selection, arrayOf(calendarId.toString()), null
            )
            while (cursor != null && cursor.moveToNext()) {
                events.add(SystemCalendarEvent(
                    id          = cursor.getLong(0),
                    title       = cursor.getString(1) ?: "",
                    startMillis = cursor.getLong(2),
                    endMillis   = cursor.getLong(3),
                    description = cursor.getString(4) ?: "",
                    allDay      = cursor.getInt(5) == 1
                ))
            }
        } finally {
            cursor?.close()
        }
        events
    }

    // ─── Bulk sync helpers ────────────────────────────────────────────────

    /**
     * Exports a list of LoveApp calendar event dates to the Android system calendar.
     * Avoids duplicates by title+date matching.
     */
    @RequiresPermission(allOf = [Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR])
    suspend fun exportEventsToSystem(
        events: List<Pair<String, Long>> // (title, dateMillis)
    ): Int {
        val calendarId = getOrCreateLoveAppCalendarId()
        if (calendarId < 0) return 0
        val existing = getEvents(calendarId)
        var inserted = 0
        events.forEach { (title, millis) ->
            val alreadyExists = existing.any { it.title == title && it.startMillis == millis }
            if (!alreadyExists) {
                insertEvent(calendarId, title, millis)
                inserted++
            }
        }
        return inserted
    }
}
