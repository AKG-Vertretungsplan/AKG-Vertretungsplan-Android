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
    private SharedPreferences sharedPreferences;
    private WebView webView;
    private CustomWebViewClient customWebViewClient;
    private TextView textView;
    private int style;


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
        webView.loadData(sharedPreferences.getString("pref_html_latest", ""), "text/html", "utf-8");
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
        getMenuInflater().inflate(R.menu.main, menu);
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
                String link = DownloadService.PLAN_ADDRESS;
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
                webView.loadData(data, "text/html", "utf-8");
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
}
