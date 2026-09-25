package app.vanta.launcher.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.domain.model.AppItem
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.StandardType
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TileFolder(
    val id: String,
    val name: String,
    val appPackages: List<String>,
    val color: Long = 0xFF000000
)

@Composable
fun FolderTile(
    folder: TileFolder,
    apps: List<AppItem>,
    onOpenApp: (String) -> Unit,
    onRename: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val resolved = remember(folder.appPackages, apps) {
        folder.appPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
    }
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !expanded,
            enter = scaleIn(animationSpec = springSpec),
            exit = scaleOut(animationSpec = springSpec)
        ) {
            CollapsedFolderTile(
                folder = folder,
                apps = resolved,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                onTap = { expanded = true }
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = scaleIn(animationSpec = springSpec),
            exit = scaleOut(animationSpec = springSpec)
        ) {
            ExpandedFolderTile(
                folder = folder,
                apps = resolved,
                onOpenApp = onOpenApp,
                onRename = onRename,
                onRemoveApp = onRemoveApp,
                onClose = { expanded = false },
                modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp)
            )
        }
    }
}

@Composable
private fun CollapsedFolderTile(
    folder: TileFolder,
    apps: List<AppItem>,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val fill = Color(folder.color)
    Tile(
        modifier = modifier,
        style = TileStyle.Outline,
        fill = fill,
        onClick = onTap
    ) {
        val content = LocalTileColors.current.content
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            val preview = apps.take(4)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                preview.chunked(2).forEach { rowApps ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowApps.forEach { a ->
                            Box(Modifier.size(20.dp)) { AppIcon(a.packageName, 20.dp) }
                        }
                        repeat(2 - rowApps.size) { Spacer(Modifier.size(20.dp)) }
                    }
                }
            }
            Text(
                text = folder.name.uppercase(),
                style = StandardType.mono(10.sp),
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExpandedFolderTile(
    folder: TileFolder,
    apps: List<AppItem>,
    onOpenApp: (String) -> Unit,
    onRename: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    val fill = Color(folder.color)
    var renaming by remember { mutableStateOf(false) }
    var draft by remember(folder.name) { mutableStateOf(folder.name) }

    Tile(
        modifier = modifier,
        style = TileStyle.Outline,
        fill = fill
    ) {
        val content = LocalTileColors.current.content
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (renaming) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = StandardType.mono(12.sp).copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.accent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (draft.isNotBlank()) onRename(draft.trim())
                        renaming = false
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.tile)
                        .padding(6.dp)
                )
            } else {
                Text(
                    text = folder.name.uppercase(),
                    style = StandardType.mono(12.sp, FontWeight.Bold),
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (apps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    apps.chunked(3).forEach { rowApps ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowApps.forEach { a ->
                                AppGridCell(
                                    app = a,
                                    onOpenApp = onOpenApp,
                                    onRemoveApp = onRemoveApp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoButton("RENAME", content) { renaming = true }
                Spacer(Modifier.weight(1f))
                MonoButton("CLOSE", content) { onClose() }
            }
        }
    }
}

@Composable
private fun AppGridCell(
    app: AppItem,
    onOpenApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val content = LocalTileColors.current.content
    Column(
        modifier = modifier
            .padding(4.dp)
            .tilePress(
                onTap = { onOpenApp(app.packageName) },
                onLongPress = { onRemoveApp(app.packageName) },
                tilt = false
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(app.packageName, 32.dp)
        Spacer(Modifier.size(4.dp))
        Text(
            text = app.label.uppercase(),
            style = StandardType.mono(9.sp),
            color = content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MonoButton(
    text: String,
    content: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(1.dp, content)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .tilePress(onTap = onClick, tilt = false)
    ) {
        Text(
            text = text,
            style = StandardType.mono(10.sp, FontWeight.Bold),
            color = content
        )
    }
}

class FolderStore(context: Context) {
    private val prefs = context.getSharedPreferences("standard_folders", Context.MODE_PRIVATE)

    fun load(): List<TileFolder> {
        val raw = prefs.getString("folders", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val pkgs = o.optJSONArray("apps")?.let { a ->
                    (0 until a.length()).map { a.getString(it) }
                } ?: emptyList()
                TileFolder(
                    id = o.getString("id"),
                    name = o.optString("name", "FOLDER"),
                    appPackages = pkgs,
                    color = o.optLong("color", 0xFF000000)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(folders: List<TileFolder>) {
        val arr = JSONArray()
        folders.forEach { f ->
            val o = JSONObject()
            o.put("id", f.id)
            o.put("name", f.name)
            o.put("color", f.color)
            val a = JSONArray()
            f.appPackages.forEach { a.put(it) }
            o.put("apps", a)
            arr.put(o)
        }
        prefs.edit().putString("folders", arr.toString()).apply()
    }

    fun createFolder(app1: String, app2: String): TileFolder {
        val folders = load().toMutableList()
        val folder = TileFolder(
            id = UUID.randomUUID().toString(),
            name = "FOLDER",
            appPackages = listOf(app1, app2).distinct(),
            color = 0xFF000000
        )
        folders.add(folder)
        save(folders)
        return folder
    }

    fun addToFolder(folderId: String, appPackage: String) {
        val folders = load().map {
            if (it.id == folderId) {
                it.copy(appPackages = (it.appPackages + appPackage).distinct())
            } else {
                it
            }
        }
        save(folders)
    }

    fun removeFromFolder(folderId: String, appPackage: String) {
        val folders = load().map {
            if (it.id == folderId) {
                it.copy(appPackages = it.appPackages - appPackage)
            } else {
                it
            }
        }
        save(folders)
    }

    fun renameFolder(folderId: String, name: String) {
        val folders = load().map {
            if (it.id == folderId) it.copy(name = name) else it
        }
        save(folders)
    }
}

@Composable
fun Modifier.folderDropTarget(
    enabled: Boolean,
    draggedPackage: String?,
    onDrop: (String) -> Unit
): Modifier {
    val colors = LocalAppTheme.current
    var hovered by remember { mutableStateOf(false) }
    val currentPkg by rememberUpdatedState(draggedPackage)
    val currentDrop by rememberUpdatedState(onDrop)

    val detector = Modifier.pointerInput(enabled) {
        if (!enabled) {
            hovered = false
            return@pointerInput
        }
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Enter -> hovered = true
                    PointerEventType.Exit -> hovered = false
                    PointerEventType.Release -> {
                        if (hovered && currentPkg != null) currentDrop(currentPkg!!)
                        hovered = false
                    }
                    else -> {}
                }
            }
        }
    }

    return this
        .then(detector)
        .then(
            if (enabled && hovered && draggedPackage != null) {
                Modifier.border(2.dp, colors.accent)
            } else {
                Modifier
            }
        )
}
