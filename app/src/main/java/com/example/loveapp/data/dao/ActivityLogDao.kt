package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.ActivityLog

@Dao
interface ActivityLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(activityLog: ActivityLog): Long

    @Update
    suspend fun updateActivityLog(activityLog: ActivityLog)

    @Delete
    suspend fun deleteActivityLog(activityLog: ActivityLog)

    @Query("SELECT * FROM activity_logs WHERE id = :activityId")
    suspend fun getActivityById(activityId: Int): ActivityLog?

    @Query("SELECT * FROM activity_logs WHERE userId = :userId AND date = :date ORDER BY timestamp DESC")
    suspend fun getActivitiesByDate(userId: Int, date: String): List<ActivityLog>

    @Query("SELECT * FROM activity_logs WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getActivitiesByUser(userId: Int): List<ActivityLog>

    @Query("SELECT * FROM activity_logs WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY timestamp DESC")
    suspend fun getActivitiesByDateRange(userId: Int, startDate: String, endDate: String): List<ActivityLog>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    suspend fun getAllActivities(): List<ActivityLog>
}
