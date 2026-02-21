package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.CustomCalendarResponse
import com.example.loveapp.data.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    private val _calendars = MutableStateFlow<List<CustomCalendarResponse>>(emptyList())
    val calendars: StateFlow<List<CustomCalendarResponse>> = _calendars.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadCalendars()
    }

    fun loadCalendars() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = calendarRepository.getCalendars()
            result.onSuccess { calendars ->
                _calendars.value = calendars
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to load calendars"
                _isLoading.value = false
            }
        }
    }

    fun createCalendar(name: String, type: String, color: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = calendarRepository.createCalendar(name, type, color)
            
            result.onSuccess { calendar ->
                _calendars.value = listOf(calendar) + _calendars.value
                _successMessage.value = "Calendar created: $name"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to create calendar"
                _isLoading.value = false
            }
        }
    }

    fun deleteCalendar(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = calendarRepository.deleteCalendar(id)
            
            result.onSuccess {
                _calendars.value = _calendars.value.filter { it.id != id }
                _successMessage.value = "Calendar deleted"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to delete calendar"
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
