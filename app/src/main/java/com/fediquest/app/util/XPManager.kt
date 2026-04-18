// File: app/src/main/java/com/fediquest/app/util/XPManager.kt
package com.fediquest.app.util

import com.fediquest.app.data.models.Companion
import com.fediquest.app.data.models.CompanionSpecies
import com.fediquest.app.data.models.QuestCategory
import kotlin.math.pow

/**
 * Utility object for XP calculations and companion synergy logic.
 * Optimized with cached values and efficient lookups.
 */
object XPManager {

    // Precomputed synergy mappings for O(1) lookup
    private val synergyMap = mapOf(
        CompanionSpecies.ECO_DRAKE to setOf(QuestCategory.PLANTING, QuestCategory.NATURE),
        CompanionSpecies.GREEN_PHOENIX to setOf(QuestCategory.NATURE, QuestCategory.WILDLIFE),
        CompanionSpecies.FOREST_UNICORN to setOf(QuestCategory.NATURE, QuestCategory.PLANTING),
        CompanionSpecies.AQUA_TURTLE to setOf(QuestCategory.WATER, QuestCategory.CLEANUP),
        CompanionSpecies.SOLAR_LION to setOf(QuestCategory.RECYCLE, QuestCategory.CLEANUP)
    ).withDefault { emptySet() }

    /**
     * Get synergy multiplier for a companion-quest combination.
     * Returns 1.5f if there's synergy, 1.0f otherwise.
     * Optimized with Set.contains for O(1) lookup.
     */
    fun getSynergyMultiplier(companion: CompanionSpecies, questCategory: QuestCategory): Float {
        return if (synergyMap[companion]!!.contains(questCategory)) {
            XPConstants.SYNERGY_MULTIPLIER
        } else {
            1.0f
        }
    }

    /**
     * Calculate XP required to reach a specific level.
     * Uses exponential growth: base 100, increases by 25% per level.
     * Optimized with direct formula instead of iteration.
     */
    fun calculateXPForLevel(level: Int): Int {
        return when {
            level <= 1 -> XPConstants.BASE_XP_FOR_LEVEL_1
            else -> (XPConstants.BASE_XP_FOR_LEVEL_1 * 
                     XPConstants.LEVEL_GROWTH_FACTOR.pow(level - 1)).toInt()
        }
    }

    /**
     * Calculate total XP needed to reach a level from level 1.
     * Optimized using geometric series sum formula: a(r^n - 1)/(r - 1)
     */
    fun getTotalXPForLevel(level: Int): Int {
        if (level <= 1) return 0
        
        val a = XPConstants.BASE_XP_FOR_LEVEL_1.toDouble()
        val r = XPConstants.LEVEL_GROWTH_FACTOR
        val n = level - 1
        
        // Sum of geometric series: a * (r^n - 1) / (r - 1)
        return (a * (r.pow(n) - 1) / (r - 1)).toInt()
    }

    /**
     * Calculate companion evolution requirements.
     * Returns the level at which the companion evolves to the next stage.
     */
    fun getEvolutionLevel(evolutionStage: Int): Int {
        return when (evolutionStage) {
            1 -> XPConstants.FIRST_EVOLUTION_LEVEL
            2 -> XPConstants.SECOND_EVOLUTION_LEVEL
            else -> Int.MAX_VALUE
        }
    }

    /**
     * Calculate happiness decay over time.
     * Companions lose 1 happiness point every 30 minutes of inactivity.
     */
    fun calculateHappinessDecay(lastInteractionAt: Long, currentTime: Long = System.currentTimeMillis()): Int {
        val minutesSinceInteraction = (currentTime - lastInteractionAt) / (1000 * 60)
        return (minutesSinceInteraction / XPConstants.HAPPINESS_DECAY_INTERVAL_MINUTES)
            .toInt()
            .coerceIn(0, XPConstants.MAX_DECAY_POINTS)
    }

    /**
     * Calculate energy regeneration.
     * Companions regain 1 energy point every 5 minutes of rest.
     */
    fun calculateEnergyRegain(lastRestAt: Long, currentTime: Long = System.currentTimeMillis()): Int {
        val minutesSinceRest = (currentTime - lastRestAt) / (1000 * 60)
        return (minutesSinceRest / XPConstants.ENERGY_REGEN_INTERVAL_MINUTES)
            .toInt()
            .coerceIn(0, XPConstants.MAX_DECAY_POINTS)
    }

    /**
     * Check if a companion can evolve based on level and current stage.
     */
    fun canEvolve(companion: Companion): Boolean {
        return companion.evolutionStage < XPConstants.MAX_EVOLUTION_STAGE &&
               companion.level >= getEvolutionLevel(companion.evolutionStage)
    }

    /**
     * Get badge ID for completing a certain number of quests.
     * Optimized with early returns and constant references.
     */
    fun getBadgeForQuestCount(count: Int): String? {
        return when {
            count >= XPConstants.QuestBadges.MASTER -> XPConstants.QuestBadges.ID_MASTER
            count >= XPConstants.QuestBadges.VETERAN -> XPConstants.QuestBadges.ID_VETERAN
            count >= XPConstants.QuestBadges.EXPERIENCED -> XPConstants.QuestBadges.ID_EXPERIENCED
            count >= XPConstants.QuestBadges.DEDICATED -> XPConstants.QuestBadges.ID_DEDICATED
            count >= XPConstants.QuestBadges.COMMITTED -> XPConstants.QuestBadges.ID_COMMITTED
            count >= XPConstants.QuestBadges.BEGINNER -> XPConstants.QuestBadges.ID_BEGINNER
            else -> null
        }
    }

    /**
     * Get badge ID for reaching a specific level.
     * Optimized with early returns and constant references.
     */
    fun getBadgeForLevel(level: Int): String? {
        return when {
            level >= XPConstants.LevelBadges.LEGENDARY -> XPConstants.LevelBadges.ID_LEGENDARY
            level >= XPConstants.LevelBadges.EPIC -> XPConstants.LevelBadges.ID_EPIC
            level >= XPConstants.LevelBadges.RARE -> XPConstants.LevelBadges.ID_RARE
            level >= XPConstants.LevelBadges.UNCOMMON -> XPConstants.LevelBadges.ID_UNCOMMON
            level >= XPConstants.LevelBadges.COMMON -> XPConstants.LevelBadges.ID_COMMON
            else -> null
        }
    }
}
