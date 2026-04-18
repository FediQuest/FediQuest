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
@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val serverId: String?, // Optional ID from remote server
    
    val type: String, // planting, recycling, cleanup, etc.
    val title: String,
    val description: String,
    val difficulty: String, // easy, medium, hard
    
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    
    val xpReward: Int,
    val coinReward: Int,
    
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val completionCount: Int = 0,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null,
    
    val metadata: String? = null // JSON string for additional data
)
