package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.ActivityRequest
import com.example.loveapp.data.api.models.ActivityResponse
import com.example.loveapp.data.dao.ActivityLogDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ActivityRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val activityLogDao: ActivityLogDao,
    private val authRepository: AuthRepository
) {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun createActivity(
        activityType: String,
        durationMinutes: Int,
        startTime: String,
        note: String,
        date: String = dateFmt.format(Date())
    ): Result<ActivityResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val request = ActivityRequest(
            title           = activityType,
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
}
