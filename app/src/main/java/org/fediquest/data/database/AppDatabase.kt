// File: app/src/main/java/org/fediquest/data/database/AppDatabase.kt
package org.fediquest.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.fediquest.data.dao.PlayerDao
import org.fediquest.data.dao.QuestDao
import org.fediquest.data.dao.PlayerXpDao
import org.fediquest.data.entity.PlayerStateEntity
import org.fediquest.data.entity.QuestEntity
import org.fediquest.data.entity.PlayerXpEntity

/**
 * Central Room database for FediQuest.
 * Manages quests, player state, and offline-first persistence.
 * 
 * Migration 1→2: Adds 'updatedAt' column to player_state table.
 */
@Database(
    entities = [QuestEntity::class, PlayerStateEntity::class, PlayerXpEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun questDao(): QuestDao
    abstract fun playerDao(): PlayerDao
    abstract fun playerXpDao(): PlayerXpDao
    
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        
        /**
         * Migration 1→2: Add 'updatedAt' column to player_state table.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE player_state ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE player_state SET updatedAt = strftime('%s', 'now') * 1000 WHERE updatedAt = 0"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_player_updatedAt ON player_state(updatedAt)"
                )
            }
        }
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fediquest_db"
                )
                .addMigrations(MIGRATION_1_2)
                .enableMultiInstanceInvalidation()
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build().also { INSTANCE = it }
            }
        }
        
        fun closeInstance() {
            INSTANCE = null
        }
    }
}
