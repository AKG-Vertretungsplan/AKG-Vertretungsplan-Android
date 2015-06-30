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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

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
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);

        if (intent.getAction().equals(WIDGET_BUTTON_CLICKED)){        //open App
            context.startActivity(new Intent(context, FormattedActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
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
            views.setTextViewText(R.id.appwidget_button, context.getString(R.string.new_version) + " " + sharedPreferences.getString("pref_last_update", context.getString(R.string.error_could_not_load)));
            views.setTextColor(R.id.appwidget_button, Integer.parseInt(sharedPreferences.getString("pref_widget_text_color_highlight", "" + Color.RED)));
        }
        else{
            views.setTextViewText(R.id.appwidget_button, context.getString(R.string.last_checked) + " " + sharedPreferences.getString("pref_last_checked", context.getString(R.string.error_could_not_load)));
            views.setTextColor(R.id.appwidget_button, Integer.parseInt(sharedPreferences.getString("pref_widget_text_color", "" + Color.WHITE)));
        }
    }
}
