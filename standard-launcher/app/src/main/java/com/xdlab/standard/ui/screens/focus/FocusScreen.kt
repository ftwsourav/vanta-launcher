package com.xdlab.standard.ui.screens.focus

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.SystemClock
import android.provider.AlarmClock
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.xdlab.standard.R
import com.xdlab.standard.domain.model.AppItem
import com.xdlab.standard.ui.components.AppIcon
import com.xdlab.standard.ui.components.Barcode
import com.xdlab.standard.ui.components.CalloutTile
import com.xdlab.standard.ui.components.HeadlineText
import com.xdlab.standard.ui.components.LocalTileColors
import com.xdlab.standard.ui.components.MonoLabel
import com.xdlab.standard.ui.components.PlusTile
import com.xdlab.standard.ui.components.Tile
import com.xdlab.standard.ui.components.TileCaptions
import com.xdlab.standard.ui.components.TileDefaults
import com.xdlab.standard.ui.components.TileEntrance
import com.xdlab.standard.ui.components.TileStyle
import com.xdlab.standard.ui.components.WeatherTile
import com.xdlab.standard.ui.components.liveTimeFormatted
import com.xdlab.standard.ui.nav.StandardAppViewModel
import com.xdlab.standard.ui.theme.AppColors
import com.xdlab.standard.ui.theme.LocalAppTheme
import com.xdlab.standard.ui.theme.StandardType
import java.time.LocalDate
import kotlinx.coroutines.delay

private const val KEY_IMAGE = "image_uri"
private const val KEY_NOTES = "focus_notes"
private const val KEY_SW_RUNNING = "stopwatch_running"
private const val KEY_SW_START = "stopwatch_start"
private const val KEY_SW_ACCUMULATED = "stopwatch_accumulated"

/** Mockup columns: the photo/callout side is a little narrower than the rows/grid side. */
private const val LEFT = 1f
private const val RIGHT = 1.15f

private val HeroHeight = 230.dp
private val RowHeight = 56.dp
private val RowsHeight = RowHeight * 6 + 8.dp * 5
private val GridHeight = 300.dp
private val FooterHeight = 128.dp

