// File: app/src/main/java/com/fediquest/app/domain/usecases/CalculateXPUseCase.kt
package com.fediquest.app.domain.usecases

import com.fediquest.app.data.models.Companion
import com.fediquest.app.data.models.Quest
import com.fediquest.app.data.models.QuestCategory
import com.fediquest.app.util.XPManager

/**
 * Use case for calculating XP rewards from quest completion.
 * Handles base XP, difficulty multipliers, and companion synergy bonuses.
 */
class CalculateXPUseCase(
    private val xpManager: XPManager = com.fediquest.app.util.XPManager
) {
    /**
     * Calculate total XP reward for completing a quest.
     * 
     * @param quest The completed quest
     * @param companion Optional equipped companion for synergy bonus
     * @return Total XP earned (avatar XP and companion XP)
     */
    operator fun invoke(
        quest: Quest,
        companion: Companion? = null
    ): XPResult {
        // Base XP from quest
        var avatarXP = quest.xpReward
        
        // Apply difficulty multiplier
        avatarXP = (avatarXP * quest.difficulty.multiplier).toInt()
        
        // Calculate companion XP and synergy bonus
        var companionXP = 0
        var synergyMultiplier = 1.0f
        
        if (companion != null) {
            // Check for type-category synergy
            synergyMultiplier = xpManager.getSynergyMultiplier(companion.species, quest.category)
            
            // Companion gets 50% of base XP
            companionXP = (quest.xpReward * 0.5 * synergyMultiplier).toInt()
        }
        
        // Apply synergy to avatar XP as well
        avatarXP = (avatarXP * synergyMultiplier).toInt()
        
        return XPResult(
            avatarXP = avatarXP,
            companionXP = companionXP,
            synergyApplied = synergyMultiplier > 1.0f,
            synergyMultiplier = synergyMultiplier
        )
    }
    
    /**
     * Calculate XP needed to reach a specific level.
     */
    fun getXPForLevel(level: Int): Int {
        return xpManager.calculateXPForLevel(level)
    }
    
    /**
     * Calculate current level based on total XP.
     */
    fun getLevelFromXP(totalXP: Int): Int {
        var level = 1
        var remainingXP = totalXP
        var xpNeeded = getXPForLevel(level)
        
        while (remainingXP >= xpNeeded) {
            remainingXP -= xpNeeded
            level++
            xpNeeded = getXPForLevel(level)
        }
        
        return level
    }
}

/**
 * Result of XP calculation.
 */
data class XPResult(
    val avatarXP: Int,
    val companionXP: Int,
    val synergyApplied: Boolean,
    val synergyMultiplier: Float
)

// File: app/src/main/java/com/fediquest/app/domain/usecases/ValidateQuestUseCase.kt
package com.fediquest.app.domain.usecases

import com.fediquest.app.data.models.Quest
import com.fediquest.app.data.models.ValidationMode
import com.fediquest.app.data.repositories.QuestRepository
import com.fediquest.app.data.repositories.UserRepository
import com.fediquest.app.util.XPManager

/**
 * Use case for validating quest completion.
 * Handles different validation modes (AI, community, hybrid, GPS-only).
 */
