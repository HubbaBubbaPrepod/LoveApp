package com.example.loveapp

import android.app.Application
import android.util.Log

/**
 * Legacy Application class. The app currently uses [SimpleApplication] (see AndroidManifest).
 * This class is kept for reference; do not add @HiltAndroidApp here—Hilt allows only one app root.
 */
class LoveAppApplication : Application() {
    override fun onCreate() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LoveApp", "Uncaught exception in thread ${thread.name}", throwable)
        }
        
        try {
            super.onCreate()
            Log.d("LoveApp", "Application initialized successfully")
        } catch (e: Exception) {
            Log.e("LoveApp", "Error during Application initialization", e)
            e.printStackTrace()
            throw e
        }
    }
}
