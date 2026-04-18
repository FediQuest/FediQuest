# FediQuest Native Android Integration Guide

This document covers integrating **SceneView** (open-source Sceneform fork) into the FediQuest Android app. This is the **primary** flow for the FediQuest prototype.

## Architecture Overview

The native Android flow is the **primary** demo for this PR. The app defaults to native AR mode using SceneView.

```
app/
├── src/main/
│   ├── java/org/fediquest/
│   │   ├── MainActivity.kt      # Native AR entry point (SceneView)
│   │   ├── SpawnFetcher.kt      # ETag-based spawn data fetching
│   │   └── Config.kt            # App constants
│   ├── assets/                   # 3D models (glTF/GLB)
│   └── AndroidManifest.xml      # App permissions
├── build.gradle.kts             # Gradle dependencies (SceneView included)
└── README_NATIVE.md             # This file
```

## SceneView Integration (Primary Native Option)

### What is SceneView?

**SceneView** is an open-source, actively maintained AR library based on Google's Sceneform. It provides a modern, Kotlin-first API for building AR experiences on Android.

- **GitHub**: https://github.com/SceneView/sceneview
- **License**: Apache 2.0 (FOSS-friendly)
- **Status**: Actively maintained with recent updates
- **Dependencies**: Pure Kotlin/Java - no NDK or manual .so files required

### Benefits over ARToolKit

| Feature | SceneView | ARToolKit |
|---------|-----------|-----------|
| Maintenance | ✅ Active (2024+) | ❌ Inactive |
| Setup | ✅ Gradle dependency | ❌ Manual .so files |
| NDK Required | ❌ No | ✅ Yes |
| License | ✅ Apache 2.0 | LGPL v3 |
| glTF Support | ✅ Built-in | ⚠️ Manual |
| ARCore | ✅ Integrated | ❌ None |

### Adding SceneView Dependency

SceneView is already included in `app/build.gradle.kts`:

```kotlin
dependencies {
    // SceneView for AR rendering (primary AR engine)
    implementation("io.github.sceneview:arsceneview:0.10.0")
    implementation("io.github.sceneview:sceneview:0.10.0")
}
```

**No additional setup required!** Gradle will download all necessary dependencies automatically.

### Basic Usage Example

See `MainActivity.kt` for complete implementation:

```kotlin
// Initialize SceneView
private lateinit var arSceneView: ArSceneView

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    arSceneView = ArSceneView(this)
    setContentView(arSceneView)
    
    // Configure AR session
    arSceneView.arSceneView.session.apply {
        config.depthMode = when {
            isDepthModeSupported(io.github.sceneview.ar.session.DepthMode.AUTOMATIC) -> 
                io.github.sceneview.ar.session.DepthMode.AUTOMATIC
            else -> io.github.sceneview.ar.session.DepthMode.DISABLED
        }
    }
    
    // Handle tap to place objects
    arSceneView.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            val hitResult = arSceneView.arSceneView.hitTest(event.x, event.y).firstOrNull()
            hitResult?.let { hit ->
                // Place 3D model at hit position
                val modelNode = ModelNode().apply {
                    position = hit.position
                    scale = Vector3(0.5f, 0.5f, 0.5f)
                    loadModelGlbAsync("models/tree.glb") {
                        Log.d(TAG, "Model loaded successfully")
                    }
                }
                arSceneView.arSceneView.scene.addChild(modelNode)
            }
        }
        true
    }
}

override fun onResume() {
    super.onResume()
    arSceneView.onResume()
}

override fun onPause() {
    super.onPause()
    arSceneView.onPause()
}

override fun onDestroy() {
    super.onDestroy()
    arSceneView.onDestroy()
}
```

## ARCore Integration (Alternative)

**Note**: SceneView already includes ARCore support internally. This section is for reference only - no additional ARCore setup is needed when using SceneView.

ARCore is Google's AR platform that SceneView uses under the hood. It's optional but recommended for the best AR experience.

### DeGoogle Note

ARCore requires Google Play Services on most devices. For a fully deGoogled experience:
- SceneView provides fallbacks for devices without ARCore
- Basic AR features work without full ARCore support
- Consider using OpenGLES-based rendering for fully FOSS experience

## Building Native APK Locally

### Clean Build (REQUIRED)

```bash
# Clean caches first (documented commands, no scripts)
rm -rf ~/.gradle/caches
rm -rf app/build app/.gradle

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

### Reproducible Local Builds

To ensure reproducible builds:

1. Always clean caches before building (see above commands)
2. Use consistent Gradle version (specified in gradle wrapper)
3. Document your build environment (OS, SDK versions, etc.)
4. No NDK or .so files required with SceneView!

## Native Assets Placement

### 3D Models for Native AR

Place glTF/GLB models in:

```
app/src/main/assets/models/
├── tree.glb
├── recycle_bin.glb
├── cleanup_bag.glb
├── wildflower.glb
├── water_station.glb
└── birdhouse.glb
```

Update `Config.kt` to reference these paths.

SceneView supports glTF/GLB models natively - no conversion required!

## GPS Integration (Native)

Use Android's LocationManager or FusedLocationProvider for GPS:

```kotlin
// Example location request using LocationManager
val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
locationManager.requestLocationUpdates(
    LocationManager.GPS_PROVIDER,
    10000,  // Update interval ms
    0f,     // Minimum distance meters
    locationCallback
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
adb shell monkey -p org.fediquest.debug -c android.intent.category.LAUNCHER 1

# View logs
adb logcat | grep -E "FediQuest|ARToolKit"
```

### ARCore Compatibility Check

Not all devices support ARCore. Check compatibility:
- https://developers.google.com/ar/devices

For unsupported devices, use ARToolKit (primary option).

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

### ARToolKit Initialization Failed

**Problem**: ARToolKit session won't start

**Solution**:
- Verify `.so` files are correctly placed
- Check camera permissions granted
- Ensure camera hardware is available

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
- ARCore: Proprietary (Google terms apply, optional only)
- FediQuest code: FOSS (license omitted per request)

Always verify license compatibility before distribution.
