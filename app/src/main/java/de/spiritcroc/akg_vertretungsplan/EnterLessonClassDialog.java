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
import android.view.View;
import android.widget.EditText;

public class EnterLessonClassDialog extends DialogFragment {
    private SharedPreferences sharedPreferences;
    private LessonPlanActivity updateActivity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.dialog_enter_lesson_class, null);

        final EditText editLessonClass = (EditText) view.findViewById(R.id.edit_teacher_full);

        editLessonClass.setText(sharedPreferences.getString("pref_class", ""));

        return builder.setTitle(R.string.dialog_enter_lesson_class)
                .setView(view)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPreferences.edit().putString("pref_class", editLessonClass.getText().toString()).apply();
                        LessonPlan.getInstance(sharedPreferences).retrieveLessonClass();
                        if (updateActivity != null) {
                            updateActivity.setActionBarTitle();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close
                    }
                }).create();
    }

    public EnterLessonClassDialog setUpdateActivity(LessonPlanActivity updateActivity) {
        this.updateActivity = updateActivity;
        return this;
    }
}
