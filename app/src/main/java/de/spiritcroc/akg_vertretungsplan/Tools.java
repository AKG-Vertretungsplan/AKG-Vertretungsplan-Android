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
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

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
            return R.style.Theme_AppCompat_Light_DarkActionBar;
        else if (style.equals("dark"))
            return R.style.Theme_AppCompat;
        else
            return R.style.AppTheme;    //default
    }
    public static Calendar getDateFromPlanTitle(String title){
        int day = 0, month = 0, year = 0, position = 0, progress = 0;
        boolean lastWasInt = false;
        while (position < title.length()){
            char c = title.charAt(position++);
            if (c >= '0' && c <= '9'){
                if (!lastWasInt) {
                    progress++;
                    lastWasInt = true;
                }
                switch (progress){
                    case 1:
                        day = day*10+Integer.parseInt(""+c);
                        break;
                    case 3:
                        month = month*10+Integer.parseInt(""+c);
                        break;
                    case 5:
                        year = year*10+Integer.parseInt(""+c);
                        break;
                }
            }
            else if (lastWasInt){
                progress++;
                lastWasInt = false;
            }
        }
        if (day == 0 || month == 0 || year == 0){
            Log.e("Tools", "getDateFromPlanTitle: Unable to get date from title " + title);
            return null;
        }
        else{
            return new GregorianCalendar(year, month+Calendar.JANUARY-1, day);
        }

    }
    static void findViewsWithText(List<View> outViews, ViewGroup parent, String targetDescription) {//http://stackoverflow.com/questions/22046903/changing-the-android-overflow-menu-icon-programmatically/22106474#22106474
        if (parent == null || TextUtils.isEmpty(targetDescription)) {
            return;
        }
        final int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = parent.getChildAt(i);
            final CharSequence description = child.getContentDescription();
            if (!TextUtils.isEmpty(description) && targetDescription.equals(description.toString()))
                outViews.add(child);
            else if (child instanceof ViewGroup && child.getVisibility() == View.VISIBLE)
                findViewsWithText(outViews, (ViewGroup) child, targetDescription);
        }
    }

    //VPlan helper methods, former methods of ItemFragment
    public static int countHeaderCells(String row){
        int count = 0;
        for (int i=0; i<row.length(); i++){
            if (row.charAt(i)=='¿')
                count++;
        }
        return count;
    }
    public static String getCellContent(String row, int number){
        int found = 0;
        String result = "";
        for (int i = 0; i<row.length(); i++){
            if (row.charAt(i)=='¡' || row.charAt(i)=='¿')
                found++;
            else if (found == number)
                result += row.charAt(i);
            else if (found > number)
                return result;
        }
        return result;
    }
    //VPlan helper methods end

    public static boolean ignoreSubstitution(String substitution){
        String ignore = "Version";
        if (substitution.contains(ignore)){//Check if no other information given
            int ignorePos = 0;
            boolean ignoreSpecialChar = false;//required for "&nbsp;"
            for (int  substitutionPos = 0; substitutionPos < substitution.length(); substitutionPos++){
                char c = substitution.charAt(substitutionPos);
                if (c == '&')
                    ignoreSpecialChar = true;
                if (ignoreSpecialChar){
                    if (c == ';')
                        ignoreSpecialChar = false;
                }
                else {
                    if (ignorePos < ignore.length() && c == ignore.charAt(ignorePos))
                        ignorePos++;
                    else if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z')//There is also other text
                        return false;
                }
            }
            return true;//No other information found: ignore this
        }
        else
            return false;
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

    public static boolean isWebActivityEnabled(SharedPreferences sharedPreferences) {
        return sharedPreferences.getInt("last_plan_type", 1) == 1;
    }

    public static StyleSpan getStyleSpanFromPref(Context context, String stylePrefValue) {
        if (context.getString(R.string.pref_text_style_bold_value).equals(stylePrefValue)) {
            return new StyleSpan(Typeface.BOLD);
        } else if (context.getString(R.string.pref_text_style_italic_value).equals(stylePrefValue)) {
            return new StyleSpan(Typeface.ITALIC);
        } else if (context.getString(R.string.pref_text_style_bold_italic_value).equals(stylePrefValue)) {
            return new StyleSpan(Typeface.BOLD_ITALIC);
        } else {
            return null;
        }
    }
}
