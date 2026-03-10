package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.MoodAnalyticsResponse
import com.example.loveapp.data.repository.AuthRepository
import com.example.loveapp.data.api.LoveAppApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodInsightsViewModel @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _analytics = MutableStateFlow<MoodAnalyticsResponse?>(null)
    val analytics: StateFlow<MoodAnalyticsResponse?> = _analytics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val token = authRepository.getToken()
                if (token == null) {
                    _errorMessage.value = "Требуется авторизация"
                    _isLoading.value = false
                    return@launch
                }
                val resp = apiService.getMoodAnalytics("Bearer $token")
                if (resp.success && resp.data != null) {
                    _analytics.value = resp.data
                } else {
                    _errorMessage.value = resp.message ?: "Ошибка загрузки аналитики"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка сети: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _errorMessage.value = null }
}
