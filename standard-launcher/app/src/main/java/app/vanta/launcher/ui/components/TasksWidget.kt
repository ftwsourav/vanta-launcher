package app.vanta.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.SpaceGrotesk
import app.vanta.launcher.ui.theme.StandardType
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

enum class WidgetSize(val heightDp: Int, val maxItems: Int) {
    COMPACT(100, 3),
    MEDIUM(160, 5),
    EXPANDED(240, 8)
}

@Composable
fun TasksWidget(
    tasks: List<TaskItem>,
    onToggle: (Int) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
    size: WidgetSize = WidgetSize.MEDIUM,
    sizeCycle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val keyboard = LocalSoftwareKeyboardController.current
    var input by remember { mutableStateOf("") }
    val completed = tasks.count { it.completed }
    val hasIncomplete = tasks.any { !it.completed }
    val visibleTasks = tasks.take(size.maxItems)
    val pulseTransition = rememberInfiniteTransition(label = "tasksPulse")
    // Read only inside graphicsLayer so the pulse never recomposes the widget.
    val pulseAlpha = pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tasksPulseAlpha"
    )

    val submit: () -> Unit = {
        if (input.isNotBlank()) {
            onAdd(input.trim())
            input = ""
            keyboard?.hide()
        }
    }

    Tile(
        modifier = modifier.fillMaxWidth().height(size.heightDp.dp),
        style = TileStyle.Outline,
        contentPadding = 0.dp
    ) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tilePress(onTap = {}, onLongPress = { sizeCycle() }, tilt = false)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hasIncomplete) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .graphicsLayer { alpha = pulseAlpha.value }
                                .background(theme.accent)
                        )
                    }
                    MonoLabel("TASKS", size = 11.sp, color = c)
                }
                MonoLabel("$completed/${tasks.size}", size = 11.sp, color = c.copy(alpha = 0.7f))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.copy(alpha = 0.25f)))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(visibleTasks, key = { it.id }) { task ->
                    AnimatedRow(onGone = { onRemove(task.id) }) { dismiss ->
                        TaskRow(task = task, onToggle = onToggle, onRemove = dismiss)
                    }
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
                SquareButton(label = "Add task", onTap = submit) { PlusCross(color = c, size = 12.dp) }
            }
        }
    }
}

/**
 * List row motion: enters with a 4dp slide + fade; [content] receives `dismiss`, which shrinks the
 * row out (180ms) and only then fires [onGone] so the caller removes it from its list.
 */
@Composable
private fun AnimatedRow(onGone: () -> Unit, content: @Composable (dismiss: () -> Unit) -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    val slidePx = with(LocalDensity.current) { 4.dp.roundToPx() }
    LaunchedEffect(visible.isIdle, visible.targetState) {
        if (visible.isIdle && !visible.targetState) onGone()
    }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(220, easing = LumiaEasing)) + slideInVertically(tween(220, easing = LumiaEasing)) { slidePx },
        exit = shrinkVertically(tween(180, easing = LumiaEasing)) + fadeOut(tween(150, easing = LumiaEasing))
    ) {
        content { visible.targetState = false }
    }
}

/** Square outline button with the Metro press; [side] is the hit box. */
@Composable
private fun SquareButton(label: String, onTap: () -> Unit, side: Dp = 28.dp, content: @Composable () -> Unit) {
    val c = LocalTileColors.current.content
    Box(
        modifier = Modifier
            .size(side)
            .border(TileDefaults.Border, c)
            .tilePress(onTap = onTap, tilt = false)
            .button(label, onTap),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun TaskRow(
    task: TaskItem,
    onToggle: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val fill = LocalTileColors.current.fill
    val c = LocalTileColors.current.content
    // 0 = open square, 1 = ink-filled; read only in draw/graphicsLayer lambdas.
    val done by animateFloatAsState(
        targetValue = if (task.completed) 1f else 0f,
        animationSpec = tween(120, easing = LumiaEasing),
        label = "taskDone"
    )
    val toggle = { onToggle(task.id) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .drawBehind {
                    val w = TileDefaults.Border.toPx()
                    drawRect(c, topLeft = Offset(w / 2f, w / 2f), size = Size(size.width - w, size.height - w), style = Stroke(w))
                    val s = size.width * done
                    if (s > 0f) {
                        drawRect(c, topLeft = Offset((size.width - s) / 2f, (size.height - s) / 2f), size = Size(s, s))
                    }
                }
                .tilePress(onTap = toggle, tilt = false)
                .button(if (task.completed) "Mark not done" else "Mark done", toggle),
            contentAlignment = Alignment.Center
        ) {
            if (task.completed) {
                MonoLabel("✓", size = 10.sp, color = fill, weight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = task.text,
            color = c,
            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 13.sp),
            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).graphicsLayer { alpha = 1f - 0.5f * done }
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .tilePress(onTap = onRemove, tilt = false)
                .button("Remove task", onRemove),
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
                    key(realIndex, note) {
                        AnimatedRow(onGone = { onRemove(realIndex) }) { dismiss ->
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
                                        .tilePress(onTap = dismiss, tilt = false)
                                        .button("Remove note", dismiss),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MonoLabel("✕", size = 9.sp, color = c.copy(alpha = 0.5f))
                                }
                            }
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
                    SquareButton(label = "Add note", onTap = submit, side = 24.dp) { PlusCross(color = c, size = 10.dp) }
                }
            } else {
                // The whole row is the button; the square is its glyph.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .tilePress(onTap = { inputting = true }, tilt = false)
                        .button("New note") { inputting = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(24.dp).border(TileDefaults.Border, c), contentAlignment = Alignment.Center) {
                        PlusCross(color = c, size = 10.dp)
                    }
                    Spacer(Modifier.width(8.dp))
                    MonoLabel("NOTE", size = 11.sp, color = c.copy(alpha = 0.8f))
                }
            }
        }
    }
}
