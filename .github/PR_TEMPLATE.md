# FediQuest AR GPS Prototype - PR Template

## Pull Request: feature/ar-gps-prototype-native-first

This PR implements the FediQuest AR + GPS prototype with **Android-native-first approach** using ARToolKit as the primary native option. The repo is text-only with no binaries, placeholders and explicit instructions for native dependencies.

---

## PR Checklist

### Core Requirements

- [ ] **Native Android Skeleton**: Builds successfully with documented steps
- [ ] **ARToolKit Integration**: .so file placement documented (placeholders only)
- [ ] **ETag Handling**: SpawnFetcher.kt demonstrates If-None-Match caching
- [ ] **Server Config**: `server/server.json` has 6 spawn entries with ETags
- [ ] **DeGoogle**: No Google dependencies for core functionality
- [ ] **No Binary Blobs**: No .so, .apk, .glb files committed
- [ ] **Documentation**: All README files complete and accurate
- [ ] **Cache Cleaning**: Commands documented (no scripts committed)

### File Verification

- [ ] `README.md` - Android-native-first documentation
- [ ] `server/server.json` - 6 sample spawn entries with ETags
- [ ] `app/README_NATIVE.md` - Native integration guide
- [ ] `app/CMakeLists.txt` - Native build snippet
- [ ] `app/src/main/java/org/fediquest/MainActivity.kt` - Native AR default
- [ ] `app/src/main/java/org/fediquest/SpawnFetcher.kt` - ETag handling
- [ ] `app/src/main/java/org/fediquest/Config.kt` - Constants & quest types
- [ ] `.github/PR_TEMPLATE.md` - This file

---

## How to Review This PR

### 1. Verify File Structure

```bash
# Check expected files exist
ls -la README.md
ls -la server/server.json
ls -la app/README_NATIVE.md
ls -la app/CMakeLists.txt
ls -la app/src/main/java/org/fediquest/
```

Expected output should show:
- `MainActivity.kt` - Kotlin stub (native AR default)
- `SpawnFetcher.kt` - ETag/If-None-Match handling
- `Config.kt` - Constants and quest type definitions

### 2. Verify No Binary Blobs

```bash
# Check for large files (>1MB) that shouldn't be committed
find . -type f -size +1M \
  ! -path "./.git/*" \
  ! -path "./gradle/*"

# Should NOT find:
# - .so native libraries
# - .apk Android packages
# - .glb/.gltf model files
```

### 3. Check DeGoogle Compliance

Review that NO Google services are required for core features:

| Feature | Implementation | Google-Free? |
|---------|---------------|--------------|
| Maps | OpenStreetMap (native OSM tiles) | ✅ |
| GPS | Android LocationManager | ✅ |
| Auth | None required | ✅ |
| Analytics | None included | ✅ |
| Hosting | Any static host | ✅ |
| AR (Primary) | ARToolKit (FOSS, LGPL v3) | ✅ |
| AR (Optional) | ARCore (alternative only) | ⚠️ Optional |

### 4. Verify ETag Implementation

Check `app/src/main/java/org/fediquest/SpawnFetcher.kt` for:
- [ ] `If-None-Match` header usage
- [ ] `304 Not Modified` response handling
- [ ] ETag storage and retrieval
- [ ] Retry/backoff documentation

### 5. Verify Social-Ecological Quest Types

Check `server/server.json` for 6 quest types:
- [ ] `planting` - Tree planting quests
- [ ] `recycling` - Recycling station quests
- [ ] `cleanup` - Litter cleanup quests
- [ ] `wildflower` - Wildflower garden quests
- [ ] `water` - Water conservation quests
- [ ] `wildlife` - Wildlife habitat quests

Each entry should have:
- Unique ID
- Latitude/longitude coordinates
- Model URL (placeholder)
- ETag field
- Metadata with name, description, type, xpReward

---

## Cache Clean Steps for Reviewers

Before building/testing, clean all caches (documented commands, no scripts):

```bash
# Gradle cache cleaning (REQUIRED before building)
rm -rf ~/.gradle/caches
rm -rf app/build app/.gradle app/.externalNativeBuild app/.cxx

# Then rebuild
cd app
./gradlew clean assembleDebug
```

