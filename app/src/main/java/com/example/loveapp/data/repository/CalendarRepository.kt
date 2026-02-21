package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.CustomCalendarRequest
import com.example.loveapp.data.api.models.CustomCalendarResponse
import com.example.loveapp.data.dao.CustomCalendarDao
import javax.inject.Inject

class CalendarRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val calendarDao: CustomCalendarDao,
    private val authRepository: AuthRepository
) {
    suspend fun createCalendar(
        name: String,
        type: String,
        color: String,
        description: String = ""
    ): Result<CustomCalendarResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val request = CustomCalendarRequest(
            name = name,
            description = description,
            type = type,
            colorHex = color
        )
        val response = apiService.createCalendar("Bearer $token", request)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to create calendar"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCalendars(type: String? = null, page: Int = 1): Result<List<CustomCalendarResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getCalendars("Bearer $token", type, page)
        
        if (response.success && response.data != null) {
            Result.success(response.data.items)
        } else {
            Result.failure(Exception(response.message ?: "Failed to get calendars"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCalendar(id: Int): Result<CustomCalendarResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getCalendar("Bearer $token", id)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to get calendar"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateCalendar(id: Int, calendar: CustomCalendarRequest): Result<CustomCalendarResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.updateCalendar("Bearer $token", id, calendar)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to update calendar"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteCalendar(id: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.deleteCalendar("Bearer $token", id)
        
        if (response.success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.message ?: "Failed to delete calendar"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
