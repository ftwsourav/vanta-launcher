# Vanta — Progress & Changelog

## v5.5 (2026-09-27) — "Hubs": live tiles, folders, focus, widgets

### Live tiles
- Tiles show their app's active notifications: count as a display numeral, newest title and text in mono, the 8 dp accent square. Small tiles show count and title only.
- Badges come from the notification listener and update live; the old NotificationManager path (which only ever saw Vanta's own notifications) is gone.
- Live tile motion setting: FLIP (WP7 Y-flip) or PEEK (WP8 slide-up), one shared frame switcher, per-tile periods so the grid never moves in unison.

### Home
- Tile folders, WP 8.1 style: a 2-span tile expands inline and pushes the rows down; create from a tile's context menu, rename with a brutalist dialog.
- Morning brief: 5–9 am the first row becomes one Ink tile peeking through date, weather, next alarm, battery and a quote.
- Focus session strip: while a session runs, non-focus tiles collapse and a 44 dp accent row counts down; tap to end.
- Widgets section: any Android widget hosted inside an outline tile, resizable by span and height, removable; brutalist picker with bind and configure flow.
- Editable composition: in edit mode a top-left square cycles a tile through Outline, Ink and Accent; tap the caption to edit it with suggestions.

### Live page
- Action buttons on cards; inline reply through RemoteInput with a mono field and a "→" send square; "SENT" then dismiss.
- Swipe or CLEAR now cancels the notification system-wide via the listener.
- Grouped by app with count, "+N MORE" and CLEAR per group.

### Focus
- Session tile with 25 / 50 / 90 / custom minutes; Ink countdown with a draining accent rule and a blinking square; Do Not Disturb on while it runs (with a one-tap policy-access row); survives a relaunch; appends "FOCUS · 25 MIN · 21:30" to your notes when it ends.

### Search
- Calculator (safe recursive-descent parser), unit conversion, contacts with CALL / MSG, settings panels (wifi, bluetooth, dnd, airplane, flashlight, hotspot, dark/light), then web.

### Canvas and night
- Panorama photo behind all four pages, mono-filtered, moving at a third of the swipe (Settings › Canvas).
- Nightstand: Glance shows itself while charging between 21:00 and 06:00 after 30 s idle, dimmed, drifting to avoid burn-in.

### Settings
- New rows for everything above, plus WHAT'S NEW linking to the latest release.

---

## v5.4 (2026-09-27) — "Turnstile": the Lumia motion pass

### Motion
- Tiles turnstile in (hinged on the left edge, 32 ms stagger) on first show and again every time the launcher returns to the foreground.
- Tapping a tile swings it out around its left edge and the app window clip-reveals out of the tile bounds (ActivityOptions), Windows Phone tile-to-app.
- Page transforms run in the draw phase: CUBE is the Windows 8.1 edge pivot, TAP FLIP the WP7 turnstile, SMOOTH a parallax slide; one light haptic tick on settle.
- Pivot header in bold mono with an animated accent underline and parallax drift; settings sheet slides up on the Lumia curve.
- Edge swipes (gesture actions) now beat the pager: pointer read in the Initial pass, 36 dp zone, accent pull bar with a haptic notch.
- Quick settings slides from the top, nine tiles turnstile in, brutalist brightness slider, drag-to-close.
- Search, recents, glance, app picker and notification cards enter with turnstile staggers; settings values slide 8 dp on change.
- Cinematic splash is 650 ms with the real Vanta logo wiping into the page.

### Home
- One continuous pinned grid; the fake ESSENTIALS / RECENTLY ADDED halves and the hard-coded badges are gone.
- Heavy tabular clock with the weekday in accent; the now-playing strip only appears while something plays.
- Square Metro badges, square music and quick-settings buttons, a drawn plus glyph, task rows that animate in and out, a music widget that collapses when idle.

### Apps
- "GOOD APPS BETTER DAYS." headline sized to its column instead of breaking mid-word; app rows never truncate.
- Windows Phone semantic zoom: tap a letter header for the A–Z grid. Search focus animates to accent, × clears, IME Search launches the top hit.
- WP app bar with canvas-drawn glyphs and an expanding "more" row.

### Live
- Status rows laid out properly (right-aligned meta, no jammed glyphs) and only one permission prompt at a time.
- The notification feed comes from the notification listener, so every app's notifications show (before, only Vanta's own could).
- Cards swipe to dismiss with spring-back; heavy tabular clock with a minute flip.

