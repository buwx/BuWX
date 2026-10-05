package de.bsc.buwx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UpdateHandler : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (Wx.DEV) Log.d(LOG_TAG, "onReceive")
        try {
            if ((intent != null) && (intent.action != null) && (context != null)) {
                if ((intent.action?.equals(
                        Intent.ACTION_MY_PACKAGE_REPLACED,
                        ignoreCase = true,
                    ) == true) || (Intent.ACTION_PACKAGE_REPLACED == intent.action &&
                            intent.data?.schemeSpecificPart == context.packageName)
                ) WidgetNotification.scheduleWidgetUpdate(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val LOG_TAG = "UpdateHandler"
    }
}