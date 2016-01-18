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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

//Actually, this activity is not meant to be shown, but only to install a shortcut to the launcher
public abstract class ShortcutActivity extends Activity {
    // According to the AOSP browser code, there is no public string defining this intent so if Home changes the value, I  have to update this string:
    protected static final String INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        Intent intent = getShortcut(getApplicationContext(), getShortcutIntent(), getShortcutName());
        if (intent != null) {
            setResult(RESULT_OK, intent);
        }
        finish();
        overridePendingTransition(0, 0);
    }

    public static Intent getShortcut(Context context, Intent shortcutIntent, String shortcutName) {
        try {
            Intent intent = new Intent(INSTALL_SHORTCUT);
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher));
            return intent;
        } catch (Exception e){
            Toast.makeText(context, R.string.error_could_not_create_shortcut, Toast.LENGTH_LONG).show();
            Log.e("addLauncherShortcut", "Got exception: " + e);
            return null;
        }
    }

    protected abstract Intent getShortcutIntent();
    protected abstract String getShortcutName();
}
