# 3D Models Directory

Place your glTF/GLB model files here for FediQuest quests.

## Required Models

| Model File | Quest Type | Description |
|------------|------------|-------------|
| `tree.glb` | Planting | Tree planting quest marker |
| `recycle_bin.glb` | Recycling | Recycling station marker |
| `cleanup_bag.glb` | Cleanup | Litter cleanup zone marker |
| `wildflower.glb` | Wildflower | Wildflower garden marker |
| `water_station.glb` | Water Conservation | Water conservation point marker |
| `birdhouse.glb` | Wildlife Habitat | Wildlife habitat marker |

## Model Requirements

- **Format**: glTF (.gltf) or GLB (.glb)
- **Recommended**: GLB for single-file distribution
- **Scale**: Models should be appropriately scaled for AR (1 unit = 1 meter)
- **Size**: Keep under 5MB per model for optimal performance
- **License**: Ensure models are FOSS-compatible (CC0, CC-BY, etc.)

## Free Model Resources

- [Sketchfab](https://sketchfab.com/) - Filter by Creative Commons licenses
- [Poly Haven](https://polyhaven.com/) - CC0 licensed assets
- [Kenney.nl](https://kenney.nl/) - CC0 game assets
- [Google Poly](https://poly.google.com/) - Check individual licenses

## Model Placement

After downloading or creating your models, place them in this directory:

```
app/src/main/assets/models/
├── tree.glb
├── recycle_bin.glb
├── cleanup_bag.glb
├── wildflower.glb
├── water_station.glb
└── birdhouse.glb
```

The app will automatically load these models when rendering AR spawn points.
