# NEXT_PHASE_PLAN.md — STANDARD. v1.3 "Lumia"

Windows Phone Lumia-grade smoothness + tile system. 10 features, build-order batched.

## L1. TileSize system (small / medium / wide / large)
Add `TileSize { SMALL, MEDIUM, WIDE, LARGE }` enum + `tileSize` field on AppItem. Map to dp: SMALL=76 (1 col unit), MEDIUM=160 (2x1), WIDE=full-width 160, LARGE=full-width 240. Store per-app size in SharedPreferences keyed by packageName. Default MEDIUM.

## L2. Edit mode (long-press home background → resize/reorder)
Long-press empty Home area toggles `editMode`. In edit mode: tiles jiggle (rotation sin wave), show size arrows on each tile tap (cycle SMALL→MEDIUM→WIDE→LARGE), drag to reorder (persist new pinned order). Exit button top-right.

## L3. Lumia overscroll bounce
Replace `verticalScroll` with a custom `scrollable` using `rememberScrollState` + `Modifier.graphicsLayer { translationY = overscroll * 0.4f }` driven by a `derivedStateOf` on scrollState.value vs maxValue. Edge bounce via spring when released past bounds. Signature Lumia elastic edge.

## L4. Parallax background on scroll
NoiseOverlay + background shifts up at 0.5x scroll rate via `translationY = -scrollState.value * 0.5f` on the background layer. Content scrolls normally. Creates depth (Lumia wallpaper pan feel).

## L5. Live tile content cycling (not just flip)
Upgrade `LiveTile` to cycle through N content frames (not just front/back). `LiveTileCycling(frames: List<@Composable () -> Unit>, interval)` — flips through 2-3 frames with spring. Weather tile cycles: temp → condition → humidity/wind. Clock cycles: time → date → tagline.

## L6. Tile entrance animation (staggered)
On first composition / page enter, tiles animate in with a stagger: each tile `animateFloatAsState` from alpha 0 + translationY 24dp → alpha 1 + 0, delayed by `index * 40ms`. Spring-based. Feels alive on every page open.

## L7. Smooth alphabet jump (drawer)
Tapping a letter header in the drawer smooth-scrolls the LazyColumn to that letter's first item with a spring `animateScrollToItem`. Replace the static section headers with tappable headers that trigger the scroll.

## L8. Semantic zoom (pinch drawer → letter grid)
Two-finger pinch on the drawer collapses the list into a 5-column letter grid (A-Z + #). Tap a letter → zoom back in + scroll to it. Use `transformable` + a `zoomedOut` state.

## L9. Page transition continuum (refine cube)
Refine the cube: add a subtle `translationZ` + `alpha` ramp so pages feel like they recede into depth, not just rotate. Increase cameraDistance slightly. Add a 200ms scale-down on the leaving page.

## L10. Performance: stable Compose
Key all LazyColumn/Pager items by `packageName`. Use `derivedStateOf` for scroll-bound calculations. Move icon LruCache to a process-wide singleton (already done). Ensure no allocations in `graphicsLayer` lambdas.

## Build order (batch by file ownership)
- **Batch A:** TileSize enum + AppItem field + persistence (domain + AppRepositoryImpl)
- **Batch B:** LiveTileCycling + Tile entrance + edit mode (Tile.kt + HomeScreen.kt)
- **Batch C:** Lumia overscroll + parallax (HomeScreen.kt + DrawerScreen.kt)
- **Batch D:** Drawer semantic zoom + smooth alphabet jump (DrawerScreen.kt)
- **Batch E:** Page continuum (StandardApp.kt)
