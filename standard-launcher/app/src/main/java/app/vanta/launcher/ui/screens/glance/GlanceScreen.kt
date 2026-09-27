package app.vanta.launcher.ui.screens.glance

import android.app.AlarmManager
import android.content.Context
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.components.DriftingNoiseOverlay
import app.vanta.launcher.ui.components.MonoLabel
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.StandardType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE dd MMM")
private val AlarmFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE HH:mm")

/** Lumia Glance: clock, date, battery and next alarm in muted paper on pure black (the black is intentional). */
@Composable
fun GlanceScreen(modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    val colors = LocalAppTheme.current
    val context = LocalContext.current
    val paper = if (colors.isDark) colors.ink else colors.background
    val muted = paper.copy(alpha = 0.78f)
    val faint = paper.copy(alpha = 0.45f)

    var timeStr by remember { mutableStateOf(LocalTime.now().format(TimeFormat)) }
    LaunchedEffect(Unit) {
        while (true) {
            timeStr = LocalTime.now().format(TimeFormat)
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val dateStr = remember(timeStr) { LocalDate.now().format(DateFormat).uppercase() }
    val battery = remember {
        try {
            (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) { -1 }
    }
    val alarm = remember {
        try {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).nextAlarmClock?.triggerTime?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(AlarmFormat).uppercase()
            }
        } catch (e: Exception) { null }
    }
    BackHandler { onDismiss() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        DriftingNoiseOverlay(modifier = Modifier.fillMaxSize(), alpha = 0.05f, tint = paper)
        MonoLabel(
            "TAP TO DISMISS",
            size = 9.sp,
            color = faint,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeStr,
                style = StandardType.display(88.sp).copy(fontFeatureSettings = "tnum"),
                color = muted
            )
            Spacer(Modifier.height(6.dp))
            MonoLabel(dateStr, size = 14.sp, color = faint)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.width(40.dp).height(4.dp).background(colors.accent))
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (battery >= 0) MonoLabel("BATTERY $battery", size = 10.sp, color = faint)
            Spacer(Modifier.weight(1f))
            MonoLabel(if (alarm != null) "ALARM $alarm" else "NO ALARM", size = 10.sp, color = faint)
        }
    }
}

@Composable
fun GlanceOverlay(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(200))
    ) {
        GlanceScreen(onDismiss = onDismiss)
    }
}
