package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.*
import javax.inject.Inject

class BucketListRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) {
    suspend fun getItems(category: String? = null, completed: String? = null): Result<BucketListResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getBucketList("Bearer $token", category, completed)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun createItem(request: BucketItemRequest): Result<BucketItemResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.createBucketItem("Bearer $token", request)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateItem(id: Long, request: BucketItemRequest): Result<BucketItemResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.updateBucketItem("Bearer $token", id, request)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun completeItem(id: Long): Result<BucketItemResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.completeBucketItem("Bearer $token", id)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun uncompleteItem(id: Long): Result<BucketItemResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.uncompleteBucketItem("Bearer $token", id)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteItem(id: Long): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.deleteBucketItem("Bearer $token", id)
        if (resp.success) Result.success(Unit)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }
}
