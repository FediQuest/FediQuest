// File: app/src/main/java/org/fediquest/data/database/AppDatabase.kt
package org.fediquest.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fediquest.data.dao.PlayerXpDao
import org.fediquest.data.dao.QuestDao
import org.fediquest.data.entity.PlayerXpEntity
import org.fediquest.data.entity.QuestEntity

/**
 * App Database - Room Database for FediQuest
 * 
 * Offline-first database storing quests, player XP, and progression data.
 * All data is stored locally and synced with remote servers when online (opt-in).
 */
@Database(
    entities = [
        QuestEntity::class,
        PlayerXpEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun questDao(): QuestDao
    abstract fun playerXpDao(): PlayerXpDao
    
    companion object {
        private const val DATABASE_NAME = "fediquest_db"
        
        @Volatile
        private var instance: AppDatabase? = null
        
        /**
         * Get database instance (singleton)
         * Thread-safe lazy initialization
         */
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }
        
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .addCallback(DatabaseCallback())
            .fallbackToDestructiveMigration() // For development; use migrations in production
            .build()
        }
        
        /**
         * Database callback for initialization tasks
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                instance?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        // Pre-populate with default data if needed
                        // Example: Add starter quests or tutorial data
                    }
                }
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Enable foreign keys if needed
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }
        
        /**
         * Close database instance
         */
        fun closeInstance() {
            instance?.close()
            instance = null
        }
    }
}
