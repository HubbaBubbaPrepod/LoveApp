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

    @Update
    suspend fun updateEvent(event: CustomCalendarEvent)

    @Delete
    suspend fun deleteEvent(event: CustomCalendarEvent)

    @Query("SELECT * FROM custom_calendar_events WHERE id = :eventId")
    suspend fun getEventById(eventId: Int): CustomCalendarEvent?

    @Query("SELECT * FROM custom_calendar_events WHERE calendarId = :calendarId ORDER BY eventDate DESC")
    suspend fun getEventsByCalendar(calendarId: Int): List<CustomCalendarEvent>

    @Query("SELECT * FROM custom_calendar_events WHERE markedDate = :date ORDER BY eventDate DESC")
    suspend fun getEventsByDate(date: String): List<CustomCalendarEvent>

    @Query("SELECT * FROM custom_calendar_events ORDER BY eventDate DESC")
    suspend fun getAllEvents(): List<CustomCalendarEvent>
}
