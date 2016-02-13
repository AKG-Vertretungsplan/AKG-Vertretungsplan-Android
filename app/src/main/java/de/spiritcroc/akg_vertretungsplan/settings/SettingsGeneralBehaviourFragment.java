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
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;

import de.spiritcroc.akg_vertretungsplan.BReceiver;
import de.spiritcroc.akg_vertretungsplan.R;

public class SettingsGeneralBehaviourFragment extends CustomPreferenceFragment {

    private static final int BACKGROUND_INTERVAL_MINIMUM = 15;

    private EditTextPreference autoLoadOnOpenPref;
    private CheckBoxPreference backgroundServicePref;
    private EditTextPreference backgroundUpdateIntervalPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_general_behaviour);

        autoLoadOnOpenPref = (EditTextPreference) findPreference(Keys.AUTO_LOAD_ON_OPEN);
        backgroundServicePref = (CheckBoxPreference) findPreference(Keys.BACKGROUND_SERVICE);
        backgroundUpdateIntervalPref =
                (EditTextPreference) findPreference(Keys.BACKGROUND_UPDATE_INTERVAL);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        setListPreferenceSummary(Keys.AUTO_MARK_READ);
        setAutoLoadOnOpenPref();
        setBackgroundUpdateIntervalSummary();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Keys.AUTO_MARK_READ:
                setListPreferenceSummary(key);
                break;
            case Keys.AUTO_LOAD_ON_OPEN:
                setAutoLoadOnOpenPref();
                break;
            case Keys.BACKGROUND_SERVICE:
                toggleBackgroundService();
                break;
            case Keys.BACKGROUND_UPDATE_INTERVAL:
                setBackgroundUpdateIntervalSummary();
                updateBackgroundUpdateInterval();
                break;
        }
    }

    private void setAutoLoadOnOpenPref() {
        int value = correctInteger(Keys.AUTO_LOAD_ON_OPEN, autoLoadOnOpenPref.getText(), 5);
        String summary = getResources().getQuantityString(R.plurals.pref_auto_load_on_open_summary,
                value, value);
        autoLoadOnOpenPref.setSummary(summary);
    }

    private void toggleBackgroundService() {
        if (backgroundServicePref.isChecked()) {
            BReceiver.startDownloadService(getActivity(), false);
        } else {
            BReceiver.stopDownloadService(getActivity());
        }
    }

    private void setBackgroundUpdateIntervalSummary() {
        int value = correctInteger(Keys.BACKGROUND_UPDATE_INTERVAL,
                backgroundUpdateIntervalPref.getText(), 60);
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        if (value < BACKGROUND_INTERVAL_MINIMUM &&
                (!sp.getBoolean(Keys.HIDDEN_DEBUG_ENABLED, false) ||
                        !sp.getBoolean(Keys.DEBUG_ALLOW_LOW_UPDATE_INTERVALS, false))) {
            value = BACKGROUND_INTERVAL_MINIMUM;
            sp.edit().putString(Keys.BACKGROUND_UPDATE_INTERVAL, String.valueOf(value)).apply();
        }
        String summary = getResources().getQuantityString(R.plurals.plural_minute,
                value, value);
        backgroundUpdateIntervalPref.setSummary(summary);
    }

    private void updateBackgroundUpdateInterval() {
        BReceiver.stopDownloadService(getActivity());
        BReceiver.startDownloadService(getActivity(), false);
    }
}
