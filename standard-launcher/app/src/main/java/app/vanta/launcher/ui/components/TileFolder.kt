package app.vanta.launcher.ui.components

import android.content.Context
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.vanta.launcher.domain.model.AnimationStyle
import app.vanta.launcher.domain.model.AppItem
import app.vanta.launcher.domain.model.IconStyle
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

/**
 * WP 8.1 folder tile, collapsed: an outline tile with up to four mini app labels in mono and the
 * folder name along the bottom. Tap expands it inline (Home owns the expanded state), long-press renames.
 */
@Composable
fun FolderTile(
    folder: TileFolder,
    apps: List<AppItem>,
    expanded: Boolean,
    editMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    Box(modifier = modifier) {
        Tile(
            modifier = Modifier.fillMaxSize().button(folder.name + " folder", onTap),
            style = TileStyle.Outline,
            onClick = onTap,
            onLongClick = onLongPress
        ) {
            val c = LocalTileColors.current.content
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    apps.take(4).chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            pair.forEach { a -> MiniLabel(a.label, c, Modifier.weight(1f)) }
                            repeat(2 - pair.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoLabel(folder.name, size = 11.sp, color = c, weight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    MonoLabel(if (expanded) "×" else apps.size.toString(), size = 10.sp, color = c, weight = FontWeight.Bold)
                }
            }
        }
        if (editMode) {
            InkSquare("Delete folder", Modifier.align(Alignment.TopEnd), onDelete) {
                MonoLabel("✕", size = 12.sp, color = colors.onInk, weight = FontWeight.Bold)
            }
        }
    }
}

/**
 * The folder opened inline under its row: "NAME" header with a × square, then a 4-column grid of
 * small app tiles. In edit mode each tile gets a ✕ square that drops it out of the folder.
 */
