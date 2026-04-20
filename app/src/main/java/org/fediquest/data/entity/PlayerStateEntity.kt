package org.fediquest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_state")
data class PlayerStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerId: String,
    val username: String,
    val level: Int = 1,
    val experience: Long = 0L,
    val inventory: String = "[]",
    val currentQuestId: Long? = null,
    val positionX: Double = 0.0,
    val positionY: Double = 0.0,
    val positionZ: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = 0L
)
