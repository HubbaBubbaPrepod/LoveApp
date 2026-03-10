package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.ChatSettingsRequest
import com.example.loveapp.data.api.models.ChatSettingsResponse
import com.example.loveapp.data.api.models.WallpaperItem
import javax.inject.Inject

class ChatSettingsRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) {
    suspend fun getSettings(): Result<ChatSettingsResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getChatSettings("Bearer $token")
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateSettings(wallpaperUrl: String? = null, bubbleColor: String? = null, bubbleShape: String? = null): Result<ChatSettingsResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.updateChatSettings("Bearer $token", ChatSettingsRequest(wallpaperUrl, bubbleColor, bubbleShape))
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getWallpapers(): Result<List<WallpaperItem>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getWallpapers("Bearer $token")
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }
}
