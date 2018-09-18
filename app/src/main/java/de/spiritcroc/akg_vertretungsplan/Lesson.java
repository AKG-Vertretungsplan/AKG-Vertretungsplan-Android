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

import android.util.Log;

public class Lesson {
    public static final char VALUE_SEPARATOR = '©';

    private String teacherFull, teacherShort, subject, room, subjectShort;
    private boolean freeTime;

    public Lesson (String teacherShort, String teacherFull, String subject, String subjectShort, String room){
        setValues(teacherShort, teacherFull, subject, subjectShort, room);
    }
    public Lesson (){
        setFree();
    }

    public String getTeacherShort() {
        return teacherShort;
    }

    public String getTeacherFull() {
        return teacherFull;
    }

    public String getSubject() {
        return subject;
    }

    public String getSubjectShort() {
        return subjectShort;
    }

    public String getRoom() {
        return room;
    }

    public boolean teacherShortNameAvailable(){
        return teacherShort != null && !teacherShort.equals("");
    }
    public boolean teacherFullNameAvailable(){
        return teacherFull != null && !teacherFull.equals("");
    }
    public boolean subjectShortAvailable() {
        return subjectShort != null && !subjectShort.equals("");
    }

    public String getReadableName(boolean showSubjectShort){
        return (showSubjectShort && subjectShortAvailable() ? "[" + getSubjectShort() + "] " : "") + getSubject() + (teacherFullNameAvailable() ? " (" + getTeacherFull() + ")" : (teacherShortNameAvailable() ? " (" + getTeacherShort() + ")" : ""));
    }

    public boolean isFreeTime() {
        return freeTime;
    }

    public void setValues(String teacherShort, String teacherFull, String subject, String subjectShort, String room){
        this.teacherShort = teacherShort;
        this.teacherFull = teacherFull;
        this.subject = subject;
        this.subjectShort = subjectShort;
        this.room = room;
        freeTime = false;
    }
    public void setFree(){
        freeTime = true;
        teacherShort = teacherFull = subject = subjectShort = room = null;
    }

    public static Lesson recoverFromRecreationKey(String key, boolean throwException) {
        if (key.equals(""))
            return new Lesson();//Std if no key
        try {
            Boolean freeTime = Boolean.parseBoolean(Tools.getLine(key, 1, VALUE_SEPARATOR));
            if (freeTime)
                return new Lesson();
            String teacherShort = Tools.getLine(key, 2, VALUE_SEPARATOR);
            String teacherFull = Tools.getLine(key, 3, VALUE_SEPARATOR);
            String subject = Tools.getLine(key, 4, VALUE_SEPARATOR);
            String room = Tools.getLine(key, 5, VALUE_SEPARATOR);
            String subjectShort = Tools.getLine(key, 6, VALUE_SEPARATOR);
            return new Lesson(teacherShort, teacherFull, subject, subjectShort, room);
        } catch (Exception e) {
            Log.e("Lesson", "recoverFromRecreationKey: illegal key: " + key);
            Log.e("Lesson", "Got exception: " + e);
            if (throwException) throw e;
            return null;
        }
    }
    public String getRecreationKey(){
        return "" + freeTime + VALUE_SEPARATOR + teacherShort + VALUE_SEPARATOR + teacherFull + VALUE_SEPARATOR + subject + VALUE_SEPARATOR + room + VALUE_SEPARATOR + subjectShort + VALUE_SEPARATOR;
    }
}
