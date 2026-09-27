package app.vanta.launcher.ui.nav

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.vanta.launcher.ui.components.LocalEntranceTick
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.domain.model.AnimationStyle
import app.vanta.launcher.domain.model.SettingsState
import app.vanta.launcher.ui.components.CinematicSplash
import app.vanta.launcher.ui.components.DriftingNoiseOverlay
import app.vanta.launcher.ui.components.LocalHapticsEnabled
import app.vanta.launcher.ui.components.LumiaEasing
import app.vanta.launcher.ui.components.MonoLabel
import app.vanta.launcher.ui.components.SwipeDownSearch
import app.vanta.launcher.ui.components.edgeSwipeHandler
import app.vanta.launcher.ui.components.backSwipeHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import app.vanta.launcher.ui.screens.drawer.DrawerScreen
import app.vanta.launcher.ui.screens.focus.FocusScreen
import app.vanta.launcher.ui.screens.glance.GlanceOverlay
import app.vanta.launcher.ui.screens.home.HomeScreen
import app.vanta.launcher.ui.screens.settings.SettingsScreen
import app.vanta.launcher.ui.theme.JetBrainsMono
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.LocalSettings
import app.vanta.launcher.ui.theme.SpaceGrotesk
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private var splashShownThisProcess = false

/** Page-to-page spring: quick settle, no bounce (Metro). */
private val PageSpring = spring<Float>(dampingRatio = 0.88f, stiffness = 420f)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StandardApp(viewModel: StandardAppViewModel) {
    val settings by viewModel.settings.collectAsState()
    val colors = LocalAppTheme.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val prefs = remember { context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE) }
    var showSplash by remember { mutableStateOf(settings.cinematicIntro && !splashShownThisProcess) }
    LaunchedEffect(showSplash) {
        if (showSplash) splashShownThisProcess = true
    }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showGlance by remember { mutableStateOf(false) }
    // Nightstand: Glance shows itself while charging at night once the screen has been idle 30 s.
    var glanceNightstand by remember { mutableStateOf(false) }
    var lastTouchMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val battery by viewModel.battery.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(prefs.getBoolean("first_run", true)) }
    var showRecents by remember { mutableStateOf(false) }
    var showQuickSettings by remember { mutableStateOf(false) }
    val allApps by viewModel.allApps.collectAsState()

    LaunchedEffect(settings.nightstand, battery.isCharging) {
        if (!settings.nightstand || !battery.isCharging) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(5_000)
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val night = hour >= 21 || hour < 6
            val idle = System.currentTimeMillis() - lastTouchMillis > 30_000
            if (night && idle && !showGlance && !showSettings && !showSearch && !showRecents && !showQuickSettings && !showOnboarding) {
                glanceNightstand = true
                showGlance = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (showSplash) {
            kotlinx.coroutines.delay(1000)
            showSplash = false
        }
    }

    LaunchedEffect(settings.weatherLocation.lat, settings.weatherLocation.lon, settings.weatherLocation.name) {
        viewModel.refreshWeather()
    }

    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val currentPageState = rememberSaveable { mutableStateOf(0) }
    var currentPage by currentPageState
    val gesturePrefs = remember { context.getSharedPreferences("standard_gestures", Context.MODE_PRIVATE) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { currentPage = it }
    }
    // A light tick when a page settles, never mid-drag.
    val hapticsOn = LocalHapticsEnabled.current
    LaunchedEffect(pagerState, hapticsOn) {
        snapshotFlow { pagerState.settledPage }
            .drop(1)
            .collect { if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    val goTo: (Int) -> Unit = remember(pagerState) {
        { page -> scope.launch { pagerState.animateScrollToPage(page, animationSpec = PageSpring) } }
    }
    val openDrawer: () -> Unit = remember(goTo) { { goTo(1) } }
    val openFocus: () -> Unit = remember(goTo) { { goTo(2) } }
    val editMode by viewModel.editMode.collectAsState()

    // Tiles turnstile in again every time the launcher comes back to the foreground (WP behaviour).
    var entranceTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var firstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResume) firstResume = false else entranceTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // HOME key while already showing: close every overlay and snap back to the first page.
    val homeTick by viewModel.homeTick.collectAsState()
    LaunchedEffect(homeTick) {
        if (homeTick > 0) {
            showSettings = false
            showGlance = false
            showSearch = false
            showRecents = false
            showQuickSettings = false
            pagerState.animateScrollToPage(0, animationSpec = PageSpring)
        }
    }

    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showGlance) { showGlance = false }
    BackHandler(enabled = showSearch) { showSearch = false }
    BackHandler(enabled = showOnboarding) {
        prefs.edit().putBoolean("first_run", false).apply()
        showOnboarding = false
    }
    BackHandler(enabled = editMode) { viewModel.setEditMode(false) }

    val pageNames = listOf("home", "apps", "focus", "live")
    val activePage = pagerState.currentPage
    val overlayOpen = showSettings || showGlance || showSearch || showOnboarding || showRecents || showQuickSettings
    BackHandler(enabled = !overlayOpen && !editMode && activePage != 0) { goTo(0) }
    val topZonePx = with(LocalDensity.current) { 96.dp.toPx() }

    var homeVisible by remember { mutableStateOf(true) }
    LaunchedEffect(activePage) {
        homeVisible = activePage == 0
    }
    val headerOffset by animateFloatAsState(
        targetValue = if (homeVisible) 0f else -20f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
        label = "headerOffset"
    )

    val edgeSwipeModifier = Modifier.edgeSwipeHandler(
        enabled = true,
        onSwipeOpen = {
            val action = gesturePrefs.getString("swipe_left", "NEXT PAGE") ?: "NEXT PAGE"
            dispatchGestureAction(action, context, scope, pagerState) { showSearch = true }
        }
    )

    val backSwipeModifier = Modifier.backSwipeHandler(
        enabled = true,
        onSwipeBack = {
            val action = gesturePrefs.getString("swipe_right", "PREV PAGE") ?: "PREV PAGE"
            dispatchGestureAction(action, context, scope, pagerState) { showSearch = true }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(Unit) {
                // Idle clock for nightstand mode: any touch, in the Initial pass, without consuming.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    lastTouchMillis = System.currentTimeMillis()
                }
            }
            .then(edgeSwipeModifier)
            .then(backSwipeModifier)
            .pointerInput(activePage, showSettings, showSearch, showRecents, showQuickSettings, showOnboarding) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, amount ->
                        if (activePage == 0 && !showSettings && !showSearch && !showRecents && !showQuickSettings && !showOnboarding &&
                            change.position.y < topZonePx && amount > 12f) {
                            showQuickSettings = true
                        }
                    }
                )
            }
    ) {
        app.vanta.launcher.ui.components.PanoramaBackground(
            uri = settings.panoramaUri,
            pagerState = pagerState,
            pageCount = 4,
            modifier = Modifier.fillMaxSize()
        )
        DriftingNoiseOverlay(Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            PivotHeader(
                pageNames = pageNames,
                pagerState = pagerState,
                accent = colors.accent,
                text = colors.text,
                onPageClick = goTo,
                onLongPress = { if (settings.glanceEnabled) showGlance = true },
                onHomeLongPress = {
                    showRecents = true
                },
                modifier = Modifier.graphicsLayer {
                    translationY = headerOffset.dp.toPx()
                }
            )

            Box(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(LocalEntranceTick provides entranceTick) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pageTransform(pagerState, page, settings.animationStyle, settings.silkyPager, colors.ink)
                    ) {
                        when (page) {
                            0 -> HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = openDrawer,
                                onOpenSettings = { showSettings = true },
                                onOpenFocus = openFocus
                            )
                            1 -> DrawerScreen(
                                viewModel = viewModel,
                                onOpenSettings = { showSettings = true }
                            )
                            2 -> FocusScreen(
                                viewModel = viewModel,
                                onOpenSettings = { showSettings = true },
                                onOpenDrawer = openDrawer
                            )
                            3 -> app.vanta.launcher.ui.screens.live.LiveScreen()
                            else -> {}
                        }
                    }
                }
                }
            }

            PivotDots(
                totalPages = 4,
                currentPage = activePage,
                accent = colors.accent
            )
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically(tween(280, easing = LumiaEasing)) { it / 8 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(200, easing = LumiaEasing)) { it / 8 } + fadeOut(tween(160))
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onClose = { showSettings = false }
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            CinematicSplash(
                onAnimationComplete = { showSplash = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showOnboarding && !showSplash) {
            OnboardingOverlay {
                prefs.edit().putBoolean("first_run", false).apply()
                showOnboarding = false
            }
        }

        if (showSearch) {
            SwipeDownSearch(
                apps = allApps,
                onLaunch = { pkg -> viewModel.launchApp(context, pkg) },
                onDismiss = { showSearch = false },
                onDarkMode = { dark -> viewModel.setDarkMode(dark) }
            )
        }

        GlanceOverlay(
            visible = showGlance,
            nightstand = glanceNightstand,
            onDismiss = {
                showGlance = false
                glanceNightstand = false
                lastTouchMillis = System.currentTimeMillis()
            }
        )

        if (showRecents) {
            app.vanta.launcher.ui.screens.recents.RecentsScreen(
                onDismiss = { showRecents = false },
                onOpenApp = { pkg ->
                    showRecents = false
                    viewModel.launchApp(context, pkg)
                }
            )
        }

        app.vanta.launcher.ui.components.QuickSettingsPanel(
            visible = showQuickSettings,
            onDismiss = { showQuickSettings = false }
        )
    }
}

