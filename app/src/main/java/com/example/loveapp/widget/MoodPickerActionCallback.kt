package com.example.loveapp.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.loveapp.data.repository.MoodRepository
import com.example.loveapp.utils.DateUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that saves a quick mood entry from a home-screen widget tap.
 *
 * Triggered via [actionSendBroadcast] in [MoodDayWidget]:
 * ```kotlin
 * .clickable(
 *     actionSendBroadcast(
 *         Intent(MoodQuickLogReceiver.ACTION)
 *             .setPackage(context.packageName)
 *             .putExtra(MoodQuickLogReceiver.EXTRA_MOOD_TYPE, moodType)
 *     )
 * )
 * ```
 *
 * The receiver:
 *  1. Saves the mood to Room (+ server via [MoodRepository]).
 *  2. Immediately updates widget state so the emoji appears without reopening the app.
 *  3. Triggers a full re-render of all [MoodDayWidget] instances.
 */
class MoodQuickLogReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MoodWidgetEntryPoint {
        fun moodRepository(): MoodRepository
    }

    companion object {
        const val ACTION = "com.example.loveapp.widget.QUICK_MOOD_LOG"
        const val EXTRA_MOOD_TYPE = "mood_type"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val moodType = intent.getStringExtra(EXTRA_MOOD_TYPE) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    MoodWidgetEntryPoint::class.java
                )
                val today = DateUtils.getTodayDateString()
                entryPoint.moodRepository().createMood(moodType = moodType, date = today)

                // Optimistically update all MoodDayWidget instances so the emoji appears immediately
                val manager = GlanceAppWidgetManager(context)
                manager.getGlanceIds(MoodDayWidget::class.java).forEach { glanceId ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[WidgetUpdater.KEY_MOOD_MY_TYPE] = moodType
                            if (this[WidgetUpdater.KEY_MOOD_DATE].isNullOrEmpty()) {
                                this[WidgetUpdater.KEY_MOOD_DATE] = today
                            }
                        }
                    }
                    MoodDayWidget().update(context, glanceId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
