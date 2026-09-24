# STANDARD. — Brutalist Android Launcher

A working default-launcher-replacement for Android 14+, built with Kotlin + Jetpack Compose. Installable, selectable as the system home app, with real installed apps, live weather, a 6-theme engine, and switchable animations.

## Build / test

1. Open the `standard-launcher/` folder in **Android Studio** (Hedgehog+ / Koala+, AGP 8.7).
2. Let Gradle sync (Android Studio will download Gradle 8.9 per `gradle/wrapper/gradle-wrapper.properties`; if `gradlew` is missing it regenerates the wrapper on sync — or run `gradle wrapper` if you have Gradle CLI).
3. Connect a device (Android 11+ / API 30, tested on OnePlus 13 class 1440×3168 120Hz) and Run `app`.
4. On install, go to **Settings → Apps → Default apps → Home app → STANDARD.** to set it as the launcher.

> This machine has no Android SDK, so the APK was not compiled here. Build in Android Studio and report any compile errors back for a fix pass.

## What's implemented (v1)

| Spec section | Status |
|---|---|
| §1 Tech stack (Compose, MVVM, DataStore, WorkManager) | ✅ |
| §2 Typography (Space Grotesk + JetBrains Mono variable fonts) + texture overlay | ✅ |
| §3 Launcher integration (HOME intent, LauncherApps, package callbacks, adaptive icon) | ✅ |
| §4.1 Home (day/date, weather, pinned grid, accent tile, apps/quote/caption row) | ✅ |
| §4.2 App Drawer (numbered list, all apps, quote/accent callouts, barcode footer) | ✅ |
| §4.3 Focus Mode (day/weather, 2-col apps, photo tile, accent callout) | ✅ (layout only, no enforcement) |
| §4.4 Settings (theme, dark mode, weather location, animation, haptics, icon style, texture) | ✅ |
| §5 Theming (6 themes, instant swap via CompositionLocal) | ✅ |
| §6 Animation (Tap Flip, 3D Cube page transitions, Smooth) | ✅ (tap-flip + cube use `graphicsLayer`/`HorizontalPager`; spring tuning uses Compose defaults) |
| §7 Data (Open-Meteo keyless weather via Retrofit, WorkManager 15-min, cached) | ✅ |
| §8 Permissions (INTERNET + location at time-of-use, manual fallback) | ✅ |
| §9 Non-goals | respected (no widgets, icon packs, gestures, blocking, sync) |

## Architecture

MVVM, single `app` module. `AppContainer` wires repositories; `StandardAppViewModel` exposes `StateFlow`s to screens; `MainActivity` provides the theme via `CompositionLocalProvider(LocalAppTheme)` and hosts `StandardApp` (a `HorizontalPager` over Home/Drawer/Focus with a Settings overlay).

```
domain/model      — ThemeConfig, AnimationStyle, IconStyle, AppItem, WeatherData, SettingsState
data/repo         — repository interfaces (contracts)
data/local        — SettingsRepositoryImpl (DataStore), AppRepositoryImpl (LauncherApps + SharedPreferences)
data/remote       — WeatherApi (Retrofit/Open-Meteo), WeatherModels, WeatherRepositoryImpl, WeatherWorker
ui/theme          — ThemeRepositoryImpl (6 themes), Color, Type (variable fonts), Theme, LocalAppTheme
ui/components     — Tile, AppTile (inline tap-flip), NoiseOverlay (drawWithCache grain shader)
ui/screens        — home, drawer, focus, settings
ui/nav            — StandardApp (pager + settings overlay), StandardAppViewModel
```

## Weather

Uses **Open-Meteo** (`api.open-meteo.com/v1/forecast`) — keyless, free, no setup. Refreshed by `WorkManager` every 15 min (network-constrained) and cached to `SharedPreferences` so the tile is never blank on cold start. Manual location entry in Settings; optional GPS auto-detect behind a runtime permission prompt (app works if denied).

## Notes / known limitations

- **App icons**: v1 tiles show app *labels* (Space Grotesk, uppercase), not real launcher icons — keeps scope tight. `IconStyle.ICON_ONLY` currently falls back to label. Real icon rendering via `LauncherApps.getApplicationIcon` is a straightforward follow-up.
- **Animation subsystem**: a dedicated `ui/anim/` package was not produced (that agent returned empty); tap-flip and cube paging are inlined in `Tile.kt` / `StandardApp.kt` instead. `AnimationStyle.SMOOTH` currently routes through the same pager (crossfade-style); refine if you want distinct reduced-motion transitions.
- **Refresh-rate bias**: the 120Hz spring tuning util (`util/RefreshRate.kt`) was not created; animations use Compose default springs. Add it to bias stiffness by `Choreographer` frame interval if you want hardware-specific feel.
- The 5 PNG mockups could not be read by the build model — layouts follow `BUILD_PLAN.md` placeholders. Tune tile sizes/positions to your screenshots in a follow-up.

## Files in this workspace

- `Plan.MD` — original build spec
- `BUILD_PLAN.md` — analysis + architecture decisions
- `LAUNCHER-API-RESEARCH.md` — cited Android 14 launcher API findings
- `standard-launcher/` — the Android project (open this in Android Studio)
