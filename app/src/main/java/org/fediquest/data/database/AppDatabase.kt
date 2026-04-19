// app/src/main/java/org/fediquest/data/database/AppDatabase.kt
package org.fediquest.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.fediquest.data.dao.PlayerDao
import org.fediquest.data.dao.QuestDao
import org.fediquest.data.entity.PlayerStateEntity
import org.fediquest.data.entity.QuestEntity

/**
 * Central Room database for FediQuest.
 * Manages quests, player state, and offline-first persistence.
 * 
 * ✅ Updated: Added migration for 'updatedAt' column + disabled schema export for demo.
 */
@Database(
    entities = [QuestEntity::class, PlayerStateEntity::class],
    version = 2,  // ✅ Incremented from 1 → 2 to trigger migration
    exportSchema = false  // ✅ Disable to avoid schemaLocation complexity for demo build
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun questDao(): QuestDao
    abstract fun playerDao(): PlayerDao
    
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        
        /**
         * ✅ Migration 1→2: Add 'updatedAt' column to player_state table.
         * Safe for existing installs: adds column with default, backfills old rows.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Step 1: Add column with safe default
                db.execSQL(
                    "ALTER TABLE player_state ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
                )
                // Step 2: Backfill existing rows with current timestamp
                db.execSQL(
                    "UPDATE player_state SET updatedAt = strftime('%s', 'now') * 1000 WHERE updatedAt = 0"
                )
                // Optional: Add index for time-based queries (performance)
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_player_updatedAt ON player_state(updatedAt)"
                )
            }
        }
        
        /**
         * Get singleton database instance (thread-safe).
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fediquest_db"
                )
                // ✅ Register migration for existing users upgrading from v1
                .addMigrations(MIGRATION_1_2)
                // ✅ Optional: Enable WAL for better concurrent access
                .enableMultiInstanceInvalidation()
                // ✅ Optional: Set journal mode for performance
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build().also { INSTANCE = it }
            }
        }
        
        /**
         * Clear singleton reference (for testing or low-memory cleanup).
         */
        fun closeInstance() {
            INSTANCE = null
        }
    }
}
