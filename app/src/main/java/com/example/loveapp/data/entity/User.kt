package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val email: String,
    val password: String,
    val displayName: String,
    val profileImage: String? = null,
    val gender: String, // "male" or "female"
    val createdAt: Long = System.currentTimeMillis(),
    val isLoggedIn: Boolean = false
)
