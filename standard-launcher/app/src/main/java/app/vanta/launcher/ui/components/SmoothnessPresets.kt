package app.vanta.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme

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
    fun cycle() { level = SmoothnessLevel.entries[(level.ordinal + 1) % SmoothnessLevel.entries.size] }

    fun springSpec(): SpringSpec<Float> = spring(dampingRatio = level.springDamping, stiffness = level.springStiffness)
    fun <T> tweenSpec(): TweenSpec<T> = tween(durationMillis = level.tweenDurationMs, easing = FastOutSlowInEasing)
    fun animSpec(): FiniteAnimationSpec<Float> = spring(dampingRatio = level.springDamping, stiffness = level.springStiffness)
}

@Composable
fun rememberSmoothness(): SmoothnessController = remember { SmoothnessController() }

/** Five outline squares; the selected one fills accent (160ms tween, no bounce). */
@Composable
fun SmoothnessPicker(
    current: SmoothnessLevel,
    onSelected: (SmoothnessLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    Column(modifier = modifier.fillMaxWidth()) {
        MonoLabel("SMOOTHNESS // ${current.label}", size = 11.sp, weight = FontWeight.Bold, color = colors.ink)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmoothnessLevel.entries.forEach { level ->
                SmoothnessPresetTile(
                    level = level,
                    selected = level == current,
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
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val fill = animateColorAsState(
        targetValue = if (selected) colors.accent else Color.Transparent,
        animationSpec = tween(160, easing = LumiaEasing),
        label = "smoothnessPresetBg"
    )
    val text by animateColorAsState(
        targetValue = if (selected) colors.onAccent else colors.ink,
        animationSpec = tween(160, easing = LumiaEasing),
        label = "smoothnessPresetText"
    )
    Box(
        modifier = modifier
            .height(36.dp)
            .tilePress(onTap = onSelected, tilt = false)
            .drawBehind { drawRect(fill.value) }
            .border(2.dp, colors.ink)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        MonoLabel(level.label, size = 9.sp, weight = FontWeight.Bold, color = text)
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
