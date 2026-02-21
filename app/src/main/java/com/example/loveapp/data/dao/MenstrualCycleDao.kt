package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.MenstrualCycleEntry

@Dao
interface MenstrualCycleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycleEntry(cycleEntry: MenstrualCycleEntry): Long

    @Update
    suspend fun updateCycleEntry(cycleEntry: MenstrualCycleEntry)

    @Delete
    suspend fun deleteCycleEntry(cycleEntry: MenstrualCycleEntry)

    @Query("SELECT * FROM menstrual_cycle WHERE id = :cycleId")
    suspend fun getCycleEntryById(cycleId: Int): MenstrualCycleEntry?

    @Query("SELECT * FROM menstrual_cycle WHERE userId = :userId ORDER BY cycleStartDate DESC LIMIT 1")
    suspend fun getLatestCycleEntry(userId: Int): MenstrualCycleEntry?

    @Query("SELECT * FROM menstrual_cycle WHERE userId = :userId ORDER BY cycleStartDate DESC")
    suspend fun getCycleEntriesByUser(userId: Int): List<MenstrualCycleEntry>

    @Query("SELECT * FROM menstrual_cycle ORDER BY cycleStartDate DESC")
    suspend fun getAllCycleEntries(): List<MenstrualCycleEntry>
}
