// Shared module for cross-platform code (Android + Web)
plugins {
    kotlin("multiplatform") version "1.9.22"
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    js(IR) {
        browser()
        nodejs()
    }
    
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("com.squareup.okio:okio:3.7.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            }
        }
        
        androidMain {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
            }
        }
        
        jsMain {
            dependencies {
                implementation(npm("three", "0.160.0"))
                implementation(npm("@google/model-viewer", "3.4.0"))
            }
        }
    }
}

android {
    namespace = "org.fediquest.shared"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 26
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
