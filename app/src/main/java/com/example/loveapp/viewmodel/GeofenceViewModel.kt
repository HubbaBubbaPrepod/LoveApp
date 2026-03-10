package com.example.loveapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.*
import com.example.loveapp.data.repository.GeofenceRepository
import com.example.loveapp.location.GeofenceData
import com.example.loveapp.location.GeofenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeofenceViewModel @Inject constructor(
    private val geofenceRepository: GeofenceRepository,
    private val geofenceManager: GeofenceManager,
    application: Application
) : AndroidViewModel(application) {

    private val _geofences = MutableStateFlow<List<GeofenceResponse>>(emptyList())
    val geofences: StateFlow<List<GeofenceResponse>> = _geofences.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<GeofenceEventResponse>>(emptyList())
    val recentEvents: StateFlow<List<GeofenceEventResponse>> = _recentEvents.asStateFlow()

    private val _selectedGeofence = MutableStateFlow<GeofenceResponse?>(null)
    val selectedGeofence: StateFlow<GeofenceResponse?> = _selectedGeofence.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Category filter
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Creation mode (for picking location on map)
    private val _isCreatingMode = MutableStateFlow(false)
    val isCreatingMode: StateFlow<Boolean> = _isCreatingMode.asStateFlow()

    init {
        loadGeofences()
        loadRecentEvents()
    }

    fun loadGeofences() {
        viewModelScope.launch {
            _isLoading.value = true
            geofenceRepository.getGeofences()
                .onSuccess { list ->
                    _geofences.value = list
                    // Sync to OS geofencing client
                    syncGeofencesToOS(list.filter { it.isActive })
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun loadRecentEvents() {
        viewModelScope.launch {
            geofenceRepository.getRecentEvents(limit = 50)
                .onSuccess { _recentEvents.value = it }
        }
    }

    fun createGeofence(request: GeofenceRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            geofenceRepository.createGeofence(request)
                .onSuccess { created ->
                    _geofences.value = listOf(created) + _geofences.value
                    // Register with OS
                    if (created.isActive) {
                        geofenceManager.registerGeofence(
                            getApplication(),
                            requestId = created.id.toString(),
                            lat = created.latitude,
                            lon = created.longitude,
                            radiusMeters = created.radiusMeters.toFloat()
                        )
                    }
                    _isCreatingMode.value = false
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateGeofence(id: Long, request: GeofenceUpdateRequest) {
        viewModelScope.launch {
            geofenceRepository.updateGeofence(id, request)
                .onSuccess { updated ->
                    _geofences.value = _geofences.value.map {
                        if (it.id == id) updated else it
                    }
                    // Re-register with OS
                    geofenceManager.removeGeofence(id.toString())
                    if (updated.isActive) {
                        geofenceManager.registerGeofence(
                            getApplication(),
                            requestId = updated.id.toString(),
                            lat = updated.latitude,
                            lon = updated.longitude,
                            radiusMeters = updated.radiusMeters.toFloat()
                        )
                    }
                    _selectedGeofence.value = updated
                }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun deleteGeofence(id: Long) {
        viewModelScope.launch {
            geofenceRepository.deleteGeofence(id)
                .onSuccess {
                    _geofences.value = _geofences.value.filter { it.id != id }
                    geofenceManager.removeGeofence(id.toString())
                    if (_selectedGeofence.value?.id == id) {
                        _selectedGeofence.value = null
                    }
                }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun toggleGeofence(id: Long, active: Boolean) {
        updateGeofence(id, GeofenceUpdateRequest(isActive = active))
    }

    fun selectGeofence(geofence: GeofenceResponse?) {
        _selectedGeofence.value = geofence
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleCreatingMode() {
        _isCreatingMode.value = !_isCreatingMode.value
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun syncGeofencesToOS(activeGeofences: List<GeofenceResponse>) {
        if (activeGeofences.isEmpty()) return
        val data = activeGeofences.map { gf ->
            GeofenceData(
                requestId = gf.id.toString(),
                latitude = gf.latitude,
                longitude = gf.longitude,
                radiusMeters = gf.radiusMeters.toFloat(),
                notifyOnEnter = gf.notifyOnEnter,
                notifyOnExit = gf.notifyOnExit
            )
        }
        geofenceManager.registerGeofences(getApplication(), data)
    }

    val filteredGeofences: StateFlow<List<GeofenceResponse>>
        get() = _geofences // Filtering done in the UI composable based on selectedCategory

    companion object {
        val CATEGORIES = listOf(
            "home" to "🏠 Дом",
            "work" to "💼 Работа",
            "school" to "🎓 Учёба",
            "restaurant" to "🍽 Ресторан",
            "cafe" to "☕ Кафе",
            "cinema" to "🎬 Кино",
            "park" to "🌳 Парк",
            "beach" to "🏖 Пляж",
            "bar" to "🍸 Бар",
            "concert" to "🎵 Концерт",
            "museum" to "🏛 Музей",
            "gym" to "🏋️ Спортзал",
            "hospital" to "🏥 Больница",
            "airport" to "✈️ Аэропорт",
            "shop" to "🛍 Магазин",
            "hotel" to "🏨 Отель",
            "travel" to "🗺 Путешествие",
            "date" to "💕 Свидание",
            "other" to "📍 Другое"
        )

        fun categoryLabel(category: String): String {
            return CATEGORIES.firstOrNull { it.first == category }?.second ?: "📍 $category"
        }

        fun categoryIcon(category: String): String {
            return CATEGORIES.firstOrNull { it.first == category }?.second?.take(2)?.trim() ?: "📍"
        }
    }
}
