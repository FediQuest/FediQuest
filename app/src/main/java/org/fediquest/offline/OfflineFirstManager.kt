// File: app/src/main/java/org/fediquest/offline/OfflineFirstManager.kt
package org.fediquest.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.fediquest.data.dao.QuestDao
import org.fediquest.data.entity.QuestEntity
import org.fediquest.fediverse.client.ActivityPubClient
import org.fediquest.fediverse.activitypub.QuestCompletionActivity
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Offline-First Architecture Manager
 *
 * Handles synchronization between local Room database and remote ActivityPub servers.
 * Ensures all operations work offline-first, with background sync when connectivity is available.
 *
 * Key principles:
 * - All writes go to local database first (immediate persistence)
 * - Sync queue tracks pending remote operations
 * - Background sync when network becomes available
 * - Conflict resolution favors local data (user's actions are source of truth)
 */
class OfflineFirstManager(
    private val context: Context,
    private val questDao: QuestDao,
    private val activityPubClient: ActivityPubClient
) {
    companion object {
        private const val TAG = "FediQuest.OfflineFirst"
    }

    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Network state monitoring
    private val _networkState = MutableStateFlow(NetworkState.UNKNOWN)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    // Sync queue state
    private val _syncQueueSize = MutableStateFlow(0)
    val syncQueueSize: StateFlow<Int> = _syncQueueSize.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * Initialize offline-first manager
     * Start monitoring network connectivity
     */
    fun initialize() {
        Log.d(TAG, "Initializing OfflineFirstManager")
        
        // Register network callback
        registerNetworkCallback()
        
        // Update initial network state
        updateNetworkState()
        
        // Load initial sync queue size
        updateSyncQueueSize()
    }

    /**
     * Register network connectivity callback
     */
    private fun registerNetworkCallback() {
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available")
                    _networkState.value = NetworkState.ONLINE
                    coroutineScope.launch {
                        processSyncQueue()
                    }
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost")
                    _networkState.value = NetworkState.OFFLINE
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val hasInternet = networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )
                    val newState = if (hasInternet) NetworkState.ONLINE else NetworkState.OFFLINE
                    
                    if (_networkState.value != newState) {
                        Log.d(TAG, "Network capabilities changed: $newState")
                        _networkState.value = newState
                        
                        if (newState == NetworkState.ONLINE) {
                            coroutineScope.launch {
                                processSyncQueue()
                            }
                        }
                    }
                }
            }

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}", e)
        }
    }

    /**
     * Update current network state
     */
    private fun updateNetworkState() {
        val networkState = getNetworkState()
        _networkState.value = networkState
        Log.d(TAG, "Current network state: $networkState")
    }

    /**
     * Get current network state
     */
    fun getNetworkState(): NetworkState {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                NetworkState.ONLINE
            } else {
                NetworkState.OFFLINE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network state: ${e.message}", e)
            NetworkState.UNKNOWN
        }
    }

    /**
     * Update sync queue size from database
     */
    private fun updateSyncQueueSize() {
        coroutineScope.launch {
            try {
                val unsyncedCount = questDao.getUnsyncedCompletions().size
                _syncQueueSize.value = unsyncedCount
                Log.d(TAG, "Sync queue size: $unsyncedCount")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating sync queue size: ${e.message}", e)
            }
        }
    }

    /**
     * Queue a quest completion for sync to ActivityPub
     * This is called after local database update
     */
    fun queueQuestCompletion(quest: QuestEntity, xpEarned: Int) {
        Log.d(TAG, "Quest completion queued for sync: ${quest.id}")
        
        // Quest is already marked as completed in local DB
        // Just update sync queue size
        updateSyncQueueSize()
        
        // If online, attempt immediate sync
        if (_networkState.value == NetworkState.ONLINE && activityPubClient.isFediverseEnabled()) {
            coroutineScope.launch {
                syncQuestCompletion(quest, xpEarned)
            }
        }
    }

    /**
     * Process sync queue - send all pending items to ActivityPub
     */
    suspend fun processSyncQueue() {
        if (!activityPubClient.isFediverseEnabled()) {
            Log.d(TAG, "ActivityPub not enabled, skipping sync")
            return
        }

        if (_networkState.value != NetworkState.ONLINE) {
            Log.d(TAG, "Not online, skipping sync")
            return
        }

        try {
            val unsyncedQuests = questDao.getUnsyncedCompletions()
            Log.d(TAG, "Processing sync queue: ${unsyncedQuests.size} items")

            for (quest in unsyncedQuests) {
                syncQuestCompletion(quest, quest.xpReward)
            }

            updateSyncQueueSize()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing sync queue: ${e.message}", e)
        }
    }

    /**
     * Sync a single quest completion to ActivityPub
     */
    private suspend fun syncQuestCompletion(quest: QuestEntity, xpEarned: Int) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(quest.createdAt))

            val activity = QuestCompletionActivity(
                questId = quest.id,
                questType = quest.type.name,
                xpEarned = xpEarned,
                location = null, // TODO: Add location if available
                imageUrl = quest.imageUrl,
                timestamp = timestamp
            )

            val result = activityPubClient.postQuestCompletion(activity)
            
            result.fold(
                onSuccess = { postId ->
                    Log.d(TAG, "Quest completion synced successfully: $postId")
                    // Mark as synced in database (would need additional DAO method)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to sync quest completion: ${error.message}")
                    // Keep in queue for retry
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing quest completion: ${e.message}", e)
        }
    }

    /**
     * Force sync now (user-initiated)
     */
    fun forceSync() {
        if (_networkState.value != NetworkState.ONLINE) {
            Log.w(TAG, "Cannot force sync: not online")
            return
        }

        coroutineScope.launch {
            Log.d(TAG, "User-initiated force sync")
            processSyncQueue()
        }
    }

    /**
     * Check if sync is needed
     */
    fun isSyncNeeded(): Boolean {
        return _syncQueueSize.value > 0
    }

    /**
     * Check if device is online
     */
    fun isOnline(): Boolean {
        return _networkState.value == NetworkState.ONLINE
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            networkCallback?.let {
                connectivityManager.unregisterNetworkCallback(it)
            }
            networkCallback = null
            Log.d(TAG, "OfflineFirstManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }
}

/**
 * Network state enumeration
 */
enum class NetworkState {
    ONLINE,
    OFFLINE,
    UNKNOWN
}

/**
 * Factory for creating OfflineFirstManager instances
 */
object OfflineFirstManagerFactory {

    @Volatile
    private var instance: OfflineFirstManager? = null

    fun getInstance(
        context: Context,
        questDao: QuestDao,
        activityPubClient: ActivityPubClient
    ): OfflineFirstManager {
        return instance ?: synchronized(this) {
            instance ?: OfflineFirstManager(
                context.applicationContext,
                questDao,
                activityPubClient
            ).also { instance = it }
        }
    }

    fun reset() {
        instance?.cleanup()
        instance = null
    }
}
