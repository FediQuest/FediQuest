/**
 * FediQuest Root Build Configuration
 * Top-level build file for configuring common settings across all sub-projects.
 * The Foojay resolver is applied via settings.gradle.kts convention plugin.
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}
