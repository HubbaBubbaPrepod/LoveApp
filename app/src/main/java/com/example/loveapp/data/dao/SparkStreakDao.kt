package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.loveapp.data.entity.SparkStreak
import kotlinx.coroutines.flow.Flow

@Dao
interface SparkStreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(streak: SparkStreak): Long

    @Query("SELECT * FROM spark_streaks WHERE coupleKey = :coupleKey LIMIT 1")
    fun observe(coupleKey: String): Flow<SparkStreak?>

    @Query("SELECT * FROM spark_streaks WHERE coupleKey = :coupleKey LIMIT 1")
    suspend fun get(coupleKey: String): SparkStreak?

    @Query("SELECT * FROM spark_streaks WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): SparkStreak?
}
