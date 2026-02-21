package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.CycleRequest
import com.example.loveapp.data.api.models.CycleResponse
import com.example.loveapp.data.dao.MenstrualCycleDao
import javax.inject.Inject

class CycleRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val cycleDao: MenstrualCycleDao,
    private val authRepository: AuthRepository
) {
    suspend fun createCycle(
        cycleStartDate: String,
        cycleDuration: Int = 28,
        periodDuration: Int = 5,
        symptoms: String = "",
        mood: String = ""
    ): Result<CycleResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val request = CycleRequest(
            cycleStartDate = cycleStartDate,
            cycleDuration = cycleDuration,
            periodDuration = periodDuration,
            symptoms = symptoms,
            mood = mood
        )
        val response = apiService.createCycle("Bearer $token", request)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to create cycle"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCycles(page: Int = 1): Result<List<CycleResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getCycles("Bearer $token", page)
        
        if (response.success && response.data != null) {
            Result.success(response.data.items)
        } else {
            Result.failure(Exception(response.message ?: "Failed to get cycles"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getLatestCycle(): Result<CycleResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getLatestCycle("Bearer $token")
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "No cycle data found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateCycle(id: Int, cycle: CycleRequest): Result<CycleResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.updateCycle("Bearer $token", id, cycle)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to update cycle"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteCycle(id: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.deleteCycle("Bearer $token", id)
        
        if (response.success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.message ?: "Failed to delete cycle"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
