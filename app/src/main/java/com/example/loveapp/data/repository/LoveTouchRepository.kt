package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.LoveTouchSessionResponse
import javax.inject.Inject

class LoveTouchRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) {
    suspend fun startSession(): Result<LoveTouchSessionResponse> {
        return try {
            val token = authRepository.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val resp = apiService.startLoveTouchSession("Bearer $token")
            if (resp.success && resp.data != null) Result.success(resp.data)
            else Result.failure(Exception(resp.message ?: "Error"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun joinSession(sessionId: Int): Result<LoveTouchSessionResponse> {
        return try {
            val token = authRepository.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val resp = apiService.joinLoveTouchSession("Bearer $token", sessionId)
            if (resp.success && resp.data != null) Result.success(resp.data)
            else Result.failure(Exception(resp.message ?: "Error"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun endSession(sessionId: Int, heartsCount: Int): Result<LoveTouchSessionResponse> {
        return try {
            val token = authRepository.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val resp = apiService.endLoveTouchSession("Bearer $token", sessionId, mapOf("hearts_count" to heartsCount))
            if (resp.success && resp.data != null) Result.success(resp.data)
            else Result.failure(Exception(resp.message ?: "Error"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getHistory(): Result<List<LoveTouchSessionResponse>> {
        return try {
            val token = authRepository.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val resp = apiService.getLoveTouchHistory("Bearer $token")
            if (resp.success && resp.data != null) Result.success(resp.data)
            else Result.success(emptyList())
        } catch (e: Exception) { Result.failure(e) }
    }
}
