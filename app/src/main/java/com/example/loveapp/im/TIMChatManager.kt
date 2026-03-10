package com.example.loveapp.im

import android.util.Log
import com.google.gson.Gson
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMMessageManager
import com.tencent.imsdk.v2.V2TIMMessageListGetOption
import com.tencent.imsdk.v2.V2TIMSendCallback
import com.tencent.imsdk.v2.V2TIMValueCallback

/**
 * High-level chat service that wraps V2TIM message and conversation APIs.
 * Mirrors Love8's approach: C2C messaging between couple members.
 */
object TIMChatManager {

    private const val TAG = "TIMChatManager"
    private val gson = Gson()

    private val msgManager: V2TIMMessageManager
        get() = V2TIMManager.getMessageManager()

    // ───────── Send Messages ─────────

    /** Send a plain text message to a user (C2C). */
    fun sendTextMessage(
        toUserId: String,
        text: String,
        onProgress: ((Int) -> Unit)? = null,
        onResult: (Boolean, V2TIMMessage?, String?) -> Unit
    ) {
        val msg = V2TIMManager.getMessageManager().createTextMessage(text)
        sendMessage(msg, toUserId, onProgress, onResult)
    }

    /** Send an image message (local path). */
    fun sendImageMessage(
        toUserId: String,
        imagePath: String,
        onProgress: ((Int) -> Unit)? = null,
        onResult: (Boolean, V2TIMMessage?, String?) -> Unit
    ) {
        val msg = msgManager.createImageMessage(imagePath)
        sendMessage(msg, toUserId, onProgress, onResult)
    }

    /** Send a voice/sound message. */
    fun sendSoundMessage(
        toUserId: String,
        soundPath: String,
        duration: Int,
        onProgress: ((Int) -> Unit)? = null,
        onResult: (Boolean, V2TIMMessage?, String?) -> Unit
    ) {
        val msg = msgManager.createSoundMessage(soundPath, duration)
        sendMessage(msg, toUserId, onProgress, onResult)
    }

    /** Send a video message. */
    fun sendVideoMessage(
        toUserId: String,
        videoPath: String,
        type: String,
        duration: Int,
        snapshotPath: String,
        onProgress: ((Int) -> Unit)? = null,
        onResult: (Boolean, V2TIMMessage?, String?) -> Unit
    ) {
        val msg = msgManager.createVideoMessage(videoPath, type, duration, snapshotPath)
        sendMessage(msg, toUserId, onProgress, onResult)
    }

    /** Send a location message. */
    fun sendLocationMessage(
        toUserId: String,
        desc: String,
        longitude: Double,
        latitude: Double,
        onProgress: ((Int) -> Unit)? = null,
        onResult: (Boolean, V2TIMMessage?, String?) -> Unit
    ) {
        val msg = msgManager.createLocationMessage(desc, longitude, latitude)
        sendMessage(msg, toUserId, onProgress, onResult)
    }

    /**
     * Send a custom message (used for Spark, Intimacy, LoveTouch, Draw, etc.).
     * Payload is JSON-serialised [CustomMessagePayload].
     */
    fun sendCustomMessage(
        toUserId: String,
        payload: CustomMessagePayload,
        onProgress: ((Int) -> Unit)? = null,
        onResult: (Boolean, V2TIMMessage?, String?) -> Unit
    ) {
        val json = gson.toJson(payload)
        val msg = msgManager.createCustomMessage(json.toByteArray(Charsets.UTF_8))
        sendMessage(msg, toUserId, onProgress, onResult)
    }

    /** Internal helper — sends any V2TIMMessage as C2C. */
    private fun sendMessage(
        msg: V2TIMMessage,
        toUserId: String,
        onProgress: ((Int) -> Unit)?,
        onResult: (Boolean, V2TIMMessage?, String?) -> Unit
    ) {
        msgManager.sendMessage(
            msg,
            toUserId,        // receiver
            null,            // groupID (null for C2C)
            V2TIMMessage.V2TIM_PRIORITY_DEFAULT,
            false,           // onlineUserOnly
            null,            // offlinePushInfo (configure later)
            object : V2TIMSendCallback<V2TIMMessage> {
                override fun onProgress(progress: Int) {
                    onProgress?.invoke(progress)
                }
                override fun onSuccess(result: V2TIMMessage) {
                    Log.d(TAG, "Message sent to $toUserId, msgID=${result.msgID}")
                    onResult(true, result, null)
                }
                override fun onError(code: Int, desc: String?) {
                    Log.e(TAG, "Send failed: $code – $desc")
                    onResult(false, null, "$code: $desc")
                }
            }
        )
    }

