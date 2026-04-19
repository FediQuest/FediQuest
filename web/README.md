# FediQuest WebAR Module

Web-based Augmented Reality module for cross-platform AR experiences using WebXR and model-viewer.

## Features

- **WebXR Support**: Native AR on Android (via Chrome) and iOS (via Safari Scene Viewer)
- **ETag Caching**: Bandwidth-optimized spawn data fetching with If-None-Match support
- **3D Model Viewer**: Fallback 3D viewing for devices without AR capabilities
- **Progressive Enhancement**: Works on all modern browsers, enhances to AR when available

## Quick Start

### Development Server

```bash
# Using Python
cd web/src/main
python -m http.server 8080

# Or using Node.js
npx serve .
```

Then open: `http://localhost:8080`

### Build with Gradle

```bash
# Build web module
./gradlew :web:jsBrowserProductionWebpack

# Output in: web/build/distributions/
```

## Architecture

```
web/src/main/
├── index.html          # Main HTML entry point
├── js/
│   ├── quest-fetcher.js    # ETag-based data fetching
│   └── ar-manager.js       # WebXR AR management
└── models/             # 3D glTF/GLB models
```

## Browser Support

| Browser | AR Mode | Notes |
|---------|---------|-------|
| Chrome (Android) | ✅ WebXR | Full AR support |
| Safari (iOS) | ✅ Scene Viewer | Quick Look fallback |
| Firefox Reality | ✅ WebXR | VR/AR headsets |
| Desktop Chrome | ⚠️ 3D Only | No AR, 3D viewer only |
| Desktop Safari | ⚠️ 3D Only | No AR, 3D viewer only |

## API Reference

### QuestFetcher

```javascript
const fetcher = new QuestFetcher();

// Fetch spawns with automatic ETag caching
const spawns = await fetcher.fetchSpawns('/server/server.json');

// Clear cache
fetcher.clearCache('/server/server.json');
fetcher.clearAllCaches();
```

### ARManager

```javascript
const arManager = new ARManager();

// Initialize AR
await arManager.initialize();

// Load a model
arManager.loadModel('models/tree.glb');

// Place at GPS location
arManager.placeAtLocation(40.7128, -74.0060, 'models/tree.glb');

// Start AR session
await arManager.startAR();

// Take screenshot
const screenshot = await arManager.takeScreenshot();

// Check status
const status = arManager.getStatus();
```

## Integration with Android App

The WebAR module can be embedded in the Android app using WebView:

```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    allowFileAccess = true
    allowContentAccess = true
}
webView.loadUrl("file:///android_asset/web/index.html")
```

## Deployment

### Static Hosting

Deploy to any static host:

```bash
# Build production bundle
./gradlew :web:jsBrowserProductionWebpack

# Deploy web/build/distributions/ to:
# - GitHub Pages
# - Netlify
# - Vercel
# - Cloudflare Pages
# - Any S3-compatible storage
```

### PWA Support (Future)

Add service worker for offline support:

```javascript
// TODO: Implement service worker
// - Cache 3D models
// - Offline quest data
// - Background sync
```

## Security Considerations

- **HTTPS Required**: WebXR requires secure context (HTTPS or localhost)
- **CORS**: Configure CORS headers for cross-origin model loading
- **Content Security Policy**: Recommended CSP header:
  ```
  default-src 'self';
  script-src 'self' 'unsafe-inline' https://ajax.googleapis.com;
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: blob:;
  model-src 'self' blob:;
  ```

## Performance Optimization

1. **Model Compression**: Use Draco compression for glTF models
2. **Texture Optimization**: Use KTX2/BasisU textures
3. **Lazy Loading**: Load models on-demand
4. **ETag Caching**: Reduce bandwidth with conditional requests

## Testing

```bash
# Run tests (future)
./gradlew :web:jsTest

# Lighthouse audit for performance
# Open Chrome DevTools > Lighthouse > Run audit
```

## License

Same as main FediQuest project (FOSS)
