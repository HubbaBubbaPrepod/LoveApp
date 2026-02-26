package com.example.loveapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.loveapp.MainActivity
import com.example.loveapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central helper that owns notification channels and builds/sends every
 * notification in the app.  Inject it wherever you need to fire a notification.
 *
 * Channels:
 *  • partner_updates  – new mood / activity from your partner (HIGH importance)
 *  • daily_reminder   – "don't forget to log today" nudges (DEFAULT importance)
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ── Constants ────────────────────────────────────────────────────────────

    companion object {
        const val CHANNEL_PARTNER  = "partner_updates"
        const val CHANNEL_REMINDER = "daily_reminder"

        const val NOTIF_PARTNER_MOOD     = 1001
        const val NOTIF_PARTNER_ACTIVITY = 1002
        const val NOTIF_REMINDER_MOOD    = 1003
        const val NOTIF_REMINDER_ACTIVITY = 1004

        /** Preference name for the DataStore that tracks last-seen partner content. */
        const val NOTIF_PREFS_NAME = "notification_prefs"
    }

    // ── Channel creation (call once in Application.onCreate) ─────────────────

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PARTNER,
                "Активность партнёра",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новом настроении и активностях партнёра"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDER,
                "Ежедневные напоминания",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Напоминания о ведении дневника настроения и активностей"
            }
        )
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Returns a [PendingIntent] that opens [MainActivity] and navigates to [destination].
     * Pass an empty string to simply open the app on the current screen.
     */
    private fun openScreenIntent(destination: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (destination.isNotEmpty()) putExtra("destination", destination)
        }
        return PendingIntent.getActivity(
            context,
            destination.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS permission not yet granted — silently skip.
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Partner logged a new mood today. */
    fun sendPartnerMoodNotification(partnerName: String, moodEmoji: String) {
        val name = partnerName.ifBlank { "Партнёр" }
        notify(
            NOTIF_PARTNER_MOOD,
            NotificationCompat.Builder(context, CHANNEL_PARTNER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("$name поделился(ась) настроением $moodEmoji")
                .setContentText("Открой приложение, чтобы увидеть как дела у $name")
                .setAutoCancel(true)
                .setContentIntent(openScreenIntent("mood_tracker"))
        )
    }

    /** Partner logged new activities today. */
    fun sendPartnerActivityNotification(partnerName: String, count: Int) {
        val name = partnerName.ifBlank { "Партнёр" }
        val text = if (count == 1) "добавил(а) 1 активность" else "добавил(а) $count активностей"
        notify(
            NOTIF_PARTNER_ACTIVITY,
            NotificationCompat.Builder(context, CHANNEL_PARTNER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("$name $text сегодня 🏃")
                .setContentText("Посмотри, чем занимался(ась) $name")
                .setAutoCancel(true)
                .setContentIntent(openScreenIntent("activity_feed"))
        )
    }

    /** Remind the user to log their mood for today. */
    fun sendMoodReminderNotification() {
        notify(
            NOTIF_REMINDER_MOOD,
            NotificationCompat.Builder(context, CHANNEL_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Как твоё настроение? 💬")
                .setContentText("Не забудь записать своё настроение за сегодня")
                .setAutoCancel(true)
                .setContentIntent(openScreenIntent("mood_tracker"))
        )
    }

    /** Partner started a new cycle or logged a cycle day. */
    fun sendPartnerCycleNotification(partnerName: String, isNewCycle: Boolean) {
        val name  = partnerName.ifBlank { "Партнёр" }
        val title = if (isNewCycle)
            "$name начал(а) новый цикл 🌸"
        else
            "$name обновил(а) данные цикла 🌸"
        val body = "Открой приложение, чтобы посмотреть"
        notify(
            NOTIF_PARTNER_MOOD,   // reuse the partner channel slot — distinct enough
            NotificationCompat.Builder(context, CHANNEL_PARTNER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(openScreenIntent("menstrual_calendar"))
        )
    }

    /** Fallback for any other FCM message with a notification payload. */
    fun sendGenericNotification(title: String, body: String) {
        if (title.isBlank() && body.isBlank()) return
        notify(
            NOTIF_PARTNER_MOOD,
            NotificationCompat.Builder(context, CHANNEL_PARTNER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(openScreenIntent(""))
        )
    }

    /** Remind the user to log their activities for today. */
    fun sendActivityReminderNotification() {
        notify(
            NOTIF_REMINDER_ACTIVITY,
            NotificationCompat.Builder(context, CHANNEL_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Твои активности за сегодня? 🏃")
                .setContentText("Запиши, чем ты занимался(ась) сегодня")
                .setAutoCancel(true)
                .setContentIntent(openScreenIntent("activity_feed"))
        )
    }
}
