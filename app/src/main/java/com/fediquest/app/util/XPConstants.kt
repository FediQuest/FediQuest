// File: app/src/main/java/com/fediquest/app/util/XPConstants.kt
package com.fediquest.app.util

/**
 * Centralized constants for XP calculations.
 * Extracted from XPManager for better maintainability and testability.
 */
object XPConstants {
    
    // Base XP values
    const val BASE_XP_FOR_LEVEL_1 = 100
    const val LEVEL_GROWTH_FACTOR = 1.25
    
    // Synergy bonus
    const val SYNERGY_MULTIPLIER = 1.5f
    const val COMPANION_XP_RATIO = 0.5f
    
    // Evolution thresholds
    const val FIRST_EVOLUTION_LEVEL = 10
    const val SECOND_EVOLUTION_LEVEL = 30
    const val MAX_EVOLUTION_STAGE = 3
    
    // Decay and regeneration rates (in minutes)
    const val HAPPINESS_DECAY_INTERVAL_MINUTES = 30L
    const val ENERGY_REGEN_INTERVAL_MINUTES = 5L
    const val MAX_DECAY_POINTS = 100
    
    // Badge thresholds
    object QuestBadges {
        const val BEGINNER = 1
        const val COMMITTED = 5
        const val DEDICATED = 10
        const val EXPERIENCED = 25
        const val VETERAN = 50
        const val MASTER = 100
        
        const val ID_BEGINNER = "badge_beginner_eco_warrior"
        const val ID_COMMITTED = "badge_committed_eco_warrior"
        const val ID_DEDICATED = "badge_dedicated_eco_warrior"
        const val ID_EXPERIENCED = "badge_experienced_eco_warrior"
        const val ID_VETERAN = "badge_veteran_eco_warrior"
        const val ID_MASTER = "badge_master_eco_warrior"
    }
    
    object LevelBadges {
        const val COMMON = 5
        const val UNCOMMON = 10
        const val RARE = 20
        const val EPIC = 30
        const val LEGENDARY = 50
        
        const val ID_COMMON = "badge_common_eco_hero"
        const val ID_UNCOMMON = "badge_uncommon_eco_hero"
        const val ID_RARE = "badge_rare_eco_hero"
        const val ID_EPIC = "badge_epic_eco_hero"
        const val ID_LEGENDARY = "badge_legendary_eco_hero"
    }
}
