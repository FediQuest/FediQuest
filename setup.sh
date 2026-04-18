#!/bin/bash
# FediQuest Setup and Build Script
# This script sets up the development environment and builds the FediQuest Android app
# with all necessary dependencies using SceneView as the primary AR engine.

set -e  # Exit on error

echo "=============================================="
echo "  FediQuest - Open FOSS AR + GPS Prototype"
echo "  Setup and Build Script"
echo "=============================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if ANDROID_HOME is set
if [ -z "$ANDROID_HOME" ]; then
    print_warn "ANDROID_HOME environment variable is not set."
    print_info "Attempting to detect Android SDK location..."
    
    # Common locations
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
        print_info "Found Android SDK at: $ANDROID_HOME"
    elif [ -d "/usr/lib/android-sdk" ]; then
        export ANDROID_HOME="/usr/lib/android-sdk"
        print_info "Found Android SDK at: $ANDROID_HOME"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
        print_info "Found Android SDK at: $ANDROID_HOME"
    else
        print_error "Could not find Android SDK automatically."
        print_info "Please set ANDROID_HOME manually:"
        echo "  export ANDROID_HOME=/path/to/android/sdk"
        exit 1
    fi
fi

print_info "Using Android SDK at: $ANDROID_HOME"

# Check if ANDROID_NDK_HOME is set (optional for SceneView)
if [ -z "$ANDROID_NDK_HOME" ]; then
    print_warn "ANDROID_NDK_HOME is not set. SceneView does not require NDK."
    print_info "If you want to add custom native code, set ANDROID_NDK_HOME manually."
else
    print_info "Using Android NDK at: $ANDROID_NDK_HOME"
fi

# Verify Java installation
print_info "Checking Java installation..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    print_info "Java found: $JAVA_VERSION"
else
    print_error "Java is not installed. Please install JDK 17 or higher."
    exit 1
fi

# Clean build caches
print_info "Cleaning build caches for reproducible build..."
rm -rf ~/.gradle/caches 2>/dev/null || true
rm -rf app/build 2>/dev/null || true
rm -rf app/.gradle 2>/dev/null || true
rm -rf app/.externalNativeBuild 2>/dev/null || true
rm -rf app/.cxx 2>/dev/null || true
rm -rf .gradle 2>/dev/null || true
print_info "Build caches cleaned successfully."

# Create required asset directories
print_info "Creating asset directories..."
mkdir -p app/src/main/assets/models
mkdir -p app/src/main/assets/markers
print_info "Asset directories created:"
echo "  - app/src/main/assets/models/ (for 3D models)"
echo "  - app/src/main/assets/markers/ (for AR markers if needed)"

# Download placeholder info for 3D models
print_info "Creating model placeholder info file..."
cat > app/src/main/assets/models/README.md << 'EOF'
# 3D Models Directory

Place your glTF/GLB model files here for FediQuest quests:

Required models:
- tree.glb              (Tree Planting quest)
- recycle_bin.glb       (Recycling Station quest)
- cleanup_bag.glb       (Cleanup Zone quest)
- wildflower.glb        (Wildflower Garden quest)
- water_station.glb     (Water Conservation quest)
- birdhouse.glb         (Wildlife Habitat quest)

Model requirements:
- Format: glTF (.gltf) or GLB (.glb)
- Recommended: GLB for single-file distribution
- Scale: Models should be appropriately scaled for AR (1 unit = 1 meter)
- Size: Keep under 5MB per model for optimal performance
- License: Ensure models are FOSS-compatible (CC0, CC-BY, etc.)

Free model resources:
- Sketchfab (filter by CC licenses): https://sketchfab.com/
- Poly Haven (CC0): https://polyhaven.com/
- Kenney.nl (CC0): https://kenney.nl/
EOF

# Update Gradle wrapper permissions
print_info "Setting up Gradle wrapper..."
chmod +x gradlew

# Install required SDK components (if sdkmanager is available)
if [ -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    print_info "Installing required Android SDK components..."
    "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --install \
        "platforms;android-34" \
        "build-tools;34.0.0" \
        "platform-tools" 2>/dev/null || print_warn "SDK installation skipped (may require manual acceptance of licenses)"
else
    print_warn "sdkmanager not found. Please ensure Android SDK command-line tools are installed."
    print_info "You may need to install SDK components manually via Android Studio."
fi

# Build the app
print_info "Building FediQuest debug APK..."

# Run Gradle build from project root (gradlew is in project root, not app/)
./gradlew clean assembleDebug --stacktrace

# Check build result
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    APK_SIZE=$(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')
    print_info "=============================================="
    print_info "Build Successful!"
    print_info "=============================================="
    echo ""
    echo "APK Location: app/build/outputs/apk/debug/app-debug.apk"
    echo "APK Size: $APK_SIZE"
    echo ""
    print_info "To install on a connected device:"
    echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    print_info "To run the app:"
    echo "  1. Connect an ARCore-compatible Android device"
    echo "  2. Enable USB debugging on the device"
    echo "  3. Run: adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo "  4. Launch FediQuest from your device"
    echo ""
    print_warn "Note: For full AR functionality, you need to:"
    echo "  1. Add 3D models to app/src/main/assets/models/"
    echo "  2. Configure server.json URL in Config.kt"
    echo "  3. Test on a device with ARCore support"
else
    print_error "Build failed! APK not found."
    print_info "Check the build output above for errors."
    exit 1
fi

# Print summary
echo ""
print_info "=============================================="
print_info "Setup Complete!"
print_info "=============================================="
echo ""
echo "Next steps:"
echo "1. Add 3D models to app/src/main/assets/models/"
echo "2. Update SERVER_URL in app/src/main/java/org/fediquest/Config.kt"
echo "3. Deploy server/server.json to your web server"
echo "4. Install the APK on an ARCore-compatible device"
echo "5. Start exploring and completing eco-quests!"
echo ""
print_info "For more information, see README.md"
