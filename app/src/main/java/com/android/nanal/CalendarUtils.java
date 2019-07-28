package com.android.nanal;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarCache;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import java.util.Formatter;
import java.util.HashSet;
import java.util.Locale;

/**
 * A class containing utility methods related to Calendar apps.
 * 캘린더 앱과 관련된 유틸리티 메소드를 포함하는 클래스
 *
 * This class is expected to move into the app framework eventually.
 * 이 클래스는 결국 앱 프레임워크로 옮겨갈 것으로 예상됨
 */
public class CalendarUtils {
    private static final boolean DEBUG = false;
    private static final String TAG = "CalendarUtils";
    public static final String AUTHORITY = "com.android.nanal";

    /**
     * A helper method for writing a String value to the preferences
     * asynchronously.
     * Preference에 문자열 값을 비동기식으로 쓰는 헬퍼 메소드
     *
     * @param prefs A context with access to the correct preferences
     *               올바른 Preference에 액세스할 수 있는 prefs
     * @param key   The preference to write to
     * @param value   The value to write
     */
    public static void setSharedPreference(SharedPreferences prefs, String key, String value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }
    public static void setSharedPreference(SharedPreferences prefs, String key, boolean value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Return a properly configured SharedPreferences instance
     * 올바르게 구성된 SharedPreferences 인스턴스 반환
     */
    public static SharedPreferences getSharedPreferences(Context context, String prefsName) {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }

    /**
     * This class contains methods specific to reading and writing time zone
     * values.
     * 이 클래스는 표준 시간대 값 읽기 및 쓰기 관련 메소드를 포함함
     */
    public static class TimeZoneUtils {
        public static final String[] CALENDAR_CACHE_POJECTION = {
                CalendarCache.KEY, CalendarCache.VALUE
        };
        /**
         * This is the key used for writing whether or not a home time zone should
         * be used in the Calendar app to the Calendar Preferences.
         * 캘린더 앱에서 캘린더 Preference의 홈 시간대 사용 여부를 작성하는 데 사용되는 키
         */
        public static final String KEY_HOME_TZ_ENABLED = "preferences_home_tz_enabled";
        /**
         * This is the key used for writing the time zone that should be used if
         * home time zones are enabled for the Calendar app.
         * 캘린더 앱에서 홈 시간대를 활성화한 경우, 사용해야 하는 시간대를 작성하는 데 사용되는 키
         */
        public static final String KEY_HOME_TZ = "preferences_home_tz";
        private static final String[] TIMEZONE_TYPE_ARGS = {CalendarContract.CalendarCache.KEY_TIMEZONE_TYPE};
        private static final String[] TIMEZONE_INSTANCES_ARGS =
                {CalendarContract.CalendarCache.KEY_TIMEZONE_INSTANCES};
        private static StringBuilder mSB = new StringBuilder(50);
        private static Formatter mF = new Formatter(mSB, Locale.getDefault());
        private volatile static boolean mFirstTZRequest = true;
        private volatile static boolean mTZQueryInProgress = false;
        private volatile static boolean mUseHomeTZ = false;
        private volatile static String mHomeTZ = Time.getCurrentTimezone();
        private static HashSet<Runnable> mTZCallbacks = new HashSet<Runnable>();
        private static int mToken = 1;
        private static AsyncTZHandler mHandler;
        // The name of the shared preferences file. This name must be maintained for historical
        // reasons, as it's what PreferenceManager assigned the first time the file was created.
        // 공유 Preference 파일의 이름
        // 이 이름은 파일이 처음 만들어졌을 때 PreferenceManager가 할당한 것이기 때문에 역사적인 이유로 유지되어야 함
        private final String mPrefsName;

        /**
         * The name of the file where the shared prefs for Calendar are stored
         * must be provided. All activities within an app should provide the
         * same preferences name or behavior may become erratic.
         * 캘린더에 대한 공유된 prefs가 저장되는 파일의 이름을 제공해야 함
         * 앱 내의 모든 활동은 동일한 Preference 이름이나 행동이 불규칙해질 수 있다는 걸 제공해야 함
         *
         * @param prefsName
         */
        public TimeZoneUtils(String prefsName) {
            mPrefsName = prefsName;
        }


        /**
         * Formats a date or a time range according to the local conventions.
         * 지역 관례에 따라 날짜 또는 시간 범위를 형성함
         *
         * This formats a date/time range using Calendar's time zone and the
         * local conventions for the region of the device.
         * 캘린더의 표준 시간대와 장치 영역에 대한 로컬 규칙을 사용하여 날짜/시간 범위를 포맷함
         *
         * If the {@link DateUtils#FORMAT_UTC} flag is used it will pass in
         * the UTC time zone instead.
         * 만약 #FORMAT_UTC 플래그를 사용할 경우 UTC 시간대를 통과함
         *
         * @param context the context is required only if the time is shown
         *                 시간이 표시된 경우에만 컨텍스트가 필요함
         * @param startMillis the start time in UTC milliseconds
         * @param endMillis the end time in UTC milliseconds
         * @param flags a bit mask of options See
         * {@link DateUtils#formatDateRange(Context, Formatter, long, long, int, String) formatDateRange}
         * @return a string containing the formatted date/time range.
         *          포맷된 날짜/시간 범위를 포함하는 문자열
         */
        public String formatDateRange(Context context, long startMillis,
                                      long endMillis, int flags) {
            String date;
            String tz;
            if ((flags & DateUtils.FORMAT_UTC) != 0) {
                tz = Time.TIMEZONE_UTC;
            } else {
                tz = getTimeZone(context, null);
            }
            synchronized (mSB) {
                mSB.setLength(0);
                date = DateUtils.formatDateRange(context, mF, startMillis, endMillis, flags,
                        tz).toString();
            }
            return date;
        }


        /**
         * Writes a new home time zone to the db.
         * 새 홈 시간대를 db에 기록함
         *
         * Updates the home time zone in the db asynchronously and updates
         * the local cache. Sending a time zone of
         * {@link CalendarContract.CalendarCache#TIMEZONE_TYPE_AUTO} will cause it to be set
         * to the device's time zone. null or empty tz will be ignored.
         * db의 홈 시간대를 비동기식으로 업데이트하고 로컬 캐시를 업데이트함
         * #TIMEZONE_TYPE_AUTO의 시간대를 전송하면 장치의 시간대로 설정됨
         * null 또는 비어 있으면 시간대는 무시됨
         *
         * @param context The calling activity
         *                 호출한 액티비티
         * @param timeZone The time zone to set Calendar to, or
         * {@link CalendarContract.CalendarCache#TIMEZONE_TYPE_AUTO}
         *                  캘린더에 설정할 표준 시간대 또는 #TIMEZONE_TYPE_AUTO
         */
        public void setTimeZone(Context context, String timeZone) {
            if (TextUtils.isEmpty(timeZone)) {
                if (DEBUG) {
                    Log.d(TAG, "Empty time zone, nothing to be done.");
                }
                return;
            }
            boolean updatePrefs = false;
            synchronized (mTZCallbacks) {
                if (CalendarContract.CalendarCache.TIMEZONE_TYPE_AUTO.equals(timeZone)) {
                    if (mUseHomeTZ) {
                        updatePrefs = true;
                    }
                    mUseHomeTZ = false;
                } else {
                    if (!mUseHomeTZ || !TextUtils.equals(mHomeTZ, timeZone)) {
                        updatePrefs = true;
                    }
                    mUseHomeTZ = true;
                    mHomeTZ = timeZone;
                }
            }
            if (updatePrefs) {
                // Write the prefs
                SharedPreferences prefs = getSharedPreferences(context, mPrefsName);
                setSharedPreference(prefs, KEY_HOME_TZ_ENABLED, mUseHomeTZ);
                setSharedPreference(prefs, KEY_HOME_TZ, mHomeTZ);

                // Update the db
                ContentValues values = new ContentValues();
                if (mHandler != null) {
                    mHandler.cancelOperation(mToken);
                }

                mHandler = new AsyncTZHandler(context.getContentResolver());

                // skip 0 so query can use it
                // 쿼리가 사용할 수 있도록 0 스킵함
                if (++mToken == 0) {
                    mToken = 1;
                }

                // Write the use home tz setting
                // 사용 홈 시간대 설정 작성
                values.put(CalendarContract.CalendarCache.VALUE, mUseHomeTZ ? CalendarContract.CalendarCache.TIMEZONE_TYPE_HOME
                        : CalendarContract.CalendarCache.TIMEZONE_TYPE_AUTO);
                mHandler.startUpdate(mToken, null, CalendarContract.CalendarCache.URI, values, "key=?",
                        TIMEZONE_TYPE_ARGS);

                // If using a home tz write it to the db
                // 홈 시간대를 사용하는 경우 db에 기록함
                if (mUseHomeTZ) {
                    ContentValues values2 = new ContentValues();
                    values2.put(CalendarContract.CalendarCache.VALUE, mHomeTZ);
                    mHandler.startUpdate(mToken, null, CalendarContract.CalendarCache.URI, values2,
                            "key=?", TIMEZONE_INSTANCES_ARGS);
                }
            }
        }


        /**
         * Gets the time zone that Calendar should be displayed in
         * 캘린더를 표시할 시간대를 가져옴
         *
         * This is a helper method to get the appropriate time zone for Calendar. If this
         * is the first time this method has been called it will initiate an asynchronous
         * query to verify that the data in preferences is correct. The callback supplied
         * will only be called if this query returns a value other than what is stored in
         * preferences and should cause the calling activity to refresh anything that
         * depends on calling this method.
         * 캘린더에 적합한 시간대를 얻기 위한 헬퍼 메소드임
         * 이 메소드가 처음 호출된 경우, Preference의 데이터가 올바른지 확인하기 위해 비동기 쿼리를 시작할 것임
         * 제공된 콜백은 이 쿼리가 Preference에 저장되어 있는 값 이외의 값을 반환하는 경우에만 호출되며,
         * 이 메소드를 호출하는 데 종속된 호출 액티비티를 새로고침해야 함
         *
         * @param context The calling activity
         * @param callback The runnable that should execute if a query returns new values
         *                   쿼리가 새 값을 반환하는 경우 실행할 runnable
         * @return The string value representing the time zone Calendar should display
         *          캘린더가 표시할 표준 시간대를 나타내는 문자열 값
         */
        public String getTimeZone(Context context, Runnable callback) {
            synchronized (mTZCallbacks){
                if (mFirstTZRequest) {
                    mTZQueryInProgress = true;
                    mFirstTZRequest = false;

                    SharedPreferences prefs = getSharedPreferences(context, mPrefsName);
                    mUseHomeTZ = prefs.getBoolean(KEY_HOME_TZ_ENABLED, false);
                    mHomeTZ = prefs.getString(KEY_HOME_TZ, Time.getCurrentTimezone());

                    // When the async query returns it should synchronize on
                    // mTZCallbacks, update mUseHomeTZ, mHomeTZ, and the
                    // preferences, set mTZQueryInProgress to false, and call all
                    // the runnables in mTZCallbacks.
                    // 비동기(async) 쿼리가 반환되면 mTZCallbacks에 동기화,
                    // mUseHomeTZ, mHomeTZ 및 Preference을 업데이트,
                    // TZQueryInProgress를 false로 설정,
                    // mTZCallbacks의 모든 runnable 호출해야 함
                    if (mHandler == null) {
                        mHandler = new AsyncTZHandler(context.getContentResolver());
                    }
                    mHandler.startQuery(0, context, CalendarCache.URI, CALENDAR_CACHE_POJECTION,
                            null, null, null);
                }
                if (mTZQueryInProgress) {
                    mTZCallbacks.add(callback);
                }
            }
            return mUseHomeTZ ? mHomeTZ : Time.getCurrentTimezone();
        }

        /**
         * Forces a query of the database to check for changes to the time zone.
         * This should be called if another app may have modified the db. If a
         * query is already in progress the callback will be added to the list
         * of callbacks to be called when it returns.
         * 데이터베이스 조회를 수행하여 표준 시간대 변경 사항을 확인함
         * 다른 앱이 db를 수정한 경우 이를 호출해야 함
         * 쿼리가 이미 진행 중인 경우, 콜백은 반환될 때 호출될 콜백 목록에 추가됨
         *
         * @param context The calling activity
         * @param callback The runnable that should execute if a query returns
         *            new values
         *                  쿼리가 새 값을 반환하는 경우 실행해야 하는 runnable
         */
        public void forceDBRequery(Context context, Runnable callback) {
            synchronized (mTZCallbacks){
                if (mTZQueryInProgress) {
                    mTZCallbacks.add(callback);
                    return;
                }
                mFirstTZRequest = true;
                getTimeZone(context, callback);
            }
        }
        /**
         * This is a helper class for handling the async queries and updates for the
         * time zone settings in Calendar.
         * 캘린더에서 시간대 설정에 대한 비동기(async) 쿼리 처리와 업데이트를 위한 헬퍼 클래스
         */
        private class AsyncTZHandler extends AsyncQueryHandler {
            public AsyncTZHandler(ContentResolver cr) {
                super(cr);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                synchronized (mTZCallbacks) {
                    if (cursor == null) {
                        mTZQueryInProgress = false;
                        mFirstTZRequest = true;
                        return;
                    }

                    boolean writePrefs = false;
                    // Check the values in the db
                    int keyColumn = cursor.getColumnIndexOrThrow(CalendarContract.CalendarCache.KEY);
                    int valueColumn = cursor.getColumnIndexOrThrow(CalendarContract.CalendarCache.VALUE);
                    while (cursor.moveToNext()) {
                        String key = cursor.getString(keyColumn);
                        String value = cursor.getString(valueColumn);
                        if (TextUtils.equals(key, CalendarContract.CalendarCache.KEY_TIMEZONE_TYPE)) {
                            boolean useHomeTZ = !TextUtils.equals(
                                    value, CalendarContract.CalendarCache.TIMEZONE_TYPE_AUTO);
                            if (useHomeTZ != mUseHomeTZ) {
                                writePrefs = true;
                                mUseHomeTZ = useHomeTZ;
                            }
                        } else if (TextUtils.equals(
                                key, CalendarContract.CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS)) {
                            if (!TextUtils.isEmpty(value) && !TextUtils.equals(mHomeTZ, value)) {
                                writePrefs = true;
                                mHomeTZ = value;
                            }
                        }
                    }
                    cursor.close();
                    if (writePrefs) {
                        SharedPreferences prefs = getSharedPreferences((Context) cookie, mPrefsName);
                        // Write the prefs
                        setSharedPreference(prefs, KEY_HOME_TZ_ENABLED, mUseHomeTZ);
                        setSharedPreference(prefs, KEY_HOME_TZ, mHomeTZ);
                    }

                    mTZQueryInProgress = false;
                    for (Runnable callback : mTZCallbacks) {
                        if (callback != null) {
                            callback.run();
                        }
                    }
                    mTZCallbacks.clear();
                }
            }
        }
    }
}
