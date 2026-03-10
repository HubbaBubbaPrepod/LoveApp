package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.GameRoundResponse
import com.example.loveapp.data.api.models.GameSessionResponse
import com.example.loveapp.data.repository.AuthRepository
import com.example.loveapp.data.repository.GamesRepository
import com.example.loveapp.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GamesViewModel @Inject constructor(
    private val gamesRepository: GamesRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<GameSessionResponse>>(emptyList())
    val sessions: StateFlow<List<GameSessionResponse>> = _sessions.asStateFlow()

    private val _activeSession = MutableStateFlow<GameSessionResponse?>(null)
    val activeSession: StateFlow<GameSessionResponse?> = _activeSession.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _gameStarted = MutableStateFlow(false)
    val gameStarted: StateFlow<Boolean> = _gameStarted.asStateFlow()

    private val _myUserId = MutableStateFlow(-1)
    val myUserId: StateFlow<Int> = _myUserId.asStateFlow()

    init {
        viewModelScope.launch {
            val id = tokenManager.getUserId()?.toIntOrNull()
            if (id != null) {
                _myUserId.value = id
            } else {
                _errorMessage.value = "Не удалось определить пользователя"
            }
        }
        loadGames()
    }

    private fun loadGames() {
        viewModelScope.launch {
            _isLoading.value = true
            gamesRepository.getGames()
                .onSuccess { _sessions.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun startGame(gameType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            gamesRepository.startGame(gameType)
                .onSuccess { session ->
                    _gameStarted.value = true
                    loadSession(session.id)
                    loadGames()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun loadSession(id: Int) {
        viewModelScope.launch {
            gamesRepository.getSession(id)
                .onSuccess { _activeSession.value = it }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun submitAnswer(roundNumber: Int, answer: String) {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            gamesRepository.submitAnswer(session.id, roundNumber, answer)
                .onSuccess {
                    // Reload session to get updated state
                    loadSession(session.id)
                }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun clearActiveSession() {
        _activeSession.value = null
        _gameStarted.value = false
    }

    fun refresh() { loadGames() }

    fun clearMessages() {
        _errorMessage.value = null
        _gameStarted.value = false
    }
}
