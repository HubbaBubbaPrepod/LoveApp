package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.CyclePatchRequest
import com.example.loveapp.data.api.models.CycleRequest
import com.example.loveapp.data.api.models.CycleResponse
import com.example.loveapp.data.dao.MenstrualCycleDao
import com.example.loveapp.utils.DateUtils
import javax.inject.Inject

class CycleRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val cycleDao: MenstrualCycleDao,
    private val authRepository: AuthRepository
) {
    //  My cycles 

    suspend fun getCycles(limit: Int = 100): Result<List<CycleResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getCycles(token = "Bearer $token", limit = limit)
        if (response.success && response.data != null)
            Result.success(response.data.items)
        else
            Result.failure(Exception(response.message ?: "Failed to get cycles"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getLatestCycle(): Result<CycleResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getLatestCycle("Bearer $token")
        if (response.success && response.data != null)
            Result.success(response.data)
        else
            Result.failure(Exception(response.message ?: "No cycle data found"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun createCycle(
        cycleStartDate: String,
        cycleDuration: Int = 28,
        periodDuration: Int = 5,
        symptoms: Map<String, List<String>> = emptyMap(),
        mood: Map<String, String> = emptyMap(),
        notes: String = ""
    ): Result<CycleResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val request = CycleRequest(
            cycleStartDate = cycleStartDate,
            cycleDuration  = cycleDuration,
            periodDuration = periodDuration,
            symptoms       = symptoms,
            mood           = mood,
            notes          = notes
        )
        val response = apiService.createCycle("Bearer $token", request)
        if (response.success && response.data != null)
            Result.success(response.data)
        else
            Result.failure(Exception(response.message ?: "Failed to create cycle"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateCycle(id: Int, cycle: CycleRequest): Result<CycleResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.updateCycle("Bearer $token", id, cycle)
        if (response.success && response.data != null)
            Result.success(response.data)
        else
            Result.failure(Exception(response.message ?: "Failed to update cycle"))
    } catch (e: Exception) { Result.failure(e) }

    /** Patch a single day's symptoms/mood on an existing cycle record */
    suspend fun patchCycleDay(
        cycleId: Int,
        date: String,
        symptomsDay: List<String>? = null,
        moodDay: String? = null
    ): Result<CycleResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.patchCycle(
            token   = "Bearer $token",
            id      = cycleId,
            request = CyclePatchRequest(date = date, symptomsDay = symptomsDay, moodDay = moodDay)
        )
        if (response.success && response.data != null)
            Result.success(response.data)
        else
            Result.failure(Exception(response.message ?: "Failed to patch cycle"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteCycle(id: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.deleteCycle("Bearer $token", id)
        if (response.success)
            Result.success(Unit)
        else
            Result.failure(Exception(response.message ?: "Failed to delete cycle"))
    } catch (e: Exception) { Result.failure(e) }

    //  Partner cycles (read-only) 

    suspend fun getPartnerCycles(limit: Int = 100): Result<List<CycleResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getPartnerCycles(token = "Bearer $token", limit = limit)
        if (response.success && response.data != null)
            Result.success(response.data.items)
        else
            Result.success(emptyList())   // no partner = empty, not an error
    } catch (e: Exception) { Result.success(emptyList()) }
}