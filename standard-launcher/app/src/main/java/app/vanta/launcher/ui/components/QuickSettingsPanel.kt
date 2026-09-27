package app.vanta.launcher.ui.components

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.StandardType
import app.vanta.launcher.util.RefreshRate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SheetInMs = 280
private const val SheetOutMs = 200

/**
 * WP7 turnstile entrance: hinged on the left edge, swings from 60 degrees to flat while fading in,
 * staggered by [index]. Re-runs whenever [key] changes; [visible] = false swings it back out.
 */
@Composable
fun Modifier.metroTurnstileIn(
    index: Int,
    key: Any? = Unit,
    visible: Boolean = true,
    staggerMs: Int = 25,
    capMs: Int = 300,
    durationMs: Int = 260
): Modifier {
    val p = remember { Animatable(0f) }
    LaunchedEffect(key, visible) {
        if (!visible) {
            p.animateTo(0f, tween(120, easing = LumiaEasing))
            return@LaunchedEffect
        }
        p.snapTo(0f)
        delay(minOf(index * staggerMs, capMs).toLong())
        p.animateTo(1f, tween(durationMs, easing = LumiaEasing))
    }
    return graphicsLayer {
        val v = p.value
        transformOrigin = TransformOrigin(0f, 0.5f)
        rotationY = 60f * (1f - v)
        alpha = v
        cameraDistance = 14f * density
    }
}

