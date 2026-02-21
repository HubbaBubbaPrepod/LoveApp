package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relationship_info")
data class RelationshipInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val relationshipStartDate: Long,
    val firstKissDate: Long? = null,
    val anniversaryDate: Long? = null,
    val userId1: Int,
    val userId2: Int,
    val nickname1: String = "", // nicknames for each person
    val nickname2: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
