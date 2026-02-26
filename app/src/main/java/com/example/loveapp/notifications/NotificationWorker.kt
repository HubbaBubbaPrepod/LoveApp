package com.example.loveapp.notifications

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.loveapp.data.repository.ActivityRepository
import com.example.loveapp.data.repository.MoodRepository
import com.example.loveapp.utils.DateUtils
import com.example.loveapp.utils.settingsDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

// DataStore for notification state (last-seen partner counts) - unique file name, no conflict
private val Context.notifDataStore by preferencesDataStore("notification_state")

/**
 * Runs every 15 minutes and checks whether the partner posted new mood entries or
 * activities since the last check.  When new content is detected it fires the
 * matching notification via [NotificationHelper].
 *
 * Uses the same @EntryPoint / EntryPointAccessors pattern as WidgetSyncWorker so that
 * we don't need the hilt-work artifact.
 */
class NotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // ── Hilt entry point ─────────────────────────────────────────────────────

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotifEntryPoint {
        fun moodRepository(): MoodRepository
        fun activityRepository(): ActivityRepository
        fun notificationHelper(): NotificationHelper
    }

    // ── DataStore keys ────────────────────────────────────────────────────────

    companion object {
        private const val WORK_NAME = "loveapp_notification_check"

        // Settings DataStore keys (must match SettingsViewModel companion)
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")

        // Notification-state DataStore keys
        private val KEY_LAST_PT_MOOD_COUNT = intPreferencesKey("last_pt_mood_count")
        private val KEY_LAST_PT_ACT_COUNT  = intPreferencesKey("last_pt_act_count")

        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraints)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    // ── Worker body ───────────────────────────────────────────────────────────

    override suspend fun doWork(): Result {
        return try {
            val ep     = EntryPointAccessors.fromApplication(applicationContext, NotifEntryPoint::class.java)
            val helper = ep.notificationHelper()

            // 1. Check master notifications toggle (uses the same "settings" DataStore)
            val settingsStore = applicationContext.settingsDataStore
            val notifEnabled  = settingsStore.data.first()[NOTIFICATIONS_ENABLED_KEY] ?: true
            if (!notifEnabled) return Result.success()

            val moodRepo = ep.moodRepository()
            val actRepo  = ep.activityRepository()
            val today    = DateUtils.getTodayDateString()
            val store    = applicationContext.notifDataStore

            // 2. Read last-seen counts from notification state DataStore
            val prefs          = store.data.first()
            val lastMoodCount  = prefs[KEY_LAST_PT_MOOD_COUNT] ?: 0
            val lastActCount   = prefs[KEY_LAST_PT_ACT_COUNT]  ?: 0

            // 3. Fetch current partner data
            val ptMoods = moodRepo.getPartnerMoods(date = today).getOrElse { emptyList() }
            val ptActs  = actRepo.getPartnerActivities(date = today).getOrElse { emptyList() }
            val curMoodCount = ptMoods.size
            val curActCount  = ptActs.size

            // 4. Notify if count increased since last check
            if (curMoodCount > lastMoodCount && ptMoods.isNotEmpty()) {
                val mood    = ptMoods.firstOrNull()
                val name    = mood?.displayName ?: ""
                val emoji   = moodTypeToEmoji(mood?.moodType ?: "")
                helper.sendPartnerMoodNotification(name, emoji)
            }

            if (curActCount > lastActCount && ptActs.isNotEmpty()) {
                val name = ptActs.firstOrNull()?.displayName ?: ""
                helper.sendPartnerActivityNotification(name, curActCount)
            }

            // 5. Persist new counts
            store.edit { p ->
                p[KEY_LAST_PT_MOOD_COUNT] = curMoodCount
                p[KEY_LAST_PT_ACT_COUNT]  = curActCount
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun moodTypeToEmoji(type: String) = when (type.lowercase()) {
        "great", "отлично"    -> "😄"
        "good", "хорошо"      -> "🙂"
        "okay", "нормально"   -> "😐"
        "bad", "плохо"        -> "😔"
        "terrible", "ужасно"  -> "😢"
        else                  -> "💬"
    }
}
