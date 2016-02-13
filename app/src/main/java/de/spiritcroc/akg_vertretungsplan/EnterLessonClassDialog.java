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
import android.view.View;
import android.widget.EditText;

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

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

        editLessonClass.setText(sharedPreferences.getString(Keys.CLASS, ""));

        return builder.setTitle(R.string.dialog_enter_lesson_class)
                .setView(view)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPreferences.edit().putString(Keys.CLASS, editLessonClass.getText().toString()).apply();
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
