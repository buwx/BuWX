package de.bsc.buwx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts the periodic widget updates after a reboot or an app update,
 * because the alarm does not survive either.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        logDebug(LOG_TAG) { "onReceive ${intent.action}" }
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> WidgetUpdateScheduler.schedule(context)
        }
    }

    companion object {
        private const val LOG_TAG = "RescheduleReceiver"
    }
}
