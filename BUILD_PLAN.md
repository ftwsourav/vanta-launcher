# BUILD_PLAN.md — STANDARD. Brutalist Launcher

## 0. Verified facts from analysis

- Workspace is a clean slate: only `Plan.MD` + 5 PNG mockups (unreadable by the model — user must supply visual specifics).
- **Android 14 specifics (cited):** HOME activity = `MAIN`+`HOME`+`DEFAULT`, `launchMode=singleTask`, `exported=true`, separate `LAUNCHER` activity for drawer presence. `HOME` category alone is enough — no `BIND_HOME`. List apps via `LauncherApps.getActivityList()`; declare `<queries> ACTION_MAIN` for visibility without the sensitive `QUERY_ALL_PACKAGES`. Launch via `getLaunchIntentForPackage()`+`FLAG_ACTIVITY_NEW_TASK`, catching `ActivityNotFoundException`. Package add/remove via `LauncherApps.registerCallback` (runtime `RECEIVER_EXPORTED` receivers are the fallback). Wallpaper = `peekBitmap()` snapshot under a grain overlay (`windowShowWallpaper` option). Back = no-op on the singleTask root. Adaptive icon = `mipmap-anydpi-v26` 108x108, 66dp safe zone. Build config `minSdk=30 / target=34 / compile=34`.
- **Compose:** tap-flip via `graphicsLayer { rotationY; cameraDistance=12*density }` + `Animatable<Float>`, content swap at 90°, `spring(dampingRatio≈0.6, high stiffness)`. Cube transition via `HorizontalPager` + per-page `rotationY` (jetpack-only; Accompanist pager is archived → avoid). Smooth = `Crossfade`/`AnimatedContent`. Refresh rate via `Choreographer` → bias spring stiffness at 120Hz.
- **Weather:** OpenWeatherMap v2.5 by lat/lon free tier (60/min, 1000/day) — WorkManager 15–30min is safe. Retrofit + kotlinx.serialization recommended; Room 2.6+ for app cache; plain Preferences DataStore for settings; PeriodicWorkRequest min 15min constrained.

## 1. Architecture (MVVM, single module)

```
app/
├─ com.xdlab.standard/
│  ├─ MainActivity.kt            (HOME + LAUNCHER routes)
│  ├─ LauncherActivity.kt        (LAUNCHER entry, launches MainActivity)
│  ├─ di/AppContainer.kt
│  ├─ data/
│  │  ├─ local/DataStore.kt      (Preferences DataStore)
│  │  ├─ local/AppDatabase.kt    (Room)
│  │  ├─ local/WeatherCache.kt
│  │  ├─ repo/AppRepository.kt   (LauncherApps)
│  │  ├─ repo/WeatherRepository.kt (Retrofit)
│  │  ├─ remote/WeatherApi.kt    (interface)
│  │  ├─ remote/WeatherWorker.kt (WorkManager)
│  │  └─ remote/WeatherModels.kt
│  ├─ domain/
│  │  ├─ model/ThemeConfig.kt
│  │  ├─ model/AnimationStyle.kt
│  │  ├─ model/IconStyle.kt
│  │  └─ model/WeatherData.kt
│  ├─ ui/
│  │  ├─ theme/Theme.kt / Color.kt / Type.kt / LocalAppTheme.kt
│  │  ├─ components/NoiseOverlay.kt, Tile.kt, PinnedGrid.kt
│  │  ├─ screens/
│  │  │  ├─ home/HomeScreen.kt / HomeViewModel.kt
│  │  │  ├─ drawer/DrawerScreen.kt / DrawerViewModel.kt
│  │  │  ├─ focus/FocusScreen.kt / FocusViewModel.kt
│  │  │  └─ settings/SettingsScreen.kt / SettingsViewModel.kt
│  │  └─ nav/AppNavHost.kt       (HorizontalPager cube transitions)
│  └─ util/RefreshRate.kt
├─ res/
│  ├─ values/strings.xml, themes.xml, colors.xml
│  ├─ font/ (space_grotesk_*.ttf, jetbrains_mono_*.ttf)
│  ├─ drawable/ic_launcher_*.xml (adaptive icon)
│  └─ mipmap-anydpi-v26/ic_launcher.xml
└─ build.gradle.kts, settings.gradle.kts, gradle.properties
```

## 2. Dependencies

