package de.spiritcroc.akg_vertretungsplan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class InfoscreenWarningDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        return builder.setTitle(R.string.dialog_warning)
                .setMessage(R.string.dialog_infoscreen_warning_message)
                .setNegativeButton(R.string.dialog_close_app, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                })
                .setPositiveButton(R.string.dialog_use_plan_1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                                .putString("pref_plan", "1")
                                .remove("seen_infoscreen_warning")// Does not exist anymore
                                .apply();
                    }
                }).create();
    }
}
