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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

import com.android.nanal.DynamicTheme;
import com.android.nanal.R;
import com.android.nanal.event.Utils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;


/**
 * <p>
 * This displays a titled list of weeks with selectable days. It can be
 * configured to display the week number, start the week on a given day, show a
 * reduced number of days, or display an arbitrary number of weeks at a time. By
 * overriding methods and changing variables this fragment can be customized to
 * easily display a month selection component in a given style.
 * 선택 가능한 요일과 함께 주 목록을 표시함
 * 주 번호를 표시하거나, 지정된 요일에 주를 시작하거나, 일수를 줄이거나,
 * 한번에 임의의 주 수를 표시하도록 구성할 수 있음
 * 메소드를 오버라이딩하고 변수를 변경함으로써 주어진 스타일로 달 선택 컴포넌트를 쉽게 표시하도록 사용자 정의할 수 있음
 * </p>
 */
@SuppressLint("ValidFragment")
public class SimpleDayPickerFragment extends ListFragment implements OnScrollListener {

    // The number of days to display in each week
    // 매주 표시할 일수
    public static final int DAYS_PER_WEEK = 7;
    // Affects when the month selection will change while scrolling up
    // 위로 스크롤하는 동안 선택 월이 변경될 때에 영향
    protected static final int SCROLL_HYST_WEEKS = 2;
    // How long the GoTo fling animation should last
    // GoTo 플링 애니메이션의 지속 시간
    protected static final int GOTO_SCROLL_DURATION = 500;
    // How long to wait after receiving an onScrollStateChanged notification
    // before acting on it
    // 실행하기 전에 onScrollStateChanged 알림을 받은 후 대기할 시간
    protected static final int SCROLL_CHANGE_DELAY = 40;
    // The size of the month name displayed above the week list
    // 주 목록 위에 표시되는 월 이름의 크기
    protected static final int MINI_MONTH_NAME_TEXT_SIZE = 18;
    private static final String TAG = "MonthFragment";
    private static final String KEY_CURRENT_TIME = "current_time";
    public static int LIST_TOP_OFFSET = -1;  // so that the top line will be under the separator
    // 상단 라인이 분리기 아래에 있도록
    private static float mScale = 0;
    protected int WEEK_MIN_VISIBLE_HEIGHT = 12;
    protected int BOTTOM_BUFFER = 20;
    protected int mSaturdayColor = 0;
    protected int mSundayColor = 0;
    protected int mDayNameColor = 0;
    // You can override these numbers to get a different appearance
    // 다른 모습을 원하려면 이 숫자들을 오버라이드하기
    protected int mNumWeeks = 5;
    protected boolean mShowWeekNumber = false;
    protected int mDaysPerWeek = 7;
    // These affect the scroll speed and feel
    // 스크롤 속도와 느낌에 영향을 미침
    protected float mFriction = 1.0f;
    protected Context mContext;
    protected Handler mHandler;
    protected float mMinimumFlingVelocity;
    // highlighted time
    // 강조된 시간
    protected Time mSelectedDay = new Time();
    protected SimpleWeeksAdapter mAdapter;
    protected ListView mListView;
    protected ViewGroup mDayNamesHeader;
    protected String[] mDayLabels;
    // disposable variable used for time calculations
    // 시간 계산에 사용되는 일회용 변수
    protected Time mTempTime = new Time();
    // When the week starts; numbered like Time.<WEEKDAY> (e.g. SUNDAY=0).
    // 주의 시작; Time.<WEEKDAY>처럼 번호가 매겨짐
    protected int mFirstDayOfWeek;
    // The first day of the focus month
    // 포커스된 달의 첫 번째 날
    protected Time mFirstDayOfMonth = new Time();
    // The first day that is visible in the view
    // 뷰에 보이는 첫 번째 날
    protected Time mFirstVisibleDay = new Time();
    // The name of the month to display
    // 표시할 달의 이름
    protected TextView mMonthName;
    // The last name announced by accessibility
    // 접근성에 의해 얻은 last name
    protected CharSequence mPrevMonthName;
    // which month should be displayed/highlighted [0-11]
    // 어느 달이 표시/강조되어야 하는지
    protected int mCurrentMonthDisplayed;
    // used for tracking during a scroll
    // 스크롤 중 트래킹에 사용
    protected long mPreviousScrollPosition;
    // used for tracking which direction the view is scrolling
    // 뷰가 스크롤되는 방향을 트래킹하는 데 사용
    protected boolean mIsScrollingUp = false;
    // used for tracking what state listview is in
    // 리스트뷰가 어떤 상태인지...? 트래킹하는 데 사용
    protected int mPreviousScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    protected int mCurrentScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    // This causes an update of the view at midnight
    // 자정에 뷰가 업데이트됨
    protected Runnable mTodayUpdater = new Runnable() {
        @Override
        public void run() {
            Time midnight = new Time(mFirstVisibleDay.timezone);
            midnight.setToNow();
            long currentMillis = midnight.toMillis(true);

            midnight.hour = 0;
            midnight.minute = 0;
            midnight.second = 0;
            midnight.monthDay++;
            long millisToMidnight = midnight.normalize(true) - currentMillis;
            mHandler.postDelayed(this, millisToMidnight);

            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    };
    protected ScrollStateRunnable mScrollStateChangedRunnable = new ScrollStateRunnable();
    // This allows us to update our position when a day is tapped
    // 날짜가 탭 됐을 때, 위치를 업데이트 할 수 있음?
    protected DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            Time day = mAdapter.getSelectedDay();
            if (day.year != mSelectedDay.year || day.yearDay != mSelectedDay.yearDay) {
                goTo(day.toMillis(true), true, true, false);
            }
        }
    };

    public SimpleDayPickerFragment(long initialTime) {
        goTo(initialTime, false, true, true);
        mHandler = new Handler();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        String tz = Time.getCurrentTimezone();
        ViewConfiguration viewConfig = ViewConfiguration.get(activity);
        mMinimumFlingVelocity = viewConfig.getScaledMinimumFlingVelocity();

        // Ensure we're in the correct time zone
        // 올바른 시간대에 있는지 확인
        mSelectedDay.switchTimezone(tz);
        mSelectedDay.normalize(true);
        mFirstDayOfMonth.timezone = tz;
        mFirstDayOfMonth.normalize(true);
        mFirstVisibleDay.timezone = tz;
        mFirstVisibleDay.normalize(true);
        mTempTime.timezone = tz;

        Context c = getActivity();
        DynamicTheme theme = new DynamicTheme();
        mSaturdayColor = theme.getColor(c, "month_saturday");
        mSundayColor = theme.getColor(c, "month_sunday");
        mDayNameColor = theme.getColor(c, "month_day_names_color");

        // Adjust sizes for screen density
        // 화면 밀도에 대한 크기 조정
        if (mScale == 0) {
            mScale = activity.getResources().getDisplayMetrics().density;
            if (mScale != 1) {
                WEEK_MIN_VISIBLE_HEIGHT *= mScale;
                BOTTOM_BUFFER *= mScale;
                LIST_TOP_OFFSET *= mScale;
            }
        }
        setUpAdapter();
        setListAdapter(mAdapter);
    }

    /**
     * Creates a new adapter if necessary and sets up its parameters. Override
     * this method to provide a custom adapter.
     * 필요한 경우 새 어댑터를 생성하고, 해당 매개변수들을 설정함
     * 커스텀 어댑터를 제공하려면 이 메소드를 오버라이딩함
     */
    protected void setUpAdapter() {
        HashMap<String, Integer> weekParams = new HashMap<String, Integer>();
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_NUM_WEEKS, mNumWeeks);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_SHOW_WEEK, mShowWeekNumber ? 1 : 0);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_WEEK_START, mFirstDayOfWeek);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_JULIAN_DAY,
                Time.getJulianDay(mSelectedDay.toMillis(false), mSelectedDay.gmtoff));
        if (mAdapter == null) {
            mAdapter = new SimpleWeeksAdapter(getActivity(), weekParams);
            mAdapter.registerDataSetObserver(mObserver);
        } else {
            mAdapter.updateParams(weekParams);
        }
        // refresh the view with the new parameters
        // 새 매개변수를 사용하여 뷰 새로고침
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_CURRENT_TIME)) {
            goTo(savedInstanceState.getLong(KEY_CURRENT_TIME), false, true, true);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setUpListView();
        setUpHeader();

        mMonthName = (TextView) getView().findViewById(R.id.month_name);
        SimpleWeekView child = (SimpleWeekView) mListView.getChildAt(0);
        if (child == null) {
            return;
        }
        int julianDay = child.getFirstJulianDay();
        mFirstVisibleDay.setJulianDay(julianDay);
        // set the title to the month of the second week
        // 둘째 주의 타이틀 설정
        mTempTime.setJulianDay(julianDay + DAYS_PER_WEEK);
        setMonthDisplayed(mTempTime, true);
    }

    /**
     * Sets up the strings to be used by the header. Override this method to use
     * different strings or modify the view params.
     * 헤더가 사용할 문자열을 설정함
     * 다른 문자열을 사용하거나 뷰 매개변수를 수정하려면 이 메소드를 오버라이딩하기
     */
    protected void setUpHeader() {
        mDayLabels = new String[7];
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            mDayLabels[i - Calendar.SUNDAY] = DateUtils.getDayOfWeekString(i,
                    DateUtils.LENGTH_SHORTEST).toUpperCase();
        }
    }

    /**
     * Sets all the required fields for the list view. Override this method to
     * set a different list view behavior.
     * 리스트뷰에 필요한 모든 필드를 설정함
     * 다른 리스트뷰 동작을 설정하려면 이 메소드를 오버라이딩하기
     */
    protected void setUpListView() {
        // Configure the listview
        mListView = getListView();
        // Transparent background on scroll
        mListView.setCacheColorHint(0);
        // No dividers
        mListView.setDivider(null);
        // Items are clickable
        mListView.setItemsCanFocus(true);
        // The thumb gets in the way, so disable it
        mListView.setFastScrollEnabled(false);
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setOnScrollListener(this);
        mListView.setFadingEdgeLength(0);
        // Make the scrolling behavior nicer
        mListView.setFriction(ViewConfiguration.getScrollFriction() * mFriction);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpAdapter();
        doResumeUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mTodayUpdater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(KEY_CURRENT_TIME, mSelectedDay.toMillis(true));
    }

    /**
     * Updates the user preference fields. Override this to use a different
     * preference space.
     * 사용자 Preference 필드를 업데이트함
     * 다른 Preference 공간을 사용하려면 이 메소드를 오버라이딩하기
     */
    protected void doResumeUpdates() {
        // Get default week start based on locale, subtracting one for use with android Time.
        // 안드로이드 Time에 사용하기 위해 하나를 뺀...? locale에 기초한 기본 주를 가져옴
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        mFirstDayOfWeek = cal.getFirstDayOfWeek() - 1;

        mShowWeekNumber = false;

        updateHeader();
        goTo(mSelectedDay.toMillis(true), false, false, false);
        mAdapter.setSelectedDay(mSelectedDay);
        mTodayUpdater.run();
    }

    /**
     * Fixes the day names header to provide correct spacing and updates the
     * label text. Override this to set up a custom header.
     * 일 이름 헤더를 수정하여 올바른 간격을 제공하고 레이블 텍스트를 업데이트함
     * 커스텀 헤더를 사용하려면 이 메소드를 오버라이딩
     */
    protected void updateHeader() {
        TextView label = (TextView) mDayNamesHeader.findViewById(R.id.wk_label);
        label.setVisibility(View.GONE);

        int offset = mFirstDayOfWeek - 1;
        for (int i = 1; i < 8; i++) {
            label = (TextView) mDayNamesHeader.getChildAt(i);
            if (i < mDaysPerWeek + 1) {
                int position = (offset + i) % 7;
                label.setText(mDayLabels[position]);
                label.setVisibility(View.VISIBLE);
                if (position == Time.SATURDAY) {
                    label.setTextColor(mSaturdayColor);
                } else if (position == Time.SUNDAY) {
                    label.setTextColor(mSundayColor);
                } else {
                    label.setTextColor(mDayNameColor);
                }
            } else {
                label.setVisibility(View.GONE);
            }
        }
        mDayNamesHeader.invalidate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.month_by_week,
                container, false);
        mDayNamesHeader = (ViewGroup) v.findViewById(R.id.day_names);
        return v;
    }

    /**
     * Returns the UTC millis since epoch representation of the currently
     * selected time.
     * 현재 선택한 시간을 epoch로 나타낸 후, UTC millis를 반환함
     *
     * @return
     */
    public long getSelectedTime() {
        return mSelectedDay.toMillis(true);
    }

    /**
     * This moves to the specified time in the view. If the time is not already
     * in range it will move the list so that the first of the month containing
     * the time is at the top of the view. If the new time is already in view
     * the list will not be scrolled unless forceScroll is true. This time may
     * optionally be highlighted as selected as well.
     * 뷰에서 지정된 시간으로 이동함
     * 만약 시간이 아직 범위를 벗어나지 않은 경우, 해당 시간을 포함하는 월의
     * 첫 번째 시간이 뷰의 맨 위에 오도록 리스트를 이동함
     * 새 시간이 이미 뷰에 있는 경우, forceScroll이 true가 아니면 목록이 스크롤되지 않음
     *
     * @param time The time to move to
     *              이동할 시간
     * @param animate Whether to scroll to the given time or just redraw at the
     *            new location
     *            지정된 시간까지 스크롤할지, 새 위치를 다시 그릴지
     * @param setSelected Whether to set the given time as selected
     *                      지정된 시간을 선택한 대로 설정할지
     * @param forceScroll Whether to recenter even if the time is already
     *            visible
     *                      시간이 이미 보이는 경우에 recenter... 할 건지
     * @return Whether or not the view animated to the new location
     *          뷰가 새 위치로 애니메이션되는지 여부
     */
    public boolean goTo(long time, boolean animate, boolean setSelected, boolean forceScroll) {
        if (time == -1) {
            Log.e(TAG, "time is invalid");
            return false;
        }

        // Set the selected day
        // 선택된 날 설정
        if (setSelected) {
            mSelectedDay.set(time);
            mSelectedDay.normalize(true);
        }

        // If this view isn't returned yet we won't be able to load the lists
        // current position, so return after setting the selected day.
        // 이 뷰가 아직 반환되지 않으면 현재 위지를 로드할 수 없으므로
        // 선택한 날짜를 설정한 후 반환함
        if (!isResumed()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "We're not visible yet");
            }
            return false;
        }

        mTempTime.set(time);
        long millis = mTempTime.normalize(true);
        // Get the week we're going to
        // 앞으로 다룰 주를 가져옴
        // TODO push Util function into Calendar public api.
        int position = Utils.getWeeksSinceEpochFromJulianDay(
                Time.getJulianDay(millis, mTempTime.gmtoff), mFirstDayOfWeek);

        View child;
        int i = 0;
        int top = 0;
        // Find a child that's completely in the view
        // 완전히 뷰에 있는 자식 찾기
        do {
            child = mListView.getChildAt(i++);
            if (child == null) {
                break;
            }
            top = child.getTop();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "child at " + (i-1) + " has top " + top);
            }
        } while (top < 0);

        // Compute the first and last position visible
        // 표시되는 첫 번째 및 마지막 위치 계산
        int firstPosition;
        if (child != null) {
            firstPosition = mListView.getPositionForView(child);
        } else {
            firstPosition = 0;
        }
        int lastPosition = firstPosition + mNumWeeks - 1;
        if (top > BOTTOM_BUFFER) {
            lastPosition--;
        }

        if (setSelected) {
            mAdapter.setSelectedDay(mSelectedDay);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "GoTo position " + position);
        }
        // Check if the selected day is now outside of our visible range
        // and if so scroll to the month that contains it
        // 선택된 날짜가 현재 가시 범위 밖에 있는지 확인하고
        // 표시된 월로 스크롤함
        if (position < firstPosition || position > lastPosition || forceScroll) {
            mFirstDayOfMonth.set(mTempTime);
            mFirstDayOfMonth.monthDay = 1;
            millis = mFirstDayOfMonth.normalize(true);
            setMonthDisplayed(mFirstDayOfMonth, true);
            position = Utils.getWeeksSinceEpochFromJulianDay(
                    Time.getJulianDay(millis, mFirstDayOfMonth.gmtoff), mFirstDayOfWeek);

            mPreviousScrollState = OnScrollListener.SCROLL_STATE_FLING;
            if (animate) {
                mListView.smoothScrollToPositionFromTop(
                        position, LIST_TOP_OFFSET, GOTO_SCROLL_DURATION);
                return true;
            } else {
                mListView.setSelectionFromTop(position, LIST_TOP_OFFSET);
                // Perform any after scroll operations that are needed
                // 필요한 스크롤 작업 후 수행
                onScrollStateChanged(mListView, OnScrollListener.SCROLL_STATE_IDLE);
            }
        } else if (setSelected) {
            // Otherwise just set the selection
            // 선택만 설정
            setMonthDisplayed(mSelectedDay, true);
        }
        return false;
    }

    /**
     * Updates the title and selected month if the view has moved to a new
     * month.
     */
    @Override
    public void onScroll(
            AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        SimpleWeekView child = (SimpleWeekView)view.getChildAt(0);
        if (child == null) {
            return;
        }

        // Figure out where we are
        long currScroll = view.getFirstVisiblePosition() * child.getHeight() - child.getBottom();
        mFirstVisibleDay.setJulianDay(child.getFirstJulianDay());

        // If we have moved since our last call update the direction
        if (currScroll < mPreviousScrollPosition) {
            mIsScrollingUp = true;
        } else if (currScroll > mPreviousScrollPosition) {
            mIsScrollingUp = false;
        } else {
            return;
        }

        mPreviousScrollPosition = currScroll;
        mPreviousScrollState = mCurrentScrollState;

        updateMonthHighlight(mListView);
    }

    /**
     * Figures out if the month being shown has changed and updates the
     * highlight if needed
     * 표시되는 월이 변경되었는지 확인하고 필요한 경우 하이라이트 업데이트
     *
     * @param view The ListView containing the weeks
     *               주를 포함하는 ListView
     */
    private void updateMonthHighlight(AbsListView view) {
        SimpleWeekView child = (SimpleWeekView) view.getChildAt(0);
        if (child == null) {
            return;
        }

        // Figure out where we are 어디에 있는지 알아내기
        int offset = child.getBottom() < WEEK_MIN_VISIBLE_HEIGHT ? 1 : 0;
        // Use some hysteresis for checking which month to highlight. This
        // causes the month to transition when two full weeks of a month are
        // visible.
        // 강조할 달을 확인하기 위해...
        // 달의 두 주 전체를 볼 수 있을 때...? 변화시킨다 뭔 소리
        child = (SimpleWeekView) view.getChildAt(SCROLL_HYST_WEEKS + offset);

        if (child == null) {
            return;
        }

        // Find out which month we're moving into
        // 어디로 이동하고 있는지...? 알아보기...
        int month;
        if (mIsScrollingUp) {
            month = child.getFirstMonth();
        } else {
            month = child.getLastMonth();
        }

        // And how it relates to our current highlighted month
        // 그리고 그것이 현재 강조된 달과 어떤 관련이 있는지
        int monthDiff;
        if (mCurrentMonthDisplayed == 11 && month == 0) {
            monthDiff = 1;
        } else if (mCurrentMonthDisplayed == 0 && month == 11) {
            monthDiff = -1;
        } else {
            monthDiff = month - mCurrentMonthDisplayed;
        }

        // Only switch months if we're scrolling away from the currently
        // selected month
        // 현재 선택된 월에서 스크롤할 경우에만 월 전환
        if (monthDiff != 0) {
            int julianDay = child.getFirstJulianDay();
            if (mIsScrollingUp) {
                // Takes the start of the week
                // 한 주의 시작
            } else {
                // Takes the start of the following week
                // 다음 주의 시작
                julianDay += DAYS_PER_WEEK;
            }
            mTempTime.setJulianDay(julianDay);
            setMonthDisplayed(mTempTime, false);
        }
    }

    /**
     * Sets the month displayed at the top of this view based on time. Override
     * to add custom events when the title is changed.
     * 시간을 기준으로 이 view 상단에 표시되는 월을 설정함
     * 타이틀이 변경될 때 사용자 정의 이벤트를 추가하려면 오버라이딩함
     *
     * @param time A day in the new focus month.
     *              새로운 초점..달의 하루
     * @param updateHighlight TODO(epastern):
     */
    protected void setMonthDisplayed(Time time, boolean updateHighlight) {
        CharSequence oldMonth = mMonthName.getText();
        mMonthName.setText(Utils.formatMonthYear(mContext, time));
        mMonthName.invalidate();
        if (!TextUtils.equals(oldMonth, mMonthName.getText())) {
            mMonthName.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }
        mCurrentMonthDisplayed = time.month;
        if (updateHighlight) {
            mAdapter.updateFocusMonth(mCurrentMonthDisplayed);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // use a post to prevent re-entering onScrollStateChanged before it
        // exits
        // onScrollStateChanged가 종료되기 전에 다시 입력?re-enter되는 걸 방지하기 위하여 post를 사용함
        mScrollStateChangedRunnable.doScrollStateChange(view, scrollState);
    }

    protected class ScrollStateRunnable implements Runnable {
        private int mNewState;

        /**
         * Sets up the runnable with a short delay in case the scroll state
         * immediately changes again.
         * 스크롤 상태가 즉시 다시 변경될 경우 짧은 딜레이로 runnable을 설정함
         *
         * @param view The list view that changed state
         *              상태가 변경된 listview
         * @param scrollState The new state it changed to
         *                     변경된 새로운 상태
         */
        public void doScrollStateChange(AbsListView view, int scrollState) {
            mHandler.removeCallbacks(this);
            mNewState = scrollState;
            mHandler.postDelayed(this, SCROLL_CHANGE_DELAY);
        }

        public void run() {
            mCurrentScrollState = mNewState;
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG,
                        "new scroll state: " + mNewState + " old state: " + mPreviousScrollState);
            }
            // Fix the position after a scroll or a fling ends
            // 스크롤 또는 플링 종료 후 위치 고정
            if (mNewState == OnScrollListener.SCROLL_STATE_IDLE
                    && mPreviousScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mPreviousScrollState = mNewState;
                // Uncomment the below to add snap to week back
//                int i = 0;
//                View child = mView.getChildAt(i);
//                while (child != null && child.getBottom() <= 0) {
//                    child = mView.getChildAt(++i);
//                }
//                if (child == null) {
//                    // The view is no longer visible, just return
//                    return;
//                }
//                int dist = child.getTop();
//                if (dist < LIST_TOP_OFFSET) {
//                    if (Log.isLoggable(TAG, Log.DEBUG)) {
//                        Log.d(TAG, "scrolling by " + dist + " up? " + mIsScrollingUp);
//                    }
//                    int firstPosition = mView.getFirstVisiblePosition();
//                    int lastPosition = mView.getLastVisiblePosition();
//                    boolean scroll = firstPosition != 0 && lastPosition != mView.getCount() - 1;
//                    if (mIsScrollingUp && scroll) {
//                        mView.smoothScrollBy(dist, 500);
//                    } else if (!mIsScrollingUp && scroll) {
//                        mView.smoothScrollBy(child.getHeight() + dist, 500);
//                    }
//                }
                mAdapter.updateFocusMonth(mCurrentMonthDisplayed);
            } else {
                mPreviousScrollState = mNewState;
            }
        }
    }
}
