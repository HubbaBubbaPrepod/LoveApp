package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val moodType: String, // "excellent", "good", "neutral", "sad", "romantic", "nervous", "tired"
    val timestamp: Long = System.currentTimeMillis(),
    val date: String, // YYYY-MM-DD format
    val note: String = "",
    val color: String = "" // hex color code
)
