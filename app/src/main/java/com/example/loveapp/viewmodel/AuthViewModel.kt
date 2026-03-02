package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
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

    /** One-shot: true when login/signup succeeded (UI should navigate and then call clearAuthSuccessEvent()) */
    private val _authSuccessEvent = MutableStateFlow(false)
    val authSuccessEvent: StateFlow<Boolean> = _authSuccessEvent.asStateFlow()

    /** One-shot: true when Google login succeeded but profile is not yet complete */
    private val _needsProfileSetupEvent = MutableStateFlow(false)
    val needsProfileSetupEvent: StateFlow<Boolean> = _needsProfileSetupEvent.asStateFlow()

    /** null = ещё проверяем токен, true = залогинен, false = не залогинен. При старте проверяем сохранённый токен. */
    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoggedIn.value = authRepository.getToken() != null
        }
    }

    fun clearAuthSuccessEvent() {
        _authSuccessEvent.value = false
    }

    fun clearNeedsProfileSetupEvent() {
        _needsProfileSetupEvent.value = false
    }

    fun signup(username: String, email: String, password: String, displayName: String, gender: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _authSuccessEvent.value = false

            val result = authRepository.signup(username, email, password, displayName, gender)
            result.onSuccess { authResponse ->
                _currentUser.value = authResponse
                _isLoggedIn.value = true
                _successMessage.value = "Signup successful"
                _authSuccessEvent.value = true
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
            _authSuccessEvent.value = false

            val result = authRepository.login(email, password)
            result.onSuccess { authResponse ->
                _currentUser.value = authResponse
                _isLoggedIn.value = true
                _successMessage.value = "Login successful"
                _authSuccessEvent.value = true
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Login failed"
                _isLoading.value = false
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _authSuccessEvent.value = false
            _needsProfileSetupEvent.value = false
            val result = authRepository.loginWithGoogle(idToken)
            result.onSuccess { authResponse ->
                _currentUser.value = authResponse
                _isLoggedIn.value = true
                if (authResponse.needsProfileSetup) {
                    _needsProfileSetupEvent.value = true
                } else {
                    _authSuccessEvent.value = true
                }
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Google sign-in failed"
                _isLoading.value = false
            }
        }
    }

    fun setupProfile(displayName: String, gender: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.setupProfile(displayName, gender)
            result.onSuccess { authResponse ->
                _currentUser.value = authResponse
                _needsProfileSetupEvent.value = false
                _authSuccessEvent.value = true
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Profile setup failed"
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
                _isLoggedIn.value = false
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

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    private val _isAvatarUploading = MutableStateFlow(false)
    val isAvatarUploading: StateFlow<Boolean> = _isAvatarUploading.asStateFlow()

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _isAvatarUploading.value = true
            val result = authRepository.uploadAvatar(uri)
            result.onSuccess { url ->
                // Update current user in-memory so the UI reflects the new avatar immediately
                _currentUser.value = _currentUser.value?.copy(profileImage = url)
                _successMessage.value = "Аватар обновлён"
            }.onFailure {
                _errorMessage.value = it.message ?: "Ошибка загрузки"
            }
            _isAvatarUploading.value = false
        }
    }
}
