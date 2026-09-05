# Indian Nutrition & Weight Tracker (Android)

A native Android app for tracking calories, protein, water, and body weight,
optimized for Indian foods. Built with **Kotlin + Jetpack Compose (Material 3)**
as a single-activity app.

## Status

This repository was rewritten from a web app (React + Vite + Express +
Capacitor) into a **native Android app**. The web toolchain and Capacitor shell
have been removed (see git history); the native rewrite is in progress:

- [x] Phase 0 — remove web/Capacitor, native Compose skeleton, CI build gate
- [x] Phase 1 — theme, navigation, domain models, target calculator, settings
- [x] Phase 2 — food datasets + Room persistence
- [x] Phase 3 — home dashboard & logging flows
- [x] Phase 4 — food search + Open Food Facts
- [x] Phase 5 — progress charts
- [x] Phase 6 — calculator/settings + CSV/JSON export & import
- [x] Phase 7 — tests & hardening
- [x] Phase 8 — release readiness

## ⚠️ Upgrading from the old Capacitor/Web version

The old app stored data inside a WebView (localStorage). The new native app
uses Room + DataStore, which is a **completely separate storage layer**.
**Your old data will not carry over automatically.**

Before updating:

1. **Export your data** — open the old app → Calculator → **Backup JSON**
2. **Update** the app (it installs over the existing one)
3. **Import your data** — open the new app → Calculator → Data Management →
   **Import JSON** → choose **Replace All** (or Merge to keep any new entries)

If you update without exporting first, your old logs, weights, and water
history will be lost. The JSON backup file is the only way to migrate.

The native app also migrates its Room database in place when new local fields
are added; existing logs and custom foods are preserved during an app update.

## Features

- **Food logging** from three sources: a curated NIN/IFCT 2017 database
  (56 Indian foods), curated Indian packaged products (36), and live
  Open Food Facts search with an offline cache.
- **Target calculator**: Mifflin-St Jeor BMR → TDEE → calorie, protein, and
  water targets with transparent formula breakdowns and custom overrides.
- **Daily dashboard**: calories/protein vs targets, macros, meals grouped by
  Breakfast/Lunch/Snack/Dinner, weight + BMI summary, water tracker.
- **Progress charts**: weight trend, daily calories/protein/water vs targets.
- **Data export/import**: CSV and JSON backup with round-trip fidelity.
- **Editing & recipes**: edit food, water, and weight entries; build custom recipes from database ingredients with calculated macros; choose g, ml, pieces, cups, or bowls for custom servings.
- **System-aware UI**: follows system light/dark mode, uses dynamic system colours on Android 12+, and switches to dark surfaces while Battery Saver is active.
- **Privacy-first**: all data stored on-device (Room + DataStore). No accounts.

## Requirements

- JDK 17+ (JDK 21 recommended)
- Android SDK 36 (compile/target), min SDK 24

## Build

```bash
./gradlew :app:assembleDebug      # debug APK (no minification)
./gradlew :app:assembleRelease     # release APK (R8 minified + shrunk)
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

Open the project in Android Studio and run `app` on an emulator or device.

## Tests

```bash
./gradlew :app:testDebugUnitTest            # JVM unit tests
./gradlew :app:connectedAndroidTest          # instrumented tests (device)
```

### Unit test coverage

| Module | Test |
|--------|------|
| Target calculator | `TargetCalculatorTest` |
| Date utilities | `DateUtilsTest` |
| Unit conversions | `UnitConvertersTest` |
| Number utilities | `NumberUtilsTest` |
| Settings repository | `SettingsRepositoryTest` |
| Food lookup / search | `FoodLookupTest` |
| OFF API client | `OffApiClientTest` |
| OFF product parser | `OffProductParserTest` |
| OFF cache repository | `OffCacheRepositoryTest` |
| OFF search repository | `OffSearchRepositoryTest` |
| Weight repository | `WeightRepositoryTest` |
| Chart math | `ChartMathTest` |
| CSV export | `CsvExporterTest` |
| JSON backup | `JsonBackupTest` |

### Instrumented test coverage

| Test | What it covers |
|------|---------------|
| `DaoInstrumentedTest` | Room DAO CRUD, upsert, ordering, eviction |
| `RepositoryInstrumentedTest` | Repository import, upsert-per-date, clear |
| `AppUiTest` | Home screen, water quick-add, weight logging, food search, custom food creation, calculator |

## Continuous integration

GitHub Actions (`.github/workflows/android.yml`) assembles the debug APK and
runs unit tests on every push/PR. Instrumented tests run on an API 34 emulator.

## Releases

Pushing a version tag (`git tag v1.0.0 && git push origin v1.0.0`) triggers
`.github/workflows/release.yml`, which builds a **signed release APK**, checks
its signature with `apksigner`, and publishes it to a GitHub Release.

Release signing credentials are supplied as repository secrets (never
committed):

| Secret | Value |
|--------|-------|
| `ANDROID_KEYSTORE_BASE64` | base64-encoded PKCS12/JKS keystore |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias in the keystore |
| `ANDROID_KEY_PASSWORD` | password of that key |

The workflow decodes the keystore at build time and hands it to Gradle via
environment variables (`ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` — see `app/build.gradle`). Bump
`versionCode` / `versionName` in `app/build.gradle` before tagging a release.

## Data & attributions

- **Indian food composition:** derived from *Indian Food Composition Tables
  (IFCT 2017)*, National Institute of Nutrition (NIN), Indian Council of
  Medical Research (ICMR).
- **Branded products:** data from [Open Food Facts](https://world.openfoodfacts.org)
  under the Open Database License (ODbL) and its database contents license.
- All personal logs remain on-device; exports (CSV/JSON) are generated
  locally and shared only when you choose to share them.