class ValidateQuestUseCase(
    private val questRepository: QuestRepository,
    private val xpManager: XPManager = XPManager
) {
    /**
     * Validate quest completion based on validation mode.
     * 
     * @param questId ID of the quest to validate
     * @param proofImageUrl URL or path to proof image
     * @param userLatitude User's current latitude
     * @param userLongitude User's current longitude
     * @return ValidationResult with success status and rewards
     */
    suspend operator fun invoke(
        questId: String,
        proofImageUrl: String? = null,
        userLatitude: Double,
        userLongitude: Double
    ): ValidationResult {
        val quest = questRepository.getQuestById(questId)
            ?: return ValidationResult(false, "Quest not found")
        
        // Check if user is within quest radius
        val distance = calculateDistance(
            userLatitude, userLongitude,
            quest.latitude, quest.longitude
        )
        
        if (distance > quest.radiusMeters) {
            return ValidationResult(
                success = false,
                message = "You are too far from the quest location. Distance: ${distance.toInt()}m, Required: ${quest.radiusMeters.toInt()}m"
            )
        }
        
        // Validate based on mode
        return when (quest.validationMode) {
            ValidationMode.AI_AUTO -> validateWithAI(quest, proofImageUrl)
            ValidationMode.COMMUNITY -> submitForCommunityReview(quest, proofImageUrl)
            ValidationMode.HYBRID -> validateHybrid(quest, proofImageUrl)
            ValidationMode.GPS_ONLY -> ValidationResult(
                success = true,
                message = "Location verified! Quest completed."
            )
        }
    }
    
    private suspend fun validateWithAI(quest: Quest, proofImageUrl: String?): ValidationResult {
        // TODO: Integrate TFLite object detection
        // For now, assume success if image is provided
        return if (proofImageUrl != null) {
            ValidationResult(
                success = true,
                message = "AI verification successful!",
                xpReward = quest.xpReward,
                coinReward = quest.coinReward
            )
        } else {
            ValidationResult(false, "Proof image required for AI validation")
        }
    }
    
    private suspend fun submitForCommunityReview(quest: Quest, proofImageUrl: String?): ValidationResult {
        // TODO: Submit to Fediverse for community voting
        questRepository.updateQuestStatus(quest.id, com.fediquest.app.data.models.QuestStatus.PENDING_VALIDATION)
        
        return ValidationResult(
            success = false,
            message = "Submitted for community review. You'll be notified when approved.",
            isPending = true
        )
    }
    
    private suspend fun validateHybrid(quest: Quest, proofImageUrl: String?): ValidationResult {
        // AI validation first, then community spot-check
        val aiResult = validateWithAI(quest, proofImageUrl)
        if (!aiResult.success) return aiResult
        
        // 10% chance of community review for quality control
        val needsReview = Math.random() < 0.1
        if (needsReview) {
            return submitForCommunityReview(quest, proofImageUrl)
        }
        
        return aiResult
    }
    
    /**
     * Calculate distance between two coordinates in meters.
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }
}

/**
 * Result of quest validation.
 */
data class ValidationResult(
    val success: Boolean,
    val message: String,
    val xpReward: Int = 0,
    val coinReward: Int = 0,
    val isPending: Boolean = false
)

// File: app/src/main/java/com/fediquest/app/domain/usecases/PostToFediverseUseCase.kt
package com.fediquest.app.domain.usecases

import com.fediquest.app.data.models.FediversePost
import com.fediquest.app.data.models.Quest
import com.fediquest.app.util.FediverseClient

/**
 * Use case for posting quest completions to the Fediverse.
 * Creates ActivityPub Note objects with quest details.
 */
class PostToFediverseUseCase(
    private val fediverseClient: FediverseClient = FediverseClient
) {
    /**
     * Post quest completion to Fediverse.
     * 
     * @param quest The completed quest
     * @param imageUrl Optional proof image URL
     * @param visibility Post visibility (default: public)
     * @return Result with post ID or error message
     */
    suspend operator fun invoke(
        quest: Quest,
        imageUrl: String? = null,
        visibility: com.fediquest.app.data.models.Visibility = com.fediquest.app.data.models.Visibility.PUBLIC
    ): Result<String> {
        return try {
            val content = buildPostContent(quest)
            val tags = buildTags(quest)
            
            val post = FediversePost(
                id = "quest_${quest.id}_${System.currentTimeMillis()}",
                type = com.fediquest.app.data.models.PostType.NOTE,
                content = content,
                questId = quest.id,
                imageUrl = imageUrl,
                tags = tags,
                visibility = visibility
            )
            
            val postId = fediverseClient.postNote(post)
            Result.success(postId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildPostContent(quest: Quest): String {
        return "🌿 Just completed the \"${quest.title}\" quest in FediQuest! " +
               "${quest.category.icon} #EcoQuest #${quest.category.name}"
    }
    
    private fun buildTags(quest: Quest): List<String> {
        return listOf(
            "FediQuest",
            "EcoQuest",
            quest.category.name,
            quest.difficulty.name
        )
    }
}

// File: app/src/main/java/com/fediquest/app/domain/models/QuestValidation.kt
package com.fediquest.app.domain.models

/**
 * Domain model for quest validation result.
 * Used across use cases and repositories.
 */
data class QuestValidationResult(
    val questId: String,
    val isValid: Boolean,
    val validationMethod: ValidationMethod,
    val confidenceScore: Float = 1.0f,
    val detectedObjects: List<DetectedObject> = emptyList(),
    val gpsVerified: Boolean = false,
    val communityVotes: CommunityVotes? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ValidationMethod {
    AI_AUTO,
    COMMUNITY,
    HYBRID,
    GPS_ONLY,
    MANUAL
}

data class DetectedObject(
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox? = null
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class CommunityVotes(
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val totalVoters: Int = 0
) {
    val approvalRatio: Float = 
        if (totalVoters == 0) 0f else upvotes.toFloat() / totalVoters
}
