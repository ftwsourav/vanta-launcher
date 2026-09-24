package com.xdlab.standard.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.domain.model.AppItem
import com.xdlab.standard.ui.theme.LocalAppTheme
import com.xdlab.standard.ui.theme.StandardType

@Composable
fun SwipeDownSearch(
    modifier: Modifier = Modifier,
    apps: List<AppItem>,
    onLaunch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppTheme.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val historyPrefs = remember {
        context.getSharedPreferences("standard_search_history", Context.MODE_PRIVATE)
    }
    var history by remember { mutableStateOf(historyPrefs.getString("history", "") ?: "") }
    val historyPackages = remember(history) {
        if (history.isBlank()) emptyList()
        else history.split(",").filter { it.isNotBlank() }
    }
    val recentApps = remember(apps, historyPackages) {
        historyPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }.take(6)
    }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(300)) }
    LaunchedEffect(Unit) {
        focus.requestFocus()
        delay(120)
        keyboard?.show()
    }
    BackHandler { onDismiss() }

    val results = remember(apps, query) {
        if (query.isBlank()) emptyList()
        else apps.filter { it.label.contains(query, ignoreCase = true) }.take(12)
    }
    val launch: (String) -> Unit = { pkg ->
        keyboard?.hide()
        val updated = (listOf(pkg) + historyPackages.filter { it != pkg }).take(8)
        val joined = updated.joinToString(",")
        historyPrefs.edit().putString("history", joined).apply()
        history = joined
        onLaunch(pkg)
        onDismiss()
    }
    val launchWeb: () -> Unit = {
        val q = query.trim()
        if (q.isNotBlank()) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { context.startActivity(intent) } catch (e: Exception) { }
        }
    }
    val clearHistory: () -> Unit = {
        historyPrefs.edit().remove("history").apply()
        history = ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(appear.value)
            .background(Color.Black.copy(alpha = 0.9f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        if (amount < -10f) onDismiss()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                    textStyle = StandardType.mono(18.sp, FontWeight.Bold).copy(color = colors.onInk),
                    cursorBrush = SolidColor(colors.accent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onDismiss() })
                )
                if (query.isEmpty()) {
                    MonoLabel("SEARCH APPS", size = 18.sp, color = colors.onInk.copy(alpha = 0.4f))
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 16.dp)
                    .background(colors.accent)
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onDismiss() }
                        )
                    }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter),
                    horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
                ) {
                    if (query.isNotBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(TileDefaults.Border, colors.accent)
                                    .tilePress(onTap = launchWeb, tilt = false)
                                    .button("SEARCH $query ON WEB") { launchWeb() }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MonoLabel("🔍", size = 16.sp, color = colors.accent)
                                    MonoLabel(
                                        "SEARCH '$query' ON WEB",
                                        size = 12.sp,
                                        color = colors.onInk,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        items(results, key = { it.packageName }) { app ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(TileDefaults.Border, colors.onInk.copy(alpha = 0.5f))
                                    .tilePress(onTap = { launch(app.packageName) }, tilt = false)
                                    .button(app.label) { launch(app.packageName) }
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppIcon(packageName = app.packageName, size = 40.dp)
                                MonoLabel(
                                    app.label,
                                    size = 11.sp,
                                    color = colors.onInk,
                                    maxLines = 1
                                )
                            }
                        }
                    } else if (recentApps.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MonoLabel(
                                    "RECENT",
                                    size = 11.sp,
                                    color = colors.onInk.copy(alpha = 0.6f)
                                )
                                MonoLabel(
                                    "CLEAR",
                                    size = 11.sp,
                                    color = colors.accent,
                                    modifier = Modifier
                                        .tilePress(onTap = clearHistory, tilt = false)
                                        .button("CLEAR HISTORY") { clearHistory() }
                                        .padding(4.dp)
                                )
                            }
                        }
                        items(recentApps, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(TileDefaults.Border, colors.onInk.copy(alpha = 0.5f))
                                    .tilePress(onTap = { launch(app.packageName) }, tilt = false)
                                    .button(app.label) { launch(app.packageName) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AppIcon(packageName = app.packageName, size = 32.dp)
                                MonoLabel(
                                    app.label,
                                    size = 13.sp,
                                    color = colors.onInk,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
