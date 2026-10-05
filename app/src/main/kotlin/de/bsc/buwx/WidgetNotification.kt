package de.bsc.buwx

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object WidgetNotification {
    private const val LOG_TAG = "WidgetNotification"
    private const val WIDGET_REQUEST_CODE = 685355
    private const val UPDATE_INTERVAL_SEC = 60

    @JvmStatic
    fun scheduleWidgetUpdate(context: Context) {
        if (Wx.DEV) Log.d(LOG_TAG, "scheduleWidgetUpdate")

        if ((getActiveWidgetIds(context) != null) && (getActiveWidgetIds(context)?.isNotEmpty() == true)) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = getWidgetAlarmIntent(context)

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()

            am.setInexactRepeating(
                AlarmManager.RTC,
                calendar.timeInMillis,
                (UPDATE_INTERVAL_SEC * 1000).toLong(),
                pi,
            )
        }
    }

    @JvmStatic
    fun clearWidgetUpdate(context: Context) {
        if (Wx.DEV) Log.d(LOG_TAG, "clearWidgetUpdate")

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(getWidgetAlarmIntent(context))
    }

    private fun getWidgetAlarmIntent(context: Context): PendingIntent {
        var pendingFlags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) pendingFlags =
            pendingFlags or PendingIntent.FLAG_MUTABLE
        val intent = Intent(context, WxWidget::class.java)
            .setAction(WxWidget.ACTION_AUTO_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, getActiveWidgetIds(context))
        return PendingIntent.getBroadcast(
            context,
            WIDGET_REQUEST_CODE,
            intent,
            pendingFlags,
        )
    }

    private fun getActiveWidgetIds(context: Context): IntArray? {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        return appWidgetManager.getAppWidgetIds(ComponentName(context, WxWidget::class.java))
    }
}
