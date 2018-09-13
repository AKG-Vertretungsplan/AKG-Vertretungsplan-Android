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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

public class EditLessonDialog extends DialogFragment {
    private Lesson lesson;
    private LessonPlanFragment fragment;
    private int lessonPosition;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.dialog_edit_lesson, null);

        String[] lessonPlanTimes = getResources().getStringArray(R.array.lesson_plan_times);
        String title;
        if (lessonPlanTimes.length > lessonPosition)
            title = lessonPlanTimes[lessonPosition];
        else
            title = getString(R.string.dialog_edit_lesson);

        final AutoCompleteTextView editSubject = (AutoCompleteTextView) view.findViewById(R.id.edit_subject),
                editTeacherShort = (AutoCompleteTextView) view.findViewById(R.id.edit_teacher_short),
                editRoom = (AutoCompleteTextView) view.findViewById(R.id.edit_room);
        final EditText editTeacherFull = (EditText) view.findViewById(R.id.edit_teacher_full),
                editSubjectShort = (EditText) view.findViewById(R.id.edit_subject_short);

        final LessonPlan lessonPlan = LessonPlan.getInstance(PreferenceManager.getDefaultSharedPreferences(activity));
        editSubject.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, lessonPlan.getSubjects()));
        editTeacherShort.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, lessonPlan.getTeachersShort()));
        editRoom.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, lessonPlan.getRooms()));
        editSubject.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editTeacherShort.setText(lessonPlan.getTeacherShortForSubject(editSubject.getText().toString()));
                editTeacherFull.setText(lessonPlan.getTeacherFullForSubject(editSubject.getText().toString()));
                editRoom.setText(lessonPlan.getRoomForSubject(editSubject.getText().toString()));
                editSubjectShort.setText(lessonPlan.getSubjectShortForSubject(editSubject.getText().toString()));
            }
        });
        editTeacherShort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editTeacherFull.setText(lessonPlan.getTeacherFullForTeacherShort(editTeacherShort.getText().toString()));
            }
        });

        if (lesson == null) {
            Log.e("EditLessonDialog", "No lesson to edit!");
            dismiss();
        }
        else{
            editSubject.setText(lesson.getSubject());
            editSubjectShort.setText(lesson.getSubjectShort());
            editTeacherShort.setText(lesson.getTeacherShort());
            editTeacherFull.setText(lesson.getTeacherFull());
            editRoom.setText(lesson.getRoom());
        }

        builder.setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.dialog_ok, null)
                .setNeutralButton(R.string.dialog_free, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        lesson.setFree();
                        fragment.update();
                        if (getActivity() instanceof LessonPlanActivity) {
                            ((LessonPlanActivity) getActivity()).updateLessonPlan();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //only close dialog
                    }
                });

        final AlertDialog alertDialog = builder.create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {//listeners added to button this way so they don't lead to dialog.dismiss if wrong ip type combination
            @Override
            public void onShow(final DialogInterface dialog) {
                editSubject.dismissDropDown();//prevent them from opening in edit dialog without editing
                editTeacherShort.dismissDropDown();
                editRoom.dismissDropDown();

                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {//listener added to button this way so it doesn't lead to dialog.dismiss if missing subject
                    @Override
                    public void onClick(View v) {
                        String subject = editSubject.getText().toString();
                        if (subject.equals(""))
                            Toast.makeText(getActivity(), R.string.toast_please_enter_subject, Toast.LENGTH_LONG).show();
                        else {
                            String subjectShort = editSubjectShort.getText().toString(),
                                    teacherShort = editTeacherShort.getText().toString(),
                                    teacherFull = editTeacherFull.getText().toString(),
                                    room = editRoom.getText().toString();
                            if (containsIllegalCharacter(subject))
                                Toast.makeText(getActivity(), R.string.toast_subject_illegal_character, Toast.LENGTH_SHORT).show();
                            else if (containsIllegalCharacter(subjectShort))
                                Toast.makeText(getActivity(), R.string.toast_subject_short_illegal_character, Toast.LENGTH_SHORT).show();
                            else if (containsIllegalCharacter(teacherShort))
                                Toast.makeText(getActivity(), R.string.toast_teacher_short_illegal_character, Toast.LENGTH_SHORT).show();
                            else if (containsIllegalCharacter(teacherFull))
                                Toast.makeText(getActivity(), R.string.toast_teacher_full_illegal_character, Toast.LENGTH_SHORT).show();
                            else if (containsIllegalCharacter(room))
                                Toast.makeText(getActivity(), R.string.toast_room_illegal_character, Toast.LENGTH_SHORT).show();
                            else {
                                lesson.setValues(teacherShort, teacherFull, subject, subjectShort, room);
                                fragment.update();
                                if (getActivity() instanceof LessonPlanActivity) {
                                    ((LessonPlanActivity) getActivity()).updateLessonPlan();
                                }
                                dismiss();
                            }
                        }
                    }
                });
            }
        });

        return alertDialog;
    }

    public EditLessonDialog setValues(Lesson lesson, LessonPlanFragment fragment, int lessonPosition){
        this.lesson = lesson;
        this.fragment = fragment;
        this.lessonPosition = lessonPosition;
        return this;
    }

    private boolean containsIllegalCharacter(String text){
        return text.contains(""+LessonPlan.DAY_SEPARATOR) || text.contains(""+LessonPlan.LESSON_SEPARATOR) || text.contains(""+Lesson.VALUE_SEPARATOR);
    }
}
