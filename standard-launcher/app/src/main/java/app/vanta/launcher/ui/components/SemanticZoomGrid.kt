package app.vanta.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme

private val AllLetters = ('A'..'Z').map { it.toString() } + "#"

/**
 * Windows Phone semantic zoom: a full-page grid of letter tiles (A-Z, #) over the list. Letters
 * with apps are outlined ink tiles; the rest sit at 35%. Scales in from 0.92 with a fade; tap a
 * letter to jump, tap the page or press Back to close.
 */
@Composable
fun SemanticZoomGrid(
    visible: Boolean,
    available: Set<String>,
    onLetterSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    var shown by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            shown = true
            progress.animateTo(1f, tween(220, easing = LumiaEasing))
        } else if (shown) {
            progress.animateTo(0f, tween(150, easing = LumiaEasing))
            shown = false
        }
    }
    if (!shown) return
    BackHandler(enabled = visible) { onDismiss() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(colors.background.copy(alpha = 0.96f * progress.value)) }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClickLabel = "Close letter grid"
            ) { onDismiss() }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(12.dp)
                .graphicsLayer {
                    val p = progress.value
                    scaleX = 0.92f + 0.08f * p
                    scaleY = 0.92f + 0.08f * p
                    alpha = p
                },
            verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter, Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
        ) {
            items(AllLetters, key = { it }) { letter ->
                val has = letter in available
                val onTap: (() -> Unit)? = if (has) ({ onLetterSelected(letter) }) else null
                Tile(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .alpha(if (has) 1f else 0.35f)
                        .semantics { contentDescription = if (has) "Jump to $letter" else "$letter, no apps" },
                    style = TileStyle.Outline,
                    contentPadding = 0.dp,
                    onClick = onTap
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        HeadlineText(letter, 32.sp, color = LocalTileColors.current.content)
                    }
                }
            }
        }
    }
}
