package app.vanta.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.util.RefreshRate

enum class MotionProfile {
    LUMIA, SILKY, GENTLE, WINDOWS_PHONE
}

fun MotionProfile.silkyEasing(): CubicBezierEasing =
    if (this == MotionProfile.SILKY || this == MotionProfile.WINDOWS_PHONE) {
        CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    } else {
        CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    }

fun MotionProfile.tileSpring(): androidx.compose.animation.core.SpringSpec<Float> =
    spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

fun MotionProfile.gentleEasing(): CubicBezierEasing =
    if (this == MotionProfile.GENTLE) {
        CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)
    } else {
        CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    }

fun MotionProfile.durationMs(): Int = when (this) {
    MotionProfile.LUMIA -> 450
    MotionProfile.SILKY -> 300
    MotionProfile.GENTLE -> 500
    MotionProfile.WINDOWS_PHONE -> 450
}

/**
 * Windows Phone toggle switch, display only (the row owns the tap): a 48x24 track with a 2dp ink
 * outline and a 20dp square knob that slides on [RefreshRate.springSpec]; the track fills accent
 * when on. Every animated value is read in the draw phase.
 */
@Composable
fun WpSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalAppTheme.current
    val knob = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) { knob.animateTo(if (checked) 1f else 0f, RefreshRate.springSpec()) }
    val track = animateColorAsState(
        targetValue = if (checked) colors.accent else Color.Transparent,
        animationSpec = tween(160, easing = LumiaEasing),
        label = "wpTrack"
    )
    val knobColor = animateColorAsState(
        targetValue = if (checked) colors.onAccent else colors.ink,
        animationSpec = tween(160, easing = LumiaEasing),
        label = "wpKnob"
    )
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 24.dp)
            .drawBehind {
                drawRect(track.value)
                val edge = 2.dp.toPx()
                val k = size.height - 2f * edge
                val x = edge + knob.value.coerceIn(0f, 1f) * (size.width - 2f * edge - k)
                drawRect(knobColor.value, topLeft = Offset(x, edge), size = Size(k, k))
            }
            .border(2.dp, colors.ink)
    )
}

@Composable
fun MotionToggle(
    label: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .tilePress(onTap = { onToggle(!enabled) })
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = ToggleableState(enabled)
                onClick { onToggle(!enabled); true }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonoLabel(label, size = 12.sp, weight = FontWeight.Bold, color = colors.ink, modifier = Modifier.weight(1f), maxLines = 1)
        Spacer(Modifier.width(10.dp))
        MonoLabel(if (enabled) "ON" else "OFF", size = 9.sp, color = colors.muted)
        Spacer(Modifier.width(8.dp))
        WpSwitch(enabled)
    }
}

data class MotionSettings(
    val profile: MotionProfile = MotionProfile.WINDOWS_PHONE,
    val mediumTiles: Boolean = true,
    val silkyPager: Boolean = true,
    val slideableHome: Boolean = true,
    val motionTouch: Boolean = true,
    val edgePeek: Boolean = true
)
