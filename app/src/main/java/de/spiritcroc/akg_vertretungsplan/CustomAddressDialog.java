/*
 * Copyright (C) 2015-2018 SpiritCroc
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
import android.widget.EditText;

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

public class CustomAddressDialog extends DialogFragment {
    private SharedPreferences sharedPreferences;
    private EditText editAddress;
    private DialogInterface.OnDismissListener onDismissListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editAddress = new EditText(getActivity());
        editAddress.setText(sharedPreferences.getString(Keys.CUSTOM_ADDRESS, ""));
        return builder.setTitle(R.string.dialog_custom_address)
                .setView(editAddress)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close
                    }
                })
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPreferences.edit().putString(Keys.CUSTOM_ADDRESS,
                                editAddress.getText().toString()).apply();
                    }
                }).create();
    }

    @Override
    public void onDismiss (DialogInterface dialog){
        super.onDismiss(dialog);
        Activity currentActivity = getActivity();
        if (currentActivity instanceof NavigationDrawerActivity)
            ((NavigationDrawerActivity) currentActivity).afterDisclaimer(new Bool());
        if (onDismissListener != null) {
            onDismissListener.onDismiss(dialog);
        }
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }
}
