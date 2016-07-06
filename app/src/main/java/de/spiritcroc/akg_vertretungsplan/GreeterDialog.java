/*
 * Copyright (C) 2016 SpiritCroc
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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

public class GreeterDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        ScrollView view = new ScrollView(activity);
        TextView textView = new TextView(activity);
        textView.setText(R.string.dialog_greeter_message);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_dialog));
        view.addView(textView);
        int dialogPadding = (int) getResources().getDimension(R.dimen.dialog_padding);
        view.setPadding(dialogPadding, dialogPadding, dialogPadding, dialogPadding);
        builder.setView(view)
                .setTitle(R.string.dialog_greeter_title)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreferenceManager.getDefaultSharedPreferences(activity)
                                .edit()
                                .putBoolean(Keys.SEEN_GREETER, true)
                                .apply();
                    }
                });

        return builder.create();
    }
}
