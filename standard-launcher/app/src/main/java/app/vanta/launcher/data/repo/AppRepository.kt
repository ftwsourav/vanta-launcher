package app.vanta.launcher.data.repo

import android.content.Context
import app.vanta.launcher.domain.model.AppItem
import app.vanta.launcher.domain.model.TileSize
import kotlinx.coroutines.flow.StateFlow

interface AppRepository {
    val pinnedApps: StateFlow<List<AppItem>>
    val allApps: StateFlow<List<AppItem>>
    /** The drawer's QUICK TOOLS 2x2 (max 4). */
    val quickTools: StateFlow<List<AppItem>>
    /** The curated Focus list (max 10). */
    val focusApps: StateFlow<List<AppItem>>
    fun refresh()
    suspend fun setPinned(packageNames: List<String>)
    suspend fun movePinned(packageName: String, delta: Int)
    suspend fun unpin(packageName: String)
    /** Null or blank clears the accent. */
    suspend fun setAccent(packageName: String?)
    suspend fun setTileSize(packageName: String, size: TileSize)
    /** Null or blank restores the default caption. */
    suspend fun setCaption(packageName: String, caption: String?)
    /** "outline" | "ink" | "accent"; null or blank restores the default look. */
    suspend fun setTileStyle(packageName: String, style: String?)
    suspend fun setQuickTools(packageNames: List<String>)
    suspend fun setFocusApps(packageNames: List<String>)
    /** Wipes pins, sizes, captions, accent, quick tools and focus list, then reseeds defaults. */
    suspend fun resetLayout()
    fun launch(context: Context, packageName: String): Boolean
}
