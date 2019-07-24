package com.android.nanal;

import android.content.Context;
import android.text.format.Time;

public class CalendarUtils {
    public static class TimeZoneUtils {
        private final String mPrefsName;
        public String getTimeZone(Context context, Runnable callback) {
            return Time.getCurrentTimezone();
        }

        public TimeZoneUtils(String prefsName) {
            mPrefsName = prefsName;
        }
    }

}
