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

data class GoogleSignInRequest(
    @SerializedName("id_token")
    val idToken: String
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
    val dueDate: String? = null,
    @SerializedName("is_private")
    val isPrivate: Boolean = false,
    @SerializedName("image_urls")
    val imageUrls: String? = null,
    val emoji: String? = null
)

data class WishResponse(
    val id: Int,
    val title: String,
    val description: String,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    @SerializedName("completed_at")
    val completedAt: String? = null,
    val priority: Int,
    val category: String,
    @SerializedName("is_private")
    val isPrivate: Boolean = false,
    @SerializedName("image_urls")
    val imageUrls: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    val emoji: String? = null
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
    val date: String = "",
    val note: String = "",
    val color: String? = null,
    @SerializedName("display_name")
    val displayName: String? = null
)

// Activity Models
data class ActivityRequest(
    val title: String,
    val description: String,
    val date: String,
    val category: String = "",
    @SerializedName("activity_type")
    val activityType: String = "",
    @SerializedName("duration_minutes")
    val durationMinutes: Int = 0,
    @SerializedName("start_time")
    val startTime: String = "",
    val note: String = ""
)

data class ActivityResponse(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    val title: String,
    val description: String,
    val timestamp: String = "",
    val date: String = "",
    @SerializedName("image_urls")
    val imageUrls: String = "",
    val category: String = "",
    @SerializedName("activity_type")
    val activityType: String = "",
    @SerializedName("duration_minutes")
    val durationMinutes: Int = 0,
    @SerializedName("start_time")
    val startTime: String = "",
    val note: String = "",
    @SerializedName("display_name")
    val displayName: String? = null
)

// Cycle Models
// symptoms: Map<"YYYY-MM-DD", List<"sex"|"pain_mild"|"pain_severe"|"medication"|...>>
// mood:     Map<"YYYY-MM-DD", "happy"|"sad"|"tired"|"anxious">
data class CycleRequest(
    @SerializedName("cycle_start_date")
    val cycleStartDate: String,
    @SerializedName("cycle_duration")
    val cycleDuration: Int = 28,
    @SerializedName("period_duration")
    val periodDuration: Int = 5,
    val symptoms: Map<String, List<String>> = emptyMap(),
    val mood: Map<String, String> = emptyMap(),
    val notes: String = ""
)

data class CyclePatchRequest(
    val date: String,
    @SerializedName("symptoms_day")
    val symptomsDay: List<String>? = null,
    @SerializedName("mood_day")
    val moodDay: String? = null
)

data class CycleResponse(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("cycle_start_date")
    val cycleStartDate: String,
    @SerializedName("cycle_duration")
    val cycleDuration: Int = 28,
    @SerializedName("period_duration")
    val periodDuration: Int = 5,
    @SerializedName("created_at")
    val lastUpdated: String = "",
    val symptoms: Map<String, List<String>> = emptyMap(),
    val mood: Map<String, String> = emptyMap(),
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

// Custom Calendar Event Models
data class CalendarEventRequest(
    @SerializedName("event_date") val eventDate: String,
    val title: String = "",
    val description: String = ""
)

data class CalendarEventResponse(
    val id: Int,
    @SerializedName("calendar_id") val calendarId: Int,
    @SerializedName("event_date") val eventDate: String,
    val title: String = "",
    val description: String = "",
    @SerializedName("created_at") val createdAt: String = ""
)

// Relationship Info Models
data class RelationshipRequest(
    @SerializedName("relationship_start_date")
    val relationshipStartDate: String,
    @SerializedName("first_kiss_date")
    val firstKissDate: String? = null,
    @SerializedName("anniversary_date")
    val anniversaryDate: String? = null,
    @SerializedName("my_birthday")
    val myBirthday: String? = null,
    @SerializedName("partner_birthday")
    val partnerBirthday: String? = null,
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
    @SerializedName("my_birthday")
    val myBirthday: String? = null,
    @SerializedName("partner_birthday")
    val partnerBirthday: String? = null,
    @SerializedName("partner_display_name")
    val partnerDisplayName: String? = null,
    @SerializedName("user_id")
    val userId1: Int = 0,
    @SerializedName("partner_user_id")
    val userId2: Int = 0,
    @SerializedName("nickname_1")
    val nickname1: String = "",
    @SerializedName("nickname_2")
    val nickname2: String = "",
    @SerializedName("created_at")
    val createdAt: String = ""
)

// Upload Response
data class UploadResponse(
    val url: String
)

// Generic Response Wrapper (matches server sendResponse)
// errors typed as Any? to avoid Gson ClassCastException (Map<K,V> loses
// its ParameterizedType after R8 optimisation → Class cast error)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val errors: Any? = null
)

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int
)

// FCM
data class FcmTokenRequest(
    @SerializedName("fcm_token")
    val fcmToken: String
)

// Partner Pairing
data class PairingCodeResponse(
    val code: String,
    @SerializedName("expires_minutes")
    val expiresMinutes: Int
)

data class LinkPartnerRequest(
    val code: String
)

data class LinkPartnerResponse(
    @SerializedName("partner_id")
    val partnerId: Int,
    @SerializedName("partner_name")
    val partnerName: String,
    @SerializedName("partner_username")
    val partnerUsername: String
)
