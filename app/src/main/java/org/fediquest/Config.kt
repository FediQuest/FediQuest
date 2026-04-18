// File: app/src/main/java/org/fediquest/Config.kt
package org.fediquest

/**
 * FediQuest Configuration Constants
 * 
 * This file contains all configuration constants for the FediQuest app.
 * Update these values as needed for your deployment.
 */
object Config {

    // =============================================================================
    // Server Configuration
    // =============================================================================

    /**
     * Base URL for spawn data server
     * 
     * For local development, use: http://localhost:8080/server/server.json
     * For production, replace with your hosted server.json URL
     * 
     * The server must support ETag headers for efficient caching.
     */
    const val SERVER_URL = "http://localhost:8080/server/server.json"

    /**
     * Connection timeout in seconds
     */
    const val CONNECTION_TIMEOUT_SECONDS = 10L

    /**
     * Read timeout in seconds
     */
    const val READ_TIMEOUT_SECONDS = 30L

    // =============================================================================
    // Model Paths (Placeholders)
    // =============================================================================

    /**
     * Base path for 3D models in assets directory
     * 
     * Place your glTF/GLB model files in:
     * app/src/main/assets/models/
     */
    const val ASSETS_MODEL_PATH = "models/"

    /**
     * Model file names for social-ecological quest types
     * 
     * These are placeholders - actual .glb files must be added to assets/models/
     * See README.md for model requirements and placement instructions.
     */
    object QuestModels {
        const val TREE = "tree.glb"                    // Planting quest
        const val RECYCLE_BIN = "recycle_bin.glb"      // Recycling quest
        const val CLEANUP_BAG = "cleanup_bag.glb"      // Cleanup quest
        const val WILDFLOWER = "wildflower.glb"        // Wildflower planting
        const val WATER_STATION = "water_station.glb"  // Water conservation
        const val BIRDHOUSE = "birdhouse.glb"          // Wildlife habitat
    }

    /**
     * Full asset paths for quest models
     */
    val MODEL_PATHS = mapOf(
        "planting" to "${ASSETS_MODEL_PATH}${QuestModels.TREE}",
        "recycling" to "${ASSETS_MODEL_PATH}${QuestModels.RECYCLE_BIN}",
        "cleanup" to "${ASSETS_MODEL_PATH}${QuestModels.CLEANUP_BAG}",
        "wildflower" to "${ASSETS_MODEL_PATH}${QuestModels.WILDFLOWER}",
        "water" to "${ASSETS_MODEL_PATH}${QuestModels.WATER_STATION}",
        "wildlife" to "${ASSETS_MODEL_PATH}${QuestModels.BIRDHOUSE}"
    )

    // =============================================================================
    // AR Configuration
    // =============================================================================

    /**
     * Default AR mode
     * 
     * Options:
     * - "ARTOOLKIT": Primary native AR (recommended, FOSS)
     * - "ARCORE": Optional alternative (requires Google Play Services)
     */
    const val DEFAULT_AR_MODE = "ARTOOLKIT"

    /**
     * Marker pattern path for ARToolKit
     * 
     * Place marker files in: app/src/main/assets/markers/
     */
    const val MARKER_PATH = "markers/"

    // =============================================================================
    // GPS Configuration
    // =============================================================================

    /**
     * GPS update interval in milliseconds
     */
    const val GPS_UPDATE_INTERVAL_MS = 10000L

    /**
     * Minimum distance change for GPS updates in meters
     */
    const val GPS_MIN_DISTANCE_METERS = 5f

    /**
     * Search radius for nearby spawns in meters
     */
    const val SPAWN_SEARCH_RADIUS_METERS = 100f

    // =============================================================================
    // Cache Configuration
    // =============================================================================

    /**
     * Cache expiration time in hours
     * 
     * After this duration, cached spawn data will be refreshed.
     */
    const val CACHE_EXPIRATION_HOURS = 24L

    /**
     * Enable debug logging
     * 
     * Set to false for production builds
     */
    const val DEBUG_LOGGING = true

    // =============================================================================
    // Social-Ecological Quest Types
    // =============================================================================

    /**
     * Available quest types for social-ecological activities
     * 
     * Each type has associated XP rewards and model assets.
     * These quests encourage people to go outside, help each other, 
     * and do something good for the environment.
     */
    enum class QuestType(val displayName: String, val baseXpReward: Int, val emoji: String) {
        PLANTING("Tree Planting", 100, "🌱"),
        RECYCLING("Recycling Station", 75, "♻️"),
        CLEANUP("Cleanup Zone", 50, "🧹"),
        WILDFLOWER("Wildflower Garden", 120, "🌸"),
        WATER("Water Conservation", 90, "💧"),
        WILDLIFE("Wildlife Habitat", 110, "🦋")
    }

    // =============================================================================
    // Avatar & Character Progression System
    // =============================================================================

    /**
     * Level titles for player progression
     * 
     * Players level up by completing quests and earning XP.
     * Higher levels unlock new avatar skins and companion options.
     */
    val LEVEL_TITLES = listOf(
        "Newcomer",       // Level 1
        "Helper",         // Level 2
        "Supporter",      // Level 3
        "Advocate",       // Level 4
        "Champion",       // Level 5
        "Guardian",       // Level 6
        "Protector",      // Level 7
        "Hero",           // Level 8
        "Legend",         // Level 9
        "Eco Warrior"     // Level 10+
    )

