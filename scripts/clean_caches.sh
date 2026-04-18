#!/bin/bash

# FediQuest Cache Cleaning Script
# Clears npm, Gradle, and local build caches for reproducible builds
# Usage: ./scripts/clean_caches.sh

set -e  # Exit on error

echo "========================================"
echo "FediQuest Cache Cleaner"
echo "========================================"
echo ""

# Get script directory (works regardless of where script is called from)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "Project root: $PROJECT_ROOT"
echo ""

# ---------------------------------------------
# Clean npm caches
# ---------------------------------------------
echo "[1/5] Cleaning npm caches..."

if command -v npm &> /dev/null; then
    echo "  - Clearing npm cache..."
    npm cache clean --force 2>/dev/null || echo "    Warning: npm cache clean failed"
    
    if [ -d "web/node_modules" ]; then
        echo "  - Removing web/node_modules..."
        rm -rf web/node_modules
    fi
    
    if [ -f "web/package-lock.json" ]; then
        echo "  - Note: Keeping package-lock.json (delete manually if needed)"
    fi
else
    echo "  - npm not found, skipping npm cache cleanup"
fi

echo ""

# ---------------------------------------------
# Clean Gradle caches
# ---------------------------------------------
echo "[2/5] Cleaning Gradle caches..."

if [ -d "app/build" ]; then
    echo "  - Removing app/build..."
    rm -rf app/build
fi

if [ -d "app/.gradle" ]; then
    echo "  - Removing app/.gradle..."
    rm -rf app/.gradle
fi

if [ -d ".gradle" ]; then
    echo "  - Removing project .gradle..."
    rm -rf .gradle
fi

# Optional: Clean global Gradle cache (commented out by default)
# Uncomment if you want to clean the global Gradle cache
# if [ -d "$HOME/.gradle/caches" ]; then
#     echo "  - Removing global Gradle caches (~/.gradle/caches)..."
#     rm -rf "$HOME/.gradle/caches"
# fi

echo "  - Gradle build directories cleaned"
echo ""

# ---------------------------------------------
# Clean Python caches
# ---------------------------------------------
echo "[3/5] Cleaning Python caches..."

find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
find . -type f -name "*.pyc" -delete 2>/dev/null || true
find . -type f -name "*.pyo" -delete 2>/dev/null || true
find . -type f -name "*.pyd" -delete 2>/dev/null || true

echo "  - Python cache files removed"
echo ""

# ---------------------------------------------
# Clean Godot caches
# ---------------------------------------------
echo "[4/5] Cleaning Godot caches..."

if [ -d "godot/.godot" ]; then
    echo "  - Removing godot/.godot..."
    rm -rf godot/.godot
fi

find . -type d -name ".import" -exec rm -rf {} + 2>/dev/null || true

echo "  - Godot cache files removed"
echo ""

# ---------------------------------------------
# Clean other build artifacts
# ---------------------------------------------
echo "[5/5] Cleaning other build artifacts..."

# Remove any .DS_Store files (macOS)
find . -type f -name ".DS_Store" -delete 2>/dev/null || true

# Remove any Thumbs.db files (Windows)
find . -type f -name "Thumbs.db" -delete 2>/dev/null || true

# Remove any .log files in build directories
find . -path "*/build/*.log" -delete 2>/dev/null || true

# Remove web server caches
if [ -d "web/.cache" ]; then
    rm -rf web/.cache
fi

echo "  - Miscellaneous build artifacts removed"
echo ""

# ---------------------------------------------
# Summary
# ---------------------------------------------
echo "========================================"
echo "Cache cleaning complete!"
echo "========================================"
echo ""
echo "Cleaned:"
echo "  ✓ npm cache and node_modules"
echo "  ✓ Gradle build directories"
echo "  ✓ Python __pycache__ and .pyc files"
echo "  ✓ Godot .import and .godot caches"
echo "  ✓ Miscellaneous build artifacts"
echo ""
echo "NOT cleaned (manual action required):"
echo "  - Global Gradle cache (~/.gradle/caches)"
echo "  - Android SDK build-tools/cache"
echo "  - Local npm global cache (run 'npm cache verify' to check)"
echo ""
echo "Next steps:"
echo "  1. Run 'npm install' in web/ directory"
echo "  2. Rebuild your project with './gradlew clean assembleDebug'"
echo "  3. Test the web demo with 'python3 -m http.server 8080' in web/"
echo ""
