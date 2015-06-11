/*
 * Copyright (C) 2015 SpiritCroc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
                        editor.putBoolean("pref_seen_disclaimer", true);
                        editor.apply();
                        Activity currentActivity = getActivity();
                        if (currentActivity instanceof FormattedActivity)
                            ((FormattedActivity) currentActivity).onCreateAfterDisclaimer();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        buttonPressed = true;
                        SharedPreferences.Editor editor = getSharedPreferences().edit();
                        editor.putBoolean("pref_seen_disclaimer", false);
                        editor.apply();
                        getActivity().finish();
                    }
                });
        return builder.create();
    }
    @Override
    public void onDismiss (DialogInterface dialog){
        super.onDismiss(dialog);
        if (!buttonPressed && !getSharedPreferences().getBoolean("pref_seen_disclaimer", false))
            new DisclaimerDialog().show(getFragmentManager(), "DisclaimerDialog");
    }
}
