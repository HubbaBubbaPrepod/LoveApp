package com.example.loveapp.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.loveapp.MainActivity
import com.example.loveapp.R
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.LocationUpdateRequest
import com.example.loveapp.data.repository.AuthRepository
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    companion object {
        const val TAG = "LocationTrackingService"
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 9001
        const val ACTION_START = "com.example.loveapp.location.START"
        const val ACTION_STOP = "com.example.loveapp.location.STOP"
        const val EXTRA_INTERVAL_MS = "interval_ms"

        fun start(context: Context, intervalMs: Long = 60_000L) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INTERVAL_MS, intervalMs)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient
    @Inject lateinit var apiService: LoveAppApiService
    @Inject lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var locationCallback: LocationCallback? = null
    private var intervalMs = 60_000L

    // Offline buffer for when network is unavailable
    private val offlineBuffer = mutableListOf<LocationUpdateRequest>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 60_000L)
                    .coerceIn(10_000L, 300_000L)
                startForegroundWithNotification()
                startLocationUpdates()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Отслеживание местоположения активно")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted")
            stopSelf()
            return
        }

        stopLocationUpdates() // Clear any existing callback

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val batteryInfo = getBatteryInfo()
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }

                val request = LocationUpdateRequest(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    speed = location.speed,
                    bearing = location.bearing,
                    altitude = location.altitude,
                    battery_level = batteryInfo.first,
                    is_charging = batteryInfo.second,
                    activity_type = "unknown",
                    recorded_at = isoFormat.format(Date(location.time))
                )

                serviceScope.launch {
                    sendLocationToServer(request)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        Log.i(TAG, "Location updates started, interval=${intervalMs}ms")
    }

    private suspend fun sendLocationToServer(request: LocationUpdateRequest) {
        try {
            val token = authRepository.getToken() ?: return
            apiService.updateLocation("Bearer $token", request)

            // Flush offline buffer if we have connectivity
            if (offlineBuffer.isNotEmpty()) {
                flushOfflineBuffer(token)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send location, buffering: ${e.message}")
            synchronized(offlineBuffer) {
                if (offlineBuffer.size < 500) {
                    offlineBuffer.add(request)
                }
            }
        }
    }

    private suspend fun flushOfflineBuffer(token: String) {
        val toSend: List<LocationUpdateRequest>
        synchronized(offlineBuffer) {
            toSend = offlineBuffer.toList()
            offlineBuffer.clear()
        }
        if (toSend.isEmpty()) return
        try {
            apiService.batchLocationUpdate("Bearer $token", mapOf("points" to toSend))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to flush buffer: ${e.message}")
            synchronized(offlineBuffer) {
                offlineBuffer.addAll(0, toSend.take(500 - offlineBuffer.size))
            }
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = bm.isCharging
        return level to charging
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
            "Отслеживание местоположения",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Показывает, что приложение отслеживает местоположение"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }
}
