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

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class DownloadService extends IntentService {
    public static final String ACTION_DOWNLOAD_PLAN = "de.spiritcroc.akg_vertretungsplan.action.downloadPlan";
    public static final String ACTION_RETRY = "de.spiritcroc.akg_vertretungsplan.action.retry";

    public static final String PLAN_1_ADDRESS = "http://www.sc.shuttle.de/sc/akg/Ver/";
    public static final String PLAN_2_ADDRESS = "https://akg-sc.selfip.org/INFOSCREEN/";
    public static final String PLAN_2_ADDRESS_1 = "https://akg-sc.selfip.org/INFOSCREEN/links.html?";
    public static final String PLAN_2_ADDRESS_2 = "https://akg-sc.selfip.org/INFOSCREEN/rechts.html?";
    public static final String CSS_ADDRESS = "http://www.sc.shuttle.de/sc/akg/Ver/willi.css";

    public static final String NO_PLAN = "**null**";

    public static final int NOTIFICATION_IMPORTANCE_RELEVANT = 3;
    public static final int NOTIFICATION_IMPORTANCE_GENERAL = 2;
    public static final int NOTIFICATION_IMPORTANCE_IRRELEVANT = 1;
    public static final int NOTIFICATION_IMPORTANCE_NONE = 0;

    public enum ContentType {AWAIT, IGNORE, HEADER, TABLE_START_FLAG, TABLE_END_FLAG, TABLE_ROW, TABLE_CONTENT}
    private String username, password;
    private final String cssHeader = "<style media=\"screen\" type=\"text/css\">";
    private final String cssFoot = "</style>";
    private SharedPreferences sharedPreferences;
    private boolean loginFailed = false;
    private static boolean downloading = false;
    private boolean skipLoginActivity = false;

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD_PLAN.equals(action)){
                if (getSharedPreferences().getBoolean("pref_reload_on_resume", false))
                    skipLoginActivity = true;// Don't prompt to login activity if coming from there
                getSharedPreferences().edit().putBoolean("pref_reload_on_resume", false)
                        .putBoolean("pref_last_offline", false).apply();

                if (sharedPreferences.getBoolean("pref_background_service", true))
                    BReceiver.startDownloadService(getApplicationContext(), false);//Schedule next download

                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    if (!loginFailed) {
                        downloading = true;
                        //hidden debug stuff start
                        int tmp = 0;
                        try {
                            tmp = Integer.parseInt(getSharedPreferences().getString("pref_debugging_enabled", "0"));
                        } catch (Exception e) {
                        }

                        if (tmp != 0)
                            dummyHandleActionDownloadPlan(tmp);
                        else//hidden debug stuff end
                            handleActionDownloadPlan();
                        downloading = false;
                    }
                }
                else{
                    getSharedPreferences().edit().putBoolean("pref_last_offline", true).apply();
                    loadOfflinePlan();
                }
            }
            else if (ACTION_RETRY.equals(action)){
                loginFailed = false;
            }
        }
    }
    private SharedPreferences getSharedPreferences(){
        if (sharedPreferences == null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences;
    }
    public static boolean isDownloading(){
        return downloading;
    }

    private void handleActionDownloadPlan() {
        setTextViewText(getString(R.string.loading));
        Tools.updateWidgets(this);
        maybeSaveFormattedPlan();
        // Dismiss wrong userdata notifications
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(2);
        try {
            //int plan = Integer.parseInt(getSharedPreferences().getString("pref_plan", "1"));
            //switch (plan) {
            //    case 1:
            //    {
                    username = getSharedPreferences().getString("pref_username", "");
                    password = getSharedPreferences().getString("pref_password", "");
                    String base64EncodedCredentials = Base64.encodeToString((username + ":" + password).getBytes("US-ASCII"), Base64.URL_SAFE | Base64.NO_WRAP);
                    DefaultHttpClient httpClient = new DefaultHttpClient();//On purpose use deprecated stuff because it works better (I have problems with HttpURLConnection: it does not read the credentials each time they are needed, and if they are wrong, there is no appropriate message (just an java.io.FileNotFoundException))
                    HttpGet httpGet = new HttpGet(PLAN_1_ADDRESS);
                    httpGet.setHeader("Authorization", "Basic " + base64EncodedCredentials);
                    String result = EntityUtils.toString(httpClient.execute(httpGet).getEntity());
                    httpGet = new HttpGet(CSS_ADDRESS);
                    httpGet.setHeader("Authorization", "Basic " + base64EncodedCredentials);
                    String css = EntityUtils.toString(httpClient.execute(httpGet).getEntity());
                    processPlan(cssHeader + css + cssFoot + result);
            //      break;
            //    }
            //    case 2:
            //    {
            //        String result = getPlanInsecure(PLAN_2_ADDRESS_1) + getPlanInsecure(PLAN_2_ADDRESS_2);
            //        processPlan(result);
            //        break;
            //    }
            //}
            getSharedPreferences().edit().putInt("last_plan_type", 1).apply();
        }
        catch (UnknownHostException e){
            loadOfflinePlan();
        }
        catch (IOException e){
            Log.e("downloadPlan", "Got Exception " + e);
            showText(getString(R.string.error_download_error));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private String getPlanInsecure(String address) {
        // The infoscreen site does not provide a verified certificate, but uses https
        String result = "";
        HttpURLConnection connection;
        TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers(){
                return null;
            }
            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException{}
            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException{}
        }};
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new java.security.SecureRandom());
            URL url = new URL(address);
            URLConnection urlConnection = url.openConnection();
            if (urlConnection instanceof HttpsURLConnection){
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) urlConnection;
                httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
                connection = httpsURLConnection;
            } else {
                connection = (HttpURLConnection) urlConnection;
            }
            try {
                int buffer;
                InputStream in = new BufferedInputStream(connection.getInputStream());
                while (true) {
                    buffer = in.read();

                    if (buffer == -1) {
                        break;
                    }
                    result += (char) buffer;
                }
                in.close();
            } finally {
                connection.disconnect();
            }
        }  catch (IOException |NoSuchAlgorithmException |KeyManagementException e) {
            Log.i("DownloadService", "getPlanInsecure(" + address + "): Got exception " + e);
        }
        return result;
    }

    private void maybeSaveFormattedPlan(){
        if (!getSharedPreferences().getBoolean("pref_unseen_changes", false) && getSharedPreferences().getString("pref_auto_mark_read", "").equals("onPlanReloaded")){
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putString("pref_latest_title_1", getSharedPreferences().getString("pref_current_title_1", ""));
            editor.putString("pref_latest_plan_1", getSharedPreferences().getString("pref_current_plan_1", ""));
            editor.putString("pref_latest_title_2", getSharedPreferences().getString("pref_current_title_2", ""));
            editor.putString("pref_latest_plan_2", getSharedPreferences().getString("pref_current_plan_2", ""));
            editor.apply();
        }
    }

    private void processPlan(String result){
        String latestHtml = getSharedPreferences().getString("pref_html_latest", "");
        boolean newVersion = false;
        if (result.contains("401 Authorization Required")) {
            showText((username.equals("") ? getString(R.string.enter_userdata) : getString(R.string.correct_userdata)));
            loginFailed = true;
            if (IsRunningSingleton.getInstance().isRunning()) {
                startLoginActivity();
            } else {
                postLoginNotification();
            }
        } else {
            String time = timeAndDateToString(Calendar.getInstance());
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putBoolean("pref_illegal_plan", false).apply();//there are some textView updates below, and we don't know yet whether plan is illegal, so just pretend it is not for now and handle stuff later
            editor.putString("pref_last_checked", time);
            String latestContent = getContentFromHtml(latestHtml),
                    currentContent = getContentFromHtml(result);
            if (latestHtml.equals("") || !currentContent.equals(latestContent)) {    //site has changed or nothing saved yet
                if (newVersionNotify(latestContent, currentContent)){   //are new entries available?
                    newVersion = true;
                    setTextViewText(getString(R.string.new_version));
                    editor.putString("pref_last_update", time);
                    editor.putBoolean("pref_unseen_changes", true);
                }
                else{
                    if (Integer.parseInt(getSharedPreferences().getString("pref_no_change_since_max_precision", "2")) > 0)
                        setTextViewText(getString(R.string.no_change_for) + " " + compareTime(stringToCalendar(getSharedPreferences().getString("pref_last_update", "???")), Calendar.getInstance()));
                    else
                        setTextViewText(getString(R.string.no_change));
                }
                editor.putString("pref_html_latest", result);
            }
            else {
                if (Integer.parseInt(getSharedPreferences().getString("pref_no_change_since_max_precision", "2")) > 0)
                    setTextViewText(getString(R.string.no_change_for) + " " + compareTime(stringToCalendar(getSharedPreferences().getString("pref_last_update", "???")), Calendar.getInstance()));
                else
                    setTextViewText(getString(R.string.no_change));
            }
            editor.apply();
            loadWebViewData(result);
            if (!loadFormattedPlans(result))
                setTextViewText(getString(R.string.error_illegal_plan));
            if (newVersion) {//Notification after loadFormattedPlans, because getNewRelevantInformationCount depends on it
                ArrayList<String> relevantInformation = new ArrayList<>();
                ArrayList<String> generalInformation = new ArrayList<>();
                ArrayList<String> irrelevantInformation = new ArrayList<>();
                int newRelevantNotificationCount = getNewRelevantInformationCount(this, relevantInformation, generalInformation, irrelevantInformation);

                String message = (newRelevantNotificationCount > 0 ? getResources().getQuantityString(R.plurals.new_relevant_information, newRelevantNotificationCount, newRelevantNotificationCount) :
                        (generalInformation.size() > 0 ?  getResources().getQuantityString(R.plurals.new_general_information, generalInformation.size(), generalInformation.size()) :
                                (irrelevantInformation.size() > 0 ?  getResources().getQuantityString(R.plurals.new_irrelevant_information, irrelevantInformation.size(), irrelevantInformation.size()) :
                        getString(R.string.last_checked) + " " + time)));
                if (!sharedPreferences.getBoolean("pref_notification_only_if_relevant", false) || newRelevantNotificationCount > 0
                        || (!sharedPreferences.getBoolean("pref_notification_general_not_relevant", false) && generalInformation.size() > 0)) {
                    int importance = newRelevantNotificationCount > 0 ? NOTIFICATION_IMPORTANCE_RELEVANT :
                            (generalInformation.size() > 0 ? NOTIFICATION_IMPORTANCE_GENERAL :
                                    (irrelevantInformation.size() > 0 ? NOTIFICATION_IMPORTANCE_IRRELEVANT : NOTIFICATION_IMPORTANCE_NONE));

                    maybePostNotification(getString(R.string.new_version), message, importance, relevantInformation, generalInformation, irrelevantInformation);
                }
                if (newRelevantNotificationCount > 0 || generalInformation.size() > 0 || irrelevantInformation.size() > 0)
                    setTextViewText(message);

                if (sharedPreferences.getBoolean("pref_tesla_unread_enable", true)){
                    int fullCount = newRelevantNotificationCount;
                    if (sharedPreferences.getBoolean("pref_tesla_unread_use_complete_count", false))
                        fullCount += generalInformation.size() + irrelevantInformation.size();
                    else if (sharedPreferences.getBoolean("pref_tesla_unread_include_general_information_count", true))
                        fullCount += generalInformation.size();
                    if (!IsRunningSingleton.getInstance().isRunning()){
                        try{
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("tag", "de.spiritcroc.akg_vertretungsplan/.FormattedActivity");
                            contentValues.put("count", fullCount);
                            getContentResolver().insert(Uri.parse("content://com.teslacoilsw.notifier/unread_count"), contentValues);
                        }
                        catch (IllegalArgumentException e){
                            Log.d("DownloadService", "TeslaUnread is not installed");
                        }
                        catch (Exception e){
                            Log.e("DownloadService", "Got exception while trying to sending count to TeslaUnread: " + e);
                        }
                    }
                }
            }
        }
        Tools.updateWidgets(this);
    }
    private void loadOfflinePlan(){
        String latestHtml = getSharedPreferences().getString("pref_html_latest", "");
        if (latestHtml.equals(""))  //first download
            setTextViewText(getString(R.string.error_could_not_load));
        else {
            setTextViewText(getString(R.string.showing_offline) + " (" + getSharedPreferences().getString("pref_last_checked", "???") + ")");
            //loadWebViewData(latestHtml);  //no need for loading again, as already used on creation of activities
            //getPlans(latestHtml);
            Tools.updateWidgets(this);
        }
    }
    private void loadWebViewData(String data){
        Intent intent = new Intent("PlanDownloadServiceUpdate");
        intent.putExtra("action", "loadWebViewData");
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    private void loadFragmentData(String title1, String plan1, String title2, String plan2){
        Intent intent = new Intent("PlanDownloadServiceUpdate");
        intent.putExtra("action", "loadFragmentData");
        intent.putExtra("title1", title1);
        intent.putExtra("plan1", plan1);
        intent.putExtra("title2", title2);
        intent.putExtra("plan2", plan2);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    private void setTextViewText(String text){
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString("pref_text_view_text", text);
        editor.apply();
        Intent intent = new Intent("PlanDownloadServiceUpdate");
        intent.putExtra("action", "setTextViewText");
        intent.putExtra("text", text);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    private void showText(String text){   //toast+textView
        Intent intent = new Intent("PlanDownloadServiceUpdate");
        intent.putExtra("action", "showToast");
        intent.putExtra("text", text);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        setTextViewText(text);
    }

    private void startLoginActivity() {
        if (skipLoginActivity) {
            // Only skip once
            skipLoginActivity = false;
        } else {
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            getSharedPreferences().edit().putBoolean("pref_reload_on_resume", true).apply();//reload on resume of FormattedActivity
        }
    }
    private void postLoginNotification() {
        postNotification(getString(R.string.wrong_userdata), getString(R.string.enter_userdata), 2, R.drawable.ic_stat_notify_wrong_credentials, SettingsActivity.class, true, NOTIFICATION_IMPORTANCE_NONE, null, null, null);
    }

    public static String timeAndDateToString (Calendar calendar){
        String minute = "" + calendar.get(Calendar.MINUTE);
        while (minute.length() < 2)
            minute = "0"+minute;
        String second = "" + calendar.get(Calendar.SECOND);
        while (second.length() < 2)
            second = "0"+second;
        return (calendar.get(Calendar.DAY_OF_MONTH)+"."+(calendar.get(Calendar.MONTH)+1)+"."+calendar.get(Calendar.YEAR) + " " + calendar.get(Calendar.HOUR_OF_DAY)+":"+minute+":"+second);
    }
    public static GregorianCalendar stringToCalendar(String string){
        String separated[] = {"", "", "", "", "", ""};
        int index = 0;
        boolean row = false;
        for (int i = 0; i < string.length() && index < separated.length; i++){
            if (string.charAt(i)>='0'&&string.charAt(i)<='9'){
                row = true;
                separated[index] += string.charAt(i);
            }
            else {
                if (row)
                    index++;
                row = false;
            }
        }
        try {
            return new GregorianCalendar(Integer.parseInt(separated[2]), Integer.parseInt(separated[1]) - 1, Integer.parseInt(separated[0]), Integer.parseInt(separated[3]), Integer.parseInt(separated[4]), Integer.parseInt(separated[5]));
        }
        catch (Exception e){
            Log.e("stringToCalendar", "" + e);
            return null;
        }
    }
    private String compareTime(Calendar first, Calendar second){
        if (first == null || second == null){
            Log.e("compareTime", "calendar == null");
            return getString(R.string.error_unknown);
        }
        int precision = 0;
        long difference = (second.getTime().getTime() - first.getTime().getTime())/1000;       //result: difference in seconds | yes, the .getTime().getTime() is on purpose
        if (difference < 0)
            difference *= -1;
        String[] everythingIn = new String[4];  //seconds, minutes, hours, days

        long tmp = difference % 60; //→seconds
        difference /= 60;   //→minutes
        if (tmp == 0)
            everythingIn[0] = "";
        else {
            everythingIn[0] = getResources().getQuantityString(R.plurals.plural_second, (int) tmp, (int) tmp);
            precision = 1;
        }

        tmp = difference % 60;  //→minutes
        difference /= 60;   //→hours
        if (tmp == 0)
            everythingIn[1] = "";
        else {
            everythingIn[1] = getResources().getQuantityString(R.plurals.plural_minute, (int) tmp, (int) tmp);
            precision = 2;
        }

        tmp = difference % 24;  //→hours
        difference /= 24;   //→days
        if (tmp == 0)
            everythingIn[2] = "";
        else {
            everythingIn[2] = getResources().getQuantityString(R.plurals.plural_hour, (int) tmp, (int) tmp);
            precision = 3;
        }

        if (difference == 0)
            everythingIn[3] = "";
        else {
            everythingIn[3] = getResources().getQuantityString(R.plurals.plural_day, (int) difference, (int) difference);
            precision = 4;
        }

        //shorten due to maxPrecision
        for (int i = 0; precision > Integer.parseInt(getSharedPreferences().getString("pref_no_change_since_max_precision", "2")) && i < everythingIn.length; i++){
            precision--;
            everythingIn[i] = "";
        }
        String result = (everythingIn[3].length()!=0 ? everythingIn[3] + " " : "") + (everythingIn[2].length()!=0 ? everythingIn[2] + " " : "") + (everythingIn[1].length()!=0 ? everythingIn[1] + " " : "") + (everythingIn[0].length()!=0 ? everythingIn[0] + " " : "");
        if (result.charAt(result.length()-1) == ' ')
            result = result.substring(0, result.length()-1);
        return result;
    }
    private String convertSpecialChars(String html){
        html = html.replace("&quot;", "\"");
        html = html.replace("&amp;", "&");
        html = html.replace("&lt;", "<");
        html = html.replace("&gt;", ">");
        html = html.replace("&nbsp;", " ");
        html = html.replace("&Auml;", "Ä");
        html = html.replace("&Ouml;", "Ö");
        html = html.replace("&Uuml;", "Ü");
        html = html.replace("&auml;", "ä");
        html = html.replace("&ouml;", "ö");
        html = html.replace("&uuml;", "ü");
        html = html.replace("&szlig;", "ß");  //add more if needed: http://de.selfhtml.org/html/referenz/zeichen.htm

        /*html = html.replace("&Agrave;", "À");     //unneeded, automatically translates unlike German Umlaute
        html = html.replace("&Egrave;", "È");
        html = html.replace("&Igrave;", "Ì");
        html = html.replace("&Ograve;", "Ò");
        html = html.replace("&Ugrave;", "Ù");
        html = html.replace("&agrave;", "à");
        html = html.replace("&egrave;", "è");
        html = html.replace("&igrave;", "ì");
        html = html.replace("&ograve;", "ò");
        html = html.replace("&ugrave;", "ù");

        html = html.replace("&Aacute;", "Á");
        html = html.replace("&Eacute;", "É");
        html = html.replace("&Iacute;", "Í");
        html = html.replace("&Oacute;", "Ó");
        html = html.replace("&Uacute;", "Ú");
        html = html.replace("&aacute;", "á");
        html = html.replace("&eacute;", "é");
        html = html.replace("&iacute;", "í");
        html = html.replace("&oacute;", "ó");
        html = html.replace("&uacute;", "ú");

        html = html.replace("&Acirc;", "Â");
        html = html.replace("&Ecirc;", "Ê");
        html = html.replace("&Icirc;", "Î");
        html = html.replace("&Ocirc;", "Ô");
        html = html.replace("&Ucirc;", "Û");
        html = html.replace("&acirc;", "â");
        html = html.replace("&ecirc;", "ê");
        html = html.replace("&icirc;", "î");
        html = html.replace("&ocirc;", "ô");
        html = html.replace("&ucirc;", "û");*/
        return html;
    }
    private boolean loadFormattedPlans(String html){
        String title1, plan1, title2, plan2;
        plan1 = convertSpecialChars(getContentFromHtml(html));

        SharedPreferences.Editor editor = getSharedPreferences().edit();

        //remove spacers from empty cells
        for (int i = 0; i < plan1.length()-1; i++){
            if ((plan1.charAt(i)=='¡' || plan1.charAt(i)=='¿') && plan1.charAt(i+1) == ' ')
                plan1=plan1.substring(0,i+1)+plan1.substring(i+2);
        }

        String searchingFor = ContentType.TABLE_START_FLAG + "¡Vertretungsplan für ";
        int index = plan1.indexOf(searchingFor);
        if (index == -1){
            Log.e("getHeadsAndDivide", "Could not find " + searchingFor);
            editor.putBoolean("pref_illegal_plan", true).apply();
            return false;
        }
        else
            title1 = Tools.getLine(plan1.substring(index + searchingFor.length()), 1);

        searchingFor = ContentType.TABLE_START_FLAG + "¡Vertretungsplan für ";
        index = plan1.indexOf(searchingFor, index + searchingFor.length());
        if (index == -1){
            Log.d("getHeadsAndDivide", "Could not find " + searchingFor + " twice");
            title2 = NO_PLAN;
            plan2 = NO_PLAN;
        }
        else {
            title2 = Tools.getLine(plan1.substring(index + searchingFor.length()), 1);
            plan2 = plan1.substring(index);
            plan1 = plan1.substring(0, index);
        }
        editor.putString("pref_current_title_1", title1);
        editor.putString("pref_current_plan_1", plan1);
        editor.putString("pref_current_title_2", title2);
        editor.putString("pref_current_plan_2", plan2);
        editor.putBoolean("pref_illegal_plan", false);
        editor.apply();
        loadFragmentData(title1, plan1, title2, plan2);
        return true;
    }

    //output format: type, specific content: normal spacer = ¡, fat content spacer: ¿ \n
    private String getContentFromHtml (String html){
        boolean bracketsOpen = false, newRowspan=false;
        String tmpHeader = "", tmpRowSpanningHeader="";
        int rowSpanNumber = 0;
        ContentType contentType = ContentType.AWAIT;
        String result = "", bracketContent = "";
        for (int index = 0; index < html.length(); index++){
            if (html.charAt(index) == '<'){
                bracketsOpen = true;
                bracketContent = "" + html.charAt(index);
            }
            else if (bracketsOpen && html.charAt(index) == '>') {
                bracketsOpen = false;
                bracketContent += html.charAt(index);

                //use bracket-information
                if (bracketContent.length() >= 2 && bracketContent.substring(0,2).equals("<a"))             //ignore links
                    contentType = ContentType.IGNORE;
                else if (bracketContent.equals("</a>"))
                    contentType = ContentType.AWAIT;
                else if (bracketContent.length() == 4 && bracketContent.substring(0,2).equals("<h")) {      //header=readable name for table
                    contentType = ContentType.HEADER;
                    tmpHeader = "";
                }
                else if (bracketContent.length() == 5 && bracketContent.substring(0,3).equals("</h"))       //header end
                    contentType = ContentType.AWAIT;
                else if (bracketContent.length() >= 7 && bracketContent.substring(0,6).equals("<table"))    //table
                    result += ContentType.TABLE_START_FLAG + "¡" + tmpHeader + "\n";
                    //else if (bracketContent.length() >= 8 && bracketContent.substring(0,7).equals("</table"))   //table end
                    //result += contentType.TABLE_END_FLAG + "\n";      //TABLE_END_FLAG not needed anymore
                else if (bracketContent.length() >= 4 && bracketContent.substring(0,3).equals("<tr")) {     //table row
                    result += ContentType.TABLE_ROW;
                    if (rowSpanNumber>0){
                        result += "¿" + tmpRowSpanningHeader;
                        rowSpanNumber--;
                    }
                }
                else if (bracketContent.equals("</tr>")){                                                   //table row end
                    result += "\n";
                }
                else if (bracketContent.length() >= 4 && bracketContent.substring(0,3).equals("<th")) {      //table header
                    int rowspan = bracketContent.indexOf("rowspan");
                    if (rowspan != -1){
                        rowspan = bracketContent.indexOf('"', rowspan)+1;
                        int end = bracketContent.indexOf('"', rowspan);
                        if (rowspan==-1 || end==-1){
                            Log.e("getContentFromHtml: ", "Could find String rowspan, but not the value");
                            break;
                        }
                        rowSpanNumber = Integer.parseInt(bracketContent.substring(rowspan, end))-1;
                        newRowspan = true;
                        tmpRowSpanningHeader = "";
                    }
                    contentType = ContentType.TABLE_CONTENT;
                    result += "¿";
                }
                else if (bracketContent.equals("</th>")) {                                                   //table header end
                    contentType = ContentType.AWAIT;
                    newRowspan = false;
                }
                else if (bracketContent.length() >= 4 && bracketContent.substring(0,3).equals("<td")){      //table data
                    contentType = ContentType.TABLE_CONTENT;
                    result += "¡";
                }
                else if (bracketContent.equals("</td>"))                                                    //table data end
                    contentType = ContentType.AWAIT;

            }
            else if (bracketsOpen){
                bracketContent += html.charAt(index);
            }
            else if (contentType.equals(ContentType.HEADER))
                tmpHeader += html.charAt(index);
            else if (contentType.equals(ContentType.TABLE_CONTENT) && html.charAt(index)!='¿' && (html.charAt(index) >= ' ' && html.charAt(index) <= '~' || html.charAt(index) >= '¡')) {
                result += html.charAt(index);

                if (newRowspan)
                    tmpRowSpanningHeader += html.charAt(index);
            }
        }
        return result;
    }

    private boolean newVersionNotify(String latestContent, String currentContent){
        if (latestContent.length() != currentContent.length())
            return true;
        String tmp = "a";   //not empty
        for (int i = 0; !tmp.equals(""); i++) {
            tmp = Tools.getLine(currentContent, i + 1);
            if (!Tools.lineAvailable(latestContent, tmp)) {//new plan available
                if ("".equals(Tools.getCellContent(tmp, 2))){//if only one cell
                    if (!Tools.ignoreSubstitution(Tools.getCellContent(tmp, 1)))
                        return true;
                }
                else
                    return true;
            }
        }
        return false;
    }

    private void maybePostNotification(String title, String text, int importance, ArrayList<String> relevantInformation, ArrayList<String> generalInformation, @Nullable ArrayList<String> irrelevantInformation) {
        if (!IsRunningSingleton.getInstance().isRunning() && getSharedPreferences().getBoolean("pref_notification_enabled", false))
            postNotification(title, text, 1, R.drawable.ic_stat_notify_plan_update, FormattedActivity.class, false, importance, relevantInformation, generalInformation, irrelevantInformation);
    }
    private void postNotification(String title, @Nullable String text, int id, int smallIconResource, Class touchActivity, boolean silent, int importance, @Nullable ArrayList<String> relevantInformation, @Nullable ArrayList<String> generalInformation, @Nullable ArrayList<String> irrelevantInformation) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(smallIconResource).setContentTitle(title);
        if (text != null)
            builder.setContentText(text);
        if (!silent) {
            if (getSharedPreferences().getBoolean("pref_notification_sound_enabled", false)) {
                String notificationSound = getSharedPreferences().getString("pref_notification_sound", "");
                if (notificationSound.equals(""))
                    builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                else
                    builder.setSound(Uri.parse(notificationSound));
            }
            if (getSharedPreferences().getBoolean("pref_led_notification_enabled", false))
                builder.setLights(Integer.parseInt(getSharedPreferences().getString("pref_led_notification_color", "-1")), 500, 500);
            if (getSharedPreferences().getBoolean("pref_vibrate_notification_enabled", false)) {
                long[] vibrationPattern;
                switch (importance) {
                    case NOTIFICATION_IMPORTANCE_RELEVANT:
                        vibrationPattern = new long[] {50, 500, 250, 500};
                        break;
                    case NOTIFICATION_IMPORTANCE_GENERAL:
                        vibrationPattern = new long[] {50, 500};
                        break;
                    case NOTIFICATION_IMPORTANCE_IRRELEVANT:
                        vibrationPattern = new long[] {50, 200};
                        break;
                    default:
                    case NOTIFICATION_IMPORTANCE_NONE:
                        vibrationPattern = new long[] {50, 100};
                        break;
                }
                builder.setVibrate(vibrationPattern);
            }
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            boolean addedItems = false;
            if (relevantInformation != null) {
                addedItems = !relevantInformation.isEmpty();
                for (int i = 0; i < relevantInformation.size(); i++) {
                    inboxStyle.addLine(relevantInformation.get(i));
                }
            }
            if (!addedItems && generalInformation != null) {
                addedItems = !generalInformation.isEmpty();
                for (int i = 0; i < generalInformation.size(); i++) {
                    inboxStyle.addLine(generalInformation.get(i));
                }
            }
            if (!addedItems && irrelevantInformation != null) {
                for (int i = 0; i < irrelevantInformation.size(); i++) {
                    inboxStyle.addLine(irrelevantInformation.get(i));
                }
            }

            if (text != null)
                inboxStyle.setSummaryText(text);
            builder.setStyle(inboxStyle);
        }

        Intent resultIntent = new Intent(this, touchActivity);
        android.support.v4.app.TaskStackBuilder stackBuilder = android.support.v4.app.TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    public static int getNewRelevantInformationCount(Context context, ArrayList<String> relevantInformation, ArrayList<String> generalInformation, ArrayList<String> irrelevantInformation){
        relevantInformation.clear();
        generalInformation.clear();
        irrelevantInformation.clear();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int count = getNewRelevantInformationCount(context, sharedPreferences.getString("pref_current_plan_1", ""), sharedPreferences.getString("pref_current_title_1", ""), relevantInformation, generalInformation, irrelevantInformation) +
                getNewRelevantInformationCount(context, sharedPreferences.getString("pref_current_plan_2", ""), sharedPreferences.getString("pref_current_title_2", ""), relevantInformation, generalInformation, irrelevantInformation);
        if (!LessonPlan.getInstance(sharedPreferences).isConfigured()){
            ArrayList<String> irrelevantCopy = (ArrayList<String>) irrelevantInformation.clone();
            irrelevantInformation.clear();
            irrelevantInformation.addAll(generalInformation);
            irrelevantInformation.addAll(irrelevantCopy);
            generalInformation.clear();
            count = 0;
        }
        return count;
    }
    private static int getNewRelevantInformationCount(Context context, String currentContent, String title, ArrayList<String> relevantInformation, ArrayList<String> generalInformation, ArrayList<String> irrelevantInformation) {
        if (title.equals(NO_PLAN)) {
            return 0;
        }
        String latestContent;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (title.equals(sharedPreferences.getString("pref_latest_title_1", "")))    //check date for comparison
            latestContent = sharedPreferences.getString("pref_latest_plan_1", "");
        else if (title.equals(sharedPreferences.getString("pref_latest_title_2", "")))
            latestContent = sharedPreferences.getString("pref_latest_plan_2", "");
        else
            latestContent = "";

        Calendar calendar = Tools.getDateFromPlanTitle(title);
        if (calendar == null){
            Log.e("DownloadService", "getNewRelevantInformationCount: Could not read calendar " + title);
            return 0;
        }

        int count = 0, tmpCellCount;
        String tmp = "a";   //not empty
        String[] tmpRowContent = new String[ItemFragment.cellCount];
        LessonPlan lessonPlan = LessonPlan.getInstance(sharedPreferences);
        for (int i = 1; !tmp.equals(""); i++) {
            tmp = Tools.getLine(currentContent, i);
            if (!Tools.lineAvailable(latestContent, tmp)) {//new information
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
                                    generalInformation.add(ItemFragment.createItem(context, tmpRowContent, true));
                                }
                            } else {
                                generalInformation.add(ItemFragment.createItem(context, tmpRowContent, false));
                            }
                        }
                        else {
                            try {
                                if (lessonPlan.isRelevant(tmpRowContent[0], calendar.get(Calendar.DAY_OF_WEEK), Integer.parseInt(tmpRowContent[2]), tmpRowContent[1])) {
                                    count++;
                                    relevantInformation.add(ItemFragment.createItem(context, tmpRowContent, false));
                                } else {
                                    irrelevantInformation.add(ItemFragment.createItem(context, tmpRowContent, false));
                                }
                            } catch (Exception e) {
                                Log.e("DownloadService", "getNewRelevantInformationCount: Got exception while checking for relevancy: " + e);
                            }
                        }
                    }
                }
            }
        }
        return count;
    }


    //hidden debug plans:
    private void dummyHandleActionDownloadPlan(int no){
        setTextViewText(getString(R.string.loading));
        Tools.updateWidgets(this);
        maybeSaveFormattedPlan();
        String result = cssHeader + dummyCSS + cssFoot;
        switch (no){
            case -1:
                // Not actually dummy, but infoscreen
                result = getPlanInsecure(PLAN_2_ADDRESS_1) + getPlanInsecure(PLAN_2_ADDRESS_2);
                break;
            case 1:
                result += dummy1;
                break;
            case 2:
                result += dummy2;
                break;
            case 3:
                result += dummy3;
                break;
            case 4:
                result += dummy4;
                break;
            case 5:
                result += dummy5;
                break;
            case 6:
                result += dummy6;
                break;
            case 7:
                result += dummy7;
                break;
            case 8:
                result += dummy8;
                break;
        }

        processPlan(result);
    }
    private final String dummyCSS = "body { margin-top:20px;font-family:sans-serif;\n" +
            "       }\n" +
            "\n" +
            "th { color: #eee; \n" +
            "     background-color: #048; \n" +
            "     height:0px;}\n" +
            "\n" +
            "td { color: #000;\n" +
            "     background-color:#cce; \n" +
            "     }\n" +
            "table.innen\n" +
            "{  \n" +
            "  border:0;\n" +
            "}\n" +
            "\n" +
            "table.F\n" +
            "{\n" +
            "  border-style:none;border-width:0;border-collapse:collapse; \n" +
            "}\n" +
            "\n" +
            "th.a \n" +
            "{   \n" +
            "  text-align:left;\n" +
            "}\n" +
            "th.F \n" +
            "{   \n" +
            "  text-align:left;\n" +
            "}\n";
    private final String dummy1 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
            "<html><head>\n" +
            "<meta http-equiv=\"content-type\" content=\"text/html; charset=windows-1252\"><link rel=\"stylesheet\" type=\"text/css\" href=\"WILLI-Dateien/willi.css\"><script src=\"WILLI-Dateien/willi.htm\" type=\"text/javascript\"></script><title>WILLI</title></head>\n" +
            "<body>\n" +
            "<a name=\"oben\"><h1>Vertretungspl&auml;ne f&uuml;r </h1><br>\n" +
            "</a><a href=\"#07.01.2015\">07.01.2015</a><br>\n" +
            "<a href=\"#08.01.2015\">08.01.2015</a><br>\n" +
            "<a name=\"07.01.2015\"><hr></a>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h2>Vertretungsplan f&uuml;r Mi, 7.1.2015</h2>erstellt: 7.1. 8:32 <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            " </p><table class=\"F\" border-width=\"3\"><colgroup><col width=\"899\"></colgroup> <tbody><tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "***&nbsp;&nbsp; Version 2&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "DIES IST NICHT DER ECHTE VERTRETUNGSPLAN, SONDERN SOLL NUR DIE FUNKTIONSWEISE VERANSCHAULICHEN!</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Alle Heinzelm&auml;nnchen treffen sich heute bitte in der 2. Pause im Atrium!</th>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            " <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h4>Vertretungen:</h4> <table class=\"k\" border-width=\"3\"><tbody><tr><th width=\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=\"50\">\n" +
            "Std.</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=\"50\">\n" +
            "Fach</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"2\" class=\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;TS</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "9</td>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;TS</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"1\" class=\"k\">\n" +
            "12</th>\n" +
            "<td>\n" +
            "&nbsp;Yoda</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Yoda</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;R42</td>\n" +
            "<td>\n" +
            "&nbsp;Raum&auml;nderung</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"2\" class=\"k\">\n" +
            "PK</th>\n" +
            "<td>\n" +
            "&nbsp;Pap</td>\n" +
            "<td>\n" +
            "7</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Kehr</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "<a href=\"https://itunes.apple.code/app/akg-vertretungsplan/id918844717?l=de&amp;ls=1&amp;mt=8\" target=\"_new\">zur IOS-App von Yann Rekker</a>&nbsp; -&nbsp; <a href=\"https://play.google.com/store/apps/details?id=de.atozdev.vertretungsplanapp&amp;hl=de\" target=\"_new\">zur Android-App von Philipp B&uuml;chner</a> </p>\n" +
            "<hr>\n" +
            "<a name=\"08.01.2015\"><hr></a>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h2>Vertretungsplan f&uuml;r Do, 8.1.2015</h2>erstellt: 7.1. 8:32 <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            " </p><table class=\"F\" border-width=\"3\"><colgroup><col width=\"899\"></colgroup> <tbody><tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "***&nbsp;&nbsp; Version 2&nbsp;&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Wer einen besseren Beispielsvertretungsplan haben will, der macht bitte selber einen ;)</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "PS5 spielen entf&auml;llt</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Wer das liest ist do</th>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            " <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h4>Vertretungen:</h4> <table class=\"k\" border-width=\"3\"><tbody><tr><th width=\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=\"50\">\n" +
            "Std.</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=\"50\">\n" +
            "Fach</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"2\" class=\"k\">\n" +
            "11</th>\n" +
            "\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S135</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "4</td>\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S135</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"3\" class=\"k\">\n" +
            "12</th>\n" +
            "<td>\n" +
            "&nbsp;Lud</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Lud</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Nim</td>\n" +
            "<td>\n" +
            "9</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"1\" class=\"k\">\n" +
            "PK</th>\n" +
            "<td>\n" +
            "&nbsp;AG</td>\n" +
            "<td>\n" +
            "7</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "<a href=\"https://itunes.apple.code/app/akg-vertretungsplan/id918844717?l=de&amp;ls=1&amp;mt=8\" target=\"_new\">zur IOS-App von Yann Rekker</a>&nbsp; -&nbsp; <a href=\"https://play.google.com/store/apps/details?id=de.atozdev.vertretungsplanapp&amp;hl=de\" target=\"_new\">zur Android-App von Philipp B&uuml;chner</a> </p>\n" +
            "<hr>\n" +
            "</body></html>";
    private final String dummy2 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
            "<html><head>\n" +
            "<meta http-equiv=\"content-type\" content=\"text/html; charset=windows-1252\"><link rel=\"stylesheet\" type=\"text/css\" href=\"WILLI-Dateien/willi.css\"><script src=\"WILLI-Dateien/willi.htm\" type=\"text/javascript\"></script><title>WILLI</title></head>\n" +
            "<body>\n" +
            "<a name=\"oben\"><h1>Vertretungspl&auml;ne für </h1><br>\n" +
            "</a><a href=\"#07.01.2015\">07.01.2015</a><br>\n" +
            "<a href=\"#08.01.2015\">08.01.2015</a><br>\n" +
            "<a name=\"07.01.2015\"><hr></a>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h2>Vertretungsplan für Mi, 7.1.2015</h2>erstellt: 7.1. 8:32 <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            " </p><table class=\"F\" border-width=\"3\"><colgroup><col width=\"899\"></colgroup> <tbody><tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "***&nbsp;&nbsp; Version 2&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "DIES IST NICHT DER ECHTE VERTRETUNGSPLAN, SONDERN SOLL NUR DIE FUNKTIONSWEISE VERANSCHAULICHEN!</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Alle Heinzelm&auml;nnchen treffen sich heute bitte in der 2. Pause im Atrium!</th>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            " <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h4>Vertretungen:</h4> <table class=\"k\" border-width=\"3\"><tbody><tr><th width=\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=\"50\">\n" +
            "Std.</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=\"50\">\n" +
            "Fach</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"2\" class=\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;TS</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "9</td>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;TS</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"1\" class=\"k\">\n" +
            "12</th>\n" +
            "<td>\n" +
            "&nbsp;Yoda</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Yoda</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;R42</td>\n" +
            "<td>\n" +
            "&nbsp;Raum&auml;nderung</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"2\" class=\"k\">\n" +
            "PK</th>\n" +
            "<td>\n" +
            "&nbsp;Pap</td>\n" +
            "<td>\n" +
            "7</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Kehr</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "<a href=\"https://itunes.apple.code/app/akg-vertretungsplan/id918844717?l=de&amp;ls=1&amp;mt=8\" target=\"_new\">zur IOS-App von Yann Rekker</a>&nbsp; -&nbsp; <a href=\"https://play.google.com/store/apps/details?id=de.atozdev.vertretungsplanapp&amp;hl=de\" target=\"_new\">zur Android-App von Philipp Büchner</a> </p>\n" +
            "<hr>\n" +
            "<a name=\"08.01.2015\"><hr></a>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h2>Vertretungsplan für Do, 8.1.2015</h2>erstellt: 7.1. 8:32 <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            " </p><table class=\"F\" border-width=\"3\"><colgroup><col width=\"899\"></colgroup> <tbody><tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "***&nbsp;&nbsp; Version 2&nbsp;&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Wer einen besseren Beispielsvertretungsplan haben will, der macht bitte selber einen ;)</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "PS5 spielen entf&auml;llt</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Wer das liest ist do</th>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            " <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h4>Vertretungen:</h4> <table class=\"k\" border-width=\"3\"><tbody><tr><th width=\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=\"50\">\n" +
            "Std.</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=\"50\">\n" +
            "Fach</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"4\" class=\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;Bodo</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;Bodo</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;Bagger</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S135</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Bodo</td>\n" +
            "<td>\n" +
            "4</td>\n" +
            "<td>\n" +
            "&nbsp;Bodo</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;Bagger</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "4</td>\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S135</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"3\" class=\"k\">\n" +
            "12</th>\n" +
            "<td>\n" +
            "&nbsp;Lud</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Lud</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Nim</td>\n" +
            "<td>\n" +
            "9</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"1\" class=\"k\">\n" +
            "PK</th>\n" +
            "<td>\n" +
            "&nbsp;AG</td>\n" +
            "<td>\n" +
            "7</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "<a href=\"https://itunes.apple.code/app/akg-vertretungsplan/id918844717?l=de&amp;ls=1&amp;mt=8\" target=\"_new\">zur IOS-App von Yann Rekker</a>&nbsp; -&nbsp; <a href=\"https://play.google.com/store/apps/details?id=de.atozdev.vertretungsplanapp&amp;hl=de\" target=\"_new\">zur Android-App von Philipp Büchner</a> </p>\n" +
            "<hr>\n" +
            "</body></html>";
    private final String dummy3 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xht=\n" +
            "ml11/DTD/xhtml11.dtd\"><html><head><meta charset=3D\"ISO-8859-1\"><link rel=3D=\n" +
            "\"stylesheet\" type=3D\"text/css\" href=3D\"http://www.sc.shuttle.de/sc/akg/Ver/=\n" +
            "willi.css\"><title>WILLI</title></head>\n" +
            "<body>\n" +
            "<a name=3D\"oben\"><h1>Vertretungspl=E4ne für </h1><br>\n" +
            "</a><a href=3D\"http://www.sc.shuttle.de/sc/akg/Ver/#23.12.2014\">23.12.2014<=\n" +
            "/a><br>\n" +
            "<a href=3D\"http://www.sc.shuttle.de/sc/akg/Ver/#07.01.2015\">07.01.2015</a><=\n" +
            "br>\n" +
            "<a name=3D\"23.12.2014\"><hr></a>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "</p><h2>Vertretungsplan für Di, 23.12.2014</h2>erstellt: 23.12. 9:10 <p><=\n" +
            "/p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            " </p><table class=3D\"F\" border-width=3D\"3\"><colgroup><col width=3D\"899\"></c=\n" +
            "olgroup> <tbody><tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "***&nbsp;&nbsp; Version 3&nbsp;&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "8.00 Uhr Weihnachtsgottesdienst in St. Sebald (Teilnahme freiwillig)</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "3. - 5. Stunde Unterricht</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Für die 10. Klassen: Seminarvorstellung nach Plan</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Unterrichtsschluss 12.05 Uhr</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "AK Soziales Engagement:</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Alle unter Termin-Gruppe B eingeteilten Mitglieder des Arbeitskreises</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "treffen sich heute um 14 Uhr direkt an den Pflegeeinrichtungen.</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Für das Seniorenzentrum sind das neben den Q12-Leitern Sarah Weichert, Ha=\n" +
            "nnah Schmidt, </th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Jessica Moritz, Chiara Palotto, Christina Anton, Adrian Perras, Dominik Lei=\n" +
            "del, </th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "für das AWO-Heim neben den Q12-Leitern Milena Roth, Hilal Yilmazel, Ohda =\n" +
            "Mier, Nadin Moustafa, </th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Mona Mohamad, Ann-Sophie Janssen, Nevin Negit</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Bitte erscheint alle pünktlich! Wegen des Ferienbeginns k=F6nnt ihr in de=\n" +
            "n Heimen etwas früher aufh=F6ren.</th>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            " <p></p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "</p><h4>Gesamte Schule:</h4> <table class=3D\"G\"><colgroup><col width=3D\"150=\n" +
            "\"><col width=3D\"250\"></colgroup> <tbody><tr class=3D\"G\"><th rowspan=3D\"1\" c=\n" +
            "lass=3D\"G\">\n" +
            "1.-2. </th>\n" +
            "<td>\n" +
            "Weihnachtsgottesdienst</td>\n" +
            "</tr>\n" +
            "<tr class=3D\"G\"><th rowspan=3D\"1\" class=3D\"G\">\n" +
            "6.-11. </th>\n" +
            "<td>\n" +
            "Weihnachtsferien</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "</p><h4>Vertretungen:</h4> <table class=3D\"k\" border-width=3D\"3\"><tbody><tr=\n" +
            "><th width=3D\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=3D\"50\">\n" +
            "Std.</th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=3D\"50\">\n" +
            "Fach</th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=3D\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"k\"><th rowspan=3D\"2\" class=3D\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;Mzl</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Mzl</td>\n" +
            "<td>\n" +
            "4</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr>\n" +
            "<tr class=3D\"k\"><th rowspan=3D\"3\" class=3D\"k\">\n" +
            "12</th>\n" +
            "<td>\n" +
            "&nbsp;Lei</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Lei</td>\n" +
            "<td>\n" +
            "4</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Rml</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Rml</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;H19</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "<a href=3D\"https://itunes.apple.code/app/akg-vertretungsplan/id918844717?l=\n" +
            "=3Dde&amp;ls=3D1&amp;mt=3D8\" target=3D\"_new\">zur IOS-App von Yann Rekker</a=\n" +
            ">&nbsp; -&nbsp; <a href=3D\"https://play.google.com/store/apps/details?id=3D=\n" +
            "de.atozdev.vertretungsplanapp&amp;hl=3Dde\" target=3D\"_new\">zur Android-App =\n" +
            "von Philipp Büchner</a> </p>\n" +
            "<hr>\n" +
            "<a name=3D\"07.01.2015\"><hr></a>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "</p><h2>Vertretungsplan für Mi, 7.1.2015</h2>erstellt: 23.12. 9:10 <p></p=\n" +
            ">\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            " </p><table class=3D\"F\" border-width=3D\"3\"><colgroup><col width=3D\"899\"></c=\n" +
            "olgroup> <tbody><tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "***&nbsp;&nbsp; Version 1&nbsp;&nbsp; ***</th>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            " <p></p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "</p><h4>Vertretungen:</h4> <table class=3D\"k\" border-width=3D\"3\"><tbody><tr=\n" +
            "><th width=3D\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=3D\"50\">\n" +
            "Std.</th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=3D\"50\">\n" +
            "Fach</th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=3D\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"k\"><th rowspan=3D\"2\" class=3D\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;Web</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;Web</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S05</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Web</td>\n" +
            "<td>\n" +
            "9</td>\n" +
            "<td>\n" +
            "&nbsp;Web</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S05</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr>\n" +
            "<tr class=3D\"k\"><th rowspan=3D\"1\" class=3D\"k\">\n" +
            "12</th>\n" +
            "<td>\n" +
            "&nbsp;Zg</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Zg</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;W17</td>\n" +
            "<td>\n" +
            "&nbsp;Raum=E4nderung</td>\n" +
            "</tr>\n" +
            "<tr class=3D\"k\"><th rowspan=3D\"2\" class=3D\"k\">\n" +
            "PK</th>\n" +
            "<td>\n" +
            "&nbsp;Ton</td>\n" +
            "<td>\n" +
            "7</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Shu</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "<a href=3D\"https://itunes.apple.code/app/akg-vertretungsplan/id918844717?l=\n" +
            "=3Dde&amp;ls=3D1&amp;mt=3D8\" target=3D\"_new\">zur IOS-App von Yann Rekker</a=\n" +
            ">&nbsp; -&nbsp; <a href=3D\"https://play.google.com/store/apps/details?id=3D=\n" +
            "de.atozdev.vertretungsplanapp&amp;hl=3Dde\" target=3D\"_new\">zur Android-App =\n" +
            "von Philipp Büchner</a> </p>\n" +
            "<hr>\n" +
            "</body></html>";
    private final String dummy4 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xht=\n" +
            "ml11/DTD/xhtml11.dtd\"><html><head><meta charset=3D\"ISO-8859-1\"><link rel=3D=\n" +
            "\"stylesheet\" type=3D\"text/css\" href=3D\"http://www.sc.shuttle.de/sc/akg/Ver/=\n" +
            "willi.css\"><title>WILLI</title></head>\n" +
            "<body>\n" +
            "<a name=3D\"oben\"><h1>Vertretungspl=E4ne für </h1><br>\n" +
            "</a><a href=3D\"http://www.sc.shuttle.de/sc/akg/Ver/#23.12.2014\">23.12.2014<=\n" +
            "/a><br>\n" +
            "<a href=3D\"http://www.sc.shuttle.de/sc/akg/Ver/#07.01.2015\">07.01.2015</a><=\n" +
            "br>\n" +
            "<a name=3D\"23.12.2014\"><hr></a>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "</p><h2>Vertretungsplan für Di, 23.12.2014</h2>erstellt: 23.12. 11:43 <p>=\n" +
            "</p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            " </p><table class=3D\"F\" border-width=3D\"3\"><colgroup><col width=3D\"899\"></c=\n" +
            "olgroup> <tbody><tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "***&nbsp;&nbsp; Version 3&nbsp;&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "8.00 Uhr Weihnachtsgottesdienst in St. Sebald (Teilnahme freiwillig)</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "3. - 5. Stunde Unterricht</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Für die 10. Klassen: Seminarvorstellung nach Plan</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Unterrichtsschluss 12.05 Uhr</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "AK Soziales Engagement:</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Alle unter Termin-Gruppe B eingeteilten Mitglieder des Arbeitskreises</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "treffen sich heute um 14 Uhr direkt an den Pflegeeinrichtungen.</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Für das Seniorenzentrum sind das neben den Q12-Leitern Sarah Weichert, Ha=\n" +
            "nnah Schmidt, </th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Jessica Moritz, Chiara Palotto, Christina Anton, Adrian Perras, Dominik Lei=\n" +
            "del, </th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "für das AWO-Heim neben den Q12-Leitern Milena Roth, Hilal Yilmazel, Ohda =\n" +
            "Mier, Nadin Moustafa, </th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Mona Mohamad, Ann-Sophie Janssen, Nevin Negit</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "Bitte erscheint alle pünktlich! Wegen des Ferienbeginns k=F6nnt ihr in de=\n" +
            "n Heimen etwas früher aufh=F6ren.</th>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            " <p></p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "</p><h4>Gesamte Schule:</h4> <table class=3D\"G\"><colgroup><col width=3D\"150=\n" +
            "\"><col width=3D\"250\"></colgroup> <tbody><tr class=3D\"G\"><th rowspan=3D\"1\" c=\n" +
            "lass=3D\"G\">\n" +
            "1.-2. </th>\n" +
            "<td>\n" +
            "Weihnachtsgottesdienst</td>\n" +
            "</tr>\n" +
            "<tr class=3D\"G\"><th rowspan=3D\"1\" class=3D\"G\">\n" +
            "6.-11. </th>\n" +
            "<td>\n" +
            "Weihnachtsferien</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "</p><h4>Vertretungen:</h4> <table class=3D\"k\" border-width=3D\"3\"><tbody><tr=\n" +
            "><th width=3D\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=3D\"50\">\n" +
            "Std.</th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=3D\"50\">\n" +
            "Fach</th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=3D\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"k\"><th rowspan=3D\"2\" class=3D\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;Mzl</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Mzl</td>\n" +
            "<td>\n" +
            "4</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr>\n" +
            "<tr class=3D\"k\"><th rowspan=3D\"3\" class=3D\"k\">\n" +
            "12</th>\n" +
            "<td>\n" +
            "&nbsp;Lei</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Lei</td>\n" +
            "<td>\n" +
            "4</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Rml</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Rml</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;H19</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "<a href=3D\"https://itunes.apple.code/app/akg-vertretungsplan/id918844717?l=\n" +
            "=3Dde&amp;ls=3D1&amp;mt=3D8\" target=3D\"_new\">zur IOS-App von Yann Rekker</a=\n" +
            ">&nbsp; -&nbsp; <a href=3D\"https://play.google.com/store/apps/details?id=3D=\n" +
            "de.atozdev.vertretungsplanapp&amp;hl=3Dde\" target=3D\"_new\">zur Android-App =\n" +
            "von Philipp Büchner</a> </p>\n" +
            "<hr>\n" +
            "<a name=3D\"07.01.2015\"><hr></a>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "</p><h2>Vertretungsplan für Mi, 7.1.2015</h2>erstellt: 23.12. 11:43 <p></=\n" +
            "p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            " </p><table class=3D\"F\" border-width=3D\"3\"><colgroup><col width=3D\"899\"></c=\n" +
            "olgroup> <tbody><tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "***&nbsp;&nbsp; Version 1&nbsp;&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"F\"><th rowspan=3D\"1\" class=3D\"F\">\n" +
            "MObS-Chor: keine Probe, Klausurvorsingen VOK11/12 ab 8. Stunde</th>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            " <p></p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "</p><h4>Vertretungen:</h4> <table class=3D\"k\" border-width=3D\"3\"><tbody><tr=\n" +
            "><th width=3D\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=3D\"50\">\n" +
            "Std.</th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=3D\"50\">\n" +
            "Fach</th>\n" +
            "<th width=3D\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=3D\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=3D\"k\"><th rowspan=3D\"2\" class=3D\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;Web</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;Web</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S05</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Web</td>\n" +
            "<td>\n" +
            "9</td>\n" +
            "<td>\n" +
            "&nbsp;Web</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S05</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr>\n" +
            "<tr class=3D\"k\"><th rowspan=3D\"1\" class=3D\"k\">\n" +
            "12</th>\n" +
            "<td>\n" +
            "&nbsp;Zg</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Zg</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;W17</td>\n" +
            "<td>\n" +
            "&nbsp;Raum=E4nderung</td>\n" +
            "</tr>\n" +
            "<tr class=3D\"k\"><th rowspan=3D\"2\" class=3D\"k\">\n" +
            "PK</th>\n" +
            "<td>\n" +
            "&nbsp;Ton</td>\n" +
            "<td>\n" +
            "7</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Shu</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf=E4llt</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=3D\"seite\" align=3D\"left\">\n" +
            "<a href=3D\"https://itunes.apple.code/app/akg-vertretungsplan/id918844717?l=\n" +
            "=3Dde&amp;ls=3D1&amp;mt=3D8\" target=3D\"_new\">zur IOS-App von Yann Rekker</a=\n" +
            ">&nbsp; -&nbsp; <a href=3D\"https://play.google.com/store/apps/details?id=3D=\n" +
            "de.atozdev.vertretungsplanapp&amp;hl=3Dde\" target=3D\"_new\">zur Android-App =\n" +
            "von Philipp Büchner</a> </p>\n" +
            "<hr>\n" +
            "</body></html>";
    private final String dummy5 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
            "<html><head>\n" +
            "<meta http-equiv=\"content-type\" content=\"text/html; charset=windows-1252\"><link rel=\"stylesheet\" type=\"text/css\" href=\"WILLI-Dateien/willi.css\"><script src=\"WILLI-Dateien/willi.htm\" type=\"text/javascript\"></script><title>WILLI</title></head>\n" +
            "<body>\n" +
            "<a name=\"oben\"><h1>Vertretungspl&auml;ne f&uuml;r </h1><br>\n" +
            "</a><a href=\"#07.01.2015\">07.01.2015</a><br>\n" +
            "<a href=\"#08.01.2015\">08.01.2015</a><br>\n" +
            "<a name=\"07.01.2015\"><hr></a>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h2>Vertretungsplan f&uuml;r Mi, 7.1.2015</h2>erstellt: 7.1. 8:32 <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            " </p><table class=\"F\" border-width=\"3\"><colgroup><col width=\"899\"></colgroup> <tbody><tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "***&nbsp;&nbsp; Version 2&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "DIES IST NICHT DER ECHTE VERTRETUNGSPLAN, SONDERN SOLL NUR DIE FUNKTIONSWEISE VERANSCHAULICHEN!</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Alle Heinzelm&auml;nnchen treffen sich heute bitte in der 2. Pause im Atrium!</th>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            " <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h4>Vertretungen:</h4> <table class=\"k\" border-width=\"3\"><tbody><tr><th width=\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=\"50\">\n" +
            "Std.</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=\"50\">\n" +
            "Fach</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"2\" class=\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;TS</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "9</td>\n" +
            "<td>\n" +
            "&nbsp;DV</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;TS</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"1\" class=\"k\">\n" +
            "12</th>\n" +
            "<td>\n" +
            "&nbsp;Yoda</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Yoda</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;R42</td>\n" +
            "<td>\n" +
            "&nbsp;Raum&auml;nderung</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"2\" class=\"k\">\n" +
            "PK</th>\n" +
            "<td>\n" +
            "&nbsp;Pap</td>\n" +
            "<td>\n" +
            "7</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Kehr</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "<a href=\"https://itunes.apple.code/app/akg-vertretungsplan/id918844717?l=de&amp;ls=1&amp;mt=8\" target=\"_new\">zur IOS-App von Yann Rekker</a>&nbsp; -&nbsp; <a href=\"https://play.google.com/store/apps/details?id=de.atozdev.vertretungsplanapp&amp;hl=de\" target=\"_new\">zur Android-App von Philipp B&uuml;chner</a> </p>\n" +
            "<hr>\n" +
            "<a name=\"08.01.2015\"><hr></a>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h2>Vertretungsplan f&uuml;r Do, 8.1.2015</h2>erstellt: 7.1. 8:32 <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            " </p><table class=\"F\" border-width=\"3\"><colgroup><col width=\"899\"></colgroup> <tbody><tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "***&nbsp;&nbsp; Version 2&nbsp;&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Wer einen besseren Beispielsvertretungsplan haben will, der macht bitte selber einen ;)</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "PS5 spielen entf&auml;llt</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Wer das liest ist do</th>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            " <p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "</p><h4>Vertretungen:</h4> <table class=\"k\" border-width=\"3\"><tbody><tr><th width=\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=\"50\">\n" +
            "Std.</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=\"50\">\n" +
            "Fach</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"2\" class=\"k\">\n" +
            "11</th>\n" +
            "\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S135</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "4</td>\n" +
            "<td>\n" +
            "&nbsp;Bow</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S135</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"3\" class=\"k\">\n" +
            "12</th>\n" +
            "<td>\n" +
            "&nbsp;Lud</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Nim</td>\n" +
            "<td>\n" +
            "9</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Lud</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"1\" class=\"k\">\n" +
            "PK</th>\n" +
            "<td>\n" +
            "&nbsp;AG</td>\n" +
            "<td>\n" +
            "7</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "</tbody></table>\n" +
            "<p></p>\n" +
            "<p class=\"seite\" align=\"left\">\n" +
            "<a href=\"https://itunes.apple.code/app/akg-vertretungsplan/id918844717?l=de&amp;ls=1&amp;mt=8\" target=\"_new\">zur IOS-App von Yann Rekker</a>&nbsp; -&nbsp; <a href=\"https://play.google.com/store/apps/details?id=de.atozdev.vertretungsplanapp&amp;hl=de\" target=\"_new\">zur Android-App von Philipp B&uuml;chner</a> </p>\n" +
            "<hr>\n" +
            "</body></html>";
    private final String dummy6 = "<style media=&quot;screen&quot; type=&quot;text/css&quot;>body { margin-top:20px;font-family:sans-serif;\n" +
            "       }\n" +
            "\n" +
            "th { color: #eee; \n" +
            "     background-color: #048; \n" +
            "     height:0px;}\n" +
            "\n" +
            "td { color: #000;\n" +
            "     background-color:#cce; \n" +
            "     }\n" +
            "table.innen\n" +
            "{  \n" +
            "  border:0;\n" +
            "}\n" +
            "\n" +
            "table.F\n" +
            "{\n" +
            "  border-style:none;border-width:0;border-collapse:collapse; \n" +
            "}\n" +
            "\n" +
            "th.a \n" +
            "{   \n" +
            "  text-align:left;\n" +
            "}\n" +
            "th.F \n" +
            "{   \n" +
            "  text-align:left;\n" +
            "}\n" +
            "</style><!DOCTYPE html PUBLIC &quot;-//W3C//DTD XHTML 1.1//EN&quot; &quot;http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd&quot;>\n" +
            "<html>\n" +
            "<head><link rel=&quot;stylesheet&quot; type=&quot;text/css&quot; href=&quot;willi.css&quot;></link><script src=&quot;willi.js&quot; type=&quot;text/javascript&quot;></script><title>WILLI</title></head>\n" +
            "<body>\n" +
            "<center>\n" +
            "<h1>\n" +
            "Leider ist unser Server ausgefallen. \n" +
            "Daher gibt es zur Zeit keinen Online-Vertretungsplan.\n" +
            "Bitte achtet auf die Aush&auml;nge im Atrium \n" +
            "und an der Pinnwand im 1. Stock im DG, wo der Plan t&auml;glich per Fax ankommt.\n" +
            "</h1>\n" +
            "</center>\n" +
            "</body></html>\n";
    private final String dummy7 = "<style media=\"screen\" type=\"text/css\">body { margin-top:20px;font-family:sans-serif;\n" +
            "       }\n" +
            "\n" +
            "th { color: #eee; \n" +
            "     background-color: #048; \n" +
            "     height:0px;}\n" +
            "\n" +
            "td { color: #000;\n" +
            "     background-color:#cce; \n" +
            "     }\n" +
            "table.innen\n" +
            "{  \n" +
            "  border:0;\n" +
            "}\n" +
            "\n" +
            "table.F\n" +
            "{\n" +
            "  border-style:none;border-width:0;border-collapse:collapse; \n" +
            "}\n" +
            "\n" +
            "th.a \n" +
            "{   \n" +
            "  text-align:left;\n" +
            "}\n" +
            "th.F \n" +
            "{   \n" +
            "  text-align:left;\n" +
            "}\n" +
            "</style><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
            "<html>\n" +
            "<head><link rel=\"stylesheet\" type=\"text/css\" href=\"willi.css\"></link><script src=\"willi.js\" type=\"text/javascript\"></script><title>WILLI</title></head>\n" +
            "<body>\n" +
            "<a name=\"oben\"/><h1>Vertretungspl&auml;ne f&uuml;r </h1><br />\n" +
            "<a href=\"#17.07.2015\">17.07.2015</a><br />\n" +
            "<a href=\"#20.07.2015\">20.07.2015</a><br />\n" +
            "<a name=\"17.07.2015\"><hr /></a>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<h2>Vertretungsplan f&uuml;r Fr, 17.7.2015</h2>erstellt: 17.7. 16:59 </p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            " <table class=\"F\" border-width=\"3\"><colgroup><col width=\"899\"/></colgroup> <tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "***&nbsp;&nbsp; Version 1&nbsp;&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Q11_4: Raum&auml;nderungen beachten!</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "5. Std. AK Beachtag H21</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "AB 12.05 UHR HITZEFREI!</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Nachsrbeit 6. Std. in H14</th>\n" +
            "</tr>\n" +
            "</table>\n" +
            " </p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<h4>Vertretungen:</h4> <table class=\"k\" border-width=\"3\"><tr><th width=\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=\"50\">\n" +
            "Std.</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=\"50\">\n" +
            "Fach</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"15\" class=\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;RTZ</td>\n" +
            "<td>\n" +
            "1</td>\n" +
            "<td>\n" +
            "&nbsp;RTZ</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S11</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "1</td>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S01</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;RTZ</td>\n" +
            "<td>\n" +
            "2</td>\n" +
            "<td>\n" +
            "&nbsp;RTZ</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S11</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "2</td>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S01</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;AZR</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;MIK</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;A06</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S11</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "4</td>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S11</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S21</td>\n" +
            "<td>\n" +
            "&nbsp;Raum&auml;nderung</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;AZR</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;MIK</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Zg</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Zg</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S11</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Gd</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Rei</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;SWB</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Sns</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"5\" class=\"k\">\n" +
            "PK</th>\n" +
            "<td>\n" +
            "&nbsp;Rp</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Kro</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Rp</td>\n" +
            "<td>\n" +
            "9</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;H03</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Men</td>\n" +
            "<td>\n" +
            "10</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;M&ouml;h</td>\n" +
            "<td>\n" +
            "10</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "</table>\n" +
            "</p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<a href=\"https://appsto.re/de/X3S64.i\" target=\"_new\">zur IOS-App von Yann Rekker</a>&nbsp; -&nbsp; <a href=\"https://play.google.com/store/appsetails?id=de.atozdev.vertretungsplanapp&hl=de\" target=\"_new\">zur Android-App von Philipp B&uuml;chner</a> - <a href=\"https://play.google.com/store/apps/details?id=de.spiritcroc.akg_vertretungsplan\" target=\"_new\">zur Android-App von Tobias B&uuml;ttner</a> </p>\n" +
            "<hr />\n" +
            "<a name=\"20.07.2015\"><hr /></a>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<h2>Vertretungsplan f&uuml;r Mo, 20.7.2015</h2>erstellt: 17.7. 16:59 </p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            " <table class=\"F\" border-width=\"3\"><colgroup><col width=\"899\"/></colgroup> <tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "***&nbsp;&nbsp; Version&nbsp; 2&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Die Teilnehmer am Sozialpraktikum treffen sich in der 3./4. Stunde im Blauen Theater</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Wegen einer Lehrerkonferenz endet der Unterricht heute um 12.50 Uhr.</th>\n" +
            "</tr>\n" +
            "</table>\n" +
            " </p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<h4>Vertretungen:</h4> <table class=\"k\" border-width=\"3\"><tr><th width=\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=\"50\">\n" +
            "Std.</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=\"50\">\n" +
            "Fach</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"11\" class=\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;Men</td>\n" +
            "<td>\n" +
            "1</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "1</td>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S02</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Shw</td>\n" +
            "<td>\n" +
            "1</td>\n" +
            "<td>\n" +
            "&nbsp;Shw</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;W19</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "2</td>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S02</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Shw</td>\n" +
            "<td>\n" +
            "2</td>\n" +
            "<td>\n" +
            "&nbsp;Shw</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;W19</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Snd</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Snd</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;Raum&auml;nderung</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;TMM</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;TMM</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Snd</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;Snd</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;Raum&auml;nderung</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;K&ouml;n</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt; verlegt auf Di 7. Std. in S01</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;M&ouml;h</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;M&ouml;h</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;W26</td>\n" +
            "<td>\n" +
            "&nbsp;heute schon um 13.05 Uhr</td>\n" +
            "</tr>\n" +
            "</table>\n" +
            "</p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<a href=\"https://appsto.re/de/X3S64.i\" target=\"_new\">zur IOS-App von Yann Rekker</a>&nbsp; -&nbsp; <a href=\"https://play.google.com/store/appsetails?id=de.atozdev.vertretungsplanapp&hl=de\" target=\"_new\">zur Android-App von Philipp B&uuml;chner</a> - <a href=\"https://play.google.com/store/apps/details?id=de.spiritcroc.akg_vertretungsplan\" target=\"_new\">zur Android-App von Tobias B&uuml;ttner</a> </p>\n" +
            "<hr />\n" +
            "</body></html>";
    private final String dummy8 = "<style media=\"screen\" type=\"text/css\">body { margin-top:20px;font-family:sans-serif;\n" +
            "       }\n" +
            "\n" +
            "th { color: #eee; \n" +
            "     background-color: #048; \n" +
            "     height:0px;}\n" +
            "\n" +
            "td { color: #000;\n" +
            "     background-color:#cce; \n" +
            "     }\n" +
            "table.innen\n" +
            "{  \n" +
            "  border:0;\n" +
            "}\n" +
            "\n" +
            "table.F\n" +
            "{\n" +
            "  border-style:none;border-width:0;border-collapse:collapse; \n" +
            "}\n" +
            "\n" +
            "th.a \n" +
            "{   \n" +
            "  text-align:left;\n" +
            "}\n" +
            "th.F \n" +
            "{   \n" +
            "  text-align:left;\n" +
            "}\n" +
            "</style><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
            "<html>\n" +
            "<head><link rel=\"stylesheet\" type=\"text/css\" href=\"willi.css\"></link><script src=\"willi.js\" type=\"text/javascript\"></script><title>WILLI</title></head>\n" +
            "<body>\n" +
            "<a name=\"oben\"/><h1>Vertretungspl&auml;ne f&uuml;r </h1><br />\n" +
            "<a href=\"#17.07.2015\">17.07.2015</a><br />\n" +
            "<a href=\"#20.07.2015\">20.07.2015</a><br />\n" +
            "<a name=\"17.07.2015\"><hr /></a>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<h2>Vertretungsplan f&uuml;r Fr, 17.7.2015</h2>erstellt: 17.7. 16:59 </p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            " <table class=\"F\" border-width=\"3\"><colgroup><col width=\"899\"/></colgroup> <tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "***&nbsp;&nbsp; Version 2&nbsp;&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Q11_4: Raum&auml;nderungen beachten!</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "5. Std. AK Beachtag H21</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "AB 12.05 UHR HITZEFREI!</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Nachsrbeit 6. Std. in H14</th>\n" +
            "</tr>\n" +
            "</table>\n" +
            " </p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<h4>Vertretungen:</h4> <table class=\"k\" border-width=\"3\"><tr><th width=\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=\"50\">\n" +
            "Std.</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=\"50\">\n" +
            "Fach</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"15\" class=\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;RTZ</td>\n" +
            "<td>\n" +
            "1</td>\n" +
            "<td>\n" +
            "&nbsp;RTZ</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S11</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "1</td>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S01</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;RTZ</td>\n" +
            "<td>\n" +
            "2</td>\n" +
            "<td>\n" +
            "&nbsp;RTZ</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S11</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "2</td>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S01</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;AZR</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;MIK</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;A06</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "3</td>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S11</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "4</td>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S11</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Thz</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S21</td>\n" +
            "<td>\n" +
            "&nbsp;Raum&auml;nderung</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;AZR</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;MIK</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Zg</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Zg</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S11</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Gd</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Rei</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;SWB</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Sns</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"5\" class=\"k\">\n" +
            "PK</th>\n" +
            "<td>\n" +
            "&nbsp;Rp</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Kro</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Rp</td>\n" +
            "<td>\n" +
            "9</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;H03</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Men</td>\n" +
            "<td>\n" +
            "10</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;M&ouml;h</td>\n" +
            "<td>\n" +
            "10</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr>\n" +
            "</table>\n" +
            "</p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<a href=\"https://appsto.re/de/X3S64.i\" target=\"_new\">zur IOS-App von Yann Rekker</a>&nbsp; -&nbsp; <a href=\"https://play.google.com/store/appsetails?id=de.atozdev.vertretungsplanapp&hl=de\" target=\"_new\">zur Android-App von Philipp B&uuml;chner</a> - <a href=\"https://play.google.com/store/apps/details?id=de.spiritcroc.akg_vertretungsplan\" target=\"_new\">zur Android-App von Tobias B&uuml;ttner</a> </p>\n" +
            "<hr />\n" +
            "<a name=\"20.07.2015\"><hr /></a>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<h2>Vertretungsplan f&uuml;r Mo, 20.7.2015</h2>erstellt: 17.7. 16:59 </p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            " <table class=\"F\" border-width=\"3\"><colgroup><col width=\"899\"/></colgroup> <tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "***&nbsp;&nbsp; Version&nbsp; 3&nbsp; ***</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Die Teilnehmer am Sozialpraktikum treffen sich in der 3./4. Stunde im Blauen Theater</th>\n" +
            "</tr>\n" +
            "<tr class=\"F\"><th rowspan=\"1\" class=\"F\">\n" +
            "Wegen einer Lehrerkonferenz endet der Unterricht heute um 12.50 Uhr.</th>\n" +
            "</tr>\n" +
            "</table>\n" +
            " </p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<h4>Vertretungen:</h4> <table class=\"k\" border-width=\"3\"><tr><th width=\"50\">\n" +
            "Klasse </th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Lkr.</th>\n" +
            "<th width=\"50\">\n" +
            "Std.</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;vertreten durch</th>\n" +
            "<th width=\"50\">\n" +
            "Fach</th>\n" +
            "<th width=\"50\">\n" +
            "&nbsp;Raum</th>\n" +
            "<th width=\"150\">\n" +
            "</th>\n" +
            "</tr>\n" +
            "<tr class=\"k\"><th rowspan=\"11\" class=\"k\">\n" +
            "11</th>\n" +
            "<td>\n" +
            "&nbsp;Men</td>\n" +
            "<td>\n" +
            "1</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "1</td>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S02</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Shw</td>\n" +
            "<td>\n" +
            "1</td>\n" +
            "<td>\n" +
            "&nbsp;Shw</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;W19</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "2</td>\n" +
            "<td>\n" +
            "&nbsp;Wer</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;S02</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Shw</td>\n" +
            "<td>\n" +
            "2</td>\n" +
            "<td>\n" +
            "&nbsp;Shw</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;W19</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Snd</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;Snd</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;Raum&auml;nderung</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;TMM</td>\n" +
            "<td>\n" +
            "5</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;TMM</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;Snd</td>\n" +
            "<td>\n" +
            "6</td>\n" +
            "<td>\n" +
            "&nbsp;Snd</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;Raum&auml;nderung</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;K&ouml;n</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;</td>\n" +
            "<td>\n" +
            "&nbsp;entf&auml;llt; verlegt auf Di 7. Std. in S01</td>\n" +
            "</tr><tr>\n" +
            "<td>\n" +
            "&nbsp;M&ouml;h</td>\n" +
            "<td>\n" +
            "8</td>\n" +
            "<td>\n" +
            "&nbsp;M&ouml;h</td>\n" +
            "<td>\n" +
            "</td>\n" +
            "<td>\n" +
            "&nbsp;W26</td>\n" +
            "<td>\n" +
            "&nbsp;heute schon um 13.05 Uhr</td>\n" +
            "</tr>\n" +
            "</table>\n" +
            "</p>\n" +
            "<p class=\"seite\" style=\"text-align:left\">\n" +
            "<a href=\"https://appsto.re/de/X3S64.i\" target=\"_new\">zur IOS-App von Yann Rekker</a>&nbsp; -&nbsp; <a href=\"https://play.google.com/store/appsetails?id=de.atozdev.vertretungsplanapp&hl=de\" target=\"_new\">zur Android-App von Philipp B&uuml;chner</a> - <a href=\"https://play.google.com/store/apps/details?id=de.spiritcroc.akg_vertretungsplan\" target=\"_new\">zur Android-App von Tobias B&uuml;ttner</a> </p>\n" +
            "<hr />\n" +
            "</body></html>";
}
