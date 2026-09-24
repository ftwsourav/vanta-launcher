package com.xdlab.standard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.ui.theme.JetBrainsMono
import com.xdlab.standard.ui.theme.LocalAppTheme

enum class SmoothnessLevel(val label: String, val springDamping: Float, val springStiffness: Float, val tweenDurationMs: Int) {
    SNAPPY("SNAPPY", 0.85f, 1500f, 200),
    SMOOTH("SMOOTH", 0.75f, 800f, 350),
    LUXURIOUS("LUXURIOUS", 0.65f, 400f, 500),
    BOUNCY("BOUNCY", 0.45f, 600f, 400),
    GLASS("GLASS", 0.9f, 2000f, 250)
}

class SmoothnessController {
    var level by mutableStateOf(SmoothnessLevel.SMOOTH)
        private set

    fun changeLevel(newLevel: SmoothnessLevel) { level = newLevel }
    fun cycle() { level = SmoothnessLevel.values()[(level.ordinal + 1) % SmoothnessLevel.values().size] }

    fun springSpec(): SpringSpec<Float> = spring(dampingRatio = level.springDamping, stiffness = level.springStiffness)
    fun <T> tweenSpec(): TweenSpec<T> = tween(durationMillis = level.tweenDurationMs, easing = FastOutSlowInEasing)
    fun animSpec(): FiniteAnimationSpec<Float> = spring(dampingRatio = level.springDamping, stiffness = level.springStiffness)
}

@Composable
fun rememberSmoothness(): SmoothnessController = remember { SmoothnessController() }

@Composable
fun SmoothnessPicker(
    current: SmoothnessLevel,
    onSelected: (SmoothnessLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val controller = rememberSmoothness()
    LaunchedEffect(current) { controller.changeLevel(current) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "SMOOTHNESS // ${current.label}",
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = colors.ink
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmoothnessLevel.values().forEach { level ->
                SmoothnessPresetTile(
                    level = level,
                    selected = level == current,
                    controller = controller,
                    onSelected = { onSelected(level) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SmoothnessPresetTile(
    level: SmoothnessLevel,
    selected: Boolean,
    controller: SmoothnessController,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    var pressed by remember { mutableStateOf(false) }
    val background by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.tile,
        animationSpec = spring(),
        label = "smoothnessPresetBg"
    )
    Box(
        modifier = modifier
            .height(36.dp)
            .smoothPress(controller, pressed)
            .background(background)
            .border(2.dp, colors.ink)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onSelected() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = level.label,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            color = if (selected) colors.onAccent else colors.onTile
        )
    }
}

@Composable
fun Modifier.smoothPress(controller: SmoothnessController, pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = controller.springSpec(),
        label = "smoothPress"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Modifier.smoothFade(controller: SmoothnessController, visible: Boolean): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = controller.tweenSpec<Float>(),
        label = "smoothFade"
    )
    return this.graphicsLayer {
        this.alpha = alpha
    }
}
