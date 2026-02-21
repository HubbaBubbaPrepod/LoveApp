package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.MoodEntry

@Dao
interface MoodEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntry(moodEntry: MoodEntry): Long

    @Update
    suspend fun updateMoodEntry(moodEntry: MoodEntry)

    @Delete
    suspend fun deleteMoodEntry(moodEntry: MoodEntry)

    @Query("SELECT * FROM mood_entries WHERE id = :moodId")
    suspend fun getMoodEntryById(moodId: Int): MoodEntry?

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND date = :date ORDER BY timestamp DESC")
    suspend fun getMoodsByDate(userId: Int, date: String): List<MoodEntry>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getMoodsByUser(userId: Int): List<MoodEntry>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY timestamp DESC")
    suspend fun getMoodsByDateRange(userId: Int, startDate: String, endDate: String): List<MoodEntry>

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    suspend fun getAllMoods(): List<MoodEntry>
}
