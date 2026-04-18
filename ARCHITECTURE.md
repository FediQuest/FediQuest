# FediQuest App Architecture

## Overview

FediQuest is an offline-first AR quest app with opt-in AI and Fediverse features. This document describes the initialized app structure.

## Core Principles

1. **Offline-First**: All core functionality works without internet connectivity
2. **Opt-In AI/ML**: TensorFlow Lite image verification is disabled by default (stub mode)
3. **Opt-In Fediverse**: ActivityPub social features require explicit user configuration
4. **Local Storage**: Room database stores all quests, XP, and companion data locally

## Directory Structure

```
app/src/main/java/org/fediquest/
├── data/                          # Room Database Layer
│   ├── entity/                    # Database entities
│   │   ├── QuestEntity.kt         # Quest data model with QuestType enum
│   │   ├── PlayerStateEntity.kt   # Player state + companion evolution stage
│   │   └── PlayerXpEntity.kt      # Legacy XP tracking (deprecated)
│   ├── dao/                       # Data Access Objects
│   │   ├── QuestDao.kt            # Quest CRUD operations
│   │   └── PlayerDao.kt           # Player state operations
│   ├── repository/                # Repository Layer
│   │   ├── QuestRepository.kt     # Single source of truth for quests
│   │   └── PlayerRepository.kt    # Single source of truth for player state
│   └── database/
│       └── AppDatabase.kt         # Main Room database
│
├── ml/                            # Machine Learning (Opt-In)
│   └── tflite/
│       └── ImageVerifier.kt       # TF Lite image classification stub
│
├── fediverse/                     # ActivityPub Integration (Opt-In)
│   ├── activitypub/
│   │   └── ActivityPubTypes.kt    # ActivityPub protocol types
│   └── client/
│       └── ActivityPubClient.kt   # Fediverse API client
│
├── companion/                     # Companion System
│   ├── state/
│   │   └── CompanionState.kt      # State machine for evolution
│   └── evolution/
│       └── CompanionEvolutionManager.kt  # Evolution management
│
├── Config.kt                      # App configuration constants
├── FediQuestApp.kt                # Application class
├── MainActivity.kt                # Main UI activity + CameraX integration
├── PlayerProfile.kt               # Player progression model
├── QuestVerifier.kt               # Quest proof verification service
└── SpawnFetcher.kt                # Quest spawn fetching
```

## Components

### 1. Room Database (`data/`)

**Build Configuration:**
```kotlin
// app/build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "1.9.22-1.0.16"
}

dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
```

**Entities:**

- `QuestEntity`: Stores quest data with typed enum
  ```kotlin
  enum class QuestType { SOCIAL, ECOLOGICAL, CREATIVE }
  
  data class QuestEntity(
      @PrimaryKey val id: String,
      val title: String,
      val description: String,
      val type: QuestType,
      val locationLat: Double,
      val locationLng: Double,
      val radiusMeters: Float = 50f,
      val xpReward: Int,
      val imageUrl: String?,
      val isCompleted: Boolean = false,
      val createdAt: Long,
      val fediverseSynced: Boolean = false // For ActivityPub queue
  )
  ```

- `PlayerStateEntity`: Tracks player state and companion evolution
  ```kotlin
  data class PlayerStateEntity(
      @PrimaryKey val userId: String = "local_player",
      val totalXP: Int = 0,
      val level: Int = 1,
      val avatarSkinId: String = "default",
      val companionId: String = "starter",
      val companionEvolutionStage: Int = 0,
      val lastQuestCompletedAt: Long? = null
  )
  ```

**DAOs:**

- `QuestDao`: Async operations for quests
  - `getActiveQuests(): Flow<List<QuestEntity>>` - Stream of incomplete quests
  - `getQuestById(questId: String): QuestEntity?` - Get specific quest
  - `insertQuest(quest: QuestEntity)` - Add/update quest
  - `markQuestCompleted(questId: String)` - Mark as done
  - `getUnsyncedCompletions(): List<QuestEntity>` - Get quests pending Fediverse sync

- `PlayerDao`: Player state operations
  - `getPlayerState(userId: String): Flow<PlayerStateEntity?>` - Stream player state
  - `addXP(userId: String, amount: Int)` - Add XP
  - `updateCompanionStage(userId: String, stage: Int)` - Update evolution
  - `getTopPlayers(): Flow<List<PlayerStateEntity>>` - Local leaderboard

**Repositories:**

- `QuestRepository`: Single source of truth for quest data
  - Abstracts DAO operations
  - Handles completion logic
  - Manages Fediverse sync queue
  
- `PlayerRepository`: Single source of truth for player state
  - XP management
  - Level updates
  - Companion evolution tracking

**Features:**
- Offline-first storage
- Automatic caching with sync timestamps
- Coroutines + Flow for reactive UI updates
- Typed quest categories via `QuestType` enum
- Fediverse sync tracking via `fediverseSynced` flag

