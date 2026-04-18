# FediQuest Server

Spawn point configuration server for FediQuest AR + GPS prototype.

## Purpose

This directory contains the `server.json` file that provides spawn point data for AR quests with ETag caching support.

## Files

- `server.json` - 6 sample spawn entries with ETags for social-ecological quests

## Running a Local Server

For local testing, you can serve this directory with any static file server that supports ETag headers:

### Python (built-in)

```bash
cd server
python3 -m http.server 8080
```

Note: Python's built-in server may not support ETags properly. For full ETag support, use:

### Node.js (serve package)

```bash
cd server
npx serve .
```

### Caddy (recommended for production)

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

## Hosting for Production

For production deployment:

1. Host `server.json` on any static file server (GitHub Pages, Netlify, etc.)
2. Ensure server sends proper ETag headers
3. Update `Config.kt.SERVER_URL` to point to your hosted URL
4. Configure CORS if needed for cross-origin requests

## Updating Spawn Data

To add or modify spawn points:

1. Edit `server.json` with new entries
2. Update ETag values to force cache invalidation
3. Update `lastUpdated` timestamp
4. Deploy to your hosting provider

## No Binary Dependencies

This server directory contains only JSON configuration. No binaries, no dependencies, no build steps required.
