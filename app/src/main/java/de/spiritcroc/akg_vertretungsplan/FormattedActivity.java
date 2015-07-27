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

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.widget.TintImageView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

public class FormattedActivity extends AppCompatActivity implements ItemFragment.OnFragmentInteractionListener{
    private CustomFragmentPagerAdapter fragmentPagerAdapter;
    private static ViewPager viewPager;
    private static String plan1, plan2, title1, title2;
    private TextView textView;
    private SharedPreferences sharedPreferences;
    private static ItemFragment fragment1, fragment2;
    private static Calendar date1;
    private int style;
    private boolean created = false;
    private static boolean shortCutToPageTwo = false, filteredMode;
    private MenuItem reloadItem, filterItem, markReadItem;
    private TintImageView overflow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        style = Tools.getStyle(this);
        setTheme(style);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_formatted);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        textView = (TextView) findViewById(R.id.text_view);
        textView.setText(sharedPreferences.getString("pref_text_view_text", getString(R.string.welcome)));
        title1 = sharedPreferences.getString("pref_current_title_1", getString(R.string.today));
        plan1 = sharedPreferences.getString("pref_current_plan_1", "");
        title2 = sharedPreferences.getString("pref_current_title_2", getString(R.string.tomorrow));
        plan2 = sharedPreferences.getString("pref_current_plan_2", "");
        try {
            date1 = Tools.getDateFromPlanTitle(title1);
        }
        catch (Exception e){
            Log.e("FormattedActivity", "Got error while trying to extract date1 from the titles: " + e);
            date1 = null;//Deactivate date functionality
        }

        viewPager = (ViewPager) findViewById(R.id.pager);

        fragmentPagerAdapter = new CustomFragmentPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(fragmentPagerAdapter);

        LocalBroadcastManager.getInstance(this).registerReceiver(downloadInfoReceiver, new IntentFilter("PlanDownloadServiceUpdate"));

        if (sharedPreferences.getBoolean("pref_seen_disclaimer", false))
            onCreateAfterDisclaimer();

    }
    public void onCreateAfterDisclaimer(){
        if (created)//only run once
            return;
        //Download plan stuff  start
        Calendar calendar = DownloadService.stringToCalendar(sharedPreferences.getString("pref_last_checked", "???"));
        boolean startedDownloadService = false;
        if (calendar == null || Calendar.getInstance().getTime().getTime() - calendar.getTime().getTime() > Integer.parseInt(sharedPreferences.getString("pref_auto_load_on_open", "5"))*60000) {
            startDownloadService();
            startedDownloadService = true;
        }
        else {
            String text = sharedPreferences.getBoolean("pref_illegal_plan", false) ? getString(R.string.error_illegal_plan) : getString(R.string.last_checked) + " " + sharedPreferences.getString("pref_last_checked", getString(R.string.error_unknown));
            textView.setText(text);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("pref_text_view_text", text);
            editor.apply();
        }
        //Download plan stuff end
        created = true;

        if (sharedPreferences.getBoolean("pref_illegal_plan", false) && !startedDownloadService) {
            startActivity(new Intent(getApplication(), WebActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            Toast.makeText(getApplicationContext(), getString(R.string.error_illegal_plan), Toast.LENGTH_LONG).show();
        }

        if (date1 != null && sharedPreferences.getBoolean("pref_formatted_plan_auto_select_day", true)) {//Try to show most relevant day
            Calendar currentDate = Calendar.getInstance();
            if (currentDate.after(date1)){
                if (currentDate.get(Calendar.DAY_OF_MONTH) != date1.get(Calendar.DAY_OF_MONTH)
                        || currentDate.get(Calendar.MONTH) != date1.get(Calendar.MONTH)
                        || currentDate.get(Calendar.YEAR) != date1.get(Calendar.YEAR))
                    shortCutToPageTwo = true;
                else{
                    try{
                        if (currentDate.get(Calendar.HOUR_OF_DAY) >= Integer.parseInt(sharedPreferences.getString("pref_formatted_plan_auto_select_day_time", "")))
                            shortCutToPageTwo = true;
                    }
                    catch (Exception e){
                        Log.e("FormattedActivity", "Got exception while trying to compare current HOUR_OF_DAY with pref_formatted_plan_auto_select_day_time: " + e);
                    }
                }
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        Tools.setUnseenFalse(this);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);


        try{
            ContentValues contentValues = new ContentValues();
            contentValues.put("tag", "de.spiritcroc.akg_vertretungsplan/.FormattedActivity");
            contentValues.put("count", 0);
            getContentResolver().insert(Uri.parse("content://com.teslacoilsw.notifier/unread_count"), contentValues);
        }
        catch (IllegalArgumentException e){
            Log.d("FormattedActivity", "TeslaUnread is not installed");
        }
        catch (Exception e){
            Log.e("FormattedActivity", "Got exception while trying to sending count to TeslaUnread: " + e);
        }

        IsRunningSingleton.getInstance().registerActivity(this);

        if (!sharedPreferences.getBoolean("pref_seen_disclaimer", false))
            new DisclaimerDialog().show(getFragmentManager(), "DisclaimerDialog");

        if (filterItem != null)
            filterItem.setShowAsAction(sharedPreferences.getBoolean("pref_show_filtered_plan_as_action", false) ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
        if (reloadItem != null)
            reloadItem.setVisible(!sharedPreferences.getBoolean("pref_hide_action_reload", false));
        if (markReadItem != null)
            markReadItem.setShowAsAction(sharedPreferences.getBoolean("pref_show_mark_read_as_action", false) ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);

        //Apply color settings:
        setActionBarColor();

        if (fragment1 != null)
            fragment1.reloadContent();
        if (fragment2 != null)
            fragment2.reloadContent();
        if (style != Tools.getStyle(this)) {//Theme has to be set before activity is created, so restart activity
            Intent intent = getIntent();
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }

        textView.setVisibility(sharedPreferences.getBoolean("pref_hide_text_view", false) ? View.GONE : View.VISIBLE);

        if (sharedPreferences.getBoolean("pref_reload_on_resume", false))
            startDownloadService();
    }
    @Override
    protected void onPause(){
        super.onPause();
        IsRunningSingleton.getInstance().unregisterActivity(this);
    }
    @Override
    protected void onDestroy(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadInfoReceiver);
        super.onDestroy();
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        menu.findItem(R.id.action_send_debug_email).setVisible(sharedPreferences.getBoolean("pref_hidden_debug_enabled", false) && sharedPreferences.getBoolean("pref_enable_option_send_debug_email", false));
        menu.findItem(R.id.action_filter_plan).setVisible(LessonPlan.getInstance(sharedPreferences).isConfigured());

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_formatted, menu);

        if (menu != null) {
            reloadItem = menu.findItem(R.id.action_reload_web_view);
            reloadItem.setVisible(!sharedPreferences.getBoolean("pref_hide_action_reload", false));
            filterItem = menu.findItem(R.id.action_filter_plan);
            filterItem.setShowAsAction(sharedPreferences.getBoolean("pref_show_filtered_plan_as_action", false) ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
            markReadItem = menu.findItem(R.id.action_mark_read);
            markReadItem.setShowAsAction(sharedPreferences.getBoolean("pref_show_mark_read_as_action", false) ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
            requestRecheckUnreadChanges();

            //http://stackoverflow.com/questions/22046903/changing-the-android-overflow-menu-icon-programmatically/22106474#22106474
            final String overflowDescription = getString(R.string.abc_action_menu_overflow_description);
            final ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
            final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    final ArrayList<View> outViews = new ArrayList<>();
                    Tools.findViewsWithText(outViews, decorView, overflowDescription);
                    if (!outViews.isEmpty()) {
                        overflow = (TintImageView) outViews.get(0);
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                            decorView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        else
                            decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        setActionBarColor();
                    }
                }
            });

            MenuItem filterPlanMenuItem = menu.findItem(R.id.action_filter_plan);
            filteredMode = sharedPreferences.getBoolean("pref_filter_plan", false);
            if (!LessonPlan.getInstance(sharedPreferences).isConfigured()) {
                filteredMode = false;
                sharedPreferences.edit().putBoolean("pref_filter_plan", filteredMode).apply();
            }
            filterPlanMenuItem.setChecked(filteredMode);
            setActionBarColor();
            filterPlanMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    filteredMode = !item.isChecked();
                    item.setChecked(filteredMode);
                    sharedPreferences.edit().putBoolean("pref_filter_plan", filteredMode).apply();
                    setActionBarColor();
                    if (fragment1 != null)
                        fragment1.reloadContent();
                    if (fragment2 != null)
                        fragment2.reloadContent();
                    return false;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }
    private void setActionBarColor(){
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            int backgroundColor = filteredMode ?
                    Integer.parseInt(sharedPreferences.getString("pref_action_bar_filtered_background_color", "-33024")) :
                    Integer.parseInt(sharedPreferences.getString("pref_action_bar_normal_background_color", "" + getResources().getColor(R.color.primary_material_dark)));
            boolean darkText = filteredMode ?
                    sharedPreferences.getBoolean("pref_action_bar_filtered_dark_text", true) :
                    sharedPreferences.getBoolean("pref_action_bar_normal_dark_text", false);

            actionBar.setBackgroundDrawable(new ColorDrawable(backgroundColor));
            Spannable title = new SpannableString(actionBar.getTitle());
            title.setSpan(new ForegroundColorSpan(darkText ? Color.BLACK : Color.WHITE), 0, title.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            actionBar.setTitle(title);
            if (reloadItem != null)
                reloadItem.setIcon(darkText ? R.drawable.ic_autorenew_black_36dp : R.drawable.ic_autorenew_white_36dp);
            if (overflow != null)
                overflow.setColorFilter(darkText ? Color.BLACK : Color.WHITE);
            if (filterItem != null)
                filterItem.setIcon(darkText ? R.drawable.ic_filter_list_black_36dp : R.drawable.ic_filter_list_white_36dp);
            if (markReadItem != null)
                markReadItem.setIcon(darkText ? R.drawable.ic_done_black_36dp : R.drawable.ic_done_white_36dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            case R.id.action_reload_web_view:
                startDownloadService();
                return true;
            case R.id.action_original_activity:
                startActivity(new Intent(getApplication(), WebActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            case R.id.action_about:
                new AboutDialog().show(getFragmentManager(), "AboutDialog");
                return true;
            case R.id.action_lesson_plan:
                startActivity(new Intent(getApplication(), LessonPlanActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            case R.id.action_send_debug_email:
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.debug_email_subject));
                String text = getString(R.string.debug_email_issue_description) + "\n\n" +
                        getString(R.string.debug_email_automatically_added_information) + "\n\n" +

                        getString(R.string.debug_email_pref_last_checked) + "\n" +
                        sharedPreferences.getString("pref_last_checked", "") + "\n\n\n" +
                        getString(R.string.debug_email_pref_last_update) + "\n" +
                        sharedPreferences.getString("pref_last_update", "") + "\n\n\n" +

                        getString(R.string.debug_email_pref_latest_title_1) + "\n" +
                        sharedPreferences.getString("pref_latest_title_1", "") + "\n\n\n" +
                        getString(R.string.debug_email_pref_latest_plan_1) + "\n" +
                        sharedPreferences.getString("pref_latest_plan_1", "") + "\n\n\n" +
                        getString(R.string.debug_email_pref_latest_title_2) + "\n" +
                        sharedPreferences.getString("pref_latest_title_2", "") + "\n\n\n" +
                        getString(R.string.debug_email_pref_latest_plan_2) + "\n" +
                        sharedPreferences.getString("pref_latest_plan_2", "") + "\n\n\n" +

                        getString(R.string.debug_email_pref_current_title_1) + "\n" +
                        sharedPreferences.getString("pref_current_title_1", "") + "\n\n\n" +
                        getString(R.string.debug_email_pref_current_plan_1) + "\n" +
                        sharedPreferences.getString("pref_current_plan_1", "") + "\n\n\n" +
                        getString(R.string.debug_email_pref_current_title_2) + "\n" +
                        sharedPreferences.getString("pref_current_title_2", "") + "\n\n\n" +
                        getString(R.string.debug_email_pref_current_plan_2) + "\n" +
                        sharedPreferences.getString("pref_current_plan_2", "") + "\n\n\n" +

                        getString(R.string.debug_email_pref_html_latest) + "\n" +
                        sharedPreferences.getString("pref_html_latest", "");
                intent.putExtra(Intent.EXTRA_TEXT, text);
                intent.setData(Uri.parse("mailto:"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            case R.id.action_mark_read:
                if (fragment1 != null)
                    fragment1.markChangesAsRead();
                else
                    Log.e("FormattedActivity", "action_mark_read: fragment1 == null");
                if (fragment2 != null)
                    fragment2.markChangesAsRead();
                else
                    Log.e("FormattedActivity", "action_mark_read: fragment1 == null");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void requestRecheckUnreadChanges(){
        if (fragment1 != null && fragment2 != null && markReadItem != null)
            markReadItem.setVisible(fragment1.hasUnreadContent() || fragment2.hasUnreadContent());
    }

    @Override
    public void showDialog(String text, String shareText){
        ElementDialog.newInstance(text, shareText).show(getFragmentManager(), "ElementDialog");
    }
    public void startDownloadService(){
        if (!DownloadService.isDownloading() && !sharedPreferences.getBoolean("pref_unseen_changes", false))
            startService(new Intent(this, DownloadService.class).setAction(DownloadService.ACTION_DOWNLOAD_PLAN));
    }

    public static class CustomFragmentPagerAdapter extends FragmentPagerAdapter{
        public CustomFragmentPagerAdapter (FragmentManager fragmentManager){
            super(fragmentManager);
        }
        @Override
        public int getCount(){
            return 2;
        }
        @Override
        public Fragment getItem(int position){
            if (position==0)
                return ItemFragment.newInstance(plan1, title1, 1);
            else
                return ItemFragment.newInstance(plan2, title2, 2);
        }
        @Override
        public Object instantiateItem(ViewGroup container, int position){
            ItemFragment fragment = (ItemFragment) super.instantiateItem(container, position);
            if (position==0)
                fragment1 = fragment;
            else if (position==1)
                fragment2 = fragment;
            return fragment;
        }
        @Override
        public CharSequence getPageTitle (int position){
            if (position == 0)
                return title1;
            else if (position == 1)
                return title2;
            else
                return "???";
        }
        @Override
        public void finishUpdate(ViewGroup container){
            super.finishUpdate(container);

            if (shortCutToPageTwo) {
                viewPager.setCurrentItem(1, false);
                shortCutToPageTwo = false;//use shortcut only once
            }
        }
    }

    private BroadcastReceiver downloadInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            if (action.equals("loadFragmentData")){
                Tools.setUnseenFalse(getApplicationContext());
                Calendar oldDate2;
                if (viewPager.getCurrentItem() == 1) {//When showing plan for tomorrow
                    try {
                        oldDate2 = Tools.getDateFromPlanTitle(title2);
                    } catch (Exception e) {
                        Log.e("FormattedActivity", "Got error while trying to check for page move: " + e);
                        oldDate2 = null;
                    }
                }
                else
                    oldDate2 = null;
                title1 = intent.getStringExtra("title1");
                plan1 = intent.getStringExtra("plan1");
                title2 = intent.getStringExtra("title2");
                plan2 = intent.getStringExtra("plan2");
                try {
                    date1 = Tools.getDateFromPlanTitle(title1);
                }
                catch (Exception e){
                    Log.e("FormattedActivity", "Got error while trying to extract date1 from the titles: " + e);
                    date1 = null;//Deactivate date functionality
                }
                if (oldDate2 != null && date1 != null && ((
                        date1.get(Calendar.YEAR) == oldDate2.get(Calendar.YEAR) &&
                        date1.get(Calendar.MONTH) == oldDate2.get(Calendar.MONTH) &&
                        date1.get(Calendar.DAY_OF_MONTH) == oldDate2.get(Calendar.DAY_OF_MONTH)) ||
                        date1.after(oldDate2)))
                    viewPager.setCurrentItem(0, false);//Keep showing the same day (or the nearer day)
                if (fragment1!=null)
                    fragment1.reloadContent(plan1, title1);
                if (fragment2!=null)
                    fragment2.reloadContent(plan2, title2);
                fragmentPagerAdapter.notifyDataSetChanged();
            }
            else if (action.equals("setTextViewText")){
                String text = intent.getStringExtra("text");
                textView.setText(text);
                if (text.equals(getString(R.string.loading))){
                    if (fragment1!=null)
                        fragment1.setRefreshing(true);
                    if (fragment2!=null)
                        fragment2.setRefreshing(true);
                }
                else{
                    if (fragment1!=null)
                        fragment1.setRefreshing(false);
                    if (fragment2!=null)
                        fragment2.setRefreshing(false);

                    if (sharedPreferences.getBoolean("pref_illegal_plan", false)) {
                        startActivity(new Intent(getApplication(), WebActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                        Toast.makeText(getApplicationContext(), getString(R.string.error_illegal_plan), Toast.LENGTH_LONG).show();
                    }
                }
            }
            else if (action.equals("showToast")){
                String text = intent.getStringExtra("text");
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        }
    };
}
