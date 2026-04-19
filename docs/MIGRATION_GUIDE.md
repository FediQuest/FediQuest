# FediQuest Migration & Integration Guide

This document outlines how to integrate features and migrate from related AR/GPS projects.

## Reference Projects

### 1. [8th Wall](https://github.com/8thwall) - WebAR Platform

**Key Features to Adopt:**
- SLAM-based tracking without markers
- Multi-target recognition
- Cloud recognition service

**Integration Strategy:**
```javascript
// Future: Add 8th Wall support as premium feature
if (config.use8thWall) {
    import('../lib/8thwall/xr.js');
    XR8.run({ canvas: document.getElementById('ar-camera') });
}
```

**Migration Steps:**
1. Evaluate 8th Wall licensing for commercial use
2. Implement as optional premium AR engine
3. Keep current model-viewer as free tier

---

### 2. [MapStory Engine](https://github.com/graeburn/mapstoryengine) - Temporal GIS

**Key Features to Adopt:**
- Time-based story layers
- Geotemporal data visualization
- Community-driven map annotations

**Integration Strategy:**
```kotlin
// Add temporal quest system
data class TemporalQuest(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val recurrence: RecurrencePattern?,
    val storyLayers: List<StoryLayer>
)
```

**Migration Steps:**
1. Import MapStory data format parser
2. Add time-based quest activation
3. Implement recurrence patterns (daily, weekly, seasonal)

---

### 3. [TerranQuest](https://source.netsyms.com/TerranQuest) - Open Source AR Game

**Key Features to Adopt:**
- Server architecture reference
- Quest generation algorithms
- Multiplayer synchronization

**Integration Strategy:**
```kotlin
// Adapt TerranQuest server protocol
class TerranQuestAdapter {
    suspend fun importQuests(serverUrl: String): List<Quest> {
        // Parse TerranQuest API response
        // Convert to FediQuest Quest format
    }
}
```

**Migration Steps:**
1. Study TerranQuest server implementation
2. Implement compatibility layer
3. Allow importing existing TerranQuest servers

---

### 4. [Overpass API](https://github.com/drolbr/Overpass-API) - OpenStreetMap Query Engine

**Key Features to Adopt:**
- Dynamic POI-based quest generation
- Real-time OSM data queries
- Geographic feature detection

**Integration Strategy:**
```kotlin
// Generate quests from OSM data
class OSMQuestGenerator {
    suspend fun generateQuests(lat: Double, lng: Double, radius: Int): List<Quest> {
        val overpassQuery = """
            [out:json];
            (
              node["amenity"="recycling"](around:$radius,$lat,$lng);
              node["leisure"="park"](around:$radius,$lat,$lng);
              way["landuse"="forest"](around:$radius,$lat,$lng);
            );
            out center;
        """.trimIndent()
        
        // Parse Overpass response
        // Generate quests based on OSM features
    }
}
```

**Migration Steps:**
1. Implement Overpass API client
2. Create quest templates for OSM tags
3. Add automatic quest generation based on location

**Example Overpass Queries:**
```overpass
# Find recycling centers
node["amenity"="recycling"](around:1000,{{lat}},{{lon}});

# Find parks and green spaces
way["leisure"="park"](around:1000,{{lat}},{{lon}});
way["landuse"="grass"](around:1000,{{lat}},{{lon}});

# Find water features
way["waterway"](around:1000,{{lat}},{{lon}});
node["amenity"="drinking_water"](around:1000,{{lat}},{{lon}});
```

---

### 5. [Ingress Dual Map](https://github.com/Terrance/IngressDualMap) - Dual View Navigation

**Key Features to Adopt:**
- Split-screen map view
- Portal/waypoint visualization
- Distance-based interactions

**Integration Strategy:**
```kotlin
// Implement dual-map view for quests
class DualMapView {
    fun render(questLocation: Location, playerLocation: Location) {
        // Show overview map (large scale)
        // Show detail map (street level)
        // Draw path between player and quest
    }
}
```

**Migration Steps:**
1. Implement OSMDroid dual-view layout
2. Add pathfinding visualization
3. Create waypoint navigation UI

---

## Implementation Priority

### Phase 1: Core Integration (Weeks 1-2)
- [x] Basic project structure
- [x] Shared models module
- [x] WebAR foundation
- [ ] Overpass API client
- [ ] OSM-based quest generation

### Phase 2: Enhanced Features (Weeks 3-4)
- [ ] Temporal quest system (MapStory-inspired)
- [ ] Dual-map navigation
- [ ] Advanced ETag caching
- [ ] Offline-first sync

### Phase 3: Advanced Integration (Weeks 5-6)
- [ ] TerranQuest compatibility layer
- [ ] Multiplayer synchronization
- [ ] Community quest creation tools
- [ ] Analytics dashboard

### Phase 4: Premium Features (Weeks 7-8)
- [ ] 8th Wall integration (optional)
- [ ] Advanced SLAM tracking
- [ ] Cloud recognition service
- [ ] Enterprise deployment options

---

## Data Migration Scripts

### Import from TerranQuest

```bash
#!/bin/bash
# migrate_terranquest.sh

TQ_SERVER_URL="$1"
FQ_OUTPUT="migrated_quests.json"

curl "$TQ_SERVER_URL/api/quests" | jq '
  .quests | map({
    id: .uuid,
    title: .name,
    description: .description,
    type: (.type | ascii_downcase),
    latitude: .latitude,
    longitude: .longitude,
    xpReward: (.difficulty * 50)
  })
' > "$FQ_OUTPUT"

echo "Migrated quests saved to $FQ_OUTPUT"
```

### Import from Ingress Intel Map

```python
# import_ingress.py
import json
import requests

def parse_iitc_data(iitc_file):
    with open(iitc_file) as f:
        data = json.load(f)
    
    quests = []
    for portal in data['ents']:
        quests.append({
            'id': portal[0],
            'title': portal[8]['title'],
            'latitude': portal[2][0],
            'longitude': portal[2][1],
            'type': 'social',
            'xpReward': 100
        })
    
    return quests
```

---

## API Compatibility Matrix

| Feature | 8th Wall | MapStory | TerranQuest | Overpass | FediQuest |
|---------|----------|----------|-------------|----------|-----------|
| WebXR | ✅ | ❌ | ❌ | ❌ | ✅ |
| Native AR | ✅ | ❌ | ✅ | ❌ | ✅ |
| Temporal Data | ⚠️ | ✅ | ❌ | ❌ | 🔄 |
| OSM Integration | ❌ | ✅ | ⚠️ | ✅ | ✅ |
| Fediverse | ❌ | ❌ | ❌ | ❌ | ✅ |
| Offline-First | ⚠️ | ❌ | ⚠️ | ❌ | ✅ |

Legend: ✅ Native | ⚠️ Partial | ❌ No | 🔄 In Progress

---

## Contributing

When migrating features from other projects:

1. **License Check**: Verify compatibility with FediQuest license
2. **Attribution**: Credit original authors
3. **Documentation**: Update this guide
4. **Testing**: Ensure cross-platform compatibility
5. **Performance**: Benchmark against existing implementations

---

## Resources

- [OpenStreetMap Wiki](https://wiki.openstreetmap.org/)
- [Overpass API Language Guide](https://wiki.openstreetmap.org/wiki/Overpass_API/Language_Guide)
- [WebXR Specification](https://www.w3.org/TR/webxr/)
- [glTF Specification](https://www.khronos.org/gltf/)
- [ActivityPub Protocol](https://www.w3.org/TR/activitypub/)
