package com.xdlab.standard.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.xdlab.standard.domain.model.SettingsState

/** Current settings for composables that should not be threaded a parameter (units, clock format). */
val LocalSettings = compositionLocalOf { SettingsState() }
