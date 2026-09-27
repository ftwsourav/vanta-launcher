package app.vanta.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme

/** Section caption for a tile group: mono bold label, a gutter, then a 2dp ink rule to the edge. */
@Composable
fun TileGroupHeader(name: String, modifier: Modifier = Modifier) {
    val ink = LocalAppTheme.current.ink
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonoLabel(name, size = 11.sp, color = ink, weight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.width(TileDefaults.Gutter))
        Box(Modifier.weight(1f).height(TileDefaults.Border).background(ink))
    }
}

@Composable
fun TileGroup(name: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)) {
        TileGroupHeader(name = name)
        content()
    }
}
