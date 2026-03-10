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
    val createdAt: String? = null,
    @SerializedName("needs_profile_setup")
    val needsProfileSetup: Boolean = false
)

data class SetupProfileRequest(
    @SerializedName("display_name")
    val displayName: String,
    val gender: String
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
    @SerializedName("profile_image")
    val userAvatar: String? = null,
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
    @SerializedName("profile_image")
    val userAvatar: String? = null,
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
    val displayName: String? = null,
    @SerializedName("profile_image")
    val userAvatar: String? = null
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
    val displayName: String? = null,
    @SerializedName("profile_image")
    val userAvatar: String? = null
)

// Custom Activity Type Models
data class CustomActivityTypeRequest(
    val name: String,
    val emoji: String,
    @SerializedName("color_hex")
    val colorHex: String = "#FF6B9D"
)

data class CustomActivityTypeResponse(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    val name: String,
    val emoji: String,
    @SerializedName("color_hex")
    val colorHex: String = "#FF6B9D",
    @SerializedName("is_mine")
    val isMine: Boolean = true,
    @SerializedName("created_at")
    val createdAt: String = ""
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
    @SerializedName("partner_avatar")
    val partnerAvatar: String? = null,
    @SerializedName("my_avatar")
    val myAvatar: String? = null,
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

// Avatar upload response
data class AvatarUploadResponse(
    @SerializedName("profile_image")
    val profileImage: String
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

// ==================== Art Canvas Models ====================

data class ArtCanvasResponse(
    val id: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    val title: String,
    @SerializedName("created_by")
    val createdBy: Int,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String = "",
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class ArtCanvasRequest(
    val title: String = "Без названия"
)

data class ArtCanvasUpdateRequest(
    val title: String
)

data class ArtCanvasThumbnailResponse(
    val id: Int,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String,
    @SerializedName("updated_at")
    val updatedAt: String = ""
)

// Stroke persistence models
data class CanvasStrokesResponse(
    val strokes: String   // JSON-encoded array of SavedStroke
)

data class CanvasStrokesSaveRequest(
    val strokes: String   // JSON-encoded array of SavedStroke
)

/** Mirror of DrawPoint for JSON serialization (no Compose dependency). */
data class SavedDrawPoint(val x: Float, val y: Float)

/** Serializable representation of one DrawPath stroke. */
data class SavedStroke(
    val color: String,          // hex e.g. "#FF0000"
    val strokeWidth: Float,
    val isFromPartner: Boolean,
    val points: List<SavedDrawPoint>
)

// ==================== Chat Models ====================

data class ChatMessageRequest(
    val content: String,
    @SerializedName("message_type")
    val messageType: String = "text",
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    @SerializedName("audio_duration_seconds")
    val audioDurationSeconds: Int = 0,
    @SerializedName("sticker_id")
    val stickerId: String? = null,
    @SerializedName("sticker_url")
    val stickerUrl: String? = null,
    @SerializedName("is_popup_sticker")
    val isPopupSticker: Boolean = false,
    @SerializedName("drawing_url")
    val drawingUrl: String? = null,
    @SerializedName("video_url")
    val videoUrl: String? = null,
    @SerializedName("video_duration_seconds")
    val videoDurationSeconds: Int = 0,
    @SerializedName("video_thumbnail_url")
    val videoThumbnailUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("location_name")
    val locationName: String? = null,
    @SerializedName("emoji_type")
    val emojiType: String? = null
)

data class ChatMessageResponse(
    val id: Int,
    @SerializedName("sender_id")
    val senderId: Int,
    @SerializedName("receiver_id")
    val receiverId: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("message_type")
    val messageType: String = "text",
    val content: String = "",
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    @SerializedName("audio_duration_seconds")
    val audioDurationSeconds: Int = 0,
    @SerializedName("sticker_id")
    val stickerId: String? = null,
    @SerializedName("sticker_url")
    val stickerUrl: String? = null,
    @SerializedName("is_popup_sticker")
    val isPopupSticker: Boolean = false,
    @SerializedName("drawing_url")
    val drawingUrl: String? = null,
    @SerializedName("video_url")
    val videoUrl: String? = null,
    @SerializedName("video_duration_seconds")
    val videoDurationSeconds: Int = 0,
    @SerializedName("video_thumbnail_url")
    val videoThumbnailUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("location_name")
    val locationName: String? = null,
    @SerializedName("emoji_type")
    val emojiType: String? = null,
    @SerializedName("is_read")
    val isRead: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("sender_name")
    val senderName: String? = null,
    @SerializedName("sender_avatar")
    val senderAvatar: String? = null
)

data class StickerPackResponse(
    val id: Int,
    val code: String,
    val name: String,
    val description: String = "",
    @SerializedName("cover_url")
    val coverUrl: String? = null,
    @SerializedName("is_premium")
    val isPremium: Boolean = false,
    @SerializedName("is_animated")
    val isAnimated: Boolean = false,
    val owned: Boolean = false
)

data class StickerResponse(
    val id: Int,
    @SerializedName("pack_id")
    val packId: Int,
    val code: String,
    val name: String,
    val url: String,
    @SerializedName("is_animated")
    val isAnimated: Boolean = false,
    @SerializedName("is_popup")
    val isPopup: Boolean = false
)

data class MissYouCounterRequest(
    @SerializedName("emoji_type")
    val emojiType: String = "❤️"
)

data class MissYouCounterResponse(
    @SerializedName("myCount")
    val myCount: Int = 0,
    @SerializedName("partnerCount")
    val partnerCount: Int = 0,
    @SerializedName("myTotal")
    val myTotal: Int = 0,
    @SerializedName("partnerTotal")
    val partnerTotal: Int = 0
)

// ==================== Memorial Day Models ====================

data class MemorialDayRequest(
    val title: String,
    val date: String,
    val type: String = "custom",
    val icon: String = "💕",
    @SerializedName("color_hex")
    val colorHex: String = "#FF6B9D",
    @SerializedName("repeat_yearly")
    val repeatYearly: Boolean = true,
    @SerializedName("reminder_days")
    val reminderDays: Int = 1,
    val note: String = ""
)

data class MemorialDayResponse(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    val title: String,
    val date: String,
    val type: String = "custom",
    val icon: String = "💕",
    @SerializedName("color_hex")
    val colorHex: String = "#FF6B9D",
    @SerializedName("repeat_yearly")
    val repeatYearly: Boolean = true,
    @SerializedName("reminder_days")
    val reminderDays: Int = 1,
    val note: String = "",
    @SerializedName("created_at")
    val createdAt: String = ""
)

// ==================== Spark/Streak Models ====================

data class SparkStreakResponse(
    val id: Int? = null,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("current_streak")
    val currentStreak: Int = 0,
    @SerializedName("longest_streak")
    val longestStreak: Int = 0,
    @SerializedName("last_spark_date")
    val lastSparkDate: String? = null,
    @SerializedName("total_sparks")
    val totalSparks: Int = 0
)

data class SparkLogRequest(
    @SerializedName("spark_type")
    val sparkType: String = "manual"
)

data class SparkHistoryItem(
    val date: String,
    @SerializedName("spark_count")
    val sparkCount: Int = 0,
    val participants: Int = 0
)

// ── Mood Analytics ──────────────────────────────────────────────────────────

data class MoodDistributionItem(
    @SerializedName("mood_type")
    val moodType: String,
    val count: Int
)

data class MoodDailyTrendItem(
    val date: String,
    val count: Int,
    @SerializedName("dominant_mood")
    val dominantMood: String? = null
)

data class MoodDayOfWeekItem(
    val dow: Int,             // 0 = Sunday, 6 = Saturday
    @SerializedName("dominant_mood")
    val dominantMood: String? = null,
    val count: Int
)

data class MoodAnalyticsResponse(
    val distribution: List<MoodDistributionItem> = emptyList(),
    @SerializedName("daily_trend")
    val dailyTrend: List<MoodDailyTrendItem> = emptyList(),
    @SerializedName("day_of_week")
    val dayOfWeek: List<MoodDayOfWeekItem> = emptyList(),
    @SerializedName("this_month_count")
    val thisMonthCount: Int = 0,
    @SerializedName("last_month_count")
    val lastMonthCount: Int = 0,
    @SerializedName("month_change_percent")
    val monthChangePercent: Int? = null,
    @SerializedName("mood_streak")
    val moodStreak: Int = 0,
    @SerializedName("most_common_mood")
    val mostCommonMood: String? = null,
    @SerializedName("partner_distribution")
    val partnerDistribution: List<MoodDistributionItem> = emptyList()
)

// ── Spark Breakdown ─────────────────────────────────────────────────────────

data class SparkTypeCount(
    @SerializedName("spark_type")
    val sparkType: String,
    val count: Int
)

data class SparkWeeklyItem(
    @SerializedName("week_start")
    val weekStart: String,
    @SerializedName("active_days")
    val activeDays: Int = 0,
    @SerializedName("total_sparks")
    val totalSparks: Int = 0,
    val types: List<String> = emptyList()
)

data class SparkBreakdownResponse(
    val types: List<SparkTypeCount> = emptyList(),
    val weekly: List<SparkWeeklyItem> = emptyList()
)

// ── Game Compatibility ──────────────────────────────────────────────────────

data class CompatibilityHistoryItem(
    val id: Int,
    @SerializedName("game_type")
    val gameType: String,
    @SerializedName("compatibility_score")
    val compatibilityScore: Int? = null,
    @SerializedName("finished_at")
    val finishedAt: String? = null
)

data class CompatibilityByTypeItem(
    @SerializedName("game_type")
    val gameType: String,
    val average: Int = 0,
    @SerializedName("games_played")
    val gamesPlayed: Int = 0
)

data class CompatibilityStatsResponse(
    val average: Int? = null,
    @SerializedName("total_games")
    val totalGames: Int = 0,
    @SerializedName("best_score")
    val bestScore: Int? = null,
    @SerializedName("worst_score")
    val worstScore: Int? = null,
    val history: List<CompatibilityHistoryItem> = emptyList(),
    @SerializedName("by_type")
    val byType: List<CompatibilityByTypeItem> = emptyList()
)

// ==================== Task Models ====================

data class CoupleTaskRequest(
    val title: String,
    val description: String = "",
    val category: String = "custom",
    val icon: String = "💕",
    val points: Int = 10,
    @SerializedName("due_date")
    val dueDate: String? = null
)

data class CoupleTaskResponse(
    val id: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("user_id")
    val userId: Int,
    val title: String,
    val description: String = "",
    val category: String = "daily",
    val icon: String = "💕",
    val points: Int = 10,
    @SerializedName("is_completed")
    val isCompleted: Boolean = false,
    @SerializedName("completed_by")
    val completedBy: Int? = null,
    @SerializedName("completed_at")
    val completedAt: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    @SerializedName("is_system")
    val isSystem: Boolean = false,
    @SerializedName("creator_name")
    val creatorName: String? = null,
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class TasksListResponse(
    val items: List<CoupleTaskResponse>,
    val total: Int,
    @SerializedName("total_points")
    val totalPoints: Int = 0
)

// ==================== Sleep Models ====================

data class SleepEntryRequest(
    val date: String,
    val bedtime: String? = null,
    @SerializedName("wake_time")
    val wakeTime: String? = null,
    @SerializedName("duration_minutes")
    val durationMinutes: Int? = null,
    val quality: Int? = null,
    val note: String = ""
)

data class SleepEntryResponse(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    val date: String,
    val bedtime: String? = null,
    @SerializedName("wake_time")
    val wakeTime: String? = null,
    @SerializedName("duration_minutes")
    val durationMinutes: Int? = null,
    val quality: Int? = null,
    val note: String = "",
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class SleepStatsResponse(
    @SerializedName("avg_duration")
    val avgDuration: Int? = null,
    @SerializedName("avg_quality")
    val avgQuality: Float? = null,
    @SerializedName("min_duration")
    val minDuration: Int? = null,
    @SerializedName("max_duration")
    val maxDuration: Int? = null
)

data class SleepPartnerResponse(
    val items: List<SleepEntryResponse>
)

// ==================== Gallery Models ====================

data class GalleryPhotoRequest(
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,
    val caption: String = "",
    val width: Int = 0,
    val height: Int = 0
)

data class GalleryPhotoResponse(
    val id: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,
    val caption: String = "",
    val width: Int = 0,
    val height: Int = 0,
    @SerializedName("uploader_name")
    val uploaderName: String? = null,
    @SerializedName("uploader_avatar")
    val uploaderAvatar: String? = null,
    @SerializedName("created_at")
    val createdAt: String = ""
)

// ==================== Love Touch Models ====================

data class LoveTouchSessionResponse(
    val id: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("started_by")
    val startedBy: Int,
    @SerializedName("partner_joined")
    val partnerJoined: Boolean = false,
    @SerializedName("hearts_count")
    val heartsCount: Int = 0,
    @SerializedName("ended_at")
    val endedAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String = ""
)

// ==================== Miss You Models ====================

data class MissYouRequest(
    val emoji: String = "❤\uFE0F",
    val message: String = ""
)

data class MissYouResponse(
    val id: Int,
    @SerializedName("sender_id")
    val senderId: Int,
    @SerializedName("receiver_id")
    val receiverId: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    val emoji: String = "❤\uFE0F",
    val message: String = "",
    @SerializedName("sender_name")
    val senderName: String? = null,
    @SerializedName("sender_avatar")
    val senderAvatar: String? = null,
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class MissYouTodayResponse(
    val count: Int = 0,
    val sentByMe: Int = 0,
    val sentByPartner: Int = 0
)

// ==================== App Lock Models ====================

data class AppLockSetRequest(
    val pin: String,
    @SerializedName("is_biometric")
    val isBiometric: Boolean = false
)

data class AppLockVerifyRequest(
    val pin: String
)

data class AppLockUpdateRequest(
    @SerializedName("current_pin")
    val currentPin: String,
    @SerializedName("new_pin")
    val newPin: String? = null,
    @SerializedName("is_biometric")
    val isBiometric: Boolean? = null
)

data class AppLockDeleteRequest(
    val pin: String
)

data class AppLockStatusResponse(
    val enabled: Boolean = false,
    val isBiometric: Boolean = false
)

data class AppLockVerifyResponse(
    val valid: Boolean = false
)

// ==================== Chat Settings Models ====================

data class ChatSettingsResponse(
    @SerializedName("wallpaper_url")
    val wallpaperUrl: String? = null,
    @SerializedName("bubble_color")
    val bubbleColor: String? = null,
    @SerializedName("bubble_shape")
    val bubbleShape: String? = null
)

data class ChatSettingsRequest(
    @SerializedName("wallpaper_url")
    val wallpaperUrl: String? = null,
    @SerializedName("bubble_color")
    val bubbleColor: String? = null,
    @SerializedName("bubble_shape")
    val bubbleShape: String? = null
)

data class WallpaperItem(
    val id: String,
    val name: String,
    val url: String,
    val preview: String
)

// ==================== Daily Q&A Models ====================

data class DailyQuestionResponse(
    val id: Int,
    @SerializedName("question_text")
    val questionText: String,
    val category: String = "",
    val options: List<String>? = null
)

data class DailyAnswerRequest(
    val answer: String
)

data class DailyAnswerResponse(
    val id: Int,
    @SerializedName("question_id")
    val questionId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    val answer: String,
    val date: String,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("profile_image")
    val profileImage: String? = null,
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class DailyQATodayResponse(
    val question: DailyQuestionResponse,
    val myAnswer: DailyAnswerResponse? = null,
    val partnerAnswer: DailyAnswerResponse? = null,
    val bothAnswered: Boolean = false
)

data class DailyQAHistoryItem(
    @SerializedName("question_id")
    val questionId: Int,
    @SerializedName("question_text")
    val questionText: String,
    val category: String = "",
    val options: List<String>? = null,
    @SerializedName("my_answer")
    val myAnswer: String? = null,
    @SerializedName("partner_answer")
    val partnerAnswer: String? = null,
    val date: String = ""
)

// ==================== Intimacy Score Models ====================

data class IntimacyScoreResponse(
    val score: Int = 0,
    val level: Int = 1,
    val name: String = "",
    @SerializedName("nextLevel")
    val nextLevel: IntimacyNextLevel? = null,
    val levels: List<IntimacyLevelDef> = emptyList(),
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class IntimacyNextLevel(
    val level: Int,
    val name: String,
    @SerializedName("pointsNeeded")
    val pointsNeeded: Int
)

data class IntimacyLevelDef(
    val level: Int,
    val name: String,
    @SerializedName("minScore")
    val minScore: Int
)

data class IntimacyLogItem(
    val id: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("action_type")
    val actionType: String,
    val points: Int,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("profile_image")
    val profileImage: String? = null,
    @SerializedName("created_at")
    val createdAt: String = ""
)

// ==================== GIF Models ====================

data class GifItem(
    val id: String,
    val title: String = "",
    val url: String,
    val preview: String,
    val width: Int = 0,
    val height: Int = 0
)

data class GifSearchResult(
    val items: List<GifItem> = emptyList(),
    val next: String = ""
)

// ==================== Moments (ShareNow) Models ====================

data class MomentRequest(
    val content: String = "",
    @SerializedName("image_url")
    val imageUrl: String? = null,
    val mood: String? = null,
    @SerializedName("location_name")
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class MomentResponse(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    val content: String = "",
    @SerializedName("image_url")
    val imageUrl: String? = null,
    val mood: String = "",
    @SerializedName("location_name")
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("profile_image")
    val profileImage: String? = null,
    @SerializedName("created_at")
    val createdAt: String = ""
)

// ==================== Common Places Models ====================

data class PlaceRequest(
    val name: String,
    val address: String? = null,
    val category: String = "other",
    val note: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null
)

data class PlaceResponse(
    val id: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("user_id")
    val userId: Int,
    val name: String,
    val address: String = "",
    val category: String = "other",
    val note: String = "",
    val latitude: Double,
    val longitude: Double,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("profile_image")
    val profileImage: String? = null,
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class PlaceListResponse(
    val items: List<PlaceResponse> = emptyList(),
    val categories: List<String> = emptyList()
)

// ==================== Real-time Location Tracking Models ====================

data class LocationUpdateRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val altitude: Double? = null,
    @SerializedName("battery_level")
    val battery_level: Int? = null,
    @SerializedName("is_charging")
    val is_charging: Boolean = false,
    @SerializedName("activity_type")
    val activity_type: String = "unknown",
    @SerializedName("recorded_at")
    val recorded_at: String? = null
)

data class LocationPointResponse(
    val id: Long? = null,
    @SerializedName("user_id")
    val userId: Int = 0,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val altitude: Double? = null,
    @SerializedName("battery_level")
    val batteryLevel: Int? = null,
    @SerializedName("is_charging")
    val isCharging: Boolean = false,
    @SerializedName("activity_type")
    val activityType: String = "unknown",
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("profile_image")
    val profileImage: String? = null,
    @SerializedName("recorded_at")
    val recordedAt: String = ""
)

data class LatestLocationsResponse(
    val self: LocationPointResponse? = null,
    val partner: LocationPointResponse? = null
)

data class LocationHistoryResponse(
    val points: List<LocationPointResponse> = emptyList(),
    @SerializedName("user_id")
    val userId: Int? = null
)

data class LocationStatsResponse(
    @SerializedName("distance_km")
    val distanceKm: Double? = null,
    @SerializedName("partner_speed")
    val partnerSpeed: Float? = null,
    @SerializedName("partner_avg_speed")
    val partnerAvgSpeed: Float? = null,
    @SerializedName("partner_max_speed")
    val partnerMaxSpeed: Float? = null
)

data class LocationSettingsResponse(
    @SerializedName("sharing_enabled")
    val sharingEnabled: Boolean = true,
    @SerializedName("update_interval_sec")
    val updateIntervalSec: Int = 60,
    @SerializedName("show_speed")
    val showSpeed: Boolean = true,
    @SerializedName("show_battery")
    val showBattery: Boolean = true,
    @SerializedName("show_distance")
    val showDistance: Boolean = true
)

data class LocationSettingsRequest(
    @SerializedName("sharing_enabled")
    val sharingEnabled: Boolean? = null,
    @SerializedName("update_interval_sec")
    val updateIntervalSec: Int? = null,
    @SerializedName("show_speed")
    val showSpeed: Boolean? = null,
    @SerializedName("show_battery")
    val showBattery: Boolean? = null,
    @SerializedName("show_distance")
    val showDistance: Boolean? = null
)

// ==================== Phase 5: Virtual Pet Models (Overhauled) ====================

data class PetResponse(
    val id: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    val name: String = "Котик",
    @SerializedName("pet_type")
    val petType: String = "cat",
    val level: Int = 1,
    val xp: Int = 0,
    val hunger: Int = 80,
    val happiness: Int = 80,
    val cleanliness: Int = 80,
    val energy: Int = 100,
    val mood: String = "normal",
    val coins: Int = 0,
    @SerializedName("max_level")
    val maxLevel: Int = 60,
    @SerializedName("checkin_streak")
    val checkinStreak: Int = 0,
    @SerializedName("total_checkins")
    val totalCheckins: Int = 0,
    @SerializedName("adventure_count")
    val adventureCount: Int = 0,
    @SerializedName("last_checkin")
    val lastCheckin: String? = null,
    @SerializedName("last_fed")
    val lastFed: String? = null,
    @SerializedName("last_played")
    val lastPlayed: String? = null,
    @SerializedName("last_cleaned")
    val lastCleaned: String? = null,
    @SerializedName("last_decay")
    val lastDecay: String? = null,
    @SerializedName("created_at")
    val createdAt: String = "",
    // Enriched fields from backend
    @SerializedName("xp_for_next_level")
    val xpForNextLevel: Int = 100,
    @SerializedName("xp_in_current_level")
    val xpInCurrentLevel: Int = 0,
    @SerializedName("xp_needed")
    val xpNeeded: Int = 100,
    @SerializedName("xp_progress")
    val xpProgress: Float = 0f,
    @SerializedName("is_max_level")
    val isMaxLevel: Boolean = false
)

data class PetUpdateRequest(
    val name: String? = null,
    @SerializedName("pet_type")
    val petType: String? = null
)

data class PetActionResponse(
    val id: Long,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("action_type")
    val actionType: String,
    @SerializedName("stat_delta")
    val statDelta: Int = 0,
    @SerializedName("xp_gained")
    val xpGained: Int = 0,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class CareActionResult(
    val pet: PetResponse,
    @SerializedName("level_reward")
    val levelReward: LevelReward? = null
)

data class LevelReward(
    val id: Int = 0,
    val level: Int = 0,
    @SerializedName("reward_type")
    val rewardType: String = "coins",
    @SerializedName("reward_amount")
    val rewardAmount: Int = 0,
    val description: String = ""
)

data class PetTypeInfo(
    val id: Int,
    val code: String,
    val name: String,
    val emoji: String = "🐾",
    val rarity: String = "common",
    val description: String = "",
    @SerializedName("unlock_level")
    val unlockLevel: Int = 1
)

data class PetEgg(
    val id: Long,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("pet_type_code")
    val petTypeCode: String,
    val rarity: String = "common",
    @SerializedName("is_hatched")
    val isHatched: Boolean = false,
    @SerializedName("pet_name")
    val petName: String? = null,
    val emoji: String? = null,
    val source: String = "spin",
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class EggHatchResult(
    @SerializedName("pet_type_code")
    val petTypeCode: String,
    val rarity: String,
    @SerializedName("is_duplicate")
    val isDuplicate: Boolean = false,
    @SerializedName("coins_gained")
    val coinsGained: Int = 0
)

data class FurnitureItem(
    val id: Int,
    val code: String,
    val name: String,
    val emoji: String = "🪑",
    val category: String = "decor",
    val rarity: String = "common",
    @SerializedName("price_coins")
    val priceCoins: Int = 50,
    @SerializedName("happiness_bonus")
    val happinessBonus: Int = 0,
    val description: String = ""
)

data class OwnedFurniture(
    val id: Long,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("furniture_id")
    val furnitureId: Int,
    val code: String = "",
    val name: String = "",
    val emoji: String = "🪑",
    val category: String = "decor",
    @SerializedName("happiness_bonus")
    val happinessBonus: Int = 0,
    @SerializedName("is_placed")
    val isPlaced: Boolean = false,
    @SerializedName("position_x")
    val positionX: Int = 0,
    @SerializedName("position_y")
    val positionY: Int = 0
)

data class FurniturePlaceRequest(
    @SerializedName("is_placed")
    val isPlaced: Boolean,
    @SerializedName("position_x")
    val positionX: Int = 0,
    @SerializedName("position_y")
    val positionY: Int = 0
)

data class AdventureInfo(
    val id: Int,
    val code: String,
    val name: String,
    val emoji: String = "🗺️",
    val description: String = "",
    @SerializedName("duration_minutes")
    val durationMinutes: Int = 60,
    @SerializedName("min_level")
    val minLevel: Int = 1,
    @SerializedName("xp_reward")
    val xpReward: Int = 20,
    @SerializedName("coin_reward")
    val coinReward: Int = 10,
    @SerializedName("energy_cost")
    val energyCost: Int = 30,
    val unlocked: Boolean = true
)

data class ActiveAdventure(
    val id: Long,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("adventure_id")
    val adventureId: Int,
    val code: String = "",
    val name: String = "",
    val emoji: String = "🗺️",
    @SerializedName("started_at")
    val startedAt: String = "",
    @SerializedName("ends_at")
    val endsAt: String = "",
    @SerializedName("is_completed")
    val isCompleted: Boolean = false
)

data class AdventuresData(
    val available: List<AdventureInfo> = emptyList(),
    val active: List<ActiveAdventure> = emptyList(),
    val energy: Int = 100
)

data class AdventureStartResult(
    val id: Long = 0,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("ends_at")
    val endsAt: String = "",
    val adventure: AdventureInfo? = null
)

data class AdventureClaimResult(
    @SerializedName("xp_gained")
    val xpGained: Int = 0,
    @SerializedName("coins_gained")
    val coinsGained: Int = 0,
    @SerializedName("new_level")
    val newLevel: Int = 0,
    val adventure: AdventureClaimAdventure? = null
)

data class AdventureClaimAdventure(
    val code: String = "",
    val name: String = ""
)

data class PetWish(
    val id: Long,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("wish_text")
    val wishText: String,
    val emoji: String = "⭐",
    @SerializedName("is_fulfilled")
    val isFulfilled: Boolean = false,
    @SerializedName("fulfilled_by")
    val fulfilledBy: Int? = null,
    @SerializedName("fulfilled_by_name")
    val fulfilledByName: String? = null,
    @SerializedName("xp_reward")
    val xpReward: Int = 15,
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class PetWishRequest(
    @SerializedName("wish_text")
    val wishText: String,
    val emoji: String = "⭐"
)

data class WishFulfillResult(
    @SerializedName("xp_gained")
    val xpGained: Int = 0
)

data class CheckinStatus(
    @SerializedName("checked_in_today")
    val checkedInToday: Boolean = false,
    val streak: Int = 0,
    @SerializedName("total_checkins")
    val totalCheckins: Int = 0,
    @SerializedName("last_checkin")
    val lastCheckin: String? = null
)

data class CheckinResult(
    val streak: Int = 0,
    @SerializedName("coins_earned")
    val coinsEarned: Int = 0,
    @SerializedName("xp_earned")
    val xpEarned: Int = 0,
    @SerializedName("total_checkins")
    val totalCheckins: Int = 0
)

data class SpinResult(
    @SerializedName("reward_type")
    val rewardType: String,
    @SerializedName("reward_amount")
    val rewardAmount: Int = 0,
    @SerializedName("reward_detail")
    val rewardDetail: String = ""
)

data class PetCollectionItem(
    val id: Long,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("collection_type")
    val collectionType: String,
    @SerializedName("item_code")
    val itemCode: String,
    @SerializedName("item_name")
    val itemName: String,
    @SerializedName("item_emoji")
    val itemEmoji: String = "🏅",
    @SerializedName("unlocked_at")
    val unlockedAt: String = ""
)

data class CollectionsData(
    val collections: Map<String, List<PetCollectionItem>> = emptyMap(),
    val total: Int = 0
)

data class PassportStats(
    @SerializedName("total_checkins")
    val totalCheckins: Int = 0,
    @SerializedName("checkin_streak")
    val checkinStreak: Int = 0,
    @SerializedName("adventure_count")
    val adventureCount: Int = 0,
    val coins: Int = 0
)

data class PassportCollections(
    val pets: PassportCount = PassportCount(),
    val furniture: PassportCount = PassportCount(),
    val adventures: PassportCount = PassportCount()
)

data class PassportCount(
    val unlocked: Int = 0,
    val owned: Int = 0,
    val completed: Int = 0,
    val total: Int = 0
)

data class PassportEggs(
    val total: Int = 0,
    val hatched: Int = 0
)

data class PassportWishes(
    val total: Int = 0,
    val fulfilled: Int = 0
)

data class PetPassport(
    val pet: PetResponse? = null,
    val stats: PassportStats = PassportStats(),
    val collections: PassportCollections = PassportCollections(),
    val eggs: PassportEggs = PassportEggs(),
    val wishes: PassportWishes = PassportWishes()
)

// ==================== Gold Coin Economy Models ====================

data class CoinBalanceResponse(
    val coins: Int = 0,
    @SerializedName("premiumMultiplier")
    val premiumMultiplier: Float = 1.0f
)

data class ShopItem(
    val id: Int,
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "general",
    val icon: String = "⭐",
    @SerializedName("price_coins")
    val priceCoins: Int = 0,
    val type: String = "consumable",
    @SerializedName("effect_type")
    val effectType: String? = null,
    @SerializedName("effect_amount")
    val effectAmount: Int? = null,
    @SerializedName("is_premium_only")
    val isPremiumOnly: Boolean = false,
    @SerializedName("sort_order")
    val sortOrder: Int = 0
)

data class ShopPurchaseResult(
    val success: Boolean = false,
    val balance: Int = 0,
    val item: ShopItem? = null,
    val effect: ShopEffect? = null
)

data class ShopEffect(
    val type: String = "",
    val amount: Int = 0
)

data class DailyDeal(
    val id: Int,
    @SerializedName("item_code")
    val itemCode: String = "",
    @SerializedName("item_name")
    val itemName: String = "",
    val description: String = "",
    @SerializedName("original_price")
    val originalPrice: Int = 0,
    @SerializedName("deal_price")
    val dealPrice: Int = 0,
    @SerializedName("discount_percent")
    val discountPercent: Int = 0,
    val icon: String = "🎁",
    @SerializedName("valid_until")
    val validUntil: String = "",
    @SerializedName("already_purchased")
    val alreadyPurchased: Boolean = false,
    @SerializedName("redeemed_count")
    val redeemedCount: Int = 0,
    @SerializedName("total_available")
    val totalAvailable: Int = -1
)

data class DealPurchaseResult(
    val success: Boolean = false,
    val balance: Int = 0,
    val deal: DailyDeal? = null
)

data class DailyMission(
    val id: Int,
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "🎯",
    @SerializedName("mission_type")
    val missionType: String = "",
    @SerializedName("target_count")
    val targetCount: Int = 1,
    @SerializedName("reward_coins")
    val rewardCoins: Int = 0,
    @SerializedName("reward_xp")
    val rewardXp: Int = 0,
    @SerializedName("current_count")
    val currentCount: Int = 0,
    @SerializedName("is_completed")
    val isCompleted: Boolean = false,
    @SerializedName("is_claimed")
    val isClaimed: Boolean = false
)

data class MissionProgressRequest(
    @SerializedName("missionType")
    val missionType: String
)

data class MissionProgressResult(
    val updated: Boolean = false,
    @SerializedName("mission_id")
    val missionId: Int? = null,
    @SerializedName("current_count")
    val currentCount: Int = 0,
    val target: Int = 0
)

data class MissionClaimResult(
    val success: Boolean = false,
    @SerializedName("coinsEarned")
    val coinsEarned: Int = 0,
    @SerializedName("xpEarned")
    val xpEarned: Int = 0,
    val balance: Int = 0
)

data class CoinTransaction(
    val id: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("tx_type")
    val txType: String = "",
    val amount: Int = 0,
    @SerializedName("balance_before")
    val balanceBefore: Int = 0,
    @SerializedName("balance_after")
    val balanceAfter: Int = 0,
    @SerializedName("ref_type")
    val refType: String? = null,
    val description: String? = null,
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class TransactionsResponse(
    val transactions: List<CoinTransaction> = emptyList(),
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0
)

data class EconomySummary(
    val coins: Int = 0,
    @SerializedName("premiumMultiplier")
    val premiumMultiplier: Float = 1.0f,
    @SerializedName("todayEarned")
    val todayEarned: Int = 0,
    @SerializedName("todaySpent")
    val todaySpent: Int = 0,
    @SerializedName("totalEarned")
    val totalEarned: Int = 0,
    @SerializedName("missionsCompletedToday")
    val missionsCompletedToday: Int = 0,
    @SerializedName("activeDealsCount")
    val activeDealsCount: Int = 0
)

// ==================== Phase 5: Couple Games Models ====================

data class GameSessionResponse(
    val id: Int,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("game_type")
    val gameType: String,
    val status: String = "active",
    @SerializedName("current_round")
    val currentRound: Int = 1,
    @SerializedName("player1_id")
    val player1Id: Int,
    @SerializedName("player2_id")
    val player2Id: Int? = null,
    @SerializedName("player1_score")
    val player1Score: Int = 0,
    @SerializedName("player2_score")
    val player2Score: Int = 0,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("finished_at")
    val finishedAt: String? = null,
    val rounds: List<GameRoundResponse>? = null,
    @SerializedName("compatibility_score")
    val compatibilityScore: Int? = null
)

data class GameRoundResponse(
    val id: Long,
    @SerializedName("session_id")
    val sessionId: Int,
    @SerializedName("round_number")
    val roundNumber: Int,
    @SerializedName("question_text")
    val questionText: String? = null,
    @SerializedName("option_a")
    val optionA: String? = null,
    @SerializedName("option_b")
    val optionB: String? = null,
    @SerializedName("player1_answer")
    val player1Answer: String? = null,
    @SerializedName("player2_answer")
    val player2Answer: String? = null,
    @SerializedName("is_match")
    val isMatch: Boolean? = null
)

data class GameStartRequest(
    @SerializedName("game_type")
    val gameType: String
)

data class GameAnswerRequest(
    @SerializedName("session_id")
    val sessionId: Int,
    @SerializedName("round_number")
    val roundNumber: Int,
    val answer: String
)

data class GameListResponse(
    val items: List<GameSessionResponse> = emptyList()
)

// ==================== Phase 5: Widget Summary Models ====================

data class WidgetSummaryResponse(
    @SerializedName("partner_name")
    val partnerName: String? = null,
    @SerializedName("partner_avatar")
    val partnerAvatar: String? = null,
    @SerializedName("days_together")
    val daysTogether: Int? = null,
    @SerializedName("next_memorial")
    val nextMemorial: WidgetMemorial? = null,
    val streak: Int? = null,
    val pet: WidgetPetMood? = null
)

data class WidgetMemorial(
    val title: String,
    val date: String,
    val icon: String? = null,
    @SerializedName("days_left")
    val daysLeft: Int
)

data class WidgetPetMood(
    val name: String,
    val type: String,
    val level: Int,
    val mood: String,
    @SerializedName("avg_stat")
    val avgStat: Int
)

// ==================== Phase 6: Achievements Models ====================

data class AchievementResponse(
    val id: Int,
    val code: String,
    val title: String,
    val description: String = "",
    val icon: String = "🏆",
    val category: String = "general",
    val threshold: Int = 1,
    @SerializedName("xp_reward")
    val xpReward: Int = 10,
    val unlocked: Boolean = false,
    @SerializedName("unlocked_at")
    val unlockedAt: String? = null
)

data class AchievementListResponse(
    val items: List<AchievementResponse> = emptyList()
)

data class AchievementProgressResponse(
    @SerializedName("chat_messages")
    val chatMessages: Int = 0,
    @SerializedName("current_streak")
    val currentStreak: Int = 0,
    val notes: Int = 0,
    val wishes: Int = 0,
    @SerializedName("wishes_completed")
    val wishesCompleted: Int = 0,
    val moods: Int = 0,
    val tasks: Int = 0,
    @SerializedName("tasks_completed")
    val tasksCompleted: Int = 0,
    @SerializedName("gallery_photos")
    val galleryPhotos: Int = 0,
    @SerializedName("memorial_days")
    val memorialDays: Int = 0,
    @SerializedName("pet_level")
    val petLevel: Int = 0,
    @SerializedName("games_played")
    val gamesPlayed: Int = 0,
    @SerializedName("games_matched")
    val gamesMatched: Int = 0,
    @SerializedName("letters_sent")
    val lettersSent: Int = 0,
    @SerializedName("bucket_items")
    val bucketItems: Int = 0,
    @SerializedName("bucket_completed")
    val bucketCompleted: Int = 0,
    @SerializedName("intimacy_score")
    val intimacyScore: Int = 0,
    @SerializedName("miss_you_sent")
    val missYouSent: Int = 0,
    val moments: Int = 0,
    val places: Int = 0
)

data class AchievementCheckResponse(
    @SerializedName("newly_unlocked")
    val newlyUnlocked: List<AchievementResponse> = emptyList(),
    val count: Int = 0
)

// ==================== Phase 6: Love Letters Models ====================

data class LoveLetterRequest(
    val title: String = "",
    val content: String,
    val mood: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("open_date")
    val openDate: String
)

data class LoveLetterResponse(
    val id: Long,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("sender_id")
    val senderId: Int,
    @SerializedName("receiver_id")
    val receiverId: Int,
    val title: String = "",
    val content: String? = null,
    val mood: String = "",
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("open_date")
    val openDate: String,
    @SerializedName("is_opened")
    val isOpened: Boolean = false,
    @SerializedName("opened_at")
    val openedAt: String? = null,
    @SerializedName("can_open")
    val canOpen: Boolean = false,
    @SerializedName("days_until_open")
    val daysUntilOpen: Int? = null,
    @SerializedName("sender_name")
    val senderName: String? = null,
    @SerializedName("sender_avatar")
    val senderAvatar: String? = null,
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class LoveLetterListResponse(
    val items: List<LoveLetterResponse> = emptyList()
)

data class LoveLetterStatsResponse(
    val total: Int = 0,
    val opened: Int = 0,
    @SerializedName("ready_to_open")
    val readyToOpen: Int = 0,
    val sealed: Int = 0
)

// ==================== Phase 6: Bucket List Models ====================

data class BucketItemRequest(
    val title: String,
    val description: String? = null,
    val category: String = "other",
    val emoji: String = "✨",
    @SerializedName("target_date")
    val targetDate: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null
)

data class BucketItemResponse(
    val id: Long,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("user_id")
    val userId: Int,
    val title: String,
    val description: String = "",
    val category: String = "other",
    val emoji: String = "✨",
    @SerializedName("is_completed")
    val isCompleted: Boolean = false,
    @SerializedName("completed_at")
    val completedAt: String? = null,
    @SerializedName("completed_by")
    val completedBy: Int? = null,
    @SerializedName("target_date")
    val targetDate: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("created_by_name")
    val createdByName: String? = null,
    @SerializedName("completed_by_name")
    val completedByName: String? = null,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("updated_at")
    val updatedAt: String = ""
)

data class BucketListStatsResponse(
    val total: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0
)

data class BucketListResponse(
    val items: List<BucketItemResponse> = emptyList(),
    val stats: BucketListStatsResponse? = null,
    val categories: List<String> = emptyList()
)

// ══════════════════════════ TIM (Tencent IM) ══════════════════════════

data class TIMUserSigResponse(
    @SerializedName("sdkAppId") val sdkAppId: Int,
    @SerializedName("userId")   val userId: String,
    @SerializedName("userSig")  val userSig: String,
    @SerializedName("expireIn") val expireIn: Long
)

// ══════════════════════════ Phase 8: Premium / VIP ══════════════════════════

data class SubscriptionPlanResponse(
    val id: Int,
    val title: String,
    val description: String = "",
    @SerializedName("plan_type")
    val planType: String,
    @SerializedName("google_play_id")
    val googlePlayId: String? = null,
    @SerializedName("price_cents")
    val priceCents: Int = 0,
    val currency: String = "RUB",
    @SerializedName("duration_months")
    val durationMonths: Int? = null,
    val features: List<String> = emptyList()
)

data class SubscriptionPlanListResponse(
    val items: List<SubscriptionPlanResponse> = emptyList()
)

data class PremiumStatusResponse(
    @SerializedName("is_premium")
    val isPremium: Boolean = false,
    val plan: Int? = null,
    @SerializedName("plan_title")
    val planTitle: String? = null,
    @SerializedName("expires_at")
    val expiresAt: String? = null,
    @SerializedName("google_play_id")
    val googlePlayId: String? = null,
    @SerializedName("auto_renew")
    val autoRenew: Boolean = false
)

data class PurchaseVerifyRequest(
    @SerializedName("purchase_token")
    val purchaseToken: String,
    @SerializedName("product_id")
    val productId: String,
    @SerializedName("order_id")
    val orderId: String? = null
)

data class PurchaseRestoreRequest(
    @SerializedName("purchase_token")
    val purchaseToken: String,
    @SerializedName("product_id")
    val productId: String
)

// ══════════════════════════ Phase 8: Love Story ══════════════════════════

data class StoryEntryRequest(
    val title: String,
    val content: String = "",
    @SerializedName("entry_type")
    val entryType: String = "text",
    @SerializedName("entry_date")
    val entryDate: String? = null,
    @SerializedName("media_url")
    val mediaUrl: String? = null,
    val emoji: String = "❤️"
)

data class StoryEntryResponse(
    val id: Long,
    @SerializedName("couple_key")
    val coupleKey: String = "",
    @SerializedName("author_id")
    val authorId: Int,
    val title: String,
    val content: String = "",
    @SerializedName("entry_type")
    val entryType: String = "text",
    @SerializedName("entry_date")
    val entryDate: String,
    @SerializedName("media_url")
    val mediaUrl: String? = null,
    val emoji: String = "❤️",
    @SerializedName("author_name")
    val authorName: String? = null,
    @SerializedName("author_avatar")
    val authorAvatar: String? = null,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("updated_at")
    val updatedAt: String = ""
)

data class StoryListResponse(
    val items: List<StoryEntryResponse> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerializedName("page_size")
    val pageSize: Int = 20
)

data class StoryStatsResponse(
    @SerializedName("total_entries")
    val totalEntries: Int = 0,
    @SerializedName("photo_entries")
    val photoEntries: Int = 0,
    val milestones: Int = 0,
    @SerializedName("first_entry_date")
    val firstEntryDate: String? = null,
    @SerializedName("last_entry_date")
    val lastEntryDate: String? = null
)

// ==================== Phase 10: Geofences Models ====================

data class GeofenceRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("radius_meters")
    val radiusMeters: Int = 200,
    val category: String = "other",
    val address: String? = null,
    val icon: String = "\uD83D\uDCCD",
    val color: String = "#26A69A",
    @SerializedName("notify_on_enter")
    val notifyOnEnter: Boolean = true,
    @SerializedName("notify_on_exit")
    val notifyOnExit: Boolean = true
)

data class GeofenceResponse(
    val id: Long = 0,
    @SerializedName("user_id")
    val userId: Int = 0,
    @SerializedName("couple_key")
    val coupleKey: String? = null,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerializedName("radius_meters")
    val radiusMeters: Int = 200,
    val category: String = "other",
    val address: String? = null,
    val icon: String = "\uD83D\uDCCD",
    val color: String = "#26A69A",
    @SerializedName("notify_on_enter")
    val notifyOnEnter: Boolean = true,
    @SerializedName("notify_on_exit")
    val notifyOnExit: Boolean = true,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("creator_name")
    val creatorName: String? = null,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("updated_at")
    val updatedAt: String = ""
)

data class GeofenceUpdateRequest(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("radius_meters")
    val radiusMeters: Int? = null,
    val category: String? = null,
    val address: String? = null,
    val icon: String? = null,
    val color: String? = null,
    @SerializedName("notify_on_enter")
    val notifyOnEnter: Boolean? = null,
    @SerializedName("notify_on_exit")
    val notifyOnExit: Boolean? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null
)

data class GeofenceEventRequest(
    @SerializedName("geofence_id")
    val geofenceId: Long,
    @SerializedName("event_type")
    val eventType: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class GeofenceEventResponse(
    val id: Long = 0,
    @SerializedName("geofence_id")
    val geofenceId: Long = 0,
    @SerializedName("user_id")
    val userId: Int = 0,
    @SerializedName("event_type")
    val eventType: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("geofence_name")
    val geofenceName: String? = null,
    @SerializedName("geofence_icon")
    val geofenceIcon: String? = null,
    @SerializedName("geofence_category")
    val geofenceCategory: String? = null,
    @SerializedName("triggered_at")
    val triggeredAt: String = ""
)

// ═══ Phase 11: Phone Status Models ═══════════════════════════════════════════

data class PhoneStatusUpdateRequest(
    @SerializedName("battery_level")
    val batteryLevel: Int? = null,
    @SerializedName("is_charging")
    val isCharging: Boolean = false,
    @SerializedName("screen_status")
    val screenStatus: String = "off",
    @SerializedName("wifi_name")
    val wifiName: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = false,
    @SerializedName("network_type")
    val networkType: String? = null,
    @SerializedName("app_in_foreground")
    val appInForeground: Boolean = false
)

data class PhoneStatusResponse(
    @SerializedName("user_id")
    val userId: Int = 0,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("battery_level")
    val batteryLevel: Int? = null,
    @SerializedName("is_charging")
    val isCharging: Boolean = false,
    @SerializedName("screen_status")
    val screenStatus: String = "off",
    @SerializedName("wifi_name")
    val wifiName: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = false,
    @SerializedName("last_active_at")
    val lastActiveAt: String? = null,
    @SerializedName("app_in_foreground")
    val appInForeground: Boolean = false,
    @SerializedName("network_type")
    val networkType: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class BothPhoneStatusResponse(
    val me: PhoneStatusResponse? = null,
    val partner: PhoneStatusResponse? = null
)

data class PhoneStatusHistoryItem(
    @SerializedName("battery_level")
    val batteryLevel: Int? = null,
    @SerializedName("is_charging")
    val isCharging: Boolean = false,
    @SerializedName("screen_status")
    val screenStatus: String = "off",
    @SerializedName("wifi_name")
    val wifiName: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = false,
    @SerializedName("network_type")
    val networkType: String? = null,
    @SerializedName("recorded_at")
    val recordedAt: String = ""
)