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

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

public class DisclaimerDialog extends DialogFragment{
    private SharedPreferences sharedPreferences;
    private boolean buttonPressed = false;

    private SharedPreferences getSharedPreferences(){
        if (sharedPreferences == null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return sharedPreferences;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.disclaimer)
                .setMessage(R.string.disclaimer_text)
                .setPositiveButton(R.string.dialog_agree, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        buttonPressed = true;
                        SharedPreferences.Editor editor = getSharedPreferences().edit();
                        editor.putBoolean(Keys.SEEN_DISCLAIMER, true);
                        editor.apply();
                        Activity currentActivity = getActivity();
                        if (currentActivity instanceof NavigationDrawerActivity)
                            ((NavigationDrawerActivity) currentActivity).afterDisclaimer(new Bool());
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        buttonPressed = true;
                        SharedPreferences.Editor editor = getSharedPreferences().edit();
                        editor.putBoolean(Keys.SEEN_DISCLAIMER, false);
                        editor.apply();
                        getActivity().finish();
                    }
                });
        return builder.create();
    }
    @Override
    public void onDismiss (DialogInterface dialog){
        super.onDismiss(dialog);
        if (!buttonPressed && !getSharedPreferences().getBoolean(Keys.SEEN_DISCLAIMER, false))
            new DisclaimerDialog().show(getFragmentManager(), "DisclaimerDialog");
    }
}
