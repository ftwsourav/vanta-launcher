package com.xdlab.standard.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

class WidgetHostManager(context: Context) {
    private val ctx = context
    private val appWidgetHost = AppWidgetHost(context, 0x5EED)
    private val appWidgetManager = AppWidgetManager.getInstance(context)

    fun startListening() { appWidgetHost.startListening() }
    fun stopListening() { appWidgetHost.stopListening() }

    fun allocateWidgetId(): Int = appWidgetHost.allocateAppWidgetId()
    fun deleteWidgetId(id: Int) { appWidgetHost.deleteAppWidgetId(id) }

    fun getProviders(): List<AppWidgetProviderInfo> = appWidgetManager.getInstalledProviders()

    fun bindWidget(widgetId: Int, provider: AppWidgetProviderInfo): Boolean {
        return try {
            appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider.provider)
        } catch (e: Exception) { false }
    }

    fun createView(widgetId: Int): AppWidgetHostView? {
        val info = appWidgetManager.getAppWidgetInfo(widgetId) ?: return null
        return appWidgetHost.createView(ctx, widgetId, info)
    }
}