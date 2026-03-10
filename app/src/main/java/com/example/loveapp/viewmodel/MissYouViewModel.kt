package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.MissYouTodayResponse
import com.example.loveapp.data.entity.MissYouEvent
import com.example.loveapp.data.repository.MissYouRepository
import com.example.loveapp.data.repository.RelationshipRepository
import com.example.loveapp.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MissYouViewModel @Inject constructor(
    private val missYouRepository: MissYouRepository,
    private val relationshipRepository: RelationshipRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _events = MutableStateFlow<List<MissYouEvent>>(emptyList())
    val events: StateFlow<List<MissYouEvent>> = _events.asStateFlow()

    private val _todayStats = MutableStateFlow(MissYouTodayResponse())
    val todayStats: StateFlow<MissYouTodayResponse> = _todayStats.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var myUserId: Int = 0
    private var coupleKey: String = ""

    init {
        viewModelScope.launch {
            myUserId = tokenManager.getUserId()?.toIntOrNull() ?: 0
            loadCoupleKey()
            refreshFromServer()
            loadTodayStats()
            observeEvents()
        }
    }

    private suspend fun loadCoupleKey() {
        relationshipRepository.getRelationship().onSuccess {
            val uid1 = it.userId1
            val uid2 = it.userId2
            if (uid1 > 0 && uid2 > 0) {
                coupleKey = "${minOf(uid1, uid2)}_${maxOf(uid1, uid2)}"
            }
        }
    }

    private suspend fun observeEvents() {
        if (coupleKey.isBlank()) return
        missYouRepository.observeEvents(coupleKey)
            .distinctUntilChanged()
            .collect { _events.value = it }
    }

    private suspend fun refreshFromServer() {
        missYouRepository.refreshFromServer()
    }

    private suspend fun loadTodayStats() {
        missYouRepository.getTodayStats().onSuccess {
            _todayStats.value = it
        }
    }

    fun sendMissYou(emoji: String = "❤️", message: String = "") {
        viewModelScope.launch {
            _isSending.value = true
            missYouRepository.sendMissYou(emoji, message)
                .onSuccess { loadTodayStats() }
                .onFailure { _errorMessage.value = it.message }
            _isSending.value = false
        }
    }

    fun isMyEvent(event: MissYouEvent): Boolean = event.senderId == myUserId

    fun clearError() { _errorMessage.value = null }
}
