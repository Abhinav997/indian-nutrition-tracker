# Indian Nutrition & Weight Tracker (Android)

A native Android app for tracking calories, protein, water, and body weight,
optimized for Indian foods. Built with **Kotlin + Jetpack Compose (Material 3)**
as a single-activity app.

## Status

This repository is being rewritten from a web app (React + Vite + Express +
Capacitor) into a **native Android app**. The web toolchain and Capacitor shell
have been removed (see git history); the native rewrite is in progress:

- [x] Phase 0 — remove web/Capacitor, native Compose skeleton, CI build gate
- [x] Phase 1 — theme, navigation, domain models, target calculator, settings
- [ ] Phase 2 — food datasets + Room persistence
- [ ] Phase 3 — home dashboard & logging flows
- [ ] Phase 4 — food search + Open Food Facts
- [ ] Phase 5 — progress charts
- [ ] Phase 6 — calculator/settings + CSV/JSON export & import
- [ ] Phase 7 — tests & hardening
- [ ] Phase 8 — release readiness

## Planned features (parity with the web app)

- **Food logging** from three sources: a curated NIN/IFCT 2017 database
  (56 Indian foods), curated Indian packaged products (36), and live
  Open Food Facts search with an offline cache.
- **Target calculator**: Mifflin-St Jeor BMR → TDEE → calorie, protein, and
  water targets with transparent formula breakdowns and custom overrides.
- **Daily dashboard**: calories/protein vs targets, macros, meals grouped by
  Breakfast/Lunch/Snack/Dinner, weight + BMI summary, water tracker.
- **Progress charts**: weight trend, daily calories/protein/water vs targets.
- **Privacy-first**: all data stored on-device (Room + DataStore). No accounts.

## Requirements

- JDK 17+ (JDK 21 recommended)
- Android SDK 36 (compile/target), min SDK 24

## Build

```bash
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

Open the project in Android Studio and run `app` on an emulator or device.

## Tests

```bash
./gradlew :app:testDebugUnitTest            # JVM unit tests
./gradlew :app:connectedAndroidTest          # instrumented tests (device)
```

## Continuous integration

GitHub Actions (`.github/workflows/android.yml`) assembles the debug APK and
runs unit tests on every push/PR.

## Data & attributions

- **Indian food composition:** derived from *Indian Food Composition Tables
  (IFCT 2017)*, National Institute of Nutrition (NIN), Indian Council of
  Medical Research (ICMR).
- **Branded products:** data from [Open Food Facts](https://world.openfoodfacts.org)
  under the Open Database License (ODbL) and its database contents license.
- All personal logs remain on-device; exports (CSV/JSON) are generated
  locally and shared only when you choose to share them.
