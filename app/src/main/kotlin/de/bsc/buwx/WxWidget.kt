/*
 * ----------------------------------------------------------------------------
 *
 * Copyright 2015 Michael Buchfink (buchfink@web.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ----------------------------------------------------------------------------
 *
 * 30.09.2015 - Creation date
 *
 * ----------------------------------------------------------------------------
 */
package de.bsc.buwx

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import kotlin.concurrent.thread

/**
 * Home screen widget showing the current measurements of the weather station.
 *
 * Measurements are fetched on every update and never cached: when the data is
 * older than [WxData.MAX_AGE_SEC] or cannot be loaded, the widget shows dashes.
 */
class WxWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        logDebug(LOG_TAG) { "onReceive ${intent?.action}" }
        if (intent?.action == ACTION_AUTO_UPDATE && isScreenOn(context)) {
            refresh(context, activeWidgetIds(context))
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        refresh(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        refresh(context, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context) = WidgetUpdateScheduler.schedule(context)

    override fun onDisabled(context: Context) = WidgetUpdateScheduler.cancel(context)

    /** Loads the data in the background and renders all given widgets. */
    private fun refresh(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        thread(name = LOG_TAG) {
            try {
                val data = loadData()
                val appWidgetManager = AppWidgetManager.getInstance(context)
                for (appWidgetId in appWidgetIds) {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, options, data))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun loadData(): WxData? = try {
        WxData.load()
    } catch (e: Exception) {
        logDebug(LOG_TAG) { e.toString() }
        null
    }

    private fun buildViews(context: Context, options: Bundle, data: WxData?): RemoteViews {
        val views = RemoteViews(context.packageName, layoutFor(options))
        views.setOnClickPendingIntent(android.R.id.background, openAppIntent(context))
        when {
            data == null -> showUnavailable(context, views, R.string.widget_offline)
            !data.isCurrent() -> showUnavailable(context, views, R.string.widget_stale)
            else -> showMeasurements(context, views, data)
        }
        return views
    }

    private fun showMeasurements(context: Context, views: RemoteViews, data: WxData) {
        views.setBackground(data.tempBand.background)
        views.setTextViewText(R.id.view_temp, data.formatTemp())
        views.setViewVisibility(R.id.view_status, View.GONE)
        views.setDetail(context, R.id.view_humidity, R.string.widget_humidity, data.formatHumidity())
        views.setDetail(context, R.id.view_wind, R.string.widget_wind, data.formatWind())
        views.setDetail(context, R.id.view_rain, R.string.widget_rain, data.formatRain())
    }

    /** Outdated or missing data: show dashes rather than old values. */
    private fun showUnavailable(context: Context, views: RemoteViews, @StringRes status: Int) {
        val dash = context.getString(R.string.default_value)
        views.setBackground(R.drawable.widget_bg_stale)
        views.setTextViewText(R.id.view_temp, dash)
        views.setTextViewText(R.id.view_status, context.getString(status))
        views.setViewVisibility(R.id.view_status, View.VISIBLE)
        views.setDetail(context, R.id.view_humidity, R.string.widget_humidity, dash)
        views.setDetail(context, R.id.view_wind, R.string.widget_wind, dash)
        views.setDetail(context, R.id.view_rain, R.string.widget_rain, null)
    }

    private fun RemoteViews.setBackground(@DrawableRes background: Int) =
        setInt(android.R.id.background, "setBackgroundResource", background)

    /** Shows a detail value with an accessible label, or hides the view when [value] is null. */
    private fun RemoteViews.setDetail(
        context: Context,
        @IdRes viewId: Int,
        @StringRes label: Int,
        value: String?,
    ) {
        if (value == null) {
            setViewVisibility(viewId, View.GONE)
            return
        }
        setTextViewText(viewId, value)
        setContentDescription(viewId, context.getString(label) + " " + value)
        setViewVisibility(viewId, View.VISIBLE)
    }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Picks the layout matching the widget size (portrait dimensions in dp). */
    @LayoutRes
    private fun layoutFor(options: Bundle): Int {
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return when {
            minWidth in 1 until SMALL_MAX_WIDTH_DP -> R.layout.wx_widget_small
            maxHeight >= TALL_MIN_HEIGHT_DP -> R.layout.wx_widget_tall
            else -> R.layout.wx_widget
        }
    }

    /** True when at least one display of the device is on. */
    private fun isScreenOn(context: Context): Boolean =
        context.getSystemService(DisplayManager::class.java).displays
            .any { it.state != Display.STATE_OFF }

    companion object {
        const val ACTION_AUTO_UPDATE: String = "de.bsc.buwx.AUTO_UPDATE"

        private const val LOG_TAG = "WxWidget"
        private const val SMALL_MAX_WIDTH_DP = 100
        private const val TALL_MIN_HEIGHT_DP = 130

        fun activeWidgetIds(context: Context): IntArray =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, WxWidget::class.java))
    }
}
