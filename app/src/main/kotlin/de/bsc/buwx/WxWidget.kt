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
import android.util.Log
import android.view.Display
import android.view.View
import android.widget.RemoteViews
import de.bsc.buwx.WidgetNotification.clearWidgetUpdate
import de.bsc.buwx.WidgetNotification.scheduleWidgetUpdate

/**
 * Home screen widget showing the current measurements of the weather station.
 *
 * Measurements are fetched on every update and never cached: when the data is
 * older than [WxData.MAX_AGE_SEC] or cannot be loaded, the widget shows dashes.
 */
class WxWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (Wx.DEV) Log.d(LOG_TAG, "onReceive ${intent?.action}")
        if (intent?.action == ACTION_AUTO_UPDATE && isScreenOn(context)) {
            refresh(context, activeWidgetIds(context))
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (Wx.DEV) Log.d(LOG_TAG, "onUpdate")
        refresh(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        if (Wx.DEV) Log.d(LOG_TAG, "onAppWidgetOptionsChanged")
        refresh(context, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context) {
        if (Wx.DEV) Log.d(LOG_TAG, "onEnabled")
        scheduleWidgetUpdate(context)
    }

    override fun onDisabled(context: Context) {
        if (Wx.DEV) Log.d(LOG_TAG, "onDisabled")
        clearWidgetUpdate(context)
    }

    /** Loads the data in the background and renders all given widgets. */
    private fun refresh(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        Thread {
            try {
                val data = try {
                    WxData.load()
                } catch (e: Exception) {
                    if (Wx.DEV) Log.d(LOG_TAG, e.toString())
                    null
                }
                val appWidgetManager = AppWidgetManager.getInstance(context)
                for (appWidgetId in appWidgetIds) {
                    render(context, appWidgetManager, appWidgetId, data)
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun render(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        data: WxData?,
    ) {
        val layout = layoutFor(appWidgetManager.getAppWidgetOptions(appWidgetId))
        val views = RemoteViews(context.packageName, layout)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(android.R.id.background, openApp)

        if (data != null && data.isCurrent()) {
            val humidity = data.formatHumidity()
            val wind = data.formatWind()
            val rain = data.formatRain()
            views.setInt(android.R.id.background, "setBackgroundResource", background(data.tempBand))
            views.setTextViewText(R.id.view_temp, data.formatTemp())
            views.setViewVisibility(R.id.view_status, View.GONE)
            views.setTextViewText(R.id.view_humidity, humidity)
            views.setContentDescription(
                R.id.view_humidity,
                context.getString(R.string.widget_humidity) + " " + humidity,
            )
            views.setTextViewText(R.id.view_wind, wind)
            views.setContentDescription(
                R.id.view_wind,
                context.getString(R.string.widget_wind) + " " + wind,
            )
            if (rain != null) {
                views.setTextViewText(R.id.view_rain, rain)
                views.setContentDescription(
                    R.id.view_rain,
                    context.getString(R.string.widget_rain) + " " + rain,
                )
                views.setViewVisibility(R.id.view_rain, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.view_rain, View.GONE)
            }
        } else {
            // outdated or missing data: show dashes rather than old values
            val status = if (data == null) R.string.widget_offline else R.string.widget_stale
            views.setInt(android.R.id.background, "setBackgroundResource", R.drawable.widget_bg_stale)
            views.setTextViewText(R.id.view_temp, context.getString(R.string.default_value))
            views.setTextViewText(R.id.view_status, context.getString(status))
            views.setViewVisibility(R.id.view_status, View.VISIBLE)
            views.setTextViewText(R.id.view_humidity, context.getString(R.string.default_value))
            views.setTextViewText(R.id.view_wind, context.getString(R.string.default_value))
            views.setViewVisibility(R.id.view_rain, View.GONE)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    /** Picks the layout matching the widget size (portrait dimensions in dp). */
    private fun layoutFor(options: Bundle): Int {
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return when {
            minWidth in 1 until 100 -> R.layout.wx_widget_small
            maxHeight >= 130 -> R.layout.wx_widget_tall
            else -> R.layout.wx_widget
        }
    }

    private fun background(band: WxData.TempBand): Int = when (band) {
        WxData.TempBand.FROST -> R.drawable.widget_bg_frost
        WxData.TempBand.COLD -> R.drawable.widget_bg_cold
        WxData.TempBand.MILD -> R.drawable.widget_bg_mild
        WxData.TempBand.WARM -> R.drawable.widget_bg_warm
        WxData.TempBand.HOT -> R.drawable.widget_bg_hot
    }

    private fun activeWidgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, WxWidget::class.java))

    /**
     * Is the screen of the device on.
     * @param context the context
     * @return true when (at least one) screen is on
     */
    private fun isScreenOn(context: Context): Boolean {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return dm.displays.any { it.state != Display.STATE_OFF }
    }

    companion object {
        const val ACTION_AUTO_UPDATE: String = "de.bsc.buwx.AUTO_UPDATE"

        private const val LOG_TAG = "WxWidget"
    }
}
