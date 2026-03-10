package com.example.loveapp.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.loveapp.MainActivity
import com.example.loveapp.R
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.PhoneStatusUpdateRequest
import com.example.loveapp.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class PhoneStatusService : Service() {

    companion object {
        const val TAG = "PhoneStatusService"
        const val CHANNEL_ID = "phone_status_channel"
        const val NOTIFICATION_ID = 9002
        const val ACTION_START = "com.example.loveapp.phonestatus.START"
        const val ACTION_STOP = "com.example.loveapp.phonestatus.STOP"
        private const val DEFAULT_INTERVAL_MS = 30_000L

        fun start(context: Context) {
            val intent = Intent(context, PhoneStatusService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PhoneStatusService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject lateinit var apiService: LoveAppApiService
    @Inject lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var isScreenOn = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                pollingJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForegroundWithNotification()
                startPolling()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Мониторинг статуса активен")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    sendStatusUpdate()
                } catch (e: Exception) {
                    Log.w(TAG, "Status update failed: ${e.message}")
                }
                delay(DEFAULT_INTERVAL_MS)
            }
        }
        Log.i(TAG, "Phone status polling started, interval=${DEFAULT_INTERVAL_MS}ms")
    }

    private suspend fun sendStatusUpdate() {
        val token = authRepository.getToken() ?: return

        val batteryInfo = getBatteryInfo()
        val wifiName = getWifiName()
        val networkType = getNetworkType()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val screenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            pm.isInteractive
        } else {
            @Suppress("DEPRECATION")
            pm.isScreenOn
        }
        isScreenOn = screenOn

        val request = PhoneStatusUpdateRequest(
            batteryLevel = batteryInfo.first,
            isCharging = batteryInfo.second,
            screenStatus = if (screenOn) "on" else "off",
            wifiName = wifiName,
            isActive = screenOn,
            networkType = networkType,
            appInForeground = isAppInForeground()
        )

        try {
            apiService.updatePhoneStatus("Bearer $token", request)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send phone status: ${e.message}")
        }
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = bm.isCharging
        return level to charging
    }

    @SuppressLint("MissingPermission")
    private fun getWifiName(): String? {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        @Suppress("DEPRECATION")
        val info = wifiManager.connectionInfo ?: return null
        @Suppress("DEPRECATION")
        val ssid = info.ssid
        if (ssid == null || ssid == "<unknown ssid>" || ssid == "0x") return null
        return ssid.removePrefix("\"").removeSuffix("\"")
    }

    private fun getNetworkType(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "none"
        val caps = cm.getNetworkCapabilities(network) ?: return "none"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            else -> "none"
        }
    }

    private fun isAppInForeground(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val tasks = am.getRunningTasks(1)
        if (tasks.isNullOrEmpty()) return false
        val topActivity = tasks[0].topActivity ?: return false
        return topActivity.packageName == packageName
    }

    private fun registerScreenReceiver() {
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        isScreenOn = true
                        serviceScope.launch { sendStatusUpdate() }
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenOn = false
                        serviceScope.launch { sendStatusUpdate() }
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        // Screen unlocked
                        serviceScope.launch { sendStatusUpdate() }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Love App")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Мониторинг статуса телефона",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Показывает, что приложение отслеживает статус телефона"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        screenReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
