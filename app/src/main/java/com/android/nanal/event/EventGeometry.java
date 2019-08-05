package com.android.nanal.event;

/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.graphics.Rect;

import com.android.nanal.DayView;

public class EventGeometry {
    // This is the space from the grid line to the event rectangle.
    // 그리드 라인에서 이벤트 사각형까지의 공간임
    private int mCellMargin = 0;

    private float mMinuteHeight;

    private float mHourGap;
    private float mMinEventHeight;

    public void setCellMargin(int cellMargin) {
        mCellMargin = cellMargin;
    }

    public void setHourGap(float gap) {
        mHourGap = gap;
    }

    public void setMinEventHeight(float height) {
        mMinEventHeight = height;
    }

    public void setHourHeight(float height) {
        mMinuteHeight = height / 60.0f;
    }

    // Computes the rectangle coordinates of the given event on the screen.
    // Returns true if the rectangle is visible on the screen.
    // 화면에서 주어진 이벤트 사각형의 좌표를 계산함
    // 화면에 사각형이 있다면 true를 반환
    public boolean computeEventRect(int date, int left, int top, int cellWidth, Event event) {
        if (event.drawAsAllday()) {
            return false;
        }

        float cellMinuteHeight = mMinuteHeight;
        int startDay = event.startDay;
        int endDay = event.endDay;

        if (startDay > date || endDay < date) {
            return false;
        }

        int startTime = event.startTime;
        int endTime = event.endTime;

        // If the event started on a previous day, then show it starting
        // at the beginning of this day.
        // 과거에 이벤트가 시작됐다면, 이날 시작부터 보여 줌
        if (startDay < date) {
            startTime = 0;
        }

        // If the event ends on a future day, then show it extending to
        // the end of this day.
        // 미래에 이벤트가 끝난다면, 오늘의 마지막까지 연장해서 보여 줌
        if (endDay > date) {
            endTime = DayView.MINUTES_PER_DAY;
        }

        int col = event.getColumn();
        int maxCols = event.getMaxColumns();
        int startHour = startTime / 60;
        int endHour = endTime / 60;

        // If the end point aligns on a cell boundary then count it as
        // ending in the previous cell so that we don't cross the border
        // between hours.
        // end point가 셀 경계에서 정렬되면, 이전 셀에서 끝나는 것으로 계산해서
        // 시간과 시간 사이의 경계(border)를 넘지 않도록 함
        if (endHour * 60 == endTime)
            endHour -= 1;

        event.top = top;
        event.top += (int) (startTime * cellMinuteHeight);
        event.top += startHour * mHourGap;

        event.bottom = top;
        event.bottom += (int) (endTime * cellMinuteHeight);
        event.bottom += endHour * mHourGap - 1;

        // Make the rectangle be at least mMinEventHeight pixels high
        if (event.bottom < event.top + mMinEventHeight) {
            event.bottom = event.top + mMinEventHeight;
        }

        float colWidth = (float) (cellWidth - (maxCols + 1) * mCellMargin) / (float) maxCols;
        event.left = left + col * (colWidth + mCellMargin);
        event.right = event.left + colWidth;
        return true;
    }

    /**
     * Returns true if this event intersects the selection region.
     * 이 이벤트가 선택 영역과 교차하는 경우 true 반환
     */
    public boolean eventIntersectsSelection(Event event, Rect selection) {
        if (event.left < selection.right && event.right >= selection.left
                && event.top < selection.bottom && event.bottom >= selection.top) {
            return true;
        }
        return false;
    }

    /**
     * Computes the distance from the given point to the given event.
     * 주어진 지점부터 주어진 이벤트까지의 거리 계산
     */
    public float pointToEvent(float x, float y, Event event) {
        float left = event.left;
        float right = event.right;
        float top = event.top;
        float bottom = event.bottom;

        if (x >= left) {
            if (x <= right) {
                if (y >= top) {
                    if (y <= bottom) {
                        // x,y is inside the event rectangle
                        // x,y는 이벤트 사각형 안에 있음
                        return 0f;
                    }
                    // x,y is below the event rectangle
                    // x,y는 이벤트 사각형 아래에 있음
                    return y - bottom;
                }
                // x,y is above the event rectangle
                // x,y는 이벤트 사각형 위에 있음
                return top - y;
            }

            // x > right
            float dx = x - right;
            if (y < top) {
                // the upper right corner
                // 오른쪽 위 모서리
                float dy = top - y;
                return (float) Math.sqrt(dx * dx + dy * dy);
            }
            if (y > bottom) {
                // the lower right corner
                // 오른쪽 아래 모서리
                float dy = y - bottom;
                return (float) Math.sqrt(dx * dx + dy * dy);
            }
            // x,y is to the right of the event rectangle
            // x,y가 이벤트 사각형의 오른쪽에 있음?
            return dx;
        }
        // x < left
        float dx = left - x;
        if (y < top) {
            // the upper left corner
            float dy = top - y;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
        if (y > bottom) {
            // the lower left corner
            float dy = y - bottom;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
        // x,y is to the left of the event rectangle
        return dx;
    }
}
