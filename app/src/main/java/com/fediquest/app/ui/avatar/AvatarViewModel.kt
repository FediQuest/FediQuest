// File: app/src/main/java/com/fediquest/app/ui/avatar/AvatarViewModel.kt
package com.fediquest.app.ui.avatar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fediquest.app.data.models.Avatar
import com.fediquest.app.data.models.AvatarCategory
import com.fediquest.app.data.repositories.AvatarRepository
import com.fediquest.app.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for AvatarFragment.
 * Manages avatar selection and equipment.
 */
class AvatarViewModel(
    private val avatarRepository: AvatarRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _avatars = MutableStateFlow<List<Avatar>>(emptyList())
    val avatars: StateFlow<List<Avatar>> = _avatars.asStateFlow()

    private val _equippedAvatar = MutableStateFlow<Avatar?>(null)
    val equippedAvatar: StateFlow<Avatar?> = _equippedAvatar.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Load avatars for a specific category.
     */
    fun loadAvatarsForCategory(category: AvatarCategory) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val avatars = avatarRepository.getAvatarsByCategory(category)
                _avatars.value = avatars
                
                // Find currently equipped avatar in this category
                _equippedAvatar.value = avatars.find { it.isEquipped }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Equip an avatar part.
     */
    fun equipAvatar(avatarId: String) {
        viewModelScope.launch {
            avatarRepository.equipAvatar(avatarId)
            loadAvatarsForCategory(AvatarCategory.HAIR) // Refresh current category
        }
    }

    /**
     * Unlock an avatar (e.g., after achieving a milestone).
     */
    fun unlockAvatar(avatarId: String) {
        viewModelScope.launch {
            avatarRepository.unlockAvatar(avatarId)
            loadAvatarsForCategory(AvatarCategory.HAIR) // Refresh
        }
    }

    /**
     * Load all avatars (for initialization).
     */
    fun loadAllAvatars() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _avatars.value = avatarRepository.getAllAvatars()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Factory for AvatarViewModel
class AvatarViewModelFactory(
    private val avatarRepository: AvatarRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return AvatarViewModel(avatarRepository, userRepository) as T
    }
}
