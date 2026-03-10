package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.loveapp.data.entity.SleepEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SleepEntry): Long

    @Query("SELECT * FROM sleep_entries WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: Int): SleepEntry?

    @Query("SELECT * FROM sleep_entries WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): SleepEntry?

    @Query("SELECT * FROM sleep_entries WHERE userId = :userId AND deletedAt IS NULL ORDER BY date DESC")
    fun observeByUser(userId: Int): Flow<List<SleepEntry>>

    @Query("SELECT * FROM sleep_entries WHERE userId = :userId AND date = :date AND deletedAt IS NULL LIMIT 1")
    suspend fun getByDate(userId: Int, date: String): SleepEntry?

    @Query("SELECT * FROM sleep_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate AND deletedAt IS NULL ORDER BY date DESC")
    suspend fun getByDateRange(userId: Int, startDate: String, endDate: String): List<SleepEntry>

    @Query("SELECT * FROM sleep_entries WHERE deletedAt IS NULL ORDER BY date DESC")
    fun observeAll(): Flow<List<SleepEntry>>

    @Query("SELECT * FROM sleep_entries WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<SleepEntry>
}
