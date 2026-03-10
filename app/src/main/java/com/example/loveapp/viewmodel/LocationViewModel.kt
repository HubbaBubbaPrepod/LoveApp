package com.example.loveapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.*
import com.example.loveapp.data.repository.LocationRepository
import com.example.loveapp.location.LocationTrackingService
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _selfLocation = MutableStateFlow<LocationPointResponse?>(null)
    val selfLocation: StateFlow<LocationPointResponse?> = _selfLocation.asStateFlow()

    private val _partnerLocation = MutableStateFlow<LocationPointResponse?>(null)
    val partnerLocation: StateFlow<LocationPointResponse?> = _partnerLocation.asStateFlow()

    private val _partnerHistory = MutableStateFlow<List<LocationPointResponse>>(emptyList())
    val partnerHistory: StateFlow<List<LocationPointResponse>> = _partnerHistory.asStateFlow()

    private val _selfHistory = MutableStateFlow<List<LocationPointResponse>>(emptyList())
    val selfHistory: StateFlow<List<LocationPointResponse>> = _selfHistory.asStateFlow()

    private val _stats = MutableStateFlow<LocationStatsResponse?>(null)
    val stats: StateFlow<LocationStatsResponse?> = _stats.asStateFlow()

    private val _settings = MutableStateFlow(LocationSettingsResponse())
    val settings: StateFlow<LocationSettingsResponse> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Orbit path — polyline for partner movement (last 2h)
    private val _orbitPath = MutableStateFlow<List<LatLng>>(emptyList())
    val orbitPath: StateFlow<List<LatLng>> = _orbitPath.asStateFlow()

    // Distance between partners
    private val _distanceKm = MutableStateFlow<Double?>(null)
    val distanceKm: StateFlow<Double?> = _distanceKm.asStateFlow()

    // History view hours selection
    private val _historyHours = MutableStateFlow(24)
    val historyHours: StateFlow<Int> = _historyHours.asStateFlow()

    // Show orbit trail
    private val _showOrbit = MutableStateFlow(true)
    val showOrbit: StateFlow<Boolean> = _showOrbit.asStateFlow()

    init {
        loadLatestLocations()
        loadSettings()
        loadStats()
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000) // Refresh every 30 seconds
                loadLatestLocations()
                loadStats()
            }
        }
    }

    fun loadLatestLocations() {
        viewModelScope.launch {
            locationRepository.getLatestLocations()
                .onSuccess { data ->
                    _selfLocation.value = data.self
                    _partnerLocation.value = data.partner
                    // Calculate distance
                    if (data.self != null && data.partner != null) {
                        _distanceKm.value = haversineDistance(
                            data.self.latitude, data.self.longitude,
                            data.partner.latitude, data.partner.longitude
                        )
                    }
                }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun loadPartnerHistory(hours: Int = _historyHours.value) {
        viewModelScope.launch {
            _isLoading.value = true
            locationRepository.getLocationHistory(user = "partner", hours = hours, limit = 1000)
                .onSuccess { data ->
                    _partnerHistory.value = data.points
                    _orbitPath.value = data.points.map { LatLng(it.latitude, it.longitude) }
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun loadSelfHistory(hours: Int = _historyHours.value) {
        viewModelScope.launch {
            locationRepository.getLocationHistory(user = "self", hours = hours, limit = 1000)
                .onSuccess { _selfHistory.value = it.points }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            locationRepository.getLocationStats()
                .onSuccess { _stats.value = it }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            locationRepository.getLocationSettings()
                .onSuccess { _settings.value = it }
        }
    }

    fun updateSettings(request: LocationSettingsRequest) {
        viewModelScope.launch {
            locationRepository.updateLocationSettings(request)
                .onSuccess { updated ->
                    _settings.value = updated
                    // Restart tracking service with new interval if needed
                    if (updated.sharingEnabled) {
                        val intervalMs = updated.updateIntervalSec * 1000L
                        LocationTrackingService.start(getApplication(), intervalMs)
                    } else {
                        LocationTrackingService.stop(getApplication())
                    }
                }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun toggleSharing(enabled: Boolean) {
        updateSettings(LocationSettingsRequest(sharingEnabled = enabled))
    }

    fun setHistoryHours(hours: Int) {
        _historyHours.value = hours
        loadPartnerHistory(hours)
    }

    fun toggleOrbit() {
        _showOrbit.value = !_showOrbit.value
    }

    fun startTracking() {
        val intervalMs = _settings.value.updateIntervalSec * 1000L
        LocationTrackingService.start(getApplication(), intervalMs)
    }

    fun stopTracking() {
        LocationTrackingService.stop(getApplication())
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val R = 6371.0 // Earth radius in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return R * c
        }
    }
}
