package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.MoodRequest
import com.example.loveapp.data.api.models.MoodResponse
import com.example.loveapp.data.dao.MoodEntryDao
import javax.inject.Inject

class MoodRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val moodEntryDao: MoodEntryDao,
    private val authRepository: AuthRepository
) {
    suspend fun createMood(moodType: String, date: String, note: String = ""): Result<MoodResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.createMood("Bearer $token", MoodRequest(moodType, date, note))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message ?: "Failed to create mood"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateMood(id: Int, moodType: String, note: String = ""): Result<MoodResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.updateMood("Bearer $token", id, MoodRequest(moodType, "", note))
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message ?: "Failed to update mood"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getMoods(
        date: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1
    ): Result<List<MoodResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getMoods("Bearer $token", date, startDate, endDate, page, 500)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.failure(Exception(response.message ?: "Failed to get moods"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getPartnerMoods(
        date: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<List<MoodResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getPartnerMoods("Bearer $token", date, startDate, endDate)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.failure(Exception(response.message ?: "Failed to get partner moods"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteMood(id: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.deleteMood("Bearer $token", id)
        if (response.success) Result.success(Unit)
        else Result.failure(Exception(response.message ?: "Failed to delete mood"))
    } catch (e: Exception) { Result.failure(e) }
}

