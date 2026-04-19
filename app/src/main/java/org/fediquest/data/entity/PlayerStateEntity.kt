// File: app/src/main/java/org/fediquest/data/entity/PlayerStateEntity.kt
package org.fediquest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Player State Entity for Room Database
 * 
 * Tracks player progression, companion state, and avatar customization.
 * Offline-first: all state changes are stored locally immediately.
 */
@Entity(tableName = "player_state")
data class PlayerStateEntity(
    @PrimaryKey val userId: String = "local_player",
    val totalXP: Int = 0,
    val level: Int = 1,
    val avatarSkinId: String = "default",
    val companionId: String = "starter",
    val companionEvolutionStage: Int = 0,
    val lastQuestCompletedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
