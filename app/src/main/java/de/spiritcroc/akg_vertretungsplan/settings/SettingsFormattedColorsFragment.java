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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import de.spiritcroc.akg_vertretungsplan.R;

public class SettingsFormattedColorsFragment extends CustomPreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_formatted_colors);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_preferences_formatted_colors, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_reset:
                promptReset();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void init() {
        setListPreferenceSummary(Keys.HEADER_TEXT_TEXT_COLOR);
        setListPreferenceSummary(Keys.HEADER_TEXT_BG_COLOR);
        setListPreferenceSummary(Keys.HEADER_TEXT_TEXT_COLOR_HL);
        setListPreferenceSummary(Keys.HEADER_TEXT_BG_COLOR_HL);
        setListPreferenceSummary(Keys.CLASS_TEXT_TEXT_COLOR);
        setListPreferenceSummary(Keys.CLASS_TEXT_BG_COLOR);
        setListPreferenceSummary(Keys.NORMAL_TEXT_TEXT_COLOR);
        setListPreferenceSummary(Keys.NORMAL_TEXT_BG_COLOR);
        setListPreferenceSummary(Keys.NORMAL_TEXT_TEXT_COLOR_HL);
        setListPreferenceSummary(Keys.NORMAL_TEXT_BG_COLOR_HL);
        setListPreferenceSummary(Keys.RELEVANT_TEXT_TEXT_COLOR);
        setListPreferenceSummary(Keys.RELEVANT_TEXT_BG_COLOR);
        setListPreferenceSummary(Keys.RELEVANT_TEXT_TEXT_COLOR_HL);
        setListPreferenceSummary(Keys.RELEVANT_TEXT_BG_COLOR_HL);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Keys.HEADER_TEXT_TEXT_COLOR:
            case Keys.HEADER_TEXT_BG_COLOR:
            case Keys.HEADER_TEXT_TEXT_COLOR_HL:
            case Keys.HEADER_TEXT_BG_COLOR_HL:
            case Keys.CLASS_TEXT_TEXT_COLOR:
            case Keys.CLASS_TEXT_BG_COLOR:
            case Keys.NORMAL_TEXT_TEXT_COLOR:
            case Keys.NORMAL_TEXT_BG_COLOR:
            case Keys.NORMAL_TEXT_TEXT_COLOR_HL:
            case Keys.NORMAL_TEXT_BG_COLOR_HL:
            case Keys.RELEVANT_TEXT_TEXT_COLOR:
            case Keys.RELEVANT_TEXT_BG_COLOR:
            case Keys.RELEVANT_TEXT_TEXT_COLOR_HL:
            case Keys.RELEVANT_TEXT_BG_COLOR_HL:
                setListPreferenceSummary(key);
                break;
        }
    }

    private void promptReset() {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.pref_restore_default_values)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        reset();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                })
                .show();
    }

    private void reset() {
        getPreferenceManager().getSharedPreferences().edit()
                .remove(Keys.HEADER_TEXT_TEXT_COLOR)
                .remove(Keys.HEADER_TEXT_BG_COLOR)
                .remove(Keys.HEADER_TEXT_TEXT_COLOR_HL)
                .remove(Keys.HEADER_TEXT_BG_COLOR_HL)
                .remove(Keys.CLASS_TEXT_TEXT_COLOR)
                .remove(Keys.CLASS_TEXT_BG_COLOR)
                .remove(Keys.NORMAL_TEXT_TEXT_COLOR)
                .remove(Keys.NORMAL_TEXT_BG_COLOR)
                .remove(Keys.NORMAL_TEXT_TEXT_COLOR_HL)
                .remove(Keys.NORMAL_TEXT_BG_COLOR_HL)
                .remove(Keys.RELEVANT_TEXT_TEXT_COLOR)
                .remove(Keys.RELEVANT_TEXT_BG_COLOR)
                .remove(Keys.RELEVANT_TEXT_TEXT_COLOR_HL)
                .remove(Keys.RELEVANT_TEXT_BG_COLOR_HL)
                .apply();
        SettingsUserInterfaceFragment.applyThemeToCustomColors(getActivity(),
                SettingsUserInterfaceFragment.APPLY_THEME_FORMATTED, true);
        // Restart to immediately show changes
        getActivity().recreate();
    }

}
