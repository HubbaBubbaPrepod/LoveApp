package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.repository.AppLockRepository
import com.example.loveapp.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockRepository: AppLockRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _isLockEnabled = MutableStateFlow(false)
    val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    private val _isBiometric = MutableStateFlow(false)
    val isBiometric: StateFlow<Boolean> = _isBiometric.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var userId: Int = 0

    init {
        viewModelScope.launch {
            userId = tokenManager.getUserId()?.toIntOrNull() ?: 0
            checkLockStatus()
        }
    }

    private suspend fun checkLockStatus() {
        val enabled = appLockRepository.isLockEnabled(userId)
        _isLockEnabled.value = enabled
        if (!enabled) _isUnlocked.value = true // No lock = always unlocked
    }

    fun setPin(pin: String, biometric: Boolean = false) {
        viewModelScope.launch {
            appLockRepository.setPin(userId, pin, biometric).onSuccess {
                _isLockEnabled.value = true
                _isBiometric.value = biometric
            }.onFailure { _errorMessage.value = it.message }
        }
    }

    fun verifyPin(pin: String) {
        viewModelScope.launch {
            val valid = appLockRepository.verifyPin(userId, pin)
            if (valid) {
                _isUnlocked.value = true
                _errorMessage.value = null
            } else {
                _errorMessage.value = "Неверный PIN"
            }
        }
    }

    fun updatePin(currentPin: String, newPin: String, biometric: Boolean? = null) {
        viewModelScope.launch {
            appLockRepository.updatePin(userId, currentPin, newPin, biometric)
                .onSuccess { _errorMessage.value = null }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun removePin(pin: String) {
        viewModelScope.launch {
            appLockRepository.removePin(userId, pin).onSuccess {
                _isLockEnabled.value = false
                _isUnlocked.value = true
            }.onFailure { _errorMessage.value = it.message }
        }
    }

    fun setBiometricUnlocked() {
        _isUnlocked.value = true
    }

    fun clearError() { _errorMessage.value = null }
}
