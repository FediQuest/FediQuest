// File: app/src/main/java/com/fediquest/app/ui/map/MapViewModel.kt
package com.fediquest.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fediquest.app.data.models.FediLocation
import com.fediquest.app.data.models.Quest
import com.fediquest.app.data.repositories.QuestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for MapFragment.
 * Manages quest data and user location.
 */
class MapViewModel(
    private val questRepository: QuestRepository
) : ViewModel() {

    private val _quests = MutableStateFlow<List<Quest>>(emptyList())
    val quests: StateFlow<List<Quest>> = _quests.asStateFlow()

    private val _currentLocation = MutableStateFlow<FediLocation?>(null)
    val currentLocation: StateFlow<FediLocation?> = _currentLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Start listening to location updates and fetch nearby quests.
     */
    fun startLocationUpdates() {
        viewModelScope.launch {
            // In a real implementation, this would subscribe to LocationTracker
            // For now, we'll just load mock quests
            loadQuests()
        }
    }

    /**
     * Load quests from repository.
     */
    private suspend fun loadQuests() {
        try {
            _isLoading.value = true
            
            // Get current location (mocked for now)
            val location = _currentLocation.value ?: FediLocation(37.7749, -122.4194) // San Francisco
            
            // Fetch quests near current location
            questRepository.fetchRemoteQuests(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusKm = 10.0
            ).onSuccess { quests ->
                _quests.value = quests
            }.onFailure { error ->
                // Handle error (show toast in fragment)
            }
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Update current location.
     */
    fun updateLocation(location: FediLocation) {
        _currentLocation.value = location
        viewModelScope.launch {
            loadQuests()
        }
    }

    /**
     * Refresh quests manually.
     */
    fun refreshQuests() {
        viewModelScope.launch {
            loadQuests()
        }
    }

    /**
     * Filter quests by category.
     */
    fun filterByCategory(category: com.fediquest.app.data.models.QuestCategory) {
        viewModelScope.launch {
            val allQuests = _quests.value
            // This is simplified - in production you'd query the database
        }
    }
}

// File: app/src/main/java/com/fediquest/app/ui/scan/CameraScanFragment.kt
package com.fediquest.app.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fediquest.app.R
import com.fediquest.app.databinding.FragmentCameraScanBinding
import com.fediquest.app.di.ServiceLocator
import com.fediquest.app.util.ImageUtils
import com.fediquest.app.viewmodel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Fragment for camera-based trash detection using CameraX and TFLite.
 */
class CameraScanFragment : Fragment() {

    private var _binding: FragmentCameraScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CameraViewModel
    private lateinit var sharedViewModel: SharedViewModel
    
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(
            this,
            CameraViewModelFactory(ServiceLocator.questRepository)
        )[CameraViewModel::class.java]
        
        sharedViewModel = ViewModelProvider(
            requireActivity(),
            SharedViewModelFactory(ServiceLocator.userRepository)
        )[SharedViewModel::class.java]
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
        
        setupClickListeners()
        observeViewModel()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_PERMISSIONS)
    }

    private fun setupClickListeners() {
        binding.btnCapture.setOnClickListener {
            takePhoto()
        }
        
        binding.btnToggleDetection.setOnClickListener {
            toggleDetection()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.detectionResult.collect { result ->
                result?.let {
                    showDetectionResult(it)
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // Preview
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            
            // Image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            // Image analysis for TFLite
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        analyzeImage(imageProxy)
                    }
                }
            
            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_camera),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun analyzeImage(imageProxy: ImageProxy) {
        // TODO: Integrate TFLite object detection here
        // For now, just close the image
        imageProxy.close()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        val photoFile = createTempFile()
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModel.onPhotoCaptured(photoFile)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.scan_quest_completed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        requireContext(),
                        "Photo capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun createTempFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "quest_proof_$timestamp.jpg"
        return File(requireContext().cacheDir, "quest_proofs/$fileName").apply {
            parentFile?.mkdirs()
        }
    }

    private fun toggleDetection() {
        // Toggle AI detection on/off
        viewModel.toggleDetection()
    }

    private fun showDetectionResult(result: String) {
        binding.tvDetectionResult.text = result
        binding.tvDetectionResult.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}

// Factory for CameraViewModel
class CameraViewModelFactory(
    private val questRepository: com.fediquest.app.data.repositories.QuestRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return CameraViewModel(questRepository) as T
    }
}