- Jetpack Compose BOM 2024.xx, Kotlin 2.0.x
- `androidx.activity:activity-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`, `androidx.lifecycle:lifecycle-runtime-compose`
- `androidx.navigation:navigation-compose` (or HorizontalPager cube), `androidx.core:core-ktx`
- `androidx.datastore:datastore-preferences`
- `androidx.room:room-runtime/ktx/compiler` 2.6+
- `androidx.work:work-runtime-ktx`
- `com.squareup.retrofit2:retrofit` 2.11 + `converter-kotlinx-serialization`; `org.jetbrains.kotlinx:kotlinx-serialization-json`
- `io.coil-kt:coil-compose` 2.6+ (icons, focus-mode photo)
- Fonts: bundle OFL static TTFs from Google Fonts CDN into `res/font`
- **No** Accompanist (archived), **no** QUERY_ALL_PACKAGES.

## 3. Theming engine

`ThemeConfig(id, background, tileFill, accent, textColor, useTexture)` — 6 themes, dark variants, instant swap via `CompositionLocalProvider`.

| Theme | Light background | tileFill | accent | onSurface | Dark background | dark tileFill | dark onSurface |
|-------|------------------|----------|--------|-----------|-----------------|---------------|----------------|
| Mono (default) | #F4F1EA (paper) | #111111 | #D0342C | #111111 | #1A1A1A | #F4F1EA | #F4F1EA |
| Blue | #EDF1F7 | #1E4D8C | #0F6FB8 | #12334F | #0D1420 | #1E4D8C | #D9E6F5 |
| Red | #F7EDEC | #B3241F | #7A1410 | #3A0E0C | #1A0E0D | #B3241F | #F7D9D5 |
| Green | #EDF6EE | #1E7A3A | #0F9D4A | #12331E | #0E1A11 | #1E7A3A | #D7F0DE |
| Purple | #F2EDF7 | #4A1E8C | #7A3AF0 | #24123F | #130E1A | #4A1E8C | #E4D6F7 |
| Orange | #F7F0E9 | #B3541E | #F07A0F | #3A2A12 | #1A130B | #B3541E | #F7E2CE |

Default theme = Mono. Settings re-keyed to selected `ThemeConfig`; dark toggle composes `darkColorScheme`/`lightColorScheme`.

## 4. Animation system

- `AnimationStyle { TAP_FLIP, CUBE, SMOOTH }` — runtime-switchable.
- TAP_FLIP: tile tap → `Animatable(rotationY 0→180)`, swap content at 90°, `cameraDistance=12*density`, `spring(dampingRatio=0.6, stiffness=600f)`.
- CUBE: `HorizontalPager` (`userScrollEnabled` + `animateScrollToPage`) applying `rotationY`/`scaleX` per page — Metro cube pages.
- SMOOTH: `Crossfade`/`AnimatedContent`, `prefersReducedMotion` respected.
- `util/RefreshRate.kt`: read `Choreographer` frame interval → compute `120f.coerceAtMost(rate)` bias; on API 30+ optionally `Surface.setFrameRate`.

## 5. Data layer

- Weather: Retrofit `GET api.openweathermap.org/data/2.5/weather?lat=&lon=&appid=`, fields consumed: temp, condition, high/low, humidity, wind, last-updated. Cache in DataStore; `PeriodicWorkRequest(15min, constrained network)`; cold start reads cache first — never blank.
- Apps: `LauncherApps.getActivityList` → Room table (`packageName PK, label, lastUpdated, sortOrder`), invalidated by `LauncherApps.registerCallback`.
- Focus-mode photo: `coil` load from `filesDir`; pick via `ActivityResultContracts.GetContent`.
- Settings (Preferences DataStore): themeId, animationStyle, iconStyle, haptics, darkMode, useTexture, weatherLocation(lat/lon/name).

## 6. Permissions (v1)

`INTERNET` only (weather). `ACCESS_FINE/COARSE_LOCATION` requested at time-of-use only for GPS auto-detect; manual entry fallback → app works offline. No `QUERY_ALL_PACKAGES`, no notification listener.

## 7. Build order (per Plan.MD §10)

1. Scaffold + HOME filter + `LAUNCHER` activity + basic real pinned grid (no theming/animation) → verify default-launcher selection + real app launch.
2. Theming engine (6 themes) + Settings screen (DataStore-backed).
3. Animation system (3 styles) wired to pages + tile taps.
4. Live weather + time data layer (WorkManager + Retrofit + cache).
5. Focus Mode layout + Drawer polish.
6. IconStyle modes, haptics, dark toggle, texture-overlay toggle.

## 8. Verification

Cannot compile on this machine without the Android SDK — user must build/test in Android Studio or provide a keystore + SDK. First deliverable = step 1 source; user compiles to APK.

## 9. Open questions (blocking — asked via question tool)

See separate question prompt: visual layout (unreadable PNGs), weather provider/API key, fonts source, package name, min SDK, build/test expectation.