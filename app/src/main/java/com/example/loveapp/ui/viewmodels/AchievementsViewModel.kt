package com.example.loveapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.AchievementProgressResponse
import com.example.loveapp.data.api.models.AchievementResponse
import com.example.loveapp.data.repository.AchievementsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val repository: AchievementsRepository
) : ViewModel() {

    private val _achievements = MutableStateFlow<List<AchievementResponse>>(emptyList())
    val achievements: StateFlow<List<AchievementResponse>> = _achievements

    private val _progress = MutableStateFlow(AchievementProgressResponse())
    val progress: StateFlow<AchievementProgressResponse> = _progress

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _newlyUnlocked = MutableStateFlow<List<AchievementResponse>>(emptyList())
    val newlyUnlocked: StateFlow<List<AchievementResponse>> = _newlyUnlocked

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getAchievements()
                .onSuccess { _achievements.value = it }
                .onFailure {
                    _error.value = it.message
                    _isLoading.value = false
                    return@launch
                }

            repository.getProgress()
                .onSuccess { _progress.value = it }
                .onFailure { _error.value = "Ошибка загрузки прогресса: ${it.message}" }

            // Auto-check for new achievements
            repository.checkAchievements()
                .onSuccess { response ->
                    if (response.count > 0) {
                        _newlyUnlocked.value = response.newlyUnlocked
                        repository.getAchievements().onSuccess { _achievements.value = it }
                    }
                }
                .onFailure { /* Non-critical: silent fail on achievement check */ }

            _isLoading.value = false
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun clearNewlyUnlocked() {
        _newlyUnlocked.value = emptyList()
    }
}
