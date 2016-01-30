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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class OwnLog {
    private static final String KEY = "own_log";
    private static final String PREF_LOG_ENABLED = "pref_own_log";
    private static final String LOG_TAG = "OwnLog";

    // Don't instantiate
    private OwnLog(){}


    public static void add(Context context, String msg) {
        add(getSharedPreferences(context), msg);
    }

    public static void forceAdd(Context context, String msg) {
        forceAdd(getSharedPreferences(context), msg);
    }

    public static void clear(Context context) {
        clear(getSharedPreferences(context));
    }

    public static String getFull(Context context) {
        return getFull(getSharedPreferences(context));
    }



    public static void add(SharedPreferences sharedPreferences, String msg) {
        if (sharedPreferences.getBoolean(PREF_LOG_ENABLED, false)) {
            forceAdd(sharedPreferences, msg);
        }
    }

    public static void forceAdd(SharedPreferences sharedPreferences, String msg) {
        Log.d(LOG_TAG, msg);
        msg = sharedPreferences.getString(KEY, "Log start") + "\n" +
                new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS").format(new Date()) +
                ": " + msg;
        sharedPreferences.edit().putString(KEY, msg).apply();
    }

    public static void clear(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().remove(KEY).apply();
        forceAdd(sharedPreferences, "Log cleared");
    }

    public static String getFull(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(KEY, "[empty]");
    }



    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
