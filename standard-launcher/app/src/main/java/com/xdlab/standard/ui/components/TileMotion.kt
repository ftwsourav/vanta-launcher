package com.xdlab.standard.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xdlab.standard.ui.theme.LocalAppTheme
import kotlinx.coroutines.delay

@Composable
fun Modifier.breathing(enabled: Boolean, period: Int = 4000): Modifier {
    if (!enabled) return this
    val transition = rememberInfiniteTransition(label = "breathing")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = period, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    return this.graphicsLayer {
        val s = 1f + 0.01f * breath
        scaleX = s
        scaleY = s
    }
}

@Composable
fun Modifier.idleShimmer(enabled: Boolean, intervalMs: Long = 30000L): Modifier {
    if (!enabled) return this
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(enabled, intervalMs) {
        while (true) {
            delay(intervalMs)
            sweep.animateTo(1f, tween(durationMillis = 800, easing = LinearEasing))
            sweep.snapTo(0f)
        }
    }
    return this.drawWithContent {
        drawContent()
        val p = sweep.value
        if (p > 0f && p < 1f) {
            val w = size.width
            val bandW = w * 0.2f
            val center = (p * 1.2f - 0.1f) * w
            val left = center - bandW / 2f
            val brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.04f), Color.Transparent),
                start = Offset(left, 0f),
                end = Offset(left + bandW, 0f)
            )
            drawRect(brush = brush)
        }
    }
}

@Composable
fun Modifier.scrollTilt(scrollState: ScrollState, index: Int): Modifier {
    var contentCenterY by remember(index) { mutableFloatStateOf(0f) }
    return this
        .onGloballyPositioned { coords ->
            contentCenterY = coords.positionInWindow().y + coords.size.height / 2f + scrollState.value
        }
        .graphicsLayer {
            val windowCenterY = contentCenterY - scrollState.value
            val viewportCenterY = 800f
            val delta = (windowCenterY - viewportCenterY) * 0.02f
            rotationX = delta.coerceIn(-3f, 3f)
            cameraDistance = 16f * density
        }
}

@Composable
fun Modifier.tileRipple(enabled: Boolean): Modifier {
    if (!enabled) return this
    val theme = LocalAppTheme.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            progress.snapTo(0f)
            progress.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        }
    }
    return this.drawWithContent {
        drawContent()
        val p = progress.value
        if (p > 0f && p < 1f) {
            val maxR = maxOf(size.width, size.height) * 0.5f
            val r = p * maxR
            if (r > 0.001f) {
                drawCircle(
                    color = theme.accent.copy(alpha = 0.25f * (1f - p)),
                    radius = r,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun Modifier.sensorTilt(enabled: Boolean, maxTilt: Float = 2f): Modifier {
    if (!enabled) return this
    val context = LocalContext.current
    val tiltX = remember { mutableFloatStateOf(0f) }
    val tiltY = remember { mutableFloatStateOf(0f) }
    DisposableEffect(enabled) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val g = SensorManager.STANDARD_GRAVITY
                val rawX = (event.values[0] / g).coerceIn(-1f, 1f)
                val rawY = (event.values[1] / g).coerceIn(-1f, 1f)
                tiltX.floatValue = tiltX.floatValue * 0.8f + rawX * 0.2f
                tiltY.floatValue = tiltY.floatValue * 0.8f + rawY * 0.2f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (accel != null) {
            sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sm.unregisterListener(listener) }
    }
    return this.graphicsLayer {
        rotationZ = tiltX.floatValue * maxTilt
        translationX = tiltY.floatValue * maxTilt * 2.dp.toPx()
    }
}
