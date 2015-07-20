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

import android.util.Log;

public class Lesson {
    public static final char VALUE_SEPARATOR = '©';

    private String teacherFull, teacherShort, subject, room;
    private boolean freeTime;

    public Lesson (String teacherShort, String teacherFull, String subject, String room){
        setValues(teacherShort, teacherFull, subject, room);
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

    public String getRoom() {
        return room;
    }

    public boolean teacherShortNameAvailable(){
        return teacherShort != null && !teacherShort.equals("");
    }
    public boolean teacherFullNameAvailable(){
        return teacherFull != null && !teacherFull.equals("");
    }

    public String getReadableName(){
        return getSubject() + (teacherFullNameAvailable() ? " (" + getTeacherFull() + ")" : (teacherShortNameAvailable() ? " (" + getTeacherShort() + ")" : ""));
    }

    public boolean isFreeTime() {
        return freeTime;
    }

    public void setValues(String teacherShort, String teacherFull, String subject, String room){
        this.teacherShort = teacherShort;
        this.teacherFull = teacherFull;
        this.subject = subject;
        this.room = room;
        freeTime = false;
    }
    public void setFree(){
        freeTime = true;
        teacherShort = teacherFull = subject = room = null;
    }

    public static Lesson recoverFromRecreationKey(String key){
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
            return new Lesson(teacherShort, teacherFull, subject, room);
        }
        catch (Exception e){
            Log.e("Lesson", "recoverFromRecreationKey: illegal key: " + key);
            Log.e("Lesson", "Got exception: " + e);
            return null;
        }
    }
    public String getRecreationKey(){
        return "" + freeTime + VALUE_SEPARATOR + teacherShort + VALUE_SEPARATOR + teacherFull + VALUE_SEPARATOR + subject + VALUE_SEPARATOR + room + VALUE_SEPARATOR;
    }
}
