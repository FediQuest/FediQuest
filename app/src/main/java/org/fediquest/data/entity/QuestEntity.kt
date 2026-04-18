// File: app/src/main/java/org/fediquest/data/entity/QuestEntity.kt
package org.fediquest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Quest Entity for Room Database
 * 
 * Represents a social-ecological quest in the local database.
 * Offline-first: quests are cached locally and synced when online.
 */
enum class QuestType {
    SOCIAL,         // Community engagement quests
    ECOLOGICAL,     // Environmental action quests
    CREATIVE        // Artistic/creative expression quests
}

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val type: QuestType, // ENUM: SOCIAL, ECOLOGICAL, CREATIVE
    val locationLat: Double,
    val locationLng: Double,
    val radiusMeters: Float = 50f,
    val xpReward: Int,
    val imageUrl: String?,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val fediverseSynced: Boolean = false // For ActivityPub queue
)
