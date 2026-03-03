package com.example.loveapp

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.loveapp.notifications.NotificationHelper
import com.example.loveapp.notifications.NotificationWorker
import com.example.loveapp.notifications.ReminderWorker
import com.example.loveapp.sync.SyncManager
import com.example.loveapp.widget.WidgetSyncWorker
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import javax.inject.Inject

@HiltAndroidApp
class SimpleApplication : Application(), Configuration.Provider {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncManager: SyncManager

    /** Provide Hilt-aware WorkerFactory so @HiltWorker classes are injected correctly. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        Log.d("LoveApp", "SimpleApplication.onCreate() START")

        // Sentry — init before super.onCreate() so crashes during init are captured
        SentryAndroid.init(this) { options ->
            // DSN is read from AndroidManifest meta-data: io.sentry.dsn
            options.isEnableAutoSessionTracking = true
            options.sessionTrackingIntervalMillis = 30_000
            options.isAttachScreenshot = false  // PII-sensitive for a couples app
            options.isEnableUserInteractionTracing = true
            options.tracesSampleRate = 0.2       // 20% of transactions
        }

        // super.onCreate() initialises Hilt — must be called first
        super.onCreate()
        Log.d("LoveApp", "SimpleApplication.onCreate() Hilt OK")

        try {
            WidgetSyncWorker.schedulePeriodicSync(this)
            Log.d("LoveApp", "WidgetSyncWorker scheduled")
        } catch (e: Exception) {
            Log.e("LoveApp", "WidgetSyncWorker schedule failed", e)
        }

        try {
            notificationHelper.createChannels()
            Log.d("LoveApp", "Notification channels created")
        } catch (e: Exception) {
            Log.e("LoveApp", "createChannels failed", e)
        }

        try {
            NotificationWorker.schedule(this)
            Log.d("LoveApp", "NotificationWorker scheduled")
        } catch (e: Exception) {
            Log.e("LoveApp", "NotificationWorker schedule failed", e)
        }

        try {
            ReminderWorker.schedule(this)
            Log.d("LoveApp", "ReminderWorker scheduled")
        } catch (e: Exception) {
            Log.e("LoveApp", "ReminderWorker schedule failed", e)
        }

        // Initialise WebSocket connection + offline sync
        try {
            syncManager.init()
            Log.d("LoveApp", "SyncManager initialised")
        } catch (e: Exception) {
            Log.e("LoveApp", "SyncManager.init() failed", e)
        }

        Log.d("LoveApp", "SimpleApplication.onCreate() DONE")
    }
}
