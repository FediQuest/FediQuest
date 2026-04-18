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
    
    @Query("SELECT * FROM quests WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveQuests(): Flow<List<QuestEntity>>
    
    @Query("SELECT * FROM quests WHERE id = :questId")
    suspend fun getQuestById(questId: String): QuestEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity)
    
    @Query("UPDATE quests SET isCompleted = 1, fediverseSynced = 0 WHERE id = :questId")
    suspend fun markQuestCompleted(questId: String)
    
    @Query("SELECT * FROM quests WHERE fediverseSynced = 0 AND isCompleted = 1")
    suspend fun getUnsyncedCompletions(): List<QuestEntity>
}
