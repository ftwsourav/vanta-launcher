package app.vanta.launcher.ui.components

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A hosted Android widget placed on Home. [span] is grid columns (1..4), [heightDp] the tile height. */
data class WidgetEntry(val id: Int, val span: Int = 4, val heightDp: Int = 120, val label: String = "")

/** Persists the widgets on Home in prefs "widget_prefs" -> "widgets" = "id:span:heightDp:label,..." */
class WidgetStore(context: Context) {
    private val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
    private val _widgets = MutableStateFlow(load())
    val widgets: StateFlow<List<WidgetEntry>> = _widgets

    private fun load(): List<WidgetEntry> =
        prefs.getString("widgets", "").orEmpty().split(",").filter { it.isNotBlank() }.mapNotNull { raw ->
            val p = raw.split(":")
            val id = p.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            WidgetEntry(
                id = id,
                span = p.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 4) ?: 4,
                heightDp = p.getOrNull(2)?.toIntOrNull()?.coerceIn(56, 480) ?: 120,
                label = p.getOrNull(3).orEmpty()
            )
        }

    private fun save(list: List<WidgetEntry>) {
        prefs.edit().putString("widgets", list.joinToString(",") { "${it.id}:${it.span}:${it.heightDp}:${it.label.replace(',', ' ').replace(':', ' ')}" }).apply()
        _widgets.value = list
    }

    fun add(entry: WidgetEntry) = save(_widgets.value.filter { it.id != entry.id } + entry)
    fun remove(id: Int) = save(_widgets.value.filter { it.id != id })
    fun update(entry: WidgetEntry) = save(_widgets.value.map { if (it.id == entry.id) entry else it })
    fun move(id: Int, delta: Int) {
        val list = _widgets.value.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = (i + delta).coerceIn(0, list.lastIndex)
        if (i < 0 || i == j) return
        list.add(j, list.removeAt(i))
        save(list)
    }
}
