# Native Android Rewrite — Implementation Plan
**Remove web (React/Vite/Express) + Capacitor → rewrite in Jetpack Compose (Kotlin)**

Goal version: **feature-parity native Android app**, same data model, same offline-first behavior, same UI language (teal/slate, card-based, mobile-first), with the web-layer bugs fixed along the way.

---

## 1. Scope

### In scope
- Delete the entire web toolchain: `package.json`, `package-lock.json`, `bun.lock`, `vite.config.ts`, `tsconfig.json`, `server.ts`, `index.html`, `public/`, `src/`, `.env.example`, `metadata.json`, root `.idea/`, AI Studio README.
- Remove Capacitor from the Android project (plugins, `capacitor.*.gradle`, `capacitor-cordova-android-plugins`, `capacitor.config.ts`, node-sourced `capacitor-android` module).
- Rewrite all app logic in Kotlin: data layer (Room + DataStore), calculators, OFF search client, CSV/JSON export.
- Rewrite all screens in Jetpack Compose (Material 3), single-activity.
- Port the food datasets (56 NIN/IFCT + 36 packaged foods) without value changes.
- Carry over known web bugs as **fixed behavior** (see §7).

### Out of scope (first pass — explicit no's)
- iOS / web targets, cloud sync, accounts, server (the Express/OFF proxy **goes away**; a native client calls OFF directly with proper `User-Agent`, so CORS is no longer an issue).
- Gemini / AI features (already unused in web version).
- Barcode scanner camera, notifications/reminders, meal planning — defer as follow-up epics.

---

## 2. Current Android state (what we're rewriting from)

