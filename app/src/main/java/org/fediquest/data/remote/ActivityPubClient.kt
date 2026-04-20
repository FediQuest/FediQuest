package org.fediquest.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.fediquest.data.entity.QuestEntity

class ActivityPubClient {

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun fetchQuestsFromInstance(instanceUrl: String, actorId: String): List<QuestEntity> {
        return try {
            val response = httpClient.get("$instanceUrl/api/quests?actor=$actorId")
            // Parse response and convert to QuestEntity list
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun publishQuestCompletion(instanceUrl: String, questId: String, proof: String): Boolean {
        return try {
            val response = httpClient.post("$instanceUrl/api/quest/complete") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "questId" to questId,
                    "proof" to proof
                ))
            }
            response.status.value == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncUnsyncedQuests(instanceUrl: String, quests: List<QuestEntity>): List<Long> {
        val syncedIds = mutableListOf<Long>()
        
        for (quest in quests) {
            if (publishQuestCompletion(instanceUrl, quest.questId, quest.description)) {
                syncedIds.add(quest.id)
            }
        }
        
        return syncedIds
    }

    fun close() {
        httpClient.close()
    }
}
