package com.example.loveapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.entity.ChatMessage
import com.example.loveapp.data.api.models.GifItem
import com.example.loveapp.data.api.models.MissYouCounterResponse
import com.example.loveapp.data.api.models.StickerPackResponse
import com.example.loveapp.data.api.models.StickerResponse
import com.example.loveapp.data.repository.AuthRepository
import com.example.loveapp.data.repository.ChatRepository
import com.example.loveapp.data.repository.ChatSettingsRepository
import com.example.loveapp.data.repository.GifRepository
import com.example.loveapp.data.repository.RelationshipRepository
import com.example.loveapp.im.TIMChatManager
import com.example.loveapp.im.TIMLoginManager
import com.example.loveapp.im.TIMManager
import com.example.loveapp.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val relationshipRepository: RelationshipRepository,
    private val gifRepository: GifRepository,
    private val chatSettingsRepository: ChatSettingsRepository,
    private val tokenManager: TokenManager,
    private val timLoginManager: TIMLoginManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _partnerName = MutableStateFlow<String?>(null)
    val partnerName: StateFlow<String?> = _partnerName.asStateFlow()

    private val _partnerAvatar = MutableStateFlow<String?>(null)
    val partnerAvatar: StateFlow<String?> = _partnerAvatar.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    private val _isPartnerOnline = MutableStateFlow(false)
    val isPartnerOnline: StateFlow<Boolean> = _isPartnerOnline.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _wallpaperUrl = MutableStateFlow<String?>(null)
    val wallpaperUrl: StateFlow<String?> = _wallpaperUrl.asStateFlow()

    private val _bubbleColor = MutableStateFlow<String?>(null)
    val bubbleColor: StateFlow<String?> = _bubbleColor.asStateFlow()

    private val _bubbleShape = MutableStateFlow<String?>(null)
    val bubbleShape: StateFlow<String?> = _bubbleShape.asStateFlow()

    private val _gifResults = MutableStateFlow<List<GifItem>>(emptyList())
    val gifResults: StateFlow<List<GifItem>> = _gifResults.asStateFlow()

    // ── Sticker state ──────────────────────────────────────────
    private val _stickerPacks = MutableStateFlow<List<StickerPackResponse>>(emptyList())
    val stickerPacks: StateFlow<List<StickerPackResponse>> = _stickerPacks.asStateFlow()

    private val _currentStickers = MutableStateFlow<List<StickerResponse>>(emptyList())
    val currentStickers: StateFlow<List<StickerResponse>> = _currentStickers.asStateFlow()

    // ── Popup sticker overlay ──────────────────────────────────
    private val _popupSticker = MutableStateFlow<StickerResponse?>(null)
    val popupSticker: StateFlow<StickerResponse?> = _popupSticker.asStateFlow()

    // ── Miss You counter ───────────────────────────────────────
    private val _missYouCounter = MutableStateFlow(MissYouCounterResponse())
    val missYouCounter: StateFlow<MissYouCounterResponse> = _missYouCounter.asStateFlow()

    private var myUserId: Int = 0
    private var partnerId: String = ""
    private var coupleKey: String = ""
    private var messageCollectionJob: Job? = null
    private var typingResetJob: Job? = null
    private var onlineStatusJob: Job? = null

    init {
        viewModelScope.launch {
            myUserId = tokenManager.getUserId()?.toIntOrNull() ?: 0
            partnerId = tokenManager.getPartnerId() ?: ""
            loadPartnerInfo()
            refreshFromServer()
            observeTypingEvents()
            startOnlineStatusPolling()
            loadChatSettings()
            loadStickerPacks()
            loadMissYouCounter()
        }
    }

    private fun loadPartnerInfo() {
        viewModelScope.launch {
            relationshipRepository.getRelationship().onSuccess {
                _partnerName.value = it.partnerDisplayName
                _partnerAvatar.value = it.partnerAvatar
                val uid1 = it.userId1
                val uid2 = it.userId2
                if (uid1 > 0 && uid2 > 0) {
                    coupleKey = "${minOf(uid1, uid2)}_${maxOf(uid1, uid2)}"
                    partnerId = if (uid1 == myUserId) uid2.toString() else uid1.toString()
                    startObservingMessages()
                }
            }
        }
    }

    private fun startObservingMessages() {
        if (coupleKey.isBlank()) return
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            chatRepository.observeMessages(coupleKey)
                .distinctUntilChanged()
                .collect { _messages.value = it }
        }
    }

    private suspend fun refreshFromServer() {
        _isLoading.value = true
        chatRepository.refreshFromServer()
        _isLoading.value = false
    }

    private fun observeTypingEvents() {
        val flow = timLoginManager.typingEvents ?: return
        viewModelScope.launch {
            flow.collect {
                _isPartnerTyping.value = true
                typingResetJob?.cancel()
                typingResetJob = viewModelScope.launch {
                    delay(5_000)
                    _isPartnerTyping.value = false
                }
            }
        }
    }

    private fun startOnlineStatusPolling() {
        onlineStatusJob?.cancel()
        onlineStatusJob = viewModelScope.launch {
            while (true) {
                ensureActive()
                if (TIMManager.isLoggedIn() && partnerId.isNotBlank()) {
                    try {
                        TIMChatManager.getUserOnlineStatus(listOf(partnerId)) { statusMap ->
                            _isPartnerOnline.value = statusMap[partnerId] == true
                        }
                    } catch (_: Exception) {}
                }
                delay(30_000)
            }
        }
    }

    private fun loadChatSettings() {
        viewModelScope.launch {
            chatSettingsRepository.getSettings().onSuccess {
                _wallpaperUrl.value = it.wallpaperUrl
                _bubbleColor.value = it.bubbleColor
                _bubbleShape.value = it.bubbleShape
            }
        }
    }

    // ── Sending messages ───────────────────────────────────────

    fun sendMessage(
        content: String,
        messageType: String = "text",
        imageUrl: String? = null,
        audioUrl: String? = null,
        audioDurationSeconds: Int = 0,
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
    ) {
        viewModelScope.launch {
            chatRepository.sendMessage(
                content = content,
                messageType = messageType,
                imageUrl = imageUrl,
                audioUrl = audioUrl,
                audioDurationSeconds = audioDurationSeconds,
                partnerId = partnerId.ifBlank { null },
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
            ).onFailure { _errorMessage.value = it.message }
        }
    }

    fun sendImageMessage(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                chatRepository.uploadImage(uri)
                    .onSuccess { url -> sendMessage(content = "", messageType = "image", imageUrl = url) }
                    .onFailure { _errorMessage.value = "Ошибка загрузки: ${it.message}" }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки: ${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun sendGifMessage(gif: GifItem) {
        sendMessage(content = gif.title, messageType = "image", imageUrl = gif.url)
    }

    fun sendStickerMessage(sticker: StickerResponse) {
        sendMessage(
            content = sticker.name,
            messageType = "sticker",
            stickerId = sticker.code,
            stickerUrl = sticker.url,
            isPopupSticker = sticker.isPopup
        )
    }

    fun sendDrawingMessage(drawingBytes: ByteArray) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                chatRepository.uploadDrawing(drawingBytes)
                    .onSuccess { url -> sendMessage(content = "Рисунок", messageType = "drawing", drawingUrl = url) }
                    .onFailure { _errorMessage.value = "Ошибка загрузки рисунка: ${it.message}" }
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun sendVideoMessage(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                chatRepository.uploadFile(uri, "video_${System.currentTimeMillis()}.mp4", "video/mp4")
                    .onSuccess { url -> sendMessage(content = "", messageType = "video", videoUrl = url) }
                    .onFailure { _errorMessage.value = "Ошибка загрузки видео: ${it.message}" }
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun sendLocationMessage(lat: Double, lng: Double, name: String) {
        sendMessage(
            content = name,
            messageType = "location",
            latitude = lat,
            longitude = lng,
            locationName = name
        )
    }

    fun sendTypingIndicator() {
        if (TIMManager.isLoggedIn() && partnerId.isNotBlank()) {
            TIMChatManager.sendTypingStatus(partnerId)
        }
    }

    // ── Stickers ───────────────────────────────────────────────

    private fun loadStickerPacks() {
        viewModelScope.launch {
            chatRepository.getStickerPacks().onSuccess { _stickerPacks.value = it }
        }
    }

    fun loadStickersForPack(packId: Int) {
        viewModelScope.launch {
            chatRepository.getStickers(packId).onSuccess { _currentStickers.value = it }
        }
    }

    // ── Miss You Counter ───────────────────────────────────────

    private fun loadMissYouCounter() {
        viewModelScope.launch {
            chatRepository.getMissYouCounter().onSuccess { _missYouCounter.value = it }
        }
    }

    fun sendMissYou(emojiType: String = "❤️") {
        viewModelScope.launch {
            chatRepository.sendMissYou(emojiType).onSuccess { loadMissYouCounter() }
        }
    }

    // ── Popup sticker overlay ──────────────────────────────────

    fun showPopupSticker(sticker: StickerResponse) { _popupSticker.value = sticker }
    fun dismissPopupSticker() { _popupSticker.value = null }

    // ── GIF search ─────────────────────────────────────────────

    fun loadTrendingGifs() {
        viewModelScope.launch {
            gifRepository.trending(30).onSuccess { _gifResults.value = it.items }
        }
    }

    fun searchGifs(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                gifRepository.trending(30).onSuccess { _gifResults.value = it.items }
            } else {
                gifRepository.search(query, 30).onSuccess { _gifResults.value = it.items }
            }
        }
    }

    // ── Message actions ────────────────────────────────────────

    fun deleteMessage(localId: Int) {
        viewModelScope.launch { chatRepository.deleteMessage(localId) }
    }

    fun getMessageText(msg: ChatMessage): String = msg.content

    fun markAllRead() {
        viewModelScope.launch { chatRepository.markAllRead(myUserId, partnerId.ifBlank { null }) }
    }

    fun isMyMessage(msg: ChatMessage): Boolean = msg.senderId == myUserId

    fun clearError() { _errorMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        onlineStatusJob?.cancel()
        typingResetJob?.cancel()
    }
}
