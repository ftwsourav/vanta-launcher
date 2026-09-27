package app.vanta.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.StandardType

private val BadgeSide = 18.dp
private val BadgeInset = 4.dp

private fun badgeLabel(count: Int) = if (count > 99) "99+" else count.toString()

/** Metro count badge: an 18dp accent square with a mono bold digit. Draws nothing for zero. */
@Composable
fun NotificationBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val colors = LocalAppTheme.current
    Box(
        modifier = modifier.padding(BadgeInset).size(BadgeSide).background(colors.accent),
        contentAlignment = Alignment.Center
    ) {
        MonoLabel(badgeLabel(count), size = 10.sp, color = colors.onAccent, weight = FontWeight.Bold, maxLines = 1)
    }
}

/** Same badge drawn over any content, top-end, without adding a layout node. */
@Composable
fun Modifier.notificationBadgeOverlay(count: Int): Modifier {
    if (count <= 0) return this
    val colors = LocalAppTheme.current
    val measurer = rememberTextMeasurer()
    val label = badgeLabel(count)
    val layout = remember(label, colors.onAccent) {
        measurer.measure(label, StandardType.mono(10.sp, FontWeight.Bold).copy(color = colors.onAccent))
    }
    return this.drawWithContent {
        drawContent()
        val side = BadgeSide.toPx()
        val inset = BadgeInset.toPx()
        val left = size.width - inset - side
        drawRect(color = colors.accent, topLeft = Offset(left, inset), size = Size(side, side))
        drawText(
            layout,
            topLeft = Offset(left + (side - layout.size.width) / 2f, inset + (side - layout.size.height) / 2f)
        )
    }
}

class NotificationCounter {
    private val counts = mutableStateMapOf<String, Int>()

    fun setCount(packageName: String, count: Int) {
        counts[packageName] = count
    }

    fun getCount(packageName: String): Int = counts[packageName] ?: 0

    fun clear() {
        counts.clear()
    }
}
