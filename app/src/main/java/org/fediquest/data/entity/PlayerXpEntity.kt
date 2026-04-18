// File: app/src/main/java/org/fediquest/data/entity/PlayerXpEntity.kt
package org.fediquest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Player XP Entity for Room Database
 * 
 * Tracks player experience points, level, and progression.
 * Offline-first: all XP gains are stored locally immediately.
 */
@Entity(tableName = "player_xp")
data class PlayerXpEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val playerId: String,
    
    val totalXP: Int = 0,
    val currentLevelXP: Int = 0,
    val level: Int = 1,
    
    val lastQuestCompletedAt: Long? = null,
    val dailyStreak: Int = 0,
    val lastActiveDate: Long = System.currentTimeMillis(),
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
