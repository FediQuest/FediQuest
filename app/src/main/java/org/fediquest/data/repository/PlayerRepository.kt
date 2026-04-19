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
        // TODO: getPlayerStateOnce removed from DAO, delegate to getPlayerStateSync
        return dao.getPlayerStateSync(userId)
    }
    
    /**
     * Initialize or update player state
     */
    suspend fun savePlayerState(playerState: PlayerStateEntity) {
        // TODO: insertOrReplace renamed to insertOrUpdate in Room DAO
        dao.insertOrUpdate(playerState)
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
        // TODO: updateXP removed from DAO, use entity copy + insertOrUpdate pattern
        val currentState = getPlayerStateOnce(userId) ?: return
        val updated = currentState.copy(
            level = level,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertOrUpdate(updated)
    }
    
    /**
     * Update companion evolution stage
     * Called when companion evolves based on XP/level milestones
     */
    suspend fun updateCompanionStage(userId: String = "local_player", stage: Int) {
        // TODO: updateCompanionStage removed from DAO, use new updateCompanion API
        // PlayerDao now has: suspend fun updateCompanion(userId, companionId, evolutionStage)
        dao.updateCompanion(
            userId = userId,
            companionId = "default_companion", // TODO: Pass companionId as parameter
            evolutionStage = stage
        )
    }
    
    /**
     * Update avatar skin
     */
    suspend fun updateAvatarSkin(userId: String = "local_player", skinId: String) {
        // TODO: updateAvatarSkin removed from DAO, use entity copy + insertOrUpdate pattern
        val currentState = getPlayerStateOnce(userId) ?: return
        val updated = currentState.copy(
            avatarSkinId = skinId,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertOrUpdate(updated)
    }
    
    /**
     * Get top players by XP (for local leaderboard)
     */
    fun getTopPlayers(): Flow<List<PlayerStateEntity>> {
        // TODO: getTopPlayers now requires limit parameter
        return dao.getTopPlayers(limit = 10)
    }
}
