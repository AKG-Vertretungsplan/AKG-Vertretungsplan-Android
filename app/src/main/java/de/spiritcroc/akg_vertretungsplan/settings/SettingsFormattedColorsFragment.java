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

import de.spiritcroc.akg_vertretungsplan.R;

public class SettingsFormattedColorsFragment extends CustomPreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_formatted_colors);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
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

}
