package de.bsc.buwx;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class WidgetNotification {

    private static final String LOG_TAG = "WidgetNotification";
    private static final int WIDGET_REQUEST_CODE = 685355;
    private static final int UPDATE_INTERVAL_SEC = 60;

    public static void scheduleWidgetUpdate(Context context) {
        if (Wx.DEV) Log.d(LOG_TAG, "scheduleWidgetUpdate");

        if(getActiveWidgetIds(context)!=null && getActiveWidgetIds(context).length>0) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi = getWidgetAlarmIntent(context);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());

            am.setInexactRepeating(
                    AlarmManager.RTC,
                    calendar.getTimeInMillis(),
                    (UPDATE_INTERVAL_SEC*1000),
                    pi);
        }
    }

    public static void clearWidgetUpdate(Context context) {
        if (Wx.DEV) Log.d(LOG_TAG, "clearWidgetUpdate");

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getWidgetAlarmIntent(context));
    }

    private static PendingIntent getWidgetAlarmIntent(Context context) {
        int pendingFlags = PendingIntent.FLAG_CANCEL_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            pendingFlags |= PendingIntent.FLAG_MUTABLE;
        Intent intent = new Intent(context, WxWidget.class)
                .setAction(WxWidget.ACTION_AUTO_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,getActiveWidgetIds(context));
        return PendingIntent.getBroadcast(
                context,
                WIDGET_REQUEST_CODE,
                intent,
                pendingFlags);
    }

    private static int[] getActiveWidgetIds(Context context){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        return appWidgetManager.getAppWidgetIds(new ComponentName(context, WxWidget.class));
    }
}
