package com.android.nanal.datetimepicker;

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
import android.annotation.SuppressLint;
import android.os.Build;
import android.text.format.Time;
import android.view.View;

import java.util.Calendar;

/**
 *
 * Utility helper functions for time and date pickers.
 * 시간과 데이터 픽커를 위한 유틸리티 헬퍼 함수
 * @deprecated This module is deprecated. Do not use this class.
 */
@Deprecated
public class Utils {

    public static final int MONDAY_BEFORE_JULIAN_EPOCH = Time.EPOCH_JULIAN_DAY - 3;
    public static final int PULSE_ANIMATOR_DURATION = 544;

    // Alpha level for time picker selection.
    // 타임 피커에 대한 알파값
    public static final int SELECTED_ALPHA = 51;
    public static final int SELECTED_ALPHA_THEME_DARK = 102;
    // Alpha level for fully opaque.
    // 불투명한 경우의 알파값
    public static final int FULL_ALPHA = 255;


    static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";

    public static boolean isJellybeanOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Try to speak the specified text, for accessibility. Only available on JB or later.
     * 접근성을 위해 지정된 텍스트를 말하기, 젤리빈 이상에서만 사용 가능
     * @param text Text to announce.
     */
    @SuppressLint("NewApi")
    public static void tryAccessibilityAnnounce(View view, CharSequence text) {
        if (isJellybeanOrLater() && view != null && text != null) {
            view.announceForAccessibility(text);
        }
    }

    public static int getDaysInMonth(int month, int year) {
        switch (month) {
            case Calendar.JANUARY:
            case Calendar.MARCH:
            case Calendar.MAY:
            case Calendar.JULY:
            case Calendar.AUGUST:
            case Calendar.OCTOBER:
            case Calendar.DECEMBER:
                return 31;
            case Calendar.APRIL:
            case Calendar.JUNE:
            case Calendar.SEPTEMBER:
            case Calendar.NOVEMBER:
                return 30;
            case Calendar.FEBRUARY:
                return (year % 4 == 0) ? 29 : 28;
            default:
                throw new IllegalArgumentException("Invalid Month");
        }
    }

    /**
     * Takes a number of weeks since the epoch and calculates the Julian day of
     * the Monday for that week.
     * epoch로부터 몇 주가 걸리며 그 주 월요일의 줄리안 데이를 계산
     *
     * This assumes that the week containing the {@link Time#EPOCH_JULIAN_DAY}
     * is considered week 0. It returns the Julian day for the Monday
     * {@code week} weeks after the Monday of the week containing the epoch.
     *
     * 이는 {}가 포함된 주를 0번째 주로 간주한다고 가정함
     * epoch가 포함된 주의 월요일 이후 {}주 동안의 줄리안 데이를 반환
     *
     * @param week Number of weeks since the epoch
     *              epoch 이후 주 수
     * @return The julian day for the Monday of the given week since the epoch
     *          epoch 이후 주어진 주의 월요일의 줄리안 데이
     */
    public static int getJulianMondayFromWeeksSinceEpoch(int week) {
        return MONDAY_BEFORE_JULIAN_EPOCH + week * 7;
    }

    /**
     * Returns the week since {@link Time#EPOCH_JULIAN_DAY} (Jan 1, 1970)
     * adjusted for first day of week.
     * 1970-01-01 이후 주의 첫 번째 날로 정의된 그 주를 리턴
     *
     * This takes a julian day and the week start day and calculates which
     * week since {@link Time#EPOCH_JULIAN_DAY} that day occurs in, starting
     * at 0. *Do not* use this to compute the ISO week number for the year.
     *
     * 줄리안 데이와 주 시작 요일이 필요하고, 해당 날짜(epoch)의 주부터 0으로 시작하여 계산
     * 연도의 ISO 주를 계산하는 데 사용 금지
     *
     *
     * @param julianDay The julian day to calculate the week number for
     *                    주 수를 계산할 줄리안 데이
     * @param firstDayOfWeek Which week day is the first day of the week,
     *                        어느 날이 주의 첫 번째 날인지
     *          see {@link Time#SUNDAY}
     * @return Weeks since the epoch
     */
    public static int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
        int diff = Time.THURSDAY - firstDayOfWeek;
        if (diff < 0) {
            diff += 7;
        }
        int refDay = Time.EPOCH_JULIAN_DAY - diff;
        return (julianDay - refDay) / 7;
    }

    /**
     * Render an animator to pulsate a view in place.
     * 뷰에 펄스 애니메이션을 렌더링
     *
     * @param labelToAnimate the view to pulsate.
     * @return The animator object. Use .start() to begin.
     */
    public static ObjectAnimator getPulseAnimator(View labelToAnimate, float decreaseRatio,
                                                  float increaseRatio) {
        Keyframe k0 = Keyframe.ofFloat(0f, 1f);
        Keyframe k1 = Keyframe.ofFloat(0.275f, decreaseRatio);
        Keyframe k2 = Keyframe.ofFloat(0.69f, increaseRatio);
        Keyframe k3 = Keyframe.ofFloat(1f, 1f);

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofKeyframe("scaleX", k0, k1, k2, k3);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofKeyframe("scaleY", k0, k1, k2, k3);
        ObjectAnimator pulseAnimator =
                ObjectAnimator.ofPropertyValuesHolder(labelToAnimate, scaleX, scaleY);
        pulseAnimator.setDuration(PULSE_ANIMATOR_DURATION);

        return pulseAnimator;
    }
}

