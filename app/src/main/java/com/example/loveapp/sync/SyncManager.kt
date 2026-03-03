package com.example.loveapp.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.loveapp.network.SocketState
import com.example.loveapp.network.WebSocketManager
import com.example.loveapp.utils.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates sync:
 *  - Maintains the WebSocket connection with the server.
 *  - Registers a [ConnectivityManager.NetworkCallback] so that when the device
 *    regains internet access the socket reconnects and [SyncWorker] is
 *    triggered immediately.
 *  - Schedules a periodic background [SyncWorker] for resilience.
 *
 * Call [init] once from [com.example.loveapp.SimpleApplication.onCreate].
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webSocketManager: WebSocketManager,
    private val tokenManager: TokenManager,
    private val dataPreloadManager: DataPreloadManager
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i(TAG, "Network available – reconnecting socket & triggering sync")
            webSocketManager.connect()
            SyncWorker.scheduleImmediateSync(context)
        }

        override fun onLost(network: Network) {
            Log.i(TAG, "Network lost")
            // Socket will auto-reconnect; no explicit disconnect needed
        }
    }

    fun init() {
        scope.launch { initInternal() }
    }

    private suspend fun initInternal() {
        // Only initialise if the user is logged in.
        if (tokenManager.getToken() == null) {
            Log.d(TAG, "init() skipped – no auth token")
            return
        }

        // Connect socket
        webSocketManager.connect()

        // Reconnect automatically if the connection drops into ERROR state
        webSocketManager.connectionState
            .onEach { state ->
                if (state == SocketState.ERROR) {
                    Log.w(TAG, "Socket in ERROR state – scheduling reconnect via immediate sync")
                    SyncWorker.scheduleImmediateSync(context)
                }
            }
            .launchIn(scope)

        // Register network callback
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            cm.registerNetworkCallback(req, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "registerNetworkCallback failed", e)
        }

        // Periodic background sync (15-minute cadence, survives process kill)
        SyncWorker.schedulePeriodicSync(context)

        // Preload all repository caches so every screen opens with data already in Room
        scope.launch { dataPreloadManager.preloadAll() }

        Log.i(TAG, "SyncManager initialised")
    }

    /** Call after login to start syncing with a fresh token. */
    fun onLogin() {
        webSocketManager.reconnectWithNewToken()
        SyncWorker.scheduleImmediateSync(context)
        scope.launch { dataPreloadManager.preloadAll() }
    }

    /** Call after logout to tear down the connection. */
    fun onLogout() {
        webSocketManager.disconnect()
    }
}
