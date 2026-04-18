# FediQuest — Open FOSS AR + GPS Prototype

FediQuest is an open-source augmented reality GPS-based experience platform that encourages people to:
- **Go outside** and explore their environment
- **Help each other** through community quests  
- **Do something good for the environment**

Players earn XP by completing social-ecological quests and unlock **digital goods** like:
- 🎨 Avatar skins (cosmetic upgrades)
- 🐾 Companion creatures (with special abilities)
- ⭐ Level upgrades and titles
- 🌍 Environmental impact tracking

This prototype prioritizes an **Android-native-first approach** with **SceneView** (open-source Sceneform fork) as the primary AR engine, while retaining a minimal, text-only repo with no binaries.

## Table of Contents

- [Quick Start](#quick-start)
- [Setup Script](#setup-script)
- [Game Features](#game-features)
- [Architecture Overview](#architecture-overview)
- [DeGoogle Checklist](#degoogle-checklist)
- [Native Android Build](#native-android-build)
- [Server Configuration](#server-configuration)
- [Cache Cleaning & Reproducible Builds](#cache-cleaning--reproducible-builds)
- [File Placement Guide](#file-placement-guide)
- [PR Checklist](#pr-checklist)

## Quick Start

### Option 1: Automated Setup (Recommended)

Use the provided setup script to automatically configure and build the project:

```bash
# Make the script executable
chmod +x setup.sh

# Run the setup script
./setup.sh
```

The script will:
- Detect your Android SDK location
- Clean build caches for reproducible builds
- Create required asset directories
- Set up the Gradle wrapper
- Build the debug APK
- Provide installation instructions

### Option 2: Manual Setup

```bash
# Ensure Android SDK/NDK installed
export ANDROID_HOME=/path/to/android/sdk
export ANDROID_NDK_HOME=/path/to/android/ndk

# No external .so files needed - SceneView is included as a Gradle dependency
# See app/build.gradle.kts for dependencies

# Clean caches first (required before building)
rm -rf ~/.gradle/caches
rm -rf app/build app/.gradle

# Build debug APK
cd app
./gradlew assembleDebug

# APK output: app/build/outputs/apk/debug/app-debug.apk
# Install on device: adb install -r build/outputs/apk/debug/app-debug.apk
```

## Setup Script

The `setup.sh` script automates the entire build process:

```bash
# Make executable and run
chmod +x setup.sh
./setup.sh
```

**What the script does:**

1. **Environment Detection**: Automatically finds Android SDK if ANDROID_HOME is not set
2. **Cache Cleaning**: Removes Gradle caches for reproducible builds
3. **Asset Setup**: Creates required directories for 3D models and markers
4. **Dependency Installation**: Installs required SDK components (if sdkmanager available)
5. **Build**: Compiles the debug APK with all dependencies
6. **Guidance**: Provides clear next steps for installation and testing

**Requirements:**

- Java JDK 17 or higher
- Android SDK (API 34 recommended)
- Internet connection (for Gradle dependencies)

**Output:**

On success, the script produces:
- `app/build/outputs/apk/debug/app-debug.apk`
- Asset directories ready for 3D models
- Clear instructions for device installation


## Game Features

### Social-Ecological Quests

FediQuest features 6 types of environmental quests that reward players for real-world positive actions:

| Quest Type | Description | Base XP | Emoji |
|------------|-------------|---------|-------|
| 🌱 Tree Planting | Plant trees in your community | 100 XP | 🌱 |
| ♻️ Recycling Station | Visit recycling centers | 75 XP | ♻️ |
| 🧹 Cleanup Zone | Participate in cleanup events | 50 XP | 🧹 |
| 🌸 Wildflower Garden | Plant native wildflowers | 120 XP | 🌸 |
| 💧 Water Conservation | Support water conservation efforts | 90 XP | 💧 |
| 🦋 Wildlife Habitat | Create wildlife-friendly spaces | 110 XP | 🦋 |

### Avatar & Progression System

**Level Titles**: Newcomer → Helper → Supporter → Advocate → Champion → Guardian → Protector → Hero → Legend → Eco Warrior

**Avatar Skins** (unlocked by leveling up):
- Default Outfit (Level 1) 👕
- Nature Explorer (Level 3) 🥾
- Eco Scientist (Level 5) 🔬
- Master Gardener (Level 7) 👒
- Earth Guardian (Level 10) 🛡️
- Climate Activist (Level 12) 📢
- Eco Legend (Level 15) 👑

### Companion System

Companions are creature friends that accompany players and provide special bonuses:

| Companion | Unlock Level | Special Ability |
|-----------|--------------|-----------------|
| 🐝 Busy Bee | 2 | Highlights nearby flower planting spots |
| 🐦 Song Bird | 4 | Alerts you to wildlife conservation areas |
| 🦊 Forest Fox | 6 | Finds hidden cleanup opportunities |
| 🐢 Sea Turtle | 8 | Guides to water conservation sites |
| 🦉 Wise Owl | 10 | Provides +10% XP bonus on all quests |
| 🦋 Monarch Butterfly | 12 | Reveals rare quest locations |

### Fediverse Integration

- Share quest completions to your Fediverse instance (ActivityPub protocol)
- Configure your own instance (default: mastodon.social)
- Optional feature - no social account required to play
- Activities include: quest started, quest completed, level up, companion unlocked, skin equipped

### Environmental Impact Tracking

Track your real-world positive impact:
- Trees planted
- Recycling trips made
- Cleanup events participated in
- Wildflowers planted
- Water saved (liters)
- Wildlife habitats created

## Architecture Overview

```
FediQuest
├── app/                    # Primary: Native Android skeleton
│   ├── README_NATIVE.md    # SceneView integration guide
│   ├── CMakeLists.txt      # Native build configuration (optional)
│   └── src/main/java/org/fediquest/
│       ├── MainActivity.kt     # Kotlin stub (native AR default)
│       ├── SpawnFetcher.kt     # ETag/If-None-Match handling
│       └── Config.kt           # Constants & placeholders
├── server/                 # Spawn configuration
│   └── server.json         # 6 sample spawn entries with ETags
└── .github/
    └── PR_TEMPLATE.md      # Reviewer checklist
```

### Primary vs Secondary Flows

| Flow | Technology | Priority | Install Required |
|------|------------|----------|------------------|
| **Native AR** | SceneView (Sceneform fork) | Primary | Gradle dependency only |
| Native AR | ARCore (optional) | Alternative | Google deps (optional) |
| Web Demo | Removed from this PR | N/A | N/A |

## DeGoogle Checklist

FediQuest is committed to avoiding proprietary Google services for core features:

- ✅ **Maps**: Uses OpenStreetMap (native OSM tile rendering, no Google Maps API)
- ✅ **GPS**: Uses Android LocationManager/FusedLocationProvider (no Google Location Services required for core)
- ✅ **Authentication**: No Google Sign-In required; optional OAuth with self-hosted providers
- ✅ **Analytics**: No analytics by default; opt-in privacy-respecting solutions only
- ✅ **Hosting**: Works on any static host; no Firebase required
- ✅ **ARCore**: Optional alternative only; NOT required for core app flows
- ✅ **Play Services**: Not required for core app flows

### What We Avoid

- ❌ Google Play Services (core flows)
- ❌ Google Maps API
- ❌ Google Sign-In / OAuth
- ❌ Firebase (unless self-hosted alternative)
- ❌ Google Analytics
- ❌ Proprietary tracking SDKs

## Native Android Build

### Prerequisites

- Android Studio or command-line SDK tools
- Android SDK 34 (or newer)
- No NDK required for SceneView (optional only for custom native code)

### Environment Setup

```bash
# Set environment variables
export ANDROID_HOME=$HOME/Android/Sdk

# Verify installation
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --list
```

### SceneView Integration (Primary Native Option)

SceneView is an open-source AR library based on Sceneform, actively maintained and FOSS-friendly.

**No manual library setup required** - SceneView is included as a Gradle dependency:

```kotlin
// app/build.gradle.kts
dependencies {
    // SceneView for AR rendering (primary AR engine)
    implementation("io.github.sceneview:arsceneview:0.10.0")
    implementation("io.github.sceneview:sceneview:0.10.0")
}
```

**Benefits over ARToolKit:**
- ✅ Actively maintained (recent updates)
- ✅ No manual .so file management
- ✅ Pure Kotlin/Java - no NDK required
- ✅ Apache 2.0 license (FOSS-friendly)
- ✅ Built-in ARCore support with fallbacks
- ✅ glTF/GLB model support out of the box

**Resources:**
- GitHub: https://github.com/SceneView/sceneview
- Documentation: https://github.com/SceneView/sceneview#readme

### Building the APK

```bash
# Clean caches (REQUIRED before building)
rm -rf ~/.gradle/caches
rm -rf app/build app/.gradle

# Navigate to app directory
cd app

# Build debug APK
./gradlew assembleDebug

# Output location
ls -lh build/outputs/apk/debug/app-debug.apk

# Install on device
adb install -r build/outputs/apk/debug/app-debug.apk
```

### Build Variants

```bash
# Debug build (default)
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

See `app/README_NATIVE.md` for detailed SceneView integration instructions.

## Server Configuration

The `server/server.json` file contains spawn point definitions with ETag support:

```json
{
  "spawns": [
    {
      "id": "spawn_001",
      "latitude": 40.7128,
      "longitude": -74.0060,
      "modelUrl": "models/tree.glb",
      "etag": "\"v1-abc123\"",
      "metadata": {
        "name": "Eco Tree #1",
        "description": "Plant a tree in Central Park",
        "type": "planting",
        "xpReward": 100
      }
    }
  ]
}
```

See `server/server.json` for 6 sample entries with social-ecological quest types.

### ETag Caching

The `SpawnFetcher.kt` implements ETag/If-None-Match handling:

1. First request: Fetch full `server.json`, store ETag
2. Subsequent requests: Send `If-None-Match: <etag>` header
3. Server returns `304 Not Modified` if data unchanged
4. Reduces bandwidth and improves load times

## Cache Cleaning & Reproducible Builds

To ensure reproducible builds and clean state for CI/reviewers, run these commands locally before building:

### Gradle Cache Cleaning

```bash
# Remove Gradle build outputs
rm -rf app/build
rm -rf app/.gradle
rm -rf .gradle

# Remove Gradle cache (global)
rm -rf ~/.gradle/caches
```

### NDK Cache Cleaning

```bash
# Remove CMake build artifacts
rm -rf app/.externalNativeBuild
rm -rf app/.cxx
```

### Full Clean Build

```bash
# From project root
rm -rf app/build app/.gradle app/.externalNativeBuild app/.cxx ~/.gradle/caches

# Then rebuild
cd app
./gradlew clean assembleDebug
```

**Note**: No cache-clean scripts are committed to this repo. Reviewers and CI must run these commands locally as documented.

## File Placement Guide

### 3D Models for Quests (Not Included)

```
app/src/main/assets/models/
├── tree.glb              # Planting quest model
├── recycle_bin.glb       # Recycling quest model
├── cleanup_bag.glb       # Cleanup quest model
├── wildflower.glb        # Wildflower planting model
├── water_station.glb     # Water conservation model
└── birdhouse.glb         # Wildlife habitat model
```

Place your own glTF/GLB models in this directory. Update `Config.kt` with correct paths.

**Note**: SceneView handles model loading automatically - no native library setup required.

## PR Checklist

Before submitting or reviewing this PR:

- [ ] Native Android skeleton builds successfully
- [ ] SceneView dependency enabled in `build.gradle.kts`
- [ ] `server/server.json` has 6 spawn entries with ETags
- [ ] `SpawnFetcher.kt` demonstrates ETag/If-None-Match handling with JSON parsing
- [ ] `MainActivity.kt` defaults to native AR mode
- [ ] No binary blobs committed (no `.so`, `.apk`, `.glb` files)
- [ ] README documentation complete
- [ ] DeGoogle checklist verified (no Google dependencies for core)
- [ ] `setup.sh` script provided for automated builds
- [ ] All placeholder files have clear placement instructions

## Contributing

This is a FOSS project. Contributions welcome!

- Native Android enhancements (SceneView)
- Social-ecological quest implementations
- Privacy-focused feature additions
- Documentation improvements

## Acknowledgments

- [SceneView](https://github.com/SceneView/sceneview) - Open-source AR library (Apache 2.0)
- [OpenStreetMap](https://www.openstreetmap.org/) - Open map data
- [Android SDK](https://developer.android.com/) - Android development

## License Notes

- SceneView: Apache 2.0 (FOSS-friendly)
- ARCore: Proprietary (Google terms apply, optional only)
- FediQuest code: FOSS (license omitted per request)

Always verify license compatibility before distribution.
