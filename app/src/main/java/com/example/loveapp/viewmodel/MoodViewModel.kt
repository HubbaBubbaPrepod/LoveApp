package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.MoodResponse
import com.example.loveapp.data.repository.MoodRepository
import com.example.loveapp.ui.theme.MoodExcellent
import com.example.loveapp.ui.theme.MoodGood
import com.example.loveapp.ui.theme.MoodNeutral
import com.example.loveapp.ui.theme.MoodRomantic
import com.example.loveapp.ui.theme.MoodSad
import com.example.loveapp.ui.theme.MoodTired
import com.example.loveapp.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodViewModel @Inject constructor(
    private val moodRepository: MoodRepository
) : ViewModel() {

    private val _todayMoods = MutableStateFlow<List<MoodResponse>>(emptyList())
    val todayMoods: StateFlow<List<MoodResponse>> = _todayMoods.asStateFlow()

    private val _monthlyMoods = MutableStateFlow<List<MoodResponse>>(emptyList())
    val monthlyMoods: StateFlow<List<MoodResponse>> = _monthlyMoods.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val moodTypes = listOf("Excellent", "Good", "Neutral", "Sad", "Romantic", "Nervous", "Tired")
    val moodColors = mapOf(
        "Excellent" to MoodExcellent,
        "Good" to MoodGood,
        "Neutral" to MoodNeutral,
        "Sad" to MoodSad,
        "Romantic" to MoodRomantic,
        "Nervous" to MoodRomantic,
        "Tired" to MoodTired
    )

    fun addMood(moodType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val today = DateUtils.getTodayDateString()
            val result = moodRepository.createMood(moodType, today)
            
            result.onSuccess {
                loadTodayMoods()
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to add mood"
                _isLoading.value = false
            }
        }
    }

    fun loadTodayMoods() {
        viewModelScope.launch {
            _isLoading.value = true
            val today = DateUtils.getTodayDateString()
            val result = moodRepository.getMoods(today)
            
            result.onSuccess { moods ->
                _todayMoods.value = moods
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to load moods"
                _isLoading.value = false
            }
        }
    }

    fun loadMonthlyMoods(startDate: String, endDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Load each day of the month
            val result = moodRepository.getMoods(startDate)
            
            result.onSuccess { moods ->
                _monthlyMoods.value = moods
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to load monthly moods"
                _isLoading.value = false
            }
        }
    }

    fun deleteMood(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = moodRepository.deleteMood(id)
            
            result.onSuccess {
                loadTodayMoods()
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to delete mood"
                _isLoading.value = false
            }
        }
    }
}