    // ───────── Read Receipts ─────────

    /** Send C2C read receipt (tells partner you read their messages). */
    fun markC2CMessageAsRead(userId: String, callback: ((Boolean) -> Unit)? = null) {
        @Suppress("DEPRECATION")
        V2TIMManager.getMessageManager().markC2CMessageAsRead(
            userId,
            object : V2TIMCallback {
                override fun onSuccess() {
                    Log.d(TAG, "Marked c2c $userId as read")
                    callback?.invoke(true)
                }
                override fun onError(code: Int, desc: String?) {
                    Log.e(TAG, "markAsRead error: $code – $desc")
                    callback?.invoke(false)
                }
            }
        )
    }

    // ───────── History ─────────

    /** Pull historical messages for a C2C conversation. */
    fun getC2CHistoryMessages(
        userId: String,
        count: Int = 20,
        lastMsg: V2TIMMessage? = null,
        onResult: (Boolean, List<V2TIMMessage>?) -> Unit
    ) {
        val option = V2TIMMessageListGetOption().apply {
            userID = userId
            this.count = count
            this.lastMsg = lastMsg
            getType = V2TIMMessageListGetOption.V2TIM_GET_CLOUD_OLDER_MSG
        }
        msgManager.getHistoryMessageList(option, object : V2TIMValueCallback<List<V2TIMMessage>> {
            override fun onSuccess(msgs: List<V2TIMMessage>) {
                onResult(true, msgs)
            }
            override fun onError(code: Int, desc: String?) {
                Log.e(TAG, "getHistory error: $code – $desc")
                onResult(false, null)
            }
        })
    }

    // ───────── Typing Indicator ─────────

    /** Send a typing indicator to partner. Love8 uses this for real-time UX. */
    fun sendTypingStatus(toUserId: String) {
        try {
            // TIM SDK provides C2C typing detection via custom online-only message
            val payload = CustomMessagePayload(type = "typing")
            val json = gson.toJson(payload)
            val msg = msgManager.createCustomMessage(json.toByteArray(Charsets.UTF_8))
            msgManager.sendMessage(
                msg, toUserId, null,
                V2TIMMessage.V2TIM_PRIORITY_LOW,
                true, // onlineUserOnly — don't persist
                null,
                object : V2TIMSendCallback<V2TIMMessage> {
                    override fun onProgress(p: Int) {}
                    override fun onSuccess(m: V2TIMMessage) {}
                    override fun onError(c: Int, d: String?) {}
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "sendTypingStatus error", e)
        }
    }

    // ───────── Delete / Revoke ─────────

    /** Revoke (recall) a message within the revoke time window. */
    fun revokeMessage(msg: V2TIMMessage, onResult: (Boolean, String?) -> Unit) {
        msgManager.revokeMessage(msg, object : V2TIMCallback {
            override fun onSuccess() { onResult(true, null) }
            override fun onError(code: Int, desc: String?) { onResult(false, "$code: $desc") }
        })
    }

    // ───────── User Status ─────────

    /** Check if a user is currently online. */
    fun getUserOnlineStatus(userIds: List<String>, onResult: (Map<String, Boolean>) -> Unit) {
        V2TIMManager.getInstance().getUserStatus(userIds, object : V2TIMValueCallback<List<com.tencent.imsdk.v2.V2TIMUserStatus>> {
            override fun onSuccess(statuses: List<com.tencent.imsdk.v2.V2TIMUserStatus>) {
                val map = statuses.associate { it.userID to (it.statusType == com.tencent.imsdk.v2.V2TIMUserStatus.V2TIM_USER_STATUS_ONLINE) }
                onResult(map)
            }
            override fun onError(code: Int, desc: String?) {
                Log.e(TAG, "getUserStatus error: $code – $desc")
                onResult(emptyMap())
            }
        })
    }

    // ───────── Helpers ─────────

    /** Parse a custom element's data into our payload object. */
    fun parseCustomPayload(data: ByteArray?): CustomMessagePayload? {
        if (data == null || data.isEmpty()) return null
        return try {
            gson.fromJson(String(data, Charsets.UTF_8), CustomMessagePayload::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse custom payload", e)
            null
        }
    }
}
