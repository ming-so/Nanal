package com.android.nanal.color;

/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

import com.android.nanal.event.Utils;

/**
 * A custom view for a color chip for an event that can be drawn differently
 * accroding to the event's status.
 * 이벤트 상태에 따라 다르게 그릴 수 있는 이벤트의 컬러칩에 대한 사용자 정의 view
 *
 */
public class ColorChipView extends View {

    public static final int DRAW_FULL = 0;
    // Style of drawing, drawing 스타일
    // Full rectangle for accepted events, 허용된 이벤트에 대한 전체 사각형
    // Border for tentative events, 임시 이벤트의 border 경계?
    // Cross-hatched with 50% transparency for declined events, 감소된 이벤트에 대해 투명도가 50%인 cross-hatched..
    public static final int DRAW_BORDER = 1;
    public static final int DRAW_FADED = 2;
    private static final String TAG = "ColorChipView";
    private static final int DEF_BORDER_WIDTH = 4;
    int mBorderWidth = DEF_BORDER_WIDTH;
    int mColor;
    private int mDrawStyle = DRAW_FULL;
    private float mDefStrokeWidth;
    private Paint mPaint;

    public ColorChipView(Context context) {
        super(context);
        init();
    }

    public ColorChipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mDefStrokeWidth = mPaint.getStrokeWidth();
        mPaint.setStyle(Style.FILL_AND_STROKE);
    }


    public void setDrawStyle(int style) {
        if (style != DRAW_FULL && style != DRAW_BORDER && style != DRAW_FADED) {
            return;
        }
        mDrawStyle = style;
        invalidate();
    }

    public void setBorderWidth(int width) {
        if (width >= 0) {
            mBorderWidth = width;
            invalidate();
        }
    }

    public void setColor(int color) {
        mColor = color;
        invalidate();
    }

    @Override
    public void onDraw(Canvas c) {

        int right = getWidth() - 1;
        int bottom = getHeight() - 1;
        mPaint.setColor(mDrawStyle == DRAW_FADED ?
                Utils.getDeclinedColorFromColor(mColor) : mColor);

        switch (mDrawStyle) {
            case DRAW_FADED:
            case DRAW_FULL:
                mPaint.setStrokeWidth(mDefStrokeWidth);
                c.drawRect(0, 0, right, bottom, mPaint);
                break;
            case DRAW_BORDER:
                if (mBorderWidth <= 0) {
                    return;
                }
                int halfBorderWidth = mBorderWidth / 2;
                int top = halfBorderWidth;
                int left = halfBorderWidth;
                mPaint.setStrokeWidth(mBorderWidth);

                float[] lines = new float[16];
                int ptr = 0;
                lines [ptr++] = 0;
                lines [ptr++] = top;
                lines [ptr++] = right;
                lines [ptr++] = top;
                lines [ptr++] = 0;
                lines [ptr++] = bottom - halfBorderWidth;
                lines [ptr++] = right;
                lines [ptr++] = bottom - halfBorderWidth;
                lines [ptr++] = left;
                lines [ptr++] = 0;
                lines [ptr++] = left;
                lines [ptr++] = bottom;
                lines [ptr++] = right - halfBorderWidth;
                lines [ptr++] = 0;
                lines [ptr++] = right - halfBorderWidth;
                lines [ptr++] = bottom;
                c.drawLines(lines, mPaint);
                break;
        }
    }
}
