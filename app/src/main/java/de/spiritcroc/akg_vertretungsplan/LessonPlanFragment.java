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
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class LessonPlanFragment extends ListFragment {
    private static final String ARG_DAY = "day";

    private int day;
    private Lesson[] lessons;
    private ArrayList<Integer> textColors = new ArrayList<>();

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

        update();
    }

    private String[] createContent(){
        textColors.clear();
        int style = Tools.getStyle(getActivity());
        boolean lightStyle = style == R.style.AppTheme || style == R.style.Theme_AppCompat_Light || style == R.style.Theme_AppCompat_Light_DarkActionBar;

        lessons = LessonPlan.getInstance(PreferenceManager.getDefaultSharedPreferences(getActivity())).getLessonsForDay(day);
        String shownContent[] = new String[lessons.length];
        for (int i = 0; i < shownContent.length; i++){
            shownContent[i] = (i+1) + ": ";
            if (lessons[i].isFreeTime()) {
                if (i+1 == LessonPlan.LUNCH_BREAK)
                    shownContent[i] += getString(R.string.lunch_break);
                else
                    shownContent[i] += getString(R.string.free_time);
                textColors.add(Color.GRAY);
            } else {
                shownContent[i] += lessons[i].getReadableName();
                textColors.add(lightStyle ? Color.BLACK : Color.WHITE);
            }
        }
        return shownContent;
    }
    @Override
    public void onPause(){
        super.onPause();
        LessonPlan.getInstance(PreferenceManager.getDefaultSharedPreferences(getActivity())).saveLessons();
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id){
        if (getActivity() instanceof LessonPlanActivity)
            ((LessonPlanActivity) getActivity()).showEditLessonDialog(lessons[position], this, position);
        else
            Log.e("LessonPlanFragment", "getActivity is not a LessonPlanActivity");
    }


    public void update(){
        setListAdapter(new CustomArrayAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, createContent()));
    }

    public class CustomArrayAdapter extends ArrayAdapter {
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
                textView.setTextColor(Color.GRAY);//default color for not crashing the app
            return view;
        }
    }
}
