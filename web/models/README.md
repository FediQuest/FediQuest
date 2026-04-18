# FediQuest Web Models

This directory contains 3D models for AR spawns in FediQuest.

## Model Requirements

- **Format**: glTF 2.0 (.glb preferred, .gltf + .bin + textures also supported)
- **Scale**: Models should be sized appropriately for real-world display (1 unit = 1 meter)
- **Optimization**: Keep polygon count reasonable for mobile devices (<50k triangles recommended)
- **Textures**: Use compressed textures (KTX2/Basis) when possible for faster loading
- **License**: Ensure all models are FOSS-compatible (CC0, CC-BY, etc.)

## Placeholder Models

The following model files are referenced in `server/server.json` but NOT included in this repository:

| Filename | Description | Suggested Source |
|----------|-------------|------------------|
| `tree.glb` | Tree/plant model for planting quests | Create in Blender or download from Poly Pizza |
| `recycle_bin.glb` | Recycling bin for recycling quests | Create in Blender or download from Sketchfab (CC) |
| `cleanup_bag.glb` | Trash bag for cleanup quests | Create in Blender |
| `wildflower.glb` | Wildflower cluster for planting quests | Create in Blender |
| `water_station.glb` | Water quality station for water quests | Create in Blender |
| `birdhouse.glb` | Bird house for wildlife quests | Create in Blender |

## Where to Get Models

### Free Model Sources (FOSS-compatible)

1. **Poly Pizza** - https://poly.pizza/ (CC0 low-poly models)
2. **Sketchfab** - https://sketchfab.com/ (filter by CC licenses)
3. **Kenney.nl** - https://kenney.nl/assets (CC0 game assets)
4. **OpenGameArt** - https://opengameart.org/ (various licenses)
5. **BlenderKit** - https://www.blenderkit.com/ (filter by CC0)

### Creating Your Own

Use Blender (https://blender.org/) to create custom models:

1. Model your object at real-world scale
2. Apply transforms (Ctrl+A → All Transforms)
3. Export as glTF 2.0 (.glb)
4. Test in the web demo

## Placing Models

Once you have your `.glb` files:

1. Place them in this `web/models/` directory
2. Update `server/server.json` with correct filenames
3. The web demo will automatically load them at GPS coordinates

## Model Optimization Tips

- Use Draco compression for smaller file sizes
- Limit texture resolution to 1024x1024 or lower for mobile
- Remove unnecessary materials and vertices
- Test loading times on mobile devices
- Consider using LOD (Level of Detail) for complex models

## Testing Models Locally

```bash
cd web
python3 -m http.server 8080
# Visit: http://localhost:8080
# Use browser dev tools to spoof GPS location near spawn coordinates
```

## Troubleshooting

**Model not showing:**
- Check browser console for loading errors
- Verify CORS headers if hosting externally
- Ensure model path in server.json is correct
- Check that model uses supported glTF features

**Model too large/small:**
- Adjust scale in Blender before export
- Or modify the `scale` attribute in index.html's `placeSpawnsInAR()` function

**Model facing wrong direction:**
- Rotate model in Blender (typically should face +Z)
- Or adjust rotation in `placeSpawnsInAR()` function
