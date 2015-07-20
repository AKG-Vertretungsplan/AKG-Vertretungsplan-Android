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

import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

public class LessonPlan {
    public static final int DAY_COUNT = 5;
    public static final int LESSON_COUNT = 11;
    public static final int LUNCH_BREAK = 7;
    public static final char LESSON_SEPARATOR = '®';
    public static final char DAY_SEPARATOR = '\n';

    private static LessonPlan instance;

    private Lesson[][] lessons;
    private SharedPreferences sharedPreferences;

    private LessonPlan(SharedPreferences sharedPreferences){
        this.sharedPreferences = sharedPreferences;
        lessons = new Lesson[DAY_COUNT][LESSON_COUNT];

        String recreationKey = sharedPreferences.getString("pref_lesson_plan", ""), dayKey;

        for (int j = 0; j < lessons.length; j++) {
            dayKey = Tools.getLine(recreationKey, j+1, DAY_SEPARATOR);
            for (int i = 0; i < lessons[j].length; i++)
                lessons[j][i] = Lesson.recoverFromRecreationKey(Tools.getLine(dayKey, i+1, LESSON_SEPARATOR));
        }
    }

    public static LessonPlan getInstance(SharedPreferences sharedPreferences){
        if (instance == null)
            instance = new LessonPlan(sharedPreferences);
        return instance;
    }

    public Lesson[] getLessonsForDay(int day){
        return lessons[day];
    }

    public void saveLessons(){
        String recreationKey = "";
        for (int j = 0; j < lessons.length; j++) {
            for (int i = 0; i < lessons[j].length; i++) {
                recreationKey += lessons[j][i].getRecreationKey() + LESSON_SEPARATOR;
            }
            recreationKey += DAY_SEPARATOR;
        }
        sharedPreferences.edit().putString("pref_lesson_plan", recreationKey).apply();
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
        for (int j = 0; j < lessons.length; j++) {
            for (int i = 0; i < lessons[j].length; i++)
                lessons[j][i] = new Lesson();
        }
    }

    public boolean isConfigured(){
        for (int j = 0; j < lessons.length; j++)
            for (int i = 0; i < lessons[j].length; i++)
                if (!lessons[j][i].isFreeTime())
                    return true;
        return false;
    }
    public boolean isRelevant(int day, int lesson, String teacherShort){
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
        return !lessons[dayPosition][lesson-1].isFreeTime() && teacherShort.equals(lessons[dayPosition][lesson-1].getTeacherShort());
    }
}
