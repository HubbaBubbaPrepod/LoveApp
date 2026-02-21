package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.CustomCalendar

@Dao
interface CustomCalendarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendar(calendar: CustomCalendar): Long

    @Update
    suspend fun updateCalendar(calendar: CustomCalendar)

    @Delete
    suspend fun deleteCalendar(calendar: CustomCalendar)

    @Query("SELECT * FROM custom_calendars WHERE id = :calendarId")
    suspend fun getCalendarById(calendarId: Int): CustomCalendar?

    @Query("SELECT * FROM custom_calendars WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getCalendarsByUser(userId: Int): List<CustomCalendar>

    @Query("SELECT * FROM custom_calendars WHERE type = :type ORDER BY createdAt DESC")
    suspend fun getCalendarsByType(type: String): List<CustomCalendar>

    @Query("SELECT * FROM custom_calendars ORDER BY createdAt DESC")
    suspend fun getAllCalendars(): List<CustomCalendar>
}
