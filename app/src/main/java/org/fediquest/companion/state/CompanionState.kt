// File: app/src/main/java/org/fediquest/companion/state/CompanionState.kt
package org.fediquest.companion.state

/**
 * Companion State Machine
 * 
 * Defines the evolution stages and state transitions for companions.
 * Companions evolve based on bond level, quest completions, and player level.
 * 
 * All companion data is stored locally (offline-first).
 */

/**
 * Evolution stage of a companion
 */
enum class EvolutionStage(val displayName: String, val minBondLevel: Int) {
    EGG("Egg", 0),
    HATCHLING("Hatchling", 10),
    YOUNG("Young", 25),
    ADULT("Adult", 50),
    MATURE("Mature", 75),
    ELDER("Elder", 90),
    LEGENDARY("Legendary", 100)
}

/**
 * Companion mood states
 */
enum class CompanionMood(val displayName: String, val emoji: String) {
    HAPPY("Happy", "😊"),
    EXCITED("Excited", "🤩"),
    CONTENT("Content", "😌"),
    TIRED("Tired", "😴"),
    HUNGRY("Hungry", "😋"),
    SAD("Sad", "😢"),
    SLEEPING("Sleeping", "💤")
}

/**
 * Companion personality traits (affects behavior and bonuses)
 */
enum class CompanionPersonality(val displayName: String, val bonus: String) {
    ENTHUSIASTIC("Enthusiastic", "+5% XP from planting quests"),
    CURIOUS("Curious", "Finds rare items more often"),
    LAZY("Lazy", "Restores bond faster"),
    ADVENTUROUS("Adventurous", "+10% XP from exploration"),
    CARING("Caring", "+5% XP from helping quests"),
    WISE("Wise", "+15% total XP bonus at elder stage")
}

/**
 * Companion state data class
 */
