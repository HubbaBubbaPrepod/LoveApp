package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.SparkLogRequest
import com.example.loveapp.data.api.models.SparkHistoryItem
import com.example.loveapp.data.api.models.SparkBreakdownResponse
import com.example.loveapp.data.dao.SparkStreakDao
import com.example.loveapp.data.entity.SparkStreak
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SparkRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val sparkStreakDao: SparkStreakDao,
    private val authRepository: AuthRepository
) {
    fun observeStreak(coupleKey: String): Flow<SparkStreak?> =
        sparkStreakDao.observe(coupleKey)

    suspend fun logSpark(sparkType: String = "manual"): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.logSpark("Bearer $token", SparkLogRequest(sparkType))
        if (resp.success) { refreshFromServer(); Result.success(Unit) }
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getHistory(days: Int = 7): Result<List<SparkHistoryItem>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getSparkHistory("Bearer $token", days)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getBreakdown(): Result<SparkBreakdownResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getSparkBreakdown("Bearer $token")
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        try {
            val resp = apiService.getSparkStreak("Bearer $token")
            if (resp.success && resp.data != null) {
                val d = resp.data
                val ex = sparkStreakDao.get(d.coupleKey)
                sparkStreakDao.upsert(SparkStreak(
                    id = ex?.id ?: 0,
                    coupleKey = d.coupleKey,
                    currentStreak = d.currentStreak,
                    longestStreak = d.longestStreak,
                    lastSparkDate = d.lastSparkDate,
                    totalSparks = d.totalSparks,
                    serverId = d.id,
                    syncPending = false
                ))
            }
        } catch (_: Exception) {}
    }
}
