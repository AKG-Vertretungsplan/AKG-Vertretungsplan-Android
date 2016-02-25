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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.widget.Toast;

import de.spiritcroc.akg_vertretungsplan.LessonPlanShortcutActivity;
import de.spiritcroc.akg_vertretungsplan.R;

public class SettingsLessonPlanFragment extends CustomPreferenceFragment {

    private static final String KEY_ADD_LAUNCHER_SHORTCUT =
            "pref_lesson_plan_add_launcher_shortcut";

    private EditTextPreference autoSelectDayTimePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_lesson_plan);

        autoSelectDayTimePref =
                (EditTextPreference) findPreference(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        setAutoSelectDayTimeSummary();
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_TIME);
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_LESSON);
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_FREE_TIME);
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_ROOM);
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_RELEVANT_INFORMATION);
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_GENERAL_INFORMATION);
        setListPreferenceSummary(Keys.LESSON_PLAN_BG_COLOR_CURRENT_LESSON);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME:
                setAutoSelectDayTimeSummary();
                break;
            case Keys.LESSON_PLAN_COLOR_TIME:
            case Keys.LESSON_PLAN_COLOR_LESSON:
            case Keys.LESSON_PLAN_COLOR_FREE_TIME:
            case Keys.LESSON_PLAN_COLOR_ROOM:
            case Keys.LESSON_PLAN_COLOR_RELEVANT_INFORMATION:
            case Keys.LESSON_PLAN_COLOR_GENERAL_INFORMATION:
            case Keys.LESSON_PLAN_BG_COLOR_CURRENT_LESSON:
                setListPreferenceSummary(key);
                break;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         @NonNull Preference preference) {
        String key = preference.getKey();
        if (KEY_ADD_LAUNCHER_SHORTCUT.equals(key)) {
            addLauncherShortcut();
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return false;
    }

    private void setAutoSelectDayTimeSummary() {
        int value = correctInteger(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME,
                autoSelectDayTimePref.getText(), 17);
        String summary = getString(R.string.pref_lesson_plan_auto_select_day_time_summary, value);
        autoSelectDayTimePref.setSummary(summary);
    }

    private void addLauncherShortcut() {
        Intent intent = LessonPlanShortcutActivity.getShortcut(getActivity());
        getActivity().sendBroadcast(intent);
        Toast.makeText(getActivity(), R.string.launcher_added_successfully, Toast.LENGTH_SHORT)
                .show();
    }

}
