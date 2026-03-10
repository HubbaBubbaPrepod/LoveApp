package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.IntimacyLogItem
import com.example.loveapp.data.api.models.IntimacyScoreResponse
import com.example.loveapp.data.repository.IntimacyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntimacyViewModel @Inject constructor(
    private val intimacyRepository: IntimacyRepository
) : ViewModel() {

    private val _score = MutableStateFlow<IntimacyScoreResponse?>(null)
    val score: StateFlow<IntimacyScoreResponse?> = _score.asStateFlow()

    private val _history = MutableStateFlow<List<IntimacyLogItem>>(emptyList())
    val history: StateFlow<List<IntimacyLogItem>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            loadScore()
            loadHistory()
        }
    }

    private suspend fun loadScore() {
        _isLoading.value = true
        intimacyRepository.getScore()
            .onSuccess { _score.value = it }
            .onFailure { _errorMessage.value = it.message }
        _isLoading.value = false
    }

    private suspend fun loadHistory() {
        intimacyRepository.getHistory()
            .onSuccess { _history.value = it }
    }

    fun refresh() {
        viewModelScope.launch {
            loadScore()
            loadHistory()
        }
    }

    fun clearError() { _errorMessage.value = null }
}
