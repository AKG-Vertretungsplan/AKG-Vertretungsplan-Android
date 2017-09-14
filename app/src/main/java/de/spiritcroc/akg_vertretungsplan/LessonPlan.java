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

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

public class LessonPlan {
    public static final int DAY_COUNT = 5;
    public static final int LESSON_COUNT = 11;
    public static final int LUNCH_BREAK = 7;
    public static final char LESSON_SEPARATOR = '®';
    public static final char DAY_SEPARATOR = '\n';

    private static LessonPlan instance;

    private Lesson[][] lessons;
    private String lessonClass;
    private SharedPreferences sharedPreferences;
    private boolean savePlan = true;

    private LessonPlan(SharedPreferences sharedPreferences){
        this.sharedPreferences = sharedPreferences;
        lessons = new Lesson[DAY_COUNT][LESSON_COUNT];

        retrieveLessonClass();

        recreate();
    }

    private void recreate() {
        savePlan = true;
        recreate(sharedPreferences.getString(Keys.LESSON_PLAN, ""), false);
    }

    private void recreate(String recreationKey, boolean throwException) {
        String dayKey;
        for (int j = 0; j < lessons.length; j++) {
            dayKey = Tools.getLine(recreationKey, j+1, DAY_SEPARATOR);
            if (throwException && TextUtils.isEmpty(dayKey)) {
                throw new RuntimeException("Missing daykey");
            }
            for (int i = 0; i < lessons[j].length; i++) {
                String lessonKey = Tools.getLine(dayKey, i + 1, LESSON_SEPARATOR);
                if (throwException && TextUtils.isEmpty(lessonKey)) {
                    throw new RuntimeException("Missing lessonkey");
                }
                lessons[j][i] = Lesson.recoverFromRecreationKey(lessonKey, throwException);
            }
        }
    }

    public void retrieveLessonClass() {
        lessonClass = sharedPreferences.getString(Keys.CLASS, "");
    }
    public String getLessonClass() {
        return lessonClass;
    }

    public static LessonPlan getInstance(SharedPreferences sharedPreferences){
        if (instance == null)
            instance = new LessonPlan(sharedPreferences);
        return instance;
    }

    public Lesson[] getLessonsForDay(int day){
        return lessons[day];
    }

    public String saveLessons(){
        String recreationKey = "";
        for (int j = 0; j < lessons.length; j++) {
            for (int i = 0; i < lessons[j].length; i++) {
                recreationKey += lessons[j][i].getRecreationKey() + LESSON_SEPARATOR;
            }
            recreationKey += DAY_SEPARATOR;
        }
        if (savePlan) {
            sharedPreferences.edit().putString(Keys.LESSON_PLAN, recreationKey).apply();
        }
        return recreationKey;
    }

