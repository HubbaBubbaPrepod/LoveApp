package com.example.loveapp.notifications

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.example.loveapp.utils.settingsDataStore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Receives FCM messages and token refreshes.
 *
 * The server sends DATA-ONLY messages (no `notification` block) so Android always
 * routes delivery through this service — even when the app is in the background —
 * which lets us honour the user’s [notificationsEnabled] toggle.
 */
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var fcmTokenManager: FcmTokenManager

    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")

    /** Called for EVERY incoming FCM message (foreground AND background, data-only). */
    override fun onMessageReceived(message: RemoteMessage) {
        // Respect the user’s master toggle — read synchronously (we’re on a worker thread)
        val enabled = runBlocking {
            applicationContext.settingsDataStore.data.first()[notificationsEnabledKey] ?: true
        }
        if (!enabled) return

        val data = message.data
        when (data["type"]) {
            "partner_mood" -> notificationHelper.sendPartnerMoodNotification(
                partnerName = data["partnerName"] ?: "",
                moodEmoji   = data["moodEmoji"]   ?: "💬"
            )
            "partner_activity" -> notificationHelper.sendPartnerActivityNotification(
                partnerName = data["partnerName"] ?: "",
                count       = data["count"]?.toIntOrNull() ?: 1
            )
            "partner_cycle" -> notificationHelper.sendPartnerCycleNotification(
                partnerName = data["partnerName"] ?: "",
                isNewCycle  = data["isNewCycle"] == "true"
            )
            else -> {
                val title = data["title"] ?: return
                val body  = data["body"]  ?: return
                notificationHelper.sendGenericNotification(title = title, body = body)
            }
        }
    }

    /** Called when FCM rotates the registration token. Re-register with our server. */
    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            fcmTokenManager.registerToken(token)
        }
    }
}
