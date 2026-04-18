// File: app/src/main/java/org/fediquest/fediverse/activitypub/ActivityPubTypes.kt
package org.fediquest.fediverse.activitypub

/**
 * ActivityPub Type Definitions
 * 
 * Data classes representing ActivityPub protocol objects.
 * Used for decentralized social features (OPT-IN only).
 * 
 * Reference: https://www.w3.org/TR/activitypub/
 */

/**
 * Actor types in ActivityPub
 */
enum class ActorType(val typeValue: String) {
    PERSON("Person"),
    GROUP("Group"),
    ORGANIZATION("Organization"),
    SERVICE("Service"),
    APPLICATION("Application")
}

/**
 * Activity types in ActivityPub
 */
enum class ActivityType(val typeValue: String) {
    CREATE("Create"),
    UPDATE("Update"),
    DELETE("Delete"),
    FOLLOW("Follow"),
    ACCEPT("Accept"),
    REJECT("Reject"),
    ADD("Add"),
    REMOVE("Remove"),
    LIKE("Like"),
    DISLIKE("Dislike"),
    ANNOUNCE("Announce"), // Share/Boost
    UNDO("Undo")
}

/**
 * Object types commonly used in ActivityPub
 */
enum class ObjectType(val typeValue: String) {
    NOTE("Note"),
    ARTICLE("Article"),
    IMAGE("Image"),
    VIDEO("Video"),
    EVENT("Event"),
    PLACE("Place"),
    COLLECTION("Collection")
}

/**
 * Base ActivityPub object
 */
sealed class ActivityPubObject(
    open val context: String = "https://www.w3.org/ns/activitystreams"
)

/**
 * ActivityPub Actor
 */
data class Actor(
    val id: String,
    val type: ActorType,
    val inbox: String,
    val outbox: String,
    val following: String? = null,
    val followers: String? = null,
    val preferredUsername: String,
    val name: String? = null,
    val summary: String? = null,
    val url: String? = null,
    val icon: Image? = null,
    val publicKey: PublicKey? = null,
    override val context: String = "https://www.w3.org/ns/activitystreams"
) : ActivityPubObject(context)

/**
 * Public Key for ActivityPub actors
 */
data class PublicKey(
    val id: String,
    val owner: String,
    val publicKeyPem: String
)

/**
 * ActivityPub Activity
 */
data class Activity(
    val id: String,
    val type: ActivityType,
    val actor: String,
    val published: String,
    val to: List<String>? = null,
    val cc: List<String>? = null,
    val object_: ActivityObject? = null, // Using object_ to avoid keyword conflict
    val target: ActivityObject? = null,
    override val context: String = "https://www.w3.org/ns/activitystreams"
) : ActivityPubObject(context)

/**
 * ActivityPub Object (content of an activity)
 */
data class ActivityObject(
    val id: String,
    val type: ObjectType,
    val content: String? = null,
    val name: String? = null,
    val attributedTo: String? = null,
    val published: String? = null,
    val updated: String? = null,
    val url: String? = null,
    val image: Image? = null,
    val tag: List<Tag>? = null,
    val attachment: List<Attachment>? = null
)

/**
 * Image attachment
 */
data class Image(
    val type: String = "Image",
    val mediaType: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null
)

/**
 * Hashtag or mention tag
 */
data class Tag(
    val type: String,
    val href: String,
    val name: String
)

/**
 * Media attachment (photos, videos, etc.)
 */
data class Attachment(
    val type: String,
    val mediaType: String,
    val url: String,
    val name: String? = null
)

/**
 * FediQuest-specific quest completion activity
 */
data class QuestCompletionActivity(
    val questId: String,
    val questType: String,
    val xpEarned: Int,
    val location: Location? = null,
    val imageUrl: String? = null,
    val timestamp: String
)

/**
 * Geographic location (optional extension)
 */
data class Location(
    val type: String = "Place",
    val name: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Companion evolution event for ActivityPub sharing
 */
data class CompanionEvolutionEvent(
    val companionId: String,
    val previousStage: String,
    val newStage: String,
    val bondLevel: Int,
    val timestamp: String
)
