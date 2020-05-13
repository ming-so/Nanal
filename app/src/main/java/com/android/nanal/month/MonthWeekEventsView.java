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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Service;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.provider.CalendarContract.Attendees;
import android.text.DynamicLayout;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.android.nanal.DynamicTheme;
import com.android.nanal.R;
import com.android.nanal.calendar.LunarUtils;
import com.android.nanal.calendar.ViewDetailsPreferences;
import com.android.nanal.diary.Diary;
import com.android.nanal.event.Event;
import com.android.nanal.event.GeneralPreferences;
import com.android.nanal.event.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;


public class MonthWeekEventsView extends SimpleWeekView {

    public static final String VIEW_PARAMS_ORIENTATION = "orientation";
    public static final String VIEW_PARAMS_ANIMATE_TODAY = "animate_today";
    private static final String TAG = "MonthView";
    private static final boolean DEBUG_LAYOUT = true;
    private static final int mClickedAlpha = 128;
    protected static StringBuilder mStringBuilder = new StringBuilder(50);
    // TODO recreate formatter when locale changes
    protected static Formatter mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
    /* NOTE: these are not constants, and may be multiplied by a scale factor */
    private static int mTextSizeMonthNumber = 28;
    private static int mTextSizeLunar = 10;
    private static int mTextSizeEvent = 12;
    private static int mTextSizeEventTitle = 14;
    private static int mTextSizeMoreEvents = 12;
    private static int mTextSizeMonthName = 14;
    private static int mTextSizeWeekNum = 9;
    private static int mDnaMargin = 4;
    private static int mDnaAllDayHeight = 4;
    private static int mDnaMinSegmentHeight = 4;
    private static int mDnaWidth = 8;
    private static int mDnaAllDayWidth = 32;
    private static int mDnaSidePadding = 6;
    private static int mConflictColor = Color.BLACK;
    private static int mEventTextColor = Color.WHITE;
    private static int mDefaultEdgeSpacing = 0;
    private static int mSidePaddingMonthNumber = 4;
    private static int mTopPaddingMonthNumber = 3;
    private static int mTopPaddingWeekNumber = 4;
    private static int mSidePaddingWeekNumber = 12;
    private static int mDaySeparatorOuterWidth = 0;
    private static int mDaySeparatorInnerWidth = 1;
    private static int mDaySeparatorVerticalLength = 53;
    private static int mDaySeparatorVerticalLenghtPortrait = 64;
    private static int mMinWeekWidth = 50;
    private static int mLunarPaddingLunar = 2;
    private static int mEventXOffsetLandscape = 38;
    private static int mEventYOffsetLandscape = 8;
    private static int mEventYOffsetPortrait = 2;
    private static int mEventSquareWidth = 3;
    private static int mEventSquareHeight = 10;     // 이벤트 상자 높이!
    private static int mEventSquareBorder = 0;
    private static int mEventLinePadding = 2;
    private static int mEventRightPadding = 4;
    private static int mEventBottomPadding = 1;
    private static int mTodayHighlightWidth = 2;
    private static int mSpacingWeekNumber = 0;
    private static int mBorderSpace;
    private static int mStrokeWidthAdj;
    private static int mDiaryCircleSize = 3;
    private static boolean mInitialized = false;
    private static boolean mShowDetailsInMonth;
    private static int mMaxLinesInEvent = 8; //todo - should be configurable
    private static int mMaxLinesInDiary = 2;
    private final TodayAnimatorListener mAnimatorListener = new TodayAnimatorListener();
    protected Time mToday = new Time();
    protected boolean mHasToday = false;
    protected int mTodayIndex = -1;
    protected int mOrientation = Configuration.ORIENTATION_LANDSCAPE;
    protected List<ArrayList<Event>> mEvents = null;
    protected List<ArrayList<Diary>> mDiaries = null;
    protected ArrayList<Event> mUnsortedEvents = null;
    // This is for drawing the outlines around event chips and supports up to 10
    // events being drawn on each day. The code will expand this if necessary.
    // 이벤트 칩 주위의 아웃라인을 그리고 매일 최대 10개의 이벤트를 지원하기 위한 것임
    // 필요하다면 코드에서 이걸 확장할 것임
    protected FloatRef mEventOutlines = new FloatRef(10 * 4 * 4 * 7);
    protected Paint mMonthNamePaint;
    protected TextPaint mEventPaint;
    protected TextPaint mSolidBackgroundEventPaint;
    protected TextPaint mFramedEventPaint;
    protected TextPaint mDeclinedEventPaint;
    protected TextPaint mEventExtrasPaint;
    protected TextPaint mEventDeclinedExtrasPaint;
    protected Paint mWeekNumPaint;
    protected Paint mDNAAllDayPaint;
    protected Paint mDNATimePaint;
    protected Paint mEventSquarePaint;
    protected Paint mDiaryCirclePaint;
    protected Drawable mTodayDrawable;
    protected int mMonthNumHeight;
    protected int mMonthNumAscentHeight;
    protected int mEventHeight;
    protected int mEventAscentHeight;
    protected int mExtrasHeight;
    protected int mExtrasAscentHeight;
    protected int mExtrasDescent;
    protected int mWeekNumAscentHeight;
    protected int mMonthBGColor;
    protected int mMonthBGOtherColor;
    protected int mMonthBGTodayColor;
    protected int mMonthBGFocusMonthColor;
    protected int mMonthNumColor;
    protected int mMonthNumOtherColor;
    protected int mMonthNumTodayColor;
    protected int mMonthNameColor;
    protected int mMonthNameOtherColor;
    protected int mMonthEventColor;
    protected int mMonthDeclinedEventColor;
    protected int mMonthDeclinedExtrasColor;
    protected int mMonthEventExtraColor;
    protected int mMonthEventOtherColor;
    protected int mMonthEventExtraOtherColor;
    protected int mMonthWeekNumColor;
    protected int mMonthBusyBitsBgColor;
    protected int mMonthBusyBitsBusyTimeColor;
    protected int mMonthBusyBitsConflictTimeColor;
    protected int mEventChipOutlineColor = 0xFFFFFFFF;
    protected int mDaySeparatorInnerColor;
    protected int mTodayAnimateColor;
    protected final Resources mResources;
    HashMap<Integer, Utils.DNAStrand> mDna = null;
    HashMap<Integer, Utils.DNAStrand> mDnaD = null;
    private int mClickedDayIndex = -1;
    private int mClickedDayColor;
    private boolean mAnimateToday;
    private int mAnimateTodayAlpha = 0;
    private ObjectAnimator mTodayAnimator = null;
    private int[] mDayXs;
    private int[] mDayXsD;

    /**
     * Shows up as an error if we don't include this.
     * 포함하지 않으면 오류가 뜸
     */
    public MonthWeekEventsView(Context context) {
        super(context);
        mResources = context.getResources();
    }

    // Sets the list of events for this week. Takes a sorted list of arrays
    // divided up by day for generating the large month version and the full
    // arraylist sorted by start time to generate the dna version.
    // 이번 주에 대한 이벤트 목록을 설정함
    // 대규모 월 버전을 생성하기 위해 일별로 분할된 배열들의 정렬된 List와
    // dna 버전을 생성하기 위해 시작 시간으로 정렬된 전체 ArrayList를 사용함
    public void setEvents(List<ArrayList<Event>> sortedEvents, ArrayList<Event> unsortedEvents) {
        setEvents(sortedEvents);
        // The mMinWeekWidth is a hack to prevent the view from trying to
        // generate dna bits before its width has been fixed.
        // mMinWeekWidth는 width가 고정되기 전에 dna 비트를 생성하려는 view를 막기 위한 장치임
        createDna(unsortedEvents);
    }

    public void setDiaries(List<ArrayList<Diary>> sortedDiaries, ArrayList<Diary> unsortedDiaries) {
        setDiaries(sortedDiaries);
    }

    /**
     * Sets up the dna bits for the view. This will return early if the view
     * isn't in a state that will create a valid set of dna yet (such as the
     * views width not being set correctly yet).
     * view에 대한 dna bits 설정
     * view가 아직 유효한 dna set을 생성하는 상태가 아닌 경우
     * (예: view의 width가 아직 올바르게 설정되지 않은 경우) 이르게 반환됨
     */
    public void createDna(ArrayList<Event> unsortedEvents) {
        if (unsortedEvents == null || mWidth <= mMinWeekWidth || getContext() == null) {
            // Stash the list of events for use when this view is ready, or
            // just clear it if a null set has been passed to this view
            // 이 view가 준비되었을 때 사용할 이벤트의 목록을 저장하거나,
            // null set이 이 view로 전달된 경우에는 삭제함
            mUnsortedEvents = unsortedEvents;
            mDna = null;
            return;
        } else {
            // clear the cached set of events since we're ready to build it now
            // 지금 만들 준비가 되었기 때문에 캐시된 이벤트 set을 비워 줌
            mUnsortedEvents = null;
        }
        // Create the drawing coordinates for dna
        // dna에 대한 drawing 좌표 작성
        if (!mShowDetailsInMonth) {
            int numDays = mEvents.size();
            int effectiveWidth = mWidth - mPadding * 2;

            mDnaAllDayWidth = effectiveWidth / numDays - 2 * mDnaSidePadding;
            mDNAAllDayPaint.setStrokeWidth(mDnaAllDayWidth);
            mDayXs = new int[numDays];
            for (int day = 0; day < numDays; day++) {
                mDayXs[day] = computeDayLeftPosition(day) + mDnaWidth / 2 + mDnaSidePadding;

            }

            int top = mDaySeparatorInnerWidth + mDnaMargin + mDnaAllDayHeight + 1;
            int bottom = mHeight - mDnaMargin;
            mDna = Utils.createDNAStrands(mFirstJulianDay, unsortedEvents, top, bottom,
                    mDnaMinSegmentHeight, mDayXs, getContext());
        }
    }

