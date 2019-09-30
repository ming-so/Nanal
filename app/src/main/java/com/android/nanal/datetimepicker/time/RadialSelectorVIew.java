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

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

import com.android.nanal.DynamicTheme;
import com.android.nanal.R;
import com.android.nanal.datetimepicker.Utils;
import com.android.nanal.event.GeneralPreferences;

/**
 * View to show what number is selected. This will draw a blue circle over the number, with a blue
 * line coming from the center of the main circle to the edge of the blue selection.
 * 선택된 숫자를 보여 주는 view
 * 숫자 위에 파란색 원을 그리며, 메인 원 중심으로부터 파란 줄
 */
class RadialSelectorView extends View {
    private static final String TAG = "RadialSelectorView";

    // Alpha level for selected circle.
    private static final int SELECTED_ALPHA = Utils.SELECTED_ALPHA;
    private static final int SELECTED_ALPHA_THEME_DARK = Utils.SELECTED_ALPHA_THEME_DARK;
    // Alpha level for the line.
    private static final int FULL_ALPHA = Utils.FULL_ALPHA;

    private final Paint mPaint = new Paint();

    private boolean mIsInitialized;
    private boolean mDrawValuesReady;

    private float mCircleRadiusMultiplier;
    private float mAmPmCircleRadiusMultiplier;
    private float mInnerNumbersRadiusMultiplier;
    private float mOuterNumbersRadiusMultiplier;
    private float mNumbersRadiusMultiplier;
    private float mSelectionRadiusMultiplier;
    private float mAnimationRadiusMultiplier;
    private boolean mIs24HourMode;
    private boolean mHasInnerCircle;
    private int mSelectionAlpha;

    private int mXCenter;
    private int mYCenter;
    private int mCircleRadius;
    private float mTransitionMidRadiusMultiplier;
    private float mTransitionEndRadiusMultiplier;
    private int mLineLength;
    private int mSelectionRadius;
    private InvalidateUpdateListener mInvalidateUpdateListener;

    private int mSelectionDegrees;
    private double mSelectionRadians;
    private boolean mForceDrawDot;

    public RadialSelectorView(Context context) {
        super(context);
        mIsInitialized = false;
    }

