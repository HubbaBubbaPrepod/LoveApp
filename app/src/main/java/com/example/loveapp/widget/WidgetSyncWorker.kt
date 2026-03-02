package com.example.loveapp.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.loveapp.data.api.models.ActivityResponse
import com.example.loveapp.data.repository.ActivityRepository
import com.example.loveapp.data.repository.MoodRepository
import com.example.loveapp.ui.screens.CUSTOM_ICON_MAP
import com.example.loveapp.utils.DateUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that fetches today's mood and activity data from the server
 * and pushes it to all home-screen widgets.
 *
 * Runs every 30 minutes in the background (even when the app is closed).
 * Uses [EntryPointAccessors] to access Hilt-managed repositories without
 * requiring the hilt-work artifact.
 */
class WidgetSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // ── Hilt entry point (no hilt-work dependency needed) ─────────────────────
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncEntryPoint {
        fun moodRepository(): MoodRepository
        fun activityRepository(): ActivityRepository
        fun widgetUpdater(): WidgetUpdater
    }

    // ── Worker body ───────────────────────────────────────────────────────────

    override suspend fun doWork(): Result = try {
        val ep      = EntryPointAccessors.fromApplication(applicationContext, SyncEntryPoint::class.java)
        val moodRepo = ep.moodRepository()
        val actRepo  = ep.activityRepository()
        val updater  = ep.widgetUpdater()
        val today    = DateUtils.getTodayDateString()

        // Fetch all four collections in parallel-ish (sequential is fine for background worker)
        val myMoods = moodRepo.getMoods(date = today).getOrElse { emptyList() }
        val ptMoods = moodRepo.getPartnerMoods(date = today).getOrElse { emptyList() }
        val myActs  = actRepo.getActivities(date = today).getOrElse { emptyList() }
        val ptActs  = actRepo.getPartnerActivities(date = today).getOrElse { emptyList() }

        // Resolve custom activity types so c_1/c_3 → proper name + icon
        val customTypes = actRepo.getCustomActivityTypes().getOrElse { emptyList() }

        // Push mood data
        val myFirst = myMoods.firstOrNull()
        val ptFirst = ptMoods.firstOrNull()
        // Prerender mood avatars
        val myMoodAvatar = WidgetIconPreparer.prepareAvatar(applicationContext, myFirst?.userAvatar, "mood_my")
        val ptMoodAvatar = WidgetIconPreparer.prepareAvatar(applicationContext, ptFirst?.userAvatar, "mood_pt")
        updater.pushMoodUpdate(
            myType = myFirst?.moodType     ?: "",
            myNote = myFirst?.note         ?: "",
            myName = myFirst?.displayName,
            ptType = ptFirst?.moodType     ?: "",
            ptNote = ptFirst?.note         ?: "",
            ptName = ptFirst?.displayName,
            myAvatarPath = myMoodAvatar,
            ptAvatarPath = ptMoodAvatar
        )

        // Build (displayName, rawIconValue) pairs — same logic as ActivityViewModel
        fun buildTypeIconPairs(list: List<ActivityResponse>) =
            list.map { a ->
                val displayName = if (a.activityType.startsWith("c_")) a.title else a.activityType
                val rawIconValue = if (a.activityType.startsWith("c_")) {
                    val id = a.activityType.removePrefix("c_").toIntOrNull()
                    customTypes.find { it.id == id }?.emoji ?: ""
                } else {
                    a.activityType  // built-in key like "work", "sport"
                }
                Pair(displayName, rawIconValue)
            }.distinctBy { it.first }.take(4)

        val myPairs = buildTypeIconPairs(myActs)
        val ptPairs = buildTypeIconPairs(ptActs)
        val myTypes = myPairs.joinToString(",") { it.first }
        val ptTypes = ptPairs.joinToString(",") { it.first }

        // Pre-render icons to cached PNG files (same as ActivityViewModel)
        val myIcons = WidgetIconPreparer.prepareIcons(
            applicationContext, myPairs.map { it.second }, CUSTOM_ICON_MAP, "my")
        val ptIcons = WidgetIconPreparer.prepareIcons(
            applicationContext, ptPairs.map { it.second }, CUSTOM_ICON_MAP, "pt")

        // Prerender activity avatars
        val myActAvatar = WidgetIconPreparer.prepareAvatar(applicationContext, myActs.firstOrNull()?.userAvatar, "act_my")
        val ptActAvatar = WidgetIconPreparer.prepareAvatar(applicationContext, ptActs.firstOrNull()?.userAvatar, "act_pt")

        updater.pushActivityUpdate(
            myCount = myActs.size,
            myTypes = myTypes,
            myIcons = myIcons,
            myName  = myActs.firstOrNull()?.displayName,
            ptCount = ptActs.size,
            ptTypes = ptTypes,
            ptIcons = ptIcons,
            ptName  = ptActs.firstOrNull()?.displayName,
            myAvatarPath = myActAvatar,
            ptAvatarPath = ptActAvatar
        )

        Result.success()
    } catch (e: Exception) {
        // Retry on any error (e.g. no network right now)
        Result.retry()
    }

    // ── Scheduling helpers ────────────────────────────────────────────────────

    companion object {
        private const val WORK_NAME = "loveapp_widget_sync_periodic"

        private val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Enqueue a periodic sync every 30 minutes.
         * [ExistingPeriodicWorkPolicy.KEEP] means calling this multiple times is safe —
         * it only registers the job once.
         */
        fun schedulePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetSyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /**
         * Run an immediate one-shot sync (called from widget receivers on every onUpdate).
         */
        fun runImmediately(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetSyncWorker>()
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueue(request)
        }
    }
}
