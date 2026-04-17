// File: app/src/main/java/com/fediquest/app/util/XPManager.kt
package com.fediquest.app.util

import com.fediquest.app.data.models.Companion
import com.fediquest.app.data.models.CompanionSpecies
import com.fediquest.app.data.models.QuestCategory

/**
 * Utility object for XP calculations and companion synergy logic.
 */
object XPManager {

    // Synergy mappings between companion species and quest categories
    private val synergyMap = mapOf(
        CompanionSpecies.ECO_DRAKE to listOf(QuestCategory.PLANTING, QuestCategory.NATURE),
        CompanionSpecies.GREEN_PHOENIX to listOf(QuestCategory.NATURE, QuestCategory.WILDLIFE),
        CompanionSpecies.FOREST_UNICORN to listOf(QuestCategory.NATURE, QuestCategory.PLANTING),
        CompanionSpecies.AQUA_TURTLE to listOf(QuestCategory.WATER, QuestCategory.CLEANUP),
        CompanionSpecies.SOLAR_LION to listOf(QuestCategory.RECYCLE, QuestCategory.CLEANUP)
    )

    // Synergy bonus multiplier (50% bonus)
    private const val SYNERGY_MULTIPLIER = 1.5f

    /**
     * Get synergy multiplier for a companion-quest combination.
     * Returns 1.5f if there's synergy, 1.0f otherwise.
     */
    fun getSynergyMultiplier(companion: CompanionSpecies, questCategory: QuestCategory): Float {
        return if (synergyMap[companion]?.contains(questCategory) == true) {
            SYNERGY_MULTIPLIER
        } else {
            1.0f
        }
    }

    /**
     * Calculate XP required to reach a specific level.
     * Uses exponential growth: base 100, increases by 25% per level.
     */
    fun calculateXPForLevel(level: Int): Int {
        if (level <= 1) return 100
        return (100 * Math.pow(1.25, level - 1.0)).toInt()
    }

    /**
     * Calculate total XP needed to reach a level from level 1.
     */
    fun getTotalXPForLevel(level: Int): Int {
        var total = 0
        for (i in 1 until level) {
            total += calculateXPForLevel(i)
        }
        return total
    }

    /**
     * Calculate companion evolution requirements.
     * Returns the level at which the companion evolves to the next stage.
     */
    fun getEvolutionLevel(evolutionStage: Int): Int {
        return when (evolutionStage) {
            1 -> 10   // First evolution at level 10
            2 -> 30   // Second evolution at level 30
            else -> Int.MAX_VALUE
        }
    }

    /**
     * Calculate happiness decay over time.
     * Companions lose 1 happiness point every 30 minutes of inactivity.
     */
    fun calculateHappinessDecay(lastInteractionAt: Long, currentTime: Long = System.currentTimeMillis()): Int {
        val minutesSinceInteraction = (currentTime - lastInteractionAt) / (1000 * 60)
        return (minutesSinceInteraction / 30).toInt().coerceIn(0, 100)
    }

    /**
     * Calculate energy regeneration.
     * Companions regain 1 energy point every 5 minutes of rest.
     */
    fun calculateEnergyRegain(lastRestAt: Long, currentTime: Long = System.currentTimeMillis()): Int {
        val minutesSinceRest = (currentTime - lastRestAt) / (1000 * 60)
        return (minutesSinceRest / 5).toInt().coerceIn(0, 100)
    }

    /**
     * Check if a companion can evolve based on level and current stage.
     */
    fun canEvolve(companion: Companion): Boolean {
        val nextEvolutionLevel = getEvolutionLevel(companion.evolutionStage)
        return companion.level >= nextEvolutionLevel && companion.evolutionStage < 3
    }

    /**
     * Get badge ID for completing a certain number of quests.
     */
    fun getBadgeForQuestCount(count: Int): String? {
        return when {
            count >= 100 -> "badge_master_eco_warrior"
            count >= 50 -> "badge_veteran_eco_warrior"
            count >= 25 -> "badge_experienced_eco_warrior"
            count >= 10 -> "badge_dedicated_eco_warrior"
            count >= 5 -> "badge_committed_eco_warrior"
            count >= 1 -> "badge_beginner_eco_warrior"
            else -> null
        }
    }

    /**
     * Get badge ID for reaching a specific level.
     */
    fun getBadgeForLevel(level: Int): String? {
        return when {
            level >= 50 -> "badge_legendary_eco_hero"
            level >= 30 -> "badge_epic_eco_hero"
            level >= 20 -> "badge_rare_eco_hero"
            level >= 10 -> "badge_uncommon_eco_hero"
            level >= 5 -> "badge_common_eco_hero"
            else -> null
        }
    }
}

// File: app/src/main/java/com/fediquest/app/util/LocationTracker.kt
package com.fediquest.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.fediquest.app.data.models.FediLocation
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Utility class for tracking user location using FusedLocationProvider.
 */
