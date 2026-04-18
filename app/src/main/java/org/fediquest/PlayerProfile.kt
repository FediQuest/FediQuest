// File: app/src/main/java/org/fediquest/PlayerProfile.kt
package org.fediquest

/**
 * Player Profile Model
 * 
 * Represents the player's avatar, level, XP, and progression in FediQuest.
 * Players earn XP by completing social-ecological quests and unlock digital goods
 * like avatar skins and companions as rewards.
 */
data class PlayerProfile(
    val playerId: String,
    val displayName: String,
    val fediverseHandle: String?, // Optional ActivityPub handle for social sharing
    var totalXP: Int = 0,
    var level: Int = 1,
    var currentLevelXP: Int = 0,
    val unlockedSkins: MutableList<String> = mutableListOf("default"),
    val unlockedCompanions: MutableList<String> = mutableListOf(),
    var equippedSkin: String = "default",
    var companionId: String? = null,
    var companionBond: Int = 0,
    val questStats: QuestStatistics = QuestStatistics()
) {

    /**
     * Add XP and update level if threshold reached
     */
    fun addXP(amount: Int) {
        totalXP += amount
        currentLevelXP += amount
        
        // Check for level up
        while (currentLevelXP >= Config.xpRequiredForLevel(level) && level < Config.LEVEL_TITLES.size) {
            currentLevelXP -= Config.xpRequiredForLevel(level)
            level++
            onLevelUp()
        }
    }

    /**
     * Unlock a new avatar skin
     */
    fun unlockSkin(skinId: String) {
        if (!unlockedSkins.contains(skinId)) {
            unlockedSkins.add(skinId)
        }
    }

    /**
     * Unlock a new companion
     */
    fun unlockCompanion(companionId: String) {
        if (!unlockedCompanions.contains(companionId)) {
            unlockedCompanions.add(companionId)
        }
    }

    /**
     * Equip an unlocked skin
     */
    fun equipSkin(skinId: String): Boolean {
        return if (unlockedSkins.contains(skinId)) {
            equippedSkin = skinId
            true
        } else {
            false
        }
    }

    /**
     * Set active companion
     */
    fun setCompanion(companionId: String?): Boolean {
        return if (companionId == null || unlockedCompanions.contains(companionId)) {
            companionId?.let {
                companionBond = getCompanionBond(it)
            }
            this.companionId = companionId
            true
        } else {
            false
        }
    }

    /**
     * Increase bond with current companion
     */
    fun increaseCompanionBond(amount: Int) {
        companionId?.let {
            companionBond = kotlin.math.min(Config.MAX_COMPANION_BOND, companionBond + amount)
        }
    }

    /**
     * Get current level title
     */
    fun getLevelTitle(): String {
        return if (level <= Config.LEVEL_TITLES.size) {
            Config.LEVEL_TITLES[level - 1]
        } else {
            Config.LEVEL_TITLES.last()
        }
    }

    /**
     * Get XP progress to next level (0.0 - 1.0)
     */
    fun getLevelProgress(): Float {
        val xpNeeded = Config.xpRequiredForLevel(level)
        return if (xpNeeded > 0) {
            currentLevelXP.toFloat() / xpNeeded.toFloat()
        } else {
            1.0f
        }
    }

    /**
     * Get currently equipped companion
     */
    fun getCurrentCompanion(): Config.Companion? {
        return companionId?.let { id ->
            Config.COMPANIONS.find { it.id == id }
        }
    }

    /**
     * Get currently equipped skin
     */
    fun getCurrentSkin(): Config.AvatarSkin? {
        return Config.AVATAR_SKINS.find { it.id == equippedSkin }
    }

    /**
     * Check if player can unlock a skin at their level
     */
    fun canUnlockSkin(skinId: String): Boolean {
        val skin = Config.AVATAR_SKINS.find { it.id == skinId }
        return skin != null && level >= skin.unlockLevel
    }

    /**
     * Check if player can unlock a companion at their level
     */
    fun canUnlockCompanion(companionId: String): Boolean {
        val companion = Config.COMPANIONS.find { it.id == companionId }
        return companion != null && level >= companion.unlockLevel
    }

    /**
     * Calculate XP bonus from companion
     */
    fun getXPBonusMultiplier(): Float {
        return getCurrentCompanion()?.let { companion ->
            if (companion.specialAbility.contains("XP bonus")) {
                1.10f // 10% bonus
            } else {
                1.0f
            }
        } ?: 1.0f
    }

    private fun onLevelUp() {
        // Auto-unlock skins available at new level
        Config.AVATAR_SKINS.filter { it.unlockLevel == level }.forEach { skin ->
            unlockSkin(skin.id)
        }
        
        // Auto-unlock companions available at new level
        Config.COMPANIONS.filter { it.unlockLevel == level }.forEach { companion ->
            unlockCompanion(companion.id)
        }
    }

    private fun getCompanionBond(companionId: String): Int {
        // In a full implementation, this would load from persistent storage
        return 0
    }
}

/**
 * Quest Statistics Tracking
 */
data class QuestStatistics(
    var totalQuestsCompleted: Int = 0,
    var treesPlanted: Int = 0,
    var recyclingTrips: Int = 0,
    var cleanupEvents: Int = 0,
    var wildflowersPlanted: Int = 0,
    var waterSaved: Int = 0, // in liters
    var wildlifeHabitatsCreated: Int = 0,
    val questsByType: MutableMap<String, Int> = mutableMapOf()
) {
    fun recordQuestCompletion(type: String) {
        totalQuestsCompleted++
        questsByType[type] = (questsByType[type] ?: 0) + 1
        
        when (type.lowercase()) {
            "planting" -> treesPlanted++
            "recycling" -> recyclingTrips++
            "cleanup" -> cleanupEvents++
            "wildflower" -> wildflowersPlanted++
            "water" -> waterSaved += 100 // Assume 100L saved per action
            "wildlife" -> wildlifeHabitatsCreated++
        }
    }
    
    fun getEnvironmentalImpact(): String {
        val sb = StringBuilder()
        if (treesPlanted > 0) sb.append("🌱 $treesPlanted trees planted\n")
        if (recyclingTrips > 0) sb.append("♻️ $recyclingTrips recycling trips\n")
        if (cleanupEvents > 0) sb.append("🧹 $cleanupEvents cleanup events\n")
        if (wildflowersPlanted > 0) sb.append("🌸 $wildflowersPlanted wildflowers planted\n")
        if (waterSaved > 0) sb.append("💧 ${waterSaved}L water saved\n")
        if (wildlifeHabitatsCreated > 0) sb.append("🦋 $wildlifeHabitatsCreated habitats created")
        return sb.toString()
    }
}
