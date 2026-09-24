# V2_MASTER_PLAN.md — STANDARD. v2.0

Deep audit of actual codebase. Every item below is a **verified, specific gap** found by reading the code — not a generic suggestion. Each has the file, line, and exact problem.

---

## CATEGORY A: DEAD CODE (features written but never wired in)

### A1. LumiaScroll is dead code — `LumiaScroll.kt`
The Lumia overscroll bounce + parallax composable was written (90 lines) but **never called from any screen**. HomeScreen.kt:87 still uses `verticalScroll(rememberScrollState())`. The Lumia bounce feature was claimed as delivered but is completely unused. **Fix: replace HomeScreen's `verticalScroll` with `LumiaScroll`, pass scroll offset for parallax.**

### A2. LiveTileCycling is dead code — `Tile.kt:278`
The multi-frame cycling live tile was written but **never called**. HomeScreen still uses the old 2-frame `LiveTile`. The weather tile should cycle temp→condition→humidity/wind but doesn't. **Fix: convert the WeatherTile to use `LiveTileCycling` with 3 frames.**

---

## CATEGORY B: BROKEN FEATURES (implemented but don't work)

### B1. WIDE/LARGE tile sizes are broken — `HomeScreen.kt:472-483`
`TileSize.WIDE` and `LARGE` are defined as `full = true` but the grid renders every tile inside a `Row` with `Modifier.weight(1f)` — so a WIDE tile still only takes 1/3 of the row width. A full-width tile is impossible in the current layout. **Fix: when a tile is WIDE/LARGE, it should break out of the 3-column row and span `fillMaxWidth()` on its own row.**

### B2. SMOOTH mode can't navigate to Focus — `StandardApp.kt:100`
In SMOOTH mode, `openDrawer` sets `currentPage = 1`. But there's no `openFocus` — the AnimatedContent only responds to `currentPage`, and nothing ever sets it to 2. In CUBE/TAP_FLIP modes you swipe to Focus, but in SMOOTH mode there's no pager to swipe. **Fix: add navigation from Home→Focus (a Focus button) + drawer→focus.**

### B3. PivotHeader is not tappable — `StandardApp.kt:117`
The pivot headers ("home"/"apps"/"focus") are `Text` labels with no `.clickable`. In real Windows Phone, tapping a pivot header navigates to that section. Here they're decorative only. **Fix: add `.clickable` to each header that scrolls the pager / sets currentPage.**

### B4. Edit mode has no drag-to-reorder — `HomeScreen.kt`
The plan (NEXT_PHASE_PLAN L2) said "drag to reorder (persist new pinned order)". Only size-cycling was implemented. Reordering was claimed but not delivered. **Fix: add long-press-drag on tiles in edit mode that reorders the pinned list and calls `viewModel.setPinned(newOrder)`.**

### B5. Drawer alphabet jump bar claimed but missing — `DrawerScreen.kt`
The plan (L7/L8) said "tapping a letter header smooth-scrolls the LazyColumn" + "semantic zoom". Only static letter grouping was added (`groupBy { first letter }`). The letter headers are not tappable, there's no jump bar on the right edge, and no semantic zoom. **Fix: make letter headers tappable → `animateScrollToItem`, add a right-edge alphabet overlay.**

---

## CATEGORY C: INCONSISTENCIES (parts of the app that don't match the rest)

### C1. Drawer quick-shortcut row uses plain `.clickable` — `DrawerScreen.kt:157`
The 4 quick-shortcut tiles at the top of the drawer use `.clickable` directly, while every other tile in the app uses `pressScale` (press-scale + haptic). They feel dead compared to the rest. **Fix: use `AppTile` or at least `pressScale` for these 4 tiles.**

### C2. `pressScale` fires LongPress haptic on every press — `Tile.kt:216`
`HapticFeedbackType.LongPress` fires on every press-down, including in edit mode where you're cycling sizes rapidly. A long-press haptic on every size cycle is jarring. **Fix: use `HapticFeedbackType.TextHandleMove` (lighter) for tap, keep LongPress only for long-press gestures.**

### C3. Focus image has no remove option — `FocusScreen.kt`
Once you pick a focus image, there's no way to remove it or change it except picking a new one. Long-press should offer "Remove image". **Fix: long-press on the image tile → clear `imageUri` + remove from prefs.**

### C4. No back-button handling for settings overlay — `StandardApp.kt`
The settings overlay is dismissed only by tapping the X icon. Android's back gesture/button does nothing — it should close settings first. **Fix: `BackHandler { showSettings = false }` when settings is visible.**

---

## CATEGORY D: PERFORMANCE / SAFETY

### D1. `drawableToBitmap` can return a huge original — `Tile.kt:267`
For `BitmapDrawable`, it returns `drawable.bitmap` directly — which could be 512×512 for some apps. The LruCache (48 entries) could hold 48 × 512² × 4 = 48MB → OOM risk. **Fix: always scale to 144×144, never return the original bitmap.**

### D2. AppIcon `produceState` not keyed on context — `Tile.kt:232`
`produceState(initialValue, packageName)` — only keyed on `packageName`. If the activity is recreated (config change), the stale icon persists for a package that may have changed its icon. **Fix: add `context` as a key.**

---

## CATEGORY E: NEW FEATURES (high-impact, specific)

### E1. Tile drag-to-reorder in edit mode
Long-press a tile in edit mode → drag it to a new position → release → persist new order via `setPinned`. Use `detectDragGesturesAfterLongPress` + a `dragOffset` state + `graphicsLayer { translationY }` during drag. Reorder the `pinned` list on drop.

### E2. Weather tile as cycling live tile (3 frames)
Convert WeatherTile to `LiveTileCycling` with 3 frames:
- Frame 1: temp + condition + icon (current)
- Frame 2: humidity + wind + location
- Frame 3: 3-day forecast mini-strip
Each frame shows for 3.5s, spring-flip to next.

### E3. Alphabet jump bar (right edge of drawer)
A vertical column of letters (A-Z + #) on the right edge of the drawer, ~20dp wide. Tapping a letter → `lazyListState.animateScrollToItem(firstIndexOfLetter)`. The current letter highlights based on scroll position (`derivedStateOf` on first visible item).

### E4. BackHandler for settings + edit mode
- Settings visible → back closes settings
- Edit mode active → back exits edit mode
- Otherwise → no-op (launcher root)

### E5. Tile size badge in edit mode
When cycling sizes in edit mode, briefly show the size name ("SMALL"/"MEDIUM"/"WIDE"/"LARGE") as an overlay badge on the tile. Fades after 800ms.

---

## BUILD ORDER (batched by file to avoid conflicts)

**Batch 1 — Wire dead code + fix tile layout:**
- A1: LumiaScroll → HomeScreen (replace verticalScroll)
- A2: LiveTileCycling → WeatherTile (3 frames)
- B1: WIDE/LARGE tiles break out of row to fillMaxWidth
- D1: drawableToBitmap always scales to 144
- D2: AppIcon key on context

**Batch 2 — Navigation + edit mode:**
- B2: SMOOTH mode Focus navigation
- B3: PivotHeader tappable
- B4+E1: Drag-to-reorder in edit mode
- E5: Size badge overlay
- C4+E4: BackHandler

**Batch 3 — Drawer + Focus polish:**
- B5+E3: Alphabet jump bar
- C1: Quick-shortcut uses pressScale
- C2: Lighter haptic on tap
- C3: Focus image remove
