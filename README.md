# FediQuest — Open FOSS AR + GPS Prototype

FediQuest is an open-source augmented reality GPS-based experience platform. This prototype prioritizes a **public browser demo using A-Frame + AR.js (WebAR)** for easy contribution and fast prototyping, while retaining native alternative flows (ARToolKit primary native option, ARCore alternative) and an optional Godot project for cross-platform exports.

## Table of Contents

- [Quick Start](#quick-start)
- [Architecture Overview](#architecture-overview)
- [DeGoogle Checklist](#degoogle-checklist)
- [Running the Web Demo](#running-the-web-demo)
- [Server Configuration](#server-configuration)
- [Native Build (Optional)](#native-build-optional)
- [Godot Project (Optional)](#godot-project-optional)
- [Cache Cleaning & Reproducible Builds](#cache-cleaning--reproducible-builds)
- [File Placement Guide](#file-placement-guide)
- [PR Checklist](#pr-checklist)

## Quick Start

```bash
# Clone and navigate to web directory
cd web

# Option 1: Python HTTP server (no dependencies)
python3 -m http.server 8080

# Option 2: npm dev server
npm install
npm start

# Open browser at: http://localhost:8080
# Enable GPS permissions when prompted
```

## Architecture Overview

```
FediQuest
├── web/                    # Primary: A-Frame + AR.js WebAR demo
│   ├── index.html          # Main demo page
│   ├── js/spawn-loader.js  # GPS & spawn data handling
│   └── models/             # glTF model placeholders
├── app/                    # Secondary: Native Android skeleton
│   ├── README_NATIVE.md    # ARToolKit/ARCore integration guide
│   └── src/                # Kotlin stubs
├── godot/                  # Optional: Godot cross-platform export
│   ├── project.godot       # Godot project config
│   └── scripts/            # GDScript spawn loaders
├── server/                 # Spawn configuration
│   └── server.json         # Sample spawn entries
└── scripts/                # Build & maintenance utilities
    └── clean_caches.sh     # Cache cleaning script
```

### Primary vs Secondary Flows

| Flow | Technology | Priority | Install Required |
|------|------------|----------|------------------|
| **Web Demo** | A-Frame + AR.js | Primary | None (browser only) |
| Native AR | ARToolKit | Secondary | Android build |
| Native AR | ARCore (optional) | Alternative | Android build + Google deps |
| Cross-platform | Godot | Optional | Godot Engine |

## DeGoogle Checklist

FediQuest is committed to avoiding proprietary Google services for core features:

- ✅ **Maps**: Uses OpenStreetMap + Leaflet (not Google Maps)
- ✅ **GPS**: Uses browser/device native GPS APIs (no Google Location Services required)
- ✅ **Authentication**: No Google Sign-In required; optional OpenID/OAuth with self-hosted providers
- ✅ **Analytics**: No analytics by default; opt-in privacy-respecting solutions only (e.g., Plausible, Matomo)
- ✅ **Hosting**: Works on any static host (GitHub Pages, Netlify, self-hosted); no Firebase required
- ✅ **ARCore**: Optional alternative only; not required for web demo or core functionality
- ✅ **Play Services**: Not required for core app flows

### What We Avoid

- ❌ Google Play Services (core flows)
- ❌ Google Maps API
- ❌ Google Sign-In / OAuth
- ❌ Firebase (unless self-hosted alternative)
- ❌ Google Analytics
- ❌ Proprietary tracking SDKs

## Running the Web Demo

### Prerequisites

- Modern browser with WebXR/WebGL support (Firefox, Chrome, Edge)
- GPS-enabled device (phone/tablet recommended for AR experience)
- HTTPS context required for GPS permissions (use localhost for development)

### Method 1: Python HTTP Server (Recommended for Testing)

```bash
cd web
python3 -m http.server 8080
# Visit: http://localhost:8080
```

### Method 2: npm Dev Server

```bash
cd web
npm install
npm start
# Visit: http://localhost:8080
```

### Method 3: Direct File Opening (Limited Functionality)

Some browsers allow opening `index.html` directly, but GPS permissions may be restricted without HTTPS.

### Testing GPS Without Mobile Device

For desktop testing, most browsers allow GPS spoofing in developer tools:

1. Open DevTools (F12)
2. Go to "Sensors" tab (Chrome: More tools → Sensors)
3. Set custom location coordinates
4. Refresh page

### CORS Guidance

When hosting `server.json` separately from the web demo, ensure CORS headers are set:

```
Access-Control-Allow-Origin: *
```

Or host both on the same origin.

## Server Configuration

The `server/server.json` file contains spawn point definitions:

```json
{
  "spawns": [
    {
      "id": "spawn_001",
      "latitude": 40.7128,
      "longitude": -74.0060,
      "modelUrl": "models/example.glb",
      "metadata": {
        "name": "Example Spawn",
        "description": "A sample AR object"
      }
    }
  ]
}
```

See `server/server.json` for 6 sample entries.

### Updating Spawn Points

1. Edit `server/server.json`
2. Update `modelUrl` paths to point to your hosted glTF models
3. Adjust latitude/longitude for your test location
4. The web demo fetches this automatically with ETag caching

## Native Build (Optional)

See `app/README_NATIVE.md` for detailed instructions.

### Quick Native Build Steps

```bash
# Ensure Android SDK/NDK installed
export ANDROID_HOME=/path/to/android/sdk
export ANDROID_NDK_HOME=/path/to/android/ndk

# Place ARToolKit .so files in app/src/main/jniLibs/
# See app/README_NATIVE.md for download links

# Clean caches first
./scripts/clean_caches.sh

# Build APK
cd app
./gradlew assembleDebug

# APK output: app/build/outputs/apk/debug/app-debug.apk
```

**Note**: Native builds produce APKs locally only. Do not commit binary artifacts (>500MB repo limit).

## Godot Project (Optional)

See `godot/README_GODOT.md` for setup instructions.

### Quick Godot Setup

1. Install Godot 4.x from https://godotengine.org
2. Open `godot/project.godot`
3. Place your models in `godot/assets/models/`
4. Run project or export to target platform

## Cache Cleaning & Reproducible Builds

To ensure reproducible builds and clean state for CI/reviewers:

### Automated Cache Cleaning

```bash
# Run the cache cleaning script
./scripts/clean_caches.sh
```

This removes:
- npm cache and node_modules
- Gradle caches and build directories
- Local dev server caches
- Browser cache hints

### Manual Cache Cleaning

```bash
# npm cache
npm cache clean --force
rm -rf web/node_modules

# Gradle cache
rm -rf ~/.gradle/caches
rm -rf app/build
rm -rf app/.gradle

# Python cache
find . -type d -name "__pycache__" -exec rm -rf {} +
find . -type f -name "*.pyc" -delete
```

### Reproducible Build Checklist

- [ ] Run `./scripts/clean_caches.sh` before building
- [ ] Verify `server/server.json` is accessible
- [ ] Confirm GPS permissions enabled in browser/device
- [ ] Use same Node.js version (check `web/package.json` engines)
- [ ] Use same Gradle version (check `app/build.gradle`)

## File Placement Guide

### Models (Not Included in Repo)

```
web/models/          # Place .glb/.gltf files here
app/src/main/assets/ # Place native assets here
godot/assets/models/ # Place Godot models here
```

See `web/models/README.md` for model requirements.

### Native Libraries (Not Included in Repo)

```
app/src/main/jniLibs/arm64-v8a/  # ARToolKit .so files
app/src/main/jniLibs/armeabi-v7a/
```

See `app/README_NATIVE.md` for ARToolKit download links.

### Godot Export Templates (Not Included)

Download from Godot Engine or configure in export settings.

## PR Checklist

Before submitting or reviewing this PR:

- [ ] Web demo runs locally (`python3 -m http.server 8080`)
- [ ] GPS permissions handled gracefully (fallback shown on denial)
- [ ] `server/server.json` loads with ETag caching
- [ ] OpenStreetMap fallback works when AR unavailable
- [ ] Cache cleaning script runs successfully
- [ ] No binary blobs committed (models, .so files, APKs)
- [ ] README documentation complete
- [ ] DeGoogle checklist verified (no Google dependencies for core)
- [ ] All placeholder files have clear placement instructions

## Contributing

This is a FOSS project. Contributions welcome!

- Web AR improvements (A-Frame/AR.js)
- Native Android enhancements (ARToolKit)
- Godot export configurations
- Privacy-focused feature additions
- Documentation improvements

## Acknowledgments

- [A-Frame](https://aframe.io/) - Web framework for VR/AR
- [AR.js](https://ar-js-org.github.io/AR.js-Docs/) - WebAR library
- [ARToolKit](https://artoolkit.org/) - Native AR library
- [Godot Engine](https://godotengine.org/) - Cross-platform game engine
- [OpenStreetMap](https://www.openstreetmap.org/) - Open map data
