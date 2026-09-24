package com.xdlab.standard.ui.nav

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.domain.model.AnimationStyle
import com.xdlab.standard.domain.model.SettingsState
import com.xdlab.standard.ui.components.CinematicSplash
import com.xdlab.standard.ui.components.DriftingNoiseOverlay
import com.xdlab.standard.ui.components.LocalHapticsEnabled
import com.xdlab.standard.ui.components.SwipeDownSearch
import com.xdlab.standard.ui.components.edgeSwipeHandler
import com.xdlab.standard.ui.components.backSwipeHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.xdlab.standard.ui.screens.drawer.DrawerScreen
import com.xdlab.standard.ui.screens.focus.FocusScreen
import com.xdlab.standard.ui.screens.glance.GlanceOverlay
import com.xdlab.standard.ui.screens.home.HomeScreen
import com.xdlab.standard.ui.screens.settings.SettingsScreen
import com.xdlab.standard.ui.theme.JetBrainsMono
import com.xdlab.standard.ui.theme.LocalAppTheme
import com.xdlab.standard.ui.theme.LocalSettings
import com.xdlab.standard.ui.theme.SpaceGrotesk
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private var splashShownThisProcess = false

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
    var showSearch by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(prefs.getBoolean("first_run", true)) }
    var showRecents by remember { mutableStateOf(false) }
    var showQuickSettings by remember { mutableStateOf(false) }
    val allApps by viewModel.allApps.collectAsState()

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
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect {
                currentPage = it
                if (settings.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
    }

    val openDrawer: () -> Unit = {
        if (settings.animationStyle == AnimationStyle.SMOOTH) {
            currentPage = 1
        } else {
            scope.launch { pagerState.animateScrollToPage(1) }
        }
    }
    val openFocus: () -> Unit = {
        if (settings.animationStyle == AnimationStyle.SMOOTH) {
            currentPage = 2
        } else {
            scope.launch { pagerState.animateScrollToPage(2) }
        }
    }

    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showGlance) { showGlance = false }
    BackHandler(enabled = showSearch) { showSearch = false }
    BackHandler(enabled = showOnboarding) {
        prefs.edit().putBoolean("first_run", false).apply()
        showOnboarding = false
    }
    BackHandler(enabled = viewModel.editMode.value) { viewModel.setEditMode(false) }

    val pageNames = listOf("home", "apps", "focus", "live")
    val activePage = if (settings.animationStyle == AnimationStyle.SMOOTH) currentPage else pagerState.currentPage

    var homeVisible by remember { mutableStateOf(true) }
    LaunchedEffect(activePage) {
        homeVisible = activePage == 0
    }
    val homeAlpha by animateFloatAsState(
        targetValue = if (homeVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "homeAlpha"
    )
    val homeScale by animateFloatAsState(
        targetValue = if (homeVisible) 1f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "homeScale"
    )
    val headerOffset by animateFloatAsState(
        targetValue = if (homeVisible) 0f else -20f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "headerOffset"
    )

    val edgeSwipeModifier = Modifier.edgeSwipeHandler(
        enabled = true,
        onSwipeOpen = {
            val action = gesturePrefs.getString("swipe_left", "NEXT PAGE") ?: "NEXT PAGE"
            dispatchGestureAction(action, context, scope, pagerState, currentPageState, settings) { showSearch = true }
        }
    )

    val backSwipeModifier = Modifier.backSwipeHandler(
        enabled = true,
        onSwipeBack = {
            val action = gesturePrefs.getString("swipe_right", "PREV PAGE") ?: "PREV PAGE"
            dispatchGestureAction(action, context, scope, pagerState, currentPageState, settings) { showSearch = true }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .then(edgeSwipeModifier)
            .then(backSwipeModifier)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, amount ->
                        if (change.position.y < 60f && amount > 8f) {
                            showQuickSettings = true
                        }
                    }
                )
            }
    ) {
        DriftingNoiseOverlay(Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            PivotHeader(
                pageNames = pageNames,
                currentPage = activePage,
                accent = colors.accent,
                text = colors.text,
                onPageClick = { page ->
                    if (settings.animationStyle == AnimationStyle.SMOOTH) {
                        currentPage = page
                    } else {
                        scope.launch { pagerState.animateScrollToPage(page) }
                    }
                },
                onLongPress = { if (settings.glanceEnabled) showGlance = true },
                onHomeLongPress = {
                    showRecents = true
                },
                modifier = Modifier.graphicsLayer {
                    translationY = headerOffset.dp.toPx()
                }
            )

            Box(modifier = Modifier.weight(1f)) {
                if (settings.animationStyle == AnimationStyle.SMOOTH) {
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "smoothPage"
                    ) { page ->
                        when (page) {
                            0 -> Box(modifier = Modifier.graphicsLayer {
                                alpha = homeAlpha
                                scaleX = homeScale
                                scaleY = homeScale
                            }) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onOpenDrawer = openDrawer,
                                    onOpenSettings = { showSettings = true },
                                    onOpenFocus = openFocus
                                )
                            }
                            1 -> DrawerScreen(
                                viewModel = viewModel,
                                onOpenSettings = { showSettings = true }
                            )
                            2 -> FocusScreen(
                                viewModel = viewModel,
                                onOpenSettings = { showSettings = true },
                                onOpenDrawer = openDrawer
                            )
                            3 -> com.xdlab.standard.ui.screens.live.LiveScreen()
                            else -> {}
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val pageOffset = pagerState.currentPage - page + pagerState.currentPageOffsetFraction
                        val pageModifier = if (settings.animationStyle == AnimationStyle.CUBE) {
                            Modifier.fillMaxSize().graphicsLayer {
                                val absOff = abs(pageOffset)
                                rotationY = pageOffset * -28f
                                cameraDistance = 14f * density
                                scaleX = 1f - 0.08f * absOff
                                scaleY = 1f - 0.04f * absOff
                                alpha = 1f - 0.18f * absOff.coerceAtMost(1f)
                                shadowElevation = absOff * 16f
                            }
                        } else if (settings.silkyPager) {
                            val silkyActive = pagerState.currentPageOffsetFraction.coerceIn(-1f, 1f)
                            val silkyIsActive = page == pagerState.currentPage
                            val silkyScale by animateFloatAsState(
                                targetValue = if (silkyIsActive) 1f else 0.96f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                                label = "silkyScale"
                            )
                            val silkyRotation by animateFloatAsState(
                                targetValue = if (silkyIsActive) 0f else -1.5f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                                label = "silkyRotation"
                            )
                            val silkyAlpha by animateFloatAsState(
                                targetValue = if (silkyIsActive) 1f else 0.55f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                                label = "silkyAlpha"
                            )
                            val silkyTranslateX = if (silkyIsActive) silkyActive * 0.25f else 0f
                            val silkyShadow = colors.ink.copy(alpha = 0.35f)
                            Modifier.fillMaxSize().graphicsLayer {
                                this.alpha = silkyAlpha
                                this.scaleX = silkyScale
                                this.scaleY = silkyScale
                                this.rotationZ = silkyRotation
                                this.translationX = silkyTranslateX * size.width
                                shadowElevation = 0.06f * size.width.coerceAtLeast(1f)
                                ambientShadowColor = silkyShadow
                                spotShadowColor = silkyShadow
                            }
                        } else {
                            Modifier.fillMaxSize()
                        }
                        Box(modifier = pageModifier) {
                            when (page) {
                                0 -> Box(modifier = Modifier.graphicsLayer {
                                    alpha = homeAlpha
                                    scaleX = homeScale
                                    scaleY = homeScale
                                }) {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onOpenDrawer = openDrawer,
                                        onOpenSettings = { showSettings = true },
                                        onOpenFocus = openFocus
                                    )
                                }
                                1 -> DrawerScreen(
                                    viewModel = viewModel,
                                    onOpenSettings = { showSettings = true }
                                )
                                2 -> FocusScreen(
                                    viewModel = viewModel,
                                    onOpenSettings = { showSettings = true },
                                    onOpenDrawer = openDrawer
                                )
                                3 -> com.xdlab.standard.ui.screens.live.LiveScreen()
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
            enter = fadeIn(),
            exit = fadeOut()
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
                onDismiss = { showSearch = false }
            )
        }

        GlanceOverlay(
            visible = showGlance,
            onDismiss = { showGlance = false }
        )

        if (showRecents) {
            com.xdlab.standard.ui.screens.recents.RecentsScreen(
                onDismiss = { showRecents = false },
                onOpenApp = { pkg ->
                    showRecents = false
                    viewModel.launchApp(context, pkg)
                }
            )
        }

        com.xdlab.standard.ui.components.QuickSettingsPanel(
            visible = showQuickSettings,
            onDismiss = { showQuickSettings = false }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PivotHeader(
    pageNames: List<String>,
    currentPage: Int,
    accent: Color,
    text: Color,
    onPageClick: (Int) -> Unit,
    onLongPress: () -> Unit = {},
    onHomeLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        pageNames.forEachIndexed { index, name ->
            Column(
                modifier = Modifier.combinedClickable(
                    onClick = { onPageClick(index) },
                    onLongClick = { if (index == 0) onHomeLongPress() else onLongPress() }
                )
            ) {
                Text(
                    text = name,
                    color = if (index == currentPage) accent else text.copy(alpha = 0.35f),
                    fontFamily = FontFamily.Default,
                    fontWeight = if (index == currentPage) FontWeight.Black else FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(if (index == currentPage) accent else Color.Transparent)
                        .height(3.dp)
                        .width(if (index == currentPage) 32.dp else 0.dp)
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
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (i == currentPage) 8.dp else 6.dp)
                    .background(if (i == currentPage) accent else accent.copy(alpha = 0.3f))
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
                    0 -> "SWIPE TO NAVIGATE" to "Edge-swipe left or right to move between home, apps, focus and live. Pivot dots show where you are."
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
                    Text(
                        text = slide.first,
                        color = colors.text,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Black,
                        fontSize = 48.sp,
                        lineHeight = 44.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = slide.second,
                        color = colors.muted,
                        fontFamily = JetBrainsMono,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
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
                        .combinedClickable(onClick = onDone)
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
                        .combinedClickable(onClick = {
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
    currentPage: MutableState<Int>,
    settings: SettingsState,
    openSearch: () -> Unit
) {
    val smooth = settings.animationStyle == AnimationStyle.SMOOTH
    val from = if (smooth) currentPage.value else pagerState.currentPage
    when (action) {
        "NEXT PAGE" -> {
            val next = (from + 1) % 4
            if (smooth) currentPage.value = next else scope.launch { pagerState.animateScrollToPage(next) }
        }
        "PREV PAGE" -> {
            val prev = (from + 3) % 4
            if (smooth) currentPage.value = prev else scope.launch { pagerState.animateScrollToPage(prev) }
        }
        "SEARCH" -> openSearch()
        "FOCUS" -> if (smooth) currentPage.value = 2 else scope.launch { pagerState.animateScrollToPage(2) }
        "NOTIFICATIONS" -> if (smooth) currentPage.value = 3 else scope.launch { pagerState.animateScrollToPage(3) }
        else -> Toast.makeText(context, action, Toast.LENGTH_SHORT).show()
    }
}