    public void setEvents(List<ArrayList<Event>> sortedEvents) {
        mEvents = sortedEvents;
        if (sortedEvents == null) {
            return;
        }
        if (sortedEvents.size() != mNumDays) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.wtf(TAG, "Events size must be same as days displayed: size="
                        + sortedEvents.size() + " days=" + mNumDays);
            }
            mEvents = null;
            return;
        }
    }

    public void setDiaries(List<ArrayList<Diary>> sortedDiaries) {
        mDiaries = sortedDiaries;
        if(sortedDiaries == null) {
            Log.wtf(TAG, "setDiaries, sortedDiaries == null!!");
            return;
        }
        //Log.i(TAG, "setDiaries, mDiaries.size="+mDiaries.size());
        if(sortedDiaries.size() != mNumDays) {
            if(Log.isLoggable(TAG, Log.ERROR)) {
                Log.wtf(TAG, "Diaries size must be same as days displayed: size="
                        + sortedDiaries.size() + " days=" + mNumDays);
            }
            mDiaries = null;
            return;
        }
    }

    protected void loadColors(Context context) {
        Resources res = context.getResources();
        DynamicTheme dynamicTheme = new DynamicTheme();

        String selectedColorName = Utils.getSharedPreference(context, GeneralPreferences.KEY_COLOR_PREF, "teal");

        mMonthWeekNumColor = dynamicTheme.getColor(context, "month_week_num_color");
        mMonthNumColor = dynamicTheme.getColor(context, "month_day_number");
        mMonthNumOtherColor = dynamicTheme.getColor(context, "month_day_number_other");
        mMonthNumTodayColor = dynamicTheme.getColor(context, "month_today_number");
        mMonthEventColor = dynamicTheme.getColor(context, "month_event_color");
        mMonthDeclinedEventColor = dynamicTheme.getColor(context, "agenda_item_declined_color");
        mMonthDeclinedExtrasColor = dynamicTheme.getColor(context, "agenda_item_where_declined_text_color");
        mMonthEventExtraColor = dynamicTheme.getColor(context, "month_event_extra_color");
        mMonthEventOtherColor = dynamicTheme.getColor(context, "month_event_other_color");
        mMonthEventExtraOtherColor = dynamicTheme.getColor(context, "month_event_extra_other_color");
//        mMonthBGTodayColor = dynamicTheme.getColor(context, "month_today_bgcolor");
        mMonthBGTodayColor = res.getColor(DynamicTheme.getColorToday(selectedColorName));
        mMonthBGFocusMonthColor = dynamicTheme.getColor(context, "month_focus_month_bgcolor");
        mMonthBGOtherColor = dynamicTheme.getColor(context, "month_other_bgcolor");
        mMonthBGColor = dynamicTheme.getColor(context, "month_bgcolor");
        mDaySeparatorInnerColor = dynamicTheme.getColor(context, "month_grid_lines");
        mTodayAnimateColor = dynamicTheme.getColor(context, "today_highlight_color");
        mClickedDayColor = dynamicTheme.getColor(context, "day_clicked_background_color");
        mTodayDrawable = res.getDrawable(R.drawable.today_blue_week_holo_light);
    }

    /**
     * Sets up the text and style properties for painting. Override this if you
     * want to use a different paint.
     * painting을 위한 텍스트와 스타일 특성을 설정함
     * 다른 paint를 사용하려면 오버라이딩하기
     */
    @Override
    protected void initView() {
        super.initView();

        if (!mInitialized) {
            Resources resources = getContext().getResources();
            mShowDetailsInMonth = Utils.getConfigBool(getContext(), R.bool.show_details_in_month);
            mTextSizeEventTitle = resources.getInteger(R.integer.text_size_event_title);
            mTextSizeMonthNumber = resources.getInteger(R.integer.text_size_month_number);
            mSidePaddingMonthNumber = resources.getInteger(R.integer.month_day_number_margin);
            mConflictColor = resources.getColor(R.color.month_dna_conflict_time_color);
            mEventTextColor = resources.getColor(R.color.calendar_event_text_color);
            if (mScale != 1) {
                mTopPaddingMonthNumber *= mScale;
                mTopPaddingWeekNumber *= mScale;
                mSidePaddingMonthNumber *= mScale;
                mSidePaddingWeekNumber *= mScale;
                mSpacingWeekNumber *= mScale;
                mTextSizeMonthNumber *= mScale;
                mTextSizeLunar *= mScale;
                mTextSizeEvent *= mScale;
                mTextSizeEventTitle *= mScale;
                mTextSizeMoreEvents *= mScale;
                mTextSizeMonthName *= mScale;
                mTextSizeWeekNum *= mScale;
                mDaySeparatorOuterWidth *= mScale;
                mDaySeparatorInnerWidth *= mScale;
                mDaySeparatorVerticalLength *= mScale;
                mDaySeparatorVerticalLenghtPortrait *= mScale;
                mEventXOffsetLandscape *= mScale;
                mEventYOffsetLandscape *= mScale;
                mEventYOffsetPortrait *= mScale;
                mEventSquareWidth *= mScale;
                mEventSquareHeight *= mScale;
                mEventSquareBorder *= mScale;
                mEventLinePadding *= mScale;
                mEventBottomPadding *= mScale;
                mEventRightPadding *= mScale;
                mDnaMargin *= mScale;
                mDnaWidth *= mScale;
                mDnaAllDayHeight *= mScale;
                mDnaMinSegmentHeight *= mScale;
                mDnaSidePadding *= mScale;
                mDefaultEdgeSpacing *= mScale;
                mDnaAllDayWidth *= mScale;
                mTodayHighlightWidth *= mScale;
                mDiaryCircleSize *= mScale;
            }
            mBorderSpace = mEventSquareBorder + 1;       // want a 1-pixel gap inside border  1픽셀의 간격
            mStrokeWidthAdj = mEventSquareBorder / 2;   // adjust bounds for stroke width  선 width의 경계 조정
            if (!mShowDetailsInMonth) {
                mTopPaddingMonthNumber += mDnaAllDayHeight + mDnaMargin;
            }
            mInitialized = true;
        }
        mPadding = mDefaultEdgeSpacing;
        loadColors(getContext());
        // TODO modify paint properties depending on isMini

        mMonthNumPaint = new Paint();
        mMonthNumPaint.setFakeBoldText(false);
        mMonthNumPaint.setAntiAlias(true);
        mMonthNumPaint.setTextSize(mTextSizeMonthNumber);
        mMonthNumPaint.setColor(mMonthNumColor);
        mMonthNumPaint.setStyle(Style.FILL);
        mMonthNumPaint.setTextAlign(Align.RIGHT);
        mMonthNumPaint.setTypeface(Typeface.DEFAULT);

        mMonthNumAscentHeight = (int) (-mMonthNumPaint.ascent() + 0.5f);
        mMonthNumHeight = (int) (mMonthNumPaint.descent() - mMonthNumPaint.ascent() + 0.5f);

        mEventPaint = new TextPaint();
        mEventPaint.setFakeBoldText(true);
        mEventPaint.setAntiAlias(true);
        mEventPaint.setTextSize(mTextSizeEventTitle);
        mEventPaint.setColor(mMonthEventColor);

        mSolidBackgroundEventPaint = new TextPaint(mEventPaint);
        mSolidBackgroundEventPaint.setColor(mEventTextColor);
        mFramedEventPaint = new TextPaint(mSolidBackgroundEventPaint);

        mDeclinedEventPaint = new TextPaint();
        mDeclinedEventPaint.setFakeBoldText(true);
        mDeclinedEventPaint.setAntiAlias(true);
        mDeclinedEventPaint.setTextSize(mTextSizeEventTitle);
        mDeclinedEventPaint.setColor(mMonthDeclinedEventColor);

        mEventAscentHeight = (int) (-mEventPaint.ascent() + 0.5f);
        mEventHeight = (int) (mEventPaint.descent() - mEventPaint.ascent() + 0.5f);

        mEventExtrasPaint = new TextPaint();
        mEventExtrasPaint.setFakeBoldText(false);
        mEventExtrasPaint.setAntiAlias(true);
        mEventExtrasPaint.setStrokeWidth(mEventSquareBorder);
        mEventExtrasPaint.setTextSize(mTextSizeEvent);
        mEventExtrasPaint.setColor(mMonthEventExtraColor);
        mEventExtrasPaint.setStyle(Style.FILL);
        mEventExtrasPaint.setTextAlign(Align.LEFT);
        mExtrasHeight = (int)(mEventExtrasPaint.descent() - mEventExtrasPaint.ascent() + 0.5f);
        mExtrasAscentHeight = (int)(-mEventExtrasPaint.ascent() + 0.5f);
        mExtrasDescent = (int)(mEventExtrasPaint.descent() + 0.5f);

        mEventDeclinedExtrasPaint = new TextPaint();
        mEventDeclinedExtrasPaint.setFakeBoldText(false);
        mEventDeclinedExtrasPaint.setAntiAlias(true);
        mEventDeclinedExtrasPaint.setStrokeWidth(mEventSquareBorder);
        mEventDeclinedExtrasPaint.setTextSize(mTextSizeEvent);
        mEventDeclinedExtrasPaint.setColor(mMonthDeclinedExtrasColor);
        mEventDeclinedExtrasPaint.setStyle(Style.FILL);
        mEventDeclinedExtrasPaint.setTextAlign(Align.LEFT);

        mWeekNumPaint = new Paint();
        mWeekNumPaint.setFakeBoldText(false);
        mWeekNumPaint.setAntiAlias(true);
        mWeekNumPaint.setTextSize(mTextSizeWeekNum);
        mWeekNumPaint.setColor(mWeekNumColor);
        mWeekNumPaint.setStyle(Style.FILL);
        mWeekNumPaint.setTextAlign(Align.RIGHT);

        mWeekNumAscentHeight = (int) (-mWeekNumPaint.ascent() + 0.5f);

        mDNAAllDayPaint = new Paint();
        mDNATimePaint = new Paint();
        mDNATimePaint.setColor(mMonthBusyBitsBusyTimeColor);
        mDNATimePaint.setStyle(Style.FILL_AND_STROKE);
        mDNATimePaint.setStrokeWidth(mDnaWidth);
        mDNATimePaint.setAntiAlias(false);
        mDNAAllDayPaint.setColor(mMonthBusyBitsConflictTimeColor);
        mDNAAllDayPaint.setStyle(Style.FILL_AND_STROKE);
        mDNAAllDayPaint.setStrokeWidth(mDnaAllDayWidth);
        mDNAAllDayPaint.setAntiAlias(false);

        mEventSquarePaint = new Paint();
        mEventSquarePaint.setStrokeWidth(mEventSquareBorder);
        mEventSquarePaint.setAntiAlias(false);

        mDiaryCirclePaint = new Paint();
        String selectedColorName = Utils.getSharedPreference(getContext(), GeneralPreferences.KEY_COLOR_PREF, "teal");
        mDiaryCirclePaint.setColor(getContext().getResources().getColor(DynamicTheme.getColorId(selectedColorName)));
        mDiaryCirclePaint.setAntiAlias(false);

        if (DEBUG_LAYOUT) {
            Log.d("EXTRA", "mScale=" + mScale);
            Log.d("EXTRA", "mMonthNumPaint ascent=" + mMonthNumPaint.ascent()
                    + " descent=" + mMonthNumPaint.descent() + " int height=" + mMonthNumHeight);
            Log.d("EXTRA", "mEventPaint ascent=" + mEventPaint.ascent()
                    + " descent=" + mEventPaint.descent() + " int height=" + mEventHeight
                    + " int ascent=" + mEventAscentHeight);
            Log.d("EXTRA", "mEventExtrasPaint ascent=" + mEventExtrasPaint.ascent()
                    + " descent=" + mEventExtrasPaint.descent() + " int height=" + mExtrasHeight);
            Log.d("EXTRA", "mWeekNumPaint ascent=" + mWeekNumPaint.ascent()
                    + " descent=" + mWeekNumPaint.descent());
        }
    }

    @Override
    public void setWeekParams(HashMap<String, Integer> params, String tz) {
        super.setWeekParams(params, tz);

        if (params.containsKey(VIEW_PARAMS_ORIENTATION)) {
            mOrientation = params.get(VIEW_PARAMS_ORIENTATION);
        }

        updateToday(tz);
        mNumCells = mNumDays + 1;

        if (params.containsKey(VIEW_PARAMS_ANIMATE_TODAY) && mHasToday) {
            synchronized (mAnimatorListener) {
                if (mTodayAnimator != null) {
                    mTodayAnimator.removeAllListeners();
                    mTodayAnimator.cancel();
                }
                mTodayAnimator = ObjectAnimator.ofInt(this, "animateTodayAlpha",
                        Math.max(mAnimateTodayAlpha, 80), 255);
                mTodayAnimator.setDuration(150);
                mAnimatorListener.setAnimator(mTodayAnimator);
                mAnimatorListener.setFadingIn(true);
                mTodayAnimator.addListener(mAnimatorListener);
                mAnimateToday = true;
                mTodayAnimator.start();
            }
        }
    }

    /**
     * @param tz
     */
    public boolean updateToday(String tz) {
        mToday.timezone = tz;
        mToday.setToNow();
        mToday.normalize(true);
        int julianToday = Time.getJulianDay(mToday.toMillis(false), mToday.gmtoff);
        if (julianToday >= mFirstJulianDay && julianToday < mFirstJulianDay + mNumDays) {
            mHasToday = true;
            mTodayIndex = julianToday - mFirstJulianDay;
        } else {
            mHasToday = false;
            mTodayIndex = -1;
        }
        return mHasToday;
    }

    public void setAnimateTodayAlpha(int alpha) {
        mAnimateTodayAlpha = alpha;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        drawWeekNums(canvas);
        drawDaySeparators(canvas);
        if (mHasToday && mAnimateToday) {
            drawToday(canvas);
        }
        if (mShowDetailsInMonth) {
            drawEvents(canvas);
            drawDiaries(canvas);
        } else {
            if (mDna == null && mUnsortedEvents != null) {
                createDna(mUnsortedEvents);
            }
            drawDNA(canvas);
        }
        drawClick(canvas);
    }

    protected void drawToday(Canvas canvas) {
        r.top = mDaySeparatorInnerWidth + (mTodayHighlightWidth / 2);
        r.bottom = mHeight - (int) Math.ceil(mTodayHighlightWidth / 2.0f);
        p.setStyle(Style.STROKE);
        p.setStrokeWidth(mTodayHighlightWidth);
        r.left = computeDayLeftPosition(mTodayIndex) + (mTodayHighlightWidth / 2);
        r.right = computeDayLeftPosition(mTodayIndex + 1)
                - (int) Math.ceil(mTodayHighlightWidth / 2.0f);
        p.setColor(mTodayAnimateColor | (mAnimateTodayAlpha << 24));
        canvas.drawRect(r, p);
        p.setStyle(Style.FILL);
    }

    // TODO move into SimpleWeekView
    // Computes the x position for the left side of the given day
    // 주어진 날의 왼쪽에 대한 x 좌표 계산
    private int computeDayLeftPosition(int day) {
        int effectiveWidth = mWidth;
        int x = 0;
        x = day * effectiveWidth / mNumDays;
        return x;
    }

    @Override
    protected void drawDaySeparators(Canvas canvas) {
        float lines[] = new float[8 * 4];
        int count = 6 * 4;
        int wkNumOffset = 0;
        int i = 0;

        count += 4;
        lines[i++] = 0;
        lines[i++] = 0;
        lines[i++] = mWidth;
        lines[i++] = 0;
        int y0 = 0;
        int y1 = mHeight;

        while (i < count) {
            int x = computeDayLeftPosition(i / 4 - wkNumOffset);
            lines[i++] = x;
            lines[i++] = y0;
            lines[i++] = x;
            lines[i++] = y1;
        }
        p.setColor(mDaySeparatorInnerColor);
        p.setStrokeWidth(mDaySeparatorInnerWidth);
        canvas.drawLines(lines, 0, count, p);
    }

    @Override
    protected void drawBackground(Canvas canvas) {
        int i = 0;
        int offset = 0;
        r.top = mDaySeparatorInnerWidth;
        r.bottom = mHeight;
        if (mShowWeekNum) {
            i++;
            offset++;
        }
        if (mFocusDay[i]) {
            while (++i < mOddMonth.length && mFocusDay[i])
                ;
            r.right = computeDayLeftPosition(i - offset);
            r.left = 0;
            p.setColor(mMonthBGFocusMonthColor);
            canvas.drawRect(r, p);
            // compute left edge for i, set up r, draw
            // i에 대한 왼쪽 모서리, r 설정, 그리기
        } else if (mFocusDay[(i = mFocusDay.length - 1)]) {
            while (--i >= offset && mFocusDay[i])
                ;
            i++;
            // compute left edge for i, set up r, draw
            // i에 대한 왼쪽 모서리, r 설정, 그리기
            r.right = mWidth;
            r.left = computeDayLeftPosition(i - offset);
            p.setColor(mMonthBGFocusMonthColor);
            canvas.drawRect(r, p);
        } else if (!mOddMonth[i]) {
            while (++i < mOddMonth.length && !mOddMonth[i])
                ;
            r.right = computeDayLeftPosition(i - offset);
            r.left = 0;
            p.setColor(mMonthBGOtherColor);
            canvas.drawRect(r, p);
            // compute left edge for i, set up r, draw
            // i에 대한 왼쪽 모서리, r 설정, 그리기
        } else if (!mOddMonth[(i = mOddMonth.length - 1)]) {
            while (--i >= offset && !mOddMonth[i])
                ;
            i++;
            // compute left edge for i, set up r, draw
            // i에 대한 왼쪽 모서리, r 설정, 그리기
            r.right = mWidth;
            r.left = computeDayLeftPosition(i - offset);
            p.setColor(mMonthBGOtherColor);
            canvas.drawRect(r, p);
        }
        if (mHasToday) {
            p.setColor(mMonthBGTodayColor);
            r.left = computeDayLeftPosition(mTodayIndex);
            r.right = computeDayLeftPosition(mTodayIndex + 1);
            canvas.drawRect(r, p);
        }
    }

    // Draw the "clicked" color on the tapped day
    // 탭된 날짜에 "클릭된" 색상 그리기
    private void drawClick(Canvas canvas) {
        if (mClickedDayIndex != -1) {
            int alpha = p.getAlpha();
            p.setColor(mClickedDayColor);
            p.setAlpha(mClickedAlpha);
            r.left = computeDayLeftPosition(mClickedDayIndex);
            r.right = computeDayLeftPosition(mClickedDayIndex + 1);
            r.top = mDaySeparatorInnerWidth;
            r.bottom = mHeight;
            canvas.drawRect(r, p);
            p.setAlpha(alpha);
        }
    }

    @Override
    protected void drawWeekNums(Canvas canvas) {
        int y;

        int i = 0;
        int offset = -1;
        int todayIndex = mTodayIndex;
        int x = 0;
        int numCount = mNumDays;
        if (mShowWeekNum) {
            x = mSidePaddingWeekNumber + mPadding;
            y = mWeekNumAscentHeight + mTopPaddingWeekNumber;
            canvas.drawText(mDayNumbers[0], x, y, mWeekNumPaint);
            numCount++;
            i++;
            todayIndex++;
            offset++;

        }

        y = mMonthNumAscentHeight + mTopPaddingMonthNumber;

        boolean isFocusMonth = mFocusDay[i];
        boolean isBold = false;
        mMonthNumPaint.setColor(isFocusMonth ? mMonthNumColor : mMonthNumOtherColor);

        // Get the julian monday used to show the lunar info.
        // lunar 정보를 보여 주는 데 사용되는 줄리안 데이를 가져오기
        int julianMonday = Utils.getJulianMondayFromWeeksSinceEpoch(mWeek);
        Time time = new Time(mTimeZone);
        time.setJulianDay(julianMonday);

        for (; i < numCount; i++) {
            if (mHasToday && todayIndex == i) {
                mMonthNumPaint.setColor(mMonthNumTodayColor);
                mMonthNumPaint.setFakeBoldText(isBold = true);
                if (i + 1 < numCount) {
                    // Make sure the color will be set back on the next
                    // iteration
                    // 다음 번 반복시 색상이 다시 설정되었는지 확인
                    isFocusMonth = !mFocusDay[i + 1];
                }
            } else if (mFocusDay[i] != isFocusMonth) {
                isFocusMonth = mFocusDay[i];
                mMonthNumPaint.setColor(isFocusMonth ? mMonthNumColor : mMonthNumOtherColor);
            }
            x = computeDayLeftPosition(i - offset) - (mSidePaddingMonthNumber);
            canvas.drawText(mDayNumbers[i], x, y, mMonthNumPaint);
            if (isBold) {
                mMonthNumPaint.setFakeBoldText(isBold = false);
            }

            if (LunarUtils.showLunar(getContext())) {
                // adjust the year and month
                // 년과 월을 조정함
                int year = time.year;
                int month = time.month;
                int julianMondayDay = time.monthDay;
                int monthDay = Integer.parseInt(mDayNumbers[i]);
                if (monthDay != julianMondayDay) {
                    int offsetDay = monthDay - julianMondayDay;
                    if (offsetDay > 0 && offsetDay > 6) {
                        month = month - 1;
                        if (month < 0) {
                            month = 11;
                            year = year - 1;
                        }
                    } else if (offsetDay < 0 && offsetDay < -6) {
                        month = month + 1;
                        if (month > 11) {
                            month = 0;
                            year = year + 1;
                        }
                    }
                }

                ArrayList<String> infos = new ArrayList<String>();
                LunarUtils.get(getContext(), year, month, monthDay,
                        LunarUtils.FORMAT_LUNAR_SHORT | LunarUtils.FORMAT_MULTI_FESTIVAL, false,
                        infos);
                if (infos.size() > 0) {
                    float originalTextSize = mMonthNumPaint.getTextSize();
                    mMonthNumPaint.setTextSize(mTextSizeLunar);
                    Resources res = getResources();
                    int mOrientation = res.getConfiguration().orientation;

                    int num = 0;
                    for (int index = 0; index < infos.size(); index++) {
                        String info = infos.get(index);
                        if (TextUtils.isEmpty(info)) continue;

                        int infoX = 0;
                        int infoY = 0;
                        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                            infoX = x - mMonthNumHeight - mTopPaddingMonthNumber;
                            infoY = y + (mMonthNumHeight + mLunarPaddingLunar) * num;
                        } else {
                            infoX = x;
                            infoY = y + (mMonthNumHeight + mLunarPaddingLunar) * (num + 1);
                        }
                        canvas.drawText(info, infoX, infoY, mMonthNumPaint);
                        num = num + 1;
                    }

                    // restore the text size.
                    // 텍스트 크기를 복원
                    mMonthNumPaint.setTextSize(originalTextSize);
                }
            }
        }
    }

    protected void drawEvents(Canvas canvas) {
        if (mEvents == null || mEvents.isEmpty()) {
            return;
        }

        DayBoxBoundaries boxBoundaries = new DayBoxBoundaries();
        WeekEventFormatter weekFormatter = new WeekEventFormatter(boxBoundaries);
        ArrayList<DayEventFormatter> dayFormatters = weekFormatter.prepareFormattedEvents();
        for (DayEventFormatter dayEventFormatter : dayFormatters) {
            dayEventFormatter.drawDay(canvas, boxBoundaries);
        }
    }

    protected void drawDiaries(Canvas canvas) {
        //Log.i(TAG, "drawDiaries 실행");
        if (mDiaries == null || mDiaries.isEmpty()) {
            Log.i(TAG, "drawDiaries, mDiaries == null");
            return;
        }

        DayBoxBoundaries boxBoundaries = new DayBoxBoundaries();
        WeekDiaryFormatter weekDiaryFormatter = new WeekDiaryFormatter(boxBoundaries);
        ArrayList<DayDiaryFormatter> dayDiaryFormatters = weekDiaryFormatter.prepareFormattedDiaries();
        Log.i(TAG, "drawDiaries, dayDiaryFormatters.size="+dayDiaryFormatters.size());
        for (DayDiaryFormatter dayDiaryFormatter : dayDiaryFormatters) {
            dayDiaryFormatter.drawDay(canvas, boxBoundaries);
        }
    }

    protected int addChipOutline(FloatRef lines, int count, int x, int y) {
        lines.ensureSize(count + 16);
        // top of box
        lines.array[count++] = x;
        lines.array[count++] = y;
        lines.array[count++] = x + mEventSquareWidth;
        lines.array[count++] = y;
        // right side of box
        lines.array[count++] = x + mEventSquareWidth;
        lines.array[count++] = y;
        lines.array[count++] = x + mEventSquareWidth;
        lines.array[count++] = y + mEventSquareWidth;
        // left side of box
        lines.array[count++] = x;
        lines.array[count++] = y;
        lines.array[count++] = x;
        lines.array[count++] = y + mEventSquareWidth + 1;
        // bottom of box
        lines.array[count++] = x;
        lines.array[count++] = y + mEventSquareWidth;
        lines.array[count++] = x + mEventSquareWidth + 1;
        lines.array[count++] = y + mEventSquareWidth;

        return count;
    }

    protected class DayEventSorter {
        private final EventFormat virtualFormat = new EventFormat(0, 0);
        private LinkedList<FormattedEventBase> mRemainingEvents;
        private BoundariesSetter mFixedHeightBoundaries;
        private FormattedEventBase mVirtualEvent;
        private int mListSize;
        private int mMinItems;

        public DayEventSorter(BoundariesSetter boundariesSetter) {
            mRemainingEvents = new LinkedList<>();
            mFixedHeightBoundaries = boundariesSetter;
            mVirtualEvent = new NullFormattedEvent(virtualFormat, boundariesSetter);
        }

        /**
         * Adds event to list of remaining events putting events spanning most days first.
         * 많은 날짜에 걸친 이벤트를 우선시하여 남아 있는 이벤트 리스트에 이벤트를 추가함?
         * @param remainingEvents
         * @param event
         */
        protected void sortedAddRemainingEventToList(LinkedList<FormattedEventBase> remainingEvents,
                                                     FormattedEventBase event) {
            int eventSpan = event.getFormat().getTotalSpan();
            if (eventSpan > 1) {
                ListIterator<FormattedEventBase> iterator = remainingEvents.listIterator();
                while (iterator.hasNext()) {
                    if (iterator.next().getFormat().getTotalSpan() < eventSpan) {
                        iterator.previous();
                        break;
                    }
                }
                iterator.add(event);
            } else {
                remainingEvents.add(event);
            }
        }

        protected void sortedAddRemainingDiaryToList(LinkedList<FormattedDiaryBase> remainingDiaries,
                                                     FormattedDiaryBase diary) {
            int diarySpan = diary.getFormat().getTotalSpan();
            if (diarySpan > 1) {
                ListIterator<FormattedDiaryBase> iterator = remainingDiaries.listIterator();
                while (iterator.hasNext()) {
                    if(iterator.next().getFormat().getTotalSpan() < diarySpan) {
                        iterator.previous();
                        break;
                    }
                }
                iterator.add(diary);
            } else {
                remainingDiaries.add(diary);
            }
        }

        /**
         * Checks what should be the size of array corresponding to lines of event in a given day
         * 특정 날짜의 이벤트 라인에 해당하는 배열 크기 확인
         * @param dayEvents
         */
        protected void init(ArrayList<FormattedEventBase> dayEvents) {
            mMinItems = -1;
            int eventsHeight = 0;
            for (FormattedEventBase event : dayEvents) {
                eventsHeight += event.getFormat().getEventLines();
                int yIndex = event.getFormat().getYIndex();
                mMinItems = Math.max(mMinItems, yIndex);
            }
            mListSize = Math.max(mMinItems + 1, eventsHeight);
            mRemainingEvents.clear();
        }

        /**
         * Returns index of next slot in FormattedEventBase Array.
         * FormattedEventBase 배열에서 다음 슬롯의 인덱스를 반환함
         * @param indexedEvents
         * @param index
         * @return index of next slot
         */
        protected int getNextIndex(FormattedEventBase[] indexedEvents, int index) {
            if (index < mMinItems) {
                return index + 1;
            }
            return index + indexedEvents[index].getFormat().getEventLines();
        }

        protected FormattedEventBase[] fillInIndexedEvents(ArrayList<FormattedEventBase> dayEvents) {
            FormattedEventBase[] indexedEvents = new FormattedEventBase[mListSize];
            for (FormattedEventBase event : dayEvents) {
                if (event.getFormat().getYIndex() != -1) {
                    indexedEvents[event.getFormat().getYIndex()] = event;
                } else {
                    sortedAddRemainingEventToList(mRemainingEvents, event);
                }
            }
            return indexedEvents;
        }

        protected ArrayList<FormattedEventBase> getSortedEvents(FormattedEventBase[] indexedEvents,
                                                                int expectedSize) {
            ArrayList<FormattedEventBase> sortedEvents = new ArrayList<>(expectedSize);
            for (FormattedEventBase event : indexedEvents) {
                if (event != null) {
                    sortedEvents.add(event);
                }
            }
            return sortedEvents;
        }

        protected void fillInRemainingEvents(FormattedEventBase[] indexedEvents) {
            int index = 0;
            for (FormattedEventBase event : mRemainingEvents) {
                if (!event.getFormat().isVisible()) {
                    continue;
                }
                while (index < indexedEvents.length) {
                    if (indexedEvents[index] == null) {
                        event.getFormat().setYIndex(index);
                        if (index < mMinItems) {
                            event.getFormat().capEventLinesAt(1);
                            if (!event.isBordered()) {
                                event.setBoundaries(mFixedHeightBoundaries);
                            }
                        }
                        indexedEvents[index] = event;
                        index = getNextIndex(indexedEvents, index);
                        break;
                    }
                    index = getNextIndex(indexedEvents, index);
                }
            }
            addVirtualEvents(indexedEvents, index);
        }

        protected void addVirtualEvents(FormattedEventBase[] indexedEvents, int initialIndex)  {
            for (int index = initialIndex; index < mMinItems; index++) {
                if (indexedEvents[index] == null) {
                    indexedEvents[index] = mVirtualEvent;
                }
            }
        }

        public ArrayList<FormattedEventBase> sort(ArrayList<FormattedEventBase> dayEvents) {
            if (dayEvents.isEmpty()) {
                return new ArrayList<>();
            }
            init(dayEvents);
            FormattedEventBase[] indexedEvents = fillInIndexedEvents(dayEvents);
            fillInRemainingEvents(indexedEvents);
            return getSortedEvents(indexedEvents, dayEvents.size());
        }
    }

    protected class WeekEventFormatter {
        private List<ArrayList<FormattedEventBase>> mFormattedEvents;
        private DayBoxBoundaries mBoxBoundaries;
        private BoundariesSetter mFullDayBoundaries;
        private BoundariesSetter mRegularBoundaries;

        public WeekEventFormatter(DayBoxBoundaries boxBoundaries) {
            mBoxBoundaries = boxBoundaries;
            mFullDayBoundaries = new AllDayBoundariesSetter(boxBoundaries);
            mRegularBoundaries = new RegularBoundariesSetter(boxBoundaries);
        }

        /**
         * Prepares events to be drawn. It creates FormattedEvents from mEvent.
         * 그려야 할 이벤트를 준비함
         * mEvent에서 FormattedEvent를 생성함
         * @return ArrayList of DayEventFormatters
         */
        public ArrayList<DayEventFormatter> prepareFormattedEvents() {
            prepareFormattedEventsWithEventDaySpan();
            ViewDetailsPreferences.Preferences preferences =
                    ViewDetailsPreferences.getPreferences(getContext());
            preFormatEventText(preferences);
            setYindexInEvents();
            return formatDays(mBoxBoundaries.getAvailableYSpace(), preferences);
        }

        /**
         * Handles text formatting in events - sets number of lines in in each event.
         * In order to produce right values DaySpan needs to be set first (in EventFormat)
         * 이벤트에서 텍스트 형식 처리 - 각 이벤트의 라인수를 설정함
         * 올바른 값을 생성하려면 DaySpan을 먼저 설정해야 함(EventFormat에서)
         */
        protected void preFormatEventText(ViewDetailsPreferences.Preferences preferences) {
            for (ArrayList<FormattedEventBase> dayEvents : mFormattedEvents) {
                for (FormattedEventBase event : dayEvents) {
                    event.initialPreFormatText(preferences);
                }
            }
        }

        /**
         * Creates DayEventFormatters for each day and formats each day to prepare it for drawing.
         * 각 요일에 대한 DayEventFormatter를 만들고, draw하기 위해 각 날짜를 포맷함
         * @param availableSpace
         * @return
         */
        protected ArrayList<DayEventFormatter> formatDays(int availableSpace, ViewDetailsPreferences.Preferences preferences) {
            int dayIndex = 0;
            ArrayList<DayEventFormatter> dayFormatters = new ArrayList<>(mFormattedEvents.size());
            for (ArrayList<FormattedEventBase> dayEvents : mFormattedEvents) {
                DayEventFormatter dayEventFormatter = new DayEventFormatter(dayEvents, dayIndex, preferences);
                dayEventFormatter.formatDay(availableSpace);
                dayFormatters.add(dayEventFormatter);
                dayIndex++;
            }
            return dayFormatters;
        }

        /**
         * Sets y-index in events (and sorts the list according to it). Events spanning multiple
         * days are put first (starting with the longest ones). Event y-index is maintained (does
         * not change) in subsequent days. If free slots appear events will be put there first.
         * Order of events starting and finishing the same day is preserved.
         * 이벤트의 y-index 설정(그리고 그에 따른 리스트 정렬)
         * 여러 날에 걸친 이벤트가 우선임(가장 긴 이벤트부터 시작)
         * 이벤트 y-index는 그 다음 날까지 유지(바뀌지 않음)됨
         * 빈 슬롯이 나타나면 이벤트가 먼저 거기에 놓일 것임
         * 같은 날 시작과 종료되는 이벤트의 순서는 보존됨
         */
        protected void setYindexInEvents() {
            ArrayList<ArrayList<FormattedEventBase>> newFormattedEvents = new ArrayList<>(mFormattedEvents.size());
            DayEventSorter sorter = new DayEventSorter(
                    new FixedHeightRegularBoundariesSetter(mBoxBoundaries));
            for (ArrayList<FormattedEventBase> dayEvents : mFormattedEvents) {
                newFormattedEvents.add(sorter.sort(dayEvents));
            }
            mFormattedEvents = newFormattedEvents;
        }

        protected BoundariesSetter getBoundariesSetter(Event event) {
            if (event.drawAsAllday()) {
                return mFullDayBoundaries;
            }
            return mRegularBoundaries;
        }



        protected FormattedEventBase makeFormattedEvent(Event event, EventFormat format) {
            return new FormattedEvent(event, format, getBoundariesSetter(event));
        }

        // day is provided as an optimisation to look only on a certain day
        // 날은 특정한 날만 보는 최적화를 통해 제공됨
        protected EventFormat getFormatByEvent(Event event, int day) {
            if (day < 0 || mFormattedEvents.size() <= day) {
                return null;
            }
            for (FormattedEventBase formattedEvent : mFormattedEvents.get(day)) {
                if (formattedEvent.containsEvent(event))
                {
                    return formattedEvent.getFormat();
                }
            }
            return null;
        }

        protected ArrayList<FormattedEventBase> prepareFormattedEventDay(ArrayList<Event> dayEvents,
                                                                         int day,
                                                                         int daysInWeek) {
            final int eventCount = (dayEvents == null) ? 0 : dayEvents.size();
            ArrayList<FormattedEventBase> formattedDayEvents = new ArrayList<>(eventCount);
            if (eventCount == 0) {
                return formattedDayEvents;
            }
            for (Event event : dayEvents) {
                if (event == null) {
                    EventFormat format = new EventFormat(day, daysInWeek);
                    format.hide(day);
                    formattedDayEvents.add(new NullFormattedEvent(format, mFullDayBoundaries));
                    continue;
                }
                EventFormat lastFormat = getFormatByEvent(event, day -1);
                if ((lastFormat != null) && (event.drawAsAllday())) {
                    lastFormat.extendDaySpan(day);
                    formattedDayEvents.add(makeFormattedEvent(event, lastFormat));
                }
                else if (lastFormat == null) {
                    EventFormat format = new EventFormat(day, daysInWeek);
                    formattedDayEvents.add(makeFormattedEvent(event, format));
                }
            }
            return formattedDayEvents;
        }

        /**
         * Fills mFormattedEvents with FormattedEvents created based on Events in mEvents. While
         * creating ArrayList of ArrayLists of FormattedEvents, DaySpan of each FormattedEvent is
         * set.
         * mFormattedEvents를 mEvents의 이벤트를 기반으로 생성된 FormattedEvents로 채움
         * FormattedEvents ArrayLists의 ArrayList를 만드는 동안, 각 FormattedEvent의 DaySpan을 설정함
         */
        protected void prepareFormattedEventsWithEventDaySpan() {
            mFormattedEvents = new ArrayList<>(mEvents.size());
            if (mEvents == null || mEvents.isEmpty()) {
                return;
            }
            int day = 0;
            final int daysInWeek = mEvents.size();
            for (ArrayList<Event> dayEvents : mEvents) {
                mFormattedEvents.add(prepareFormattedEventDay(dayEvents, day, daysInWeek));
                day++;
            }
        }
    }

    protected class WeekDiaryFormatter {
        private List<ArrayList<FormattedDiaryBase>> mFormattedDiaries;
        private DayBoxBoundaries mBoxBoundaries;
        private BoundariesSetter mDiaryBoundaries;

        public WeekDiaryFormatter(DayBoxBoundaries boxBoundaries) {
            mBoxBoundaries = boxBoundaries;
            mDiaryBoundaries = new DiaryBoundariesSetter(boxBoundaries);
        }

        public ArrayList<DayDiaryFormatter> prepareFormattedDiaries() {
            //Log.i(TAG, "prepareFormattedDiaries 실행");
            prepareFormattedDiariesWithDiaryDaySpan();
            ViewDetailsPreferences.Preferences preferences =
                    ViewDetailsPreferences.getPreferences(getContext());
            setXindexInDiaries();
            return formatDays(mBoxBoundaries.getAvailableXSpace(), preferences);
        }

        protected ArrayList<DayDiaryFormatter> formatDays(int availableSpace, ViewDetailsPreferences.Preferences preferences) {
            //Log.i(TAG, "formatDays 실행");
            int dayIndex = 0;
            ArrayList<DayDiaryFormatter> dayDiaryFormatters = new ArrayList<>(mFormattedDiaries.size());
            // mFormattedDiaries.size() : 7
            for(ArrayList<FormattedDiaryBase> dayDiaries : mFormattedDiaries) {
                DayDiaryFormatter dayDiaryFormatter = new DayDiaryFormatter(dayDiaries, dayIndex, preferences);
                dayDiaryFormatter.formatDay(availableSpace);
                dayDiaryFormatters.add(dayDiaryFormatter);
                dayIndex++;
            }
            return dayDiaryFormatters;
        }

        protected void setXindexInDiaries() {
            ArrayList<ArrayList<FormattedDiaryBase>> newFormattedDiaries = new ArrayList<>(mFormattedDiaries.size());
            DayDiarySorter sorter = new DayDiarySorter(
                    new FixedWidthRegularBoundariesSetter(mBoxBoundaries));
            for (ArrayList<FormattedDiaryBase> dayDiaries : mFormattedDiaries) {
                newFormattedDiaries.add(sorter.sort(dayDiaries));
            }
            mFormattedDiaries = newFormattedDiaries;
        }

        protected BoundariesSetter getBoundariesSetter(Diary diary) {
            return mDiaryBoundaries;
        }

        protected FormattedDiaryBase makeFormattedDiary(Diary diary, DiaryFormat format) {
            Log.i(TAG, "makeFormattedDiary 실행");
            return new FormattedDiary(diary, format, getBoundariesSetter(diary));
        }

        protected DiaryFormat getFormatByDiary(Diary diary, int day) {
            if(day < 0 || mFormattedDiaries.size() <= day) {
                return null;
            }
            for(FormattedDiaryBase formattedDiary : mFormattedDiaries.get(day)) {
                if(formattedDiary.containsDiary(diary)) {
                    return formattedDiary.getFormat();
                }
            }
            return null;
        }

        protected ArrayList<FormattedDiaryBase> prepareFormattedDiaryDay(ArrayList<Diary> dayDiaries,
                                                                         int day,
                                                                         int daysInWeek) {
            //Log.i(TAG, "prepareFormattedDiaryDay 실행, day="+day);
            final int diaryCount = (dayDiaries == null) ? 0 : dayDiaries.size();
            ArrayList<FormattedDiaryBase> formattedDayDiaries = new ArrayList<>(diaryCount);
            if(diaryCount == 0) {
                //Log.i(TAG, "prepareFormattedDiaryDay, diary 없음");
                return formattedDayDiaries;
            }
            //Log.i(TAG, "prepareFormattedDiaryDay, diary 있음");
            for(Diary diary : dayDiaries) {
                if (diary == null) {
                    //Log.i(TAG, "prepareFormattedDiaryDay, diary == null");
                    DiaryFormat format = new DiaryFormat(day, daysInWeek);
                    format.hide(day);
                    formattedDayDiaries.add(new NullFormattedDiary(format, mDiaryBoundaries));
                    continue;
                }
                DiaryFormat lastFormat = getFormatByDiary(diary, day-1);
                if ((lastFormat != null)) {
                    lastFormat.extendDaySpan(day);
                    formattedDayDiaries.add(makeFormattedDiary(diary,lastFormat));
                } else if(lastFormat == null) {
                    DiaryFormat format = new DiaryFormat(day, daysInWeek);
                    formattedDayDiaries.add(makeFormattedDiary(diary, format));
                }
            }
            return formattedDayDiaries;
        }

        protected void prepareFormattedDiariesWithDiaryDaySpan() {
            mFormattedDiaries = new ArrayList<>(mDiaries.size());
            if (mDiaries == null || mDiaries.isEmpty()) {
                Log.i(TAG, "prepareFormattedDiariesWithDiaryDaySpan, mDiaries == null");
                return;
            }
            //Log.i(TAG, "prepareFormattedDiariesWithDiaryDaySpan, mDiaries.size="+mDiaries.size());
            // mDiaries.size() : 7
            int day = 0;
            final int daysInWeek = mDiaries.size();
            for (ArrayList<Diary> dayDiaries : mDiaries) {
                //Log.i(TAG, "prepareFormattedDiariesWithDiaryDaySpan, dayDiaries 추가");
                if(!dayDiaries.isEmpty()) Log.i(TAG, "dayDiaries="+dayDiaries.get(0).content);
                //else Log.i(TAG, "dayDiaries == null");
                mFormattedDiaries.add(prepareFormattedDiaryDay(dayDiaries, day, daysInWeek));
                day++;
            }
        }
    }

    /**
     * Takes care of laying events out vertically.
     * 이벤트를 수직으로 배치하는 것을 주의함
     * Vertical layout: 수직 레이아웃:
     *   (top of box) 상자 상단
     * a. mEventYOffsetLandscape or portrait equivalent
     * b. Event title: mEventHeight for a normal event, + 2xBORDER_SPACE for all-day event
     *    이벤트 제목: 일반 이벤트 mEventHeight, 종일 이벤트 + 2xBORDER_SPACE
     * c. [optional] Time range (mExtrasHeight)
     *    [옵션] 시간 범위(mExtrasHeight)
     * d. mEventLinePadding
     *
     * Repeat (b,c,d) as needed and space allows.
     * 필요에 따라 b,c,d를 반복하고 공간을 허용함
     * If we have more events than fit, we need to leave room for something like "+2" at the bottom:
     * 적합치보다 더 많은 이벤트를 가지고 있는 경우, 아래쪽에 "+2" 같은 걸 표시할 공간을 남겨 둬야 함
     *
     * e. "+ more" line (mExtrasHeight)
     * f. mEventBottomPadding (overlaps mEventLinePadding)
     *   (bottom of box) 상자 아래
     */
    protected class DayEventFormatter {
        private ArrayList<FormattedEventBase> mEventDay;
        private int mDay;
        private ViewDetailsPreferences.Preferences mViewPreferences;
        //members initialized by the init function:
        // 초기 기능에 의해 초기화된 멤버 변수들:
        private int mFullDayEventsCount;
        private ArrayList<ArrayList<FormattedEventBase>> mEventsByHeight;
        private int mMaxNumberOfLines;
        private int mVisibleEvents;

        public DayEventFormatter(ArrayList<FormattedEventBase> eventDay,
                                 int day,
                                 ViewDetailsPreferences.Preferences viewPreferences) {
            mEventDay = eventDay;
            mDay = day;
            mViewPreferences = viewPreferences;
            init();
        }

        /**
         * Initializes members storing information about events in mEventDay
         * mEventDay에 이벤트 정보를 저장하는 멤버 변수 초기화
         */
        protected void init() {
            mMaxNumberOfLines = mMaxLinesInEvent;
            mEventsByHeight = new ArrayList<>(mMaxLinesInEvent + 1);
            for (int i = 0; i < mMaxLinesInEvent + 1; i++) {
                mEventsByHeight.add(new ArrayList<FormattedEventBase>());
            }
            ListIterator<FormattedEventBase> iterator = mEventDay.listIterator();
            while (iterator.hasNext()) {
                FormattedEventBase event = iterator.next();
                final int eventHeight = event.getFormat().getEventLines();
                if (eventHeight > 0) {
                    mVisibleEvents++;
                    if (event.isBordered()) {
                        mFullDayEventsCount++;
                    }
                }
                mEventsByHeight.get(eventHeight).add(event);
            }
        }

        /**
         * Checks if event should be skipped (in case if it was already drawn)
         * 이벤트를 건너뛰어야 하는지 확인(이미 그려진 경우)
         * @param event
         * @return True if event should be skipped
         *          건너뛰어야 하면 true 반환
         */
        protected boolean eventShouldBeSkipped(FormattedEventBase event) {
            return event.getFormat().getDaySpan(mDay) <= 0;
        }

        /**
         * Draws all events in a given day and more events indicator if needed.
         * As a result of this call boxBoundaries will be set to next day.
         * 특정 날짜의 모든 이벤트와 필요한 경우 더 많은 이벤트 표시기를 그림
         * 이 호출의 결과로 boxBoundaries(박스 경계선)는 다음 날로 설정될 것임
         * @param canvas
         * @param boxBoundaries
         */
        public void drawDay(Canvas canvas, DayBoxBoundaries boxBoundaries) {
            int x = mDiaryPadding;
            int y = 0;
            for (FormattedEventBase event : mEventDay) {
                if (eventShouldBeSkipped(event)) {
                    event.skip(mViewPreferences);
                } else {
                    event.draw(canvas, mViewPreferences, mDay);
                }
                x += boxBoundaries.mXWidth;
            }
            if (moreLinesWillBeDisplayed()) {
                int hiddenEvents = mEventsByHeight.get(0).size();
                drawMoreEvents(canvas, hiddenEvents, boxBoundaries.getX());
            }

            boxBoundaries.nextDay();
        }

        /**
         * Disables showing of time in a day handled by this class in case if it doesn't fit
         * availableSpace
         * availdableSpace에 맞지 않을 경우, 이 클래스가 처리하는 하루 동안의 시간 표시 비활성화
         * @param availableSpace
         */
        protected void hideTimeRangeIfNeeded(int availableSpace) {
            if (mViewPreferences.isTimeShownBelow()
                    && (getMaxNumberOfLines(availableSpace) < mVisibleEvents)) {
                mViewPreferences = mViewPreferences.hideTime();
            }
        }

        /**
         * Reduces the number of available lines by one (all events spanning more lines than current
         * limit will be capped)
         * 사용 가능한 라인 수를 한 개 감소함(현재 한계보다 많은 라인을 포함하는 모든 이벤트가 cap...될 것임)
         */
        protected void reduceNumberOfLines() {
            if (mMaxNumberOfLines > 0) {
                final int index = mMaxNumberOfLines;
                mMaxNumberOfLines--;
                for (FormattedEventBase event : mEventsByHeight.get(index)) {
                    event.getFormat().capEventLinesAt(mMaxNumberOfLines);
                }
                mEventsByHeight.get(index - 1).addAll(mEventsByHeight.get(index));
                mEventsByHeight.get(index).clear();
            }
        }

        /**
         * Reduces height of last numberOfEventsToReduce events with highest possible height by one
         * 가능한 가장 높은 높이로 마지막 numberOfEventsToReduce 이벤트으 높이를 1 감소함
         * @param numberOfEventsToReduce
         */
        protected void reduceHeightOfEvents(int numberOfEventsToReduce) {
            final int nonReducedEvents = getNumberOfHighestEvents() - numberOfEventsToReduce;
            ListIterator<FormattedEventBase> iterator =
                    mEventsByHeight.get(mMaxNumberOfLines).listIterator(nonReducedEvents);
            final int cap = mMaxNumberOfLines - 1;
            while (iterator.hasNext()) {
                FormattedEventBase event = iterator.next();
                event.getFormat().capEventLinesAt(cap);
                mEventsByHeight.get(cap).add(event);
                iterator.remove();
            }
        }

        /**
         * Returns number of events with highest allowed height
         * 허용된 높이가 가장 높은 이벤트 수 반환
         * @return
         */
        protected int getNumberOfHighestEvents() {
            return mEventsByHeight.get(mMaxNumberOfLines).size();
        }

        protected int getMaxNumberOfLines(int availableSpace) {
            final int textSpace = availableSpace - getOverheadHeight() - getHeightOfTimeRanges();
            return textSpace / mEventHeight;
        }

        /**
         * Reduces height of events in order to allow all of them to fit the screen
         * 모든 이벤트가 화면에 맞도록 이벤트 높이 감소
         * @param availableSpace
         */
        protected void fitAllItemsOnScrean(int availableSpace) {
            final int maxNumberOfLines = getMaxNumberOfLines(availableSpace);
            int numberOfLines = getTotalEventLines();
            while (maxNumberOfLines < numberOfLines - getNumberOfHighestEvents()) {
                numberOfLines -= getNumberOfHighestEvents();
                reduceNumberOfLines();
            }
            final int linesToCut = numberOfLines - maxNumberOfLines;
            reduceHeightOfEvents(linesToCut);
        }

        /**
         * Reduces height of events to one line - which is the minimum
         * 이벤트 높이를 최소 한 선으로 감소함
         */
        protected void reduceHeightOfEventsToOne() {
            final int cap = 1;
            for (int i = 2; i <= mMaxNumberOfLines; i++) {
                for (FormattedEventBase event : mEventsByHeight.get(i)) {
                    event.getFormat().capEventLinesAt(cap);
                }
                mEventsByHeight.get(cap).addAll(mEventsByHeight.get(i));
                mEventsByHeight.get(i).clear();
            }
            mMaxNumberOfLines = cap;
        }

        /**
         * After reducing height of events to minimum, reduces their count in order to fit most of
         * the events in availableSpace (and let enough space to display "more events" indication)
         * 이벤트 높이를 최소로 줄인 후, availableSpace에서 대부분의 이벤트에 맞도록 카운터를 줄임
         * (그리고 "more events" 표시를 보여 줄 수 있는 충분한 공간을 확보함)
         * @param availableSpace
         */
        protected void reduceNumberOfEventsToFit(int availableSpace) {
            reduceHeightOfEventsToOne();
            int height = getEventsHeight();
            if (!moreLinesWillBeDisplayed())  {
                height += mExtrasHeight;
            }
            ListIterator<FormattedEventBase> backIterator = mEventDay.listIterator(mEventDay.size());
            while ((height > availableSpace) && backIterator.hasPrevious()) {
                FormattedEventBase event = backIterator.previous();
                if (event == null || event.getFormat().getEventLines() == 0) {
                    continue;
                }
                height -= event.getHeight(mViewPreferences);
                event.getFormat().hide(mDay);
                mVisibleEvents--;
                mEventsByHeight.get(0).add(event);
                mEventsByHeight.remove(event);
            }
        }

        /**
         * Formats day according to the layout given at class description
         * 클래스 설명에서 주어진 레이아웃에 따라 날짜를 포맷함
         * @param availableSpace
         */
        public void formatDay(int availableSpace) {
            hideTimeRangeIfNeeded(availableSpace);
            if (getEventsHeight() > availableSpace) {
                if (willAllItemsFitOnScreen(availableSpace)) {
                    fitAllItemsOnScrean(availableSpace);
                } else {
                    reduceNumberOfEventsToFit(availableSpace);
                }
            }
        }

        /**
         * Checks if all events can fit the screen (assumes that in the worst case they need to be
         * capped at one line per event)
         * 모든 이벤트가 화면에 맞을 수 있는지 확인 (최악의 경우, 이벤트당 한 줄로 연결해야 한다고 가정)
         * @param availableSpace
         * @return
         */
        protected boolean willAllItemsFitOnScreen(int availableSpace) {
            return (getOverheadHeight() + mVisibleEvents * mEventHeight <= availableSpace);
        }

        /**
         * Checks how many lines all events would take
         * 모든 이벤트에 소요되는 라인 수 확인
         * @return
         */
        protected int getTotalEventLines() {
            int lines = 0;
            for (int i = 1; i < mEventsByHeight.size(); i++) {
                lines += i * mEventsByHeight.get(i).size();
            }
            return lines;
        }

        protected boolean moreLinesWillBeDisplayed() {
            return mEventsByHeight.get(0).size() > 0;
        }

        protected int getHeightOfMoreLine() {
            return moreLinesWillBeDisplayed() ? mExtrasHeight : 0;
        }

        /**
         * Returns the amount of space required to fit all spacings between events
         * 이벤트 간의 모든 간격에 맞도록 필요한 공간의 양을 반환함
         * @return
         */
        protected int getOverheadHeight() {
            return getHeightOfMoreLine() + mFullDayEventsCount * mBorderSpace * 2
                    + (mVisibleEvents - 1) * mEventLinePadding;
        }

        protected int getHeightOfTimeRanges() {
            return mViewPreferences.isTimeShownBelow() ?
                    mExtrasHeight  * (mVisibleEvents - mFullDayEventsCount) : 0;
        }

        /**
         * Returns Current height required to fit all events
         * 모든 이벤트에 맞는 데 필요한 현재 높이 반환
         * @return
         */
        protected int getEventsHeight() {
            return getOverheadHeight()
                    + getTotalEventLines() * mEventHeight
                    + getHeightOfTimeRanges();
        }
    }

    protected class DayDiaryFormatter {
        private ArrayList<FormattedDiaryBase> mDiaryDay;
        private int mDay;
        private ViewDetailsPreferences.Preferences mViewPreferences;
        private ArrayList<ArrayList<FormattedDiaryBase>> mDiariesByWidth;
        private int mMaxNumberOfLines;
        private int mVisibleDiaries;

        public DayDiaryFormatter(ArrayList<FormattedDiaryBase> diaryDay,
                                 int day,
                                 ViewDetailsPreferences.Preferences viewPreferences) {
            mDiaryDay = diaryDay;
            //Log.i(TAG, "DayDiaryFormatter, mDiaryDay.size="+mDiaryDay.size());
            mDay = day;
            mViewPreferences = viewPreferences;
            init();
        }

        protected void init() {
            //Log.i(TAG, "init 실행");
            mMaxNumberOfLines = mMaxLinesInDiary;
            mDiariesByWidth = new ArrayList<>(mMaxLinesInDiary + 1);
            for (int i = 0; i < mMaxNumberOfLines + 1; i++) {
                mDiariesByWidth.add(new ArrayList<FormattedDiaryBase>());
            }
            //Log.i(TAG, "init, mMaxNumberOfLines="+mMaxNumberOfLines+", mDiariesByWidth.size="+mDiariesByWidth.size());
            ListIterator<FormattedDiaryBase> iterator = mDiaryDay.listIterator();
            while(iterator.hasNext()) {
                FormattedDiaryBase diary = iterator.next();
                final int diaryWidth = diary.getFormat().getDiaryLines();
                if (diaryWidth > 0) {
                    mVisibleDiaries++;
                }
                mDiariesByWidth.get(diaryWidth).add(diary);
            }
        }

        protected boolean diaryShouldBeSkipped(FormattedDiaryBase diary) {
            return diary.getFormat().getDaySpan(mDay) <= 0;
        }

        public void drawDay(Canvas canvas, DayBoxBoundaries boxBoundaries) {
            //Log.i(TAG, "drawDay 실행, mDiaryDay.size="+mDiaryDay.size());
            for (FormattedDiaryBase diary : mDiaryDay) {
                if (diaryShouldBeSkipped(diary)) {
                    //Log.i(TAG, "drawDay, 스킵");
                    diary.skip(mViewPreferences);
                } else {
                    //Log.i(TAG, "drawDay, 그림");
                    diary.draw(canvas, mViewPreferences, mDay);
                }
            }
            if(moreLinesWillBeDisplayed()) {
                int hiddenDiaries = mDiariesByWidth.get(0).size();
//                drawMoreDiaries(canvas, hiddenDiaries, boxBoundaries.getX());
            }
            boxBoundaries.nextDay();
        }

        protected void hideTimeRangeIfNeeded(int availableSpace) {
            if(mViewPreferences.isTimeShownBelow() &&
                    (getMaxNumberOfLines(availableSpace) < mVisibleDiaries)) {
                mViewPreferences = mViewPreferences.hideTime();
            }
        }

        protected void reduceNumberOfLines() {
            if(mMaxNumberOfLines > 0) {
                final int index = mMaxNumberOfLines;
                mMaxNumberOfLines--;
                for (FormattedDiaryBase diary : mDiariesByWidth.get(index)) {
                    diary.getFormat().capDiaryLinesAt(mMaxNumberOfLines);
                }
                mDiariesByWidth.get(index - 1).addAll(mDiariesByWidth.get(index));
                mDiariesByWidth.get(index).clear();
            }
        }

        protected void reduceWidthOfDiaries(int numberOfDiariesToReduce) {
            final int nonReducedDiaries = getNumberOfHighestDiaries() - numberOfDiariesToReduce;
            ListIterator<FormattedDiaryBase> iterator =
                    mDiariesByWidth.get(mMaxNumberOfLines).listIterator(nonReducedDiaries);
            final int cap = mMaxNumberOfLines - 1;
            while (iterator.hasNext()) {
                FormattedDiaryBase event = iterator.next();
                event.getFormat().capDiaryLinesAt(cap);
                mDiariesByWidth.get(cap).add(event);
                iterator.remove();
            }
        }

        protected int getNumberOfHighestDiaries() {
            return mDiariesByWidth.get(mMaxNumberOfLines).size();
        }

        protected int getMaxNumberOfLines(int availableSpace) {
            final int textSpace = availableSpace - getOverheadHeight();
            return textSpace / mEventHeight;
        }

        protected void fitAllItemsOnScrean(int availableSpace) {
            final int maxNumberOfLines = getMaxNumberOfLines(availableSpace);
            int numberOfLines = getTotalDiaryLines();
            while (maxNumberOfLines < numberOfLines - getNumberOfHighestDiaries()) {
                numberOfLines -= getNumberOfHighestDiaries();
                reduceNumberOfLines();
            }
            final int linesToCut = numberOfLines - maxNumberOfLines;
            reduceWidthOfDiaries(linesToCut);
        }

        protected void reduceHeightOfDiariesToOne() {
            final int cap = 1;
            for (int i = 2; i <= mMaxNumberOfLines; i++) {
                for (FormattedDiaryBase event : mDiariesByWidth.get(i)) {
                    event.getFormat().capDiaryLinesAt(cap);
                }
                mDiariesByWidth.get(cap).addAll(mDiariesByWidth.get(i));
                mDiariesByWidth.get(i).clear();
            }
            mMaxNumberOfLines = cap;
        }

        protected void reduceNumberOfEventsToFit(int availableSpace) {
            reduceHeightOfDiariesToOne();
            int height = getDiariesHeight();
            if (!moreLinesWillBeDisplayed())  {
                height += mExtrasHeight;
            }
            ListIterator<FormattedDiaryBase> backIterator = mDiaryDay.listIterator(mDiaryDay.size());
            while ((height > availableSpace) && backIterator.hasPrevious()) {
                FormattedDiaryBase event = backIterator.previous();
                if (event == null || event.getFormat().getDiaryLines() == 0) {
                    continue;
                }
                height -= event.getHeight(mViewPreferences);
                event.getFormat().hide(mDay);
                mVisibleDiaries--;
                mDiariesByWidth.get(0).add(event);
                mDiariesByWidth.remove(event);
            }
        }

        /**
         * Formats day according to the layout given at class description
         * 클래스 설명에서 주어진 레이아웃에 따라 날짜를 포맷함
         * @param availableSpace
         */
        public void formatDay(int availableSpace) {
            hideTimeRangeIfNeeded(availableSpace);
            if (getDiariesHeight() > availableSpace) {
                if (willAllItemsFitOnScreen(availableSpace)) {
                    fitAllItemsOnScrean(availableSpace);
                } else {
                    reduceNumberOfEventsToFit(availableSpace);
                }
            }
        }

        /**
         * Checks if all events can fit the screen (assumes that in the worst case they need to be
         * capped at one line per event)
         * 모든 이벤트가 화면에 맞을 수 있는지 확인 (회악의 경우, 이벤트당 한 줄로 연결해야 한다고 가정)
         * @param availableSpace
         * @return
         */
        protected boolean willAllItemsFitOnScreen(int availableSpace) {
            return (getOverheadHeight() + mVisibleDiaries * mEventHeight <= availableSpace);
        }

        /**
         * Checks how many lines all events would take
         * 모든 이벤트에 소요되는 라인 수 확인
         * @return
         */
        protected int getTotalDiaryLines() {
            int lines = 0;
            for (int i = 1; i < mDiariesByWidth.size(); i++) {
                lines += i * mDiariesByWidth.get(i).size();
            }
            return lines;
        }

        protected boolean moreLinesWillBeDisplayed() {
            return mDiariesByWidth.get(0).size() > 0;
        }

        protected int getHeightOfMoreLine() {
            return moreLinesWillBeDisplayed() ? mExtrasHeight : 0;
        }

        /**
         * Returns the amount of space required to fit all spacings between events
         * 이벤트 간의 모든 간격에 맞도록 필요한 공간의 양을 반환함
         * @return
         */
        protected int getOverheadHeight() {
            return getHeightOfMoreLine() + (mVisibleDiaries - 1) * mEventLinePadding;
        }

//        protected int getHeightOfTimeRanges() {
//            return mViewPreferences.isTimeShownBelow() ?
//                    mExtrasHeight  * (mVisibleDiaries - mFullDayEventsCount) : 0;
//        }

        /**
         * Returns Current height required to fit all events
         * 모든 이벤트에 맞는 데 필요한 현재 높이 반환
         * @return
         */
        protected int getDiariesHeight() {
            return getOverheadHeight()
                    + getTotalDiaryLines() * mEventHeight;
        }
    }

    protected class DayDiarySorter {
        private final DiaryFormat virtualFormat = new DiaryFormat(0, 0);
        private LinkedList<FormattedDiaryBase> mRemainingDiaries;
        private BoundariesSetter mFixedWidthBoundaries;
        private FormattedDiaryBase mVirtualDiary;
        private int mListSize;
        private int mMinItems;

        public DayDiarySorter(BoundariesSetter boundariesSetter) {
            mRemainingDiaries = new LinkedList<>();
            mFixedWidthBoundaries = boundariesSetter;
            mVirtualDiary = new NullFormattedDiary(virtualFormat, boundariesSetter);
        }

        protected void sortedAddRemainingDiaryToList(LinkedList<FormattedDiaryBase> remainingDiaries,
                                                     FormattedDiaryBase diary) {
            int diarySpan = diary.getFormat().getTotalSpan();
            if (diarySpan > 1) {
                ListIterator<FormattedDiaryBase> iterator = remainingDiaries.listIterator();
                while (iterator.hasNext()) {
                    if(iterator.next().getFormat().getTotalSpan() < diarySpan) {
                        iterator.previous();
                        break;
                    }
                }
                iterator.add(diary);
            } else {
                remainingDiaries.add(diary);
            }
        }

        protected void init(ArrayList<FormattedDiaryBase> dayDiaries) {
            mMinItems = -1;
            int diariesWidth = 0;
            for (FormattedDiaryBase diary : dayDiaries) {
                diariesWidth += diary.getFormat().getDiaryLines();
                int xIndex = diary.getFormat().getXIndex();
                mMinItems = Math.max(mMinItems, xIndex);
            }
            mListSize = Math.max(mMinItems + 1, diariesWidth);
            mRemainingDiaries.clear();
        }

        protected int getNextIndex(FormattedDiaryBase[] indexedEvents, int index) {
            if (index < mMinItems) {
                return index + 1;
            }
            return index + indexedEvents[index].getFormat().getDiaryLines();
        }

        protected  FormattedDiaryBase[] fillInIndexedDiaries(ArrayList<FormattedDiaryBase> dayDiaries) {
            FormattedDiaryBase[] indexedDiaries = new FormattedDiaryBase[mListSize];
            for(FormattedDiaryBase diary : dayDiaries) {
                if(diary.getFormat().getXIndex() != -1) {
                    indexedDiaries[diary.getFormat().getXIndex()] = diary;
                } else {
                    sortedAddRemainingDiaryToList(mRemainingDiaries, diary);
                }
            }
            return indexedDiaries;
        }

        protected ArrayList<FormattedDiaryBase> getSortedDiaries(FormattedDiaryBase[] indexedDiaries,
                                                                 int expectedSize) {
            ArrayList<FormattedDiaryBase> sortedDiaries = new ArrayList<>(expectedSize);
            for (FormattedDiaryBase diary : indexedDiaries) {
                if (diary != null) {
                    sortedDiaries.add(diary);
                }
            }
            return sortedDiaries;
        }

        protected void fillInRemainingDiaries(FormattedDiaryBase[] indexedDiaries) {
            int index = 0;
            for(FormattedDiaryBase diary : mRemainingDiaries) {
                if(!diary.getFormat().isVisible()) {
                    continue;
                }
                while(index < indexedDiaries.length) {
                    if(indexedDiaries[index] == null) {
                        diary.getFormat().setXIndex(index);
                        if (index < mMinItems) {
                            diary.getFormat().capDiaryLinesAt(1);
                            if(!diary.isBordered()) {
                                diary.setBoundaries(mFixedWidthBoundaries);
                            }
                        }
                        indexedDiaries[index] = diary;
                        index =  getNextIndex(indexedDiaries, index);
                        break;
                    }
                    index = getNextIndex(indexedDiaries, index);
                }
            }
            addVirtualDiaries(indexedDiaries, index);
        }

        protected void addVirtualDiaries(FormattedDiaryBase[] indexedDiaries, int initialIndex)  {
            for (int index = initialIndex; index < mMinItems; index++) {
                if (indexedDiaries[index] == null) {
                    indexedDiaries[index] = mVirtualDiary;
                }
            }
        }

        public ArrayList<FormattedDiaryBase> sort (ArrayList<FormattedDiaryBase> dayDiaries) {
            if(dayDiaries.isEmpty()) {
                return new ArrayList<>();
            }
            init(dayDiaries);
            FormattedDiaryBase[] indexedDiaries = fillInIndexedDiaries(dayDiaries);
            fillInRemainingDiaries(indexedDiaries);
            return getSortedDiaries(indexedDiaries, dayDiaries.size());
        }


    }

    /**
     * Class responsible for maintaining information about box related to a given day.
     * When created it is set at first day (with index 0).
     * 일정 날짜와 관련된 박스에 대한 정보 유지 관리를 담당하는 클래스
     * 생성시 첫날로 설정됨(인덱스 0)
     */
    protected class DayBoxBoundaries {
        private int mX;
        private int mY;
        private int mRightEdge;
        private int mYOffset;
        private int mXWidth;

        public DayBoxBoundaries() {
            mXWidth = mWidth / mNumDays;
            mYOffset = 0;
            mX = 1;
            mY = mEventYOffsetPortrait + mMonthNumHeight + mTopPaddingMonthNumber;
            mRightEdge = -1;
        }

        public void nextDay() {
            mX += mXWidth;
            mRightEdge += mXWidth;
            mYOffset = 0;
        }

        public int getX() { return  mX;}
        public int getY() { return  mY + mYOffset;}
        public int getYOrig() { return mY; }
        public int getRightEdge(int spanningDays) {return spanningDays * mXWidth + mRightEdge;}
        public int getAvailableXSpace() { return  mWidth - getX();}
        public int getAvailableYSpace() { return  mHeight - getY() - mEventBottomPadding;}
        public void moveDown(int y) { mYOffset += y; }
    }

    protected abstract class BoundariesSetter {
        protected DayBoxBoundaries mBoxBoundaries;
        protected int mBorderThickness;
        protected int mXPadding;
        public BoundariesSetter(DayBoxBoundaries boxBoundaries, int borderSpace, int xPadding) {
            mBoxBoundaries = boxBoundaries;
            mBorderThickness = borderSpace;
            mXPadding = xPadding;
        }
        public int getY() { return mBoxBoundaries.getY(); }
        public abstract void setRectangle(int spanningDays, int numberOfLines);
        public abstract void setCircle(int spanningDays);
        public int getTextX() { return mBoxBoundaries.getX() + mBorderThickness + mXPadding; }
        public int getTextY() {
            return mBoxBoundaries.getY() + mEventAscentHeight;
        }
        public int getTextRightEdge(int spanningDays) {
            return mBoxBoundaries.getRightEdge(spanningDays) - mBorderThickness;
        }
        public void moveToFirstLine() {
            mBoxBoundaries.moveDown(mBorderThickness);
        }
        public void moveLinesDown(int count) {
            mBoxBoundaries.moveDown(mEventHeight * count);
        }
        public void moveAfterDrawingTimes() {
            mBoxBoundaries.moveDown(mExtrasHeight);
        }
        public void moveToNextItem() {
            mBoxBoundaries.moveDown(mEventLinePadding + mBorderThickness);
        }
        public int getHeight(int numberOfLines) {
            return numberOfLines * mEventHeight + 2* mBorderThickness + mEventLinePadding;
        }
        public boolean hasBorder() {
            return mBorderThickness > 0;
        }
    }

    protected class AllDayBoundariesSetter extends BoundariesSetter {
        public AllDayBoundariesSetter(DayBoxBoundaries boxBoundaries) {
            super(boxBoundaries, mBorderSpace, 0);
        }
        @Override
        public void setRectangle(int spanningDays, int numberOfLines) {
            // We shift the render offset "inward", because drawRect with a stroke width greater
            // than 1 draws outside the specified bounds.  (We don't adjust the left edge, since
            // we want to match the existing appearance of the "event square".)
            // 선 폭이 1보다 큰 drawRect가 지정된 경계를 벗어나기 때문에, 렌더 오프셋을 "내향"으로 전환함
            // (기존의 "이벤트 스퀘어"의 외관과 일치할 것이기 때문에 왼쪽 가장자리를 조정하지 않음)
            r.left = mBoxBoundaries.getX();
            r.right = mBoxBoundaries.getRightEdge(spanningDays) - mStrokeWidthAdj;
            r.top = mBoxBoundaries.getY() + mStrokeWidthAdj;
            r.bottom = mBoxBoundaries.getY() + mEventHeight * numberOfLines + mBorderSpace * 2 - mStrokeWidthAdj;
        }

        @Override
        public void setCircle(int spanningDays) {
            circleX = mBoxBoundaries.getX() + 20;
            circleY = mBoxBoundaries.getYOrig() - 40;
        }
    }

    protected class RegularBoundariesSetter extends BoundariesSetter {
        public RegularBoundariesSetter(DayBoxBoundaries boxBoundaries) {
            super(boxBoundaries, 0, mEventSquareWidth + mEventRightPadding);
        }
        protected RegularBoundariesSetter(DayBoxBoundaries boxBoundaries, int border) {
            super(boxBoundaries, border, mEventSquareWidth + mEventRightPadding - border);
        }
        @Override
        public void setRectangle(int spanningDays, int numberOfLines) {
            r.left = mBoxBoundaries.getX();
            r.right = mBoxBoundaries.getX() + mEventSquareWidth;
            r.top = mBoxBoundaries.getY() + mEventAscentHeight - mEventSquareHeight;
            r.bottom = mBoxBoundaries.getY() + mEventAscentHeight + (numberOfLines - 1) * mEventHeight;
        }

        @Override
        public void setCircle(int spanningDays) {
            circleX = mBoxBoundaries.getX() + 20;
            circleY = mBoxBoundaries.getYOrig() - 40;
        }
    }
    protected class FixedHeightRegularBoundariesSetter extends RegularBoundariesSetter {
        public FixedHeightRegularBoundariesSetter(DayBoxBoundaries boxBoundaries) {
            super(boxBoundaries, mBorderSpace);
        }
    }

    protected class FixedWidthRegularBoundariesSetter extends RegularBoundariesSetter {
        public FixedWidthRegularBoundariesSetter(DayBoxBoundaries boxBoundaries) {
            super(boxBoundaries, mBorderSpace);
        }
    }

    protected class DiaryBoundariesSetter extends BoundariesSetter {
        public DiaryBoundariesSetter(DayBoxBoundaries boxBoundaries) {
            super(boxBoundaries, 0, mEventSquareWidth + mEventRightPadding);
        }
        protected DiaryBoundariesSetter(DayBoxBoundaries boxBoundaries, int border) {
            super(boxBoundaries, border, mEventSquareWidth + mEventRightPadding - border);
        }
        @Override
        public void setRectangle(int spanningDays, int numberOfLines) {
            r.left = mBoxBoundaries.getX();
            r.right = mBoxBoundaries.getX() + mEventSquareWidth;
            r.top = mBoxBoundaries.getY();
            r.bottom = mBoxBoundaries.getY() + mEventAscentHeight;
        }

        @Override
        public void setCircle(int spanningDays) {
            circleX = mBoxBoundaries.getX() + 20;
            circleY = mBoxBoundaries.getYOrig() - 40;
        }
    }

    /**
     * Contains information about event formatting
     * 이벤트 형식 지정에 대한 정보 포함
     */
    protected static class EventFormat {
        private int mLines;
        private int[] mDaySpan;
        private int mYIndex;
        private boolean mPartiallyHidden;
        private final int Y_INDEX_NOT_SET = -1;

        public EventFormat(int day, int weekDays) {
            mDaySpan = new int[weekDays];
            if (day < weekDays && day >= 0) {
                mDaySpan[day] = 1;
            }
            mLines = 1;
            mYIndex = Y_INDEX_NOT_SET;
            mPartiallyHidden = false;
        }

        /**
         * Returns information about how many event lines are above this event
         * If y-order is not yet determined returns -1
         * 이 이벤트 위에 있는 이벤트 줄 수에 대한 정보 반환
         * 만약 y-order가 아직 결정되지 않은 경우 -1 반환
         * @return
         */
        public int getYIndex() { return mYIndex;}
        public void setYIndex(int index) { mYIndex = index;}
        public boolean isVisible() { return mLines > 0; }
        public void hide(int day) {
            if (mDaySpan.length <= day) {
                return;
            }
            if (getTotalSpan() > 1) {
                mPartiallyHidden = true;
                int splitIndex = day;
                while (splitIndex >= 0) {
                    if (mDaySpan[splitIndex] > 0) {
                        break;
                    }
                    splitIndex--;
                }
                int span = mDaySpan[splitIndex];
                mDaySpan[splitIndex] = day - splitIndex;
                mDaySpan[day] = 0;
                if (mDaySpan.length > day + 1) {
                    mDaySpan[day + 1] = span - 1 - mDaySpan[splitIndex];
                }
            } else {
                mLines = 0;
                mPartiallyHidden = false;
            }
        }

        public boolean isPartiallyHidden() {
            return mPartiallyHidden;
        }
        public int getEventLines() { return  mLines; }

        /**
         * If event is visible, sets new value of event lines
         * 이벤트가 표시되는 경우, 이벤트 라인의 새 값 설정
         * @param lines
         */
        public void setEventLines(int lines) {
            if (mLines != 0) {
                mLines = lines;
            }
        }
        public void capEventLinesAt(int cap) { mLines = Math.min(mLines, cap); }
        public void extendDaySpan(int day) {
            for (int index = Math.min(day, mDaySpan.length - 1); index >= 0; index--) {
                if (mDaySpan[index] > 0) {
                    mDaySpan[index]++;
                    break;
                }
            }
        }
        public int getDaySpan(int day) { return day < mDaySpan.length ? mDaySpan[day] : 0; }
        public int getTotalSpan() {
            int span = 0;
            for (int i : mDaySpan) {
                span += i;
            }
            return span;
        }
    }

    protected static class DiaryFormat {
        private int mLines;
        private int[] mDaySpan;
        private int mXIndex;
        private boolean mPartiallyHidden;
        private final int Y_INDEX_NOT_SET = -1;

        public DiaryFormat(int day, int weekDays) {
            mDaySpan = new int[weekDays];
            if (day < weekDays && day >= 0) {
                mDaySpan[day] = 1;
            }
            mLines = 1;
            mXIndex = Y_INDEX_NOT_SET;
            mPartiallyHidden = false;
        }

        /**
         * Returns information about how many event lines are above this event
         * If y-order is not yet determined returns -1
         * 이 이벤트 위에 있는 이벤트 줄 수에 대한 정보 반환
         * 만약 y-order가 아직 결정되지 않은 경우 -1 반환
         * @return
         */
        public int getXIndex() { return mXIndex;}
        public void setXIndex(int index) { mXIndex = index;}
        public boolean isVisible() { return mLines > 0; }
        public void hide(int day) {
            if (mDaySpan.length <= day) {
                return;
            }
            if (getTotalSpan() > 1) {
                mPartiallyHidden = true;
                int splitIndex = day;
                while (splitIndex >= 0) {
                    if (mDaySpan[splitIndex] > 0) {
                        break;
                    }
                    splitIndex--;
                }
                int span = mDaySpan[splitIndex];
                mDaySpan[splitIndex] = day - splitIndex;
                mDaySpan[day] = 0;
                if (mDaySpan.length > day + 1) {
                    mDaySpan[day + 1] = span - 1 - mDaySpan[splitIndex];
                }
            } else {
                mLines = 0;
                mPartiallyHidden = false;
            }
        }

        public boolean isPartiallyHidden() {
            return mPartiallyHidden;
        }
        public int getDiaryLines() { return  mLines; }

        /**
         * If event is visible, sets new value of event lines
         * 이벤트가 표시되는 경우, 이벤트 라인의 새 값 설정
         * @param lines
         */
        public void setEventLines(int lines) {
            if (mLines != 0) {
                mLines = lines;
            }
        }
        public void capDiaryLinesAt(int cap) { mLines = Math.min(mLines, cap); }
        public void extendDaySpan(int day) {
            for (int index = Math.min(day, mDaySpan.length - 1); index >= 0; index--) {
                if (mDaySpan[index] > 0) {
                    mDaySpan[index]++;
                    break;
                }
            }
        }
        public int getDaySpan(int day) { return day < mDaySpan.length ? mDaySpan[day] : 0; }
        public int getTotalSpan() {
            int span = 0;
            for (int i : mDaySpan) {
                span += i;
            }
            return span;
        }
    }

    protected abstract class FormattedEventBase {
        protected BoundariesSetter mBoundaries;
        protected EventFormat mFormat;
        FormattedEventBase(EventFormat format, BoundariesSetter boundaries) {
            mBoundaries = boundaries;
            mFormat = format;
        }
        public void setBoundaries(BoundariesSetter boundaries) { mBoundaries = boundaries; }
        public boolean isBordered() { return mBoundaries.hasBorder(); }
        public EventFormat getFormat() { return mFormat; }
        public abstract void initialPreFormatText(ViewDetailsPreferences.Preferences preferences);
        protected abstract boolean isTimeInNextLine(ViewDetailsPreferences.Preferences preferences);
        public abstract void draw(Canvas canvas, ViewDetailsPreferences.Preferences preferences, int day);
        public abstract boolean containsEvent(Event event);

        public void skip(ViewDetailsPreferences.Preferences preferences) {
            if (mFormat.isVisible()) {
                mBoundaries.moveToFirstLine();
                mBoundaries.moveLinesDown(mFormat.getEventLines());
                if (isTimeInNextLine(preferences)) {
                    mBoundaries.moveAfterDrawingTimes();
                }
                mBoundaries.moveToNextItem();
            }
        }

        public int getHeight(ViewDetailsPreferences.Preferences preferences) {
            int timesHeight = isTimeInNextLine(preferences) ? mExtrasHeight : 0;
            return mBoundaries.getHeight(mFormat.getEventLines()) + timesHeight;
        }
    }

    protected abstract class FormattedDiaryBase {
        protected BoundariesSetter mBoundaries;
        protected DiaryFormat mFormat;
        FormattedDiaryBase(DiaryFormat format, BoundariesSetter boundaries) {
            mBoundaries = boundaries;
            mFormat = format;
        }
        public void setBoundaries(BoundariesSetter boundaries) { mBoundaries = boundaries; }
        public boolean isBordered() { return mBoundaries.hasBorder(); }
        public DiaryFormat getFormat() { return mFormat; }
        public abstract void initialPreFormatText(ViewDetailsPreferences.Preferences preferences);
        protected abstract boolean isTimeInNextLine(ViewDetailsPreferences.Preferences preferences);
        public abstract void draw(Canvas canvas, ViewDetailsPreferences.Preferences preferences, int day);
        public abstract boolean containsDiary(Diary diary);

        public void skip(ViewDetailsPreferences.Preferences preferences) {
            if (mFormat.isVisible()) {
                mBoundaries.moveToFirstLine();
                mBoundaries.moveLinesDown(mFormat.getDiaryLines());
                if (isTimeInNextLine(preferences)) {
                    mBoundaries.moveAfterDrawingTimes();
                }
                mBoundaries.moveToNextItem();
            }
        }

        public int getHeight(ViewDetailsPreferences.Preferences preferences) {
            int timesHeight = isTimeInNextLine(preferences) ? mExtrasHeight : 0;
            return mBoundaries.getHeight(mFormat.getDiaryLines()) + timesHeight;
        }
    }

    protected class NullFormattedEvent extends FormattedEventBase {
        NullFormattedEvent(EventFormat format, BoundariesSetter boundaries) {
            super(format, boundaries);
        }

        /**
         * Null object has no text to be formatted
         * Null 개체에는 포맷할 텍스트가 없음
         */
        public void initialPreFormatText(ViewDetailsPreferences.Preferences preferences) { /*nop*/ }
        protected boolean isTimeInNextLine(ViewDetailsPreferences.Preferences preferences) { return false; }

        /**
         * Null object won't be drawn
         * Null 개체는 그려지지 않을 것임
         * @param canvas
         * @param preferences
         * @param day
         */
        public void draw(Canvas canvas, ViewDetailsPreferences.Preferences preferences, int day) { /*nop*/ }
        public boolean containsEvent(Event event) { return false; }
    }

    protected class NullFormattedDiary extends FormattedDiaryBase {
        NullFormattedDiary(DiaryFormat format, BoundariesSetter boundaries) {
            super(format, boundaries);
        }

        /**
         * Null object has no text to be formatted
         * Null 개체에는 포맷할 텍스트가 없음
         */
        public void initialPreFormatText(ViewDetailsPreferences.Preferences preferences) { /*nop*/ }
        protected boolean isTimeInNextLine(ViewDetailsPreferences.Preferences preferences) { return false; }

        /**
         * Null object won't be drawn
         * Null 개체는 그려지지 않을 것임
         * @param canvas
         * @param preferences
         * @param day
         */
        public void draw(Canvas canvas, ViewDetailsPreferences.Preferences preferences, int day) { /*nop*/ }
        public boolean containsDiary(Diary diary) { return false; }
    }

    protected class FormattedEvent extends FormattedEventBase {
        private Event mEvent;
        private DynamicLayout mTextLayout;
        public FormattedEvent(Event event, EventFormat format, BoundariesSetter boundaries) {
            super(format, boundaries);
            mEvent = event;
        }

        protected boolean isDeclined() {
            return mEvent.selfAttendeeStatus == Attendees.ATTENDEE_STATUS_DECLINED;
        }

        protected boolean isAtendeeStatusInvited() {
            return mEvent.selfAttendeeStatus == Attendees.ATTENDEE_STATUS_INVITED;
        }

        protected Paint.Style getRectanglePaintStyle() {
            return (isAtendeeStatusInvited()) ?
                    Style.STROKE : Style.FILL_AND_STROKE;
        }
        protected int getRectangleColor() {
            return isDeclined() ? Utils.getDeclinedColorFromColor(mEvent.color) : mEvent.color;
        }

        protected void drawEventRectangle(Canvas canvas, int day)  {
            mBoundaries.setRectangle(mFormat.getDaySpan(day), mFormat.getEventLines());
            mEventSquarePaint.setStyle(getRectanglePaintStyle());
            mEventSquarePaint.setColor(getRectangleColor());
            canvas.drawRect(r, mEventSquarePaint);
        }

        protected int getAvailableSpaceForText(int spanningDays) {
            return mBoundaries.getTextRightEdge(spanningDays) - mBoundaries.getTextX();
        }

        @Override
        public void initialPreFormatText(ViewDetailsPreferences.Preferences preferences) {
            if (mTextLayout == null) {
                final int span = mFormat.getTotalSpan();
                preFormatText(preferences, span);
                if (span == 1) {
                    /* make events higher only if they are not spanning multiple days to avoid
                        tricky situations
                         까다로운 상황을 피하기 위해 여러 날에 걸쳐 있지 않은 경우에만 이벤트를 더 높게 함 */
                    mFormat.setEventLines(Math.min(mTextLayout.getLineCount(), mMaxLinesInEvent));
                }
            }
        }

        protected boolean isTimeInline(ViewDetailsPreferences.Preferences preferences) {
            return preferences.isTimeVisible() && !isTimeInNextLine(preferences) && !mEvent.allDay;
        }

        protected CharSequence getBaseText(ViewDetailsPreferences.Preferences preferences) {
            StringBuilder baseText = new StringBuilder();
            if (isTimeInline(preferences)) {
                baseText.append(getFormattedTime(preferences));
                baseText.append(" ");
            }
            baseText.append(mEvent.title);
            if (preferences.SHOW_LOCATION && mEvent.location != null && mEvent.location.length() > 0) {
                baseText.append("\n@ ");
                baseText.append(mEvent.location);
            }
            return baseText;
        }

        protected void preFormatText(ViewDetailsPreferences.Preferences preferences, int span) {
            if (mEvent == null) {
                return;
            }
            mTextLayout = new DynamicLayout(getBaseText(preferences), mEventPaint,
                    getAvailableSpaceForText(span), Layout.Alignment.ALIGN_NORMAL,
                    0.0f, 0.0f, false);
        }

        protected CharSequence getFormattedText(CharSequence text, int span) {
            float avail = getAvailableSpaceForText(span);
            return TextUtils.ellipsize(text, mEventPaint, avail, TextUtils.TruncateAt.END);
        }

        protected Paint getTextPaint() {
            if (!isAtendeeStatusInvited() && mEvent.drawAsAllday()){
                // Text color needs to contrast with solid background.
                // 텍스트 색상은 견고한... 배경과 대비되어야 함
                return mSolidBackgroundEventPaint;
            } else if (isDeclined()) {
                // Use "declined event" color.
                // "지연된 이벤트" 색상 사용
                return mDeclinedEventPaint;
            } else if (mEvent.drawAsAllday()) {
                // Text inside frame is same color as frame.
                // 프레임 내부 텍스트의 색상은 프레임과 동일함
                mFramedEventPaint.setColor(getRectangleColor());
                return mFramedEventPaint;
            }
            // Use generic event text color.
            // 일반 이벤트 텍스트 색상을 사용
            return mEventPaint;
        }

        protected void drawText(Canvas canvas, ViewDetailsPreferences.Preferences preferences, int day) {
            CharSequence baseText = getBaseText(preferences);
            final int linesNo = mFormat.getEventLines();
            final int span = mFormat.getDaySpan(day);
            if (mFormat.isPartiallyHidden()) {
                preFormatText(preferences, span);
            }
            for (int i = 0; i < linesNo; i++) {
                CharSequence lineText;
                if (i == linesNo - 1) {
                    lineText = getFormattedText(baseText.subSequence(mTextLayout.getLineStart(i),
                            baseText.length()), span);
                } else {
                    lineText = baseText.subSequence(mTextLayout.getLineStart(i),
                            mTextLayout.getLineEnd(i));
                }
                canvas.drawText(lineText.toString(), mBoundaries.getTextX(), mBoundaries.getTextY(),
                        getTextPaint());
                mBoundaries.moveLinesDown(1);
            }
        }

        @Override
        protected boolean isTimeInNextLine(ViewDetailsPreferences.Preferences preferences) {
            return preferences.isTimeShownBelow() && !mBoundaries.hasBorder();
        }

        protected Paint getTimesPaint() {
            return isDeclined() ? mEventDeclinedExtrasPaint : mEventExtrasPaint;
        }

        protected CharSequence getFormattedTime(ViewDetailsPreferences.Preferences preferences) {
            StringBuilder time = new StringBuilder();
            if (preferences.isStartTimeVisible()) {
                mStringBuilder.setLength(0);
                time.append(DateUtils.formatDateRange(getContext(), mFormatter, mEvent.startMillis,
                        mEvent.startMillis, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL,
                        Utils.getTimeZone(getContext(), null)));
            }
            if (preferences.isEndTimeVisible()) {
                time.append(" \u2013 ");
                if (mEvent.startDay != mEvent.endDay) {
                    mStringBuilder.setLength(0);
                    time.append(DateUtils.formatDateRange(getContext(), mFormatter, mEvent.endMillis,
                            mEvent.endMillis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL,
                            Utils.getTimeZone(getContext(), null)));
                    time.append(", ");
                }
                mStringBuilder.setLength(0);
                time.append(DateUtils.formatDateRange(getContext(), mFormatter, mEvent.endMillis,
                        mEvent.endMillis, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL,
                        Utils.getTimeZone(getContext(), null)));
            }
            if (preferences.isDurationVisible()) {
                if (time.length() > 0) {
                    time.append(' ');
                }
                time.append('[');
                time.append(DateUtils.formatElapsedTime((mEvent.endMillis - mEvent.startMillis)/1000));
                time.append(']');
            }
            return time;
        }

        protected void drawTimes(Canvas canvas, ViewDetailsPreferences.Preferences preferences) {
            CharSequence text = getFormattedTime(preferences);
            float avail = getAvailableSpaceForText(1);
            text = TextUtils.ellipsize(text, mEventExtrasPaint, avail, TextUtils.TruncateAt.END);
            canvas.drawText(text.toString(), mBoundaries.getTextX(),
                    mBoundaries.getTextY(), getTimesPaint());
            mBoundaries.moveAfterDrawingTimes();
        }

        @Override
        public void draw(Canvas canvas, ViewDetailsPreferences.Preferences preferences, int day) {
            if (mFormat.isVisible() && mEvent != null) {
                drawEventRectangle(canvas, day);
                mBoundaries.moveToFirstLine();
                drawText(canvas, preferences, day);
                if (isTimeInNextLine(preferences)) {
                    drawTimes(canvas, preferences);
                }
                mBoundaries.moveToNextItem();
            }
        }
        public boolean containsEvent(Event event) { return event.equals(mEvent); }
    }

    protected class FormattedDiary extends FormattedDiaryBase {
        private Diary mDiary;
        private DynamicLayout mTextLayout;
        public FormattedDiary(Diary diary, DiaryFormat format, BoundariesSetter boundaries) {
            super(format, boundaries);
            mDiary = diary;
        }

        protected Paint.Style getCirclePaintStyle() {
            return Style.FILL;
        }

        protected int getCircleColor() {
            String selectedColorName = Utils.getSharedPreference(getContext(), GeneralPreferences.KEY_COLOR_PREF, "teal");
            return getContext().getResources().getColor(DynamicTheme.getColorId(selectedColorName));
        }

        protected void drawDiaryCircle(Canvas canvas, int day) {
            mBoundaries.setCircle(mFormat.getDaySpan(day));
            mDiaryCirclePaint.setStyle(getCirclePaintStyle());
            mDiaryCirclePaint.setColor(getCircleColor());
            Log.i(TAG, "drawDiaryCircle, circleX="+circleX+", circleY"+circleY);
            canvas.drawCircle(circleX, circleY, mDiaryCircleSize, mDiaryCirclePaint);

        }

        @Override
        public void initialPreFormatText(ViewDetailsPreferences.Preferences preferences) {

        }

        @Override
        protected boolean isTimeInNextLine(ViewDetailsPreferences.Preferences preferences) {
            return false;
        }

        @Override
        public void draw(Canvas canvas, ViewDetailsPreferences.Preferences preferences, int day) {
            Log.i(TAG, "FormattedDiary, draw 실행");
            if(mFormat.isVisible() && mDiary != null) {
                drawDiaryCircle(canvas, day);
                mBoundaries.moveToNextItem();
            }
        }

        @Override
        public boolean containsDiary(Diary diary) {
            return diary.equals(mDiary);
        }
    }

    protected void drawMoreEvents(Canvas canvas, int remainingEvents, int x) {
        int y = mHeight - (mExtrasDescent + mEventBottomPadding);
        String text = getContext().getResources().getQuantityString(
                R.plurals.month_more_events, remainingEvents);
        mEventExtrasPaint.setAntiAlias(true);
        mEventExtrasPaint.setFakeBoldText(true);
        canvas.drawText(String.format(text, remainingEvents), x, y, mEventExtrasPaint);
        mEventExtrasPaint.setFakeBoldText(false);
    }

    /**
     * Draws a line showing busy times in each day of week The method draws
     * non-conflicting times in the event color and times with conflicting
     * events in the dna conflict color defined in colors.
     * 주 중 매일 바쁜 시간을 표시하는 라인을 그림
     * 이 메소드는 colors에 정의된 dna 충돌 색상과 충돌하는 이벤트와
     * 이벤트 컬러와 시간이 충돌하지 않는 시간을 표시함
     *
     * @param canvas
     */
    protected void drawDNA(Canvas canvas) {
        // Draw event and conflict times
        // 이벤트 및 충돌 시간 그리기
        if (mDna != null) {
            for (Utils.DNAStrand strand : mDna.values()) {
                if (strand.color == mConflictColor || strand.points == null
                        || strand.points.length == 0) {
                    continue;
                }
                mDNATimePaint.setColor(strand.color);
                canvas.drawLines(strand.points, mDNATimePaint);
            }
            // Draw black last to make sure it's on top
            // 맨 위에 있는지 확인하기 위해 마지막으로 검은색 그리기
            Utils.DNAStrand strand = mDna.get(mConflictColor);
            if (strand != null && strand.points != null && strand.points.length != 0) {
                mDNATimePaint.setColor(strand.color);
                canvas.drawLines(strand.points, mDNATimePaint);
            }
            if (mDayXs == null) {
                return;
            }
            int numDays = mDayXs.length;
            int xOffset = (mDnaAllDayWidth - mDnaWidth) / 2;
            if (strand != null && strand.allDays != null && strand.allDays.length == numDays) {
                for (int i = 0; i < numDays; i++) {
                    // this adds at most 7 draws. We could sort it by color and
                    // build an array instead but this is easier.
                    // 최대 7개까지 추가함
                    // 컬러별로 분류하고 배열을 만들 수도 있지만 이게 더 쉬움
                    if (strand.allDays[i] != 0) {
                        mDNAAllDayPaint.setColor(strand.allDays[i]);
                        canvas.drawLine(mDayXs[i] + xOffset, mDnaMargin, mDayXs[i] + xOffset,
                                mDnaMargin + mDnaAllDayHeight, mDNAAllDayPaint);
                    }
                }
            }
        }
    }

    @Override
    protected void updateSelectionPositions() {
        if (mHasSelectedDay) {
            int selectedPosition = mSelectedDay - mWeekStart;
            if (selectedPosition < 0) {
                selectedPosition += 7;
            }
            int effectiveWidth = mWidth - mPadding * 2;
            mSelectedLeft = selectedPosition * effectiveWidth / mNumDays + mPadding;
            mSelectedRight = (selectedPosition + 1) * effectiveWidth / mNumDays + mPadding;
        }
    }

    public int getDayIndexFromLocation(float x) {
        int dayStart = mPadding;
        if (x < dayStart || x > mWidth - mPadding) {
            return -1;
        }
        // Selection is (x - start) / (pixels/day) == (x -s) * day / pixels
        return ((int) ((x - dayStart) * mNumDays / (mWidth - dayStart - mPadding)));
    }

    @Override
    public Time getDayFromLocation(float x) {
        int dayPosition = getDayIndexFromLocation(x);
        if (dayPosition == -1) {
            return null;
        }
        int day = mFirstJulianDay + dayPosition;

        Time time = new Time(mTimeZone);
        if (mWeek == 0) {
            // This week is weird...
            if (day < Time.EPOCH_JULIAN_DAY) {
                day++;
            } else if (day == Time.EPOCH_JULIAN_DAY) {
                time.set(1, 0, 1970);
                time.normalize(true);
                return time;
            }
        }

        time.setJulianDay(day);
        return time;
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        Context context = getContext();
        // only send accessibility events if accessibility and exploration are
        // on.
        // 접근성 및 탐사...가 켜져 있는 경우에만 접근성 이벤트를 전송함
        AccessibilityManager am = (AccessibilityManager) context
                .getSystemService(Service.ACCESSIBILITY_SERVICE);
        if (!am.isEnabled() || !am.isTouchExplorationEnabled()) {
            return super.onHoverEvent(event);
        }
        if (event.getAction() != MotionEvent.ACTION_HOVER_EXIT) {
            Time hover = getDayFromLocation(event.getX());
            if (hover != null
                    && (mLastHoverTime == null || Time.compare(hover, mLastHoverTime) != 0)) {
                Long millis = hover.toMillis(true);
                String date = Utils.formatDateRange(context, millis, millis,
                        DateUtils.FORMAT_SHOW_DATE);
                AccessibilityEvent accessEvent = AccessibilityEvent
                        .obtain(AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
                accessEvent.getText().add(date);
                if (mShowDetailsInMonth && mEvents != null) {
                    int dayStart = mSpacingWeekNumber + mPadding;
                    int dayPosition = (int) ((event.getX() - dayStart) * mNumDays / (mWidth
                            - dayStart - mPadding));
                    ArrayList<Event> events = mEvents.get(dayPosition);
                    List<CharSequence> text = accessEvent.getText();
                    for (Event e : events) {
                        text.add(e.getTitleAndLocation() + ". ");
                        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
                        if (!e.allDay) {
                            flags |= DateUtils.FORMAT_SHOW_TIME;
                            if (DateFormat.is24HourFormat(context)) {
                                flags |= DateUtils.FORMAT_24HOUR;
                            }
                        } else {
                            flags |= DateUtils.FORMAT_UTC;
                        }
                        text.add(Utils.formatDateRange(context, e.startMillis, e.endMillis,
                                flags) + ". ");
                    }
                }
                sendAccessibilityEventUnchecked(accessEvent);
                mLastHoverTime = hover;
            }
        }
        return true;
    }

    public void setClickedDay(float xLocation) {
        mClickedDayIndex = getDayIndexFromLocation(xLocation);
        invalidate();
    }

    public void clearClickedDay() {
        mClickedDayIndex = -1;
        invalidate();
    }

    class TodayAnimatorListener extends AnimatorListenerAdapter {
        private volatile Animator mAnimator = null;
        private volatile boolean mFadingIn = false;

        @Override
        public void onAnimationEnd(Animator animation) {
            synchronized (this) {
                if (mAnimator != animation) {
                    animation.removeAllListeners();
                    animation.cancel();
                    return;
                }
                if (mFadingIn) {
                    if (mTodayAnimator != null) {
                        mTodayAnimator.removeAllListeners();
                        mTodayAnimator.cancel();
                    }
                    mTodayAnimator = ObjectAnimator.ofInt(MonthWeekEventsView.this,
                            "animateTodayAlpha", 255, 0);
                    mAnimator = mTodayAnimator;
                    mFadingIn = false;
                    mTodayAnimator.addListener(this);
                    mTodayAnimator.setDuration(600);
                    mTodayAnimator.start();
                } else {
                    mAnimateToday = false;
                    mAnimateTodayAlpha = 0;
                    mAnimator.removeAllListeners();
                    mAnimator = null;
                    mTodayAnimator = null;
                    invalidate();
                }
            }
        }

        public void setAnimator(Animator animation) {
            mAnimator = animation;
        }

        public void setFadingIn(boolean fadingIn) {
            mFadingIn = fadingIn;
        }

    }

    /**
     * This provides a reference to a float array which allows for easy size
     * checking and reallocation. Used for drawing lines.
     * 쉽게 사이즈를 확인하고 재할당할 수 있는 float 배열에 대한 참조를 제공함
     * 선 그리기에서 사용함
     */
    private class FloatRef {
        float[] array;

        public FloatRef(int size) {
            array = new float[size];
        }

        public void ensureSize(int newSize) {
            if (newSize >= array.length) {
                // Add enough space for 7 more boxes to be drawn
                // 7개의 박스를 더 그릴 수 있는 충분한 공간 추가
                array = Arrays.copyOf(array, newSize + 16 * 7);
            }
        }
    }
}

