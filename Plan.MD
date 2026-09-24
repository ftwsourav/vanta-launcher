# STANDARD. — Brutalist Android Launcher
### Build specification for an AI coding agent (Kotlin + Jetpack Compose, Android 14+)

You are building STANDARD., a working default-launcher-replacement app for Android. This is not a mockup or demo — it must be installable, selectable as the system default home app, and functional with real installed apps, real timeweather, and a working theming + animation engine from day one. Target device for testing OnePlus 13 class hardware, 1440×3168, 120Hz, Android 14+.

Treat might publish to Play Store later as a real constraint avoid hacky shortcuts that would need to be rewritten (hardcoded package names for buttons that should launch arbitrary apps, no permission handling, etc).

---

## 1. Tech stack

- Kotlin + Jetpack Compose (not XML views) — the tile animations depend on Compose's `graphicsLayer``Animatable`spring physics.
- MVVM architecture `ViewModel` + `StateFlow` per screen, repository layer for appsweathersettings.
- Jetpack DataStore (Preferences) for all user settings (theme, animation style, icon style, weather location, haptics, dark mode).
- WorkManager for periodic weather refresh (do not poll in a tight loop).
- Package name placeholder `com.xdlab.standard` (adjust to your actual signing identity).

## 2. Typography & texture

Use your existing brand fonts for consistency and to avoid extra asset work
- Space Grotesk (BoldBlack weight) for all large headline tiles (day name, GOOD APPS BETTER DAYS., etc.)
- JetBrains Mono for metalabel text — the barcode caption, version strings, small uppercase labels (`WHATSAPP  MESSAGES  CALLS`), settings rows.
- A subtle paper-grainhalftone noise overlay (semi-transparent PNG or a `drawWithCache` noise shader) on the background — this is core to the brutalist look in the mockups and should be a toggleable setting, not hardcoded.

## 3. Core launcher requirements (system integration)

- `AndroidManifest` activity with `category androidname=android.intent.category.HOME` and `DEFAULT` so it can be set as default launcher.
- Real installed-app list via `PackageManager.queryIntentActivities()` (not a hardcoded list) — the tiles shown in mockups (WhatsApp, Telegram, YouTube, Spotify, Chrome, Reddit, Gallery, Camera, Notes, Settings, Files) should be user-pinnable defaults, but the App Drawer must show every installed app.
- `BroadcastReceiver` for `ACTION_PACKAGE_ADDED`  `ACTION_PACKAGE_REMOVED` to keep the drawer in sync live.
- Handle the case where the user has zero network  denies a permission gracefully — a launcher that crashes or blank-screens on missing permissions will not survive a Play Store review.
- App's own adaptive launcher icon (ironic but required) in the brutalist style — simple monogram, e.g. a black square with a white S. mark.

## 4. Screens

### 4.1 Home
- Daydate tile (large, e.g. FRI SEP 06, 2026)
- Weather tile location, temp, condition, highlow, humidity, wind, last-updated time
- Grid of pinned app tiles, sizes varying (some full-width like WhatsAppTelegram, some third-width like YouTubeSpotifyChrome), one accent-colored tile (the red CAMERA — CAPTURE EVERYTHING block in the mockups) as a visual anchor
- Bottom row + Apps tile (opens App Drawer) + a quotetagline tile + a small DISCIPLINE BUILDS FREEDOM. caption tile

### 4.2 App Drawer
- Full numbered list (01, 02, 03…) of pinnedfrequent apps with an arrow affordance, scrollable to all installed apps below
- A quote tile and an accent-colored callout tile (e.g. MAKE SHIT HAPPEN.) in the side column
- Quick-tool shortcuts row (AIFilesCalcMaps or whatever the user pins) — these should be configurable app shortcuts, not fixed to specific packages
- Footer versionbarcode-style branding tile

### 4.3 Focus Mode (third swipeable page)
- Reuses the dayweather tiles from Home
- A curated, user-editable subset of apps in a compact two-column list format
- A full-bleed photoimage tile (user-supplied image, stored locally)
- An accent callout tile (LESS SCROLLING MORE DOING.)
- v1 scope note Focus Mode is a layout, not an enforcement mechanism — it does not block or time-limit apps in v1. Real usage-limiting requires `UsageStatsManager`Accessibility permissions, which add significant complexity and Play Store scrutiny — treat as a stretch goal, not part of the initial build.

