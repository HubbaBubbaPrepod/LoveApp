package com.example.loveapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.WishRequest
import com.example.loveapp.data.api.models.WishResponse
import com.example.loveapp.data.dao.WishDao
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WishRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val wishDao: WishDao,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun createWish(
        title: String,
        description: String = "",
        priority: Int = 0,
        category: String = "",
        isPrivate: Boolean = false,
        imageUrls: String? = null,
        emoji: String? = null
    ): Result<WishResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val request = WishRequest(
            title = title,
            description = description,
            priority = priority,
            category = category,
            isPrivate = isPrivate,
            imageUrls = imageUrls,
            emoji = emoji
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

    suspend fun getWishById(id: Int): Result<WishResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getWish("Bearer $token", id)
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to get wish"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun compressImage(uri: Uri, maxDim: Int = 1920, quality: Int = 85): ByteArray {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val scale = maxOf(1, maxOf(opts.outWidth, opts.outHeight) / maxDim)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: throw Exception("Cannot decode image")
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    suspend fun uploadImage(uri: Uri): Result<String> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val bytes = withContext(Dispatchers.IO) { compressImage(uri) }
        val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "wish.jpg", requestBody)
        val response = apiService.uploadImage("Bearer $token", part)
        if (response.success && response.data != null) {
            Result.success(response.data.url)
        } else {
            Result.failure(Exception(response.message ?: "Upload failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCurrentUserId(): Int? = authRepository.getUserId()
}
