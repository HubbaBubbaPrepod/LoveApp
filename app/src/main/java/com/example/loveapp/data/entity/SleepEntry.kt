package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_entries")
data class SleepEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val date: String, // YYYY-MM-DD
    val bedtime: String? = null, // HH:mm
    val wakeTime: String? = null, // HH:mm
    val durationMinutes: Int? = null,
    val quality: Int? = null, // 1-5
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    // ── Sync metadata ──────────────────────────────────────
    val serverId: Int? = null,
    val syncPending: Boolean = true,
    val serverUpdatedAt: Long? = null,
    val deletedAt: Long? = null
)
