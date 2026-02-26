package com.example.loveapp.notifications

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Runs once per day and reminds the user to log their mood and activities
 * if they haven't done so yet.
 *
 * The initial delay is calculated so the first run fires at 20:00 local time.
 * If it is already past 20:00, the first run is deferred to the same time
 * tomorrow.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // ── Hilt entry point ─────────────────────────────────────────────────────

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderEntryPoint {
        fun moodRepository(): MoodRepository
        fun activityRepository(): ActivityRepository
        fun notificationHelper(): NotificationHelper
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val WORK_NAME = "loveapp_daily_reminder"

        // Settings DataStore keys (must match SettingsViewModel companion)
        private val REMINDERS_ENABLED_KEY = booleanPreferencesKey("reminders_enabled")

        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Schedules the first run at 20:00 local time, then repeats every 24 hours. */
        fun schedule(context: Context) {
            val initialDelay = millisUntil(hour = 20, minute = 0)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setConstraints(networkConstraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /**
         * Returns the number of milliseconds from now until the next occurrence
         * of [hour]:[minute] in the local time zone.  If the target time is in the
         * past today, the next occurrence is used (24 h from now minus remainder).
         */
        private fun millisUntil(hour: Int, minute: Int): Long {
            val now    = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
            return target.timeInMillis - now.timeInMillis
        }
    }

    // ── Worker body ───────────────────────────────────────────────────────────

    override suspend fun doWork(): Result {
        return try {
            val ep = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)

            // 1. Check reminders toggle
            val remindersEnabled = applicationContext.settingsDataStore
                .data.first()[REMINDERS_ENABLED_KEY] ?: true
            if (!remindersEnabled) return Result.success()

            val moodRepo = ep.moodRepository()
            val actRepo  = ep.activityRepository()
            val helper   = ep.notificationHelper()
            val today    = DateUtils.getTodayDateString()

            // 2. Check if the user already logged mood today
            val myMoods = moodRepo.getMoods(date = today).getOrElse { emptyList() }
            if (myMoods.isEmpty()) {
                helper.sendMoodReminderNotification()
            }

            // 3. Check if the user already logged activity today
            val myActs = actRepo.getActivities(date = today).getOrElse { emptyList() }
            if (myActs.isEmpty()) {
                helper.sendActivityReminderNotification()
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
