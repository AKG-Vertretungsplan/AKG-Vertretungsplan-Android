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

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

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

    private static final String ARG_NUMBER = "number";

    private String content;
    private String date;
    private int number;

    private OnFragmentInteractionListener mListener;

    public static ItemFragment newInstance(int number) {
        ItemFragment fragment = new ItemFragment();
        Bundle args = new Bundle();
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
            number = getArguments().getInt(ARG_NUMBER);
        }
        else {
            Log.e("ItemFragment.onCreate", "getArguments()==null");
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        if (content != null) {
            setListAdapter(new CustomArrayAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, createContent(content)));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        final View listFragmentView = super.onCreateView(inflater, container, savedInstanceState);

        swipeRefreshLayout = new ListFragmentSwipeRefreshLayout(container == null ? getActivity() : container.getContext());

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
                    ((FormattedActivity) activity).startDownloadService(true);
                else {
                    Log.e("ItemFragment", "cannot download plan: activity is not instance of FormattedActivity");
                    setRefreshing(false);
                }
            }
        });
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
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
        if (sharedPreferences.getString(Keys.AUTO_MARK_READ, "").equals("onActivityPause"))
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
                mListener.showDialog(date, dividedText[0], shareMessage + "\n" + dividedText[0]);
            else if (emptyCount == cellCount - 2 && dividedText[0]!=null && dividedText[0].length()!=0 && dividedText[1]!=null && dividedText[1].length()!=0)//→ two row table
                mListener.showDialog(date, getLessonTimeCombinationString(dividedText[0]) + "\n" + dividedText[1], shareMessage + "\n" + dividedText[0] + "\n" + dividedText[1]);
            else {
                String text = "";
                LessonPlan lessonPlan = LessonPlan.getInstance(sharedPreferences);
                for (int i = 0; i < dividedText.length; i++) {
                    if (dividedText[i] != null && dividedText[i].length() > 0) {
                        text += (text.length() == 0 ? "" : "\n") + (headerRow[i] == null || headerRow[i].length() == 0 ? "" : headerRow[i] + "\n\t") + (i == 2 ? getLessonTimeCombinationString(dividedText[i]) : getTeacherCombinationString(sharedPreferences, lessonPlan, dividedText[i]));
                        shareMessage += "\n" + (headerRow[i] == null || headerRow[i].length() == 0 ? "" : headerRow[i] + ": ") + getTeacherCombinationString(sharedPreferences, lessonPlan, dividedText[i]);
                    }
                }
                mListener.showDialog(date, text, shareMessage);
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
            Log.w("reloadContent", "getActivity()==null");
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
        void showDialog(String title, String text, String messageText);
    }

    public void markChangesAsRead(){
        DownloadService.markPlanRead(getActivity());

        setListAdapterRememberPos(new CustomArrayAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, createContent(content)));
    }

    private String[] createContent(String unformattedContent){
        if (date.equals(DownloadService.NO_PLAN)) {
            return new String[0];
        }

        String oldPlan;
        if (date.equals(sharedPreferences.getString(Keys.LATEST_TITLE_1, "")))    //check date for comparison
            oldPlan = sharedPreferences.getString(Keys.LATEST_PLAN_1, "");
        else if (date.equals(sharedPreferences.getString(Keys.LATEST_TITLE_2, "")))
            oldPlan = sharedPreferences.getString(Keys.LATEST_PLAN_2, "");
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
        boolean filterResults = sharedPreferences.getBoolean(Keys.FILTER_PLAN, false) && LessonPlan.getInstance(sharedPreferences).isConfigured(),
                lastAddedHeader = false;
        unreadContent = false;
        for (int i = 0; !tmp.equals(""); i++){
            tmp = Tools.getLine(unformattedContent, i + 1);
            if (getRow(tmp, "" + DownloadService.ContentType.TABLE_ROW)){
                if (tmpCellCount==1) {
                    if (!filterResults || !sharedPreferences.getBoolean(Keys.FILTER_GENERAL, false)) {
                        result.add(tmpRowContent[0]);       //header text
                        fullFormattedContent.add(tmpRowContent.clone());
                        if (Tools.lineAvailable(oldPlan, tmp)) {
                            textColors.add(Integer.parseInt(sharedPreferences.getString(Keys.HEADER_TEXT_TEXT_COLOR, "" + Color.BLACK)));
                            backgroundColors.add(Integer.parseInt(sharedPreferences.getString(Keys.HEADER_TEXT_BG_COLOR, "" + Color.TRANSPARENT)));
                        } else {       //highlight changes
                            textColors.add(Integer.parseInt(sharedPreferences.getString(Keys.HEADER_TEXT_TEXT_COLOR_HL, "" + Color.RED)));
                            backgroundColors.add(Integer.parseInt(sharedPreferences.getString(Keys.HEADER_TEXT_BG_COLOR_HL, "" + Color.TRANSPARENT)));
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
                            textColors.add(Integer.parseInt(sharedPreferences.getString(Keys.CLASS_TEXT_TEXT_COLOR, "" + Color.BLUE)));
                            backgroundColors.add(Integer.parseInt(sharedPreferences.getString(Keys.CLASS_TEXT_BG_COLOR, "" + Color.TRANSPARENT)));
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
                        textColors.add(Integer.parseInt(sharedPreferences.getString(Keys.CLASS_TEXT_TEXT_COLOR, "" + Color.BLUE)));
                        backgroundColors.add(Integer.parseInt(sharedPreferences.getString(Keys.CLASS_TEXT_BG_COLOR, "" + Color.TRANSPARENT)));
                    }
                    if (headerRow[0]==null) { //e.g. when extra table "Gesamte Schule:"
                        add = createItem(getActivity(), tmpRowContent, true);
                        relevant = lessonPlan.isConfigured() && !sharedPreferences.getBoolean(Keys.FILTER_GENERAL, false);
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
                                textColors.add(Integer.parseInt(sharedPreferences.getString(Keys.RELEVANT_TEXT_TEXT_COLOR, "" + Color.BLACK)));
                                backgroundColors.add(Integer.parseInt(sharedPreferences.getString(Keys.RELEVANT_TEXT_BG_COLOR, "" + Color.YELLOW)));
                            }
                            else {
                                textColors.add(Integer.parseInt(sharedPreferences.getString(Keys.NORMAL_TEXT_TEXT_COLOR, "" + Color.BLACK)));
                                backgroundColors.add(Integer.parseInt(sharedPreferences.getString(Keys.NORMAL_TEXT_BG_COLOR, "" + Color.TRANSPARENT)));
                            }
                        } else {       //highlight changes
                            if (relevant && !filterResults) {
                                textColors.add(Integer.parseInt(sharedPreferences.getString(Keys.RELEVANT_TEXT_TEXT_COLOR_HL, "" + Color.RED)));
                                backgroundColors.add(Integer.parseInt(sharedPreferences.getString(Keys.RELEVANT_TEXT_BG_COLOR_HL, "" + Color.YELLOW)));
                            }
                            else{
                                textColors.add(Integer.parseInt(sharedPreferences.getString(Keys.NORMAL_TEXT_TEXT_COLOR_HL, "" + Color.RED)));
                                backgroundColors.add(Integer.parseInt(sharedPreferences.getString(Keys.NORMAL_TEXT_BG_COLOR_HL, "" + Color.TRANSPARENT)));
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
        boolean useFullTeacherNames = sharedPreferences.getBoolean(Keys.FORMATTED_PLAN_REPLACE_TEACHER_SHORT_WITH_TEACHER_FULL, true);
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

    public static String getTeacherCombinationString(SharedPreferences sharedPreferences, LessonPlan lessonPlan, String teacherShort){
        String result = lessonPlan.getTeacherFullForTeacherShort(teacherShort);
        if (result == null || result.equals(""))
            result = teacherShort;
        else if (sharedPreferences.getBoolean(Keys.FORMATTED_PLAN_SHOW_TEACHER_FULL_AND_SHORT, true))
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
