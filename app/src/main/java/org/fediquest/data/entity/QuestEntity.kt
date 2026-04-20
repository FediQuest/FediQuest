// File: app/src/main/java/org/fediquest/data/entity/QuestEntity.kt
package org.fediquest.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Quest Entity
 * Represents a location-based AR quest with verification requirements.
 */
@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "type") val type: QuestType,
    @ColumnInfo(name = "locationLat") val locationLat: Double,
    @ColumnInfo(name = "locationLng") val locationLng: Double,
    @ColumnInfo(name = "radiusMeters") val radiusMeters: Float,
    @ColumnInfo(name = "xpReward") val xpReward: Int,
    @ColumnInfo(name = "imageUrl") val imageUrl: String? = null,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completedAt") val completedAt: Long? = null,
    @ColumnInfo(name = "isCompleted") val isCompleted: Boolean = false
)

enum class QuestType {
    ECO_CLEANUP,
    TREE_PLANTING,
    WILDLIFE_SPOTTING,
    COMMUNITY_SERVICE,
    EDUCATIONAL
}
