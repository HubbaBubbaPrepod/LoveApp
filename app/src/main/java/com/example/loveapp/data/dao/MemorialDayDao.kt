package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.loveapp.data.entity.MemorialDay
import kotlinx.coroutines.flow.Flow

@Dao
interface MemorialDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: MemorialDay): Long

    @Query("SELECT * FROM memorial_days WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: Int): MemorialDay?

    @Query("SELECT * FROM memorial_days WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): MemorialDay?

    @Query("SELECT * FROM memorial_days WHERE deletedAt IS NULL ORDER BY date ASC")
    fun observeAll(): Flow<List<MemorialDay>>

    @Query("SELECT * FROM memorial_days WHERE deletedAt IS NULL ORDER BY date ASC")
    suspend fun getAll(): List<MemorialDay>

    @Query("SELECT * FROM memorial_days WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<MemorialDay>
}
