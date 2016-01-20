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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Calendar;

public class CheckPlanWidget extends AppWidgetProvider {
    private static final String WIDGET_BUTTON_CLICKED = "widget_button_clicked";
    private final String WIDGET_RELOAD_BUTTON_CLICKED = "widget_reload_button_clicked";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
        onUpdate(context, appWidgetManager, appWidgetIds, R.layout.check_plan_widget);
    }

    protected void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, int layout) {  //added param layout so CheckPlanWidget can easily be extended and changed layout only
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            RemoteViews views = new RemoteViews(context.getPackageName(), layout);
            int appWidgetId = appWidgetIds[i];

            Intent intent = new Intent(context, getClass()).setAction(WIDGET_BUTTON_CLICKED);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.appwidget_button, pendingIntent);

            Intent reloadIntent = new Intent(context, getClass()).setAction(WIDGET_RELOAD_BUTTON_CLICKED);
            PendingIntent pendingReloadIntent = PendingIntent.getBroadcast(context, 0, reloadIntent, 0);
            views.setOnClickPendingIntent(R.id.appwidget_reload_button, pendingReloadIntent);

            updateViews(views, context);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent){
        super.onReceive(context, intent);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (intent.getAction().equals(WIDGET_BUTTON_CLICKED)){        //open App
            switch (Integer.parseInt(sharedPreferences.getString("pref_widget_opens_activity", "0"))) {
                case 1:
                    context.startActivity(new Intent(context, WebActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    break;
                case 2:
                    context.startActivity(new Intent(context, LessonPlanActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    break;
                default:
                case 0:
                    context.startActivity(new Intent(context, FormattedActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    break;
            }
        }
        else if (intent.getAction().equals(WIDGET_RELOAD_BUTTON_CLICKED)){
            if (!DownloadService.isDownloading())
                context.startService(new Intent(context, DownloadService.class).setAction(DownloadService.ACTION_DOWNLOAD_PLAN));
        }
    }
    static void updateViews(RemoteViews views, Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.getString("pref_text_view_text", "").equals(context.getString(R.string.loading))){
            views.setTextViewText(R.id.appwidget_button, context.getString(R.string.loading));
            //views.setTextColor(R.id.appwidget_button, Integer.parseInt(sharedPreferences.getString("pref_widget_text_color", "" + Color.WHITE))); //keep loading red if new version
        }
        else if (sharedPreferences.getBoolean("pref_illegal_plan", false)){
            views.setTextViewText(R.id.appwidget_button, context.getString(R.string.error_illegal_plan));
        }
        else if (sharedPreferences.getBoolean("pref_unseen_changes", false)){
            ArrayList<String> relevantInformation = new ArrayList<>(),
                    generalInformation = new ArrayList<>(),
                    irrelevantInformation = new ArrayList<>();
            int newRelevantNotificationCount = DownloadService.getNewRelevantInformationCount(context, relevantInformation, generalInformation, irrelevantInformation);
            String text;
            int color;
            boolean amendUpdateTime;
            if (newRelevantNotificationCount > 0) {
                text = context.getResources().getQuantityString(R.plurals.new_relevant_information, newRelevantNotificationCount, newRelevantNotificationCount);
                color = Integer.parseInt(sharedPreferences.getString("pref_widget_text_color_highlight_relevant", "" + Color.RED));
                amendUpdateTime = sharedPreferences.getBoolean("pref_widget_last_update_relevant", false);
            } else if (generalInformation.size() > 0) {
                text = context.getResources().getQuantityString(R.plurals.new_general_information, generalInformation.size(), generalInformation.size());
                color = Integer.parseInt(sharedPreferences.getString("pref_widget_text_color_highlight_general", "" + Color.RED));
                amendUpdateTime = sharedPreferences.getBoolean("pref_widget_last_update_general", false);
            } else if (irrelevantInformation.size() > 0) {
                text = context.getResources().getQuantityString(R.plurals.new_irrelevant_information, irrelevantInformation.size(), irrelevantInformation.size());
                color = Integer.parseInt(sharedPreferences.getString("pref_widget_text_color_highlight", "" + Color.RED));
                amendUpdateTime = sharedPreferences.getBoolean("pref_widget_last_update_irrelevant", false);
            } else {
                text = context.getString(R.string.new_version);
                color = Integer.parseInt(sharedPreferences.getString("pref_widget_text_color", "" + Color.WHITE));
                amendUpdateTime = sharedPreferences.getBoolean("pref_widget_last_update_none", true);
            }
            if (amendUpdateTime) {
                text += " (" + getLastUpdateTime(context, sharedPreferences) + ")";
            }
            views.setTextViewText(R.id.appwidget_button, text);
            views.setTextColor(R.id.appwidget_button, color);
        }
        else{
            if (sharedPreferences.getBoolean("pref_widget_last_update_none", true)) {
                views.setTextViewText(R.id.appwidget_button, context.getString(R.string.last_checked) + " " + getLastUpdateTime(context, sharedPreferences));
            } else {
                views.setTextViewText(R.id.appwidget_button, context.getString(R.string.no_change));
            }
            views.setTextColor(R.id.appwidget_button, Integer.parseInt(sharedPreferences.getString("pref_widget_text_color", "" + Color.WHITE)));
        }
    }

    private static String getLastUpdateTime(Context context, SharedPreferences sharedPreferences) {
        String time = sharedPreferences.getString("pref_last_checked", context.getString(R.string.error_could_not_load));
        if (!sharedPreferences.getBoolean("pref_widget_last_update_show_seconds", false)) {
            int separatorIndex = time.lastIndexOf(":");
            if (separatorIndex > 0) {
                time = time.substring(0, separatorIndex);
            }
        }
        // Try to find out whether last update was today or this year
        String currentTime = DownloadService.timeAndDateToString(Calendar.getInstance());
        int i1 = time.indexOf(" ");
        int i2 = currentTime.indexOf(" ");
        if (i1 > 0 && i2 > 0) {
            if (time.substring(0, i1).equals(currentTime.substring(0, i2))) {
                // Don't show date
                time = time.substring(i1+1);
            } else {
                int i3 = time.indexOf(".");
                int i4 = currentTime.indexOf(".");
                if (i3 > 0 && i4 > 0) {
                    int i5 = time.indexOf(".", i3+1);
                    int i6 = currentTime.indexOf(".", i4+1);
                    if (i5 > 0 && i6 > 0 && i5 < i1 && i6 < i2 &&
                            time.substring(i5, i1).equals(currentTime.substring(i6, i2))) {
                        // Don't show year
                        time = time.substring(0, i5+1) + time.substring(i1);
                    }
                }
            }
        }
        return time;
    }
}