| Item | Current | Action |
|---|---|---|
| Gradle wrapper | Gradle 9.7.1 | Keep |
| AGP | 9.4.0 | Keep |
| Kotlin | 2.1.0 + `kotlin.plugin.compose` | Keep |
| `settings.gradle` | includes `:capacitor-cordova-android-plugins` + `capacitor.settings.gradle` | **Remove** |
| `app/build.gradle` | Compose BOM 2024.12.01, `ui`, `material3`, `activity-compose` already present; Capacitor deps + `flatDir` + `capacitor.build.gradle` + google-services try/catch | Clean (see §6) |
| `variables.gradle` | sdk versions + capacitor/cordova/compose template vars | Clean |
| `MainActivity.java` | Empty `BridgeActivity` | Replace with Kotlin `MainActivity` (`ComponentActivity` + `setContent`) |
| `ComposeActivity.kt` | "Hello World" Compose sample, exported in manifest | **Delete** |
| `activity_main.xml` | WebView layout | **Delete** |
| `AndroidManifest.xml` | ComposeActivity entry + FileProvider | Rewrite (single activity, keep FileProvider for exports) |
| `res/values/colors.xml` | **Missing** — `styles.xml` references `@color/colorPrimary` (won't compile as-is) | Add teal palette / switch to Compose theme |
| Tests | template placeholder in `com.getcapacitor.myapp` | Move to `com.indian.nutrition.tracker`, rewrite |

---

## 3. Target architecture

Single-activity, **MVVM + unidirectional data flow**, manual DI (small app — Hilt optional, see §9):

```
MainActivity (ComponentActivity)
 └─ NutritionApp()  → MaterialTheme
      └─ NavHost (Navigation Compose)
           ├─ home        → HomeScreen + HomeViewModel
           ├─ search      → FoodSearchScreen + FoodSearchViewModel
           ├─ progress    → ProgressScreen + ProgressViewModel
           └─ calculator  → CalculatorSettingsScreen + CalculatorViewModel
            (dialogs/bottom sheets hosted by screens)
                 │
                  ▼ ViewModels (StateFlow<UiState>, 1 per screen)
                 │
                  ▼ Repositories  (FoodRepository, LogRepository, WeightRepository,
                                   WaterRepository, SettingsRepository, OffSearchRepository)
                 │
      ┌───────────┼──────────────────┬────────────────────┐
      ▼           ▼                  ▼                    ▼
   Room DB    DataStore        OFF API client        Assets (JSON datasets)
  (logs,      (settings/       (Retrofit+OkHttp,     (nin_ifct.json,
   weight,     preferences)     kotlinx.serialization) packaged_foods.json)
   water,
   custom foods,
   off cache)
```

### Package layout (new)

```
com.indian.nutrition.tracker
├── MainActivity.kt
├── NutritionApp.kt                     // theme + nav graph
├── di/AppContainer.kt                  // manual DI (or Hilt)
├── data
│   ├── local
│   │   ├── AppDatabase.kt              // Room
│   │   ├── entities/ (DailyLogEntity, WeightLogEntity, WaterLogEntity,
│   │   │             CustomFoodEntity, OffCacheEntity)
│   │   ├── dao/     (DailyLogDao, WeightLogDao, WaterLogDao,
│   │   │             CustomFoodDao, OffCacheDao)
│   │   └── SettingsDataStore.kt        // preferences
│   ├── remote
│   │   ├── OffApiService.kt            // Retrofit interface (in + world)
│   │   ├── OffApiClient.kt             // fallback chain + cache policy
│   │   └── dto/ (OffProductDto, OffSearchResponseDto)
│   ├── repository/ (SettingsRepository, FoodRepository, LogRepository,
│   │                WeightRepository, WaterRepository, OffSearchRepository)
│   └── mapper/ (dto↔entity↔domain)
├── domain
│   ├── model/    (Food, FoodSource, MealType, DailyLog, WeightLog, WaterLog,
│   │              CustomFood, UserSettings, CalculatorResult, Sex, ActivityLevel,
│   │              GoalType, UnitSystem, DateRange)
│   ├── calculator/TargetCalculator.kt  // port of utils/calculator.ts
│   └── export/   (CsvExporter.kt, JsonBackup.kt, backup schema DTOs)
├── util/ (DateUtils.kt — LocalDate keys, UnitConverters.kt — kg↔lb, bmi)
└── ui
    ├── theme/ (Color.kt, Type.kt, Theme.kt)   // teal/slate palette from web CSS
    ├── components/ (MacroProgressBar, SourceBadge, ServingPickerSheet,
    │                WeightSheet, CustomFoodDialog, WaterCard, StatCard, ...)
    └── screens/ (home/, search/, progress/, calculator/)
```

### Domain model mapping (from `types.ts`)

| TS type | Kotlin |
|---|---|
| `FoodSource` | enum `FoodSource { NIN, OFF, CUSTOM }` |
| `FoodMaster` | `data class Food(...)` — **ids stay identical** (`nin_roti_wheat`, `pkg_amul_butter`, `off_<barcode>`, `cust_<ts>`) |
| `DailyLog` | `DailyLogEntity` (Room, index on `date`, `meal_type`) |
| `WeightLog` / `WaterLog` / `CustomFood` | same-named entities |
| `UserSettings` | `SettingsDataStore` keys (see schema §5) |
| `CalculatorResult` | `data class CalculatorResult(...)` with `FormulaDetails` |

---

## 4. Feature parity map (web screen → Compose screen)

| Web piece | Native replacement | Notes |
|---|---|---|
| `TopBar` (date switcher, jump-to-today) | `CenterAlignedTopAppBar` + date row + `IconButton`s, `"Today"` chip | `LocalDate` + `DateTimeFormatter`; label "Today/Yesterday/Tomorrow" |
| `Navigation` (4 tabs) | `Scaffold` + `NavigationBar` + `NavHost`, 4 routes | Route args: today, search, progress, calculator |
| `HomeScreen` cards | `Card`/custom composables; `LinearProgressIndicator` for kcal/protein; meal groups in `LazyColumn` with headers | Sectioned list = `LazyColumn` + sticky headers |
| `WaterTracker` | `WaterCard`: `LinearProgressIndicator`, glass grid, quick-add `FilledTonalButton`s (+250/+500/+750), custom input, expandable logs | Same quick presets |
| `AddServingModal` | `ModalBottomSheet`: meal-type `FilterChips`, portion presets, quantity multiplier, live macro values | Keep grams×multiplier math identical |
| `LogWeightModal` | `ModalBottomSheet`/`AlertDialog` with unit toggle, kg↔lb conversion, 20–350 kg validation | Replaces same-date weight (upsert) + syncs `current_weight_kg` |
| `CustomFoodModal` | `ModalBottomSheet` or dialog route; macro inputs + auto kcal from 4/4/9 | Keep auto-calc |
| `FoodSearchScreen` | Search field + 3-tab `TabRow` (DB / Frequent / Custom) + source filter chips; debounced OFF query (450 ms) via `Flow`/`snapshotFlow` | Behavior identical; OFF cache stored in **Room** (not localStorage) |
| `ProgressScreen` | Stats banner + **custom Canvas charts** (see §9 alternative) + averages grid | Weight = line+area, intake = bars; hover → touch points |
| `CalculatorSettingsScreen` | Scrollable form; segmented controls; formula breakdown card; auto/manual target mode; export buttons (CSV/JSON via `ACTION_CREATE_DOCUMENT` or share `FileProvider`) | Port formulas verbatim (§8) |
| `About/Attribution` section | Same card, plus link to OFF ODbL + IFCT 2017 | Keep legal attribution |
| `confirm()` / `alert()` | Compose dialogs + Snackbar | Replace blocking JS dialogs |

---

## 5. Data layer design

### Room database (`inw.db`) — replaces `localStorage` keys

| Storage key (web) | Room table | Notes |
|---|---|---|
| `inw_daily_logs_v1` | `daily_logs` | `food_id`, `food_name`, `source`, `serving_grams`, `calories`, `protein`, `carbs`, `fat`, `meal_type`, `date` (indexed), `created_at` |
| `inw_weight_logs_v1` | `weight_logs` | unique index on `date` (upsert), `weight_kg`, `note` |
| `inw_water_logs_v1` | `water_logs` | `date`, `amount_ml`, `time`, `created_at` |
| `inw_custom_foods_v1` | `custom_foods` | full custom food row + `notes`, `created_at` |
| `inw_off_cache_v1` | `off_cache` | key = `barcode ?: normalized_name` (**primary key**), `product_name`, `brand`, macros, `last_fetched` |
| `inw_user_settings_v1` | **DataStore preferences** (not Room) | each field a key; `unit_system`, `default_chart_range`, `protein_basis`, etc. |

Fix the web cache bugs while porting (see §7): cache only genuine OFF network results, evict by `last_fetched` (TTL 30 days, cap ~500 rows), dedupe by canonical key.

### Static datasets

- `nin_ifct.json` (56 items) and `packaged_foods.json` (36 items) in `src/main/assets/data/`, parsed with `kotlinx.serialization` at startup into a `FoodRepository` in-memory list.
- **Generate, don't hand-copy:** write a one-off Node script (`tools/convert_foods.mjs`) that reads the current TS arrays and emits the JSON — guarantees the 92 rows and all ids/macros are byte-identical. Script runs once, is committed for auditability, then deleted or kept under `tools/` (no npm project needed).
- Keep `typical_serving_grams` as-is (note: milk/liquid rows still encode ml as grams — mark for a v2 field `serving_unit`, not part of parity).

---

## 6. Build configuration changes (exact)

### `android/settings.gradle`
```gradle
plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}
include ':app'
// delete: include ':capacitor-cordova-android-plugins' + projectDir + apply from capacitor.settings.gradle
```

### `android/app/build.gradle`
```gradle
// Remove: flatDir repository, implementation project(':capacitor-android'),
//         implementation project(':capacitor-cordova-android-plugins'),
//         apply from: 'capacitor.build.gradle', google-services try/catch block
// Keep:   compose plugin, buildFeatures { compose true }, Compose BOM deps

// Add plugins
id 'org.jetbrains.kotlin.plugin.serialization'
id 'com.google.devtools.ksp'                       // version 2.1.0-<ksp-version> (verify)

// Add dependencies
implementation "androidx.core:core-ktx:1.17.0"
implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.9.x"
implementation "androidx.lifecycle:lifecycle-runtime-compose:2.9.x"
implementation "androidx.navigation:navigation-compose:2.9.x"
implementation "androidx.room:room-runtime:2.7.x"
implementation "androidx.room:room-ktx:2.7.x"
ksp "androidx.room:room-compiler:2.7.x"
implementation "androidx.datastore:datastore-preferences:1.1.x"
implementation "com.squareup.retrofit2:retrofit:2.11.0"
implementation "com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0"
implementation "com.squareup.okhttp3:okhttp:4.12.0"
implementation "com.squareup.okhttp3:logging-interceptor:4.12.0"
implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.x"
implementation "io.coil-kt:coil-compose:2.7.0"     // OFF product images
implementation "androidx.compose.material:material-icons-extended"
coreLibraryDesugaring "com.android.tools:desugar_jdk_libs:2.1.x"   // java.time on API 24–25
```
Set `compileOptions`/`kotlinOptions` to Java 17 (or 21, per `capacitor.build.gradle` today), and enable `coreLibraryDesugaring = true`.

### `android/variables.gradle`
Keep: `minSdkVersion 24`, `compileSdkVersion 36`, `targetSdkVersion 36`, `kotlinVersion`, Compose BOM, androidx versions.
Remove: `composeBomVersion` template leftovers only if unused after cleanup — actually keep BOM version, remove cordova/capacitor vars.

### `gradle.properties`
Remove capacitor-era flags (`android.newDsl=false`, `android.builtInKotlin=false`, `android.sync.suppressAgpWarnings=FLAT_DIR_REPOSITORY_USED`, etc.) after verifying AGP 9.4 builds cleanly without them. Keep `android.useAndroidX=true`.

### Manifest (rewrite)
```xml
<application android:allowBackup="true" android:icon="@mipmap/ic_launcher"
             android:label="@string/app_name" android:supportsRtl="true"
             android:theme="@style/Theme.App">
    <activity android:name=".MainActivity" android:exported="true"
              android:launchMode="singleTask"
              android:configChanges="orientation|screenSize|keyboardHidden|screenLayout|uiMode|density|smallestScreenSize|locale">
        <intent-filter> MAIN / LAUNCHER </intent-filter>
    </activity>
    <provider ... FileProvider for export sharing (keep) />
</application>
<uses-permission android:name="android.permission.INTERNET"/>
```
Delete `ComposeActivity` entry. Add `values/colors.xml` (teal `#0D9488`, slate palette from web) and a proper `Theme.App` (`Theme.Material3.DayNight.NoActionBar` or splash-based) so the project compiles.

### Files to delete wholesale
`index.html`, `public/`, `src/`, `server.ts`, `vite.config.ts`, `tsconfig.json`, `package.json`, `package-lock.json`, `bun.lock`, `capacitor.config.ts`, `.env.example`, `metadata.json`, root `.idea/`, `android/capacitor.settings.gradle`, `android/app/capacitor.build.gradle`, `android/capacitor-cordova-android-plugins/`, `ComposeActivity.kt`, `res/layout/activity_main.xml`.

**Repo layout decision (§9):** either flatten `android/` → repo root (recommended — it's now *the* project; `git mv` preserves history) or keep it nested. Plan assumes flatten; reversible either way.

---

## 7. Bugs to fix while porting (do NOT reproduce)

1. **`getOffsetDate` missing export** → replaced by `DateUtils.offsetDate(days)` using `LocalDate`; trivially correct.
2. **Modal Rules-of-Hooks crashes** → no equivalent in Compose; state lives cleanly in ViewModels/sheet state.
3. **OFF parser crash** (`nutriments.proteins_100g` can be null/string → `.toFixed` throws) → DTO uses `JsonElement`/nullable `Double?` + `NumberUtils.coerce()`; skip products with no macros.
4. **Cache pollution** → see §5 (network-only caching, TTL, cap, canonical dedupe).
5. **Duplicate search rows** (`pkg_*` + `off_cache_*`) → dedupe by normalized name; prefer curated/source-priority order.
6. **"Today's Intake" header on past dates** → parametrize label by selected date.
7. **Charts averages ignoring unlogged days** → average over full range (0s included) or show "n tracked days" caption.
8. **`bmrFormula` shows `+5` for 'Other'** → render `-78` branch.
9. **JSON backup has no import** → add JSON **import** (see §10) so local web data can migrate.
10. **`user-scalable=no`, missing a11y** → Compose: proper `contentDescription`, focus management in dialogs, respect `reduced motion` (animate only if `LocalViewConfiguration` allows), no zoom blocking.

---

## 8. Logic ports (exact)

| Web module | Kotlin target | Parity notes |
|---|---|---|
| `utils/calculator.ts` | `domain/calculator/TargetCalculator.kt` | Mifflin-St Jeor: `10w + 6.25h − 5a + (M:+5 / F:−161 / Other:−78)`; activity factors 1.2/1.375/1.55/1.725/1.9; deficit = `(rate/0.5)*500`, surplus = `(rate/0.25)*250`; floors 1200 F / 1400 M; protein 1.2/1.1/1.5 g/kg defaults; water `w*35 + bonus` rounded to 250 ml, min 2000. Keep `FormulaDetails` strings for the breakdown card. |
| `storage.ts` (date utils, unit conversion, CSV/JSON export) | `util/DateUtils.kt`, `util/UnitConverters.kt`, `domain/export/*` | CSV format identical (FOOD/WATER/WEIGHT rows, quoted/escaped names); JSON backup keeps web schema field names (`@SerialName`) for import compatibility. |
| `offApi.ts` fallback chain | `OffApiClient` | server-proxy tier **dropped**; client chain = (1) `https://in.openfoodfacts.org/...` (2) `https://world.openfoodfacts.org/...` (3) cached rows (4) curated packaged list. Real `User-Agent` (`IndianNutritionTracker/1.0 (contact: <real email>)`), 4 s timeouts, paginate 25. |
| `packagedFoods.ts` / `nin_ifct_data.ts` | JSON assets + `FoodRepository` | values unchanged; `source: 'OFF'` items inside the NIN file stay as-is on first port (flag for later re-categorization). |

---

## 9. Decisions to confirm before coding

| Decision | Recommendation | Alternative |
|---|---|---|
| Repo layout | **Flatten `android/` to repo root** | keep `android/` (works, slightly awkward once web is gone) |
| DI | **Manual `AppContainer`** (this scale doesn't need Hilt) | Hilt (more boilerplate, better for growth) |
| Charts | **Custom `Canvas`** (zero deps, matches current SVG look, full touch control) | Vico (faster to build, opinionated look) |
| Networking | **Retrofit + OkHttp + kotlinx.serialization** | Ktor Client (fewer annotations, bigger dep) |
| Dates | `java.time` + **desugaring** (minSdk 24) | raise minSdk to 26 (drops Android 7.0/7.1 devices) |
| Room vs plain JSON files | **Room** for mutable logs (queryable, indexed, future-proof) | JSON files for everything (simpler, slower) |
| Settings storage | **DataStore Preferences** | Room table (overkill) |

---

## 10. Data migration (web → native)

No automatic migration is possible (web localStorage ≠ app storage). Instead:

1. **Phase 6 ships both** `JsonBackup.export()` *and* `JsonBackup.import()`.
2. Import accepts the **existing web backup format** (`exportDataJSON`): `settings`, `dailyLogs`, `weightLogs`, `waterLogs`, `customFoods`, `exportedAt`, `version` — mapped via `@SerialName` (snake_case → camelCase).
3. Publish a small "Export from web" instruction (web app still runs in browser) → user exports JSON → opens native app → Calculator → Data Management → **Import JSON**.
4. Import = replace-all or merge (confirm with dialog); validate rows before commit (date format, macro ranges, weight 20–350 kg).

---

## 11. Phased execution plan

### Phase 0 — Cleanup & repo restructure (≈0.5–1 dev-day)
- [ ] Delete web files (§6 list), remove Capacitor from Android config, flatten `android/` to root.
- [ ] Rewrite `settings.gradle`, `app/build.gradle`, `gradle.properties`, `variables.gradle` (§6).
- [ ] New manifest, delete `ComposeActivity.kt`, WebView layout; add `colors.xml`; fix `strings.xml`.
- [ ] `MainActivity.kt` → `ComponentActivity` + `setContent { NutritionApp() }` with a placeholder screen.
- [ ] New README (project reality: feature list, build instructions, attribution).
- ✅ **Gate:** `./gradlew :app:assembleDebug` succeeds on clean checkout.

### Phase 1 — Foundations: theme, nav, domain models, calculator, settings (≈2–3 days)
- [ ] `ui/theme/` (teal/slate palette, Material 3 typography, dark mode support).
- [ ] Domain models (all `data class`es) + enums; `DateUtils`, `UnitConverters`, `calculateBMI`.
- [ ] `TargetCalculator` + **parameterized unit tests** (reference values: e.g. M/82kg/176cm/28y/Moderate/Lose/0.5 → BMR 1785, TDEE 2767, target 2285, protein 98g; F/60kg/165cm/30y/Sedentary/Maintain → BMR 1320, TDEE 1584).
- [ ] `SettingsDataStore` + `SettingsRepository`; default settings identical to web (82 kg / 74 kg / 176 cm / 28 / M / Moderate / Lose / −0.5 / 1950 kcal / 115 g / 2750 ml).
- [ ] `AppContainer` + `NutritionApp` NavHost with 4 placeholder destinations + `NavigationBar`.
- ✅ **Gate:** unit tests green; settings survive process death.

### Phase 2 — Food data + Room persistence (≈2 days)
- [ ] `tools/convert_foods.mjs` → `assets/data/*.json` (92 rows, byte-identical values).
- [ ] Room: entities, DAOs, `AppDatabase`; `LogRepository`, `WeightRepository`, `WaterRepository`, `FoodRepository` (master list = custom + NIN + packaged + cache).
- [ ] `getFrequentlyUsedFoods` port (frequency by `food_id`, fallback to NIN staples, limit 12).
- [ ] DAO repository unit/instrumented tests (in-memory Room).
- ✅ **Gate:** repository tests green; logs survive app restart.

### Phase 3 — Home/Today + logging flows (≈3–4 days)
- [ ] HomeScreen: intake cards + progress bars, weight/BMI summary, WaterCard, meal-grouped `LazyColumn`.
- [ ] AddServing -> `ModalBottomSheet` (meal chips, presets, quantity, live macros) → `LogRepository.add()`.
- [ ] Weight sheet (upsert per date, 20–350 validation, syncs current weight) + delete.
- [ ] Water quick-add (+250/+500/+750/custom) + per-entry delete + expandable history.
- [ ] Date navigation in `TopAppBar` (prev/next/today; label Yesterday/Tomorrow).
- ✅ **Gate:** manual QA matrix rows 1–8 pass (see §13); logging persists.

### Phase 4 — Search & Open Food Facts (≈2–3 days)
- [x] Search screen: 3 tabs, source filter chips, debounced local filtering; `"Frequent"` and `"Custom"` tabs, custom-food CRUD dialog.
- [x] `OffApiClient` (Retrofit, in→world fallback, 4 s timeouts, real UA `torwer2021@gmail.com`), `OffCacheRepository` with TTL/cap/dedupe fixes, Coil image loading.
- [x] Offline behavior: no network → cached + curated results, non-blocking "offline" snackbar.
- ✅ **Gate:** search works with network disabled; no duplicate rows; cache capped. (Unit tests cover fallback/cache/dedupe; on-device manual check deferred to QA.)

### Phase 5 — Progress & charts (≈2–3 days)
- [x] Stats banner (current/net-change/target, BMI category).
- [x] Custom Canvas charts: weight line+area with forward-fill; bars for calories/protein/water; target dashed line; touch points (not just hover); x-axis thinning for 30/60-day ranges.
- [x] Averages → full-range denominator; `DateRange` (`7d/14d/30d/All=60d`) persists via settings.
- [x] Weight history list with delete.
- ✅ **Gate:** charts render on a phone-sized emulator; touch points work. (Chart math unit-tested; on-device emulator check deferred to manual QA.)

### Phase 6 — Calculator/Settings + export/import (≈2 days)
- [ ] Full calculator form (kg/lb toggle, activity select, goal chips, rate select, protein-basis radios, auto/manual target mode, formula breakdown).
- [ ] CSV export (`ACTION_CREATE_DOCUMENT`) and JSON backup export/import (§10).
- [ ] "Clear all logs" with confirmation dialog; Data Management card; About/attribution card.
- ✅ **Gate:** export CSV produces byte-identical schema to web; import round-trips.

### Phase 7 — Hardening & tests (≈2–3 days)
- [ ] Move tests to `com.indian.nutrition.tracker`; unit (`TargetCalculator`, `CsvExporter`, `JsonBackup`, `NumberUtils`, `DateUtils`), instrumented (DAOs, repositories), Compose UI tests with `testTag`s mirroring the web `id`s (e.g. `food-search-input`, `save-and-use-targets-btn`).
- [ ] A11y pass (content descriptions, dialog focus, contrast), dark theme, dynamic type spot-check.
- [ ] Edge cases: future-date logging, DST/date rollover, lb round-trip, huge serving amounts, corrupt JSON import (reject, don't crash).
- ✅ **Gate:** `./gradlew test` + `connectedAndroidTest` green; ktlint/detekt clean if adopted.

### Phase 8 — Release readiness (≈1 day)
- [ ] App icon set (replace Capacitor splash PNGs), versioning (`versionCode/versionName`), ProGuard rules for Retofit/serialization, `minifyEnabled` release smoke test.
- [ ] CI via GitHub Actions (`gradle/actions/setup-gradle`): assemble + unit tests on PR.
- [ ] Optional Play Store checklist: privacy policy text ("all data on-device"), data-safety form, signed bundle.

**Total estimate: ~15–20 dev-days** for one engineer (single PR per phase is ideal for review).

---

## 12. Testing strategy

| Layer | Tool | Coverage |
|---|---|---|
| Domain | JUnit5 + parameterized | calculator math (all sexes/goals/rates), BMI categories, unit conversions, CSV escaping |
| Data | Room (in-memory) + `kotlinx-coroutines-test` | DAO CRUD, date upsert, frequently-used ranking, cache eviction/TTL |
| Network | OkHttp `MockWebServer` | OFF fallback chain (in→world→cache→curated), parser null/string handling |
| UI | Compose UI test + `createAndroidComposeRule` | every screen renders, sheet flow (select food → add serving → appears on home), delete flows, import dialog validation |
| E2E manual | QA matrix (below) | before each phase merge |

## 13. Manual QA matrix (sample)
1. Fresh install → defaults applied (82 kg/1950 kcal), Home renders.
2. Log dal + roti + paneer (NIN), verify macros vs web math.
3. Search "amul" offline → curated Amul items appear, no crash.
4. Search "protein" online → OFF rows + images, cache grows only from network.
5. Log weight twice on same date → updates, not duplicates; current weight syncs.
6. View past date → headers say that date; "Today" jump works.
7. Switch kg↔lb in calculator → weights convert, targets recompute.
8. Export CSV/JSON → import into a second install → data matches.
9. Chart touch on mobile → tooltip; 60-day range smooth.
10. Delete every log type → no crash; clear-all keeps settings.

---

## 14. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Compose/AGP/Future-version mismatch (AGP 9.4 + Gradle 9.7.1 are bleeding-edge) | Phase 0 gate: build clean checkout **first**; pin versions in `libs.versions.toml`; record exact working versions in README |
| Food data migration typos (92 rows) | Generate JSON from TS programmatically; spot-check 10 rows against web UI; ids unchanged so old backups stay compatible |
| Charts fidelity/time | Custom Canvas is bounded (4 metrics × 1 chart type each); if it balloons, swap to Vico behind the same composable API |
| Behavior drift on dates/timezone | Central `DateUtils` (ISO `LocalDate` only, no `Date`/`Calendar`); all async work on `Dispatchers.IO` |
| Data loss during switch | JSON import in Phase 6 (never trap user data); keep web app runnable until Phase 6 ships |
| Scope creep (adding AI/sync/scan) | Explicit out-of-scope list; separate epics after parity |
| Template leftovers (missing `colors.xml`, package-name mismatch in tests) | Phase 0 hard gate: `assembleDebug` + `test` must pass before any feature work |

---

## 15. PR sequence
1. `chore: remove web app + capacitor, native Compose skeleton` (Phase 0)
2. `feat: theme, nav, domain models, calculator + tests` (Phase 1)
3. `feat: food datasets + Room persistence` (Phase 2)
4. `feat: home dashboard, logging sheets, water/weight tracking` (Phase 3)
5. `feat: food search + Open Food Facts with offline cache` (Phase 4)
6. `feat: progress charts` (Phase 5)
7. `feat: calculator/settings, CSV/JSON export + import` (Phase 6)
8. `test: unit/instrumented/UI suite + hardening` (Phase 7)
9. `chore: release readiness` (Phase 8)
