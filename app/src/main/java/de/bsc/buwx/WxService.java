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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;
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
 * The Wx Widget
 */
public class WxService extends Service {

    private static final String TAG_NAME = "WxService";
    private static final String EMPTY_VALUE = "-";

    private BroadcastReceiver receiver = null;

    private boolean screenOn = true;

    private String outTemp = EMPTY_VALUE;
    private String outHumidity = EMPTY_VALUE;
    private String windSpeed = EMPTY_VALUE;
    private String windDir = EMPTY_VALUE;
    private long timeStamp = 0;

    public WxService() {
        if (Wx.DEV) Log.d(TAG_NAME, "WxService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final String channelId = "ForegroundServiceChannel";
        if (Wx.DEV) Log.d(TAG_NAME, "onCreate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    channelId,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);

            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("Foreground Service")
                    .build();
            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Wx.DEV) Log.d(TAG_NAME, "onStartCommand");

        if (receiver == null) {
            IntentFilter screenStateFilter = new IntentFilter();
            screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
            screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                        if (Wx.DEV) Log.d(TAG_NAME, "screenOff");
                        screenOn = false;
                    } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                        if (Wx.DEV) Log.d(TAG_NAME, "screenOn");
                        screenOn = true;
                        update();
                    }
                }
            };
            getApplicationContext().registerReceiver(receiver, screenStateFilter);
        }

        update();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (Wx.DEV) Log.d(TAG_NAME, "onBind");

        return null;
    }

    private void update() {
        if (screenOn) {
            new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... voids) {
                    return loadJsonData();
                }

                @Override
                protected void onPostExecute(Boolean flag) {
                    if (flag != null && flag)
                        updateWidget();
                }
            }.execute();
        }
    }

    private boolean loadJsonData() {
        if (Wx.DEV) Log.d(TAG_NAME, "loadJsonData");

        NumberFormat f = NumberFormat.getInstance();
        boolean validData = false;
        try {
            JSONObject json = readJsonFromUrl(Wx.JSON_URL);

            // temperature
            double outTempValue = json.optDouble("outTemp", 0.0);
            outTemp = new StringBuilder(f.format(outTempValue))
                    .append("°C")
                    .toString();

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
            windSpeed = new StringBuilder(f.format(windSpeedValue))
                    .append(" km/h")
                    .toString();

            // wind speed
            windDir = json.getString("windDir");

            // time stamp
            timeStamp = json.optLong("time", 0L);
            validData = true;
        } catch (Exception e) {
            if (Wx.DEV) Log.d(TAG_NAME, e.toString());
        }
        return validData;
    }

    public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String line = null;
            while ((line = rd.readLine()) != null)
                sb.append(line);
            return new JSONObject(sb.toString());
        } finally {
            is.close();
        }
    }

    private void updateWidget() {
        if (Wx.DEV) Log.d(TAG_NAME, "updateWidget");

        Context context = getApplicationContext();
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.wx_widget);
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
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, WxWidget.class);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }
}
