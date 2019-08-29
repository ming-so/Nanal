package com.android.nanal.diary;

import android.graphics.Rect;

public class DiaryGeometry {
    private int mCellMargin = 0;

    private float mMinuteHeight;

    private float mHourGap;
    private float mMinDiaryHeight;

    public void setCellMargin(int cellMargin) {
        mCellMargin = cellMargin;
    }

    public void setHourGap(float gap) {
        mHourGap = gap;
    }

    public void setMinDiaryHeight(float height) {
        mMinDiaryHeight = height;
    }

    public void setHourHeight(float height) {
        mMinuteHeight = height / 60.0f;
    }

    // Computes the rectangle coordinates of the given event on the screen.
    // Returns true if the rectangle is visible on the screen.
    // 화면에서 주어진 이벤트 사각형의 좌표를 계산함
    // 화면에 사각형이 있다면 true를 반환
    public boolean computeDiaryRect(int date, int left, int top, int cellWidth, Diary diary) {
        float cellMinuteHeight = mMinuteHeight;
        long day = diary.day;
        int col = diary.getColumn();
        int maxCols = diary.getMaxColumns();

        diary.top = top;
        diary.top += (long) (day * cellMinuteHeight);

        float colWidth = (float) (cellWidth - (maxCols + 1) * mCellMargin) / (float) maxCols;
        diary.left = left + col * (colWidth + mCellMargin);
        diary.right = diary.left + colWidth;
        return true;
    }

    /**
     * Returns true if this event intersects the selection region.
     * 이 이벤트가 선택 영역과 교차하는 경우 true 반환
     */
    public boolean diaryIntersectsSelection(Diary event, Rect selection) {
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
    public float pointToEvent(float x, float y, Diary event) {
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
