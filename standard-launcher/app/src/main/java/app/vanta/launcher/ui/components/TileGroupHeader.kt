package app.vanta.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.SpaceGrotesk

@Composable
fun TileGroupHeader(name: String, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name.uppercase(),
            color = theme.ink,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            letterSpacing = 1.sp
        )
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .padding(start = 8.dp)
                .background(theme.accent.copy(alpha = 0.4f))
        )
    }
}

@Composable
fun TileGroup(name: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        TileGroupHeader(name = name)
        content()
    }
}
