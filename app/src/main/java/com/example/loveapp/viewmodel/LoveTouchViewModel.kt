package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.LoveTouchSessionResponse
import com.example.loveapp.data.repository.LoveTouchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoveTouchViewModel @Inject constructor(
    private val loveTouchRepository: LoveTouchRepository
) : ViewModel() {

    private val _currentSession = MutableStateFlow<LoveTouchSessionResponse?>(null)
    val currentSession: StateFlow<LoveTouchSessionResponse?> = _currentSession.asStateFlow()

    private val _history = MutableStateFlow<List<LoveTouchSessionResponse>>(emptyList())
    val history: StateFlow<List<LoveTouchSessionResponse>> = _history.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _partnerJoined = MutableStateFlow(false)
    val partnerJoined: StateFlow<Boolean> = _partnerJoined.asStateFlow()

    private val _heartsCount = MutableStateFlow(0)
    val heartsCount: StateFlow<Int> = _heartsCount.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadHistory()
    }

    fun startSession() {
        viewModelScope.launch {
            loveTouchRepository.startSession().onSuccess {
                _currentSession.value = it
                _isActive.value = true
                _partnerJoined.value = false
                _heartsCount.value = 0
            }.onFailure { _errorMessage.value = it.message }
        }
    }

    fun joinSession(sessionId: Int) {
        viewModelScope.launch {
            loveTouchRepository.joinSession(sessionId).onSuccess {
                _currentSession.value = it
                _isActive.value = true
                _partnerJoined.value = true
            }.onFailure { _errorMessage.value = it.message }
        }
    }

    fun onTouch() {
        if (_isActive.value && _partnerJoined.value) {
            _heartsCount.value += 1
        }
    }

    fun onPartnerJoined() {
        _partnerJoined.value = true
    }

    fun endSession() {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            loveTouchRepository.endSession(session.id, _heartsCount.value).onSuccess {
                _isActive.value = false
                _currentSession.value = null
                loadHistory()
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            loveTouchRepository.getHistory().onSuccess {
                _history.value = it
            }
        }
    }

    fun clearError() { _errorMessage.value = null }
}
