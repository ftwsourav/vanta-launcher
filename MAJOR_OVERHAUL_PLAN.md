# MAJOR_OVERHAUL_PLAN.md — STANDARD. v3.0 "Lumia Pro"

12 transformations. Going all-in. Skip safe iteration. Build a launcher that turns heads.

## TIER 1: TRANSFORMATIVE

### 1. People Hub contact tile
Favorite contacts as 3-column tiles with auto-generated initial avatars (colored bg from hash of name), tap → dial. Reads `ContactsContract.Contacts.CONTENT_URI` with permission prompt. Shows on Home when contacts exist, otherwise hidden.

### 2. Media Now-Playing tile
Listens for `MediaSession` updates via `NotificationListenerService` polling (no special permission needed for now-playing metadata via session). When music plays: shows track/album/artist + transport controls (skip/play-pause). Hidden when no active media session.

### 3. Battery tile with progress ring
Uses `BatteryManager.BATTERY_PROPERTY_CAPACITY`. Tile is square with a custom-drawn circular progress arc using Canvas, percentage centered, charging bolt glyph when plugged in. Updates every 30s via coroutine.

### 4. Quick Settings floating action button
Bottom-right FAB expands into a radial menu (Wifi/Bluetooth/DND/Night) using WifiManager/BluetoothAdapter toggle APIs. NO permissions needed beyond BLUETOOTH_CONNECT for Android 12+ (graceful degrade if denied).

### 5. Animated drifting noise texture
The NoiseOverlay gets a slow horizontal drift driven by a frame-driven Animatable on the shader offset. Lumia homescreen feel — subtle but you notice it immediately.

## TIER 2: WOW POLISH

### 6. Edge-swipe gesture for drawer
Left-edge swipe opens DrawerScreen over HomeScreen, like WP's peck-from-edge gesture. Uses `detectHorizontalDragGestures` with edge-detection modifier. Drag right = open drawer; drag left from drawer = close.

### 7. Semantic letter-grid zoom in drawer
Pinch out (pinch fingers together) in the drawer zooms out to a letter grid showing A-Z and #, each letter tappable → animate back to normal + scroll to that letter. Vertical pinch + spring on expansion.

### 8. Edit-mode resize grip triangles + size label
In edit mode, each tile shows a small corner triangle (corner-tap target) plus an overlay text "SMALL/MEDIUM/WIDE/LARGE" briefly after size cycles. Triangles glow accent color.

### 9. Floating search bar on Home
Above the day tile (or accessible via FOCUS area), an expandable search field. Tap expands inline, types filter pinned apps live with realtime matches rendered below as pill chips. Hides on collapse.

### 10. Time-of-day Mono palette tint
A coroutine samples LocalTime every minute; when hour crosses dawn/dusk/noon/midnight thresholds, `animateColorAsState` shifts background/tileFill/accent toward warmer or cooler tones within the Mono palette. Subtle — backgrounds drift from off-white paper at noon to amber at sunset to charcoal-blue at midnight.

### 11. Cinematic intro animation
Replace the static S splash with a 60-frame 800ms sequence:
- Frame 1-20: black field, "S." mark appears with stroke growth + grain noise burst
- Frame 21-40: shards fall into tile grid layout positions
- Frame 41-60: scale up to full screen with crossfade to actual content

### 12. Glance screen (lockscreen clock)
Window-overlay technique: detect phone unlock state, show a translucent clock + date + tagline above the lockscreen using FLAG_SECURE-aware window flags. Optional setting — opt-in only. If WindowOverlay isn't permitted (most devices restrict it post-API 26), gracefully falls back to in-app full-screen glance shown on app foreground.

---