### Focus
- Tabular stopwatch and timer digits, drawn progress rule and blink, brutalist note dialog, photo crossfade, suggestions limited to installed apps.

### Settings
- Windows Phone toggle switches, animated value rows, collapsible sections with a rotating +/−, square swatches with a selection ring, a brutalist app-picker sheet, a backup/restore status label.

### Fixes
- 53 double-encoded characters (· “ ” —) across 13 files.
- Live-tile name frames no longer render huge and truncated.
- Task ids no longer collide after a removal.
- Duplicate onboarding removed from Settings; the dead SLIDEABLE HOME toggle is gone.

---


> A brutalist Android launcher replacement inspired by Windows Phone / Lumia Metro UI.
> Built with Kotlin + Jetpack Compose. Targeting OnePlus 13 (CPH2653), Android 14+.

---

## Quick Reference

| Item | Value |
|------|-------|
| Package | `com.xdlab.standard` |
| Project root | `C:\Users\Lab\Desktop\Brutalist Android launcher\standard-launcher` |
| APK output | `C:\Users\Lab\Desktop\Brutalist Android launcher\STANDARD-v3-lumia-pro-final.apk` |
| Device | OnePlus CPH2653, ID `466871ba` |
| minSdk | 30 |
| compileSdk | 35 |
| targetSdk | 34 |
| Kotlin | 2.0 |
| Compose BOM | 2024.12 |
| Java | 17 (Eclipse Adoptium) |
| Gradle | 8.9 |
| versionCode | 1 |
| versionName | 1.0.0 |

### Build & Install Commands

```powershell
# Set env
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "C:\Users\Lab\AppData\Local\Temp\opencode\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$gradle = "C:\Users\Lab\AppData\Local\Temp\opencode\gradle\gradle-8.9\bin\gradle.bat"
$adb = "C:\Users\Lab\AppData\Local\Temp\opencode\android-sdk\platform-tools\adb.exe"

# Compile check
& $gradle :app:compileDebugKotlin --no-daemon --console=plain *> $log

# Full APK build
& $gradle assembleDebug --no-daemon --console=plain *> $log

# Install
& $adb install -r "app\build\outputs\apk\debug\app-debug.apk"

# Log is UTF-16 encoded, convert for reading:
Get-Content -LiteralPath $log -Encoding Unicode | Out-File -Encoding UTF8 "utf8_log.txt"
```

### Gotchas & Known Issues

- **Gradle log encoding**: output is UTF-16, must convert with `Get-Content -Encoding Unicode | Out-File -Encoding UTF8` before searching
- **DataStore 1.1.1**: `booleanKey` etc. are Kotlin-internal, settings use SharedPreferences instead
- **Retrofit converter**: Jake Wharton retrofit converter 1.0.0 — all classes internal. Weather uses OkHttp + kotlinx.serialization directly (no Retrofit converter)
- **`rememberSaveable`**: in package `androidx.compose.runtime.saveable`, NOT `androidx.compose.runtime`
- **`FontVariation`**: experimental — Type.kt uses `Font(resId, weight)` without FontVariation
- **`Animatable<Float>`**: needs 2 type args in 1.7.6: `Animatable<Float, AnimationVector1D>`
- **WorkManager 2.10**: requires compileSdk 35 (bumped from 34)
- **`detectTransformGestures`**: import from `androidx.compose.foundation.gestures`
- **`combinedClickable`**: requires `@OptIn(ExperimentalFoundationApi::class)`
- **`LocalAppTheme.current`**: returns object with `background`, `tileFill`, `accent`, `text`, `useTexture` (all `Color` / `Boolean`)
- **Fonts**: `SpaceGrotesk` and `JetBrainsMono` are `FontFamily` vals in `com.xdlab.standard.ui.theme`

---

## Version History

### v1.0 — Initial Build
- Full launcher scaffold: HomeScreen, DrawerScreen, FocusScreen, SettingsScreen
- 3-page pivot navigation (Home / Apps / Focus) with HorizontalPager + Smooth animation modes
- Pinned apps grid with tile sizes (SMALL/MEDIUM/WIDE/LARGE) + edit mode
- Weather integration (Open-Meteo API via OkHttp + kotlinx.serialization)
- Live clock tile with date flip cycling
- Settings: dark mode, haptics, animation style, icon style, weather location
- Brutalist design system: Space Grotesk + JetBrains Mono fonts, noise overlay, barcode footer
- Installed on device, functional as default launcher

