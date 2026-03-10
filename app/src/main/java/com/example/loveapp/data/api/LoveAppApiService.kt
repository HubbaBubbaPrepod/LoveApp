package com.example.loveapp.data.api

import com.example.loveapp.data.api.models.ArtCanvasRequest
import com.example.loveapp.data.api.models.ArtCanvasResponse
import com.example.loveapp.data.api.models.ArtCanvasThumbnailResponse
import com.example.loveapp.data.api.models.ArtCanvasUpdateRequest
import com.example.loveapp.data.api.models.CanvasStrokesResponse
import com.example.loveapp.data.api.models.CanvasStrokesSaveRequest
import com.example.loveapp.data.api.models.AvatarUploadResponse
import com.example.loveapp.data.api.models.ActivityRequest
import com.example.loveapp.data.api.models.ActivityResponse
import com.example.loveapp.data.api.models.CustomActivityTypeRequest
import com.example.loveapp.data.api.models.CustomActivityTypeResponse
import com.example.loveapp.data.api.models.ApiResponse
import com.example.loveapp.data.api.models.AuthResponse
import com.example.loveapp.data.api.models.CycleRequest
import com.example.loveapp.data.api.models.CycleResponse
import com.example.loveapp.data.api.models.CustomCalendarRequest
import com.example.loveapp.data.api.models.CustomCalendarResponse
import com.example.loveapp.data.api.models.CalendarEventRequest
import com.example.loveapp.data.api.models.CalendarEventResponse
import com.example.loveapp.data.api.models.GoogleSignInRequest
import com.example.loveapp.data.api.models.LoginRequest
import com.example.loveapp.data.api.models.MoodRequest
import com.example.loveapp.data.api.models.MoodResponse
import com.example.loveapp.data.api.models.NoteRequest
import com.example.loveapp.data.api.models.NoteResponse
import com.example.loveapp.data.api.models.PaginatedResponse
import com.example.loveapp.data.api.models.FcmTokenRequest
import com.example.loveapp.data.api.models.LinkPartnerRequest
import com.example.loveapp.data.api.models.LinkPartnerResponse
import com.example.loveapp.data.api.models.PairingCodeResponse
import com.example.loveapp.data.api.models.RelationshipRequest
import com.example.loveapp.data.api.models.RelationshipResponse
import com.example.loveapp.data.api.models.SignupRequest
import com.example.loveapp.data.api.models.UploadResponse
import com.example.loveapp.data.api.models.WishRequest
import com.example.loveapp.data.api.models.WishResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface LoveAppApiService {

    // ==================== Auth Endpoints ====================
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): ApiResponse<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("auth/google")
    suspend fun googleAuth(@Body request: GoogleSignInRequest): ApiResponse<AuthResponse>

    @PUT("auth/setup-profile")
    suspend fun setupProfile(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.SetupProfileRequest
    ): ApiResponse<AuthResponse>

    @GET("auth/profile")
    suspend fun getProfile(@Header("Authorization") token: String): ApiResponse<AuthResponse>

    // ==================== Notes Endpoints ====================
    @POST("notes")
    suspend fun createNote(
        @Header("Authorization") token: String,
        @Body request: NoteRequest
    ): ApiResponse<NoteResponse>

    @GET("notes")
    suspend fun getNotes(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<PaginatedResponse<NoteResponse>>

    @GET("notes/{id}")
    suspend fun getNote(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<NoteResponse>

    @PUT("notes/{id}")
    suspend fun updateNote(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: NoteRequest
    ): ApiResponse<NoteResponse>

    @DELETE("notes/{id}")
    suspend fun deleteNote(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    // ==================== Upload Endpoints ====================
    @Multipart
    @POST("upload/image")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): ApiResponse<UploadResponse>

    @Multipart
    @POST("upload/profile")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): ApiResponse<AvatarUploadResponse>

    // ==================== Wishes Endpoints ====================
    @POST("wishes")
    suspend fun createWish(
        @Header("Authorization") token: String,
        @Body request: WishRequest
    ): ApiResponse<WishResponse>

    @GET("wishes")
    suspend fun getWishes(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<PaginatedResponse<WishResponse>>

    @GET("wishes/{id}")
    suspend fun getWish(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<WishResponse>

    @PUT("wishes/{id}")
    suspend fun updateWish(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: WishRequest
    ): ApiResponse<WishResponse>

    @DELETE("wishes/{id}")
    suspend fun deleteWish(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    @POST("wishes/{id}/complete")
    suspend fun completeWish(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<WishResponse>

    // ==================== Mood Endpoints ====================
    @POST("moods")
    suspend fun createMood(
        @Header("Authorization") token: String,
        @Body request: MoodRequest
    ): ApiResponse<MoodResponse>

    @PUT("moods/{id}")
    suspend fun updateMood(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: MoodRequest
    ): ApiResponse<MoodResponse>

    @GET("moods")
    suspend fun getMoods(
        @Header("Authorization") token: String,
        @Query("date") date: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): ApiResponse<PaginatedResponse<MoodResponse>>

    @GET("moods/partner")
    suspend fun getPartnerMoods(
        @Header("Authorization") token: String,
        @Query("date") date: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): ApiResponse<PaginatedResponse<MoodResponse>>

    @GET("moods/{id}")
    suspend fun getMood(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<MoodResponse>

    @DELETE("moods/{id}")
    suspend fun deleteMood(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    @GET("moods/analytics")
    suspend fun getMoodAnalytics(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.MoodAnalyticsResponse>

    // ==================== Activity Endpoints ====================
    @POST("activities")
    suspend fun createActivity(
        @Header("Authorization") token: String,
        @Body request: ActivityRequest
    ): ApiResponse<ActivityResponse>

    @GET("activities")
    suspend fun getActivities(
        @Header("Authorization") token: String,
        @Query("date") date: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 500
    ): ApiResponse<PaginatedResponse<ActivityResponse>>

    @GET("activities/partner")
    suspend fun getPartnerActivities(
        @Header("Authorization") token: String,
        @Query("date") date: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): ApiResponse<PaginatedResponse<ActivityResponse>>

    @DELETE("activities/{id}")
    suspend fun deleteActivity(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    // ==================== Custom Activity Type Endpoints ====================
    @POST("activities/types")
    suspend fun createCustomActivityType(
        @Header("Authorization") token: String,
        @Body request: CustomActivityTypeRequest
    ): ApiResponse<CustomActivityTypeResponse>

    @GET("activities/types")
    suspend fun getCustomActivityTypes(
        @Header("Authorization") token: String
    ): ApiResponse<List<CustomActivityTypeResponse>>

    @DELETE("activities/types/{id}")
    suspend fun deleteCustomActivityType(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    // ==================== Menstrual Cycle Endpoints ====================
    @POST("cycles")
    suspend fun createCycle(
        @Header("Authorization") token: String,
        @Body request: CycleRequest
    ): ApiResponse<CycleResponse>

    @GET("cycles")
    suspend fun getCycles(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 100
    ): ApiResponse<PaginatedResponse<CycleResponse>>

    @GET("cycles/latest")
    suspend fun getLatestCycle(
        @Header("Authorization") token: String
    ): ApiResponse<CycleResponse>

    @GET("cycles/partner")
    suspend fun getPartnerCycles(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 100
    ): ApiResponse<PaginatedResponse<CycleResponse>>

    @PUT("cycles/{id}")
    suspend fun updateCycle(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: CycleRequest
    ): ApiResponse<CycleResponse>

    @PATCH("cycles/{id}")
    suspend fun patchCycle(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: com.example.loveapp.data.api.models.CyclePatchRequest
    ): ApiResponse<CycleResponse>

    @DELETE("cycles/{id}")
    suspend fun deleteCycle(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    // ==================== Custom Calendars Endpoints ====================
    @POST("calendars")
    suspend fun createCalendar(
        @Header("Authorization") token: String,
        @Body request: CustomCalendarRequest
    ): ApiResponse<CustomCalendarResponse>

    @GET("calendars")
    suspend fun getCalendars(
        @Header("Authorization") token: String,
        @Query("type") type: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<PaginatedResponse<CustomCalendarResponse>>

    @GET("calendars/{id}")
    suspend fun getCalendar(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<CustomCalendarResponse>

    @PUT("calendars/{id}")
    suspend fun updateCalendar(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: CustomCalendarRequest
    ): ApiResponse<CustomCalendarResponse>

    @DELETE("calendars/{id}")
    suspend fun deleteCalendar(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    @GET("calendars/partner")
    suspend fun getPartnerCalendars(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 100
    ): ApiResponse<PaginatedResponse<CustomCalendarResponse>>

    @GET("calendars/{id}/events")
    suspend fun getCalendarEvents(
        @Header("Authorization") token: String,
        @Path("id") calendarId: Int
    ): ApiResponse<PaginatedResponse<CalendarEventResponse>>

    @POST("calendars/{id}/events")
    suspend fun createCalendarEvent(
        @Header("Authorization") token: String,
        @Path("id") calendarId: Int,
        @Body request: CalendarEventRequest
    ): ApiResponse<CalendarEventResponse>

    @DELETE("calendars/events/{eventId}")
    suspend fun deleteCalendarEvent(
        @Header("Authorization") token: String,
        @Path("eventId") eventId: Int
    ): ApiResponse<Any?>

    // ==================== Relationship Endpoints ====================
    // Server has no POST; use PUT for both create and update (server does upsert)
    @GET("relationship")
    suspend fun getRelationship(
        @Header("Authorization") token: String
    ): ApiResponse<RelationshipResponse>

    @PUT("relationship")
    suspend fun updateRelationship(
        @Header("Authorization") token: String,
        @Body request: RelationshipRequest
    ): ApiResponse<RelationshipResponse>

    // ==================== FCM Token ====================
    @POST("fcm-token")
    suspend fun registerFcmToken(
        @Header("Authorization") authHeader: String,
        @Body request: FcmTokenRequest
    ): ApiResponse<Any?>

    // ==================== Partner Pairing ====================
    @POST("partner/generate-code")
    suspend fun generatePairingCode(
        @Header("Authorization") token: String
    ): ApiResponse<PairingCodeResponse>

    @POST("partner/link")
    suspend fun linkPartner(
        @Header("Authorization") token: String,
        @Body request: LinkPartnerRequest
    ): ApiResponse<LinkPartnerResponse>

    // ==================== Art Canvases ====================
    @GET("art/canvases")
    suspend fun getCanvases(
        @Header("Authorization") token: String
    ): ApiResponse<List<ArtCanvasResponse>>

    @POST("art/canvases")
    suspend fun createCanvas(
        @Header("Authorization") token: String,
        @Body request: ArtCanvasRequest
    ): ApiResponse<ArtCanvasResponse>

    @GET("art/canvases/{id}")
    suspend fun getCanvas(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<ArtCanvasResponse>

    @PUT("art/canvases/{id}")
    suspend fun updateCanvas(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: ArtCanvasUpdateRequest
    ): ApiResponse<ArtCanvasResponse>

    @DELETE("art/canvases/{id}")
    suspend fun deleteCanvas(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    @Multipart
    @POST("art/canvases/{id}/thumbnail")
    suspend fun uploadCanvasThumbnail(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Part thumbnail: MultipartBody.Part
    ): ApiResponse<ArtCanvasThumbnailResponse>

    @GET("art/canvases/{id}/strokes")
    suspend fun getCanvasStrokes(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<CanvasStrokesResponse>

    @PUT("art/canvases/{id}/strokes")
    suspend fun saveCanvasStrokes(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: CanvasStrokesSaveRequest
    ): ApiResponse<Any?>

    // ==================== Chat Endpoints ====================
    @POST("chat")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.ChatMessageRequest
    ): ApiResponse<com.example.loveapp.data.api.models.ChatMessageResponse>

    @GET("chat")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): ApiResponse<PaginatedResponse<com.example.loveapp.data.api.models.ChatMessageResponse>>

    @PUT("chat/{id}/read")
    suspend fun markMessageRead(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    @PUT("chat/read-all")
    suspend fun markAllMessagesRead(
        @Header("Authorization") token: String
    ): ApiResponse<Any?>

    @DELETE("chat/{id}")
    suspend fun deleteMessage(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    @GET("chat/stickers/packs")
    suspend fun getStickerPacks(
        @Header("Authorization") token: String
    ): ApiResponse<List<com.example.loveapp.data.api.models.StickerPackResponse>>

    @GET("chat/stickers/pack/{id}")
    suspend fun getStickers(
        @Header("Authorization") token: String,
        @Path("id") packId: Int
    ): ApiResponse<List<com.example.loveapp.data.api.models.StickerResponse>>

    @POST("chat/stickers/acquire/{packId}")
    suspend fun acquireStickerPack(
        @Header("Authorization") token: String,
        @Path("packId") packId: Int
    ): ApiResponse<Any?>

    @POST("chat/missyou")
    suspend fun sendChatMissYou(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.MissYouCounterRequest
    ): ApiResponse<Any?>

    @GET("chat/missyou")
    suspend fun getChatMissYouCounter(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.MissYouCounterResponse>

    @Multipart
    @POST("upload/file")
    suspend fun uploadFile(
        @Header("Authorization") token: String,
        @Part file: okhttp3.MultipartBody.Part
    ): ApiResponse<UploadResponse>

    // ==================== Memorial Day Endpoints ====================
    @POST("memorial")
    suspend fun createMemorialDay(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.MemorialDayRequest
    ): ApiResponse<com.example.loveapp.data.api.models.MemorialDayResponse>

    @GET("memorial")
    suspend fun getMemorialDays(
        @Header("Authorization") token: String
    ): ApiResponse<List<com.example.loveapp.data.api.models.MemorialDayResponse>>

    @PUT("memorial/{id}")
    suspend fun updateMemorialDay(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: com.example.loveapp.data.api.models.MemorialDayRequest
    ): ApiResponse<com.example.loveapp.data.api.models.MemorialDayResponse>

    @DELETE("memorial/{id}")
    suspend fun deleteMemorialDay(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    // ==================== Spark/Streak Endpoints ====================
    @GET("spark")
    suspend fun getSparkStreak(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.SparkStreakResponse>

    @GET("spark/history")
    suspend fun getSparkHistory(
        @Header("Authorization") token: String,
        @Query("days") days: Int = 7
    ): ApiResponse<List<com.example.loveapp.data.api.models.SparkHistoryItem>>

    @POST("spark/log")
    suspend fun logSpark(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.SparkLogRequest
    ): ApiResponse<Any?>

    @GET("spark/breakdown")
    suspend fun getSparkBreakdown(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.SparkBreakdownResponse>

    // ==================== Task Endpoints ====================
    @GET("tasks")
    suspend fun getTasks(
        @Header("Authorization") token: String,
        @Query("date") date: String? = null
    ): ApiResponse<com.example.loveapp.data.api.models.TasksListResponse>

    @POST("tasks")
    suspend fun createTask(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.CoupleTaskRequest
    ): ApiResponse<com.example.loveapp.data.api.models.CoupleTaskResponse>

    @PUT("tasks/{id}/complete")
    suspend fun completeTask(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<com.example.loveapp.data.api.models.CoupleTaskResponse>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    // ==================== Sleep Endpoints ====================
    @POST("sleep")
    suspend fun createSleepEntry(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.SleepEntryRequest
    ): ApiResponse<com.example.loveapp.data.api.models.SleepEntryResponse>

    @GET("sleep")
    suspend fun getSleepEntries(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 14
    ): ApiResponse<PaginatedResponse<com.example.loveapp.data.api.models.SleepEntryResponse>>

    @GET("sleep/partner")
    suspend fun getPartnerSleep(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 7
    ): ApiResponse<com.example.loveapp.data.api.models.SleepPartnerResponse>

    @GET("sleep/stats")
    suspend fun getSleepStats(
        @Header("Authorization") token: String,
        @Query("days") days: Int = 7
    ): ApiResponse<com.example.loveapp.data.api.models.SleepStatsResponse>

    @DELETE("sleep/{id}")
    suspend fun deleteSleepEntry(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    // ==================== Gallery Endpoints ====================

    @POST("gallery")
    suspend fun addGalleryPhoto(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.GalleryPhotoRequest
    ): ApiResponse<com.example.loveapp.data.api.models.GalleryPhotoResponse>

    @GET("gallery")
    suspend fun getGalleryPhotos(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): ApiResponse<PaginatedResponse<com.example.loveapp.data.api.models.GalleryPhotoResponse>>

    @PUT("gallery/{id}")
    suspend fun updateGalleryPhoto(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: Map<String, String>
    ): ApiResponse<com.example.loveapp.data.api.models.GalleryPhotoResponse>

    @DELETE("gallery/{id}")
    suspend fun deleteGalleryPhoto(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    // ==================== Love Touch Endpoints ====================

    @POST("lovetouch/start")
    suspend fun startLoveTouchSession(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.LoveTouchSessionResponse>

    @POST("lovetouch/{id}/join")
    suspend fun joinLoveTouchSession(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<com.example.loveapp.data.api.models.LoveTouchSessionResponse>

    @POST("lovetouch/{id}/end")
    suspend fun endLoveTouchSession(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: Map<String, Int>
    ): ApiResponse<com.example.loveapp.data.api.models.LoveTouchSessionResponse>

    @GET("lovetouch/history")
    suspend fun getLoveTouchHistory(
        @Header("Authorization") token: String
    ): ApiResponse<List<com.example.loveapp.data.api.models.LoveTouchSessionResponse>>

    // ==================== Miss You Endpoints ====================

    @POST("missyou")
    suspend fun sendMissYou(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.MissYouRequest
    ): ApiResponse<com.example.loveapp.data.api.models.MissYouResponse>

    @GET("missyou")
    suspend fun getMissYouHistory(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): ApiResponse<PaginatedResponse<com.example.loveapp.data.api.models.MissYouResponse>>

    @GET("missyou/today")
    suspend fun getMissYouToday(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.MissYouTodayResponse>

    // ==================== App Lock Endpoints ====================

    @GET("applock")
    suspend fun getAppLockStatus(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.AppLockStatusResponse>

    @POST("applock")
    suspend fun setAppLock(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.AppLockSetRequest
    ): ApiResponse<com.example.loveapp.data.api.models.AppLockStatusResponse>

    @POST("applock/verify")
    suspend fun verifyAppLock(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.AppLockVerifyRequest
    ): ApiResponse<com.example.loveapp.data.api.models.AppLockVerifyResponse>

    @PUT("applock")
    suspend fun updateAppLock(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.AppLockUpdateRequest
    ): ApiResponse<Any?>

    @HTTP(method = "DELETE", path = "applock", hasBody = true)
    suspend fun removeAppLock(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.AppLockDeleteRequest
    ): ApiResponse<Any?>

    // ==================== Chat Settings Endpoints ====================

    @GET("chatsettings")
    suspend fun getChatSettings(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.ChatSettingsResponse>

    @PUT("chatsettings")
    suspend fun updateChatSettings(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.ChatSettingsRequest
    ): ApiResponse<com.example.loveapp.data.api.models.ChatSettingsResponse>

    @GET("chatsettings/wallpapers")
    suspend fun getWallpapers(
        @Header("Authorization") token: String
    ): ApiResponse<List<com.example.loveapp.data.api.models.WallpaperItem>>

    // ==================== Daily Q&A Endpoints ====================

    @GET("questions/today")
    suspend fun getDailyQuestion(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.DailyQATodayResponse>

    @POST("questions/{id}/answer")
    suspend fun submitDailyAnswer(
        @Header("Authorization") token: String,
        @Path("id") questionId: Int,
        @Body request: com.example.loveapp.data.api.models.DailyAnswerRequest
    ): ApiResponse<com.example.loveapp.data.api.models.DailyAnswerResponse>

    @GET("questions/history")
    suspend fun getDailyQAHistory(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): ApiResponse<com.example.loveapp.data.api.models.PaginatedResponse<com.example.loveapp.data.api.models.DailyQAHistoryItem>>

    // ==================== Intimacy Score Endpoints ====================

    @GET("intimacy")
    suspend fun getIntimacyScore(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.IntimacyScoreResponse>

    @GET("intimacy/history")
    suspend fun getIntimacyHistory(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<com.example.loveapp.data.api.models.PaginatedResponse<com.example.loveapp.data.api.models.IntimacyLogItem>>

    // ==================== GIF Search Endpoints ====================

    @GET("gif/search")
    suspend fun searchGifs(
        @Header("Authorization") token: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("pos") pos: String? = null
    ): ApiResponse<com.example.loveapp.data.api.models.GifSearchResult>

    @GET("gif/trending")
    suspend fun getTrendingGifs(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 20
    ): ApiResponse<com.example.loveapp.data.api.models.GifSearchResult>

    // ==================== Moments (ShareNow) Endpoints ====================

    @POST("moments")
    suspend fun shareMoment(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.MomentRequest
    ): ApiResponse<com.example.loveapp.data.api.models.MomentResponse>

    @GET("moments")
    suspend fun getMoments(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<com.example.loveapp.data.api.models.PaginatedResponse<com.example.loveapp.data.api.models.MomentResponse>>

    @DELETE("moments/{id}")
    suspend fun deleteMoment(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    // ==================== Common Places Endpoints ====================

    @POST("places")
    suspend fun addPlace(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.PlaceRequest
    ): ApiResponse<com.example.loveapp.data.api.models.PlaceResponse>

    @GET("places")
    suspend fun getPlaces(
        @Header("Authorization") token: String,
        @Query("category") category: String? = null
    ): ApiResponse<com.example.loveapp.data.api.models.PlaceListResponse>

    @PUT("places/{id}")
    suspend fun updatePlace(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: com.example.loveapp.data.api.models.PlaceRequest
    ): ApiResponse<com.example.loveapp.data.api.models.PlaceResponse>

    @DELETE("places/{id}")
    suspend fun deletePlace(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<Any?>

    // ==================== Real-time Location Tracking Endpoints ====================

    @POST("location/update")
    suspend fun updateLocation(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.LocationUpdateRequest
    ): ApiResponse<com.example.loveapp.data.api.models.LocationPointResponse>

    @POST("location/batch")
    suspend fun batchLocationUpdate(
        @Header("Authorization") token: String,
        @Body body: Map<String, List<com.example.loveapp.data.api.models.LocationUpdateRequest>>
    ): ApiResponse<Any?>

    @GET("location/latest")
    suspend fun getLatestLocations(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.LatestLocationsResponse>

    @GET("location/history")
    suspend fun getLocationHistory(
        @Header("Authorization") token: String,
        @Query("user") user: String? = null,
        @Query("hours") hours: Int? = null,
        @Query("limit") limit: Int? = null
    ): ApiResponse<com.example.loveapp.data.api.models.LocationHistoryResponse>

    @GET("location/stats")
    suspend fun getLocationStats(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.LocationStatsResponse>

    @GET("location/settings")
    suspend fun getLocationSettings(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.LocationSettingsResponse>

    @PUT("location/settings")
    suspend fun updateLocationSettings(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.LocationSettingsRequest
    ): ApiResponse<com.example.loveapp.data.api.models.LocationSettingsResponse>

    // ==================== Virtual Pet Endpoints (Overhauled) ====================

    @GET("pet")
    suspend fun getPet(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.PetResponse>

    @PUT("pet")
    suspend fun updatePet(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.PetUpdateRequest
    ): ApiResponse<com.example.loveapp.data.api.models.PetResponse>

    @POST("pet/feed")
    suspend fun feedPet(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.CareActionResult>

    @POST("pet/play")
    suspend fun playWithPet(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.CareActionResult>

    @POST("pet/clean")
    suspend fun cleanPet(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.CareActionResult>

    @GET("pet/history")
    suspend fun getPetHistory(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 20
    ): ApiResponse<com.example.loveapp.data.api.models.PaginatedResponse<com.example.loveapp.data.api.models.PetActionResponse>>

    @GET("pet/types")
    suspend fun getPetTypes(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.PaginatedResponse<com.example.loveapp.data.api.models.PetTypeInfo>>

    // Eggs
    @GET("pet/eggs")
    suspend fun getPetEggs(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.PaginatedResponse<com.example.loveapp.data.api.models.PetEgg>>

    @POST("pet/eggs/hatch/{id}")
    suspend fun hatchEgg(
        @Header("Authorization") token: String,
        @Path("id") eggId: Long
    ): ApiResponse<com.example.loveapp.data.api.models.EggHatchResult>

    // Furniture
    @GET("pet/furniture/shop")
    suspend fun getFurnitureShop(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.PaginatedResponse<com.example.loveapp.data.api.models.FurnitureItem>>

    @GET("pet/furniture/owned")
    suspend fun getOwnedFurniture(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.PaginatedResponse<com.example.loveapp.data.api.models.OwnedFurniture>>

    @POST("pet/furniture/buy/{id}")
    suspend fun buyFurniture(
        @Header("Authorization") token: String,
        @Path("id") furnitureId: Int
    ): ApiResponse<com.example.loveapp.data.api.models.FurnitureItem>

    @PUT("pet/furniture/place/{id}")
    suspend fun placeFurniture(
        @Header("Authorization") token: String,
        @Path("id") ownedId: Long,
        @Body request: com.example.loveapp.data.api.models.FurniturePlaceRequest
    ): ApiResponse<com.example.loveapp.data.api.models.OwnedFurniture>

    // Adventures
    @GET("pet/adventures")
    suspend fun getAdventures(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.AdventuresData>

    @POST("pet/adventures/start/{id}")
    suspend fun startAdventure(
        @Header("Authorization") token: String,
        @Path("id") adventureId: Int
    ): ApiResponse<com.example.loveapp.data.api.models.AdventureStartResult>

    @POST("pet/adventures/claim/{id}")
    suspend fun claimAdventure(
        @Header("Authorization") token: String,
        @Path("id") activeId: Long
    ): ApiResponse<com.example.loveapp.data.api.models.AdventureClaimResult>

    // Wishes
    @GET("pet/wishes")
    suspend fun getPetWishes(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.PaginatedResponse<com.example.loveapp.data.api.models.PetWish>>

    @POST("pet/wishes")
    suspend fun createPetWish(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.PetWishRequest
    ): ApiResponse<com.example.loveapp.data.api.models.PetWish>

    @POST("pet/wishes/fulfill/{id}")
    suspend fun fulfillPetWish(
        @Header("Authorization") token: String,
        @Path("id") wishId: Long
    ): ApiResponse<com.example.loveapp.data.api.models.WishFulfillResult>

    @DELETE("pet/wishes/{id}")
    suspend fun deletePetWish(
        @Header("Authorization") token: String,
        @Path("id") wishId: Long
    ): ApiResponse<Any>

    // Check-in
    @GET("pet/checkin")
    suspend fun getCheckinStatus(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.CheckinStatus>

    @POST("pet/checkin")
    suspend fun doCheckin(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.CheckinResult>

    // Spin
    @POST("pet/spin")
    suspend fun doSpin(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.SpinResult>

    // Collections & Passport
    @GET("pet/collections")
    suspend fun getPetCollections(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.CollectionsData>

    @GET("pet/passport")
    suspend fun getPetPassport(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.PetPassport>

    @GET("pet/level-rewards")
    suspend fun getLevelRewards(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.PaginatedResponse<com.example.loveapp.data.api.models.LevelReward>>

    // ==================== Gold Coin Economy Endpoints ====================

    @GET("shop/balance")
    suspend fun getCoinBalance(
        @Header("Authorization") token: String
    ): com.example.loveapp.data.api.models.CoinBalanceResponse

    @GET("shop/items")
    suspend fun getShopItems(
        @Header("Authorization") token: String,
        @Query("category") category: String? = null
    ): List<com.example.loveapp.data.api.models.ShopItem>

    @POST("shop/buy/{id}")
    suspend fun buyShopItem(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): com.example.loveapp.data.api.models.ShopPurchaseResult

    @GET("shop/daily-deals")
    suspend fun getDailyDeals(
        @Header("Authorization") token: String
    ): List<com.example.loveapp.data.api.models.DailyDeal>

    @POST("shop/daily-deals/buy/{id}")
    suspend fun buyDailyDeal(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): com.example.loveapp.data.api.models.DealPurchaseResult

    @GET("shop/missions")
    suspend fun getDailyMissions(
        @Header("Authorization") token: String
    ): List<com.example.loveapp.data.api.models.DailyMission>

    @POST("shop/missions/progress")
    suspend fun progressMission(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.MissionProgressRequest
    ): com.example.loveapp.data.api.models.MissionProgressResult

    @POST("shop/missions/claim/{id}")
    suspend fun claimMission(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): com.example.loveapp.data.api.models.MissionClaimResult

    @GET("shop/transactions")
    suspend fun getCoinTransactions(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): com.example.loveapp.data.api.models.TransactionsResponse

    @GET("shop/summary")
    suspend fun getEconomySummary(
        @Header("Authorization") token: String
    ): com.example.loveapp.data.api.models.EconomySummary

    // ==================== Couple Games Endpoints ====================

    @POST("games/start")
    suspend fun startGame(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.GameStartRequest
    ): ApiResponse<com.example.loveapp.data.api.models.GameSessionResponse>

    @GET("games/session/{id}")
    suspend fun getGameSession(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ApiResponse<com.example.loveapp.data.api.models.GameSessionResponse>

    @POST("games/answer")
    suspend fun submitGameAnswer(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.GameAnswerRequest
    ): ApiResponse<com.example.loveapp.data.api.models.GameRoundResponse>

    @GET("games")
    suspend fun getGames(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 20
    ): ApiResponse<com.example.loveapp.data.api.models.GameListResponse>

    @GET("games/compatibility")
    suspend fun getGameCompatibility(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.CompatibilityStatsResponse>

    // ==================== Widget Summary Endpoint ====================

    @GET("widget/summary")
    suspend fun getWidgetSummary(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.WidgetSummaryResponse>

    // ==================== Phase 6: Achievements Endpoints ====================

    @GET("achievements")
    suspend fun getAchievements(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.AchievementListResponse>

    @GET("achievements/progress")
    suspend fun getAchievementProgress(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.AchievementProgressResponse>

    @POST("achievements/check")
    suspend fun checkAchievements(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.AchievementCheckResponse>

    // ==================== Phase 6: Love Letters Endpoints ====================

    @GET("letters")
    suspend fun getLetters(
        @Header("Authorization") token: String,
        @Query("filter") filter: String? = null
    ): ApiResponse<com.example.loveapp.data.api.models.LoveLetterListResponse>

    @GET("letters/stats")
    suspend fun getLetterStats(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.LoveLetterStatsResponse>

    @GET("letters/{id}")
    suspend fun getLetter(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ApiResponse<com.example.loveapp.data.api.models.LoveLetterResponse>

    @POST("letters")
    suspend fun createLetter(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.LoveLetterRequest
    ): ApiResponse<com.example.loveapp.data.api.models.LoveLetterResponse>

    @DELETE("letters/{id}")
    suspend fun deleteLetter(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ApiResponse<Unit>

    // ==================== Phase 6: Bucket List Endpoints ====================

    @GET("bucketlist")
    suspend fun getBucketList(
        @Header("Authorization") token: String,
        @Query("category") category: String? = null,
        @Query("completed") completed: String? = null
    ): ApiResponse<com.example.loveapp.data.api.models.BucketListResponse>

    @POST("bucketlist")
    suspend fun createBucketItem(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.BucketItemRequest
    ): ApiResponse<com.example.loveapp.data.api.models.BucketItemResponse>

    @PUT("bucketlist/{id}")
    suspend fun updateBucketItem(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: com.example.loveapp.data.api.models.BucketItemRequest
    ): ApiResponse<com.example.loveapp.data.api.models.BucketItemResponse>

    @POST("bucketlist/{id}/complete")
    suspend fun completeBucketItem(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ApiResponse<com.example.loveapp.data.api.models.BucketItemResponse>

    @POST("bucketlist/{id}/uncomplete")
    suspend fun uncompleteBucketItem(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ApiResponse<com.example.loveapp.data.api.models.BucketItemResponse>

    @DELETE("bucketlist/{id}")
    suspend fun deleteBucketItem(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ApiResponse<Unit>

    // ==================== TIM (Tencent IM) Endpoints ====================
    @GET("tim/usersig")
    suspend fun getTIMUserSig(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.TIMUserSigResponse>

    // ==================== Phase 8: Premium Endpoints ====================
    @GET("premium/plans")
    suspend fun getPremiumPlans(
    ): ApiResponse<com.example.loveapp.data.api.models.SubscriptionPlanListResponse>

    @GET("premium/status")
    suspend fun getPremiumStatus(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.PremiumStatusResponse>

    @POST("premium/verify")
    suspend fun verifyPurchase(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.PurchaseVerifyRequest
    ): ApiResponse<com.example.loveapp.data.api.models.PremiumStatusResponse>

    @POST("premium/restore")
    suspend fun restorePurchase(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.PurchaseRestoreRequest
    ): ApiResponse<com.example.loveapp.data.api.models.PremiumStatusResponse>

    @POST("premium/cancel")
    suspend fun cancelSubscription(
        @Header("Authorization") token: String
    ): ApiResponse<Unit>

    // ==================== Phase 8: Story Endpoints ====================
    @GET("story")
    suspend fun getStoryEntries(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<com.example.loveapp.data.api.models.StoryListResponse>

    @GET("story/stats")
    suspend fun getStoryStats(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.StoryStatsResponse>

    @GET("story/{id}")
    suspend fun getStoryEntry(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ApiResponse<com.example.loveapp.data.api.models.StoryEntryResponse>

    @POST("story")
    suspend fun createStoryEntry(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.StoryEntryRequest
    ): ApiResponse<com.example.loveapp.data.api.models.StoryEntryResponse>

    @PUT("story/{id}")
    suspend fun updateStoryEntry(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: com.example.loveapp.data.api.models.StoryEntryRequest
    ): ApiResponse<com.example.loveapp.data.api.models.StoryEntryResponse>

    @DELETE("story/{id}")
    suspend fun deleteStoryEntry(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ApiResponse<Unit>

    // ── Geofences ────────────────────────────────────────────────────────────

    @POST("geofences")
    suspend fun createGeofence(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.GeofenceRequest
    ): ApiResponse<com.example.loveapp.data.api.models.GeofenceResponse>

    @GET("geofences")
    suspend fun getGeofences(
        @Header("Authorization") token: String
    ): ApiResponse<List<com.example.loveapp.data.api.models.GeofenceResponse>>

    @GET("geofences/{id}")
    suspend fun getGeofence(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ApiResponse<com.example.loveapp.data.api.models.GeofenceResponse>

    @PUT("geofences/{id}")
    suspend fun updateGeofence(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: com.example.loveapp.data.api.models.GeofenceUpdateRequest
    ): ApiResponse<com.example.loveapp.data.api.models.GeofenceResponse>

    @DELETE("geofences/{id}")
    suspend fun deleteGeofence(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ApiResponse<Unit>

    @POST("geofences/event")
    suspend fun reportGeofenceEvent(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.GeofenceEventRequest
    ): ApiResponse<com.example.loveapp.data.api.models.GeofenceEventResponse>

    @GET("geofences/{id}/events")
    suspend fun getGeofenceEvents(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Query("limit") limit: Int? = null,
        @Query("hours") hours: Int? = null
    ): ApiResponse<List<com.example.loveapp.data.api.models.GeofenceEventResponse>>

    @GET("geofences/events/recent")
    suspend fun getRecentGeofenceEvents(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int? = null
    ): ApiResponse<List<com.example.loveapp.data.api.models.GeofenceEventResponse>>

    // ═══ Phase 11: Phone Status ══════════════════════════════════════════════

    @POST("phonestatus/update")
    suspend fun updatePhoneStatus(
        @Header("Authorization") token: String,
        @Body request: com.example.loveapp.data.api.models.PhoneStatusUpdateRequest
    ): ApiResponse<Any>

    @GET("phonestatus/partner")
    suspend fun getPartnerPhoneStatus(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.PhoneStatusResponse>

    @GET("phonestatus/me")
    suspend fun getMyPhoneStatus(
        @Header("Authorization") token: String
    ): ApiResponse<Any>

    @GET("phonestatus/both")
    suspend fun getBothPhoneStatus(
        @Header("Authorization") token: String
    ): ApiResponse<com.example.loveapp.data.api.models.BothPhoneStatusResponse>

    @GET("phonestatus/history")
    suspend fun getPhoneStatusHistory(
        @Header("Authorization") token: String,
        @Query("hours") hours: Int? = null
    ): ApiResponse<List<com.example.loveapp.data.api.models.PhoneStatusHistoryItem>>
}
