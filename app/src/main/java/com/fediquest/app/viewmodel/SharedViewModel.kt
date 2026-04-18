// File: app/src/main/java/com/fediquest/app/viewmodel/SharedViewModel.kt
package com.fediquest.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fediquest.app.data.models.User
import com.fediquest.app.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for cross-fragment state management.
 * Holds user state, permissions, navigation events, and toast messages.
 */
class SharedViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    // User state
    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()

    // Permission states
    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()

    private val _cameraPermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted.asStateFlow()

    private val _notificationPermissionGranted = MutableStateFlow(false)
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()

    // Navigation events (one-time)
    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    // Toast messages (one-time)
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Initialize the SharedViewModel by loading user data.
     */
    fun initialize() {
        loadUser()
    }

    /**
     * Load current user from repository.
     */
    private fun loadUser() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val user = userRepository.getCurrentUser()
                _userState.value = user
            } catch (e: Exception) {
                showToast("Failed to load user data")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update permission state.
     */
    fun updatePermission(permissionType: PermissionType, isGranted: Boolean) {
        when (permissionType) {
            PermissionType.LOCATION -> _locationPermissionGranted.value = isGranted
            PermissionType.CAMERA -> _cameraPermissionGranted.value = isGranted
            PermissionType.NOTIFICATION -> _notificationPermissionGranted.value = isGranted
        }
    }

    /**
     * Navigate to a destination.
     */
    fun navigate(event: NavigationEvent) {
        viewModelScope.launch {
            _navigationEvent.value = event
        }
    }

    /**
     * Mark navigation event as consumed.
     */
    fun onNavigationEventConsumed() {
        viewModelScope.launch {
            _navigationEvent.value = null
        }
    }

    /**
     * Show a toast message.
     */
    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.value = message
        }
    }

    /**
     * Mark toast message as consumed.
     */
    fun onToastConsumed() {
        viewModelScope.launch {
            _toastMessage.value = null
        }
    }

    /**
     * Refresh user data.
     */
    fun refreshUser() {
        loadUser()
    }

    /**
     * Update user XP and check for level-ups.
     */
    fun addXP(amount: Int) {
        viewModelScope.launch {
            val currentUser = _userState.value ?: return@launch
            userRepository.updateUserXP(currentUser.id, amount)
            
            // Reload user to get updated XP/level
            loadUser()
            
            // Check if leveled up
            val newUser = userRepository.getCurrentUser()
            if (newUser != null && newUser.level > currentUser.level) {
                showToast("🎉 Level Up! You are now level ${newUser.level}!")
                navigate(NavigationEvent.ShowLevelUp(newUser.level))
            }
        }
    }

    /**
     * Add coins to user balance.
     */
    fun addCoins(amount: Int) {
        viewModelScope.launch {
            val currentUser = _userState.value ?: return@launch
            userRepository.addCoins(currentUser.id, amount)
            loadUser()
            showToast("+${amount} coins! 💰")
        }
    }
}

/**
 * Types of permissions tracked by the app.
 */
enum class PermissionType {
    LOCATION,
    CAMERA,
    NOTIFICATION
}

/**
 * Navigation events for one-time navigation actions.
 */
sealed class NavigationEvent {
    data class NavigateToQuestDetail(val questId: String) : NavigationEvent()
    data class NavigateToScan(val questId: String) : NavigationEvent()
    data class NavigateToAvatar : NavigationEvent()
    data class NavigateToCompanion : NavigationEvent()
    data class ShowLevelUp(val newLevel: Int) : NavigationEvent()
    object ShowRewards : NavigationEvent()
}
