package app.vanta.launcher.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.vanta.launcher.ui.theme.LocalAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LiveTileContent {
    fun batteryLevel(context: Context): Int = try {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        (level * 100 / scale).coerceIn(0, 100)
    } catch (e: Exception) { 0 }

    fun isCharging(context: Context): Boolean = try {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        status == BatteryManager.BATTERY_STATUS_CHARGING
    } catch (e: Exception) { false }

    fun weatherText(context: Context): String {
        val prefs = context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE)
        val temp = prefs.getString("weather_temp", null) ?: "--"
        val cond = prefs.getString("weather_condition", null) ?: ""
        val city = prefs.getString("weather_city", null) ?: ""
        return buildString {
            append(temp); if (cond.isNotBlank()) append(" $cond")
            if (city.isNotBlank()) append(" · $city")
        }.uppercase()
    }

    fun clockText(): String = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
    fun dateText(): String = SimpleDateFormat("EEE · MMM d", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault())
    fun secondsText(): String = SimpleDateFormat(":ss", Locale.getDefault()).format(Date())
}

@Composable
fun rememberBatteryState(): Pair<Int, Boolean> {
    val context = LocalContext.current
    var state by remember { mutableStateOf(0 to false) }
    LaunchedEffect(Unit) {
        while (true) {
            state = LiveTileContent.batteryLevel(context) to LiveTileContent.isCharging(context)
            kotlinx.coroutines.delay(30000)
        }
    }
    return state
}

@Composable
fun rememberClockTick(intervalMs: Long = 1000): Long {
    var tick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            kotlinx.coroutines.delay(intervalMs)
        }
    }
    return tick
}

fun clockFrames(): List<String> = listOf(
    LiveTileContent.clockText(),
    LiveTileContent.clockText() + LiveTileContent.secondsText(),
    LiveTileContent.dateText()
)

fun weatherFrames(context: Context): List<String> = listOf(
    LiveTileContent.weatherText(context),
    "TAP FOR FORECAST"
)

fun batteryFrames(context: Context): List<String> {
    val (level, charging) = LiveTileContent.batteryLevel(context) to LiveTileContent.isCharging(context)
    return listOf(
        "$level%",
        if (charging) "CHARGING" else "BATTERY"
    )
}
