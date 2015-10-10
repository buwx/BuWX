package de.bsc.buwx;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Implementation of App Widget functionality.
 */
public class WxWidget extends AppWidgetProvider {

    private static final String LOG_TAG = "WxWidget";
    private static final int UPDATE_INTERVAL_SEC = 30;

    private PendingIntent service = null;

    @Override
    public void onEnabled(Context context) {
        if (Wx.DEV) Log.d(LOG_TAG, "onEnabled");

        final AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        final Calendar TIME = Calendar.getInstance();
        TIME.set(Calendar.MINUTE, 0);
        TIME.set(Calendar.SECOND, 0);
        TIME.set(Calendar.MILLISECOND, 0);

        if (service == null) {
            final Intent intent = new Intent(context, WxService.class);
            service = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        manager.setRepeating(AlarmManager.RTC, TIME.getTime().getTime(), 1000 * UPDATE_INTERVAL_SEC, service);
    }

    @Override
    public void onDisabled(Context context) {
        if (Wx.DEV) Log.d(LOG_TAG, "onDisabled");

        final AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(service);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        if (Wx.DEV) Log.d(LOG_TAG, "onUpdate");

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wx_widget);
            views.setTextViewText(R.id.view_outTemp, "-");
            views.setTextViewText(R.id.view_outHumidity, "-");
            views.setTextViewText(R.id.view_dailyRain, "-");
            views.setTextViewText(R.id.view_windSpeed, "-");
            views.setTextViewText(R.id.view_windDir, "-");
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
