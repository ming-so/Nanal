package com.android.nanal.month;

/*
 * Copyright (C) 2010 The Android Open Source Project
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

// TODO Remove calendar imports when the required methods have been
// refactored into the public api
import android.content.Context;
import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.android.nanal.calendar.CalendarController;
import com.android.nanal.event.Utils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

/**
 * <p>
 * This is a specialized adapter for creating a list of weeks with selectable
 * days. It can be configured to display the week number, start the week on a
 * given day, show a reduced number of days, or display an arbitrary number of
 * weeks at a time. See {@link SimpleDayPickerFragment} for usage.
 * 이 어댑터는 선택 가능한 요일을 포함한 주 목록을 생성하기 위한 특수 어댑터임
 * 주 번호를 표시하거나, 지정된 요일에 주를 시작하거나, 일수를 줄이거나,
 * 한 번에 임의의 주 수를 표시하도록 구성 가능함
 * 사용 방법은 link
 *
 * </p>
 */
public class SimpleWeeksAdapter extends BaseAdapter implements OnTouchListener {

    private static final String TAG = "MonthByWeek";

    /**
     * The number of weeks to display at a time.
     * 한 번에 표시할 수 있는 주의 갯수
     */
    public static final String WEEK_PARAMS_NUM_WEEKS = "num_weeks";
    /**
     * Which month should be in focus currently.
     * 현재 어느 달에 집중해야 하는지
     */
    public static final String WEEK_PARAMS_FOCUS_MONTH = "focus_month";
    /**
     * Whether the week number should be shown. Non-zero to show them.
     * 주 번호를 표시할지의 여부, 0이 아니면 표시함
     */
    public static final String WEEK_PARAMS_SHOW_WEEK = "week_numbers";
    /**
     * Which day the week should start on. {@link Time#SUNDAY} through
     * {@link Time#SATURDAY}.
     * 어느 요일에 시작할 것인지, SUNDAY부터 SATURDAY까지
     */
    public static final String WEEK_PARAMS_WEEK_START = "week_start";
    /**
     * The Julian day to highlight as selected.
     * 선택된 대로 강조할 줄리안 데이
     */
    public static final String WEEK_PARAMS_JULIAN_DAY = "selected_day";
    /**
     * How many days of the week to display [1-7].
     * 한 주에 얼마나 많은 날을 표시할 것인지 [1-7]
     */
    public static final String WEEK_PARAMS_DAYS_PER_WEEK = "days_per_week";

    protected static final int WEEK_COUNT = CalendarController.MAX_CALENDAR_WEEK
            - CalendarController.MIN_CALENDAR_WEEK;
    protected static int DEFAULT_NUM_WEEKS = 6;
    protected static int DEFAULT_MONTH_FOCUS = 0;
    protected static int DEFAULT_DAYS_PER_WEEK = 7;
    protected static int DEFAULT_WEEK_HEIGHT = 32;
    protected static int WEEK_7_OVERHANG_HEIGHT = 7;

    protected static float mScale = 0;
    protected Context mContext;
    // The day to highlight as selected
    // 선택된 대로 강조할 날
    protected Time mSelectedDay;
    // The week since 1970 that the selected day is in
    // 선택한 날이 있는 주(1970년 이후)
    protected int mSelectedWeek;
    // When the week starts; numbered like Time.<WEEKDAY> (e.g. SUNDAY=0).
    // 언제 주가 시작하는지; Time.<WEEKDAY>처럼 넘버링함 (SUNDAY=0)
    protected int mFirstDayOfWeek;
    protected boolean mShowWeekNumber = false;
    protected GestureDetector mGestureDetector;
    protected int mNumWeeks = DEFAULT_NUM_WEEKS;
    protected int mDaysPerWeek = DEFAULT_DAYS_PER_WEEK;
    protected int mFocusMonth = DEFAULT_MONTH_FOCUS;

    public SimpleWeeksAdapter(Context context, HashMap<String, Integer> params) {
        mContext = context;

        // Get default week start based on locale, subtracting one for use with android Time.
        // android time에 사용할 기본 주 시작을 가져옴, 1을 뺌
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        mFirstDayOfWeek = cal.getFirstDayOfWeek() - 1;

        if (mScale == 0) {
            mScale = context.getResources().getDisplayMetrics().density;
            if (mScale != 1) {
                WEEK_7_OVERHANG_HEIGHT *= mScale;
            }
        }
        init();
        updateParams(params);
    }

    /**
     * Set up the gesture detector and selected time
     * 제스쳐 디텍터 및 선택한 시간 설정
     */
    protected void init() {
        mGestureDetector = new GestureDetector(mContext, new CalendarGestureListener());
        mSelectedDay = new Time();
        mSelectedDay.setToNow();
    }

