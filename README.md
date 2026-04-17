# 🌿 FediQuest - Eco-Quest AR/GPS Mobile App

A copyright-safe, Pokémon Go-inspired AR/GPS eco-quest mobile app built with Kotlin for Android.

## 🎯 Project Goal

FediQuest encourages users to complete real-world environmental quests:
- **View nearby eco-quests** on an OSMDroid map
- **Use CameraX + TFLite** to detect trash via AI
- **Place AR markers** with SceneView/ARCore
- **Customize avatar & companion** with vector drawables
- **Earn XP/coins, level up**, unlock badges
- **Sync achievements** to the Fediverse (ActivityPub)

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 1.9.22, JVM Target 17 |
| UI | ViewBinding, Material 3, Jetpack Navigation |
| Architecture | MVVM + Repository + UseCase pattern |
| DI | Manual ServiceLocator (no Hilt/Koin) |
| Maps | OSMDroid 6.1.17 (FOSS) |
| Camera | CameraX 1.3.1 |
| ML | TFLite 2.14.0 + Task Vision 0.4.4 |
| AR | ARCore 1.41.0 + SceneView 0.10.0 |
| Location | FusedLocationProvider |
| Networking | Retrofit 2.9.0 + OkHttp 4.12.0 |
| Database | Room 2.6.1 |
| Coroutines | kotlinx-coroutines 1.7.3 |

## 📁 Project Structure

```
app/src/main/java/com/fediquest/app/
├── FediQuestApp.kt          # Application class
├── MainActivity.kt           # Main activity with bottom nav
├── di/ServiceLocator.kt      # Manual dependency injection
├── data/
│   ├── models/               # Data classes (Quest, User, etc.)
│   ├── repositories/         # Data repositories
│   └── local/AppDatabase.kt  # Room database
├── domain/
│   ├── usecases/             # Business logic use cases
│   └── models/               # Domain models
├── ui/
│   ├── map/                  # OSMDroid map fragment
│   ├── scan/                 # CameraX + TFLite scan
│   ├── avatar/               # Avatar customization
│   └── quest/                # Quest detail screens
├── util/                     # Utilities (XPManager, etc.)
└── viewmodel/                # SharedViewModel
```

## 🎨 Copyright-Safe Content

All content is original and copyright-safe:

| Original Content | Description |
|-----------------|-------------|
| Eco-Drake | Small dragon that loves planting trees |
| Green Phoenix | Bird made of leaves and vines |
| Forest Unicorn | Mystical guardian of nature |
| Aqua Turtle | Water guardian that cleans oceans |
| Solar Lion | Lion powered by sunlight |

**Quest Categories:** Nature 🌳, Recycling ♻️, Cleanup 🧹, Planting 🌱, Wildlife 🦋, Water 💧

## 🚀 Build & Run

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34

### Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell monkey -p com.fediquest.app.debug -c android.intent.category.LAUNCHER 1
```

### Debugging

```bash
# Logcat filter
adb logcat | grep -E "FediQuest|AndroidRuntime|TFLite|ARCore"
```

## 🏗️ Architecture

### MVVM Pattern
```
Fragment → ViewModel → UseCase → Repository → Database/API
```

### Key Components

1. **ServiceLocator**: Manual DI container for testability
2. **SharedViewModel**: Cross-fragment state management
3. **UseCases**: Single-responsibility business logic
4. **Repositories**: Abstract data sources

## 📝 License

This project is open source and available under the [Apache License 2.0](LICENSE).

---

**Note:** This is a development scaffold. Production features like TFLite model integration, Fediverse API, and AR markers require additional implementation.
