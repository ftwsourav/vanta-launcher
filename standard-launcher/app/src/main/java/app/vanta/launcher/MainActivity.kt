package app.vanta.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
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
import app.vanta.launcher.di.AppContainer
import app.vanta.launcher.domain.model.DarkMode
import app.vanta.launcher.ui.components.LocalHapticsEnabled
import app.vanta.launcher.ui.components.LocalTileColors
import app.vanta.launcher.ui.components.TileStyle
import app.vanta.launcher.ui.components.tileColors
import app.vanta.launcher.ui.nav.StandardApp
import app.vanta.launcher.ui.nav.StandardAppViewModel
import app.vanta.launcher.ui.theme.AppColors
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.LocalSettings
import app.vanta.launcher.ui.theme.animateAppColors
import app.vanta.launcher.util.RefreshRate

open class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as StandardApplication).container

    protected val viewModel: StandardAppViewModel by viewModels { StandardAppViewModel.factory(container) }

    /** Hosted widgets only receive updates while the host listens; scope it to the visible activity. */
    override fun onStart() {
        super.onStart()
        app.vanta.launcher.ui.components.VantaWidgetHost.startListening(this)
    }

    override fun onStop() {
        app.vanta.launcher.ui.components.VantaWidgetHost.stopListening()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.app_name)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("standard_settings", Context.MODE_PRIVATE)
        val refreshMode = prefs.getString("refresh_rate_mode", "AUTO") ?: "AUTO"
        RefreshRate.applyRefreshRateMode(this, refreshMode)
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        val batteryPrompted = prefs.getBoolean("battery_prompted", false)
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName) && !batteryPrompted) {
            try {
                val intent = Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = android.net.Uri.parse("package:$packageName")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (e: Exception) { }
            prefs.edit().putBoolean("battery_prompted", true).apply()
        }
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
            val todPalette = app.vanta.launcher.ui.theme.rememberTimeOfDayPalette(enabled = settings.timeOfDayTint)
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
