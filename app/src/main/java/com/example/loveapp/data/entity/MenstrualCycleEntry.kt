package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menstrual_cycle")
data class MenstrualCycleEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val cycleStartDate: Long, // timestamp of first day of period
    val cycleDuration: Int = 28, // average cycle length
    val periodDuration: Int = 5, // average period length
    val lastUpdated: Long = System.currentTimeMillis(),
    val symptoms: String = "", // comma-separated symptoms
    val mood: String = "", // associated mood
    val notes: String = ""
)
