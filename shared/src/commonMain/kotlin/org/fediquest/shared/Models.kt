package org.fediquest.shared

import kotlinx.serialization.Serializable

/**
 * Shared quest model for cross-platform compatibility
 */
@Serializable
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val type: QuestType,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 50f,
    val xpReward: Int,
    val imageUrl: String? = null,
    val modelUrl: String? = null,
    val metadata: QuestMetadata? = null
)

@Serializable
enum class QuestType {
    PLANTING,
    RECYCLING,
    CLEANUP,
    WILDFLOWER,
    WATER,
    WILDLIFE,
    SOCIAL,
    ECOLOGICAL,
    CREATIVE
}

@Serializable
data class QuestMetadata(
    val companionHint: String? = null,
    val environmentalImpact: String? = null,
    val difficulty: String = "normal"
)

/**
 * Shared player state model
 */
@Serializable
data class PlayerState(
    val userId: String = "local_player",
    val totalXP: Int = 0,
    val level: Int = 1,
    val avatarSkinId: String = "default",
    val companionId: String = "starter",
    val companionEvolutionStage: Int = 0
)

/**
 * Companion evolution stages
 */
@Serializable
enum class CompanionStage {
    EGG,
    BABY,
    CHILD,
    TEEN,
    ADULT,
    ELDER,
    LEGENDARY
}

/**
 * Spawn point data from server
 */
@Serializable
data class SpawnData(
    val spawns: List<Spawn>,
    val lastUpdated: String,
    val version: String,
    val gameInfo: GameInfo? = null
)

@Serializable
data class Spawn(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val modelUrl: String,
    val etag: String,
    val metadata: SpawnMetadata
)

@Serializable
data class SpawnMetadata(
    val name: String,
    val description: String,
    val type: String,
    val xpReward: Int,
    val companionHint: String? = null,
    val environmentalImpact: String? = null
)

@Serializable
data class GameInfo(
    val description: String,
    val rewards: String,
    val fediverseIntegration: String
)
