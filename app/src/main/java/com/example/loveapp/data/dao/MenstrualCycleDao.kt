package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.MenstrualCycleEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MenstrualCycleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycleEntry(cycleEntry: MenstrualCycleEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cycleEntry: MenstrualCycleEntry): Long

    @Update
    suspend fun updateCycleEntry(cycleEntry: MenstrualCycleEntry)

    @Delete
    suspend fun deleteCycleEntry(cycleEntry: MenstrualCycleEntry)

    @Query("SELECT * FROM menstrual_cycle WHERE id = :cycleId AND deletedAt IS NULL")
    suspend fun getCycleEntryById(cycleId: Int): MenstrualCycleEntry?

    @Query("SELECT * FROM menstrual_cycle WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): MenstrualCycleEntry?

    @Query("SELECT * FROM menstrual_cycle WHERE userId = :userId AND deletedAt IS NULL ORDER BY cycleStartDate DESC LIMIT 1")
    suspend fun getLatestCycleEntry(userId: Int): MenstrualCycleEntry?

    @Query("SELECT * FROM menstrual_cycle WHERE userId = :userId AND deletedAt IS NULL ORDER BY cycleStartDate DESC")
    suspend fun getCycleEntriesByUser(userId: Int): List<MenstrualCycleEntry>

    @Query("SELECT * FROM menstrual_cycle WHERE deletedAt IS NULL ORDER BY cycleStartDate DESC")
    suspend fun getAllCycleEntries(): List<MenstrualCycleEntry>

    @Query("SELECT * FROM menstrual_cycle WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<MenstrualCycleEntry>

    @Query("SELECT * FROM menstrual_cycle WHERE deletedAt IS NULL ORDER BY cycleStartDate DESC")
    fun observeAllCycleEntries(): Flow<List<MenstrualCycleEntry>>

    @Query("SELECT * FROM menstrual_cycle WHERE userId = :userId AND deletedAt IS NULL ORDER BY cycleStartDate DESC")
    fun observeCycleEntriesByUser(userId: Int): Flow<List<MenstrualCycleEntry>>
}
