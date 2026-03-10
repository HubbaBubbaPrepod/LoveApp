package com.example.loveapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.BothPhoneStatusResponse
import com.example.loveapp.data.api.models.PhoneStatusHistoryItem
import com.example.loveapp.data.api.models.PhoneStatusResponse
import com.example.loveapp.data.repository.PhoneStatusRepository
import com.example.loveapp.location.PhoneStatusService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneStatusViewModel @Inject constructor(
    private val app: Application,
    private val phoneStatusRepository: PhoneStatusRepository
) : AndroidViewModel(app) {

    private val _partnerStatus = MutableStateFlow<PhoneStatusResponse?>(null)
    val partnerStatus: StateFlow<PhoneStatusResponse?> = _partnerStatus

    private val _bothStatus = MutableStateFlow<BothPhoneStatusResponse?>(null)
    val bothStatus: StateFlow<BothPhoneStatusResponse?> = _bothStatus

    private val _history = MutableStateFlow<List<PhoneStatusHistoryItem>>(emptyList())
    val history: StateFlow<List<PhoneStatusHistoryItem>> = _history

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isMonitoringActive = MutableStateFlow(false)
    val isMonitoringActive: StateFlow<Boolean> = _isMonitoringActive

    init {
        loadBothStatus()
        startAutoRefresh()
    }

    fun loadPartnerStatus() {
        viewModelScope.launch {
            try {
                _partnerStatus.value = phoneStatusRepository.getPartnerStatus()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun loadBothStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _bothStatus.value = phoneStatusRepository.getBothStatus()
                _partnerStatus.value = _bothStatus.value?.partner
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadHistory(hours: Int = 24) {
        viewModelScope.launch {
            try {
                _history.value = phoneStatusRepository.getHistory(hours)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun startMonitoring() {
        PhoneStatusService.start(app)
        _isMonitoringActive.value = true
    }

    fun stopMonitoring() {
        PhoneStatusService.stop(app)
        _isMonitoringActive.value = false
    }

    fun toggleMonitoring() {
        if (_isMonitoringActive.value) stopMonitoring() else startMonitoring()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                try {
                    _bothStatus.value = phoneStatusRepository.getBothStatus()
                    _partnerStatus.value = _bothStatus.value?.partner
                } catch (_: Exception) {}
            }
        }
    }

    companion object {
        fun formatLastActive(lastActiveAt: String?): String {
            if (lastActiveAt == null) return "Неизвестно"
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val date = sdf.parse(lastActiveAt.substringBefore('.').substringBefore('Z'))
                    ?: return lastActiveAt
                val diffMs = System.currentTimeMillis() - date.time
                val minutes = diffMs / 60_000
                when {
                    minutes < 1 -> "Сейчас в сети"
                    minutes < 60 -> "$minutes мин. назад"
                    minutes < 1440 -> "${minutes / 60} ч. назад"
                    else -> "${minutes / 1440} дн. назад"
                }
            } catch (_: Exception) {
                lastActiveAt
            }
        }

        fun batteryEmoji(level: Int?, isCharging: Boolean): String {
            if (level == null) return "🔋"
            return when {
                isCharging -> "⚡"
                level > 80 -> "🔋"
                level > 50 -> "🔋"
                level > 20 -> "🪫"
                else -> "🪫"
            }
        }

        fun screenStatusLabel(status: String): String {
            return when (status) {
                "on" -> "Экран включён"
                "off" -> "Экран выключен"
                "locked" -> "Заблокирован"
                "unlocked" -> "Разблокирован"
                else -> status
            }
        }

        fun networkTypeLabel(type: String?): String {
            return when (type) {
                "wifi" -> "Wi-Fi"
                "cellular" -> "Мобильная сеть"
                "ethernet" -> "Ethernet"
                "vpn" -> "VPN"
                "none" -> "Нет сети"
                else -> "Неизвестно"
            }
        }
    }
}
