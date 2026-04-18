// File: app/src/main/java/com/fediquest/app/data/repositories/QuestRepository.kt
package com.fediquest.app.data.repositories

import com.fediquest.app.data.local.QuestDao
import com.fediquest.app.data.models.Quest
import com.fediquest.app.data.models.QuestStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Repository for quest data operations.
 * Abstracts data sources (local DB, remote API).
 */
class QuestRepository(
    private val questDao: QuestDao,
    private val retrofit: Retrofit
) {
    private val apiService = retrofit.create(QuestApiService::class.java)

    /**
     * Get all available quests from local database.
     */
    fun getAvailableQuests(): Flow<List<Quest>> = flow {
        emit(questDao.getQuestsByStatus(QuestStatus.AVAILABLE))
    }

    /**
     * Get quests within geographic bounds.
     */
    suspend fun getQuestsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<Quest> {
        return questDao.getQuestsInBounds(minLat, maxLat, minLng, maxLng)
    }

    /**
     * Get a specific quest by ID.
     */
    suspend fun getQuestById(id: String): Quest? {
        return questDao.getQuestById(id)
    }

    /**
     * Insert or update a quest.
     */
    suspend fun saveQuest(quest: Quest) {
        questDao.insertQuest(quest)
    }

    /**
     * Update quest status.
     */
    suspend fun updateQuestStatus(questId: String, status: QuestStatus) {
        val quest = getQuestById(questId) ?: return
        questDao.updateQuest(quest.copy(status = status))
    }

    /**
     * Fetch quests from remote API (mock implementation).
     */
    suspend fun fetchRemoteQuests(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 10.0
    ): Result<List<Quest>> {
        return try {
            // In production, this would call the actual API
            // For now, return mock data
            val mockQuests = generateMockQuests(latitude, longitude)
            questDao.insertQuests(mockQuests)
            Result.success(mockQuests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate mock quests for development/testing.
     */
    private fun generateMockQuests(centerLat: Double, centerLng: Double): List<Quest> {
        return listOf(
            Quest(
                id = "quest_1",
                title = "Park Cleanup Challenge",
                description = "Help clean up litter in the local park. Every piece counts!",
                category = com.fediquest.app.data.models.QuestCategory.CLEANUP,
                difficulty = com.fediquest.app.data.models.QuestDifficulty.EASY,
                latitude = centerLat + 0.001,
                longitude = centerLng + 0.001,
                radiusMeters = 50f,
                xpReward = 100,
                coinReward = 50,
                validationMode = com.fediquest.app.data.models.ValidationMode.AI_AUTO
            ),
            Quest(
                id = "quest_2",
                title = "Tree Planting Mission",
                description = "Plant a native tree species in your community.",
                category = com.fediquest.app.data.models.QuestCategory.PLANTING,
                difficulty = com.fediquest.app.data.models.QuestDifficulty.MEDIUM,
                latitude = centerLat - 0.002,
                longitude = centerLng + 0.001,
                radiusMeters = 100f,
                xpReward = 250,
                coinReward = 120,
                validationMode = com.fediquest.app.data.models.ValidationMode.HYBRID
            ),
            Quest(
                id = "quest_3",
                title = "Recycling Hero",
                description = "Collect and properly sort recyclable materials.",
                category = com.fediquest.app.data.models.QuestCategory.RECYCLE,
                difficulty = com.fediquest.app.data.models.QuestDifficulty.EASY,
                latitude = centerLat + 0.001,
                longitude = centerLng - 0.002,
                radiusMeters = 75f,
                xpReward = 150,
                coinReward = 75,
                validationMode = com.fediquest.app.data.models.ValidationMode.AI_AUTO
            )
        )
    }
}

/**
 * Retrofit API service for quest endpoints.
 */
interface QuestApiService {
    @GET("api/v1/quests")
    suspend fun getQuests(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radiusKm: Double
    ): List<Quest>
}
