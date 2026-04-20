/**
 * FediQuest Android Project Settings
 * Configures plugin management, dependency resolution, and project structure.
 * Uses Gradle toolchain for automatic Java 17 provisioning.
 */
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    }
}

// Enable auto-download of Java 17 if missing via Foojay Toolchain Resolver
toolchainManagement {
    jvm {
        javaRepositories {
            repository("foojay") {
                resolverClass.set(org.gradle.toolchains.foojay.FoojayToolchainResolver::class.java)
            }
        }
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
