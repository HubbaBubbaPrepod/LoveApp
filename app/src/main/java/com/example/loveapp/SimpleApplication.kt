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

        // super.onCreate() инициализирует Hilt — без неё приложение не запустится
        super.onCreate()
        Log.d("LoveApp", "SimpleApplication.onCreate() Hilt OK")

        // Остальные шаги инициализации не должны крашить приложение.
        // На MIUI/Android 10 WorkManager или каналы могут бросить исключение
        // при первом запуске — изолируем каждый шаг отдельно.

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

        Log.d("LoveApp", "SimpleApplication.onCreate() DONE")
    }
}
