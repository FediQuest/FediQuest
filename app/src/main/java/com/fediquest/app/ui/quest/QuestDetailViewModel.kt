// File: app/src/main/java/com/fediquest/app/ui/quest/QuestDetailViewModel.kt
package com.fediquest.app.ui.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fediquest.app.data.models.Quest
import com.fediquest.app.data.models.QuestStatus
import com.fediquest.app.data.repositories.QuestRepository
import com.fediquest.app.data.repositories.UserRepository
import com.fediquest.app.domain.usecases.PostToFediverseUseCase
import com.fediquest.app.domain.usecases.ValidateQuestUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for QuestDetailFragment.
 * Manages quest details, completion, and Fediverse sharing.
 */
class QuestDetailViewModel(
    private val questRepository: QuestRepository,
    private val userRepository: UserRepository,
    private val validateQuestUseCase: ValidateQuestUseCase = ValidateQuestUseCase(questRepository),
    private val postToFediverseUseCase: PostToFediverseUseCase = PostToFediverseUseCase()
) : ViewModel() {

    private val _quest = MutableStateFlow<Quest?>(null)
    val quest: StateFlow<Quest?> = _quest.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _validationResult = MutableStateFlow<com.fediquest.app.domain.usecases.ValidationResult?>(null)
    val validationResult: StateFlow<com.fediquest.app.domain.usecases.ValidationResult?> = _validationResult.asStateFlow()

    private val _shareResult = MutableStateFlow<Boolean?>(null)
    val shareResult: StateFlow<Boolean?> = _shareResult.asStateFlow()

    /**
     * Load quest details by ID.
     */
    fun loadQuest(questId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _quest.value = questRepository.getQuestById(questId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Start a quest (change status to IN_PROGRESS).
     */
    fun startQuest(questId: String) {
        viewModelScope.launch {
            questRepository.updateQuestStatus(questId, QuestStatus.IN_PROGRESS)
            loadQuest(questId) // Refresh
        }
    }

    /**
     * Complete a quest with optional proof image.
     */
    fun completeQuest(questId: String, proofImageUrl: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // For now, use mock location - in production, get actual GPS coordinates
                val result = validateQuestUseCase(
                    questId = questId,
                    proofImageUrl = proofImageUrl,
                    userLatitude = 37.7749, // Mock: San Francisco
                    userLongitude = -122.4194
                )

                _validationResult.value = result

                if (result.success) {
                    // Update quest status
                    questRepository.updateQuestStatus(questId, QuestStatus.COMPLETED)
                    
                    // Reward user
                    val userId = "current_user"
                    userRepository.addCoins(userId, result.coinReward)
                    userRepository.updateUserXP(userId, result.xpReward)
                    userRepository.incrementQuestsCompleted(userId)
                    
                    // Check for badges
                    checkAndAwardBadges()
                    
                    loadQuest(questId) // Refresh
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Share quest completion to Fediverse.
     */
    fun shareToFediverse(questId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val quest = questRepository.getQuestById(questId) ?: return@launch
                
                val result = postToFediverseUseCase(quest)
                _shareResult.value = result.isSuccess
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check and award badges based on user progress.
     */
    private suspend fun checkAndAwardBadges() {
        val user = userRepository.getCurrentUser() ?: return
        
        // Quest count badges
        val questBadge = com.fediquest.app.util.XPManager.getBadgeForQuestCount(user.questsCompleted + 1)
        questBadge?.let { badgeId ->
            if (badgeId !in user.badges) {
                userRepository.addBadge(user.id, badgeId)
            }
        }
        
        // Level badges
        val levelBadge = com.fediquest.app.util.XPManager.getBadgeForLevel(user.level)
        levelBadge?.let { badgeId ->
            if (badgeId !in user.badges) {
                userRepository.addBadge(user.id, badgeId)
            }
        }
    }
}

// Factory for QuestDetailViewModel
class QuestDetailViewModelFactory(
    private val questRepository: QuestRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return QuestDetailViewModel(questRepository, userRepository) as T
    }
}
