// File: app/src/main/java/org/fediquest/data/entity/PlayerStateEntity.kt
package org.fediquest.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Player State Entity
 * Stores local player progress, XP, and companion evolution state.
 * Offline-first: synced to Fediverse only when online.
 */
@Entity(tableName = "player_state")
data class PlayerStateEntity(
    @PrimaryKey val playerId: String,
    @ColumnInfo(name = "displayName") val displayName: String,
    @ColumnInfo(name = "totalXP") val totalXP: Int = 0,
    @ColumnInfo(name = "level") val level: Int = 1,
    @ColumnInfo(name = "fediverseHandle") val fediverseHandle: String? = null,
    @ColumnInfo(name = "companionStage") val companionStage: Int = 0,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis() // ✅ Added for migration
)