@Composable
fun FolderPanel(
    folder: TileFolder,
    apps: List<AppItem>,
    iconStyle: IconStyle,
    animationStyle: AnimationStyle,
    editMode: Boolean,
    onOpenApp: (String) -> Unit,
    onRename: () -> Unit,
    onRemoveApp: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.tile)
            .border(TileDefaults.Border, colors.ink)
            .padding(TileDefaults.Gutter),
        verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel(
                folder.name,
                size = 12.sp,
                color = colors.ink,
                weight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .tilePress(onTap = onRename, tilt = false)
                    .button("Rename folder", onRename)
                    .padding(vertical = 4.dp)
            )
            Spacer(Modifier.width(TileDefaults.Gutter))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(colors.ink)
                    .tilePress(onTap = onClose, tilt = false)
                    .button("Close folder", onClose),
                contentAlignment = Alignment.Center
            ) {
                MonoLabel("×", size = 14.sp, color = colors.onInk, weight = FontWeight.Bold)
            }
        }
        if (apps.isEmpty()) {
            MonoLabel("EMPTY · HOLD A TILE IN EDIT MODE → ADD TO FOLDER", size = 10.sp, color = colors.muted)
        }
        apps.chunked(4).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)) {
                row.forEachIndexed { i, app ->
                    TileEntrance(index = r * 4 + i, modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        Box(Modifier.fillMaxSize()) {
                            AppTile(
                                app = app,
                                iconStyle = iconStyle,
                                animationStyle = animationStyle,
                                onTap = { onOpenApp(app.packageName) },
                                modifier = Modifier.fillMaxSize(),
                                caption = app.caption,
                                titleSize = 22.sp,
                                liveEnabled = false
                            )
                            if (editMode) {
                                InkSquare("Remove from folder", Modifier.align(Alignment.TopEnd), { onRemoveApp(app.packageName) }) {
                                    MonoLabel("✕", size = 12.sp, color = colors.onInk, weight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Brutalist text prompt: outline paper box, mono title, one field, optional suggestion rows, square buttons. */
@Composable
fun BrutalTextDialog(
    title: String,
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    suggestions: List<String> = emptyList(),
    onReset: (() -> Unit)? = null
) {
    val colors = LocalAppTheme.current
    var draft by remember(initial) { mutableStateOf(initial) }
    val save = { onSave(draft.trim()) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.tile)
                .border(TileDefaults.Border, colors.ink)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MonoLabel(title, size = 11.sp, color = colors.ink, weight = FontWeight.Bold)
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = StandardType.mono(14.sp, FontWeight.Bold).copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(TileDefaults.Border, colors.ink)
                    .padding(12.dp)
            )
            suggestions.forEach { s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.ink)
                        .tilePress(onTap = { draft = s }, tilt = false)
                        .button(s) { draft = s }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoLabel(s, size = 10.sp, color = colors.ink, maxLines = 1, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    MonoLabel("↑", size = 10.sp, color = colors.muted)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SquareAction("SAVE", filled = true, modifier = Modifier.weight(1f), onTap = save)
                if (onReset != null) SquareAction("RESET", filled = false, modifier = Modifier.weight(1f), onTap = onReset)
                SquareAction("CANCEL", filled = false, modifier = Modifier.weight(1f), onTap = onDismiss)
            }
        }
    }
}

/** "ADD TO FOLDER…": existing folders as rows, then NEW FOLDER. */
@Composable
fun FolderPickerDialog(
    folders: List<TileFolder>,
    onPick: (TileFolder) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.tile)
                .border(TileDefaults.Border, colors.ink)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonoLabel("ADD TO FOLDER", size = 11.sp, color = colors.ink, weight = FontWeight.Bold)
            folders.forEach { f ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .border(TileDefaults.Border, colors.ink)
                        .tilePress(onTap = { onPick(f) }, tilt = false)
                        .button(f.name) { onPick(f) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoLabel(f.name, size = 12.sp, color = colors.ink, weight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    MonoLabel("${f.appPackages.size} APPS", size = 10.sp, color = colors.muted)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SquareAction("+ NEW FOLDER", filled = true, modifier = Modifier.weight(1f), onTap = onNew)
                SquareAction("CANCEL", filled = false, modifier = Modifier.weight(1f), onTap = onDismiss)
            }
        }
    }
}

/** Square 44dp action: ink-filled for the primary, outline for the rest. */
@Composable
fun SquareAction(label: String, filled: Boolean, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val colors = LocalAppTheme.current
    Box(
        modifier = modifier
            .height(44.dp)
            .background(if (filled) colors.ink else colors.tile)
            .border(TileDefaults.Border, colors.ink)
            .tilePress(onTap = onTap, tilt = false)
            .button(label, onTap),
        contentAlignment = Alignment.Center
    ) {
        MonoLabel(label, size = 11.sp, color = if (filled) colors.onInk else colors.ink, weight = FontWeight.Bold, maxLines = 1)
    }
}

/** One-line 1dp-boxed mono label, the WP 8.1 folder-preview cell. */
@Composable
private fun MiniLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(18.dp).border(1.dp, color).padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        MonoLabel(text, size = 8.sp, color = color, weight = FontWeight.Bold, maxLines = 1)
    }
}

/** 40dp hit area around a 24dp ink square (mirrors Home's edit buttons). */
@Composable
private fun InkSquare(label: String, modifier: Modifier, onTap: () -> Unit, content: @Composable () -> Unit) {
    val ink = LocalAppTheme.current.ink
    Box(
        modifier = modifier.size(40.dp).tilePress(onTap = onTap, tilt = false).button(label, onTap),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(24.dp).background(ink), contentAlignment = Alignment.Center) { content() }
    }
}

/** Folders persisted in prefs "standard_folders" -> "folders" (JSON). Empty folders are dropped on save. */
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
        folders.filter { it.appPackages.isNotEmpty() }.forEach { f ->
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

    fun createFolder(vararg apps: String): TileFolder {
        val folders = load()
        val folder = TileFolder(
            id = UUID.randomUUID().toString(),
            name = "FOLDER ${folders.size + 1}",
            appPackages = apps.toList().distinct()
        )
        save(folders + folder)
        return folder
    }

    fun addToFolder(folderId: String, appPackage: String) {
        // One app lives in at most one folder.
        save(load().map {
            when {
                it.id == folderId -> it.copy(appPackages = (it.appPackages + appPackage).distinct())
                else -> it.copy(appPackages = it.appPackages - appPackage)
            }
        })
    }

    fun removeFromFolder(folderId: String, appPackage: String) {
        save(load().map { if (it.id == folderId) it.copy(appPackages = it.appPackages - appPackage) else it })
    }

    fun renameFolder(folderId: String, name: String) {
        save(load().map { if (it.id == folderId) it.copy(name = name) else it })
    }

    fun deleteFolder(folderId: String) {
        save(load().filterNot { it.id == folderId })
    }
}
