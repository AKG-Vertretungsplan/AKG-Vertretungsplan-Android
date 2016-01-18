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

public class IsRunningSingleton {
    private static IsRunningSingleton instance;
    private Activity currentActivity;

    private IsRunningSingleton(){

    }
    public static IsRunningSingleton getInstance(){
        if (instance == null)
            instance = new IsRunningSingleton();
        return instance;
    }
    public boolean isRunning(){
        return currentActivity!=null;
    }
    public void registerActivity(Activity activity){
        currentActivity = activity;
    }
    public void unregisterActivity(Activity activity){
        if (currentActivity == activity)
            currentActivity = null;
    }
}
