// File: app/src/main/java/org/fediquest/MainActivity.kt
package org.fediquest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.github.sceneview.ArSceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberModelNode
import android.view.MotionEvent

/**
 * Main activity for FediQuest - Native AR First Approach
 * 
 * This activity serves as the entry point for the FediQuest native Android app.
 * It defaults to native AR mode using SceneView (open-source Sceneform fork) as the primary AR engine.
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
 * - SCENEVIEW (default): Open-source Sceneform fork (actively maintained, Apache 2.0)
 * - ARCORE (optional): Direct ARCore integration (alternative, requires Google deps)
 * 
 * For the AR GPS prototype PR, the app defaults to SCENEVIEW mode.
 */
class MainActivity : AppCompatActivity() {

    enum class ARMode {
        SCENEVIEW,     // Primary: SceneView (open-source, actively maintained)
        ARCORE         // Optional: Direct ARCore (alternative only)
    }

    // Default to SceneView mode (primary AR engine)
    private val currentMode = ARMode.SCENEVIEW
    
    // Player profile (in full implementation, load from persistent storage)
    private var playerProfile: PlayerProfile? = null
    
    // SceneView for AR rendering
    private lateinit var arSceneView: ArSceneView

    companion object {
        private const val TAG = "FediQuest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "FediQuest starting in ${currentMode} mode")
        
        // Initialize player profile
        initializePlayer()
        
        // Initialize AR scene view
        arSceneView = ArSceneView(this)
        setContentView(arSceneView)
        
        // Check which mode to run
        when (currentMode) {
            ARMode.SCENEVIEW -> {
                setupSceneView()
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
     * Setup SceneView integration (Primary AR Engine)
     * 
     * SceneView is an open-source AR library based on Sceneform.
     * GitHub: https://github.com/SceneView/sceneview
     * License: Apache 2.0
     * 
     * No native library setup required - included as Gradle dependency.
     */
    private fun setupSceneView() {
        Log.d(TAG, "Initializing SceneView native AR")
        
        // Configure AR session
        arSceneView.arSceneView.session.apply {
            // Enable depth occlusion if supported
            config.depthMode = when {
                isDepthModeSupported(io.github.sceneview.ar.session.DepthMode.AUTOMATIC) -> 
                    io.github.sceneview.ar.session.DepthMode.AUTOMATIC
                else -> io.github.sceneview.ar.session.DepthMode.DISABLED
            }
        }
        
        // Set up tap listener for placing spawn objects
        arSceneView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                handleTapOnScreen(event.x, event.y)
                true
            } else {
                false
            }
        }
        
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
        
        Log.d(TAG, "SceneView initialized successfully")
    }

    /**
     * Handle tap on screen to place or interact with spawn objects
     */
    private fun handleTapOnScreen(x: Float, y: Float) {
        // Perform hit test to find surfaces in the real world
        val hitResult = arSceneView.arSceneView.hitTest(x, y).firstOrNull()
        
        hitResult?.let { hit ->
            Log.d(TAG, "Hit detected at: ${hit.distance}m")
            
            // TODO: Place spawn model at hit position
            // val spawnNode = ModelNode().apply {
            //     position = hit.position
            //     scale = Vector3(0.5f, 0.5f, 0.5f)
            //     loadModelGlbAsync("models/tree.glb") {
            //         Log.d(TAG, "Model loaded successfully")
            //     }
            // }
            // arSceneView.arSceneView.scene.addChild(spawnNode)
        }
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
        //     else -> fallbackToSceneView()
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
        
        // Resume AR session
        arSceneView.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity paused")
        
        // Pause AR session to save resources
        arSceneView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed")
        
        // Clean up AR resources
        arSceneView.onDestroy()
        
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