/** Grayscale plus a touch of contrast so any picked photo stays monochrome like the mockup. */
private val MonoPhoto: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply {
        setToSaturation(0f)
        timesAssign(
            ColorMatrix(
                floatArrayOf(
                    1.15f, 0f, 0f, 0f, -19f,
                    0f, 1.15f, 0f, 0f, -19f,
                    0f, 0f, 1.15f, 0f, -19f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
)

@Composable
fun FocusScreen(viewModel: StandardAppViewModel, onOpenSettings: () -> Unit = {}, onOpenDrawer: () -> Unit = {}) {
    val context = LocalContext.current
    val focusApps by viewModel.focusApps.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val weatherLoading by viewModel.weatherLoading.collectAsState()
    val weatherError by viewModel.weatherError.collectAsState()
    val forecast by viewModel.forecast.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val prefs = remember { context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE) }
    var imageUri by remember { mutableStateOf(prefs.getString(KEY_IMAGE, null)) }
    val clearImage = {
        imageUri = null
        prefs.edit().remove(KEY_IMAGE).apply()
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
                // Provider without persistable grants: the picked URI still works for this session.
            }
            imageUri = uri.toString()
            prefs.edit().putString(KEY_IMAGE, uri.toString()).apply()
        }
    }
    val launch: (AppItem) -> Unit = { viewModel.launchApp(context, it.packageName) }

    val quotes = settings.quotes
    val quote = quotes.takeIf { it.isNotEmpty() }?.let { it[LocalDate.now().dayOfYear % it.size] }

    var soundOn by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
    ) {
        // Quick note capture; notes persisted to focus_prefs.
        TileEntrance(0) {
            QuickNoteTile(context, Modifier.fillMaxWidth())
        }

        // SUN / 06 SEP + caption on the paper, weather tile beside it.
        TileEntrance(1) {
            Row(Modifier.fillMaxWidth().height(HeroHeight), horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)) {
                val ink = LocalAppTheme.current.ink
                Column(Modifier.weight(LEFT).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    HeadlineText(liveTimeFormatted("EEE") + "\n" + liveTimeFormatted("dd MMM"), 52.sp, maxLines = 2)
                    Column {
                        MonoLabel("DISCIPLINE\nCREATES\nFREEDOM.", size = 12.sp, color = ink)
                        MonoLabel("—", size = 12.sp, color = ink)
                    }
                }
                WeatherTile(
                    weather = weather,
                    isLoading = weatherLoading,
                    error = weatherError,
                    forecast = forecast,
                    modifier = Modifier.weight(RIGHT).fillMaxHeight(),
                    quote = quote,
                    onRefresh = viewModel::refreshWeather,
                    onSetLocation = onOpenSettings
                )
            }
        }

        // Stopwatch counts up; ring fills each minute, laps below.
        TileEntrance(2) {
            StopwatchTile(Modifier.fillMaxWidth(), prefs, soundOn) { soundOn = !soundOn }
        }

        TileEntrance(3) {
            QuickTimerTile(Modifier.fillMaxWidth(), soundOn)
        }

        TileEntrance(4) {
            OpenClockButton(Modifier.fillMaxWidth())
        }

        // Productivity suggestions: 4 apps in a 2x2 grid.
        TileEntrance(5) {
            ProductivitySuggestionsTile(context, viewModel, Modifier.fillMaxWidth())
        }

        // Photo tile beside numbered rows 01..06.
        TileEntrance(6) {
            Row(Modifier.fillMaxWidth().height(RowsHeight), horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)) {
                PhotoTile(
                    uri = imageUri,
                    modifier = Modifier.weight(LEFT).fillMaxHeight(),
                    onPick = { pickImage.launch(arrayOf("image/*")) },
                    onClear = clearImage
                )
                Column(Modifier.weight(RIGHT).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (focusApps.isEmpty()) {
                        Tile(Modifier.fillMaxSize().button("Open drawer", onOpenDrawer), onClick = onOpenDrawer) {
                            val c = LocalTileColors.current.content
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                HeadlineText("PIN APPS\nIN DRAWER", 28.sp, color = c)
                                MonoLabel("TAP TO OPEN →", size = 10.sp, color = c)
                            }
                        }
                    } else {
                        focusApps.take(6).forEachIndexed { i, app ->
                            FocusRow(i + 1, app, Modifier.height(RowHeight)) { launch(app) }
                        }
                        if (focusApps.size < 6) {
                            PlusTile("ADD", Modifier.fillMaxWidth().weight(1f).button("Add apps", onOpenSettings), onClick = onOpenSettings)
                        }
                    }
                }
            }
        }

        // LESS SCROLLING MORE DOING. beside the 2x2 grid 07..10.
        TileEntrance(7) {
            Row(Modifier.fillMaxWidth().height(GridHeight), horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)) {
                CalloutTile(
                    headline = stringResource(R.string.callout_less_scrolling),
                    modifier = Modifier.weight(LEFT).fillMaxHeight(),
                    bottomCaption = "APPS / TOOLS / IDEAS / YOU",
                    headlineSize = 26.sp
                )
                val grid = focusApps.drop(6).take(4)
                Column(Modifier.weight(RIGHT).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)) {
                    for (r in 0 until 2) {
                        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)) {
                            for (col in 0 until 2) {
                                val i = r * 2 + col
                                val app = grid.getOrNull(i)
                                if (app != null) {
                                    GridCell(7 + i, app, Modifier.weight(1f).fillMaxHeight()) { launch(app) }
                                } else {
                                    PlusTile("ADD", Modifier.weight(1f).fillMaxHeight().button("Add apps", onOpenSettings), onClick = onOpenSettings)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Crosshair quote tile (opens Settings) and the barcode mantra.
        TileEntrance(8) {
            Row(Modifier.fillMaxWidth().height(FooterHeight), horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)) {
                Tile(
                    modifier = Modifier.weight(RIGHT).fillMaxHeight().button("Open settings", onOpenSettings),
                    style = TileStyle.Ink,
                    onClick = onOpenSettings
                ) {
                    val c = LocalTileColors.current.content
                    Box(Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Canvas(Modifier.size(40.dp)) {
                                val stroke = 1.dp.toPx()
                                drawLine(c, Offset(0f, center.y), Offset(size.width, center.y), stroke)
                                drawLine(c, Offset(center.x, 0f), Offset(center.x, size.height), stroke)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                MonoLabel("A BETTER\nYOU IS\nA BRIGHTER\nTOMORROW.", size = 10.sp, color = c)
                                MonoLabel("—", size = 10.sp, color = c)
                            }
                        }
                        MonoLabel("SETTINGS →", size = 9.sp, color = c.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.BottomEnd))
                    }
                }
                BarcodeFooter(Modifier.weight(LEFT).fillMaxHeight())
            }
        }
    }
}

