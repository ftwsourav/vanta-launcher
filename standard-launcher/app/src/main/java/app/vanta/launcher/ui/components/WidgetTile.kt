package app.vanta.launcher.ui.components

import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.vanta.launcher.ui.theme.LocalAppTheme

private const val MIN_HEIGHT_DP = 112
private const val MAX_HEIGHT_DP = 480
private const val STEP_DP = 56

/**
 * An Android widget hosted in a paper tile. Long-press opens the brutalist resize/remove menu.
 * Height comes from [entry]; width is the caller's (span-based) modifier.
 * If the provider was uninstalled the tile reads WIDGET UNAVAILABLE and a tap calls [onRemove].
 */
@Composable
fun WidgetTile(
    entry: WidgetEntry,
    modifier: Modifier = Modifier,
    onRemove: () -> Unit,
    onResize: (WidgetEntry) -> Unit = {}
) {
    val context = LocalContext.current
    val available = remember(entry.id) { VantaWidgetHost.info(context, entry.id) != null }
    val sized = modifier.height(entry.heightDp.dp)

    if (!available) {
        Tile(modifier = sized, style = TileStyle.Outline, onClick = onRemove) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MonoLabel("WIDGET UNAVAILABLE · TAP TO REMOVE", size = 10.sp, color = LocalTileColors.current.content, maxLines = 1)
            }
        }
        return
    }

    val hapticsOn = LocalHapticsEnabled.current
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }

    Tile(modifier = sized, style = TileStyle.Outline, contentPadding = 0.dp) {
        val tc = LocalTileColors.current
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                // Created inside the factory (not remembered) so a re-entering tile never re-parents a live view.
                factory = { ctx -> VantaWidgetHost.createView(ctx, entry.id) ?: View(ctx) },
                update = { v ->
                    (v as? VantaWidgetHostView)?.onLongPress = {
                        if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (entry.label.isNotBlank()) {
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 6.dp)
                        .background(tc.fill)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(Modifier.width(18.dp).height(2.dp).background(tc.content))
                    MonoLabel(entry.label, size = 9.sp, color = tc.content, maxLines = 1)
                }
            }
            if (menu) {
                WidgetMenu(
                    entry = entry,
                    onDismiss = { menu = false },
                    onResize = { menu = false; onResize(it) },
                    onRemove = { menu = false; onRemove() }
                )
            }
        }
    }
}

@Deprecated("Use WidgetTile(entry, modifier, onRemove, onResize)")
@Composable
fun WidgetTile(modifier: Modifier = Modifier, widgetId: Int, onRemove: () -> Unit) {
    WidgetTile(entry = WidgetEntry(widgetId), modifier = modifier, onRemove = onRemove)
}

/** Span steps 1 <-> 2 <-> 4. */
private fun wider(span: Int) = if (span < 2) 2 else 4
private fun narrower(span: Int) = if (span > 2) 2 else 1

/** Brutalist long-press menu: an outline tile of mono rows, sliding in from the tile corner. */
@Composable
private fun WidgetMenu(
    entry: WidgetEntry,
    onDismiss: () -> Unit,
    onResize: (WidgetEntry) -> Unit,
    onRemove: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(200, easing = LumiaEasing)) }
    val offsetPx = with(LocalDensity.current) { 12.dp.roundToPx() }
    val rows = listOf(
        "TALLER" to entry.copy(heightDp = (entry.heightDp + STEP_DP).coerceAtMost(MAX_HEIGHT_DP)),
        "SHORTER" to entry.copy(heightDp = (entry.heightDp - STEP_DP).coerceAtLeast(MIN_HEIGHT_DP)),
        "WIDER" to entry.copy(span = wider(entry.span)),
        "NARROWER" to entry.copy(span = narrower(entry.span))
    )
    Popup(
        offset = IntOffset(offsetPx, offsetPx),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Tile(
            modifier = Modifier
                .width(200.dp)
                .graphicsLayer {
                    val p = progress.value
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = 0.92f + 0.08f * p
                    scaleY = 0.92f + 0.08f * p
                    alpha = p
                },
            style = TileStyle.Outline,
            contentPadding = 0.dp
        ) {
            val tc = LocalTileColors.current
            Column {
                rows.forEach { (label, next) ->
                    val enabled = next != entry
                    MenuRow(label, tc.content.copy(alpha = if (enabled) 1f else 0.35f)) { if (enabled) onResize(next) }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(tc.outline))
                }
                MenuRow("REMOVE", LocalAppTheme.current.accent, onRemove)
            }
        }
    }
}

@Composable
private fun MenuRow(label: String, color: Color, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .tilePress(onTap = onTap, tilt = false)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        MonoLabel(label, size = 12.sp, color = color)
    }
}
