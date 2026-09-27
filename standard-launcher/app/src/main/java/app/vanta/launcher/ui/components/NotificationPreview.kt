package app.vanta.launcher.ui.components

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.JetBrainsMono
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.SpaceGrotesk
import kotlinx.coroutines.delay
import org.json.JSONObject

private fun timeAgo(timestamp: Long, now: Long): String {
    val delta = (now - timestamp) / 1000
    return when {
        delta < 60 -> "now"
        delta < 3600 -> "${delta / 60}m"
        delta < 86400 -> "${delta / 3600}h"
        else -> "${delta / 86400}d"
    }
}

@Composable
fun NotificationPreview(
    packageName: String,
    title: String?,
    text: String?,
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    if (title.isNullOrBlank() && text.isNullOrBlank()) return
    val colors = LocalAppTheme.current
    val slide = remember { Animatable(1f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(timestamp) {
        slide.snapTo(1f)
        alpha.snapTo(1f)
        slide.animateTo(0f, animationSpec = tween(durationMillis = 300, easing = LumiaEasing))
        delay(10_000L)
        alpha.animateTo(0f, animationSpec = tween(durationMillis = 200, easing = LumiaEasing))
    }

    val ago = remember(timestamp) { timeAgo(timestamp, System.currentTimeMillis()) }

    Column(
        modifier = modifier
            .graphicsLayer {
                translationY = slide.value * 40.dp.toPx()
                this.alpha = alpha.value
            }
            .background(colors.ink.copy(alpha = 0.1f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                color = colors.accent,
                fontFamily = JetBrainsMono,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!text.isNullOrBlank()) {
            Text(
                text = text,
                color = colors.ink,
                fontFamily = SpaceGrotesk,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = ago,
            color = colors.ink.copy(alpha = 0.5f),
            fontFamily = JetBrainsMono,
            fontSize = 8.sp
        )
    }
}

@Composable
fun BackgroundActivityIndicator(
    packageName: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isActive) return
    val colors = LocalAppTheme.current
    val pulse = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        pulse.animateTo(
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 750, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }
    Box(
        modifier = modifier
            .size(6.dp)
            .graphicsLayer { this.alpha = pulse.value }
            .background(colors.accent)
    )
}

class NotificationPreviewStore(context: Context) {
    private val prefs = context.getSharedPreferences("standard_notif_preview", Context.MODE_PRIVATE)

    fun setNotification(packageName: String, title: String, text: String, timestamp: Long) {
        val json = JSONObject()
        json.put("title", title)
        json.put("text", text)
        json.put("timestamp", timestamp)
        prefs.edit().putString(packageName, json.toString()).apply()
    }

    fun getNotification(packageName: String): Triple<String, String, Long>? {
        val raw = prefs.getString(packageName, null) ?: return null
        return try {
            val json = JSONObject(raw)
            Triple(json.getString("title"), json.getString("text"), json.getLong("timestamp"))
        } catch (e: Exception) {
            null
        }
    }

    fun clearNotification(packageName: String) {
        prefs.edit().remove(packageName).apply()
    }

    fun getActiveApps(): Set<String> {
        val now = System.currentTimeMillis()
        val active = mutableSetOf<String>()
        for ((key, value) in prefs.all) {
            val raw = value as? String ?: continue
            try {
                val json = JSONObject(raw)
                val ts = json.getLong("timestamp")
                if (now - ts < 30_000L) active.add(key)
            } catch (e: Exception) {
            }
        }
        return active
    }
}

@Composable
fun NotificationTicker(
    notifications: List<Triple<String, String, Long>>,
    modifier: Modifier = Modifier
) {
    if (notifications.isEmpty()) return
    var contentWidth by remember { mutableStateOf(0f) }
    val scroll = remember { Animatable(0f) }

    LaunchedEffect(contentWidth) {
        if (contentWidth > 0f) {
            scroll.snapTo(0f)
            scroll.animateTo(
                targetValue = contentWidth,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (contentWidth / 0.05f).toInt().coerceAtLeast(6000),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Box(modifier = modifier.clipToBounds()) {
        Row(
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .offset { IntOffset(-scroll.value.toInt(), 0) }
                .onSizeChanged { contentWidth = it.width.toFloat() }
        ) {
            TickerItems(notifications)
        }
        Row(
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .offset { IntOffset((contentWidth - scroll.value).toInt(), 0) }
        ) {
            TickerItems(notifications)
        }
    }
}

@Composable
private fun TickerItems(notifications: List<Triple<String, String, Long>>) {
    val colors = LocalAppTheme.current
    notifications.forEachIndexed { index, (pkg, title, _) ->
        if (index > 0) {
            Text(
                text = " · ",
                color = colors.accent,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp
            )
        }
        Text(
            text = "$pkg: $title",
            color = colors.ink,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}
