// File: app/src/main/java/org/fediquest/MainActivity.kt
package org.fediquest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * Main activity for FediQuest - Native AR First Approach
 * 
 * This activity serves as the entry point for the FediQuest native Android app.
 * It defaults to native AR mode using ARToolKit as the primary option.
 * 
 * FediQuest encourages people to:
 * - Go outside and explore their environment
 * - Help each other through community quests
 * - Do something good for the environment
 * 
 * Rewards include digital goods like:
 * - Avatar skins (cosmetic upgrades)
 * - Companion creatures (with special abilities)
 * - Level upgrades and titles
 * 
 * AR Mode Selection:
 * - ARTOOLKIT (default): Native ARToolKit integration (requires .so files)
 * - ARCORE (optional): Native ARCore integration (requires Google Play Services)
 * 
 * For the AR GPS prototype PR, the app defaults to ARTOOLKIT mode.
 */
class MainActivity : AppCompatActivity() {

    enum class ARMode {
        ARTOOLKIT,     // Primary: Native ARToolKit (requires prebuilt .so files)
        ARCORE         // Optional: Native ARCore (alternative, requires Google deps)
    }

    // Default to native ARToolKit mode
    private val currentMode = ARMode.ARTOOLKIT
    
    // Player profile (in full implementation, load from persistent storage)
    private var playerProfile: PlayerProfile? = null

    companion object {
        private const val TAG = "FediQuest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "FediQuest starting in ${currentMode} mode")
        
        // Initialize player profile
        initializePlayer()
        
        // Check which mode to run
        when (currentMode) {
            ARMode.ARTOOLKIT -> {
                setupARToolKit()
            }
            ARMode.ARCORE -> {
                setupARCore()
            }
        }
    }

    /**
     * Initialize player profile with default values
     */
    private fun initializePlayer() {
        playerProfile = PlayerProfile(
            playerId = generatePlayerId(),
            displayName = "Eco Explorer",
            fediverseHandle = null // User can configure in settings
        )
        
        Log.d(TAG, "Player initialized: ${playerProfile?.displayName}, Level ${playerProfile?.level}")
    }

    /**
     * Generate a unique player ID (placeholder - use proper UUID in production)
     */
    private fun generatePlayerId(): String {
        return "player_${System.currentTimeMillis()}"
    }

    /**
     * Setup native ARToolKit integration
     * 
     * Requires ARToolKit .so files placed in:
     * - app/src/main/jniLibs/arm64-v8a/libAR.so
     * - app/src/main/jniLibs/armeabi-v7a/libAR.so
     * 
     * See app/README_NATIVE.md for download/build instructions.
     */
    private fun setupARToolKit() {
        Log.d(TAG, "Initializing ARToolKit native AR")
        
        // TODO: Load native ARToolKit library
        // System.loadLibrary("AR")
        
        // TODO: Initialize ARToolKit session
        // - Set up camera preview
        // - Load marker patterns
        // - Configure spawn renderer
        
        // TODO: Fetch spawn data with ETag caching
        // SpawnFetcher.fetchSpawns { spawns ->
        //     renderSpawns(spawns)
        // }
        
        // TODO: Display player's avatar with equipped skin
        // val currentSkin = playerProfile?.getCurrentSkin()
        // renderAvatar(currentSkin)
        
        // TODO: Display player's companion if equipped
        // playerProfile?.getCurrentCompanion()?.let { companion ->
        //     renderCompanion(companion)
        // }
        
        // Placeholder: Content view will be set by AR surface
        // setContentView(arSurfaceView)
    }

    /**
     * Setup optional ARCore integration
     * 
     * Requires ARCore dependency in build.gradle.kts:
     * implementation("com.google.ar:core:1.41.0")
     * 
     * Note: ARCore is optional and NOT required for core flows.
     * It requires Google Play Services on most devices.
     */
    private fun setupARCore() {
        Log.d(TAG, "Initializing ARCore (optional alternative)")
        
        // TODO: Check ARCore availability
        // val availability = ArCoreApk.getInstance().checkAvailability(this)
        // when {
        //     availability.isSupported -> proceedWithARCore()
        //     else -> fallbackToARToolKit()
        // }
        
        // TODO: Create ARCore session
        // arSession = Session(this)
        
        // TODO: Set up AR scene view
        // setContentView(arSceneView)
    }

    /**
     * Complete a quest and award XP to player
     */
    fun completeQuest(questType: String, locationData: LocationData) {
        playerProfile?.let { profile ->
            val baseXP = Config.getXpRewardForQuestType(questType)
            val bonusMultiplier = profile.getXPBonusMultiplier()
            val totalXP = (baseXP * bonusMultiplier).toInt()
            
            profile.addXP(totalXP)
            profile.questStats.recordQuestCompletion(questType)
            
            Log.d(TAG, "Quest completed: $questType, XP earned: $totalXP, New level: ${profile.level}")
            
            // Share achievement to Fediverse (optional)
            shareToFediverse(Config.FediverseActivity.QUEST_COMPLETED, questType)
        }
    }

    /**
     * Share achievement to Fediverse (ActivityPub protocol)
     */
    private fun shareToFediverse(activity: Config.FediverseActivity, details: String) {
        playerProfile?.fediverseHandle?.let { handle ->
            // TODO: Post activity to user's Fediverse instance
            // Example: "@user@mastodon.social just completed a Tree Planting quest!"
            Log.d(TAG, "Fediverse share: $handle ${activity.action} - $details")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed")
        
        // Resume AR session if active
        // arSession?.resume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity paused")
        
        // Pause AR session to save resources
        // arSession?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed")
        
        // Clean up AR resources
        // arSession?.close()
        
        // Save player profile
        savePlayerProfile()
    }

    /**
     * Save player profile to persistent storage
     */
    private fun savePlayerProfile() {
        playerProfile?.let { profile ->
            // TODO: Save to SharedPreferences or database
            Log.d(TAG, "Saving player profile: ${profile.displayName}, XP: ${profile.totalXP}")
        }
    }
}

/**
 * Location data wrapper for quest completion
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)
