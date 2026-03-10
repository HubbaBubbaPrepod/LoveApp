package com.example.loveapp.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.GeofenceEventRequest
import com.example.loveapp.data.repository.AuthRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages registration/unregistration of geofences with Google Play Services
 * and handles geofence transition events.
 */
@Singleton
class GeofenceManager @Inject constructor(
    private val geofencingClient: GeofencingClient
) {
    companion object {
        const val TAG = "GeofenceManager"
        const val ACTION_GEOFENCE_EVENT = "com.example.loveapp.GEOFENCE_EVENT"
    }

    /**
     * Register a single geofence.
     * @param context Application context
     * @param requestId Unique string ID (use server geofence ID)
     * @param lat Latitude
     * @param lon Longitude
     * @param radiusMeters Radius in meters
     * @param transitionTypes Bitmask of [Geofence.GEOFENCE_TRANSITION_ENTER], [Geofence.GEOFENCE_TRANSITION_EXIT], [Geofence.GEOFENCE_TRANSITION_DWELL]
     */
    @SuppressLint("MissingPermission")
    fun registerGeofence(
        context: Context,
        requestId: String,
        lat: Double,
        lon: Double,
        radiusMeters: Float = 200f,
        transitionTypes: Int = Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
    ) {
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "No location permission, skipping geofence registration")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(lat, lon, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes)
            .setLoiteringDelay(5 * 60 * 1000) // 5 min for dwell
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, getGeofencePendingIntent(context))
            .addOnSuccessListener { Log.d(TAG, "Geofence registered: $requestId") }
            .addOnFailureListener { Log.e(TAG, "Geofence registration failed: $requestId", it) }
    }

    /**
     * Register multiple geofences at once.
     */
    @SuppressLint("MissingPermission")
    fun registerGeofences(
        context: Context,
        geofences: List<GeofenceData>
    ) {
        if (!hasLocationPermission(context) || geofences.isEmpty()) return

        val gfList = geofences.map { gf ->
            var transitions = 0
            if (gf.notifyOnEnter) transitions = transitions or Geofence.GEOFENCE_TRANSITION_ENTER
            if (gf.notifyOnExit) transitions = transitions or Geofence.GEOFENCE_TRANSITION_EXIT

            Geofence.Builder()
                .setRequestId(gf.requestId)
                .setCircularRegion(gf.latitude, gf.longitude, gf.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transitions)
                .setLoiteringDelay(5 * 60 * 1000)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(gfList)
            .build()

        geofencingClient.addGeofences(request, getGeofencePendingIntent(context))
            .addOnSuccessListener { Log.d(TAG, "Registered ${geofences.size} geofences") }
            .addOnFailureListener { Log.e(TAG, "Batch geofence registration failed", it) }
    }

    /**
     * Remove a geofence by request ID.
     */
    fun removeGeofence(requestId: String) {
        geofencingClient.removeGeofences(listOf(requestId))
            .addOnSuccessListener { Log.d(TAG, "Geofence removed: $requestId") }
            .addOnFailureListener { Log.e(TAG, "Geofence removal failed: $requestId", it) }
    }

    /**
     * Remove all registered geofences.
     */
    fun removeAllGeofences(context: Context) {
        geofencingClient.removeGeofences(getGeofencePendingIntent(context))
            .addOnSuccessListener { Log.d(TAG, "All geofences removed") }
            .addOnFailureListener { Log.e(TAG, "Failed to remove all geofences", it) }
    }

    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Data class for batch geofence registration.
 */
data class GeofenceData(
    val requestId: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 200f,
    val notifyOnEnter: Boolean = true,
    val notifyOnExit: Boolean = true
)

/**
 * BroadcastReceiver that handles geofence transition events from the OS
 * and reports them to the backend.
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var apiService: LoveAppApiService
    @Inject lateinit var authRepository: AuthRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GeofenceManager.ACTION_GEOFENCE_EVENT) return

        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e(GeofenceManager.TAG, "Geofence event error: ${event.errorCode}")
            return
        }

        val transitionType = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "enter"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "exit"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "dwell"
            else -> return
        }

        val triggeringLocation = event.triggeringLocation

        event.triggeringGeofences?.forEach { geofence ->
            val geofenceId = geofence.requestId.toLongOrNull() ?: return@forEach
            Log.d(GeofenceManager.TAG, "Geofence $transitionType: $geofenceId")

            scope.launch {
                try {
                    val token = "Bearer ${authRepository.getToken()}"
                    apiService.reportGeofenceEvent(
                        token,
                        GeofenceEventRequest(
                            geofenceId = geofenceId,
                            eventType = transitionType,
                            latitude = triggeringLocation?.latitude,
                            longitude = triggeringLocation?.longitude
                        )
                    )
                } catch (e: Exception) {
                    Log.e(GeofenceManager.TAG, "Failed to report geofence event", e)
                }
            }
        }
    }
}
