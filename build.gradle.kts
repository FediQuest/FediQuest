/**
 * FediQuest Root Build Configuration
 * Top-level build file for configuring common settings across all sub-projects.
 * Uses Gradle toolchain resolver for automatic Java 17 provisioning.
 */
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
}
