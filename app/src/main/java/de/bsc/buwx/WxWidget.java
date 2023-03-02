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
package de.bsc.buwx;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;
import android.widget.RemoteViews;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.NumberFormat;

/**
 * Implementation of App Widget functionality.
 */
public class WxWidget extends AppWidgetProvider {

    public static final String ACTION_AUTO_UPDATE = "de.bsc.buwx.AUTO_UPDATE";

    private static final String LOG_TAG = "WxWidget";
    private static final String EMPTY_VALUE = "-";

    private String outTemp = EMPTY_VALUE;
    private String outHumidity = EMPTY_VALUE;
    private String windSpeed = EMPTY_VALUE;
    private String windDir = EMPTY_VALUE;
    private long timeStamp = System.currentTimeMillis();

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Wx.DEV) Log.d(LOG_TAG, "onReceive");
        if (Wx.DEV && intent != null && intent.getAction() != null) Log.d(LOG_TAG, intent.getAction());
        if (isScreenOn(context) && intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_AUTO_UPDATE)) {
            // load json data in the background an update the widget
            Handler myHandler = new Handler();
            new Thread(() -> {
                if (loadJsonData()) {
                    myHandler.post(() -> updateWidget(context));
                }
            }).start();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (Wx.DEV) Log.d(LOG_TAG, "onUpdate");

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        if (Wx.DEV) Log.d(LOG_TAG, "onEnabled");

        WidgetNotification.scheduleWidgetUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        if (Wx.DEV) Log.d(LOG_TAG, "onDisabled");

        WidgetNotification.clearWidgetUpdate(context);
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        Intent intent = new Intent(context, MainActivity.class);
        int pendingFlags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            pendingFlags = PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingFlags);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wx_widget);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

        long currentTimeStamp = System.currentTimeMillis()/1000;
        int shapeId = currentTimeStamp - timeStamp < 600 ? // 10 min
                R.drawable.wxshape : R.drawable.wxshape_outdated;
        views.setInt(R.id.widget_layout, "setBackgroundResource", shapeId);
        views.setTextViewText(R.id.view_outTemp, outTemp);
        views.setTextViewText(R.id.view_outHumidity, outHumidity);
        views.setTextViewText(R.id.view_windSpeed, windSpeed);
        views.setTextViewText(R.id.view_windDir, windDir);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private boolean loadJsonData() {
        if (Wx.DEV) Log.d(LOG_TAG, "loadJsonData");

        NumberFormat f = NumberFormat.getInstance();
        boolean validData = false;
        try {
            JSONObject json = readJsonFromUrl(Wx.JSON_URL);

            // temperature
            double outTempValue = json.optDouble("outTemp", 0.0);
            outTemp = f.format(outTempValue) + "°C";

            // humidity and rain
            double outHumidityValue = json.optDouble("outHumidity", 0.0);
            StringBuilder outHumidityBuilder = new StringBuilder(f.format(outHumidityValue))
                    .append("%");
            double dailyRainValue = Math.round(json.optDouble("dailyRain", 0.0));
            if (dailyRainValue > 0.0)
                outHumidityBuilder.append(" ")
                        .append(f.format(dailyRainValue))
                        .append("l");
            outHumidity = outHumidityBuilder.toString();

            // wind speed
            double windSpeedValue = Math.round(json.optDouble("windSpeed", 0.0));
            windSpeed = f.format(windSpeedValue) + " km/h";

            // wind speed
            windDir = json.getString("windDir");

            // time stamp
            timeStamp = json.optLong("time", 0L);
            validData = true;
        } catch (Exception e) {
            if (Wx.DEV) Log.d(LOG_TAG, e.toString());
        }
        return validData;
    }

    public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String line;
            while ((line = rd.readLine()) != null)
                sb.append(line);
            return new JSONObject(sb.toString());
        }
    }

    /**
     * Is the screen of the device on.
     * @param context the context
     * @return true when (at least one) screen is on
     */
    public boolean isScreenOn(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        }
        else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm.isScreenOn();
        }
    }
    public void updateWidget(Context context) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(),getClass().getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
