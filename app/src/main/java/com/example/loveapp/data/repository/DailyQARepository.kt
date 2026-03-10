package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.DailyAnswerRequest
import com.example.loveapp.data.api.models.DailyAnswerResponse
import com.example.loveapp.data.api.models.DailyQAHistoryItem
import com.example.loveapp.data.api.models.DailyQATodayResponse
import javax.inject.Inject

class DailyQARepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) {
    suspend fun getTodayQuestion(): Result<DailyQATodayResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getDailyQuestion("Bearer $token")
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun submitAnswer(questionId: Int, answer: String): Result<DailyAnswerResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.submitDailyAnswer("Bearer $token", questionId, DailyAnswerRequest(answer))
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getHistory(page: Int = 1): Result<List<DailyQAHistoryItem>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getDailyQAHistory("Bearer $token", page)
        if (resp.success && resp.data != null) Result.success(resp.data.items)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }
}
