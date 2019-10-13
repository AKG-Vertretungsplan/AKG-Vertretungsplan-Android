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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import androidx.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import de.spiritcroc.akg_vertretungsplan.CustomAddressDialog;
import de.spiritcroc.akg_vertretungsplan.DismissListenableListPreference;
import de.spiritcroc.akg_vertretungsplan.DownloadService;
import de.spiritcroc.akg_vertretungsplan.OwnLog;
import de.spiritcroc.akg_vertretungsplan.R;

public class SettingsFragment extends CustomPreferenceFragment {

    private static final int HIDDEN_DEBUG_HITS = 7;
    private static final int HIDDEN_DEBUG_MILLISECONDS = 3000;

    private static final String KEY_INDIVIDUALISATION = "pref_individualisation";
    private static final String KEY_TESLA_UNREAD = "pref_tesla_unread";
    private static final String KEY_HIDDEN_DEBUG = "hidden_debug";
    private static final String KEY_HIDDEN_DEBUG_SCREEN = "pref_screen_hidden_debug";
    private static final String KEY_CLEAR_OWN_LOG = "pref_clear_own_log";

    private PreferenceCategory individualisationPrefCategory;
    private PreferenceScreen teslaUnreadPrefScreen;
    private Preference hiddenDebugPref;
    private PreferenceCategory hiddenDebugPrefCategory;
    private CheckBoxPreference enableHiddenDebugPref;
    private CheckBoxPreference ownLogPref;

    private long[] hiddenPrefHits = new long[HIDDEN_DEBUG_HITS];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        individualisationPrefCategory = (PreferenceCategory) findPreference(KEY_INDIVIDUALISATION);
        teslaUnreadPrefScreen = (PreferenceScreen) findPreference(KEY_TESLA_UNREAD);
        hiddenDebugPref = findPreference(KEY_HIDDEN_DEBUG);
        hiddenDebugPrefCategory = (PreferenceCategory) findPreference(KEY_HIDDEN_DEBUG_SCREEN);
        enableHiddenDebugPref = (CheckBoxPreference) findPreference(Keys.HIDDEN_DEBUG_ENABLED);
        ownLogPref = (CheckBoxPreference) findPreference(Keys.OWN_LOG);

        ((DismissListenableListPreference)findPreference(Keys.PLAN)).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                if ("3".equals(sharedPreferences.getString(Keys.PLAN, getResources().getString(R.string.default_plan_selection)))) {
                    CustomAddressDialog customAddressDialog = new CustomAddressDialog();
                    customAddressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            updatePlanSummary();
                        }
                    });
                    customAddressDialog.show(getFragmentManager(), "CustomAddressDialog");
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        setTeslaUnreadPrefHidden();
        setHiddenDebugHidden();
        updatePlanSummary();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Keys.USERNAME:
            case Keys.PASSWORD:
                onCredentialsUpdated();
                break;
            case Keys.OWN_LOG:
                toggleOwnLog();
                break;
            case Keys.PLAN:
                updatePlanSummary();
                break;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         @NonNull Preference preference) {
        if (preference == hiddenDebugPref) {
            // Inspiration from AOSP easteregg
            System.arraycopy(hiddenPrefHits, 1, hiddenPrefHits, 0, hiddenPrefHits.length-1);
            hiddenPrefHits[hiddenPrefHits.length-1] = SystemClock.uptimeMillis();
            SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
            if (hiddenPrefHits[0] >= (SystemClock.uptimeMillis()-HIDDEN_DEBUG_MILLISECONDS)){
                sharedPreferences.edit().putBoolean(Keys.HIDDEN_DEBUG_ENABLED, true).apply();
                Toast.makeText(getActivity(), R.string.toast_enabled_hidden_debug,
                        Toast.LENGTH_SHORT).show();
                getPreferenceScreen().removePreference(hiddenDebugPref);
                getPreferenceScreen().addPreference(hiddenDebugPrefCategory);
                enableHiddenDebugPref.setChecked(true);
            }
        } else if (KEY_CLEAR_OWN_LOG.equals(preference.getKey())) {
            OwnLog.clear(getPreferenceManager().getSharedPreferences());
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return false;
    }

    private void setTeslaUnreadPrefHidden() {
        PackageManager packageManager = getActivity().getPackageManager();
        try {
            packageManager
                    .getPackageInfo("com.teslacoilsw.notifier", PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.v(LOG_TAG, "TeslaUnread not installed");
            individualisationPrefCategory.removePreference(teslaUnreadPrefScreen);
        }
    }

    private void setHiddenDebugHidden() {
        boolean enabled = enableHiddenDebugPref.isChecked();
        getPreferenceScreen().removePreference(enabled ? hiddenDebugPref : hiddenDebugPrefCategory);
    }

    private void toggleOwnLog() {
        boolean enabled = ownLogPref.isChecked();
        OwnLog.forceAdd(getPreferenceManager().getSharedPreferences(),
                enabled ? "Log enabled" : "Log disabled");
    }

    private void onCredentialsUpdated() {
        Activity activity = getActivity();
        DownloadService.enqueueWork(activity, new Intent(activity, DownloadService.class)
                .setAction(DownloadService.ACTION_RETRY));
    }

    private void updatePlanSummary() {
        ListPreference planPreference = (ListPreference) findPreference(Keys.PLAN);
        if ("3".equals(planPreference.getValue())) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            planPreference.setSummary(sharedPreferences.getString(Keys.CUSTOM_ADDRESS, getResources().getString(R.string.default_plan_custom_address)));
        } else {
            setListPreferenceSummary(planPreference);
        }
    }
}
