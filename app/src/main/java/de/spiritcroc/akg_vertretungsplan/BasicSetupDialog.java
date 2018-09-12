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

package de.spiritcroc.akg_vertretungsplan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.Arrays;

public class BasicSetupDialog extends DialogFragment {
    private SharedPreferences sharedPreferences;
    private int selection;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] planValues = getResources().getStringArray(R.array.pref_plan_value_array);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        selection = Arrays.asList(planValues).indexOf(sharedPreferences.getString("pref_plan", "2"));
        if (selection < 0) {
            selection = 0;
        }
        return builder.setTitle(R.string.dialog_select_plan)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPreferences.edit().putString("pref_plan", planValues[selection]).apply();
                    }
                })
                .setSingleChoiceItems(R.array.pref_plan_array, selection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selection = which;
                    }
                }).create();
    }
    @Override
    public void onDismiss (DialogInterface dialog){
        super.onDismiss(dialog);
        Activity currentActivity = getActivity();
        if (currentActivity instanceof NavigationDrawerActivity)
            ((NavigationDrawerActivity) currentActivity).afterDisclaimer(new Bool());
    }
}
