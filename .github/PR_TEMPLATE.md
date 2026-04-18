# FediQuest AR GPS Prototype - PR Template

## Pull Request: feature/ar-gps-prototype

This PR implements the FediQuest AR + GPS prototype with A-Frame/AR.js WebAR as the primary demo, plus native alternative flows (ARToolKit, ARCore) and optional Godot project.

---

## PR Checklist

### Core Requirements

- [ ] **Web Demo Functional**: A-Frame + AR.js demo runs locally
- [ ] **GPS Handling**: Permissions handled gracefully with fallback
- [ ] **Server Config**: `server/server.json` loads with ETag caching
- [ ] **Map Fallback**: OpenStreetMap + Leaflet works when AR unavailable
- [ ] **Cache Script**: `scripts/clean_caches.sh` runs successfully
- [ ] **No Binary Blobs**: No models, .so files, or APKs committed
- [ ] **Documentation**: All README files complete and accurate
- [ ] **DeGoogle**: No Google dependencies for core functionality

### File Verification

- [ ] `README.md` - Updated with new architecture
- [ ] `server/server.json` - 6 sample spawn entries
- [ ] `web/index.html` - Complete A-Frame + AR.js demo
- [ ] `web/js/spawn-loader.js` - ETag caching, GPS handling
- [ ] `web/models/README.md` - Model placement instructions
- [ ] `web/package.json` - npm scripts for dev server
- [ ] `web/README_WEB.md` - Web demo documentation
- [ ] `app/README_NATIVE.md` - Native integration guide
- [ ] `app/CMakeLists.txt` - Native build snippet
- [ ] `app/src/main/java/.../MainActivity.kt` - Mode toggle stubs
- [ ] `godot/project.godot` - Godot project config
- [ ] `godot/scripts/spawn_loader.gd` - GDScript spawn loader
- [ ] `godot/scripts/geo_utils.gd` - GDScript GPS utilities
- [ ] `godot/README_GODOT.md` - Godot documentation
- [ ] `scripts/clean_caches.sh` - Cache cleaning script
- [ ] `.github/PR_TEMPLATE.md` - This file

---

## How to Review This PR

### 1. Run Web Demo Locally (Required)

```bash
# Navigate to web directory
cd web

# Option A: Python (no dependencies)
python3 -m http.server 8080

# Option B: npm
npm install
npm start

# Open browser: http://localhost:8080
```

**Expected behavior:**
- Page loads with AR camera view
- GPS permission prompt appears
- On allow: Shows nearby spawns from `server/server.json`
- On deny: Shows map fallback option
- Spawn list displays at bottom with distances
- Clicking spawns shows metadata panel

### 2. Test GPS Spoofing (Desktop Testing)

```
Chrome DevTools → Sensors → Set location to:
Latitude:  40.7128
Longitude: -74.0060
Refresh page
```

**Expected:** Spawns appear in list with calculated distances.

### 3. Verify Cache Cleaning

```bash
# From project root
./scripts/clean_caches.sh

# Verify directories removed:
ls -la web/node_modules      # Should not exist
ls -la app/build             # Should not exist
```

### 4. Check DeGoogle Compliance

Review that NO Google services are required for core features:

| Feature | Implementation | Google-Free? |
|---------|---------------|--------------|
| Maps | OpenStreetMap + Leaflet | ✅ |
| GPS | Browser/Device native API | ✅ |
| Auth | None required | ✅ |
| Analytics | None included | ✅ |
| Hosting | Any static host | ✅ |
| AR (Web) | AR.js (FOSS) | ✅ |
| AR (Native Alt) | ARToolKit (FOSS) | ✅ |
| AR (Native Opt) | ARCore (optional) | ⚠️ Optional only |

### 5. Verify No Binary Blobs

```bash
# Check for large files (>1MB)
find . -type f -size +1M \
  ! -path "./node_modules/*" \
  ! -path "./.git/*" \
  ! -path "./gradle/*"

# Should NOT find:
# - .glb/.gltf model files
# - .so native libraries
# - .apk Android packages
# - .aab Android bundles
```

---

## Cache Clean Steps for Reviewers

Before building/testing, clean all caches:

```bash
# Automated (recommended)
./scripts/clean_caches.sh

# Manual alternative
npm cache clean --force
rm -rf web/node_modules
rm -rf app/build app/.gradle .gradle
find . -name "__pycache__" -exec rm -rf {} +
```

Then rebuild:

```bash
# Web demo
cd web
npm install  # if using npm server
# OR just use python3 -m http.server 8080

# Native APK (optional)
cd app
./gradlew clean assembleDebug
```

---

## Architecture Summary

```
┌─────────────────────────────────────────────────────┐
│                  FediQuest                          │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  PRIMARY: Web Demo (A-Frame + AR.js)        │   │
│  │  - Zero install, browser-based              │   │
│  │  - GPS via browser API                      │   │
│  │  - OpenStreetMap fallback                   │   │
│  │  - Files: web/                              │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  SECONDARY: Native Android                  │   │
│  │  - ARToolKit (primary native)               │   │
│  │  - ARCore (optional alternative)            │   │
│  │  - Requires .so files (not included)        │   │
│  │  - Files: app/                              │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  OPTIONAL: Godot Project                    │   │
│  │  - Cross-platform exports                   │   │
│  │  - GDScript implementation                  │   │
│  │  - Files: godot/                            │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  Server Config                              │   │
│  │  - server/server.json                       │   │
│  │  - 6 sample spawns (NYC area)               │   │
│  │  - ETag caching support                     │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## Known Limitations

1. **Models Not Included**: glTF/GLB files must be added separately (see `web/models/README.md`)
2. **Native .so Files**: ARToolKit libraries must be built/downloaded (see `app/README_NATIVE.md`)
3. **iOS Support**: Limited; WebAR works but native iOS requires additional work
4. **AR on Desktop**: WebAR requires webcam; GPS spoofing needed for testing
5. **Godot Scenes**: Main scene (`main.tscn`) is a placeholder; requires creation

---

## Testing Scenarios

### Scenario 1: First-Time User (Mobile)

1. Open `http://your-host.com/web/` on mobile
2. Grant GPS permission
3. Grant camera permission (for AR)
4. See nearby spawns in list
5. Walk toward spawn location
6. View AR model through camera

### Scenario 2: Desktop Testing

1. Run `python3 -m http.server 8080` in `web/`
2. Open `http://localhost:8080`
3. Spoof GPS in DevTools
4. View map fallback mode
5. Click spawns to see metadata

### Scenario 3: GPS Denied

1. Open web demo
2. Deny GPS permission
3. See permission prompt
4. Click "Use Map Only"
5. View OpenStreetMap with spawn markers

### Scenario 4: Offline/Cache

1. Load web demo once (caches data)
2. Go offline
3. Reload page
4. Cached spawns should still display

---

## Post-Merge Tasks

After this PR is merged:

- [ ] Add actual 3D models to `web/models/`
- [ ] Build/download ARToolKit for native flow
- [ ] Create Godot main scene
- [ ] Set up CI/CD for web deployment
- [ ] Configure production server.json hosting
- [ ] Test on multiple devices/browsers

---

## References

- [A-Frame Documentation](https://aframe.io/docs/)
- [AR.js Documentation](https://ar-js-org.github.io/AR.js-Docs/)
- [Leaflet Documentation](https://leafletjs.com/reference.html)
- [OpenStreetMap](https://www.openstreetmap.org/)
- [ARToolKit](https://artoolkit.org/)
- [Godot Engine](https://godotengine.org/)

---

## Contact

For questions about this PR, contact the FediQuest maintainers or open an issue.
