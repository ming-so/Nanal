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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.nanal.DynamicTheme;
import com.android.nanal.R;
import com.android.nanal.datetimepicker.Utils;
import com.android.nanal.datetimepicker.date.MonthAdapter.CalendarDay;
import com.android.nanal.event.GeneralPreferences;

import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * A calendar-like view displaying a specified month and the appropriate selectable day numbers
 * within the specified month.
 * 지정된 달과 지정된 달 내의 선택 가능한 날짜 번호를 표시하는 캘린더 같은 view
 *
 */
abstract class MonthView extends View {
    private static final String TAG = "MonthView";

    /**
     * These params can be passed into the view to control how it appears.
     * 이 패러미터들은 어떻게 나타나는지 컨트롤하기 위해 view에 전달될 수 있음
     *
     * {@link #VIEW_PARAMS_WEEK} is the only required field, though the default
     * values are unlikely to fit most layouts correctly.
     * 기본값이 레이아웃에 제대로 맞지 않을 것 같지만, {@link #VIEW_PARAMS_WEEK}는 필수 필드임
     */
    /**
     * This sets the height of this week in pixels
     * 이번 주의 높이를 픽셀로 설정
     */
    public static final String VIEW_PARAMS_HEIGHT = "height";
    /**
     * This specifies the position (or weeks since the epoch) of this week,
     * calculated using {@link Utils#getWeeksSinceEpochFromJulianDay}
     * 이 값은 {}를 사용하여 계산된 이 주의 위치(혹은 epoch 이후의)를 지정
     */
    public static final String VIEW_PARAMS_MONTH = "month";
    /**
     * This specifies the position (or weeks since the epoch) of this week,
     * calculated using {@link Utils#getWeeksSinceEpochFromJulianDay}
     */
    public static final String VIEW_PARAMS_YEAR = "year";
    /**
     * This sets one of the days in this view as selected {@link Time#SUNDAY}
     * through {@link Time#SATURDAY}.
     * view에서 선택된 날을 일요일~토요일로 설정
     */
    public static final String VIEW_PARAMS_SELECTED_DAY = "selected_day";
    /**
     * Which day the week should start on. {@link Time#SUNDAY} through
     * {@link Time#SATURDAY}.
     * 어느 요일에 시작할 것인가
     */
    public static final String VIEW_PARAMS_WEEK_START = "week_start";
    /**
     * How many days to display at a time. Days will be displayed starting with
     * {@link #mWeekStart}.
     * 한 번에 표시할 날짜
     */
    public static final String VIEW_PARAMS_NUM_DAYS = "num_days";
    /**
     * Which month is currently in focus, as defined by {@link Time#month}
     * [0-11].
     * {}에서 정의한, 현재 포커싱하고 있는 월
     */
    public static final String VIEW_PARAMS_FOCUS_MONTH = "focus_month";
    /**
     * If this month should display week numbers. false if 0, true otherwise.
     * 이번 달에 주간 번호가 표시되어야 하는가
     * 0이면 false
     */
    public static final String VIEW_PARAMS_SHOW_WK_NUM = "show_wk_num";

    protected static int DEFAULT_HEIGHT = 32;
    protected static int MIN_HEIGHT = 10;
    protected static final int DEFAULT_SELECTED_DAY = -1;
    protected static final int DEFAULT_WEEK_START = Calendar.SUNDAY;
    protected static final int DEFAULT_NUM_DAYS = 7;
    protected static final int DEFAULT_SHOW_WK_NUM = 0;
    protected static final int DEFAULT_FOCUS_MONTH = -1;
    protected static final int DEFAULT_NUM_ROWS = 5;
    protected static final int MAX_NUM_ROWS = 5;

    private static final int SELECTED_CIRCLE_ALPHA = 60;