class LocationTracker(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000L // Update interval: 5 seconds
    ).apply {
        setMinUpdateIntervalMillis(2000L) // Fastest update: 2 seconds
        setWaitForAccurateLocation(true)
    }.build()

    /**
     * Check if location permission is granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get current location as a Flow.
     * Emits location updates continuously until cancelled.
     */
    fun getLocationUpdates(): Flow<FediLocation?> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(FediLocation.fromAndroidLocation(location))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ) {
            trySend(null)
            close(it)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Get last known location (one-time).
     */
    suspend fun getLastLocation(): FediLocation? {
        return try {
            if (!hasLocationPermission()) return null
            
            awaitLocation()?.let { FediLocation.fromAndroidLocation(it) }
        } catch (e: SecurityException) {
            null
        }
    }

    private suspend fun awaitLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                continuation.resume(location, null)
            }.addOnFailureListener { e ->
                continuation.resume(null, e)
            }
        }
    }
}

// File: app/src/main/java/com/fediquest/app/util/FediverseClient.kt
package com.fediquest.app.util

import com.fediquest.app.data.models.FediversePost
import com.fediquest.app.data.models.Visibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Client for posting to Fediverse instances via ActivityPub protocol.
 * Currently a mock implementation for development.
 */
object FediverseClient {

    // Mock mode flag - set to false in production
    private const val MOCK_MODE = true

    /**
     * Post a note to the Fediverse.
     * 
     * @param post The post to publish
     * @return Post ID if successful
     */
    suspend fun postNote(post: FediversePost): String = withContext(Dispatchers.IO) {
        if (MOCK_MODE) {
            // Mock implementation - simulate network delay
            kotlinx.coroutines.delay(500)
            
            // Simulate success/failure
            if (Math.random() > 0.1) {
                "mock_post_${post.id}"
            } else {
                throw Exception("Mock network error")
            }
        } else {
            // TODO: Implement actual ActivityPub POST request
            // This would serialize the post to ActivityStreams JSON
            // and POST to the user's outbox endpoint
            throw NotImplementedError("Production Fediverse client not implemented")
        }
    }

    /**
     * Serialize a post to ActivityStreams JSON format.
     */
    fun serializeToActivityStreams(post: FediversePost): String {
        // Simplified ActivityStreams 2.0 Note object
        return buildString {
            appendLine("{")
            appendLine("  \"@context\": \"https://www.w3.org/ns/activitystreams\",")
            appendLine("  \"type\": \"Note\",")
            appendLine("  \"id\": \"${post.id}\",")
            appendLine("  \"content\": \"${escapeJson(post.content)}\",")
            if (post.imageUrl != null) {
                appendLine("  \"attachment\": [{")
                appendLine("    \"type\": \"Image\",")
                appendLine("    \"url\": \"${post.imageUrl}\"")
                appendLine("  }],")
            }
            appendLine("  \"tag\": [")
            post.tags.forEachIndexed { index, tag ->
                appendLine("    {\"type\": \"Hashtag\", \"name\": \"#$tag\"}${if (index < post.tags.lastIndex) "," else ""}")
            }
            appendLine("  ],")
            appendLine("  \"to\": [\"${getAudience(post.visibility)}\"]")
            appendLine("}")
        }
    }

    private fun escapeJson(text: String): String {
        return text.replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun getAudience(visibility: Visibility): String {
        return when (visibility) {
            Visibility.PUBLIC -> "https://www.w3.org/ns/activitystreams#Public"
            Visibility.UNLISTED -> "https://www.w3.org/ns/activitystreams#Public"
            Visibility.FOLLOWERS_ONLY -> "followers"
            Visibility.DIRECT -> "direct"
        }
    }
}

// File: app/src/main/java/com/fediquest/app/util/ImageUtils.kt
package com.fediquest.app.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility functions for image processing.
 */
object ImageUtils {

    /**
     * Resize bitmap to maximum dimensions while maintaining aspect ratio.
     */
    fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        
        val scaleFactor = when {
            width > height -> maxDimension.toFloat() / width
            else -> maxDimension.toFloat() / height
        }
        
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Rotate bitmap based on EXIF orientation.
     */
    fun rotateBitmap(bitmap: Bitmap, filePath: String): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: IOException) {
            bitmap
        }
    }

    /**
     * Save bitmap to file as JPEG.
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int = 85): File {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return file
    }

    /**
     * Create a temporary file for storing quest proof images.
     */
    fun createTempImageFile(directory: File? = null, prefix: String = "quest_proof"): File {
        val timestamp = System.currentTimeMillis()
        val fileName = "${prefix}_$timestamp.jpg"
        
        return if (directory != null) {
            File(directory, fileName)
        } else {
            File.createTempFile(prefix, ".jpg")
        }
    }
}