### 2. TF Lite Image Verifier (`ml/tflite/`)

**Purpose:** Verify quest completion photos using on-device ML

**Stub Mode:** When no `.tflite` model is present:
- Returns success with `isStubMode = true`
- App functions normally without AI verification
- No network calls or external dependencies

**Opt-In:** To enable AI verification:
1. Add `quest_verifier.tflite` to `assets/models/`
2. User must explicitly enable AI features in settings

### 3. Quest Verifier Service (`QuestVerifier.kt`)

**Purpose:** Combine TF Lite image verification with GPS proximity and timestamp validation

**Features:**
- **GPS Validation**: Checks user is within 50m of quest location
- **Timestamp Validation**: Ensures proof submitted within 5-minute window
- **Image Verification**: Uses TF Lite (or stub mode) for image classification
- **Confidence Scoring**: Returns confidence score (0.0 - 1.0) for verification
- **XP Award Calculation**: Awards XP based on verification success and confidence

**Usage:**
```kotlin
// Initialize during app startup
QuestVerifier.initialize(context)

// Verify quest proof
val result = QuestVerifier.verify(
    questId = "tree_cleanup_001",
    image = capturedBitmap,
    userLocation = currentLocation,
    questLocation = quest.location,
    questTimestamp = quest.timestamp,
    xpReward = 100
)

if (result.confidence >= 0.85f && result.gpsValid && result.timestampValid) {
    // Success - award XP
    playerProfile.addXP(result.xpAward)
} else {
    // Failed - show retry dialog
    showRetryDialog("Proof not verified")
}
```

**Offline-First:** All verification runs locally on device. No network required.

### 4. ActivityPub Client (`fediverse/`)

**Purpose:** Share quest completions to decentralized social networks

**Disabled by Default:**
- `isFediverseEnabled()` returns `false` initially
- User must configure instance URL and authenticate
- All features check `isFediverseEnabled()` before executing

**Supported Activities:**
- Quest completion posts
- Companion evolution announcements
- Following other players

**STUB Implementation:**
- Currently logs actions instead of making real HTTP requests
- Full implementation would require HTTP Signatures library

### 5. Companion Evolution System (`companion/`)

**State Machine:**
```
Egg → Hatchling → Young → Adult → Mature → Elder → Legendary
```

**Features:**
- Bond level tracking (0-100)
- Mood system (Happy, Excited, Tired, Sad, etc.)
- Personality traits affecting bonuses
- Interaction types (Pet, Feed, Play, Rest)
- Evolution based on bond level thresholds

**Offline-First:**
- All companion state stored locally
- Evolution events logged for optional ActivityPub sharing

## Usage Examples

### Accessing Database
```kotlin
val db = FediQuestApp.getDatabase()
val quests = db.questDao().getAllActiveQuests().collect { ... }
db.playerXpDao().addXP(playerId, 100)
```

### Using Image Verifier
```kotlin
val verifier = FediQuestApp.getImageVerifier()
verifier.initialize() // Loads model or enters stub mode
val result = verifier.verifyImage(bitmap, "tree")
if (result.isStubMode) { /* Handle gracefully */ }
```

### Quest Verification
```kotlin
// Initialize during app startup
QuestVerifier.initialize(context)

// Submit quest proof with image + GPS + timestamp validation
lifecycleScope.launch(Dispatchers.IO) {
    val result = QuestVerifier.verify(
        questId = questId,
        image = capturedBitmap,
        userLocation = currentLocation,
        questLocation = quest.location,
        questTimestamp = quest.timestamp,
        xpReward = 100
    )
    
    withContext(Dispatchers.Main) {
        if (result.confidence >= 0.85f && result.gpsValid && result.timestampValid) {
            playerProfile.addXP(result.xpAward)
            showSuccessDialog(result.xpAward)
        } else {
            showRetryDialog("Proof not verified")
        }
    }
}
```

### ActivityPub (Opt-In)
```kotlin
val client = FediQuestApp.getActivityPubClient()
if (client.isFediverseEnabled()) {
    client.postQuestCompletion(questActivity)
}
```

### Companion Management
```kotlin
val manager = FediQuestApp.getCompanionManager()
val companion = manager.getOrCreateCompanion("bee", "Busy Bee", "🐝")
manager.setActiveCompanion("bee")
manager.interactWithCompanion(InteractionType.PLAY)
```

## Configuration

All feature flags are in `Config.kt`:
- Server URLs (local development defaults)
- Quest types and rewards
- Companion definitions
- Level progression settings

## Next Steps

1. **Database Migration**: Add proper migration strategy for production
2. **TF Lite Model**: Train and add quest verification model
3. **ActivityPub**: Implement full HTTP Signatures for Mastodon compatibility
4. **Companion Persistence**: Save companion state to Room database
5. **UI Integration**: Connect components to activities/fragments
