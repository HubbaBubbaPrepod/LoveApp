package com.example.loveapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.WishRequest
import com.example.loveapp.data.api.models.WishResponse
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.dao.WishDao
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.data.entity.Wish
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class WishRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val wishDao: WishDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    // ─── Live Flows ───────────────────────────────────────────────────────
    fun observeAllWishes(): Flow<List<Wish>> = wishDao.observeAllWishes()
    fun observeActiveWishesByUser(userId: Int): Flow<List<Wish>> =
        wishDao.observeActiveWishesByUser(userId)


// ─── Write: Room-first ────────────────────────────────────────────────
    suspend fun createWish(
        title: String,
        description: String = "",
        priority: Int = 0,
        category: String = "",
        isPrivate: Boolean = false,
        imageUrls: String? = null,
        emoji: String? = null
    ): Result<Wish> {
        val local = Wish(title = title, description = description, priority = priority,
            category = category, userId = 0, syncPending = true)
        val localId = wishDao.insertWish(local).toInt()
        val saved = local.copy(id = localId)
        return try {
            val token = authRepository.getToken() ?: return enqueue("create", saved, localId)
            val resp = apiService.createWish("Bearer $token",
                WishRequest(title = title, description = description, priority = priority,
                    category = category, isPrivate = isPrivate, imageUrls = imageUrls, emoji = emoji))
            if (resp.success && resp.data != null) {
                val synced = saved.copy(serverId = resp.data.id, syncPending = false)
                wishDao.upsert(synced); Result.success(synced)
            } else enqueue("create", saved, localId)
        } catch (e: Exception) { enqueue("create", saved, localId) }
    }

    suspend fun getWishes(page: Int = 1): Result<List<WishResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getWishes("Bearer $token", page)
        if (response.success && response.data != null) Result.success(response.data.items)
        else Result.failure(Exception(response.message ?: "Failed to get wishes"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateWish(localId: Int, wish: WishRequest): Result<Wish> {
        val ex = wishDao.getWishById(localId) ?: return Result.failure(Exception("Not found"))
        val updated = ex.copy(title = wish.title ?: ex.title, description = wish.description ?: ex.description,
            priority = wish.priority ?: ex.priority, category = wish.category ?: ex.category,
            syncPending = true)
        wishDao.upsert(updated)
        return try {
            val token = authRepository.getToken() ?: return enqueue("update", updated, localId)
            val sId = updated.serverId ?: return enqueue("update", updated, localId)
            val resp = apiService.updateWish("Bearer $token", sId, wish)
            if (resp.success) { wishDao.upsert(updated.copy(syncPending = false)); Result.success(updated.copy(syncPending = false)) }
            else enqueue("update", updated, localId)
        } catch (e: Exception) { enqueue("update", updated, localId) }
    }

    suspend fun completeWish(localId: Int): Result<Wish> {
        val ex = wishDao.getWishById(localId) ?: return Result.failure(Exception("Not found"))
        val updated = ex.copy(isCompleted = true, syncPending = true)
        wishDao.upsert(updated)
        return try {
            val token = authRepository.getToken() ?: return enqueue("complete", updated, localId)
            val sId = updated.serverId ?: return enqueue("complete", updated, localId)
            val resp = apiService.completeWish("Bearer $token", sId)
            if (resp.success) { wishDao.upsert(updated.copy(syncPending = false)); Result.success(updated.copy(syncPending = false)) }
            else enqueue("complete", updated, localId)
        } catch (e: Exception) { enqueue("complete", updated, localId) }
    }

    suspend fun deleteWish(localId: Int): Result<Unit> {
        val ex = wishDao.getWishById(localId) ?: return Result.success(Unit)
        val sd = ex.copy(deletedAt = System.currentTimeMillis(), syncPending = true)
        wishDao.upsert(sd)
        return try {
            val token = authRepository.getToken() ?: return enqueueDelete(sd, localId)
            val sId = sd.serverId ?: return enqueueDelete(sd, localId)
            apiService.deleteWish("Bearer $token", sId)
            wishDao.upsert(sd.copy(syncPending = false)); Result.success(Unit)
        } catch (e: Exception) { enqueueDelete(sd, localId) }
    }

    suspend fun getWishById(id: Int): Result<WishResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val response = apiService.getWish("Bearer $token", id)
        if (response.success && response.data != null) Result.success(response.data)
        else Result.failure(Exception(response.message ?: "Failed to get wish"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        val resp = apiService.getWishes("Bearer $token", 1)
        if (resp.success && resp.data != null) {
            resp.data.items.forEach { w ->
                val ex = w.id?.let { wishDao.getByServerId(it) }
                wishDao.upsert(Wish(id = ex?.id ?: 0, title = w.title ?: "",
                    description = w.description ?: "", priority = w.priority ?: 0,
                    category = w.category ?: "", isCompleted = w.isCompleted ?: false,
                    userId = w.userId ?: 0,
                    createdAt = DateUtils.parseIsoTs(w.createdAt),
                    serverId = w.id, syncPending = false))
            }
        }
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
        if (response.success && response.data != null) Result.success(response.data.url)
        else Result.failure(Exception(response.message ?: "Upload failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getCurrentUserId(): Int? = authRepository.getUserId()

    private suspend fun enqueue(action: String, wish: Wish, localId: Int): Result<Wish> {
        outboxDao.enqueue(OutboxEntry(entityType = "wish", action = action,
            payload = gson.toJson(wish), localId = localId, serverId = wish.serverId))
        return Result.success(wish)
    }
    private suspend fun enqueueDelete(wish: Wish, localId: Int): Result<Unit> {
        outboxDao.enqueue(OutboxEntry(entityType = "wish", action = "delete",
            payload = gson.toJson(mapOf("id" to wish.serverId)), localId = localId, serverId = wish.serverId))
        return Result.success(Unit)
    }
}
