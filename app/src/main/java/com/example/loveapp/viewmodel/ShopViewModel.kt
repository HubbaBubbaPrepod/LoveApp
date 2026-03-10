package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.*
import com.example.loveapp.data.repository.ShopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val repo: ShopRepository
) : ViewModel() {

    private val _summary = MutableStateFlow<EconomySummary?>(null)
    val summary: StateFlow<EconomySummary?> = _summary

    private val _shopItems = MutableStateFlow<List<ShopItem>>(emptyList())
    val shopItems: StateFlow<List<ShopItem>> = _shopItems

    private val _dailyDeals = MutableStateFlow<List<DailyDeal>>(emptyList())
    val dailyDeals: StateFlow<List<DailyDeal>> = _dailyDeals

    private val _missions = MutableStateFlow<List<DailyMission>>(emptyList())
    val missions: StateFlow<List<DailyMission>> = _missions

    private val _transactions = MutableStateFlow<TransactionsResponse?>(null)
    val transactions: StateFlow<TransactionsResponse?> = _transactions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _purchaseResult = MutableStateFlow<ShopPurchaseResult?>(null)
    val purchaseResult: StateFlow<ShopPurchaseResult?> = _purchaseResult

    private val _claimResult = MutableStateFlow<MissionClaimResult?>(null)
    val claimResult: StateFlow<MissionClaimResult?> = _claimResult

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.getSummary().onSuccess { _summary.value = it }
            repo.getShopItems().onSuccess { _shopItems.value = it }
            repo.getDailyDeals().onSuccess { _dailyDeals.value = it }
            repo.getMissions().onSuccess { _missions.value = it }
            _isLoading.value = false
        }
    }

    fun loadSummary() {
        viewModelScope.launch {
            repo.getSummary().onSuccess { _summary.value = it }
        }
    }

    fun loadShopItems(category: String? = null) {
        viewModelScope.launch {
            repo.getShopItems(category).onSuccess { _shopItems.value = it }
        }
    }

    fun loadDailyDeals() {
        viewModelScope.launch {
            repo.getDailyDeals().onSuccess { _dailyDeals.value = it }
        }
    }

    fun loadMissions() {
        viewModelScope.launch {
            repo.getMissions().onSuccess { _missions.value = it }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            repo.getTransactions(limit = 50).onSuccess { _transactions.value = it }
        }
    }

    fun buyItem(id: Int) {
        viewModelScope.launch {
            repo.buyItem(id).onSuccess {
                _purchaseResult.value = it
                loadSummary()
                loadShopItems()
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun buyDeal(id: Int) {
        viewModelScope.launch {
            repo.buyDeal(id).onSuccess {
                loadSummary()
                loadDailyDeals()
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun claimMission(id: Int) {
        viewModelScope.launch {
            repo.claimMission(id).onSuccess {
                _claimResult.value = it
                loadSummary()
                loadMissions()
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun clearPurchaseResult() { _purchaseResult.value = null }
    fun clearClaimResult() { _claimResult.value = null }
    fun clearError() { _errorMessage.value = null }
}
