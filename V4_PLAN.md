# V4_PLAN.md — STANDARD. v4.0 "Paper & Metro"

Goal: make the launcher look like the five mockups, feel like a Lumia at 120Hz, and be fully configurable.
Verified on the real device: OnePlus CPH2653, Android 16, 1440x3168 @ 120Hz.

## What the audit found (confirmed on device screenshots)

| # | Problem | Where |
|---|---------|-------|
| 1 | Every tile is a solid black block with white text. Mockups use paper-fill tiles with a heavy black outline and black text; only weather / quote / footer are solid "ink", one tile is red accent. The app is the visual inverse of its own design. | Tile.kt, all screens |
| 2 | Fonts render fake-bold. Both TTFs are variable fonts loaded without variation settings, so Compose picks the Regular instance and synthesizes bold. Headlines look thin. | Type.kt |
| 3 | Pivot header ("home apps focus") is drawn under the status bar. It overlaps the clock and its taps are swallowed by the system bar. | StandardApp.kt |
| 4 | Cannot swipe from Apps to Focus: the drawer's pinch-zoom gesture detector consumes single-finger horizontal drags before the pager sees them. | DrawerScreen.kt |
| 5 | Dark mode is a no-op (only tints status icons + 12% overlay). Blue theme is light grey, not the mockup's navy Metro. | ThemeRepositoryImpl.kt, MainActivity.kt |
| 6 | Texture toggle is a no-op; haptics toggle ignored by tile presses. | MainActivity.kt, Tile.kt |
| 7 | Default pins/accent target AOSP packages that do not exist on OnePlus (camera, gallery, files, notes), so the red accent tile never appears. | AppRepositoryImpl.kt |
| 8 | Weather "SET" only renames the label; coordinates stay London. | SettingsScreen.kt |
| 9 | Tile sublabel "OPEN" is clipped and meaningless; mockups use per-app captions ("MESSAGES / CALLS / COMMUNITY"). | Tile.kt |
| 10 | Media tile can never show anything (no notification listener). Quick-settings FAB buttons do nothing. Battery polls every 30s instead of listening. | SystemTiles.kt, MediaRepositoryImpl.kt |
| 11 | Splash is force-cut at 400ms of an 800ms animation and replays on every activity recreation. | StandardApp.kt |
| 12 | Springs are scaled by refresh rate (damping 0.25 at 120Hz), which is physically wrong; springs are already frame-rate independent. | RefreshRate.kt |
| 13 | No Metro signature motion: no tilt-on-press, no turnstile page-in, tile entrance stagger only on first composition, header has no parallax. | Tile.kt, StandardApp.kt |
| 14 | Settings uses Material Switch / Button / OutlinedTextField (rounded, Roboto) on a brutalist app. Missing: pin reorder, captions, quotes, quick tools, focus list, units, 12/24h, columns, home modules, intro toggle, glance toggle, set-default, reset, about. | SettingsScreen.kt |
| 15 | Focus image picked with GET_CONTENT cannot be persisted; disappears after reboot. | FocusScreen.kt |
| 16 | HOME press while inside the launcher does nothing (no onNewIntent → page 0). No set-as-default prompt. targetSdk 34 on an Android 16 device. Room / Retrofit / Navigation dependencies unused (APK 59 MB). | LauncherActivity.kt, build.gradle.kts |

## Phases (an APK is built and installed on the phone after each)

### Phase 1 — Design system foundation
- Type.kt: true variable weights via FontVariation (Space Grotesk 700, JetBrains Mono 400/500/700/800); a named type scale (Display / Headline / Title / Label / Mono).
- ThemeConfig v2: every theme has a light "paper" and a dark "metro" surface (background, outline, ink, onInk, accent, onAccent). Dark mode really inverts. Blue dark = mockup navy.
- Tile v2: `style = Outline | Ink | Accent | Photo`, 2.5dp square border, text color from style, title/caption/index/arrow slots. No hardcoded Color.White anywhere.
- Texture: stronger paper grain, strength slider, drift toggle, applies to Settings too.
- Status/nav bar icon color derived from surface luminance; header gets status-bar inset; edge-to-edge done right.

### Phase 2 — Home to mockup
- Hero row: giant day-name tile (FRI / SEP 06, 2026) + ink weather tile (city, temp 64sp, condition, H/L, humidity, wind, live time).
- 4-column span grid: SMALL = 1 col, MEDIUM = 2 cols, WIDE = 4 cols, LARGE = 4 cols tall. Greedy row packing, keyed by package.
- Per-app captions (default map for common apps, editable), accent tile uses its own caption.
- Bottom row: "+ APPS", quote tile, caption tile. Quotes rotate from an editable list.
- Search, media, battery, people, quick-settings become optional Home modules (default off) so the mockup grid is the default.
- Edit mode: tiles scale down + jiggle, DONE bar slides in, size grip, move-left/right chevrons for reorder, unpin.

### Phase 3 — Drawer + Focus to mockup
- Drawer: giant "GOOD APPS BETTER DAYS." headline + ink weather, numbered 01.. rows with → arrow, right column (MAKE SHIT HAPPEN accent, quote, date, QUICK TOOLS 2x2), full A-Z list with bold letter headers, barcode footer. Brutalist search field. Fix the pinch gesture so paging works; big draggable alphabet scrubber.
- Focus: "SUN 06 SEP" + "DISCIPLINE CREATES FREEDOM.", weather+quote, tall photo tile beside numbered two-column app list with one-word captions, LESS SCROLLING accent, 2x2 grid, footer. Focus list editable. Image persisted via OpenDocument.

### Phase 4 — Motion system
- Remove refresh-rate spring scaling; keep 120Hz frame-rate hint.
- Tile tilt toward touch point + press scale (spring), Metro-style.
- Page transitions: TAP FLIP = WP turnstile page-in with per-tile stagger; WINDOWS 8.1 = refined cube with depth/alpha; SMOOTH = fade+slide (reduced motion).
- Staggered tile entrance every time a page becomes visible; pivot header parallax + sliding underline.
- Live tiles: weather cycles 3 frames, quote tile cycles quotes, day tile flips to time.
- Splash plays fully, only on cold start, can be disabled. Settings panel slides up with spring. Theme change crossfades all colors.

### Phase 5 — Settings rebuild
- Brutalist rows: SectionHeader, ValueRow (label · value › cycles on tap), ToggleRow (square indicator), SwatchRow (named themes), TextRow (bordered mono input), ActionRow.
- Sections: APPEARANCE, HOME, TILES (pinned list: reorder, unpin, caption, accent w/ NONE), MOTION, CLOCK & WEATHER (geocoded city search + GPS, C/F, 12/24h), DRAWER (quick tools ×4), FOCUS (focus apps, quotes editor), SYSTEM (set as default launcher, notification access, reset), ABOUT.

### Phase 6 — Platform + cleanup
- HOME press → page 0 + close overlays. Set-as-default via RoleManager on first run. Predictive back. targetSdk 35.
- Battery via ACTION_BATTERY_CHANGED receiver. Media via NotificationListenerService (real now-playing + transport), opt-in. Quick settings real (Wi-Fi/Internet panel, Bluetooth settings, DND with policy access, Night = dark mode).
- Remove Room / Retrofit / Navigation deps, dead code (static NoiseOverlay, duplicate TileSize, unused zoom state), version from BuildConfig.
- Onboarding in brand fonts: pick theme, set default, gesture hints.
