# Launcher Android API Research

Concrete 2024-era (Android 14+ / API 34) implementation notes for a custom launcher built in Kotlin + Jetpack Compose. Sources cited are official Android docs, AOSP source, or Stack Overflow / dev-blog answers from 2023–2025 only.

---

## 1. AndroidManifest activity — becoming the default HOME launcher

The definitive reference is the AOSP **Launcher3** manifest (`android.googlesource.com/platform/packages/apps/Launcher3/+/master/AndroidManifest.xml`, minSdk 30 / targetSdk 33). The HOME activity uses `launchMode="singleTask"` plus several stack-control attributes:

```xml
<activity
    android:name=".Launcher"
    android:launchMode="singleTask"
    android:clearTaskOnLaunch="true"
    android:stateNotNeeded="true"
    android:windowSoftInputMode="adjustPan"
    android:screenOrientation="unspecified"
    android:configChanges="keyboard|keyboardHidden|mcc|mnc|navigation|orientation|screenSize|screenLayout|smallestScreenSize"
    android:resizeableActivity="true"
    android:resumeWhilePausing="true"
    android:taskAffinity=""
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

- **Intent filter:** `android.intent.action.MAIN` + `android.intent.category.HOME` + `android.intent.category.DEFAULT`. The `DEFAULT` category is what lets the system resolve the intent without an explicit component (developer.android.com/guide/topics/manifest/intent-filter-element). This is the exact pattern that makes the system offer your app in Settings → Default apps → Home app.
- **launchMode:** `singleTask`. This is the recommended mode for launcher/entry-point activities per Android's Tasks & back stack docs (developer.android.com/guide/components/activities/tasks-and-back-stack). A 2024 Stack Overflow thread confirms it (stackoverflow.com/questions/79124054, "Android Launcher activity and the launch mode", Oct 2024): `singleTask` avoids recreating the launcher on repeated Home presses; removing it (default `standard`) causes `onCreate` to run and the splash to replay on every launch.
- **meta-data:** **None are required** to be a selectable Home app. AOSP Launcher3 includes a `com.android.launcher3.graphics.control` meta-data only for its own grid extension, not for HOME resolution. (A benefit is available: system apps get `com.android.launcher.permission.READ_SETTINGS`/`WRITE_SETTINGS` — these are **signature-level** AOSP permissions not usable by third parties.) Do not add `BIND_HOME` (see §5).
- **Optional but recommended extras** for the brutalist look: `stateNotNeeded`, `clearTaskOnLaunch` and `taskAffinity=""` are standard AOSP launcher attributes; they keep the task clean in Recents and avoid a stale back stack (per AOSP source above).

**Both HOME and LAUNCHER in one app:** To also appear in the apps drawer, a second activity uses `MAIN`+`LAUNCHER` (stackoverflow.com/questions/37807389). Keep them as separate activities; do **not** merge HOME+LAUNCHER into one filter.

---

## 2. Listing installed apps — PackageManager vs LauncherApps

- **For a home launcher, `LauncherApps` is the recommended modern API.** It's designed for launchers — it returns `LauncherActivityInfo` (label, icon, `ComponentName`) directly, and handles icon resolution (including adaptive-icon foreground/background/monochrome) correctly through `getIcon(..., density, options)` / `getShortcutIcon`. It also delivers real-time package install/uninstall callbacks via `registerCallback`, which is the natural fit for a Compose launcher. (developer.android.com/reference/android/content/pm/LauncherApps; official docs mark LauncherApps specifically for launcher/launching use cases.)
- **PackageManager still works but you must fight package visibility (API 30+).** On Android 11+ the system filters which apps your app can see by default (developer.android.com/training/package-visibility). `queryIntentActivities()` returns **only** apps whitelisted by your `<queries>` block (developer.android.com/training/package-visibility/declaring). The de-facto launcher whitelist is:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

This returns nearly all user-installed apps (those with a launcher activity) **without** the sensitive `QUERY_ALL_PACKAGES` permission (code-examples.net/en/q/4bd7da3 "The ACTION_MAIN Queries Workaround", Nov 2025). **Gotcha:** apps with no launcher activity (headless system services, component-only apps) are hidden — that's expected and why `LauncherApps` + `LauncherApps` icon/query `getInstalledProfiles`/`getActivityList` is cleaner for a real launcher.
- **`QUERY_ALL_PACKAGES` gotchas:** It's a "sensitive" permission. Per the accepted Stack Overflow answer (stackoverflow.com/questions/60679685, "What does QUERY_ALL_PACKAGES permission do?", CommonsWare, accepted 2020, updated 2024): you can declare it in the manifest, but Google Play restricts it to apps whose **core function** needs broad visibility (search, antivirus, file managers) — using it "will eventually be banned from the Play Store by a bot." (Google Play policy: support.google.com/googleplay/android-developer/answer/10158779.) A launcher's core function *is* app inventory, so `QUERY_ALL_PACKAGES` is defensible, but the `<queries>` ACTION_MAIN block is the pragmatic, review-safe route.
- **Do not declare `getInstalledPackages()`-style full enumeration** — use the query/LauncherApps query above; `getInstalledPackages` is also filtered on API 30+ (same CommonsWare answer).

**Recommended Compose approach:** Use `LauncherApps` (`getActivityList` per user profile → `LauncherActivityInfo`), and drive UI updates with `registerCallback` (package added/removed). Fall back to `PackageManager.getLaunchIntentForPackage` only for launching (see §3).

---

## 3. Launching an app from a Compose tap handler

Best modern approach — resolve the MAIN/LAUNCHER intent and start it; handle `ActivityNotFoundException` gracefully.

```kotlin
val pm = context.packageManager
val intent = pm.getLaunchIntentForPackage(componentName.packageName)  // returns MAIN+LAUNCHER intent
    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
