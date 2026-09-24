package com.xdlab.standard.util

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Refresh-rate helpers. Springs are time-based, so they are NOT scaled by Hz any more;
 * the only thing the panel rate changes is that we ask the window for its highest mode.
 */
object RefreshRate {

    const val FALLBACK_HZ = 60f

    fun readHz(context: Context): Float = try {
        context.display?.refreshRate ?: FALLBACK_HZ
    } catch (e: Exception) {
        FALLBACK_HZ
    }

    /** Ask for the highest refresh rate available at the current resolution (120Hz on the OnePlus 13). */
    fun preferHighestRefreshRate(activity: Activity) {
        try {
            val display = activity.display ?: return
            val current = display.mode
            val best = display.supportedModes
                .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
                .maxByOrNull { it.refreshRate } ?: return
            if (best.refreshRate > current.refreshRate + 0.5f) {
                val lp = activity.window.attributes
                lp.preferredDisplayModeId = best.modeId
                activity.window.attributes = lp
            }
        } catch (e: Exception) {
        }
    }

    fun request120fps(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                activity.window.decorView.setRequestedFrameRate(120f)
            }
        } catch (e: Exception) {
        }
    }

    fun applyRefreshRateMode(activity: Activity, mode: String) {
        when (mode) {
            "AUTO" -> preferHighestRefreshRate(activity)
            "HZ60" -> setExactHz(activity, 60f)
            "HZ90" -> setExactHz(activity, 90f)
            "HZ120" -> setExactHz(activity, 120f)
            "MAX" -> {
                preferHighestRefreshRate(activity)
                request120fps(activity)
            }
            else -> preferHighestRefreshRate(activity)
        }
    }

    private fun setExactHz(activity: Activity, targetHz: Float) {
        try {
            val display = activity.display ?: return
            val matching = display.supportedModes.filter {
                it.physicalWidth == display.mode.physicalWidth &&
                it.physicalHeight == display.mode.physicalHeight &&
                kotlin.math.abs(it.refreshRate - targetHz) < 1f
            }.maxByOrNull { it.refreshRate }
            if (matching != null) {
                val lp = activity.window.attributes
                lp.preferredDisplayModeId = matching.modeId
                activity.window.attributes = lp
            } else {
                preferHighestRefreshRate(activity)
            }
        } catch (e: Exception) { }
    }

    /** Default tile/flip spring: quick settle with a hint of overshoot. */
    fun springSpec(): SpringSpec<Float> = spring(dampingRatio = 0.78f, stiffness = 560f)

    val Snappy: SpringSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 900f)
    val Soft: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 300f)
    val Bouncy: SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 500f)
}
