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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{
    private PreferenceCategory userdataPrefScreen;
    private Preference hiddenDebug;
    private PreferenceScreen basePrefScreen;
    private PreferenceCategory hiddenDebugPrefScreen;
    private long[] hits = new long[7];
    private SharedPreferences sharedPreferences;

    private SharedPreferences getSharedPreferences(){
        if (sharedPreferences == null)
            sharedPreferences = getPreferenceManager().getSharedPreferences();
        return sharedPreferences;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Resources resources = getResources();

        basePrefScreen = (PreferenceScreen) findPreference("pref_screen_base");
        userdataPrefScreen = (PreferenceCategory) findPreference("pref_userdata");
        hiddenDebug = findPreference("hidden_debug");
        hiddenDebugPrefScreen = (PreferenceCategory) findPreference("pref_screen_hidden_debug");

        setSummaryToValue("pref_class_text_text_color");
        setSummaryToValue("pref_class_text_background_color");
        setSummaryToValue("pref_normal_text_text_color");
        setSummaryToValue("pref_normal_text_background_color");
        setSummaryToValue("pref_normal_text_text_color_highlight");
        setSummaryToValue("pref_normal_text__background_color_highlight");
        setSummaryToValue("pref_header_text_text_color");
        setSummaryToValue("pref_header_text_background_color");
        setSummaryToValue("pref_header_text_text_color_highlight");
        setSummaryToValue("pref_header_text__background_color_highlight");
        setSummaryToValue("pref_widget_text_color");
        setSummaryToValue("pref_widget_text_color_highlight");
        setSummaryToValue("pref_auto_mark_read");
        setSummaryToValue("pref_led_notification_color");
        setSummaryToValue("pref_relevant_text_text_color");
        setSummaryToValue("pref_relevant_text_background_color");
        setSummaryToValue("pref_relevant_text_text_color_highlight");
        setSummaryToValue("pref_relevant_text_background_color_highlight");
        setSummaryToValue("pref_action_bar_normal_background_color");
        setSummaryToValue("pref_action_bar_filtered_background_color");
        setSummaryToValue("pref_theme");
        setSummaryToValue("pref_plan");

        EditTextPreference tmpEditTextPreference = (EditTextPreference) findPreference("pref_auto_load_on_open");
        int tmpValue = correctInteger(getSharedPreferences(), "pref_auto_load_on_open", tmpEditTextPreference.getText(), 5);
        tmpEditTextPreference.setSummary(resources.getQuantityString(R.plurals.pref_auto_load_on_open_summary, tmpValue, tmpValue));

        tmpEditTextPreference = (EditTextPreference) findPreference("pref_background_update_interval");
        tmpValue = Integer.parseInt(tmpEditTextPreference.getText());
        tmpEditTextPreference.setSummary(resources.getQuantityString(R.plurals.plural_minute, tmpValue, tmpValue));

        tmpEditTextPreference = (EditTextPreference) findPreference("pref_no_change_since_max_precision");
        tmpValue = correctInteger(getSharedPreferences(), "pref_no_change_since_max_precision", tmpEditTextPreference.getText(), 2);
        tmpEditTextPreference.setSummary(resources.getQuantityString(R.plurals.pref_no_change_since_max_precision_summary, tmpValue, tmpValue));

        tmpEditTextPreference = (EditTextPreference) findPreference("pref_formatted_plan_auto_select_day_time");
        tmpValue = correctInteger(getSharedPreferences(), "pref_formatted_plan_auto_select_day_time", tmpEditTextPreference.getText(), 17);
        tmpEditTextPreference.setSummary(getString(R.string.pref_formatted_plan_auto_select_day_time_summary_pre) + tmpValue + getString(R.string.pref_formatted_plan_auto_select_day_time_summary_post));

        tmpEditTextPreference = (EditTextPreference) findPreference("pref_lesson_plan_auto_select_day_time");
        tmpValue = correctInteger(getSharedPreferences(), "pref_lesson_plan_auto_select_day_time", tmpEditTextPreference.getText(), 17);
        tmpEditTextPreference.setSummary(getString(R.string.pref_lesson_plan_auto_select_day_time_summary_pre) + tmpValue + getString(R.string.pref_lesson_plan_auto_select_day_time_summary_post));

        setUserdataVisibility();

        //hidden debug: (inspiration from AICP's hidden shit
        boolean hiddenDebugEnabled = getSharedPreferences().getBoolean("pref_hidden_debug_enabled", false);
        if (hiddenDebugEnabled)
            basePrefScreen.removePreference(hiddenDebug);
        else
            basePrefScreen.removePreference(hiddenDebugPrefScreen);

        PackageManager packageManager = getActivity().getPackageManager();
        try{
            packageManager.getPackageInfo("com.teslacoilsw.notifier", PackageManager.GET_ACTIVITIES);
        }
        catch (PackageManager.NameNotFoundException e){
            Log.v("SettingsFragment", "TeslaUnread not installed");
            PreferenceScreen teslaUnreadPrefScreen = (PreferenceScreen) findPreference("pref_tesla_unread");
            PreferenceCategory parent = (PreferenceCategory) findPreference("pref_individualisation");
            parent.removePreference(teslaUnreadPrefScreen);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    @Override
    public void onPause(){
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key){
        if (key.equals("pref_class_text_text_color") || key.equals("pref_class_text_background_color") || key.equals("pref_normal_text_text_color") || key.equals("pref_normal_text_background_color") || key.equals("pref_normal_text_text_color_highlight") || key.equals("pref_normal_text__background_color_highlight") || key.equals("pref_header_text_text_color") || key.equals("pref_header_text_background_color") || key.equals("pref_header_text_text_color_highlight") || key.equals("pref_header_text__background_color_highlight" ) || key.equals("pref_widget_text_color")  || key.equals("pref_widget_text_color_highlight") || key.equals("pref_auto_mark_read") || key.equals("pref_led_notification_color") || key.equals("pref_relevant_text_text_color") || key.equals("pref_relevant_text_background_color") || key.equals("pref_relevant_text_text_color_highlight") || key.equals("pref_relevant_text_background_color_highlight") || key.equals("pref_action_bar_normal_background_color") || key.equals("pref_action_bar_filtered_background_color"))
            setSummaryToValue(key);
        else if (key.equals("pref_plan")) {
            setSummaryToValue(key);
            setUserdataVisibility();
        }
        else if (key.equals("pref_username") || key.equals("pref_password")){
            getActivity().startService(new Intent(getActivity(), DownloadService.class).setAction(DownloadService.ACTION_RETRY));
        }
        else if (key.equals("pref_auto_load_on_open")){
            EditTextPreference tmpEditTextPreference = (EditTextPreference) findPreference("pref_auto_load_on_open");
            int tmpValue = correctInteger(sharedPreferences, "pref_auto_load_on_open", tmpEditTextPreference.getText(), 5);
            tmpEditTextPreference.setSummary(getResources().getQuantityString(R.plurals.pref_auto_load_on_open_summary, tmpValue, tmpValue));
        }
        else if (key.equals("pref_theme")){
            ListPreference listPreference = (ListPreference) findPreference("pref_theme");
            String theme = listPreference.getValue();
            setSummaryToValue(key);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            //adapt custom colors to new theme:
            if (theme.equals(getString(R.string.pref_theme_light_value))){
                if (sharedPreferences.getString("pref_header_text_text_color", "").equals(getString(R.string.pref_color_white_value)))
                    editor.putString("pref_header_text_text_color", getString(R.string.pref_color_black_value));
                if (sharedPreferences.getString("pref_class_text_text_color", "").equals(getString(R.string.pref_color_cyan_value)))
                    editor.putString("pref_class_text_text_color", getString(R.string.pref_color_blue_value));
                if (sharedPreferences.getString("pref_normal_text_text_color", "").equals(getString(R.string.pref_color_white_value)))
                    editor.putString("pref_normal_text_text_color", getString(R.string.pref_color_black_value));
                if (sharedPreferences.getString("pref_relevant_text_text_color", "").equals(getString(R.string.pref_color_white_value)))
                    editor.putString("pref_relevant_text_text_color", getString(R.string.pref_color_black_value));

                if (sharedPreferences.getString("pref_relevant_text_background_color", "").equals(getString(R.string.pref_color_blue_value)))
                    editor.putString("pref_relevant_text_background_color", getString(R.string.pref_color_yellow_value));
                if (sharedPreferences.getString("pref_relevant_text_background_color_highlight", "").equals(getString(R.string.pref_color_blue_value)))
                    editor.putString("pref_relevant_text_background_color_highlight", getString(R.string.pref_color_yellow_value));
            }
            else if (theme.equals(getString(R.string.pref_theme_dark_value))){
                if (sharedPreferences.getString("pref_header_text_text_color", "").equals(getString(R.string.pref_color_black_value)))
                    editor.putString("pref_header_text_text_color", getString(R.string.pref_color_white_value));
                if (sharedPreferences.getString("pref_class_text_text_color", "").equals(getString(R.string.pref_color_blue_value)))
                    editor.putString("pref_class_text_text_color", getString(R.string.pref_color_cyan_value));
                if (sharedPreferences.getString("pref_normal_text_text_color", "").equals(getString(R.string.pref_color_black_value)))
                    editor.putString("pref_normal_text_text_color", getString(R.string.pref_color_white_value));
                if (sharedPreferences.getString("pref_relevant_text_text_color", "").equals(getString(R.string.pref_color_black_value)))
                    editor.putString("pref_relevant_text_text_color", getString(R.string.pref_color_white_value));


                if (sharedPreferences.getString("pref_relevant_text_background_color", "").equals(getString(R.string.pref_color_yellow_value)))
                    editor.putString("pref_relevant_text_background_color", getString(R.string.pref_color_blue_value));
                if (sharedPreferences.getString("pref_relevant_text_background_color_highlight", "").equals(getString(R.string.pref_color_yellow_value)))
                    editor.putString("pref_relevant_text_background_color_highlight", getString(R.string.pref_color_blue_value));
            }
            editor.apply();
        }
        else if (key.equals("pref_background_service")){
            CheckBoxPreference tmpCheckBoxPreference = (CheckBoxPreference) findPreference("pref_background_service");
            if (tmpCheckBoxPreference.isChecked())
                BReceiver.startDownloadService(getActivity(), false);
            else
                BReceiver.stopDownloadService(getActivity());
        }
        else if (key.equals("pref_background_update_interval")){
            EditTextPreference tmpEditTextPreference = (EditTextPreference) findPreference("pref_background_update_interval");
            int tmpValue = correctInteger(sharedPreferences, "pref_background_update_interval", tmpEditTextPreference.getText(), 60);
            if (tmpValue < 15 && (!getSharedPreferences().getBoolean("pref_hidden_debug_enabled", false) || !getSharedPreferences().getBoolean("pref_allow_low_update_intervals", false))){
                tmpValue = 15;
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("pref_background_update_interval", String.valueOf(tmpValue));
                editor.apply();
            }
            tmpEditTextPreference.setSummary(getResources().getQuantityString(R.plurals.plural_minute, tmpValue, tmpValue));

            BReceiver.stopDownloadService(getActivity());
            BReceiver.startDownloadService(getActivity(), false);
        }
        else if (key.equals("pref_no_change_since_max_precision")){
            EditTextPreference tmpEditTextPreference = (EditTextPreference) findPreference("pref_no_change_since_max_precision");
            int tmpValue = correctInteger(sharedPreferences, "pref_no_change_since_max_precision", tmpEditTextPreference.getText(), 2);
            tmpEditTextPreference.setSummary(getResources().getQuantityString(R.plurals.pref_no_change_since_max_precision_summary, tmpValue, tmpValue));
        }
        else if (key.equals("pref_formatted_plan_auto_select_day_time")){
            EditTextPreference tmpEditTextPreference = (EditTextPreference) findPreference("pref_formatted_plan_auto_select_day_time");
            int tmpValue = correctInteger(getSharedPreferences(), "pref_formatted_plan_auto_select_day_time", tmpEditTextPreference.getText(), 17);
            tmpEditTextPreference.setSummary(getString(R.string.pref_formatted_plan_auto_select_day_time_summary_pre) + tmpValue + getString(R.string.pref_formatted_plan_auto_select_day_time_summary_post));
        }
        else if (key.equals("pref_lesson_plan_auto_select_day_time")){
            EditTextPreference tmpEditTextPreference = (EditTextPreference) findPreference("pref_lesson_plan_auto_select_day_time");
            int tmpValue = correctInteger(getSharedPreferences(), "pref_lesson_plan_auto_select_day_time", tmpEditTextPreference.getText(), 17);
            tmpEditTextPreference.setSummary(getString(R.string.pref_lesson_plan_auto_select_day_time_summary_pre) + tmpValue + getString(R.string.pref_lesson_plan_auto_select_day_time_summary_post));
        }
    }

    private void setSummaryToValue (String ListPreferenceKey){
        ListPreference preference = (ListPreference) findPreference(ListPreferenceKey);
        preference.setSummary(preference.getEntry());
    }

    private int correctInteger(SharedPreferences sharedPreferences, String key, String value, int defaultValue){
        try {
            return Integer.parseInt(value);
        }
        catch (Exception e){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, String.valueOf(defaultValue));
            editor.apply();
            return defaultValue;
        }
    }

    private void setUserdataVisibility(){
        if (getSharedPreferences().getString("pref_plan", "1").equals("1")) {
            basePrefScreen.addPreference(userdataPrefScreen);
        } else {
            basePrefScreen.removePreference(userdataPrefScreen);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, @NonNull Preference preference){
        if (preference == hiddenDebug){
            System.arraycopy(hits, 1, hits, 0, hits.length-1);
            hits[hits.length-1] = SystemClock.uptimeMillis();
            if (!getSharedPreferences().getBoolean("pref_hidden_debug_enabled", false) && hits[0] >= (SystemClock.uptimeMillis()-1300)){
                getSharedPreferences().edit().putBoolean("pref_hidden_debug_enabled", true).apply();
                Toast.makeText(getActivity(), R.string.toast_enabled_hidden_debug, Toast.LENGTH_SHORT).show();
                basePrefScreen.removePreference(hiddenDebug);
                basePrefScreen.addPreference(hiddenDebugPrefScreen);
                ((CheckBoxPreference)findPreference("pref_hidden_debug_enabled")).setChecked(true);
            }
        }
        else
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        return false;
    }
}
