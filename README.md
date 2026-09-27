<p align="center"><img src="logos/hero_banner.svg" width="820" alt="Vanta"></p>

# Vanta

> Good apps. Better days.

[![Latest release v5.5](https://img.shields.io/badge/release-v5.5-D93A2B?style=flat-square)](https://github.com/ftwsourav/vanta-launcher/releases/latest)
![Android 11+](https://img.shields.io/badge/android-11%2B-111111?style=flat-square)
![Kotlin · Compose](https://img.shields.io/badge/kotlin%20%C2%B7%20compose-ECE8DF?style=flat-square&labelColor=111111)

**Current version: v5.5 "Hubs"** — [download the APK](https://github.com/ftwsourav/vanta-launcher/releases/download/v5.5/Vanta-v5.5-release.apk) · [release notes](https://github.com/ftwsourav/vanta-launcher/releases/tag/v5.5) · [full changelog](PROGRESS_CHANGELOG.md)

## What's new in 5.5
- **Real live tiles**: with notification access, a tile shows its app's unread count and latest message, badges update the instant a notification lands, and tiles can peek (WP8 slide-up) instead of flip.
- **Inline reply** on the Live page, action buttons on cards, and dismiss that really clears the notification.
- **Focus session**: 25 / 50 / 90 minutes from the Focus page, Do Not Disturb on, non-focus tiles hidden on Home, logged to your notes when it ends.
- **Tile folders** (WP 8.1 inline expand), a **morning brief** tile from 5 to 9 am, editable tile captions and per-tile Outline / Ink / Accent looks.
- **Search that answers**: calculator, unit conversion, contacts with call and message, settings panels, then web.
- **Widgets inside tiles** through a brutalist picker, resizable on the grid.
- **Panorama**: one wide mono photo behind all four pages, moving at a third of the swipe.
- **Nightstand**: the Glance clock shows itself while charging at night after 30 s idle.

## What's new in 5.4
- Tiles turnstile in around the left edge every time you return to the launcher; apps reveal out of their own tile.
- Windows 8.1 edge-pivot, WP7 turnstile and parallax page transitions, all in the draw phase at 120 Hz.
- Edge-swipe gesture actions, Windows Phone toggle switches, semantic-zoom A–Z grid, brutalist quick settings.
- Live page now shows every app's notifications, with swipe-to-dismiss cards.
- New mark: ink tile, paper outline, display-weight V, one red accent square.

A brutalist Android launcher replacement inspired by Windows Phone 8.1 / Lumia Metro UI. Built with Kotlin + Jetpack Compose. Runs on Android 11+ and targets Android 15, tuned for 120 Hz.

**Developer:** [@ftwsourav](https://github.com/ftwsourav)

---

## Download

Grab the latest APK from the [Releases page](https://github.com/ftwsourav/vanta-launcher/releases). Install it on your phone and set as your default launcher.

## Features

### Home Screen
- **Live Tiles** — every tile cycles through frames (icon+label → text-only → icon-only) at random intervals with Lumia-style 3D Y-axis flip
- **Music Widget** — album art, transport controls (⏮ ▶/⏸ ⏭), 5-bar visualizer, queue/favorite/shuffle buttons
- **Tasks Widget** — todo list with checkboxes, add/remove, persisted
- **Notes Widget** — quick notes, last 2 shown, inline add
- **Battery Tile** — animated count-up percentage with charging/low states
- **People Hub** — contacts with initial avatars, tap to dial
- **Weather Tile** — real Open-Meteo data, 3-day forecast, live conditions
- **Floating Search Bar** — expandable, live-filters pinned apps
- **Quick Settings** — pull down from the header for a WiFi/BT/DND/flashlight/rotate/airplane/brightness sheet

### Tile Management
- **Drag-to-reorder** — immediate drag in edit mode, tile lifts with shadow + scale
- **Drag-to-resize** — drag corner triangle to grow/shrink tiles
- **4 Tile Sizes** — SMALL (1×1), MEDIUM (2×2), WIDE (4×2), LARGE (4×4)
- **Turnstile motion** — tiles swing in around the left edge on every return to the launcher, and apps reveal out of their tile
- **Long-press context menu** — Resize/Move/Pin/Remove/App Info/Add Widget

### Navigation
- **4-page pivot** — Home / Apps / Focus / Live with Windows 8.1 edge-pivot, WP7 turnstile or parallax transitions
- **Edge swipe** — pull from either screen edge for your configured gesture action (next/prev page, search, focus, live)
- **Smoothness presets** — SNAPPY / SMOOTH / LUXURIOUS / BOUNCY / GLASS

### Apps Drawer
- **Alphabetical** with letter headers
- **Alphabet scrubber** rail for fast jumping
- **Semantic zoom** — tap a letter header (or pinch) for the Windows Phone A–Z letter grid
- **App icons** shown on every row
- **WP-style app bar** at bottom

### Focus Mode
- **Stopwatch** — count-up timer with progress ring, laps
- **Productivity suggestions** — Keep, Todoist, MS To Do, Evernote
- **Quick notes** — add notes with dialog
- **Focus apps** — 2×2 grid of your focus apps

### Design
- **6 themes** — Mono, Blue, Red, Green, Purple, Orange
- **Dark/Light mode** with animated crossfade
- **Time-of-day palette tint** — colors shift through dawn/day/dusk/night/midnight
- **Icon theming** — Original, Monochrome, Accent-tinted, Text-only, Circle, Rounded-square
- **Wallpaper background** — device wallpaper behind tiles
- **Parallax background** — drifts behind tiles on scroll
- **Drifting noise overlay** — subtle paper grain texture
- **Space Grotesk + JetBrains Mono** typography
- **120fps** support

### Motion
- **Cinematic intro** — 650 ms splash: the Vanta mark scales up through paper grain and wipes into the home page
- **Glance screen** — large clock, tap to dismiss
- **Turnstile transition** — tile scales up to fullscreen on app launch
- **Home return transition** — content fades + scales in smoothly
- **Tile motion** — breathing scale, idle shimmer, scroll-linked 3D tilt, accent pulse
- **Spring animations** everywhere with configurable smoothness

### Settings
- Theme picker, dark mode, paper grain, noise drift
- Time-of-day tint, wallpaper background
- Live tiles, random flip timing, notification previews
- Smoothness picker (5 levels)
- Icon style (6 styles)
- Animation style (Tap-flip / 3D Cube / Smooth)
- Cinematic intro, glance, haptics
- Weather location + units + clock format
- Quotes editor
- Developer credit: @ftwsourav

## Tech Stack

| | |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose (BOM 2024.12) |
| Architecture | MVVM + Repository pattern |
| Min SDK | 30 (Android 11) |
| Target SDK | 35 (Android 15) |
| Compile SDK | 35 |
| Build | Gradle 8.9 |
| Fonts | Space Grotesk + JetBrains Mono |

## Building

```bash
git clone https://github.com/ftwsourav/vanta-launcher.git
cd vanta-launcher/standard-launcher
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## License

This project is open source. Feel free to fork and modify.

---

**Developed by [@ftwsourav](https://github.com/ftwsourav) · GitHub**
