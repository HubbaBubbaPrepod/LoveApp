package com.example.loveapp.im

import android.content.Context
import android.util.Log
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMSDKConfig
import com.tencent.imsdk.v2.V2TIMSDKListener

/**
 * Singleton wrapper around Tencent IM SDK.
 * Call [init] once from Application.onCreate() and [login] after the user authenticates.
 */
object TIMManager {

    private const val TAG = "TIMManager"

    /** Replace with your Tencent IM SDKAppID from the console. */
    // TODO: Move to BuildConfig or remote config
    var sdkAppId: Int = 0
        private set

    @Volatile
    private var initialized = false

    @Volatile
    private var loggedIn = false

    /** Initialise the IM SDK. Safe to call multiple times — subsequent calls are no-ops. */
    fun init(context: Context, sdkAppId: Int) {
        if (initialized) return
        this.sdkAppId = sdkAppId

        val config = V2TIMSDKConfig().apply {
            logLevel = V2TIMSDKConfig.V2TIM_LOG_WARN
        }

        V2TIMManager.getInstance().initSDK(
            context.applicationContext,
            sdkAppId,
            config,
            object : V2TIMSDKListener() {
                override fun onConnecting() {
                    Log.d(TAG, "IM connecting…")
                }
                override fun onConnectSuccess() {
                    Log.d(TAG, "IM connected")
                }
                override fun onConnectFailed(code: Int, error: String?) {
                    Log.e(TAG, "IM connect failed: $code – $error")
                }
                override fun onKickedOffline() {
                    Log.w(TAG, "IM kicked offline")
                    loggedIn = false
                }
                override fun onUserSigExpired() {
                    Log.w(TAG, "UserSig expired — re-login required")
                    loggedIn = false
                }
            }
        )

        initialized = true
        Log.d(TAG, "IM SDK initialised (appId=$sdkAppId)")
    }

    /** Log the current user into TIM. [userSig] is obtained from the backend. */
    fun login(userId: String, userSig: String, onResult: (Boolean, String?) -> Unit) {
        if (!initialized) {
            onResult(false, "IM SDK not initialised")
            return
        }
        V2TIMManager.getInstance().login(userId, userSig, object : V2TIMCallback {
            override fun onSuccess() {
                loggedIn = true
                Log.d(TAG, "IM login success (userId=$userId)")
                onResult(true, null)
            }
            override fun onError(code: Int, desc: String?) {
                Log.e(TAG, "IM login error: $code – $desc")
                onResult(false, "$code: $desc")
            }
        })
    }

    /** Logout from TIM. */
    fun logout(onResult: ((Boolean) -> Unit)? = null) {
        V2TIMManager.getInstance().logout(object : V2TIMCallback {
            override fun onSuccess() {
                loggedIn = false
                Log.d(TAG, "IM logout success")
                onResult?.invoke(true)
            }
            override fun onError(code: Int, desc: String?) {
                Log.e(TAG, "IM logout error: $code – $desc")
                onResult?.invoke(false)
            }
        })
    }

    fun isLoggedIn(): Boolean = loggedIn
}
