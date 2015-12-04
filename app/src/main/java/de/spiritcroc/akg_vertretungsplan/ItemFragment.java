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

/*
    inspiration for the swipe-to-refresh feature from https://developer.android.com/samples/SwipeRefreshListFragment/src/com.example.android.swiperefreshlistfragment/SwipeRefreshListFragment.html
 */

package de.spiritcroc.akg_vertretungsplan;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

public class ItemFragment extends ListFragment{
    public static final int cellCount = 7;

    private SharedPreferences sharedPreferences;
    private String[] tmpRowContent = new String[cellCount];
    private String[] headerRow = new String[cellCount];
    private ArrayList<Integer> textColors = new ArrayList<>(), backgroundColors = new ArrayList<>();
    private ArrayList<String[]> fullFormattedContent = new ArrayList<>();   //Save List content so it can be shown in a Dialog
    private String currentClass = "";
    private int tmpCellCount;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean unreadContent = false;

    private static final String ARG_CONTENT = "content";
    private static final String ARG_DATE = "date";
    private static final String ARG_NUMBER = "number";

    private String content;
    private String date;
    private int number;

    private OnFragmentInteractionListener mListener;

    public static ItemFragment newInstance(String content, String date, int number) {
        ItemFragment fragment = new ItemFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONTENT, content);
        args.putString(ARG_DATE, date);
        args.putInt(ARG_NUMBER, number);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            content = getArguments().getString(ARG_CONTENT);
            date = getArguments().getString(ARG_DATE);
            number = getArguments().getInt(ARG_NUMBER);
        }
        else {
            Log.e("ItemFragment.onCreate", "getArguments()==null");
            content = date = "error";
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        setListAdapter(new CustomArrayAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, createContent(content)));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        final View listFragmentView = super.onCreateView(inflater, container, savedInstanceState);

        swipeRefreshLayout = new ListFragmentSwipeRefreshLayout(container.getContext());

        swipeRefreshLayout.addView(listFragmentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        swipeRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return swipeRefreshLayout;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Activity activity = getActivity();
                if (activity instanceof FormattedActivity)
                    ((FormattedActivity) activity).startDownloadService();
                else {
                    Log.e("ItemFragment", "cannot download plan: activity is not instance of FormattedActivity");
                    setRefreshing(false);
                }
            }
        });
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onPause(){
        super.onPause();
        if (sharedPreferences.getString("pref_auto_mark_read", "").equals("onActivityPause"))
            markChangesAsRead();
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id){
        String[] dividedText = fullFormattedContent.get(position);
        String shareMessage = getString(R.string.share_header) + "\n" + date;
        if (dividedText != null) {
            int emptyCount = 0;
            for (String divided: dividedText) {
                if (divided == null || divided.length() == 0) {
                    emptyCount++;
                }
            }
            if (emptyCount == cellCount - 1 && dividedText[0]!=null && dividedText[0].length()!=0)
                mListener.showDialog(dividedText[0], shareMessage + "\n" + dividedText[0]);
            else if (emptyCount == cellCount - 2 && dividedText[0]!=null && dividedText[0].length()!=0 && dividedText[1]!=null && dividedText[1].length()!=0)//→ two row table
                mListener.showDialog(getLessonTimeCombinationString(dividedText[0]) + "\n" + dividedText[1], shareMessage + "\n" + dividedText[0] + "\n" + dividedText[1]);
            else {
                String text = "";
                LessonPlan lessonPlan = LessonPlan.getInstance(sharedPreferences);
                for (int i = 0; i < dividedText.length; i++) {
                    if (dividedText[i] != null && dividedText[i].length() > 0) {
                        text += (text.length() == 0 ? "" : "\n") + (headerRow[i] == null || headerRow[i].length() == 0 ? "" : headerRow[i] + "\n\t") + (i == 2 ? getLessonTimeCombinationString(dividedText[i]) : getTeacherCombinationString(sharedPreferences, lessonPlan, dividedText[i]));
                        shareMessage += "\n" + (headerRow[i] == null || headerRow[i].length() == 0 ? "" : headerRow[i] + ": ") + getTeacherCombinationString(sharedPreferences, lessonPlan, dividedText[i]);
                    }
                }
                mListener.showDialog(text, shareMessage);
            }
        }
    }

    public boolean hasUnreadContent(){
        return !DownloadService.NO_PLAN.equals(date) && unreadContent;
    }

    public void setRefreshing(boolean refreshing){
        swipeRefreshLayout.setRefreshing(refreshing);
    }
    public void reloadContent(String content, String date){
        this.content = content;
        this.date = date;
        reloadContent();
    }
    public void reloadContent(){
        Activity activity = getActivity();
        if (activity == null)
            Log.e("reloadContent", "getActivity()==null");
        else {
            setListAdapterRememberPos(new CustomArrayAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, createContent(content)));
        }
    }

    private void setListAdapterRememberPos(ListAdapter listAdapter) {
        Activity activity = getActivity();
        if (activity == null)
            Log.e("ItemFragment", "setListAdapter: getActivity()==null");
        else {
            ListView listView = getListView();
            View view = listView.getChildAt(0);
            //int top = (view == null ? 0 : (view.getTop() - listView.getPaddingTop()));    //http://stackoverflow.com/questions/3014089/maintain-save-restore-scroll-position-when-returning-to-a-listview says that
            int top = (view == null ? 0 : view.getTop());
            int index = listView.getFirstVisiblePosition();
            setListAdapter(listAdapter);
            listView.setSelectionFromTop(index, top);
        }
    }


    public interface OnFragmentInteractionListener {
        void showDialog(String text, String messageText);
    }

    public void markChangesAsRead(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("pref_latest_title_"+number, date);
        editor.putString("pref_latest_plan_"+number, content);
        editor.apply();

        setListAdapterRememberPos(new CustomArrayAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, createContent(content)));
    }

    private String[] createContent(String unformattedContent){
        if (date.equals(DownloadService.NO_PLAN)) {
            return new String[0];
        }

        String oldPlan;
        if (date.equals(sharedPreferences.getString("pref_latest_title_1", "")))    //check date for comparison
            oldPlan = sharedPreferences.getString("pref_latest_plan_1", "");
        else if (date.equals(sharedPreferences.getString("pref_latest_title_2", "")))
            oldPlan = sharedPreferences.getString("pref_latest_plan_2", "");
        else
            oldPlan = "";   //no plan available for this date
        boolean putHeaderAsClass = false;
        for (int  i = 0; i < headerRow.length; i++){//reset header row
            headerRow[i] = null;
        }

        LessonPlan lessonPlan = LessonPlan.getInstance(sharedPreferences);
        ArrayList<String> result= new ArrayList<>();
        fullFormattedContent.clear();
        textColors.clear();
        backgroundColors.clear();
        String add;
        String tmp = "a";   //not empty
        boolean filterResults = sharedPreferences.getBoolean("pref_filter_plan", false) && LessonPlan.getInstance(sharedPreferences).isConfigured(),
                lastAddedHeader = false;
        unreadContent = false;
        for (int i = 0; !tmp.equals(""); i++){
            tmp = Tools.getLine(unformattedContent, i + 1);
            if (getRow(tmp, "" + DownloadService.ContentType.TABLE_ROW)){
                if (tmpCellCount==1) {
                    if (!filterResults || !sharedPreferences.getBoolean("pref_filter_general", false)) {
                        result.add(tmpRowContent[0]);       //header text
                        fullFormattedContent.add(tmpRowContent.clone());
                        if (Tools.lineAvailable(oldPlan, tmp)) {
                            textColors.add(Integer.parseInt(sharedPreferences.getString("pref_header_text_text_color", "" + Color.BLACK)));
                            backgroundColors.add(Integer.parseInt(sharedPreferences.getString("pref_header_text_background_color", "" + Color.TRANSPARENT)));
                        } else {       //highlight changes
                            textColors.add(Integer.parseInt(sharedPreferences.getString("pref_header_text_text_color_highlight", "" + Color.RED)));
                            backgroundColors.add(Integer.parseInt(sharedPreferences.getString("pref_header_text__background_color_highlight", "" + Color.TRANSPARENT)));
                            unreadContent = true;
                        }
                        currentClass = "";
                    }
                }
                else{
                    boolean relevant;
                    if (headerRow[0]==null){
                        if (putHeaderAsClass){
                            if (filterResults && lastAddedHeader)
                                removeLastAddedItem(result);
                            lastAddedHeader = true;
                            result.add(currentClass);    //class text
                            fullFormattedContent.add(null);
                            textColors.add(Integer.parseInt(sharedPreferences.getString("pref_class_text_text_color", "" + Color.BLUE)));
                            backgroundColors.add(Integer.parseInt(sharedPreferences.getString("pref_class_text_background_color", "" + Color.TRANSPARENT)));
                            putHeaderAsClass = false;
                        }
                        //else: no header available
                    }
                    else if (!currentClass.equals(tmpRowContent[0])){
                        if (filterResults && lastAddedHeader)
                            removeLastAddedItem(result);
                        lastAddedHeader = true;
                        currentClass = tmpRowContent[0];
                        result.add(headerRow[0] + " " + tmpRowContent[0]);    //class text
                        fullFormattedContent.add(null);
                        textColors.add(Integer.parseInt(sharedPreferences.getString("pref_class_text_text_color", "" + Color.BLUE)));
                        backgroundColors.add(Integer.parseInt(sharedPreferences.getString("pref_class_text_background_color", "" + Color.TRANSPARENT)));
                    }
                    if (headerRow[0]==null) { //e.g. when extra table "Gesamte Schule:"
                        add = createItem(getActivity(), tmpRowContent, true);
                        relevant = !sharedPreferences.getBoolean("pref_filter_general", false);
                        if (relevant)
                            lastAddedHeader = false;
                    } else {
                        add = createItem(getActivity(), tmpRowContent, false);
                        try {
                            relevant = lessonPlan.isRelevant( tmpRowContent[0], Tools.getDateFromPlanTitle(this.date).get(Calendar.DAY_OF_WEEK), Integer.parseInt(tmpRowContent[2]), tmpRowContent[1]);
                            if (relevant)
                                lastAddedHeader = false;
                        } catch (Exception e) {
                            Log.e("ItemFragment", "Check for relevancy threw exception: " + e);
                            relevant = false;
                        }
                    }
                    if (!filterResults || relevant) {
                        result.add(add);                    //substitution text
                        fullFormattedContent.add(tmpRowContent.clone());
                        if (Tools.lineAvailable(oldPlan, tmp)) {
                            if (relevant && !filterResults){
                                textColors.add(Integer.parseInt(sharedPreferences.getString("pref_relevant_text_text_color", "" + Color.BLACK)));
                                backgroundColors.add(Integer.parseInt(sharedPreferences.getString("pref_relevant_text_background_color", "" + Color.YELLOW)));
                            }
                            else {
                                textColors.add(Integer.parseInt(sharedPreferences.getString("pref_normal_text_text_color", "" + Color.BLACK)));
                                backgroundColors.add(Integer.parseInt(sharedPreferences.getString("pref_normal_text_background_color", "" + Color.TRANSPARENT)));
                            }
                        } else {       //highlight changes
                            if (relevant && !filterResults) {
                                textColors.add(Integer.parseInt(sharedPreferences.getString("pref_relevant_text_text_color_highlight", "" + Color.RED)));
                                backgroundColors.add(Integer.parseInt(sharedPreferences.getString("pref_relevant_text_background_color_highlight", "" + Color.YELLOW)));
                            }
                            else{
                                textColors.add(Integer.parseInt(sharedPreferences.getString("pref_normal_text_text_color_highlight", "" + Color.RED)));
                                backgroundColors.add(Integer.parseInt(sharedPreferences.getString("pref_normal_text__background_color_highlight", "" + Color.TRANSPARENT)));
                            }
                            unreadContent = true;
                        }
                    }
                }
            }
            else if (getRow(tmp, "" + DownloadService.ContentType.TABLE_START_FLAG)){
                currentClass = tmpRowContent[0];
                putHeaderAsClass = true;
            }
        }
        if (filterResults && lastAddedHeader)
            removeLastAddedItem(result);
        if (getActivity() instanceof FormattedActivity)
            ((FormattedActivity) getActivity()).requestRecheckUnreadChanges();
        return result.toArray(new String[result.size()]);
    }

    /**
     * @param noHeader
     * true e.g. when extra table "Gesamte Schule:" else false
     */
    public static String createItem(Context context, String[] values, boolean noHeader) {
        if (noHeader) {
            return values[0] + (values[1].equals("") ? "" : " → " + values[1]);
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        LessonPlan lessonPlan = LessonPlan.getInstance(sharedPreferences);
        boolean useFullTeacherNames = sharedPreferences.getBoolean("pref_formatted_plan_replace_teacher_short_with_teacher_full", true);
        String result = values[2] + " " + (useFullTeacherNames ? getTeacherCombinationString(sharedPreferences, lessonPlan, values[1]) : values[1]) + " →";
        if (!values[3].equals(""))
            result += " " + (useFullTeacherNames ? getTeacherCombinationString(sharedPreferences, lessonPlan, values[3]) : values[3]);
        if (!values[4].equals(""))
            result += " (" + values[4] + ")";
        if (!values[5].equals(""))
            result += " " + values[5];
        if (!values[6].equals(""))
            result += " " + values[6];
        return result;
    }
    private void removeLastAddedItem(ArrayList result){
        result.remove(result.size()-1);
        fullFormattedContent.remove(fullFormattedContent.size()-1);
        textColors.remove(textColors.size()-1);
        backgroundColors.remove(backgroundColors.size()-1);
    }
    private boolean getRow(String line, String searchingFor){
        if (line.length() > searchingFor.length()+1 && line.substring(0, searchingFor.length()).equals(searchingFor)) { //ignore empty rows
            line = line.substring(searchingFor.length());
            tmpCellCount = 0;
            if (Tools.countHeaderCells(line)>1) {  //save as headerRow
                for (int i = 0; i < headerRow.length; i++){
                    headerRow[i] = Tools.getCellContent(line, i+1);
                    if (!headerRow[i].equals(""))
                        tmpCellCount++;
                }
                return false;   //do not add headerRow to List
            }
            else {
                for (int i = 0; i < tmpRowContent.length; i++) {
                    tmpRowContent[i] = Tools.getCellContent(line, i + 1);
                    if (!tmpRowContent[i].equals(""))
                        tmpCellCount++;
                }
                return true;
            }
        }
        else
            return false;
    }

    private static String getTeacherCombinationString(SharedPreferences sharedPreferences, LessonPlan lessonPlan, String teacherShort){
        String result = lessonPlan.getTeacherFullForTeacherShort(teacherShort);
        if (result == null || result.equals(""))
            result = teacherShort;
        else if (sharedPreferences.getBoolean("pref_formatted_plan_show_teacher_full_and_short", true))
            result += " (" + teacherShort + ")";
        return result;
    }
    private String getLessonTimeCombinationString(String time){
        try{
            return time + " (" + getResources().getStringArray(R.array.lesson_plan_times)[Integer.parseInt(time)-1] + ")";
        }
        catch (Exception e){
            Log.e("ItemFragment", "getLessonTimeCombinationString: Got exception: " + e);
            return time;
        }
    }

    public class CustomArrayAdapter extends ArrayAdapter{
        public CustomArrayAdapter (Context context, int resource, String[] objects){
            super(context, resource, objects);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View view = super.getView(position, convertView, parent);
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            if (textColors.size()>position)
                textView.setTextColor(textColors.get(position));
            else
                textView.setTextColor(Color.BLACK);     //default color
            if (backgroundColors.size()>position)
                view.setBackgroundColor(backgroundColors.get(position));
            else
                view.setBackgroundColor(Color.WHITE);   //default background color
            return view;
        }
    }

    private class ListFragmentSwipeRefreshLayout extends SwipeRefreshLayout{
        public ListFragmentSwipeRefreshLayout(Context context){
            super(context);
        }
        @Override
        public boolean canChildScrollUp(){
            final ListView listView = getListView();
            if (listView.getVisibility() == View.VISIBLE){
                if (Build.VERSION.SDK_INT >= 14)
                    return ViewCompat.canScrollVertically(listView, -1);
                else
                    return listView.getChildCount() > 0 && (listView.getFirstVisiblePosition() > 0 || listView.getChildAt(0).getTop() < listView.getPaddingTop());
            }
            else
                return false;
        }
    }
}
