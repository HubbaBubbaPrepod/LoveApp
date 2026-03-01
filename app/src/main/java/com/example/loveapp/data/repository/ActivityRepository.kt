package com.example.loveapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.ActivityRequest
import com.example.loveapp.data.api.models.ActivityResponse
import com.example.loveapp.data.api.models.CustomActivityTypeRequest
import com.example.loveapp.data.api.models.CustomActivityTypeResponse
import com.example.loveapp.data.dao.ActivityLogDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ActivityRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val activityLogDao: ActivityLogDao,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun createActivity(
        title: String,
        activityType: String,
        durationMinutes: Int,
        startTime: String,
        note: String,
        date: String = dateFmt.format(Date())
    ): Result<ActivityResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val request = ActivityRequest(
            title           = title,
            description     = note,
            date            = date,
            category        = activityType,
            activityType    = activityType,
            durationMinutes = durationMinutes,
            startTime       = startTime,
            note            = note
        )
        val response = apiService.createActivity("Bearer $token", request)
        if (response.success && response.data != null)
            Result.success(response.data)
        else
            Result.failure(Exception(response.message ?: "Failed to create activity"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getActivities(
        date: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<List<ActivityResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getActivities(
            "Bearer $token",
            date      = date,
            startDate = startDate,
            endDate   = endDate,
            limit     = 500
        )
        if (response.success && response.data != null)
            Result.success(response.data.items)
        else
            Result.failure(Exception(response.message ?: "Failed to get activities"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getPartnerActivities(
        date: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<List<ActivityResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getPartnerActivities(
            "Bearer $token",
            date      = date,
            startDate = startDate,
            endDate   = endDate
        )
        if (response.success && response.data != null)
            Result.success(response.data.items)
        else
            Result.failure(Exception(response.message ?: "Failed to get partner activities"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteActivity(id: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.deleteActivity("Bearer $token", id)
        if (response.success) Result.success(Unit)
        else Result.failure(Exception(response.message ?: "Failed to delete activity"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getCustomActivityTypes(): Result<List<CustomActivityTypeResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getCustomActivityTypes("Bearer $token")
        if (response.success && response.data != null)
            Result.success(response.data)
        else
            Result.failure(Exception(response.message ?: "Failed to get custom activity types"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun createCustomActivityType(
        name: String,
        emoji: String,
        colorHex: String
    ): Result<CustomActivityTypeResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.createCustomActivityType(
            "Bearer $token",
            CustomActivityTypeRequest(name = name, emoji = emoji, colorHex = colorHex)
        )
        if (response.success && response.data != null)
            Result.success(response.data)
        else
            Result.failure(Exception(response.message ?: "Failed to create custom activity type"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteCustomActivityType(id: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.deleteCustomActivityType("Bearer $token", id)
        if (response.success) Result.success(Unit)
        else Result.failure(Exception(response.message ?: "Failed to delete custom activity type"))
    } catch (e: Exception) { Result.failure(e) }

    private suspend fun compressImage(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)!!
        val bmp = BitmapFactory.decodeStream(input)
        input.close()
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
        bmp.recycle()
        out.toByteArray()
    }

    suspend fun uploadImage(uri: Uri): Result<String> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val bytes = compressImage(uri)
        val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "activity_icon.jpg", requestBody)
        val response = apiService.uploadImage("Bearer $token", part)
        if (response.success && response.data != null)
            Result.success(response.data.url)
        else
            Result.failure(Exception(response.message ?: "Upload failed"))
    } catch (e: Exception) { Result.failure(e) }
}
