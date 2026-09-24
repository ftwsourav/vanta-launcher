package com.xdlab.standard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.ui.theme.JetBrainsMono
import com.xdlab.standard.ui.theme.LocalAppTheme

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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onToggle(!enabled) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = colors.ink,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(10.dp))
        val thumbColor by animateColorAsState(
            targetValue = if (enabled) colors.accent else colors.ink.copy(alpha = 0.35f),
            animationSpec = spring<Color>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "thumb"
        )
        Box(modifier = Modifier.size(width = 40.dp, height = 22.dp).border(2.dp, colors.ink)) {
            Box(
                modifier = Modifier
                    .offset(x = if (enabled) 22.dp else 2.dp, y = 2.dp)
                    .size(14.dp)
                    .background(thumbColor)
            )
        }
    }
}

class RippleConfig(
    val enabled: Boolean
) {
    fun onTap(offset: Offset, size: Size, color: Color) {
    }
}

@Composable
fun rememberRipple(enabled: Boolean): RippleConfig {
    return remember(enabled) { RippleConfig(enabled) }
}

data class MotionSettings(
    val profile: MotionProfile = MotionProfile.WINDOWS_PHONE,
    val mediumTiles: Boolean = true,
    val silkyPager: Boolean = true,
    val slideableHome: Boolean = true,
    val motionTouch: Boolean = true,
    val edgePeek: Boolean = true
)