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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ElementDialog extends DialogFragment{
    private String message, shareMessage;
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_SHARE_MESSAGE = "share_message";
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setNeutralButton(R.string.dialog_share, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                        intent.setType("text/plain");
                        startActivity(Intent.createChooser(intent, getResources().getText(R.string.dialog_share)));
                    }
                })
                .setNegativeButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //exit
                    }
                });
        return builder.create();
    }

    public static ElementDialog newInstance(String message, String shareMessage) {
        ElementDialog fragment = new ElementDialog();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_SHARE_MESSAGE, shareMessage);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            message = getArguments().getString(ARG_MESSAGE);
            shareMessage = getArguments().getString(ARG_SHARE_MESSAGE);
        }
        else {
            Log.e("ElementDialog.onCreate", "getArguments()==null");
            message = shareMessage = getString(R.string.error_unknown);
            dismiss();
        }
    }
}