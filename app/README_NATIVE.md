# FediQuest Native Android Integration Guide

This document covers integrating native AR libraries (ARToolKit primary, ARCore alternative) into the FediQuest Android app.

## Architecture Overview

The native Android flow is **secondary** to the WebAR demo. The app defaults to launching the web demo but includes integration points for native AR.

```
app/
├── src/main/
│   ├── java/com/fediquest/app/
│   │   └── MainActivity.kt      # Chooses web vs native flow
│   ├── jniLibs/                  # Native .so libraries (not included)
│   │   ├── arm64-v8a/
│   │   └── armeabi-v7a/
│   └── assets/                   # Native assets/models
├── CMakeLists.txt               # Native build config
└── README_NATIVE.md             # This file
```

## ARToolKit Integration (Primary Native Option)

### Download ARToolKit

ARToolKit is open-source and FOSS-compatible:

1. **GitHub**: https://github.com/artoolkitx/artoolkit5
2. **Prebuilt binaries**: Check releases or build from source
3. **License**: LGPL v3 (compatible with FOSS projects)

### Building ARToolKit from Source

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

### Placing ARToolKit Libraries

After building or downloading:

```
app/src/main/jniLibs/
├── arm64-v8a/
│   ├── libAR.so
│   ├── libARw.so
│   └── libglog.so
└── armeabi-v7a/
    ├── libAR.so
    ├── libARw.so
    └── libglog.so
```

**Note**: These `.so` files are NOT included in the repository (>500MB limit).

### CMakeLists.txt Configuration

See `app/CMakeLists.txt` for native build configuration snippet.

## ARCore Integration (Alternative)

ARCore is Google's AR platform. It's included as an **optional alternative** but NOT required for core functionality.

### Adding ARCore Dependency

In `app/build.gradle.kts`:

```kotlin
dependencies {
    // Optional ARCore support
    implementation("com.google.ar:core:1.41.0")
    implementation("com.google.ar.sceneform:core:1.17.1")
}
```

### ARCore Feature Detection

Check ARCore availability at runtime:

```kotlin
val availability = ArCoreApk.getInstance().checkAvailability(context)
when {
    availability.isTransient -> Retry later
    availability.isSupported -> Proceed with ARCore
    else -> Fall back to web demo or ARToolKit
}
```

### DeGoogle Note

ARCore requires Google Play Services on most devices. For a fully deGoogled experience:
- Use ARToolKit instead
- Or rely on the WebAR demo (primary flow)

## MainActivity Integration Points

`MainActivity.kt` includes toggles for different AR flows:

```kotlin
// In MainActivity.kt
enum class ARMode {
    WEB_DEMO,      // Default: Launch web view with A-Frame/AR.js
    ARTOOLKIT,     // Native ARToolKit
    ARCORE         // Native ARCore (optional)
}

private val currentMode = ARMode.WEB_DEMO  // Change to switch modes
```

### Switching to Native Mode

1. Edit `MainActivity.kt`
2. Change `currentMode` to `ARTOOLKIT` or `ARCORE`
3. Ensure native libraries are placed in `jniLibs/`
4. Build APK

## NDK Setup

### Prerequisites

- Android NDK r25c or newer
- CMake 3.22+
- Ninja build system

### Installing NDK

Via Android Studio SDK Manager:
1. Open SDK Manager
2. Go to "SDK Tools" tab
3. Check "NDK (Side by side)"
4. Install version 25.x or newer

Or download manually: https://developer.android.com/ndk/downloads

### Environment Variables

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/25.2.9519653
```

## Building Native APK Locally

### Clean Build

```bash
# Clean caches first
./scripts/clean_caches.sh

# Build debug APK
cd app
./gradlew clean assembleDebug

# Output location
ls -lh build/outputs/apk/debug/app-debug.apk
```

### Build Variants

```bash
# Debug build (default)
./gradlew assembleDebug

# Release build (requires signing)
./gradlew assembleRelease
```

## Native Assets Placement

### 3D Models for Native AR

Place glTF/GLB models in:

```
app/src/main/assets/models/
├── tree.glb
├── recycle_bin.glb
└── ...
```

Update spawn loader to reference these paths.

### Marker Files (ARToolKit)

For marker-based AR:

```
app/src/main/assets/markers/
├── marker1.dat
├── marker2.dat
└── patterns.xml
```

## GPS Integration (Native)

Use Android's FusedLocationProvider for GPS:

```kotlin
// Example location request
val locationRequest = LocationRequest.Builder(
    Priority.PRIORITY_HIGH_ACCURACY,
    10000  // Update interval ms
).build()

// Request location updates
locationClient.requestLocationUpdates(
    locationRequest,
    locationCallback,
    Looper.getMainLooper()
)
```

## Permission Requirements

Add to `AndroidManifest.xml`:

```xml
<!-- Camera for AR -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />

<!-- GPS for location-based spawns -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Internet for fetching spawn data -->
<uses-permission android:name="android.permission.INTERNET" />
```

## Testing Native AR

### On Physical Device

```bash
# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell monkey -p com.fediquest.app.debug -c android.intent.category.LAUNCHER 1

# View logs
adb logcat | grep -E "FediQuest|ARToolKit|ARCore"
```

### ARCore Compatibility Check

Not all devices support ARCore. Check compatibility:
- https://developers.google.com/ar/devices

For unsupported devices, fall back to:
1. ARToolKit mode
2. WebAR demo (default)

## Troubleshooting

### UnsatisfiedLinkError

**Problem**: Native library not found

**Solution**:
- Verify `.so` files are in correct `jniLibs/` directory
- Check ABI matches device architecture
- Rebuild native libraries

### GPS Not Working

**Problem**: Location updates not received

**Solution**:
- Check permissions granted at runtime
- Enable location services on device
- Test on physical device (emulator GPS limited)

### ARCore Session Failed

**Problem**: ARCore session won't start

**Solution**:
- Check device compatibility
- Update Google Play Services
- Fall back to ARToolKit or WebAR

## Performance Considerations

- Target 60 FPS for smooth AR experience
- Limit draw distance for GPS spawns
- Use LOD (Level of Detail) for distant models
- Cache model data locally
- Monitor memory usage on lower-end devices

## Resources

- [ARToolKit Documentation](https://artoolkit.org/documentation/)
- [ARCore Developer Guide](https://developers.google.com/ar/develop)
- [Android NDK Guide](https://developer.android.com/ndk/guides)
- [CMake for Android](https://developer.android.com/ndk/guides/cmake)

## License Notes

- ARToolKit: LGPL v3 (ensure compliance if distributing)
- ARCore: Proprietary (Google terms apply)
- FediQuest code: FOSS (see project license)

Always verify license compatibility before distribution.
