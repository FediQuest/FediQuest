// File: app/src/main/java/org/fediquest/data/repository/QuestRepository.kt
package org.fediquest.data.repository

import kotlinx.coroutines.flow.Flow
import org.fediquest.data.dao.QuestDao
import org.fediquest.data.entity.QuestEntity
import org.fediquest.data.entity.QuestType

/**
 * Repository for Quest data operations
 * Handles local storage and deferred sync to Fediverse
 */
class QuestRepository(private val questDao: QuestDao) {
    
    fun getActiveQuests(): Flow<List<QuestEntity>> {
        return questDao.getActiveQuests()
    }

    suspend fun getQuestById(questId: String): QuestEntity? {
        return questDao.getQuestById(questId)
    }

    fun getQuestsByType(type: QuestType): Flow<List<QuestEntity>> {
        return questDao.getQuestsByType(type)
    }

    fun getCompletedQuests(): Flow<List<QuestEntity>> {
        return questDao.getCompletedQuests()
    }

    suspend fun insertQuest(quest: QuestEntity) {
        questDao.insertQuest(quest)
    }

    suspend fun updateQuest(quest: QuestEntity) {
        questDao.updateQuest(quest)
    }

    suspend fun completeQuest(questId: String) {
        questDao.completeQuest(questId)
    }

    suspend fun deleteQuest(quest: QuestEntity) {
        questDao.deleteQuest(quest)
    }
}
