# FediQuest

[![Android CI](https://github.com/fediquest/FediQuest/actions/workflows/android-ci.yml/badge.svg)](https://github.com/fediquest/FediQuest/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

FediQuest is an open-source augmented reality GPS-based experience platform that encourages people to go outside, explore their environment, help each other through community quests, and do something good for the environment.

## Features

- **Offline-First Architecture**: All verification happens locally with deferred sync
- **AR Quest Placement**: SceneView 4.0.1 for AR object placement with tap interaction
- **Local Verification**: TensorFlow Lite for on-device image classification
- **ActivityPub Integration**: Federated quest sharing across instances
- **Room Database**: Persistent local storage with migration support

## Tech Stack

- **Language**: Kotlin
- **Min SDK**: 26
- **Target SDK**: 34
- **Compile SDK**: 34
- **Java Version**: 17

### Dependencies

- **SceneView**: 4.0.1 (AR rendering)
- **CameraX**: 1.3.1 (camera access)
- **Room**: 2.6.1 (local database)
- **TensorFlow Lite**: 2.14.0 (on-device ML)
- **Ktor**: 2.3.7 (HTTP client for ActivityPub)
- **Kotlinx Coroutines**: 1.7.3 (async operations)

## Project Structure

```
app/src/main/java/org/fediquest/
├── MainActivity.kt              # Main activity with AR scene
├── QuestVerifier.kt             # Local quest verification logic
├── data/
│   ├── entity/
│   │   ├── PlayerStateEntity.kt # Player state data class
│   │   └── QuestEntity.kt       # Quest data class
│   ├── dao/
│   │   ├── PlayerDao.kt         # Player database access
│   │   └── QuestDao.kt          # Quest database access
│   ├── database/
│   │   └── AppDatabase.kt       # Room database configuration
│   └── remote/
│       └── ActivityPubClient.kt # ActivityPub network client
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Build Instructions

1. Clone the repository:
```bash
git clone https://github.com/fediquest/FediQuest.git
cd FediQuest
```

2. Build the project:
```bash
./gradlew assembleDebug
```

3. Install on device:
```bash
./gradlew installDebug
```

## Configuration

### Version Catalog

Dependencies are managed via Gradle version catalog in `gradle/libs.versions.toml`.

### Database Migrations

Room migrations are defined in `AppDatabase.kt`. Current migration:
- Migration 1→2: Added `updatedAt` column to `player_state` table

## Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

Run lint:
```bash
./gradlew lint
```

## CI/CD

GitHub Actions workflow (`.github/workflows/android-ci.yml`) provides:
- Build verification on push/PR
- Unit test execution
- Lint checks
- APK artifact generation

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Roadmap

- [ ] Multiplayer AR interactions
- [ ] Environmental quest templates
- [ ] Instance federation protocol
- [ ] Companion creature system
- [ ] Avatar customization

## Contact

- Project: https://github.com/fediquest/FediQuest
- Issues: https://github.com/fediquest/FediQuest/issues
