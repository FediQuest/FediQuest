// File: app/src/main/java/org/fediquest/MainActivity.kt
package org.fediquest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.lifecycleScope
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import android.widget.FrameLayout
import android.graphics.Bitmap
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fediquest.data.database.AppDatabase
import org.fediquest.data.repository.QuestRepository
import org.fediquest.data.repository.PlayerRepository
import org.fediquest.companion.evolution.CompanionEvolutionManagerFactory

/**
 * Main activity for FediQuest - Native AR First Approach
 *
 * This activity serves as the entry point for the FediQuest native Android app.
 * It defaults to native AR mode using SceneView (open-source Sceneform fork) as the primary AR engine.
 * 
 * Architecture Principles:
 * - Offline-first: All core features work without internet
 * - Opt-in AI/Fediverse: ML and social features are disabled by default
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
    
    // Current active quest ID (for proof submission)
    private var currentQuestId: String? = null
    
    // Database and repositories
    private lateinit var database: AppDatabase
    private lateinit var questRepository: QuestRepository
    private lateinit var playerRepository: PlayerRepository
    private lateinit var companionManager: org.fediquest.companion.evolution.CompanionEvolutionManager

    companion object {
        private const val TAG = "FediQuest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "FediQuest starting in ${currentMode} mode")

        // Initialize database and repositories
        database = AppDatabase.getInstance(this)
        questRepository = QuestRepository(database.questDao())
        playerRepository = PlayerRepository(database.playerDao(), database.playerXpDao())
        
        // Initialize companion manager
        companionManager = CompanionEvolutionManagerFactory.getInstance(this)

        // Initialize player profile
        initializePlayer()
        
        // Initialize QuestVerifier
        QuestVerifier.initialize(this)

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
     * Uses setOnTapListener for AR object placement as per SceneView 4.0.1 API
     */
    private fun setupSceneView() {
        Log.d(TAG, "Initializing SceneView native AR")

        try {
            // Set up tap listener for placing spawn objects using SceneView 4.0.1 API
            arSceneView.setOnTapListener { hitResult ->
                // HitResult contains position and plane information for AR placement
                val position = Position(
                    hitResult.position.x,
                    hitResult.position.y,
                    hitResult.position.z
                )
                
                // Create and place a model node at the hit position
                val node = ModelNode(
                    model = io.github.sceneview.model.ModelBuilder.create {
                        uri = android.net.Uri.parse("file:///android_asset/spawn.glb")
                        scale = Scale(0.5f, 0.5f, 0.5f)
                    }
                )
                node.position = position
                arSceneView.scene.addChild(node)
                
                Log.d(TAG, "AR object placed at: ${position}")
                true
            }

            Log.d(TAG, "SceneView initialized successfully with setOnTapListener")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SceneView: ${e.message}", e)
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

            // Update database
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    playerRepository.addXP(amount = totalXP)
                    
                    // Notify companion manager for evolution tracking
                    val questEntity = org.fediquest.data.entity.QuestEntity(
                        id = "temp_quest_${System.currentTimeMillis()}",
                        title = questType,
                        description = "",
                        type = org.fediquest.data.entity.QuestType.valueOf(questType.uppercase()),
                        locationLat = locationData.latitude,
                        locationLng = locationData.longitude,
                        radiusMeters = 50f,
                        xpReward = totalXP,
                        imageUrl = null
                    )
                    companionManager.onQuestCompleted(questEntity, totalXP)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating database after quest completion: ${e.message}")
                }
            }

            shareToFediverse(Config.FediverseActivity.QUEST_COMPLETED, questType)
        }
    }

    /**
     * Submit quest proof with image verification
     */
    private fun submitQuestProof(questId: String, image: Bitmap, location: Location) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val database = AppDatabase.getInstance(this@MainActivity)
                val questRepository = QuestRepository(database.questDao())
                val playerRepository = PlayerRepository(database.playerDao(), database.playerXpDao())
                
                val quest = questRepository.getQuestById(questId)
                if (quest == null) {
                    withContext(Dispatchers.Main) {
                        showRetryDialog("Quest not found. Please try again.")
                    }
                    return@launch
                }
                
                val questLocation = android.location.Location("").apply {
                    latitude = quest.locationLat
                    longitude = quest.locationLng
                }
                val questTimestamp = quest.createdAt
                val xpReward = quest.xpReward
                
                val result = QuestVerifier.verify(
                    questId = questId,
                    image = image,
                    userLocation = location,
                    questLocation = questLocation,
                    questTimestamp = questTimestamp,
                    xpReward = xpReward
                )
                
                withContext(Dispatchers.Main) {
                    if (result.confidence >= 0.85f && result.gpsValid && result.timestampValid) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                questRepository.completeQuest(questId)
                                playerRepository.addXP(amount = result.xpAward)
                                
                                withContext(Dispatchers.Main) {
                                    playerProfile?.addXP(result.xpAward)
                                    showSuccessDialog(result.xpAward)
                                    Log.d(TAG, "Quest proof verified successfully. XP awarded: ${result.xpAward}")
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    showRetryDialog("Database error: ${e.message}")
                                }
                                Log.e(TAG, "Database error during quest completion", e)
                            }
                        }
                    } else {
                        val reason = when {
                            !result.gpsValid -> "GPS location mismatch"
                            !result.timestampValid -> "Proof submitted outside time window"
                            result.confidence < 0.85f -> "Image verification failed (confidence: ${result.confidence})"
                            else -> "Verification failed"
                        }
                        showRetryDialog("Proof not verified. $reason. Try again?")
                        Log.w(TAG, "Quest proof verification failed: $reason")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showRetryDialog("Error: ${e.message}")
                }
                Log.e(TAG, "Unexpected error during quest proof submission", e)
            }
        }
    }

    /**
     * Capture image from camera and submit as quest proof
     */
    private fun captureAndSubmitQuestProof(questId: String, userLocation: Location) {
        currentQuestId = questId
        
        val photoFile = java.io.File(externalCacheDir, "${System.currentTimeMillis()}.jpg")
        
        // Placeholder for CameraX implementation
        Log.d(TAG, "Would capture image for quest: $questId")
    }

    /**
     * Show success dialog after quest completion
     */
    private fun showSuccessDialog(xpAward: Int) {
        Log.d(TAG, "Quest completed successfully! XP awarded: $xpAward")
    }

    /**
     * Show retry dialog after verification failure
     */
    private fun showRetryDialog(message: String) {
        Log.w(TAG, "Showing retry dialog: $message")
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
        arSceneView.onResume(owner = this)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity paused")
        arSceneView.onPause(owner = this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed")
        arSceneView.onDestroy(owner = this)
        QuestVerifier.close()
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
