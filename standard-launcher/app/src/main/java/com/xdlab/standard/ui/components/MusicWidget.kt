package com.xdlab.standard.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.ui.theme.JetBrainsMono
import com.xdlab.standard.ui.theme.LocalAppTheme
import com.xdlab.standard.ui.theme.SpaceGrotesk
import kotlin.random.Random

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
    val widgetHeightDp = when (size) {
        WidgetSize.COMPACT -> 80
        WidgetSize.MEDIUM -> 120
        WidgetSize.EXPANDED -> 180
    }
    val pulseTransition = rememberInfiniteTransition(label = "musicPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "musicPulseAlpha"
    )
    val sizedModifier = modifier
        .fillMaxWidth()
        .animateContentSize(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        .height(widgetHeightDp.dp)
    val tileModifier = if (isPlaying) {
        sizedModifier.border(2.dp, theme.accent.copy(alpha = pulseAlpha))
    } else {
        sizedModifier
    }

    Tile(modifier = tileModifier) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
            ) {
                if (size != WidgetSize.COMPACT) {
                    AlbumArt(bitmap = albumArt, modifier = Modifier.size(76.dp))
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = if (size == WidgetSize.COMPACT) Arrangement.Center else Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.tilePress(onTap = {}, onLongPress = { sizeCycle() }, tilt = false)
                    ) {
                        HeadlineText(
                            text = title?.uppercase() ?: "NO MUSIC PLAYING",
                            size = if (size == WidgetSize.COMPACT) 16.sp else 18.sp,
                            color = c,
                            maxLines = 1
                        )
                        if (size != WidgetSize.COMPACT) {
                            MonoLabel(
                                text = artist ?: "OPEN YOUR MUSIC APP",
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
                            TransportGlyph("\u23EE", onPrevious, "Previous track")
                            TransportGlyph(if (isPlaying) "\u23F8" else "\u25B6", onPlayPause, "Play or pause")
                            TransportGlyph("\u23ED", onNext, "Next track")
                        }
                        if (size == WidgetSize.EXPANDED) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OptionGlyph("\u2630", onQueue, "Queue")
                                OptionGlyph("\u2661", onFavorite, "Favorite")
                                OptionGlyph("\u21C4", onShuffle, "Shuffle")
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

@Composable
fun AlbumArt(bitmap: Bitmap?, modifier: Modifier = Modifier) {
    val colors = LocalAppTheme.current
    if (bitmap != null) {
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(colors.accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♪",
                color = Color.White,
                fontSize = 36.sp,
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "optionGlyphScale"
    )
    Box(
        modifier = Modifier
            .size(28.dp)
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onTap
            )
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
