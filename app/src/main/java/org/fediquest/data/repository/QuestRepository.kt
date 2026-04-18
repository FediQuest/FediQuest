// File: app/src/main/java/org/fediquest/data/repository/QuestRepository.kt
package org.fediquest.data.repository

import kotlinx.coroutines.flow.Flow
import org.fediquest.data.dao.QuestDao
import org.fediquest.data.entity.QuestEntity

/**
 * Quest Repository - Single Source of Truth for Quest Data
 * 
 * Provides a clean API for quest operations, abstracting the data layer.
 * All operations work offline-first with local Room database.
 */
class QuestRepository(private val dao: QuestDao) {
    
    /**
     * Flow of active (incomplete) quests
     * Observers will be notified of any changes
     */
    val activeQuests: Flow<List<QuestEntity>> = dao.getActiveQuests()
    
    /**
     * Get a specific quest by ID
     */
    suspend fun getQuestById(questId: String): QuestEntity? {
        return dao.getQuestById(questId)
    }
    
    /**
     * Insert or update a quest
     */
    suspend fun insertQuest(quest: QuestEntity) {
        dao.insertQuest(quest)
    }
    
    /**
     * Mark a quest as completed
     * This persists the completion locally immediately (offline-first)
     * 
     * TODO: Trigger XP calculation + companion evolution check
     * TODO: Queue for ActivityPub sync if opted-in
     */
    suspend fun completeQuest(questId: String) {
        dao.markQuestCompleted(questId)
        // XP calculation and companion evolution will be handled by the caller
        // ActivityPub sync happens asynchronously via WorkManager
    }
    
    /**
     * Get quests that are completed but not yet synced to Fediverse
     */
    suspend fun getUnsyncedCompletions(): List<QuestEntity> {
        return dao.getUnsyncedCompletions()
    }
    
    /**
     * Mark a quest as synced to Fediverse
     * Call this after successful ActivityPub post
     */
    suspend fun markAsSynced(questId: String) {
        // Note: This would require an additional DAO method
        // For now, this is a placeholder for future implementation
    }
    
    /**
     * Sync completed quest to Fediverse
     * 
     * TODO: Call ActivityPubClient.postCompletion()
     * TODO: Update fediverseSynced = true on success
     */
    suspend fun syncToFediverse(questId: String) {
        val quest = getQuestById(questId) ?: return
        
        // This will be implemented when ActivityPub client is integrated
        // ActivityPubClient.postCompletion(quest)
        // On success: dao.markAsSynced(questId)
    }
}
