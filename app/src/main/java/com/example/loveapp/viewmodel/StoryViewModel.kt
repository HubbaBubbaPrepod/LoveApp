package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.StoryEntryRequest
import com.example.loveapp.data.api.models.StoryEntryResponse
import com.example.loveapp.data.api.models.StoryStatsResponse
import com.example.loveapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<StoryEntryResponse>>(emptyList())
    val entries: StateFlow<List<StoryEntryResponse>> = _entries.asStateFlow()

    private val _stats = MutableStateFlow<StoryStatsResponse?>(null)
    val stats: StateFlow<StoryStatsResponse?> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentPage = 1
    private var totalEntries = 0

    init {
        loadEntries()
        loadStats()
    }

    fun loadEntries() {
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
                val resp = apiService.getStoryEntries("Bearer $token", page = 1, limit = 50)
                if (resp.success && resp.data != null) {
                    _entries.value = resp.data.items
                    totalEntries = resp.data.total
                    currentPage = 1
                } else {
                    _errorMessage.value = resp.message ?: "Ошибка загрузки"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка сети: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val token = authRepository.getToken() ?: return@launch
                val resp = apiService.getStoryStats("Bearer $token")
                if (resp.success && resp.data != null) {
                    _stats.value = resp.data
                }
            } catch (_: Exception) { }
        }
    }

    fun createEntry(title: String, content: String, entryType: String, entryDate: String?, emoji: String) {
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
                val req = StoryEntryRequest(
                    title = title,
                    content = content,
                    entryType = entryType,
                    entryDate = entryDate,
                    emoji = emoji
                )
                val resp = apiService.createStoryEntry("Bearer $token", req)
                if (resp.success) {
                    loadEntries()
                    loadStats()
                } else {
                    _errorMessage.value = resp.message ?: "Ошибка создания"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            try {
                val token = authRepository.getToken() ?: return@launch
                val resp = apiService.deleteStoryEntry("Bearer $token", id)
                if (resp.success) {
                    _entries.value = _entries.value.filter { it.id != id }
                    loadStats()
                }
            } catch (_: Exception) { }
        }
    }

    fun clearError() { _errorMessage.value = null }
}
