package com.xdlab.standard.ui.components

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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch

@Composable
fun QuickSettingsPanel(
    modifier: Modifier = Modifier,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppTheme.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
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
            progress.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = 380f))
        } else {
            progress.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 380f))
        }
    }

    if (!visible && progress.value <= 0f) return

    val dismiss: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch {
            progress.animateTo(0f, tween(180))
            currentOnDismiss()
        }
    }

    val openIntent: (Intent) -> Unit = { intent ->
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, "NOT AVAILABLE", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleWifi: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        try {
            openIntent(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        } catch (e: Exception) {
            openIntent(Intent(Settings.ACTION_SETTINGS))
        }
    }

    val openSettings: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        openIntent(Intent(Settings.ACTION_SETTINGS))
    }

    val onBrightnessChange: (Float) -> Unit = { value -> brightness = value }

    val onBrightnessFinished: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                .background(colors.ink.copy(alpha = 0.45f * progress.value))
                .pointerInput(Unit) { detectTapGestures { dismiss() } }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = -panelHeightPx * (1f - progress.value)
                    alpha = progress.value
                }
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            total += dragAmount
                            if (total < -220f) {
                                dismiss()
                                total = Float.POSITIVE_INFINITY
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(TileDefaults.Gutter)
                    .onSizeChanged { panelHeightPx = it.height.toFloat() },
                verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoLabel("QUICK SETTINGS //", size = 11.sp, color = colors.ink)
                    MonoLabel("SWIPE UP TO CLOSE", size = 10.sp, color = colors.ink.copy(alpha = 0.6f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
                ) {
                    QsToggleTile("W", "WI-FI", wifiOn, toggleWifi, Modifier.weight(1f))
                    QsToggleTile("B", "BLUETOOTH", btOn, toggleBt, Modifier.weight(1f))
                    QsToggleTile("D", "DND", dndOn, toggleDnd, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
                ) {
                    QsToggleTile("F", "FLASHLIGHT", torchOn, toggleTorch, Modifier.weight(1f))
                    QsToggleTile("R", "AUTO-ROTATE", autoRotateOn, toggleAutoRotate, Modifier.weight(1f))
                    QsToggleTile("A", "AIRPLANE", airplaneOn, toggleAirplane, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
                ) {
                    QsBatteryTile(batteryPct, openBattery, Modifier.weight(1f))
                    QsBrightnessTile(brightness, onBrightnessChange, onBrightnessFinished, Modifier.weight(1f))
                    QsToggleTile("S", "SETTINGS", true, openSettings, Modifier.weight(1f))
                }
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
    val borderColor = if (active) colors.accent else outline
    val textColor = if (active) colors.accent else colors.muted
    Box(
        modifier = modifier
            .height(104.dp)
            .background(colors.tile)
            .border(TileDefaults.Border, borderColor)
            .pointerInput(Unit) { detectTapGestures { onTap() } }
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
            .background(colors.tile)
            .border(TileDefaults.Border, outline)
            .pointerInput(Unit) { detectTapGestures { onTap() } }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            HeadlineText("$percent", 28.sp, color = colors.ink)
            MonoLabel("BATTERY", size = 10.sp, color = colors.ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            MonoLabel("BRIGHTNESS", size = 9.sp, color = colors.accent, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Slider(
                value = brightness,
                onValueChange = onBrightnessChange,
                valueRange = 0f..255f,
                onValueChangeFinished = onBrightnessFinished,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.ink.copy(alpha = 0.2f)
                )
            )
        }
    }
}