    /**
     * Avatar skin options (digital goods unlocked with XP)
     * 
     * Incentivize environmental action through cosmetic rewards.
     */
    data class AvatarSkin(val id: String, val name: String, val unlockLevel: Int, val emoji: String)
    
    val AVATAR_SKINS = listOf(
        AvatarSkin("default", "Default Outfit", 1, "👕"),
        AvatarSkin("explorer", "Nature Explorer", 3, "🥾"),
        AvatarSkin("scientist", "Eco Scientist", 5, "🔬"),
        AvatarSkin("gardener", "Master Gardener", 7, "👒"),
        AvatarSkin("guardian", "Earth Guardian", 10, "🛡️"),
        AvatarSkin("activist", "Climate Activist", 12, "📢"),
        AvatarSkin("legend", "Eco Legend", 15, "👑")
    )

    // =============================================================================
    // Companion System
    // =============================================================================

    /**
     * Companion creatures that accompany players on their journey
     * 
     * Companions provide encouragement, hints, and special bonuses.
     * Unlock new companions by reaching milestones and completing quest chains.
     */
    data class Companion(
        val id: String, 
        val name: String, 
        val emoji: String, 
        val unlockLevel: Int,
        val specialAbility: String,
        val description: String
    )
    
    val COMPANIONS = listOf(
        Companion(
            id = "bee",
            name = "Busy Bee",
            emoji = "🐝",
            unlockLevel = 2,
            specialAbility = "Highlights nearby flower planting spots",
            description = "A friendly bee who loves helping plant wildflowers!"
        ),
        Companion(
            id = "bird",
            name = "Song Bird",
            emoji = "🐦",
            unlockLevel = 4,
            specialAbility = "Alerts you to wildlife conservation areas",
            description = "This cheerful bird guides you to wildlife habitats."
        ),
        Companion(
            id = "fox",
            name = "Forest Fox",
            emoji = "🦊",
            unlockLevel = 6,
            specialAbility = "Finds hidden cleanup opportunities",
            description = "A clever fox that sniffs out areas needing cleanup."
        ),
        Companion(
            id = "turtle",
            name = "Sea Turtle",
            emoji = "🐢",
            unlockLevel = 8,
            specialAbility = "Guides to water conservation sites",
            description = "Wise turtle helps protect our waterways."
        ),
        Companion(
            id = "owl",
            name = "Wise Owl",
            emoji = "🦉",
            unlockLevel = 10,
            specialAbility = "Provides +10% XP bonus on all quests",
            description = "The wise owl shares knowledge and boosts your progress!"
        ),
        Companion(
            id = "butterfly",
            name = "Monarch Butterfly",
            emoji = "🦋",
            unlockLevel = 12,
            specialAbility = "Reveals rare quest locations",
            description = "Beautiful butterfly leads you to special environmental missions."
        )
    )

    // =============================================================================
    // Fediverse Integration
    // =============================================================================

    /**
     * Default Fediverse instance for social features
     * 
     * Users can configure their own instance in settings.
     * Supports ActivityPub protocol for decentralized social interaction.
     */
    const val DEFAULT_FEDIVERSE_INSTANCE = "https://mastodon.social"
    
    /**
     * Fediverse activity types for quest sharing
     */
    enum class FediverseActivity(val action: String) {
        QUEST_STARTED("started a quest"),
        QUEST_COMPLETED("completed a quest"),
        LEVEL_UP("leveled up"),
        COMPANION_UNLOCKED("unlocked a new companion"),
        SKIN_EQUIPPED("equipped a new avatar skin")
    }

    // =============================================================================
    // XP & Leveling System
    // =============================================================================

    /**
     * Base XP required for level 1
     */
    const val BASE_XP_FOR_LEVEL_1 = 100
    
    /**
     * XP multiplier per level (exponential growth)
     */
    const val LEVEL_XP_MULTIPLIER = 1.5f
    
    /**
     * Maximum companion bond level (increases through shared quests)
     */
    const val MAX_COMPANION_BOND = 100
    
    /**
     * Calculate XP required for a given level
     */
    fun xpRequiredForLevel(level: Int): Int {
        return (BASE_XP_FOR_LEVEL_1 * Math.pow(LEVEL_XP_MULTIPLIER.toDouble(), (level - 1).toDouble())).toInt()
    }
    
    /**
     * Calculate player level from total XP
     */
    fun calculateLevelFromXP(totalXP: Int): Int {
        var level = 1
        var xpNeeded = BASE_XP_FOR_LEVEL_1
        var remainingXP = totalXP
        
        while (remainingXP >= xpNeeded && level < LEVEL_TITLES.size) {
            remainingXP -= xpNeeded
            level++
            xpNeeded = xpRequiredForLevel(level)
        }
        
        return level
    }

    // =============================================================================
    // Get model path for quest type
    // =============================================================================

    /**
     * Get model path for quest type
     */
    fun getModelPathForQuestType(type: String): String {
        return MODEL_PATHS[type] ?: MODEL_PATHS["planting"]!!
    }

    /**
     * Get XP reward for quest type
     */
    fun getXpRewardForQuestType(type: String): Int {
        return QuestType.values().find { it.name.equals(type, ignoreCase = true) }
            ?.baseXpReward ?: 50
    }
}
