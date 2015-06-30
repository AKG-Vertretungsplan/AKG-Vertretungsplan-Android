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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class FormattedActivity extends ActionBarActivity implements ItemFragment.OnFragmentInteractionListener{
    private CustomFragmentPagerAdapter fragmentPagerAdapter;
    private ViewPager viewPager;
    private static String plan1, plan2, title1, title2;
    private TextView textView;
    private SharedPreferences sharedPreferences;
    private static ItemFragment fragment1, fragment2;
    private int style;
    private boolean created = false;

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
        if (sharedPreferences.getBoolean("pref_background_service", false) && BReceiver.getAlarmPendingIntent(this, PendingIntent.FLAG_NO_CREATE) == null)
            BReceiver.startDownloadService(this);
        //Download plan stuff  start
        Calendar calendar = DownloadService.stringToCalendar(sharedPreferences.getString("pref_last_checked", "???"));
        if (calendar == null || Calendar.getInstance().getTime().getTime() - calendar.getTime().getTime() > Integer.parseInt(sharedPreferences.getString("pref_auto_load_on_open", "5"))*60000)
            startDownloadService();
        else {
            String text = sharedPreferences.getBoolean("pref_illegal_plan", false) ? getString(R.string.error_illegal_plan) : getString(R.string.last_checked) + " " + sharedPreferences.getString("pref_last_checked", getString(R.string.error_unknown));
            textView.setText(text);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("pref_text_view_text", text);
            editor.apply();
        }
        //Download plan stuff end
        created = true;

        if (sharedPreferences.getBoolean("pref_illegal_plan", false)) {
            startActivity(new Intent(getApplication(), WebActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            Toast.makeText(getApplicationContext(), getString(R.string.error_illegal_plan), Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        Tools.setUnseenFalse(this);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);

        IsRunningSingleton.getInstance().registerActivity(this);

        if (!sharedPreferences.getBoolean("pref_seen_disclaimer", false))
            new DisclaimerDialog().show(getFragmentManager(), "DisclaimerDialog");
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_formatted, menu);

        if (menu != null && style == R.style.Theme_AppCompat_Light){
            MenuItem item = menu.findItem(R.id.action_reload_web_view);
            if (item != null)
                item.setIcon(R.drawable.ic_autorenew_black_36dp);
        }
        return super.onCreateOptionsMenu(menu);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void showDialog(String text){
        ElementDialog.newInstance(text).show(getFragmentManager(), text);
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
                return fragment1 = ItemFragment.newInstance(plan1, title1, 1);
            else
                return fragment2 = ItemFragment.newInstance(plan2, title2, 2);
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
    }

    private BroadcastReceiver downloadInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            if (action.equals("loadFragmentData")){
                Tools.setUnseenFalse(getApplicationContext());
                title1 = intent.getStringExtra("title1");
                plan1 = intent.getStringExtra("plan1");
                title2 = intent.getStringExtra("title2");
                plan2 = intent.getStringExtra("plan2");
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
                }
            }
            else if (action.equals("showToast")){
                String text = intent.getStringExtra("text");
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        }
    };
}
