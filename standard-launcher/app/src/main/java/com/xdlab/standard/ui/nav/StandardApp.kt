package com.xdlab.standard.ui.nav

import android.content.Context
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdlab.standard.domain.model.AnimationStyle
import com.xdlab.standard.ui.components.CinematicSplash
import com.xdlab.standard.ui.components.DriftingNoiseOverlay
import com.xdlab.standard.ui.components.LocalHapticsEnabled
import com.xdlab.standard.ui.components.edgeSwipeHandler
import com.xdlab.standard.ui.components.backSwipeHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.xdlab.standard.ui.screens.drawer.DrawerScreen
import com.xdlab.standard.ui.screens.focus.FocusScreen
import com.xdlab.standard.ui.screens.glance.GlanceOverlay
import com.xdlab.standard.ui.screens.home.HomeScreen
import com.xdlab.standard.ui.screens.settings.SettingsScreen
import com.xdlab.standard.ui.theme.LocalAppTheme
import com.xdlab.standard.ui.theme.LocalSettings
import kotlin.math.abs
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StandardApp(viewModel: StandardAppViewModel) {
    val settings by viewModel.settings.collectAsState()
    val colors = LocalAppTheme.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showSplash by remember { mutableStateOf(settings.cinematicIntro) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showGlance by remember { mutableStateOf(false) }
    var isFirstRun by remember {
        mutableStateOf(
            context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE)
                .getBoolean("first_run", true)
        )
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
    var currentPage by rememberSaveable { mutableStateOf(0) }

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

    val edgeSwipeModifier = if (activePage == 0) {
        Modifier.edgeSwipeHandler(
            enabled = true,
            onSwipeOpen = { scope.launch { pagerState.animateScrollToPage(1) } }
        )
    } else {
        Modifier
    }

    val backSwipeModifier = if (activePage == 1) {
        Modifier.backSwipeHandler(
            enabled = true,
            onSwipeBack = { scope.launch { pagerState.animateScrollToPage(0) } }
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .then(edgeSwipeModifier)
            .then(backSwipeModifier)
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

        if (isFirstRun && !showSplash) {
            AnimatedVisibility(
                visible = isFirstRun,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .combinedClickable(onClick = {
                            isFirstRun = false
                            context.getSharedPreferences("standard_settings", Context.MODE_PRIVATE)
                                .edit().putBoolean("first_run", false).apply()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "STANDARD.",
                            color = Color.White,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                        Text(
                            text = "Swipe for Apps / Focus\nTap any tile to launch\nLong-press in Apps to pin",
                            color = Color.White.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "TAP TO DISMISS",
                            color = colors.accent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }

        GlanceOverlay(
            visible = showGlance,
            onDismiss = { showGlance = false }
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
                    onLongClick = { onLongPress() }
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
