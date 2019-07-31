package com.android.nanal.datetimepicker.date;

/*
 * Copyright (C) 2013 The Android Open Source Project
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
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.android.nanal.datetimepicker.Utils;
import com.android.nanal.datetimepicker.date.DatePickerDialog.OnDateChangedListener;
import com.android.nanal.datetimepicker.date.MonthAdapter.CalendarDay;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * This displays a list of months in a calendar format with selectable days.
 * 월 달력을 선택 가능한 날짜와 함께 표시한다.
 */
abstract class DayPickerView extends ListView implements OnScrollListener,
        OnDateChangedListener {

    private static final String TAG = "MonthFragment";

    // Affects when the month selection will change while scrolling up
    // 위로 스크롤하는 동안 월 선택이 변경되는 데에 영향을 미침
    protected static final int SCROLL_HYST_WEEKS = 2;
    // How long the GoTo fling animation should last
    // GoTo fling 애니메이션의 지속 시간
    protected static final int GOTO_SCROLL_DURATION = 250;
    // How long to wait after receiving an onScrollStateChanged notification
    // before acting on it
    // 실행하기 전, onScrollStateChanged 알림을 받은 후 대기할 시간
    protected static final int SCROLL_CHANGE_DELAY = 40;
    // The number of days to display in each week
    // 주마다 표시할 일수
    public static final int DAYS_PER_WEEK = 7;
    public static int LIST_TOP_OFFSET = -1; // so that the top line will be
    // under the separator
    // You can override these numbers to get a different appearance
    // 상단 라인이 separator 아래에 있도록
    // 다른 형태를 얻으려면 이 숫자들을 override하기

    protected int mNumWeeks = 6;
    protected boolean mShowWeekNumber = false;
    protected int mDaysPerWeek = 7;
    private static SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy", Locale.getDefault());

    // These affect the scroll speed and feel
    // 스크롤 속도 및 느낌에 영향을 미침
    protected float mFriction = 1.0f;

    protected Context mContext;
    protected Handler mHandler;

    // highlighted time
    // 하이라이트된 시간
    protected CalendarDay mSelectedDay = new CalendarDay();
    protected MonthAdapter mAdapter;

    protected CalendarDay mTempDay = new CalendarDay();

    // When the week starts; numbered like Time.<WEEKDAY> (e.g. SUNDAY=0).
    // 한 주가 시작되면 시간처럼 번호가 매겨짐
    protected int mFirstDayOfWeek;
    // The last name announced by accessibility
    // 접근성(Accessibility)에 의해 알려진 성(姓)
    protected CharSequence mPrevMonthName;
    // which month should be displayed/highlighted [0-11]
    // 어느 월을 표시/하이라이트 해야 하는지 [0-11]
    protected int mCurrentMonthDisplayed;
    // used for tracking during a scroll
    // 스크롤 트래킹에 사용
    protected long mPreviousScrollPosition;
    // used for tracking what state listview is in
    // 현재 상태 listview에 뭐가 있는지 트래킹에 사용
    protected int mPreviousScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    // used for tracking what state listview is in
    protected int mCurrentScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    private DatePickerController mController;
    private boolean mPerformingScroll;

    public DayPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DayPickerView(Context context, DatePickerController controller) {
        super(context);
        init(context);
        setController(controller);
    }

    public void setController(DatePickerController controller) {
        mController = controller;
        mController.registerOnDateChangedListener(this);
        refreshAdapter();
        onDateChanged();
    }

    public void init(Context context) {
        mHandler = new Handler();
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setDrawSelectorOnTop(false);

        mContext = context;
        setUpListView();
    }

    public void onChange() {
        refreshAdapter();
    }

    /**
     * Creates a new adapter if necessary and sets up its parameters. Override
     * this method to provide a custom adapter.
     * 필요하다면 새로운 어댑터를 만들고, 매개 변수를 설정하기
     * 커스텀 어댑터를 제공하려면 이 메소드를 오버라이드하기
     */
    protected void refreshAdapter() {
        if (mAdapter == null) {
            mAdapter = createMonthAdapter(getContext(), mController);
        } else {
            mAdapter.setSelectedDay(mSelectedDay);
        }
        // refresh the view with the new parameters
        // 새 매개변수를 사용하여 view를 새로 고침
        setAdapter(mAdapter);
    }

    public abstract MonthAdapter createMonthAdapter(Context context,
                                                    DatePickerController controller);

    /*
     * Sets all the required fields for the list view. Override this method to
     * set a different list view behavior.
     * list view에 필요한 모든 필드를 설정하기
     * 다른 list view 행동을 설정하려면 이 메소드를 오버라이드하기
     */
    protected void setUpListView() {
        // Transparent background on scroll
        // 스크롤의 투명한 배경
        setCacheColorHint(0);
        // No dividers
        // listview의 구분선 없애기
        setDivider(null);
        // Items are clickable
        // 아이템들 클릭 가능하게 설정
        setItemsCanFocus(true);
        // The thumb gets in the way, so disable it
        // thumb를 없앰
        // thumb: 목록에서 스크롤 시작하면 오른쪽에 나타나는 스크롤 버튼
        setFastScrollEnabled(false);
        setVerticalScrollBarEnabled(false);
        setOnScrollListener(this);
        setFadingEdgeLength(0);
        // Make the scrolling behavior nicer
        // 스크롤 동작을 더 낫게 만듦
        setFriction(ViewConfiguration.getScrollFriction() * mFriction);
    }

    /**
     * This moves to the specified time in the view. If the time is not already
     * in range it will move the list so that the first of the month containing
     * the time is at the top of the view. If the new time is already in view
     * the list will not be scrolled unless forceScroll is true. This time may
     * optionally be highlighted as selected as well.
     *
     * view에서 지정된 날짜로 이동한다. 날짜가 월의 범위에 있지 않은 경우,
     * 해당 날짜를 포함하는 월의 첫 번째가 뷰의 맨 위에 오도록 listview를 이동한다.
     * 새 날짜가 이미 뷰에 있는 경우, forceScroll이 true가 아니라면 목록이 스크롤되지 않는다.
     * 선택적으로 이 날짜를 하이라이트 할 수도 있다.
     *
     * @param day The time to move to
     *             이동할 시간
     * @param animate Whether to scroll to the given time or just redraw at the
     *            new location
     *                 지정된 날짜까지 스크롤할지, 새 위치를 다시 그릴지
     * @param setSelected Whether to set the given time as selected
     *                    주어진 날짜를 선택된 걸로 설정할지(?)
     * @param forceScroll Whether to recenter even if the time is already
     *            visible
     *                     시간이 이미 보이는 경우에도 최신 상태인지
     * @return Whether or not the view animated to the new location
     *          view가 새 위치로 애니메이션되었는지
     */
    public boolean goTo(CalendarDay day, boolean animate, boolean setSelected, boolean forceScroll) {

        // Set the selected day
        // 선택된 날을 설정
        if (setSelected) {
            mSelectedDay.set(day);
        }

        mTempDay.set(day);
        final int position = (day.year - mController.getMinYear())
                * MonthAdapter.MONTHS_IN_YEAR + day.month;

        View child;
        int i = 0;
        int top = 0;
        // Find a child that's completely in the view
        // 완전히 뷰에 있는(?) child 찾기
        // listview.getChildAt(i): 현재 보이고 있는 child 중에서 i번째를 달라, 0이면 현재 보이는 것 중 맨 위에 있는 child
        do {
            child = getChildAt(i++);
            if (child == null) {
                break;
            }
            top = child.getTop();       // 상단(Y좌표)
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "child at " + (i - 1) + " has top " + top);
            }
        } while (top < 0);

        // Compute the first and last position visible
        // 표시되는 첫 번째, 마지막 위치 계산
        int selectedPosition;
        if (child != null) {
            selectedPosition = getPositionForView(child);
        } else {
            selectedPosition = 0;
        }

        if (setSelected) {
            mAdapter.setSelectedDay(mSelectedDay);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "GoTo position " + position);
        }
        // Check if the selected day is now outside of our visible range
        // and if so scroll to the month that contains it
        // 선택한 날짜가 현재 가시 범위 밖에 있는지 확인하고, 표시된 월로 스크롤하기
        if (position != selectedPosition || forceScroll) {
            setMonthDisplayed(mTempDay);
            mPreviousScrollState = OnScrollListener.SCROLL_STATE_FLING;
            if (animate) {
                smoothScrollToPositionFromTop(
                        position, LIST_TOP_OFFSET, GOTO_SCROLL_DURATION);
                return true;
            } else {
                postSetSelection(position);
            }
        } else if (setSelected) {
            setMonthDisplayed(mSelectedDay);
        }
        return false;
    }

    public void postSetSelection(final int position) {
        clearFocus();
        // post(new Runnable()... )v스레드 핸들러
        post(new Runnable() {

            @Override
            public void run() {
                setSelection(position);
            }
        });
        onScrollStateChanged(this, OnScrollListener.SCROLL_STATE_IDLE);
    }

    /**
     * Updates the title and selected month if the view has moved to a new
     * month.
     * 뷰가 새로운 달로 이동한 경우, 타이틀과 선택된 달 업데이트
     */
    @Override
    public void onScroll(
            AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        MonthView child = (MonthView) view.getChildAt(0);
        if (child == null) {
            return;
        }

        // Figure out where we are
        // 어디에 있는지
        long currScroll = view.getFirstVisiblePosition() * child.getHeight() - child.getBottom();
        mPreviousScrollPosition = currScroll;
        mPreviousScrollState = mCurrentScrollState;
    }

    /**
     * Sets the month displayed at the top of this view based on time. Override
     * to add custom events when the title is changed.
     * 시간을 기준으로 표시되는 달을 설정
     * 타이틀이 변경될 때, 커스텀 이벤트를 추가하려면 오버라이드하기
     */
    protected void setMonthDisplayed(CalendarDay date) {
        mCurrentMonthDisplayed = date.month;
        invalidateViews();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // use a post to prevent re-entering onScrollStateChanged before it
        // exits
        // 종료되기 전에 onScrollStateChanged에 재접속하는 걸 방지하기 위해 post를 사용
        mScrollStateChangedRunnable.doScrollStateChange(view, scrollState);
    }

    protected ScrollStateRunnable mScrollStateChangedRunnable = new ScrollStateRunnable();

    protected class ScrollStateRunnable implements Runnable {
        private int mNewState;

        /**
         * Sets up the runnable with a short delay in case the scroll state
         * immediately changes again.
         * 스크롤 상태가 즉시 재변경될 경우 짧은 딜레이로 runnable을 설정하기
         *
         *
         * @param view The list view that changed state
         *              상태를 변경한 list view
         * @param scrollState The new state it changed to
         *                     변경된 새로운 상태
         */
        public void doScrollStateChange(AbsListView view, int scrollState) {
            mHandler.removeCallbacks(this);
            mNewState = scrollState;
            mHandler.postDelayed(this, SCROLL_CHANGE_DELAY);
        }

        @Override
        public void run() {
            mCurrentScrollState = mNewState;
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG,
                        "new scroll state: " + mNewState + " old state: " + mPreviousScrollState);
            }
            // Fix the position after a scroll or a fling ends
            // 스크롤 또는 플링 종료 후 위치 고정
            if (mNewState == OnScrollListener.SCROLL_STATE_IDLE
                    && mPreviousScrollState != OnScrollListener.SCROLL_STATE_IDLE
                    && mPreviousScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                mPreviousScrollState = mNewState;
                int i = 0;
                View child = getChildAt(i);
                while (child != null && child.getBottom() <= 0) {
                    child = getChildAt(++i);
                }
                if (child == null) {
                    // The view is no longer visible, just return
                    // 더 이상 view가 보이지 않으면 return
                    return;
                }
                int firstPosition = getFirstVisiblePosition();
                int lastPosition = getLastVisiblePosition();
                boolean scroll = firstPosition != 0 && lastPosition != getCount() - 1;
                final int top = child.getTop();
                final int bottom = child.getBottom();
                final int midpoint = getHeight() / 2;
                if (scroll && top < LIST_TOP_OFFSET) {
                    if (bottom > midpoint) {
                        smoothScrollBy(top, GOTO_SCROLL_DURATION);
                    } else {
                        smoothScrollBy(bottom, GOTO_SCROLL_DURATION);
                    }
                }
            } else {
                mPreviousScrollState = mNewState;
            }
        }
    }

    /**
     * Gets the position of the view that is most prominently displayed within the list view.
     * list view 내에 가장 눈에 띄게 표시되는(?) view의 위치(position)을 가져오기
     */
    public int getMostVisiblePosition() {
        final int firstPosition = getFirstVisiblePosition();
        final int height = getHeight();

        int maxDisplayedHeight = 0;
        int mostVisibleIndex = 0;
        int i=0;
        int bottom = 0;
        while (bottom < height) {
            View child = getChildAt(i);
            if (child == null) {
                break;
            }
            bottom = child.getBottom();
            int displayedHeight = Math.min(bottom, height) - Math.max(0, child.getTop());
            if (displayedHeight > maxDisplayedHeight) {
                mostVisibleIndex = i;
                maxDisplayedHeight = displayedHeight;
            }
            i++;
        }
        return firstPosition + mostVisibleIndex;
    }

    @Override
    public void onDateChanged() {
        goTo(mController.getSelectedDay(), false, true, true);
    }

    /**
     * Attempts to return the date that has accessibility focus.
     * 접근성 포커스가 있는 날짜 반환 시도
     *
     * @return The date that has accessibility focus, or {@code null} if no date
     *         has focus.
     *         접근성 포커스가 있는 날짜 또는 포커스가 없는 경우 {@code null}
     */
    private CalendarDay findAccessibilityFocus() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child instanceof MonthView) {
                final CalendarDay focus = ((MonthView) child).getAccessibilityFocus();
                if (focus != null) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        // Clear focus to avoid ListView bug in Jelly Bean MR1.
                        // 젤리빈 MR1의 ListView 버그를 피하려면 포커스 삭제
                        ((MonthView) child).clearAccessibilityFocus();
                    }
                    return focus;
                }
            }
        }

        return null;
    }

    /**
     * Attempts to restore accessibility focus to a given date. No-op if
     * {@code day} is {@code null}.
     * 특정 날짜에 대한 접근성 복원 시도
     * {@code day}가 {@code null}인 경우 아무 일도 하지 않기
     *
     * @param day The date that should receive accessibility focus
     *             접근성 포커스를 받아야 하는 날짜
     * @return {@code true} if focus was restored
     *                      포커스가 복원된 경우
     */
    private boolean restoreAccessibilityFocus(CalendarDay day) {
        if (day == null) {
            return false;
        }

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child instanceof MonthView) {
                if (((MonthView) child).restoreAccessibilityFocus(day)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void layoutChildren() {
        final CalendarDay focusedDay = findAccessibilityFocus();
        super.layoutChildren();
        if (mPerformingScroll) {
            mPerformingScroll = false;
        } else {
            restoreAccessibilityFocus(focusedDay);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setItemCount(-1);
    }

    private static String getMonthAndYearString(CalendarDay day) {
        Calendar cal = Calendar.getInstance();
        cal.set(day.year, day.month, day.day);

        StringBuffer sbuf = new StringBuffer();
        sbuf.append(cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()));
        sbuf.append(" ");
        sbuf.append(YEAR_FORMAT.format(cal.getTime()));
        return sbuf.toString();
    }

    /**
     * Necessary for accessibility, to ensure we support "scrolling" forward and backward
     * in the month list.
     * 월 목록에서 앞뒤로 "스크롤링"을 지원하려면 접근성이 필요함
     */
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    /**
     * When scroll forward/backward events are received, announce the newly scrolled-to month.
     * 앞/뒤 스크롤 이벤트가 들어오면 새로 스크롤할 월 알리기
     */
    @SuppressLint("NewApi")
    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action != AccessibilityNodeInfo.ACTION_SCROLL_FORWARD &&
                action != AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            return super.performAccessibilityAction(action, arguments);
        }

        // Figure out what month is showing.
        // 몇 월을 보고 있는지
        int firstVisiblePosition = getFirstVisiblePosition();
        int month = firstVisiblePosition % 12;
        int year = firstVisiblePosition / 12 + mController.getMinYear();
        CalendarDay day = new CalendarDay(year, month, 1);

        // Scroll either forward or backward one month.
        // 한 달 앞으로 또는 뒤로 스크롤하기
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
            day.month++;
            if (day.month == 12) {
                day.month = 0;
                day.year++;
            }
        } else if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            View firstVisibleView = getChildAt(0);
            // If the view is fully visible, jump one month back. Otherwise, we'll just jump
            // to the first day of first visible month.
            // 뷰가 완전히 보이면, 다음 달로 점프함
            // 그렇지 않으면, 보이는 첫 달의 첫 번째 날로 이동함
            if (firstVisibleView != null && firstVisibleView.getTop() >= -1) {
                // There's an off-by-one somewhere, so the top of the first visible item will
                // actually be -1 when it's at the exact top.
                // 어디엔가 하나씩 따로 있기 때문에,
                // 첫 번째 눈에 보이는 아이템의 상단은 위에 있을 때 -1이 됨
                day.month--;
                if (day.month == -1) {
                    day.month = 11;
                    day.year--;
                }
            }
        }

        // Go to that month.
        // 그 달로 이동
        Utils.tryAccessibilityAnnounce(this, getMonthAndYearString(day));
        goTo(day, true, false, true);
        mPerformingScroll = true;
        return true;
    }
}
