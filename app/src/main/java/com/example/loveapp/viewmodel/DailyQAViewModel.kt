package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.DailyQAHistoryItem
import com.example.loveapp.data.api.models.DailyQATodayResponse
import com.example.loveapp.data.repository.DailyQARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyQAViewModel @Inject constructor(
    private val dailyQARepository: DailyQARepository
) : ViewModel() {

    private val _today = MutableStateFlow<DailyQATodayResponse?>(null)
    val today: StateFlow<DailyQATodayResponse?> = _today.asStateFlow()

    private val _history = MutableStateFlow<List<DailyQAHistoryItem>>(emptyList())
    val history: StateFlow<List<DailyQAHistoryItem>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _answerSubmitted = MutableStateFlow(false)
    val answerSubmitted: StateFlow<Boolean> = _answerSubmitted.asStateFlow()

    init {
        viewModelScope.launch {
            loadToday()
            loadHistory()
        }
    }

    private suspend fun loadToday() {
        _isLoading.value = true
        dailyQARepository.getTodayQuestion()
            .onSuccess { _today.value = it }
            .onFailure { _errorMessage.value = it.message }
        _isLoading.value = false
    }

    private suspend fun loadHistory() {
        dailyQARepository.getHistory()
            .onSuccess { _history.value = it }
    }

    fun submitAnswer(answer: String) {
        val questionId = _today.value?.question?.id ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            dailyQARepository.submitAnswer(questionId, answer)
                .onSuccess {
                    _answerSubmitted.value = true
                    loadToday()
                    loadHistory()
                }
                .onFailure { _errorMessage.value = it.message }
            _isSubmitting.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadToday()
            loadHistory()
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _answerSubmitted.value = false
    }
}
