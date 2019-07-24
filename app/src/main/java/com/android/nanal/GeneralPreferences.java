package com.android.nanal;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceFragment;

public class GeneralPreferences extends PreferenceFragment {
    public static final String KEY_DETAILED_VIEW = "preferred_detailedView";

    public static final int DEFAULT_DETAILED_VIEW = CalendarController.ViewType.DAY;

    static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }
}
