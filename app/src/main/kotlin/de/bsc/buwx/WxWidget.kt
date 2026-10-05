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
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.widget.RemoteViews
import de.bsc.buwx.WidgetNotification.clearWidgetUpdate
import de.bsc.buwx.WidgetNotification.scheduleWidgetUpdate
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.Charset
import java.text.NumberFormat
import kotlin.math.round

/**
 * Implementation of App Widget functionality.
 */
class WxWidget : AppWidgetProvider() {
    private var outTemp: String = EMPTY_VALUE
    private var outHumidity: String = EMPTY_VALUE
    private var windSpeed: String = EMPTY_VALUE
    private var windDir: String = EMPTY_VALUE
    private var timeStamp = System.currentTimeMillis()

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (Wx.DEV) Log.d(LOG_TAG, "onReceive")
        if (Wx.DEV && (intent != null) && (intent.action != null)) Log.d(
            LOG_TAG,
            intent.action!!,
        )
        if (isScreenOn(context) && (intent != null) && (intent.action != null) && (intent.action == ACTION_AUTO_UPDATE)) {
            // load json data in the background an update the widget
            val myHandler = Handler(Looper.getMainLooper())
            Thread {
                if (loadJsonData()) {
                    myHandler.post { updateWidget(context) }
                }
            }.start()
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (Wx.DEV) Log.d(LOG_TAG, "onUpdate")

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        if (Wx.DEV) Log.d(LOG_TAG, "onEnabled")

        scheduleWidgetUpdate(context)
    }

    override fun onDisabled(context: Context) {
        if (Wx.DEV) Log.d(LOG_TAG, "onDisabled")

        clearWidgetUpdate(context)
    }

    private fun updateAppWidget(
        context: Context, appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val intent = Intent(context, MainActivity::class.java)
        var pendingFlags = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) pendingFlags =
            PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingFlags)

        val views = RemoteViews(context.packageName, R.layout.wx_widget)
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

        val currentTimeStamp = System.currentTimeMillis() / 1000
        val shapeId = if ((currentTimeStamp - timeStamp) < 600)  // 10 min
            R.drawable.wxshape else R.drawable.wxshape_outdated
        views.setInt(R.id.widget_layout, "setBackgroundResource", shapeId)
        views.setTextViewText(R.id.view_outTemp, outTemp)
        views.setTextViewText(R.id.view_outHumidity, outHumidity)
        views.setTextViewText(R.id.view_windSpeed, windSpeed)
        views.setTextViewText(R.id.view_windDir, windDir)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun loadJsonData(): Boolean {
        if (Wx.DEV) Log.d(LOG_TAG, "loadJsonData")

        val f = NumberFormat.getInstance()
        var validData = false
        try {
            val json = readJsonFromUrl(Wx.JSON_URL)

            // temperature
            val outTempValue = json.optDouble("outTemp", 0.0)
            outTemp = f.format(outTempValue) + "°C"

            // humidity and rain
            val outHumidityValue = json.optDouble("outHumidity", 0.0)
            val outHumidityBuilder = StringBuilder(f.format(outHumidityValue))
                .append("%")
            val dailyRainValue = round(json.optDouble("dailyRain", 0.0))
            if (dailyRainValue > 0.0) outHumidityBuilder.append(" ")
                .append(f.format(dailyRainValue))
                .append("l")
            outHumidity = outHumidityBuilder.toString()

            // wind speed
            val windSpeedValue = round(json.optDouble("windSpeed", 0.0))
            windSpeed = f.format(windSpeedValue) + " km/h"

            // wind speed
            windDir = json.getString("windDir")

            // time stamp
            timeStamp = json.optLong("time", 0L)
            validData = true
        } catch (e: Exception) {
            if (Wx.DEV) Log.d(LOG_TAG, e.toString())
        }
        return validData
    }

    @Throws(IOException::class, JSONException::class)
    fun readJsonFromUrl(url: String?): JSONObject {
        URL(url).openStream().use { `is` ->
            val sb = StringBuilder()
            val rd = BufferedReader(InputStreamReader(`is`, Charset.forName("UTF-8")))
            var line: String?
            while ((rd.readLine().also { line = it }) != null) sb.append(line)
            return JSONObject(sb.toString())
        }
    }

    /**
     * Is the screen of the device on.
     * @param context the context
     * @return true when (at least one) screen is on
     */
    fun isScreenOn(context: Context): Boolean {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        var screenOn = false
        for (display in dm.displays) {
            if (display.state != Display.STATE_OFF) {
                screenOn = true
            }
        }
        return screenOn
    }

    fun updateWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidgetComponentName =
            ComponentName(context.packageName, javaClass.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        const val ACTION_AUTO_UPDATE: String = "de.bsc.buwx.AUTO_UPDATE"

        private const val LOG_TAG = "WxWidget"
        private const val EMPTY_VALUE = "-"
    }
}
