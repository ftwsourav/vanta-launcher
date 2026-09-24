# UPGRADE_PLAN.md — STANDARD. v1.1

Top 10 upgrades, prioritized by visual/UX impact. Each is a self-contained change set.

## 1. Real 3D Cube page transitions (CUBE style actually cubes)
`StandardApp.kt` HorizontalPager currently slides flat. Add per-page `Modifier.graphicsLayer { rotationY = pageOffset * -28f; cameraDistance = 12*density; scaleX = 1 - 0.08*abs(pageOffset) }` driven by `pagerState.currentPage + currentPageOffsetFraction`. Keep TAP_FLIP and SMOOTH as distinct (plain pager / Crossfade via AnimatedContent).

## 2. SMOOTH style = real crossfade (not the same pager)
Route `AnimationStyle.SMOOTH` to `AnimatedContent(currentPage)` with `fadeIn+fadeOut` transitionSpec, bypassing HorizontalPager entirely.

## 3. Real app icons in tiles
`AppTile` loads the actual launcher icon via `LauncherApps.getApplicationIcon(packageName)` (drawable) rendered with Coil or `paintResource`. `IconStyle.ICON_ONLY` shows just the icon; `ICON_TEXT` shows icon + label. Cache icons in `AppRepository` (map `packageName -> Drawable`).

## 4. Animated theme + dark-mode transition
When `themeId` or `darkMode` changes, animate `background`/`tileFill`/`accent`/`text` via `animateColorAsState`. Wrap `LocalAppTheme` in an animated colors holder so the whole UI cross-fades on theme swap instead of snapping.

## 5. Press-scale + haptic on every tile tap
Every `Tile`/`AppTile` gets `Modifier.pointerInput` detecting press → `animateFloatAsState(scale 0.96)` + `performHapticFeedback(LongPress)` on down, spring back on up. Replaces the fixed-delay tap-flip; feels physical at 120Hz.

## 6. Robust adaptive Home grid (no index slicing)
Replace the `if (others.size >= N)` chain in `HomeScreen.PinnedGrid` with a real `LazyVerticalGrid` / flow layout keyed by pinned order, with the accent tile injected at the 5th slot. Handles any count 0–N gracefully.

## 7. Drawer: search bar + app icons + long-press pin/unpin
Add a search `OutlinedTextField` at top filtering `allApps` by label. Each row shows a small app icon. Long-press a row toggles its pinned state (toast confirms). Drawer becomes usable beyond ~20 apps.

## 8. Animated splash + first-run hint
A 400ms brutalist splash (`Box` black with white `S.` mark cross-fading to background) on cold start using `LaunchedEffect`. On first run (DataStore/SharedPrefs flag `first_run`), overlay a one-screen hint card "Swipe ← → for Apps / Focus. Tap ≡ for Settings." that dismisses on first page swipe.

## 9. Weather: pull-to-refresh + auto-refresh on location change
Wrap weather tile in a Material3 `PullToRefreshBox` (or simple `pullRefresh` modifier). When `settings.weatherLocation` changes (distinctUntilChanged in WeatherRepository init), trigger an immediate `refresh()`. Show a small loading spinner instead of "—" while `isLoading`.

## 10. Edge-to-edge theming + animated system bars
Set status bar icons to follow dark-mode (`WindowInsetsControllerCompat.isAppearanceLightStatusBars`), animate `statusBarColor`/`navigationBarColor` to `LocalAppTheme.current.background` via `animateColorAsState`. Removes the jarring white system bars on dark themes.