/** The user's photo, forced monochrome, with the mockup's caption block and version stamp. */
@Composable
private fun PhotoTile(uri: String?, modifier: Modifier, onPick: () -> Unit, onClear: () -> Unit) {
    val colors = LocalAppTheme.current
    Tile(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.Button
            contentDescription = "Focus image"
            onClick("Choose image") { onPick(); true }
            if (uri != null) onLongClick("Remove image") { onClear(); true }
        },
        contentPadding = 0.dp,
        onClick = onPick,
        onLongClick = if (uri != null) onClear else null
    ) {
        if (uri == null) {
            val c = LocalTileColors.current.content
            Column(Modifier.fillMaxSize().padding(TileDefaults.Padding), verticalArrangement = Arrangement.SpaceBetween) {
                HeadlineText("ADD\nIMAGE", 32.sp, color = c)
                MonoLabel("TAP TO CHOOSE", size = 10.sp, color = c)
            }
        } else {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = MonoPhoto,
                onError = { onClear() }
            )
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.TopEnd).background(colors.ink).padding(10.dp)) {
                    MonoLabel("SAME IDEAS\nDIFFERENT\nREALITY.", size = 10.sp, color = colors.onInk)
                }
                Box(Modifier.align(Alignment.BottomStart).background(colors.ink).padding(horizontal = 8.dp, vertical = 6.dp)) {
                    MonoLabel("V 1.0\n2026", size = 10.sp, color = colors.onInk)
                }
                Box(Modifier.align(Alignment.BottomEnd).background(colors.ink).padding(horizontal = 8.dp, vertical = 6.dp)) {
                    MonoLabel("HOLD TO REMOVE", size = 10.sp, color = colors.onInk.copy(alpha = 0.8f))
                }
            }
        }
    }
}

/** "01 │ WHATSAPP │ CHAT / CALL" sized for the half-width Focus column. */
@Composable
private fun FocusRow(index: Int, app: AppItem, modifier: Modifier, onLaunch: () -> Unit) {
    Tile(modifier = modifier.fillMaxWidth().button("Open", onLaunch), contentPadding = 0.dp, onClick = onLaunch) {
        val c = LocalTileColors.current.content
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            MonoLabel("%02d".format(index), size = 9.sp, color = c, modifier = Modifier.padding(horizontal = 8.dp))
            Box(Modifier.fillMaxHeight().width(2.dp).background(c))
            HeadlineText(
                app.label.uppercase(),
                labelSize(app.label, 16.sp, 16.sp, 13.sp),
                color = c,
                maxLines = 1,
                modifier = Modifier.weight(1.25f).padding(horizontal = 8.dp)
            )
            Box(Modifier.fillMaxHeight().width(2.dp).background(c))
            MonoLabel(
                stackCaption(captionOf(app), 2),
                size = 8.sp,
                color = c,
                maxLines = 2,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
            )
        }
    }
}

/** "@ 07 / CAMERA / CAPTURE CREATE PRESERVE" grid cell. */
@Composable
private fun GridCell(index: Int, app: AppItem, modifier: Modifier, onLaunch: () -> Unit) {
    Tile(modifier = modifier.button("Open", onLaunch), contentPadding = 10.dp, onClick = onLaunch) {
        val c = LocalTileColors.current.content
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                MonoLabel("@ %02d".format(index), size = 9.sp, color = c.copy(alpha = 0.85f))
                Spacer(Modifier.height(4.dp))
                HeadlineText(app.label.uppercase(), labelSize(app.label, 19.sp, 14.sp, 11.sp), color = c, maxLines = 2)
            }
            Column {
                MonoLabel(stackCaption(captionOf(app), 3), size = 9.sp, color = c, maxLines = 3)
                MonoLabel("—", size = 9.sp, color = c)
            }
        }
    }
}

/** Barcode on the left, BUILD / LEARN / IMPROVE / REPEAT on the right, as in the Focus mockup. */
@Composable
private fun BarcodeFooter(modifier: Modifier) {
    Tile(modifier = modifier) {
        val c = LocalTileColors.current.content
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(60.dp).fillMaxHeight()) {
                Barcode(Modifier.fillMaxWidth().weight(1f), color = c)
                Spacer(Modifier.height(4.dp))
                MonoLabel("STANDARD.\n//2026", size = 8.sp, color = c.copy(alpha = 0.8f), maxLines = 2)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                listOf("BUILD", "LEARN", "IMPROVE", "REPEAT").forEach { MonoLabel(it, size = 10.sp, color = c) }
                Spacer(Modifier.height(4.dp))
                MonoLabel("—", size = 10.sp, color = c)
            }
        }
    }
}

