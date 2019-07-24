package com.android.nanal;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.nanal.CalendarUtils.TimeZoneUtils;

public class Utils {
    static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";
    private static final TimeZoneUtils mTZUtils = new CalendarUtils.TimeZoneUtils(SHARED_PREFS_NAME);

    public void returnToCalendarHome(Context context) {

    }

    public static int getSharedPreference(Context context, String key, int defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getInt(key, defaultValue);
    }

    public static String getTimeZone(Context context, Runnable callback) {
        return mTZUtils.getTimeZone(context, callback);
    }
}