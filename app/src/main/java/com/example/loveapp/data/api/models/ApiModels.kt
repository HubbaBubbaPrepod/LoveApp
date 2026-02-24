package com.example.loveapp.data.api.models

import com.google.gson.annotations.SerializedName

// Auth Models
data class SignupRequest(
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("display_name")
    val displayName: String,
    val gender: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("display_name")
    val displayName: String,
    val gender: String? = null,
    @SerializedName("profile_image")
    val profileImage: String? = null,
    val token: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

// Notes Models
data class NoteRequest(
    val title: String,
    val content: String,
    @SerializedName("is_private")
    val isPrivate: Boolean = false,
    val tags: String = "",
    @SerializedName("due_date")
    val dueDate: String? = null
)

data class NoteResponse(
    val id: Int,
    val title: String,
    val content: String,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("is_private")
    val isPrivate: Boolean,
    val tags: String,
    @SerializedName("due_date")
    val dueDate: String? = null
)

// Wishes Models
data class WishRequest(
    val title: String,
    val description: String = "",
    val priority: Int = 0,
    @SerializedName("category")
    val category: String = "",
    @SerializedName("due_date")
    val dueDate: String? = null
)

data class WishResponse(
    val id: Int,
    val title: String,
    val description: String,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    @SerializedName("completed_at")
    val completedAt: String? = null,
    val priority: Int,
    val category: String,
    @SerializedName("due_date")
    val dueDate: String? = null
)

// Mood Models
data class MoodRequest(
    @SerializedName("mood_type")
    val moodType: String,
    val date: String,
    val note: String = ""
)

data class MoodResponse(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("mood_type")
    val moodType: String,
    @SerializedName("created_at")
    val timestamp: String = "",
    val date: String,
    val note: String = "",
    val color: String? = null
)

// Activity Models
data class ActivityRequest(
    val title: String,
    val description: String,
    val date: String,
    val category: String = ""
)

data class ActivityResponse(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    val title: String,
    val description: String,
    val timestamp: String,
    val date: String,
    @SerializedName("image_urls")
    val imageUrls: String = "",
    val category: String
)

// Cycle Models
data class CycleRequest(
    @SerializedName("cycle_start_date")
    val cycleStartDate: String,
    @SerializedName("cycle_duration")
    val cycleDuration: Int = 28,
    @SerializedName("period_duration")
    val periodDuration: Int = 5,
    val symptoms: String = "",
    val mood: String = "",
    val notes: String = ""
)

data class CycleResponse(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("cycle_start_date")
    val cycleStartDate: String,
    @SerializedName("cycle_duration")
    val cycleDuration: Int,
    @SerializedName("period_duration")
    val periodDuration: Int,
    @SerializedName("updated_at")
    val lastUpdated: String = "",
    val symptoms: String = "",
    val mood: String = "",
    val notes: String = ""
)

// Custom Calendar Models
data class CustomCalendarRequest(
    val name: String,
    val description: String = "",
    val type: String,
    @SerializedName("color_hex")
    val colorHex: String
)

data class CustomCalendarResponse(
    val id: Int,
    val name: String,
    val description: String = "",
    val type: String,
    @SerializedName("color_hex")
    val colorHex: String,
    val icon: String? = null,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("user_id")
    val userId: Int
)

// Relationship Info Models
data class RelationshipRequest(
    @SerializedName("relationship_start_date")
    val relationshipStartDate: String,
    @SerializedName("first_kiss_date")
    val firstKissDate: String? = null,
    @SerializedName("anniversary_date")
    val anniversaryDate: String? = null,
    @SerializedName("nickname_1")
    val nickname1: String = "",
    @SerializedName("nickname_2")
    val nickname2: String = ""
)

data class RelationshipResponse(
    val id: Int,
    @SerializedName("relationship_start_date")
    val relationshipStartDate: String,
    @SerializedName("first_kiss_date")
    val firstKissDate: String? = null,
    @SerializedName("anniversary_date")
    val anniversaryDate: String? = null,
    @SerializedName("user_id_1")
    val userId1: Int,
    @SerializedName("user_id_2")
    val userId2: Int,
    @SerializedName("nickname_1")
    val nickname1: String,
    @SerializedName("nickname_2")
    val nickname2: String,
    @SerializedName("created_at")
    val createdAt: String
)

// Generic Response Wrapper (matches server sendResponse)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val errors: Map<String, String>? = null
)

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int
)
