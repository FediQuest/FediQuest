// File: app/src/main/java/com/fediquest/app/ui/avatar/CompanionViewModel.kt
package com.fediquest.app.ui.avatar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fediquest.app.data.models.Companion
import com.fediquest.app.data.repositories.CompanionRepository
import com.fediquest.app.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for CompanionFragment.
 * Manages companion interactions, stats, and evolution.
 */
class CompanionViewModel(
    private val companionRepository: CompanionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _companions = MutableStateFlow<List<Companion>>(emptyList())
    val companions: StateFlow<List<Companion>> = _companions.asStateFlow()

    private val _equippedCompanion = MutableStateFlow<Companion?>(null)
    val equippedCompanion: StateFlow<Companion?> = _equippedCompanion.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * Load all companions.
     */
    fun loadAllCompanions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val companions = companionRepository.getAllCompanions()
                _companions.value = companions
                _equippedCompanion.value = companions.find { it.isEquipped }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Feed the companion (increases happiness).
     */
    fun feedCompanion() {
        viewModelScope.launch {
            val companion = _equippedCompanion.value ?: return@launch
            
            val newHappiness = (companion.happiness + 20).coerceAtMost(100)
            companionRepository.updateCompanionStats(
                companionId = companion.id,
                happiness = newHappiness
            )
            
            _message.value = "🍎 ${companion.name} enjoyed the food! Happiness +20"
            loadAllCompanions()
        }
    }

    /**
     * Play with the companion (increases happiness, decreases energy).
     */
    fun playWithCompanion() {
        viewModelScope.launch {
            val companion = _equippedCompanion.value ?: return@launch
            
            if (companion.energy < 20) {
                _message.value = "⚠️ ${companion.name} is too tired to play!"
                return@launch
            }
            
            val newHappiness = (companion.happiness + 15).coerceAtMost(100)
            val newEnergy = (companion.energy - 20).coerceAtLeast(0)
            
            companionRepository.updateCompanionStats(
                companionId = companion.id,
                happiness = newHappiness,
                energy = newEnergy
            )
            
            _message.value = "🎾 ${companion.name} had fun playing! Happiness +15, Energy -20"
            loadAllCompanions()
        }
    }

    /**
     * Let the companion rest (restores energy).
     */
    fun restCompanion() {
        viewModelScope.launch {
            val companion = _equippedCompanion.value ?: return@launch
            
            val newEnergy = (companion.energy + 50).coerceAtMost(100)
            
            companionRepository.updateCompanionStats(
                companionId = companion.id,
                energy = newEnergy
            )
            
            _message.value = "💤 ${companion.name} rested and regained energy! Energy +50"
            loadAllCompanions()
        }
    }

    /**
     * Equip the selected companion.
     */
    fun equipCurrentCompanion() {
        viewModelScope.launch {
            val companion = _equippedCompanion.value ?: return@launch
            companionRepository.equipCompanion(companion.id)
            _message.value = "${companion.name} is now your active companion!"
            loadAllCompanions()
        }
    }

    /**
     * Add XP to the equipped companion.
     */
    fun addXPToCompanion(xpAmount: Int) {
        viewModelScope.launch {
            val companion = _equippedCompanion.value ?: return@launch
            
            var newXp = companion.currentXP + xpAmount
            var newLevel = companion.level
            var xpToNext = companion.xpToNextLevel
            var newEvolutionStage = companion.evolutionStage
            
            // Level up logic
            while (newXp >= xpToNext) {
                newXp -= xpToNext
                newLevel++
                xpToNext = calculateXPForLevel(newLevel)
                
                // Check for evolution
                if (newLevel >= getEvolutionLevel(newEvolutionStage)) {
                    newEvolutionStage++
                    _message.value = "🎉 ${companion.name} evolved to stage $newEvolutionStage!"
                }
            }
            
            companionRepository.saveCompanion(
                companion.copy(
                    currentXP = newXp,
                    level = newLevel,
                    xpToNextLevel = xpToNext,
                    evolutionStage = newEvolutionStage
                )
            )
            
            loadAllCompanions()
        }
    }

    private fun calculateXPForLevel(level: Int): Int {
        return (100 * Math.pow(1.25, level - 1)).toInt()
    }

    private fun getEvolutionLevel(stage: Int): Int {
        return when (stage) {
            1 -> 10  // First evolution at level 10
            2 -> 20  // Second evolution at level 20
            else -> Int.MAX_VALUE
        }
    }
}

// Factory for CompanionViewModel
class CompanionViewModelFactory(
    private val companionRepository: CompanionRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return CompanionViewModel(companionRepository, userRepository) as T
    }
}
