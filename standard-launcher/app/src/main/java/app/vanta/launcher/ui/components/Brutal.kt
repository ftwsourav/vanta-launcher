package app.vanta.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.domain.model.AppItem
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.StandardType
import kotlin.random.Random

/**
 * Giant Space Grotesk headline. [stacked] puts one word per line like the mockups.
 * The shipped font tops out at weight 700, so the fill is drawn once more as a thin outline
 * ([boost] of the size) to reach the mockups' condensed-black density.
 */
@Composable
fun HeadlineText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = LocalAppTheme.current.ink,
    stacked: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    boost: Float = 0.045f
) {
    val shown = if (stacked) text.trim().replace(Regex("\\s+"), "\n") else text
    val style = StandardType.display(size)
    Box(modifier = modifier) {
        Text(
            text = shown,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign
        )
        if (boost > 0f) {
            Text(
                text = shown,
                style = style.copy(drawStyle = Stroke(width = size.value * boost * LocalDensity.current.density, join = StrokeJoin.Round)),
                color = color,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign
            )
        }
    }
}

/**
 * Display headline that steps its size down (4sp at a time, never below [minSize]) until every line
 * fits the available width, so words never break mid-word. Stacked text is measured line by line.
 * Draws nothing on the first frame (width unknown) instead of flashing at the wrong size.
 */
@Composable
fun FitHeadlineText(
    text: String,
    maxSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = LocalAppTheme.current.ink,
    minSize: TextUnit = 18.sp,
    stacked: Boolean = false,
    maxLines: Int = if (stacked) Int.MAX_VALUE else 1,
    boost: Float = 0.045f,
    textAlign: TextAlign? = null
) {
    val measurer = rememberTextMeasurer()
    var widthPx by remember { mutableIntStateOf(0) }
    val shown = if (stacked) text.trim().replace(Regex("\\s+"), "\n") else text
    val lines = remember(shown) { shown.split('\n') }
    val size = remember(shown, widthPx, maxSize, minSize) {
        var s = maxSize.value
        fun tooWide(sz: Float) = lines.any { line ->
            measurer.measure(line, StandardType.display(sz.sp), softWrap = false, maxLines = 1).size.width > widthPx
        }
        while (widthPx > 0 && s - 4f >= minSize.value && tooWide(s)) s -= 4f
        s.sp
    }
    HeadlineText(
        text = shown,
        size = size,
        color = color,
        maxLines = maxLines,
        boost = boost,
        textAlign = textAlign,
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width }
            .alpha(if (widthPx == 0) 0f else 1f)
    )
}

/** Uppercase JetBrains Mono meta label. */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 11.sp,
    color: Color = LocalAppTheme.current.ink,
    weight: FontWeight = FontWeight.Medium,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null
) {
    Text(
        text = text.uppercase(),
        style = StandardType.mono(size, weight),
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign
    )
}

/** Section caption on the page background, e.g. "UTILITIES / CREATIVITY / LIFE". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, size: TextUnit = 11.sp) {
    MonoLabel(text, modifier = modifier, size = size, color = LocalAppTheme.current.ink.copy(alpha = 0.85f))
}

/**
 * The mockups' callout: MAKE / SHIT / HAPPEN. on the accent tile, a stamp top-right,
 * a mono caption bottom-left and a short ink rule bottom-right.
 */
@Composable
fun CalloutTile(
    headline: String,
    modifier: Modifier = Modifier,
    style: TileStyle = TileStyle.Accent,
    topStamp: String? = null,
    bottomCaption: String? = null,
    headlineSize: TextUnit = 44.sp,
    onClick: (() -> Unit)? = null
) {
    Tile(modifier = modifier, style = style, onClick = onClick) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (topStamp != null) {
                    MonoLabel(
                        topStamp,
                        size = 10.sp,
                        color = c.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(4.dp))
                }
                HeadlineText(headline, headlineSize, color = c, stacked = true)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (bottomCaption != null) {
                    MonoLabel(bottomCaption, size = 10.sp, color = c.copy(alpha = 0.85f), modifier = Modifier.weight(1f, fill = false))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.width(36.dp).height(8.dp).background(c))
            }
        }
    }
}

/** A quote in mono with the mockups' trailing em dash. */
@Composable
fun QuoteTile(
    quote: String,
    modifier: Modifier = Modifier,
    style: TileStyle = TileStyle.Ink,
    size: TextUnit = 14.sp,
    onClick: (() -> Unit)? = null
) {
    Tile(modifier = modifier, style = style, onClick = onClick) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            MonoLabel("“" + quote + "”", size = size, color = c)
            MonoLabel("—", size = size, color = c)
        }
    }
}

