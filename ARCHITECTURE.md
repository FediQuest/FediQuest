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
│   │   ├── QuestEntity.kt         # Quest data model
│   │   └── PlayerXpEntity.kt      # Player XP/level tracking
│   ├── dao/                       # Data Access Objects
│   │   ├── QuestDao.kt            # Quest CRUD operations
│   │   └── PlayerXpDao.kt         # XP/level operations
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
├── MainActivity.kt                # Main UI activity
├── PlayerProfile.kt               # Player progression model
└── SpawnFetcher.kt                # Quest spawn fetching
```

## Components

### 1. Room Database (`data/`)

**Entities:**
- `QuestEntity`: Stores quest data (type, location, rewards, completion status)
- `PlayerXpEntity`: Tracks player XP, level, daily streaks

**DAOs:**
- `QuestDao`: Async operations for quests with Flow support
- `PlayerXpDao`: XP tracking and leaderboard queries

**Features:**
- Offline-first storage
- Automatic caching with sync timestamps
- Coroutines + Flow for reactive UI updates

### 2. TF Lite Image Verifier (`ml/tflite/`)

**Purpose:** Verify quest completion photos using on-device ML

**Stub Mode:** When no `.tflite` model is present:
- Returns success with `isStubMode = true`
- App functions normally without AI verification
- No network calls or external dependencies

**Opt-In:** To enable AI verification:
1. Add `quest_verifier.tflite` to `assets/models/`
2. User must explicitly enable AI features in settings

### 3. ActivityPub Client (`fediverse/`)

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

### 4. Companion Evolution System (`companion/`)

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
