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

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;

import de.spiritcroc.akg_vertretungsplan.R;

public class SettingsNotificationFragment extends CustomPreferenceFragment {

    private static final String KEY_NOTIFICATION_HEADS_UP_CATEGORY =
            "pref_category_which_has_heads_up_in_it";

    private CheckBoxPreference notificationEnabledPref;
    private PreferenceCategory notificationHeadsUpPrefCategory;
    private ListPreference notificationHeadsUpPref;
    private CheckBoxPreference notificationRelevantOnlyPref;
    private CheckBoxPreference notificationGeneralIrrelevantPref;
    private ListPreference notificationTextColorGeneralPref;
    private ListPreference notificationTextStyleGeneralPref;
    private ListPreference notificationTextColorIrrelevantPref;
    private ListPreference notificationTextStyleIrrelevantPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_notification);

        notificationEnabledPref = (CheckBoxPreference) findPreference(Keys.NOTIFICATION_ENABLED);
        notificationHeadsUpPrefCategory =
                (PreferenceCategory) findPreference(KEY_NOTIFICATION_HEADS_UP_CATEGORY);
        notificationHeadsUpPref = (ListPreference) findPreference(Keys.NOTIFICATION_HEADS_UP);
        notificationRelevantOnlyPref =
                (CheckBoxPreference) findPreference(Keys.NOTIFICATION_ONLY_IF_RELEVANT);
        notificationGeneralIrrelevantPref =
                (CheckBoxPreference) findPreference(Keys.NOTIFICATION_GENERAL_NOT_RELEVANT);
        notificationTextColorGeneralPref =
                (ListPreference) findPreference(Keys.NOTIFICATION_PREVIEW_GENERAL_COLOR);
        notificationTextStyleGeneralPref =
                (ListPreference) findPreference(Keys.NOTIFICATION_PREVIEW_GENERAL_STYLE);
        notificationTextColorIrrelevantPref =
                (ListPreference) findPreference(Keys.NOTIFICATION_PREVIEW_IRRELEVANT_COLOR);
        notificationTextStyleIrrelevantPref =
                (ListPreference) findPreference(Keys.NOTIFICATION_PREVIEW_IRRELEVANT_STYLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        if (Build.VERSION.SDK_INT < 21) {
            notificationHeadsUpPrefCategory.removePreference(notificationHeadsUpPref);
        }

        setNotificationDependencies();
        setListPreferenceSummary(Keys.LED_NOTIFICATION_COLOR);
        setListPreferenceSummary(notificationHeadsUpPref);
        setListPreferenceSummary(Keys.NOTIFICATION_PREVIEW_RELEVANT_COLOR);
        setListPreferenceSummary(Keys.NOTIFICATION_PREVIEW_RELEVANT_STYLE);
        setListPreferenceSummary(notificationTextColorGeneralPref);
        setListPreferenceSummary(notificationTextStyleGeneralPref);
        setListPreferenceSummary(notificationTextColorIrrelevantPref);
        setListPreferenceSummary(notificationTextStyleIrrelevantPref);
        setListPreferenceSummary(Keys.NOTIFICATION_BUTTON_MARK_SEEN);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Keys.NOTIFICATION_ENABLED:
            case Keys.NOTIFICATION_ONLY_IF_RELEVANT:
            case Keys.NOTIFICATION_GENERAL_NOT_RELEVANT:
                setNotificationDependencies();
                break;
            case Keys.LED_NOTIFICATION_COLOR:
            case Keys.NOTIFICATION_PREVIEW_RELEVANT_COLOR:
            case Keys.NOTIFICATION_PREVIEW_RELEVANT_STYLE:
            case Keys.NOTIFICATION_HEADS_UP:
            case Keys.NOTIFICATION_PREVIEW_GENERAL_COLOR:
            case Keys.NOTIFICATION_PREVIEW_GENERAL_STYLE:
            case Keys.NOTIFICATION_PREVIEW_IRRELEVANT_COLOR:
            case Keys.NOTIFICATION_PREVIEW_IRRELEVANT_STYLE:
            case Keys.NOTIFICATION_BUTTON_MARK_SEEN:
                setListPreferenceSummary(key);
                break;
        }
    }

    private void setNotificationDependencies() {
        boolean notificationEnabled = notificationEnabledPref.isChecked();
        boolean notificationRelevantOnly = notificationRelevantOnlyPref.isChecked();
        boolean notificationGeneralIrrelevant = notificationGeneralIrrelevantPref.isChecked();
        if (notificationEnabled && (!notificationRelevantOnly||!notificationGeneralIrrelevant)) {
            notificationTextColorGeneralPref.setEnabled(true);
            notificationTextStyleGeneralPref.setEnabled(true);
        } else {
            notificationTextColorGeneralPref.setEnabled(false);
            notificationTextStyleGeneralPref.setEnabled(false);
        }
        if (notificationEnabled && !notificationRelevantOnly) {
            notificationTextColorIrrelevantPref.setEnabled(true);
            notificationTextStyleIrrelevantPref.setEnabled(true);
        } else {
            notificationTextColorIrrelevantPref.setEnabled(false);
            notificationTextStyleIrrelevantPref.setEnabled(false);
        }
    }
}
