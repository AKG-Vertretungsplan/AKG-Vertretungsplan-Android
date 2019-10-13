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

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import androidx.annotation.NonNull;
import android.util.Log;

public abstract class CustomPreferenceFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected static final String LOG_TAG = CustomPreferenceFragment.class.getSimpleName();

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         @NonNull Preference preference) {
        Activity activity = getActivity();
        if (activity instanceof SettingsActivity) {
            return ((SettingsActivity) activity).onPreferenceClick(preference) ||
                    super.onPreferenceTreeClick(preferenceScreen, preference);
        } else {
            Log.w(LOG_TAG, "activity not instanceof SettingsActivity");
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Default implementation; override in subclasses
    }

    // Util methods

    protected int correctInteger(String key, String value, int defaultValue){
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
            editor.putString(key, String.valueOf(defaultValue));
            editor.apply();
            return defaultValue;
        }
    }

    protected void setListPreferenceSummary(String key) {
        ListPreference preference = (ListPreference) findPreference(key);
        setListPreferenceSummary(preference);
    }

    protected void setListPreferenceSummary(ListPreference preference) {
        String value = preference.getEntry().toString();
        preference.setSummary(value);
    }
}
