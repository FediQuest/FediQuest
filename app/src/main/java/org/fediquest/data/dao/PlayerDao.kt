package org.fediquest.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.fediquest.data.entity.PlayerStateEntity

@Dao
interface PlayerDao {

    @Query("SELECT * FROM player_state ORDER BY createdAt DESC")
    fun getAllPlayers(): Flow<List<PlayerStateEntity>>

    @Query("SELECT * FROM player_state WHERE playerId = :playerId LIMIT 1")
    suspend fun getPlayerById(playerId: String): PlayerStateEntity?

    @Query("SELECT * FROM player_state WHERE playerId = :playerId LIMIT 1")
    fun getPlayerByIdFlow(playerId: String): Flow<PlayerStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerStateEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerStateEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerStateEntity)

    @Query("DELETE FROM player_state WHERE playerId = :playerId")
    suspend fun deletePlayerById(playerId: String)

    @Query("SELECT COUNT(*) FROM player_state")
    fun getPlayerCount(): Flow<Int>

    @Query("UPDATE player_state SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: Long, timestamp: Long)
}
