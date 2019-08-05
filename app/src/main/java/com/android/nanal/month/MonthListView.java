package com.android.nanal.month;

/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ListView;

import com.android.nanal.event.Utils;

public class MonthListView extends ListView {

    private static final String TAG = "MonthListView";
    VelocityTracker mTracker;
    private static float mScale = 0;

    // These define the behavior of the fling. Below MIN_VELOCITY_FOR_FLING, do the system fling
    // behavior. Between MIN_VELOCITY_FOR_FLING and MULTIPLE_MONTH_VELOCITY_THRESHOLD, do one month
    // fling. Above MULTIPLE_MONTH_VELOCITY_THRESHOLD, do multiple month flings according to the
    // fling strength. When doing multiple month fling, the velocity is reduced by this threshold
    // to prevent moving from one month fling to 4 months and above flings.
    // 이것들은 플링의 행동을 정의함, MIN_VELOCITY_FOR_FLING 하위?밑?에서 시스템 플링 동작을 수행함
    // MIN_VELOCITY_FOR_FLING과 MULTIPLE_MONTH_VELOCITY_THRESHOLD 사이에 한 달 동안 플링함
    // MULTIPLE_MONTH_VELOCITY_THRESHOLD 상위?위?에서는 플링 강도에 따라 다개월 플링 작업을 수행함
    // 여러 달 동안 플링할 때, 한 달 플링에서 4개월 이상 플링 이상으로 이동하는 것을
    // 방지하기 위해서 속도가 감소함

    private static int MIN_VELOCITY_FOR_FLING = 1500;
    private static int MULTIPLE_MONTH_VELOCITY_THRESHOLD = 2000;
    private static int FLING_VELOCITY_DIVIDER = 500;
    private static int FLING_TIME = 1000;

    // disposable variable used for time calculations
    // 시간 계산에 사용되는 변수
    protected Time mTempTime;
    private long mDownActionTime;
    private final Rect mFirstViewRect = new Rect();

    Context mListContext;

    // Updates the time zone when it changes
    // 변경시 표준 시간대 업데이트
    private final Runnable mTimezoneUpdater = new Runnable() {
        @Override
        public void run() {
            if (mTempTime != null && mListContext != null) {
                mTempTime.timezone =
                        Utils.getTimeZone(mListContext, mTimezoneUpdater);
            }
        }
    };

    public MonthListView(Context context) {
        super(context);
        init(context);
    }

