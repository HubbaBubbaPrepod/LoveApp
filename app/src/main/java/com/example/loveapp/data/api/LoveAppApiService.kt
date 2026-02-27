package com.example.loveapp.data.api

import com.example.loveapp.data.api.models.ActivityRequest
import com.example.loveapp.data.api.models.ActivityResponse
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
}
