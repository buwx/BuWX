/*
 * ----------------------------------------------------------------------------
 *
 * Copyright 2015 Michael Buchfink (buchfink@web.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ----------------------------------------------------------------------------
 *
 * 30.09.2015 - Creation date
 *
 * ----------------------------------------------------------------------------
 */
package de.bsc.buwx;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

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
}
