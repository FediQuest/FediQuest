package org.fediquest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questId: String,
    val title: String,
    val description: String,
    val locationLatitude: Double,
    val locationLongitude: Double,
    val locationAltitude: Double,
    val difficulty: Int = 1,
    val rewardExperience: Long = 100L,
    val rewardItem: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = 0L,
    val syncedAt: Long? = null
)
