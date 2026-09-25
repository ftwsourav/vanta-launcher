package app.vanta.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.*

@Composable
fun NotificationBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val colors = LocalAppTheme.current
    val label = if (count > 99) "99+" else count.toString()
    Box(modifier = modifier.padding(4.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .clip(CircleShape)
                .background(colors.accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun Modifier.notificationBadgeOverlay(count: Int): Modifier {
    if (count <= 0) return this
    val colors = LocalAppTheme.current
    val measurer = rememberTextMeasurer()
    val label = if (count > 99) "99+" else count.toString()
    val layout = remember(label) {
        measurer.measure(
            label,
            TextStyle(
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.White
            )
        )
    }
    return this.drawWithContent {
        drawContent()
        val diameter = 20.dp.toPx()
        val inset = 4.dp.toPx()
        val radius = diameter / 2f
        val cx = size.width - inset - radius
        val cy = inset + radius
        drawCircle(color = colors.accent, radius = radius, center = Offset(cx, cy))
        drawText(
            layout,
            topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f)
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
