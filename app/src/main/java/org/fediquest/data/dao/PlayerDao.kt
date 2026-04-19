// app/src/main/java/org/fediquest/data/dao/PlayerDao.kt
package org.fediquest.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.fediquest.data.entity.PlayerStateEntity

/**
 * Data Access Object for player state operations.
 * All queries are validated at compile-time by Room/KSP.
 * 
 * ✅ Fixed: All queries now reference the 'updatedAt' column correctly.
 */
@Dao
interface PlayerDao {
    
    // === READ OPERATIONS ===
    
    /**
     * Observe player state changes reactively (Flow).
     */
    @Query("SELECT * FROM player_state WHERE userId = :userId")
    fun getPlayerState(userId: String): Flow<PlayerStateEntity>
    
    /**
     * Get player state synchronously (for one-off reads).
     */
    @Query("SELECT * FROM player_state WHERE userId = :userId LIMIT 1")
    suspend fun getPlayerStateSync(userId: String): PlayerStateEntity?
    
    /**
     * ✅ FIXED: Query players updated since a timestamp (for Fediverse sync).
     */
    @Query("SELECT * FROM player_state WHERE updatedAt > :since ORDER BY updatedAt DESC")
    fun getRecentlyUpdated(since: Long): Flow<List<PlayerStateEntity>>
    
    /**
     * Get all players for leaderboard (local-only for demo).
     */
    @Query("SELECT * FROM player_state ORDER BY totalXP DESC LIMIT :limit")
    fun getTopPlayers(limit: Int): Flow<List<PlayerStateEntity>>
    
    // === WRITE OPERATIONS ===
    
    /**
     * Insert or replace player state (idempotent upsert).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(player: PlayerStateEntity)
    
    /**
     * Update existing player state (fails if not exists).
     */
    @Update
    suspend fun update(player: PlayerStateEntity)
    
    /**
     * ✅ FIXED: Update only the timestamp (for sync tracking).
     */
    @Query("UPDATE player_state SET updatedAt = :timestamp WHERE userId = :userId")
    suspend fun updateTimestamp(userId: String, timestamp: Long)
    
    /**
     * Add XP and auto-level if threshold reached.
     */
    @Transaction
    suspend fun addXP(userId: String, xpAmount: Int): PlayerStateEntity {
        val current = getPlayerStateSync(userId) 
            ?: PlayerStateEntity(userId = userId)
        
        val newXP = current.totalXP + xpAmount
        val newLevel = if (newXP >= current.nextLevelThreshold()) {
            current.level + 1
        } else current.level
        
        val updated = current.copy(
            totalXP = newXP,
            level = newLevel,
            updatedAt = System.currentTimeMillis()
        )
        insertOrUpdate(updated)
        return updated
    }
    
    /**
     * Update companion evolution state.
     */
    @Transaction
    suspend fun updateCompanion(
        userId: String,
        companionId: String,
        evolutionStage: Int
    ): PlayerStateEntity {
        val current = getPlayerStateSync(userId)
            ?: PlayerStateEntity(userId = userId)
        
        val updated = current.copy(
            companionId = companionId,
            companionEvolutionStage = evolutionStage,
            updatedAt = System.currentTimeMillis()
        )
        insertOrUpdate(updated)
        return updated
    }
    
    // === DELETE OPERATIONS ===
    
    /**
     * Delete player state (for reset/testing).
     */
    @Query("DELETE FROM player_state WHERE userId = :userId")
    suspend fun delete(userId: String)
    
    /**
     * Delete all player data (factory reset).
     */
    @Query("DELETE FROM player_state")
    suspend fun deleteAll()
}
