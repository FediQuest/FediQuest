// File: app/src/main/java/org/fediquest/data/dao/PlayerXpDao.kt
package org.fediquest.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.fediquest.data.entity.PlayerXpEntity

/**
 * Data Access Object for Player XP Transactions
 */
@Dao
interface PlayerXpDao {
    @Query("SELECT * FROM player_xp WHERE playerId = :playerId ORDER BY timestamp DESC")
    fun getXpHistory(playerId: String): Flow<List<PlayerXpEntity>>

    @Insert
    suspend fun insertXpTransaction(transaction: PlayerXpEntity)

    @Query("SELECT SUM(amount) FROM player_xp WHERE playerId = :playerId")
    suspend fun getTotalXP(playerId: String): Int?

    @Query("DELETE FROM player_xp WHERE playerId = :playerId")
    suspend fun deleteAllForPlayer(playerId: String)
}
