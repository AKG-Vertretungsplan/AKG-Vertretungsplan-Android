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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class AboutDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_about, null);
        String text = getString(R.string.version);
        try {
            text += " " + activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        }
        catch (PackageManager.NameNotFoundException e){
            Log.e("AboutDialog", "Got exception " + e);
        }
        ((TextView) view.findViewById(R.id.version_view)).setText(text);
        ((TextView) view.findViewById(R.id.about_view)).setMovementMethod(LinkMovementMethod.getInstance());
        builder.setView(view)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //only close dialog
                    }
                })
                .setNegativeButton(R.string.disclaimer, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DisclaimerDialog().show(getFragmentManager(), "DisclaimerDialog");
                    }
                })
                .setNeutralButton(R.string.dialog_greeter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new GreeterDialog().show(getFragmentManager(), "GreeterDialog");
                    }
                });

        return builder.create();
    }
}
