package com.xdlab.standard.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.domain.model.AppItem
import com.xdlab.standard.ui.theme.LocalAppTheme
import com.xdlab.standard.ui.theme.StandardType

/**
 * Outlined paper search tile. Tap the row to expand (keyboard opens), type to filter [apps],
 * tap a result to launch it, long-press to pin/unpin. Back or the ✕ collapses it.
 */
@Composable
fun FloatingSearchBar(
    apps: List<AppItem>,
    onLaunch: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPin: ((AppItem) -> Unit)? = null
) {
    val colors = LocalAppTheme.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val collapse: () -> Unit = {
        expanded = false
        query = ""
        keyboard?.hide()
    }
    BackHandler(enabled = expanded) { collapse() }
    LaunchedEffect(expanded) { if (expanded) focus.requestFocus() }

    val results = remember(apps, query) {
        if (query.isBlank()) emptyList()
        else apps.filter { it.label.contains(query, ignoreCase = true) }.take(8)
    }

    Tile(modifier = modifier.fillMaxWidth(), contentPadding = 0.dp) {
        val c = LocalTileColors.current.content
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .then(
                        if (expanded) Modifier
                        else Modifier
                            .tilePress(onTap = { expanded = true }, tilt = false)
                            .semantics { role = Role.Button; contentDescription = "Search apps" }
                    )
                    .padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (expanded) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            textStyle = StandardType.mono(14.sp, FontWeight.Bold).copy(color = c),
                            cursorBrush = SolidColor(colors.accent),
                            singleLine = true
                        )
                        if (query.isEmpty()) MonoLabel("SEARCH //", size = 14.sp, color = c.copy(alpha = 0.45f))
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .tilePress(onTap = collapse, tilt = false)
                            .semantics { role = Role.Button; contentDescription = "Close search" },
                        contentAlignment = Alignment.Center
                    ) {
                        MonoLabel("✕", size = 16.sp, color = c, weight = FontWeight.Bold)
                    }
                } else {
                    MonoLabel("SEARCH //", size = 14.sp, color = c, weight = FontWeight.Bold)
                }
            }
            if (expanded && results.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
                ) {
                    results.forEach { app ->
                        Row(
                            modifier = Modifier
                                .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                                .border(TileDefaults.Border, c)
                                .tilePress(
                                    onTap = { onLaunch(app.packageName); collapse() },
                                    onLongPress = onPin?.let { { it(app) } },
                                    tilt = false
                                )
                                .semantics { role = Role.Button; contentDescription = app.label }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.pinned) {
                                Box(Modifier.size(8.dp).background(colors.accent))
                                Spacer(Modifier.width(8.dp))
                            }
                            MonoLabel(app.label, size = 11.sp, color = c, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
