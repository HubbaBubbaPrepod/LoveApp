package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.CustomCalendarEvent

@Dao
interface CustomCalendarEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CustomCalendarEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: CustomCalendarEvent): Long

    @Update
    suspend fun updateEvent(event: CustomCalendarEvent)

    @Delete
    suspend fun deleteEvent(event: CustomCalendarEvent)

    @Query("SELECT * FROM custom_calendar_events WHERE id = :eventId AND deletedAt IS NULL")
    suspend fun getEventById(eventId: Int): CustomCalendarEvent?

    @Query("SELECT * FROM custom_calendar_events WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): CustomCalendarEvent?

    @Query("SELECT * FROM custom_calendar_events WHERE calendarId = :calendarId AND deletedAt IS NULL ORDER BY eventDate DESC")
    suspend fun getEventsByCalendar(calendarId: Int): List<CustomCalendarEvent>

    @Query("SELECT * FROM custom_calendar_events WHERE markedDate = :date AND deletedAt IS NULL ORDER BY eventDate DESC")
    suspend fun getEventsByDate(date: String): List<CustomCalendarEvent>

    @Query("SELECT * FROM custom_calendar_events WHERE deletedAt IS NULL ORDER BY eventDate DESC")
    suspend fun getAllEvents(): List<CustomCalendarEvent>

    @Query("SELECT * FROM custom_calendar_events WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<CustomCalendarEvent>
}
