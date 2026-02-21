package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_calendars")
data class CustomCalendar(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String = "",
    val type: String, // "special_dates", "sex", "sports", "events", "custom"
    val colorHex: String,
    val icon: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val userId: Int
)
