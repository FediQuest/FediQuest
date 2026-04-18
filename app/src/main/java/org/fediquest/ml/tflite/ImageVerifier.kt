// File: app/src/main/java/org/fediquest/ml/tflite/ImageVerifier.kt
package org.fediquest.ml.tflite

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TF Lite Image Verification Stub
 * 
 * Provides image classification/verification for quest completion.
 * This is a STUB implementation - actual model loading and inference
 * requires adding a .tflite model file to assets.
 * 
 * OFFLINE-FIRST: All inference runs locally on device.
 * AI features are OPT-IN and can be disabled in settings.
 */
class ImageVerifier(private val context: Context) {
    
    companion object {
        private const val TAG = "FediQuest.ImageVerifier"
        private const val MODEL_PATH = "models/quest_verifier.tflite"
        private const val INPUT_SIZE = 224 // Expected input size for most image models
    }
    
    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    
    /**
     * Initialize the TF Lite interpreter
     * Call this before running inference
     */
    fun initialize() {
        try {
            val model = FileUtil.loadMappedFile(context, MODEL_PATH)
            val options = Interpreter.Options().apply {
                setNumThreads(2)
                // Add GPU delegate if available (optional, opt-in)
                // addDelegate(GpuDelegate())
            }
            interpreter = Interpreter(model, options)
            isModelLoaded = true
            Log.d(TAG, "TF Lite model loaded successfully")
        } catch (e: Exception) {
            Log.w(TAG, "TF Lite model not found or failed to load: ${e.message}")
            Log.w(TAG, "Image verification will use stub mode (always returns success)")
            isModelLoaded = false
        }
    }
    
    /**
     * Verify if an image matches the expected quest type
     * 
     * @param bitmap The image to verify
     * @param expectedType The expected quest type (e.g., "tree", "recycling")
     * @return VerificationResult with confidence score
     */
    fun verifyImage(bitmap: Bitmap, expectedType: String): VerificationResult {
        if (!isModelLoaded) {
            // STUB MODE: Return success when model is not available
            // This allows the app to work offline without AI features
            Log.d(TAG, "Running in stub mode (no model loaded)")
            return VerificationResult(
                isSuccess = true,
                confidence = 0.0f,
                label = expectedType,
                isStubMode = true
            )
        }
        
        return try {
            // Preprocess image
            val inputBuffer = preprocessImage(bitmap)
            
            // Prepare output buffer
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            val outputBuffer = ByteBuffer.allocateDirect(
                outputShape.fold(1) { acc, dim -> acc * dim } * 4
            ).apply {
                order(ByteOrder.nativeOrder())
            }
            
            // Run inference
            interpreter!!.run(inputBuffer, outputBuffer)
            
            // Process results
            val results = processOutput(outputBuffer, expectedType)
            Log.d(TAG, "Verification result: $results")
            results
            
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            VerificationResult(
                isSuccess = false,
                confidence = 0.0f,
                label = expectedType,
                error = e.message,
                isStubMode = false
            )
        }
    }
    
    /**
     * Preprocess image for TF Lite model
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Resize bitmap to model input size
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        // Convert to ByteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3).apply {
            order(ByteOrder.nativeOrder())
        }
        
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaledBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        for (pixel in pixels) {
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            byteBuffer.putFloat((pixel and 0xFF) / 255.0f)          // B
        }
        
        byteBuffer.rewind()
        scaledBitmap.recycle()
        return byteBuffer
    }
    
    /**
     * Process model output into verification result
     */
    private fun processOutput(outputBuffer: ByteBuffer, expectedType: String): VerificationResult {
        // Simple softmax to get confidence scores
        outputBuffer.rewind()
        val scores = mutableListOf<Float>()
        while (outputBuffer.hasRemaining()) {
            scores.add(outputBuffer.float)
        }
        
        // Find highest confidence label
        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
        val maxConfidence = scores[maxIndex]
        
        // Map index to label (simplified - would need proper label mapping)
        val label = expectedType
        
        // Determine if verification passed (threshold: 0.7)
        val isSuccess = maxConfidence >= 0.7f
        
        return VerificationResult(
            isSuccess = isSuccess,
            confidence = maxConfidence,
            label = label,
            allScores = scores.toFloatArray(),
            isStubMode = false
        )
    }
    
    /**
     * Check if AI verification is available
     */
    fun isAiAvailable(): Boolean = isModelLoaded
    
    /**
     * Close resources
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}

/**
 * Verification result data class
 */
data class VerificationResult(
    val isSuccess: Boolean,
    val confidence: Float,
    val label: String,
    val allScores: FloatArray? = null,
    val error: String? = null,
    val isStubMode: Boolean = false
) {
    override fun toString(): String {
        return if (isStubMode) {
            "VerificationResult(success=$isSuccess, mode=STUB)"
        } else {
            "VerificationResult(success=$isSuccess, confidence=$confidence, label=$label)"
        }
    }
}
