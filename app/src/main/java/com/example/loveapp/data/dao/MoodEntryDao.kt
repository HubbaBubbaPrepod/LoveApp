package com.example.loveapp.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.MoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntry(moodEntry: MoodEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(moodEntry: MoodEntry): Long

    @Update
    suspend fun updateMoodEntry(moodEntry: MoodEntry)

    @Delete
    suspend fun deleteMoodEntry(moodEntry: MoodEntry)

    @Query("SELECT * FROM mood_entries WHERE id = :moodId AND deletedAt IS NULL")
    suspend fun getMoodEntryById(moodId: Int): MoodEntry?

    @Query("SELECT * FROM mood_entries WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): MoodEntry?

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND date = :date AND deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getMoodsByDate(userId: Int, date: String): List<MoodEntry>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getMoodsByUser(userId: Int): List<MoodEntry>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate AND deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getMoodsByDateRange(userId: Int, startDate: String, endDate: String): List<MoodEntry>

    @Query("SELECT * FROM mood_entries WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getAllMoods(): List<MoodEntry>

    @Query("SELECT * FROM mood_entries WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<MoodEntry>

    @Query("SELECT * FROM mood_entries WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    fun observeAllMoods(): Flow<List<MoodEntry>>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun observeMoodsByUser(userId: Int): Flow<List<MoodEntry>>

    /** Reactive: all moods (own + partner) for a given date, used by MoodViewModel to avoid per-screen network calls. */
    @Query("SELECT * FROM mood_entries WHERE date = :date AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun observeByDate(date: String): Flow<List<MoodEntry>>

    /** Reactive: moods in a date range (own + partner), used for calendar month view cache. */
    @Query("SELECT * FROM mood_entries WHERE date >= :startDate AND date <= :endDate AND deletedAt IS NULL ORDER BY date DESC, timestamp DESC")
    fun observeByDateRange(startDate: String, endDate: String): Flow<List<MoodEntry>>

    /** Paging 3 – mood history (all users) newest first. */
    @Query("SELECT * FROM mood_entries WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    fun pagingSource(): PagingSource<Int, MoodEntry>
}
