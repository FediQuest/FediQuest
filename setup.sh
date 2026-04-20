#!/usr/bin/env bash
# =============================================================================
# FediQuest Android Project Setup Script
# =============================================================================
# Purpose: Initialize development environment, verify dependencies, and prepare
#          the project for local assembly and GitHub synchronization.
#
# Features:
# - Java 17 Toolchain verification (Foojay auto-resolver ready)
# - Gradle wrapper validation
# - Local properties generation (gitignored)
# - Asset directory structure creation
# - Git initialization and initial commit
#
# Usage: ./setup.sh
# =============================================================================

set -e # Exit immediately if a command exits with a non-zero status

# --- Colors for Output ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# --- Configuration ---
PROJECT_NAME="FediQuest"
MIN_JAVA_VERSION=17
GRADLE_WRAPPER="./gradlew"

# --- Helper Functions ---
log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# --- 1. Java Environment Verification ---
verify_java() {
    log_info "Checking Java environment..."
    
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d '.' -f 1)
        
        # Handle potential non-numeric version strings (e.g., "17-ea")
        if [[ ! "$JAVA_VERSION" =~ ^[0-9]+$ ]]; then
            JAVA_VERSION=$(echo "$JAVA_VERSION" | grep -o '[0-9]\+' | head -1)
        fi

        if [ "$JAVA_VERSION" -ge "$MIN_JAVA_VERSION" ]; then
            log_success "Java $JAVA_VERSION detected. Meets requirement (≥ $MIN_JAVA_VERSION)."
        else
            log_warn "Java $JAVA_VERSION found, but $MIN_JAVA_VERSION is recommended."
            log_info "Gradle Foojay Toolchain Resolver will automatically download Java $MIN_JAVA_VERSION if missing during build."
        fi
    else
        log_warn "Java not found in PATH."
        log_info "Gradle Foojay Toolchain Resolver will automatically download Java $MIN_JAVA_VERSION during first build."
    fi
}

# --- 2. Gradle Wrapper Validation ---
verify_gradle() {
    log_info "Checking Gradle wrapper..."
    
    if [ -f "$GRADLE_WRAPPER" ]; then
        chmod +x "$GRADLE_WRAPPER"
        log_success "Gradle wrapper found and made executable."
    else
        log_error "Gradle wrapper not found. Please ensure gradlew exists in the project root."
    fi
}

# --- 3. Local Configuration Setup ---
setup_local_config() {
    log_info "Setting up local configuration..."
    
    if [ ! -f "local.properties" ]; then
        # Only set android.sdk.path if ANDROID_HOME is explicitly set, 
        # otherwise let Android Studio/Gradle detect it automatically.
        if [ -n "$ANDROID_HOME" ]; then
            echo "sdk.dir=$ANDROID_HOME" > local.properties
            log_success "Created local.properties using ANDROID_HOME."
        else
            # Create empty file to prevent build errors, let Gradle find SDK
            touch local.properties
            log_info "Created empty local.properties. Ensure ANDROID_HOME is set or Android Studio is installed."
        fi
    else
        log_info "local.properties already exists. Skipping."
    fi
}

# --- 4. Directory Structure Initialization ---
setup_directories() {
    log_info "Creating required directory structure..."
    
    mkdir -p app/src/main/assets
    mkdir -p app/src/main/res/raw
    mkdir -p app/build
    mkdir -p gradle/wrapper
    
    # Create placeholder for TF Lite model to prevent missing asset errors
    if [ ! -f "app/src/main/assets/quest_classifier.tflite" ]; then
        touch app/src/main/assets/quest_classifier.tflite
        log_warn "Created placeholder quest_classifier.tflite. Replace with actual model for AI verification."
    fi
    
    # Create placeholder GLB for AR
    if [ ! -f "app/src/main/assets/spawn.glb" ]; then
        touch app/src/main/assets/spawn.glb
        log_warn "Created placeholder spawn.glb. Replace with actual 3D model for AR spawning."
    fi

    log_success "Directory structure initialized."
}

# --- 5. Git Initialization ---
setup_git() {
    log_info "Checking Git repository status..."
    
    if [ -d ".git" ]; then
        log_info "Git repository already initialized."
        
        # Check if there are uncommitted changes
        if ! git diff --quiet || ! git diff --cached --quiet; then
            log_warn "Uncommitted changes detected."
            read -p "Do you want to commit all changes now? (y/n) " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                git add .
                git commit -m "chore: initial project setup and configuration"
                log_success "Changes committed."
            fi
        else
            log_success "Working tree clean."
        fi
    else
        log_info "Initializing new Git repository..."
        git init
        git add .
        git commit -m "chore: initial commit - FediQuest Android project skeleton"
        log_success "Git repository initialized and initial commit created."
    fi
}

# --- Main Execution ---
main() {
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}  $PROJECT_NAME Project Setup${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo

    verify_java
    verify_gradle
    setup_local_config
    setup_directories
    setup_git

    echo
    log_success "Setup complete!"
    echo
    log_info "Next steps:"
    echo "  1. Open project in Android Studio."
    echo "  2. Wait for Gradle sync to complete (Java 17 will be auto-downloaded if needed)."
    echo "  3. Replace placeholder assets (quest_classifier.tflite, spawn.glb) with real files."
    echo "  4. Run: ./gradlew assembleDebug"
    echo
}

main "$@"
