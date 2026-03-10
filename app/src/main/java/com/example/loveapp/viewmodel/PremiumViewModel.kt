package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.PremiumStatusResponse
import com.example.loveapp.data.api.models.PurchaseVerifyRequest
import com.example.loveapp.data.api.models.SubscriptionPlanResponse
import com.example.loveapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _plans = MutableStateFlow<List<SubscriptionPlanResponse>>(emptyList())
    val plans: StateFlow<List<SubscriptionPlanResponse>> = _plans.asStateFlow()

    private val _status = MutableStateFlow<PremiumStatusResponse?>(null)
    val status: StateFlow<PremiumStatusResponse?> = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Load plans (no auth needed)
                val plansResp = apiService.getPremiumPlans()
                if (plansResp.success && plansResp.data != null) {
                    _plans.value = plansResp.data.items
                }

                // Load status (needs auth)
                val token = authRepository.getToken()
                if (token != null) {
                    val statusResp = apiService.getPremiumStatus("Bearer $token")
                    if (statusResp.success && statusResp.data != null) {
                        _status.value = statusResp.data
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyPurchase(purchaseToken: String, productId: String, orderId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val token = authRepository.getToken() ?: run {
                    _errorMessage.value = "Требуется авторизация"
                    _isLoading.value = false
                    return@launch
                }
                val resp = apiService.verifyPurchase(
                    "Bearer $token",
                    PurchaseVerifyRequest(purchaseToken, productId, orderId)
                )
                if (resp.success && resp.data != null) {
                    _status.value = resp.data
                    _successMessage.value = "Премиум активирован!"
                } else {
                    _errorMessage.value = resp.message ?: "Ошибка верификации"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = authRepository.getToken() ?: run {
                    _errorMessage.value = "Требуется авторизация"
                    _isLoading.value = false
                    return@launch
                }
                val resp = apiService.cancelSubscription("Bearer $token")
                if (resp.success) {
                    _successMessage.value = "Подписка отменена"
                    loadData()
                } else {
                    _errorMessage.value = resp.message ?: "Ошибка отмены"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
