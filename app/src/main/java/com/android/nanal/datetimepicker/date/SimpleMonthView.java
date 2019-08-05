package com.android.nanal.datetimepicker.date;


import android.content.Context;
import android.graphics.Canvas;

class SimpleMonthView extends MonthView {

    public SimpleMonthView(Context context) {
        super(context);
    }

    @Override
    public void drawMonthDay(Canvas canvas, int year, int month, int day,
                             int x, int y, int startX, int stopX, int startY, int stopY) {
        if (mSelectedDay == day) {      // 선택된 날이 매개변수의 날과 같다면 원 그리기
            canvas.drawCircle(x , y - (MINI_DAY_NUMBER_TEXT_SIZE / 3), DAY_SELECTED_CIRCLE_SIZE,
                    mSelectedCirclePaint);
        }

        // If we have a mindate or maxdate, gray out the day number if it's outside the range.
        // 만약 최소 날짜나 최대 날짜가 있다면(즉, 해당 달 밖의 날짜라면), 그 날의 날짜를 회색으로 칠하기
        if (isOutOfRange(year, month, day)) {
            mMonthNumPaint.setColor(mDisabledDayTextColor);
        } else if (mHasToday && mToday == day) {
            mMonthNumPaint.setColor(mTodayNumberColor);
        } else {
            mMonthNumPaint.setColor(mDayTextColor);
        }
        canvas.drawText(String.format("%d", day), x, y, mMonthNumPaint);
    }
}
