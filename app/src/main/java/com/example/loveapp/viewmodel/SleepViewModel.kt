package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.SleepStatsResponse
import com.example.loveapp.data.entity.SleepEntry
import com.example.loveapp.data.repository.SleepRepository
import com.example.loveapp.utils.DateUtils
import com.example.loveapp.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val sleepRepository: SleepRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _entries = MutableStateFlow<List<SleepEntry>>(emptyList())
    val entries: StateFlow<List<SleepEntry>> = _entries.asStateFlow()

    private val _stats = MutableStateFlow<SleepStatsResponse?>(null)
    val stats: StateFlow<SleepStatsResponse?> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        viewModelScope.launch {
            sleepRepository.refreshFromServer()
            loadStats()
            sleepRepository.observeAll()
                .distinctUntilChanged()
                .collect { _entries.value = it }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            sleepRepository.getStats(7).onSuccess { _stats.value = it }
        }
    }

    fun saveSleepEntry(date: String, bedtime: String?, wakeTime: String?,
                       durationMinutes: Int?, quality: Int?, note: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            sleepRepository.saveSleepEntry(date, bedtime, wakeTime, durationMinutes, quality, note)
                .onSuccess { _successMessage.value = "Запись сна сохранена"; loadStats() }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteSleepEntry(localId: Int) {
        viewModelScope.launch {
            sleepRepository.deleteSleepEntry(localId)
                .onSuccess { loadStats() }
        }
    }

    fun clearMessages() { _errorMessage.value = null; _successMessage.value = null }
}
