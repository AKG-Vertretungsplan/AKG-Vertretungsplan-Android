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
                .setNeutralButton(R.string.dialog_do_not_show_again, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("seen_infoscreen_warning", true).apply();
                    }
                })
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Show again next time; only close dialog
                    }
                }).create();
    }
}
