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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import de.spiritcroc.akg_vertretungsplan.settings.Keys;

public class LessonPlanFragment extends ListFragment
        implements AdapterView.OnItemLongClickListener {
    private static final String ARG_DAY = "day";

    private static final String FREE_SUBSTITUTION = "→  entfällt";

    private int day;
    private String date;
    private Lesson[] lessons;
    private String[] startTimes;
    private String[] fullTimes;
    private String[] headerRow;
    private ArrayList<Integer> textColors = new ArrayList<>();
    private ArrayList<String> relevantInformation, relevantRoomInformation, generalInformation;
    private ArrayList<Integer> relevantInformationLessons;
    private ArrayList<String[]> fullAddedInformation;

    private ArrayList<TextView> timeViews = new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private boolean showTime;
    private boolean showFullTime;
    private boolean showInformation;
    private int currentLesson = -1;

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
        getListView().setDrawSelectorOnTop(true);
        getListView().setOnItemLongClickListener(this);
    }

    private LessonViewContent[] createContent(){
        textColors.clear();

        showInformation = sharedPreferences.getBoolean(Keys.LESSON_PLAN_SHOW_INFORMATION, false);

        int color = Integer.parseInt(sharedPreferences.getString(Keys.LESSON_PLAN_COLOR_LESSON, "" + Color.BLACK)),
                colorFreeTime = Integer.parseInt(sharedPreferences.getString(Keys.LESSON_PLAN_COLOR_FREE_TIME, "" + Color.GRAY));

        lessons = LessonPlan.getInstance(sharedPreferences).getLessonsForDay(day);
        LessonViewContent[] lessonViewContent = new LessonViewContent[showInformation ? (lessons.length + (generalInformation == null ? 0 : generalInformation.size())) : lessons.length];
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
        if (showInformation) {
            if (relevantInformationLessons != null) {
                for (int i = 0; i < relevantInformationLessons.size(); i++) {
                    int lesson = relevantInformationLessons.get(i) - 1;
                    if (lesson < 0 || lesson > lessonViewContent.length) {
                        Log.e("LessonPlanFragment", "createContent: cannot find lesson with index " + lesson);
                        continue;
                    }
                    if (relevantInformation.get(i).equals(FREE_SUBSTITUTION)) {
                        textColors.remove(lesson);
                        textColors.add(lesson, colorFreeTime);
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
                if (showInformation) {
                    for (int i = 0; i < fullAddedInformation.size(); i++) {
                        if (fullAddedInformation.get(i)[2].equals("" + (position + 1))) {
                            String text = ItemFragment.makeDialogMessage(getActivity(),
                                    fullAddedInformation.get(i), headerRow);
                            if (text != null) {
                                String shareText = ItemFragment.makeDialogShareMessage(getActivity(),
                                        date, text);
                                ElementDialog.newInstance(date, text, shareText)
                                        .show(getActivity().getFragmentManager(), "ElementDialog");
                            }
                        }
                    }
                } else {
                    ((LessonPlanActivity) getActivity()).showEditLessonDialog(lessons[position], this, position);
                }
            } else {
                // General information
                String text = ItemFragment.makeDialogMessage(getActivity(), fullAddedInformation.get(position-lessons.length), headerRow);
                if (text != null) {
                    String shareText = ItemFragment.makeDialogShareMessage(getActivity(),
                            date, text);
                    ElementDialog.newInstance(date, text, shareText)
                            .show(getActivity().getFragmentManager(), "ElementDialog");
                }
            }
        } else {
            Log.e("LessonPlanFragment", "getActivity is not a LessonPlanActivity");
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < lessons.length) {
            ((LessonPlanActivity) getActivity()).showEditLessonDialog(lessons[position], this, position);
            return true;
        } else {
            return false;
        }
    }

    public LessonPlanFragment markCurrentLesson(int lesson) {
        currentLesson = lesson;
        update();
        return this;
    }

    public void update(){
        if (sharedPreferences == null) {
            return;
        }
        showTime = sharedPreferences.getBoolean(Keys.LESSON_PLAN_SHOW_TIME, false);
        showFullTime = sharedPreferences.getBoolean(Keys.LESSON_PLAN_SHOW_FULL_TIME, false);
        timeViews.clear();
        setListAdapter(new CustomArrayAdapter(getActivity().getApplicationContext(), R.layout.lesson_plan_item, createContent()));
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
                holder.itemLayout = (LinearLayout) view.findViewById(R.id.item_layout);
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

            holder.itemLayout.setBackgroundColor(position == currentLesson ? Integer.parseInt(sharedPreferences.getString(Keys.LESSON_PLAN_BG_COLOR_CURRENT_LESSON, "" + Color.LTGRAY)): Color.TRANSPARENT);

            int informationViewTopPadding = 0;
            if (((LessonViewContent) getItem(position)).generalInformation) {
                holder.lessonLayout.setVisibility(View.GONE);
                holder.timeView.setVisibility(View.GONE);
                holder.informationView.setText(((LessonViewContent) getItem(position)).extraInformation);
                holder.informationView.setVisibility(View.VISIBLE);
                holder.informationView.setTextColor(Integer.parseInt(sharedPreferences.getString(Keys.LESSON_PLAN_COLOR_GENERAL_INFORMATION, "" + Color.DKGRAY)));
            } else {
                holder.timeView.setText(((LessonViewContent) getItem(position)).time);
                holder.timeView.setVisibility(View.VISIBLE);
                holder.subjectView.setText(((LessonViewContent) getItem(position)).content);
                holder.roomView.setText(((LessonViewContent) getItem(position)).room);
                holder.lessonLayout.setVisibility(View.VISIBLE);
                holder.informationView.setTextColor(Integer.parseInt(sharedPreferences.getString(Keys.LESSON_PLAN_COLOR_RELEVANT_INFORMATION, "" + Color.DKGRAY)));
                if (((LessonViewContent) getItem(position)).extraInformation.equals("")) {
                    holder.informationView.setVisibility(View.GONE);
                } else {
                    if (((LessonViewContent) getItem(position)).extraInformation.equals(FREE_SUBSTITUTION)) {
                        // Strike through
                        Spannable subject = new SpannableString(holder.subjectView.getText());
                        subject.setSpan(new StrikethroughSpan(), 0, subject.length(), 0);
                        holder.subjectView.setText(subject);
                    }
                    holder.informationView.setText(((LessonViewContent) getItem(position)).extraInformation);
                    holder.informationView.setVisibility(View.VISIBLE);
                    informationViewTopPadding = getResources().getDimensionPixelSize(R.dimen.list_content_padding);
                }
                holder.roomInformationView.setTextColor(Integer.parseInt(sharedPreferences.getString(Keys.LESSON_PLAN_COLOR_RELEVANT_INFORMATION, "" + Color.DKGRAY)));
                holder.roomInformationView.setText(((LessonViewContent) getItem(position)).roomExtraInformation);
            }

            holder.informationView.setPadding(holder.informationView.getPaddingLeft(),
                    informationViewTopPadding,
                    holder.informationView.getPaddingRight(),
                    holder.informationView.getPaddingBottom());

            timeViews.add(holder.timeView);

            holder.timeView.setGravity(showTime && showFullTime ? Gravity.CENTER_HORIZONTAL : Gravity.RIGHT);

            holder.timeView.setTextColor(Integer.parseInt(sharedPreferences.getString(Keys.LESSON_PLAN_COLOR_TIME, "" + Color.BLUE)));
            if (textColors.size()>position)
                holder.subjectView.setTextColor(textColors.get(position));
            else
                holder.subjectView.setTextColor(Color.GRAY);//default color for not crashing the app
            holder.roomView.setTextColor(Integer.parseInt(sharedPreferences.getString(Keys.LESSON_PLAN_COLOR_ROOM, "" + Color.DKGRAY)));

            view.post(updateTimeViewWidth);

            return view;
        }
    }

    static class LessonViewHolder{
        LinearLayout itemLayout, lessonLayout;
        TextView timeView, subjectView, roomView, informationView, roomInformationView;
    }

    private class LessonViewContent{
        String time, content, room;
        String extraInformation = "", roomExtraInformation = "";
        boolean generalInformation = false;
    }

    public LessonPlanFragment setRelevantInformation(ArrayList<String> relevantInformation,
                                                     ArrayList<String> relevantRoomInformation,
                                                     ArrayList<Integer> relevantInformationLessons,
                                                     ArrayList<String> generalInformation,
                                                     String date,
                                                     String[] headerRow,
                                                     ArrayList<String[]> informationCells) {
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
        this.date = date;
        this.headerRow = headerRow;
        fullAddedInformation = informationCells;
        update();
        return this;
    }
}
