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

// File: app/src/main/java/com/fediquest/app/MainActivity.kt
package com.fediquest.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.fediquest.app.databinding.ActivityMainBinding
import com.fediquest.app.di.ServiceLocator
import com.fediquest.app.viewmodel.PermissionType
import com.fediquest.app.viewmodel.SharedViewModel
import com.google.android.material.snackbar.Snackbar

/**
 * Main activity hosting the navigation graph and bottom navigation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    private val sharedViewModel: SharedViewModel by viewModels {
        // Manual factory using ServiceLocator
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SharedViewModel(ServiceLocator.userRepository) as T
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        observeViewModel()
        requestNecessaryPermissions()
    }

    private fun setupNavigation() {
        val navController = findNavController(R.id.nav_host_fragment)
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun observeViewModel() {
        // Observe user state
        lifecycleScope.launch {
            sharedViewModel.userState.collect { user ->
                // Update UI with user info if needed
            }
        }

        // Observe navigation events
        lifecycleScope.launch {
            sharedViewModel.navigationEvent.collect { event ->
                event?.let { handleNavigationEvent(it) }
            }
        }

        // Observe toast messages
        lifecycleScope.launch {
            sharedViewModel.toastMessage.collect { message ->
                message?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                    sharedViewModel.onToastConsumed()
                }
            }
        }

        // Observe permission states
        lifecycleScope.launch {
            sharedViewModel.locationPermissionGranted.collect { granted ->
                // Update map fragment or show rationale
            }
        }
    }

    private fun handleNavigationEvent(event: com.fediquest.app.viewmodel.NavigationEvent) {
        when (event) {
            is com.fediquest.app.viewmodel.NavigationEvent.NavigateToQuestDetail -> {
                // Navigate to quest detail
            }
            is com.fediquest.app.viewmodel.NavigationEvent.NavigateToScan -> {
                // Navigate to scan fragment
            }
            is com.fediquest.app.viewmodel.NavigationEvent.ShowLevelUp -> {
                showLevelUpSnackbar(event.newLevel)
            }
            else -> {}
        }
        sharedViewModel.onNavigationEventConsumed()
    }

    private fun requestNecessaryPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        // Location permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Camera permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        permissions.forEach { (permission, isGranted) ->
            when (permission) {
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    sharedViewModel.updatePermission(PermissionType.LOCATION, isGranted)
                    if (!isGranted) {
                        showPermissionRationale("Location permission is needed to find nearby eco-quests.")
                    }
                }
                Manifest.permission.CAMERA -> {
                    sharedViewModel.updatePermission(PermissionType.CAMERA, isGranted)
                    if (!isGranted) {
                        showPermissionRationale("Camera permission is needed to scan trash and complete quests.")
                    }
                }
                Manifest.permission.POST_NOTIFICATIONS -> {
                    sharedViewModel.updatePermission(PermissionType.NOTIFICATION, isGranted)
                }
            }
        }
    }

    private fun showPermissionRationale(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).setAction("OK") {
            requestNecessaryPermissions()
        }.show()
    }

    private fun showLevelUpSnackbar(level: Int) {
        Snackbar.make(
            binding.root,
            "🎉 Congratulations! You reached Level $level!",
            Snackbar.LENGTH_LONG
        ).setBackgroundTint(ContextCompat.getColor(this, R.color.color_accent))
            .show()
    }
}
