package org.fediquest

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fediquest.data.entity.QuestEntity
import org.tensorflow.lite.task.vision.core.TensorImage
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.vision.classifier.ImageClassifier.ImageClassifierOptions
import java.io.File

class QuestVerifier(private val context: Context) {

    private var imageClassifier: ImageClassifier? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val options = ImageClassifierOptions.builder()
                .setMaxResults(5)
                .setScoreThreshold(0.5f)
                .build()
            imageClassifier = ImageClassifier.createFromFileAndOptions(context, "model.tflite", options)
        } catch (e: Exception) {
            imageClassifier = null
        }
    }

    suspend fun verifyQuestCompletion(
        quest: QuestEntity,
        capturedImagePath: String
    ): QuestVerificationResult = withContext(Dispatchers.Default) {
        val result = verifyImage(capturedImagePath)
        
        if (result.confidence >= 0.7f) {
            QuestVerificationResult.Success(
                questId = quest.id,
                confidence = result.confidence,
                verifiedLabel = result.label,
                timestamp = System.currentTimeMillis()
            )
        } else {
            QuestVerificationResult.Failure(
                questId = quest.id,
                reason = "Insufficient confidence: ${result.confidence}",
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private fun verifyImage(imagePath: String): ClassificationResult {
        val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
            ?: return ClassificationResult(0f, "unknown")

        val tensorImage = TensorImage.fromBitmap(bitmap)
        
        val classifier = imageClassifier ?: return ClassificationResult(0f, "unknown")
        
        val results = classifier.classify(tensorImage)
        
        if (results.isNotEmpty() && results[0].categories.isNotEmpty()) {
            val topCategory = results[0].categories[0]
            return ClassificationResult(topCategory.score, topCategory.label)
        }
        
        return ClassificationResult(0f, "unknown")
    }

    fun cleanup() {
        imageClassifier?.close()
        imageClassifier = null
    }
}

data class ClassificationResult(
    val confidence: Float,
    val label: String
)

sealed class QuestVerificationResult {
    data class Success(
        val questId: Long,
        val confidence: Float,
        val verifiedLabel: String,
        val timestamp: Long
    ) : QuestVerificationResult()

    data class Failure(
        val questId: Long,
        val reason: String,
        val timestamp: Long
    ) : QuestVerificationResult()
}
