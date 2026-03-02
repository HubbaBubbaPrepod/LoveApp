package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.ActivityLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(activityLog: ActivityLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activityLog: ActivityLog): Long

    @Update
    suspend fun updateActivityLog(activityLog: ActivityLog)

    @Delete
    suspend fun deleteActivityLog(activityLog: ActivityLog)

    @Query("SELECT * FROM activity_logs WHERE id = :activityId AND deletedAt IS NULL")
    suspend fun getActivityById(activityId: Int): ActivityLog?

    @Query("SELECT * FROM activity_logs WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): ActivityLog?

    @Query("SELECT * FROM activity_logs WHERE userId = :userId AND date = :date AND deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getActivitiesByDate(userId: Int, date: String): List<ActivityLog>

    @Query("SELECT * FROM activity_logs WHERE userId = :userId AND deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getActivitiesByUser(userId: Int): List<ActivityLog>

    @Query("SELECT * FROM activity_logs WHERE userId = :userId AND date >= :startDate AND date <= :endDate AND deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getActivitiesByDateRange(userId: Int, startDate: String, endDate: String): List<ActivityLog>

    @Query("SELECT * FROM activity_logs WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getAllActivities(): List<ActivityLog>

    @Query("SELECT * FROM activity_logs WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<ActivityLog>

    @Query("SELECT * FROM activity_logs WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    fun observeAllActivities(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE userId = :userId AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun observeActivitiesByUser(userId: Int): Flow<List<ActivityLog>>
}
