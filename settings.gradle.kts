pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.mozilla.org/maven2/") } // Mozilla WebXR
    }
}

rootProject.name = "FediQuest"
include(":app")
include(":web")  // WebAR module for cross-platform support
include(":shared")  // Shared code between Android and Web
