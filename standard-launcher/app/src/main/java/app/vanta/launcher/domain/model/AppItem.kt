package app.vanta.launcher.domain.model

data class AppItem(
    val packageName: String,
    val label: String,
    val pinned: Boolean = false,
    val pinnedOrder: Int = 0,
    val isAccent: Boolean = false,
    val tileSize: TileSize = TileSize.MEDIUM,
    /** User caption under the tile; null means "use the default for this package". */
    val caption: String? = null,
    /** Per-tile look override: "outline", "ink" or "accent"; null means the default. */
    val tileStyle: String? = null
)
