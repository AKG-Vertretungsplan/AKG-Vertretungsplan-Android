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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class LessonPlanActivity extends NavigationDrawerActivity {
    private CustomFragmentPagerAdapter fragmentPagerAdapter;
    private ViewPager viewPager;
    private SharedPreferences sharedPreferences;
    private int style;
    private String[] dayName;
    private String[] dayAdd;
    private TextView textView;
    private LessonPlanFragment[] lessonPlanFragments;
    private ArrayList<String>[] relevantInformation, relevantRoomInformation, generalInformation;
    private ArrayList<Integer>[] relevantInformationLessons;
    private int shortcutDay = -1;//-1 if no shortcut
    private boolean discardSavedInstance = false;

    private MenuItem showFullTimeMenuItem, reloadItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        style = Tools.getStyle(this);
        setTheme(style);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lesson_plan);
        initDrawer();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        textView = (TextView) findViewById(R.id.text_view);
        textView.setText(sharedPreferences.getString("pref_text_view_text", getString(R.string.welcome)));

        dayName = getResources().getStringArray(R.array.lesson_plan_days);
        dayAdd = new String[LessonPlan.DAY_COUNT];
        lessonPlanFragments = new LessonPlanFragment[LessonPlan.DAY_COUNT];
        relevantInformation = new ArrayList[LessonPlan.DAY_COUNT];
        relevantRoomInformation = new ArrayList[LessonPlan.DAY_COUNT];
        relevantInformationLessons = new ArrayList[LessonPlan.DAY_COUNT];
        generalInformation = new ArrayList[LessonPlan.DAY_COUNT];
        for (int i = 0; i < LessonPlan.DAY_COUNT; i++) {
            dayAdd[i] = "";
            relevantInformation[i] = new ArrayList<>();
            relevantRoomInformation[i] = new ArrayList<>();
            relevantInformationLessons[i] = new ArrayList<>();
            generalInformation[i] = new ArrayList<>();
        }

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

            shortcutDay = getDayShortcut(calendar);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(downloadInfoReceiver, new IntentFilter("PlanDownloadServiceUpdate"));
    }
    private int getDayShortcut(Calendar calendar) {
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return 0;
            case Calendar.TUESDAY:
                return  1;
            case Calendar.WEDNESDAY:
                return 2;
            case Calendar.THURSDAY:
                return 3;
            case Calendar.FRIDAY:
                return 4;
            default:
                return -1;
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        if (style != Tools.getStyle(this)) {//Theme has to be set before activity is created, so restart activity
            Intent intent = getIntent();
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else {
            getRelevantInformation();
            updateAll();// Colors could have changed
            if (!sharedPreferences.contains("pref_class")) {
                new EnterLessonClassDialog().setUpdateActivity(this).show(getFragmentManager(), "EnterLessonClassDialog");
            }
            setActionBarTitle();
            setInformationVisibilities();
        }
    }

    @Override
    protected void onDestroy(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadInfoReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lesson_plan, menu);

        boolean showTime = sharedPreferences.getBoolean("pref_lesson_plan_show_time", false);
        menu.findItem(R.id.action_show_time).setChecked(showTime);
        showFullTimeMenuItem = menu.findItem(R.id.action_show_full_time).setChecked(sharedPreferences.getBoolean("pref_lesson_plan_show_full_time", false));
        showFullTimeMenuItem.setVisible(showTime);
        menu.findItem(R.id.action_show_information).setChecked(sharedPreferences.getBoolean("pref_lesson_plan_show_information", false));
        reloadItem = menu.findItem(R.id.action_reload_web_view);
        setInformationVisibilities();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_reset:
                new ConfirmRemoveLessonsDialog().setValues(this).show(getFragmentManager(), "ConfirmRemoveLessonsDialog");
                return true;
            case R.id.action_lesson_class:
                new EnterLessonClassDialog().setUpdateActivity(this).show(getFragmentManager(), "EnterLessonClassDialog");
                return true;
            case R.id.action_show_time:
                boolean showTime = !item.isChecked();
                item.setChecked(showTime);
                sharedPreferences.edit().putBoolean("pref_lesson_plan_show_time", showTime).apply();
                showFullTimeMenuItem.setVisible(showTime);
                updateAll();
                return true;
            case R.id.action_show_full_time:
                boolean showFullTime = !item.isChecked();
                item.setChecked(showFullTime);
                sharedPreferences.edit().putBoolean("pref_lesson_plan_show_full_time", showFullTime).apply();
                updateAll();
                return true;
            case R.id.action_show_information:
                boolean showInformation = !item.isChecked();
                item.setChecked(showInformation);
                sharedPreferences.edit().putBoolean("pref_lesson_plan_show_information", showInformation).apply();
                fragmentPagerAdapter.notifyDataSetChanged();
                updateAll();
                setInformationVisibilities();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setActionBarTitle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            String lessonClass = LessonPlan.getInstance(sharedPreferences).getLessonClass();
            if (lessonClass != null && !lessonClass.equals(""))
                actionBar.setTitle(getString(R.string.lesson_plan) + ": " + lessonClass);
            else
                actionBar.setTitle(getString(R.string.lesson_plan));
        } else {
            Log.d("LessonPlanActivity", "setActionBarTitle: actionBar is null");
        }
    }

    public void updateAll(){
        for (int i = 0; i < lessonPlanFragments.length; i++)
            if (lessonPlanFragments[i] != null)
                lessonPlanFragments[i].update();
    }

    public void updateLessonPlan() {
        getRelevantInformation();
        LessonPlan.getInstance(PreferenceManager.getDefaultSharedPreferences(this)).saveLessons();
        DownloadService.updateNavigationDrawerInformation(this);
    }

    public void showEditLessonDialog(Lesson lesson, LessonPlanFragment lessonPlanFragment, int lessonPosition){
        new EditLessonDialog().setValues(lesson, lessonPlanFragment, lessonPosition).show(getFragmentManager(), "EditLessonDialog");
    }

    public class CustomFragmentPagerAdapter extends FragmentPagerAdapter {
        public CustomFragmentPagerAdapter (FragmentManager fragmentManager){
            super(fragmentManager);
        }
        @Override
        public int getCount(){
            return LessonPlan.DAY_COUNT;
        }
        @Override
        public Fragment getItem(int position){
            return LessonPlanFragment.newInstance(position).setRelevantInformation(relevantInformation[position], relevantRoomInformation[position], relevantInformationLessons[position], generalInformation[position]);
        }
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return lessonPlanFragments[position] =  (LessonPlanFragment) super.instantiateItem(container, position);
        }
        @Override
        public CharSequence getPageTitle (int position){
            return position < dayName.length ?
                    dayName[position] + (sharedPreferences.getBoolean("pref_lesson_plan_show_information", false) ? dayAdd[position] : "") :
                    "???";
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

    private void getRelevantInformation() {
        getRelevantInformation(sharedPreferences.getString("pref_current_title_1", ""),
                sharedPreferences.getString("pref_current_plan_1", ""),
                sharedPreferences.getString("pref_current_title_2", ""),
                sharedPreferences.getString("pref_current_plan_2", ""));
    }
    private void getRelevantInformation(String title1, String plan1, String title2, String plan2) {
        for (int i = 0; i < LessonPlan.DAY_COUNT; i++) {
            dayAdd[i] = "";
            relevantInformation[i].clear();
            relevantRoomInformation[i].clear();
            relevantInformationLessons[i].clear();
            generalInformation[i].clear();
            if (lessonPlanFragments[i] != null) {
                lessonPlanFragments[i].setRelevantInformation(relevantInformation[i], relevantRoomInformation[i], relevantInformationLessons[i], generalInformation[i]);
            }
        }
        getRelevantInformation(title1, plan1);
        getRelevantInformation(title2, plan2);
    }

    private void getRelevantInformation(String title, String plan) {
        int tmpCellCount;
        String tmp = "a";   //not empty
        String[] tmpRowContent = new String[ItemFragment.cellCount];
        LessonPlan lessonPlan = LessonPlan.getInstance(sharedPreferences);

        Calendar calendar = Tools.getDateFromPlanTitle(title);
        if (calendar == null){
            Log.w("LessonPlanActivity", "getRelevantInformation: Could not get date from title");
            return;
        }

        int day = getDayShortcut(calendar);
        if (day < 0 || day >= LessonPlan.DAY_COUNT) {
            Log.w("LessonPlanActivity", "getRelevantInformation: Could not get relevant fragment");
            return;
        }

        dayAdd[day] = " (" + new SimpleDateFormat("d.M.").format(calendar.getTime()) + ")";
        fragmentPagerAdapter.notifyDataSetChanged();

        if (!relevantInformation[day].isEmpty() || !generalInformation[day].isEmpty()) {
            Log.w("LessonPlanActivity", "getRelevantInformation: already information present, don't add more");
            return;
        }

        for (int i = 1; !tmp.equals(""); i++) {
            tmp = Tools.getLine(plan, i);

            String searchingFor = "" + DownloadService.ContentType.TABLE_ROW;

            if (tmp.length() > searchingFor.length()+1 && tmp.substring(0, searchingFor.length()).equals(searchingFor)) { //ignore empty rows
                tmpCellCount = 0;
                tmp = tmp.substring(searchingFor.length());
                if (Tools.countHeaderCells(tmp)<=1) {//ignore headerRows
                    for (int j = 0; j < tmpRowContent.length; j++) {
                        tmpRowContent[j] = Tools.getCellContent(tmp, j+1);
                        if (!tmpRowContent[j].equals(""))
                            tmpCellCount++;
                    }

                    if (tmpCellCount <= 2) {//general info for whole school
                        if (tmpCellCount == 1) {
                            if (!Tools.ignoreSubstitution(tmpRowContent[0])) {
                                createItem(day, tmpRowContent, true);
                            }
                        } else {
                            createItem(day, tmpRowContent, true);
                        }
                    }
                    else {
                        try {
                            if (lessonPlan.isRelevant(tmpRowContent[0], calendar.get(Calendar.DAY_OF_WEEK), Integer.parseInt(tmpRowContent[2]), tmpRowContent[1])) {
                                createItem(day, tmpRowContent, false);
                            }
                        } catch (Exception e) {
                            Log.w("LessonPlanActivity", "getRelevantInformation: Got exception while checking for relevancy: " + e);
                        }
                    }
                }
            }
        }

        if (lessonPlanFragments[day] != null) {
            lessonPlanFragments[day].setRelevantInformation(relevantInformation[day], relevantRoomInformation[day], relevantInformationLessons[day], generalInformation[day]);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (discardSavedInstance) {
            outState.clear();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        // Restart without saving fragment instance state to prevent issues
        discardSavedInstance = true;
        Intent intent = getIntent();
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void createItem(int day, String[] values, boolean general) {
        if (general) {
            generalInformation[day].add(values[0] + (values[1].equals("") ? "" : " → " + values[1]));
        } else {
            LessonPlan lessonPlan = LessonPlan.getInstance(sharedPreferences);
            boolean useFullTeacherNames = sharedPreferences.getBoolean("pref_formatted_plan_replace_teacher_short_with_teacher_full", true);
            String result = "", roomResult = "";
            if (!values[3].equals("") && !values[3].equals(values[1]))//Ignore if same teacher
                result += " " + (useFullTeacherNames ? ItemFragment.getTeacherCombinationString(sharedPreferences, lessonPlan, values[3]) : values[3]);
            if (!values[4].equals(""))
                result += " (" + values[4] + ")";
            if (!values[5].equals(""))
                roomResult += " " + values[5];
            if (!values[6].equals(""))
                result += " " + values[6];
            if (result.length() > 0) {
                result = "→ " + result;
            }
            if (roomResult.length() > 0) {
                roomResult = "   → " + roomResult;
            }
            relevantInformation[day].add(result);
            relevantRoomInformation[day].add(roomResult);
            relevantInformationLessons[day].add(Integer.parseInt(values[2]));
        }
    }

    private void setInformationVisibilities() {
        boolean showInformation =
                sharedPreferences.getBoolean("pref_lesson_plan_show_information", false);
        textView.setVisibility(!sharedPreferences.getBoolean("pref_hide_text_view", false) &&
                showInformation ? View.VISIBLE : View.GONE);
        if (reloadItem != null) {
            reloadItem.setVisible(showInformation);
        }
    }

    private BroadcastReceiver downloadInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            if (action.equals("loadFragmentData")){
                String title1 = intent.getStringExtra("title1");
                String plan1 = intent.getStringExtra("plan1");
                String title2 = intent.getStringExtra("title2");
                String plan2 = intent.getStringExtra("plan2");
                getRelevantInformation(title1, plan1, title2, plan2);
            } else if (action.equals("setTextViewText")) {
                String text = intent.getStringExtra("text");
                textView.setText(text);
            }
        }
    };
}
