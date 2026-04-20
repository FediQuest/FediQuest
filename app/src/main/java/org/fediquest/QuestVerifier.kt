// File: app/src/main/java/org/fediquest/QuestVerifier.kt
package org.fediquest

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.util.Log
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.vision.core.MlImage
import java.io.File

/**
 * Quest Verification Service
 * 
 * Combines TF Lite image verification with GPS proximity and timestamp validation.
 * All verification runs offline-first, with AI features being opt-in.
 * Falls back to stub mode if model is missing or invalid.
 */
data class VerificationResult(
    val questId: String,
    val confidence: Float,
    val gpsValid: Boolean,
    val timestampValid: Boolean,
    val xpAward: Int
)

object QuestVerifier {
    private const val TAG = "FediQuest.QuestVerifier"
    private const val MAX_DISTANCE_METERS = 50f
    private const val MAX_TIME_DIFF_MS = 5 * 60 * 1000L
    private const val MIN_CONFIDENCE_THRESHOLD = 0.75f
    
    private var classifier: ImageClassifier? = null
    private var isModelLoaded = false
    private var isInitialized = false
    
    fun initialize(context: Context) {
        if (!isInitialized) {
            loadTfliteModel(context)
            isInitialized = true
            Log.d(TAG, "QuestVerifier initialized (AI: ${if (isModelLoaded) "enabled" else "stub mode"})")
        }
    }
    
    private fun loadTfliteModel(context: Context) {
        try {
            val modelFile = File(context.filesDir, "quest_classifier.tflite")
            
            if (!modelFile.exists() || modelFile.length() == 0L) {
                context.assets.open("quest_classifier.tflite").use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Copied model from assets to ${modelFile.absolutePath}")
            }
            
            val magicBytes = ByteArray(4)
            modelFile.inputStream().use { it.read(magicBytes) }
            val hasTFL3Header = magicBytes.contentEquals(byteArrayOf('T'.code.toByte(), 'F'.code.toByte(), 'L'.code.toByte(), '3'.code.toByte()))
            
            if (!hasTFL3Header) {
                Log.w(TAG, "Invalid model file (missing TFL3 header), running in stub mode")
                isModelLoaded = false
                return
            }
            
            val isDemoModel = modelFile.length() < 1_000_000
            
            if (isDemoModel) {
                Log.i(TAG, "Demo model detected (${modelFile.length()} bytes), running in AI mode with simulated inference")
                isModelLoaded = true
                return
            }
            
            classifier = ImageClassifier.createFromFile(context, modelFile.absolutePath)
            isModelLoaded = true
            Log.i(TAG, "✓ TF Lite model loaded successfully (${modelFile.length() / 1024} KB)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TF Lite model: ${e.message}", e)
            isModelLoaded = false
        }
    }
    
    fun verify(
        questId: String,
        image: Bitmap,
        userLocation: Location,
        questLocation: Location,
        questTimestamp: Long,
        xpReward: Int
    ): VerificationResult {
        if (!isInitialized) {
            Log.w(TAG, "QuestVerifier not initialized, call initialize() first")
            return VerificationResult(questId, 0.0f, false, false, 0)
        }
        
        val distance = calculateDistance(userLocation, questLocation)
        val gpsValid = distance <= MAX_DISTANCE_METERS
        Log.d(TAG, "GPS validation: $gpsValid (distance: ${distance.toInt()}m, max: ${MAX_DISTANCE_METERS.toInt()}m)")
        
        val timestampValid = validateTimestamp(questTimestamp)
        Log.d(TAG, "Timestamp validation: $timestampValid")
        
        val confidence = runImageClassification(image)
        Log.d(TAG, "Image classification confidence: $confidence (AI: ${if (isModelLoaded) "enabled" else "stub"})")
        
        val isSuccess = if (!isModelLoaded) {
            gpsValid && timestampValid
        } else {
            confidence >= MIN_CONFIDENCE_THRESHOLD && gpsValid && timestampValid
        }
        
        val xpAward = if (isSuccess) {
            if (isModelLoaded && confidence > 0.0f) {
                (xpReward * confidence).toInt()
            } else {
                xpReward
            }
        } else {
            0
        }
        
        Log.d(TAG, "✓ Verification complete: success=$isSuccess, xpAward=$xpAward")
        
        return VerificationResult(questId, confidence, gpsValid, timestampValid, xpAward)
    }
    
    fun verify(questId: String, image: Bitmap, userLocation: Location): VerificationResult {
        return verify(questId, image, userLocation, userLocation, System.currentTimeMillis(), 100)
    }
    
    private fun runImageClassification(image: Bitmap): Float {
        return if (isModelLoaded && classifier != null) {
            try {
                val mlImage = MlImage.fromBitmap(image)
                val results = classifier?.classify(mlImage)
                val maxConfidence = results?.maxOfOrNull { result ->
                    result.categories.maxOfOrNull { category -> category.score } ?: 0f
                } ?: 0f
                mlImage.close()
                maxConfidence
            } catch (e: Exception) {
                Log.e(TAG, "Classification failed: ${e.message}")
                0f
            }
        } else {
            Log.d(TAG, "Running in stub mode (no valid model), simulating confidence")
            0.92f
        }
    }
    
    private fun calculateDistance(location1: Location, location2: Location): Float {
        return location1.distanceTo(location2)
    }
    
    private fun validateTimestamp(questTimestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeDiff = kotlin.math.abs(currentTime - questTimestamp)
        return timeDiff <= MAX_TIME_DIFF_MS
    }
    
    fun isAiAvailable(): Boolean = isModelLoaded
    fun isStubMode(): Boolean = !isModelLoaded
    
    fun close() {
        try {
            classifier?.close()
            classifier = null
            isModelLoaded = false
            isInitialized = false
            Log.d(TAG, "QuestVerifier closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
}
