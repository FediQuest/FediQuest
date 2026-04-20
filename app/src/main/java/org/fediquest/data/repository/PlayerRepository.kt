// File: app/src/main/java/org/fediquest/data/repository/PlayerRepository.kt
package org.fediquest.data.repository

import kotlinx.coroutines.flow.Flow
import org.fediquest.data.dao.PlayerDao
import org.fediquest.data.dao.PlayerXpDao
import org.fediquest.data.entity.PlayerStateEntity
import org.fediquest.data.entity.PlayerXpEntity

/**
 * Repository for Player data operations
 * Abstracts data source (local DB vs remote sync)
 */
class PlayerRepository(
    private val playerDao: PlayerDao,
    private val playerXpDao: PlayerXpDao
) {
    fun getPlayerById(playerId: String): Flow<PlayerStateEntity?> {
        return playerDao.getPlayerById(playerId)
    }

    suspend fun getPlayerByIdSync(playerId: String): PlayerStateEntity? {
        return playerDao.getPlayerByIdSync(playerId)
    }

    suspend fun insertPlayer(player: PlayerStateEntity) {
        playerDao.insertPlayer(player)
    }

    suspend fun updatePlayer(player: PlayerStateEntity) {
        playerDao.updatePlayer(player)
    }

    suspend fun addXP(amount: Int) {
        // Note: playerId would need to be passed in real impl
        playerDao.addXP("default_player", amount)
        
        // Record transaction
        val transaction = PlayerXpEntity(
            playerId = "default_player",
            amount = amount,
            source = "quest_completion"
        )
        playerXpDao.insertXpTransaction(transaction)
    }

    fun getXpHistory(playerId: String): Flow<List<PlayerXpEntity>> {
        return playerXpDao.getXpHistory(playerId)
    }

    suspend fun getTopPlayer(): PlayerStateEntity? {
        return playerDao.getTopPlayer()
    }
}