    public String[] getSubjects(){
        ArrayList<String> result = new ArrayList<>();
        for (int j = 0; j < lessons.length; j++) {
            for (int i = 0; i < lessons[j].length; i++)
                if (!lessons[j][i].isFreeTime() && !Tools.stringAvailable(result, lessons[j][i].getSubject()))
                    result.add(lessons[j][i].getSubject());
        }
        return result.toArray(new String[result.size()]);
    }
    public String[] getTeachersShort(){
        ArrayList<String> result = new ArrayList<>();
        for (int j = 0; j < lessons.length; j++) {
            for (int i = 0; i < lessons[j].length; i++)
                if (!lessons[j][i].isFreeTime() && !Tools.stringAvailable(result, lessons[j][i].getTeacherShort()))
                    result.add(lessons[j][i].getTeacherShort());
        }
        return result.toArray(new String[result.size()]);
    }
    public String[] getRooms(){
        ArrayList<String> result = new ArrayList<>();
        for (int j = 0; j < lessons.length; j++) {
            for (int i = 0; i < lessons[j].length; i++)
                if (!lessons[j][i].isFreeTime() && !Tools.stringAvailable(result, lessons[j][i].getRoom()) && !"".equals(lessons[j][i].getRoom()))
                    result.add(lessons[j][i].getRoom());
        }
        return result.toArray(new String[result.size()]);
    }
    public String getTeacherShortForSubject(String subject){
        Lesson lesson = getLessonForSubject(subject);
        return lesson == null ? "" : lesson.getTeacherShort();
    }
    public String getTeacherFullForSubject(String subject){
        Lesson lesson = getLessonForSubject(subject);
        return lesson == null ? "" : lesson.getTeacherFull();
    }
    public String getTeacherFullForTeacherShort(String teacherShort){
        if (teacherShort.equals("")) {
            return "";
        }
        for (int j = 0; j < lessons.length; j++) {
            for (int i = 0; i < lessons[j].length; i++)
                if (teacherShort.equals(lessons[j][i].getTeacherShort()))
                    return lessons[j][i].getTeacherFull();
        }
        return "";
    }
    private Lesson getLessonForSubject(String subject){
        for (int j = 0; j < lessons.length; j++) {
            for (int i = 0; i < lessons[j].length; i++)
                if (subject.equals(lessons[j][i].getSubject()))
                    return lessons[j][i];
        }
        return null;
    }
    public String getRoomForSubject(String subject){
        for (int j = 0; j < lessons.length; j++) {
            for (int i = 0; i < lessons[j].length; i++)
                if (subject.equals(lessons[j][i].getSubject())){
                    String room = lessons[j][i].getRoom();
                    if (room != null && !"".equals(room))
                        return room;
                }
        }
        return "";
    }

    public void resetLessons(){
        if (savePlan) {
            sharedPreferences.edit().remove(Keys.CLASS).apply();
            lessonClass = "";
        }
        for (int j = 0; j < lessons.length; j++) {
            for (int i = 0; i < lessons[j].length; i++)
                lessons[j][i] = new Lesson();
        }
    }

    public boolean isConfigured(){
        if (sharedPreferences.contains(Keys.CLASS)) {
            return true;
        }
        for (int j = 0; j < lessons.length; j++)
            for (int i = 0; i < lessons[j].length; i++)
                if (lessons[j][i].getTeacherShort() != null && !lessons[j][i].getTeacherShort().equals(""))
                    return true;
        return false;
    }
    public boolean isRelevant(String lessonClass, int day, int lesson, String teacherShort){
        if (!TextUtils.isEmpty(lessonClass) && !TextUtils.isEmpty(this.lessonClass) &&
                lessonClass.contains(this.lessonClass)) {
            return true;
        }
        int dayPosition;
        switch (day) {
            case Calendar.MONDAY:
                dayPosition = 0;
                break;
            case Calendar.TUESDAY:
                dayPosition = 1;
                break;
            case Calendar.WEDNESDAY:
                dayPosition = 2;
                break;
            case Calendar.THURSDAY:
                dayPosition = 3;
                break;
            case Calendar.FRIDAY:
                dayPosition = 4;
                break;
            default:
                Log.e("LessonPlan", "Day " + day + " is not defined");
                return false;
        }
        if (lessons[dayPosition].length <= lesson-1){
            Log.e("LessonPlan", "Lesson " + lesson + " not available (array to short)");
            return false;
        }
        if (!lessons[dayPosition][lesson-1].isFreeTime()) {
            if (teacherShort.equals(lessons[dayPosition][lesson-1].getTeacherShort())) {
                return true;
            } else if (sharedPreferences.getBoolean(Keys.TEACHER_SHORT_RELEVANCY_IGNORE_CASE, true) && teacherShort.equalsIgnoreCase(lessons[dayPosition][lesson-1].getTeacherShort())) {
                return true;
            }
        }
        return false;
    }

    public String getExportContent() {
        return saveLessons();
    }

    public boolean importContent(Context context, String content, boolean persist) {
        savePlan = persist;
        try {
            recreate(content, true);
            if (savePlan) {
                saveLessons();
            } else {
                lessonClass = context.getString(R.string.import_lesson_plan_tmp_class);
            }
            return true;
        } catch (Exception e) {
            // Back to previous
            recreate();
            return false;
        }
    }
}
