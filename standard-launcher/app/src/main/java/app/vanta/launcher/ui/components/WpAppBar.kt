package app.vanta.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme
import kotlin.math.cos
import kotlin.math.sin

/** Line glyphs drawn on the canvas, so nothing depends on the bundled fonts' symbol coverage. */
enum class WpGlyph { Sort, Refresh, Settings, More }

data class AppBarAction(
    val label: String,
    val glyph: WpGlyph,
    val onClick: () -> Unit
)

/**
 * Windows Phone app bar: 2dp ink circle buttons on a 44dp target, and a "more" button that grows
 * the bar upward to reveal mono labels under each icon plus the overflow text actions.
 */
@Composable
fun WpAppBar(
    modifier: Modifier = Modifier,
    actions: List<AppBarAction> = emptyList(),
    menuItems: List<String> = emptyList(),
    onMenuItemClick: (String) -> Unit = {}
) {
    val colors = LocalAppTheme.current
    var expanded by remember { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background.copy(alpha = 0.96f))
            .drawBehind { drawRect(colors.ink, size = Size(size.width, 2.dp.toPx())) }
            .animateContentSize(tween(220, easing = LumiaEasing))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top
            ) {
                actions.take(4).forEach { action ->
                    WpCircleButton(
                        glyph = action.glyph,
                        label = action.label,
                        labelShown = expanded,
                        onClick = {
                            expanded = false
                            action.onClick()
                        }
                    )
                }
            }
            WpCircleButton(
                glyph = WpGlyph.More,
                label = if (expanded) "LESS" else "MORE",
                labelShown = false,
                onClick = { expanded = !expanded }
            )
        }
        AnimatedVisibility(
            visible = expanded && menuItems.isNotEmpty(),
            enter = fadeIn(tween(220, easing = LumiaEasing)),
            exit = fadeOut(tween(120, easing = LumiaEasing))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                menuItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .tilePress(onTap = { expanded = false; onMenuItemClick(item) }, tilt = false)
                            .semantics {
                                role = Role.Button
                                onClick { expanded = false; onMenuItemClick(item); true }
                            }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        MonoLabel(item, size = 13.sp, color = colors.ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun WpCircleButton(
    glyph: WpGlyph,
    label: String,
    labelShown: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppTheme.current
    Column(
        modifier = Modifier
            .tilePress(onTap = onClick, tilt = false)
            .semantics {
                role = Role.Button
                contentDescription = label
                onClick { onClick(); true }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(44.dp)) {
            val ring = 2.dp.toPx()
            drawCircle(colors.ink, radius = 18.dp.toPx() - ring / 2f, style = Stroke(ring))
            drawGlyph(glyph, colors.ink, 1.5.dp.toPx())
        }
        AnimatedVisibility(
            visible = labelShown,
            enter = fadeIn(tween(220, easing = LumiaEasing)),
            exit = fadeOut(tween(120, easing = LumiaEasing))
        ) {
            MonoLabel(label, size = 9.sp, color = colors.ink, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private fun DrawScope.drawGlyph(glyph: WpGlyph, color: Color, stroke: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val u = 1.dp.toPx()
    val cap = StrokeCap.Square
    when (glyph) {
        WpGlyph.Sort -> {
            val left = cx - 7f * u
            listOf(14f, 9f, 4f).forEachIndexed { i, len ->
                val y = cy + (i - 1) * 5f * u
                drawLine(color, Offset(left, y), Offset(left + len * u, y), stroke, cap)
            }
        }
        WpGlyph.Refresh -> {
            val r = 7f * u
            drawArc(
                color = color,
                startAngle = -30f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(2f * r, 2f * r),
                style = Stroke(stroke)
            )
            // Arrowhead at the arc's end (12 o'clock), pointing clockwise.
            val tip = Offset(cx, cy - r)
            val a = 3.5f * u
            drawLine(color, tip, Offset(tip.x - a, tip.y - a), stroke, cap)
            drawLine(color, tip, Offset(tip.x - a, tip.y + a), stroke, cap)
        }
        WpGlyph.Settings -> {
            drawCircle(color, radius = 4f * u, center = Offset(cx, cy), style = Stroke(stroke))
            for (i in 0 until 8) {
                val ang = i * (Math.PI / 4.0).toFloat()
                val c = cos(ang)
                val s = sin(ang)
                drawLine(
                    color,
                    Offset(cx + c * 6f * u, cy + s * 6f * u),
                    Offset(cx + c * 8.5f * u, cy + s * 8.5f * u),
                    stroke,
                    cap
                )
            }
        }
        WpGlyph.More -> {
            for (dx in listOf(-5.5f, 0f, 5.5f)) {
                drawCircle(color, radius = 1.6f * u, center = Offset(cx + dx * u, cy))
            }
        }
    }
}
