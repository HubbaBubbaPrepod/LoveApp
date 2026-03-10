package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.loveapp.data.entity.MissYouEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface MissYouEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: MissYouEvent): Long

    @Query("SELECT * FROM miss_you_events WHERE coupleKey = :coupleKey ORDER BY timestamp DESC")
    fun observeEvents(coupleKey: String): Flow<List<MissYouEvent>>

    @Query("SELECT * FROM miss_you_events WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): MissYouEvent?

    @Query("SELECT COUNT(*) FROM miss_you_events WHERE coupleKey = :coupleKey AND senderId != :userId AND timestamp > :since")
    fun observeReceivedCount(coupleKey: String, userId: Int, since: Long): Flow<Int>
}