    protected static int DAY_SEPARATOR_WIDTH = 1;
    protected static int MINI_DAY_NUMBER_TEXT_SIZE;
    protected static int MONTH_LABEL_TEXT_SIZE;
    protected static int MONTH_DAY_LABEL_TEXT_SIZE;
    protected static int MONTH_HEADER_SIZE;
    protected static int DAY_SELECTED_CIRCLE_SIZE;

    // used for scaling to the device density
    // 장치 밀도로 확장하는 데 사용(?)
    protected static float mScale = 0;

    protected DatePickerController mController;

    // affects the padding on the sides of this view
    // 이 뷰의 측면/사이드에 있는 패딩에 영향을 줌
    protected int mEdgePadding = 0;

    private String mDayOfWeekTypeface;
    private String mMonthTitleTypeface;

    protected Paint mMonthNumPaint;
    protected Paint mMonthTitlePaint;
    protected Paint mMonthTitleBGPaint;
    protected Paint mSelectedCirclePaint;
    protected Paint mMonthDayLabelPaint;

    private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;

    // The Julian day of the first day displayed by this item
    // 이 아이템이 표시하는 첫 날의 줄리안 데이
    protected int mFirstJulianDay = -1;
    // The month of the first day in this week
    // 이번 주 첫날의 달
    protected int mFirstMonth = -1;
    // The month of the last day in this week
    // 이번 주 마지막 날의 월
    protected int mLastMonth = -1;

    protected int mMonth;

    protected int mYear;
    // Quick reference to the width of this view, matches parent
    // 이 뷰의 너비에 대한 빠른 참조, 부모와 일지
    protected int mWidth;
    // The height this view should draw at in pixels, set by height param
    // 이 뷰가 그려야 하는 높이(픽셀 단위)
    protected int mRowHeight = DEFAULT_HEIGHT;
    // If this view contains the today
    // 이 뷰가 오늘을 포함하고 있는가
    protected boolean mHasToday = false;
    // Which day is selected [0-6] or -1 if no day is selected
    // 선택된 날[0-6], 선택되지 않은 경우[-1]
    protected int mSelectedDay = -1;
    // Which day is today [0-6] or -1 if no day is today
    // 오늘[0-6], 없음[-1]
    protected int mToday = DEFAULT_SELECTED_DAY;
    // Which day of the week to start on [0-6]
    // 주의 시작 날[0-6]
    protected int mWeekStart = DEFAULT_WEEK_START;
    // How many days to display
    // 얼마나 많이 보여 줄 건지
    protected int mNumDays = DEFAULT_NUM_DAYS;
    // The number of days + a spot for week number if it is displayed
    // 일 수 + 주 번호(표시된 경우)
    protected int mNumCells = mNumDays;
    // The left edge of the selected day
    // 선택된 날의 왼쪽...?
    protected int mSelectedLeft = -1;
    // The right edge of the selected day
    // 선택된 날의 오른쪽...?
    protected int mSelectedRight = -1;

    private final Calendar mCalendar;
    protected final Calendar mDayLabelCalendar;
    private final MonthViewTouchHelper mTouchHelper;

    protected int mNumRows = DEFAULT_NUM_ROWS;

    // Optional listener for handling day click actions
    // 날짜 클릭 액션을 핸들링하기 위한 optinal listener
    protected OnDayClickListener mOnDayClickListener;

    // Whether to prevent setting the accessibility delegate
    // 접근성 delegate 설정 방지 여부
    // delegate: C의 Function Pointer와 유사, 주소값을 가지며 실시간으로 호출할 수 있음
    private boolean mLockAccessibilityDelegate;

    protected int mDayTextColor;
    protected int mTodayNumberColor;
    protected int mDisabledDayTextColor;
    protected int mMonthTitleColor;
    protected int mMonthTitleBGColor;

    public MonthView(Context context) {
        this(context, null);
    }

