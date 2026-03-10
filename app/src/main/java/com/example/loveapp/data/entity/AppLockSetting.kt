package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_lock_settings")
data class AppLockSetting(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val pinHash: String,
    val isBiometric: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
