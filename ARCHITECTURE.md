# FediQuest App Architecture

## Overview

FediQuest is an offline-first AR quest app with opt-in AI and Fediverse features. This document describes the initialized app structure.

## Core Principles

1. **Offline-First**: All core functionality works without internet connectivity
2. **Opt-In AI/ML**: TensorFlow Lite image verification runs in stub mode when no model is present
3. **Opt-In Fediverse**: ActivityPub social features require explicit user configuration
4. **Local Storage**: Room database stores all quests, XP, and companion data locally

## Directory Structure

```
app/src/main/java/org/fediquest/
├── data/                          # Room Database Layer
│   ├── entity/                    # Database entities
│   │   ├── QuestEntity.kt         # Quest data model with QuestType enum
│   │   ├── PlayerStateEntity.kt   # Player state + companion evolution stage
│   └── dao/                       # Data Access Objects
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
├── QuestVerifier.kt               # Real TF Lite verification + GPS + timestamp
└── SpawnFetcher.kt                # Quest spawn fetching

app/src/main/assets/
└── quest_classifier.tflite        # TF Lite model (downloaded or stub)
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
      val createdAt: Long = System.currentTimeMillis(),
      val fediverseSynced: Boolean = false
  )
  ```

- `PlayerStateEntity`: Tracks player progression
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

- `QuestDao`: Async DAO with Flow support
  - `getActiveQuests()`: Flow<List<QuestEntity>>
  - `getQuestById(questId)`: QuestEntity?
  - `insertQuest(quest)`: suspend
  - `markQuestCompleted(questId)`: suspend
  - `getUnsyncedCompletions()`: List<QuestEntity> (for ActivityPub queue)

- `PlayerDao`: Player state operations
  - `getPlayerState()`: Flow<PlayerStateEntity>
  - `updateXP(xp)`: suspend
  - `updateLevel(level)`: suspend
  - `updateCompanionStage(stage)`: suspend

**Repositories:**

- `QuestRepository`: Single source of truth for quests
  - `activeQuests`: Flow<List<QuestEntity>>
  - `completeQuest(questId)`: Persists completion, triggers XP calculation
  - `syncToFediverse(questId)`: Posts to ActivityPub if opted-in

- `PlayerRepository`: Single source of truth for player state
  - `addXP(amount)`: Updates XP, checks level-up, triggers companion evolution
  - `playerState`: Flow<PlayerStateEntity>

### 2. TF Lite Image Verification (`QuestVerifier.kt`)

**Features:**
- Real TF Lite model loading from assets
- Automatic fallback to stub mode if model missing or < 1MB
- GPS proximity validation (within 50m)
- Timestamp validation (within 5 minutes)
- Confidence threshold (≥ 0.75 for AI mode)
- XP calculation scaled by confidence

**Usage:**
```kotlin
// Initialize during app startup
QuestVerifier.initialize(context)

// Verify quest proof
val result = QuestVerifier.verify(
    questId = "cleanup_001",
    image = capturedBitmap,
    userLocation = currentLocation,
    questLocation = questLocation,
    questTimestamp = quest.createdAt,
    xpReward = quest.xpReward
)

if (result.confidence >= 0.75f && result.gpsValid && result.timestampValid) {
    // Success: Award XP
    playerRepository.addXP(result.xpAward)
} else {
    // Failed: Show retry dialog
}

// Check if running with real AI or stub mode
if (QuestVerifier.isStubMode()) {
    Log.d("Demo", "Running in stub mode (no AI model)")
}
```

**Model Download:**
```bash
# Place EfficientNet-Lite0 in assets
mkdir -p app/src/main/assets
curl -L "https://tfhub.dev/tensorflow/lite-model/efficientnet/lite0/classification/2/default/1?lite-format=tflite" \
  -o app/src/main/assets/quest_classifier.tflite
```

### 3. ActivityPub Client (`fediverse/`)

**Disabled by Default:**
- `ActivityPubClient.isFediverseEnabled()` returns `false` until user configures
- All posts are logged instead of sent when disabled
- Requires explicit user configuration (server URL, credentials)

**Features:**
- Post quest completions to Mastodon
- Share companion evolution milestones
- Decentralized guild support (future)

### 4. Companion Evolution (`companion/`)

**State Machine:**
- 7 evolution stages: Egg → Baby → Child → Teen → Adult → Elder → Legendary
- 8 moods: Neutral, Happy, Excited, Sleepy, Hungry, Sad, Angry, Playful
- Personality traits with XP bonuses
- Bond level tracking (0-100)

**Interactions:**
- Pet, Feed, Play, Rest actions affect mood and bond
- Evolution triggered by XP thresholds + bond level

### 5. CameraX Integration (`MainActivity.kt`)

**Quest Proof Capture:**
```kotlin
private fun captureAndSubmitQuestProof(questId: String, userLocation: Location) {
    val photoFile = File(externalCacheDir, "${System.currentTimeMillis()}.jpg")
    
    imageCapture?.takePicture(
        OutputFileOptions.Builder(photoFile).build(),
        cameraExecutor
    ) { bitmap ->
        submitQuestProof(questId, bitmap, userLocation)
    }
}

private fun submitQuestProof(questId: String, image: Bitmap, location: Location) {
    lifecycleScope.launch(Dispatchers.IO) {
        val quest = questRepository.getQuestById(questId)
        val result = QuestVerifier.verify(
            questId = questId,
            image = image,
            userLocation = location,
            questLocation = Location("").apply {
                latitude = quest.locationLat
                longitude = quest.locationLng
            },
            questTimestamp = quest.createdAt,
            xpReward = quest.xpReward
        )
        
        withContext(Dispatchers.Main) {
            if (result.confidence >= 0.75f && result.gpsValid && result.timestampValid) {
                questRepository.completeQuest(questId)
                playerRepository.addXP(result.xpAward)
                showSuccessDialog(result.xpAward)
            } else {
                showRetryDialog("Verification failed")
            }
        }
    }
}
```

## Demo Flow

1. **App Launch**: Initialize QuestVerifier with TF Lite model (or stub mode)
2. **Quest Selection**: Display active quests from Room DB
3. **GPS Validation**: Create demo quest at user's current location
4. **Capture Proof**: User taps "📸 Capture Proof" button in QuestDetailFragment
5. **Verification**: 
   - TF Lite classifies image (or stub mode simulates confidence)
   - GPS distance checked (≤ 50m)
   - Timestamp validated (≤ 5 min)
6. **Success**: 
   - Quest marked completed in Room DB
   - XP awarded to player
   - Companion evolution check triggered
   - Optional: Queue for Fediverse sync
7. **Failure**: Show retry dialog with reason

## Build & Run

```bash
# Sync dependencies
./gradlew sync

# Download TF Lite model (optional, will use stub mode if missing)
curl -L "https://tfhub.dev/tensorflow/lite-model/efficientnet/lite0/classification/2/default/1?lite-format=tflite" \
  -o app/src/main/assets/quest_classifier.tflite

# Build debug APK
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Testing Offline-First

1. Enable airplane mode
2. Launch app → Quests load from Room DB
3. Complete quest → Verification runs locally
4. XP awarded → Companion evolves offline
5. Disable airplane mode → Optional Fediverse sync

## Future Enhancements

- [ ] **GPS→AR Conversion**: GeoAnchor utility for ARCore placement
- [ ] **LLM Quest Generator**: Dynamic quest creation with location context
- [ ] **WorkManager**: Background Fediverse sync queue
- [ ] **Multiplayer**: Decentralized guilds via ActivityPub
