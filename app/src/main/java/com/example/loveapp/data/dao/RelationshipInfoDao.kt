package com.example.loveapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loveapp.data.entity.RelationshipInfo

@Dao
interface RelationshipInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationshipInfo(info: RelationshipInfo): Long

    @Update
    suspend fun updateRelationshipInfo(info: RelationshipInfo)

    @Delete
    suspend fun deleteRelationshipInfo(info: RelationshipInfo)

    @Query("SELECT * FROM relationship_info WHERE id = :relationshipId")
    suspend fun getRelationshipInfoById(relationshipId: Int): RelationshipInfo?

    @Query("SELECT * FROM relationship_info WHERE userId1 = :userId OR userId2 = :userId LIMIT 1")
    suspend fun getRelationshipInfoByUserId(userId: Int): RelationshipInfo?

    @Query("SELECT * FROM relationship_info")
    suspend fun getAllRelationshipInfo(): List<RelationshipInfo>
}
