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
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.net.URL;
import java.text.NumberFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
                    return loadData();
                }

                @Override
                protected void onPostExecute(Boolean flag) {
                    if (flag != null && flag)
                        updateWidget();
                }
            }.execute();
        }
    }

    private boolean loadData() {
        if (Wx.DEV) Log.d(TAG_NAME, "loadData");

        NumberFormat f = NumberFormat.getInstance();
        boolean validData = false;
        try {
            URL url = new URL(Wx.API_URL);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new InputSource(url.openStream()));
            document.getDocumentElement().normalize();
            NodeList nodes = document.getElementsByTagName("wxdata");
            Element wxdata = (nodes != null && nodes.getLength() > 0) ? (Element) nodes.item(0) : null;

            // temperature
            double outTempValue = getDoubleValueByName(wxdata, "outTemp");
            outTemp = new StringBuilder(f.format(outTempValue))
                    .append("°C")
                    .toString();

            // humidity
            double outHumidityValue = getDoubleValueByName(wxdata, "outHumidity");
            StringBuilder outHumidityBuilder = new StringBuilder(f.format(outHumidityValue))
                    .append("%");
            double dailyRainValue = Math.round(getDoubleValueByName(wxdata, "dailyRain"));
            if (dailyRainValue > 0.0)
                outHumidityBuilder.append(" ")
                        .append(f.format(dailyRainValue))
                        .append("l");
            outHumidity = outHumidityBuilder.toString();

            // wind speed
            double windSpeedValue = Math.round(getDoubleValueByName(wxdata, "windSpeed"));
            windSpeed = new StringBuilder(f.format(windSpeedValue))
                    .append(" km/h")
                    .toString();

            // wind speed
            windDir = getStringValueByName(wxdata, "windDir");

            // time stamp
            timeStamp = getLongValueByName(wxdata, "time");
            validData = true;
        } catch (Exception e) {
            if (Wx.DEV) Log.d(TAG_NAME, e.toString());
        }
        return validData;
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

    static private String getStringValueByName(Element element, String name) {
        if (element != null) {
            NodeList nodes = element.getElementsByTagName(name);
            if (nodes != null && nodes.getLength() > 0) {
                Element nodeElement = (Element)nodes.item(0);
                NodeList names = nodeElement.getChildNodes();
                if (names.getLength() > 0)
                    return String.valueOf(names.item(0).getNodeValue());
            }
        }
        return EMPTY_VALUE;
    }

    static private long getLongValueByName(Element element, String name) {

        String stringValue = getStringValueByName(element, name);
        try {
            if (!EMPTY_VALUE.equals(stringValue))
                return Long.parseLong(stringValue);
        }
        catch (NumberFormatException e) {
            // ignore;
        }
        return 0;
    }

    static private double getDoubleValueByName(Element element, String name) {

        String stringValue = getStringValueByName(element, name);
        try {
            if (!EMPTY_VALUE.equals(stringValue))
                return Double.parseDouble(stringValue);
        }
        catch (NumberFormatException e) {
            // ignore;
        }
        return 0.0;
    }
}
