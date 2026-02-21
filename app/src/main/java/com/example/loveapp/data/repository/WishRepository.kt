package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.WishRequest
import com.example.loveapp.data.api.models.WishResponse
import com.example.loveapp.data.dao.WishDao
import javax.inject.Inject

class WishRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val wishDao: WishDao,
    private val authRepository: AuthRepository
) {
    suspend fun createWish(
        title: String,
        description: String = "",
        priority: Int = 0,
        category: String = ""
    ): Result<WishResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val request = WishRequest(
            title = title,
            description = description,
            priority = priority,
            category = category
        )
        val response = apiService.createWish("Bearer $token", request)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to create wish"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getWishes(page: Int = 1): Result<List<WishResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getWishes("Bearer $token", page)
        
        if (response.success && response.data != null) {
            Result.success(response.data.items)
        } else {
            Result.failure(Exception(response.message ?: "Failed to get wishes"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateWish(id: Int, wish: WishRequest): Result<WishResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.updateWish("Bearer $token", id, wish)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to update wish"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun completeWish(id: Int): Result<WishResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.completeWish("Bearer $token", id)
        
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to complete wish"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteWish(id: Int): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.deleteWish("Bearer $token", id)
        
        if (response.success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.message ?: "Failed to delete wish"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
