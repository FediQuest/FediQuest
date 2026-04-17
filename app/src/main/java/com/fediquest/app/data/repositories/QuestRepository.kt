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

// File: app/src/main/java/com/fediquest/app/data/repositories/AvatarRepository.kt
package com.fediquest.app.data.repositories

import com.fediquest.app.data.local.AvatarDao
import com.fediquest.app.data.models.Avatar
import com.fediquest.app.data.models.AvatarCategory

/**
 * Repository for avatar data operations.
 */
class AvatarRepository(
    private val avatarDao: AvatarDao
) {
    /**
     * Get all avatars.
     */
    suspend fun getAllAvatars(): List<Avatar> {
        return avatarDao.getAllAvatars()
    }

    /**
     * Get avatars by category.
     */
    suspend fun getAvatarsByCategory(category: AvatarCategory): List<Avatar> {
        return avatarDao.getAvatarsByCategory(category)
    }

    /**
     * Get currently equipped avatar.
     */
    suspend fun getEquippedAvatar(): Avatar? {
        return avatarDao.getEquippedAvatar()
    }

    /**
     * Save an avatar.
     */
    suspend fun saveAvatar(avatar: Avatar) {
        avatarDao.insertAvatar(avatar)
    }

    /**
     * Equip an avatar part.
     */
    suspend fun equipAvatar(avatarId: String) {
        val avatar = getAllAvatars().find { it.id == avatarId } ?: return
        
        // Unequip current avatar in same category
        val currentEquipped = getAvatarsByCategory(avatar.category).find { it.isEquipped }
        currentEquipped?.let {
            avatarDao.updateAvatar(it.copy(isEquipped = false))
        }
        
        // Equip new avatar
        avatarDao.updateAvatar(avatar.copy(isEquipped = true))
    }

    /**
     * Unlock an avatar.
     */
    suspend fun unlockAvatar(avatarId: String) {
        val avatar = getAllAvatars().find { it.id == avatarId } ?: return
        avatarDao.updateAvatar(avatar.copy(isUnlocked = true))
    }
}

// File: app/src/main/java/com/fediquest/app/data/repositories/CompanionRepository.kt
package com.fediquest.app.data.repositories

import com.fediquest.app.data.local.CompanionDao
import com.fediquest.app.data.models.Companion

/**
 * Repository for companion data operations.
 */
class CompanionRepository(
    private val companionDao: CompanionDao
) {
    /**
     * Get all companions.
     */
    suspend fun getAllCompanions(): List<Companion> {
        return companionDao.getAllCompanions()
    }

    /**
     * Get a specific companion by ID.
     */
    suspend fun getCompanionById(id: String): Companion? {
        return companionDao.getCompanionById(id)
    }

    /**
     * Get currently equipped companion.
     */
    suspend fun getEquippedCompanion(): Companion? {
        return companionDao.getEquippedCompanion()
    }

    /**
     * Save a companion.
     */
    suspend fun saveCompanion(companion: Companion) {
        companionDao.insertCompanion(companion)
    }

    /**
     * Update companion stats.
     */
    suspend fun updateCompanionStats(
        companionId: String,
        xp: Int? = null,
        happiness: Int? = null,
        energy: Int? = null
    ) {
        val companion = getCompanionById(companionId) ?: return
        
        val updated = companion.copy(
            currentXP = xp ?: companion.currentXP,
            happiness = happiness ?: companion.happiness,
            energy = energy ?: companion.energy,
            lastInteractionAt = System.currentTimeMillis()
        )
        
        companionDao.updateCompanion(updated)
    }

    /**
     * Equip a companion.
     */
    suspend fun equipCompanion(companionId: String) {
        val companion = getCompanionById(companionId) ?: return
        
        // Unequip current companion
        getEquippedCompanion()?.let {
            companionDao.updateCompanion(it.copy(isEquipped = false))
        }
        
        // Equip new companion
        companionDao.updateCompanion(companion.copy(isEquipped = true))
    }
}

// File: app/src/main/java/com/fediquest/app/data/repositories/UserRepository.kt
package com.fediquest.app.data.repositories

import com.fediquest.app.data.local.UserDao
import com.fediquest.app.data.models.User

/**
 * Repository for user data operations.
 */
class UserRepository(
    private val userDao: UserDao
) {
    /**
     * Get current user.
     */
    suspend fun getCurrentUser(): User? {
        return userDao.getUser("current_user")
    }

    /**
     * Create or update user.
     */
    suspend fun saveUser(user: User) {
        userDao.insertUser(user)
    }

    /**
     * Update user XP and level.
     * Optimized to reduce database calls and use centralized XP calculation.
     */
    suspend fun updateUserXP(userId: String, xpGained: Int) {
        val user = getCurrentUser() ?: return
        
        var newXP = user.currentXP + xpGained
        var newLevel = user.level
        var xpToNext = user.xpToNextLevel
        
        // Level up logic - using optimized XPManager calculation
        while (newXP >= xpToNext) {
            newXP -= xpToNext
            newLevel++
            xpToNext = com.fediquest.app.util.XPManager.calculateXPForLevel(newLevel)
        }
        
        userDao.updateUser(
            user.copy(
                currentXP = newXP,
                level = newLevel,
                xpToNextLevel = xpToNext
            )
        )
    }

    /**
     * Add coins to user balance.
     */
    suspend fun addCoins(userId: String, amount: Int) {
        val user = getCurrentUser() ?: return
        userDao.updateUser(user.copy(totalCoins = user.totalCoins + amount))
    }

    /**
     * Add badge to user.
     * Checks for duplicates before adding.
     */
    suspend fun addBadge(userId: String, badgeId: String) {
        val user = getCurrentUser() ?: return
        if (badgeId !in user.badges) {
            userDao.updateUser(user.copy(badges = user.badges + badgeId))
        }
    }

    /**
     * Increment quests completed counter.
     */
    suspend fun incrementQuestsCompleted(userId: String) {
        val user = getCurrentUser() ?: return
        userDao.updateUser(user.copy(questsCompleted = user.questsCompleted + 1))
    }
}
