package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "couple_tasks")
data class CoupleTask(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val coupleKey: String = "",
    val userId: Int,
    val title: String,
    val description: String = "",
    val category: String = "daily", // "daily", "custom"
    val icon: String = "💕",
    val points: Int = 10,
    val isCompleted: Boolean = false,
    val completedBy: Int? = null,
    val completedAt: Long? = null,
    val dueDate: String? = null, // YYYY-MM-DD
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    // ── Sync metadata ──────────────────────────────────────
    val serverId: Int? = null,
    val syncPending: Boolean = true,
    val serverUpdatedAt: Long? = null,
    val deletedAt: Long? = null
)
