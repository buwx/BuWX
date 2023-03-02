package de.bsc.buwx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootHandler extends BroadcastReceiver {
    private static final String LOG_TAG = "BootHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Wx.DEV) Log.d(LOG_TAG, "onReceive");
        try {
            if (intent.getAction() != null && context != null) {
                if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED))
                    WidgetNotification.scheduleWidgetUpdate(context);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}