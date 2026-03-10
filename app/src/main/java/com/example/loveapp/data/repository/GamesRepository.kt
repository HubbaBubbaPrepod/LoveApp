package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.GameAnswerRequest
import com.example.loveapp.data.api.models.GameRoundResponse
import com.example.loveapp.data.api.models.GameSessionResponse
import com.example.loveapp.data.api.models.GameStartRequest
import javax.inject.Inject

class GamesRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) {
    suspend fun startGame(gameType: String): Result<GameSessionResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.startGame("Bearer $token", GameStartRequest(gameType))
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getSession(id: Int): Result<GameSessionResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getGameSession("Bearer $token", id)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun submitAnswer(sessionId: Int, roundNumber: Int, answer: String): Result<GameRoundResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.submitGameAnswer("Bearer $token", GameAnswerRequest(sessionId, roundNumber, answer))
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getGames(limit: Int = 20): Result<List<GameSessionResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getGames("Bearer $token", limit)
        if (resp.success && resp.data != null) Result.success(resp.data.items)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }
}
