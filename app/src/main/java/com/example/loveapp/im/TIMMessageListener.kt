package com.example.loveapp.im

import android.util.Log
import com.example.loveapp.data.dao.ChatMessageDao
import com.example.loveapp.data.entity.ChatMessage
import com.tencent.imsdk.v2.V2TIMAdvancedMsgListener
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMMessageReceipt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Bridges Tencent IM SDK message callbacks into Room DB + SharedFlow events.
 * Mirrors Love8's listener pattern: receive messages → persist → notify UI.
 *
 * Must be registered after TIM SDK init and unregistered on logout/destroy.
 */
class TIMMessageListener(
    private val chatMessageDao: ChatMessageDao,
    private val myUserId: Int,
    private val coupleKey: String
) {

    private val TAG = "TIMMessageListener"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Emits incoming custom message payloads for LoveTouch, Drawing, etc. */
    private val _customEvents = MutableSharedFlow<CustomMessagePayload>(extraBufferCapacity = 64)
    val customEvents: SharedFlow<CustomMessagePayload> = _customEvents.asSharedFlow()

    /** Emits typing indicators (partner userId). */
    private val _typingEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val typingEvents: SharedFlow<String> = _typingEvents.asSharedFlow()

    /** Emits read receipt events (partner read our messages). */
    private val _readReceiptEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val readReceiptEvents: SharedFlow<Unit> = _readReceiptEvents.asSharedFlow()

    /** Emits revoke events (msgID revoked). */
    private val _revokeEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val revokeEvents: SharedFlow<String> = _revokeEvents.asSharedFlow()

    private val advancedListener = object : V2TIMAdvancedMsgListener() {

        override fun onRecvNewMessage(msg: V2TIMMessage) {
            Log.d(TAG, "onRecvNewMessage: msgID=${msg.msgID}, type=${msg.elemType}")
            scope.launch { handleIncomingMessage(msg) }
        }

        override fun onRecvC2CReadReceipt(receiptList: MutableList<V2TIMMessageReceipt>?) {
            Log.d(TAG, "C2C read receipts: ${receiptList?.size}")
            scope.launch { _readReceiptEvents.emit(Unit) }
        }

        override fun onRecvMessageRevoked(msgID: String?) {
            if (msgID == null) return
            Log.d(TAG, "Message revoked: $msgID")
            scope.launch {
                val existing = chatMessageDao.getByTimMsgId(msgID)
                if (existing != null) {
                    chatMessageDao.upsert(existing.copy(isRevoked = true, content = ""))
                }
                _revokeEvents.emit(msgID)
            }
        }
    }

    /** Start listening. Call after TIM login. */
    fun register() {
        V2TIMManager.getMessageManager().addAdvancedMsgListener(advancedListener)
        Log.d(TAG, "Registered TIM advanced message listener")
    }

    /** Stop listening. Call before TIM logout or Activity destroy. */
    fun unregister() {
        V2TIMManager.getMessageManager().removeAdvancedMsgListener(advancedListener)
        scope.cancel()
        Log.d(TAG, "Unregistered TIM advanced message listener")
    }

    private suspend fun handleIncomingMessage(msg: V2TIMMessage) {
        val senderId = msg.sender?.toIntOrNull() ?: return
        val timMsgId = msg.msgID ?: return

        // Skip messages we sent ourselves (echoed back)
        if (senderId == myUserId) return

        // Check for duplicate
        val existing = chatMessageDao.getByTimMsgId(timMsgId)
        if (existing != null) return

        when (msg.elemType) {
            V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> {
                val text = msg.textElem?.text ?: ""
                persistMessage(
                    senderId = senderId,
                    timMsgId = timMsgId,
                    messageType = "text",
                    content = text,
                    timestamp = msg.timestamp * 1000L
                )
            }

            V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE -> {
                // V2TIMImage doesn't expose URL as sync property in SDK 7.x;
                // persist placeholder, download lazily on display via getUrl(path, cb).
                val imageElem = msg.imageElem
                val path = imageElem?.path ?: ""
                persistMessage(
                    senderId = senderId,
                    timMsgId = timMsgId,
                    messageType = "image",
                    content = "",
                    imageUrl = path,
                    timestamp = msg.timestamp * 1000L
                )
            }

            V2TIMMessage.V2TIM_ELEM_TYPE_SOUND -> {
                val soundElem = msg.soundElem
                persistMessage(
                    senderId = senderId,
                    timMsgId = timMsgId,
                    messageType = "voice",
                    content = "",
                    audioUrl = soundElem?.path ?: "",
                    audioDurationSeconds = soundElem?.duration ?: 0,
                    timestamp = msg.timestamp * 1000L
                )
            }

            V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO -> {
                val videoElem = msg.videoElem
                persistMessage(
                    senderId = senderId,
                    timMsgId = timMsgId,
                    messageType = "video",
                    content = "",
                    imageUrl = videoElem?.snapshotPath ?: "",
                    timestamp = msg.timestamp * 1000L
                )
            }

            V2TIMMessage.V2TIM_ELEM_TYPE_LOCATION -> {
                val locElem = msg.locationElem
                val desc = locElem?.desc ?: ""
                val lat = locElem?.latitude ?: 0.0
                val lng = locElem?.longitude ?: 0.0
                persistMessage(
                    senderId = senderId,
                    timMsgId = timMsgId,
                    messageType = "location",
                    content = "$desc|$lat|$lng",
                    timestamp = msg.timestamp * 1000L
                )
            }

            V2TIMMessage.V2TIM_ELEM_TYPE_CUSTOM -> {
                val customElem = msg.customElem
                val payload = TIMChatManager.parseCustomPayload(customElem?.data)
                if (payload != null) {
                    when (payload.type) {
                        "typing" -> {
                            // Typing indicator — don't persist, just emit
                            _typingEvents.emit(senderId.toString())
                        }
                        CustomMessagePayload.TYPE_LOVE_TOUCH_MOVE,
                        CustomMessagePayload.TYPE_DRAW_ACTION -> {
                            // High-frequency real-time events — emit, don't persist
                            _customEvents.emit(payload)
                        }
                        else -> {
                            // Persistable custom messages (spark, intimacy, gift, etc.)
                            persistMessage(
                                senderId = senderId,
                                timMsgId = timMsgId,
                                messageType = "custom",
                                content = payload.data ?: "",
                                timCustomType = payload.type,
                                timCustomData = payload.data,
                                timestamp = msg.timestamp * 1000L
                            )
                            _customEvents.emit(payload)
                        }
                    }
                }
            }

            else -> {
                Log.d(TAG, "Unhandled message type: ${msg.elemType}")
            }
        }
    }

    private suspend fun persistMessage(
        senderId: Int,
        timMsgId: String,
        messageType: String,
        content: String,
        imageUrl: String? = null,
        audioUrl: String? = null,
        audioDurationSeconds: Int = 0,
        timCustomType: String? = null,
        timCustomData: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val chatMsg = ChatMessage(
            senderId = senderId,
            receiverId = myUserId,
            coupleKey = coupleKey,
            messageType = messageType,
            content = content,
            imageUrl = imageUrl,
            audioUrl = audioUrl,
            audioDurationSeconds = audioDurationSeconds,
            isRead = false,
            timestamp = timestamp,
            timMsgId = timMsgId,
            timCustomType = timCustomType,
            timCustomData = timCustomData,
            syncPending = false
        )
        try {
            chatMessageDao.upsert(chatMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist message timMsgId=$timMsgId", e)
        }
    }
}
