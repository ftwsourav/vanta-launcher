package app.vanta.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.*

data class AppBarAction(
    val label: String,
    val glyph: String,
    val onClick: () -> Unit
)

@Composable
fun WpAppBar(
    modifier: Modifier = Modifier,
    actions: List<AppBarAction> = emptyList(),
    menuItems: List<String> = emptyList(),
    onMenuItemClick: (String) -> Unit = {}
) {
    val colors = LocalAppTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.tileFill.copy(alpha = 0.92f))
            .drawBehind {
                drawLine(
                    color = colors.ink.copy(alpha = 0.2f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp)
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.take(4).forEach { action ->
                WpCircleButton(glyph = action.glyph, onClick = action.onClick)
            }
        }
        var menuExpanded by remember { mutableStateOf(false) }
        Box {
            WpCircleButton(
                glyph = "â‹¯",
                onClick = { menuExpanded = !menuExpanded }
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                menuItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            menuExpanded = false
                            onMenuItemClick(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WpCircleButton(
    glyph: String,
    onClick: () -> Unit
) {
    val colors = LocalAppTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (pressed) colors.accent else Color.Transparent)
            .border(2.dp, colors.ink.copy(alpha = 0.3f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            color = if (pressed) Color.White else colors.ink,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}
