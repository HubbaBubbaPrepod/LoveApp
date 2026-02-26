package com.example.loveapp.notifications

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.FcmTokenRequest
import com.example.loveapp.utils.TokenManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the device's FCM registration token.
 *
 * Call [refreshAndRegister] once after a successful login to make sure
 * the server always has the latest token for this device.
 * [onNewToken] (called from [MyFirebaseMessagingService]) keeps it up-to-date
 * when FCM rotates the token automatically.
 */
@Singleton
class FcmTokenManager @Inject constructor(
    private val apiService: LoveAppApiService,
    private val tokenManager: TokenManager
) {
    /** Fetches the current FCM token and sends it to our server. */
    suspend fun refreshAndRegister() {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            registerToken(fcmToken)
        } catch (_: Exception) { /* FCM not available — ignore */ }
    }

    /** Registers an already-known [fcmToken] with our server. */
    suspend fun registerToken(fcmToken: String) {
        val authToken = tokenManager.getToken() ?: return
        try {
            apiService.registerFcmToken(
                authHeader = "Bearer $authToken",
                request    = FcmTokenRequest(fcmToken = fcmToken)
            )
        } catch (_: Exception) { /* Network error — will retry on next login */ }
    }
}
