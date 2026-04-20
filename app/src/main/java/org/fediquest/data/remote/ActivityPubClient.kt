// app/src/main/java/org/fediquest/data/remote/ActivityPubClient.kt
package org.fediquest.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * ActivityPub Client for Fediverse Integration
 * 
 * Handles outbound ActivityPub requests for quest completions and achievements.
 * Designed for offline-first: queues requests when offline, syncs when online.
 * 
 * NOTE: This is a minimal implementation. Full ActivityPub requires:
 * - HTTP Signatures (draft-cavage-http-signatures)
 * - WebFinger discovery
 * - Inbox/Outbox handling
 * - Actor profile management
 */
data class ActivityPubRequest(
    val actorUri: String,      // e.g., "https://mastodon.social/users/alice"
    val inboxUrl: String,      // Target inbox URL
    val activityType: String,  // e.g., "Create", "Announce"
    val objectJson: String,    // JSON-LD activity object
    val createdAt: Long = System.currentTimeMillis()
)

data class ActivityPubResponse(
    val success: Boolean,
    val statusCode: Int?,
    val responseBody: String?,
    val error: String?
)

class ActivityPubClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "FediQuest.ActivityPub"
        private val JSON_MEDIA_TYPE = "application/activity+json".toMediaType()
    }

    /**
     * Send an ActivityPub activity to a remote inbox.
     * Runs on IO dispatcher for coroutine compatibility.
     */
    suspend fun sendActivity(request: ActivityPubRequest): ActivityPubResponse = withContext(Dispatchers.IO) {
        try {
            // Build the HTTP request
            val httpRequest = Request.Builder()
                .url(request.inboxUrl)
                .post(request.objectJson.toRequestBody(JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/activity+json")
                .addHeader("Accept", "application/activity+json, application/ld+json")
                .addHeader("User-Agent", "FediQuest/1.0 (+https://github.com/fediquest/fediquest-android)")
                .build()

            // Execute request
            val response = okHttpClient.newCall(httpRequest).execute()
            
            val responseBody = response.body?.string()
            val isSuccess = response.isSuccessful
            
            ActivityPubResponse(
                success = isSuccess,
                statusCode = response.code,
                responseBody = responseBody,
                error = if (!isSuccess) "HTTP ${response.code}" else null
            )
        } catch (e: IOException) {
            // Network error - queue for retry
            ActivityPubResponse(
                success = false,
                statusCode = null,
                responseBody = null,
                error = "Network error: ${e.message}"
            )
        } catch (e: Exception) {
            // Unexpected error
            ActivityPubResponse(
                success = false,
                statusCode = null,
                responseBody = null,
                error = "Unexpected error: ${e.message}"
            )
        }
    }

    /**
     * Build a Create activity for quest completion.
     * Returns JSON-LD formatted ActivityPub object.
     */
    fun buildQuestCompletionActivity(
        actorUri: String,
        questId: String,
        questTitle: String,
        xpEarned: Int,
        timestamp: Long
    ): String {
        val activityId = "$actorUri/activities/quest_${questId}_${timestamp}"
        val noteContent = "I just completed \"$questTitle\" in #FediQuest and earned $xpEarned XP! 🎮🌍"
        
        return JSONObject().apply {
            put("@context", listOf(
                "https://www.w3.org/ns/activitystreams",
                mapOf("xsd" to "http://www.w3.org/2001/XMLSchema#")
            ))
            put("id", activityId)
            put("type", "Create")
            put("actor", actorUri)
            put("published", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.TimeZone.getTimeZone("UTC")).format(timestamp))
            put("object", JSONObject().apply {
                put("id", "$activityId/object")
                put("type", "Note")
                put("attributedTo", actorUri)
                put("content", noteContent)
                put("published", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.TimeZone.getTimeZone("UTC")).format(timestamp))
                put("tag", listOf(
                    mapOf("type" to "Hashtag", "href" to "https://mastodon.social/tags/FediQuest", "name" to "#FediQuest"),
                    mapOf("type" to "Hashtag", "href" to "https://mastodon.social/tags/PlayToGiveBack", "name" to "#PlayToGiveBack")
                ))
            })
        }.toString()
    }

    /**
     * Build an Announce activity for sharing achievements.
     */
    fun buildAnnounceActivity(
        actorUri: String,
        originalActivityId: String,
        timestamp: Long
    ): String {
        val announceId = "$actorUri/activities/announce_${timestamp}"
        
        return JSONObject().apply {
            put("@context", "https://www.w3.org/ns/activitystreams")
            put("id", announceId)
            put("type", "Announce")
            put("actor", actorUri)
            put("published", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.TimeZone.getTimeZone("UTC")).format(timestamp))
            put("object", originalActivityId)
        }.toString()
    }

    /**
     * Check if a URL is a valid ActivityPub actor URI.
     * Basic validation - full implementation would use WebFinger.
     */
    fun isValidActorUri(uri: String): Boolean {
        return try {
            val url = java.net.URL(uri)
            url.protocol == "https" &&
            url.host.isNotEmpty() &&
            uri.contains("/users/") || uri.contains("/@")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extract inbox URL from actor profile (simplified).
     * Full implementation would fetch and parse actor JSON-LD.
     */
    suspend fun discoverInboxUrl(actorUri: String): String? = withContext(Dispatchers.IO) {
        try {
            // In production: fetch actor URI, parse JSON-LD, extract inbox
            // For now: assume standard Mastodon-style inbox
            val url = java.net.URL(actorUri)
            val baseUrl = "${url.protocol}://${url.host}"
            val username = actorUri.substringAfterLast("/users/").substringBeforeLast("/")
                .ifEmpty { actorUri.substringAfterLast("/@").substringBefore("/") }
            
            "$baseUrl/inbox"
        } catch (e: Exception) {
            null
        }
    }
}
