package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.loveapp.data.entity.CoupleTask
import kotlinx.coroutines.flow.Flow

@Dao
interface CoupleTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: CoupleTask): Long

    @Query("SELECT * FROM couple_tasks WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: Int): CoupleTask?

    @Query("SELECT * FROM couple_tasks WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): CoupleTask?

    @Query("SELECT * FROM couple_tasks WHERE coupleKey = :coupleKey AND deletedAt IS NULL AND (dueDate = :date OR dueDate IS NULL) ORDER BY isCompleted ASC, timestamp ASC")
    fun observeByDate(coupleKey: String, date: String): Flow<List<CoupleTask>>

    @Query("SELECT * FROM couple_tasks WHERE coupleKey = :coupleKey AND deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getAll(coupleKey: String): List<CoupleTask>

    @Query("SELECT COALESCE(SUM(points), 0) FROM couple_tasks WHERE coupleKey = :coupleKey AND isCompleted = 1 AND dueDate = :date AND deletedAt IS NULL")
    fun observeTotalPoints(coupleKey: String, date: String): Flow<Int>

    @Query("SELECT * FROM couple_tasks WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<CoupleTask>
}
