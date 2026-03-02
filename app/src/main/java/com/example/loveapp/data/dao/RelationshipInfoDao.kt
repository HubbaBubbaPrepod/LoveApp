package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.RelationshipInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationshipInfo(info: RelationshipInfo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(info: RelationshipInfo): Long

    @Update
    suspend fun updateRelationshipInfo(info: RelationshipInfo)

    @Delete
    suspend fun deleteRelationshipInfo(info: RelationshipInfo)

    @Query("SELECT * FROM relationship_info WHERE id = :relationshipId AND deletedAt IS NULL")
    suspend fun getRelationshipInfoById(relationshipId: Int): RelationshipInfo?

    @Query("SELECT * FROM relationship_info WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): RelationshipInfo?

    @Query("SELECT * FROM relationship_info WHERE (userId1 = :userId OR userId2 = :userId) AND deletedAt IS NULL LIMIT 1")
    suspend fun getRelationshipInfoByUserId(userId: Int): RelationshipInfo?

    @Query("SELECT * FROM relationship_info WHERE deletedAt IS NULL")
    suspend fun getAllRelationshipInfo(): List<RelationshipInfo>

    @Query("SELECT * FROM relationship_info WHERE syncPending = 1 AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<RelationshipInfo>

    @Query("SELECT * FROM relationship_info WHERE userId1 = :userId OR userId2 = :userId AND deletedAt IS NULL LIMIT 1")
    fun observeRelationshipByUserId(userId: Int): Flow<RelationshipInfo?>
}
