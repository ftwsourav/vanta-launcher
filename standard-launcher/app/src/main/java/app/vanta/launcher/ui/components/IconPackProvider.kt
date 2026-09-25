package app.vanta.launcher.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class IconPackProvider(private val context: android.content.Context) {
    private val prefs = context.getSharedPreferences("standard_settings", android.content.Context.MODE_PRIVATE)
    private var packPackage: String? = null
    private var iconMap: Map<String, String> = emptyMap()

    init { loadPack(prefs.getString("icon_pack", "") ?: "") }

    fun loadPack(packageName: String) {
        packPackage = packageName.ifBlank { null }
        iconMap = if (packPackage == null) emptyMap() else parseAppFilter(packPackage!!)
    }

    fun getIconForPackage(targetPackage: String): android.graphics.drawable.Drawable? {
        if (packPackage == null) return null
        val resName = iconMap[targetPackage] ?: return null
        return try {
            val res = context.packageManager.getResourcesForApplication(packPackage!!)
            val id = res.getIdentifier(resName, "drawable", packPackage)
            if (id != 0) res.getDrawable(id) else null
        } catch (e: Exception) { null }
    }

    fun listInstalledPacks(): List<String> {
        val known = listOf("app.lawnicons", "com.nexusbit.nova", "com.dlazartero.icons")
        return known.filter { pkg ->
            try { context.packageManager.getApplicationInfo(pkg, 0); true } catch (e: Exception) { false }
        }
    }

    private fun parseAppFilter(packPkg: String): Map<String, String> {
        return try {
            val res = context.packageManager.getResourcesForApplication(packPkg)
            val id = res.getIdentifier("appfilter", "xml", packPkg)
            if (id == 0) return emptyMap()
            val parser = res.getXml(id)
            val map = mutableMapOf<String, String>()
            while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) {
                        val pkg = component.substringAfter("ComponentInfo{").substringBefore("/")
                        map[pkg] = drawable
                    }
                }
            }
            map
        } catch (e: Exception) { emptyMap() }
    }

    fun setActivePack(pkg: String) {
        prefs.edit().putString("icon_pack", pkg).apply()
        loadPack(pkg)
    }

    fun isActive(): Boolean = packPackage != null
}

@Composable
fun rememberIconPackDrawable(packageName: String): android.graphics.drawable.Drawable? {
    val context = LocalContext.current
    val provider = remember { IconPackProvider(context) }
    return remember(packageName, provider.isActive()) { provider.getIconForPackage(packageName) }
}
