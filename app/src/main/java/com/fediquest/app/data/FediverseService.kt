// File: app/src/main/java/com/fediquest/app/data/FediverseService.kt
package com.fediquest.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fediquest.app.data.models.Quest
import com.fediquest.app.data.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for interacting with Fediverse instances (Mastodon, Pixelfed, etc.)
 * Uses ActivityPub protocol for decentralized social networking.
 */
class FediverseService(private val context: Context) {
    
    private var connectedInstance: String? = null
    private var accessToken: String? = null
    
    /**
     * Connect to a Fediverse instance
     */
    suspend fun connectToInstance(instanceUrl: String): Boolean = withContext(Dispatchers.IO) {
        // In a real implementation, this would use OAuth2 to authenticate
        // For now, we'll simulate a connection
        try {
            connectedInstance = instanceUrl
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Disconnect from current Fediverse instance
     */
    fun disconnect() {
        connectedInstance = null
        accessToken = null
    }
    
    /**
     * Check if connected to a Fediverse instance
     */
    fun isConnected(): Boolean = connectedInstance != null
    
    /**
     * Get the connected instance URL
     */
    fun getConnectedInstance(): String? = connectedInstance
    
    /**
     * Share a quest to the Fediverse
     */
    suspend fun shareQuest(quest: Quest, user: User): ShareResult = withContext(Dispatchers.IO) {
        try {
            val content = buildQuestPost(quest, user)
            
            // Use Android's native share intent as fallback
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out this eco-quest on FediQuest!")
                putExtra(Intent.EXTRA_TEXT, content)
            }
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
            
            ShareResult.Success("Quest shared successfully")
        } catch (e: Exception) {
            ShareResult.Error(e.message ?: "Failed to share quest")
        }
    }
    
    /**
     * Share quest completion to the Fediverse
     */
    suspend fun shareCompletion(quest: Quest, user: User, xpEarned: Int): ShareResult = 
        withContext(Dispatchers.IO) {
        try {
            val content = buildCompletionPost(quest, user, xpEarned)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "I completed an eco-quest!")
                putExtra(Intent.EXTRA_TEXT, content)
            }
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
            
            ShareResult.Success("Completion shared successfully")
        } catch (e: Exception) {
            ShareResult.Error(e.message ?: "Failed to share completion")
        }
    }
    
    /**
     * Build a post for sharing a quest
     */
    private fun buildQuestPost(quest: Quest, user: User): String {
        return buildString {
            appendLine("🌍 New Eco-Quest Alert! 🌱")
            appendLine()
            appendLine("\"${quest.title}\"")
            appendLine()
            appendLine(quest.description)
            appendLine()
            appendLine("Rewards: ${quest.xpReward} XP, ${quest.coinReward} Coins")
            appendLine("Difficulty: ${quest.difficulty.name}")
            appendLine()
            appendLine("Join me on FediQuest and let's make a difference! 💚")
            appendLine()
            append("#FediQuest #EcoChallenge #Sustainability #${quest.type.name}")
            append(" #ClimateAction")
        }
    }
    
    /**
     * Build a post for sharing quest completion
     */
    private fun buildCompletionPost(quest: Quest, user: User, xpEarned: Int): String {
        return buildString {
            appendLine("✅ Quest Completed! 🎉")
            appendLine()
            appendLine("I just completed \"${quest.title}\" on FediQuest!")
            appendLine()
            appendLine("Earned: $xpEarned XP")
            appendLine("Total Quests: ${user.questsCompleted}")
            appendLine("Level: ${user.level}")
            appendLine()
            appendLine("Challenge yourself and join the movement! 🌍💚")
            appendLine()
            append("#FediQuest #EcoWarrior #Sustainability #MissionAccomplished")
            append(" #GreenLiving")
        }
    }
    
    /**
     * Open Fediverse instance in browser
     */
    fun openInstanceInBrowser() {
        connectedInstance?.let { instance ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(if (instance.startsWith("http")) instance else "https://$instance")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    /**
     * Result of a Fediverse operation
     */
    sealed class ShareResult {
        data class Success(val message: String) : ShareResult()
        data class Error(val message: String) : ShareResult()
    }
}
