// File: app/src/main/java/org/fediquest/data/repository/PlayerRepository.kt
package org.fediquest.data.repository

import kotlinx.coroutines.flow.Flow
import org.fediquest.data.dao.PlayerDao
import org.fediquest.data.entity.PlayerStateEntity

/**
 * Player Repository - Single Source of Truth for Player State
 * 
 * Provides a clean API for player state operations, abstracting the data layer.
 * All operations work offline-first with local Room database.
 */
class PlayerRepository(private val dao: PlayerDao) {
    
    /**
     * Flow of player state
     * Observers will be notified of any changes
     */
    fun getPlayerState(userId: String = "local_player"): Flow<PlayerStateEntity?> {
        return dao.getPlayerState(userId)
    }
    
    /**
     * Get player state once (suspend function)
     */
    suspend fun getPlayerStateOnce(userId: String = "local_player"): PlayerStateEntity? {
        return dao.getPlayerStateOnce(userId)
    }
    
    /**
     * Initialize or update player state
     */
    suspend fun savePlayerState(playerState: PlayerStateEntity) {
        dao.insertOrReplace(playerState)
    }
    
    /**
     * Add XP to player
     * This persists the change locally immediately (offline-first)
     */
    suspend fun addXP(userId: String = "local_player", amount: Int) {
        dao.addXP(userId, amount)
    }
    
    /**
     * Update player level
     */
    suspend fun updateLevel(userId: String = "local_player", level: Int) {
        val currentState = getPlayerStateOnce(userId) ?: return
        dao.updateXP(userId, currentState.totalXP, level)
    }
    
    /**
     * Update companion evolution stage
     * Called when companion evolves based on XP/level milestones
     */
    suspend fun updateCompanionStage(userId: String = "local_player", stage: Int) {
        dao.updateCompanionStage(userId, stage)
    }
    
    /**
     * Update avatar skin
     */
    suspend fun updateAvatarSkin(userId: String = "local_player", skinId: String) {
        dao.updateAvatarSkin(userId, skinId)
    }
    
    /**
     * Get top players by XP (for local leaderboard)
     */
    fun getTopPlayers(): Flow<List<PlayerStateEntity>> {
        return dao.getTopPlayers()
    }
}
