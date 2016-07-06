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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

import de.spiritcroc.akg_vertretungsplan.settings.Keys;
import de.spiritcroc.akg_vertretungsplan.settings.SettingsActivity;

public abstract class NavigationDrawerActivity extends AppCompatActivity {

    private static final String EXTRA_SWAPPED_ACTIVITY =
            "de.spiritcroc.akg_vertretungsplan.extra.swapped_activity";

    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private TextView informationView;
    private TextView formattedPlanButton;
    private TextView webPlanButton;
    private TextView lessonPlanButton;
    private TextView settingsButton;
    private TextView aboutButton;
    private TextView debugButton;

    private SharedPreferences sharedPreferences;
    private DrawerClickListener clickListener;

    private int style;
    private boolean darkActionBarText, themeDefaultDarkActionBarText;
    private int drawerActiveItemColor;
    private boolean ranAfterDisclaimer = false;

    /**
     * Call after inflating layout
     */
    protected void initDrawer() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        drawerActiveItemColor = Integer.parseInt(sharedPreferences.getString(Keys.DRAWER_ACTIVE_ITEM_TEXT_COLOR, "" + Color.YELLOW));

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        drawerToggle = new ActionBarDrawerToggle(this,
                drawerLayout,
                R.string.action_open_drawer,
                R.string.action_close_drawer) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                updateActionHomeAsUp();
            }
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
                updateActionHomeAsUp();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        clickListener = new DrawerClickListener();

        informationView = (TextView) findViewById(R.id.information_view);
        formattedPlanButton = (TextView) findViewById(R.id.formatted_activity_button);
        webPlanButton = (TextView) findViewById(R.id.web_activity_button);
        lessonPlanButton = (TextView) findViewById(R.id.lesson_activity_button);
        settingsButton = (TextView) findViewById(R.id.settings_button);
        aboutButton = (TextView) findViewById(R.id.about_button);
        debugButton = (TextView) findViewById(R.id.debug_mail_button);

        setCompoundDrawable(formattedPlanButton, R.drawable.ic_format_list_bulleted_white_24dp);
        setCompoundDrawable(webPlanButton, R.drawable.ic_web_white_24dp);
        setCompoundDrawable(lessonPlanButton, R.drawable.ic_format_list_numbered_white_24dp);
        setCompoundDrawable(settingsButton, R.drawable.ic_settings_white_24dp);
        setCompoundDrawable(aboutButton, R.drawable.ic_help_white_24dp);
        setCompoundDrawable(debugButton, R.drawable.ic_bug_report_white_24dp);

        useDrawerButton(formattedPlanButton, this instanceof FormattedActivity);
        useDrawerButton(webPlanButton, this instanceof WebActivity);
        useDrawerButton(lessonPlanButton, this instanceof LessonPlanActivity);
        useDrawerButton(settingsButton, false);
        useDrawerButton(aboutButton, false);
        useDrawerButton(debugButton, false);

        informationView.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(NavigationDrawerActivity.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent event) {
                    return true;
                }
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    Log.d("NavigationDrawer", "Swipe with y velocity " + velocityY);
                    if (velocityY > 500) {
                        startDownloadService(false);
                        return true;
                    }
                    return false;
                }
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    Toast.makeText(NavigationDrawerActivity.this, R.string.swipe_down_to_refresh, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        updateInformationViewText();
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadInfoReceiver, new IntentFilter("PlanDownloadServiceUpdate"));

        themeDefaultDarkActionBarText = !Tools.isStyleWithDarkActionBar(Tools.getStyle(this));
        darkActionBarText = themeDefaultDarkActionBarText;

        if (sharedPreferences.getBoolean(Keys.SEEN_DISCLAIMER, false)) {
            afterDisclaimer(new Bool());
        } else {
            new DisclaimerDialog().show(getFragmentManager(), "DisclaimerDialog");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        style = Tools.getStyle(this);
        setTheme(style);

        super.onCreate(savedInstanceState);

        Tools.setupUpdate(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (style != Tools.getStyle(this) ||
                Integer.parseInt(sharedPreferences.getString(Keys.DRAWER_ACTIVE_ITEM_TEXT_COLOR, "" + Color.YELLOW)) !=
                        drawerActiveItemColor) {
            finish();
            overridePendingTransition(0, 0);
            startActivity(getIntent());
            overridePendingTransition(0, 0);
        } else {

            debugButton.setVisibility(sharedPreferences.getBoolean(Keys.HIDDEN_DEBUG_ENABLED, false) && sharedPreferences.getBoolean(Keys.DEBUG_OPTION_SEND_DEBUG_MAIL, false) ?
                    View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onDestroy(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadInfoReceiver);
        super.onDestroy();
    }

    /**
     * @param startedDownloadService
     * new Bool()
     * @return
     * true if already called once and should not run any code again
     */
    public boolean afterDisclaimer(Bool startedDownloadService){
        if (ranAfterDisclaimer)//only run once
            return true;
        ranAfterDisclaimer = true;
        /*if (!sharedPreferences.contains(Keys.PLAN)) {
            new BasicSetupDialog().show(getFragmentManager(), "BasicSetupDialog");
            // BasicSetupDialog will call this method again
            return;
        }*/
        if (sharedPreferences.getString(Keys.PLAN, "1").equals("2")) {
            new InfoscreenWarningDialog().show(getFragmentManager(), "InfoscreenWarningDialog");
        } else if (!sharedPreferences.getBoolean(Keys.SEEN_GREETER, false)) {
            new GreeterDialog().show(getFragmentManager(), "GreeterDialog");
        }
        //Download plan stuff  start
        Calendar calendar = DownloadService.stringToCalendar(sharedPreferences.getString(Keys.LAST_CHECKED, "???"));
        startedDownloadService.value = false;
        if (!getIntent().getBooleanExtra(EXTRA_SWAPPED_ACTIVITY, false) &&
                (calendar == null || Calendar.getInstance().getTime().getTime() - calendar.getTime().getTime() > Integer.parseInt(sharedPreferences.getString(Keys.AUTO_LOAD_ON_OPEN, "5"))*60000)) {
            startDownloadService(false);
            startedDownloadService.value = true;
        }
        //Download plan stuff end

        if (sharedPreferences.getBoolean(Keys.ILLEGAL_PLAN, false) && !startedDownloadService.value) {
            illegalPlan();
        }

        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (drawerLayout.isDrawerOpen(getDrawer())) {
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setVisible(false);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(getDrawer())) {
                drawerLayout.closeDrawer(getDrawer());
            } else {
                drawerLayout.openDrawer(getDrawer());
            }
            return true;
        }
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private void setCompoundDrawable(TextView textView, @DrawableRes int res) {
        textView.setCompoundDrawablesWithIntrinsicBounds(res, 0, 0, 0);
        textView.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.navigation_drawer_item_compound_drawable_padding));
    }

    private void useDrawerButton(TextView v, boolean active) {
        v.setOnClickListener(clickListener);
        if (active) {
            // Indicate current activity
            v.setTextColor(drawerActiveItemColor);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            v.setBackgroundResource(R.drawable.drawer_ripple);
            Drawable[] compounds = v.getCompoundDrawables();
            if (compounds.length > 0 && compounds[0] != null) {
                compounds[0].setTint(v.getCurrentTextColor());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(getDrawer())) {
            drawerLayout.closeDrawer(getDrawer());
        } else {
            super.onBackPressed();
        }
    }

    private class DrawerClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (view.equals(formattedPlanButton)) {
                drawerLayout.closeDrawer(getDrawer());
                if (!(NavigationDrawerActivity.this instanceof FormattedActivity)) {
                    swapActivity(FormattedActivity.class);
                }
            } else if (view.equals(webPlanButton)) {
                drawerLayout.closeDrawer(getDrawer());
                if (!(NavigationDrawerActivity.this instanceof WebActivity)) {
                    swapActivity(WebActivity.class);
                }
            } else if (view.equals(lessonPlanButton)) {
                drawerLayout.closeDrawer(getDrawer());
                if (!(NavigationDrawerActivity.this instanceof LessonPlanActivity)) {
                    swapActivity(LessonPlanActivity.class);
                }
            } else if (view.equals(settingsButton)) {
                drawerLayout.closeDrawer(getDrawer());
                startActivity(new Intent(NavigationDrawerActivity.this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            } else if (view.equals(aboutButton)) {
                drawerLayout.closeDrawer(getDrawer());
                new AboutDialog().show(getFragmentManager(), "AboutDialog");
            } else if (view.equals(debugButton)) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.debug_email_subject));
                String text = getString(R.string.debug_email_issue_description) + "\n\n" +
                        getString(R.string.debug_email_automatically_added_information) + "\n\n" +

                        "-------pref_last_checked-------\n" +
                        sharedPreferences.getString(Keys.LAST_CHECKED, "") + "\n\n\n" +
                        "-------pref_last_update-------\n" +
                        sharedPreferences.getString(Keys.LAST_UPDATE, "") + "\n\n\n" +

                        "-------pref_latest_title_1-------\n" +
                        sharedPreferences.getString(Keys.LATEST_TITLE_1, "") + "\n\n\n" +
                        "-------pref_latest_plan_1-------\n" +
                        sharedPreferences.getString(Keys.LATEST_PLAN_1, "") + "\n\n\n" +
                        "-------pref_latest_title_2-------\n" +
                        sharedPreferences.getString(Keys.LATEST_TITLE_2, "") + "\n\n\n" +
                        "-------pref_latest_plan_2-------\n" +
                        sharedPreferences.getString(Keys.LATEST_PLAN_2, "") + "\n\n\n" +

                        "-------pref_current_title_1-------\n" +
                        sharedPreferences.getString(Keys.CURRENT_TITLE_1, "") + "\n\n\n" +
                        "-------pref_current_plan_1-------\n" +
                        sharedPreferences.getString(Keys.CURRENT_PLAN_1, "") + "\n\n\n" +
                        "-------pref_current_title_2-------\n" +
                        sharedPreferences.getString(Keys.CURRENT_TITLE_2, "") + "\n\n\n" +
                        "-------pref_current_plan_2-------\n" +
                        sharedPreferences.getString(Keys.CURRENT_PLAN_2, "") + "\n\n\n" +

                        "-------pref_html_latest-------\n" +
                        sharedPreferences.getString(Keys.HTML_LATEST, "") + "\n\n\n" +

                        "-------Log-------\n" +
                        OwnLog.getFull(sharedPreferences);

                intent.putExtra(Intent.EXTRA_TEXT, text);
                intent.setData(Uri.parse("mailto:"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    protected void swapActivity(Class<?> newActivity) {
        finish();
        overridePendingTransition(0, 0);
        startActivity(new Intent(this, newActivity).putExtra(EXTRA_SWAPPED_ACTIVITY, true));
        overridePendingTransition(0, 0);
    }

    protected int getStyle() {
        return style;
    }

    private void updateActionHomeAsUp() {
        // Use the inbuilt indicator if the themed color matches the current one
        drawerToggle.setDrawerIndicatorEnabled(darkActionBarText == themeDefaultDarkActionBarText);
        boolean drawerOpen = drawerLayout.isDrawerOpen(getDrawer());
        if (darkActionBarText) {
            drawerToggle.setHomeAsUpIndicator(drawerOpen ? R.drawable.ic_arrow_back_black_24dp : R.drawable.ic_menu_black_24dp);
        } else {
            drawerToggle.setHomeAsUpIndicator(drawerOpen ? R.drawable.ic_arrow_back_white_24dp : R.drawable.ic_menu_white_24dp);
        }
    }

    protected void updateActionHomeAsUp(boolean darkText) {
        darkActionBarText = darkText;
        updateActionHomeAsUp();
    }

    private View getDrawer() {
        return drawerLayout.getChildAt(1);
    }

    private void updateInformationViewText() {
        ArrayList<String> relevantInformation = new ArrayList<>();
        ArrayList<String> generalInformation = new ArrayList<>();
        ArrayList<String> irrelevantInformation = new ArrayList<>();
        ArrayList<String> allRelevant = new ArrayList<>();
        ArrayList<String> allGeneral = new ArrayList<>();
        ArrayList<String> allIrrelevant = new ArrayList<>();
        DownloadService.getNewRelevantInformationCount(this, relevantInformation, generalInformation, irrelevantInformation, allRelevant, allGeneral, allIrrelevant);
        String message = getString(R.string.information_view_update_check,
                Tools.shortestTime(sharedPreferences.getString("pref_last_update", getString(R.string.never))),
                Tools.shortestTime(sharedPreferences.getString("pref_last_checked", getString(R.string.never))));
        if (LessonPlan.getInstance(sharedPreferences).isConfigured()) {
            message += "\n" + getString(R.string.information_view_information_relevancy,
                    relevantInformation.size(), allRelevant.size(),
                    generalInformation.size(), allGeneral.size(),
                    irrelevantInformation.size(), allIrrelevant.size());
        } else {
            message += "\n" +  getString(R.string.information_view_information,
                    irrelevantInformation.size(), allIrrelevant.size());
        }

        if (DownloadService.isDownloading()) {
            message = getString(R.string.loading) + "\n" + message;
        }
        if (sharedPreferences.getBoolean(Keys.LAST_OFFLINE, false)) {
            message = getString(R.string.offline) + "\n" + message;
        }

        informationView.setText(message);
    }

    /**
     * @param force
     * Whether to start download service if there are unseen changes
     */
    public void startDownloadService(boolean force){
        if (!DownloadService.isDownloading() && (force || !sharedPreferences.getBoolean(Keys.UNSEEN_CHANGES, false)))
            startService(new Intent(this, DownloadService.class).setAction(DownloadService.ACTION_DOWNLOAD_PLAN));
    }

    protected void illegalPlan() {}

    private BroadcastReceiver downloadInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            if (action.equals("showToast")){
                String text = intent.getStringExtra("text");
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            } else if (action.equals("updateNavigationDrawerInformation")) {
                updateInformationViewText();
            } else if (action.equals("updateLoadingInformation")) {
                updateInformationViewText();
            }
        }
    };
}
