package app.vanta.launcher.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(context: Context) {
    private val prefs = context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE)
    private val pins = context.getSharedPreferences("pins_prefs", Context.MODE_PRIVATE)

    fun export(): String {
        val json = JSONObject()
        json.put("theme", prefs.getString("theme_id", "mono"))
        json.put("dark_mode", prefs.getBoolean("dark_mode", false))
        json.put("pinned", pins.getString("pinned_order", ""))
        json.put("accent", pins.getString("accent", ""))
        json.put("modules", prefs.getString("home_modules", ""))
        json.put("animation_style", prefs.getInt("animation_style", 0))
        json.put("icon_style", prefs.getInt("icon_style", 0))
        json.put("haptics_enabled", prefs.getBoolean("haptics_enabled", true))
        json.put("use_texture", prefs.getBoolean("use_texture", true))
        json.put("texture_strength", prefs.getFloat("texture_strength", 0.12f).toDouble())
        json.put("noise_drift", prefs.getBoolean("noise_drift", false))
        json.put("silky_pager", prefs.getBoolean("silky_pager", true))
        json.put("slideable_home", prefs.getBoolean("slideable_home", false))
        json.put("motion_touch", prefs.getBoolean("motion_touch", false))
        json.put("tod_tint", prefs.getBoolean("tod_tint", false))
        json.put("weather_unit", prefs.getString("weather_unit", "metric"))
        json.put("clock_format", prefs.getString("clock_format", "12"))
        json.put("cinematic_intro", prefs.getBoolean("cinematic_intro", true))
        json.put("glance_enabled", prefs.getBoolean("glance_enabled", true))
        json.put("weather_name", prefs.getString("weather_name", ""))
        json.put("weather_latlon", prefs.getString("weather_latlon", ""))
        json.put("quotes", prefs.getString("quotes", ""))
        json.put("focus_apps", pins.getString("focus_apps", ""))
        json.put("quick_tools", pins.getString("quick_tools", ""))
        val already = setOf(
            "theme_id", "dark_mode", "pinned_order", "accent", "home_modules",
            "animation_style", "icon_style", "haptics_enabled", "use_texture",
            "texture_strength", "noise_drift", "silky_pager", "slideable_home",
            "motion_touch", "tod_tint", "weather_unit", "clock_format",
            "cinematic_intro", "glance_enabled", "weather_name", "weather_latlon",
            "quotes", "focus_apps", "quick_tools", "first_run"
        )
        prefs.all.forEach { (k, v) -> if (k !in already) putValue(json, k, v) }
        pins.all.forEach { (k, v) -> if (k !in already) putValue(json, k, v) }
        return json.toString()
    }

    fun import(json: String): Boolean {
        return try {
            val obj = JSONObject(json)
            val s = prefs.edit()
            val p = pins.edit()
            if (obj.has("theme")) s.putString("theme_id", obj.getString("theme"))
            if (obj.has("dark_mode")) s.putBoolean("dark_mode", obj.getBoolean("dark_mode"))
            if (obj.has("pinned")) p.putString("pinned_order", obj.getString("pinned"))
            if (obj.has("accent")) p.putString("accent", obj.getString("accent"))
            if (obj.has("modules")) s.putString("home_modules", obj.getString("modules"))
            if (obj.has("texture_strength")) s.putFloat("texture_strength", obj.getDouble("texture_strength").toFloat())
            val handled = setOf("theme", "dark_mode", "pinned", "accent", "modules", "texture_strength")
            obj.keys().forEach { key ->
                if (key in handled) return@forEach
                val value = obj.get(key)
                val editor = if (key == "accent" || key == "pinned_order" || key == "focus_apps" ||
                    key == "quick_tools" || key.startsWith("tile_size_") || key.startsWith("caption_")
                ) p else s
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                }
            }
            s.apply()
            p.apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun saveToFile(uri: Uri, context: Context): Boolean {
        return try {
            val json = export()
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadFromFile(uri: Uri, context: Context): Boolean {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (json != null) import(json) else false
        } catch (e: Exception) {
            false
        }
    }

    private fun putValue(json: JSONObject, key: String, value: Any?) {
        when (value) {
            is Boolean -> json.put(key, value)
            is Int -> json.put(key, value)
            is Long -> json.put(key, value)
            is Float -> json.put(key, value.toDouble())
            is Double -> json.put(key, value)
            is String -> json.put(key, value)
            is Set<*> -> json.put(key, value.joinToString(","))
            else -> json.put(key, value.toString())
        }
    }
}

private fun readNextEvent(context: Context): String? {
    val now = System.currentTimeMillis()
    val projection = arrayOf(
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.ALL_DAY,
        CalendarContract.Events.STATUS
    )
    val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DELETED} = 0"
    val selectionArgs = arrayOf(now.toString())
    val sortOrder = "${CalendarContract.Events.DTSTART} ASC"
    return try {
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            var result: String? = null
            while (cursor.moveToNext() && result == null) {
                val title = cursor.getString(0) ?: continue
                val start = cursor.getLong(1)
                val allDay = cursor.getInt(2) == 1
                val status = cursor.getInt(3)
                if (status == CalendarContract.Events.STATUS_CANCELED || allDay) continue
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(start))
                result = "NEXT: $title · $time"
            }
            result
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun CalendarNextEvent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    val granted = context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    var eventText by remember { mutableStateOf("NO UPCOMING EVENTS") }
    LaunchedEffect(granted) {
        if (!granted) {
            eventText = "NO UPCOMING EVENTS"
            return@LaunchedEffect
        }
        eventText = withContext(Dispatchers.IO) { readNextEvent(context) ?: "NO UPCOMING EVENTS" }
    }
    MonoLabel(
        text = eventText,
        modifier = modifier,
        size = 11.sp,
        color = theme.ink,
        maxLines = 1
    )
}

/** EXPORT / IMPORT outline buttons; the result is a mono status line that fades in and hides after 3s. */
@Composable
fun BackupRestoreButtons(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    val manager = remember { BackupManager(context) }
    var status by remember { mutableStateOf<String?>(null) }
    val shownStatus = remember { arrayOfNulls<String>(1) }
    if (status != null) shownStatus[0] = status
    LaunchedEffect(status) {
        if (status != null) {
            delay(3000)
            status = null
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) status = if (manager.saveToFile(uri, context)) "EXPORTED" else "EXPORT FAILED"
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) status = if (manager.loadFromFile(uri, context)) "IMPORTED" else "IMPORT FAILED"
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)) {
            BackupButton("EXPORT", Modifier.weight(1f)) { exportLauncher.launch("vanta_backup.json") }
            BackupButton("IMPORT", Modifier.weight(1f)) { importLauncher.launch(arrayOf("application/json")) }
        }
        AnimatedVisibility(
            visible = status != null,
            enter = fadeIn(tween(160, easing = LumiaEasing)),
            exit = fadeOut(tween(180, easing = LumiaEasing))
        ) {
            val text = shownStatus[0] ?: ""
            MonoLabel(
                text = text,
                size = 10.sp,
                weight = FontWeight.Bold,
                color = if (text.endsWith("FAILED")) theme.accent else theme.ink,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun BackupButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalAppTheme.current
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .tilePress(onTap = onClick, tilt = false)
            .border(2.dp, colors.ink)
            .button(label, onClick),
        contentAlignment = Alignment.Center
    ) {
        MonoLabel(
            text = label,
            size = 11.sp,
            color = colors.ink,
            weight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
