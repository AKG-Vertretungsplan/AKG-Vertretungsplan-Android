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

    private CheckBoxPreference notificationEnabled;
    private CheckBoxPreference notificationRelevantOnly;
    private CheckBoxPreference notificationGeneralIrrelevant;
    private Preference notificationTextColorGeneral;
    private Preference notificationTextStyleGeneral;
    private Preference notificationTextColorIrrelevant;
    private Preference notificationTextStyleIrrelevant;

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

        notificationEnabled = (CheckBoxPreference) findPreference("pref_notification_enabled");
        notificationRelevantOnly = (CheckBoxPreference) findPreference("pref_notification_only_if_relevant");
        notificationGeneralIrrelevant = (CheckBoxPreference) findPreference("pref_notification_general_not_relevant");
        notificationTextColorGeneral = findPreference("pref_notification_preview_general_color");
        notificationTextStyleGeneral = findPreference("pref_notification_preview_general_style");
        notificationTextColorIrrelevant = findPreference("pref_notification_preview_irrelevant_color");
        notificationTextStyleIrrelevant = findPreference("pref_notification_preview_irrelevant_style");
        setNotificationDependencies();

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
        setSummaryToValue("pref_widget_text_color_highlight_relevant");
        setSummaryToValue("pref_widget_text_color_highlight_general");
        setSummaryToValue("pref_auto_mark_read");
        setSummaryToValue("pref_led_notification_color");
        setSummaryToValue("pref_relevant_text_text_color");
        setSummaryToValue("pref_relevant_text_background_color");
        setSummaryToValue("pref_relevant_text_text_color_highlight");
        setSummaryToValue("pref_relevant_text_background_color_highlight");
        setSummaryToValue("pref_action_bar_normal_background_color");
        setSummaryToValue("pref_action_bar_filtered_background_color");
        setSummaryToValue("pref_theme");
        setSummaryToValue("pref_lesson_plan_color_time");
        setSummaryToValue("pref_lesson_plan_color_lesson");
        setSummaryToValue("pref_lesson_plan_color_free_time");
        setSummaryToValue("pref_lesson_plan_color_room");
        //setSummaryToValue("pref_plan");
        setSummaryToValue("pref_notification_preview_relevant_color");
        setSummaryToValue("pref_notification_preview_relevant_style");
        setSummaryToValue("pref_notification_preview_general_color");
        setSummaryToValue("pref_notification_preview_general_style");
        setSummaryToValue("pref_notification_preview_irrelevant_color");
        setSummaryToValue("pref_notification_preview_irrelevant_style");
        setSummaryToValue("pref_notification_button_mark_seen");

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
        if (key.equals("pref_class_text_text_color") || key.equals("pref_class_text_background_color") || key.equals("pref_normal_text_text_color") || key.equals("pref_normal_text_background_color") || key.equals("pref_normal_text_text_color_highlight") || key.equals("pref_normal_text__background_color_highlight") || key.equals("pref_header_text_text_color") || key.equals("pref_header_text_background_color") || key.equals("pref_header_text_text_color_highlight") || key.equals("pref_header_text__background_color_highlight" ) || key.equals("pref_widget_text_color")  || key.equals("pref_widget_text_color_highlight") || key.equals("pref_widget_text_color_highlight_relevant") || key.equals("pref_widget_text_color_highlight_general") || key.equals("pref_auto_mark_read") || key.equals("pref_led_notification_color") || key.equals("pref_relevant_text_text_color") || key.equals("pref_relevant_text_background_color") || key.equals("pref_relevant_text_text_color_highlight") || key.equals("pref_relevant_text_background_color_highlight") || key.equals("pref_action_bar_normal_background_color") || key.equals("pref_action_bar_filtered_background_color") ||
                key.equals("pref_lesson_plan_color_time") || key.equals("pref_lesson_plan_color_lesson") || key.equals("pref_lesson_plan_color_free_time") || key.equals("pref_lesson_plan_color_room") || key.equals("pref_notification_preview_relevant_color") || key.equals("pref_notification_preview_relevant_style") || key.equals("pref_notification_preview_general_color") || key.equals("pref_notification_preview_general_style") || key.equals("pref_notification_preview_irrelevant_color") || key.equals("pref_notification_preview_irrelevant_style") || key.equals("pref_notification_button_mark_seen"))
            setSummaryToValue(key);
        /*else if (key.equals("pref_plan")) {
            setSummaryToValue(key);
            setUserdataVisibility();
            if (getSharedPreferences().getString("pref_plan", "1").equals("2") && !getSharedPreferences().getBoolean("seen_infoscreen_warning", false)) {
                new InfoscreenWarningDialog().show(getFragmentManager(), "InfoscreenWarningDialog");
            }
        }*/
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
            if (Tools.isLightStyle(Tools.getStyle(theme))) {
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

                if (sharedPreferences.getString("pref_lesson_plan_color_time", "").equals(getString(R.string.pref_color_cyan_value)))
                    editor.putString("pref_lesson_plan_color_time", getString(R.string.pref_color_blue_value));
                if (sharedPreferences.getString("pref_lesson_plan_color_lesson", "").equals(getString(R.string.pref_color_white_value)))
                    editor.putString("pref_lesson_plan_color_lesson", getString(R.string.pref_color_black_value));
                if (sharedPreferences.getString("pref_lesson_plan_color_room", "").equals(getString(R.string.pref_color_ltgray_value)))
                    editor.putString("pref_lesson_plan_color_room", getString(R.string.pref_color_dkgray_value));
            } else if (Tools.isDarkStyle(Tools.getStyle(theme))) {
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

                if (sharedPreferences.getString("pref_lesson_plan_color_time", "").equals(getString(R.string.pref_color_blue_value)))
                    editor.putString("pref_lesson_plan_color_time", getString(R.string.pref_color_cyan_value));
                if (sharedPreferences.getString("pref_lesson_plan_color_lesson", "").equals(getString(R.string.pref_color_black_value)))
                    editor.putString("pref_lesson_plan_color_lesson", getString(R.string.pref_color_white_value));
                if (sharedPreferences.getString("pref_lesson_plan_color_room", "").equals(getString(R.string.pref_color_dkgray_value)))
                    editor.putString("pref_lesson_plan_color_room", getString(R.string.pref_color_ltgray_value));
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
        } else if (key.equals("pref_notification_enabled") || key.equals("pref_notification_only_if_relevant") || key.equals("pref_notification_general_not_relevant")) {
            setNotificationDependencies();
        } else if (key.equals("pref_web_plan_custom_style_preset")) {
            String ignore = "ignore";
            String value = ((ListPreference) findPreference(key)).getValue();
            if (!ignore.equals(value)) {
                sharedPreferences.edit()
                        .putString("pref_web_plan_custom_style", value)
                        .putString(key, ignore)// Don't save the full CSS in this pref, but don't run this code again because of sharedPref changed when removing pref
                        .apply();
            }
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
        // Always visible
        /*if (getSharedPreferences().getString("pref_plan", "1").equals("1")) {
            basePrefScreen.addPreference(userdataPrefScreen);
        } else {
            basePrefScreen.removePreference(userdataPrefScreen);
        }*/
    }
    private void setNotificationDependencies() {
        boolean notificationEnabled = this.notificationEnabled.isChecked();
        boolean notificationRelevantOnly = this.notificationRelevantOnly.isChecked();
        boolean notificationGeneralIrrelevant = this.notificationGeneralIrrelevant.isChecked();
        if (notificationEnabled && (!notificationRelevantOnly||!notificationGeneralIrrelevant)) {
            notificationTextColorGeneral.setEnabled(true);
            notificationTextStyleGeneral.setEnabled(true);
        } else {
            notificationTextColorGeneral.setEnabled(false);
            notificationTextStyleGeneral.setEnabled(false);
        }
        if (notificationEnabled && !notificationRelevantOnly) {
            notificationTextColorIrrelevant.setEnabled(true);
            notificationTextStyleIrrelevant.setEnabled(true);
        } else {
            notificationTextColorIrrelevant.setEnabled(false);
            notificationTextStyleIrrelevant.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, @NonNull Preference preference){
        if (preference == hiddenDebug) {
            System.arraycopy(hits, 1, hits, 0, hits.length-1);
            hits[hits.length-1] = SystemClock.uptimeMillis();
            if (!getSharedPreferences().getBoolean("pref_hidden_debug_enabled", false) && hits[0] >= (SystemClock.uptimeMillis()-1300)){
                getSharedPreferences().edit().putBoolean("pref_hidden_debug_enabled", true).apply();
                Toast.makeText(getActivity(), R.string.toast_enabled_hidden_debug, Toast.LENGTH_SHORT).show();
                basePrefScreen.removePreference(hiddenDebug);
                basePrefScreen.addPreference(hiddenDebugPrefScreen);
                ((CheckBoxPreference)findPreference("pref_hidden_debug_enabled")).setChecked(true);
            }
        } else if ("pref_lesson_plan_add_launcher_shortcut".equals(preference.getKey())){
            Intent intent = ShortcutActivity.getLessonPlanLauncherShortcut(getActivity().getApplicationContext());
            getActivity().sendBroadcast(intent);
            Toast.makeText(getActivity(), R.string.launcher_added_successfully, Toast.LENGTH_SHORT).show();
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return false;
    }
}
