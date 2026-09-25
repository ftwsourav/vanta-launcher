package app.vanta.launcher.ui.components

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.vanta.launcher.ui.theme.LocalAppTheme

@Composable
fun WidgetTile(
    modifier: Modifier = Modifier,
    widgetId: Int,
    onRemove: () -> Unit
) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.tileFill)
            .border(2.dp, theme.ink)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { showMenu = true })
            }
    ) {
        AndroidView(
            factory = { ctx ->
                AppWidgetHostView(ctx).apply {
                    val info = appWidgetManager.getAppWidgetInfo(widgetId)
                    if (info != null) setAppWidget(widgetId, info)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("REMOVE WIDGET") },
                onClick = {
                    showMenu = false
                    onRemove()
                }
            )
        }
    }
}
