package app.vanta.launcher.ui.theme

import androidx.compose.runtime.compositionLocalOf
import app.vanta.launcher.domain.model.SettingsState

/** Current settings for composables that should not be threaded a parameter (units, clock format). */
val LocalSettings = compositionLocalOf { SettingsState() }
