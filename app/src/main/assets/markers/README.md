# AR Markers Directory

This directory is reserved for AR marker files if needed.

## Current Status

**FediQuest uses SceneView as the primary AR engine**, which does not require physical markers.
SceneView uses feature point detection and plane finding for AR placement.

## When Would Markers Be Used?

Markers would only be needed if:
1. You want to use image-based AR tracking
2. You're implementing QR code-based quest activation
3. You need fiducial markers for specific AR experiences

## Supported Marker Formats

If you need to add markers in the future:
- **ARToolKit**: `.patt` pattern files
- **Vuforia**: Image target databases
- **Custom**: Any image format supported by your AR library

## File Placement

Place any marker files in this directory:
```
app/src/main/assets/markers/
└── (marker files as needed)
```

Update `Config.kt` with the correct marker paths if you add markers.
