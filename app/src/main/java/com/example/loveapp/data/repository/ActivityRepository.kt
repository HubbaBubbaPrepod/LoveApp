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
    suspend fun createActivity(
        title: String,
        description: String,
        category: String = ""
    ): Result<ActivityResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.format(Date())
        
        val request = ActivityRequest(
            title = title,
            description = description,
            date = date,
            category = category
        )
        val response = apiService.createActivity("Bearer $token", request)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to create activity"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getActivities(page: Int = 1): Result<List<ActivityResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getActivities("Bearer $token", null, page)
        
        if (response.success && response.data != null) {
            Result.success(response.data.items)
        } else {
            Result.failure(Exception(response.message ?: "Failed to get activities"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteActivity(id: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.deleteActivity("Bearer $token", id)
        
        if (response.success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.message ?: "Failed to delete activity"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
