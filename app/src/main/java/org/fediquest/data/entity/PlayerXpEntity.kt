// File: app/src/main/java/org/fediquest/data/entity/PlayerXpEntity.kt
package org.fediquest.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Player XP Transaction Entity
 * Tracks individual XP gains/losses for audit trail.
 */
@Entity(tableName = "player_xp")
data class PlayerXpEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "playerId") val playerId: String,
    @ColumnInfo(name = "amount") val amount: Int,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
