# NEXT_UPDATE_PLAN.md — STANDARD. v1.2

Audited the full codebase against Plan.MD and real-device behavior. Found 6 bugs, 4 perf issues, and 10 UX gaps. Prioritized below: bugs first, then perf, then UX.

---

## BUGS (fix all — these break real usage)

### B1. Drawer long-press pin logic is inverted
`DrawerScreen.kt` `togglePin`: `pinned.filterNot { it.packageName != app.packageName }` is a double negative — it keeps ONLY the app being unpinned (opposite of intent). Should be `pinned.filterNot { it.packageName == app.packageName }`.

### B2. Focus Mode image resets on app restart
`FocusScreen.kt:58`: picked image URI stored in `rememberSaveable` but never persisted with `contentResolver.takePersistableUriPermission()`. The URI is single-use; the image disappears after process death. Fix: call `takePersistableUriPermission` on pick, store the URI string in SharedPreferences, restore on load.

### B3. Weather response.body checked after close
`WeatherRepositoryImpl.kt:87-91`: reads `response.body?.string()`, calls `response.close()`, THEN checks `response.isSuccessful`. Reorder: check `isSuccessful` first, read body only if successful, close in a `finally`.

### B4. Settings location text field doesn't sync on GPS update
`SettingsScreen.kt:61`: `remember { mutableStateOf(settings.weatherLocation.name) }` — the initializer only runs once. If GPS sets a new location name, the text field still shows the old value. Fix: `remember(settings.weatherLocation.name) { ... }`.

### B5. GPS silently fails with no feedback
`SettingsScreen.kt:268-275`: `fetchLocation` catches `SecurityException` but also returns silently when `getLastKnownLocation` is null (no location history). User sees nothing happen. Fix: show a Toast "Location unavailable — enter manually" when null.

### B6. Weather auto-refresh fires on every recomposition
`StandardApp.kt` `LaunchedEffect(settings.weatherLocation)` — `weatherLocation` is a data class; if any unrelated field in `SettingsState` changes and causes `settings` to emit, the `LaunchedEffect` key comparison may re-fire if the `WeatherLocation` instance changes. Add `.distinctUntilChanged()` on the location flow, or key on `lat,lon,name` tuple.

---

## PERFORMANCE (fix all — visible on 120+ app drawers)

### P1. App icons loaded on main thread
`Tile.kt` `loadAppIcon` calls `PackageManager.getApplicationIcon` synchronously inside a Composable via `remember`. For 100+ drawer rows this blocks the UI thread on first scroll. Fix: load async with a `produceState` or `LaunchedEffect` + `Dispatchers.IO`, cache results in a static `LruCache<String, ImageBitmap>` (max 64 entries).

### P2. AppRepository.refresh() runs on main thread
`AppRepositoryImpl.kt:62`: `refresh()` is called from `LauncherApps.Callback` (main thread) and does `sortedBy` on 100+ apps. Move the sort + map to `Dispatchers.Default` via `scope.launch`, then post to the StateFlow.

### P3. AppTile re-renders icon on every recomposition
`Tile.kt` `AppIcon` uses `remember(packageName)` but the `loadAppIcon` function itself does a `PackageManager` call each time the cache is invalidated (e.g. on theme change). With the LruCache from P1, this becomes a cache hit — no re-load.

### P4. Drawer has no view recycling for icons
`DrawerScreen.kt` uses `LazyColumn` (good) but each `NumberedRow` calls `AppIcon` which triggers a `remember(packageName)` → `loadAppIcon`. With P1's LruCache this is a cache hit during scroll — fast. No additional work needed beyond P1.

---

## UX UPGRADES (prioritized by impact)

### U1. Live clock that ticks every second
Home + Focus day/date tiles are static — computed once on composition. Add a `ClockViewModel` or a `produceState` that emits the current `LocalDateTime` every second. The day tile shows `HH:MM` (live) above the date. This is the single most visible "alive" improvement for a launcher.

### U2. Weather condition icons (Unicode glyphs, no assets)
Replace the text-only condition ("Clear sky", "Rain") with a condition icon + text. Use Unicode weather glyphs mapped from WMO code: 0/1 → ☀, 2 → ⛅, 3 → ☁, 45/48 → 🌫, 51-67 → 🌧, 71-77 → ❄, 80-82 → 🌦, 95-99 → ⛈. Render at 28sp next to the temp. Zero new assets, instant visual upgrade.

### U3. Multi-day weather forecast (expandable)
Weather tile tap → expands a 3-day forecast strip (fetch `forecast_days=3` from Open-Meteo, already supports it). Each day: day name, icon, high/low. Collapse on second tap. Uses the existing `DailyWeather` model (extend to 3 entries).

### U4. Accent tile picker in Settings
Currently the accent app is hardcoded to `com.android.camera`. Add a "ACCENT APP" section in Settings: a dropdown/row that lets the user pick which pinned app is the accent tile (calls `viewModel.setAccent(packageName)`). Shows the current accent app + a picker listing all pinned apps.

### U5. Pinned apps reorder (long-press drag in a settings sub-screen)
Add "EDIT PINNED" in Settings → opens a reorderable list (drag handle on each row, reorder updates `setPinned` with new order). Uses `ReorderableColumn` from Compose Foundation or a simple `LazyColumn` with `detectDragGesturesAfterLongPress`. This is the #1 customization request for any launcher.

### U6. Fast-scroll alphabet jump bar in drawer
For 100+ apps, add a vertical alphabet strip on the right edge of the drawer. Tapping a letter scrolls the LazyColumn to the first app starting with that letter. Compute the first-index map from `allApps` (already sorted alphabetically).

### U7. About screen (version + OFL + privacy)
Settings → "ABOUT" section at the bottom: app version (1.0.0), "Fonts: Space Grotesk + JetBrains Mono (OFL)", "Weather: Open-Meteo", "No tracking, no cloud sync." Brutalist one-screen credits.

### U8. Weather loading + error feedback in UI
`HomeScreen` weather tile currently shows "—" when null. Observe `weatherLoading` and `weatherError` StateFlows (already exist in the VM). Show a small `CircularProgressIndicator` (14dp) while loading. Show a tiny "!" indicator if error != null, tappable → Toast with the error message.

### U9. Empty state handling
If `allApps` is empty (zero installed apps — edge case but possible on fresh devices), show a centered "NO APPS FOUND" tile instead of blank rows. If `pinnedApps` is empty, show "PIN APPS IN DRAWER" hint on Home.

### U10. Haptic feedback on settings changes
Every toggle/selection in Settings fires `performHapticFeedback(LongPress)` when `settings.hapticsEnabled` is true. Currently only page changes have haptics — settings changes are silent.

---

## Implementation order (batch by file ownership to avoid conflicts)

**Batch A — Bug fixes + perf (data + components):**
- B1 (DrawerScreen.kt), B2 (FocusScreen.kt), B3 (WeatherRepositoryImpl.kt), B6 (StandardApp.kt)
- P1+P3 (Tile.kt — async icon loading + LruCache)
- P2 (AppRepositoryImpl.kt — async refresh)

**Batch B — Settings UX (SettingsScreen.kt + new sub-screens):**
- B4, B5 (SettingsScreen.kt)
- U4 (accent picker), U5 (pinned reorder), U7 (about), U10 (haptics)

**Batch C — Home/Focus/Weather UX (screens + weather):**
- U1 (live clock), U2 (weather icons), U3 (forecast), U8 (loading/error), U9 (empty states)

**Batch D — Drawer UX (DrawerScreen.kt):**
- U6 (alphabet jump bar)
