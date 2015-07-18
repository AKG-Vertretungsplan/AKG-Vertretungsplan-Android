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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import java.util.Calendar;

public class LessonPlanActivity extends AppCompatActivity {
    private CustomFragmentPagerAdapter fragmentPagerAdapter;
    private static ViewPager viewPager;
    private SharedPreferences sharedPreferences;
    private int style;
    private static String[] dayName;
    private static LessonPlanFragment[] lessonPlanFragments;
    private static int shortcutDay = -1;//-1 if no shortcut

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        style = Tools.getStyle(this);
        setTheme(style);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lesson_plan);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        dayName = getResources().getStringArray(R.array.lesson_plan_days);
        lessonPlanFragments = new LessonPlanFragment[LessonPlan.DAY_COUNT];

        viewPager = (ViewPager) findViewById(R.id.pager);

        fragmentPagerAdapter = new CustomFragmentPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(fragmentPagerAdapter);

        if (sharedPreferences.getBoolean("pref_lesson_plan_auto_select_day", true)) {//Try to show current day
            Calendar calendar = Calendar.getInstance();

            try{
                if (calendar.get(Calendar.HOUR_OF_DAY) >= Integer.parseInt(sharedPreferences.getString("pref_lesson_plan_auto_select_day_time", "")))
                    calendar.add(Calendar.DAY_OF_WEEK, 1);
            }
            catch (Exception e){
                Log.e("LessonPlanActivity", "Got exception while trying to compare current HOUR_OF_DAY with pref_lesson_plan_auto_select_day_time: " + e);
            }

            switch (calendar.get(Calendar.DAY_OF_WEEK)) {
                case Calendar.TUESDAY:
                    shortcutDay = 1;
                    break;
                case Calendar.WEDNESDAY:
                    shortcutDay = 2;
                    break;
                case Calendar.THURSDAY:
                    shortcutDay = 3;
                    break;
                case Calendar.FRIDAY:
                    shortcutDay = 4;
                    break;
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lesson_plan, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            case R.id.action_reset:
                new ConfirmRemoveLessonsDialog().setValues(this).show(getFragmentManager(), "ConfirmRemoveLessonsDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateAll(){
        for (int i = 0; i < lessonPlanFragments.length; i++)
            lessonPlanFragments[i].update();
    }

    public void showEditLessonDialog(Lesson lesson, LessonPlanFragment lessonPlanFragment, int lessonPosition){
        new EditLessonDialog().setValues(lesson, lessonPlanFragment, lessonPosition).show(getFragmentManager(), "EditLessonDialog");
    }

    public static class CustomFragmentPagerAdapter extends FragmentPagerAdapter {
        public CustomFragmentPagerAdapter (FragmentManager fragmentManager){
            super(fragmentManager);
        }
        @Override
        public int getCount(){
            return LessonPlan.DAY_COUNT;
        }
        @Override
        public Fragment getItem(int position){
            return lessonPlanFragments[position] = LessonPlanFragment.newInstance(position);
        }
        @Override
        public CharSequence getPageTitle (int position){
            return position < dayName.length ? dayName[position] : "???";
        }
        @Override
        public void finishUpdate(ViewGroup container){
            super.finishUpdate(container);

            if (shortcutDay >= 0 && shortcutDay < getCount()) {
                viewPager.setCurrentItem(shortcutDay);
                shortcutDay = -1;//use shortcut only once
            }
        }
    }
}
