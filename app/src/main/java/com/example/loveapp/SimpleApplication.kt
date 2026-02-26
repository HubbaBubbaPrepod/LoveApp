package com.example.loveapp

import android.app.Application
import android.util.Log
import com.example.loveapp.notifications.NotificationHelper
import com.example.loveapp.notifications.NotificationWorker
import com.example.loveapp.notifications.ReminderWorker
import com.example.loveapp.widget.WidgetSyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SimpleApplication : Application() {

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        Log.d("LoveApp", "SimpleApplication.onCreate() START")
        try {
            super.onCreate()
            Log.d("LoveApp", "SimpleApplication.onCreate() SUCCESS - Hilt initialized")

            // Widget background sync every 30 min
            WidgetSyncWorker.schedulePeriodicSync(this)

            // Notification channels (must be created before any notification is posted)
            notificationHelper.createChannels()

            // Partner-activity polling every 15 min
            NotificationWorker.schedule(this)

            // Daily reminder at 20:00 local time
            ReminderWorker.schedule(this)

        } catch (e: Exception) {
            Log.e("LoveApp", "SimpleApplication.onCreate() FAILED", e)
            e.printStackTrace()
            throw e
        }
    }
}
