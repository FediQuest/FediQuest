// File: app/src/main/java/org/fediquest/QuestVerifier.kt
package org.fediquest

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.util.Log
import org.fediquest.ml.tflite.ImageVerifier
import org.fediquest.ml.tflite.VerificationResult as TfLiteVerificationResult

/**
 * Quest Verification Service
 * 
 * Combines TF Lite image verification with GPS proximity and timestamp validation.
 * All verification runs offline-first, with AI features being opt-in.
 * 
 * TODO: Load TFLite model from assets (e.g., quest_classifier.tflite)
 * TODO: Implement classify(image: Bitmap): VerificationResult
 * TODO: Validate GPS proximity + timestamp within 5min window
 * TODO: Return confidence score + XP award decision
 */
data class VerificationResult(
    val questId: String,
    val confidence: Float,      // 0.0 - 1.0
    val gpsValid: Boolean,
    val timestampValid: Boolean,
    val xpAward: Int            // 0 if failed
)

object QuestVerifier {
    private const val TAG = "FediQuest.QuestVerifier"
    
    // Maximum allowed distance from quest location (in meters)
    private const val MAX_DISTANCE_METERS = 50f
    
    // Maximum allowed time difference (in milliseconds) - 5 minutes
    private const val MAX_TIME_DIFF_MS = 5 * 60 * 1000L
    
    // Minimum confidence threshold for successful verification
    private const val MIN_CONFIDENCE_THRESHOLD = 0.85f
    
    private var imageVerifier: ImageVerifier? = null
    private var isInitialized = false
    
    /**
     * Initialize the QuestVerifier with context
     * Call this once during app startup
     */
    fun initialize(context: Context) {
        if (!isInitialized) {
            imageVerifier = ImageVerifier(context)
            imageVerifier?.initialize()
            isInitialized = true
            Log.d(TAG, "QuestVerifier initialized")
        }
    }
    
    /**
     * Verify a quest completion proof
     * 
     * @param questId The ID of the quest being verified
     * @param image The proof image captured by the user
     * @param userLocation The user's current GPS location
     * @param questLocation The expected quest location
     * @param questTimestamp The timestamp when the quest was activated
     * @param xpReward The base XP reward for this quest
     * @return VerificationResult with all validation checks
     */
    fun verify(
        questId: String,
        image: Bitmap,
        userLocation: Location,
        questLocation: Location,
        questTimestamp: Long,
        xpReward: Int
    ): VerificationResult {
        if (!isInitialized) {
            Log.w(TAG, "QuestVerifier not initialized, running in stub mode")
            return VerificationResult(
                questId = questId,
                confidence = 0.0f,
                gpsValid = false,
                timestampValid = false,
                xpAward = 0
            )
        }
        
        // Step 1: Validate GPS proximity
        val gpsValid = validateGpsProximity(userLocation, questLocation)
        Log.d(TAG, "GPS validation: $gpsValid (distance: ${calculateDistance(userLocation, questLocation)}m)")
        
        // Step 2: Validate timestamp (within 5 min window)
        val timestampValid = validateTimestamp(questTimestamp)
        Log.d(TAG, "Timestamp validation: $timestampValid")
        
        // Step 3: Run image verification (TF Lite or stub mode)
        val imageResult = imageVerifier?.verifyImage(image, questId) ?: TfLiteVerificationResult(
            isSuccess = false,
            confidence = 0.0f,
            label = questId,
            isStubMode = true
        )
        Log.d(TAG, "Image verification: ${imageResult.isSuccess} (confidence: ${imageResult.confidence}, stub: ${imageResult.isStubMode})")
        
        // Step 4: Determine final result
        val confidence = imageResult.confidence
        
        // In stub mode (no AI model), we rely on GPS + timestamp only
        val isSuccess = if (imageResult.isStubMode) {
            gpsValid && timestampValid
        } else {
            confidence >= MIN_CONFIDENCE_THRESHOLD && gpsValid && timestampValid
        }
        
        // Calculate XP award
        val xpAward = if (isSuccess) {
            // Apply confidence multiplier if AI is available
            if (!imageResult.isStubMode && confidence > 0.0f) {
                (xpReward * confidence).toInt()
            } else {
                xpReward
            }
        } else {
            0
        }
        
        Log.d(TAG, "Verification complete: success=$isSuccess, xpAward=$xpAward")
        
        return VerificationResult(
            questId = questId,
            confidence = confidence,
            gpsValid = gpsValid,
            timestampValid = timestampValid,
            xpAward = xpAward
        )
    }
    
    /**
     * Simplified verify method for quick usage
     * Uses default quest location and timestamp validation
     */
    fun verify(
        questId: String,
        image: Bitmap,
        userLocation: Location
    ): VerificationResult {
        // For simplified usage, assume quest location matches user location
        // and timestamp is valid (this should be replaced with actual quest data)
        return verify(
            questId = questId,
            image = image,
            userLocation = userLocation,
            questLocation = userLocation, // Placeholder
            questTimestamp = System.currentTimeMillis(), // Placeholder
            xpReward = 100 // Default XP reward
        )
    }
    
    /**
     * Validate GPS proximity between user and quest location
     */
    private fun validateGpsProximity(userLocation: Location, questLocation: Location): Boolean {
        val distance = calculateDistance(userLocation, questLocation)
        return distance <= MAX_DISTANCE_METERS
    }
    
    /**
     * Calculate distance between two locations in meters
     */
    private fun calculateDistance(location1: Location, location2: Location): Float {
        return location1.distanceTo(location2)
    }
    
    /**
     * Validate that the timestamp is within the acceptable window
     */
    private fun validateTimestamp(questTimestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeDiff = kotlin.math.abs(currentTime - questTimestamp)
        return timeDiff <= MAX_TIME_DIFF_MS
    }
    
    /**
     * Check if AI verification is available
     */
    fun isAiAvailable(): Boolean {
        return imageVerifier?.isAiAvailable() ?: false
    }
    
    /**
     * Cleanup resources
     */
    fun close() {
        imageVerifier?.close()
        imageVerifier = null
        isInitialized = false
        Log.d(TAG, "QuestVerifier closed")
    }
}
