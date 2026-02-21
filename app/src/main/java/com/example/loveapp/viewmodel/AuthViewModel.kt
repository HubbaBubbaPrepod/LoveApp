package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.AuthResponse
import com.example.loveapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<AuthResponse?>(null)
    val currentUser: StateFlow<AuthResponse?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun signup(username: String, email: String, password: String, displayName: String, gender: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = authRepository.signup(username, email, password, displayName, gender)
            result.onSuccess { authResponse ->
                _currentUser.value = authResponse
                _successMessage.value = "Signup successful"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Signup failed"
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = authRepository.login(email, password)
            result.onSuccess { authResponse ->
                _currentUser.value = authResponse
                _successMessage.value = "Login successful"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Login failed"
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.logout()
            result.onSuccess {
                _currentUser.value = null
                _successMessage.value = "Logged out"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Logout failed"
                _isLoading.value = false
            }
        }
    }

    fun getProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.getProfile()
            result.onSuccess { authResponse ->
                _currentUser.value = authResponse
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to get profile"
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