    public MonthView(Context context, AttributeSet attr) {
        super(context, attr);
        Resources res = context.getResources();

        mDayLabelCalendar = Calendar.getInstance();
        mCalendar = Calendar.getInstance();

        mDayOfWeekTypeface = res.getString(R.string.day_of_week_label_typeface);
        mMonthTitleTypeface = res.getString(R.string.sans_serif);

        mDayTextColor = res.getColor(R.color.date_picker_text_normal);
        String selectedColorName = com.android.nanal.event.Utils.getSharedPreference(context, GeneralPreferences.KEY_COLOR_PREF, "teal");
        mTodayNumberColor = res.getColor(DynamicTheme.getColorId(selectedColorName));
        mDisabledDayTextColor = res.getColor(R.color.date_picker_text_disabled);
        mMonthTitleColor = res.getColor(android.R.color.white);
        mMonthTitleBGColor = res.getColor(R.color.circle_background);

        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

        MINI_DAY_NUMBER_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.day_number_size);
        MONTH_LABEL_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.month_label_size);
        MONTH_DAY_LABEL_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.month_day_label_text_size);
        MONTH_HEADER_SIZE = res.getDimensionPixelOffset(R.dimen.month_list_item_header_height);
        DAY_SELECTED_CIRCLE_SIZE = res
                .getDimensionPixelSize(R.dimen.day_number_select_circle_radius);

        mRowHeight = (res.getDimensionPixelOffset(R.dimen.date_picker_view_animator_height)
                - getMonthHeaderSize()) / MAX_NUM_ROWS;

        // Set up accessibility components.
        // 접근성 요소 설정
        mTouchHelper = getMonthViewTouchHelper();
        ViewCompat.setAccessibilityDelegate(this, mTouchHelper);
        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        mLockAccessibilityDelegate = true;

        // Sets up any standard paints that will be used
        // 사용할 표준 paint 설정
        initView();
    }

    public void setDatePickerController(DatePickerController controller) {
        mController = controller;
    }

    protected MonthViewTouchHelper getMonthViewTouchHelper() {
        return new MonthViewTouchHelper(this);
    }

    @Override
    public void setAccessibilityDelegate(AccessibilityDelegate delegate) {
        // Workaround for a JB MR1 issue where accessibility delegates on
        // top-level ListView items are overwritten.
        // 뭔 소린지 모르겠음 ㅜㅜ
        // 최상위 ListView 항목의 접근성 대표가 덮어쓰는 젤리빈 MR1 문제에 대한 해결 방법
        if (!mLockAccessibilityDelegate) {
            super.setAccessibilityDelegate(delegate);
        }
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        mOnDayClickListener = listener;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // First right-of-refusal goes the touch exploration helper.
        if (mTouchHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                final int day = getDayFromLocation(event.getX(), event.getY());
                if (day >= 0) {
                    onDayClick(day);
                }
                break;
        }
        return true;
    }

    /**
     * Sets up the text and style properties for painting. Override this if you
     * want to use a different paint.
     * painting을 위한 텍스트 및 스타일 특성을 설정
     * 다른 paint를 사용하려는 경우, 오버라이드
     */
    protected void initView() {
        mMonthTitlePaint = new Paint();
        mMonthTitlePaint.setFakeBoldText(true);
        mMonthTitlePaint.setAntiAlias(true);
        mMonthTitlePaint.setTextSize(MONTH_LABEL_TEXT_SIZE);
        mMonthTitlePaint.setTypeface(Typeface.create(mMonthTitleTypeface, Typeface.BOLD));
        mMonthTitlePaint.setColor(mDayTextColor);
        mMonthTitlePaint.setTextAlign(Align.CENTER);
        mMonthTitlePaint.setStyle(Style.FILL);

        mMonthTitleBGPaint = new Paint();
        mMonthTitleBGPaint.setFakeBoldText(true);
        mMonthTitleBGPaint.setAntiAlias(true);
        mMonthTitleBGPaint.setColor(mMonthTitleBGColor);
        mMonthTitleBGPaint.setTextAlign(Align.CENTER);
        mMonthTitleBGPaint.setStyle(Style.FILL);

        mSelectedCirclePaint = new Paint();
        mSelectedCirclePaint.setFakeBoldText(true);
        mSelectedCirclePaint.setAntiAlias(true);
        mSelectedCirclePaint.setColor(mTodayNumberColor);
        mSelectedCirclePaint.setTextAlign(Align.CENTER);
        mSelectedCirclePaint.setStyle(Style.FILL);
        mSelectedCirclePaint.setAlpha(SELECTED_CIRCLE_ALPHA);

        mMonthDayLabelPaint = new Paint();
        mMonthDayLabelPaint.setAntiAlias(true);
        mMonthDayLabelPaint.setTextSize(MONTH_DAY_LABEL_TEXT_SIZE);
        mMonthDayLabelPaint.setColor(mDayTextColor);
        mMonthDayLabelPaint.setTypeface(Typeface.create(mDayOfWeekTypeface, Typeface.NORMAL));
        mMonthDayLabelPaint.setStyle(Style.FILL);
        mMonthDayLabelPaint.setTextAlign(Align.CENTER);
        mMonthDayLabelPaint.setFakeBoldText(true);

        mMonthNumPaint = new Paint();
        mMonthNumPaint.setAntiAlias(true);
        mMonthNumPaint.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
        mMonthNumPaint.setStyle(Style.FILL);
        mMonthNumPaint.setTextAlign(Align.CENTER);
        mMonthNumPaint.setFakeBoldText(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawMonthTitle(canvas);
        drawMonthDayLabels(canvas);
        drawMonthNums(canvas);
    }

    private int mDayOfWeekStart = 0;

    /**
     * Sets all the parameters for displaying this week. The only required
     * parameter is the week number. Other parameters have a default value and
     * will only update if a new value is included, except for focus month,
     * which will always default to no focus month if no value is passed in. See
     * {@link #VIEW_PARAMS_HEIGHT} for more info on parameters.
     * 이번 주에 표시할 모든 패러미터 설정
     * 필요한 매개 변수는 주의 번호뿐
     * 다른 패러미터는 기본값을 가지며, focus된 월을 제외한 새로운 값을 포함하는
     * 경우에만 업데이트되며, 값이 전달되지 않으면 월에 focus 되지 않음
     *
     * @param params A map of the new parameters, see
     *            {@link #VIEW_PARAMS_HEIGHT}
     */
    public void setMonthParams(HashMap<String, Integer> params) {
        if (!params.containsKey(VIEW_PARAMS_MONTH) && !params.containsKey(VIEW_PARAMS_YEAR)) {
            throw new InvalidParameterException("You must specify month and year for this view");
        }
        setTag(params);
        // We keep the current value for any params not present
        // 존재하지 않는 패러미터에 대하여 현재 값 유지
        if (params.containsKey(VIEW_PARAMS_HEIGHT)) {
            mRowHeight = params.get(VIEW_PARAMS_HEIGHT);
            if (mRowHeight < MIN_HEIGHT) {
                mRowHeight = MIN_HEIGHT;
            }
        }
        if (params.containsKey(VIEW_PARAMS_SELECTED_DAY)) {
            mSelectedDay = params.get(VIEW_PARAMS_SELECTED_DAY);
        }

        // Allocate space for caching the day numbers and focus values
        // 일 번호 및 포커스 값을 캐싱할 공간 할당
        mMonth = params.get(VIEW_PARAMS_MONTH);
        mYear = params.get(VIEW_PARAMS_YEAR);

        // Figure out what day today is
        // 오늘이 무슨 날인지
        final Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        mHasToday = false;
        mToday = -1;

        mCalendar.set(Calendar.MONTH, mMonth);
        mCalendar.set(Calendar.YEAR, mYear);
        mCalendar.set(Calendar.DAY_OF_MONTH, 1);
        mDayOfWeekStart = mCalendar.get(Calendar.DAY_OF_WEEK);

        if (params.containsKey(VIEW_PARAMS_WEEK_START)) {
            mWeekStart = params.get(VIEW_PARAMS_WEEK_START);
        } else {
            mWeekStart = mCalendar.getFirstDayOfWeek();
        }

        mNumCells = Utils.getDaysInMonth(mMonth, mYear);
        for (int i = 0; i < mNumCells; i++) {
            final int day = i + 1;
            if (sameDay(day, today)) {
                mHasToday = true;
                mToday = day;
            }
        }
        mNumRows = calculateNumRows();

        // Invalidate cached accessibility information.
        // 캐시된 접근성 정보의 유효성 검사
        mTouchHelper.invalidateRoot();
    }

    public void setSelectedDay(int day) {
        mSelectedDay = day;
    }

    public void reuse() {
        mNumRows = DEFAULT_NUM_ROWS;
        requestLayout();
    }

    private int calculateNumRows() {
        // 숫자 줄 계산
        int offset = findDayOffset();
        int dividend = (offset + mNumCells) / mNumDays;
        int remainder = (offset + mNumCells) % mNumDays;
        return (dividend + (remainder > 0 ? 1 : 0));
    }

    private boolean sameDay(int day, Time today) {
        // 오늘인지
        return mYear == today.year &&
                mMonth == today.month &&
                day == today.monthDay;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mRowHeight * mNumRows
                + getMonthHeaderSize());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;

        // Invalidate cached accessibility information.
        // 캐시된 접근성 정보의 유효성 검사
        mTouchHelper.invalidateRoot();
    }

    public int getMonth() {
        return mMonth;
    }

    public int getYear() {
        return mYear;
    }

    /**
     * A wrapper to the MonthHeaderSize to allow override it in children
     * 자식에게 오버라이드할 수 있는 MonthHeaderSize의 wrapper
     */
    protected int getMonthHeaderSize() {
        return MONTH_HEADER_SIZE;
    }

    private String getMonthAndYearString() {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                | DateUtils.FORMAT_NO_MONTH_DAY;
        mStringBuilder.setLength(0);
        long millis = mCalendar.getTimeInMillis();
        return DateUtils.formatDateRange(getContext(), mFormatter, millis, millis, flags,
                Time.getCurrentTimezone()).toString();
    }

    protected void drawMonthTitle(Canvas canvas) {
        int x = (mWidth + 2 * mEdgePadding) / 2;
        int y = (getMonthHeaderSize() - MONTH_DAY_LABEL_TEXT_SIZE) / 2 + (MONTH_LABEL_TEXT_SIZE / 3);
        canvas.drawText(getMonthAndYearString(), x, y, mMonthTitlePaint);
    }

    protected void drawMonthDayLabels(Canvas canvas) {
        int y = getMonthHeaderSize() - (MONTH_DAY_LABEL_TEXT_SIZE / 2);
        int dayWidthHalf = (mWidth - mEdgePadding * 2) / (mNumDays * 2);

        for (int i = 0; i < mNumDays; i++) {
            int calendarDay = (i + mWeekStart) % mNumDays;
            int x = (2 * i + 1) * dayWidthHalf + mEdgePadding;
            mDayLabelCalendar.set(Calendar.DAY_OF_WEEK, calendarDay);
            canvas.drawText(mDayLabelCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT,
                    Locale.getDefault()).toUpperCase(Locale.getDefault()), x, y,
                    mMonthDayLabelPaint);
        }
    }

    /**
     * Draws the week and month day numbers for this week. Override this method
     * if you need different placement.
     * 이번 주에 대한 주간 및 월 번호 그리기.
     * 다른 배치가 필요한 경우 오버라이딩하기.
     *
     * @param canvas The canvas to draw on
     */
    protected void drawMonthNums(Canvas canvas) {
        int y = (((mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2) - DAY_SEPARATOR_WIDTH)
                + getMonthHeaderSize();
        final float dayWidthHalf = (mWidth - mEdgePadding * 2) / (mNumDays * 2.0f);
        int j = findDayOffset();
        for (int dayNumber = 1; dayNumber <= mNumCells; dayNumber++) {
            final int x = (int)((2 * j + 1) * dayWidthHalf + mEdgePadding);

            int yRelativeToDay = (mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2 - DAY_SEPARATOR_WIDTH;

            final int startX = (int)(x - dayWidthHalf);
            final int stopX = (int)(x + dayWidthHalf);
            final int startY = (int)(y - yRelativeToDay);
            final int stopY = (int)(startY + mRowHeight);

            drawMonthDay(canvas, mYear, mMonth, dayNumber, x, y, startX, stopX, startY, stopY);

            j++;
            if (j == mNumDays) {
                j = 0;
                y += mRowHeight;
            }
        }
    }

    /**
     * This method should draw the month day.  Implemented by sub-classes to allow customization.
     * 월일을 정해야 하는 메소드. 사용자 지정을 허용하기 위해 하위 클래스에 의해 구현됨.
     *
     * @param canvas  The canvas to draw on
     * @param year  The year of this month day
     * @param month  The month of this month day
     * @param day  The day number of this month day
     * @param x  The default x position to draw the day number
     * @param y  The default y position to draw the day number
     * @param startX  The left boundary of the day number rect
     * @param stopX  The right boundary of the day number rect
     * @param startY  The top boundary of the day number rect
     * @param stopY  The bottom boundary of the day number rect
     */
    public abstract void drawMonthDay(Canvas canvas, int year, int month, int day,
                                      int x, int y, int startX, int stopX, int startY, int stopY);

    protected int findDayOffset() {
        // (주의 시작 요일 < 기본 시작(일요일) ? (시작 요일 + 7일) : 시작 요일) - 기본 시작일
        return (mDayOfWeekStart < mWeekStart ? (mDayOfWeekStart + mNumDays) : mDayOfWeekStart)
                - mWeekStart;
    }


    /**
     * Calculates the day that the given x position is in, accounting for week
     * number. Returns the day or -1 if the position wasn't in a day.
     * 지정된 x 위치가 있는 요일을 계산하여 주 수를 계산한다.
     * 해당 day를 리턴하고, 값이 없는 경우 -1을 리턴한다.
     *
     * @param x The x position of the touch event
     *           터치 이벤트의 x 좌표
     * @return The day number, or -1 if the position wasn't in a day
     */
    public int getDayFromLocation(float x, float y) {
        final int day = getInternalDayFromLocation(x, y);
        if (day < 1 || day > mNumCells) {
            return -1;
        }
        return day;
    }

    /**
     * Calculates the day that the given x position is in, accounting for week
     * number.
     *
     * @param x The x position of the touch event
     * @return The day number
     */
    protected int getInternalDayFromLocation(float x, float y) {
        int dayStart = mEdgePadding;
        if (x < dayStart || x > mWidth - mEdgePadding) {
            return -1;
        }
        // Selection is (x - start) / (pixels/day) == (x -s) * day / pixels
        int row = (int) (y - getMonthHeaderSize()) / mRowHeight;
        int column = (int) ((x - dayStart) * mNumDays / (mWidth - dayStart - mEdgePadding));

        int day = column - findDayOffset() + 1;
        day += row * mNumDays;
        return day;
    }

    /**
     * Called when the user clicks on a day. Handles callbacks to the
     * {@link OnDayClickListener} if one is set.
     * <p/>
     * If the day is out of the range set by minDate and/or maxDate, this is a no-op.
     * 사용자가 날짜를 클릭하면 호출된다. {}가 설정된 경우, 그에 대한 콜백 처리.
     * minDate와 maxDate에서 설정한 범위를 벗어난 경우 아무 일도 하지 않는다.
     *
     * @param day The day that was clicked
     */
    private void onDayClick(int day) {
        // If the min / max date are set, only process the click if it's a valid selection.
        // 최소/최대 날짜가 설정된 경우, 유효한 선택일 경우에만 클릭 처리
        if (isOutOfRange(mYear, mMonth, day)) {
            return;
        }


        if (mOnDayClickListener != null) {
            mOnDayClickListener.onDayClick(this, new CalendarDay(mYear, mMonth, day));
        }

        // This is a no-op if accessibility is turned off.
        // 접근성이 꺼져 있다면, 아무 일도 하지 않음
        mTouchHelper.sendEventForVirtualView(day, AccessibilityEvent.TYPE_VIEW_CLICKED);
    }

    /**
     * @return true if the specified year/month/day are within the range set by minDate and maxDate.
     * If one or either have not been set, they are considered as Integer.MIN_VALUE and
     * Integer.MAX_VALUE.
     * 지정된 연/원/일이 minDate와 maxDate에 의해 설정된 범위 내에 있는 경우 return true.
     * 둘 중 하나 또는 둘 다 설정되지 않은 경우, Integer.MIN_VALUE와 Integer.MAX_VALUE로 간주한다.
     */
    protected boolean isOutOfRange(int year, int month, int day) {
        if (isBeforeMin(year, month, day)) {
            return true;
        } else if (isAfterMax(year, month, day)) {
            return true;
        }

        return false;
    }

    private boolean isBeforeMin(int year, int month, int day) {
        if (mController == null) {
            return false;
        }
        Calendar minDate = mController.getMinDate();
        if (minDate == null) {
            return false;
        }

        if (year < minDate.get(Calendar.YEAR)) {
            return true;
        } else if (year > minDate.get(Calendar.YEAR)) {
            return false;
        }

        if (month < minDate.get(Calendar.MONTH)) {
            return true;
        } else if (month > minDate.get(Calendar.MONTH)) {
            return false;
        }

        if (day < minDate.get(Calendar.DAY_OF_MONTH)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAfterMax(int year, int month, int day) {
        if (mController == null) {
            return false;
        }
        Calendar maxDate = mController.getMaxDate();
        if (maxDate == null) {
            return false;
        }

        if (year > maxDate.get(Calendar.YEAR)) {
            return true;
        } else if (year < maxDate.get(Calendar.YEAR)) {
            return false;
        }

        if (month > maxDate.get(Calendar.MONTH)) {
            return true;
        } else if (month < maxDate.get(Calendar.MONTH)) {
            return false;
        }

        if (day > maxDate.get(Calendar.DAY_OF_MONTH)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return The date that has accessibility focus, or {@code null} if no date
     *         has focus
     *         접근성 포커스가 있는 날짜, 또는 포커스가 없는 경우 null
     */
    public CalendarDay getAccessibilityFocus() {
        final int day = mTouchHelper.getFocusedVirtualView();
        if (day >= 0) {
            return new CalendarDay(mYear, mMonth, day);
        }
        return null;
    }

    /**
     * Clears accessibility focus within the view. No-op if the view does not
     * contain accessibility focus.
     * view 내에서 접근성 포커스를 지운다.
     * 만약 view에 접근성 포커스가 포함되어 있지 않으면 아무 것도 안 한다.
     */
    public void clearAccessibilityFocus() {
        mTouchHelper.clearFocusedVirtualView();
    }

    /**
     * Attempts to restore accessibility focus to the specified date.
     * 지정된 날짜에 대한 접근성 포커스 복원 시도
     *
     * @param day The date which should receive focus
     *              포커스 받을 날짜
     * @return {@code false} if the date is not valid for this month view, or
     *                         날짜가 이 달 view에 유효하지 않은 경우,
     *         {@code true} if the date received focus
     *                       날짜가 포커스된 경우
     */
    public boolean restoreAccessibilityFocus(CalendarDay day) {
        if ((day.year != mYear) || (day.month != mMonth) || (day.day > mNumCells)) {
            return false;
        }
        mTouchHelper.setFocusedVirtualView(day.day);
        return true;
    }

    /**
     * Provides a virtual view hierarchy for interfacing with an accessibility
     * service.
     * 접근성 서비스와 인터페이스하기 위한 가상의 view 계층 제공
     */
    protected class MonthViewTouchHelper extends ExploreByTouchHelper {
        private static final String DATE_FORMAT = "dd MMMM yyyy";

        private final Rect mTempRect = new Rect();
        private final Calendar mTempCalendar = Calendar.getInstance();

        public MonthViewTouchHelper(View host) {
            super(host);
        }

        public void setFocusedVirtualView(int virtualViewId) {
            getAccessibilityNodeProvider(MonthView.this).performAction(
                    virtualViewId, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, null);
        }

        public void clearFocusedVirtualView() {
            final int focusedVirtualView = getFocusedVirtualView();
            if (focusedVirtualView != ExploreByTouchHelper.INVALID_ID) {
                getAccessibilityNodeProvider(MonthView.this).performAction(
                        focusedVirtualView,
                        AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS,
                        null);
            }
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            final int day = getDayFromLocation(x, y);
            if (day >= 0) {
                return day;
            }
            return ExploreByTouchHelper.INVALID_ID;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            for (int day = 1; day <= mNumCells; day++) {
                virtualViewIds.add(day);
            }
        }

        @Override
        protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            event.setContentDescription(getItemDescription(virtualViewId));
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId,
                                                    AccessibilityNodeInfoCompat node) {
            getItemBounds(virtualViewId, mTempRect);

            node.setContentDescription(getItemDescription(virtualViewId));
            node.setBoundsInParent(mTempRect);
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK);

            if (virtualViewId == mSelectedDay) {
                node.setSelected(true);
            }

        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action,
                                                        Bundle arguments) {
            switch (action) {
                case AccessibilityNodeInfo.ACTION_CLICK:
                    onDayClick(virtualViewId);
                    return true;
            }

            return false;
        }

        /**
         * Calculates the bounding rectangle of a given time object.
         * 주어진 시간 객체의 직사각형 범위를 계산한다.
         *
         * @param day The day to calculate bounds for
         *             범위를 계산할 날짜
         * @param rect The rectangle in which to store the bounds
         *              범위를 저장할 직사각형
         */
        protected void getItemBounds(int day, Rect rect) {
            final int offsetX = mEdgePadding;
            final int offsetY = getMonthHeaderSize();
            final int cellHeight = mRowHeight;
            final int cellWidth = ((mWidth - (2 * mEdgePadding)) / mNumDays);
            final int index = ((day - 1) + findDayOffset());
            final int row = (index / mNumDays);
            final int column = (index % mNumDays);
            final int x = (offsetX + (column * cellWidth));
            final int y = (offsetY + (row * cellHeight));

            rect.set(x, y, (x + cellWidth), (y + cellHeight));
        }

        /**
         * Generates a description for a given time object. Since this
         * description will be spoken, the components are ordered by descending
         * specificity as DAY MONTH YEAR.
         * 지정된 시간 개체에 대한 설명을 만든다.
         * 설명이 설명되기 때문에(?), 구성 요소는 DAY MONTH YEAR로 내림차순 정렬된다.
         *
         * @param day The day to generate a description for
         *             설명을 생성하는 날짜
         * @return A description of the time object
         *          시간 객체에 대한 설명
         */
        protected CharSequence getItemDescription(int day) {
            mTempCalendar.set(mYear, mMonth, day);
            final CharSequence date = DateFormat.format(DATE_FORMAT,
                    mTempCalendar.getTimeInMillis());

            if (day == mSelectedDay) {
                return getContext().getString(R.string.item_is_selected, date);
            }

            return date;
        }
    }

    /**
     * Handles callbacks when the user clicks on a time object.
     * 사용자가 시간 객체를 클릭할 때 콜백 처리
     */
    public interface OnDayClickListener {
        public void onDayClick(MonthView view, CalendarDay day);
    }
}
