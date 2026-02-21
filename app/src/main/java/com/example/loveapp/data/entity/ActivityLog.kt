package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String, // YYYY-MM-DD format
    val imageUrls: String = "", // comma-separated URLs
    val category: String = "", // e.g., "adventure", "movies", "food", etc.
    val photoPath: String? = null // local path to photo
)