### v2.0 — Deep Audit Fixes
- Fixed dead code: LumiaScroll wired, LiveTileCycling available
- Fixed WIDE/LARGE tile layout rendering
- Added tappable PivotHeader
- Added BackHandler for navigation
- Added alphabet jump bar in drawer
- Added focus image remove feature
- Fixed icon OOM (icon caching with produceState + IO dispatch)
- Lighter haptic feedback (TextHandleMove instead of LongPress)

### v3.0 — "Lumia Pro" Major Overhaul (12 Transformations)

All 12 transformations from `MAJOR_OVERHAUL_PLAN.md` implemented and shipping.

---

## The 12 Transformations

### TIER 1: TRANSFORMATIVE

#### 1. People Hub Contact Tile
- **Files**: `SystemTiles.kt`, `ContactItem.kt`, `ContactsRepository.kt`, `ContactsRepositoryImpl.kt`
- **Status**: SHIPPED
- Empty state shows "TAP TO ENABLE CONTACTS" with permission launcher
- Populated state shows 3 contact tiles with initial avatars + colored backgrounds
- Avatar colors from 8-color palette indexed by `avatarColorIndex`
- Tap enables READ_CONTACTS permission → contacts load
- Repository polls contacts via `ContactsContract` on permission grant

#### 2. Media Now-Playing Tile
- **Files**: `SystemTiles.kt`, `MediaInfo.kt`, `MediaRepository.kt`, `MediaRepositoryImpl.kt`
- **Status**: SHIPPED (metadata read-only, no transport controls wired to system yet)
- Shows "NO MEDIA PLAYING" when no active session
- When active: shows track title + artist in uppercase + transport glyphs (⏮ ▶ ⏭)
- Repository polls `MediaSessionManager.getActiveSessions()` every 2s
- Note: transport control taps are no-op placeholders (need `dispatchMediaButtonEvent` or `MediaController.getTransportControls()`)

#### 3. Battery Tile with Progress Ring
- **Files**: `SystemTiles.kt`, `BatteryState.kt`, `BatteryRepository.kt`, `BatteryRepositoryImpl.kt`
- **Status**: SHIPPED
- Custom-drawn circular progress arc via Canvas
- Color states: red (<15%), orange (<35%), white (normal), yellow (charging)
- Animated percent count-up via `Animatable`
- Status line: "CHARGING — ETA 2H 30M" / "LOW — RECHARGE" / "45% · NORMAL"
- Charging bolt glyph drawn when plugged in
- Uses `BatteryManager.BATTERY_PROPERTY_CAPACITY` (aliased as `OsBatteryManager` to avoid name clash)

#### 4. Quick Settings Floating Action Button
- **Files**: `SystemTiles.kt`
- **Status**: SHIPPED (UI only, toggles are no-op placeholders)
- Bottom-right FAB with hamburger (☰) icon, expands to X (✕) when open
- Radial menu: WIFI, BT, DND, NIGHT with glyph icons
- Each action: circular tile + label in JetBrainsMono
- Collapsible via `rememberSaveable` state
- Note: actual system toggles need `WifiManager.setWifiEnabled()` (deprecated API 29+) / `BluetoothAdapter` / `NotificationManager.setInterruptionFilter()` — not wired

### TIER 2: WOW POLISH

#### 5. Animated Drifting Noise Texture
- **Files**: `DriftingNoiseOverlay.kt` (replaces `NoiseOverlay.kt` at call sites)
- **Status**: SHIPPED
- 12-second linear horizontal drift via `infiniteRepeatable`
- Applied to `BitmapShader` localMatrix (128px = one seamless tile loop)
- Same 128×128 noise bitmap with fixed seed `0x5EEDL`
- Respects `useTexture` theme flag
- Exports `rememberDriftOffset()` helper for syncing other animations
- Integrated in `StandardApp.kt` replacing `NoiseOverlay`

