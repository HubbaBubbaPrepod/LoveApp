package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.OutboxEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entry: OutboxEntry): Long

    @Delete
    suspend fun remove(entry: OutboxEntry)

    @Update
    suspend fun update(entry: OutboxEntry)

    /** Returns all pending entries ordered by creation time (FIFO). */
    @Query("SELECT * FROM outbox ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<OutboxEntry>

    /** Observable count for UI indicators / sync badge. */
    @Query("SELECT COUNT(*) FROM outbox")
    fun pendingCount(): Flow<Int>

    /** Entries ready for the next retry attempt. */
    @Query(
        """
        SELECT * FROM outbox
        WHERE retryCount < 5 AND retryAfter <= :now
        ORDER BY createdAt ASC
        LIMIT :batchSize
        """
    )
    suspend fun getRetryable(now: Long = System.currentTimeMillis(), batchSize: Int = 20): List<OutboxEntry>

    @Query("DELETE FROM outbox WHERE entityType = :entityType AND localId = :localId")
    suspend fun removeByEntity(entityType: String, localId: Int)

    @Query("DELETE FROM outbox")
    suspend fun clearAll()
}
