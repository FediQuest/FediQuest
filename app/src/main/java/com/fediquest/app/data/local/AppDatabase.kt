// File: app/src/main/java/com/fediquest/app/data/local/AppDatabase.kt
package com.fediquest.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fediquest.app.data.models.*

/**
 * Room database for offline-first data storage.
 */
@Database(
    entities = [
        Quest::class,
        User::class,
        Avatar::class,
        Companion::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questDao(): QuestDao
    abstract fun userDao(): UserDao
    abstract fun avatarDao(): AvatarDao
    abstract fun companionDao(): CompanionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "fediquest_database"
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}

// Type converters for complex types
class Converters {
    @androidx.room.TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @androidx.room.TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",").map { it.trim() }
    }

    @androidx.room.TypeConverter
    fun fromQuestCategory(category: QuestCategory): String {
        return category.name
    }

    @androidx.room.TypeConverter
    fun toQuestCategory(value: String): QuestCategory {
        return QuestCategory.valueOf(value)
    }

    @androidx.room.TypeConverter
    fun fromQuestDifficulty(difficulty: QuestDifficulty): String {
        return difficulty.name
    }

    @androidx.room.TypeConverter
    fun toQuestDifficulty(value: String): QuestDifficulty {
        return QuestDifficulty.valueOf(value)
    }

    @androidx.room.TypeConverter
    fun fromValidationMode(mode: ValidationMode): String {
        return mode.name
    }

    @androidx.room.TypeConverter
    fun toValidationMode(value: String): ValidationMode {
        return ValidationMode.valueOf(value)
    }

    @androidx.room.TypeConverter
    fun fromQuestStatus(status: QuestStatus): String {
        return status.name
    }

    @androidx.room.TypeConverter
    fun toQuestStatus(value: String): QuestStatus {
        return QuestStatus.valueOf(value)
    }

    @androidx.room.TypeConverter
    fun fromAvatarCategory(category: AvatarCategory): String {
        return category.name
    }

    @androidx.room.TypeConverter
    fun toAvatarCategory(value: String): AvatarCategory {
        return AvatarCategory.valueOf(value)
    }

    @androidx.room.TypeConverter
    fun fromCompanionSpecies(species: CompanionSpecies): String {
        return species.name
    }

    @androidx.room.TypeConverter
    fun toCompanionSpecies(value: String): CompanionSpecies {
        return CompanionSpecies.valueOf(value)
    }
}

// DAOs
@androidx.room.Dao
interface QuestDao {
    @androidx.room.Query("SELECT * FROM quests WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getQuestsByStatus(status: QuestStatus): List<Quest>

    @androidx.room.Query("SELECT * FROM quests WHERE id = :id")
    suspend fun getQuestById(id: String): Quest?

    @androidx.room.Query("SELECT * FROM quests WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng")
    suspend fun getQuestsInBounds(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<Quest>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: Quest)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<Quest>)

    @androidx.room.Update
    suspend fun updateQuest(quest: Quest)

    @androidx.room.Delete
    suspend fun deleteQuest(quest: Quest)

    @androidx.room.Query("DELETE FROM quests")
    suspend fun deleteAll()
}

@androidx.room.Dao
interface UserDao {
    @androidx.room.Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: String): User?

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @androidx.room.Update
    suspend fun updateUser(user: User)
}

@androidx.room.Dao
interface AvatarDao {
    @androidx.room.Query("SELECT * FROM avatars")
    suspend fun getAllAvatars(): List<Avatar>

    @androidx.room.Query("SELECT * FROM avatars WHERE category = :category")
    suspend fun getAvatarsByCategory(category: AvatarCategory): List<Avatar>

    @androidx.room.Query("SELECT * FROM avatars WHERE isEquipped = 1 LIMIT 1")
    suspend fun getEquippedAvatar(): Avatar?

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAvatar(avatar: Avatar)

    @androidx.room.Update
    suspend fun updateAvatar(avatar: Avatar)
}

@androidx.room.Dao
interface CompanionDao {
    @androidx.room.Query("SELECT * FROM companions")
    suspend fun getAllCompanions(): List<Companion>

    @androidx.room.Query("SELECT * FROM companions WHERE id = :id")
    suspend fun getCompanionById(id: String): Companion?

    @androidx.room.Query("SELECT * FROM companions WHERE isEquipped = 1 LIMIT 1")
    suspend fun getEquippedCompanion(): Companion?

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertCompanion(companion: Companion)

    @androidx.room.Update
    suspend fun updateCompanion(companion: Companion)
}
