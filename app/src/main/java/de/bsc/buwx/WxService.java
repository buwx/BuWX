package de.bsc.buwx;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * The Wx Widget
 * Created by Micha on 30.09.2015.
 */
public class WxService extends Service {

    private static final String TAG_NAME = "WxService";

    private BroadcastReceiver receiver = null;

    private boolean screenOn = true;

    private String outTemp = "-";
    private String outHumidity = "-";
    private String dailyRain = "-";
    private String windSpeed = "-";
    private String windDir = "-";

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
        boolean validData = false;
        try {
            URL url = new URL("http://ws.buwx.de/wxdata.xml");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new InputSource(url.openStream()));
            document.getDocumentElement().normalize();
            NodeList nodes = document.getElementsByTagName("wxdata");
            Element wxdata = (nodes != null && nodes.getLength() > 0) ? (Element) nodes.item(0) : null;
            outTemp = getElementByName(wxdata, "outTemp");
            outHumidity = getElementByName(wxdata, "outHumidity");
            dailyRain = getElementByName(wxdata, "dailyRain");
            windSpeed = getElementByName(wxdata, "windSpeed");
            windDir = getElementByName(wxdata, "windDir");
            validData = true;
        } catch (Exception e) {
            if (Wx.DEV) Log.d(TAG_NAME, e.toString());
        }
        return validData;
    }

    private void updateWidget() {
        if (Wx.DEV) Log.d(TAG_NAME, "updateWidget");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.wx_widget);

        views.setTextViewText(R.id.view_outTemp, outTemp);
        views.setTextViewText(R.id.view_outHumidity, outHumidity);
        views.setTextViewText(R.id.view_dailyRain, dailyRain);
        views.setTextViewText(R.id.view_windSpeed, windSpeed);
        views.setTextViewText(R.id.view_windDir, windDir);

        // Instruct the widget manager to update the widget
        ComponentName thisWidget = new ComponentName(this, WxWidget.class);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    static private String getElementByName(Element element, String name) {
        if (element != null) {
            NodeList nodes = element.getElementsByTagName(name);
            if (nodes != null && nodes.getLength() > 0) {
                Element nodeElement = (Element)nodes.item(0);
                NodeList names = nodeElement.getChildNodes();
                if (names.getLength() > 0)
                    return String.valueOf(names.item(0).getNodeValue());
            }
        }
        return "-";
    }
}
