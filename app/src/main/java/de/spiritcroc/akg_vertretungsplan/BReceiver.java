/*
 * Copyright (C) 2015 SpiritCroc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.spiritcroc.akg_vertretungsplan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class BReceiver extends BroadcastReceiver {
    public static final String ACTION_START_DOWNLOAD_SERVICE = "action_start_download_service";
    @Override
    public void onReceive(Context context, Intent intent){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && sharedPreferences.getBoolean("pref_background_service", false)){
            startDownloadService(context);
        }
        else if (ACTION_START_DOWNLOAD_SERVICE.equals(intent.getAction()) ||
                ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()) && sharedPreferences.getBoolean("pref_last_offline", false)){
            if (!DownloadService.isDownloading() && !IsRunningSingleton.getInstance().isRunning())
                context.startService(new Intent(context, DownloadService.class).setAction(DownloadService.ACTION_DOWNLOAD_PLAN));
        }
    }

    public static PendingIntent getAlarmPendingIntent(Context context, int flag){
        Intent resultIntent = new Intent(context, BReceiver.class).setAction(ACTION_START_DOWNLOAD_SERVICE);
        return PendingIntent.getBroadcast(context.getApplicationContext(), 0, resultIntent, flag);
    }
    public static void startDownloadService(Context context){
        int period = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("pref_background_update_interval", "60"))*60000;
        Log.d("startDownloadService", "period: " + period);
        ((AlarmManager) context.getSystemService(context.ALARM_SERVICE)).setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), period, getAlarmPendingIntent(context, PendingIntent.FLAG_CANCEL_CURRENT));
    }
    public static void stopDownloadService(Context context){
        ((AlarmManager) context.getSystemService(context.ALARM_SERVICE)).cancel(getAlarmPendingIntent(context, PendingIntent.FLAG_CANCEL_CURRENT));
    }
}
