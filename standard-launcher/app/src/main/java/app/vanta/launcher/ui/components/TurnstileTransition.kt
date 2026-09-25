package app.vanta.launcher.ui.components

import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.SpaceGrotesk

@Composable
fun TurnstileOverlay(
    visible: Boolean,
    tileBounds: RectF?,
    appLabel: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val progress = remember { Animatable(0f) }
    val localDensity = LocalDensity.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(visible) {
        if (visible) {
            progress.animateTo(1f, tween(durationMillis = 500, easing = LumiaEasing))
        } else if (progress.value > 0f) {
            progress.animateTo(0f, tween(durationMillis = 500, easing = LumiaEasing))
            currentOnDismiss()
        }
    }

    val bounds = tileBounds
    if (bounds != null && (visible || progress.value > 0f)) {
        BoxWithConstraints(modifier = modifier) {
            val screenW = with(localDensity) { maxWidth.toPx() }.coerceAtLeast(1f)
            val screenH = with(localDensity) { maxHeight.toPx() }.coerceAtLeast(1f)
            val tileW = bounds.width().coerceAtLeast(1f)
            val tileH = bounds.height().coerceAtLeast(1f)
            val tileCenterX = bounds.left + tileW / 2f
            val tileCenterY = bounds.top + tileH / 2f
            val screenCenterX = screenW / 2f
            val screenCenterY = screenH / 2f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.tileFill)
                    .graphicsLayer {
                        val p = progress.value
                        val sx = lerp(tileW / screenW, 1f, p)
                        val sy = lerp(tileH / screenH, 1f, p)
                        val tx = lerp(tileCenterX - screenCenterX, 0f, p)
                        val ty = lerp(tileCenterY - screenCenterY, 0f, p)
                        val rotY = if (p <= 0.5f) {
                            (p / 0.5f) * 90f
                        } else {
                            90f + ((p - 0.5f) / 0.5f) * 270f
                        }
                        this.scaleX = sx
                        this.scaleY = sy
                        this.translationX = tx
                        this.translationY = ty
                        this.rotationY = rotY
                        this.transformOrigin = TransformOrigin(0.5f, 0.5f)
                        this.cameraDistance = 16f * density
                    },
                contentAlignment = Alignment.Center
            ) {
                if (appLabel != null) {
                    Text(
                        text = appLabel,
                        color = colors.onTile,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}

class TurnstileController {
    var active by mutableStateOf(false)
        private set
    var bounds by mutableStateOf<RectF?>(null)
        private set
    var label by mutableStateOf<String?>(null)
        private set

    fun launch(bounds: RectF, label: String) {
        this.bounds = bounds
        this.label = label
        active = true
    }

    fun dismiss() {
        active = false
    }
}

@Composable
fun rememberTurnstile(): TurnstileController = remember { TurnstileController() }

@Composable
fun Modifier.captureTileBounds(
    controller: TurnstileController,
    label: String,
    onCaptured: (RectF) -> Unit
): Modifier {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    return this
        .onGloballyPositioned { coords = it }
        .pointerInput(label) {
            detectTapGestures(
                onTap = { _ ->
                    val c = coords
                    if (c != null) {
                        val pos = c.positionInRoot()
                        val rect = RectF(
                            pos.x,
                            pos.y,
                            pos.x + c.size.width.toFloat(),
                            pos.y + c.size.height.toFloat()
                        )
                        controller.launch(rect, label)
                        currentOnCaptured(rect)
                    }
                }
            )
        }
}
