// File: app/src/main/java/org/fediquest/data/dao/PlayerDao.kt
package org.fediquest.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.fediquest.data.entity.PlayerStateEntity

/**
 * Player State Data Access Object
 * 
 * Provides database operations for player state tracking.
 * All operations work offline-first with local Room database.
 */
@Dao
interface PlayerDao {
    
    @Query("SELECT * FROM player_state WHERE userId = :userId LIMIT 1")
    fun getPlayerState(userId: String): Flow<PlayerStateEntity?>
    
    @Query("SELECT * FROM player_state WHERE userId = :userId LIMIT 1")
    suspend fun getPlayerStateOnce(userId: String): PlayerStateEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(playerState: PlayerStateEntity)
    
    @Update
    suspend fun update(playerState: PlayerStateEntity)
    
    @Query("UPDATE player_state SET totalXP = :totalXP, level = :level, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun updateXP(
        userId: String,
        totalXP: Int,
        level: Int,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE player_state SET totalXP = totalXP + :amount, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun addXP(
        userId: String,
        amount: Int,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE player_state SET companionEvolutionStage = :stage, lastQuestCompletedAt = :timestamp WHERE userId = :userId")
    suspend fun updateCompanionStage(
        userId: String,
        stage: Int,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE player_state SET avatarSkinId = :skinId WHERE userId = :userId")
    suspend fun updateAvatarSkin(userId: String, skinId: String)
    
    @Query("SELECT * FROM player_state ORDER BY totalXP DESC LIMIT 10")
    fun getTopPlayers(): Flow<List<PlayerStateEntity>>
}
