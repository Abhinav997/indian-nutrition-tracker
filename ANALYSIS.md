# Indian Nutrition & Weight Tracker — Project Analysis

**Analyzed:** 2026-09-04 · **Branch:** `arena/01a06d1c-indian-nutrition-tracker` (single commit `2d4a7e3`)

---

## 1. What the project is

A personal calorie / protein / water / weight tracker **optimized for Indian foods**, built as a single-page React app with an Express server, plus a Capacitor Android wrapper. It is derived from the Google AI Studio "react-example" template (template README, package name `react-example`, and AI Studio metadata are still present).

**Core features**
- **Food logging** from three sources:
  - 56 curated Indian foods (54 flagged `NIN` + 2 branded entries) in `src/data/nin_ifct_data.ts`
  - 36 curated Indian packaged-brand products in `src/data/packagedFoods.ts`
  - Live **Open Food Facts** search via an Express proxy (`/api/off-search`) → client-side fallback fetch → local curated/cache fallback
- **Target calculator**: Mifflin-St Jeor BMR → TDEE → calorie/protein/water targets with transparent formula strings and custom overrides
- **Daily dashboard**: calories & protein vs targets, macro breakdown, meals grouped by Breakfast/Lunch/Snack/Dinner, weight summary + BMI, water tracker with quick-add presets
- **Progress screen**: hand-rolled SVG charts (weight line, calorie/protein/water bars), date ranges, averages
- **Persistence**: 100% local `localStorage` (settings, food logs, weight logs, water logs, custom foods, OFF cache) with JSON/CSV export
- **Mobile**: mobile-first Tailwind UI + Capacitor Android shell (`com.indian.nutrition.tracker`)

**Hot path / state flow**

```
App.tsx (single state owner, ~screens rendered conditionally)
 ├─ TopBar (date switcher) / Navigation (4 tabs)
 ├─ HomeScreen ── WaterTracker
 ├─ FoodSearchScreen ── offApi.searchOpenFoodFacts() ── Express /api/off-search
 ├─ ProgressScreen ── Charts (SVG)
 ├─ CalculatorSettingsScreen ── calculator.ts
 └─ AddServingModal / LogWeightModal / CustomFoodModal
        └─ LocalStorageDatabase (services/storage.ts) — the only data layer
```

**Scale:** ~6,000 lines of TS/TSX/Java/Kotlin across 20 files under `src/`, ~250 KB of source, plus a 446 KB Android shell.

---

## 2. Stack

| Layer | Choice | Notes |
|---|---|---|
| UI | React 19 + TypeScript ~5.8, Vite 6 | JSX, no router (tab state in App) |
| Styling | Tailwind CSS 4 (`@tailwindcss/vite`), lucide-react icons | `index.css` is just `@import "tailwindcss";` |
| Server | Express 4 + `tsx` dev runner, Vite middleware in dev | `npm run dev` = `tsx server.ts`; prod build bundles `server.cjs` |
| Data | `localStorage` (versioned keys `*_v1`), no backend DB | single-instance, device-bound |
| External API | Open Food Facts (in + world endpoints) | proxied server-side, then direct, then offline cache |
| Mobile | Capacitor 8 (`@capacitor/android`), Android SDK 36, minSdk 24 | includes leftover Jetpack Compose sample |
| Lockfiles | `package-lock.json` (186 KB) **and** `bun.lock` (**0 bytes — empty**) | two lockfiles, one empty |

---

## 3. Verified build/test status (I ran these)

| Command | Result |
|---|---|
| `npm ci` | ✅ 303 packages, ~6 s |
| `npm run lint` (`tsc --noEmit`) | ❌ **1 error — build-blocking** |
| `npm run build` (`vite build && esbuild server.ts`) | ❌ fails on the same error |
| `npm run dev` + `curl /api/health` | ✅ server starts, health OK |
| `/api/off-search?q=amul butter` | ⚠️ returns `{"products":[],"success":false}` — egress to OFF is unavailable from this sandbox; the app's fallback chain handles it |
| `curl` page through the preview host | ❌ **HTTP 403** — Vite middleware rejects non-local hosts |
| `npm audit --omit=dev` | ⚠️ 6 moderate (via `@capacitor/cli` → `xcode` → `uuid`) — dev/build toolchain, not app runtime |

**The build-blocking error**

```
src/components/TopBar.tsx(3,25): error TS2305:
Module '"../services/storage"' has no exported member 'getOffsetDate'.
```

`TopBar.tsx` imports `getOffsetDate` (used for "Yesterday"/"Tomorrow" labels), but `storage.ts` only exports `formatDateKey`. I applied the obvious one-line fix locally to verify the rest of the codebase:

```ts
// src/services/storage.ts
export function getOffsetDate(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return formatDateKey(d);
}
```

With that one function added, `tsc --noEmit` is **clean** and `vite build` + `esbuild server.ts` both **succeed** (336 KB JS / 91 KB gzip, 45 KB CSS). So: the project is one tiny missing export away from compiling, and no other TS errors exist.

---

