package com.xdlab.standard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.ui.theme.LocalAppTheme
import com.xdlab.standard.ui.theme.SpaceGrotesk
import com.xdlab.standard.ui.theme.StandardType
import org.json.JSONArray
import org.json.JSONObject

data class TaskItem(
    val id: Int,
    val text: String,
    val completed: Boolean
)

class TaskStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("standard_tasks", android.content.Context.MODE_PRIVATE)

    fun load(): List<TaskItem> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                TaskItem(
                    id = o.getInt("id"),
                    text = o.getString("text"),
                    completed = o.getBoolean("completed")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(tasks: List<TaskItem>) {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("text", t.text)
                    put("completed", t.completed)
                }
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private companion object {
        const val KEY = "tasks"
    }
}

@Composable
fun TasksWidget(
    tasks: List<TaskItem>,
    onToggle: (Int) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val keyboard = LocalSoftwareKeyboardController.current
    var input by remember { mutableStateOf("") }
    val completed = tasks.count { it.completed }

    val submit: () -> Unit = {
        if (input.isNotBlank()) {
            onAdd(input.trim())
            input = ""
            keyboard?.hide()
        }
    }

    Tile(modifier = modifier.fillMaxWidth().height(160.dp), style = TileStyle.Outline, contentPadding = 0.dp) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonoLabel("TASKS", size = 11.sp, color = c)
                MonoLabel("$completed/${tasks.size}", size = 11.sp, color = c.copy(alpha = 0.7f))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.copy(alpha = 0.25f)))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskRow(task = task, onToggle = onToggle, onRemove = onRemove)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = StandardType.mono(12.sp).copy(color = c),
                        cursorBrush = SolidColor(theme.accent),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() })
                    )
                    if (input.isEmpty()) {
                        MonoLabel("ADD TASK...", size = 11.sp, color = c.copy(alpha = 0.4f))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(2.dp, c)
                        .tilePress(onTap = submit, tilt = false),
                    contentAlignment = Alignment.Center
                ) {
                    MonoLabel("＋", size = 13.sp, color = c, weight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskItem,
    onToggle: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    val fill = LocalTileColors.current.fill
    val c = LocalTileColors.current.content
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(2.dp, c)
                .then(if (task.completed) Modifier.background(c) else Modifier)
                .tilePress(onTap = { onToggle(task.id) }, tilt = false),
            contentAlignment = Alignment.Center
        ) {
            if (task.completed) {
                MonoLabel("✓", size = 10.sp, color = fill, weight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = task.text,
            color = c.copy(alpha = if (task.completed) 0.5f else 1f),
            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 13.sp),
            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .tilePress(onTap = { onRemove(task.id) }, tilt = false),
            contentAlignment = Alignment.Center
        ) {
            MonoLabel("✕", size = 10.sp, color = c.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun NotesWidget(
    notes: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val keyboard = LocalSoftwareKeyboardController.current
    var inputting by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    val submit: () -> Unit = {
        if (input.isNotBlank()) {
            onAdd(input.trim())
            input = ""
        }
        inputting = false
        keyboard?.hide()
    }

    val shown = notes.takeLast(2)
    val startIndex = (notes.size - 2).coerceAtLeast(0)

    Tile(modifier = modifier.fillMaxWidth().height(120.dp), style = TileStyle.Outline, contentPadding = 0.dp) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonoLabel("NOTES", size = 11.sp, color = c)
                MonoLabel("${notes.size}", size = 11.sp, color = c.copy(alpha = 0.7f))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.copy(alpha = 0.25f)))

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                shown.forEachIndexed { i, note ->
                    val realIndex = startIndex + i
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = note,
                            color = c.copy(alpha = 0.7f),
                            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 12.sp, fontStyle = FontStyle.Italic),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .tilePress(onTap = { onRemove(realIndex) }, tilt = false),
                            contentAlignment = Alignment.Center
                        ) {
                            MonoLabel("✕", size = 9.sp, color = c.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            if (inputting) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 13.sp, fontStyle = FontStyle.Italic).copy(color = c),
                        cursorBrush = SolidColor(theme.accent),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() })
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(2.dp, c)
                            .tilePress(onTap = submit, tilt = false),
                        contentAlignment = Alignment.Center
                    ) {
                        MonoLabel("✕", size = 11.sp, color = c)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .tilePress(onTap = { inputting = true }, tilt = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoLabel("＋", size = 12.sp, color = c, weight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    MonoLabel("NOTE", size = 11.sp, color = c.copy(alpha = 0.8f))
                }
            }
        }
    }
}