data class CompanionState(
    val companionId: String,
    val name: String,
    val baseEmoji: String,
    var currentStage: EvolutionStage = EvolutionStage.EGG,
    var bondLevel: Int = 0,
    var experience: Int = 0,
    var mood: CompanionMood = CompanionMood.CONTENT,
    val personality: CompanionPersonality,
    var lastInteractionTime: Long = System.currentTimeMillis(),
    var totalQuestsCompleted: Int = 0,
    var evolutionCount: Int = 0,
    val unlockedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if companion can evolve to next stage
     */
    fun canEvolve(): Boolean {
        val nextStageIndex = currentStage.ordinal + 1
        return if (nextStageIndex < EvolutionStage.values().size) {
            val nextStage = EvolutionStage.values()[nextStageIndex]
            bondLevel >= nextStage.minBondLevel
        } else {
            false
        }
    }
    
    /**
     * Get next evolution stage
     */
    fun getNextStage(): EvolutionStage? {
        val nextStageIndex = currentStage.ordinal + 1
        return if (nextStageIndex < EvolutionStage.values().size) {
            EvolutionStage.values()[nextStageIndex]
        } else {
            null
        }
    }
    
    /**
     * Evolve companion to next stage
     * Returns true if evolution was successful
     */
    fun evolve(): Boolean {
        if (!canEvolve()) return false
        
        val nextStage = getNextStage() ?: return false
        
        val previousStage = currentStage
        currentStage = nextStage
        evolutionCount++
        
        // Evolution bonus
        experience += 50
        bondLevel = kotlin.math.min(100, bondLevel + 5)
        
        return true
    }
    
    /**
     * Add experience from quest completion
     */
    fun addQuestExperience(xpEarned: Int, questType: String) {
        experience += xpEarned
        totalQuestsCompleted++
        
        // Bond increase based on quest type and personality
        val bondIncrease = calculateBondIncrease(questType)
        bondLevel = kotlin.math.min(100, bondLevel + bondIncrease)
        
        // Update mood
        updateMood(bondIncrease)
        
        // Check for evolution
        if (canEvolve()) {
            evolve()
        }
        
        lastInteractionTime = System.currentTimeMillis()
    }
    
    /**
     * Calculate bond increase based on quest type and personality
     */
    private fun calculateBondIncrease(questType: String): Int {
        val baseIncrease = when (questType.lowercase()) {
            "planting" -> 3
            "recycling" -> 2
            "cleanup" -> 2
            "wildflower" -> 3
            "water" -> 2
            "wildlife" -> 3
            else -> 1
        }
        
        // Personality bonuses
        val bonus = when (personality) {
            CompanionPersonality.CARING -> if (questType.lowercase() in listOf("planting", "wildflower", "wildlife")) 1 else 0
            CompanionPersonality.ENTHUSIASTIC -> if (questType.lowercase() == "planting") 1 else 0
            CompanionPersonality.ADVENTUROUS -> if (questType.lowercase() == "wildlife") 1 else 0
            else -> 0
        }
        
        return baseIncrease + bonus
    }
    
    /**
     * Update mood based on recent activity
     */
    private fun updateMood(bondIncrease: Int) {
        mood = when {
            bondIncrease >= 3 -> CompanionMood.EXCITED
            bondIncrease >= 1 -> CompanionMood.HAPPY
            else -> CompanionMood.CONTENT
        }
    }
    
    /**
     * Interact with companion (pet, feed, play)
     */
    fun interact(interactionType: InteractionType) {
        val bondChange = when (interactionType) {
            InteractionType.PET -> 1
            InteractionType.FEED -> 2
            InteractionType.PLAY -> 3
            InteractionType.REST -> -5 // Decreases tiredness
        }
        
        bondLevel = kotlin.math.min(100, kotlin.math.max(0, bondLevel + bondChange))
        
        mood = when (interactionType) {
            InteractionType.PET -> CompanionMood.HAPPY
            InteractionType.FEED -> CompanionMood.CONTENT
            InteractionType.PLAY -> CompanionMood.EXCITED
            InteractionType.REST -> CompanionMood.SLEEPING
        }
        
        lastInteractionTime = System.currentTimeMillis()
    }
    
    /**
     * Get current display emoji based on stage and mood
     */
    fun getDisplayEmoji(): String {
        return when (mood) {
            CompanionMood.SLEEPING -> "💤"
            CompanionMood.TIRED -> "😴"
            CompanionMood.SAD -> "😢"
            else -> {
                val stageEmoji = when (currentStage) {
                    EvolutionStage.EGG -> "🥚"
                    EvolutionStage.HATCHLING -> "🐣"
                    EvolutionStage.YOUNG -> "🐥"
                    EvolutionStage.ADULT -> baseEmoji
                    EvolutionStage.MATURE -> "⭐$baseEmoji"
                    EvolutionStage.ELDER -> "🌟$baseEmoji"
                    EvolutionStage.LEGENDARY -> "✨$baseEmoji✨"
                }
                stageEmoji
            }
        }
    }
    
    /**
     * Get progress to next evolution (0.0 - 1.0)
     */
    fun getEvolutionProgress(): Float {
        val nextStage = getNextStage() ?: return 1.0f
        val minBondForNext = nextStage.minBondLevel
        val currentStageMinBond = currentStage.minBondLevel
        
        val range = minBondForNext - currentStageMinBond
        val progress = bondLevel - currentStageMinBond
        
        return if (range > 0) {
            progress.toFloat() / range.toFloat()
        } else {
            1.0f
        }
    }
}

/**
 * Interaction types for companion
 */
enum class InteractionType {
    PET,
    FEED,
    PLAY,
    REST
}

/**
 * Companion evolution event (for tracking and ActivityPub sharing)
 */
data class CompanionEvolutionRecord(
    val companionId: String,
    val companionName: String,
    val previousStage: EvolutionStage,
    val newStage: EvolutionStage,
    val bondLevel: Int,
    val timestamp: Long = System.currentTimeMillis()
)
