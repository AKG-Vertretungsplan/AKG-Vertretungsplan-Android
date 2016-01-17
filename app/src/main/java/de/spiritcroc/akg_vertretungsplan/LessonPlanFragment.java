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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class LessonPlanFragment extends ListFragment {
    private static final String ARG_DAY = "day";

    private int day;
    private Lesson[] lessons;
    private String[] startTimes;
    private String[] fullTimes;
    private ArrayList<Integer> textColors = new ArrayList<>();
    private ArrayList<String> relevantInformation, relevantRoomInformation, generalInformation;
    private ArrayList<Integer> relevantInformationLessons;

    private ArrayList<TextView> timeViews = new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private boolean showTime;
    private boolean showFullTime;

    public static LessonPlanFragment newInstance(int day) {
        LessonPlanFragment fragment = new LessonPlanFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DAY, day);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            day = getArguments().getInt(ARG_DAY);
        }
        else {
            Log.e("LessonPlanFragment", "onCreate: getArguments()==null");
            day = 0;
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        fullTimes = getResources().getStringArray(R.array.lesson_plan_times);
        startTimes = new String[LessonPlan.LESSON_COUNT];
        for (int i = 0; i < startTimes.length; i++) {
            startTimes[i] = fullTimes[i].substring(0, fullTimes[i].indexOf(" "));
        }

        update();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().post(updateTimeViewWidth);
    }

    private LessonViewContent[] createContent(){
        textColors.clear();

        boolean addInformation = sharedPreferences.getBoolean("pref_lesson_plan_show_information", false);

        int color = Integer.parseInt(sharedPreferences.getString("pref_lesson_plan_color_lesson", "" + Color.BLACK)),
                colorFreeTime = Integer.parseInt(sharedPreferences.getString("pref_lesson_plan_color_free_time", "" + Color.GRAY));

        lessons = LessonPlan.getInstance(PreferenceManager.getDefaultSharedPreferences(getActivity())).getLessonsForDay(day);
        LessonViewContent[] lessonViewContent = new LessonViewContent[addInformation ? (lessons.length + (generalInformation == null ? 0 : generalInformation.size())) : lessons.length];
        for (int i = 0; i < lessons.length; i++){
            lessonViewContent[i] = new LessonViewContent();
            lessonViewContent[i].time = (showTime ? (showFullTime ? fullTimes[i] : startTimes[i]) : (i+1)) + ": ";
            if (lessons[i].isFreeTime()) {
                if (i+1 == LessonPlan.LUNCH_BREAK)
                    lessonViewContent[i].content = getString(R.string.lunch_break);
                else
                    lessonViewContent[i].content = getString(R.string.free_time);
                textColors.add(colorFreeTime);
            } else {
                lessonViewContent[i].content = lessons[i].getReadableName();
                textColors.add(color);
                lessonViewContent[i].room = lessons[i].getRoom();
            }
        }
        if (addInformation) {
            if (relevantInformationLessons != null) {
                for (int i = 0; i < relevantInformationLessons.size(); i++) {
                    int lesson = relevantInformationLessons.get(i) - 1;
                    if (lesson < 0 || lesson > lessonViewContent.length) {
                        Log.e("LessonPlanFragment", "createContent: cannot find lesson with index " + lesson);
                        continue;
                    }
                    lessonViewContent[lesson].extraInformation = relevantInformation.get(i);
                    lessonViewContent[lesson].roomExtraInformation = relevantRoomInformation.get(i);
                }
            }
            if (generalInformation != null) {
                for (int i = 0; i < generalInformation.size(); i++) {
                    lessonViewContent[lessons.length + i] = new LessonViewContent();
                    lessonViewContent[lessons.length + i].extraInformation = generalInformation.get(i);
                    lessonViewContent[lessons.length + i].generalInformation = true;
                }
            }
        }
        return lessonViewContent;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id){
        if (getActivity() instanceof LessonPlanActivity) {
            if (position < lessons.length) {
                ((LessonPlanActivity) getActivity()).showEditLessonDialog(lessons[position], this, position);
            }
        } else {
            Log.e("LessonPlanFragment", "getActivity is not a LessonPlanActivity");
        }
    }


    public void update(){
        if (sharedPreferences == null) {
            return;
        }
        showTime = sharedPreferences.getBoolean("pref_lesson_plan_show_time", false);
        showFullTime = sharedPreferences.getBoolean("pref_lesson_plan_show_full_time", false);
        timeViews.clear();
        setListAdapter(new CustomArrayAdapter(getActivity().getApplicationContext(), R.layout.lesson_plan_item, createContent()));
        try {
            getListView().post(updateTimeViewWidth);
        } catch (Exception e) {
            // I can't find a better way to check if listView is created
            Log.v("LessonPlanFragment", "Cannot set time width: " + e);
        }
    }

    private Runnable updateTimeViewWidth = new Runnable() {
        @Override
        public void run() {
            int maxWidth = 0;
            for (int i = 0; i < timeViews.size(); i++) {
                int width = timeViews.get(i).getWidth();
                if (width > maxWidth) {
                    maxWidth = width;
                }
            }
            for (int i = 0; i < timeViews.size(); i++) {
                timeViews.get(i).setWidth(maxWidth);
            }
        }
    };

    public class CustomArrayAdapter extends ArrayAdapter {
        public CustomArrayAdapter (Context context, int resource, LessonViewContent[] objects){
            super(context, resource, objects);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            LessonViewHolder holder;
            View view;

            if (convertView == null){
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.lesson_plan_item, parent, false);

                holder = new LessonViewHolder();
                holder.lessonLayout = (LinearLayout) view.findViewById(R.id.lesson_layout);
                holder.timeView = (TextView) view.findViewById(R.id.time_view);
                holder.subjectView = (TextView) view.findViewById(R.id.subject_view);
                holder.roomView = (TextView) view.findViewById(R.id.room_view);
                holder.informationView = (TextView) view.findViewById(R.id.information_view);
                holder.roomInformationView = (TextView) view.findViewById(R.id.room_information_view);

                view.setTag(holder);
            }
            else{
                holder = (LessonViewHolder) convertView.getTag();
                view = convertView;
            }

            int informationViewTopPadding = 0;
            if (((LessonViewContent) getItem(position)).generalInformation) {
                holder.lessonLayout.setVisibility(View.GONE);
                holder.timeView.setVisibility(View.GONE);
                holder.informationView.setText(((LessonViewContent) getItem(position)).extraInformation);
                holder.informationView.setVisibility(View.VISIBLE);
                holder.informationView.setTextColor(Integer.parseInt(sharedPreferences.getString("pref_lesson_plan_color_general_information", "" + Color.DKGRAY)));
            } else {
                holder.timeView.setText(((LessonViewContent) getItem(position)).time);
                holder.timeView.setVisibility(View.VISIBLE);
                holder.subjectView.setText(((LessonViewContent) getItem(position)).content);
                holder.roomView.setText(((LessonViewContent) getItem(position)).room);
                holder.lessonLayout.setVisibility(View.VISIBLE);
                holder.informationView.setTextColor(Integer.parseInt(sharedPreferences.getString("pref_lesson_plan_color_relevant_information", "" + Color.DKGRAY)));
                if (((LessonViewContent) getItem(position)).extraInformation.equals("")) {
                    holder.informationView.setVisibility(View.GONE);
                } else {
                    holder.informationView.setText(((LessonViewContent) getItem(position)).extraInformation);
                    holder.informationView.setVisibility(View.VISIBLE);
                    informationViewTopPadding = getResources().getDimensionPixelSize(R.dimen.list_content_padding);
                }
                holder.roomInformationView.setTextColor(Integer.parseInt(sharedPreferences.getString("pref_lesson_plan_color_relevant_information", "" + Color.DKGRAY)));
                holder.roomInformationView.setText(((LessonViewContent) getItem(position)).roomExtraInformation);
            }

            holder.informationView.setPadding(holder.informationView.getPaddingLeft(),
                    informationViewTopPadding,
                    holder.informationView.getPaddingRight(),
                    holder.informationView.getPaddingBottom());

            timeViews.add(holder.timeView);

            holder.timeView.setGravity(showTime && showFullTime ? Gravity.CENTER_HORIZONTAL : Gravity.RIGHT);

            holder.timeView.setTextColor(Integer.parseInt(sharedPreferences.getString("pref_lesson_plan_color_time", "" + Color.BLUE)));
            if (textColors.size()>position)
                holder.subjectView.setTextColor(textColors.get(position));
            else
                holder.subjectView.setTextColor(Color.GRAY);//default color for not crashing the app
            holder.roomView.setTextColor(Integer.parseInt(sharedPreferences.getString("pref_lesson_plan_color_room", "" + Color.DKGRAY)));
            return view;
        }
    }

    static class LessonViewHolder{
        LinearLayout lessonLayout;
        TextView timeView, subjectView, roomView, informationView, roomInformationView;
    }

    private class LessonViewContent{
        String time, content, room;
        String extraInformation = "", roomExtraInformation = "";
        boolean generalInformation = false;
    }

    public LessonPlanFragment setRelevantInformation(ArrayList<String> relevantInformation, ArrayList<String> relevantRoomInformation, ArrayList<Integer> relevantInformationLessons, ArrayList<String> generalInformation) {
        if (relevantInformation.size() != relevantInformationLessons.size() || relevantRoomInformation.size() != relevantInformationLessons.size()) {
            Log.e("LessonPlanFragment", "Relevant information arrays don\' match size: " +
                    relevantInformation.size() + "/" +
                    relevantRoomInformation.size() + "/" +
                    relevantInformationLessons.size());
            return this;
        }
        this.relevantInformation = relevantInformation;
        this.relevantRoomInformation = relevantRoomInformation;
        this.relevantInformationLessons = relevantInformationLessons;
        this.generalInformation = generalInformation;
        update();
        return this;
    }
}
