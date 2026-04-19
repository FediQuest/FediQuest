# FediQuest Server

Spawn point configuration server for FediQuest AR + GPS prototype.

## Purpose

This directory contains the `server.json` file that provides spawn point data for AR quests with ETag caching support, plus tools for generating quests from OpenStreetMap data.

## Files

- `server.json` - 6 sample spawn entries with ETags for social-ecological quests
- `scripts/overpass_generator.py` - Generate quests from OSM data using Overpass API

## Quick Start

### Generate Quests from OpenStreetMap

```bash
# Install dependencies
pip install requests

# Generate quests for your location (example: New York City)
python scripts/overpass_generator.py --lat 40.7128 --lon -74.0060 --radius 1000 --output server.json

# Generate quests for any location worldwide
python scripts/overpass_generator.py --lat YOUR_LAT --lon YOUR_LON --radius 500
```

### Running a Local Server

For local testing, you can serve this directory with any static file server that supports ETag headers:

#### Python (built-in)

```bash
cd server
python3 -m http.server 8080
```

Note: Python's built-in server may not support ETags properly. For full ETag support, use:

#### Node.js (serve package)

```bash
cd server
npx serve .
```

#### Caddy (recommended for production)

```bash
caddy file-server --root . --listen :8080
```

## ETag Support

The server MUST support ETag headers for efficient caching. The `SpawnFetcher.kt` in the Android app implements:

1. First request: Fetch full `server.json`, store ETag from response header
2. Subsequent requests: Send `If-None-Match: <etag>` header
3. Server returns `304 Not Modified` if data unchanged (no body sent)
4. Server returns `200 OK` with new data if changed

## Sample server.json Structure

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
  ],
  "lastUpdated": "2024-01-15T10:00:00Z",
  "version": "1.0.0"
}
```

See `server.json` for all 6 sample entries covering different quest types.

## Quest Types

The sample spawns cover these social-ecological quest types:

1. **planting** - Tree planting quests (🌱)
2. **recycling** - Recycling station visits (♻️)
3. **cleanup** - Litter cleanup events (🧹)
4. **wildflower** - Wildflower garden planting (🌸)
5. **water** - Water conservation actions (💧)
6. **wildlife** - Wildlife habitat creation (🦋)

## Overpass API Integration

The included `overpass_generator.py` script automatically generates quests from OpenStreetMap data:

### Features

- **Dynamic Quest Generation**: Creates quests based on real-world POIs
- **Multiple Quest Types**: Automatically categorizes by OSM tags
- **Environmental Impact**: Each quest includes impact metrics
- **Companion Hints**: Fun companion character interactions

### Supported OSM Tags

| Quest Type | OSM Tags |
|------------|----------|
| Recycling | `amenity=recycling`, `amenity=waste_basket` |
| Cleanup | `leisure=park`, `landuse=grass` |
| Water | `waterway=*`, `amenity=drinking_water` |
| Planting | `landuse=forest`, `natural=wood` |
| Wildflower | `leisure=garden`, `landuse=allotments` |

### Example Usage

```bash
# Generate quests for downtown San Francisco
python scripts/overpass_generator.py \
  --lat 37.7749 \
  --lon -122.4194 \
  --radius 2000 \
  --output sf_quests.json

# Generate quests with custom radius
python scripts/overpass_generator.py \
  --lat 51.5074 \
  --lon -0.1278 \
  --radius 500 \
  --output london_quests.json
```

## Hosting for Production

For production deployment:

1. Host `server.json` on any static file server (GitHub Pages, Netlify, etc.)
2. Ensure server sends proper ETag headers
3. Update `Config.kt.SERVER_URL` to point to your hosted URL
4. Configure CORS if needed for cross-origin requests

### GitHub Pages Deployment

```bash
# Commit and push server.json to a GitHub repo
# Enable GitHub Pages in repo settings
# URL will be: https://YOUR_USERNAME.github.io/YOUR_REPO/server.json
```

### Netlify Deployment

```bash
# Drag and drop the server folder to Netlify Drop
# Or connect your Git repo for automatic deploys
```

## Updating Spawn Data

To add or modify spawn points:

1. **Manual**: Edit `server.json` with new entries, update ETag values
2. **Automatic**: Run `overpass_generator.py` for your location
3. Update `lastUpdated` timestamp
4. Deploy to your hosting provider

## No Binary Dependencies

This server directory contains only JSON configuration and Python scripts. No binaries, no complex dependencies, no build steps required.

## Advanced Configuration

### Custom Quest Templates

Edit `QUEST_TEMPLATES` in `overpass_generator.py` to customize:

- XP rewards per quest type
- Title and description templates
- Companion hints
- Environmental impact descriptions

### Rate Limiting

The Overpass API has rate limits. For large-scale generation:

```bash
# Use smaller radius values
--radius 500

# Cache results locally
# Don't regenerate unless necessary

# Consider running your own Overpass instance
# https://wiki.openstreetmap.org/wiki/Overpass_API
```