#### 6. Edge-Swipe Gesture for Drawer
- **Files**: `EdgeSwipeHandler.kt`
- **Status**: SHIPPED
- `Modifier.edgeSwipeHandler(enabled, onSwipeOpen)` extension
- Detects drag starting within 24dp of left edge
- Threshold: 80dp horizontal drag to trigger
- Visual feedback: accent-colored vertical bar grows with drag progress
- Wired in `StandardApp.kt` — active only on Home page (page 0)
- Triggers `pagerState.animateScrollToPage(1)` on success

#### 7. Semantic Letter-Grid Zoom in Drawer
- **Files**: `SemanticZoomGrid.kt`
- **Status**: SHIPPED
- Pinch out in drawer → `detectTransformGestures` sets zoom target 0→1
- Overlay fades in showing A-Z grid (5 columns, `LazyVerticalGrid`)
- Each letter cell staggers in with scale+alpha based on index
- Tap letter → springs back to list + scrolls to that section
- Uses `animateFloatAsState` with `DampingRatioMediumBouncy` spring
- `SemanticZoomState` + `rememberZoomState()` helper exported

#### 8. Edit-Mode Resize Grip Triangles + Size Label
- **Files**: `ResizeGrip.kt`
- **Status**: SHIPPED
- Corner triangle (24dp legs) in bottom-right of tile, accent-colored
- Drawn via `drawBehind` + `Path` with three points
- Tap area bounded to 24dp×24dp corner region
- After cycling: shows size label ("SMALL"/"MEDIUM"/"WIDE"/"LARGE") centered for 800ms
- Label: black semi-transparent box + 2dp accent border, JetBrainsMono Black 14sp
- Haptic feedback on tap (`TextHandleMove`)
- `Modifier.resizeGripOverlay(enabled, onCycleSize)` chained on AppTile in edit mode
- Note: defines its own `TileSize` enum in `ui.components` package (separate from `domain.model.TileSize`)

#### 9. Floating Search Bar on Home
- **Files**: `FloatingSearchBar.kt`
- **Status**: SHIPPED
- Collapsed: 44dp row with 🔍 + "SEARCH" placeholder in JetBrainsMono
- Expanded (tap): BasicTextField appears inline, live-filters pinned apps by label
- Results: horizontal scrolling pill chips, max 8 shown
- Each chip: uppercase label, JetBrainsMono, tap → launches app
- `rememberSaveable` for expanded state + query
- `animateColorAsState` for background, `AnimatedVisibility` for results
- 2dp border at text-30% alpha when expanded
- Integrated in `HomeScreen.kt`, shown when NOT in edit mode

### TIER 3: AMBIENCE

#### 10. Time-of-Day Mono Palette Tint
- **Files**: `TimeOfDayPalette.kt`
- **Status**: SHIPPED (as subtle overlay, 12% alpha tint)
- 5 palettes: DAWN (warm paper), DAY (clean mono), DUSK (amber), NIGHT (charcoal-blue), MIDNIGHT (deep black)
- `paletteForHour(hour)` selects based on time bands
- `lerpPalette(a, b, t)` linearly interpolates colors for smooth transitions
- `rememberTimeOfDayPalette(enabled)` samples every 60s, lerps toward next hour's palette
- `darkPaletteForHour(hour)` forces dark variants only (DUSK/NIGHT/MIDNIGHT)
- Applied in `StandardApp.kt` as a 12% alpha background overlay
- Note: currently only active when `settings.darkMode` is true

#### 11. Cinematic Intro Animation
- **Files**: `CinematicSplash.kt`
- **Status**: SHIPPED
- Replaces static "S." splash with 800ms 3-phase animation
- Phase 1 (0-250ms): "S." springs in (0.3→1.0 scale) + grain noise burst (200 white dots)
- Phase 2 (250-550ms): 12 accent-colored shards fall into 4×3 grid with 15ms stagger
- Phase 3 (550-800ms): scale up to 2.0x + fade out, background lerps Black→tileFill
- Single `Animatable<Float>` progress drives all phases
- Calls `onAnimationComplete()` when done
- Integrated in `StandardApp.kt` replacing static splash

