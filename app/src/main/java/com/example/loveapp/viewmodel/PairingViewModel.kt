package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairingUiState(
    val isLoading: Boolean = false,
    val myCode: String? = null,          // generated code to share
    val partnerName: String? = null,     // set after successful link
    val partnerUsername: String? = null,
    val error: String? = null
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    fun generateCode() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, myCode = null)
            val result = authRepository.generatePairingCode()
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false, myCode = result.getOrNull())
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Ошибка генерации кода"
                )
            }
        }
    }

    fun linkPartner(code: String) {
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Введите код")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.linkPartner(code.trim().uppercase())
            _uiState.value = if (result.isSuccess) {
                val data = result.getOrNull()
                _uiState.value.copy(
                    isLoading = false,
                    partnerName = data?.partnerName,
                    partnerUsername = data?.partnerUsername
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Неверный или истёкший код"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