### 4.4 Settings
Must be a real, functional screen backed by DataStore, not a mockup
- Theme colour (see §5)
- Dark mode toggle
- Weather location (manual entry, with optional GPS auto-detect behind a runtime location permission prompt)
- Tile animation style (see §6)
- Haptics toggle
- Icon style Text Only  Icon Only  Icon + Text

## 5. Theming engine (build all of this from day one, not just the default theme)

Define a `ThemeConfig` data class `id`, `background`, `tileFill`, `accent`, `textColor`, `useTexture Boolean`.

Ship these themes, matching the mockups exactly
 Theme  Feel 
------
 Mono (default)  Off-white paper background, black textborders, red accent tiles — the core brutalist look 
 Blue  Deep navyblue gradient tiles, Windows Phone Metro feel 
 Red  Same structure, red-dominant tile fills 
 Green  Green-dominant tile fills 
 Purple  Purple-dominant tile fills 
 Orange  Orange-dominant tile fills 

Theme swapping should be instant (recomposition via a `CompositionLocal` for theme, not an activity restart).

## 6. Animation system

Define an `AnimationStyle` enum with three options, all real (not just the default)

1. Tap Flip — tapping a tile flips it 180° on the Y-axis to briefly reveal a back (e.g. app category label) before launching. Implement via `Modifier.graphicsLayer { rotationY = angle; cameraDistance = 12  density }`, swapping content at 90°.
2. Windows 8.1 style 3D rotate — page-to-page transitions (Home ↔ Drawer ↔ Focus) use a 3D cubecarousel rotation rather than a slide, evoking classic Metro page transitions.
3. Smooth transitions — simple crossfadeslide, no 3D — this should be the reduced motion  low-effort option.

Technical requirements
- Use spring-based animation (`spring(dampingRatio, stiffness)`), not fixed-duration tweens, so motion feels physical at high refresh rates.
- Read the actual panel refresh rate (`Display.getRefreshRate()`, or `Surface.setFrameRate()` on API 30+) rather than assuming 60fps — this is the whole point of building for 120Hz hardware.
- All three styles must be genuinely switchable at runtime from Settings, since theminganimation choice is a first-class v1 feature per this spec.

## 7. Data layer

- Weather real API (e.g. OpenWeatherMap) via Retrofit, refreshed by WorkManager on a reasonable interval (15–30 min), cached in DataStore so the Home tile never shows stale-blank data on cold start.
- Apps `PackageManager` + `LauncherApps` API, cached in a Room table for fast drawer scrolling, invalidated by the installuninstall broadcast receiver.
- Notification badges (stretch, not v1-blocking) `NotificationListenerService` + its special permission — flag clearly in-app why this permission is being requested, since it's a sensitive one for Play Store review.

## 8. Permissions checklist

- `ACCESS_FINE_LOCATION`  `ACCESS_COARSE_LOCATION` — only if auto weather-location is used; must be requested at time-of-use with a clear rationale, and the app must work fine if denied (manual location entry fallback).
- `INTERNET` — weather.
- Notification listener permission — stretch feature only, request separatelylater, never bundled into first-run onboarding.
- No other sensitive permissions needed for v1.

## 9. Non-goals for v1 (explicitly out of scope — do not build these unless asked)

- Third-party widget hosting (embedding other apps' widgets via `AppWidgetHost`)
- Icon pack support  custom icon theming beyond the three icon-style modes above
- Gesture customization (swipe actions, double-tap-to-lock)
- App usage blocking  screen-time enforcement in Focus Mode
- Cloud sync of settingslayout across devices

## 10. Suggested internal build order

Even though this is one build (not phased across separate prompts), sequence the work in this order to avoid a half-broken result

1. Project scaffold, HOME intent-filter, basic real app grid (no theminganimation yet) — confirm it can be set as default launcher and launches real apps
2. Theming engine wired to all 6 themes + Settings screen
3. Animation system (all 3 styles) wired to page transitions and tile taps
4. Live weather + time data layer
5. Focus Mode layout + App Drawer polish
6. Icon style modes, haptics, dark mode toggle, texture overlay toggle

---

When handing this to GLM 5.2  Claude Fable 5.1 paste this whole document as the initial project brief, then work through section 10's order as separate follow-up promptscommits rather than asking for everything in one generation pass — even a capable coding model does better with a working scaffold to iterate on than a single mega-prompt.
