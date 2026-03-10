package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.BothPhoneStatusResponse
import com.example.loveapp.data.api.models.PhoneStatusHistoryItem
import com.example.loveapp.data.api.models.PhoneStatusResponse
import com.example.loveapp.data.api.models.PhoneStatusUpdateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneStatusRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) {
    private suspend fun token() = "Bearer ${authRepository.getToken() ?: ""}"

    suspend fun updateStatus(request: PhoneStatusUpdateRequest) =
        apiService.updatePhoneStatus(token(), request)

    suspend fun getPartnerStatus(): PhoneStatusResponse? =
        apiService.getPartnerPhoneStatus(token()).data

    suspend fun getBothStatus(): BothPhoneStatusResponse? =
        apiService.getBothPhoneStatus(token()).data

    suspend fun getHistory(hours: Int? = null): List<PhoneStatusHistoryItem> =
        apiService.getPhoneStatusHistory(token(), hours).data ?: emptyList()
}
