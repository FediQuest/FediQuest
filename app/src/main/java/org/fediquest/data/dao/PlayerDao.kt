// File: app/src/main/java/org/fediquest/data/dao/PlayerDao.kt
package org.fediquest.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.fediquest.data.entity.PlayerStateEntity

/**
 * Data Access Object for Player State
 */
@Dao
interface PlayerDao {
    @Query("SELECT * FROM player_state WHERE playerId = :playerId")
    fun getPlayerById(playerId: String): Flow<PlayerStateEntity?>

    @Query("SELECT * FROM player_state WHERE playerId = :playerId")
    suspend fun getPlayerByIdSync(playerId: String): PlayerStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerStateEntity)

    @Update
    suspend fun updatePlayer(player: PlayerStateEntity)

    @Query("UPDATE player_state SET totalXP = totalXP + :amount, updatedAt = strftime('%s', 'now') * 1000 WHERE playerId = :playerId")
    suspend fun addXP(playerId: String, amount: Int)

    @Query("SELECT * FROM player_state ORDER BY totalXP DESC LIMIT 1")
    suspend fun getTopPlayer(): PlayerStateEntity?
}
