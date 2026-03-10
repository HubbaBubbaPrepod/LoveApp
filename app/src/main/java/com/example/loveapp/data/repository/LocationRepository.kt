package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.*
import javax.inject.Inject

class LocationRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) {
    suspend fun updateLocation(request: LocationUpdateRequest): Result<LocationPointResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.updateLocation("Bearer $token", request)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getLatestLocations(): Result<LatestLocationsResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getLatestLocations("Bearer $token")
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getLocationHistory(
        user: String? = null,
        hours: Int? = null,
        limit: Int? = null
    ): Result<LocationHistoryResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getLocationHistory("Bearer $token", user, hours, limit)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getLocationStats(): Result<LocationStatsResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getLocationStats("Bearer $token")
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getLocationSettings(): Result<LocationSettingsResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getLocationSettings("Bearer $token")
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateLocationSettings(request: LocationSettingsRequest): Result<LocationSettingsResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.updateLocationSettings("Bearer $token", request)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }
}
