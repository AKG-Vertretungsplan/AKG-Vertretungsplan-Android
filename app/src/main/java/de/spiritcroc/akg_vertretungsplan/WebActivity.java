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

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

public class WebActivity extends NavigationDrawerActivity {
    private final static String cssHeader = "<style media=\"screen\" type=\"text/css\">";
    private final static String cssFoot = "</style>";

    private SharedPreferences sharedPreferences;
    private WebView webView;
    private CustomWebViewClient customWebViewClient;
    private TextView textView;
    private String css;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        initDrawer();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        webView = (WebView) findViewById(R.id.web_view);
        customWebViewClient = new CustomWebViewClient();
        webView.setWebViewClient(customWebViewClient);
        updateCss();
        loadWebView(sharedPreferences.getString(Keys.HTML_LATEST, ""));
        textView = (TextView) findViewById(R.id.text_view);
        textView.setText(sharedPreferences.getString(Keys.TEXT_VIEW_TEXT, getString(R.string.welcome)));

        LocalBroadcastManager.getInstance(this).registerReceiver(downloadInfoReceiver, new IntentFilter("PlanDownloadServiceUpdate"));

        if (!Tools.isWebActivityEnabled(sharedPreferences)) {
            // Open in browser instead in internal WebView
            openInBrowser();
            finish();
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        Tools.setUnseenFalse(this);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);

        IsRunningSingleton.getInstance().registerActivity(this);

        textView.setVisibility(sharedPreferences.getBoolean(Keys.HIDE_TEXT_VIEW, false) ? View.GONE : View.VISIBLE);

        if (updateCss()) {
            loadWebView(sharedPreferences.getString(Keys.HTML_LATEST, ""));
        }
    }
    @Override
    protected void onPause(){
        super.onPause();
        IsRunningSingleton.getInstance().unregisterActivity(this);
        webView.clearCache(true);
    }

    @Override
    protected void onDestroy(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadInfoReceiver);
        super.onDestroy();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_web, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_reload_web_view:
                startDownloadService(true);
                return true;
            case R.id.action_open_in_browser:
                openInBrowser();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openInBrowser() {
        String link;
        if (sharedPreferences.getInt(Keys.LAST_PLAN_TYPE, 1) == 2) {
            link = DownloadService.PLAN_2_ADDRESS;
        } else {
            link = DownloadService.PLAN_1_ADDRESS;
            String username = sharedPreferences.getString(Keys.USERNAME, ""),
                    password = sharedPreferences.getString(Keys.PASSWORD, "");
            String prefix = "http://";
            int index = link.indexOf(prefix);
            if (!username.equals("") && !password.equals("") && index >= 0) {
                link = link.replaceFirst(prefix, prefix + username + ":" + password + "@");
            }
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading (WebView view, String url){ //open links in default browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }

    private BroadcastReceiver downloadInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            if (action.equals("loadWebViewData")){
                Tools.setUnseenFalse(getApplicationContext());
                String data = intent.getStringExtra("data");
                updateCss();
                loadWebView(data);
            }
            else if (action.equals("setTextViewText")){
                String text = intent.getStringExtra("text");
                textView.setText(text);
            }
        }
    };

    /**
     * @return
     * Whether the CSS has changed
     */
    private boolean updateCss() {
        String oldCss = css;
        if (sharedPreferences.getBoolean(Keys.WEB_PLAN_USE_CUSTOM_STYLE, false)) {
            css = sharedPreferences.getString(Keys.WEB_PLAN_CUSTOM_STYLE, getString(R.string.web_plan_default_custom_style));
        } else {
            css = sharedPreferences.getString(Keys.CSS, getString(R.string.web_plan_custom_style_akg_default));
        }
        return oldCss == null || !oldCss.equals(css);
    }

    private void loadWebView(String data) {
        if (css != null) {
            data = cssHeader + css + cssFoot + data;
        }
        webView.loadData(data, "text/html", "utf-8");
    }
}
