package app.vanta.launcher.data.local

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import app.vanta.launcher.data.repo.AppRepository
import app.vanta.launcher.domain.model.AppItem
import app.vanta.launcher.domain.model.TileSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppRepositoryImpl(private val context: Context) : AppRepository {

    private val launcherApps: LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userHandle: UserHandle = Process.myUserHandle()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _allApps = MutableStateFlow<List<AppItem>>(emptyList())
    override val allApps: StateFlow<List<AppItem>> = _allApps.asStateFlow()

    private val _pinnedApps = MutableStateFlow<List<AppItem>>(emptyList())
    override val pinnedApps: StateFlow<List<AppItem>> = _pinnedApps.asStateFlow()

    private val _quickTools = MutableStateFlow<List<AppItem>>(emptyList())
    override val quickTools: StateFlow<List<AppItem>> = _quickTools.asStateFlow()

    private val _focusApps = MutableStateFlow<List<AppItem>>(emptyList())
    override val focusApps: StateFlow<List<AppItem>> = _focusApps.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(callback)
        refresh()
    }

    override fun refresh() {
        scope.launch { rebuild() }
    }

    @Synchronized
    private fun rebuild() {
        val activities: List<LauncherActivityInfo> = try {
            launcherApps.getActivityList(null, userHandle)
        } catch (e: Exception) {
            emptyList()
        }
        val installed: Map<String, LauncherActivityInfo> = buildMap {
            activities.forEach { info ->
                val pkg = info.applicationInfo.packageName
                if (pkg != context.packageName) putIfAbsent(pkg, info)
            }
        }
        if (installed.isEmpty()) return

        if (prefs.getString(KEY_PINNED_ORDER, null) == null) seedDefaults(installed.keys)
        migrateLegacyDefaults(installed.keys)

        val pinnedOrder = csv(KEY_PINNED_ORDER)
        val orderIndex = pinnedOrder.withIndex().associate { it.value to it.index }
        val accent = prefs.getString(KEY_ACCENT, null)?.takeIf { it.isNotBlank() }
        val sizes = loadPrefixed(KEY_TILE_SIZE_PREFIX) { runCatching { TileSize.valueOf(it) }.getOrNull() }
        val captions = loadPrefixed(KEY_CAPTION_PREFIX) { it.takeIf { c -> c.isNotBlank() } }

        val all = installed.map { (pkg, info) ->
            AppItem(
                packageName = pkg,
                label = info.label?.toString() ?: pkg,
                pinned = orderIndex.containsKey(pkg),
                pinnedOrder = orderIndex[pkg] ?: Int.MAX_VALUE,
                isAccent = pkg == accent,
                tileSize = sizes[pkg] ?: TileSize.MEDIUM,
                caption = captions[pkg]
            )
        }.sortedBy { it.label.lowercase() }
        val byPkg = all.associateBy { it.packageName }

        _allApps.value = all
        _pinnedApps.value = pinnedOrder.mapNotNull { byPkg[it] }
        _quickTools.value = csv(KEY_QUICK_TOOLS).mapNotNull { byPkg[it] }.take(4)
        _focusApps.value = csv(KEY_FOCUS_APPS).mapNotNull { byPkg[it] }.take(10)
    }

    // ---- defaults -----------------------------------------------------------------------

    private fun firstInstalled(installed: Set<String>, vararg pkgs: String): String? =
        pkgs.firstOrNull { it in installed }

    private fun resolve(installed: Set<String>, intent: Intent): String? = try {
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
            ?.takeIf { it in installed }
    } catch (e: Exception) {
        null
    }

    private fun category(cat: String): Intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, cat)

    private fun cameraPackage(installed: Set<String>): String? =
        resolve(installed, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            ?: firstInstalled(installed, "com.oplus.camera", "com.android.camera2", "com.android.camera",
                "com.google.android.GoogleCamera", "com.sec.android.app.camera", "com.oneplus.camera")

    private fun galleryPackage(installed: Set<String>): String? =
        firstInstalled(installed, "com.oneplus.gallery", "com.coloros.gallery3d", "com.google.android.apps.photos",
            "com.android.gallery3d", "com.android.gallery", "com.sec.android.gallery3d")
            ?: resolve(installed, category(Intent.CATEGORY_APP_GALLERY))

    private fun filesPackage(installed: Set<String>): String? =
        resolve(installed, category(Intent.CATEGORY_APP_FILES))
            ?: firstInstalled(installed, "com.google.android.apps.nbu.files", "com.google.android.documentsui",
                "com.android.documentsui", "com.coloros.filemanager", "com.oneplus.filemanager")

    private fun notesPackage(installed: Set<String>): String? =
        firstInstalled(installed, "com.google.android.keep", "com.simplemobiletools.notes", "com.oneplus.note",
            "com.coloros.note", "com.samsung.android.app.notes", "com.microsoft.office.onenote")

    private fun aiPackage(installed: Set<String>): String? =
        firstInstalled(installed, "com.openai.chatgpt", "com.anthropic.claude", "com.google.android.apps.bard",
            "com.microsoft.copilot", "com.google.android.googlequicksearchbox")

    private fun quickToolDefaults(installed: Set<String>): List<String> {
        val calc = resolve(installed, category(Intent.CATEGORY_APP_CALCULATOR))
            ?: firstInstalled(installed, "com.oneplus.calculator", "com.google.android.calculator", "com.coloros.calculator")
        val maps = resolve(installed, category(Intent.CATEGORY_APP_MAPS))
            ?: firstInstalled(installed, "com.google.android.apps.maps")
        return listOfNotNull(aiPackage(installed), filesPackage(installed), calc, maps).distinct().take(4)
    }

    /** First run: pick the mockup's line-up from what is actually installed on this phone. */
    private fun seedDefaults(installed: Set<String>) {
        val browser = firstInstalled(installed, "com.android.chrome", "com.brave.browser", "org.mozilla.firefox",
            "com.duckduckgo.mobile.android") ?: resolve(installed, category(Intent.CATEGORY_APP_BROWSER))
        val camera = cameraPackage(installed)
        val pinned = listOfNotNull(
            firstInstalled(installed, "com.whatsapp"),
            firstInstalled(installed, "org.telegram.messenger"),
            firstInstalled(installed, "com.google.android.youtube"),
            firstInstalled(installed, "com.spotify.music"),
            browser,
            firstInstalled(installed, "com.reddit.frontpage"),
            galleryPackage(installed),
            camera,
            notesPackage(installed),
            firstInstalled(installed, "com.android.settings"),
            filesPackage(installed)
        ).distinct().toMutableList()
        if (pinned.size < 6) {
            installed.sorted().filter { it !in pinned }.take(8 - pinned.size).forEach { pinned.add(it) }
        }
        val small = setOf("com.google.android.youtube", "com.spotify.music", "com.reddit.frontpage",
            "com.twitter.android", "com.google.android.keep", "com.simplemobiletools.notes")
        val editor = prefs.edit()
            .putString(KEY_PINNED_ORDER, pinned.joinToString(","))
            .putString(KEY_ACCENT, camera ?: pinned.firstOrNull().orEmpty())
            .putString(KEY_QUICK_TOOLS, quickToolDefaults(installed).joinToString(","))
            .putString(KEY_FOCUS_APPS, pinned.take(10).joinToString(","))
        pinned.filter { it in small }.forEach { editor.putString(KEY_TILE_SIZE_PREFIX + it, TileSize.SMALL.name) }
        editor.apply()
    }

    /** v3 pinned AOSP package names that do not exist on OnePlus; swap them once. */
    private fun migrateLegacyDefaults(installed: Set<String>) {
        val order = csv(KEY_PINNED_ORDER)
        val legacy = mapOf(
            "com.android.camera" to cameraPackage(installed),
            "com.android.gallery" to galleryPackage(installed),
            "com.android.documentsui" to filesPackage(installed),
            "com.simplemobiletools.notes" to notesPackage(installed)
        )
        var changed = false
        val migrated = order.mapNotNull { pkg ->
            if (pkg in installed) pkg
            else legacy[pkg]?.also { changed = true }
        }.distinct()
        val accent = prefs.getString(KEY_ACCENT, null)
        val editor = prefs.edit()
        if (changed) editor.putString(KEY_PINNED_ORDER, migrated.joinToString(","))
        if (!accent.isNullOrBlank() && accent !in installed) {
            val replacement = legacy[accent] ?: cameraPackage(installed)
            if (replacement != null) {
                editor.putString(KEY_ACCENT, replacement)
                changed = true
            }
        }
        if (prefs.getString(KEY_QUICK_TOOLS, null) == null) {
            editor.putString(KEY_QUICK_TOOLS, quickToolDefaults(installed).joinToString(","))
            changed = true
        }
        if (prefs.getString(KEY_FOCUS_APPS, null) == null) {
            editor.putString(KEY_FOCUS_APPS, migrated.take(10).joinToString(","))
            changed = true
        }
        if (changed) editor.apply()
    }

    // ---- mutations ----------------------------------------------------------------------

    override suspend fun setPinned(packageNames: List<String>) {
        val cleaned = packageNames.filter { it.isNotBlank() }.distinct()
        val removed = csv(KEY_PINNED_ORDER).filter { it !in cleaned }
        val editor = prefs.edit().putString(KEY_PINNED_ORDER, cleaned.joinToString(","))
        removed.forEach { editor.remove(KEY_TILE_SIZE_PREFIX + it) }
        editor.apply()
        rebuild()
    }

    override suspend fun movePinned(packageName: String, delta: Int) {
        val order = csv(KEY_PINNED_ORDER).toMutableList()
        val from = order.indexOf(packageName)
        if (from < 0) return
        val to = (from + delta).coerceIn(0, order.lastIndex)
        if (to == from) return
        order.removeAt(from)
        order.add(to, packageName)
        prefs.edit().putString(KEY_PINNED_ORDER, order.joinToString(",")).apply()
        rebuild()
    }

    override suspend fun unpin(packageName: String) {
        setPinned(csv(KEY_PINNED_ORDER).filter { it != packageName })
    }

    override suspend fun setAccent(packageName: String?) {
        prefs.edit().putString(KEY_ACCENT, packageName.orEmpty()).apply()
        rebuild()
    }

    override suspend fun setTileSize(packageName: String, size: TileSize) {
        prefs.edit().putString(KEY_TILE_SIZE_PREFIX + packageName, size.name).apply()
        rebuild()
    }

    override suspend fun setCaption(packageName: String, caption: String?) {
        val editor = prefs.edit()
        if (caption.isNullOrBlank()) editor.remove(KEY_CAPTION_PREFIX + packageName)
        else editor.putString(KEY_CAPTION_PREFIX + packageName, caption.trim())
        editor.apply()
        rebuild()
    }

    override suspend fun setQuickTools(packageNames: List<String>) {
        prefs.edit().putString(KEY_QUICK_TOOLS, packageNames.distinct().take(4).joinToString(",")).apply()
        rebuild()
    }

    override suspend fun setFocusApps(packageNames: List<String>) {
        prefs.edit().putString(KEY_FOCUS_APPS, packageNames.distinct().take(10).joinToString(",")).apply()
        rebuild()
    }

    override suspend fun resetLayout() {
        prefs.edit().clear().apply()
        rebuild()
    }

    override fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    // ---- prefs helpers ------------------------------------------------------------------

    private fun csv(key: String): List<String> =
        prefs.getString(key, "")?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    private fun <T : Any> loadPrefixed(prefix: String, parse: (String) -> T?): Map<String, T> {
        val map = mutableMapOf<String, T>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(prefix)) {
                val parsed = (value as? String)?.let(parse)
                if (parsed != null) map[key.removePrefix(prefix)] = parsed
            }
        }
        return map
    }

    private companion object {
        const val PREFS_NAME = "pins_prefs"
        const val KEY_PINNED_ORDER = "pinned_order"
        const val KEY_ACCENT = "accent"
        const val KEY_TILE_SIZE_PREFIX = "tile_size_"
        const val KEY_CAPTION_PREFIX = "caption_"
        const val KEY_QUICK_TOOLS = "quick_tools"
        const val KEY_FOCUS_APPS = "focus_apps"
    }
}
