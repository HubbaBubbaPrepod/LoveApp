package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.CycleResponse
import com.example.loveapp.data.repository.CycleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CycleViewModel @Inject constructor(
    private val cycleRepository: CycleRepository
) : ViewModel() {

    private val _cycles = MutableStateFlow<List<CycleResponse>>(emptyList())
    val cycles: StateFlow<List<CycleResponse>> = _cycles.asStateFlow()

    private val _currentCycle = MutableStateFlow<CycleResponse?>(null)
    val currentCycle: StateFlow<CycleResponse?> = _currentCycle.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadCycles()
    }

    fun loadCycles() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = cycleRepository.getCycles()
            result.onSuccess { cycles ->
                _cycles.value = cycles
                if (cycles.isNotEmpty()) {
                    _currentCycle.value = cycles.first()
                }
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to load cycles"
                _isLoading.value = false
            }
        }
    }

    fun createCycle(startDate: String, cycleDuration: Int, periodDuration: Int, symptoms: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = cycleRepository.createCycle(startDate, cycleDuration, periodDuration, symptoms)
            
            result.onSuccess { cycle ->
                _cycles.value = listOf(cycle) + _cycles.value
                _currentCycle.value = cycle
                _successMessage.value = "Cycle recorded"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to record cycle"
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
