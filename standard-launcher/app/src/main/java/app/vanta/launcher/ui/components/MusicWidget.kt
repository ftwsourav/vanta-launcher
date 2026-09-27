package app.vanta.launcher.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.JetBrainsMono
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.SpaceGrotesk
import kotlin.random.Random

private const val IdleHeightDp = 56

@Composable
fun MusicWidget(
    title: String?,
    artist: String?,
    isPlaying: Boolean,
    albumArt: Bitmap? = null,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onQueue: () -> Unit = {},
    onFavorite: () -> Unit = {},
    onShuffle: () -> Unit = {},
    size: WidgetSize = WidgetSize.MEDIUM,
    sizeCycle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    // No media session at all: collapse to one row instead of an empty card.
    val idle = title == null && !isPlaying
    val targetHeight = if (idle) IdleHeightDp else when (size) {
        WidgetSize.COMPACT -> 80
        WidgetSize.MEDIUM -> 120
        WidgetSize.EXPANDED -> 180
    }
    val height by animateFloatAsState(
        targetValue = targetHeight.toFloat(),
        animationSpec = tween(240, easing = LumiaEasing),
        label = "musicHeight"
    )
    val pulseTransition = rememberInfiniteTransition(label = "musicPulse")
    val pulseAlpha = pulseTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "musicPulseAlpha"
    )
    val accent = theme.accent

    Tile(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .drawWithContent {
                drawContent()
                // Playing: the outline breathes in accent. Drawn, not composed, so it costs no recomposition.
                if (isPlaying) {
                    val w = TileDefaults.Border.toPx()
                    drawRect(
                        color = accent.copy(alpha = pulseAlpha.value),
                        topLeft = Offset(w / 2f, w / 2f),
                        size = Size(this.size.width - w, this.size.height - w),
                        style = Stroke(w)
                    )
                }
            },
        contentPadding = if (idle) 0.dp else TileDefaults.Padding
    ) {
        val c = LocalTileColors.current.content
        if (idle) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                MonoLabel("NOTHING PLAYING // OPEN A MUSIC APP", size = 11.sp, color = c, maxLines = 1)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
                ) {
                    if (size != WidgetSize.COMPACT) {
                        AlbumArt(bitmap = albumArt, modifier = Modifier.size(44.dp))
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = if (size == WidgetSize.COMPACT) Arrangement.Center else Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.tilePress(onTap = {}, onLongPress = { sizeCycle() }, tilt = false)
                        ) {
                            HeadlineText(
                                text = title?.uppercase() ?: "UNKNOWN",
                                size = if (size == WidgetSize.COMPACT) 16.sp else 18.sp,
                                color = c,
                                maxLines = 1
                            )
                            if (size != WidgetSize.COMPACT) {
                                MonoLabel(
                                    text = artist ?: "—",
                                    size = 11.sp,
                                    color = c.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TransportGlyph("⏮", onPrevious, "Previous track")
                                TransportGlyph(if (isPlaying) "⏸" else "▶", onPlayPause, "Play or pause")
                                TransportGlyph("⏭", onNext, "Next track")
                            }
                            if (size == WidgetSize.EXPANDED) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OptionGlyph("☰", onQueue, "Queue")
                                    OptionGlyph("♡", onFavorite, "Favorite")
                                    OptionGlyph("⇄", onShuffle, "Shuffle")
                                }
                            }
                        }
                    }
                }
                if (size != WidgetSize.COMPACT) {
                    Spacer(Modifier.height(6.dp))
                    MusicProgressBar(isPlaying = isPlaying, modifier = Modifier.fillMaxWidth())
                }
                if (size == WidgetSize.EXPANDED) {
                    MusicVisualizer(isPlaying = isPlaying, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun MusicProgressBar(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalAppTheme.current
    if (!isPlaying) {
        Canvas(modifier = modifier.fillMaxWidth().height(3.dp)) {
            drawRect(colors.ink.copy(alpha = 0.2f))
        }
        return
    }
    val transition = rememberInfiniteTransition(label = "musicProgress")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "musicProgressTravel"
    )
    Canvas(modifier = modifier.fillMaxWidth().height(3.dp)) {
        drawRect(colors.ink.copy(alpha = 0.15f))
        val segmentWidth = size.width * 0.4f
        drawRect(
            color = colors.accent,
            topLeft = Offset(progress * (size.width - segmentWidth), 0f),
            size = Size(segmentWidth, size.height)
        )
    }
}

@Composable
fun MusicVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier, bars: Int = 5) {
    val colors = LocalAppTheme.current
    val phases = remember { List(bars) { Random.nextFloat() } }
    val transition = rememberInfiniteTransition(label = "musicVisualizer")
    val heights = phases.mapIndexed { index, phase ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 300, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset((phase * 600).toInt(), StartOffsetType.FastForward)
            ),
            label = "musicVisualizerBar$index"
        )
    }
    Canvas(modifier = modifier.fillMaxWidth().height(20.dp)) {
        val gap = 3.dp.toPx()
        val slot = size.width / bars.toFloat()
        val barWidth = slot - gap
        val fullHeight = size.height
        heights.forEachIndexed { index, height ->
            val fraction = if (isPlaying) height.value else 0.2f
            val h = fraction * fullHeight
            drawRect(
                color = colors.accent,
                topLeft = Offset(index.toFloat() * slot + gap / 2f, fullHeight - h),
                size = Size(barWidth, h)
            )
        }
    }
}

/** Square album art; without a bitmap it is an accent square with a note glyph. No rounding on paper. */
@Composable
fun AlbumArt(bitmap: Bitmap?, modifier: Modifier = Modifier) {
    val colors = LocalAppTheme.current
    if (bitmap != null) {
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clipToBounds()
        )
    } else {
        Box(
            modifier = modifier.background(colors.accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♪",
                color = colors.onAccent,
                fontSize = 22.sp,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun TransportGlyph(glyph: String, onTap: () -> Unit, label: String) {
    val c = LocalTileColors.current.content
    Box(
        modifier = Modifier
            .size(40.dp)
            .tilePress(onTap = onTap, tilt = false)
            .button(label, onTap),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            color = c,
            fontSize = 24.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OptionGlyph(glyph: String, onTap: () -> Unit, label: String) {
    val c = LocalTileColors.current.content
    Box(
        modifier = Modifier
            .size(28.dp)
            .tilePress(onTap = onTap, tilt = false)
            .button(label, onTap),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            color = c,
            fontSize = 18.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold
        )
    }
}