/** 06 / SEP 2026 / SUNDAY. */
@Composable
fun DateTile(modifier: Modifier = Modifier, style: TileStyle = TileStyle.Outline, daySize: TextUnit = 56.sp) {
    val day = liveTimeFormatted("dd")
    val monthYear = liveTimeFormatted("MMM yyyy")
    val weekday = liveTimeFormatted("EEEE")
    Tile(modifier = modifier, style = style) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            HeadlineText(day, daySize, color = c)
            Column {
                MonoLabel(monthYear, size = 11.sp, color = c, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(c))
                Spacer(Modifier.height(6.dp))
                MonoLabel(weekday, size = 11.sp, color = c, maxLines = 1)
            }
        }
    }
}

/** Deterministic barcode bars. */
@Composable
fun Barcode(modifier: Modifier = Modifier, color: Color = LocalTileColors.current.content, bars: Int = 64) {
    Canvas(modifier = modifier) {
        val random = Random(0x5EEDL)
        val barW = size.width / bars
        repeat(bars) { i ->
            if (random.nextFloat() > 0.42f) {
                val w = (barW * (1f + random.nextFloat())).coerceAtMost(barW * 2f)
                drawRect(color = color, topLeft = Offset(i * barW, 0f), size = Size(w, size.height))
            }
        }
    }
}

/** Footer: mantra words stacked on the left, barcode and caption on the right. */
@Composable
fun BarcodeTile(
    mantra: List<String>,
    caption: String,
    modifier: Modifier = Modifier,
    style: TileStyle = TileStyle.Outline
) {
    Tile(modifier = modifier, style = style) {
        val c = LocalTileColors.current.content
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                mantra.forEach { MonoLabel(it, size = 12.sp, color = c) }
                Spacer(Modifier.height(6.dp))
                MonoLabel("—", size = 12.sp, color = c)
            }
            Column(
                modifier = Modifier.fillMaxHeight().width(100.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Barcode(Modifier.fillMaxWidth().height(40.dp), color = c)
                Spacer(Modifier.height(4.dp))
                MonoLabel(caption, size = 9.sp, color = c.copy(alpha = 0.8f), maxLines = 1)
            }
        }
    }
}

/** "+ / APPS" style small outlined tile. */
@Composable
fun PlusTile(label: String, modifier: Modifier = Modifier, glyph: String = "+", onClick: (() -> Unit)? = null) {
    Tile(modifier = modifier, onClick = onClick) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            HeadlineText(glyph, 40.sp, color = c)
            MonoLabel(label, size = 10.sp, color = c)
        }
    }
}

enum class RowVariant { Arrow, Caption }

/**
 * Numbered app row from the mockups. Arrow variant: "01  WHATSAPP  →".
 * Caption variant: "01 │ WHATSAPP │ CHAT / CALL" with ink dividers.
 */
@Composable
fun NumberedAppRow(
    index: Int,
    app: AppItem,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    variant: RowVariant = RowVariant.Arrow,
    caption: String? = null,
    pinned: Boolean = false,
    labelSize: TextUnit = 24.sp,
    minHeight: Dp = 56.dp
) {
    val colors = LocalAppTheme.current
    Tile(
        modifier = modifier.fillMaxWidth(),
        style = TileStyle.Outline,
        contentPadding = 0.dp,
        onClick = onLaunch,
        onLongClick = onLongPress
    ) {
        val c = LocalTileColors.current.content
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonoLabel("%02d".format(index), size = 11.sp, color = c, modifier = Modifier.padding(horizontal = 12.dp))
            if (variant == RowVariant.Caption) Box(Modifier.fillMaxHeight().width(2.dp).background(c))
            HeadlineText(
                app.label.uppercase(),
                labelSize,
                color = c,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp)
            )
            if (variant == RowVariant.Caption) {
                Box(Modifier.fillMaxHeight().width(2.dp).background(c))
                MonoLabel(
                    caption ?: app.caption ?: TileCaptions.defaultFor(app.packageName, app.label),
                    size = 9.sp,
                    color = c,
                    maxLines = 2,
                    modifier = Modifier.width(112.dp).padding(horizontal = 10.dp, vertical = 6.dp)
                )
            } else {
                if (pinned) {
                    Box(Modifier.size(8.dp).background(colors.accent))
                    Spacer(Modifier.width(10.dp))
                }
                MonoLabel("→", size = 18.sp, color = c, weight = FontWeight.Bold, modifier = Modifier.padding(end = 14.dp))
            }
        }
    }
}
