// Web module build configuration for FediQuest WebAR
plugins {
    id("org.jetbrains.kotlin.js") version "1.9.22"
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        main {
            dependencies {
                implementation(project(":shared"))
                implementation(npm("three", "0.160.0"))
                implementation(npm("@google/model-viewer", "3.4.0"))
            }
        }
    }
}
