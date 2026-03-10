package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.IntimacyLogItem
import com.example.loveapp.data.api.models.IntimacyScoreResponse
import javax.inject.Inject

class IntimacyRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) {
    suspend fun getScore(): Result<IntimacyScoreResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getIntimacyScore("Bearer $token")
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getHistory(page: Int = 1): Result<List<IntimacyLogItem>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getIntimacyHistory("Bearer $token", page)
        if (resp.success && resp.data != null) Result.success(resp.data.items)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }
}
