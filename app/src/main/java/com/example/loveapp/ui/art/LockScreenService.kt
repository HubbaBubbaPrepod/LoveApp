package com.example.loveapp.ui.art

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.loveapp.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service that monitors screen state and launches [LockScreenPanelActivity]
 * вЂ” a transparent Activity with setShowWhenLocked(true) вЂ” when the keyguard is active.
 *
 * This is the only reliable approach on Android 8+ to show UI above the keyguard
 * from a third-party app. WindowManager TYPE_APPLICATION_OVERLAY does NOT reliably
 * appear above the keyguard on Android 10+.
 *
 * Usage:
 *   LockScreenService.start(context)
 *   LockScreenService.stop(context)
 */
@AndroidEntryPoint
class LockScreenService : Service() {

    companion object {
        private const val CHANNEL_ID      = "lock_screen_canvas_silent"
        private const val NOTIFICATION_ID = 2001
        private const val EXTRA_CANVAS_ID = "canvas_id"

        fun start(context: Context, canvasId: Int = -1) {
            val intent = Intent(context, LockScreenService::class.java)
                .putExtra(EXTRA_CANVAS_ID, canvasId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockScreenService::class.java))
        }
    }

    private var storedCanvasId: Int = -1
    private val handler = Handler(Looper.getMainLooper())

    // в”Ђв”Ђ Screen lock / unlock receiver в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    // Delay 250ms so the keyguard has time to fully settle
                    handler.postDelayed({
                        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
                        if (km.isKeyguardLocked) showPanelActivity()
                    }, 250)
                }
                Intent.ACTION_SCREEN_OFF -> closePanelActivity()
            }
        }
    }

    // в”Ђв”Ђ Lifecycle в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildSilentNotification())
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        storedCanvasId = intent?.getIntExtra(EXTRA_CANVAS_ID, -1) ?: -1
        // If screen is already on and locked when service starts, show panel immediately
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        val screenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            pm.isInteractive
        } else {
            @Suppress("DEPRECATION") pm.isScreenOn
        }
        if (screenOn && km.isKeyguardLocked) showPanelActivity()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(screenReceiver) }
        closePanelActivity()
        super.onDestroy()
    }

    // в”Ђв”Ђ Panel activity control в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    private fun showPanelActivity() {
        startActivity(Intent(this, LockScreenPanelActivity::class.java).apply {
            putExtra("canvas_id", storedCanvasId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
    }

    private fun closePanelActivity() {
        sendBroadcast(Intent(LockScreenPanelActivity.ACTION_CLOSE).apply {
            setPackage(packageName)
        })
    }

    // в”Ђв”Ђ Silent foreground notification (required by Android OS) в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    private fun buildSilentNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Р РёСЃРѕРІР°РЅРёРµ Р°РєС‚РёРІРЅРѕ")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                "Р РёСЃРѕРІР°РЅРёРµ (СЃРёСЃС‚РµРјРЅС‹Р№ СЃРµСЂРІРёСЃ)",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }.also {
                getSystemService(NotificationManager::class.java).createNotificationChannel(it)
            }
        }
    }
}