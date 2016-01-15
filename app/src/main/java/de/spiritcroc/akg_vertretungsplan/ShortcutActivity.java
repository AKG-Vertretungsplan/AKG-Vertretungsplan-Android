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
        Intent intent = getShortcut(getApplicationContext(), getShortcutIntent(), getShortcutName());
        if (intent != null) {
            setResult(RESULT_OK, intent);
        }
        finish();
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
