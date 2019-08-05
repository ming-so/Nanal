package com.android.nanal.datetimepicker.time;
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
import android.util.Log;
import android.view.View;

import com.android.nanal.R;

/**
 * Draws a simple white circle on which the numbers will be drawn.
 * 숫자가 쓰여질 원을 그린다.
 */
class CircleView extends View {
    private static final String TAG = "CircleView";

    private final Paint mPaint = new Paint();
    private boolean mIs24HourMode;
    private int mCircleColor;
    private int mDotColor;
    private float mCircleRadiusMultiplier;
    private float mAmPmCircleRadiusMultiplier;
    private boolean mIsInitialized;

    private boolean mDrawValuesReady;
    private int mXCenter;
    private int mYCenter;
    private int mCircleRadius;

    public CircleView(Context context) {
        super(context);

        Resources res = context.getResources();
        mCircleColor = res.getColor(android.R.color.white);
        mDotColor = res.getColor(R.color.numbers_text_color);
        mPaint.setAntiAlias(true);

        mIsInitialized = false;
    }

    public void initialize(Context context, boolean is24HourMode) {
        if (mIsInitialized) {   // 이미 초기화가 되었다면 log 출력하고 return
            Log.e(TAG, "CircleView may only be initialized once.");
            return;
        }

        Resources res = context.getResources();
        mIs24HourMode = is24HourMode;
        if (is24HourMode) {     // 24시간 모드라면
            mCircleRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.circle_radius_multiplier_24HourMode));
        } else {                // 12시간 모드라면
            mCircleRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.circle_radius_multiplier));
            mAmPmCircleRadiusMultiplier =
                    Float.parseFloat(res.getString(R.string.ampm_circle_radius_multiplier));
        }

        mIsInitialized = true;      // 초기화 완료
    }

    /* package */ void setTheme(Context context, boolean dark) {    // 테마 적용
        Resources res = context.getResources();
        if (dark) {                    // 어두운 테마라면
            mCircleColor = res.getColor(R.color.dark_gray);         // 색상 변경
            mDotColor = res.getColor(R.color.light_gray);
        } else {
            mCircleColor = res.getColor(android.R.color.white);
            mDotColor = res.getColor(R.color.numbers_text_color);
        }
    }


    @Override
    public void onDraw(Canvas canvas) {
        int viewWidth = getWidth();
        if (viewWidth == 0 || !mIsInitialized) {
            // width 불러오기를 실패했거나 초기화가 되어 있지 않다면 return
            return;
        }

        if (!mDrawValuesReady) {
            mXCenter = getWidth() / 2;
            mYCenter = getHeight() / 2;
            mCircleRadius = (int) (Math.min(mXCenter, mYCenter) * mCircleRadiusMultiplier);

            if (!mIs24HourMode) {
                // We'll need to draw the AM/PM circles, so the main circle will need to have
                // a slightly higher center. To keep the entire view centered vertically, we'll
                // have to push it up by half the radius of the AM/PM circles.
                // AM/PM 원을 그려야 하기 때문에, 메인 원은 조금 더 높은 중심이 필요함
                // 전체 시야를 수직으로 유지하기 위해서, AM/PM 원 반지름의 절반까지 올려야 함
                int amPmCircleRadius = (int) (mCircleRadius * mAmPmCircleRadiusMultiplier);
                mYCenter -= amPmCircleRadius / 2;
            }

            mDrawValuesReady = true;
        }

        // Draw the white circle.
        // 하얀 원을 그림
        mPaint.setColor(mCircleColor);
        canvas.drawCircle(mXCenter, mYCenter, mCircleRadius, mPaint);

        // Draw a small black circle in the center.
        // 중앙에 작은 검정색 원을 그림
        mPaint.setColor(mDotColor);
        canvas.drawCircle(mXCenter, mYCenter, 2, mPaint);
    }
}
