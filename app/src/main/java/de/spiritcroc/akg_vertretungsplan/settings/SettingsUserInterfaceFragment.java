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

package de.spiritcroc.akg_vertretungsplan.settings;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.widget.TimePicker;
import android.widget.Toast;

import de.spiritcroc.akg_vertretungsplan.R;
import de.spiritcroc.akg_vertretungsplan.Tools;
import de.spiritcroc.akg_vertretungsplan.WebShortcutActivity;

public class SettingsUserInterfaceFragment extends CustomPreferenceFragment {

    private static final String KEY_WEB_PLAN_CUSTOM_STYLE_PRESET =
            "pref_web_plan_custom_style_preset";
    private static final String KEY_WEB_PLAN_ADD_LAUNCHER_SHORTCUT =
            "pref_web_plan_add_launcher_shortcut";

    private EditTextPreference noChangeSinceMaxPrecisionPref;
    private Preference autoSelectDayTimePref;
    private ListPreference themePref;
    private ListPreference webPlanCustomStylePresetPref;
    private EditTextPreference webPlanCustomStylePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_user_interface);

        noChangeSinceMaxPrecisionPref =
                (EditTextPreference) findPreference(Keys.NO_CHANGE_SINCE_MAX_PRECISION);
        autoSelectDayTimePref = findPreference(Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME);
        themePref = (ListPreference) findPreference(Keys.THEME);
        webPlanCustomStylePresetPref =
                (ListPreference) findPreference(KEY_WEB_PLAN_CUSTOM_STYLE_PRESET);
        webPlanCustomStylePref = (EditTextPreference) findPreference(Keys.WEB_PLAN_CUSTOM_STYLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        setNoChangeSinceMaxPrecisionSummary();
        setAutoSelectDayTimeSummary();
        setListPreferenceSummary(themePref);
        setListPreferenceSummary(Keys.DRAWER_ACTIVE_ITEM_TEXT_COLOR);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Keys.NO_CHANGE_SINCE_MAX_PRECISION:
                setNoChangeSinceMaxPrecisionSummary();
                break;
            case Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME:
                setAutoSelectDayTimeSummary();
                break;
            case Keys.THEME:
                setListPreferenceSummary(themePref);
                toggleTheme();
                break;
            case KEY_WEB_PLAN_CUSTOM_STYLE_PRESET:
                applyWebPlanCustomStylePreset();
                break;
            case Keys.DRAWER_ACTIVE_ITEM_TEXT_COLOR:
                setListPreferenceSummary(key);
                break;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         @NonNull Preference preference) {
        String key = preference.getKey();
        if (KEY_WEB_PLAN_ADD_LAUNCHER_SHORTCUT.equals(key)) {
            addWebPlanLauncherShortcut();
        } else if (Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME.equals(key)) {
            editAutoSelectDayTime();
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return false;
    }

    private void setNoChangeSinceMaxPrecisionSummary() {
        int value = correctInteger(Keys.NO_CHANGE_SINCE_MAX_PRECISION,
                noChangeSinceMaxPrecisionPref.getText(), 2);
        String summary = getResources().getQuantityString(
                R.plurals.pref_no_change_since_max_precision_summary, value, value);
        noChangeSinceMaxPrecisionPref.setSummary(summary);
    }

    private void setAutoSelectDayTimeSummary() {
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        int hour = correctInteger(Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME,
                sp.getString(Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME, ""), 17);
        int minute = sp.getInt(Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME_MINUTES, 0);
        String m = minute > 9 ? ("" + minute) : ("0" + minute);
        String summary = getString(R.string.pref_formatted_plan_auto_select_day_time_summary, hour, m);
        autoSelectDayTimePref.setSummary(summary);
    }

    private void editAutoSelectDayTime() {
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        int hour = Integer.parseInt(sp.getString(Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME, "17"));
        int minute = sp.getInt(Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME_MINUTES, 0);
        new TimePickerDialog(getActivity(), autoSelectDayTimeSetListener, hour, minute, true)
                .show();
    }

    private void saveAutoSelectDayTime(int hourOfDay, int minute) {
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        sp.edit().putString(Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME, "" + hourOfDay)
                .putInt(Keys.FORMATTED_PLAN_AUTO_SELECT_DAY_TIME_MINUTES, minute)
                .apply();
        setAutoSelectDayTimeSummary();
    }

    private TimePickerDialog.OnTimeSetListener autoSelectDayTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    saveAutoSelectDayTime(hourOfDay, minute);
                }
            };

    private void toggleTheme() {
        // Change default values to fit to new theme
        applyThemeToCustomColors(getActivity(), APPLY_THEME_ALL, false);

        getActivity().recreate();
    }

    public static int APPLY_THEME_ALL = 0;
    public static int APPLY_THEME_FORMATTED = 1;
    public static int APPLY_THEME_LESSONS = 2;
    /**
     * @param which
     * One of the following:
     * APPLY_THEME_ALL, APPLY_THEME_FORMATTED, APPLY_THEME_LESSONS
     * @param force
     * Whether also values should be overridden that the user has changed
     */
    public static void applyThemeToCustomColors(Context context, int which, boolean force) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int style = Tools.getStyle(context);
        SharedPreferences.Editor editor = sp.edit();

        boolean formatted = which == APPLY_THEME_ALL ||
                (APPLY_THEME_FORMATTED & which) == APPLY_THEME_FORMATTED;
        boolean lessons = which == APPLY_THEME_ALL ||
                (APPLY_THEME_LESSONS & which) == APPLY_THEME_LESSONS;

        if (Tools.isLightStyle(style)) {
            if (formatted) {
                if (force || sp.getString(Keys.HEADER_TEXT_TEXT_COLOR, "").equals(context.getString(R.string.pref_color_white_value)))
                    editor.putString(Keys.HEADER_TEXT_TEXT_COLOR, context.getString(R.string.pref_color_black_value));
                if (force || sp.getString(Keys.CLASS_TEXT_TEXT_COLOR, "").equals(context.getString(R.string.pref_color_cyan_value)))
                    editor.putString(Keys.CLASS_TEXT_TEXT_COLOR, context.getString(R.string.pref_color_blue_value));
                if (force || sp.getString(Keys.NORMAL_TEXT_TEXT_COLOR, "").equals(context.getString(R.string.pref_color_white_value)))
                    editor.putString(Keys.NORMAL_TEXT_TEXT_COLOR, context.getString(R.string.pref_color_black_value));
                if (force || sp.getString(Keys.RELEVANT_TEXT_TEXT_COLOR, "").equals(context.getString(R.string.pref_color_white_value)))
                    editor.putString(Keys.RELEVANT_TEXT_TEXT_COLOR, context.getString(R.string.pref_color_black_value));

                if (force || sp.getString(Keys.HEADER_TEXT_TEXT_COLOR_HL, "").equals(context.getString(R.string.pref_color_green_value)))
                    editor.putString(Keys.HEADER_TEXT_TEXT_COLOR_HL, context.getString(R.string.pref_color_red_value));
                if (force || sp.getString(Keys.NORMAL_TEXT_TEXT_COLOR_HL, "").equals(context.getString(R.string.pref_color_green_value)))
                    editor.putString(Keys.NORMAL_TEXT_TEXT_COLOR_HL, context.getString(R.string.pref_color_red_value));
                if (force || sp.getString(Keys.RELEVANT_TEXT_TEXT_COLOR_HL, "").equals(context.getString(R.string.pref_color_green_value)))
                    editor.putString(Keys.RELEVANT_TEXT_TEXT_COLOR_HL, context.getString(R.string.pref_color_red_value));

                if (force || sp.getString(Keys.RELEVANT_TEXT_BG_COLOR, "").equals(context.getString(R.string.pref_color_blue_value)))
                    editor.putString(Keys.RELEVANT_TEXT_BG_COLOR, context.getString(R.string.pref_color_yellow_value));
                if (force || sp.getString(Keys.RELEVANT_TEXT_BG_COLOR_HL, "").equals(context.getString(R.string.pref_color_blue_value)))
                    editor.putString(Keys.RELEVANT_TEXT_BG_COLOR_HL, context.getString(R.string.pref_color_yellow_value));
            }
            if (lessons) {
                if (force || sp.getString(Keys.LESSON_PLAN_COLOR_TIME, "").equals(context.getString(R.string.pref_color_cyan_value)))
                    editor.putString(Keys.LESSON_PLAN_COLOR_TIME, context.getString(R.string.pref_color_blue_value));
                if (force || sp.getString(Keys.LESSON_PLAN_COLOR_LESSON, "").equals(context.getString(R.string.pref_color_white_value)))
                    editor.putString(Keys.LESSON_PLAN_COLOR_LESSON, context.getString(R.string.pref_color_black_value));
                if (force || sp.getString(Keys.LESSON_PLAN_COLOR_ROOM, "").equals(context.getString(R.string.pref_color_ltgray_value)))
                    editor.putString(Keys.LESSON_PLAN_COLOR_ROOM, context.getString(R.string.pref_color_dkgray_value));
                if (force || sp.getString(Keys.LESSON_PLAN_COLOR_RELEVANT_INFORMATION, "").equals(context.getString(R.string.pref_color_ltgray_value)))
                    editor.putString(Keys.LESSON_PLAN_COLOR_RELEVANT_INFORMATION, context.getString(R.string.pref_color_dkgray_value));
                if (force || sp.getString(Keys.LESSON_PLAN_COLOR_GENERAL_INFORMATION, "").equals(context.getString(R.string.pref_color_ltgray_value)))
                    editor.putString(Keys.LESSON_PLAN_COLOR_GENERAL_INFORMATION, context.getString(R.string.pref_color_dkgray_value));

                if (force || sp.getString(Keys.LESSON_PLAN_BG_COLOR_CURRENT_LESSON, "").equals(context.getString(R.string.pref_color_dkgray_value)))
                    editor.putString(Keys.LESSON_PLAN_BG_COLOR_CURRENT_LESSON, context.getString(R.string.pref_color_ltgray_value));
            }
        } else if (Tools.isDarkStyle(style)) {
            if (formatted) {
                if (force || sp.getString(Keys.HEADER_TEXT_TEXT_COLOR, "").equals(context.getString(R.string.pref_color_black_value)))
                    editor.putString(Keys.HEADER_TEXT_TEXT_COLOR, context.getString(R.string.pref_color_white_value));
                if (force || sp.getString(Keys.CLASS_TEXT_TEXT_COLOR, "").equals(context.getString(R.string.pref_color_blue_value)))
                    editor.putString(Keys.CLASS_TEXT_TEXT_COLOR, context.getString(R.string.pref_color_cyan_value));
                if (force || sp.getString(Keys.NORMAL_TEXT_TEXT_COLOR, "").equals(context.getString(R.string.pref_color_black_value)))
                    editor.putString(Keys.NORMAL_TEXT_TEXT_COLOR, context.getString(R.string.pref_color_white_value));
                if (force || sp.getString(Keys.RELEVANT_TEXT_TEXT_COLOR, "").equals(context.getString(R.string.pref_color_black_value)))
                    editor.putString(Keys.RELEVANT_TEXT_TEXT_COLOR, context.getString(R.string.pref_color_white_value));

                if (force || sp.getString(Keys.HEADER_TEXT_TEXT_COLOR_HL, "").equals(context.getString(R.string.pref_color_red_value)))
                    editor.putString(Keys.HEADER_TEXT_TEXT_COLOR_HL, context.getString(R.string.pref_color_green_value));
                if (force || sp.getString(Keys.NORMAL_TEXT_TEXT_COLOR_HL, "").equals(context.getString(R.string.pref_color_red_value)))
                    editor.putString(Keys.NORMAL_TEXT_TEXT_COLOR_HL, context.getString(R.string.pref_color_green_value));
                if (force || sp.getString(Keys.RELEVANT_TEXT_TEXT_COLOR_HL, "").equals(context.getString(R.string.pref_color_red_value)))
                    editor.putString(Keys.RELEVANT_TEXT_TEXT_COLOR_HL, context.getString(R.string.pref_color_green_value));

                if (force || sp.getString(Keys.RELEVANT_TEXT_BG_COLOR, "").equals(context.getString(R.string.pref_color_yellow_value)))
                    editor.putString(Keys.RELEVANT_TEXT_BG_COLOR, context.getString(R.string.pref_color_blue_value));
                if (force || sp.getString(Keys.RELEVANT_TEXT_BG_COLOR_HL, "").equals(context.getString(R.string.pref_color_yellow_value)))
                    editor.putString(Keys.RELEVANT_TEXT_BG_COLOR_HL, context.getString(R.string.pref_color_blue_value));
            }
            if (lessons) {
                if (force || sp.getString(Keys.LESSON_PLAN_COLOR_TIME, "").equals(context.getString(R.string.pref_color_blue_value)))
                    editor.putString(Keys.LESSON_PLAN_COLOR_TIME, context.getString(R.string.pref_color_cyan_value));
                if (force || sp.getString(Keys.LESSON_PLAN_COLOR_LESSON, "").equals(context.getString(R.string.pref_color_black_value)))
                    editor.putString(Keys.LESSON_PLAN_COLOR_LESSON, context.getString(R.string.pref_color_white_value));
                if (force || sp.getString(Keys.LESSON_PLAN_COLOR_ROOM, "").equals(context.getString(R.string.pref_color_dkgray_value)))
                    editor.putString(Keys.LESSON_PLAN_COLOR_ROOM, context.getString(R.string.pref_color_ltgray_value));
                if (force || sp.getString(Keys.LESSON_PLAN_COLOR_RELEVANT_INFORMATION, "").equals(context.getString(R.string.pref_color_dkgray_value)))
                    editor.putString(Keys.LESSON_PLAN_COLOR_RELEVANT_INFORMATION, context.getString(R.string.pref_color_ltgray_value));
                if (force || sp.getString(Keys.LESSON_PLAN_COLOR_GENERAL_INFORMATION, "").equals(context.getString(R.string.pref_color_dkgray_value)))
                    editor.putString(Keys.LESSON_PLAN_COLOR_GENERAL_INFORMATION, context.getString(R.string.pref_color_ltgray_value));

                if (force || sp.getString(Keys.LESSON_PLAN_BG_COLOR_CURRENT_LESSON, "").equals(context.getString(R.string.pref_color_ltgray_value)))
                    editor.putString(Keys.LESSON_PLAN_BG_COLOR_CURRENT_LESSON, context.getString(R.string.pref_color_dkgray_value));
            }
        }

        editor.apply();
    }

    private void applyWebPlanCustomStylePreset() {
        String ignore = "ignore";
        String value = webPlanCustomStylePresetPref.getValue();
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        if (!ignore.equals(value)) {
            /**
             * Save ignore-string in this pref in order to not save the full CSS twice, but don't
             * run this code again because sharedPreference are changed while removing this pref
             */
            sp.edit().putString(Keys.WEB_PLAN_CUSTOM_STYLE, value)
                    .putString(KEY_WEB_PLAN_CUSTOM_STYLE_PRESET, ignore)
                    .apply();
            webPlanCustomStylePref.setText(value);
        }
    }

    private void addWebPlanLauncherShortcut() {
        Intent intent = WebShortcutActivity.getShortcut(getActivity());
        getActivity().sendBroadcast(intent);
        Toast.makeText(getActivity(), R.string.launcher_added_successfully, Toast.LENGTH_SHORT)
                .show();
    }
}
