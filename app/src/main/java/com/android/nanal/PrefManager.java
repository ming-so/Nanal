package com.android.nanal;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Lincoln on 05/05/16.
 */
public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;

    // shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "androidhive-welcome";

    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    private static final String IS_CALENDAR_CREATED = "IsCalendarCreated";
    private static final String IS_DB_CREATED = "IsDBCreated";

    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setCalendarCreated(boolean isCreated) {
        editor.putBoolean(IS_CALENDAR_CREATED, isCreated);
        editor.commit();
    }

    public boolean isCalendarCreated() {
        return pref.getBoolean(IS_CALENDAR_CREATED, false);
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    public void setDBCreated(boolean isCreated) {
        editor.putBoolean(IS_DB_CREATED, isCreated);
        editor.commit();
    }

    public boolean isDBCreated() { return pref.getBoolean(IS_DB_CREATED, false); }
}