    /**
     * Parse the parameters and set any necessary fields. See
     * {@link #WEEK_PARAMS_NUM_WEEKS} for parameter details.
     * 매개변수를 분석하고 필요한 필드를 설정함
     *
     * @param params A list of parameters for this adapter
     *                이 어댑터의 매개 변수 목록
     */
    public void updateParams(HashMap<String, Integer> params) {
        if (params == null) {
            Log.e(TAG, "WeekParameters are null! Cannot update adapter.");
            return;
        }
        if (params.containsKey(WEEK_PARAMS_FOCUS_MONTH)) {
            mFocusMonth = params.get(WEEK_PARAMS_FOCUS_MONTH);
        }
        if (params.containsKey(WEEK_PARAMS_FOCUS_MONTH)) {
            mNumWeeks = params.get(WEEK_PARAMS_NUM_WEEKS);
        }
        if (params.containsKey(WEEK_PARAMS_SHOW_WEEK)) {
            mShowWeekNumber = params.get(WEEK_PARAMS_SHOW_WEEK) != 0;
        }
        if (params.containsKey(WEEK_PARAMS_WEEK_START)) {
            mFirstDayOfWeek = params.get(WEEK_PARAMS_WEEK_START);
        }
        if (params.containsKey(WEEK_PARAMS_JULIAN_DAY)) {
            int julianDay = params.get(WEEK_PARAMS_JULIAN_DAY);
            mSelectedDay.setJulianDay(julianDay);
            mSelectedWeek = Utils.getWeeksSinceEpochFromJulianDay(julianDay, mFirstDayOfWeek);
        }
        if (params.containsKey(WEEK_PARAMS_DAYS_PER_WEEK)) {
            mDaysPerWeek = params.get(WEEK_PARAMS_DAYS_PER_WEEK);
        }
        refresh();
    }

    /**
     * Updates the selected day and related parameters.
     * 선택된 날짜 및 관련 매개변수 업데이트
     *
     * @param selectedTime The time to highlight
     *                       하이라이트할 시간
     */
    public void setSelectedDay(Time selectedTime) {
        mSelectedDay.set(selectedTime);
        long millis = mSelectedDay.normalize(true);
        mSelectedWeek = Utils.getWeeksSinceEpochFromJulianDay(
                Time.getJulianDay(millis, mSelectedDay.gmtoff), mFirstDayOfWeek);
        notifyDataSetChanged();
    }

    /**
     * Returns the currently highlighted day
     * 현재 하이라이트된 날짜 반환함
     *
     * @return
     */
    public Time getSelectedDay() {
        return mSelectedDay;
    }

    /**
     * updates any config options that may have changed and refreshes the view
     * 변경되었을 수 있는 모든 구성 옵션 업데이트 및 view 새로 고침
     */
    protected void refresh() {
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return WEEK_COUNT;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SimpleWeekView v;
        HashMap<String, Integer> drawingParams = null;
        if (convertView != null) {
            v = (SimpleWeekView) convertView;
            // We store the drawing parameters in the view so it can be recycled
            // drawing 매개변수를 view에 저장해서 재활용할 수 있도록 함
            drawingParams = (HashMap<String, Integer>) v.getTag();
        } else {
            v = new SimpleWeekView(mContext);
            // Set up the new view
            // 새 view를 설정함
            LayoutParams params = new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            v.setLayoutParams(params);
            v.setClickable(true);
            v.setOnTouchListener(this);
        }
        if (drawingParams == null) {
            drawingParams = new HashMap<String, Integer>();
        }
        drawingParams.clear();

        int selectedDay = -1;
        if (mSelectedWeek == position) {
            selectedDay = mSelectedDay.weekDay;
        }

        // pass in all the view parameters
        // 모든 view 매개변수를 패스함?
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_HEIGHT,
                (parent.getHeight() - WEEK_7_OVERHANG_HEIGHT) / mNumWeeks);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_SELECTED_DAY, selectedDay);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_SHOW_WK_NUM, mShowWeekNumber ? 1 : 0);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_WEEK_START, mFirstDayOfWeek);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_NUM_DAYS, mDaysPerWeek);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_WEEK, position);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_FOCUS_MONTH, mFocusMonth);
        v.setWeekParams(drawingParams, mSelectedDay.timezone);
        v.invalidate();

        return v;
    }

    /**
     * Changes which month is in focus and updates the view.
     * 달이 초점 맞춰져 있는지 아닌지 변경하고 및 view를 업데이트함
     *
     * @param month The month to show as in focus [0-11]
     *               초점 맞출 월 [0-11]
     */
    public void updateFocusMonth(int month) {
        mFocusMonth = month;
        notifyDataSetChanged();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) {
            SimpleWeekView view = (SimpleWeekView) v;
            Time day = ((SimpleWeekView)v).getDayFromLocation(event.getX());
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Touched day at Row=" + view.mWeek + " day=" + day.toString());
            }
            if (day != null) {
                onDayTapped(day);
            }
            return true;
        }
        return false;
    }

    /**
     * Maintains the same hour/min/sec but moves the day to the tapped day.
     * 동일한 시/분/초를 유지하지만 해당 날짜를 탭된 날짜로 이동함
     *
     * @param day The day that was tapped
     *             탭된 날짜
     */
    protected void onDayTapped(Time day) {
        day.hour = mSelectedDay.hour;
        day.minute = mSelectedDay.minute;
        day.second = mSelectedDay.second;
        setSelectedDay(day);
    }


    /**
     * This is here so we can identify single tap events and set the selected
     * day correctly
     * 단일 탭 이벤트를 식별하고 선택한 날짜를 올바르게 설정할 수 있도록
     */
    protected class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }
    }

    ListView mListView;

    public void setListView(ListView lv) {
        mListView = lv;
    }
}
