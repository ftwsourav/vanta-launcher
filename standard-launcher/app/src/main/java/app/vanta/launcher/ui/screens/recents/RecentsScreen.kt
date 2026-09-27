package app.vanta.launcher.ui.screens.recents

import android.app.ActivityManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.components.AppIcon
import app.vanta.launcher.ui.components.HeadlineText
import app.vanta.launcher.ui.components.LocalHapticsEnabled
import app.vanta.launcher.ui.components.LumiaEasing
import app.vanta.launcher.ui.components.MonoLabel
import app.vanta.launcher.ui.components.TileDefaults
import app.vanta.launcher.ui.components.button
import app.vanta.launcher.ui.components.metroTurnstileIn
import app.vanta.launcher.ui.components.tilePress
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.util.RefreshRate
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

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
    val hapticsOn = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    var recents by remember { mutableStateOf<List<RecentApp>>(emptyList()) }
    var removed by remember { mutableStateOf(setOf<String>()) }
    val page = remember { Animatable(0f) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        recents = loadRecents(context)
        page.animateTo(1f, tween(220, easing = LumiaEasing))
    }

    val dismiss: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                page.animateTo(0f, tween(200, easing = LumiaEasing))
                currentOnDismiss()
            }
        }
    }
    BackHandler { dismiss() }

    val kill: (String) -> Unit = { pkg ->
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(pkg)
        } catch (e: Exception) { }
        removed = removed + pkg
    }
    val clearAll: () -> Unit = {
        if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        recents.forEach { if (it.packageName !in removed) kill(it.packageName) }
    }

    val visible = recents.filter { it.packageName !in removed }
    val outline = if (colors.filledTiles) colors.outline else colors.ink

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = page.value }
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                HeadlineText("RECENTS", 40.sp)
                Spacer(Modifier.weight(1f))
                MonoLabel("${visible.size} OPEN", size = 10.sp, color = colors.muted)
            }
            Spacer(Modifier.height(TileDefaults.Gutter))

            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    HeadlineText("NO RECENT APPS", 22.sp, color = colors.muted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
                ) {
                    itemsIndexed(visible, key = { _, app -> app.packageName }) { i, app ->
                        RecentCard(
                            app = app,
                            onOpen = { onOpenApp(app.packageName) },
                            onKill = {
                                if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                kill(app.packageName)
                            },
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = tween(220, easing = LumiaEasing)
                                )
                                .metroTurnstileIn(i, staggerMs = 30)
                        )
                    }
                }
            }

            Spacer(Modifier.height(TileDefaults.Gutter))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .tilePress(onTap = clearAll, tilt = false)
                    .background(colors.tile)
                    .border(TileDefaults.Border, outline)
                    .button("CLEAR ALL") { clearAll() },
                contentAlignment = Alignment.Center
            ) {
                MonoLabel("CLEAR ALL", size = 11.sp, color = colors.onTile, weight = FontWeight.Bold)
            }
        }
    }
}

/** Outline tile card: swipe either way follows the finger and removes it past 35% of the width. */
@Composable
private fun RecentCard(
    app: RecentApp,
    onOpen: () -> Unit,
    onKill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val currentKill by rememberUpdatedState(onKill)
    val outline = if (colors.filledTiles) colors.outline else colors.ink
    val settle: () -> Unit = { scope.launch { offset.animateTo(0f, RefreshRate.springSpec()) } }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val x = offset.value
                translationX = x
                alpha = 1f - (abs(x) / size.width).coerceIn(0f, 0.7f)
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + amount) }
                    },
                    onDragEnd = {
                        val w = size.width.toFloat()
                        val x = offset.value
                        if (abs(x) > w * 0.35f) {
                            scope.launch {
                                offset.animateTo(sign(x) * w, tween(160, easing = LumiaEasing))
                                currentKill()
                            }
                        } else {
                            settle()
                        }
                    },
                    onDragCancel = { settle() }
                )
            }
            .tilePress(onTap = onOpen)
            .background(colors.tile)
            .border(TileDefaults.Border, outline)
            .button(app.label) { onOpen() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(packageName = app.packageName, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            HeadlineText(app.label.uppercase(), 22.sp, color = colors.onTile, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            MonoLabel(app.packageName, size = 9.sp, color = colors.onTile.copy(alpha = 0.6f), maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        MonoLabel("→", size = 16.sp, color = colors.onTile, weight = FontWeight.Bold)
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
