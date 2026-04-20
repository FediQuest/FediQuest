// File: app/src/main/java/org/fediquest/camera/CameraCaptureManager.kt
package org.fediquest.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
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
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.75f
        const val HIGH_CONFIDENCE_THRESHOLD = 0.90f
        const val LOW_CONFIDENCE_THRESHOLD = 0.50f
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
                    
                    preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetResolution(android.util.Size(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT))
                        .build()

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

            setupCaptureCallbacks(onCaptureSuccess, onCaptureError)
            
            Log.d(TAG, "Camera bound to lifecycle")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera to lifecycle: ${e.message}", e)
            onCaptureError(e)
        }
    }

    private fun setupCaptureCallbacks(
        onCaptureSuccess: (Bitmap) -> Unit,
        onCaptureError: (Exception) -> Unit
    ) {
        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            try {
                val bitmap = imageProxy.toBitmap()
                // Classification handled externally
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing image: ${e.message}", e)
            } finally {
                imageProxy.close()
            }
        }
    }

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
                    Log.d(TAG, "Image captured successfully")
                    
                    val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                    
                    if (bitmap != null) {
                        onSuccess(bitmap)
                    } else {
                        onError(Exception("Failed to decode captured image"))
                    }
                    
                    photoFile.delete()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Error capturing image: ${exception.message}", exception)
                    onError(exception)
                }
            }
        )
    }

    fun getCurrentConfidenceThreshold(): Float = currentConfidenceThreshold

    fun setConfidenceThreshold(threshold: Float) {
        currentConfidenceThreshold = threshold
        Log.d(TAG, "Confidence threshold set to: $threshold")
    }

    fun isReady(): Boolean = isInitialized && camera != null

    fun release() {
        try {
            cameraProvider?.unbindAll()
            cameraProvider = null
            cameraExecutor.shutdownNow()
            isInitialized = false
            Log.d(TAG, "Camera resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing camera resources: ${e.message}", e)
        }
    }
}
