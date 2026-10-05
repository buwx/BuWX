package de.bsc.buwx

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules the periodic broadcast that refreshes the widgets while at least one exists.
 */
object WidgetUpdateScheduler {
    private const val LOG_TAG = "WidgetUpdateScheduler"
    private const val REQUEST_CODE = 685355
    private const val INTERVAL_MS = 60_000L

    fun schedule(context: Context) {
        if (WxWidget.activeWidgetIds(context).isEmpty()) return
        logDebug(LOG_TAG) { "schedule" }
        context.alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis(),
            INTERVAL_MS,
            alarmIntent(context),
        )
    }

    fun cancel(context: Context) {
        logDebug(LOG_TAG) { "cancel" }
        context.alarmManager.cancel(alarmIntent(context))
    }

    private fun alarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, WxWidget::class.java).setAction(WxWidget.ACTION_AUTO_UPDATE)
        // The mutability flag is part of the PendingIntent identity. It stays FLAG_MUTABLE as in
        // earlier versions, otherwise an alarm scheduled by them could no longer be replaced.
        var flags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags = flags or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private val Context.alarmManager: AlarmManager
        get() = getSystemService(AlarmManager::class.java)
}
