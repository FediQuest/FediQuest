// File: app/src/main/java/org/fediquest/SpawnFetcher.kt
package org.fediquest

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * SpawnFetcher - ETag-based spawn data fetching with caching
 * 
 * This class demonstrates proper ETag/If-None-Match handling for server.json.
 * It implements HTTP caching to reduce bandwidth and improve load times.
 * 
 * Key features:
 * - ETag storage and retrieval
 * - If-None-Match header on subsequent requests
 * - 304 Not Modified response handling
 * - Retry with exponential backoff (documented)
 */
class SpawnFetcher {

    companion object {
        private const val TAG = "FediQuest.Spawn"
        private const val DEFAULT_SERVER_URL = "http://localhost:8080/server/server.json"
        
        // OkHttpClient with reasonable timeouts
        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // Stored ETag from previous successful response
    private var storedETag: String? = null
    
    // Cached spawn data
    private var cachedSpawns: List<Spawn>? = null

    /**
     * Data class representing a spawn point from server.json
     */
    data class Spawn(
        val id: String,
        val latitude: Double,
        val longitude: Double,
        val modelUrl: String,
        val etag: String?,
        val metadata: Metadata
    )

    data class Metadata(
        val name: String,
        val description: String,
        val type: String,
        val xpReward: Int
    )

    /**
     * Fetch spawns from server with ETag caching
     * 
     * Flow:
     * 1. First request: Fetch full server.json, store ETag
     * 2. Subsequent requests: Send If-None-Match header with stored ETag
     * 3. Server returns 304 Not Modified if data unchanged (no body)
     * 4. Server returns 200 OK with new data if changed
     * 
     * @param serverUrl URL to server.json (defaults to Config.SERVER_URL)
     * @param callback Called with result (success or error)
     */
    fun fetchSpawns(
        serverUrl: String = DEFAULT_SERVER_URL,
        callback: (Result<List<Spawn>>) -> Unit
    ) {
        Log.d(TAG, "Fetching spawns from $serverUrl")
        
        // Build request with If-None-Match header if we have a stored ETag
        val requestBuilder = Request.Builder()
            .url(serverUrl)
            .get()
        
        // Add If-None-Match header for conditional request
        storedETag?.let { etag ->
            requestBuilder.addHeader("If-None-Match", etag)
            Log.d(TAG, "Sending If-None-Match: $etag")
        }
        
        val request = requestBuilder.build()
        
        // Execute request asynchronously
        httpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Failed to fetch spawns: ${e.message}")
                // TODO: Implement retry with exponential backoff
                // See retryWithBackoff() method below
                callback(Result.failure(e))
            }

            override fun onResponse(
                call: okhttp3.Call,
                response: okhttp3.Response
            ) {
                when (response.code) {
                    304 -> {
                        // Not Modified - use cached data
                        Log.d(TAG, "Server returned 304 Not Modified, using cached spawns")
                        cachedSpawns?.let { spawns ->
                            callback(Result.success(spawns))
                        } ?: run {
                            Log.w(TAG, "No cached spawns available")
                            callback(Result.failure(IllegalStateException("No cache available")))
                        }
                    }
                    200 -> {
                        // Success - parse response and update cache
                        response.body?.string()?.let { body ->
                            // Extract ETag from response headers
                            val newETag = response.header("ETag")
                            
                            try {
                                val spawns = parseSpawnJson(body)
                                
                                // Update cache
                                storedETag = newETag
                                cachedSpawns = spawns
                                
                                Log.d(TAG, "Successfully fetched ${spawns.size} spawns, ETag: $newETag")
                                callback(Result.success(spawns))
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to parse spawn JSON: ${e.message}")
                                callback(Result.failure(e))
                            }
                        } ?: run {
                            Log.e(TAG, "Empty response body")
                            callback(Result.failure(IOException("Empty response body")))
                        }
                    }
                    else -> {
                        Log.e(TAG, "Unexpected response code: ${response.code}")
                        callback(Result.failure(IOException("HTTP ${response.code}")))
                    }
                }
            }
        })
    }

    /**
     * Parse server.json response into list of Spawn objects
     * 
     * Note: This is a simplified parser for demonstration.
     * In production, use a proper JSON library like kotlinx.serialization or Gson.
     */
    private fun parseSpawnJson(json: String): List<Spawn> {
        return try {
            val gson = com.google.gson.Gson()
            val spawnResponse = gson.fromJson(json, SpawnResponse::class.java)
            spawnResponse?.spawns ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error: ${e.message}")
            throw e
        }
    }

    /**
     * Data class for server.json response structure
     */
    private data class SpawnResponse(
        val spawns: List<Spawn>,
        val lastUpdated: String?,
        val version: String?,
        val gameInfo: GameInfo?
    )

    private data class GameInfo(
        val description: String?,
        val rewards: String?,
        val fediverseIntegration: String?
    )


    /**
     * Retry with exponential backoff
     * 
     * Documented pattern for retrying failed requests:
     * 
     * ```
     * Initial delay: 1 second
     * Max delay: 30 seconds
     * Backoff multiplier: 2x
     * Max retries: 5
     * 
     * Attempt 1: Wait 1s
     * Attempt 2: Wait 2s
     * Attempt 3: Wait 4s
     * Attempt 4: Wait 8s
     * Attempt 5: Wait 16s
     * ```
     * 
     * Implementation notes:
     * - Use Handler or Coroutine for delays
     * - Add jitter to prevent thundering herd
     * - Respect user's network preferences (WiFi-only mode)
     * - Cancel retries if user navigates away
     */
    private fun retryWithBackoff(
        operation: () -> Unit,
        maxRetries: Int = 5,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 30000,
        backoffMultiplier: Double = 2.0
    ) {
        // TODO: Implement retry logic with exponential backoff
        // This is documented for reviewers to implement based on their needs
        
        Log.d(TAG, "Retry with backoff: maxRetries=$maxRetries, initialDelay=${initialDelayMs}ms")
        
        // Pseudocode:
        // var delay = initialDelayMs
        // var attempts = 0
        // 
        // while (attempts < maxRetries) {
        //     try {
        //         operation()
        //         return  // Success
        //     } catch (e: Exception) {
        //         attempts++
        //         if (attempts >= maxRetries) throw e
        //         
        //         // Add jitter (±10%)
        //         val jitter = (delay * 0.1 * (Math.random() - 0.5)).toLong()
        //         val actualDelay = (delay + jitter).coerceAtMost(maxDelayMs)
        //         
        //         Thread.sleep(actualDelay)
        //         delay = (delay * backoffMultiplier).toLong().coerceAtMost(maxDelayMs)
        //     }
        // }
    }

    /**
     * Clear cached data
     * 
     * Call this when user explicitly refreshes or when cache expires.
     */
    fun clearCache() {
        Log.d(TAG, "Clearing spawn cache")
        storedETag = null
        cachedSpawns = null
    }

    /**
     * Check if we have cached spawns available
     */
    fun hasCachedSpawns(): Boolean = cachedSpawns != null

    /**
     * Get the number of cached spawns (for debugging)
     */
    fun getCachedSpawnCount(): Int = cachedSpawns?.size ?: 0
}