    public MonthListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MonthListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context c) {
        mListContext = c;
        mTracker  = VelocityTracker.obtain();
        mTempTime = new Time(Utils.getTimeZone(c,mTimezoneUpdater));
        if (mScale == 0) {
            mScale = c.getResources().getDisplayMetrics().density;
            if (mScale != 1) {
                MIN_VELOCITY_FOR_FLING *= mScale;
                MULTIPLE_MONTH_VELOCITY_THRESHOLD *= mScale;
                FLING_VELOCITY_DIVIDER *= mScale;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return processEvent(ev) || super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return processEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    private boolean processEvent (MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            // Since doFling sends a cancel, make sure not to process it.
            // doFling이 취소를 보내면 처리하지 않음
            case MotionEvent.ACTION_CANCEL:
                return false;
            // Start tracking movement velocity
            // 이동 속도 추적 시작
            case MotionEvent.ACTION_DOWN:
                mTracker.clear();
                mDownActionTime = SystemClock.uptimeMillis();
                break;
            // Accumulate velocity and do a custom fling when above threshold
            // 임계값을 초과할 경우, 속도 누적 및 사용자 지정 플링 수행
            case MotionEvent.ACTION_UP:
                mTracker.addMovement(ev);
                mTracker.computeCurrentVelocity(1000);    // in pixels per second, 초당 픽셀 단위로
                float vel =  mTracker.getYVelocity ();
                if (Math.abs(vel) > MIN_VELOCITY_FOR_FLING) {
                    doFling(vel);
                    return true;
                }
                break;
            default:
                mTracker.addMovement(ev);
                break;
        }
        return false;
    }

    // Do a "snap to start of month" fling
    private void doFling(float velocityY) {

        // Stop the list-view movement and take over
        // 중지하고 리스트뷰 이동을 중지하고 인계받음
        MotionEvent cancelEvent = MotionEvent.obtain(mDownActionTime,  SystemClock.uptimeMillis(),
                MotionEvent.ACTION_CANCEL, 0, 0, 0);
        onTouchEvent(cancelEvent);

        // Below the threshold, fling one month. Above the threshold , fling
        // according to the speed of the fling.
        // 한계점 이하에서 한 달을 플링함
        // 한계점을 넘으면 플링 속도에 따라서 플링함
        int monthsToJump;
        if (Math.abs(velocityY) < MULTIPLE_MONTH_VELOCITY_THRESHOLD) {
            if (velocityY < 0) {
                monthsToJump = 1;
            } else {
                // value here is zero and not -1 since by the time the fling is
                // detected the list moved back one month.
                // 플링이 감지될 때 리스트가 한 달 뒤로 이동되었기 때문에 여기서 값은 -1이 아니라 0임
                monthsToJump = 0;
            }
        } else {
            if (velocityY < 0) {
                monthsToJump = 1 - (int) ((velocityY + MULTIPLE_MONTH_VELOCITY_THRESHOLD)
                        / FLING_VELOCITY_DIVIDER);
            } else {
                monthsToJump = -(int) ((velocityY - MULTIPLE_MONTH_VELOCITY_THRESHOLD)
                        / FLING_VELOCITY_DIVIDER);
            }
        }

        // Get the day at the top right corner
        // 오른쪽 상단 모서리에서 일을 가져옴
        int day = getUpperRightJulianDay();
        // Get the day of the first day of the next/previous month
        // (according to scroll direction)
        // 스크롤 방향에 따라 다음/이전 달의 첫날 가져오기
        mTempTime.setJulianDay(day);
        mTempTime.monthDay = 1;
        mTempTime.month += monthsToJump;
        long timeInMillis = mTempTime.normalize(false);
        // Since each view is 7 days, round the target day up to make sure the
        // scroll will be  at least one view.
        // 각 뷰는 7일이므로, 목표일을 반올림하여 스크롤에 적어도 하나의 뷰가 있도록 함
        int scrollToDay = Time.getJulianDay(timeInMillis, mTempTime.gmtoff)
                + ((monthsToJump > 0) ? 6 : 0);

        // Since all views have the same height, scroll by pixels instead of
        // "to position".
        // 모든 뷰의 높이가 같으므로 "위치" 대신 픽셀 단위로 스크롤함
        // Compensate for the top view offset from the top.
        // 상단의 상단 뷰 오프셋을 보정함
        View firstView = getChildAt(0);
        int firstViewHeight = firstView.getHeight();
        // Get visible part length 보이는 부분의 길이 가져오기
        firstView.getLocalVisibleRect(mFirstViewRect);
        int topViewVisiblePart = mFirstViewRect.bottom - mFirstViewRect.top;
        int viewsToFling = (scrollToDay - day) / 7 - ((monthsToJump <= 0) ? 1 : 0);
        int offset = (viewsToFling > 0) ? -(firstViewHeight - topViewVisiblePart
                + SimpleDayPickerFragment.LIST_TOP_OFFSET) : (topViewVisiblePart
                - SimpleDayPickerFragment.LIST_TOP_OFFSET);
        // Fling 플링
        smoothScrollBy(viewsToFling * firstViewHeight + offset, FLING_TIME);
    }

    // Returns the julian day of the day in the upper right corner
    // 오른쪽 상단 모서리에 있는 날의 줄리안 데이를 반환함
    private int getUpperRightJulianDay() {
        SimpleWeekView child = (SimpleWeekView) getChildAt(0);
        if (child == null) {
            return -1;
        }
        return child.getFirstJulianDay() + SimpleDayPickerFragment.DAYS_PER_WEEK - 1;
    }
}

