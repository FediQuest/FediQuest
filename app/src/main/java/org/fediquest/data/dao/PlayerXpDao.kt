// File: app/src/main/java/org/fediquest/data/dao/PlayerXpDao.kt
package org.fediquest.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.fediquest.data.entity.PlayerXpEntity

/**
 * Player XP Data Access Object
 * 
 * Provides database operations for player experience tracking.
 * All operations work offline-first with local Room database.
 */
@Dao
interface PlayerXpDao {
    
    @Query("SELECT * FROM player_xp WHERE playerId = :playerId LIMIT 1")
    fun getPlayerXp(playerId: String): Flow<PlayerXpEntity?>
    
    @Query("SELECT * FROM player_xp WHERE playerId = :playerId LIMIT 1")
    suspend fun getPlayerXpOnce(playerId: String): PlayerXpEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(playerXp: PlayerXpEntity): Long
    
    @Update
    suspend fun update(playerXp: PlayerXpEntity)
    
    @Delete
    suspend fun delete(playerXp: PlayerXpEntity)
    
    @Query("UPDATE player_xp SET totalXP = :totalXP, currentLevelXP = :currentLevelXP, level = :level, updatedAt = :timestamp WHERE playerId = :playerId")
    suspend fun updateXP(
        playerId: String,
        totalXP: Int,
        currentLevelXP: Int,
        level: Int,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE player_xp SET totalXP = totalXP + :amount, currentLevelXP = currentLevelXP + :amount, updatedAt = :timestamp WHERE playerId = :playerId")
    suspend fun addXP(
        playerId: String,
        amount: Int,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE player_xp SET level = :level, lastQuestCompletedAt = :timestamp WHERE playerId = :playerId")
    suspend fun updateLevel(
        playerId: String,
        level: Int,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE player_xp SET dailyStreak = :streak, lastActiveDate = :timestamp WHERE playerId = :playerId")
    suspend fun updateDailyStreak(
        playerId: String,
        streak: Int,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("SELECT * FROM player_xp ORDER BY totalXP DESC LIMIT 10")
    fun getTopPlayers(): Flow<List<PlayerXpEntity>>
    
    @Query("SELECT AVG(totalXP) FROM player_xp")
    suspend fun getAverageXP(): Double?
}
