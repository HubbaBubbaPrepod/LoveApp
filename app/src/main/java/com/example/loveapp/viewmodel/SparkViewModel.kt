package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.SparkHistoryItem
import com.example.loveapp.data.api.models.SparkBreakdownResponse
import com.example.loveapp.data.entity.SparkStreak
import com.example.loveapp.data.repository.RelationshipRepository
import com.example.loveapp.data.repository.SparkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SparkViewModel @Inject constructor(
    private val sparkRepository: SparkRepository,
    private val relationshipRepository: RelationshipRepository
) : ViewModel() {

    private val _streak = MutableStateFlow<SparkStreak?>(null)
    val streak: StateFlow<SparkStreak?> = _streak.asStateFlow()

    private val _history = MutableStateFlow<List<SparkHistoryItem>>(emptyList())
    val history: StateFlow<List<SparkHistoryItem>> = _history.asStateFlow()

    private val _breakdown = MutableStateFlow<SparkBreakdownResponse?>(null)
    val breakdown: StateFlow<SparkBreakdownResponse?> = _breakdown.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var coupleKey: String = ""

    init {
        viewModelScope.launch {
            loadCoupleKey()
            sparkRepository.refreshFromServer()
            loadHistory()
            loadBreakdown()
            if (coupleKey.isNotBlank()) {
                sparkRepository.observeStreak(coupleKey).collect { _streak.value = it }
            }
        }
    }

    private suspend fun loadCoupleKey() {
        relationshipRepository.getRelationship()
            .onSuccess {
                val uid1 = it.userId1
                val uid2 = it.userId2
                if (uid1 > 0 && uid2 > 0) {
                    coupleKey = "${minOf(uid1, uid2)}_${maxOf(uid1, uid2)}"
                }
            }
            .onFailure {
                _errorMessage.value = "Не удалось загрузить данные пары"
            }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            sparkRepository.getHistory(14).onSuccess { _history.value = it }
        }
    }

    private fun loadBreakdown() {
        viewModelScope.launch {
            sparkRepository.getBreakdown().onSuccess { _breakdown.value = it }
        }
    }

    fun logSpark() {
        viewModelScope.launch {
            _isLoading.value = true
            sparkRepository.logSpark()
                .onFailure { _errorMessage.value = it.message }
            loadHistory()
            _isLoading.value = false
        }
    }

    fun clearMessages() { _errorMessage.value = null }
}
