package com.example.loveapp.im

import android.util.Log
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.dao.ChatMessageDao
import com.example.loveapp.utils.TokenManager
import kotlinx.coroutines.flow.SharedFlow
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the TIM SDK login lifecycle:
 *  1. Fetch UserSig from backend (GET /api/tim/usersig)
 *  2. Login to TIM via [TIMManager]
 *  3. Register [TIMMessageListener] to receive incoming messages
 *
 * Mirrors Love8's auto-login pattern: call [login] after REST auth,
 * call [logout] when the user signs out.
 */
@Singleton
class TIMLoginManager @Inject constructor(
    private val apiService: LoveAppApiService,
    private val tokenManager: TokenManager,
    private val chatMessageDao: ChatMessageDao
) {
    private val TAG = "TIMLoginManager"

    private var messageListener: TIMMessageListener? = null

    /** Expose real-time custom events (spark, intimacy, love-touch, draw) from the listener. */
    val customEvents: SharedFlow<CustomMessagePayload>?
        get() = messageListener?.customEvents

    /** Expose typing events (partner userId string). */
    val typingEvents: SharedFlow<String>?
        get() = messageListener?.typingEvents

    /** Expose read-receipt events. */
    val readReceiptEvents: SharedFlow<Unit>?
        get() = messageListener?.readReceiptEvents

    /** Expose message-revoked events (TIM msgID). */
    val revokeEvents: SharedFlow<String>?
        get() = messageListener?.revokeEvents

    /**
     * Fetch UserSig from the backend and login to TIM SDK.
     * Should be called after REST authentication succeeds.
     * Safe to call multiple times — no-ops if already logged in.
     */
    suspend fun login(): Result<Unit> {
        if (TIMManager.isLoggedIn()) {
            Log.d(TAG, "Already logged in to TIM, skipping")
            return Result.success(Unit)
        }

        val token = tokenManager.getToken() ?: return Result.failure(Exception("No auth token"))
        val userId = tokenManager.getUserId()?.toIntOrNull() ?: return Result.failure(Exception("No userId"))

        return try {
            val resp = apiService.getTIMUserSig("Bearer $token")
            if (!resp.success || resp.data == null) {
                return Result.failure(Exception(resp.message ?: "Failed to get TIM UserSig"))
            }

            val timData = resp.data
            Log.d(TAG, "Got UserSig for TIM userId=${timData.userId}, sdkAppId=${timData.sdkAppId}")

            // Update sdkAppId if the backend provides a different one
            if (timData.sdkAppId != TIMManager.sdkAppId && timData.sdkAppId > 0) {
                Log.w(TAG, "Backend sdkAppId (${timData.sdkAppId}) differs from init (${TIMManager.sdkAppId})")
            }

            // Login to TIM
            val result = kotlinx.coroutines.suspendCancellableCoroutine<Result<Unit>> { cont ->
                TIMManager.login(timData.userId, timData.userSig) { success, error ->
                    if (success) {
                        cont.resume(Result.success(Unit))
                    } else {
                        cont.resume(Result.failure(Exception("TIM login failed: $error")))
                    }
                }
            }

            if (result.isSuccess) {
                // Register message listener
                val coupleKey = buildCoupleKey(userId)
                val listener = TIMMessageListener(chatMessageDao, userId, coupleKey)
                listener.register()
                messageListener = listener
                Log.d(TAG, "TIM login + listener registration complete")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "TIM login failed", e)
            Result.failure(e)
        }
    }

    /**
     * Logout from TIM and unregister the message listener.
     * Call from AuthViewModel.logout().
     */
    fun logout() {
        messageListener?.unregister()
        messageListener = null
        TIMManager.logout()
        Log.d(TAG, "TIM logout complete")
    }

    /** Build couple key from userId. Uses partner ID from TokenManager if available. */
    private suspend fun buildCoupleKey(userId: Int): String {
        val partnerId = tokenManager.getPartnerId()?.toIntOrNull()
        return if (partnerId != null && partnerId > 0) {
            "${minOf(userId, partnerId)}_${maxOf(userId, partnerId)}"
        } else {
            // Fallback — will be updated when relationship data loads
            "${userId}_0"
        }
    }
}
