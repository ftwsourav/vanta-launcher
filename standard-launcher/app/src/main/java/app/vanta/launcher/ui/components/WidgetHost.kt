package app.vanta.launcher.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * The one AppWidgetHost of the process (id 0x5EED), created lazily with the application context.
 * MainActivity must call [startListening] in onStart and [stopListening] in onStop so hosted
 * widgets receive RemoteViews updates only while Home is visible.
 */
object VantaWidgetHost {
    const val HOST_ID = 0x5EED

    @Volatile private var host: AppWidgetHost? = null

    fun host(context: Context): AppWidgetHost =
        host ?: synchronized(this) { host ?: Host(context.applicationContext).also { host = it } }

    fun startListening(context: Context) { host(context).startListening() }
    fun stopListening() { host?.stopListening() }

    fun allocateId(context: Context): Int = host(context).allocateAppWidgetId()
    fun deleteId(context: Context, id: Int) { host(context).deleteAppWidgetId(id) }

    /** Provider info for a bound id, or null when the provider was uninstalled or the id never bound. */
    fun info(context: Context, id: Int): AppWidgetProviderInfo? =
        AppWidgetManager.getInstance(context).getAppWidgetInfo(id)

    /** A [VantaWidgetHostView] for the id, or null when its provider is gone. */
    fun createView(context: Context, id: Int): AppWidgetHostView? {
        val info = info(context, id) ?: return null
        return host(context).createView(context, id, info)
    }

    private class Host(context: Context) : AppWidgetHost(context, HOST_ID) {
        override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
            VantaWidgetHostView(context)
    }
}

/**
 * Host view that owns the long press: widgets consume touches for their own click targets, so a
 * Compose gesture above them never fires. We time the press in onInterceptTouchEvent and, once it
 * qualifies, intercept (children get ACTION_CANCEL) and call [onLongPress]. Also reports its own
 * size to the provider on layout so responsive widgets pick the right RemoteViews.
 */
class VantaWidgetHostView(context: Context) : AppWidgetHostView(context) {
    var onLongPress: (() -> Unit)? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var fired = false
    private val check = Runnable {
        fired = true
        parent?.requestDisallowInterceptTouchEvent(true)
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        track(ev)
        return fired
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        track(ev)
        return true // keep the stream so the long-press timer can be cancelled on UP
    }

    private fun track(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x; downY = ev.y; fired = false
                removeCallbacks(check)
                postDelayed(check, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE ->
                if (abs(ev.x - downX) > slop || abs(ev.y - downY) > slop) removeCallbacks(check)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(check)
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(check)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0 || appWidgetInfo == null) return
        val d = resources.displayMetrics.density
        val wDp = w / d
        val hDp = h / d
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateAppWidgetSize(Bundle(), listOf(SizeF(wDp, hDp)))
        } else {
            @Suppress("DEPRECATION")
            updateAppWidgetSize(null, wDp.toInt(), hDp.toInt(), wDp.toInt(), hDp.toInt())
        }
    }
}
