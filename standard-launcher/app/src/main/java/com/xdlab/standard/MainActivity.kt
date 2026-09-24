package com.xdlab.standard

import android.content.Context
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
import com.xdlab.standard.domain.model.DarkMode
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
        val prefs = getSharedPreferences("standard_settings", Context.MODE_PRIVATE)
        val refreshMode = prefs.getString("refresh_rate_mode", "AUTO") ?: "AUTO"
        RefreshRate.applyRefreshRateMode(this, refreshMode)
        setContent {
            val settings by viewModel.settings.collectAsState()
            val refreshModeRecheck = getSharedPreferences("standard_settings", Context.MODE_PRIVATE)
                .getString("refresh_rate_mode", "AUTO") ?: "AUTO"
            LaunchedEffect(refreshModeRecheck) {
                RefreshRate.applyRefreshRateMode(this@MainActivity, refreshModeRecheck)
            }
            val effectiveDark = when (settings.darkMode) {
                DarkMode.AUTO_SYSTEM -> {
                    val nightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
                DarkMode.AUTO_TIME -> {
                    val cal = java.util.Calendar.getInstance()
                    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    hour < 7 || hour >= 19
                }
                DarkMode.LIGHT -> false
                DarkMode.DARK -> true
            }
            val theme = container.themeRepository.themeById(settings.themeId)
            val target = AppColors(
                surface = theme.surface(effectiveDark),
                dark = effectiveDark,
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
