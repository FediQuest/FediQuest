// File: app/src/main/java/com/fediquest/app/data/models/Quest.kt
package com.fediquest.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an eco-quest that users can complete.
 * Quests are location-based challenges with rewards.
 */
@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: QuestCategory,
    val difficulty: QuestDifficulty,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 50f,
    val xpReward: Int = 100,
    val coinReward: Int = 50,
    val validationMode: ValidationMode = ValidationMode.AI_AUTO,
    val status: QuestStatus = QuestStatus.AVAILABLE,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val completedAt: Long? = null,
    val proofImageUrl: String? = null
)

enum class QuestCategory(val displayName: String, val icon: String) {
    NATURE("Nature", "🌳"),
    RECYCLE("Recycling", "♻️"),
    CLEANUP("Cleanup", "🧹"),
    PLANTING("Planting", "🌱"),
    WILDLIFE("Wildlife", "🦋"),
    WATER("Water Conservation", "💧")
}

enum class QuestDifficulty(val multiplier: Float) {
    EASY(1.0f),
    MEDIUM(1.5f),
    HARD(2.0f),
    EXPERT(3.0f)
}

enum class ValidationMode {
    AI_AUTO,      // TFLite object detection
    COMMUNITY,    // Fediverse voting
    HYBRID,       // AI + community verification
    GPS_ONLY      // Location-based only
}

enum class QuestStatus {
    AVAILABLE,
    IN_PROGRESS,
    PENDING_VALIDATION,
    COMPLETED,
    FAILED,
    EXPIRED
}

// File: app/src/main/java/com/fediquest/app/data/models/User.kt
package com.fediquest.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the user's profile and progress.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = "current_user",
    val username: String = "EcoWarrior",
    val avatarId: String = "default",
    val companionId: String? = null,
    val level: Int = 1,
    val currentXP: Int = 0,
    val xpToNextLevel: Int = 100,
    val totalCoins: Int = 0,
    val questsCompleted: Int = 0,
    val badges: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

// File: app/src/main/java/com/fediquest/app/data/models/Avatar.kt
package com.fediquest.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents customizable avatar parts.
 */
@Entity(tableName = "avatars")
data class Avatar(
    @PrimaryKey val id: String,
    val name: String,
    val category: AvatarCategory,
    val drawableResId: Int,
    val isUnlocked: Boolean = false,
    val isEquipped: Boolean = false,
    val unlockRequirement: String? = null // e.g., "level_5" or "badge_planter"
)

enum class AvatarCategory {
    HAIR,
    OUTFIT,
    ACCESSORY,
    SHOES,
    BACKGROUND
}

// File: app/src/main/java/com/fediquest/app/data/models/Companion.kt
package com.fediquest.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an eco-companion that evolves with the user.
 * Copyright-safe original creatures (not Pokemon).
 */
@Entity(tableName = "companions")
data class Companion(
    @PrimaryKey val id: String,
    val name: String,
    val species: CompanionSpecies,
    val level: Int = 1,
    val currentXP: Int = 0,
    val xpToNextLevel: Int = 100,
    val happiness: Int = 100, // 0-100
    val energy: Int = 100,    // 0-100
    val evolutionStage: Int = 1, // 1, 2, 3
    val isUnlocked: Boolean = false,
    val isEquipped: Boolean = false,
    val lastInteractionAt: Long = System.currentTimeMillis()
)

enum class CompanionSpecies(val displayName: String, val description: String) {
    ECO_DRAKE("Eco-Drake", "A small dragon that loves planting trees"),
    GREEN_PHOENIX("Green Phoenix", "A bird made of leaves and vines"),
    FOREST_UNICORN("Forest Unicorn", "A mystical guardian of nature"),
    AQUA_TURTLE("Aqua Turtle", "A water guardian that cleans oceans"),
    SOLAR_LION("Solar Lion", "A lion powered by sunlight")
}

// File: app/src/main/java/com/fediquest/app/data/models/FediversePost.kt
package com.fediquest.app.data.models

/**
 * Represents an ActivityPub post for Fediverse integration.
 */
data class FediversePost(
    val id: String,
    val type: PostType = PostType.NOTE,
    val content: String,
    val questId: String? = null,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val visibility: Visibility = Visibility.PUBLIC,
    val createdAt: Long = System.currentTimeMillis()
)

enum class PostType {
    NOTE,
    ARTICLE,
    IMAGE
}

enum class Visibility {
    PUBLIC,
    UNLISTED,
    FOLLOWERS_ONLY,
    DIRECT
}

// File: app/src/main/java/com/fediquest/app/data/models/Location.kt
package com.fediquest.app.data.models

import android.location.Location

/**
 * Wrapper for location data with utility functions.
 */
data class FediLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun distanceTo(other: FediLocation): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            this.latitude, this.longitude,
            other.latitude, other.longitude,
            results
        )
        return results[0]
    }

    fun isWithinRadius(targetLat: Double, targetLng: Double, radiusMeters: Float): Boolean {
        return distanceTo(FediLocation(targetLat, targetLng)) <= radiusMeters
    }

    companion object {
        fun fromAndroidLocation(location: Location): FediLocation {
            return FediLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestamp = location.time
            )
        }
    }
}
