package com.tunelapp

import android.app.Application
import android.util.Log

/**
 * Application class for TunelApp
 */
class TunelApplication : Application() {
    
    companion object {
        private const val TAG = "TunelApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created")
        
        // Initialize any app-wide components here
    }
}

