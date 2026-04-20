# FediQuest Android

🎮🌍 **Play-to-Give-Back AR Gaming Platform**

FediQuest is an open-source augmented reality gaming platform that combines location-based quests with social-ecological impact. Built with Kotlin and SceneView for native AR experiences, featuring offline-first architecture and ActivityPub integration for Fediverse connectivity.

## Features

- 📱 **Native AR Experience**: Powered by SceneView 4.0.1 (open-source Sceneform fork)
- 🏠 **Offline-First**: All core features work without internet; sync deferred when online
- 🧠 **AI Verification**: TensorFlow Lite for image-based quest proof verification
- 🗺️ **Location-Based Quests**: GPS-triggered social and ecological challenges
- 🌐 **Fediverse Ready**: ActivityPub client for sharing achievements to Mastodon and more
- 🎯 **Companion System**: Evolvable AR companions that grow with your progress
- 🔒 **Privacy-First**: Local verification, opt-in social features

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| Java Version | 17 |
| AR Engine | SceneView 4.0.1 |
| Database | Room 2.6.1 |
| ML Framework | TensorFlow Lite Task Vision 0.4.4 |
| Camera | CameraX 1.3.1 |
| Networking | OkHttp 4.12.0 + Retrofit 2.9.0 |
| DI | Manual (no heavy frameworks) |

## Project Structure

```
app/
├── src/main/java/org/fediquest/
│   ├── data/
│   │   ├── entity/          # Room entities (PlayerStateEntity, QuestEntity)
│   │   ├── dao/             # Data Access Objects (PlayerDao, QuestDao)
│   │   ├── database/        # Room database (AppDatabase)
│   │   └── remote/          # ActivityPub client (ActivityPubClient.kt)
│   ├── camera/              # CameraX helpers
│   ├── companion/           # Companion evolution system
│   ├── fediverse/           # ActivityPub integration
│   ├── ml/                  # TensorFlow Lite models
│   ├── offline/             # Sync queue manager
│   ├── Config.kt            # App configuration
│   ├── FediQuestApp.kt      # Application class
│   ├── MainActivity.kt      # Main AR activity
│   ├── PlayerProfile.kt     # Player state management
│   ├── QuestVerifier.kt     # Quest verification logic
│   └── SpawnFetcher.kt      # AR spawn management
├── build.gradle.kts         # App-level build config
└── proguard-rules.pro       # ProGuard rules
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Physical ARCore-compatible device (emulator AR support limited)

### Build Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/fediquest/fediquest-android.git
   cd fediquest-android
   ```

2. **Open in Android Studio**
   - File → Open → Select project directory
   - Wait for Gradle sync to complete

3. **Build locally**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on device**
   - Connect ARCore-compatible Android device
   - Click Run (Shift+F10) in Android Studio

### Configuration

No API keys required for basic functionality. Optional features:

- **Enhanced GPS**: Uncomment `play-services-location` in `build.gradle.kts`
- **Custom TFLite Model**: Place `quest_classifier.tflite` in `app/src/main/assets/`

## Architecture

### Offline-First Design

All verification runs locally:
- Quest proofs verified with TF Lite on-device
- GPS validation uses Android LocationManager
- Results stored in Room database immediately
- Fediverse sync queued and deferred until online

### AR Placement (SceneView 4.x)

```kotlin
arSceneView.setOnTapListener { hitResult ->
    val node = ModelNode(
        model = ModelBuilder.create {
            uri = Uri.parse("file:///android_asset/spawn.glb")
        }
    )
    node.position = Position(hitResult.position.x, hitResult.position.y, hitResult.position.z)
    arSceneView.scene.addChild(node)
}
```

### Room Migration Example

```kotlin
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE player_state ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE player_state SET updatedAt = strftime('%s', 'now') * 1000 WHERE updatedAt = 0")
    }
}
```

## Development

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint check
./gradlew lint
```

### Code Style

- Kotlin coding conventions
- KDoc comments for public APIs
- Maximum line length: 120 characters

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Areas for Contribution

- 🧪 Additional unit/integration tests
- 🌍 More quest types and challenges
- 🎨 UI/UX improvements
- 🤖 Enhanced ML models
- 🌐 ActivityPub protocol enhancements

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [SceneView](https://github.com/SceneView/sceneview) - Open-source AR library
- [TensorFlow Lite](https://www.tensorflow.org/lite) - On-device ML
- [ActivityPub Protocol](https://www.w3.org/TR/activitypub/) - Federated social networking
- Android Jetpack Team - Room, CameraX, Lifecycle libraries

## Contact

- GitHub Issues: [Report bugs or request features](https://github.com/fediquest/fediquest-android/issues)
- Fediverse: [@fediquest@mastodon.social](https://mastodon.social/@fediquest)

---

Built with ❤️ for a sustainable future
