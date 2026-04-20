package org.fediquest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.lifecycleScope
import io.github.sceneview.SceneView
import kotlinx.coroutines.launch
import org.fediquest.data.database.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var sceneView: SceneView
    private lateinit var questVerifier: QuestVerifier
    private lateinit var appDatabase: AppDatabase

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            initializeAR()
        } else {
            Toast.makeText(this, "Permissions required for AR", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_main)
        
        sceneView = findViewById(R.id.sceneView)
        questVerifier = QuestVerifier(this)
        appDatabase = AppDatabase.getDatabase(this)
        
        checkPermissions()
    }

    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val missingPermissions = requiredPermissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            initializeAR()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun initializeAR() {
        setupSceneView()
        loadQuests()
    }

    private fun setupSceneView() {
        sceneView.setOnTapListener { hitResult ->
            lifecycleScope.launch {
                val nearbyQuests = appDatabase.questDao().getQuestsInArea(
                    minLat = hitResult.position.latitude - 0.001,
                    maxLat = hitResult.position.latitude + 0.001,
                    minLng = hitResult.position.longitude - 0.001,
                    maxLng = hitResult.position.longitude + 0.001
                )
                
                if (nearbyQuests.isNotEmpty()) {
                    placeARObject(hitResult, nearbyQuests.first())
                }
            }
        }
    }

    private fun placeARObject(hitResult: SceneView.HitResult, quest: org.fediquest.data.entity.QuestEntity) {
        // Place AR anchor at hit result position
        val anchor = hitResult.createAnchor()
        // Add visual representation for the quest
    }

    private fun loadQuests() {
        lifecycleScope.launch {
            appDatabase.questDao().getAllQuests().collect { quests ->
                // Update UI with quests
            }
        }
    }

    private fun captureImageForVerification(outputFile: File) {
        val imageCapture = ImageCapture.Builder().build()
        
        imageCapture.takePicture(
            outputFile,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    lifecycleScope.launch {
                        val currentQuest = appDatabase.questDao().getActiveQuests().value?.firstOrNull()
                        currentQuest?.let { quest ->
                            val result = questVerifier.verifyQuestCompletion(quest, outputFile.absolutePath)
                            handleVerificationResult(result)
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@MainActivity,
                        "Image capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun handleVerificationResult(result: QuestVerificationResult) {
        when (result) {
            is QuestVerificationResult.Success -> {
                lifecycleScope.launch {
                    appDatabase.questDao().completeQuest(
                        id = result.questId,
                        completedAt = result.timestamp,
                        updatedAt = result.timestamp
                    )
                    Toast.makeText(this, "Quest verified!", Toast.LENGTH_SHORT).show()
                }
            }
            is QuestVerificationResult.Failure -> {
                Toast.makeText(this, "Verification failed: ${result.reason}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        questVerifier.cleanup()
    }
}
