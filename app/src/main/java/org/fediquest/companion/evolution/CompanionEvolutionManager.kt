// File: app/src/main/java/org/fediquest/companion/evolution/CompanionEvolutionManager.kt
package org.fediquest.companion.evolution

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.fediquest.data.entity.QuestEntity
import org.fediquest.data.database.AppDatabase
import org.fediquest.companion.state.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Companion Evolution Manager
 * 
 * Manages companion state machines, evolution tracking, and persistence.
 * All data is stored locally (offline-first).
 * 
 * Features:
 * - State machine for companion evolution stages
 * - Bond level tracking
 * - Mood management
 * - Interaction handling
 * - Evolution event logging
 */
class CompanionEvolutionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "FediQuest.CompanionMgr"
        private const val PREFS_NAME = "fediquest_companions"
    }
    
    // In-memory cache of active companions
    private val companionsCache = ConcurrentHashMap<String, CompanionState>()
    
    // Flow for UI updates
    private val _activeCompanionFlow = MutableStateFlow<CompanionState?>(null)
    val activeCompanionFlow: Flow<CompanionState?> = _activeCompanionFlow
    
    // Evolution history
    private val _evolutionHistory = MutableStateFlow<List<CompanionEvolutionRecord>>(emptyList())
    val evolutionHistoryFlow: Flow<List<CompanionEvolutionRecord>> = _evolutionHistory
    
    /**
     * Load or create a companion
     */
    fun getOrCreateCompanion(
        companionId: String,
        name: String,
        baseEmoji: String,
        personality: CompanionPersonality = CompanionPersonality.CURIOUS
    ): CompanionState {
        return companionsCache.getOrPut(companionId) {
            CompanionState(
                companionId = companionId,
                name = name,
                baseEmoji = baseEmoji,
                personality = personality,
                currentStage = EvolutionStage.EGG,
                bondLevel = 0,
                experience = 0,
                mood = CompanionMood.CONTENT
            ).also {
                Log.d(TAG, "Created new companion: $name ($companionId)")
                saveCompanion(it)
            }
        }
    }
    
    /**
     * Get companion by ID
     */
    fun getCompanion(companionId: String): CompanionState? {
        return companionsCache[companionId]
    }
    
    /**
     * Set active companion
     */
    fun setActiveCompanion(companionId: String?) {
        if (companionId == null) {
            _activeCompanionFlow.value = null
            Log.d(TAG, "No active companion")
            return
        }
        
        val companion = companionsCache[companionId] ?: run {
            Log.w(TAG, "Companion not found: $companionId")
            return
        }
        
        _activeCompanionFlow.value = companion
        Log.d(TAG, "Active companion set: ${companion.name}")
    }
    
    /**
     * Record quest completion for active companion
     */
    fun onQuestCompleted(quest: QuestEntity, xpEarned: Int) {
        val activeCompanion = _activeCompanionFlow.value ?: return
        
        Log.d(TAG, "Companion ${activeCompanion.name} gained XP from quest: ${quest.type.name}")
        
        // TODO: quest.type is now QuestType enum, convert to String for addQuestExperience
        activeCompanion.addQuestExperience(xpEarned, quest.type.name)
        
        // Check if evolution occurred
        if (activeCompanion.evolutionCount > 0) {
            recordEvolution(activeCompanion)
        }
        
        saveCompanion(activeCompanion)
        _activeCompanionFlow.value = activeCompanion // Trigger UI update
    }
    
    /**
     * Interact with active companion
     */
    fun interactWithCompanion(interactionType: InteractionType): Boolean {
        val companion = _activeCompanionFlow.value ?: return false
        
        companion.interact(interactionType)
        saveCompanion(companion)
        _activeCompanionFlow.value = companion
        
        Log.d(TAG, "Interacted with ${companion.name}: $interactionType, mood: ${companion.mood}")
        return true
    }
    
    /**
     * Record evolution event and optionally share to ActivityPub
     */
    private fun recordEvolution(companion: CompanionState) {
        val record = CompanionEvolutionRecord(
            companionId = companion.companionId,
            companionName = companion.name,
            previousStage = companion.getNextStage() ?: companion.currentStage,
            newStage = companion.currentStage,
            bondLevel = companion.bondLevel
        )
        
        val currentHistory = _evolutionHistory.value.toMutableList()
        currentHistory.add(record)
        _evolutionHistory.value = currentHistory
        
        Log.i(TAG, "🎉 ${companion.name} evolved to ${companion.currentStage.displayName}!")
        
        // Optionally share to ActivityPub if enabled (opt-in feature)
        try {
            val activityPubClient = org.fediquest.fediverse.client.ActivityPubClientFactory.getInstance(context)
            if (activityPubClient.isFediverseEnabled()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val event = org.fediquest.fediverse.activitypub.CompanionEvolutionEvent(
                        companionId = companion.companionId,
                        previousStage = record.previousStage.displayName,
                        newStage = record.newStage.displayName,
                        bondLevel = record.bondLevel,
                        timestamp = java.time.Instant.now().toString()
                    )
                    activityPubClient.postCompanionEvolution(event)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to share evolution to Fediverse: ${e.message}")
        }
    }
    
    /**
     * Get all unlocked companions
     */
    fun getAllCompanions(): List<CompanionState> {
        return companionsCache.values.toList()
    }
    
    /**
     * Check if companion can evolve
     */
    fun canEvolve(companionId: String): Boolean {
        return companionsCache[companionId]?.canEvolve() ?: false
    }
    
    /**
     * Force evolution (if conditions are met)
     */
    fun evolveCompanion(companionId: String): Boolean {
        val companion = companionsCache[companionId] ?: return false
        
        if (!companion.canEvolve()) {
            Log.d(TAG, "${companion.name} cannot evolve yet")
            return false
        }
        
        val previousStage = companion.currentStage
        val success = companion.evolve()
        
        if (success) {
            recordEvolution(companion)
            saveCompanion(companion)
            
            if (_activeCompanionFlow.value?.companionId == companionId) {
                _activeCompanionFlow.value = companion
            }
            
            Log.i(TAG, "✨ ${companion.name} evolved from ${previousStage.displayName} to ${companion.currentStage.displayName}!")
        }
        
        return success
    }
    
    /**
     * Update companion mood based on time passed
     */
    fun updateCompanionMoods() {
        val now = System.currentTimeMillis()
        val hoursSinceLastInteraction = 24
        
        companionsCache.values.forEach { companion ->
            val hoursPassed = (now - companion.lastInteractionTime) / (1000 * 60 * 60)
            
            if (hoursPassed > hoursSinceLastInteraction) {
                companion.mood = CompanionMood.SAD
                saveCompanion(companion)
            }
        }
        
        _activeCompanionFlow.value = _activeCompanionFlow.value // Refresh
    }
    
    /**
     * Save companion state to Room database
     */
    private fun saveCompanion(companion: CompanionState) {
        try {
            val db = AppDatabase.getInstance(context)
            val playerDao = db.playerDao()
            
            // TODO: updateCompanionStage removed, use new updateCompanion API with coroutines
            // PlayerDao now has: suspend fun updateCompanion(userId, companionId, evolutionStage)
            kotlinx.coroutines.runBlocking {
                playerDao.updateCompanion(
                    userId = "local_player",
                    companionId = companion.companionId,
                    evolutionStage = companion.currentStage.ordinal
                )
            }
            
            Log.d(TAG, "Saved companion: ${companion.name} (stage: ${companion.currentStage}, bond: ${companion.bondLevel})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save companion to database: ${e.message}")
        }
    }
    
    /**
     * Load all companions from Room database
     */
    fun loadAllCompanions() {
        try {
            val db = AppDatabase.getInstance(context)
            // TODO: getPlayerStateOnce removed, use getPlayerStateSync with runBlocking
            val playerState = kotlinx.coroutines.runBlocking {
                db.playerDao().getPlayerStateSync("local_player")
            }
            
            playerState?.let { state ->
                // Restore companion state from database
                val stage = EvolutionStage.values().getOrElse(state.companionEvolutionStage) { EvolutionStage.EGG }
                Log.d(TAG, "Loaded companion from storage: stage=$stage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load companions from database: ${e.message}")
        }
    }
    
    /**
     * Reset companion (for testing/debugging)
     */
    fun resetCompanion(companionId: String) {
        val companion = companionsCache[companionId] ?: return
        
        companion.currentStage = EvolutionStage.EGG
        companion.bondLevel = 0
        companion.experience = 0
        companion.mood = CompanionMood.CONTENT
        companion.totalQuestsCompleted = 0
        companion.evolutionCount = 0
        
        saveCompanion(companion)
        _activeCompanionFlow.value = companion
        
        Log.d(TAG, "Reset companion: $companionId")
    }
    
    /**
     * Clear all data (for testing)
     */
    fun clearAll() {
        companionsCache.clear()
        _activeCompanionFlow.value = null
        _evolutionHistory.value = emptyList()
        Log.d(TAG, "Cleared all companion data")
    }
}

/**
 * Factory for creating CompanionEvolutionManager
 */
object CompanionEvolutionManagerFactory {
    
    @Volatile
    private var instance: CompanionEvolutionManager? = null
    
    fun getInstance(context: Context): CompanionEvolutionManager {
        return instance ?: synchronized(this) {
            instance ?: CompanionEvolutionManager(context.applicationContext).also { instance = it }
        }
    }
    
    fun reset() {
        instance = null
    }
}
