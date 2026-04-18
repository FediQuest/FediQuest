# FediQuest Web Demo

Quick start guide for running the FediQuest WebAR demo locally.

## Quick Serve Instructions

### Option 1: Python (No Dependencies)

```bash
cd web
python3 -m http.server 8080
```

Visit: http://localhost:8080

### Option 2: npm/http-server

```bash
cd web
npm install
npm start
```

Visit: http://localhost:8080

## Demo Features

- **GPS-based AR spawns**: 3D models appear at real-world coordinates
- **AR.js + A-Frame**: Works in browser, no app installation needed
- **OpenStreetMap fallback**: Map view when AR is unavailable
- **ETag caching**: Efficient server.json fetching with cache support
- **Responsive UI**: Works on mobile and desktop
- **Permission handling**: Graceful GPS permission denial fallback

## Testing Without GPS Hardware

### Desktop Testing with GPS Spoofing

**Chrome/Edge:**
1. Open DevTools (F12)
2. Press `Esc` to open Console drawer
3. Click "⋮" → More tools → Sensors
4. Under "Location", select a preset or enter custom coordinates
5. Refresh the page

**Firefox:**
1. Open DevTools (F12)
2. Click "⋮" → More tools → Simulation
3. Set geolocation override
4. Refresh the page

### Test Coordinates (NYC Area)

Use these coordinates to test near the sample spawns in `server/server.json`:

```
Latitude:  40.7128
Longitude: -74.0060
```

## CORS Guidance

When hosting `server.json` separately from the web files:

### Server Headers Required

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, OPTIONS
Access-Control-Allow-Headers: Content-Type, If-None-Match
```

### Python CORS Example

```python
# Simple CORS-enabled server
python3 -m http.server 8080 --bind 0.0.0.0
```

### Node.js Express CORS

```javascript
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Content-Type, If-None-Match');
  next();
});
```

## HTTPS Requirement

GPS permissions require a secure context:

- ✅ `https://yourdomain.com` - Works
- ✅ `http://localhost` - Works (exception for development)
- ❌ `http://yourdomain.com` - GPS will not work

For testing on mobile devices on your local network:

```bash
# Use ngrok for HTTPS tunnel
ngrok http 8080
```

## Browser Compatibility

| Browser | AR Support | GPS Support | Notes |
|---------|-----------|-------------|-------|
| Chrome Android | ✅ | ✅ | Best support |
| Firefox Android | ✅ | ✅ | Good support |
| Safari iOS | ⚠️ | ✅ | AR.js limited on iOS |
| Chrome Desktop | ⚠️ | ⚠️ | GPS spoofing required |
| Firefox Desktop | ⚠️ | ⚠️ | GPS spoofing required |

## Debugging

### Browser Console

Open DevTools console to see logs:

```
[SpawnLoader] Loaded 6 spawns
[GPSHandler] GPS Active (10m accuracy)
```

### Common Issues

**"GPS permission denied"**
- Ensure HTTPS or localhost
- Check browser GPS permissions
- Enable location services on device

**"Models not loading"**
- Check model files exist in `web/models/`
- Verify paths in `server/server.json`
- Check CORS headers if external hosting

**"AR not working"**
- Ensure camera permissions granted
- Check browser compatibility
- Try map fallback mode

## Customization

### Changing Spawn Data

Edit `server/server.json`:
- Update latitude/longitude for your location
- Change model URLs
- Modify metadata (name, description, XP rewards)

### Adding New Models

1. Place `.glb` file in `web/models/`
2. Add entry to `server/server.json`
3. Reference model with relative path: `models/your-model.glb`

### Styling

Modify CSS in `web/index.html`:
- Colors and themes
- UI layout and positioning
- Font sizes and styles

## Performance Tips

- Keep models under 5MB each
- Use Draco compression for glTF models
- Limit texture sizes to 1024x1024
- Reduce spawn count for older devices
- Enable browser caching for production

## Production Deployment

### Static Hosting

Deploy to any static host:

- GitHub Pages
- Netlify
- Vercel
- Cloudflare Pages
- Self-hosted (nginx, Apache)

### Environment Configuration

Update `serverUrl` in `js/spawn-loader.js` for production:

```javascript
this.serverUrl = 'https://your-domain.com/server/server.json';
```

## License

This demo code is part of the FediQuest FOSS project.