## 4. Issues & risks (prioritized)

### 🔴 Critical — will break the app

1. **Missing `getOffsetDate` export (build broken as committed).** `src/components/TopBar.tsx:3`. Nothing builds or runs until added (see fix above).
2. **Rules-of-Hooks violations in all three modals — crash on open/close.** Each modal returns early *before* calling hooks:
   - `AddServingModal.tsx:17` — `if (!isOpen || !food) return null;` then `useState`/`useEffect`
   - `LogWeightModal.tsx:16` — `if (!isOpen) return null;` then `useState`
   - `CustomFoodModal.tsx:14` — `if (!isOpen) return null;` then `useState`/`useEffect`

   These components are **always mounted** in `App.tsx`, so the hook count changes when the modal toggles → React throws *"Rendered fewer hooks than expected"* and the tree crashes after the first modal interaction. (This is latent today only because the build error above prevents the app from running at all.) Fix: move the early return after all hooks (guarding effects with `isOpen`), or render `null` from the parent and mount the modal only when open.

### 🟠 High

3. **Dev server rejects the preview host (403).** The Vite middleware created in `server.ts` inherits `vite.config.ts` `server` options, whose `allowedHosts` defaults to localhost. Verified: page request with the preview host header → `403 Blocked request`, while `/api/health` still returns 200. In this sandbox the live preview will not load until `server.allowedHosts` is relaxed (e.g. `allowedHosts: true` in `vite.config.ts` or the platform hostname). Same concern applies to any deployed custom domain.
4. **OFF cache design can corrupt search quality and blow the 5 MB localStorage quota.**
   - `FoodSearchScreen.tsx` caches **every** returned product on each search, including the curated `INDIAN_PACKAGED_FOODS` matches returned as fallbacks → the same product exists as both `pkg_*` and `off_cache_*` in the unified master → **duplicate rows** in search results (dedupe is by `food.id`, not name).
   - `cacheOffProduct` keys by `barcode || product_name.toLowerCase()`; products without a barcode overwrite each other; there is **no TTL or eviction**, so repeated searches accumulate unbounded entries.
5. **OFF parsing is fragile.** `parseOffProduct` calls `.toFixed(1)` on `nutriments.proteins_100g ?? …` — OFF returns `null`/missing/string values in the wild, and `.toFixed` on a string throws a `TypeError`. It is swallowed by the surrounding try/catch (results silently skipped, falls through to next endpoint), so impact is degraded results rather than a crash — but the parser should coerce with `Number(...) ?? 0`.

### 🟡 Medium

6. **Data provenance is mixed and unverifiable.**
   - `nin_ifct_data.ts` is branded "NIN/IFCT dataset" but is a hand-curated 56-item subset with **no per-item source refs**; 2 entries inside it are actually branded `source: 'OFF'` (e.g. Amul lassi, MyPB peanut butter).
   - `packagedFoods.ts` comment claims "verified nutrition" but values appear approximated (e.g. Amul Gold milk 87 kcal/100g vs ~93–100 kcal typical) and 24/36 items have barcodes, 0 have image URLs.
   - Serving units mix **ml and grams**: milk "1 glass (~200ml)" is stored as `typical_serving_grams: 200` and multiplied as grams — small but systematic error for liquids.
   - The About section does cite IFCT 2017 (NIN/ICMR) and OFF/ODbL — good, but the app has no link to the ODbL license page or the IFCT document.
7. **No tests at all.** No test script, no test files for the app logic (only Android template placeholders in the wrong package `com.getcapacitor.myapp`). The two critical bugs above are exactly what unit/smoke tests would catch. Prime candidates: `calculator.ts`, `storage.ts`, `offApi.ts`, and modal mount/unmount.
8. **Dead weight & template leftovers.**
   - `@google/genai` dependency + `GEMINI_API_KEY` in `.env.example` + `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` in `metadata.json` — but **no code uses Gemini anywhere**.
   - `motion` dependency — **unused** (0 imports).
   - `package.json` name `react-example`, version `0.0.0`; `README.md` is the untouched AI Studio template ("Run and deploy your AI Studio app").
   - Empty `bun.lock` (0 bytes) alongside `package-lock.json` — confusing, delete the empty one.
   - Unused imports (`Sparkles` in `TopBar`/`WaterTracker`, etc.) — `noUnusedLocals` is not enabled, so tsc won't catch them.
9. **Android shell has leftover sample code.** `ComposeActivity.kt` is a stock Jetpack Compose "Hello $name!" demo, registered in the manifest with `android:exported="true"` — dead, externally launchable, and adds no value. Unit/instrumented tests still live in the template package `com.getcapacitor.myapp` (renamed app but not the tests). Gradle `variables.gradle` also carries `composeBomVersion` etc. from the template.
10. **PWA/asset hygiene.** `manifest.json` references `/favicon.ico` which **does not exist** (only `manifest.json` is in `public/`); no 192/512 px icons despite `"display": "standalone"`, and the icon entry uses a non-standard multi-size `sizes` string. `index.html` loads Plus Jakarta Sans/JetBrains Mono fonts that are never applied (`index.css` sets no font-family), and `animate-fade-in` used by modals is not a Tailwind utility (no-op).