/**
 * Page motion per style, computed in the draw phase so the four screens never recompose on a scroll frame.
 * CUBE = Windows 8.1 edge-pivot rotate. TAP_FLIP = WP7 turnstile hinged on the left edge.
 * SMOOTH = parallax slide. [silky] adds the soft scale/dim of the receding page on top of TAP_FLIP/SMOOTH.
 */
private fun Modifier.pageTransform(
    pagerState: PagerState,
    page: Int,
    style: AnimationStyle,
    silky: Boolean,
    shadow: Color
): Modifier = graphicsLayer {
    val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    val amount = abs(offset).coerceAtMost(1f)
    when (style) {
        AnimationStyle.CUBE -> {
            transformOrigin = TransformOrigin(if (offset > 0f) 1f else 0f, 0.5f)
            rotationY = (offset * -90f).coerceIn(-90f, 90f)
            cameraDistance = 32f * density
            alpha = 1f - 0.25f * amount
        }
        AnimationStyle.TAP_FLIP -> {
            transformOrigin = TransformOrigin(0f, 0.5f)
            rotationY = (offset * -70f).coerceIn(-70f, 70f)
            cameraDistance = 20f * density
            alpha = 1f - 0.55f * amount
            if (silky) {
                val sc = 1f - 0.04f * amount
                scaleX = sc
                scaleY = sc
            }
        }
        AnimationStyle.SMOOTH -> {
            translationX = offset * size.width * 0.28f
            alpha = 1f - 0.45f * amount
            if (silky) {
                val sc = 1f - 0.04f * amount
                scaleX = sc
                scaleY = sc
                rotationZ = offset * -1.5f
                shadowElevation = 0.02f * size.width * amount
                ambientShadowColor = shadow
                spotShadowColor = shadow
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PivotHeader(
    pageNames: List<String>,
    pagerState: PagerState,
    accent: Color,
    text: Color,
    onPageClick: (Int) -> Unit,
    onLongPress: () -> Unit = {},
    onHomeLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val itemShift = with(LocalDensity.current) { 12.dp.toPx() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .graphicsLayer {
                // Metro pivot: the title strip drifts at a fraction of the page offset.
                translationX = -(pagerState.currentPage + pagerState.currentPageOffsetFraction) * itemShift
            },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        pageNames.forEachIndexed { index, name ->
            val active = index == pagerState.currentPage
            val labelColor by animateColorAsState(if (active) text else text.copy(alpha = 0.38f), tween(220), label = "pivot$index")
            val underline by animateDpAsState(if (active) 22.dp else 0.dp, spring(dampingRatio = 0.8f, stiffness = 500f), label = "underline$index")
            Column(
                modifier = Modifier
                    .semantics { role = Role.Tab; selected = active }
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onPageClick(index) },
                        onLongClick = { if (index == 0) onHomeLongPress() else onLongPress() }
                    )
                    .padding(vertical = 6.dp)
            ) {
                MonoLabel(name, size = 12.sp, color = labelColor, weight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .height(3.dp)
                        .width(underline)
                        .background(accent)
                )
            }
        }
    }
}

@Composable
private fun PivotDots(totalPages: Int, currentPage: Int, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { i ->
            val active = i == currentPage
            val dot by animateDpAsState(if (active) 8.dp else 6.dp, label = "dot$i")
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(dot)
                    .background(if (active) accent else accent.copy(alpha = 0.3f))
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun OnboardingOverlay(onDone: () -> Unit) {
    val colors = LocalAppTheme.current
    val onboardingPager = rememberPagerState(pageCount = { 3 })
    val onboardingScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = onboardingPager,
                modifier = Modifier.weight(1f)
            ) { page ->
                val slide = when (page) {
                    0 -> "SWIPE TO NAVIGATE" to "Swipe left or right to move between home, apps, focus and live. Pull from the screen edge for your gesture action. Pivot dots show where you are."
                    1 -> "TILES ARE LIVE" to "Pinned apps double as live tiles. Weather, media, tasks and notifications surface on the home grid."
                    2 -> "CHECK THE LIVE PAGE" to "The live page collects what is happening now: clock, weather, media and notifications in one place."
                    else -> "" to ""
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    app.vanta.launcher.ui.components.TileEntrance(index = 0) {
                        app.vanta.launcher.ui.components.FitHeadlineText(
                            text = slide.first,
                            maxSize = 56.sp,
                            stacked = true,
                            color = colors.text
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    app.vanta.launcher.ui.components.TileEntrance(index = 3) {
                        Text(
                            text = slide.second,
                            color = colors.muted,
                            fontFamily = JetBrainsMono,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(Modifier.height(48.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SKIP",
                    color = colors.muted,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone)
                        .padding(vertical = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == onboardingPager.currentPage) 10.dp else 6.dp)
                                .background(if (i == onboardingPager.currentPage) colors.accent else colors.accent.copy(alpha = 0.3f))
                        )
                    }
                }
                Text(
                    text = if (onboardingPager.currentPage < 2) "NEXT" else "FINISH",
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {
                            if (onboardingPager.currentPage < 2) {
                                onboardingScope.launch { onboardingPager.animateScrollToPage(onboardingPager.currentPage + 1) }
                            } else {
                                onDone()
                            }
                        })
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun dispatchGestureAction(
    action: String,
    context: Context,
    scope: CoroutineScope,
    pagerState: PagerState,
    openSearch: () -> Unit
) {
    val from = pagerState.currentPage
    fun go(page: Int) = scope.launch { pagerState.animateScrollToPage(page, animationSpec = PageSpring) }
    when (action) {
        "NEXT PAGE" -> go((from + 1) % 4)
        "PREV PAGE" -> go((from + 3) % 4)
        "SEARCH" -> openSearch()
        "FOCUS" -> go(2)
        "NOTIFICATIONS" -> go(3)
        else -> Toast.makeText(context, action, Toast.LENGTH_SHORT).show()
    }
}
