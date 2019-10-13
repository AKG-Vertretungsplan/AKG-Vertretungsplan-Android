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
import androidx.annotation.NonNull;
import android.widget.RemoteViews;

import java.util.ArrayList;

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

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
            switch (Integer.parseInt(sharedPreferences.getString(Keys.WIDGET_OPENS_ACTIVITY, "0"))) {
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
                DownloadService.enqueueWork(context, new Intent(context, DownloadService.class).setAction(DownloadService.ACTION_DOWNLOAD_PLAN));
        }
    }
    static void updateViews(RemoteViews views, Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.getString(Keys.TEXT_VIEW_TEXT, "").equals(context.getString(R.string.loading))){
            views.setTextViewText(R.id.appwidget_button, context.getString(R.string.loading));
        }
        else if (sharedPreferences.getBoolean(Keys.ILLEGAL_PLAN, false)){
            views.setTextViewText(R.id.appwidget_button, context.getString(R.string.error_illegal_plan));
        }
        else if (sharedPreferences.getBoolean(Keys.UNSEEN_CHANGES, false)){
            ArrayList<String> relevantInformation = new ArrayList<>(),
                    generalInformation = new ArrayList<>(),
                    irrelevantInformation = new ArrayList<>();
            int newRelevantNotificationCount = DownloadService.getNewRelevantInformationCount(context, relevantInformation, generalInformation, irrelevantInformation);
            String text;
            int color;
            boolean amendUpdateTime;
            if (newRelevantNotificationCount > 0) {
                text = context.getResources().getQuantityString(R.plurals.new_relevant_information, newRelevantNotificationCount, newRelevantNotificationCount);
                color = Integer.parseInt(sharedPreferences.getString(Keys.WIDGET_TEXT_COLOR_HL_RELEVANT, "" + Color.RED));
                amendUpdateTime = sharedPreferences.getBoolean(Keys.WIDGET_LAST_UPDATE_RELEVANT, false);
            } else if (generalInformation.size() > 0) {
                text = context.getResources().getQuantityString(R.plurals.new_general_information, generalInformation.size(), generalInformation.size());
                color = Integer.parseInt(sharedPreferences.getString(Keys.WIDGET_TEXT_COLOR_HL_GENERAL, "" + Color.RED));
                amendUpdateTime = sharedPreferences.getBoolean(Keys.WIDGET_LAST_UPDATE_GENERAL, false);
            } else if (irrelevantInformation.size() > 0) {
                text = context.getResources().getQuantityString(R.plurals.new_irrelevant_information, irrelevantInformation.size(), irrelevantInformation.size());
                color = Integer.parseInt(sharedPreferences.getString(Keys.WIDGET_TEXT_COLOR_HL, "" + Color.RED));
                amendUpdateTime = sharedPreferences.getBoolean(Keys.WIDGET_LAST_UPDATE_IRRELEVANT, false);
            } else {
                text = context.getString(R.string.new_version);
                color = Integer.parseInt(sharedPreferences.getString(Keys.WIDGET_TEXT_COLOR, "" + Color.WHITE));
                amendUpdateTime = sharedPreferences.getBoolean(Keys.WIDGET_LAST_UPDATE_NONE, true);
            }
            if (amendUpdateTime) {
                text += " (" + getLastUpdateTime(context, sharedPreferences) + ")";
            }
            views.setTextViewText(R.id.appwidget_button, text);
            views.setTextColor(R.id.appwidget_button, color);
        }
        else{
            if (sharedPreferences.getBoolean(Keys.WIDGET_LAST_UPDATE_NONE, true)) {
                views.setTextViewText(R.id.appwidget_button, context.getString(R.string.last_checked) + " " + getLastUpdateTime(context, sharedPreferences));
            } else {
                views.setTextViewText(R.id.appwidget_button, context.getString(R.string.no_change));
            }
            views.setTextColor(R.id.appwidget_button, Integer.parseInt(sharedPreferences.getString(Keys.WIDGET_TEXT_COLOR, "" + Color.WHITE)));
        }
    }

    private static String getLastUpdateTime(Context context, SharedPreferences sharedPreferences) {
        String time = sharedPreferences.getString(Keys.LAST_CHECKED, context.getString(R.string.error_could_not_load));
        if (!sharedPreferences.getBoolean(Keys.WIDGET_LAST_UPDATE_SHOW_SECONDS, false)) {
            int separatorIndex = time.lastIndexOf(":");
            if (separatorIndex > 0) {
                time = time.substring(0, separatorIndex);
            }
        }
        return Tools.shortestTime(time);
    }
}