**Note**: No cache-clean scripts are committed to this repo. Reviewers must run these commands locally as documented.

---

## Building Locally

### Prerequisites

1. Android Studio or command-line SDK tools
2. Android NDK r25c or newer
3. CMake 3.22+

### Environment Setup

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/25.2.9519653
```

### ARToolKit Setup (Required for Full Build)

**Option A: Download Prebuilt**

1. Visit: https://github.com/artoolkitx/artoolkit5/releases
2. Download Android prebuilt package
3. Extract and copy `.so` files to:
   ```
   app/src/main/jniLibs/arm64-v8a/
   app/src/main/jniLibs/armeabi-v7a/
   ```

**Option B: Build from Source**

```bash
git clone https://github.com/artoolkitx/artoolkit5.git
cd artoolkit5
mkdir build && cd build
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI="arm64-v8a" \
  -DANDROID_PLATFORM=android-24
make -j4
cp libAR*.so ../../fediquest/app/src/main/jniLibs/arm64-v8a/
```

### Build APK

```bash
# Clean first (required)
rm -rf ~/.gradle/caches
rm -rf app/build app/.gradle app/.externalNativeBuild app/.cxx

# Build debug APK
cd app
./gradlew assembleDebug

# Output
ls -lh build/outputs/apk/debug/app-debug.apk
```

---

## Architecture Summary

```
┌─────────────────────────────────────────────────────┐
│                  FediQuest                          │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  PRIMARY: Native Android (ARToolKit)        │   │
│  │  - MainActivity.kt (native AR default)      │   │
│  │  - SpawnFetcher.kt (ETag caching)           │   │
│  │  - Config.kt (constants, quest types)       │   │
│  │  - Requires .so files (not included)        │   │
│  │  - Files: app/                              │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  OPTIONAL: ARCore Alternative               │   │
│  │  - Requires Google Play Services            │   │
│  │  - Not required for core flows              │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  Server Config                              │   │
│  │  - server/server.json                       │   │
│  │  - 6 social-ecological spawns               │   │
│  │  - ETag fields for caching                  │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## Known Limitations

1. **Native .so Files**: ARToolKit libraries must be built/downloaded separately (see `app/README_NATIVE.md`)
2. **3D Models**: glTF/GLB files not included; placeholders in `Config.kt`
3. **iOS Support**: Not included in this PR (Android-native-first)
4. **Web Demo**: Removed from this PR (focus on native flow)

---

## Testing Scenarios

### Scenario 1: Local Build Review

1. Follow build instructions above
2. Place ARToolKit .so files in jniLibs/
3. Run `./gradlew assembleDebug`
4. Verify APK builds without errors

### Scenario 2: Code Review (No Build)

1. Review Kotlin stubs in `app/src/main/java/org/fediquest/`
2. Verify ETag implementation in `SpawnFetcher.kt`
3. Check quest types in `Config.kt` and `server/server.json`
4. Confirm DeGoogle compliance

### Scenario 3: Documentation Review

1. Read `README.md` for setup instructions
2. Read `app/README_NATIVE.md` for native integration details
3. Verify cache cleaning commands are documented
4. Check placeholder paths are clearly marked

---

## Post-Merge Tasks

After this PR is merged:

- [ ] Add actual ARToolKit .so files for distribution (separate repo or download)
- [ ] Create/add 3D models for quest types
- [ ] Implement full JSON parsing in SpawnFetcher
- [ ] Add unit tests for ETag caching logic
- [ ] Set up CI/CD for automated builds
- [ ] Configure production server.json hosting

---

## References

- [ARToolKit Documentation](https://artoolkit.org/documentation/)
- [Android NDK Guide](https://developer.android.com/ndk/guides)
- [CMake for Android](https://developer.android.com/ndk/guides/cmake)
- [OpenStreetMap](https://www.openstreetmap.org/)
- [HTTP ETag Specification](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag)

---

## Contact

For questions about this PR, contact the FediQuest maintainers or open an issue.
