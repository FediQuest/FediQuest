package org.fediquest.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.fediquest.data.entity.QuestEntity

@Dao
interface QuestDao {

    @Query("SELECT * FROM quests ORDER BY createdAt DESC")
    fun getAllQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE questId = :questId LIMIT 1")
    suspend fun getQuestById(questId: String): QuestEntity?

    @Query("SELECT * FROM quests WHERE questId = :questId LIMIT 1")
    fun getQuestByIdFlow(questId: String): Flow<QuestEntity?>

    @Query("SELECT * FROM quests WHERE locationLatitude BETWEEN :minLat AND :maxLat AND locationLongitude BETWEEN :minLng AND :maxLng")
    fun getQuestsInArea(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<QuestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuests(quests: List<QuestEntity>)

    @Update
    suspend fun updateQuest(quest: QuestEntity)

    @Delete
    suspend fun deleteQuest(quest: QuestEntity)

    @Query("DELETE FROM quests WHERE questId = :questId")
    suspend fun deleteQuestById(questId: String)

    @Query("UPDATE quests SET isCompleted = 1, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun completeQuest(id: Long, completedAt: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM quests WHERE isCompleted = 0")
    fun getActiveQuestCount(): Flow<Int>

    @Query("SELECT * FROM quests WHERE syncedAt IS NULL OR syncedAt < updatedAt LIMIT 10")
    suspend fun getUnsyncedQuests(): List<QuestEntity>

    @Query("UPDATE quests SET syncedAt = :timestamp WHERE id = :id")
    suspend fun markQuestSynced(id: Long, timestamp: Long)
}
