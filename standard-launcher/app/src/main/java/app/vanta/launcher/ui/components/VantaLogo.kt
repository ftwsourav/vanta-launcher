package app.vanta.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.JetBrainsMono
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.SpaceGrotesk

@Composable
fun VantaLogo(
    style: VantaLogoStyle = VantaLogoStyle.MetroTile,
    size: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val accent = colors.accent
    val ink = colors.ink
    val bg = colors.background
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            when (style) {
                VantaLogoStyle.MetroTile -> {
                    drawRect(color = bg, size = Size(w, h))
                    drawRect(
                        color = ink,
                        topLeft = Offset(w * 0.09f, h * 0.09f),
                        size = Size(w * 0.82f, h * 0.82f),
                        style = Stroke(width = w * 0.012f)
                    )
                    // Same geometry as the launcher icon: chunky V inside the outline, accent square top-right.
                    val l = w * 0.09f
                    val t = h * 0.09f
                    val s = w * 0.82f
                    val path = Path().apply {
                        moveTo(l + s * 0.135f, t + s * 0.19f)
                        lineTo(l + s * 0.346f, t + s * 0.19f)
                        lineTo(l + s * 0.46f, t + s * 0.596f)
                        lineTo(l + s * 0.577f, t + s * 0.19f)
                        lineTo(l + s * 0.788f, t + s * 0.19f)
                        lineTo(l + s * 0.558f, t + s * 0.846f)
                        lineTo(l + s * 0.365f, t + s * 0.846f)
                        close()
                    }
                    drawPath(path, color = ink)
                    drawRect(
                        color = accent,
                        topLeft = Offset(l + s * 0.83f, t + s * 0.04f),
                        size = Size(s * 0.13f, s * 0.13f)
                    )
                }
                VantaLogoStyle.AccentBlock -> {
                    drawRect(color = bg, size = Size(w, h))
                    drawRect(
                        color = accent,
                        topLeft = Offset(w * 0.09f, h * 0.09f),
                        size = Size(w * 0.82f, h * 0.82f)
                    )
                    val path = Path().apply {
                        moveTo(w * 0.27f, h * 0.31f)
                        lineTo(w * 0.5f, h * 0.7f)
                        lineTo(w * 0.73f, h * 0.31f)
                        lineTo(w * 0.62f, h * 0.31f)
                        lineTo(w * 0.5f, h * 0.55f)
                        lineTo(w * 0.38f, h * 0.31f)
                        close()
                    }
                    drawPath(path, color = bg)
                }
                VantaLogoStyle.FourTile -> {
                    drawRect(color = bg, size = Size(w, h))
                    drawRect(
                        color = accent,
                        topLeft = Offset(w * 0.11f, h * 0.11f),
                        size = Size(w * 0.35f, h * 0.35f)
                    )
                    drawRect(
                        color = ink,
                        topLeft = Offset(w * 0.54f, h * 0.11f),
                        size = Size(w * 0.35f, h * 0.35f)
                    )
                    drawRect(
                        color = ink,
                        topLeft = Offset(w * 0.11f, h * 0.54f),
                        size = Size(w * 0.35f, h * 0.35f)
                    )
                    drawRect(
                        color = accent,
                        topLeft = Offset(w * 0.54f, h * 0.54f),
                        size = Size(w * 0.35f, h * 0.35f)
                    )
                }
            }
        }
    }
}

@Composable
fun VantaLogoWithWordmark(
    style: VantaLogoStyle = VantaLogoStyle.MetroTile,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VantaLogo(style = style, size = 100.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "VANTA",
            color = colors.ink,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            letterSpacing = 6.sp
        )
        Box(Modifier.width(120.dp).height(3.dp).background(colors.accent))
        Spacer(Modifier.height(6.dp))
        Text(
            text = "A CLEAN, POWERFUL LAUNCHER",
            color = colors.ink.copy(alpha = 0.5f),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}

enum class VantaLogoStyle { MetroTile, AccentBlock, FourTile }
