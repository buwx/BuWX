package de.bsc.buwx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UpdateHandler extends BroadcastReceiver {
    private static final String LOG_TAG = "UpdateHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Wx.DEV) Log.d(LOG_TAG, "onReceive");
        try {
            if (intent != null && intent.getAction() != null && context != null) {
                if (intent.getAction().equalsIgnoreCase(Intent.ACTION_MY_PACKAGE_REPLACED) || (
                        Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction()) &&
                                intent.getData().getSchemeSpecificPart().equals(context.getPackageName()))
                )
                    WidgetNotification.scheduleWidgetUpdate(context);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}