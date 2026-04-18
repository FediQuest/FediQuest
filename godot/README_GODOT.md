# FediQuest Godot Project

This directory contains the optional Godot Engine project for FediQuest, enabling cross-platform exports (Android, iOS, Desktop, Web).

## Godot Version

- **Recommended**: Godot 4.2 or newer
- **Minimum**: Godot 4.0
- Download from: https://godotengine.org/download

## Project Structure

```
godot/
├── project.godot          # Godot project configuration
├── README_GODOT.md        # This file
├── scripts/
│   ├── spawn_loader.gd    # Fetch and place GPS spawns
│   └── geo_utils.gd       # GPS coordinate utilities
├── scenes/                # Scene files (to be created)
│   └── main.tscn          # Main scene
└── assets/
    └── models/            # glTF/GLB 3D models (not included)
```

## Quick Start

### 1. Open Project

```bash
# Launch Godot and open project.godot
godot godot/project.godot
```

Or import via Godot's project manager.

### 2. Add Models

Place your `.glb` or `.gltf` models in:

```
godot/assets/models/
├── tree.glb
├── recycle_bin.glb
└── ...
```

See `web/models/README.md` for model requirements.

### 3. Run Project

Press F5 in Godot editor to run. The project will:
- Load spawn data from `server/server.json`
- Place 3D models at GPS coordinates
- Show mock location for desktop testing

## Scripts Overview

### SpawnLoader (`scripts/spawn_loader.gd`)

Autoload singleton that handles:
- Fetching `server.json` with ETag caching
- Parsing spawn data
- Placing 3D models in scene
- Cache management

**Usage:**
```gdscript
# Connect to signals
SpawnLoader.spawns_loaded.connect(_on_spawns_loaded)
SpawnLoader.spawns_failed.connect(_on_spawns_failed)

# Fetch spawns
SpawnLoader.fetch_spawns("res://../server/server.json")

# Place spawns in scene
func _on_spawns_loaded(spawns: Array) -> void:
    SpawnLoader.place_spawns_in_scene($World/SpawnsContainer)
```

### GeoUtils (`scripts/geo_utils.gd`)

Autoload singleton providing:
- GPS distance calculations (Haversine formula)
- GPS to local coordinate conversion
- Bearing calculations
- Location utilities

**Usage:**
```gdscript
# Calculate distance between two points
var distance := GeoUtils.calculate_distance(lat1, lon1, lat2, lon2)

# Convert GPS to scene position
var local_pos := GeoUtils.gps_to_local(latitude, longitude)

# Check if within radius
if GeoUtils.is_within_radius(lat1, lon1, lat2, lon2, 100.0):
    print("Within 100 meters!")
```

## GPS/Location Handling

### Desktop Testing

GPS is not available on most desktops. The project uses mock location by default:

```gdscript
# Set mock location for testing
GeoUtils.set_mock_location(40.7128, -74.0060)  # NYC coordinates
```

### Android Export

For real GPS on Android:
1. Enable location permissions in export preset
2. Use Godot's experimental Geolocation API or a plugin
3. See [Godot Android docs](https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_android.html)

### iOS Export

iOS requires additional provisioning and entitlements for location services.

## Exporting

### Android APK

1. Install Android SDK/NDK
2. Configure export preset in Godot
3. Set required permissions:
   - `ACCESS_FINE_LOCATION`
   - `CAMERA` (for AR)
   - `INTERNET`
4. Export → Build APK

### Web Export (HTML5)

1. Export as HTML5
2. Host on HTTPS server (required for some features)
3. Note: WebXR support varies by browser

### Desktop Builds

Export for Windows, macOS, or Linux:
- No GPS on desktop (use mock location)
- Good for development/testing

## AR Support

### Current Status

This Godot project is a **skeleton** showing how to integrate GPS-based spawns. Full AR integration requires:

1. **OpenXR** (for VR/AR headsets)
2. **ARCore/ARKit plugins** (for mobile AR)
3. **Custom shaders** for camera passthrough

### Recommended AR Plugins

- **Godot XR Tools**: https://github.com/GodotVR/godot_xr_tools
- **ARCore Plugin**: https://github.com/godotengine/godot-android-plugin-arcore
- **OpenXR**: Built into Godot 4.x

## Caching

Spawn data is cached in `user://` directory:
- `spawn_cache.json` - Cached spawn data
- `spawn_etag.txt` - ETag for conditional requests

Clear cache programmatically:
```gdscript
SpawnLoader.clear_cache()
```

Or manually delete files from user data directory.

## Troubleshooting

### Models Not Showing

- Verify models are in `assets/models/`
- Check console for load errors
- Ensure model paths match `server.json`

### GPS Not Working

- Desktop has no GPS (use mock location)
- Check platform permissions
- Test on physical device

### Export Failures

- Verify Godot version matches project format
- Check export templates are installed
- Review platform-specific requirements

## Performance Tips

- Use LOD (Level of Detail) for distant spawns
- Limit active spawns based on distance
- Compress textures for mobile
- Use occlusion culling for complex scenes

## Extending the Project

### Adding New Features

1. Create new scenes in `scenes/`
2. Add scripts to `scripts/`
3. Update `project.godot` autoloads if needed

### Custom Spawn Types

Extend spawn metadata in `server.json`:
```json
{
  "metadata": {
    "type": "planting",
    "xpReward": 100,
    "customField": "value"
  }
}
```

Access in GDScript:
```gdscript
var spawn_type = spawn_data.metadata.type
var xp = spawn_data.metadata.xpReward
```

## Resources

- [Godot Documentation](https://docs.godotengine.org/)
- [GDScript Reference](https://docs.godotengine.org/en/stable/tutorials/scripting/gdscript/gdscript_reference.html)
- [Godot Asset Library](https://godotengine.org/asset-library/asset)
- [OpenXR with Godot](https://godotvr.github.io/)

## License Notes

This Godot project is part of FediQuest FOSS. Models and assets must have compatible licenses.
