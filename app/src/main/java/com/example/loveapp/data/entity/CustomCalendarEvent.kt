package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_calendar_events")
data class CustomCalendarEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val calendarId: Int, // reference to custom calendar
    val title: String,
    val description: String = "",
    val eventDate: Long,
    val eventType: String, // "special_date", "relationship_milestone", "sex_calendar", "sports", "other"
    val imageUrl: String? = null,
    val markedDate: String = "", // YYYY-MM-DD format,
    val createdAt: Long = System.currentTimeMillis()
)