#### 12. Glance Screen
- **Files**: `GlanceScreen.kt`, `GlanceOverlay` wrapper
- **Status**: SHIPPED (in-app fullscreen, no lockscreen overlay)
- Fullscreen black + noise overlay at 0.04 alpha
- Large 72sp SpaceGrotesk clock (HH:MM), updates every second
- Date below: "EEE DD MMM" uppercase, JetBrainsMono, 60% white
- "STANDARD." tagline in accent color, SpaceGrotesk Black 20sp
- "TAP TO DISMISS" hint at 30% white
- Fade-in on appear (400ms `animateFloatAsState`)
- Triggered by long-pressing any pivot header tab in `StandardApp.kt`
- `GlanceOverlay(visible, onDismiss)` wraps in `AnimatedVisibility` with fadeIn/fadeOut
- Note: true lockscreen overlay would need `SYSTEM_ALERT_WINDOW` permission + foreground service — not implemented, in-app fallback only

---

## Architecture Overview

```
com.xdlab.standard/
├── StandardApplication.kt          # Application class
├── LauncherActivity.kt             # Launcher entry point
├── MainActivity.kt                 # Main activity
├── di/
│   └── AppContainer.kt             # DI container (repos + viewmodel factory)
├── domain/
│   └── model/                      # Domain models
│       ├── AppItem.kt
│       ├── ContactItem.kt
│       ├── MediaInfo.kt
│       ├── BatteryState.kt
│       ├── SettingsState.kt
│       ├── WeatherData.kt
│       ├── WeatherLocation.kt
│       ├── TileSize.kt
│       ├── IconStyle.kt
│       ├── AnimationStyle.kt
│       └── ThemeConfig.kt
├── data/
│   ├── repo/                       # Repository interfaces
│   │   ├── AppRepository.kt
│   ├── ContactsRepository.kt
│   ├── MediaRepository.kt
│   ├── BatteryRepository.kt
│   ├── SettingsRepository.kt
│   ├── ThemeRepository.kt
│   └── WeatherRepository.kt
│   ├── local/                      # Local impls
│   ├── AppRepositoryImpl.kt
│   ├── ContactsRepositoryImpl.kt
│   ├── BatteryRepositoryImpl.kt
│   └── SettingsRepositoryImpl.kt
│   └── remote/                     # Remote impls
│       ├── WeatherRepositoryImpl.kt
│       ├── WeatherApi.kt
│       ├── WeatherModels.kt
│       ├── WeatherWorker.kt
│       └── MediaRepositoryImpl.kt
└── ui/
    ├── theme/                      # Design system
    │   ├── Color.kt
    │   ├── Theme.kt
    │   ├── Type.kt
    │   ├── LocalAppTheme.kt
    │   ├── ThemeRepositoryImpl.kt
    │   └── TimeOfDayPalette.kt     # v3.0: ToD palettes
    ├── components/                 # Reusable composables
    │   ├── Tile.kt                 # Tile, AppTile, LiveTile, LiveTileCycling, TileEntrance, AppIcon
    │   ├── SystemTiles.kt          # v3.0: BatteryTile, MediaTile, PeopleHubTile, QuickSettingsFab
    │   ├── DriftingNoiseOverlay.kt # v3.0: Animated noise (replaces NoiseOverlay)
    │   ├── NoiseOverlay.kt        # Static noise (legacy, unused at call sites)
    │   ├── LumiaScroll.kt         # Lumia-style scroll (defined, not wired)
    │   ├── LiveClock.kt           # Live clock composable
    │   ├── FloatingSearchBar.kt   # v3.0: Expandable search
    │   ├── EdgeSwipeHandler.kt   # v3.0: Edge-swipe modifier
    │   ├── SemanticZoomGrid.kt    # v3.0: A-Z zoom grid
    │   ├── ResizeGrip.kt          # v3.0: Corner resize triangles
    │   ├── CinematicSplash.kt     # v3.0: 800ms intro animation
    │   └── AnimationPresets.kt   # v3.0: Easings, springs, shimmer, pulse, etc.
    ├── screens/
    │   ├── home/
    │   │   └── HomeScreen.kt      # Main home screen
    │   ├── drawer/
    │   │   └── DrawerScreen.kt    # App drawer + semantic zoom
    │   ├── focus/
    │   │   └── FocusScreen.kt      # Focus/do-not-disturb screen
    │   ├── settings/
    │   │   └── SettingsScreen.kt   # Settings panel
    │   └── glance/
    │       └── GlanceScreen.kt     # v3.0: Glance clock screen
    └── nav/
        ├── StandardApp.kt          # Root composable + pager + splash + glance
        └── StandardAppViewModel.kt  # ViewModel (exposes all StateFlows)
```

### StateFlow Exposed by ViewModel

