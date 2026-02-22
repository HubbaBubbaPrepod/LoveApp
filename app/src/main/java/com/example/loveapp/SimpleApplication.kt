package com.example.loveapp

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SimpleApplication : Application() {
    override fun onCreate() {
        Log.d("LoveApp", "SimpleApplication.onCreate() START")
        try {
            super.onCreate()
            Log.d("LoveApp", "SimpleApplication.onCreate() SUCCESS - Hilt initialized")
        } catch (e: Exception) {
            Log.e("LoveApp", "SimpleApplication.onCreate() FAILED", e)
            e.printStackTrace()
            throw e
        }
    }
}