```

- `getLaunchIntentForPackage()` returns the resolved `MAIN`/`LAUNCHER` intent for the target package (official `PackageManager` API; verified in Stack Overflow launcher samples, e.g. stackoverflow.com/questions/57600997). It internally queries the launcher activity of the package, so it pairs naturally with the `<queries>` ACTION_MAIN block in §2.
- **ActivityNotFoundException handling:** `startActivity` may throw `ActivityNotFoundException` if the target is disabled, missing, or hidden. Standard pattern (and the only reliable way to avoid a crash):

```kotlin
try {
    context.startActivity(intent)
} catch (e: ActivityNotFoundException) {
    // target uninstalled/disabled — refresh the list via LauncherApps callback
    Log.w("Launcher", "App not found", e)
}
```

- **Alternative for a Compose `clickable`:**
```kotlin
Modifier.clickable {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) { /* no-op */ }
}
```
- **Do not set a component explicitly** unless you have a valid `ComponentName`; using `getLaunchIntentForPackage` (implicit) lets the system choose the MAIN/LAUNCHER resolution and avoids launching non-launcher activities.
- **Background-start restriction:** if the tap happens while your app is in the foreground (normal launcher use), the activity starts fine. Only if your launcher tries to start activities from the background (app not in foreground) does the API 29+ background-activity-start restriction apply (developer.android.com/guide/components/activities/background-starts). Keep the launcher foreground and you avoid it.
- **Deprecated:** `startActivityForResult`/`onActivityResult` is deprecated; use the modern `ActivityResultLauncher` (androidx.activity) if you ever need a result. For a launcher's open-app-only tap you don't need a result.

---

## 4. Receiving PACKAGE_ADDED / PACKAGE_REMOVED on Android 14+

- **They still work, but you must register a runtime receiver with the export flag** — manifest-declared receivers for `ACTION_PACKAGE_ADDED` are **not reliably delivered on API 33+ unless `android:exported="true"` and, critically, the `android:exported` attribute is mandatory for any manifest receiver with an intent filter targeting the system** (developer.android.com/guide/topics/manifest/receiver-element). Since API 26, non-exported manifest receivers with intent filters targeting the system are blocked; and since **API 33 (Android 13)**, the `android:exported` attribute is **required** for any manifest-declared receiver that has an intent filter (Android 13 behavior change, developer.android.com/guide/topics/manifest/receiver-element). So `ACTION_PACKAGE_ADDED` **is** delivered to a manifest receiver only if `android:exported="true"` is set.
- **The 2024-era reality (Android 14):** the high-voted 2024 Stack Overflow answer (stackoverflow.com/questions/76919130, "Android 14 context registered broadcast receivers not working", Sep 2023, accepted, CommonsWare) documents that **all runtime-registered receivers now require `RECEIVER_EXPORTED` or `RECEIVER_NOT_EXPORTED`** (Android 14 behavior change, developer.android.com/guide/components/activities/background-starts; the medium post "Changes to BroadcastReceiver in Android 14", malikfarooq4321, 2023, and developermemos.com/posts/fixing-broadcast-receiver-error-android-14, 2023, all agree). For system broadcasts like `PACKAGE_ADDED`, use `RECEIVER_EXPORTED` (a system broadcast, not a private app broadcast).
- **Manifest-declared receiver that works:**
```xml
<receiver android:name=".AppInstallReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.PACKAGE_ADDED" />
        <action android:name="android.intent.action.PACKAGE_REMOVED" />
        <data android:scheme="package" />
    </intent-filter>
