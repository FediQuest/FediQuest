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

This prototype prioritizes an **Android-native-first approach** with ARToolKit as the primary native option, while retaining a minimal, text-only repo with no binaries.

## Table of Contents

- [Quick Start](#quick-start)
- [Game Features](#game-features)
- [Architecture Overview](#architecture-overview)
- [DeGoogle Checklist](#degoogle-checklist)
- [Native Android Build](#native-android-build)
- [Server Configuration](#server-configuration)
- [Cache Cleaning & Reproducible Builds](#cache-cleaning--reproducible-builds)
- [File Placement Guide](#file-placement-guide)
- [PR Checklist](#pr-checklist)

## Quick Start

```bash
# Ensure Android SDK/NDK installed
export ANDROID_HOME=/path/to/android/sdk
export ANDROID_NDK_HOME=/path/to/android/ndk

# Place ARToolKit .so files in app/src/main/jniLibs/
# See app/README_NATIVE.md for download/build instructions

# Clean caches first (required before building)
rm -rf ~/.gradle/caches
rm -rf app/build app/.gradle

# Build debug APK
cd app
./gradlew assembleDebug

# APK output: app/build/outputs/apk/debug/app-debug.apk
# Install on device: adb install -r build/outputs/apk/debug/app-debug.apk
```

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
│   ├── README_NATIVE.md    # ARToolKit/ARCore integration guide
│   ├── CMakeLists.txt      # Native build configuration
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
| **Native AR** | ARToolKit | Primary | Android build + .so files |
| Native AR | ARCore (optional) | Alternative | Android build + Google deps |
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
- Android NDK r25c or newer
- CMake 3.22+
- ARToolKit prebuilt libraries (see below)

### Environment Setup

```bash
# Set environment variables
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/25.2.9519653

# Verify installation
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --list
$ANDROID_NDK_HOME/ndk-build --version
```

### ARToolKit Integration (Primary Native Option)

**Option A: Download Prebuilt Libraries**

1. Visit: https://github.com/artoolkitx/artoolkit5/releases
2. Download the latest Android prebuilt package
3. Extract and copy `.so` files to `app/src/main/jniLibs/{abi}/`

**Option B: Build from Source**

```bash
# Clone ARToolKit
git clone https://github.com/artoolkitx/artoolkit5.git
cd artoolkit5

# Configure with NDK
mkdir build && cd build
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI="arm64-v8a" \
  -DANDROID_PLATFORM=android-24

# Build
make -j4

# Copy .so files to FediQuest project
cp libAR*.so ../../fediquest/app/src/main/jniLibs/arm64-v8a/
```

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

See `app/README_NATIVE.md` for detailed native integration instructions.

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

### Native Libraries (Not Included in Repo)

```
app/src/main/jniLibs/
├── arm64-v8a/
│   ├── libAR.so          # ARToolKit core library
│   ├── libARw.so         # ARToolKit video library
│   └── libglog.so        # Google logging (if needed)
└── armeabi-v7a/
    ├── libAR.so
    ├── libARw.so
    └── libglog.so
```

**These `.so` files are NOT included in the repository** (>500MB limit). Follow instructions in `app/README_NATIVE.md` to download or build them.

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

## PR Checklist

Before submitting or reviewing this PR:

- [ ] Native Android skeleton builds successfully
- [ ] ARToolKit `.so` files placed in `jniLibs/` (or documented as placeholder)
- [ ] `server/server.json` has 6 spawn entries with ETags
- [ ] `SpawnFetcher.kt` demonstrates ETag/If-None-Match handling
- [ ] `MainActivity.kt` defaults to native AR mode
- [ ] No binary blobs committed (no `.so`, `.apk`, `.glb` files)
- [ ] README documentation complete
- [ ] DeGoogle checklist verified (no Google dependencies for core)
- [ ] Cache cleaning commands documented (no scripts committed)
- [ ] All placeholder files have clear placement instructions

## Contributing

This is a FOSS project. Contributions welcome!

- Native Android enhancements (ARToolKit)
- Social-ecological quest implementations
- Privacy-focused feature additions
- Documentation improvements

## Acknowledgments

- [ARToolKit](https://artoolkit.org/) - Native AR library (LGPL v3)
- [OpenStreetMap](https://www.openstreetmap.org/) - Open map data
- [Android NDK](https://developer.android.com/ndk) - Native development

## License Notes

- ARToolKit: LGPL v3 (ensure compliance if distributing)
- ARCore: Proprietary (Google terms apply, optional only)
- FediQuest code: FOSS (license omitted per request)

Always verify license compatibility before distribution.
