// File: app/src/main/java/org/fediquest/data/remote/ActivityPubClient.kt
package org.fediquest.data.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * ActivityPub API interface for Retrofit
 */
interface ActivityPubApi {
    @GET
    suspend fun getActor(@Url actorUrl: String): ActorResponse
    
    @POST("api/v1/statuses")
    suspend fun postStatus(@Body params: StatusParams): StatusResponse
}

data class ActorResponse(val id: String, val username: String, val acct: String, val url: String)
data class StatusParams(val status: String, val visibility: String = "public")
data class StatusResponse(val id: String, val content: String, val url: String)

/**
 * ActivityPub Client - Retrofit-based, offline-first
 */
class ActivityPubClient(private val context: Context) {
    companion object {
        private const val TAG = "FediQuest.ActivityPub"
        private const val PREFS = "fediquest_fediverse_prefs"
        private const val KEY_URL = "instance_url"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_ENABLED = "fediverse_enabled"
    }
    
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private var api: ActivityPubApi? = null
    
    fun isFediverseEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)
    
    suspend fun configureInstance(instanceUrl: String, accessToken: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            prefs.edit().apply {
                putString(KEY_URL, instanceUrl)
                accessToken?.let { putString(KEY_TOKEN, it) }
                putBoolean(KEY_ENABLED, true)
                apply()
            }
            val retrofit = Retrofit.Builder()
                .baseUrl(if (instanceUrl.endsWith("/")) instanceUrl else "$instanceUrl/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            api = retrofit.create(ActivityPubApi::class.java)
            Log.d(TAG, "Fediverse configured: $instanceUrl")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure: ${e.message}")
            false
        }
    }
    
    suspend fun postQuestCompletion(questType: String, xpEarned: Int): Result<String> = withContext(Dispatchers.IO) {
        if (!isFediverseEnabled()) return@withContext Result.failure(IllegalStateException("Not enabled"))
        try {
            val emoji = when (questType.lowercase()) {
                "planting" -> "🌱"; "recycling" -> "♻️"; "cleanup" -> "🧹"; else -> "✨"
            }
            val content = "$emoji Completed $questType quest! Earned $xpEarned XP. #FediQuest"
            Log.d(TAG, "[OFFLINE QUEUE] $content")
            Result.success("[QUEUED] $content")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun disableFediverse() {
        prefs.edit().apply { putBoolean(KEY_ENABLED, false); remove(KEY_TOKEN); apply() }
        Log.d(TAG, "Fediverse disabled")
    }
}

object ActivityPubClientFactory {
    @Volatile private var instance: ActivityPubClient? = null
    fun getInstance(context: Context): ActivityPubClient = instance ?: synchronized(this) {
        instance ?: ActivityPubClient(context.applicationContext).also { instance = it }
    }
    fun reset() { instance = null }
}
