package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.ChatSettingsResponse
import com.example.loveapp.data.api.models.WallpaperItem
import com.example.loveapp.data.repository.ChatSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val chatSettingsRepository: ChatSettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<ChatSettingsResponse?>(null)
    val settings: StateFlow<ChatSettingsResponse?> = _settings.asStateFlow()

    private val _wallpapers = MutableStateFlow<List<WallpaperItem>>(emptyList())
    val wallpapers: StateFlow<List<WallpaperItem>> = _wallpapers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private var settingsUpdateJob: Job? = null

    val bubbleColors = listOf(
        "default" to "По умолчанию",
        "rose" to "Розовый",
        "sky" to "Голубой",
        "lavender" to "Лавандовый",
        "mint" to "Мятный",
        "sunset" to "Закат",
        "ocean" to "Океан"
    )

    val bubbleShapes = listOf(
        "rounded" to "Округлый",
        "classic" to "Классический",
        "sharp" to "Острый",
        "tail" to "С хвостиком"
    )

    init {
        viewModelScope.launch {
            loadSettings()
            loadWallpapers()
        }
    }

    private suspend fun loadSettings() {
        _isLoading.value = true
        chatSettingsRepository.getSettings()
            .onSuccess { _settings.value = it }
            .onFailure { _errorMessage.value = it.message }
        _isLoading.value = false
    }

    private suspend fun loadWallpapers() {
        chatSettingsRepository.getWallpapers()
            .onSuccess { _wallpapers.value = it }
    }

    fun selectWallpaper(url: String?) {
        settingsUpdateJob?.cancel()
        settingsUpdateJob = viewModelScope.launch {
            _isLoading.value = true
            chatSettingsRepository.updateSettings(wallpaperUrl = url ?: "")
                .onSuccess {
                    _settings.value = it
                    _successMessage.value = "Обои обновлены"
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun selectBubbleColor(color: String) {
        settingsUpdateJob?.cancel()
        settingsUpdateJob = viewModelScope.launch {
            _isLoading.value = true
            chatSettingsRepository.updateSettings(bubbleColor = color)
                .onSuccess {
                    _settings.value = it
                    _successMessage.value = "Стиль пузырей обновлён"
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun selectBubbleShape(shape: String) {
        settingsUpdateJob?.cancel()
        settingsUpdateJob = viewModelScope.launch {
            _isLoading.value = true
            chatSettingsRepository.updateSettings(bubbleShape = shape)
                .onSuccess {
                    _settings.value = it
                    _successMessage.value = "Форма пузырей обновлена"
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
