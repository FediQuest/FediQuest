/**
 * FediQuest Android Project Settings
 * Configures plugin management, dependency resolution, and project structure.
 * Uses Gradle toolchain for automatic Java 17 provisioning via Foojay.
 */
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Enable Foojay Toolchain Resolver for automatic JDK download
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    versionCatalogs {
        create("libs") {
            from("gradle/libs.versions.toml")
        }
    }
}

rootProject.name = "FediQuest"
include(":app")
