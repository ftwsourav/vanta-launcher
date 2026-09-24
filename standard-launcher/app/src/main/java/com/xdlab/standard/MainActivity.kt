package com.xdlab.standard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowInsetsControllerCompat
import com.xdlab.standard.di.AppContainer
import com.xdlab.standard.ui.components.LocalHapticsEnabled
import com.xdlab.standard.ui.components.LocalTileColors
import com.xdlab.standard.ui.components.TileStyle
import com.xdlab.standard.ui.components.tileColors
import com.xdlab.standard.ui.nav.StandardApp
import com.xdlab.standard.ui.nav.StandardAppViewModel
import com.xdlab.standard.ui.theme.AppColors
import com.xdlab.standard.ui.theme.LocalAppTheme
import com.xdlab.standard.ui.theme.LocalSettings
import com.xdlab.standard.ui.theme.animateAppColors
import com.xdlab.standard.util.RefreshRate

open class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as StandardApplication).container

    protected val viewModel: StandardAppViewModel by viewModels { StandardAppViewModel.factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.app_name)
        enableEdgeToEdge()
        RefreshRate.preferHighestRefreshRate(this)
        RefreshRate.request120fps(this)
        setContent {
            val settings by viewModel.settings.collectAsState()
            val theme = container.themeRepository.themeById(settings.themeId)
            val target = AppColors(
                surface = theme.surface(settings.darkMode),
                dark = settings.darkMode,
                useTexture = settings.useTexture,
                textureStrength = settings.textureStrength,
                noiseDrift = settings.noiseDrift
            )
            val todPalette = com.xdlab.standard.ui.theme.rememberTimeOfDayPalette(enabled = settings.timeOfDayTint)
            val effectiveTarget = if (settings.timeOfDayTint) {
                target.copy(
                    background = todPalette.background,
                    tile = todPalette.tileFill,
                    accent = todPalette.accent
                )
            } else {
                target
            }
            val colors = animateAppColors(effectiveTarget)

            val lightBars = target.background.luminance() > 0.5f
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
                StandardApp(viewModel = viewModel)
            }
        }
    }
}
