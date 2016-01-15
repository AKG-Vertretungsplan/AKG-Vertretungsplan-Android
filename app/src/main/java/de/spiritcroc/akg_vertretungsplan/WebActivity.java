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
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

public class WebActivity extends AppCompatActivity {
    private final static String cssHeader = "<style media=\"screen\" type=\"text/css\">";
    private final static String cssFoot = "</style>";

    private SharedPreferences sharedPreferences;
    private WebView webView;
    private CustomWebViewClient customWebViewClient;
    private TextView textView;
    private int style;
    private String css;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        style = Tools.getStyle(this);
        setTheme(style);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        webView = (WebView) findViewById(R.id.web_view);
        customWebViewClient = new CustomWebViewClient();
        webView.setWebViewClient(customWebViewClient);
        updateCss();
        loadWebView(sharedPreferences.getString("pref_html_latest", ""));
        textView = (TextView) findViewById(R.id.text_view);
        textView.setText(sharedPreferences.getString("pref_text_view_text", getString(R.string.welcome)));

        LocalBroadcastManager.getInstance(this).registerReceiver(downloadInfoReceiver, new IntentFilter("PlanDownloadServiceUpdate"));
    }
    @Override
    protected void onResume(){
        super.onResume();
        Tools.setUnseenFalse(this);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);

        IsRunningSingleton.getInstance().registerActivity(this);

        if (style != Tools.getStyle(this)) {//Theme has to be set before activity is created, so restart activity
            Intent intent = getIntent();
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }

        textView.setVisibility(sharedPreferences.getBoolean("pref_hide_text_view", false) ? View.GONE : View.VISIBLE);

        if (updateCss()) {
            loadWebView(sharedPreferences.getString("pref_html_latest", ""));
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
        if (menu != null){
            MenuItem lessonItem = menu.findItem(R.id.action_lesson_plan);
            lessonItem.setShowAsAction(sharedPreferences.getBoolean("pref_show_lesson_plan_as_action", false) ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
            if (style == R.style.Theme_AppCompat_Light) {
                MenuItem item = menu.findItem(R.id.action_reload_web_view);
                item.setIcon(R.drawable.ic_autorenew_black_36dp);
                lessonItem.setIcon(R.drawable.ic_format_list_numbered_black_36dp);
            }
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
                if (!DownloadService.isDownloading())
                    startService(new Intent(this, DownloadService.class).setAction(DownloadService.ACTION_DOWNLOAD_PLAN));
                return true;
            case R.id.action_about:
                new AboutDialog().show(getFragmentManager(), "AboutDialog");
                return true;
            case R.id.action_lesson_plan:
                startActivity(new Intent(getApplication(), LessonPlanActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            case R.id.action_open_in_browser:
                String username = sharedPreferences.getString("pref_username", ""),
                        password = sharedPreferences.getString("pref_password", "");
                String link = DownloadService.PLAN_1_ADDRESS;
                String prefix = "http://";
                int index = link.indexOf(prefix);
                if (!username.equals("") && !password.equals("") && index >= 0) {
                    link = link.replaceFirst(prefix, prefix + username + ":" + password + "@");
                }
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
            else if (action.equals("showToast")){
                String text = intent.getStringExtra("text");
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * @return
     * Whether the CSS has changed
     */
    private boolean updateCss() {
        String oldCss = css;
        if (sharedPreferences.getBoolean("pref_web_plan_use_custom_style", false)) {
            css = sharedPreferences.getString("pref_web_plan_custom_style", getString(R.string.web_plan_default_custom_style));
        } else {
            css = sharedPreferences.getString("pref_css", getString(R.string.web_plan_custom_style_akg_default));
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
