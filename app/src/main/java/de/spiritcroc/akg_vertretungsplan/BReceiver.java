/*
 * Copyright (C) 2015-2016 SpiritCroc
 * Email: spiritcroc@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.spiritcroc.akg_vertretungsplan;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import java.util.Calendar;

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

public class BReceiver extends BroadcastReceiver {
    public static final String ACTION_START_DOWNLOAD_SERVICE = "action_start_download_service";
    public static final String ACTION_MARK_SEEN = "de.spiritcroc.akg_vertretungsplan.action.markSeen";
    public static final String ACTION_UPDATE_WIDGETS = "de.spiritcroc.akg_vertretungsplan.action.UPDATE_WIDGETS";
    @Override
    public void onReceive(Context context, Intent intent){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        OwnLog.add(context, "BReceiver.onReceive: action: " + intent.getAction());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (sharedPreferences.getBoolean(Keys.BACKGROUND_SERVICE, true)) {
                startDownloadService(context, true);
            }
            setWidgetUpdateAlarm(context);
        }
        else if (ACTION_START_DOWNLOAD_SERVICE.equals(intent.getAction()) ||
                ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()) && sharedPreferences.getBoolean(Keys.LAST_OFFLINE, false)){
            if (!DownloadService.isDownloading() && !IsRunningSingleton.getInstance().isRunning())
                context.startService(new Intent(context, DownloadService.class).setAction(DownloadService.ACTION_DOWNLOAD_PLAN));
            else
                startDownloadService(context.getApplicationContext(), false);//Schedule next download nevertheless
        } else if (ACTION_MARK_SEEN.equals(intent.getAction())) {
            sharedPreferences.edit().putBoolean(Keys.UNSEEN_CHANGES, false).apply();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(1);
            Tools.updateWidgets(context);
        } else if (ACTION_UPDATE_WIDGETS.equals(intent.getAction())) {
            Tools.updateWidgets(context);
            setWidgetUpdateAlarm(context);
        }
    }

    public static PendingIntent getAlarmPendingIntent(Context context, int flag){
        Intent resultIntent = new Intent(context, BReceiver.class).setAction(ACTION_START_DOWNLOAD_SERVICE);
        return PendingIntent.getBroadcast(context.getApplicationContext(), 0, resultIntent, flag);
    }
    public static void startDownloadService(Context context, boolean now){
        int period = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString(Keys.BACKGROUND_UPDATE_INTERVAL, "60"))*60000;
        OwnLog.add(context, "BReceiver.startDownloadService: " + (now ? "now" : "period: " + period));
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setRepeating(AlarmManager.ELAPSED_REALTIME, now ? 0 : SystemClock.elapsedRealtime() + period, period, getAlarmPendingIntent(context, PendingIntent.FLAG_CANCEL_CURRENT));
    }
    public static void stopDownloadService(Context context){
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(getAlarmPendingIntent(context, PendingIntent.FLAG_CANCEL_CURRENT));
    }
    public static void setWidgetUpdateAlarm(Context context) {
        // Set alarm to next midnight to update widgets
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_MONTH, 1);
        Intent resultIntent = new Intent(context, BReceiver.class).setAction(ACTION_UPDATE_WIDGETS);
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, c.getTimeInMillis(), PendingIntent.getBroadcast(context.getApplicationContext(), 0, resultIntent, 0));
    }
}