```kotlin
val settings: StateFlow<SettingsState>
val pinnedApps: StateFlow<List<AppItem>>
val allApps: StateFlow<List<AppItem>>
val weather: StateFlow<WeatherData?>
val weatherLoading: StateFlow<Boolean>
val weatherError: StateFlow<String?>
val forecast: StateFlow<List<ForecastDay>>
val contacts: StateFlow<List<ContactItem>>
val contactsPermissionGranted: StateFlow<Boolean>
val nowPlaying: StateFlow<MediaInfo?>
val battery: StateFlow<BatteryState>
```

---

## AnimationPresets Library (v3.0 Bonus)

Reusable animation helpers in `AnimationPresets.kt`:

| Helper | Description |
|--------|-------------|
| `LumiaEasing` | CubicBezier(0.16, 1, 0.3, 1) — smooth deceleration |
| `SharpEasing` | CubicBezier(0.7, 0, 0.84, 0) — aggressive |
| `GlideEasing` | CubicBezier(0.25, 0.46, 0.45, 0.94) — gentle |
| `TileSpring` | spring(0.7, StiffnessMediumLow) |
| `BounceSpring` | spring(0.45, StiffnessMedium) |
| `GentleSpring` | spring(0.9, StiffnessLow) |
| `staggerDelay(index, base, max)` | Entrance stagger timing |
| `Modifier.pressScale(pressed, scaleDown)` | Animated press-to-scale |
| `Modifier.tileEntrance(index, visible)` | Staggered entrance (alpha + translateY) |
| `Modifier.shimmer(active)` | Horizontal gradient sweep (1500ms infinite) |
| `Modifier.pulseGlow(active, color, radius)` | Pulsing shadow glow (800ms) |
| `CountUpText(targetValue, suffix)` | Animated 0→N count-up display |
| `pageSlideTransition()` / `pageSlideExitTransition()` | Slide + fade page transitions |
| `rememberHaptic(enabled)` | Returns `(HapticType) -> Unit` lambda |
| `Modifier.scrollFade(scrollState, threshold)` | Fade alpha based on scroll position |

---

## Permissions

```xml
<!-- AndroidManifest.xml -->
READ_CONTACTS          <!-- People Hub -->
BLUETOOTH_CONNECT      <!-- Quick Settings FAB (BT toggle) -->
INTERNET               <!-- Weather API -->
ACCESS_NETWORK_STATE   <!-- Weather API -->
```

---

## Dependencies (build.gradle.kts)

- Compose BOM 2024.12 + UI + Material3 + Material Icons Extended
- Navigation Compose
- Room (runtime + ktx + KSP compiler)
- WorkManager Runtime KTX
- Retrofit + kotlinx-serialization converter
- OkHttp (implied via Retrofit)
- kotlinx-serialization-json
- kotlinx-coroutines-android
- Coil Compose (image loading)

---

## What Needs Polish (For Next Session)

### High Priority — Functional Gaps

1. **Media transport controls**: `MediaTile` transport glyphs (⏮ ▶ ⏭) are no-op. Need to wire to `MediaController.getTransportControls().play()/pause()/skipToNext()/skipToPrevious()`. Requires `NotificationListenerService` for full metadata access.

2. **Quick Settings toggles**: FAB action buttons (WIFI/BT/DND/NIGHT) are no-op. Need:
   - WiFi: `WifiManager.setWifiEnabled()` (deprecated 29+, use `Settings.Panel.ACTION_WIFI`)
   - Bluetooth: `BluetoothAdapter.enable()` / `disable()` (needs BLUETOOTH_CONNECT on API 31+)
   - DND: `NotificationManager.setInterruptionFilter(INTERRUPTION_FILTER_NONE)`
   - Night: toggle `settings.darkMode`

3. **LumiaScroll**: `LumiaScroll.kt` defined but STILL not wired into HomeScreen (HomeScreen uses `verticalScroll`). Replace with `LumiaScroll` for Lumia-style overscroll bounce.

4. **ToD palette**: Currently only a 12% alpha overlay. Should actually replace `colors.background` / `colors.tileFill` / `colors.accent` in the theme when enabled. Make it a proper settings toggle.

5. **Glance screen trigger**: Currently long-press on pivot header. Consider:
   - Double-tap on home background
   - Swipe down from top
   - Settings toggle to show on app foreground

