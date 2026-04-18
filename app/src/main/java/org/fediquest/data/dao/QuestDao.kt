// File: app/src/main/java/org/fediquest/data/dao/QuestDao.kt
package org.fediquest.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.fediquest.data.entity.QuestEntity

/**
 * Quest Data Access Object
 * 
 * Provides database operations for quests.
 * All operations work offline-first with local Room database.
 */
@Dao
interface QuestDao {
    
    @Query("SELECT * FROM quests WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveQuests(): Flow<List<QuestEntity>>
    
    @Query("SELECT * FROM quests WHERE id = :id")
    suspend fun getQuestById(id: Long): QuestEntity?
    
    @Query("SELECT * FROM quests WHERE serverId = :serverId")
    suspend fun getQuestByServerId(serverId: String?): QuestEntity?
    
    @Query("SELECT * FROM quests WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng AND isActive = 1")
    fun getQuestsInArea(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<QuestEntity>>
    
    @Query("SELECT * FROM quests WHERE type = :type AND isActive = 1")
    fun getQuestsByType(type: String): Flow<List<QuestEntity>>
    
    @Query("SELECT * FROM quests WHERE isCompleted = 0 AND isActive = 1")
    fun getIncompleteQuests(): Flow<List<QuestEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quests: List<QuestEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quest: QuestEntity): Long
    
    @Update
    suspend fun update(quest: QuestEntity)
    
    @Delete
    suspend fun delete(quest: QuestEntity)
    
    @Query("DELETE FROM quests WHERE serverId IS NOT NULL AND lastSyncedAt < :timestamp")
    suspend fun deleteOldSyncedQuests(timestamp: Long)
    
    @Query("UPDATE quests SET isCompleted = 1, completionCount = completionCount + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markQuestCompleted(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE quests SET isActive = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun deactivateQuest(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE quests SET lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateLastSyncedAt(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM quests WHERE isCompleted = 1")
    fun getCompletedQuestCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM quests WHERE type = :type AND isCompleted = 1")
    fun getCompletedQuestCountByType(type: String): Flow<Int>
}