private fun captionOf(app: AppItem): String = app.caption ?: TileCaptions.defaultFor(app.packageName, app.label)

/** "MESSAGES / CALLS / COMMUNITY" -> up to [lines] words stacked one per line. */
private fun stackCaption(caption: String, lines: Int): String =
    caption.split('/').map { it.trim() }.filter { it.isNotEmpty() }.take(lines).joinToString("\n")

/** Step the headline down for long names so they never break mid-word in a narrow cell. */
private fun labelSize(label: String, upToSix: TextUnit, upToEight: TextUnit, longer: TextUnit): TextUnit =
    when {
        label.length <= 6 -> upToSix
        label.length <= 8 -> upToEight
        else -> longer
    }

/** Tiles press through pointerInput only; this gives TalkBack a button it can activate. */
private fun Modifier.button(label: String, action: () -> Unit): Modifier =
    semantics(mergeDescendants = true) {
        role = Role.Button
        onClick(label) { action(); true }
    }

private data class ProductivityApp(val packageName: String, val name: String, val tag: String)

@Composable
private fun StopwatchTile(
    modifier: Modifier = Modifier,
    prefs: SharedPreferences,
    soundOn: Boolean,
    onToggleSound: () -> Unit
) {
    val colors = LocalAppTheme.current
    var isRunning by remember { mutableStateOf(prefs.getBoolean(KEY_SW_RUNNING, false)) }
    var startTime by remember { mutableLongStateOf(prefs.getLong(KEY_SW_START, 0L)) }
    var accumulatedMs by remember { mutableLongStateOf(prefs.getLong(KEY_SW_ACCUMULATED, 0L)) }
    var elapsedMs by remember {
        mutableLongStateOf(
            if (isRunning) (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(0L) else accumulatedMs
        )
    }
    var laps by remember { mutableStateOf<List<Long>>(emptyList()) }

    fun persistStopwatch() {
        prefs.edit()
            .putBoolean(KEY_SW_RUNNING, isRunning)
            .putLong(KEY_SW_START, startTime)
            .putLong(KEY_SW_ACCUMULATED, accumulatedMs)
            .apply()
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (true) {
                val computed = SystemClock.elapsedRealtime() - startTime
                if (computed < 0L) {
                    isRunning = false
                    accumulatedMs = 0L
                    startTime = 0L
                    elapsedMs = 0L
                    persistStopwatch()
                    break
                }
                elapsedMs = computed
                delay(50L)
            }
        }
    }

    fun toggleRunning() {
        if (isRunning) {
            accumulatedMs = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(0L)
            isRunning = false
        } else {
            startTime = SystemClock.elapsedRealtime() - accumulatedMs
            isRunning = true
        }
        persistStopwatch()
    }

    fun resetStopwatch() {
        isRunning = false
        accumulatedMs = 0L
        startTime = 0L
        elapsedMs = 0L
        laps = emptyList()
        persistStopwatch()
    }

    val totalSeconds = elapsedMs / 1000L
    val display = if (totalSeconds >= 3600L) {
        "%02d:%02d:%02d".format(totalSeconds / 3600L, (totalSeconds % 3600L) / 60L, totalSeconds % 60L)
    } else {
        "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
    }
    val ringProgress = (elapsedMs % 60000L).toFloat() / 60000f
    val bandEven = (elapsedMs / 60000L) % 2L == 0L
    val ringColor = if (bandEven) colors.accent else colors.ink
    val captionLabel = if (totalSeconds >= 3600L) "HH:MM:SS" else "MM:SS"

    Tile(modifier = modifier) {
        val c = LocalTileColors.current.content
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonoLabel("STOPWATCH // COUNTS UP", size = 10.sp, color = c.copy(alpha = 0.85f))
                BrutalButton(if (soundOn) "SOUND ON" else "SOUND OFF", Modifier, fillWidth = false) { onToggleSound() }
            }
            Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 8.dp.toPx()
                    val d = sw / 2f
                    val arcSize = Size(size.width - 2f * d, size.height - 2f * d)
                    drawArc(
                        color = c.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(d, d),
                        size = arcSize,
                        style = Stroke(width = sw)
                    )
                    if (ringProgress > 0f) {
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = 360f * ringProgress,
                            useCenter = false,
                            topLeft = Offset(d, d),
                            size = arcSize,
                            style = Stroke(width = sw, cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HeadlineText(display, 44.sp, color = c, maxLines = 1)
                    MonoLabel(captionLabel, size = 9.sp, color = c.copy(alpha = 0.7f))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrutalButton(if (isRunning) "PAUSE" else "START", Modifier.weight(1f)) { toggleRunning() }
                BrutalButton("LAP", Modifier.weight(1f)) {
                    if (elapsedMs > 0L) laps = laps + elapsedMs
                }
                BrutalButton("RESET", Modifier.weight(1f)) { resetStopwatch() }
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MonoLabel("LAPS", size = 9.sp, color = c.copy(alpha = 0.85f))
                if (laps.isEmpty()) {
                    MonoLabel("—", size = 10.sp, color = c.copy(alpha = 0.7f))
                } else {
                    laps.takeLast(3).reversed().forEachIndexed { i, lap ->
                        val ls = lap / 1000L
                        val lapDisplay = if (ls >= 3600L) {
                            "%02d:%02d:%02d".format(ls / 3600L, (ls % 3600L) / 60L, ls % 60L)
                        } else {
                            "%02d:%02d".format(ls / 60L, ls % 60L)
                        }
                        MonoLabel(
                            "LAP %02d  %s".format(laps.size - i, lapDisplay),
                            size = 10.sp,
                            color = c
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTimerTile(modifier: Modifier = Modifier, soundOn: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    var endTime by remember { mutableLongStateOf(0L) }
    var remainingMs by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    var timerLabel by remember { mutableStateOf("") }

    fun startTimer(label: String, durationMs: Long) {
        timerLabel = label
        endTime = SystemClock.elapsedRealtime() + durationMs
        remainingMs = durationMs
        isComplete = false
        isTimerRunning = true
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (true) {
                remainingMs = (endTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                if (remainingMs <= 0L) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    if (soundOn) {
                        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        RingtoneManager.getRingtone(context, uri)?.play()
                    }
                    Toast.makeText(context, "TIMER COMPLETE", Toast.LENGTH_SHORT).show()
                    isComplete = true
                    isTimerRunning = false
                    break
                }
                delay(100L)
            }
        }
    }

    val totalSeconds = remainingMs / 1000L
    val display = if (totalSeconds >= 3600L) {
        "%02d:%02d:%02d".format(totalSeconds / 3600L, (totalSeconds % 3600L) / 60L, totalSeconds % 60L)
    } else {
        "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
    }

    Tile(modifier = modifier) {
        val c = LocalTileColors.current.content
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MonoLabel("QUICK TIMERS // COUNTDOWN", size = 10.sp, color = c.copy(alpha = 0.85f))
            if (isTimerRunning || isComplete) {
                HeadlineText(
                    if (isComplete) "COMPLETE" else display,
                    44.sp,
                    color = c,
                    maxLines = 1
                )
                MonoLabel(if (isComplete) "DONE" else timerLabel, size = 10.sp, color = c.copy(alpha = 0.85f))
                if (isTimerRunning) {
                    BrutalButton("CANCEL", Modifier.fillMaxWidth()) {
                        isTimerRunning = false
                        isComplete = false
                        remainingMs = 0L
                    }
                } else {
                    BrutalButton("DISMISS", Modifier.fillMaxWidth()) {
                        isComplete = false
                        remainingMs = 0L
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrutalButton("1 MIN", Modifier.weight(1f)) { startTimer("1 MIN", 60_000L) }
                    BrutalButton("5 MIN", Modifier.weight(1f)) { startTimer("5 MIN", 300_000L) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrutalButton("POMODORO 25", Modifier.weight(1f)) { startTimer("POMODORO 25", 1_500_000L) }
                    BrutalButton("BREAK 5", Modifier.weight(1f)) { startTimer("BREAK 5", 300_000L) }
                }
            }
        }
    }
}

@Composable
private fun OpenClockButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BrutalButton("OPEN CLOCK APP", modifier) {
        try {
            context.startActivity(Intent(AlarmClock.ACTION_SHOW_TIMERS))
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:com.android.deskclock")))
            } catch (e2: Exception) {
                Toast.makeText(context, "NO CLOCK APP", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun BrutalButton(
    label: String,
    modifier: Modifier,
    fillWidth: Boolean = true,
    onClick: () -> Unit
) {
    Tile(
        modifier = modifier.button(label, onClick),
        contentPadding = 0.dp,
        onClick = onClick
    ) {
        val c = LocalTileColors.current.content
        Box(
            modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .padding(vertical = 12.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            MonoLabel(
                label,
                modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
                size = 11.sp,
                color = c,
                weight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProductivitySuggestionsTile(
    context: Context,
    viewModel: StandardAppViewModel,
    modifier: Modifier = Modifier
) {
    val suggestions = remember {
        listOf(
            ProductivityApp("com.google.android.keep", "Google Keep", "NOTES"),
            ProductivityApp("com.todoist", "Todoist", "TASKS"),
            ProductivityApp("com.microsoft.todos", "Microsoft To Do", "TASKS"),
            ProductivityApp("com.evernote", "Evernote", "NOTES")
        )
    }
    Tile(modifier = modifier) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonoLabel("PRODUCTIVITY SUGGESTIONS", size = 11.sp, color = c, weight = FontWeight.Bold)
                MonoLabel("//04 APPS", size = 9.sp, color = c.copy(alpha = 0.7f))
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(c))
            for (r in 0 until 2) {
                Row(
                    Modifier.fillMaxWidth().height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(TileDefaults.Gutter)
                ) {
                    for (col in 0 until 2) {
                        val i = r * 2 + col
                        val app = suggestions[i]
                        ProductivityCell(app, Modifier.weight(1f).fillMaxHeight()) {
                            val launched = viewModel.launchApp(context, app.packageName)
                            if (!launched) Toast.makeText(context, "NOT INSTALLED", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductivityCell(app: ProductivityApp, modifier: Modifier, onLaunch: () -> Unit) {
    Tile(
        modifier = modifier.button("Open " + app.name, onLaunch),
        contentPadding = 10.dp,
        onClick = onLaunch
    ) {
        val c = LocalTileColors.current.content
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.packageName, 30.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    MonoLabel(app.tag, size = 9.sp, color = c.copy(alpha = 0.85f))
                    HeadlineText(
                        app.name.uppercase(),
                        labelSize(app.name, 16.sp, 13.sp, 11.sp),
                        color = c,
                        maxLines = 1
                    )
                }
            }
            MonoLabel("→", size = 14.sp, color = c, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickNoteTile(context: Context, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val prefs = remember { context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE) }
    var notes by remember {
        mutableStateOf(prefs.getString(KEY_NOTES, "")?.split("\n")?.filter { it.isNotEmpty() } ?: emptyList())
    }
    var showDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    Tile(modifier = modifier) {
        val c = LocalTileColors.current.content
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeadlineText("QUICK\nNOTE", 26.sp, color = c)
                BrutalButton("ADD NOTE", Modifier, fillWidth = false) { showDialog = true }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(c))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MonoLabel("LAST NOTES", size = 9.sp, color = c.copy(alpha = 0.85f))
                if (notes.isEmpty()) {
                    MonoLabel("NO NOTES YET — TAP ADD NOTE", size = 10.sp, color = c.copy(alpha = 0.7f))
                } else {
                    notes.takeLast(3).reversed().forEachIndexed { i, note ->
                        MonoLabel(
                            "%02d / %s".format(notes.size - i, note.take(60)),
                            size = 10.sp,
                            color = c,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        NoteDialog(
            theme = theme,
            text = noteText,
            onTextChange = { noteText = it },
            onDismiss = { noteText = ""; showDialog = false },
            onSave = {
                if (noteText.isNotBlank()) {
                    val updated = notes + noteText.trim()
                    prefs.edit().putString(KEY_NOTES, updated.joinToString("\n")).apply()
                    notes = updated
                }
                noteText = ""
                showDialog = false
            }
        )
    }
}

@Composable
private fun NoteDialog(
    theme: AppColors,
    text: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalAppTheme provides theme) {
            Tile(modifier = Modifier.fillMaxWidth()) {
                val c = LocalTileColors.current.content
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MonoLabel("NEW NOTE", size = 11.sp, color = c, weight = FontWeight.Bold)
                    Box(
                        modifier = Modifier.fillMaxWidth().border(2.dp, c).padding(10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            MonoLabel("TYPE A NOTE...", size = 11.sp, color = c.copy(alpha = 0.4f))
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = StandardType.mono(13.sp).copy(color = c),
                            cursorBrush = SolidColor(theme.accent),
                            singleLine = true
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrutalButton("CANCEL", Modifier.weight(1f), onClick = onDismiss)
                        BrutalButton("SAVE", Modifier.weight(1f), onClick = onSave)
                    }
                }
            }
        }
    }
}