@Composable
fun QuickSettingsPanel(
    modifier: Modifier = Modifier,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val haptics = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    val sheet = remember { Animatable(0f) }
    val dim = remember { Animatable(0f) }
    val drag = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }
    var tilesIn by remember { mutableStateOf(false) }
    var panelHeightPx by remember { mutableFloatStateOf(0f) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val appContext = context.applicationContext
    val wifiManager = remember { appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val btManager = remember { appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val btAdapter: BluetoothAdapter? = remember {
        try { btManager.adapter } catch (e: SecurityException) { null }
    }
    val notifManager = remember { appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    val cameraManager = remember { appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val batteryManager = remember { appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }

    var wifiOn by remember { mutableStateOf(try { wifiManager.isWifiEnabled } catch (e: Exception) { false }) }
    var btOn by remember { mutableStateOf(try { btAdapter?.isEnabled == true } catch (e: SecurityException) { false }) }
    var dndOn by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val f = notifManager.currentInterruptionFilter
                    f != NotificationManager.INTERRUPTION_FILTER_ALL && f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
                } catch (e: Exception) { false }
            } else { false }
        )
    }
    var torchOn by remember { mutableStateOf(false) }
    var autoRotateOn by remember {
        mutableStateOf(
            try { Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1 } catch (e: Exception) { false }
        )
    }
    var airplaneOn by remember {
        mutableStateOf(
            try { Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1 } catch (e: Exception) { false }
        )
    }
    var batteryPct by remember { mutableIntStateOf(0) }
    var brightness by remember {
        mutableFloatStateOf(
            try { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128).toFloat() } catch (e: Exception) { 128f }
        )
    }
    var torchId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visible) {
        if (visible) {
            shown = true
            tilesIn = false
            launch {
                batteryPct = try {
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
                } catch (e: Exception) { 0 }
                try {
                    torchId = cameraManager.cameraIdList.firstOrNull {
                        cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }
                } catch (e: Exception) { torchId = null }
            }
            launch { dim.animateTo(1f, tween(200)) }
            sheet.animateTo(1f, tween(SheetInMs, easing = LumiaEasing))
            tilesIn = true
        } else {
            tilesIn = false
            launch { dim.animateTo(0f, tween(SheetOutMs)) }
            sheet.animateTo(0f, tween(SheetOutMs, easing = LumiaEasing))
            drag.snapTo(0f)
            shown = false
        }
    }

    if (!shown) return

    val tick: () -> Unit = {
        if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val dismiss: () -> Unit = {
        tick()
        tilesIn = false
        scope.launch {
            launch { dim.animateTo(0f, tween(SheetOutMs)) }
            sheet.animateTo(0f, tween(SheetOutMs, easing = LumiaEasing))
            drag.snapTo(0f)
            currentOnDismiss()
        }
    }

    val settle: () -> Unit = { scope.launch { drag.animateTo(0f, RefreshRate.springSpec()) } }

    val openIntent: (Intent) -> Unit = { intent ->
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, "NOT AVAILABLE", Toast.LENGTH_SHORT).show()
        }
    }

    // Tiles use tilePress, which already ticks (gated on LocalHapticsEnabled) on every tap.
    val toggleWifi: () -> Unit = {
        try {
            val newState = !wifiOn
            val ok = wifiManager.setWifiEnabled(newState)
            if (ok) {
                wifiOn = newState
                Toast.makeText(context, if (newState) "WI-FI ON" else "WI-FI OFF", Toast.LENGTH_SHORT).show()
            } else {
                wifiOn = wifiManager.isWifiEnabled
                Toast.makeText(context, "WI-FI: PERMISSION DENIED", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            wifiOn = try { wifiManager.isWifiEnabled } catch (e2: Exception) { wifiOn }
            Toast.makeText(context, "WI-FI: PERMISSION DENIED", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleBt: () -> Unit = {
        try {
            val adapter = btAdapter
            if (adapter != null) {
                val newState = !btOn
                if (newState) adapter.enable() else adapter.disable()
                btOn = newState
                Toast.makeText(context, if (newState) "BLUETOOTH ON" else "BLUETOOTH OFF", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "BLUETOOTH: UNAVAILABLE", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            btOn = try { btAdapter?.isEnabled == true } catch (e2: SecurityException) { false }
            Toast.makeText(context, "BLUETOOTH: PERMISSION DENIED", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleDnd: () -> Unit = {
        try {
            val newState = !dndOn
            notifManager.setInterruptionFilter(
                if (newState) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            dndOn = newState
            Toast.makeText(context, if (newState) "DND ON" else "DND OFF", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "DND: PERMISSION DENIED", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleTorch: () -> Unit = {
        try {
            val id = torchId
            if (id != null) {
                val newState = !torchOn
                cameraManager.setTorchMode(id, newState)
                torchOn = newState
                Toast.makeText(context, if (newState) "FLASHLIGHT ON" else "FLASHLIGHT OFF", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "FLASHLIGHT: UNAVAILABLE", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "FLASHLIGHT: PERMISSION DENIED", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleAutoRotate: () -> Unit = {
        try {
            val newState = !autoRotateOn
            val ok = Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (newState) 1 else 0)
            if (ok) {
                autoRotateOn = newState
                Toast.makeText(context, if (newState) "AUTO-ROTATE ON" else "AUTO-ROTATE OFF", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "AUTO-ROTATE: PERMISSION DENIED", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "AUTO-ROTATE: PERMISSION DENIED", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleAirplane: () -> Unit = {
        try {
            val newState = !airplaneOn
            val ok = Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (newState) 1 else 0)
            if (ok) {
                val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                intent.putExtra("state", newState)
                context.sendBroadcast(intent)
                airplaneOn = newState
                Toast.makeText(context, if (newState) "AIRPLANE ON" else "AIRPLANE OFF", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "PERMISSION DENIED", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "PERMISSION DENIED", Toast.LENGTH_SHORT).show()
        }
    }

    val openBattery: () -> Unit = {
        try {
            openIntent(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        } catch (e: Exception) {
            openIntent(Intent(Settings.ACTION_SETTINGS))
        }
    }

    val openSettings: () -> Unit = { openIntent(Intent(Settings.ACTION_SETTINGS)) }

    val onBrightnessChange: (Float) -> Unit = { value -> brightness = value }

    val onBrightnessFinished: () -> Unit = {
        tick()
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            val ok = Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness.toInt())
            if (ok) {
                Toast.makeText(context, "BRIGHTNESS ${brightness.toInt()}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "BRIGHTNESS: PERMISSION DENIED", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "BRIGHTNESS: PERMISSION DENIED", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(colors.ink.copy(alpha = 0.45f * dim.value)) }
                .pointerInput(Unit) { detectTapGestures { dismiss() } }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    val h = panelHeightPx
                    translationY = -h * (1f - sheet.value) + drag.value
                    alpha = if (h == 0f) 0f else 1f
                }
                .onSizeChanged { panelHeightPx = it.height.toFloat() }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            scope.launch { drag.snapTo((drag.value + amount).coerceAtMost(0f)) }
                        },
                        onDragEnd = {
                            if (-drag.value > panelHeightPx * 0.3f) dismiss() else settle()
                        },
                        onDragCancel = { settle() }
                    )
                }
                .background(colors.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonoLabel("QUICK SETTINGS //", size = 11.sp, color = colors.ink)
                Spacer(Modifier.weight(1f))
                MonoLabel("SWIPE UP TO CLOSE", size = 10.sp, color = colors.muted)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
            ) {
                QsToggleTile("W", "WI-FI", wifiOn, toggleWifi, Modifier.weight(1f).metroTurnstileIn(0, visible = tilesIn))
                QsToggleTile("B", "BLUETOOTH", btOn, toggleBt, Modifier.weight(1f).metroTurnstileIn(1, visible = tilesIn))
                QsToggleTile("D", "DND", dndOn, toggleDnd, Modifier.weight(1f).metroTurnstileIn(2, visible = tilesIn))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
            ) {
                QsToggleTile("F", "FLASHLIGHT", torchOn, toggleTorch, Modifier.weight(1f).metroTurnstileIn(3, visible = tilesIn))
                QsToggleTile("R", "AUTO-ROTATE", autoRotateOn, toggleAutoRotate, Modifier.weight(1f).metroTurnstileIn(4, visible = tilesIn))
                QsToggleTile("A", "AIRPLANE", airplaneOn, toggleAirplane, Modifier.weight(1f).metroTurnstileIn(5, visible = tilesIn))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
            ) {
                QsBatteryTile(batteryPct, openBattery, Modifier.weight(1f).metroTurnstileIn(6, visible = tilesIn))
                QsBrightnessTile(brightness, onBrightnessChange, onBrightnessFinished, Modifier.weight(1f).metroTurnstileIn(7, visible = tilesIn))
                QsToggleTile("S", "SETTINGS", true, openSettings, Modifier.weight(1f).metroTurnstileIn(8, visible = tilesIn))
            }
        }
    }
}

@Composable
private fun QsToggleTile(
    letter: String,
    label: String,
    active: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val outline = if (colors.filledTiles) colors.outline else colors.ink
    val borderColor by animateColorAsState(if (active) colors.accent else outline, tween(160), label = "qsBorder")
    val textColor by animateColorAsState(if (active) colors.accent else colors.muted, tween(160), label = "qsText")
    Box(
        modifier = modifier
            .height(104.dp)
            .tilePress(onTap = onTap)
            .background(colors.tile)
            .border(TileDefaults.Border, borderColor)
            .button(label) { onTap() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            HeadlineText(letter, 28.sp, color = textColor)
            MonoLabel(label, size = 10.sp, color = textColor, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun QsBatteryTile(
    percent: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val outline = if (colors.filledTiles) colors.outline else colors.ink
    Box(
        modifier = modifier
            .height(104.dp)
            .tilePress(onTap = onTap)
            .background(colors.tile)
            .border(TileDefaults.Border, outline)
            .button("BATTERY $percent PERCENT") { onTap() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "$percent",
                style = StandardType.display(30.sp).copy(fontFeatureSettings = "tnum"),
                color = colors.onTile
            )
            MonoLabel("BATTERY", size = 10.sp, color = colors.onTile, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun QsBrightnessTile(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    onBrightnessFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    Box(
        modifier = modifier
            .height(104.dp)
            .background(colors.tile)
            .border(TileDefaults.Border, colors.accent)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "${(brightness / 255f * 100f).toInt()}",
                style = StandardType.display(22.sp).copy(fontFeatureSettings = "tnum"),
                color = colors.accent
            )
            BrutalSlider(
                value = brightness / 255f,
                onChange = { onBrightnessChange(it * 255f) },
                onFinished = onBrightnessFinished,
                modifier = Modifier.fillMaxWidth()
            )
            MonoLabel("BRIGHTNESS", size = 9.sp, color = colors.accent, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

/** 8dp ink-outlined track, accent fill left of a 14dp square ink knob, drag-follow. [value] is 0..1. */
@Composable
private fun BrutalSlider(
    value: Float,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val currentValue by rememberUpdatedState(value)
    val currentChange by rememberUpdatedState(onChange)
    val currentFinished by rememberUpdatedState(onFinished)
    Box(
        modifier = modifier
            .height(14.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { currentChange((it.x / size.width).coerceIn(0f, 1f)) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        currentChange((change.position.x / size.width).coerceIn(0f, 1f))
                    },
                    onDragEnd = { currentFinished() },
                    onDragCancel = { currentFinished() }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    currentChange((it.x / size.width).coerceIn(0f, 1f))
                    currentFinished()
                }
            }
            .drawBehind {
                val knob = 14.dp.toPx()
                val track = 8.dp.toPx()
                val stroke = TileDefaults.Border.toPx()
                val knobX = (size.width - knob) * currentValue
                val top = (size.height - track) / 2f
                drawRect(
                    color = colors.accent,
                    topLeft = Offset(0f, top),
                    size = Size(knobX + knob / 2f, track)
                )
                drawRect(
                    color = colors.ink,
                    topLeft = Offset(stroke / 2f, top + stroke / 2f),
                    size = Size(size.width - stroke, track - stroke),
                    style = Stroke(stroke)
                )
                drawRect(
                    color = colors.ink,
                    topLeft = Offset(knobX, 0f),
                    size = Size(knob, knob)
                )
            }
    )
}
