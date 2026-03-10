package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.entity.MemorialDay
import com.example.loveapp.data.repository.MemorialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemorialViewModel @Inject constructor(
    private val memorialRepository: MemorialRepository
) : ViewModel() {

    private val _memorialDays = MutableStateFlow<List<MemorialDay>>(emptyList())
    val memorialDays: StateFlow<List<MemorialDay>> = _memorialDays.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        viewModelScope.launch {
            memorialRepository.refreshFromServer()
            memorialRepository.observeAll()
                .distinctUntilChanged()
                .collect { _memorialDays.value = it }
        }
    }

    fun addMemorialDay(title: String, date: String, type: String = "custom",
                       icon: String = "💕", colorHex: String = "#FF6B9D",
                       repeatYearly: Boolean = true, reminderDays: Int = 1, note: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            memorialRepository.createMemorialDay(title, date, type, icon, colorHex, repeatYearly, reminderDays, note)
                .onSuccess { _successMessage.value = "Памятный день добавлен" }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateMemorialDay(localId: Int, title: String, date: String, type: String,
                          icon: String, colorHex: String, repeatYearly: Boolean,
                          reminderDays: Int, note: String) {
        viewModelScope.launch {
            _isLoading.value = true
            memorialRepository.updateMemorialDay(localId, title, date, type, icon, colorHex, repeatYearly, reminderDays, note)
                .onSuccess { _successMessage.value = "Обновлено" }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteMemorialDay(localId: Int) {
        viewModelScope.launch {
            memorialRepository.deleteMemorialDay(localId)
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun clearMessages() { _errorMessage.value = null; _successMessage.value = null }
}
