# CMakeLists.txt - Native Build Configuration Snippet
# Place this in app/CMakeLists.txt for native AR library integration

cmake_minimum_required(VERSION 3.22.1)

# Project name and version
project("FediQuestNative" VERSION "1.0.0")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# =============================================================================
# ARToolKit Integration (Primary Native Option)
# =============================================================================

# Find prebuilt ARToolKit libraries in jniLibs directory
# These should be placed in: app/src/main/jniLibs/{abi}/
# Note: .so files are NOT included in repository (>500MB limit)

set(ARTOOLKIT_LIBS
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libAR.so
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libARw.so
)

# Check if ARToolKit libraries exist
foreach(lib ${ARTOOLKIT_LIBS})
    if(NOT EXISTS ${lib})
        message(WARNING "ARToolKit library not found: ${lib}")
        message(WARNING "Please build/download ARToolKit and place .so files in jniLibs/")
    endif()
endforeach()

# =============================================================================
# Native Source Files
# =============================================================================

# Add your native source files here
set(NATIVE_SOURCES
    src/main/cpp/fediquest_native.cpp
    src/main/cpp/gps_utils.cpp
    src/main/cpp/spawn_renderer.cpp
)

# =============================================================================
# Build Shared Library
# =============================================================================

add_library(
    fediquest-native
    SHARED
    ${NATIVE_SOURCES}
)

# =============================================================================
# Include Directories
# =============================================================================

# Add include paths for headers
target_include_directories(
    fediquest-native
    PRIVATE
    ${CMAKE_SOURCE_DIR}/src/main/cpp/include
    ${ANDROID_NDK}/sources/android/native_app_glue
)

# =============================================================================
# Link Libraries
# =============================================================================

target_link_libraries(
    fediquest-native
    android
    log
    EGL
    GLESv3
    
    # Link ARToolKit if available
    ${ARTOOLKIT_LIBS}
    
    # Android NDK libraries
    atomic
)

# =============================================================================
# Compiler Flags
# =============================================================================

# Optimization flags for release builds
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(
        fediquest-native
        PRIVATE
        -O3
        -ffast-math
    )
endif()

# Debug flags
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(
        fediquest-native
        PRIVATE
        -g
        -O0
        -DDEBUG
    )
endif()

# =============================================================================
# ARCore Alternative (Optional)
# =============================================================================

# If building with ARCore instead of/in addition to ARToolKit:
# Uncomment below and add ARCore dependency in build.gradle.kts

# find_package(arcore REQUIRED CONFIG)
# target_link_libraries(fediquest-native arcore)

# =============================================================================
# Custom Commands for Asset Copying
# =============================================================================

# Copy assets to build directory
add_custom_command(
    TARGET fediquest-native
    POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_directory
    ${CMAKE_SOURCE_DIR}/src/main/assets
    ${CMAKE_BINARY_DIR}/intermediates/assets/debug
    COMMENT "Copying assets to build directory"
)

# =============================================================================
# Notes for Reviewers
# =============================================================================

# This CMakeLists.txt is a skeleton showing where prebuilt .so libraries belong.
# The actual ARToolKit .so files must be downloaded/built separately.
# 
# To build ARToolKit from source:
# 1. Clone: git clone https://github.com/artoolkitx/artoolkit5.git
# 2. Build with NDK following instructions in app/README_NATIVE.md
# 3. Copy resulting .so files to app/src/main/jniLibs/{abi}/
#
# For WebAR demo (primary flow), native libraries are NOT required.
# The app defaults to launching the web demo at runtime.