</receiver>
```
The `<data android:scheme="package"/>` is required to receive package-scoped broadcasts (Stack Overflow answers, e.g. stackoverflow.com/questions/10888768, note the PACKAGE_ADDED vs PACKAGE_FULLY_REMOVED distinction).
- **But the recommended modern approach for a Compose launcher is `LauncherApps.registerCallback`, not raw broadcasts.** `LauncherApps` provides install/uninstall callbacks (developer.android.com/reference/android/content/pm/LauncherApps) — it fires for the current user's profile and gives you the `ComponentName`/density to reload icons — which is exactly what a launcher needs and avoids the export-flag / lifecycle headaches.
- **`ProcessLifecycleOwner` is not the mechanism.** `androidx.lifecycle.ProcessLifecycleOwner` tracks process foreground/background state; it is **not** a broadcast receiver and won't receive `PACKAGE_ADDED`. (The question conflates two unrelated things.) You should **not** use it to observe package events. Use `LauncherApps.registerCallback` (§2) or, if you prefer broadcasts, a `RECEIVER_EXPORTED` runtime receiver registered in `onStart`/`onStop` of your Compose activity.

---

## 5. Required permissions to be a default launcher

- **The `HOME` category is sufficient; no `BIND_HOME` permission is needed.** `BIND_HOME` is a **signature-level** AOSP permission (declared in `packages/apps/Launcher3/AndroidManifest.xml`) that **only the system** can grant to its own Launcher3. Third-party apps **cannot** hold it and should **not** declare it — declaring a signature permission you don't own is rejected at install / ignored. The accepted Stack Overflow launcher answers (stackoverflow.com/questions/37807389, stackoverflow.com/questions/57600997) confirm a plain `MAIN`+`HOME`+`DEFAULT` activity with no special permission is enough to be selectable as Home.
- **Similarly, `com.android.launcher.permission.READ_SETTINGS` / `WRITE_SETTINGS` are signature-level AOSP launcher permissions** not available to third-party apps (AOSP source). Don't add them.
- **You do need other normal permissions depending on features:**
  - To be listed as Home you need nothing beyond the HOME filter.
  - To read the wallpaper (`peekBitmap`, §6) you need **no permission** — `WallpaperManager` access is permission-free (official docs).
  - To *set* a wallpaper you'd need `SET_WALLPAPER` (a normal permission); `isSetWallpaperAllowed()` gates it (developer.android.com/reference/android/app/WallpaperManager). For a launcher that draws its own background you won't set it.
  - If you use `QUERY_ALL_PACKAGES` (not recommended, §2), declare `android.permission.QUERY_ALL_PACKAGES` — with the Play Store policy risk.

**Bottom line:** A `HOME` activity with `exported=true` and the `MAIN`/`HOME`/`DEFAULT` filter — nothing else — makes you selectable as the default Home app.

---

## 6. Persistent wallpaper — own background vs system wallpaper

- **Recommendation: draw your own background, optionally layering the system wallpaper underneath.** For the brutalist paper-grain overlay aesthetic, the strongest approach is:
  1. Read the system wallpaper via `WallpaperManager.peekBitmap()` or `getDrawable()` (permission-free; official `WallpaperManager` API, developer.android.com/reference/android/app/WallpaperManager; Stack Overflow sample in techblogs.42gears.com "How to programmatically get Android launcher wallpaper in your app", 2022 — still valid 2024):
  ```kotlin
  val wm = WallpaperManager.getInstance(context)
  val bitmap = wm.peekBitmap() ?: wm.drawable.toBitmap()
  ```
  2. Draw a **paper-grain overlay** on top: a high-frequency, low-contrast noise texture (use `RenderEffect`/`Bitmap` with a seeded `Noise` via `BitmapShader`, or bundle a pre-authored grain asset in `res/drawable`) composited with `android.graphics` blend modes — e.g. `PorterDuff.Mode.OVERLAY` / `MULTIPLY` — over the wallpaper bitmap. `peekBitmap` gives a static snapshot (does not animate with live wallpapers), which is exactly right for a persistent static grain texture.
  3. Use `WallpaperManager.draw(Canvas)` on the window via `windowBackground` + `windowShowWallpaper` to let the system draw the wallpaper for you **under** your translucent Compose content:
  ```xml
  <style name="Theme_Wallpaper">
      <item name="android:windowBackground">@android:color/transparent</item>
      <item name="android:windowShowWallpaper">true</item>
  </style>
  ```
  With `windowShowWallpaper`, the activity's window shows the wallpaper behind transparent `windowBackground` (Stack Overflow launcher answers; techblogs.42gears.com). Your Compose root draws only the grain overlay, keeping the brutalist look while preserving the user's chosen wallpaper.
- **Live-wallpaper caveat:** `peekBitmap` is a **static snapshot** of the wallpaper bitmap — it won't show live-wallpaper animations. For a persistent grain texture that's fine; for an animated brutalist effect you'd instead set a **live wallpaper** (a `WallpaperService`) — but that requires `SET_WALLPAPER` and `isSetWallpaperAllowed()`; a launcher normally just draws its own background and doesn't set a wallpaper at all.
- **Memory:** `peekBitmap` returns a large bitmap; cache it once and scale to screen size rather than re-decoding per frame. For the grain overlay, a small tileable texture (e.g. 128×128 noise) is enough and cheap.
- **Permission:** none needed to read or draw the wallpaper. Setting requires `SET_WALLPAPER` (normal, with `isSetWallpaperAllowed`).

**Final approach:** `WallpaperManager` snapshot → base layer; a bundled/`Res` grain asset composited with `MULTIPLY`/`OVERLAY` → overlay; Compose content on top. This is the standard modern launcher pattern and matches the brutalist paper-grain brief.

---

## 7. Back-button behavior, preventing accidental launches, Recents

Standard launcher patterns (per AOSP Launcher3 and modern Compose / activity back-handling answers):

- **Home is the root — Back should not exit the app.** As a `HOME` activity with `launchMode="singleTask"`, your launcher is the root of its own task. Pressing Back from the launcher should **do nothing** (or return to the previous task). The AOSP pattern sets `stateNotNeeded` + `clearTaskOnLaunch` so the launcher never accumulates a back stack; the 2024 Stack Overflow (stackoverflow.com/questions/79124054) explains why `singleTask` is preferred and how removing it causes splash replay.
- **Handle Back in Compose / activity:**
```kotlin
// in Activity
override fun onBackPressed() {
    // Do nothing — launcher is HOME root; don't finish()
}
```
For Android 13+ the deprecated `onBackPressed` is replaced by `OnBackPressedDispatcher` (androidx.activity); register a callback that ignores Back:
```kotlin
onBackPressedDispatcher.addCallback(this) {
    // no-op: launcher is root HOME task
}
```
- **Preventing accidental launch of other activities:** Never launch other activities implicitly from the launcher except through an explicit `getLaunchIntentForPackage`/`MAIN`+`LAUNCHER` resolution (which only opens the target's main/launcher activity, never a random activity). Standard Stack Overflow launcher answers warn about this (launching via implicit intent can open the wrong screen or the system Home). Keep `startActivity` calls explicit to the launcher entry point.
- **Recents behavior:** A `HOME` launcher normally appears in Recents as a normal recent task. Standard launcher pattern (AOSP Launcher3) sets `launchMode="singleTask"` + `taskAffinity=""` + `clearTaskOnLaunch` — the launcher is a single clean task, so Recents shows the other app tasks, not a stack of launcher copies. To **exclude** your launcher from Recents entirely, add `android:excludeFromRecents="true"` to the HOME activity — but note some launchers (including AOSP) keep it visible so the Home button / gesture switch is smooth. There is no requirement that a launcher hide itself from Recents.
- **Recent-apps gesture (gesture nav):** On Android 10+ gesture navigation, the Home gesture / Recents swipe both resolve to the `HOME` intent → your `HOME` activity (singleTask root). There's nothing special to implement beyond being a valid HOME target with `singleTask`; the system restores your task via the task state and `stateNotNeeded`/`resumeWhilePausing` (AOSP attributes) keep switching fast and jank-free.
- **`finishAffinity()`** (used in some singleTask clearTop patterns, per the Kai deep-engineering notes xckevin.com, 2026) is for clearing a stale launcher task; not needed with the AOSP attribute set.

**Bottom line:** Ignore Back (root task), launch only the MAIN/LAUNCHER entry point explicitly, keep `singleTask` + `taskAffinity=""` for a clean single task, and optionally `excludeFromRecents`.

---

## 8. Adaptive icon spec for our own app

The **adaptive icon** spec (`mipmap-anydpi-v26`) structure — official spec (developer.android.com/develop/ui/views/launch/change-icon; the "Adaptive icons" / "create a launcher icon" docs; the `adaptive-icon` reference at `developer.android.com/develop/ui/views/launch/launching`):

- **Structure:** a `res/mipmap-anydpi-v26/ic_launcher.xml` XML pointing to two drawable layers — **foreground** and **background** — each sized at **108×108 dp**, with the visible/safe zone being the inner **66×66 dp** circle (`signed `maskableIcon`/maskable` region). The adaptive-icon root must be a plain vector/drawable asset, not a bitmap (except `monochrome`).

```xml
<!-- res/mipmap-anydpi-v26/ic_launcher.xml -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
```

- **Foreground layer:** the visible artwork; draw **within the 66×66 dp safe zone** (the "safe zone" that is never masked); keep the outer 21 dp margin (108→66 = 21 dp each side) clear because it gets masked off by launcher shapes (circle, squircle, rounded square, teardrop). Your logo/art must not rely on the outer 42 dp.
- **Background layer:** a full 108×108 fill — can be a solid color or gradient tile; it's the base behind the masked shape and shows through the transparent parts of the foreground.
- **Monochrome (Android 13+, API 33):** an optional single-color (alpha-only) layer used for themed launcher icons (`android:theme` / `themed icons`); draw the same glyph as an **alpha mask** so launchers can recolor it. (Official spec: `adaptive-icon` `monochrome` added in API 33 — developer.android.com/develop/ui/views/launch/change-icon.) Since minSdk is 30, keep a **legacy** bitmap fallback in `mipmap-mdpi`…`mipmap-xxxhdpi` (or a single `mipmap-anydpi-v26` without maskable for pre-26, plus `mipmap-*dpi` bitmap for API < 26) — but on API 34 the `anydpi-v26` adaptive-icon is what's used.
- **Size / ratio:** 108×108 dp layer canvas, safe zone 66×66 (≈61% ratio), 21 dp masked margin. The spec is: **foreground and background both 108×108; artwork inside 66×66.** `roundIcon` attribute on `<application>` can point to the same adaptive icon.
- **For the brutalist brand:** draw the grain as a noise/paper texture in the **background layer** (repeating 108×108 tile) and your wordmark/glyph in the **foreground** inside the 66×66 safe zone; add a `monochrome` alpha version. Keep the outer 21 dp empty so launcher masking doesn't crop your art.

**AOSP Launcher3** itself uses `@drawable/ic_launcher_home` (a `maskableIcon`/adaptive `home` icon) and resolves icons via `LauncherApps`/`IconFactory` — confirming the modern icon pipeline is adaptive-icon-aware (`LauncherApps` `IconFactory` — official launcher icon resolution — developer.android.com/reference/android/content/pm/LauncherApps `IconFactory`).

---

## Quick build config (Kotlin + Compose, API 34)

```gradle
android {
    compileSdk = 34
    defaultConfig {
        minSdk = 30
        targetSdk = 34
    }
}
```
Use `androidx.compose`, `androidx.activity:activity-compose`, and `androidx.lifecycle:lifecycle-runtime`; there is no dependency needed for the HOME filter, and the export-flag/lifecycle confusion from §4 is resolved by `LauncherApps` + runtime `RECEIVER_EXPORTED` receivers rather than `ProcessLifecycleOwner`.