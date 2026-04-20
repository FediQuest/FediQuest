// File: app/src/main/java/org/fediquest/data/dao/QuestDao.kt
package org.fediquest.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.fediquest.data.entity.QuestEntity
import org.fediquest.data.entity.QuestType

/**
 * Data Access Object for Quests
 */
@Dao
interface QuestDao {
    @Query("SELECT * FROM quests WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :questId")
    suspend fun getQuestById(questId: String): QuestEntity?

    @Query("SELECT * FROM quests WHERE type = :type AND isCompleted = 0")
    fun getQuestsByType(type: QuestType): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedQuests(): Flow<List<QuestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity)

    @Update
    suspend fun updateQuest(quest: QuestEntity)

    @Query("UPDATE quests SET isCompleted = 1, completedAt = strftime('%s', 'now') * 1000 WHERE id = :questId")
    suspend fun completeQuest(questId: String)

    @Delete
    suspend fun deleteQuest(quest: QuestEntity)
}
