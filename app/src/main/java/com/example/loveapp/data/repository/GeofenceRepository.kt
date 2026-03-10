package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.*
import com.example.loveapp.data.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) {
    private suspend fun token() = "Bearer ${authRepository.getToken()}"

    suspend fun createGeofence(request: GeofenceRequest): Result<GeofenceResponse> = runCatching {
        apiService.createGeofence(token(), request).data!!
    }

    suspend fun getGeofences(): Result<List<GeofenceResponse>> = runCatching {
        apiService.getGeofences(token()).data ?: emptyList()
    }

    suspend fun getGeofence(id: Long): Result<GeofenceResponse> = runCatching {
        apiService.getGeofence(token(), id).data!!
    }

    suspend fun updateGeofence(id: Long, request: GeofenceUpdateRequest): Result<GeofenceResponse> = runCatching {
        apiService.updateGeofence(token(), id, request).data!!
    }

    suspend fun deleteGeofence(id: Long): Result<Unit> = runCatching {
        apiService.deleteGeofence(token(), id)
        Unit
    }

    suspend fun reportEvent(request: GeofenceEventRequest): Result<GeofenceEventResponse> = runCatching {
        apiService.reportGeofenceEvent(token(), request).data!!
    }

    suspend fun getGeofenceEvents(id: Long, limit: Int? = null, hours: Int? = null): Result<List<GeofenceEventResponse>> = runCatching {
        apiService.getGeofenceEvents(token(), id, limit, hours).data ?: emptyList()
    }

    suspend fun getRecentEvents(limit: Int? = null): Result<List<GeofenceEventResponse>> = runCatching {
        apiService.getRecentGeofenceEvents(token(), limit).data ?: emptyList()
    }
}
