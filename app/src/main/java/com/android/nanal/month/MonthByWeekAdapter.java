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

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;

import com.android.nanal.R;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarController.EventType;
import com.android.nanal.calendar.CalendarController.ViewType;
import com.android.nanal.diary.Diary;
import com.android.nanal.event.Event;
import com.android.nanal.event.Utils;

import java.util.ArrayList;
import java.util.HashMap;


public class MonthByWeekAdapter extends SimpleWeeksAdapter {
    public static final String WEEK_PARAMS_IS_MINI = "mini_month";
    private static final String TAG = "MonthByWeekAdapter";
    private static final long ANIMATE_TODAY_TIMEOUT = 1000;
    // Used to insure minimal time for seeing the click animation before switching views
    // 뷰를 전환하기 전에 클릭 애니메이션을 보기 위한 최소 시간 보장
    private static final int mOnTapDelay = 100;
    protected static int DEFAULT_QUERY_DAYS = 7 * 8; // 8 weeks
    // Minimal time for a down touch action before stating the click animation, this insures that
    // there is no click animation on flings
    // 클릭 애니메이션을 표시하기 전 다운 터치 작업을 위한 최소 시간, 플링에 클릭 애니메이션이 없음을 보장
    private static int mOnDownDelay;
    private static int mTotalClickDelay;
    // Minimal distance to move the finger in order to cancel the click animation
    // 클릭 애니메이션을 취소하기 위해 손가락을 이동하는 최소 거리
    private static float mMovedPixelToCancel;
    private final boolean mShowAgendaWithMonth;
    protected CalendarController mController;
    protected String mHomeTimeZone;
    protected Time mTempTime;
    protected Time mToday;
    protected int mFirstJulianDay;
    protected int mQueryDays;
    protected boolean mIsMiniMonth = true;
    protected int mOrientation = Configuration.ORIENTATION_LANDSCAPE;
    protected ArrayList<ArrayList<Event>> mEventDayList = new ArrayList<ArrayList<Event>>();
    protected ArrayList<Event> mEvents = null;
    protected ArrayList<ArrayList<Diary>> mDiaryDayList = new ArrayList<ArrayList<Diary>>();
    protected ArrayList<Diary> mDiaries = null;
    MonthWeekEventsView mClickedView;
    MonthWeekEventsView mSingleTapUpView;
    MonthWeekEventsView mLongClickedView;
    float mClickedXLocation;                // Used to find which day was clicked
    // 클릭된 요일을 찾는 데 사용됨
    // Perform the tap animation in a runnable to allow a delay before showing the tap color.
    // This is done to prevent a click animation when a fling is done.
    // 탭 색을 보여 주기 전에 딜레이를 허용하려면 실행 가능한 탭 애니메이션을 수행함
    // 플링이 수행될 때 클릭 애니메이션을 방지하기 위해 수행됨

    private final Runnable mDoClick = new Runnable() {
        @Override
        public void run() {
            if (mClickedView != null) {
                synchronized (mClickedView) {
                    mClickedView.setClickedDay(mClickedXLocation);
                }
                mLongClickedView = mClickedView;
                mClickedView = null;
                // This is a workaround , sometimes the top item on the listview doesn't refresh on
                // invalidate, so this forces a re-draw.
                // 때때로 목록뷰의 맨 위 항목은 무효화 시 새로 고쳐지지 않기 때문에 다시 그려야 함
                mListView.invalidate();
            }
        }
    };
    // Performs the single tap operation: go to the tapped day.
    // This is done in a runnable to allow the click animation to finish before switching views
    // 단일 탭 작업 수행: 탭된 날짜로 이동
    // 뷰를 전환하기 전에 클릭 애니메이션을 완료할 수 있도록 runnable로 수행됨

