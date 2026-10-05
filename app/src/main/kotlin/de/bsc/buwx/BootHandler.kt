package de.bsc.buwx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootHandler : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (Wx.DEV) Log.d(LOG_TAG, "onReceive")
        try {
            if ((intent.action != null) && (context != null)) {
                if (intent.action?.equals(Intent.ACTION_BOOT_COMPLETED, ignoreCase = true) == true)
                    WidgetNotification.scheduleWidgetUpdate(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val LOG_TAG = "BootHandler"
    }
}