### 🟢 Low / polish

11. **`HomeScreen` copy is date-inconsistent**: the header always says "Today's Intake" even when viewing a past/future date via the TopBar switcher (the meal section correctly says `Meals Logged for {selectedDate}`). `WaterTracker`'s empty state likewise hard-codes "today".
12. **Charts are hover-only** (`onMouseEnter`) — "Touch or hover over data points" is misleading on mobile; add `onClick`/`onTouchStart`.
13. **Charts averages understate intake**: `avgCalories`/`avgProtein`/`avgWater` average only over days with data, excluding unlogged days — flatters the numbers.
14. **`calculator.ts` formula display bug**: `bmrFormula` shows `+ 5` for anyone not `F`, including `Other`, although the computation applies `-78` for `Other`.
15. **Server hardening**: hard-coded `PORT = 3000` (should be `process.env.PORT || 3000`); unbounded in-memory `serverCache` (no max size); no rate limiting on an unauthenticated internet-fetch proxy; `User-Agent` uses a fake contact (`support@nutritionapp.local`) — OFF requests a real contact; server swallows fetch errors silently (only `console.warn`).
16. **Data management gaps**: JSON "Backup" has no **import** (data is trapped on-device and `clearAllLogsToZero` wipes it permanently); deleting a weight log doesn't roll `current_weight_kg` back; OFF cache survives "clear all logs"; CSV export writes header/comment rows to separate lines without a proper shared header row.
17. **Accessibility**: `user-scalable=no` disables pinch-zoom; several icon-only buttons rely on `title` rather than `aria-label`; modal dialogs lack focus trapping / `role="dialog"` / Escape handling; no `prefers-reduced-motion` handling.
18. **`tsconfig.json` has no `strict: true`** (defaults off) — the `any` types peppered through `App.tsx`/`storage.ts` (e.g. `source: any`) pass silently. Enabling strict mode would surface these.
19. `FoodMaster.source` typing partly eroded: `parseOffProduct` returns `'OFF' as const` (fine), but curried food lists are untyped arrays; a `satisfies FoodMaster[]` per dataset would help.

---

## 5. What's good (genuine strengths)

- **Offline-first, privacy-first design.** All user data stays in `localStorage`; the About section states this. No accounts, no telemetry.
- **Resilient 3-tier search** (server proxy → direct fetch → curated/cache) with client timeouts, so the app still works without internet (verified: OFF is unreachable from this sandbox and the app degrades gracefully).
- **Transparent nutrition math.** The calculator renders every step of BMR/TDEE/protein/water in a formula breakdown — rare and valuable for trust; sensible floors (1200/1400 kcal) and 250 ml water rounding.
- **Good UX foundation.** Mobile-first single-column layout with sticky top bar + bottom nav, per-meal grouping, quick-add water presets, lb/kg toggle, custom-foods management, frequently-used list, CSV/JSON export, date navigation for backfilling logs.
- **Clean data model** (`types.ts`): well-named interfaces, date keys in `YYYY-MM-DD`, versioned storage keys, and a single `LocalStorageDatabase` facade that keeps `App.tsx` readable.
- **Testable hooks in the UI**: pervasive `id=` attributes on interactive elements (e.g. `food-search-input`, `save-and-use-targets-btn`) — clearly set up for E2E automation.
- The hand-rolled SVG charts (no chart library) are lightweight and dependency-free.

---

## 6. Recommended next steps (in order)

1. **Fix the missing `getOffsetDate` export** (one function, ~5 lines) — unblocks everything.
2. **Fix the three modal Rules-of-Hooks violations** — the app crashes on first modal open/close otherwise.
3. **Add `allowedHosts`** to `vite.config.ts` so dev/preview works behind the AI Studio/arena proxy.
4. **Add a test setup** (Vitest + React Testing Library) with smoke tests for: build-critical exports, modal open/close, calculator math, storage round-trips, OFF parser coercion.
5. **Tame the OFF cache**: only cache network results (skip curated fallbacks), dedupe unified master by `id`/`name`, cap cache size/age, coerce nutriments with `Number() ?? 0`.
6. **Trim dead weight**: remove `@google/genai`, `motion`, empty `bun.lock`, the Compose sample activity, template test files; rename package to something real; replace README with app docs.
7. **Fix data provenance**: add per-item source references/URLs, separate branded items out of the "NIN" file, treat liquid servings as ml, add ODbL link.
8. **Enable strict TS** + `noUnusedLocals`, and add import (restore) for backups.

---

## 7. Verdict

A **well-structured, thoughtful fitness-tracking app with a genuinely strong idea** (Indian-food-first + transparent formulas + offline resilience), wrapped in template scaffolding that was never finished: the build is currently broken by one missing export, and the modal hook pattern would crash at runtime once that fixed. The data layer and UX are solid enough that the remaining work is focused: 2 critical fixes, then hardening, tests, and data provenance cleanup.
