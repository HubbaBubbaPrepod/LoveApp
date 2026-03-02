package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.CustomCalendar
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCalendarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendar(calendar: CustomCalendar): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(calendar: CustomCalendar): Long

    @Update
    suspend fun updateCalendar(calendar: CustomCalendar)

    @Delete
    suspend fun deleteCalendar(calendar: CustomCalendar)

    @Query("SELECT * FROM custom_calendars WHERE id = :calendarId AND deletedAt IS NULL")
    suspend fun getCalendarById(calendarId: Int): CustomCalendar?

    @Query("SELECT * FROM custom_calendars WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): CustomCalendar?

    @Query("SELECT * FROM custom_calendars WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getCalendarsByUser(userId: Int): List<CustomCalendar>

    @Query("SELECT * FROM custom_calendars WHERE type = :type AND deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getCalendarsByType(type: String): List<CustomCalendar>

    @Query("SELECT * FROM custom_calendars WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getAllCalendars(): List<CustomCalendar>

    @Query("SELECT * FROM custom_calendars WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<CustomCalendar>

    @Query("SELECT * FROM custom_calendars WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAllCalendars(): Flow<List<CustomCalendar>>

    @Query("SELECT * FROM custom_calendars WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeCalendarsByUser(userId: Int): Flow<List<CustomCalendar>>
}
