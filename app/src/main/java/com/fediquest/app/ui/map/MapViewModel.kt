// File: app/src/main/java/com/fediquest/app/ui/map/MapViewModel.kt
package com.fediquest.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fediquest.app.data.models.FediLocation
import com.fediquest.app.data.models.Quest
import com.fediquest.app.data.repositories.QuestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for MapFragment.
 * Manages quest data and user location.
 */
class MapViewModel(
    private val questRepository: QuestRepository
) : ViewModel() {

    private val _quests = MutableStateFlow<List<Quest>>(emptyList())
    val quests: StateFlow<List<Quest>> = _quests.asStateFlow()

    private val _currentLocation = MutableStateFlow<FediLocation?>(null)
    val currentLocation: StateFlow<FediLocation?> = _currentLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Start listening to location updates and fetch nearby quests.
     */
    fun startLocationUpdates() {
        viewModelScope.launch {
            // In a real implementation, this would subscribe to LocationTracker
            // For now, we'll just load mock quests
            loadQuests()
        }
    }

    /**
     * Load quests from repository.
     */
    private suspend fun loadQuests() {
        try {
            _isLoading.value = true
            
            // Get current location (mocked for now)
            val location = _currentLocation.value ?: FediLocation(37.7749, -122.4194) // San Francisco
            
            // Fetch quests near current location
            questRepository.fetchRemoteQuests(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusKm = 10.0
            ).onSuccess { quests ->
                _quests.value = quests
            }.onFailure { error ->
                // Handle error (show toast in fragment)
            }
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Update current location.
     */
    fun updateLocation(location: FediLocation) {
        _currentLocation.value = location
        viewModelScope.launch {
            loadQuests()
        }
    }

    /**
     * Refresh quests manually.
     */
    fun refreshQuests() {
        viewModelScope.launch {
            loadQuests()
        }
    }

    /**
     * Filter quests by category.
     */
    fun filterByCategory(category: com.fediquest.app.data.models.QuestCategory) {
        viewModelScope.launch {
            val allQuests = _quests.value
            // This is simplified - in production you'd query the database
        }
    }
}
