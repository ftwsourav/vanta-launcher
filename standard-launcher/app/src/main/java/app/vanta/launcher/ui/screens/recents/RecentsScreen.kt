package app.vanta.launcher.ui.screens.recents

import android.app.ActivityManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.SpaceGrotesk
import app.vanta.launcher.ui.theme.JetBrainsMono

private data class RecentApp(
    val packageName: String,
    val label: String
)

@Composable
fun RecentsScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onOpenApp: (String) -> Unit
) {
    val colors = LocalAppTheme.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var recents by remember { mutableStateOf<List<RecentApp>>(emptyList()) }
    var removed by remember { mutableStateOf(setOf<String>()) }
    val appear = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 800f))
        recents = loadRecents(context)
    }

    val visible = recents.filter { it.packageName !in removed }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.96f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, amount ->
                        if (amount < -120f) onDismiss()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "recents",
                    color = colors.ink,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp
                )
                Text(
                    text = "CLEAR ALL",
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                removed = recents.map { it.packageName }.toSet()
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                )
            }
            Spacer(Modifier.height(12.dp))

            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO RECENT APPS",
                        color = colors.ink.copy(alpha = 0.5f),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visible, key = { it.packageName }) { app ->
                        RecentCard(
                            app = app,
                            onOpen = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenApp(app.packageName)
                            },
                            onKill = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                try {
                                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                                    am.killBackgroundProcesses(app.packageName)
                                } catch (e: Exception) { }
                                removed = removed + app.packageName
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCard(
    app: RecentApp,
    onOpen: () -> Unit,
    onKill: () -> Unit
) {
    val colors = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.ink.copy(alpha = 0.3f))
            .background(colors.tile)
            .pointerInput(Unit) { detectTapGestures(onTap = { onOpen() }) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(onHorizontalDrag = { _, amount ->
                    if (amount > 200f) onKill()
                })
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                app.label.take(1).uppercase(),
                color = colors.accent,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label.uppercase(),
                color = colors.ink,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = app.packageName,
                color = colors.ink.copy(alpha = 0.5f),
                fontFamily = JetBrainsMono,
                fontSize = 10.sp
            )
        }
        Text(
            text = "X",
            color = colors.accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .size(32.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = { onKill() }) }
        )
    }
}

private fun loadRecents(context: Context): List<RecentApp> {
    return try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = context.packageManager
        val tasks = try { am.runningAppProcesses } catch (e: Exception) { null } ?: return emptyList()
        val seen = mutableSetOf<String>()
        val result = mutableListOf<RecentApp>()
        for (info in tasks) {
            for (pkg in info.pkgList) {
                if (pkg == context.packageName) continue
                if (pkg in seen) continue
                seen.add(pkg)
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) { pkg.substringAfterLast('.') }
                result.add(RecentApp(pkg, label))
                if (result.size >= 15) return result
            }
        }
        result
    } catch (e: Exception) {
        emptyList()
    }
}
