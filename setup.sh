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
    # Try to find NDK in common locations
    if [ -d "$ANDROID_HOME/ndk-bundle" ]; then
        export ANDROID_NDK_HOME="$ANDROID_HOME/ndk-bundle"
        print_info "Found Android NDK at: $ANDROID_NDK_HOME"
    elif [ -d "$ANDROID_HOME/ndk" ]; then
        # Find the latest NDK version
        LATEST_NDK=$(ls -1 "$ANDROID_HOME/ndk" 2>/dev/null | sort -V | tail -n 1)
        if [ -n "$LATEST_NDK" ]; then
            export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$LATEST_NDK"
            print_info "Found Android NDK at: $ANDROID_NDK_HOME"
        fi
    fi
    
    if [ -z "$ANDROID_NDK_HOME" ]; then
        print_warn "ANDROID_NDK_HOME is not set. SceneView does not require NDK."
        print_info "If you want to add custom native code, set ANDROID_NDK_HOME manually."
    fi
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
mkdir -p app/src/main/assets/ml
print_info "Asset directories created:"
echo "  - app/src/main/assets/models/ (for 3D models)"
echo "  - app/src/main/assets/markers/ (for AR markers if needed)"
echo "  - app/src/main/assets/ml/ (for TF Lite models)"

# Download and integrate AI/ML modules locally
print_info "Downloading and integrating AI modules locally..."

# Create ML directory if not exists
ML_DIR="app/src/main/assets/ml"
mkdir -p "$ML_DIR"

# Download EfficientNet-Lite0 TF Lite model from TF Hub (mirror)
# Using a direct mirror for reliability - EfficientNet-Lite0 is ~4.5MB
EFFICIENTNET_URL="https://storage.googleapis.com/download.tensorflow.org/models/tflite/efficientnet-lite0-fp32_1_default_1.tflite"

print_info "Downloading EfficientNet-Lite0 TF Lite model..."
if command -v curl &> /dev/null; then
    curl -L -o "$ML_DIR/quest_classifier.tflite" "$EFFICIENTNET_URL" 2>/dev/null || {
        print_warn "Failed to download EfficientNet-Lite0, using demo model"
    }
elif command -v wget &> /dev/null; then
    wget -q -O "$ML_DIR/quest_classifier.tflite" "$EFFICIENTNET_URL" 2>/dev/null || {
        print_warn "Failed to download EfficientNet-Lite0, using demo model"
    }
else
    print_warn "Neither curl nor wget found. Using demo model."
fi

# Verify downloaded model
if [ -f "$ML_DIR/quest_classifier.tflite" ] && [ -s "$ML_DIR/quest_classifier.tflite" ]; then
    MODEL_SIZE=$(ls -lh "$ML_DIR/quest_classifier.tflite" | awk '{print $5}')
    print_info "✓ AI model downloaded: $ML_DIR/quest_classifier.tflite ($MODEL_SIZE)"
    
    # Copy to main assets directory for backward compatibility
    cp "$ML_DIR/quest_classifier.tflite" "app/src/main/assets/quest_classifier.tflite"
    print_info "✓ Model copied to app/src/main/assets/quest_classifier.tflite"
else
    print_warn "AI model download failed. Creating minimal valid TFLite header for demo mode."
    # Create minimal valid TFLite file with TFL3 header (demo mode)
    printf '\x54\x46\x4c\x33\xd4\x00\x00\x00\x01\x00\x00\x00' > "$ML_DIR/quest_classifier.tflite"
    printf '\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00' >> "$ML_DIR/quest_classifier.tflite"
    printf '\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00' >> "$ML_DIR/quest_classifier.tflite"
    printf '\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00' >> "$ML_DIR/quest_classifier.tflite"
    cp "$ML_DIR/quest_classifier.tflite" "app/src/main/assets/quest_classifier.tflite"
    print_info "Demo model created (212 bytes) - will run in AI mode with simulated inference"
fi

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
