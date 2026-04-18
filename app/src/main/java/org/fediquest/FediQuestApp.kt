// File: app/src/main/java/org/fediquest/FediQuestApp.kt
package org.fediquest

import android.app.Application
import android.util.Log
import org.fediquest.BuildConfig
import org.fediquest.data.database.AppDatabase
import org.fediquest.ml.tflite.ImageVerifier
import org.fediquest.fediverse.client.ActivityPubClientFactory
import org.fediquest.companion.evolution.CompanionEvolutionManagerFactory

/**
 * FediQuest Application Class
 *
 * Main application entry point for FediQuest.
 * Handles global app initialization, configuration, and lifecycle management.
 * 
 * Architecture Principles:
 * - Offline-first: All core features work without internet
 * - Opt-in AI/Fediverse: ML and social features are disabled by default
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
        
        /**
         * Get database instance
         */
        fun getDatabase(): AppDatabase {
            return AppDatabase.getInstance(getInstance())
        }
        
        /**
         * Get image verifier (AI feature, opt-in)
         */
        fun getImageVerifier(): ImageVerifier {
            return ImageVerifier(getInstance())
        }
        
        /**
         * Get ActivityPub client (Fediverse feature, opt-in)
         */
        fun getActivityPubClient() = ActivityPubClientFactory.getInstance(getInstance())
        
        /**
         * Get companion evolution manager
         */
        fun getCompanionManager() = CompanionEvolutionManagerFactory.getInstance(getInstance())
    }

    // Lazy-initialized components
    private val _database by lazy { AppDatabase.getInstance(this) }
    private val _imageVerifier by lazy { ImageVerifier(this) }
    
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
        
        // Initialize Room Database (offline-first storage)
        try {
            _database
            Log.d(TAG, "✓ Room database initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize database: ${e.message}")
        }
        
        // Initialize TF Lite Image Verifier (opt-in AI feature)
        // Note: This runs in stub mode if no model is present
        try {
            _imageVerifier.initialize()
            val aiStatus = if (_imageVerifier.isAiAvailable()) {
                "✓ AI verification enabled"
            } else {
                "○ AI verification disabled (stub mode)"
            }
            Log.d(TAG, aiStatus)
        } catch (e: Exception) {
            Log.w(TAG, "Image verifier initialization failed: ${e.message}")
        }
        
        // Note: ActivityPub client is lazy-initialized and opt-in
        // It will only be configured if user explicitly enables Fediverse features
        
        // Note: Companion manager is lazy-initialized
        // Companions work offline-first with local storage
        
        Log.d(TAG, "Application components initialized successfully")
        Log.d(TAG, "Offline-first mode: ENABLED")
        Log.d(TAG, "AI features: OPT-IN (disabled by default)")
        Log.d(TAG, "Fediverse features: OPT-IN (disabled by default)")
    }

    /**
     * Get server URL from config
     */
    fun getServerUrl(): String = Config.SERVER_URL

    /**
     * Check if debug logging is enabled
     */
    fun isDebugLoggingEnabled(): Boolean = Config.DEBUG_LOGGING
    
    /**
     * Get database instance
     */
    fun getDatabase(): AppDatabase = _database
    
    /**
     * Get image verifier
     */
    fun getImageVerifier(): ImageVerifier = _imageVerifier

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "FediQuest Application terminating...")
        
        // Clean up resources
        _imageVerifier.close()
        AppDatabase.closeInstance()
        
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