    private final Runnable mDoSingleTapUp = new Runnable() {
        @Override
        public void run() {
            if (mSingleTapUpView != null) {
                Time day = mSingleTapUpView.getDayFromLocation(mClickedXLocation);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Touched day at Row=" + mSingleTapUpView.mWeek + " day=" + day.toString());
                }
                if (day != null) {
                    onDayTapped(day);
                }
                clearClickedView(mSingleTapUpView);
                mSingleTapUpView = null;
            }
        }
    };
    long mClickTime;                        // Used to calculate minimum click animation time, 최소 클릭 애니메이션 시간 계산에 사용
    private boolean mAnimateToday = false;
    private long mAnimateTime = 0;
    private Handler mEventDialogHandler;

    public MonthByWeekAdapter(Context context, HashMap<String, Integer> params, Handler handler) {
        super(context, params);
        mEventDialogHandler = handler;
        if (params.containsKey(WEEK_PARAMS_IS_MINI)) {
            mIsMiniMonth = params.get(WEEK_PARAMS_IS_MINI) != 0;
        }
        mShowAgendaWithMonth = Utils.getConfigBool(context, R.bool.show_agenda_with_month);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mOnDownDelay = ViewConfiguration.getTapTimeout();
        mMovedPixelToCancel = vc.getScaledTouchSlop();
        mTotalClickDelay = mOnDownDelay + mOnTapDelay;
    }

    public void animateToday() {
        mAnimateToday = true;
        mAnimateTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        mGestureDetector = new GestureDetector(mContext, new CalendarGestureListener());
        mController = CalendarController.getInstance(mContext);
        mHomeTimeZone = Utils.getTimeZone(mContext, null);
        mSelectedDay.switchTimezone(mHomeTimeZone);
        mToday = new Time(mHomeTimeZone);
        mToday.setToNow();
        mTempTime = new Time(mHomeTimeZone);
    }

    private void updateTimeZones() {
        mSelectedDay.timezone = mHomeTimeZone;
        mSelectedDay.normalize(true);
        mToday.timezone = mHomeTimeZone;
        mToday.setToNow();
        mTempTime.switchTimezone(mHomeTimeZone);
    }

    @Override
    public void setSelectedDay(Time selectedTime) {
        mSelectedDay.set(selectedTime);
        long millis = mSelectedDay.normalize(true);
        mSelectedWeek = Utils.getWeeksSinceEpochFromJulianDay(
                Time.getJulianDay(millis, mSelectedDay.gmtoff), mFirstDayOfWeek);
        notifyDataSetChanged();
    }

    public void setEvents(int firstJulianDay, int numDays, ArrayList<Event> events) {
        if (mIsMiniMonth) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Attempted to set events for mini view. Events only supported in full"
                        + " view.");
            }
            return;
        }
        mEvents = events;
        mFirstJulianDay = firstJulianDay;
        mQueryDays = numDays;
        // Create a new list, this is necessary since the weeks are referencing
        // pieces of the old list
        // 새 리스트 작성, 몇 주가 이전 리스트의 일부를 참조하기 때문이 이 작업이 필요함
        ArrayList<ArrayList<Event>> eventDayList = new ArrayList<ArrayList<Event>>();
        for (int i = 0; i < numDays; i++) {
            eventDayList.add(new ArrayList<Event>());
        }

        if (events == null || events.size() == 0) {
            if(Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No events. Returning early--go schedule something fun.");
            }
            mEventDayList = eventDayList;
            refresh();
            return;
        }

        // Compute the new set of days with events
        // 이벤트를 사용하여 이벤트가 있는 일의 새로운 집합을 계산
        for (Event event : events) {
            int startDay = event.startDay - mFirstJulianDay;
            int endDay = event.endDay - mFirstJulianDay + 1;
            if (startDay < numDays || endDay >= 0) {
                if (startDay < 0) {
                    startDay = 0;
                }
                if (startDay > numDays) {
                    continue;
                }
                if (endDay < 0) {
                    continue;
                }
                if (endDay > numDays) {
                    endDay = numDays;
                }
                for (int j = startDay; j < endDay; j++) {
                    eventDayList.get(j).add(event);
                }
            }
        }
        if(Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Processed " + events.size() + " events.");
        }
        mEventDayList = eventDayList;
        refresh();
    }

    public void setDiaries(int firstJulianDay, int numDays, ArrayList<Diary> diaries) {
        if (mIsMiniMonth) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Attempted to set events for mini view. Events only supported in full"
                        + " view.");
            }
            return;
        }
        mDiaries = diaries;
        mFirstJulianDay = firstJulianDay;
        mQueryDays = numDays;
        // Create a new list, this is necessary since the weeks are referencing
        // pieces of the old list
        // 새 리스트 작성, 몇 주가 이전 리스트의 일부를 참조하기 때문이 이 작업이 필요함
        ArrayList<ArrayList<Diary>> diaryDayList = new ArrayList<ArrayList<Diary>>();
        for (int i = 0; i < numDays; i++) {
            diaryDayList.add(new ArrayList<Diary>());
        }

        if (diaries == null || diaries.size() == 0) {
            if(Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No diaries. Returning early--go schedule something fun.");
            }
            mDiaryDayList = diaryDayList;
            refresh();
            return;
        }

        // Compute the new set of days with events
        // 이벤트를 사용하여 이벤트가 있는 일의 새로운 집합을 계산
        for (Diary diary : diaries) {
            long day = diary.day;
            long startDay = day - mFirstJulianDay;
            diaryDayList.get((int)day).add(diary);
        }
        if(Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Processed " + diaries.size() + " diaries.");
        }
        mDiaryDayList = diaryDayList;
        refresh();
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mIsMiniMonth) {
            return super.getView(position, convertView, parent);
        }
        MonthWeekEventsView v;
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        HashMap<String, Integer> drawingParams = null;
        boolean isAnimatingToday = false;
        if (convertView != null) {
            v = (MonthWeekEventsView) convertView;
            // Checking updateToday uses the current params instead of the new
            // params, so this is assuming the view is relatively stable
            // updateToday 검사 시 새 매개변수 대신 현재 매개변수를 사용하고,
            // 이 경우 뷰가 비교적 안정적이라고 가정함
            if (mAnimateToday && v.updateToday(mSelectedDay.timezone)) {
                long currentTime = System.currentTimeMillis();
                // If it's been too long since we tried to start the animation
                // don't show it. This can happen if the user stops a scroll
                // before reaching today.
                // 애니메이션을 시작한 지 너무 오래되었다면 보여 주지 않음
                // 사용자가 오늘에 도달하기 전에 스크롤을 중지할 경우 문제가 발생할 수 있음
                if (currentTime - mAnimateTime > ANIMATE_TODAY_TIMEOUT) {
                    mAnimateToday = false;
                    mAnimateTime = 0;
                } else {
                    isAnimatingToday = true;
                    // There is a bug that causes invalidates to not work some
                    // of the time unless we recreate the view.
                    // 뷰를 다시 만들지 않는 한 어느 정도 무효화되는 버그가 있음
                    v = new MonthWeekEventsView(mContext);
                }
            } else {
                drawingParams = (HashMap<String, Integer>) v.getTag();
            }
        } else {
            v = new MonthWeekEventsView(mContext);
        }
        if (drawingParams == null) {
            drawingParams = new HashMap<String, Integer>();
        }
        drawingParams.clear();

        v.setLayoutParams(params);
        v.setClickable(true);
        v.setOnTouchListener(this);

        int selectedDay = -1;
        if (mSelectedWeek == position) {
            selectedDay = mSelectedDay.weekDay;
        }

        drawingParams.put(SimpleWeekView.VIEW_PARAMS_HEIGHT, parent.getHeight() / mNumWeeks);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_SELECTED_DAY, selectedDay);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_SHOW_WK_NUM, mShowWeekNumber ? 1 : 0);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_WEEK_START, mFirstDayOfWeek);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_NUM_DAYS, mDaysPerWeek);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_WEEK, position);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_FOCUS_MONTH, mFocusMonth);
        drawingParams.put(MonthWeekEventsView.VIEW_PARAMS_ORIENTATION, mOrientation);

        if (isAnimatingToday) {
            drawingParams.put(MonthWeekEventsView.VIEW_PARAMS_ANIMATE_TODAY, 1);
            mAnimateToday = false;
        }

        v.setWeekParams(drawingParams, mSelectedDay.timezone);
        sendEventsToView(v);
        return v;
    }

    private void sendEventsToView(MonthWeekEventsView v) {
        if (mEventDayList.size() == 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No events loaded, did not pass any events to view.");
            }
            v.setEvents(null, null);
            return;
        }
        int viewJulianDay = v.getFirstJulianDay();
        int start = viewJulianDay - mFirstJulianDay;
        int end = start + v.mNumDays;
        if (start < 0 || end > mEventDayList.size()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Week is outside range of loaded events. viewStart: " + viewJulianDay
                        + " eventsStart: " + mFirstJulianDay);
            }
            v.setEvents(null, null);
            return;
        }
        v.setEvents(mEventDayList.subList(start, end), mEvents);
    }

    private void sendDiariesToView(MonthWeekEventsView v) {
        if (mEventDayList.size() == 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No events loaded, did not pass any events to view.");
            }
            v.setEvents(null, null);
            return;
        }
        int viewJulianDay = v.getFirstJulianDay();
        int start = viewJulianDay - mFirstJulianDay;
        int end = start + v.mNumDays;
        if (start < 0 || end > mEventDayList.size()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Week is outside range of loaded events. viewStart: " + viewJulianDay
                        + " eventsStart: " + mFirstJulianDay);
            }
            v.setEvents(null, null);
            return;
        }
        v.setEvents(mEventDayList.subList(start, end), mEvents);
    }

    @Override
    protected void refresh() {
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        mShowWeekNumber = Utils.getShowWeekNumber(mContext);
        mHomeTimeZone = Utils.getTimeZone(mContext, null);
        mOrientation = mContext.getResources().getConfiguration().orientation;
        updateTimeZones();
        notifyDataSetChanged();
    }

    @Override
    protected void onDayTapped(Time day) {
        setDayParameters(day);
        if (mShowAgendaWithMonth || mIsMiniMonth) {
            // If agenda view is visible with month view , refresh the views
            // with the selected day's info
            // 월별 뷰로 agenda 뷰를 볼 수 있는 경우, 선택한 요일 정보를 사용해서 뷰를 새로 고침
            mController.sendEvent(mContext, EventType.GO_TO, day, day, -1,
                    ViewType.CURRENT, CalendarController.EXTRA_GOTO_DATE, null, null);
        } else {
            // Else , switch to the detailed view
            // 아니라면 상세 보기로 전환
            mController.sendEvent(mContext, EventType.GO_TO, day, day, -1,
                    ViewType.DETAIL,
                    CalendarController.EXTRA_GOTO_DATE
                            | CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS, null, null);
        }
    }

    private void setDayParameters(Time day) {
        day.timezone = mHomeTimeZone;
        Time currTime = new Time(mHomeTimeZone);
        currTime.set(mController.getTime());
        day.hour = currTime.hour;
        day.minute = currTime.minute;
        day.allDay = false;
        day.normalize(true);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!(v instanceof MonthWeekEventsView)) {
            return super.onTouch(v, event);
        }

        int action = event.getAction();

        // Event was tapped - switch to the detailed view making sure the click animation
        // is done first.
        // 이벤트 탭 - 클릭 애니메이션을 먼저 수행하도록 디테일 뷰로 전환함
        if (mGestureDetector.onTouchEvent(event)) {
            mSingleTapUpView = (MonthWeekEventsView) v;
            long delay = System.currentTimeMillis() - mClickTime;
            // Make sure the animation is visible for at least mOnTapDelay - mOnDownDelay ms
            // 애니메이션이 mOnTapDelay - mOnDownDelay ms 이상 표시되는지 확인
            mListView.postDelayed(mDoSingleTapUp,
                    delay > mTotalClickDelay ? 0 : mTotalClickDelay - delay);
            return true;
        } else {
            // Animate a click - on down: show the selected day in the "clicked" color.
            // On Up/scroll/move/cancel: hide the "clicked" color.
            // 클릭 - 아래: 선택한 날짜를 "clicked" 색상으로 표시함
            // 위 / 스크롤 / 이동 / 취소: "clicked" 색상을 숨김
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mClickedView = (MonthWeekEventsView) v;
                    mClickedXLocation = event.getX();
                    mClickTime = System.currentTimeMillis();
                    mListView.postDelayed(mDoClick, mOnDownDelay);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_SCROLL:
                case MotionEvent.ACTION_CANCEL:
                    clearClickedView((MonthWeekEventsView) v);
                    break;
                case MotionEvent.ACTION_MOVE:
                    // No need to cancel on vertical movement, ACTION_SCROLL will do that.
                    // 수직 이동시에는 취소할 필요가 없고, ACTION_SCROLL이 이를 수행함
                    if (Math.abs(event.getX() - mClickedXLocation) > mMovedPixelToCancel) {
                        clearClickedView((MonthWeekEventsView) v);
                    }
                    break;
                default:
                    break;
            }
        }
        // Do not tell the frameworks we consumed the touch action so that fling actions can be
        // processed by the fragment.
        // 플링 액션이 fragment에 의해 처리될 수 있게 터치 액션을 사용한 프레임워크를 언급하지 않음
        return false;
    }

    // Clear the visual cues of the click animation and related running code.
    // 클릭 애니메이션 및 관련 실행 코드의 시각적 단서를 지움
    private void clearClickedView(MonthWeekEventsView v) {
        mListView.removeCallbacks(mDoClick);
        synchronized(v) {
            v.clearClickedDay();
        }
        mClickedView = null;
    }

    /**
     * This is here so we can identify events and process them
     * 이벤트를 구분하고 처리할 수 있게
     */
    protected class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (mLongClickedView != null) {
                Time day = mLongClickedView.getDayFromLocation(mClickedXLocation);
                if (day != null) {
                    mLongClickedView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    Message message = new Message();
                    message.obj = day;
                    mEventDialogHandler.sendMessage(message);
                }
                mLongClickedView.clearClickedDay();
                mLongClickedView = null;
            }
        }
    }
}
