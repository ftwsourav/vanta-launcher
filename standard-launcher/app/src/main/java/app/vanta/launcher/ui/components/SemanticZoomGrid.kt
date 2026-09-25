package app.vanta.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme

/**
 * Pinch-to-zoom letter grid over the drawer: outlined letter tiles on an ink scrim.
 * Tap a letter to jump; tap the scrim or press Back to dismiss.
 */
@Composable
fun SemanticZoomGrid(
    letters: List<String>,
    onLetterSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    zoomProgress: Float,
    modifier: Modifier = Modifier
) {
    if (zoomProgress < 0.01f || letters.isEmpty()) return
    val colors = LocalAppTheme.current
    BackHandler(enabled = zoomProgress > 0.5f) { onDismiss() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink.copy(alpha = zoomProgress * 0.85f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClickLabel = "Close letter grid"
            ) { onDismiss() }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp)
                .graphicsLayer {
                    val rubber = (zoomProgress - 1f).coerceAtLeast(0f) * 0.3f
                    scaleX = 1f + rubber
                    scaleY = 1f + rubber
                },
            verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter),
            horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
        ) {
            itemsIndexed(letters, key = { _, letter -> letter }) { index, letter ->
                val appear = zoomProgress > index * 0.02f
                val scale = remember { Animatable(0f) }
                LaunchedEffect(appear) {
                    scale.animateTo(
                        if (appear) 1f else 0f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    )
                }
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .graphicsLayer {
                            val s = scale.value
                            scaleX = s
                            scaleY = s
                            alpha = s
                        }
                ) {
                    Tile(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { contentDescription = "Jump to $letter" },
                        style = TileStyle.Outline,
                        contentPadding = 0.dp,
                        onClick = { onLetterSelected(letter) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            HeadlineText(letter, 28.sp, color = LocalTileColors.current.content)
                        }
                    }
                }
            }
        }
    }
}
