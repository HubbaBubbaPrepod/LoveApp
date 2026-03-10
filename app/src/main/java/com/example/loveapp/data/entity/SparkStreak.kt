package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spark_streaks")
data class SparkStreak(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val coupleKey: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastSparkDate: String? = null, // YYYY-MM-DD
    val totalSparks: Int = 0,
    // ── Sync metadata ──────────────────────────────────────
    val serverId: Int? = null,
    val syncPending: Boolean = true,
    val serverUpdatedAt: Long? = null,
    val deletedAt: Long? = null
)