### Medium Priority — Visual Polish

6. **Cinematic splash**: Phase 2 shards are simple rectangles. Could be actual tile-shaped shards that snap into the home grid positions. Add particle trail behind shards.

7. **Semantic zoom**: Pinch detection is basic (`zoom < 0.9f` / `> 1.1f`). Should use proper pinch gesture with scale tracking, not binary threshold. Add rubber-band effect at edges.

8. **Edge swipe**: Bar feedback is minimal. Add a preview shadow of the drawer sliding in during drag (like iOS peek). Consider right-edge swipe to go back from drawer to home.

9. **Resize grip**: Triangle only shows in bottom-right corner. Consider 4-corner grips (top-left for SMALL, top-right for MEDIUM, bottom-left for WIDE, bottom-right for LARGE) — more intuitive than cycling.

10. **Floating search**: Only searches pinned apps. Should search ALL apps and offer to pin. Add recents/frequency ranking. Keyboard should auto-focus on expand.

11. **Battery tile**: ETA calculation is rough (`(100-pct)*2` minutes). Could use `BatteryManager.BATTERY_PROPERTY_STATUS` + historical drain rate for better estimate. Add "time remaining" when unplugged.

12. **People Hub**: Only shows 3 contacts. Should show 4-6 with a "+" tile to open full contacts list. Tap should offer call/SMS options, not just dial.

### Low Priority — Nice to Have

13. **Noise overlay**: Drift is horizontal only. Could add vertical drift + rotation for more organic feel. Make drift speed configurable in settings.

14. **Tile entrance animations**: Currently uses basic fade. Could use the `AnimationPresets.tileEntrance` modifier for staggered entrance on Home screen load.

15. **Shimmer effect**: Defined in `AnimationPresets.kt` but not used anywhere. Could apply to loading states (weather loading, contacts loading).

16. **Pulse glow**: Defined but not used. Could apply to the battery tile when low, or to the media tile when playing.

17. **Count-up text**: Defined but not used. Could apply to battery percentage, weather temperature, app count in drawer.

18. **Scroll-based fade**: Defined but not used. Could fade the pivot header as user scrolls down in drawer.

19. **Haptic helper**: Defined but not used at call sites. Could replace direct `performHapticFeedback` calls throughout.

20. **Drawer alphabet jump bar**: Works but small (8sp). Could make it taller and add touch-and-drag scrubbing (like iOS contacts).

### Settings Screen Additions Needed

- Toggle for time-of-day palette (on/off)
- Toggle for noise drift (on/off, speed slider)
- Toggle for cinematic intro (on/off, or "simple" fallback)
- Toggle for glance screen (enable/disable trigger method)
- Slider for tile press scale depth
- Option to choose accent color manually (override ToD)

### Dead Code / Cleanup

- `NoiseOverlay.kt` — legacy, replaced by `DriftingNoiseOverlay.kt` at call sites. Can delete.
- `ResizeGrip.kt` defines its own `TileSize` enum in `ui.components` — conflicts conceptually with `domain.model.TileSize`. Should use the domain one.
- `SemanticZoomGrid.kt` has `rememberZoomState()` + `SemanticZoomState` that are unused (DrawerScreen implements its own inline zoom state). Remove or wire it.

---

## File Count

- **Total Kotlin files**: 55
- **New in v3.0**: 9 files
  - `SystemTiles.kt` — Battery/Media/People/FAB
  - `DriftingNoiseOverlay.kt` — animated noise
  - `FloatingSearchBar.kt` — expandable search
  - `EdgeSwipeHandler.kt` — edge-swipe modifier
  - `SemanticZoomGrid.kt` — A-Z zoom grid
  - `ResizeGrip.kt` — resize triangles
  - `CinematicSplash.kt` — intro animation
  - `TimeOfDayPalette.kt` — ToD palettes
  - `GlanceScreen.kt` — glance clock
  - `AnimationPresets.kt` — animation library

## Build Status

- **Last successful build**: v3.0 final, `BUILD SUCCESSFUL in 21s`
- **Last install**: Success on OnePlus CPH2653 (466871ba)
- **APK size**: 59.04 MB (debug)
- **Compile time**: ~30s (Kotlin only), ~21s (incremental APK)

---

*Last updated: v3.0 "Lumia Pro" — all 12 transformations shipped + installed.*