    /**
     * Initialize this selector with the state of the picker.
     * picker로 이 selector를 초기화
     * @param context Current context.
     * @param is24HourMode Whether the selector is in 24-hour mode, which will tell us
     * whether the circle's center is moved up slightly to make room for the AM/PM circles.
     *                     selector가 24시간 모드인지, 이 모드는 원의 중심이 AM/PM 원을 위한
     *                     공간을 만들기 위해 약간 위로 이동되어야 하는지를 알림
     * @param hasInnerCircle Whether we have both an inner and an outer circle of numbers
     * that may be selected. Should be true for 24-hour mode in the hours circle.
     *                       선택할 수 있는 숫자의 내부와 외부 원 모두를 가지고 있는지
     *                       24시간 모드의 경우 참이어야 함
     * @param disappearsOut Whether the numbers' animation will have them disappearing out
     * or disappearing in.
     *                       숫자들의 애니메이션이 사라지게 할 것인지(what is in/out?)
     * @param selectionDegrees The initial degrees to be selected.
     *                         선택된 초기 각도
     * @param isInnerCircle Whether the initial selection is in the inner or outer circle.
     * Will be ignored when hasInnerCircle is false.
     *                      초기 선택이 내부 원에 있는지, 외부 원에 있는지
     *                      hasInnerCircle이 false인 경우 무시됨
     */
    public void initialize(Context context, boolean is24HourMode, boolean hasInnerCircle,
                           boolean disappearsOut, int selectionDegrees, boolean isInnerCircle) {
        if (mIsInitialized) {
            Log.e(TAG, "This RadialSelectorView may only be initialized once.");
            return;
        }

        Resources res = context.getResources();
        String selectedColorName = com.android.nanal.event.Utils.getSharedPreference(context, GeneralPreferences.KEY_COLOR_PREF, "teal");
        int blue = res.getColor(DynamicTheme.getColorId(selectedColorName));
        mPaint.setColor(blue);
        mPaint.setAntiAlias(true);
        mSelectionAlpha = SELECTED_ALPHA;

        // Calculate values for the circle radius size.
        // 원의 반지름 사이즈에 대한 값을 계산
        mIs24HourMode = is24HourMode;
        if (is24HourMode) {
            mCircleRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.circle_radius_multiplier_24HourMode));
        } else {
            mCircleRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.circle_radius_multiplier));
            mAmPmCircleRadiusMultiplier =
                    Float.parseFloat(res.getString(R.string.ampm_circle_radius_multiplier));
        }

        // Calculate values for the radius size(s) of the numbers circle(s).
        // 숫자 원의 반지름 크기에 대한 값을 계산
        mHasInnerCircle = hasInnerCircle;
        if (hasInnerCircle) {
            mInnerNumbersRadiusMultiplier =
                    Float.parseFloat(res.getString(R.string.numbers_radius_multiplier_inner));
            mOuterNumbersRadiusMultiplier =
                    Float.parseFloat(res.getString(R.string.numbers_radius_multiplier_outer));
        } else {
            mNumbersRadiusMultiplier =
                    Float.parseFloat(res.getString(R.string.numbers_radius_multiplier_normal));
        }
        mSelectionRadiusMultiplier =
                Float.parseFloat(res.getString(R.string.selection_radius_multiplier));

        // Calculate values for the transition mid-way states.
        // 전환 중간 상태에 대한 값을 계산
        mAnimationRadiusMultiplier = 1;
        mTransitionMidRadiusMultiplier = 1f + (0.05f * (disappearsOut? -1 : 1));
        mTransitionEndRadiusMultiplier = 1f + (0.3f * (disappearsOut? 1 : -1));
        mInvalidateUpdateListener = new InvalidateUpdateListener();

        setSelection(selectionDegrees, isInnerCircle, false);
        mIsInitialized = true;
    }

    /* package */ void setTheme(Context context, boolean themeDark) {
        Resources res = context.getResources();
        String selectedColorName = com.android.nanal.event.Utils.getSharedPreference(context, GeneralPreferences.KEY_COLOR_PREF, "teal");
        int color = res.getColor(DynamicTheme.getColorId(selectedColorName));
        mSelectionAlpha = SELECTED_ALPHA;
        mPaint.setColor(color);
    }

    /**
     * Set the selection.
     * 셀렉션 설정
     * @param selectionDegrees The degrees to be selected.
     *                          선택된 각도
     * @param isInnerCircle Whether the selection should be in the inner circle or outer. Will be
     * ignored if hasInnerCircle was initialized to false.
     *                       셀렉션이 내부 원 안에 있어야 하는지, 외부 원에 있어야 하는지
     *                       hasInnerCircle이 false로 초기화되면 무시됨
     * @param forceDrawDot Whether to force the dot in the center of the selection circle to be
     * drawn. If false, the dot will be drawn only when the degrees is not a multiple of 30, i.e.
     * the selection is not on a visible number.
     *                      선택 원의 중심에 있는 점을 강제로 그릴지
     *                      만약 false라면,  도수가 30의 배수가 아닌 경우, 즉 보이는 숫자가 아닐
     *                      경우에만 점이 그려짐
     */
    public void setSelection(int selectionDegrees, boolean isInnerCircle, boolean forceDrawDot) {
        mSelectionDegrees = selectionDegrees;
        mSelectionRadians = selectionDegrees * Math.PI / 180;
        mForceDrawDot = forceDrawDot;

        if (mHasInnerCircle) {
            if (isInnerCircle) {
                mNumbersRadiusMultiplier = mInnerNumbersRadiusMultiplier;
            } else {
                mNumbersRadiusMultiplier = mOuterNumbersRadiusMultiplier;
            }
        }
    }

    /**
     * Allows for smoother animations.
     * 보다 부드러운 애니메이션 허용
     */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    /**
     * Set the multiplier for the radius. Will be used during animations to move in/out.
     * 반지름에 대한 multiplier 설정
     * 애니메이션 진행 중 들어가고 나오는 데에 사용됨
     */
    public void setAnimationRadiusMultiplier(float animationRadiusMultiplier) {
        mAnimationRadiusMultiplier = animationRadiusMultiplier;
    }

    public int getDegreesFromCoords(float pointX, float pointY, boolean forceLegal,
                                    final Boolean[] isInnerCircle) {
        if (!mDrawValuesReady) {
            return -1;
        }

        double hypotenuse = Math.sqrt(
                (pointY - mYCenter)*(pointY - mYCenter) +
                        (pointX - mXCenter)*(pointX - mXCenter));
        // Check if we're outside the range
        // 범위를 벗어나는지 체크
        if (mHasInnerCircle) {
            if (forceLegal) {
                // If we're told to force the coordinates to be legal, we'll set the isInnerCircle
                // boolean based based off whichever number the coordinates are closer to.
                // 좌표를 강제로 적절하게 변경하라고 하면,
                // 좌표가 어떤 숫자에 더 가까운지를 기준으로 isInnerCircle(boolean 타입) 설정
                int innerNumberRadius = (int) (mCircleRadius * mInnerNumbersRadiusMultiplier);
                int distanceToInnerNumber = (int) Math.abs(hypotenuse - innerNumberRadius);
                int outerNumberRadius = (int) (mCircleRadius * mOuterNumbersRadiusMultiplier);
                int distanceToOuterNumber = (int) Math.abs(hypotenuse - outerNumberRadius);

                isInnerCircle[0] = (distanceToInnerNumber <= distanceToOuterNumber);
            } else {
                // Otherwise, if we're close enough to either number (with the space between the
                // two allotted equally), set the isInnerCircle boolean as the closer one.
                // appropriately, but otherwise return -1.
                // 그렇지 않은 경우, 둘 중 하나의 숫자에 충분히 가깝다면(동등하게 할당된 두 개 사이의 공간을 사용하여)
                // inInnerCircle boolean을 더 가까운 것으로 설정함
                // 적절하지 않다면 -1을 반환
                int minAllowedHypotenuseForInnerNumber =
                        (int) (mCircleRadius * mInnerNumbersRadiusMultiplier) - mSelectionRadius;
                int maxAllowedHypotenuseForOuterNumber =
                        (int) (mCircleRadius * mOuterNumbersRadiusMultiplier) + mSelectionRadius;
                int halfwayHypotenusePoint = (int) (mCircleRadius *
                        ((mOuterNumbersRadiusMultiplier + mInnerNumbersRadiusMultiplier) / 2));

                if (hypotenuse >= minAllowedHypotenuseForInnerNumber &&
                        hypotenuse <= halfwayHypotenusePoint) {
                    isInnerCircle[0] = true;
                } else if (hypotenuse <= maxAllowedHypotenuseForOuterNumber &&
                        hypotenuse >= halfwayHypotenusePoint) {
                    isInnerCircle[0] = false;
                } else {
                    return -1;
                }
            }
        } else {
            // If there's just one circle, we'll need to return -1 if:
            // we're not told to force the coordinates to be legal, and
            // the coordinates' distance to the number is within the allowed distance.
            // 하나의 원만 있다면 -1 리턴
            // 만약 좌표를 강제로 적절하게 변경하라는 말이 없고, 좌표와 숫자의 거리는 허용된 거리 내에 있음
            if (!forceLegal) {
                int distanceToNumber = (int) Math.abs(hypotenuse - mLineLength);
                // The max allowed distance will be defined as the distance from the center of the
                // number to the edge of the circle.
                // 허용되는 최대 거리는 숫자의 중심에서 원의 가장자리까지의 거리로 정의됨
                int maxAllowedDistance = (int) (mCircleRadius * (1 - mNumbersRadiusMultiplier));
                if (distanceToNumber > maxAllowedDistance) {
                    return -1;
                }
            }
        }


        float opposite = Math.abs(pointY - mYCenter);
        double radians = Math.asin(opposite / hypotenuse);
        int degrees = (int) (radians * 180 / Math.PI);

        // Now we have to translate to the correct quadrant.
        // 올바른 사분면으로 변환
        boolean rightSide = (pointX > mXCenter);
        boolean topSide = (pointY < mYCenter);
        if (rightSide && topSide) {
            degrees = 90 - degrees;
        } else if (rightSide && !topSide) {
            degrees = 90 + degrees;
        } else if (!rightSide && !topSide) {
            degrees = 270 - degrees;
        } else if (!rightSide && topSide) {
            degrees = 270 + degrees;
        }
        return degrees;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int viewWidth = getWidth();
        if (viewWidth == 0 || !mIsInitialized) {
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
                // AM/PM 원을 그려야 하기 때문에, 메인 원은 조금 더 높은 중심이 있어야 할 것임
                // 전체 view를 수직으로 정렬한 것을 유지하려면 AM/PM 원의 반경을 위로 밀어야 함
                int amPmCircleRadius = (int) (mCircleRadius * mAmPmCircleRadiusMultiplier);
                mYCenter -= amPmCircleRadius / 2;
            }

            mSelectionRadius = (int) (mCircleRadius * mSelectionRadiusMultiplier);

            mDrawValuesReady = true;
        }

        // Calculate the current radius at which to place the selection circle.
        // 선택 원을 배치할 반지름 계산
        mLineLength = (int) (mCircleRadius * mNumbersRadiusMultiplier * mAnimationRadiusMultiplier);
        int pointX = mXCenter + (int) (mLineLength * Math.sin(mSelectionRadians));
        int pointY = mYCenter - (int) (mLineLength * Math.cos(mSelectionRadians));

        // Draw the selection circle.
        mPaint.setAlpha(mSelectionAlpha);
        canvas.drawCircle(pointX, pointY, mSelectionRadius, mPaint);

        if (mForceDrawDot | mSelectionDegrees % 30 != 0) {
            // We're not on a direct tick (or we've been told to draw the dot anyway).
            // 직접 틱하지 않음(어찌 됐든 점을 찍을 것임?)
            mPaint.setAlpha(FULL_ALPHA);
            canvas.drawCircle(pointX, pointY, (mSelectionRadius * 2 / 7), mPaint);
        } else {
            // We're not drawing the dot, so shorten the line to only go as far as the edge of the
            // selection circle.
            // 점을 그리는 게 아니기 때문에, 선을 줄여서 선택 원의 가장자리까지만
            int lineLength = mLineLength;
            lineLength -= mSelectionRadius;
            pointX = mXCenter + (int) (lineLength * Math.sin(mSelectionRadians));
            pointY = mYCenter - (int) (lineLength * Math.cos(mSelectionRadians));
        }

        // Draw the line from the center of the circle.
        // 원 중앙에 선 그리기
        mPaint.setAlpha(255);
        mPaint.setStrokeWidth(1);
        canvas.drawLine(mXCenter, mYCenter, pointX, pointY, mPaint);
    }

    public ObjectAnimator getDisappearAnimator() {
        if (!mIsInitialized || !mDrawValuesReady) {
            Log.e(TAG, "RadialSelectorView was not ready for animation.");
            return null;
        }

        Keyframe kf0, kf1, kf2;
        float midwayPoint = 0.2f;
        int duration = 500;

        kf0 = Keyframe.ofFloat(0f, 1);
        kf1 = Keyframe.ofFloat(midwayPoint, mTransitionMidRadiusMultiplier);
        kf2 = Keyframe.ofFloat(1f, mTransitionEndRadiusMultiplier);
        PropertyValuesHolder radiusDisappear = PropertyValuesHolder.ofKeyframe(
                "animationRadiusMultiplier", kf0, kf1, kf2);

        kf0 = Keyframe.ofFloat(0f, 1f);
        kf1 = Keyframe.ofFloat(1f, 0f);
        PropertyValuesHolder fadeOut = PropertyValuesHolder.ofKeyframe("alpha", kf0, kf1);

        ObjectAnimator disappearAnimator = ObjectAnimator.ofPropertyValuesHolder(
                this, radiusDisappear, fadeOut).setDuration(duration);
        disappearAnimator.addUpdateListener(mInvalidateUpdateListener);

        return disappearAnimator;
    }

    public ObjectAnimator getReappearAnimator() {
        // 애니메이터 다시 나타내기
        if (!mIsInitialized || !mDrawValuesReady) {
            Log.e(TAG, "RadialSelectorView was not ready for animation.");
            return null;
        }

        Keyframe kf0, kf1, kf2, kf3;
        float midwayPoint = 0.2f;
        int duration = 500;

        // The time points are half of what they would normally be, because this animation is
        // staggered against the disappear so they happen seamlessly. The reappear starts
        // halfway into the disappear.
        // 이 애니메이션은 균일하게 일어나기 때문에, 시간이 보통 하던 것의 절반임(?)

        float delayMultiplier = 0.25f;
        float transitionDurationMultiplier = 1f;
        float totalDurationMultiplier = transitionDurationMultiplier + delayMultiplier;
        int totalDuration = (int) (duration * totalDurationMultiplier);
        float delayPoint = (delayMultiplier * duration) / totalDuration;
        midwayPoint = 1 - (midwayPoint * (1 - delayPoint));

        kf0 = Keyframe.ofFloat(0f, mTransitionEndRadiusMultiplier);
        kf1 = Keyframe.ofFloat(delayPoint, mTransitionEndRadiusMultiplier);
        kf2 = Keyframe.ofFloat(midwayPoint, mTransitionMidRadiusMultiplier);
        kf3 = Keyframe.ofFloat(1f, 1);
        PropertyValuesHolder radiusReappear = PropertyValuesHolder.ofKeyframe(
                "animationRadiusMultiplier", kf0, kf1, kf2, kf3);

        kf0 = Keyframe.ofFloat(0f, 0f);
        kf1 = Keyframe.ofFloat(delayPoint, 0f);
        kf2 = Keyframe.ofFloat(1f, 1f);
        PropertyValuesHolder fadeIn = PropertyValuesHolder.ofKeyframe("alpha", kf0, kf1, kf2);

        ObjectAnimator reappearAnimator = ObjectAnimator.ofPropertyValuesHolder(
                this, radiusReappear, fadeIn).setDuration(totalDuration);
        reappearAnimator.addUpdateListener(mInvalidateUpdateListener);
        return reappearAnimator;
    }

    /**
     * We'll need to invalidate during the animation.
     * 애니메이션 도중 화면 갱신이 필요함
     */
    private class InvalidateUpdateListener implements AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            RadialSelectorView.this.invalidate();
        }
    }
}
