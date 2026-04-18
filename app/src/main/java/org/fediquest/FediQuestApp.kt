// File: app/src/main/java/org/fediquest/FediQuestApp.kt
package org.fediquest

import android.app.Application
import android.util.Log
import org.fediquest.BuildConfig

/**
 * FediQuest Application Class
 *
 * Main application entry point for FediQuest.
 * Handles global app initialization, configuration, and lifecycle management.
 */
class FediQuestApp : Application() {

    companion object {
        private const val TAG = "FediQuest.App"
        
        @Volatile
        private var instance: FediQuestApp? = null
        
        /**
         * Get the application instance (singleton access)
         */
        fun getInstance(): FediQuestApp = instance ?: synchronized(this) {
            instance ?: throw IllegalStateException("Application not initialized")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Log.d(TAG, "FediQuest Application starting...")
        Log.d(TAG, "Package: ${packageName}")
        Log.d(TAG, "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        
        // Initialize app-wide components
        initializeApp()
    }

    /**
     * Initialize application-wide components
     */
    private fun initializeApp() {
        Log.d(TAG, "Initializing application components...")
        
        // TODO: Initialize database
        // AppDatabase.init(this)
        
        // TODO: Initialize preferences
        // AppPreferences.init(this)
        
        // TODO: Initialize network client
        // NetworkClient.init(this)
        
        // TODO: Initialize location service
        // LocationService.init(this)
        
        Log.d(TAG, "Application components initialized successfully")
    }

    /**
     * Get server URL from config
     */
    fun getServerUrl(): String = Config.SERVER_URL

    /**
     * Check if debug logging is enabled
     */
    fun isDebugLoggingEnabled(): Boolean = Config.DEBUG_LOGGING

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "FediQuest Application terminating...")
        instance = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "System is running low on memory")
        // Clean up caches and release resources
        System.gc()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "Trimming memory at level: $level")
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Release resources that can be recreated
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                // UI is hidden, release UI-related resources
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                // App is in background, release as many resources as possible
            }
        }
    }
}
