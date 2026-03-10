package com.example.loveapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.ChatMessageRequest
import com.example.loveapp.data.api.models.MissYouCounterRequest
import com.example.loveapp.data.api.models.MissYouCounterResponse
import com.example.loveapp.data.api.models.StickerPackResponse
import com.example.loveapp.data.api.models.StickerResponse
import com.example.loveapp.data.dao.ChatMessageDao
import com.example.loveapp.data.dao.OutboxDao
import com.example.loveapp.data.entity.ChatMessage
import com.example.loveapp.data.entity.OutboxEntry
import com.example.loveapp.im.TIMChatManager
import com.example.loveapp.im.TIMManager
import com.example.loveapp.utils.DateUtils
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Chat data layer. Uses **TIM SDK as primary transport** when logged in,
 * falling back to REST API + outbox for offline resilience.
 * Incoming messages arrive via [com.example.loveapp.im.TIMMessageListener].
 */
class ChatRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val chatMessageDao: ChatMessageDao,
    private val outboxDao: OutboxDao,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {
    private val TAG = "ChatRepository"
    private val gson = Gson()

    fun observeMessages(coupleKey: String): Flow<List<ChatMessage>> =
        chatMessageDao.observeMessages(coupleKey)

    fun observeUnreadCount(userId: Int): Flow<Int> =
        chatMessageDao.observeUnreadCount(userId)

    /**
     * Send a message. If TIM is logged in, sends via TIM SDK (real-time C2C).
     * Otherwise falls back to REST API. Either way the message is persisted locally first.
     */
    suspend fun sendMessage(
        content: String,
        messageType: String = "text",
        imageUrl: String? = null,
        audioUrl: String? = null,
        audioDurationSeconds: Int = 0,
        partnerId: String? = null,
        stickerId: String? = null,
        stickerUrl: String? = null,
        isPopupSticker: Boolean = false,
        drawingUrl: String? = null,
        videoUrl: String? = null,
        videoDurationSeconds: Int = 0,
        videoThumbnailUrl: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        emojiType: String? = null
    ): Result<ChatMessage> {
        val myUserId = authRepository.getUserId() ?: 0
        val local = ChatMessage(
            senderId = myUserId,
            receiverId = 0,
            content = content,
            messageType = messageType,
            imageUrl = imageUrl,
            audioUrl = audioUrl,
            audioDurationSeconds = audioDurationSeconds,
            stickerId = stickerId,
            stickerUrl = stickerUrl,
            isPopupSticker = isPopupSticker,
            drawingUrl = drawingUrl,
            videoUrl = videoUrl,
            videoDurationSeconds = videoDurationSeconds,
            videoThumbnailUrl = videoThumbnailUrl,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            emojiType = emojiType,
            syncPending = true
        )
        val localId = chatMessageDao.upsert(local).toInt()
        val saved = local.copy(id = localId)

        val request = ChatMessageRequest(
            content = content,
            messageType = messageType,
            imageUrl = imageUrl,
            audioUrl = audioUrl,
            audioDurationSeconds = audioDurationSeconds,
            stickerId = stickerId,
            stickerUrl = stickerUrl,
            isPopupSticker = isPopupSticker,
            drawingUrl = drawingUrl,
            videoUrl = videoUrl,
            videoDurationSeconds = videoDurationSeconds,
            videoThumbnailUrl = videoThumbnailUrl,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            emojiType = emojiType
        )

        // ── TIM path (primary) ──
        if (TIMManager.isLoggedIn() && partnerId != null) {
            return try {
                val timResult = suspendCancellableCoroutine { cont ->
                    TIMChatManager.sendTextMessage(partnerId, content) { success, timMsg, error ->
                        if (success && timMsg != null) {
                            cont.resumeWith(kotlin.Result.success(
                                saved.copy(timMsgId = timMsg.msgID, syncPending = false)
                            ))
                        } else {
                            cont.resumeWith(kotlin.Result.success(null))
                        }
                    }
                }
                if (timResult != null) {
                    chatMessageDao.upsert(timResult)
                    sendViaRestAsync(request)
                    Result.success(timResult)
                } else {
                    sendViaRest(saved, localId, request)
                }
            } catch (e: Exception) {
                Log.w(TAG, "TIM send exception, falling back to REST", e)
                sendViaRest(saved, localId, request)
            }
        }

        // ── REST path (fallback) ──
        return sendViaRest(saved, localId, request)
    }

    private suspend fun sendViaRest(
        saved: ChatMessage,
        localId: Int,
        request: ChatMessageRequest
    ): Result<ChatMessage> {
        return try {
            val token = authRepository.getToken() ?: return enqueue("create", saved, localId)
            val resp = apiService.sendMessage("Bearer $token", request)
            if (resp.success && resp.data != null) {
                val d = resp.data
                val synced = saved.copy(
                    serverId = d.id,
                    senderId = d.senderId,
                    receiverId = d.receiverId,
                    coupleKey = d.coupleKey,
                    audioUrl = d.audioUrl,
                    audioDurationSeconds = d.audioDurationSeconds,
                    stickerId = d.stickerId,
                    stickerUrl = d.stickerUrl,
                    isPopupSticker = d.isPopupSticker,
                    drawingUrl = d.drawingUrl,
                    videoUrl = d.videoUrl,
                    videoDurationSeconds = d.videoDurationSeconds,
                    videoThumbnailUrl = d.videoThumbnailUrl,
                    latitude = d.latitude,
                    longitude = d.longitude,
                    locationName = d.locationName,
                    emojiType = d.emojiType,
                    syncPending = false
                )
                chatMessageDao.upsert(synced)
                Result.success(synced)
            } else enqueue("create", saved, localId)
        } catch (e: Exception) {
            enqueue("create", saved, localId)
        }
    }

    /** Fire-and-forget REST send (for server-side persistence when TIM was the primary). */
    private suspend fun sendViaRestAsync(request: ChatMessageRequest) {
        try {
            val token = authRepository.getToken() ?: return
            apiService.sendMessage("Bearer $token", request)
        } catch (_: Exception) {}
    }

    suspend fun markAllRead(myUserId: Int, partnerId: String? = null) {
        // TIM SDK read receipt — must pass partner's userId, not ours
        if (TIMManager.isLoggedIn() && !partnerId.isNullOrBlank()) {
            TIMChatManager.markC2CMessageAsRead(partnerId)
        }
        // REST API
        try {
            val token = authRepository.getToken() ?: return
            apiService.markAllMessagesRead("Bearer $token")
        } catch (_: Exception) {}
        if (myUserId > 0) chatMessageDao.markAllRead(myUserId)
    }

    suspend fun deleteMessage(localId: Int): Result<Unit> {
        val ex = chatMessageDao.getById(localId) ?: return Result.success(Unit)
        val sd = ex.copy(deletedAt = System.currentTimeMillis(), syncPending = true)
        chatMessageDao.upsert(sd)
        return try {
            val token = authRepository.getToken() ?: return Result.success(Unit)
            val sId = sd.serverId ?: return Result.success(Unit)
            apiService.deleteMessage("Bearer $token", sId)
            Result.success(Unit)
        } catch (_: Exception) { Result.success(Unit) }
    }

    suspend fun refreshFromServer() {
        val token = authRepository.getToken() ?: return
        try {
            val resp = apiService.getMessages("Bearer $token", 1, 200)
            if (resp.success && resp.data != null) {
                resp.data.items.forEach { m ->
                    val ex = chatMessageDao.getByServerId(m.id)
                    chatMessageDao.upsert(ChatMessage(
                        id = ex?.id ?: 0,
                        senderId = m.senderId,
                        receiverId = m.receiverId,
                        coupleKey = m.coupleKey,
                        messageType = m.messageType,
                        content = m.content,
                        imageUrl = m.imageUrl,
                        audioUrl = m.audioUrl,
                        audioDurationSeconds = m.audioDurationSeconds,
                        stickerId = m.stickerId,
                        stickerUrl = m.stickerUrl,
                        isPopupSticker = m.isPopupSticker,
                        drawingUrl = m.drawingUrl,
                        videoUrl = m.videoUrl,
                        videoDurationSeconds = m.videoDurationSeconds,
                        videoThumbnailUrl = m.videoThumbnailUrl,
                        latitude = m.latitude,
                        longitude = m.longitude,
                        locationName = m.locationName,
                        emojiType = m.emojiType,
                        isRead = m.isRead,
                        timestamp = DateUtils.parseIsoTs(m.createdAt),
                        serverId = m.id,
                        syncPending = false
                    ))
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun enqueue(action: String, msg: ChatMessage, localId: Int): Result<ChatMessage> {
        outboxDao.enqueue(OutboxEntry(entityType = "chat_message", action = action,
            payload = gson.toJson(msg), localId = localId, serverId = msg.serverId))
        return Result.success(msg)
    }

    /** Upload image and return server URL. */
    suspend fun uploadImage(uri: Uri): Result<String> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val bytes = withContext(Dispatchers.IO) { compressImage(uri) }
        val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "chat_${System.currentTimeMillis()}.jpg", requestBody)
        val response = apiService.uploadImage("Bearer $token", part)
        if (response.success && response.data != null) Result.success(response.data.url)
        else Result.failure(Exception(response.message ?: "Upload failed"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun compressImage(uri: Uri, maxDim: Int = 1920, quality: Int = 85): ByteArray {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val scale = maxOf(1, maxOf(opts.outWidth, opts.outHeight) / maxDim)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = scale }
        var bitmap: Bitmap? = null
        return try {
            bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: throw Exception("Cannot decode image")
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
        } finally {
            bitmap?.recycle()
        }
    }

    /** Upload a generic file (drawing / video) and return server URL. */
    suspend fun uploadFile(uri: Uri, filename: String, mimeType: String): Result<String> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("Cannot read file")
        }
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", filename, requestBody)
        val response = apiService.uploadFile("Bearer $token", part)
        if (response.success && response.data != null) Result.success(response.data.url)
        else Result.failure(Exception(response.message ?: "Upload failed"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Upload drawing bitmap and return server URL. */
    suspend fun uploadDrawing(bytes: ByteArray): Result<String> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val requestBody = bytes.toRequestBody("image/png".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "drawing_${System.currentTimeMillis()}.png", requestBody)
        val response = apiService.uploadFile("Bearer $token", part)
        if (response.success && response.data != null) Result.success(response.data.url)
        else Result.failure(Exception(response.message ?: "Upload failed"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Sticker Pack API ───────────────────────────────────────

    suspend fun getStickerPacks(): Result<List<StickerPackResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getStickerPacks("Bearer $token")
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getStickers(packId: Int): Result<List<StickerResponse>> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getStickers("Bearer $token", packId)
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    // ── Miss You Counter ───────────────────────────────────────

    suspend fun sendMissYou(emojiType: String = "❤️"): Result<Unit> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        apiService.sendChatMissYou("Bearer $token", MissYouCounterRequest(emojiType))
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getMissYouCounter(): Result<MissYouCounterResponse> = try {
        val token = authRepository.getToken() ?: return Result.failure(Exception("No token"))
        val resp = apiService.getChatMissYouCounter("Bearer $token")
        if (resp.success && resp.data != null) Result.success(resp.data)
        else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }
}
