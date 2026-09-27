package app.vanta.launcher.ui.components

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import app.vanta.launcher.StandardApplication
import app.vanta.launcher.domain.model.DarkMode
import app.vanta.launcher.ui.theme.AppColors
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.LocalSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.ceil

private const val REQ_CONFIGURE = 0x5EE0

/** minWidth ≤ 90dp → 1 column, ≤ 180dp → 2, else the full 4. */
internal fun spanFor(minWidthDp: Float): Int = when {
    minWidthDp <= 90f -> 1
    minWidthDp <= 180f -> 2
    else -> 4
}

/** Height rounded up to a 56dp step, never under 112dp. */
internal fun heightFor(minHeightDp: Float): Int = maxOf(112, ceil(minHeightDp / 56f).toInt() * 56)

/**
 * ADD WIDGET // — installed providers grouped by app. Tap: allocate an id, bind (asking the
 * system when not allowed), run the provider's configure activity if it has one, then persist
 * the entry in [WidgetStore] and finish. Any cancel frees the id.
 */
class WidgetPickerActivity : ComponentActivity() {

    private var pending: WidgetEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as StandardApplication).container
        setContent {
            val settings by container.settingsRepository.settings.collectAsState()
            val dark = when (settings.darkMode) {
                DarkMode.AUTO_SYSTEM ->
                    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                DarkMode.AUTO_TIME -> Calendar.getInstance().get(Calendar.HOUR_OF_DAY).let { it < 7 || it >= 19 }
                DarkMode.LIGHT -> false
                DarkMode.DARK -> true
            }
            val theme = container.themeRepository.themeById(settings.themeId)
            val colors = AppColors(
                surface = theme.surface(dark),
                dark = dark,
                useTexture = settings.useTexture,
                textureStrength = settings.textureStrength,
                noiseDrift = settings.noiseDrift
            )
            val lightBars = colors.background.luminance() > 0.5f
            LaunchedEffect(lightBars) {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
            CompositionLocalProvider(
                LocalAppTheme provides colors,
                LocalSettings provides settings,
                LocalTileColors provides tileColors(TileStyle.Outline, colors),
                LocalHapticsEnabled provides settings.hapticsEnabled
            ) {
                val bind = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
                    if (r.resultCode == RESULT_OK) configureOrCommit() else cancel()
                }
                WidgetPickerScreen(onPick = { provider -> pick(provider) { bind.launch(it) } })
            }
        }
    }

    private fun pick(provider: AppWidgetProviderInfo, requestBind: (Intent) -> Unit) {
        if (pending != null) return
        val id = VantaWidgetHost.allocateId(this)
        val d = resources.displayMetrics.density
        pending = WidgetEntry(
            id = id,
            span = spanFor(provider.minWidth / d),
            heightDp = heightFor(provider.minHeight / d),
            label = provider.loadLabel(packageManager).orEmpty()
        )
        val bound = try {
            AppWidgetManager.getInstance(this).bindAppWidgetIdIfAllowed(id, provider.profile, provider.provider, null)
        } catch (e: Exception) { false }
        if (bound) {
            configureOrCommit()
        } else {
            requestBind(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)
            )
        }
    }

    private fun configureOrCommit() {
        val entry = pending ?: return
        if (VantaWidgetHost.info(this, entry.id)?.configure != null) {
            try {
                VantaWidgetHost.host(this).startAppWidgetConfigureActivityForResult(this, entry.id, 0, REQ_CONFIGURE, null)
                return
            } catch (e: Exception) { /* configure screen unavailable: place it unconfigured */ }
        }
        commit()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CONFIGURE) return
        if (resultCode == RESULT_OK) commit() else cancel()
    }

    private fun commit() {
        pending?.let { WidgetStore(this).add(it) }
        pending = null
        setResult(RESULT_OK)
        finish()
    }

    private fun cancel() {
        pending?.let { VantaWidgetHost.deleteId(this, it.id) }
        pending = null
    }
}

private class PickRow(val info: AppWidgetProviderInfo, val label: String, val cells: String)
private class PickGroup(val app: String, val rows: List<PickRow>)

/** Cells per the provider contract: size = 70dp * n - 30dp. */
private fun cells(px: Int, density: Float): Int = ceil((px / density + 30f) / 70f).toInt().coerceAtLeast(1)

@Composable
private fun WidgetPickerScreen(onPick: (AppWidgetProviderInfo) -> Unit) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    var groups by remember { mutableStateOf<List<PickGroup>?>(null) }
    LaunchedEffect(Unit) {
        groups = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val d = context.resources.displayMetrics.density
            AppWidgetManager.getInstance(context).installedProviders
                .groupBy { it.provider.packageName }
                .map { (pkg, list) ->
                    val app = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } catch (e: Exception) { pkg }
                    PickGroup(
                        app = app.uppercase(),
                        rows = list.map { PickRow(it, it.loadLabel(pm).orEmpty().uppercase(), "${cells(it.minWidth, d)}×${cells(it.minHeight, d)} CELLS") }
                            .sortedBy { it.label }
                    )
                }
                .sortedBy { it.app }
        }
    }
    val epoch = remember(groups) { SystemClock.uptimeMillis() }
    val list = groups
    val count = list?.sumOf { it.rows.size } ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .background(theme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp)
    ) {
        HeadlineText("ADD WIDGET //", 34.sp, modifier = Modifier.padding(top = 16.dp), maxLines = 1)
        MonoLabel(
            text = when {
                list == null -> "SCANNING"
                list.isEmpty() -> "NO WIDGETS INSTALLED"
                else -> "$count WIDGETS · ${list.size} APPS"
            },
            size = 10.sp,
            color = theme.muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        if (list == null) return@Column
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            var index = 0
            list.forEach { group ->
                val hi = index++
                item(key = "h:${group.app}") {
                    SectionLabel(group.app, Modifier.turnstile(hi, epoch).padding(top = 6.dp))
                }
                group.rows.forEach { row ->
                    val ri = index++
                    item(key = row.info.provider.flattenToString()) {
                        WidgetRow(row, Modifier.turnstile(ri, epoch)) { onPick(row.info) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetRow(row: PickRow, modifier: Modifier, onTap: () -> Unit) {
    val tc = LocalTileColors.current
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .tilePress(onTap = onTap, tilt = false)
            .background(tc.fill)
            .border(TileDefaults.Border, tc.outline)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeadlineText(row.label, 16.sp, modifier = Modifier.weight(1f), color = tc.content, maxLines = 1)
        Spacer(Modifier.width(12.dp))
        MonoLabel(row.cells, size = 10.sp, color = tc.content.copy(alpha = 0.6f), maxLines = 1)
    }
}

/** Turnstile entrance, 30ms per row capped at 300ms; rows scrolled in later just appear. */
@Composable
private fun Modifier.turnstile(index: Int, epoch: Long): Modifier {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (SystemClock.uptimeMillis() - epoch > 900L) {
            enter.snapTo(1f)
            return@LaunchedEffect
        }
        delay((index * 30L).coerceAtMost(300L))
        enter.animateTo(1f, tween(300, easing = LumiaEasing))
    }
    return graphicsLayer {
        val p = enter.value
        if (p < 1f) {
            transformOrigin = TransformOrigin(0f, 0.5f)
            rotationY = 60f * (1f - p)
            alpha = p
            cameraDistance = 14f * density
        }
    }
}
