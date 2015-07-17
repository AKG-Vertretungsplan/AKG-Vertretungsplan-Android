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

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;

public abstract class Tools {
    public static String getLine (String text, int number){
        return getLine (text, number, '\n');
    }
    public static String getLine (String text, int number, char separator){
        if (text == null){
            //Log.i("getLine", "text == null; return empty string");
            return "";
        }
        else if (text.length()==0){
            //Log.i("getLine", "text.length()==0; return empty string");
            return "";
        }
        int found = 1;
        int index = -1;
        if (number <= 0) {
            Log.e("getLine", "wrong usage: number <= 0");
            return "";
        }
        while (index<text.length() && found<number){
            index = text.indexOf(separator, ++index);
            if (index == -1)
                return "";  //not found
            else
                found++;
        }
        index++;
        if (index >= text.length())
            return "";
        else{
            int end = text.indexOf(separator, index);

            if (end < index)
                return text.substring(index);
            else if (end == index)
                return "";
            else
                return text.substring(index, end);
        }
    }
    public static boolean lineAvailable (String text, String line){
        String checkLine = "a";
        for (int i = 0; !checkLine.equals(""); i++){
            checkLine = Tools.getLine(text, i+1);
            if (checkLine.equals(line))
                return true;
        }
        return false;
    }
    public static boolean stringAvailable(ArrayList<String> arrayList, String search){//Alternative to arrayList.contains() which checks strings with .equals() instead of "=="
        for (int i = 0; i < arrayList.size(); i++)
            if (arrayList.get(i).equals(search))
                return true;
        return false;
    }
    public static void setUnseenFalse(Context context){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean("pref_unseen_changes", false);
        editor.apply();
        updateWidgets(context);
    }
    public static void updateWidgets(Context context){
        Intent intent = new Intent(context.getApplicationContext(), CheckPlanWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context.getApplicationContext()).getAppWidgetIds(new ComponentName(context.getApplicationContext(), CheckPlanWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.getApplicationContext().sendBroadcast(intent);

        intent = new Intent(context.getApplicationContext(), CheckPlanWidgetVertical.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        ids = AppWidgetManager.getInstance(context.getApplicationContext()).getAppWidgetIds(new ComponentName(context.getApplicationContext(), CheckPlanWidgetVertical.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.getApplicationContext().sendBroadcast(intent);
    }
    public static int getStyle(Context context){
        String style = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_theme", "");
        if (style.equals("light"))
            return R.style.Theme_AppCompat_Light;
        else if (style.equals("lightDarkActionBar"))
            return  R.style.Theme_AppCompat_Light_DarkActionBar;
        else if (style.equals("dark"))
            return R.style.Theme_AppCompat;
        else
            return R.style.AppTheme;    //default
    }
    /*public static void saveStringToFile (String string, String filePath){
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filePath));
            bufferedWriter.write(string);
            bufferedWriter.close();
        }
        catch (Exception e){
            Log.e("saveStringToFile", "Got Exception " + e);
        }
    }
    public static String readStringFromFile (String filePath){
        String tmp, string="";
        try{
            BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
            while ((tmp = bufferedReader.readLine()) != null)
                string += tmp + "\n";
            bufferedReader.close();
        }
        catch (Exception e){
            Log.e("readStringFromFile", "Got Exception " + e);
        }
        return string.substring(0,string.length()-1);
    }*/
}
