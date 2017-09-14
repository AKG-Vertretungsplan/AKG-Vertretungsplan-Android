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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

public class LessonPlanActivity extends NavigationDrawerActivity {
    private CustomFragmentPagerAdapter fragmentPagerAdapter;
    private ViewPager viewPager;
    private SharedPreferences sharedPreferences;
    private String[] dayName;
    private String[] dayAdd;
    private String[] dates;
    private TextView textView;
    private LessonPlanFragment[] lessonPlanFragments;
    private ArrayList<String>[] relevantInformation, relevantRoomInformation, generalInformation;
    private ArrayList<Integer>[] relevantInformationLessons;
    private String[][] headerRow;
    private ArrayList<String[]>[] informationCells;
    private int shortcutDay = 0;//-1 if no shortcut, default: monday (0)
    private boolean discardSavedInstance = false;

    private MenuItem showFullTimeMenuItem, reloadItem;

    private Handler updateHandler = new Handler();

    private static final String EXPORT_ENDING = ".akvlp";
    private static final String EXPORT_DIR = "export";
    private static final String EXPORT_FILE = "stundenplan" + EXPORT_ENDING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lesson_plan);
        initDrawer();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        textView = (TextView) findViewById(R.id.text_view);
        textView.setText(sharedPreferences.getString(Keys.TEXT_VIEW_TEXT, getString(R.string.welcome)));

        dayName = getResources().getStringArray(R.array.lesson_plan_days);
        dayAdd = new String[LessonPlan.DAY_COUNT];
        dates = new String[LessonPlan.DAY_COUNT];
        lessonPlanFragments = new LessonPlanFragment[LessonPlan.DAY_COUNT];
        relevantInformation = new ArrayList[LessonPlan.DAY_COUNT];
        relevantRoomInformation = new ArrayList[LessonPlan.DAY_COUNT];
        relevantInformationLessons = new ArrayList[LessonPlan.DAY_COUNT];
        generalInformation = new ArrayList[LessonPlan.DAY_COUNT];
        headerRow = new String[LessonPlan.DAY_COUNT][];
        informationCells = new ArrayList[LessonPlan.DAY_COUNT];
        for (int i = 0; i < LessonPlan.DAY_COUNT; i++) {
            dayAdd[i] = "";
            dates[i] = "";
            relevantInformation[i] = new ArrayList<>();
            relevantRoomInformation[i] = new ArrayList<>();
            relevantInformationLessons[i] = new ArrayList<>();
            generalInformation[i] = new ArrayList<>();
            headerRow[i] = new String[FormattedFragment.CELL_COUNT];
            informationCells[i] = new ArrayList<>();
        }

        viewPager = (ViewPager) findViewById(R.id.pager);

        // Fix pager tab strip not showing using support libraries 24.0.0 (https://code.google.com/p/android/issues/detail?id=213359)
        ((ViewPager.LayoutParams) findViewById(R.id.pager_tab_strip).getLayoutParams()).isDecor = true;

        fragmentPagerAdapter = new CustomFragmentPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(fragmentPagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // Allow "infinite" scrolling
                if (position == 0) {
                    viewPager.setCurrentItem(LessonPlan.DAY_COUNT);
                } else if (position == LessonPlan.DAY_COUNT + 1) {
                    viewPager.setCurrentItem(1);
                }
            }
        });

        if (sharedPreferences.getBoolean(Keys.LESSON_PLAN_AUT0_SELECT_DAY, true)) {//Try to show current day
            Calendar calendar = Calendar.getInstance();

            try {
                int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                int currentMinute = calendar.get(Calendar.MINUTE);
                int settingHour = Integer.parseInt(sharedPreferences.getString(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME, "17"));
                int settingMinute = sharedPreferences.getInt(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME_MINUTES, 0);
                if (currentHour > settingHour || (currentHour == settingHour && currentMinute >= settingMinute)) {
                    calendar.add(Calendar.DAY_OF_WEEK, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            shortcutDay = getDayShortcut(calendar);
            if (shortcutDay == -1) {
                shortcutDay = 0;// Monday as default
            }
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
        getRelevantInformation();
        updateAll();// Colors could have changed
        if (!sharedPreferences.contains(Keys.CLASS)) {
            new EnterLessonClassDialog().setUpdateActivity(this).show(getFragmentManager(), "EnterLessonClassDialog");
        }
        setActionBarTitle();
        setInformationVisibilities();
        updateCurrentLesson.run();
    }

    @Override
    protected void onDestroy(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadInfoReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lesson_plan, menu);

        boolean showTime = sharedPreferences.getBoolean(Keys.LESSON_PLAN_SHOW_TIME, false);
        menu.findItem(R.id.action_show_time).setChecked(showTime);
        showFullTimeMenuItem = menu.findItem(R.id.action_show_full_time).setChecked(sharedPreferences.getBoolean(Keys.LESSON_PLAN_SHOW_FULL_TIME, false));
        showFullTimeMenuItem.setVisible(showTime);
        menu.findItem(R.id.action_show_information).setChecked(sharedPreferences.getBoolean(Keys.LESSON_PLAN_SHOW_INFORMATION, false));
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
                sharedPreferences.edit().putBoolean(Keys.LESSON_PLAN_SHOW_TIME, showTime).apply();
                showFullTimeMenuItem.setVisible(showTime);
                updateAll();
                return true;
            case R.id.action_show_full_time:
                boolean showFullTime = !item.isChecked();
                item.setChecked(showFullTime);
                sharedPreferences.edit().putBoolean(Keys.LESSON_PLAN_SHOW_FULL_TIME, showFullTime).apply();
                updateAll();
                return true;
            case R.id.action_show_information:
                boolean showInformation = !item.isChecked();
                item.setChecked(showInformation);
                sharedPreferences.edit().putBoolean(Keys.LESSON_PLAN_SHOW_INFORMATION, showInformation).apply();
                fragmentPagerAdapter.notifyDataSetChanged();
                updateAll();
                setInformationVisibilities();
                return true;
            case R.id.action_reload_web_view:
                startDownloadService(true);
                return true;
            case R.id.action_export_share_lesson_plan:
                exportPlan();
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
            return LessonPlan.DAY_COUNT + 2;
        }
        @Override
        public Fragment getItem(int position){
            Fragment f;
            int pos = position - 1;// Real day position considering the shortcut pages
            if (position == 0 || position == LessonPlan.DAY_COUNT + 1) {
                f = new Fragment();
            } else {
                f = LessonPlanFragment.newInstance(pos).setRelevantInformation(relevantInformation[pos], relevantRoomInformation[pos], relevantInformationLessons[pos], generalInformation[pos], dates[pos], headerRow[pos], informationCells[pos]);
                if (pos == getDayShortcut(Calendar.getInstance())) {
                    ((LessonPlanFragment) f).markCurrentLesson(getCurrentLesson());
                }
            }
            return f;
        }
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment f = (Fragment) super.instantiateItem(container, position);
            if (position > 0 && position < getCount() - 1) {
                lessonPlanFragments[position-1] = (LessonPlanFragment) f;
            }
            return f;
        }
        @Override
        public CharSequence getPageTitle (int position){
            int pos = position - 1;// Real day position considering the shortcut pages
            if (pos >= 0 && pos < dayName.length) {
                return dayName[pos] + (sharedPreferences.getBoolean(Keys.LESSON_PLAN_SHOW_INFORMATION, false) ? dayAdd[pos] : "");
            } else {
                return "";
            }
        }
        @Override
        public void finishUpdate(ViewGroup container){
            super.finishUpdate(container);

            if (shortcutDay >= 0 && shortcutDay < LessonPlan.DAY_COUNT) {
                viewPager.setCurrentItem(shortcutDay + 1);
                shortcutDay = -1;//use shortcut only once
            }
        }
    }

    private void getRelevantInformation() {
        getRelevantInformation(sharedPreferences.getString(Keys.CURRENT_TITLE_1, ""),
                sharedPreferences.getString(Keys.CURRENT_PLAN_1, ""),
                sharedPreferences.getString(Keys.CURRENT_TITLE_2, ""),
                sharedPreferences.getString(Keys.CURRENT_PLAN_2, ""));
    }
    private void getRelevantInformation(String title1, String plan1, String title2, String plan2) {
        for (int i = 0; i < LessonPlan.DAY_COUNT; i++) {
            dates[i] = "";
            dayAdd[i] = "";
            headerRow[i] = new String[FormattedFragment.CELL_COUNT];
            relevantInformation[i].clear();
            relevantRoomInformation[i].clear();
            relevantInformationLessons[i].clear();
            generalInformation[i].clear();
            informationCells[i].clear();
            if (lessonPlanFragments[i] != null) {
                lessonPlanFragments[i].setRelevantInformation(relevantInformation[i], relevantRoomInformation[i], relevantInformationLessons[i], generalInformation[i], dates[i], headerRow[i], informationCells[i]);
            }
        }
        getRelevantInformation(title1, plan1);
        getRelevantInformation(title2, plan2);
    }

    private void getRelevantInformation(String title, String plan) {
        int tmpCellCount;
        String tmp = "a";   //not empty
        String[] tmpRowContent = new String[FormattedFragment.CELL_COUNT];
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
        dates[day] = title;
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
                if (Tools.countHeaderCells(tmp)<=1) {
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
                } else {//headerRow
                    for (int j = 0; j < tmpRowContent.length; j++) {
                        headerRow[day][j] = Tools.getCellContent(tmp, j+1);
                    }
                }
            }
        }

        if (lessonPlanFragments[day] != null) {
            lessonPlanFragments[day].setRelevantInformation(relevantInformation[day], relevantRoomInformation[day], relevantInformationLessons[day], generalInformation[day], dates[day], headerRow[day], informationCells[day]);
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
            boolean useFullTeacherNames = sharedPreferences.getBoolean(Keys.FORMATTED_PLAN_REPLACE_TEACHER_SHORT_WITH_TEACHER_FULL, true);
            String result = "", roomResult = "";
            if (!values[3].equals("") && !values[3].equals(values[1]))//Ignore if same teacher
                result += " " + (useFullTeacherNames ? FormattedFragment.getTeacherCombinationString(sharedPreferences, lessonPlan, values[3]) : values[3]);
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
        informationCells[day].add(values.clone());
    }

    private void setInformationVisibilities() {
        boolean showInformation =
                sharedPreferences.getBoolean(Keys.LESSON_PLAN_SHOW_INFORMATION, false);
        textView.setVisibility(!sharedPreferences.getBoolean(Keys.HIDE_TEXT_VIEW, false) &&
                showInformation ? View.VISIBLE : View.GONE);
        if (reloadItem != null) {
            reloadItem.setVisible(showInformation);
        }
    }

    private int getCurrentLesson() {
        Calendar now = Calendar.getInstance();
        int day = getDayShortcut(now);
        if (day < 0) {
            return -1;
        }
        String[] stringTimes = getResources().getStringArray(R.array.lesson_plan_times);
        int markLesson = -1;
        for (int i = 0; i < stringTimes.length && markLesson == -1; i++) {
            String rmSearch = " – ";
            int rmIndex = stringTimes[i].indexOf(rmSearch);
            if (rmIndex >= 0) {
                stringTimes[i] = stringTimes[i].substring(rmIndex + rmSearch.length());
            }
            try {
                Calendar c = new GregorianCalendar();
                Date d = new SimpleDateFormat("HH:mm").parse(stringTimes[i]);
                c.setTime(d);

                int diff = Tools.timeDiffMillisNoDate(now, c);
                if (diff > 0 && diff < 3600000) {// 'current' lesson: always when within 1 hour before end
                    markLesson = i;
                    Log.d("LessonPlanActivity", "Update current lesson in " + diff + "ms");
                    updateHandler.postDelayed(updateCurrentLesson, diff);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return markLesson;
    }

    private Runnable updateCurrentLesson = new Runnable() {
        @Override
        public void run() {
            updateHandler.removeCallbacks(this);
            for (int i = 0; i < lessonPlanFragments.length; i++) {
                Calendar c = Calendar.getInstance();
                if (lessonPlanFragments[i] != null) {
                    lessonPlanFragments[i].markCurrentLesson(
                            i == getDayShortcut(c) ?
                                    getCurrentLesson() :
                                    -1
                    );
                }
            }
        }
    };

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


    private void exportPlan() {
        boolean success = false;
        FileWriter writer = null;
        File dir = new File(getCacheDir(), EXPORT_DIR);
        dir.mkdirs();
        File file = new File(dir, EXPORT_FILE);
        try {
            writer = new FileWriter(file);
            writer.write(LessonPlan.getInstance(sharedPreferences).getExportContent());
            writer.flush();
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.export_lesson_plan_failure, Toast.LENGTH_LONG).show();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (success) {
            Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this,
                    "de.spiritcroc.akg_vertretungsplan.fileprovider", file));
            startActivity(Intent.createChooser(shareIntent,
                    getString(R.string.action_export_lesson_plan_share_via)));
        }
    }
}
