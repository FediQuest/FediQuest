// File: app/src/main/java/org/fediquest/MainActivity.kt
package org.fediquest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Main activity for FediQuest - Native AR First Approach
 *
 * This activity serves as the entry point for the FediQuest native Android app.
 * It defaults to native AR mode using SceneView (open-source Sceneform fork) as the primary AR engine.
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

    // AR scene view for AR rendering
    private lateinit var arSceneView: ArSceneView
    
    // Root layout container
    private lateinit var rootLayout: FrameLayout

    companion object {
        private const val TAG = "FediQuest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "FediQuest starting in ${currentMode} mode")

        // Initialize player profile
        initializePlayer()

        // Create root layout container
        rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        setContentView(rootLayout)

        // Initialize AR scene view
        arSceneView = ArSceneView(this)
        rootLayout.addView(arSceneView)

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
            fediverseHandle = null
        )

        Log.d(TAG, "Player initialized: ${playerProfile?.displayName}, Level ${playerProfile?.level}")
    }

    /**
     * Generate a unique player ID
     */
    private fun generatePlayerId(): String {
        return "player_${System.currentTimeMillis()}"
    }

    /**
     * Setup SceneView integration (Primary AR Engine)
     */
    private fun setupSceneView() {
        Log.d(TAG, "Initializing SceneView native AR")

        try {
            // Set up tap listener for placing spawn objects
            arSceneView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    handleTapOnScreen(event.x, event.y)
                    true
                } else {
                    false
                }
            }

            Log.d(TAG, "SceneView initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SceneView: ${e.message}", e)
        }
    }

    /**
     * Handle tap on screen to place or interact with spawn objects
     */
    private fun handleTapOnScreen(x: Float, y: Float) {
        try {
            // Perform hit test to find surfaces in the real world
            val hitResult = arSceneView.hitTest(x, y).firstOrNull()

            hitResult?.let { hit ->
                Log.d(TAG, "Hit detected at: ${hit.distance}m")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling tap: ${e.message}", e)
        }
    }

    /**
     * Setup optional ARCore integration
     */
    private fun setupARCore() {
        Log.d(TAG, "Initializing ARCore (optional alternative)")
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

            shareToFediverse(Config.FediverseActivity.QUEST_COMPLETED, questType)
        }
    }

    /**
     * Share achievement to Fediverse (ActivityPub protocol)
     */
    private fun shareToFediverse(activity: Config.FediverseActivity, details: String) {
        playerProfile?.fediverseHandle?.let { handle ->
            Log.d(TAG, "Fediverse share: $handle ${activity.action} - $details")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed")

        try {
            arSceneView.onResume()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming AR session: ${e.message}", e)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity paused")

        try {
            arSceneView.onPause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing AR session: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed")

        try {
            arSceneView.onDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying AR session: ${e.message}", e)
        }

        savePlayerProfile()
    }

    /**
     * Save player profile to persistent storage
     */
    private fun savePlayerProfile() {
        playerProfile?.let { profile ->
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
