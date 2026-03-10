package com.example.loveapp.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.PlaceRequest
import com.example.loveapp.data.api.models.PlaceResponse
import com.example.loveapp.data.repository.PlacesRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PlacesViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _places = MutableStateFlow<List<PlaceResponse>>(emptyList())
    val places: StateFlow<List<PlaceResponse>> = _places.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _placeSaved = MutableStateFlow(false)
    val placeSaved: StateFlow<Boolean> = _placeSaved.asStateFlow()

    /** Current device location (lat, lng) after user requests it. */
    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private var locationCancellationToken: CancellationTokenSource? = null

    init {
        loadPlaces()
    }

    private fun loadPlaces() {
        viewModelScope.launch {
            _isLoading.value = true
            placesRepository.getPlaces(category = _selectedCategory.value)
                .onSuccess { resp ->
                    _places.value = resp.items
                    _categories.value = resp.categories
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        loadPlaces()
    }

    fun addPlace(name: String, address: String? = null, category: String = "other", note: String? = null, latitude: Double? = null, longitude: Double? = null) {
        viewModelScope.launch {
            placesRepository.addPlace(
                PlaceRequest(name = name, address = address, category = category, note = note, latitude = latitude, longitude = longitude)
            )
                .onSuccess {
                    _placeSaved.value = true
                    loadPlaces()
                }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun updatePlace(id: Int, name: String, address: String? = null, category: String = "other", note: String? = null, latitude: Double? = null, longitude: Double? = null) {
        viewModelScope.launch {
            placesRepository.updatePlace(id,
                PlaceRequest(name = name, address = address, category = category, note = note, latitude = latitude, longitude = longitude)
            )
                .onSuccess { updated ->
                    _places.value = _places.value.map { if (it.id == id) updated else it }
                }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun deletePlace(id: Int) {
        viewModelScope.launch {
            placesRepository.deletePlace(id)
                .onSuccess { _places.value = _places.value.filter { it.id != id } }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation() {
        viewModelScope.launch {
            _isLocating.value = true
            try {
                locationCancellationToken?.cancel()
                locationCancellationToken = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    locationCancellationToken!!.token
                ).await()
                if (location != null) {
                    _currentLocation.value = location.latitude to location.longitude
                } else {
                    _errorMessage.value = "Не удалось определить местоположение"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка геолокации: ${e.message}"
            } finally {
                _isLocating.value = false
            }
        }
    }

    fun clearCurrentLocation() {
        _currentLocation.value = null
    }

    override fun onCleared() {
        super.onCleared()
        locationCancellationToken?.cancel()
    }

    fun refresh() { loadPlaces() }

    fun clearMessages() {
        _errorMessage.value = null
        _placeSaved.value = false
    }
}
