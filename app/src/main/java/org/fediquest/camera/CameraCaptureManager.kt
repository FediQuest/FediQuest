// File: app/src/main/java/org/fediquest/camera/CameraCaptureManager.kt
package org.fediquest.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Camera Capture Manager
 *
 * Handles camera capture for quest proof verification.
 * Integrates with local TF Lite classification and confidence threshold checking.
 * Works offline-first - no network required for capture or classification.
 */
class CameraCaptureManager(private val context: Context) {

    companion object {
        private const val TAG = "FediQuest.CameraCapture"
        
        // Confidence threshold for accepting classification results
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.75f
        const val HIGH_CONFIDENCE_THRESHOLD = 0.90f
        const val LOW_CONFIDENCE_THRESHOLD = 0.50f
        
        // Image processing constants
        const val DEFAULT_IMAGE_WIDTH = 640
        const val DEFAULT_IMAGE_HEIGHT = 480
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    
    private val cameraExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    private var isInitialized = false
    private var currentConfidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD

    /**
     * Initialize camera components
     */
    fun initialize(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        onCaptureSuccess: (Bitmap) -> Unit,
        onCaptureError: (Exception) -> Unit
    ) {
        if (isInitialized) {
            Log.d(TAG, "Camera already initialized")
            return
        }

        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            
            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()
                    
                    // Set up preview
                    preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Set up image capture
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetResolution(android.util.Size(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT))
                        .build()

                    // Set up image analysis for real-time classification
                    imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(android.util.Size(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    bindToLifecycle(lifecycleOwner, onCaptureSuccess, onCaptureError)
                    
                    isInitialized = true
                    Log.d(TAG, "Camera initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize camera: ${e.message}", e)
                    onCaptureError(e)
                }
            }, ContextCompat.getMainExecutor(context))
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get camera provider: ${e.message}", e)
            onCaptureError(e)
        }
    }

    /**
     * Bind camera to lifecycle
     */
    private fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        onCaptureSuccess: (Bitmap) -> Unit,
        onCaptureError: (Exception) -> Unit
    ) {
        try {
            cameraProvider?.unbindAll()
            
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
                imageAnalyzer
            )

            // Set up capture callback
            setupCaptureCallbacks(onCaptureSuccess, onCaptureError)
            
            Log.d(TAG, "Camera bound to lifecycle")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera to lifecycle: ${e.message}", e)
            onCaptureError(e)
        }
    }

    /**
     * Set up capture callbacks
     */
    private fun setupCaptureCallbacks(
        onCaptureSuccess: (Bitmap) -> Unit,
        onCaptureError: (Exception) -> Unit
    ) {
        // Image analysis callback for real-time classification
        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            try {
                val bitmap = imageProxy.toBitmap()
                
                // Perform classification here (caller will handle ML)
                // For now, just pass the bitmap through
                // In full implementation, call QuestVerifier.runImageClassification(bitmap)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing image: ${e.message}", e)
            } finally {
                imageProxy.close()
            }
        }
    }

    /**
     * Capture a single image for quest proof
     */
    fun captureImage(onSuccess: (Bitmap) -> Unit, onError: (Exception) -> Unit) {
        if (!isInitialized) {
            onError(IllegalStateException("Camera not initialized"))
            return
        }

        val photoFile = java.io.File(context.cacheDir, "quest_proof_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    try {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                            ?: throw IllegalStateException("Failed to decode captured image")
                        
                        // Rotate bitmap if needed
                        val rotatedBitmap = rotateBitmap(bitmap, 0) // TODO: Get actual rotation from metadata
                        
                        onSuccess(rotatedBitmap)
                        
                        // Clean up temp file
                        photoFile.delete()
                        
                        Log.d(TAG, "Image captured successfully: ${photoFile.absolutePath}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing captured image: ${e.message}", e)
                        onError(e)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Error capturing image: ${exception.message}", exception)
                    onError(exception)
                }
            }
        )
    }

    /**
     * Capture and classify image with confidence threshold check
     * Returns result with classification confidence and whether it passed threshold
     */
    fun captureAndClassify(
        classifier: (Bitmap) -> ClassificationResult,
        onResult: (CaptureWithClassificationResult) -> Unit,
        onError: (Exception) -> Unit
    ) {
        captureImage(
            onSuccess = { bitmap ->
                try {
                    // Run classification
                    val classificationResult = classifier(bitmap)
                    
                    // Check confidence threshold
                    val passedThreshold = classificationResult.confidence >= currentConfidenceThreshold
                    
                    val result = CaptureWithClassificationResult(
                        bitmap = bitmap,
                        confidence = classificationResult.confidence,
                        label = classificationResult.label,
                        passedThreshold = passedThreshold,
                        threshold = currentConfidenceThreshold
                    )
                    
                    Log.d(TAG, "Classification result: confidence=${classificationResult.confidence}, " +
                             "passed=$passedThreshold, threshold=$currentConfidenceThreshold")
                    
                    onResult(result)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error during classification: ${e.message}", e)
                    onError(e)
                }
            },
            onError = onError
        )
    }

    /**
     * Set confidence threshold for classification
     */
    fun setConfidenceThreshold(threshold: Float) {
        require(threshold in 0.0f..1.0f) { "Confidence threshold must be between 0.0 and 1.0" }
        currentConfidenceThreshold = threshold
        Log.d(TAG, "Confidence threshold set to: $threshold")
    }

    /**
     * Get current confidence threshold
     */
    fun getConfidenceThreshold(): Float = currentConfidenceThreshold

    /**
     * Start continuous image analysis for real-time classification
     */
    fun startContinuousAnalysis(
        classifier: (Bitmap) -> ClassificationResult,
        onResult: (ClassificationResult) -> Unit
    ) {
        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            try {
                val bitmap = imageProxy.toBitmap()
                val result = classifier(bitmap)
                
                // Only emit results above minimum threshold to reduce noise
                if (result.confidence >= LOW_CONFIDENCE_THRESHOLD) {
                    onResult(result)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in continuous analysis: ${e.message}", e)
            } finally {
                imageProxy.close()
            }
        }
        
        Log.d(TAG, "Started continuous image analysis")
    }

    /**
     * Stop continuous analysis
     */
    fun stopContinuousAnalysis() {
        imageAnalyzer?.clearAnalyzer()
        Log.d(TAG, "Stopped continuous image analysis")
    }

    /**
     * Convert ImageProxy to Bitmap
     */
    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, outputStream)
        
        val jpegBytes = outputStream.toByteArray()
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        
        // Apply rotation and crop
        return rotateAndCropBitmap(bitmap, rotationDegrees, cropRect)
    }

    /**
     * Rotate and crop bitmap to match image proxy
     */
    private fun rotateAndCropBitmap(bitmap: Bitmap, rotationDegrees: Int, cropRect: Rect?): Bitmap {
        var processedBitmap = bitmap
        
        // Rotate if needed
        if (rotationDegrees != 0) {
            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
            }
            processedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        // Crop if rect provided
        cropRect?.let { rect ->
            if (rect.left >= 0 && rect.top >= 0 && rect.right <= processedBitmap.width && 
                rect.bottom <= processedBitmap.height) {
                processedBitmap = Bitmap.createBitmap(processedBitmap, rect.left, rect.top, 
                    rect.width(), rect.height())
            }
        }

        return processedBitmap
    }

    /**
     * Rotate bitmap by specified degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        
        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Release camera resources
     */
    fun release() {
        try {
            stopContinuousAnalysis()
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            isInitialized = false
            Log.d(TAG, "Camera resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing camera: ${e.message}", e)
        }
    }
}

/**
 * Classification result data class
 */
data class ClassificationResult(
    val confidence: Float,
    val label: String,
    val allScores: Map<String, Float> = emptyMap()
)

/**
 * Capture result with classification data
 */
data class CaptureWithClassificationResult(
    val bitmap: Bitmap,
    val confidence: Float,
    val label: String,
    val passedThreshold: Boolean,
    val threshold: Float
)
