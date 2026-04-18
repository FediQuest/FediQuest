// File: app/src/main/java/org/fediquest/MainActivity.kt
package org.fediquest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import android.view.MotionEvent
import android.widget.FrameLayout
import android.graphics.Bitmap
import android.location.Location
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
    
    // CameraX components for quest proof capture
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    
    // Current active quest ID (for proof submission)
    private var currentQuestId: String? = null

    companion object {
        private const val TAG = "FediQuest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "FediQuest starting in ${currentMode} mode")

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

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

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
     * Submit quest proof with image verification
     * Called when user taps "Submit Proof" button
     */
    private fun submitQuestProof(questId: String, image: Bitmap, location: Location) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Get database instances
            val database = org.fediquest.data.database.AppDatabase.getInstance(this@MainActivity)
            val questRepository = org.fediquest.data.repository.QuestRepository(database.questDao())
            val playerRepository = org.fediquest.data.repository.PlayerRepository(database.playerDao())
            
            // Get quest data from database
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
                    // Verification successful - persist to Room DB and award XP
                    lifecycleScope.launch(Dispatchers.IO) {
                        questRepository.completeQuest(questId) // Persists completion
                        playerRepository.addXP(amount = result.xpAward) // Updates level/companion
                        
                        withContext(Dispatchers.Main) {
                            playerProfile?.addXP(result.xpAward)
                            showSuccessDialog(result.xpAward)
                            
                            // TODO: Queue for Fediverse sync if opted-in
                            Log.d(TAG, "Quest proof verified successfully. XP awarded: ${result.xpAward}")
                        }
                    }
                } else {
                    // Verification failed
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
        }
    }

    /**
     * Capture image from camera and submit as quest proof
     */
    private fun captureAndSubmitQuestProof(questId: String, userLocation: Location) {
        currentQuestId = questId
        
        val photoFile = File(externalCacheDir, SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg")
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture?.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d(TAG, "Image captured: ${output.savedUri ?: photoFile.absolutePath}")
                
                // Load bitmap from file
                val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                
                // Submit for verification
                if (bitmap != null) {
                    submitQuestProof(questId, bitmap, userLocation)
                } else {
                    Log.e(TAG, "Failed to load captured image")
                    showRetryDialog("Failed to process captured image. Try again?")
                }
                
                // Clean up temp file
                photoFile.delete()
            }

            override fun onError(exception: ImageCaptureException, imageProxy: ImageProxy?) {
                Log.e(TAG, "Error capturing image: ${exception.message}", exception)
                showRetryDialog("Failed to capture image. Try again?")
            }
        })
    }

    /**
     * Show success dialog after quest completion
     */
    private fun showSuccessDialog(xpAward: Int) {
        Log.d(TAG, "Quest completed successfully! XP awarded: $xpAward")
        // TODO: Show UI dialog/success animation
    }

    /**
     * Show retry dialog after verification failure
     */
    private fun showRetryDialog(message: String) {
        Log.w(TAG, "Showing retry dialog: $message")
        // TODO: Show UI dialog with retry option
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

        // Cleanup camera executor
        cameraExecutor.shutdown()
        
        // Cleanup QuestVerifier
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
