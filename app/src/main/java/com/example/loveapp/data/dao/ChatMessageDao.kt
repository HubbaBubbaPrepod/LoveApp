package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.loveapp.data.entity.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatMessage): Long

    @Query("SELECT * FROM chat_messages WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: Int): ChatMessage?

    @Query("SELECT * FROM chat_messages WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): ChatMessage?

    @Query("SELECT * FROM chat_messages WHERE timMsgId = :timMsgId LIMIT 1")
    suspend fun getByTimMsgId(timMsgId: String): ChatMessage?

    @Query("SELECT * FROM chat_messages WHERE coupleKey = :coupleKey AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun observeMessages(coupleKey: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE coupleKey = :coupleKey AND deletedAt IS NULL ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(coupleKey: String, limit: Int = 50): List<ChatMessage>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE receiverId = :userId AND isRead = 0 AND deletedAt IS NULL")
    fun observeUnreadCount(userId: Int): Flow<Int>

    @Query("UPDATE chat_messages SET isRead = 1 WHERE receiverId = :userId AND isRead = 0")
    suspend fun markAllRead(userId: Int)

    @Query("SELECT * FROM chat_messages WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<ChatMessage>
}
