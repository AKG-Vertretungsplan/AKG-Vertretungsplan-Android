/*
 * Copyright (C) 2016 SpiritCroc
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

import android.content.Context;
import android.content.Intent;

public class LessonPlanShortcutActivity extends ShortcutActivity {

    @Override
    protected Intent getShortcutIntent() {
        return getShortcutIntent(getApplicationContext());
    }

    @Override
    protected String getShortcutName() {
        return getShortcutName(this);
    }

    /**
     * Static methods for shortcut creation from another activity
     */
    public static Intent getShortcutIntent(Context context) {
        return new Intent(context, LessonPlanActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
    public static String getShortcutName(Context context) {
        return context.getString(R.string.lesson_plan);
    }
}
