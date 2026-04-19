// app/src/main/java/org/fediquest/data/entity/PlayerStateEntity.kt
package org.fediquest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the persistent state of the local player.
 * Tracks XP, level, avatar/companion customization, and evolution progress.
 * 
 * ✅ Updated: Added 'updatedAt' column for time-based sync queries.
 */
@Entity(tableName = "player_state")
data class PlayerStateEntity(
    @PrimaryKey val userId: String = "local_player",
    
    // Core progression
    val totalXP: Int = 0,
    val level: Int = 1,
    
    // Visual customization
    val avatarSkinId: String = "default",
    val companionId: String = "starter",
    val companionEvolutionStage: Int = 0,
    
    // Activity tracking
    val lastQuestCompletedAt: Long? = null,
    
    // ✅ NEW: Timestamp for sync/diff operations (required by PlayerDao)
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Helper: Calculate XP needed for next level (simple linear scaling).
     */
    fun xpToNextLevel(): Int = level * 100
    
    /**
     * Helper: Check if player can level up with current XP.
     */
    fun canLevelUp(): Boolean = totalXP >= (level * 100)
    
    /**
     * Helper: Return next level's XP threshold.
     */
    fun nextLevelThreshold(): Int = (level + 1) * 100
}
