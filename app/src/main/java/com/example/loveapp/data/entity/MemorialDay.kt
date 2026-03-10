package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memorial_days")
data class MemorialDay(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val title: String,
    val date: String, // YYYY-MM-DD
    val type: String = "custom", // "anniversary", "birthday", "first_date", "custom"
    val icon: String = "💕",
    val colorHex: String = "#FF6B9D",
    val repeatYearly: Boolean = true,
    val reminderDays: Int = 1,
    val note: String = "",
    // ── Sync metadata ──────────────────────────────────────
    val serverId: Int? = null,
    val syncPending: Boolean = true,
    val serverUpdatedAt: Long? = null,
    val deletedAt: Long? = null
)
