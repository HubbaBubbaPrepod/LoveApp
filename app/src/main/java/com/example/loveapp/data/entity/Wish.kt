package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishes")
data class Wish(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val userId: Int, // ID of the user who created it
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val priority: Int = 0, // 0=low, 1=medium, 2=high
    val imageUrl: String? = null,
    val category: String = "", // e.g., "gift", "experience", "date idea"
    val dueDate: Long? = null,
    // ── Sync metadata ──────────────────────────────────────
    val serverId: Int? = null,
    val syncPending: Boolean = true,
    val serverUpdatedAt: Long? = null,
    val deletedAt: Long? = null
)
