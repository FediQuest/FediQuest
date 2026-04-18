// File: app/src/main/java/org/fediquest/companion/evolution/CompanionEvolutionManager.kt
package org.fediquest.companion.evolution

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.fediquest.companion.state.*
import org.fediquest.data.entity.QuestEntity
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
        
        Log.d(TAG, "Companion ${activeCompanion.name} gained XP from quest: ${quest.type}")
        
        activeCompanion.addQuestExperience(xpEarned, quest.type)
        
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
     * Record evolution event
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
        
        // TODO: Save evolution history to database
        // TODO: Optionally share to ActivityPub if enabled
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
     * Save companion state (stub - would persist to Room DB)
     */
    private fun saveCompanion(companion: CompanionState) {
        // TODO: Implement persistence to Room database
        // For now, just keep in memory
        Log.d(TAG, "Saved companion: ${companion.name} (stage: ${companion.currentStage}, bond: ${companion.bondLevel})")
    }
    
    /**
     * Load all companions from storage (stub)
     */
    fun loadAllCompanions() {
        // TODO: Load from Room database
        Log.d(TAG, "Loading companions from storage...")
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
