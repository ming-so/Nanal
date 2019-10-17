// 다 옮김!
package com.android.nanal.event;

import android.accounts.Account;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract.Calendars;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;

import com.android.nanal.DayOfMonthDrawable;
import com.android.nanal.R;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.calendar.CalendarController.ViewType;
import com.android.nanal.calendar.CalendarEventModel.ReminderEntry;
import com.android.nanal.calendar.CalendarUtils.TimeZoneUtils;
import com.android.nanal.diary.Diary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.widget.SearchView;

import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;

public class Utils {
    // Set to 0 until we have UI to perform undo
    // 실행 취소를 수행할 UI가 있을 때까지 0으로 설정함
    public static final long UNDO_DELAY = 0;
    // For recurring events which instances of the series are being modified
    // 시리즈series의 인스턴스를 수정하는 반복 이벤트의 경우
    public static final int MODIFY_UNINITIALIZED = 0;
    public static final int MODIFY_SELECTED = 1;
    public static final int MODIFY_ALL_FOLLOWING = 2;
    public static final int MODIFY_ALL = 3;
    // When the edit event view finishes it passes back the appropriate exit code.
    // 편집 이벤트 view가 완료되면 해당 종료 코드를 재전송함
    public static final int DONE_REVERT = 1;
    public static final int DONE_SAVE = 1 << 1;
    public static final int DONE_DELETE = 1 << 2;
    // And should re run with DONE_EXIT if it should also leave the view, just
    // exiting is identical to reverting
    // DONE_EXIT로 다시 실행해야 함
    // 만약 view에서 벗어나야 한다면, 그냥 나가는 것은 되돌리는 것과 동일함
    public static final int DONE_EXIT = 1;
    public static final String OPEN_EMAIL_MARKER = " <";
    public static final String CLOSE_EMAIL_MARKER = ">";
    public static final String INTENT_KEY_DETAIL_VIEW = "DETAIL_VIEW";
    public static final String INTENT_KEY_VIEW_TYPE = "VIEW";
    public static final String INTENT_VALUE_VIEW_TYPE_DAY = "DAY";
    public static final String INTENT_KEY_HOME = "KEY_HOME";
    public static final int MONDAY_BEFORE_JULIAN_EPOCH = Time.EPOCH_JULIAN_DAY - 3;
    public static final int DECLINED_EVENT_ALPHA = 0x66;
    public static final int DECLINED_EVENT_TEXT_ALPHA = 0xC0;
    public static final int YEAR_MIN = 1970;
    public static final int YEAR_MAX = 2036;
    public static final String KEY_QUICK_RESPONSES = "preferences_quick_responses";
    public static final String KEY_ALERTS_VIBRATE_WHEN = "preferences_alerts_vibrateWhen";
    public static final String APPWIDGET_DATA_TYPE = "vnd.android.data/update";
    // Defines used by the DNA generation code
    // DNA 생성 코드에 사용되는 정의
    static final int DAY_IN_MINUTES = 60 * 24;
    static final int WEEK_IN_MINUTES = DAY_IN_MINUTES * 7;
    // The name of the shared preferences file. This name must be maintained for
    // historical
    // reasons, as it's what PreferenceManager assigned the first time the file
    // was created.
    // 공유 Preference 파일의 이름 PreferenceManager가 할당했던 것이기 때문에 유지되어야 함
    static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";
    static final String MACHINE_GENERATED_ADDRESS = "calendar.google.com";
    private static final boolean DEBUG = false;
    private static final String TAG = "CalUtils";
    private static final float SATURATION_ADJUST = 1.3f;
    private static final float INTENSITY_ADJUST = 0.8f;
    private static final TimeZoneUtils mTZUtils = new TimeZoneUtils(SHARED_PREFS_NAME);
    private static final Pattern mWildcardPattern = Pattern.compile("^.*$");


    /**
     * A coordinate must be of the following form for Google Maps to correctly use it:
     * Latitude, Longitude
     * 좌표는 Google 지도에서 올바르게 사용하려면 위도, 경도의 형식이어야 함
     *
     * This may be in decimal form: 십진법으로
     * Latitude: {-90 to 90} 위도
     * Longitude: {-180 to 180} 경도
     *
     * Or, in degrees, minutes, and seconds: 또는 도, 분, 초로
     * Latitude: {-90 to 90}° {0 to 59}' {0 to 59}"
     * Latitude: {-180 to 180}° {0 to 59}' {0 to 59}"
     * + or - degrees may also be represented with N or n, S or s for latitude, and with
     * E or e, W or w for longitude, where the direction may either precede or follow the value.
     * + 또는 -도 또한 위도에 대해 N 또는 n, S 또는 s로 나타낼 수 있으며
     * 경도의 E, e, W, w 경우, 방향이 값 앞이나 뒤에 올 수 있음
     *
     * Some examples of coordinates that will be accepted by the regex:
     * 37.422081°, -122.084576°
     * 37.422081,-122.084576
     * +37°25'19.49", -122°5'4.47"
     * 37°25'19.49"N, 122°5'4.47"W
     * N 37° 25' 19.49",  W 122° 5' 4.47"
     **/
    private static final String COORD_DEGREES_LATITUDE =
            "([-+NnSs]" + "(\\s)*)?"
                    + "[1-9]?[0-9](\u00B0)" + "(\\s)*"
                    + "([1-5]?[0-9]\')?" + "(\\s)*"
                    + "([1-5]?[0-9]" + "(\\.[0-9]+)?\")?"
                    + "((\\s)*" + "[NnSs])?";
    private static final String COORD_DEGREES_LONGITUDE =
            "([-+EeWw]" + "(\\s)*)?"
                    + "(1)?[0-9]?[0-9](\u00B0)" + "(\\s)*"
                    + "([1-5]?[0-9]\')?" + "(\\s)*"
                    + "([1-5]?[0-9]" + "(\\.[0-9]+)?\")?"
                    + "((\\s)*" + "[EeWw])?";
    private static final String COORD_DEGREES_PATTERN =
            COORD_DEGREES_LATITUDE
                    + "(\\s)*" + "," + "(\\s)*"
                    + COORD_DEGREES_LONGITUDE;
    private static final String COORD_DECIMAL_LATITUDE =
            "[+-]?"
                    + "[1-9]?[0-9]" + "(\\.[0-9]+)"
                    + "(\u00B0)?";
    private static final String COORD_DECIMAL_LONGITUDE =
            "[+-]?"
                    + "(1)?[0-9]?[0-9]" + "(\\.[0-9]+)"
                    + "(\u00B0)?";
    private static final String COORD_DECIMAL_PATTERN =
            COORD_DECIMAL_LATITUDE
                    + "(\\s)*" + "," + "(\\s)*"
                    + COORD_DECIMAL_LONGITUDE;
    private static final Pattern COORD_PATTERN =
            Pattern.compile(COORD_DEGREES_PATTERN + "|" + COORD_DECIMAL_PATTERN);
    private static final String NANP_ALLOWED_SYMBOLS = "()+-*#.";
    private static final int NANP_MIN_DIGITS = 7;
    private static final int NANP_MAX_DIGITS = 11;
    // Using int constants as a return value instead of an enum to minimize resources.
    // 자원을 최소화하기 위해 열거값 대신 int 상수를 반환값으로 사용
    private static final int TODAY = 1;
    private static final int TOMORROW = 2;
    private static final int NONE = 0;
    // The work day is being counted as 6am to 8pm
    // 근무일?은 6시에서 8시까지로 계산되고 있음
    static int WORK_DAY_MINUTES = 14 * 60;
    static int WORK_DAY_START_MINUTES = 6 * 60;
    static int WORK_DAY_END_MINUTES = 20 * 60;
    static int WORK_DAY_END_LENGTH = (24 * 60) - WORK_DAY_END_MINUTES;
    static int CONFLICT_COLOR = 0xFF000000;
    static boolean mMinutesLoaded = false;
    private static boolean mAllowWeekForDetailView = false;
    private static long mTardis = 0;
    private static String sVersion = null;


    /**
     * Returns whether the SDK is the Oreo release or later.
     * SDK가 Oreo 릴리즈인지 이후 버전인지 반환함
     */
    public static boolean isOreoOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static int getViewTypeFromIntentAndSharedPref(Activity activity) {
        Intent intent = activity.getIntent();
        Bundle extras = intent.getExtras();
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(activity);

        if (TextUtils.equals(intent.getAction(), Intent.ACTION_EDIT)) {
            return ViewType.EDIT;
        }
        if (extras != null) {
            if (extras.getBoolean(INTENT_KEY_DETAIL_VIEW, false)) {
                // This is the "detail" view which is either agenda or day view
                // Agenda 또는 일 view의 "상세" view임
                return prefs.getInt(GeneralPreferences.KEY_DETAILED_VIEW,
                        GeneralPreferences.DEFAULT_DETAILED_VIEW);
            } else if (INTENT_VALUE_VIEW_TYPE_DAY.equals(extras.getString(INTENT_KEY_VIEW_TYPE))) {
                // Not sure who uses this. This logic came from LaunchActivity
                // 누가 사용했는지 확실치 않음, LaunchActivity에서 비롯된 논리?
                return ViewType.DAY;
            }
        }

        // Check if the user wants the last view or the default startup view
        // 사용자가 마지막 view 또는 기본 시작 view를 원하는지 확인
        int defaultStart = Integer.valueOf(prefs.getString(GeneralPreferences.KEY_DEFAULT_START,
                GeneralPreferences.DEFAULT_DEFAULT_START));
        if (defaultStart == -2) {
            // Return the last view used
            return prefs.getInt(
                    GeneralPreferences.KEY_START_VIEW, GeneralPreferences.DEFAULT_START_VIEW);
        } else {
            // Return the default view
            return defaultStart;
        }
    }

    /**
     * Gets the intent action for telling the widget to update.
     * 위젯을 업데이트하도록 지시하기 위한 intent action 가져옴
     */
    public static String getWidgetUpdateAction(Context context) {
        //return "com.android.calendar.APPWIDGET_UPDATE";
        return "com.android.nanal.APPWIDGET_UPDATE";
    }

    /**
     * Gets the intent action for telling the widget to update.
     * 위젯을 업데이트하도록 지시하기 위한 intent action 가져옴
     */
    public static String getWidgetScheduledUpdateAction(Context context) {
        //return "com.android.calendar.APPWIDGET_SCHEDULED_UPDATE";
        return "com.android.nanal.APPWIDGET_SCHEDULED_UPDATE";
    }


    /**
     * Gets the intent action for telling the widget to update.
     * 위젯을 업데이트하도록 지시하기 위한 intent action 가져옴
     */
    public static String getSearchAuthority(Context context) {
        //return "com.android.calendar.CalendarRecentSuggestionsProvider";
        return "com.android.nanal.CalendarRecentSuggestionsProvider";
    }

    /**
     * Writes a new home time zone to the db. Updates the home time zone in the
     * db asynchronously and updates the local cache. Sending a time zone of
     * **tbd** will cause it to be set to the device's time zone. null or empty
     * tz will be ignored.
     * 새 홈 시간대를 db에 기록함
     * db의 홈 시간대를 비동기식으로 업데이트하고 로컬 캐시를 업데이트함
     * 표준 시간대를 **tbd**로 보내면 기기의 표준 시간대로 설정됨
     * null 또는 비어 있는 timeZone은 무시됨
     *
     * @param context The calling activity
     * @param timeZone The time zone to set Calendar to, or **tbd**
     */
    public static void setTimeZone(Context context, String timeZone) {
        mTZUtils.setTimeZone(context, timeZone);
    }


    /**
     * Gets the time zone that Calendar should be displayed in This is a helper
     * method to get the appropriate time zone for Calendar. If this is the
     * first time this method has been called it will initiate an asynchronous
     * query to verify that the data in preferences is correct. The callback
     * supplied will only be called if this query returns a value other than
     * what is stored in preferences and should cause the calling activity to
     * refresh anything that depends on calling this method.
     * 캘린더를 표시할 시간대를 가져옴, 캘린더에 적절한 시간대를 가져오기 위한 헬퍼 메소드임
     * 이 메소드가 처음 호출된 경우, Preference의 데이터가 올바른지 확인하기 위해 비동기
     * 쿼리를 시작할 것임
     * 매개변수의 콜백은 이 쿼리가 Preference에 저장되어 있는 값 이외를 반환하는 경우에만 호출되며,
     * 이 방법을 호출하는 데 종속된 호출된 activity를 새로 고쳐야 함
     *
     * @param context The calling activity
     *                호출 activity
     * @param callback The runnable that should execute if a query returns new
     *            values
     *                 쿼리가 새 값을 반환하는 경우 실행할 runnable
     * @return The string value representing the time zone Calendar should
     *         display
     *         표준 시간대 캘린더를 나타내는 string 값
     */
    public static String getTimeZone(Context context, Runnable callback) {
        return mTZUtils.getTimeZone(context, callback);
    }


    /**
     * Formats a date or a time range according to the local conventions.
     * 지역 관례에 따라 날짜 또는 시간 범위를 포맷함
     *
     * @param context the context is required only if the time is shown
     *                시간이 표시된 경우에면 context가 필요함
     * @param startMillis the start time in UTC milliseconds
     *                    시작 시간(UTC, 밀리초)
     * @param endMillis the end time in UTC milliseconds
     *                  종료 시간(UTC, 밀리초)
     * @param flags a bit mask of options See {@ link DateUtils#formatDateRange(Context, Formatter,
     * long, long, int, String) formatDateRange}
     *              옵션의 비트마스크 link 확인
     * @return a string containing the formatted date/time range.
     *          포맷된 날짜/시간 범위를 포함하는 문자열
     */
    public static String formatDateRange(
            Context context, long startMillis, long endMillis, int flags) {
        return mTZUtils.formatDateRange(context, startMillis, endMillis, flags);
    }

    public static boolean getDefaultVibrate(Context context, SharedPreferences prefs) {
        boolean vibrate;
        if (prefs.contains(KEY_ALERTS_VIBRATE_WHEN)) {
            // Migrate setting to new 4.2 behavior
            //
            // silent and never -> off
            // always -> on
            String vibrateWhen = prefs.getString(KEY_ALERTS_VIBRATE_WHEN, null);
            vibrate = vibrateWhen != null && vibrateWhen.equals(context
                    .getString(R.string.prefDefault_alerts_vibrate_true));
            prefs.edit().remove(KEY_ALERTS_VIBRATE_WHEN).commit();
            Log.d(TAG, "Migrating KEY_ALERTS_VIBRATE_WHEN(" + vibrateWhen
                    + ") to KEY_ALERTS_VIBRATE = " + vibrate);
        } else {
            vibrate = prefs.getBoolean(GeneralPreferences.KEY_ALERTS_VIBRATE,
                    false);
        }
        return vibrate;
    }


    public static String[] getSharedPreference(Context context, String key, String[] defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        Set<String> ss = prefs.getStringSet(key, null);
        if (ss != null) {
            String strings[] = new String[ss.size()];
            return ss.toArray(strings);
        }
        return defaultValue;
    }


    public static String getSharedPreference(Context context, String key, String defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static int getSharedPreference(Context context, String key, int defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getInt(key, defaultValue);
    }

    public static boolean getSharedPreference(Context context, String key, boolean defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getBoolean(key, defaultValue);
    }


    /**
     * Asynchronously sets the preference with the given key to the given value
     * 지정된 키로 Preference을 지정한 값을 설정함(비동기식)
     * @param context the context to use to get preferences from
     *                preference를 얻기 위해 사용할 context
     * @param key the key of the preference to set
     *            설정할 preference의 키
     * @param value the value to set
     *              설정할 값
     */
    public static void setSharedPreference(Context context, String key, String value) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        prefs.edit().putString(key, value).apply();
    }

    public static void setSharedPreference(Context context, String key, String[] values) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (String value : values) {
            set.add(value);
        }
        prefs.edit().putStringSet(key, set).apply();
    }


    public static void tardis() {
        mTardis = System.currentTimeMillis();
    }

    public static long getTardis() {
        return mTardis;
    }

    public static void setSharedPreference(Context context, String key, boolean value) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void setSharedPreference(Context context, String key, int value) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void removeSharedPreference(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(
                GeneralPreferences.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(key).apply();
    }


    // The backed up ring tone preference should not used because it is a device
    // specific Uri. The preference now lives in a separate non-backed-up
    // shared_pref file (SHARED_PREFS_NAME_NO_BACKUP). The preference in the old
    // backed-up shared_pref file (SHARED_PREFS_NAME) is used only to control the
    // default value when the ringtone dialog opens up.
    // 백업된 벨소리 preference는 장치별 URI이기 때문에 사용해서는 안 됨
    // 현재 preference는 백업되지 않은 별도의 shared_pref 파일(SHARED_PREFS_NAME_NO_BACKUP)에 저장되어 있음
    // 이전에 백업된 shared_pref 파일(SHARED_PREFS_NAME)의 preference은 벨소리 대화상자가 열릴 때
    // 기본값을 제어하는 데만 사용됨
    //
    // At backup manager "restore" time (which should happen before launcher
    // comes up for the first time), the value will be set/reset to default
    // ringtone.
    // 백업 관리자 "복원" 시간 (런처가 처음 나오기 전에? 발생해야 함)에서 이 값은 기본 벨소리로
    // 설정/재설정됨
    public static String getRingTonePreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                GeneralPreferences.SHARED_PREFS_NAME_NO_BACKUP, Context.MODE_PRIVATE);
        String ringtone = prefs.getString(GeneralPreferences.KEY_ALERTS_RINGTONE, null);

        // If it hasn't been populated yet, that means new code is running for
        // the first time and restore hasn't happened. Migrate value from
        // backed-up shared_pref to non-shared_pref.
        // 아직 채워지지 않았다면, 새로운 코드가 처음으로 실행되고 복원이 일어나지 않았다는 것을 의미함
        // 백업된 shared_pref에서 non-shared_pref로 값을 마이그레이션함
        if (ringtone == null) {
            // Read from the old place with a default of DEFAULT_RINGTONE
            // 기본값인 DEFAULT_RINGTONE을 사용하여 이전 위치에서 읽기
            ringtone = getSharedPreference(context, GeneralPreferences.KEY_ALERTS_RINGTONE,
                    GeneralPreferences.DEFAULT_RINGTONE);

            // Write it to the new place
            // 새 위치에 씀
            setRingTonePreference(context, ringtone);
        }

        return ringtone;
    }


    public static void setRingTonePreference(Context context, String value) {
        SharedPreferences prefs = context.getSharedPreferences(
                GeneralPreferences.SHARED_PREFS_NAME_NO_BACKUP, Context.MODE_PRIVATE);
        prefs.edit().putString(GeneralPreferences.KEY_ALERTS_RINGTONE, value).apply();
    }


    /**
     * Save default agenda/day/week/month view for next time
     * 다음에 대한 기본 agenda/일/주/월 view 저장
     *
     * @param context
     * @param viewId {@link ViewType}
     */
    public static void setDefaultView(Context context, int viewId) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        boolean validDetailView = false;
        if (mAllowWeekForDetailView && viewId == ViewType.WEEK) {
            validDetailView = true;
        } else {
            validDetailView = viewId == ViewType.AGENDA
                    || viewId == ViewType.DAY;
        }

        if (validDetailView) {
            // Record the detail start view
            editor.putInt(GeneralPreferences.KEY_DETAILED_VIEW, viewId);
        }

        // Record the (new) start view
        // (새) 시작 view 기록
        editor.putInt(GeneralPreferences.KEY_START_VIEW, viewId);
        editor.apply();
    }

    public static MatrixCursor matrixCursorFromCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        String[] columnNames = cursor.getColumnNames();
        if (columnNames == null) {
            columnNames = new String[] {};
        }
        MatrixCursor newCursor = new MatrixCursor(columnNames);
        int numColumns = cursor.getColumnCount();
        String data[] = new String[numColumns];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            for (int i = 0; i < numColumns; i++) {
                data[i] = cursor.getString(i);
            }
            newCursor.addRow(data);
        }
        return newCursor;
    }


    /**
     * Compares two cursors to see if they contain the same data.
     * 두 커서를 비교하여 동일한 데이터를 포함하는지 확인함
     *
     * @return Returns true of the cursors contain the same data and are not
     *         null, false otherwise
     *         동일한 데이터를 포함하면 true, 그렇지 않으면 false
     */
    public static boolean compareCursors(Cursor c1, Cursor c2) {
        if (c1 == null || c2 == null) {
            return false;
        }

        int numColumns = c1.getColumnCount();
        if (numColumns != c2.getColumnCount()) {
            return false;
        }

        if (c1.getCount() != c2.getCount()) {
            return false;
        }

        c1.moveToPosition(-1);
        c2.moveToPosition(-1);
        while (c1.moveToNext() && c2.moveToNext()) {
            for (int i = 0; i < numColumns; i++) {
                if (!TextUtils.equals(c1.getString(i), c2.getString(i))) {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * If the given intent specifies a time (in milliseconds since the epoch),
     * then that time is returned. Otherwise, the current time is returned.
     * 주어진 Intent가 시간(epoch 이후 밀리초 단위)을 지정하면 그 시간이 반환됨
     * 그렇지 않으면 현재 시간이 반환됨
     */
    public static final long timeFromIntentInMillis(Intent intent) {
        // If the time was specified, then use that. Otherwise, use the current
        // time.
        // 시간이 지정된 경우, 해당 시간 사용함
        // 그렇지 않으면 현재 시간을 사용함
        Uri data = intent.getData();
        long millis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
        if (millis == -1 && data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("time")) {
                try {
                    millis = Long.valueOf(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    Log.i("Calendar", "timeFromIntentInMillis: Data existed but no valid time "
                            + "found. Using current time.");
                }
            }
        }
        if (millis <= 0) {
            millis = System.currentTimeMillis();
        }
        return millis;
    }

    /**
     * Formats the given Time object so that it gives the month and year (for
     * example, "September 2007").
     * 주어진 Time 객체가 월과 년("September 2007")을 제공하도록 포맷함
     *
     * @param time the time to format
     * @return the string containing the weekday and the date
     */
    public static String formatMonthYear(Context context, Time time) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                | DateUtils.FORMAT_SHOW_YEAR;
        long millis = time.toMillis(true);
        return formatDateRange(context, millis, millis, flags);
    }



    /**
     * Returns a list joined together by the provided delimiter, for example,
     * ["a", "b", "c"] could be joined into "a,b,c"
     * 제공된 구분 기호가 함께 결합한 리스트를 반환함
     * 예를 들어, ["a", "b", "c"]를 "a,b,c"로 결합할 수 있음
     *
     * @param things the things to join together
     * @param delim the delimiter to use
     * @return a string contained the things joined together
     */
    public static String join(List<?> things, String delim) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Object thing : things) {
            if (first) {
                first = false;
            } else {
                builder.append(delim);
            }
            builder.append(thing.toString());
        }
        return builder.toString();
    }

    /**
     * Returns the week since {@link Time#EPOCH_JULIAN_DAY} (Jan 1, 1970)
     * adjusted for first day of week.
     * #EPOCH_JULIAN_DAY 이후, 주 첫날로 조정된 후의 주를 반환함
     *
     * This takes a julian day and the week start day and calculates which
     * week since {@link Time#EPOCH_JULIAN_DAY} that day occurs in, starting
     * at 0. *Do not* use this to compute the ISO week number for the year.
     * 줄리안 데이와 주 시작일이 필요하며, 0에서 시작해서 해당 날짜가 EPOCH_JULIAN_DAY 이후
     * 몇 번째 주에 있는지 계산함, 연도의 ISO 주 번호를 계산하는 데 이 메소드 사용하지 말기
     *
     * @param julianDay The julian day to calculate the week number for
     * @param firstDayOfWeek Which week day is the first day of the week,
     *                       주의 첫 번째 날
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
     * Takes a number of weeks since the epoch and calculates the Julian day of
     * the Monday for that week.
     * epoch로부터 몇 주가 걸리며, 그 주의 월요일의 줄리안 데이 계산
     *
     * This assumes that the week containing the {@link Time#EPOCH_JULIAN_DAY}
     * is considered week 0. It returns the Julian day for the Monday
     * {@code week} weeks after the Monday of the week containing the epoch.
     * 이는 EPOCH_JULIAN_DAY가 포함된 주를 0이라고 가정함
     * epoch를 포함하는 주의 월요일 이후 주어진 주(week) 월요일의 줄리안 데이를 반환함
     *
     * @param week Number of weeks since the epoch
     * @return The julian day for the Monday of the given week since the epoch
     *          epoch 이후, 주어진 주 월요일의 줄리안 데이
     */
    public static int getJulianMondayFromWeeksSinceEpoch(int week) {
        return MONDAY_BEFORE_JULIAN_EPOCH + week * 7;
    }

    /**
     * Get first day of week as android.text.format.Time constant.
     * 주의 첫날을 android.text.format.Time 정수?로 가져옴
     * @return the first day of week in android.text.format.Time
     */
    public static int getFirstDayOfWeek(Context context) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        String pref = prefs.getString(
                GeneralPreferences.KEY_WEEK_START_DAY, GeneralPreferences.WEEK_START_DEFAULT);

        int startDay;
        if (GeneralPreferences.WEEK_START_DEFAULT.equals(pref)) {
            startDay = Calendar.getInstance().getFirstDayOfWeek();
        } else {
            startDay = Integer.parseInt(pref);
        }

        if (startDay == Calendar.SATURDAY) {
            return Time.SATURDAY;
        } else if (startDay == Calendar.MONDAY) {
            return Time.MONDAY;
        } else {
            return Time.SUNDAY;
        }
    }


    /**
     * Get the default length for the duration of an event, in milliseconds.
     * 이벤트 기간 동안의 기본 길이(밀리초)를 가져옴
     *
     * @return the default event length, in milliseconds
     */
    public static long getDefaultEventDurationInMillis(Context context) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        String pref = prefs.getString(GeneralPreferences.KEY_DEFAULT_EVENT_DURATION,
                GeneralPreferences.EVENT_DURATION_DEFAULT);
        final int defaultDurationInMins = Integer.parseInt(pref);
        return defaultDurationInMins * DateUtils.MINUTE_IN_MILLIS;
    }

    /**
     * Get first day of week as java.util.Calendar constant.
     * 주의 첫날을 java.util.Calendar 정수?로 가져옴
     *
     * @return the first day of week as a java.util.Calendar constant
     */
    public static int getFirstDayOfWeekAsCalendar(Context context) {
        return convertDayOfWeekFromTimeToCalendar(getFirstDayOfWeek(context));
    }

    /**
     * Converts the day of the week from android.text.format.Time to java.util.Calendar
     * 요일을 android.text.format.Time에서 java.util.Calendar로 변환함
     */
    public static int convertDayOfWeekFromTimeToCalendar(int timeDayOfWeek) {
        switch (timeDayOfWeek) {
            case Time.MONDAY:
                return Calendar.MONDAY;
            case Time.TUESDAY:
                return Calendar.TUESDAY;
            case Time.WEDNESDAY:
                return Calendar.WEDNESDAY;
            case Time.THURSDAY:
                return Calendar.THURSDAY;
            case Time.FRIDAY:
                return Calendar.FRIDAY;
            case Time.SATURDAY:
                return Calendar.SATURDAY;
            case Time.SUNDAY:
                return Calendar.SUNDAY;
            default:
                throw new IllegalArgumentException("Argument must be between Time.SUNDAY and " +
                        "Time.SATURDAY");
        }
    }


    /**
     * @return true when week number should be shown.
     *          주 번호가 표시되어야 할 때 true 반환
     */
    public static boolean getShowWeekNumber(Context context) {
        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getBoolean(
                GeneralPreferences.KEY_SHOW_WEEK_NUM, GeneralPreferences.DEFAULT_SHOW_WEEK_NUM);
    }

    /**
     * @return true when declined events should be hidden.
     *          거절된 이벤트가 감춰져야 할 때 true
     */
    public static boolean getHideDeclinedEvents(Context context) {
        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getBoolean(GeneralPreferences.KEY_HIDE_DECLINED, false);
    }

    public static int getDaysPerWeek(Context context) {
        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return Integer.valueOf(prefs.getString(GeneralPreferences.KEY_DAYS_PER_WEEK, "7"));
    }

    public static int getMDaysPerWeek(Context context) {
        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return Integer.valueOf(prefs.getString(GeneralPreferences.KEY_MDAYS_PER_WEEK, "7"));
    }

    public static boolean useCustomSnoozeDelay(Context context) {
        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getBoolean(GeneralPreferences.KEY_USE_CUSTOM_SNOOZE_DELAY, false);
    }

    public static long getDefaultSnoozeDelayMs(Context context) {
        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        final String value = prefs.getString(GeneralPreferences.KEY_DEFAULT_SNOOZE_DELAY, null);
        final long intValue = value != null
                ? Long.valueOf(value)
                : GeneralPreferences.SNOOZE_DELAY_DEFAULT_TIME;

        return intValue * 60L * 1000L; // min -> ms
    }


    /**
     * Determine whether the column position is Saturday or not.
     * 컬럼 위치가 토요일인지 아닌지를 결정함
     *
     * @param column the column position
     * @param firstDayOfWeek the first day of week in android.text.format.Time
     *                       android.text.format.Time의 형태로 주의 첫 날
     * @return true if the column is Saturday position
     *          컬럼이 토요일이라면 true 반환
     */
    public static boolean isSaturday(int column, int firstDayOfWeek) {
        return (firstDayOfWeek == Time.SUNDAY && column == 6)
                || (firstDayOfWeek == Time.MONDAY && column == 5)
                || (firstDayOfWeek == Time.SATURDAY && column == 0);
    }

    /**
     * Determine whether the column position is Sunday or not.
     * 컬럼 위치가 토요일인지 아닌지를 결정함
     *
     * @param column the column position
     * @param firstDayOfWeek the first day of week in android.text.format.Time
     * @return true if the column is Sunday position
     */
    public static boolean isSunday(int column, int firstDayOfWeek) {
        return (firstDayOfWeek == Time.SUNDAY && column == 0)
                || (firstDayOfWeek == Time.MONDAY && column == 6)
                || (firstDayOfWeek == Time.SATURDAY && column == 1);
    }

    /**
     * Convert given UTC time into current local time. This assumes it is for an
     * allday event and will adjust the time to be on a midnight boundary.
     * 지정된 UTC 시간을 현재 현지 시간으로 변환함
     * 종일 이벤트를 위한 것으로 가정하고 자정 경계,,에 오도록 시간을 조정할 것임
     *
     * @param recycle Time object to recycle, otherwise null.
     *                재활용할 Time 객체, 그렇지 않으면 null
     * @param utcTime Time to convert, in UTC.
     *                변환할 Time(UTC)
     * @param tz The time zone to convert this time to.
     *           변환할 시간대?
     */
    public static long convertAlldayUtcToLocal(Time recycle, long utcTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = Time.TIMEZONE_UTC;
        recycle.set(utcTime);
        recycle.timezone = tz;
        return recycle.normalize(true);
    }


    public static long convertAlldayLocalToUTC(Time recycle, long localTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = tz;
        recycle.set(localTime);
        recycle.timezone = Time.TIMEZONE_UTC;
        return recycle.normalize(true);
    }

    /**
     * Finds and returns the next midnight after "theTime" in milliseconds UTC
     * UTC(밀리초) 단위의 "theTime" 이후 다음 자정을 찾아서 반환함
     *
     * @param recycle - Time object to recycle, otherwise null.
     * @param theTime - Time used for calculations (in UTC)
     * @param tz The time zone to convert this time to.
     */
    public static long getNextMidnight(Time recycle, long theTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = tz;
        recycle.set(theTime);
        recycle.monthDay ++;
        recycle.hour = 0;
        recycle.minute = 0;
        recycle.second = 0;
        return recycle.normalize(true);
    }


    /**
     * Scan through a cursor of calendars and check if names are duplicated.
     * This travels a cursor containing calendar display names and fills in the
     * provided map with whether or not each name is repeated.
     * 캘린더의 커서를 스캔해서 이름이 중복되었는지 확인함
     * 이것은 캘린더 디스플레이 이름이 들어 있는 커서를 이동하고
     * 각각의 이름이 반복되는지를 제공된 map에 채움
     *
     * @param isDuplicateName The map to put the duplicate check results in.
     *                        중복 체크 결과를 넣을 map
     * @param cursor The query of calendars to check
     *               체크할 캘린더의 쿼리
     * @param nameIndex The column of the query that contains the display name
     *                  디스플레이 이름이 포함된 쿼리의 컬럼
     */
    public static void checkForDuplicateNames(
            Map<String, Boolean> isDuplicateName, Cursor cursor, int nameIndex) {
        isDuplicateName.clear();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String displayName = cursor.getString(nameIndex);
            // Set it to true if we've seen this name before, false otherwise
            // 이 이름을 전에 본 적이 있다면 true로 설정, 그렇지 않다면 false
            if (displayName != null) {
                isDuplicateName.put(displayName, isDuplicateName.containsKey(displayName));
            }
        }
    }

    /**
     * Null-safe object comparison
     * Null에 안전한 객체 비교
     *
     * @param o1
     * @param o2
     * @return
     */
    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static boolean getAllowWeekForDetailView() {
        return mAllowWeekForDetailView;
    }

    public static void setAllowWeekForDetailView(boolean allowWeekView) {
        mAllowWeekForDetailView = allowWeekView;
    }

    public static boolean getConfigBool(Context c, int key) {
        return c.getResources().getBoolean(key);
    }

    /**
     * For devices with Jellybean or later, darkens the given color to ensure that white text is
     * clearly visible on top of it.  For devices prior to Jellybean, does nothing, as the
     * sync adapter handles the color change.
     * 젤리빈 이상의 장치인 경우, 흰색 텍스트가 그 위에 선명하게 보이도록 주어진 색을 어둡게 함
     * 젤리비 이전의 장치의 경우, 동기화 어댑터가 색상 변경을 처리하므로 아무것도 하지 않음
     *
     * @param color
     */
    public static int getDisplayColorFromColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(hsv[1] * SATURATION_ADJUST, 1.0f);
        hsv[2] = hsv[2] * INTENSITY_ADJUST;
        return Color.HSVToColor(hsv);
    }

    // This takes a color and computes what it would look like blended with
    // white. The result is the color that should be used for declined events.
    // 색상을 가지고 흰색과 혼합된 모양을 계산함
    // 거절된 이벤트에 사용해야 하는 색상임
    public static int getDeclinedColorFromColor(int color) {
        int bg = 0xffffffff;
        int a = DECLINED_EVENT_ALPHA;
        int r = (((color & 0x00ff0000) * a) + ((bg & 0x00ff0000) * (0xff - a))) & 0xff000000;
        int g = (((color & 0x0000ff00) * a) + ((bg & 0x0000ff00) * (0xff - a))) & 0x00ff0000;
        int b = (((color & 0x000000ff) * a) + ((bg & 0x000000ff) * (0xff - a))) & 0x0000ff00;
        return (0xff000000) | ((r | g | b) >> 8);
    }


    /**
     * Converts a list of events to a list of segments to draw. Assumes list is
     * ordered by start time of the events. The function processes events for a
     * range of days from firstJulianDay to firstJulianDay + dayXs.length - 1.
     * The algorithm goes over all the events and creates a set of segments
     * ordered by start time. This list of segments is then converted into a
     * HashMap of strands which contain the draw points and are organized by
     * color. The strands can then be drawn by setting the paint color to each
     * strand's color and calling drawLines on its set of points. The points are
     * set up using the following parameters.
     * 이벤트 리스트를 그릴 segment 리스트로 변환함, 이벤트 시작 시간별로 리스트가 정렬된다고 가정함
     * 이 기능은 firstJulianDay에서 firstJulianDay+dayXs.length-1까지의 이벤트를 처리함
     * 알고리즘은 모든 이벤트를 검토하여 시작 시간별로 정렬된 세그먼트 set을 생성함
     * 세그먼트 리스트는 그릴 포인트(draw point)를 포함하고 있고 색상으로 구성된 strand HashMap으로 변환됨
     * 각 strand의 색상으로 페인트 색상을 설정하고, 포인트 set에서 drawLines를 호출하여 strand를 그릴 수 있음
     * 포인트는 다음 매개변수를 사용하여 설정함
     *
     * <ul>
     * <li>Events between midnight and WORK_DAY_START_MINUTES are compressed
     * into the first 1/8th of the space between top and bottom.</li>
     * 자정과 WORK_DAY_START_MINUTES 사이의 이벤트는 상단 및 하단 사이 공간의 첫 번째 1/8로 압축됨
     * <li>Events between WORK_DAY_END_MINUTES and the following midnight are
     * compressed into the last 1/8th of the space between top and bottom</li>
     * WORK_DAY_END_MINUTES와 다음 자정 사이의 이벤트는 상단 및 하단 사이 공간의 마지막 1/8로 압축됨
     * <li>Events between WORK_DAY_START_MINUTES and WORK_DAY_END_MINUTES use
     * the remaining 3/4ths of the space</li>
     * WORK_DAY_START_MINUTES와 WORK_DAY_END_MINUTES 사이의 이벤트는 공간의 나머지 3/4를 사용함
     * <li>All segments drawn will maintain at least minPixels height, except
     * for conflicts in the first or last 1/8th, which may be smaller</li>
     * 첫 번째와 마지막 1/8번째의 충돌을 제외하고, 그려진 모든 세그먼트는 최소한의 minPixel의 높이를
     * 유지하며, 더 작을 수도 있음
     * </ul>
     *
     * @param firstJulianDay The julian day of the first day of events
     *                       이벤트 첫 날의 줄리안 데이
     * @param events A list of events sorted by start time
     *               시작 시간별로 정렬된 이벤트 리스트
     * @param top The lowest y value the dna should be drawn at
     *            dna가 그려야 하는 가장 낮은 y값
     * @param bottom The highest y value the dna should be drawn at
     *               dna가 그려야 하는 가장 높은 x값
     * @param dayXs An array of x values to draw the dna at, one for each day
     *              dna가 그릴 x값의 배열(매일 하나씩)
     * @param context the color to use for conflicts
     *                충돌에 사용할 색상
     * @return
     */
    public static HashMap<Integer, DNAStrand> createDNAStrands(int firstJulianDay,
                                                               ArrayList<Event> events, int top, int bottom, int minPixels, int[] dayXs,
                                                               Context context) {

        if (!mMinutesLoaded) {
            if (context == null) {
                Log.wtf(TAG, "No context and haven't loaded parameters yet! Can't create DNA.");
            }
            Resources res = context.getResources();
            CONFLICT_COLOR = res.getColor(R.color.month_dna_conflict_time_color);
            WORK_DAY_START_MINUTES = res.getInteger(R.integer.work_start_minutes);
            WORK_DAY_END_MINUTES = res.getInteger(R.integer.work_end_minutes);
            WORK_DAY_END_LENGTH = DAY_IN_MINUTES - WORK_DAY_END_MINUTES;
            WORK_DAY_MINUTES = WORK_DAY_END_MINUTES - WORK_DAY_START_MINUTES;
            mMinutesLoaded = true;
        }

        if (events == null || events.isEmpty() || dayXs == null || dayXs.length < 1
                || bottom - top < 8 || minPixels < 0) {
            Log.e(TAG,
                    "Bad values for createDNAStrands! events:" + events + " dayXs:"
                            + Arrays.toString(dayXs) + " bot-top:" + (bottom - top) + " minPixels:"
                            + minPixels);
            return null;
        }

        LinkedList<DNASegment> segments = new LinkedList<DNASegment>();
        HashMap<Integer, DNAStrand> strands = new HashMap<Integer, DNAStrand>();
        // add a black strand by default, other colors will get added in
        // the loop
        // 기본으로 검은색 strand를 추가하면, 다른 색상이 루프에 추가됨
        DNAStrand blackStrand = new DNAStrand();
        blackStrand.color = CONFLICT_COLOR;
        strands.put(CONFLICT_COLOR, blackStrand);
        // the min length is the number of minutes that will occupy
        // MIN_SEGMENT_PIXELS in the 'work day' time slot. This computes the
        // minutes/pixel * minpx where the number of pixels are 3/4 the total
        // dna height: 4*(mins/(px * 3/4))
        // 최소 길이는 'work day' 시간 슬롯에서 MIN_SEGMENT_PIXELS를 차지하는 시간(분)
        // [분/픽셀 * 최소px]를 계산함
        // 픽셀의 수가 3/4인 경우 dna 높이: 4*(mins/(px* 3/4))
        int minMinutes = minPixels * 4 * WORK_DAY_MINUTES / (3 * (bottom - top));

        // There are slightly fewer than half as many pixels in 1/6 the space,
        // so round to 2.5x for the min minutes in the non-work area
        // 1/6 공간에는 절반 이하의 픽셀이 있으므로, 작업하지 않는 영역에서는 최소 분의 2.5배까지 반올림함
        int minOtherMinutes = minMinutes * 5 / 2;
        int lastJulianDay = firstJulianDay + dayXs.length - 1;

        Event event = new Event();
        // Go through all the events for the week
        // 주의 모든 이벤트를 살펴봄
        for (Event currEvent : events) {
            // if this event is outside the weeks range skip it
            // 이 이벤트가 주 범위를 벗어나면 건너뜀
            if (currEvent.endDay < firstJulianDay || currEvent.startDay > lastJulianDay) {
                continue;
            }
            if (currEvent.drawAsAllday()) {
                addAllDayToStrands(currEvent, strands, firstJulianDay, dayXs.length);
                continue;
            }
            // Copy the event over so we can clip its start and end to our range
            // 이벤트를 복사하여 시작과 끝을 범위 내로 오도록 함
            currEvent.copyTo(event);
            if (event.startDay < firstJulianDay) {
                event.startDay = firstJulianDay;
                event.startTime = 0;
            }
            // If it starts after the work day make sure the start is at least
            // minPixels from midnight
            // work day 이후 시작된다면, 자정부터 시작이 minPixels 이상 시작하는지... 확인함
            if (event.startTime > DAY_IN_MINUTES - minOtherMinutes) {
                event.startTime = DAY_IN_MINUTES - minOtherMinutes;
            }
            if (event.endDay > lastJulianDay) {
                event.endDay = lastJulianDay;
                event.endTime = DAY_IN_MINUTES - 1;
            }
            // If the end time is before the work day make sure it ends at least
            // minPixels after midnight
            // 종료 시간이 work day 전이라면, 자정 이후에 minPixels 이상 종료되는지... 확인함
            if (event.endTime < minOtherMinutes) {
                event.endTime = minOtherMinutes;
            }
            // If the start and end are on the same day make sure they are at
            // least minPixels apart. This only needs to be done for times
            // outside the work day as the min distance for within the work day
            // is enforced in the segment code.
            // 시작과 끝이 같은 날이라면, 최소 minPixel이 떨어져 있는지 확인함
            // 이는 작업일(work day) 내 최소 거리가 세그먼트 코드에 적용되기 때문에
            // 작업일 이외의 시간 동안만 수행하면 됨
            if (event.startDay == event.endDay &&
                    event.endTime - event.startTime < minOtherMinutes) {
                // If it's less than minPixels in an area before the work
                // day
                // 작업일 전에 있는 지역의 minPixels보다 작을 경우
                if (event.startTime < WORK_DAY_START_MINUTES) {
                    // extend the end to the first easy guarantee that it's
                    // minPixels
                    // 이것이 minPixels라는 첫 번째 쉬운 보증으로 끝을 확장함
                    event.endTime = Math.min(event.startTime + minOtherMinutes,
                            WORK_DAY_START_MINUTES + minMinutes);
                    // if it's in the area after the work day
                    // 작업일 이후에 그 지역에 있다면
                } else if (event.endTime > WORK_DAY_END_MINUTES) {
                    // First try shifting the end but not past midnight
                    // 첫 번째 끝(the end) 이동 시도, 자정을 넘기지 않음
                    event.endTime = Math.min(event.endTime + minOtherMinutes, DAY_IN_MINUTES - 1);
                    // if it's still too small move the start back
                    // 너무 작다면 다시 시작함
                    if (event.endTime - event.startTime < minOtherMinutes) {
                        event.startTime = event.endTime - minOtherMinutes;
                    }
                }
            }

            // This handles adding the first segment
            // 첫 번째 세그먼트를 추가하는 작업 처리
            if (segments.size() == 0) {
                addNewSegment(segments, event, strands, firstJulianDay, 0, minMinutes);
                continue;
            }
            // Now compare our current start time to the end time of the last
            // segment in the list
            // 이제 현재 시작 시간을 목록의 마지막 세그먼트의 종료 시간과 비교함
            DNASegment lastSegment = segments.getLast();
            int startMinute = (event.startDay - firstJulianDay) * DAY_IN_MINUTES + event.startTime;
            int endMinute = Math.max((event.endDay - firstJulianDay) * DAY_IN_MINUTES
                    + event.endTime, startMinute + minMinutes);

            if (startMinute < 0) {
                startMinute = 0;
            }
            if (endMinute >= WEEK_IN_MINUTES) {
                endMinute = WEEK_IN_MINUTES - 1;
            }
            // If we start before the last segment in the list ends we need to
            // start going through the list as this may conflict with other
            // events
            // 목록의 마지막 세그먼트가 끝나기 전에 시작하면, 다른 이벤트와
            // 충돌할 수 있기 때문에 리스트를 검토할 필요가 있음
            if (startMinute < lastSegment.endMinute) {
                int i = segments.size();
                // find the last segment this event intersects with
                // 이 이벤트가 교차하는 마지막 세그먼트를 찾음
                while (--i >= 0 && endMinute < segments.get(i).startMinute);

                DNASegment currSegment;
                // for each segment this event intersects with
                // 이 이벤트가 교차하는 각 세그먼트에 대해
                for (; i >= 0 && startMinute <= (currSegment = segments.get(i)).endMinute; i--) {
                    // if the segment is already a conflict ignore it
                    // 세그먼트가 이미 충돌한 경우 무시함
                    if (currSegment.color == CONFLICT_COLOR) {
                        continue;
                    }
                    // if the event ends before the segment and wouldn't create
                    // a segment that is too small split off the right side
                    // 이벤트가 세그먼트 이전에 종료되고, 오른쪽에서 너무 작은 세그먼트가 분리되어 생성되지 않는 경우
                    if (endMinute < currSegment.endMinute - minMinutes) {
                        DNASegment rhs = new DNASegment();
                        rhs.endMinute = currSegment.endMinute;
                        rhs.color = currSegment.color;
                        rhs.startMinute = endMinute + 1;
                        rhs.day = currSegment.day;
                        currSegment.endMinute = endMinute;
                        segments.add(i + 1, rhs);
                        strands.get(rhs.color).count++;
                        if (DEBUG) {
                            Log.d(TAG, "Added rhs, curr:" + currSegment.toString() + " i:"
                                    + segments.get(i).toString());
                        }
                    }
                    // if the event starts after the segment and wouldn't create
                    // a segment that is too small split off the left side
                    // 이벤트가 세그먼트 이후에 시작되고, 왼쪽에서 너무 작은 세그먼트가 분리되어 생성되지 않는 경우
                    if (startMinute > currSegment.startMinute + minMinutes) {
                        DNASegment lhs = new DNASegment();
                        lhs.startMinute = currSegment.startMinute;
                        lhs.color = currSegment.color;
                        lhs.endMinute = startMinute - 1;
                        lhs.day = currSegment.day;
                        currSegment.startMinute = startMinute;
                        // increment i so that we are at the right position when
                        // referencing the segments to the right and left of the
                        // current segment.
                        // 현재 세그먼트의 오른쪽과 왼쪽으로 세그먼트를 참조할 때
                        // 올바른 위치에 있도록 i를 증가시킴
                        segments.add(i++, lhs);
                        strands.get(lhs.color).count++;
                        if (DEBUG) {
                            Log.d(TAG, "Added lhs, curr:" + currSegment.toString() + " i:"
                                    + segments.get(i).toString());
                        }
                    }
                    // if the right side is black merge this with the segment to
                    // the right if they're on the same day and overlap
                    // 오른쪽이 검정 merge인 경우, 동일한 날짜에 중복되는 경우
                    // 세그먼트와 이 세그먼트를 오른쪽과 병합함... 뭐라는 거야
                    if (i + 1 < segments.size()) {
                        DNASegment rhs = segments.get(i + 1);
                        if (rhs.color == CONFLICT_COLOR && currSegment.day == rhs.day
                                && rhs.startMinute <= currSegment.endMinute + 1) {
                            rhs.startMinute = Math.min(currSegment.startMinute, rhs.startMinute);
                            segments.remove(currSegment);
                            strands.get(currSegment.color).count--;
                            // point at the new current segment
                            currSegment = rhs;
                        }
                    }
                    // if the left side is black merge this with the segment to
                    // the left if they're on the same day and overlap
                    // 왼쪽이 검정 merge인 경우, 동일한 날짜에 중복되는 경우
                    // 세그먼트와 이 세그먼트를 왼쪽과 병합함
                    if (i - 1 >= 0) {
                        DNASegment lhs = segments.get(i - 1);
                        if (lhs.color == CONFLICT_COLOR && currSegment.day == lhs.day
                                && lhs.endMinute >= currSegment.startMinute - 1) {
                            lhs.endMinute = Math.max(currSegment.endMinute, lhs.endMinute);
                            segments.remove(currSegment);
                            strands.get(currSegment.color).count--;
                            // point at the new current segment
                            // 새 현재 세그먼트를 가리킴
                            currSegment = lhs;
                            // point i at the new current segment in case new
                            // code is added
                            // 새로운 코드가 추가되는 경우, 새 현재 세그먼트의 지점 i
                            i--;
                        }
                    }
                    // if we're still not black, decrement the count for the
                    // color being removed, change this to black, and increment
                    // the black count
                    // 만약 여전히 검정색이 아닌 경우, 제거할 색에 대한 카운트를 줄이고
                    // 이것을 검정색으로 변경한 후에 검정색 카운트를 증가시킴
                    if (currSegment.color != CONFLICT_COLOR) {
                        strands.get(currSegment.color).count--;
                        currSegment.color = CONFLICT_COLOR;
                        strands.get(CONFLICT_COLOR).count++;
                    }
                }

            }
            // If this event extends beyond the last segment add a new segment
            // 이 이벤트가 마지막 세그먼트 이상으로 확장된다면 새 세그먼트를 추가함
            if (endMinute > lastSegment.endMinute) {
                addNewSegment(segments, event, strands, firstJulianDay, lastSegment.endMinute,
                        minMinutes);
            }
        }
        weaveDNAStrands(segments, firstJulianDay, strands, top, bottom, dayXs);
        return strands;
    }

    // This figures out allDay colors as allDay events are found
    // allDay 이벤트가 발견됨에 따라 allDay 색상을 계산함
    private static void addAllDayToStrands(Event event, HashMap<Integer, DNAStrand> strands,
                                           int firstJulianDay, int numDays) {
        DNAStrand strand = getOrCreateStrand(strands, CONFLICT_COLOR);
        // if we haven't initialized the allDay portion create it now
        // allDay 부분을 초기화하지 않았다면 지금 당장 만들 수 있을 것임
        if (strand.allDays == null) {
            strand.allDays = new int[numDays];
        }

        // For each day this event is on update the color
        // 이 이벤트는 각 요일에 대한 색상을 업데이트함
        int end = Math.min(event.endDay - firstJulianDay, numDays - 1);
        for (int i = Math.max(event.startDay - firstJulianDay, 0); i <= end; i++) {
            if (strand.allDays[i] != 0) {
                // if this day already had a color, it is now a conflict
                // 만약 이 날짜가 이미 이 색을 가지고 있다면, 충돌하는 것임
                strand.allDays[i] = CONFLICT_COLOR;
            } else {
                // else it's just the color of the event
                // 그렇지 않으면 단지 이벤트의 색상
                strand.allDays[i] = event.color;
            }
        }
    }


    // This processes all the segments, sorts them by color, and generates a
    // list of points to draw
    // 이렇게 하면 모든 세그먼트가 처리되고, 색상별로 정렬되며 그릴 point의 리스트가 생성됨
    private static void weaveDNAStrands(LinkedList<DNASegment> segments, int firstJulianDay,
                                        HashMap<Integer, DNAStrand> strands, int top, int bottom, int[] dayXs) {
        // First, get rid of any colors that ended up with no segments
        // 첫번째, 세그먼트가 없이 끝나 버린 색상을 처리함
        Iterator<DNAStrand> strandIterator = strands.values().iterator();
        while (strandIterator.hasNext()) {
            DNAStrand strand = strandIterator.next();
            if (strand.count < 1 && strand.allDays == null) {
                strandIterator.remove();
                continue;
            }
            strand.points = new float[strand.count * 4];
            strand.position = 0;
        }
        // Go through each segment and compute its points
        // 각 세그먼트를 살펴보고 해당 포인트를 계산함
        for (DNASegment segment : segments) {
            // Add the points to the strand of that color
            // 그 색상의 strand에 포인트를 추가함
            DNAStrand strand = strands.get(segment.color);
            int dayIndex = segment.day - firstJulianDay;
            int dayStartMinute = segment.startMinute % DAY_IN_MINUTES;
            int dayEndMinute = segment.endMinute % DAY_IN_MINUTES;
            int height = bottom - top;
            int workDayHeight = height * 3 / 4;
            int remainderHeight = (height - workDayHeight) / 2;

            int x = dayXs[dayIndex];
            int y0 = 0;
            int y1 = 0;

            y0 = top + getPixelOffsetFromMinutes(dayStartMinute, workDayHeight, remainderHeight);
            y1 = top + getPixelOffsetFromMinutes(dayEndMinute, workDayHeight, remainderHeight);
            if (DEBUG) {
                Log.d(TAG, "Adding " + Integer.toHexString(segment.color) + " at x,y0,y1: " + x
                        + " " + y0 + " " + y1 + " for " + dayStartMinute + " " + dayEndMinute);
            }
            strand.points[strand.position++] = x;
            strand.points[strand.position++] = y0;
            strand.points[strand.position++] = x;
            strand.points[strand.position++] = y1;
        }
    }

    private static void weaveDNADiaries(LinkedList<DNASegmentD> segments, int firstJulianDay,
                                        HashMap<Integer, DNADiary> strands, int top, int bottom, int[] dayXs) {
        // First, get rid of any colors that ended up with no segments
        // 첫번째, 세그먼트가 없이 끝나 버린 색상을 처리함
        Iterator<DNADiary> diaryIterator = strands.values().iterator();
        while (diaryIterator.hasNext()) {
            DNADiary strand = diaryIterator.next();
            if (strand.count < 1) {
                diaryIterator.remove();
                continue;
            }
            strand.points = new float[strand.count * 4];
            strand.position = 0;
        }
        // Go through each segment and compute its points
        // 각 세그먼트를 살펴보고 해당 포인트를 계산함
        for (DNASegmentD segment : segments) {
            // Add the points to the strand of that color
            // 그 색상의 strand에 포인트를 추가함
            DNADiary strand = strands.get(segment.color);
            int dayIndex = (int)(segment.day - firstJulianDay);
            int dayStartMinute = segment.startMinute % DAY_IN_MINUTES;
            int dayEndMinute = segment.endMinute % DAY_IN_MINUTES;
            int height = bottom - top;
            int workDayHeight = height * 3 / 4;
            int remainderHeight = (height - workDayHeight) / 2;

            int x = dayXs[dayIndex];
            int y0 = 0;
            int y1 = 0;

            y0 = top + getPixelOffsetFromMinutes(dayStartMinute, workDayHeight, remainderHeight);
            y1 = top + getPixelOffsetFromMinutes(dayEndMinute, workDayHeight, remainderHeight);
            if (DEBUG) {
                Log.d(TAG, "Adding " + Integer.toHexString(segment.color) + " at x,y0,y1: " + x
                        + " " + y0 + " " + y1 + " for " + dayStartMinute + " " + dayEndMinute);
            }
            strand.points[strand.position++] = x;
            strand.points[strand.position++] = y0;
            strand.points[strand.position++] = x;
            strand.points[strand.position++] = y1;
        }
    }


    /**
     * Compute a pixel offset from the top for a given minute from the work day
     * height and the height of the top area.
     * 업무일 높이와 상단 면적의 높이로부터 주어진 분 동안 상단으로부터 픽셀 오프셋을 계산함
     */
    private static int getPixelOffsetFromMinutes(int minute, int workDayHeight,
                                                 int remainderHeight) {
        int y;
        if (minute < WORK_DAY_START_MINUTES) {
            y = minute * remainderHeight / WORK_DAY_START_MINUTES;
        } else if (minute < WORK_DAY_END_MINUTES) {
            y = remainderHeight + (minute - WORK_DAY_START_MINUTES) * workDayHeight
                    / WORK_DAY_MINUTES;
        } else {
            y = remainderHeight + workDayHeight + (minute - WORK_DAY_END_MINUTES) * remainderHeight
                    / WORK_DAY_END_LENGTH;
        }
        return y;
    }


    /**
     * Add a new segment based on the event provided. This will handle splitting
     * segments across day boundaries and ensures a minimum size for segments.
     * 제공된 이벤트에 따라 새 세그먼트를 추가함
     * 이렇게 하면 주간 경계로 분할되는 세그먼트를 처리하고, 세그먼트의 최소 크기를 보장할 수 있음
     */
    private static void addNewSegment(LinkedList<DNASegment> segments, Event event,
                                      HashMap<Integer, DNAStrand> strands, int firstJulianDay, int minStart, int minMinutes) {
        if (event.startDay > event.endDay) {
            Log.wtf(TAG, "Event starts after it ends: " + event.toString());
        }
        // If this is a multiday event split it up by day
        // 이게 여러 날에 걸친 이벤트라면, 나날이 나눔
        if (event.startDay != event.endDay) {
            Event lhs = new Event();
            lhs.color = event.color;
            lhs.startDay = event.startDay;
            // the first day we want the start time to be the actual start time
            // 첫 번째 날의 시작 시간을 실제 시작 시간으로 하길 원함?
            lhs.startTime = event.startTime;
            lhs.endDay = lhs.startDay;
            lhs.endTime = DAY_IN_MINUTES - 1;
            // Nearly recursive iteration!
            // 거의 재귀 반복
            while (lhs.startDay != event.endDay) {
                addNewSegment(segments, lhs, strands, firstJulianDay, minStart, minMinutes);
                // The days in between are all day, even though that shouldn't
                // actually happen due to the allday filtering
                // 실제로 종일(allday) 필터링 때문에 일어나서는 안 되지만,
                // 그 사이에 있는 날은 allday임?
                lhs.startDay++;
                lhs.endDay = lhs.startDay;
                lhs.startTime = 0;
                minStart = 0;
            }
            // The last day we want the end time to be the actual end time
            // 종료 시간이 실제 종료 시간이 되길 원하는 마지막 날
            lhs.endTime = event.endTime;
            event = lhs;
        }
        // Create the new segment and compute its fields
        // 새로운 세그먼트 생성 및 해당 필드 계산
        DNASegment segment = new DNASegment();
        int dayOffset = (event.startDay - firstJulianDay) * DAY_IN_MINUTES;
        int endOfDay = dayOffset + DAY_IN_MINUTES - 1;
        // clip the start if needed
        // 필요하면 시작 고정
        segment.startMinute = Math.max(dayOffset + event.startTime, minStart);
        // and extend the end if it's too small, but not beyond the end of the
        // day
        // 그리고 종료가 너무 작으면 연장하되, 하루의 끝을 넘겨서는 안 됨
        int minEnd = Math.min(segment.startMinute + minMinutes, endOfDay);
        segment.endMinute = Math.max(dayOffset + event.endTime, minEnd);
        if (segment.endMinute > endOfDay) {
            segment.endMinute = endOfDay;
        }

        segment.color = event.color;
        segment.day = event.startDay;
        segments.add(segment);
        // increment the count for the correct color or add a new strand if we
        // don't have that color yet
        // 올바른 색에 대한 카운트를 늘리거나, 아직 색상이 없다면 새 strand를 추가함
        DNAStrand strand = getOrCreateStrand(strands, segment.color);
        strand.count++;
    }

    private static void addNewSegment(LinkedList<DNASegmentD> segments, Diary diary,
                                      HashMap<Integer, DNADiary> strands, int firstJulianDay) {
        // Create the new segment and compute its fields
        // 새로운 세그먼트 생성 및 해당 필드 계산
        DNASegmentD segment = new DNASegmentD();
        long dayOffset = diary.day - firstJulianDay;
//        int dayOffset = (event.startDay - firstJulianDay) * DAY_IN_MINUTES;

        segment.color = diary.color;
        segment.day = diary.day;
        segments.add(segment);
        // increment the count for the correct color or add a new strand if we
        // don't have that color yet
        // 올바른 색에 대한 카운트를 늘리거나, 아직 색상이 없다면 새 strand를 추가함
        DNADiary strand = getOrCreateDiary(strands, segment.color);

        strand.count++;
    }


    /**
     * Try to get a strand of the given color. Create it if it doesn't exist.
     * 주어진 색상의 strand를 가져옴, 없는 경우 생성함
     */
    private static DNAStrand getOrCreateStrand(HashMap<Integer, DNAStrand> strands, int color) {
        DNAStrand strand = strands.get(color);
        if (strand == null) {
            strand = new DNAStrand();
            strand.color = color;
            strand.count = 0;
            strands.put(strand.color, strand);
        }
        return strand;
    }

    private static DNADiary getOrCreateDiary(HashMap<Integer, DNADiary> diaries, int color) {
        DNADiary diary = diaries.get(color);
        if (diary == null) {
            diary = new DNADiary();
            diary.color = color;
            diary.count = 0;
            diaries.put(diary.color, diary);
        }
        return diary;
    }


    /**
     * Sends an intent to launch the top level Calendar view.
     * 최상위 캘린더 view를 시작하려는 intent를 전송함
     *
     * @param context
     */
    public static void returnToCalendarHome(Context context) {
        Intent launchIntent = new Intent(context, AllInOneActivity.class);
        launchIntent.setAction(Intent.ACTION_DEFAULT);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchIntent.putExtra(INTENT_KEY_HOME, true);
        context.startActivity(launchIntent);
    }

    /**
     * This sets up a search view to use Calendar's search suggestions provider
     * and to allow refining the search.
     * 캘린더의 검색 제안 제공자를 사용하고, 검색을 구체화할 수 있도록 검색 view를 설정함
     *
     * @param view The {@link SearchView} to set up
     *             설정하기 위한 SearchView
     * @param act The activity using the view
     *            view를 사용한 activity
     */
    public static void setUpSearchView(SearchView view, Activity act) {
        SearchManager searchManager = (SearchManager) act.getSystemService(Context.SEARCH_SERVICE);
        view.setSearchableInfo(searchManager.getSearchableInfo(act.getComponentName()));
        view.setQueryRefinementEnabled(true);
    }


    /**
     * Given a context and a time in millis since unix epoch figures out the
     * correct week of the year for that time.
     * context와 시간(밀리초)을 고려해서 unix epoch는가 그 해의 정확한 주를 계산함
     *
     * @param millisSinceEpoch
     * @return
     */
    public static int getWeekNumberFromTime(long millisSinceEpoch, Context context) {
        Time weekTime = new Time(getTimeZone(context, null));
        weekTime.set(millisSinceEpoch);
        weekTime.normalize(true);
        int firstDayOfWeek = getFirstDayOfWeek(context);
        // if the date is on Saturday or Sunday and the start of the week
        // isn't Monday we may need to shift the date to be in the correct
        // week
        // 만약 날짜가 토요일이거나 일요일이고, 그 주의 시작이 월요일이 아니라면
        // 올바른 주에 있기 위해 날짜를 바꿀 필요가 있을 것임
        if (weekTime.weekDay == Time.SUNDAY
                && (firstDayOfWeek == Time.SUNDAY || firstDayOfWeek == Time.SATURDAY)) {
            weekTime.monthDay++;
            weekTime.normalize(true);
        } else if (weekTime.weekDay == Time.SATURDAY && firstDayOfWeek == Time.SATURDAY) {
            weekTime.monthDay += 2;
            weekTime.normalize(true);
        }
        return weekTime.getWeekNumber();
    }


    /**
     * Formats a day of the week string. This is either just the name of the day
     * or a combination of yesterday/today/tomorrow and the day of the week.
     * 요일을 string으로 포맷함, 날짜의 이름 또는 어제/오늘/내일 + 요일의 조합임
     *
     * @param julianDay The julian day to get the string for
     * @param todayJulianDay The julian day for today's date
     * @param millis A utc millis since epoch time that falls on julian day
     *               줄리안 데이에 떨어지는,, epoch부터의 utc millis
     * @param context The calling context, used to get the timezone and do the
     *            formatting
     *                시간대를 가져와서 포맷을 수행하는 데 사용하는 calling context
     * @return
     */
    public static String getDayOfWeekString(int julianDay, int todayJulianDay, long millis,
                                            Context context) {
        getTimeZone(context, null);
        int flags = DateUtils.FORMAT_SHOW_WEEKDAY;
        String dayViewText;
        if (julianDay == todayJulianDay) {
            dayViewText = context.getString(R.string.agenda_today,
                    mTZUtils.formatDateRange(context, millis, millis, flags));
        } else if (julianDay == todayJulianDay - 1) {
            dayViewText = context.getString(R.string.agenda_yesterday,
                    mTZUtils.formatDateRange(context, millis, millis, flags));
        } else if (julianDay == todayJulianDay + 1) {
            dayViewText = context.getString(R.string.agenda_tomorrow,
                    mTZUtils.formatDateRange(context, millis, millis, flags));
        } else {
            dayViewText = mTZUtils.formatDateRange(context, millis, millis, flags);
        }
        dayViewText = dayViewText.toUpperCase();
        return dayViewText;
    }


    // Calculate the time until midnight + 1 second and set the handler to
    // do run the runnable
    // 자정+1 초까지 시간을 계산하고 runnable을 실행하도록 handler를 설정함
    public static void setMidnightUpdater(Handler h, Runnable r, String timezone) {
        if (h == null || r == null || timezone == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Time time = new Time(timezone);
        time.set(now);
        long runInMillis = (24 * 3600 - time.hour * 3600 - time.minute * 60 -
                time.second + 1) * 1000;
        h.removeCallbacks(r);
        h.postDelayed(r, runInMillis);
    }

    // Stop the midnight update thread
    // 자정 업데이트 스레드 중지
    public static void resetMidnightUpdater(Handler h, Runnable r) {
        if (h == null || r == null) {
            return;
        }
        h.removeCallbacks(r);
    }


    /**
     * Returns a string description of the specified time interval.
     * 지정된 시간 간격에 대한 문자열 설명을 반환함
     */
    public static String getDisplayedDatetime(long startMillis, long endMillis, long currentMillis,
                                              String localTimezone, boolean allDay, Context context) {
        // Configure date/time formatting.
        // 날짜/시간 형식을 구성함
        int flagsDate = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY;
        int flagsTime = DateUtils.FORMAT_SHOW_TIME;
        if (DateFormat.is24HourFormat(context)) {
            flagsTime |= DateUtils.FORMAT_24HOUR;
        }

        Time currentTime = new Time(localTimezone);
        currentTime.set(currentMillis);
        Resources resources = context.getResources();
        String datetimeString = null;
        if (allDay) {
            // All day events require special timezone adjustment.
            // 종일 이벤트는 특별한 시간대로 조정해야 함
            long localStartMillis = convertAlldayUtcToLocal(null, startMillis, localTimezone);
            long localEndMillis = convertAlldayUtcToLocal(null, endMillis, localTimezone);
            if (singleDayEvent(localStartMillis, localEndMillis, currentTime.gmtoff)) {
                // If possible, use "Today" or "Tomorrow" instead of a full date string.
                // 가능하다면, 전체 날짜 문자열 대신에 "오늘" 또는 "내일"을 사용함
                int todayOrTomorrow = isTodayOrTomorrow(context.getResources(),
                        localStartMillis, currentMillis, currentTime.gmtoff);
                if (TODAY == todayOrTomorrow) {
                    datetimeString = resources.getString(R.string.today);
                } else if (TOMORROW == todayOrTomorrow) {
                    datetimeString = resources.getString(R.string.tomorrow);
                }
            }
            if (datetimeString == null) {
                // For multi-day allday events or single-day all-day events that are not
                // today or tomorrow, use framework formatter.
                // 오늘이나 내일이 아닌, 여러날에 걸치는 종일 이벤트 또는 하루 종일 이벤트에 대해서는
                // framework formatter를 사용함
                Formatter f = new Formatter(new StringBuilder(50), Locale.getDefault());
                datetimeString = DateUtils.formatDateRange(context, f, startMillis,
                        endMillis, flagsDate, Time.TIMEZONE_UTC).toString();
            }
        } else {
            if (singleDayEvent(startMillis, endMillis, currentTime.gmtoff)) {
                // Format the time.
                String timeString = Utils.formatDateRange(context, startMillis, endMillis,
                        flagsTime);

                // If possible, use "Today" or "Tomorrow" instead of a full date string.
                int todayOrTomorrow = isTodayOrTomorrow(context.getResources(), startMillis,
                        currentMillis, currentTime.gmtoff);
                if (TODAY == todayOrTomorrow) {
                    // Example: "Today at 1:00pm - 2:00 pm"
                    datetimeString = resources.getString(R.string.today_at_time_fmt,
                            timeString);
                } else if (TOMORROW == todayOrTomorrow) {
                    // Example: "Tomorrow at 1:00pm - 2:00 pm"
                    datetimeString = resources.getString(R.string.tomorrow_at_time_fmt,
                            timeString);
                } else {
                    // Format the full date. Example: "Thursday, April 12, 1:00pm - 2:00pm"
                    String dateString = Utils.formatDateRange(context, startMillis, endMillis,
                            flagsDate);
                    datetimeString = resources.getString(R.string.date_time_fmt, dateString,
                            timeString);
                }
            } else {
                // For multiday events, shorten day/month names.
                // 여러 날 걸치는 이벤트는 일/월 이름을 줄임
                // Example format: "Fri Apr 6, 5:00pm - Sun, Apr 8, 6:00pm"
                int flagsDatetime = flagsDate | flagsTime | DateUtils.FORMAT_ABBREV_MONTH |
                        DateUtils.FORMAT_ABBREV_WEEKDAY;
                datetimeString = Utils.formatDateRange(context, startMillis, endMillis,
                        flagsDatetime);
            }
        }
        return datetimeString;
    }


    /**
     * Returns the timezone to display in the event info, if the local timezone is different
     * from the event timezone.  Otherwise returns null.
     * 로컬 시간대가 이벤트 시간대와 다른 경우, 이벤트 정보에 표시할 시간대를 반환함
     * 그렇지 않으면 null을 반환함
     */
    public static String getDisplayedTimezone(long startMillis, String localTimezone,
                                              String eventTimezone) {
        String tzDisplay = null;
        if (!TextUtils.equals(localTimezone, eventTimezone)) {
            // Figure out if this is in DST
            // DST에 있는지 확인
            TimeZone tz = TimeZone.getTimeZone(localTimezone);
            if (tz == null || tz.getID().equals("GMT")) {
                tzDisplay = localTimezone;
            } else {
                Time startTime = new Time(localTimezone);
                startTime.set(startMillis);
                tzDisplay = tz.getDisplayName(startTime.isDst != 0, TimeZone.SHORT);
            }
        }
        return tzDisplay;
    }


    /**
     * Returns whether the specified time interval is in a single day.
     * 지정된 시간 간격이 하루 내에 있는지 여부를 반환함
     */
    private static boolean singleDayEvent(long startMillis, long endMillis, long localGmtOffset) {
        if (startMillis == endMillis) {
            return true;
        }

        // An event ending at midnight should still be a single-day event, so check
        // time end-1.
        // 자정에 끝나는 이벤트는 여전히 그 날의 이벤트여야 하므로 end-1를 체크함
        int startDay = Time.getJulianDay(startMillis, localGmtOffset);
        int endDay = Time.getJulianDay(endMillis - 1, localGmtOffset);
        return startDay == endDay;
    }



    /**
     * Returns TODAY or TOMORROW if applicable.  Otherwise returns NONE.
     * 해당하는 경우 TODAY 또는 TOMORROW를 반환함, 그렇지 않으면 NONE 반환함
     */
    private static int isTodayOrTomorrow(Resources r, long dayMillis,
                                         long currentMillis, long localGmtOffset) {
        int startDay = Time.getJulianDay(dayMillis, localGmtOffset);
        int currentDay = Time.getJulianDay(currentMillis, localGmtOffset);

        int days = startDay - currentDay;
        if (days == 1) {
            return TOMORROW;
        } else if (days == 0) {
            return TODAY;
        } else {
            return NONE;
        }
    }


    /**
     * Create an intent for emailing attendees of an event.
     * 이벤트 참석자에게 이메일을 보낼 intent를 만듦
     *
     * @param resources The resources for translating strings.
     *                  문자열을 변환하기 위한 resource
     * @param eventTitle The title of the event to use as the email subject.
     *                   이메일 제목으로 사용할 이벤트의 제목
     * @param body The default text for the email body.
     * @param toEmails The list of emails for the 'to' line.
     * @param ccEmails The list of emails for the 'cc' line.
     * @param ownerAccount The owner account to use as the email sender.
     */
    public static Intent createEmailAttendeesIntent(Resources resources, String eventTitle,
                                                    String body, List<String> toEmails, List<String> ccEmails, String ownerAccount) {
        List<String> toList = toEmails;
        List<String> ccList = ccEmails;
        if (toEmails.size() <= 0) {
            if (ccEmails.size() <= 0) {
                // TODO: Return a SEND intent if no one to email to, to at least populate
                // a draft email with the subject (and no recipients).
                throw new IllegalArgumentException("Both toEmails and ccEmails are empty.");
            }

            // Email app does not work with no "to" recipient.  Move all 'cc' to 'to'
            // in this case.
            // 이메일 앱은 받는 사람이 없으면 작동하지 않음, 이 경우 모든 'cc'를 'to'로 이동함
            toList = ccEmails;
            ccList = null;
        }

        // Use the event title as the email subject (prepended with 'Re: ').
        // 이벤트 제목을 이메일 제목으로 사용함 ('Re: '로 시작됨)
        String subject = null;
        if (eventTitle != null) {
            subject = resources.getString(R.string.email_subject_prefix) + eventTitle;
        }

        // Use the SENDTO intent with a 'mailto' URI, because using SEND will cause
        // the picker to show apps like text messaging, which does not make sense
        // for email addresses.  We put all data in the URI instead of using the extra
        // Intent fields (ie. EXTRA_CC, etc) because some email apps might not handle
        // those (though gmail does).
        // 'mailto' URI에 SENDTO intent를 사용함
        // SEND는 picker가 텍스트 메시징과 같은 앱을 표시하기 때문에, 이메일 주소가... 정상적이지 않을 수 있음
        // 일부 전자 메일 앱(gmail은 괜찮음)이 이러한 것들을 처리하지 못할 수도 있기 때문에
        // 추가 Intent 필드(즉 EXTRA_CC 등...)를 사용하는 대신 모든 데이터를 URI에 넣음

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("mailto");

        // We will append the first email to the 'mailto' field later (because the
        // current state of the Email app requires it).  Add the remaining 'to' values
        // here.  When the email codebase is updated, we can simplify this.
        // 나중에 첫 번째 이메일을 'mailto' 필드에 추가할 것임 (이메일 앱의 현재 상태가 이를 필요로 하기 때문임)
        // 여기에 'to' 값을 추가함
        // 이메일 코드베이스가 업데이트되면, 이걸 단순화할 수 있음
        if (toList.size() > 1) {
            for (int i = 1; i < toList.size(); i++) {
                // The Email app requires repeated parameter settings instead of
                // a single comma-separated list.
                // 이메일 앱은 쉼표로 구분된 단일 목록 대신 반복된 매개변수 설정이 필요함
                uriBuilder.appendQueryParameter("to", toList.get(i));
            }
        }

        // Add the subject parameter.
        if (subject != null) {
            uriBuilder.appendQueryParameter("subject", subject);
        }

        // Add the subject parameter.
        if (body != null) {
            uriBuilder.appendQueryParameter("body", body);
        }

        // Add the cc parameters.
        if (ccList != null && ccList.size() > 0) {
            for (String email : ccList) {
                uriBuilder.appendQueryParameter("cc", email);
            }
        }

        // Insert the first email after 'mailto:' in the URI manually since Uri.Builder
        // doesn't seem to have a way to do this.
        // Uri.Builder에는 처리할 방법이 없으므로 'mailto:' 뒤에 첫 번째 이메일을 URI에 수동으로 삽입함
        String uri = uriBuilder.toString();
        if (uri.startsWith("mailto:")) {
            StringBuilder builder = new StringBuilder(uri);
            builder.insert(7, Uri.encode(toList.get(0)));
            uri = builder.toString();
        }

        // Start the email intent.  Email from the account of the calendar owner in case there
        // are multiple email accounts.
        // 이메일 intent 시작함, 이메일 계정이 여러 개인 경우, 캘린더 소유자의 계정의 이메일로
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
        emailIntent.putExtra("fromAccountString", ownerAccount);

        // Workaround a Email bug that overwrites the body with this intent extra.  If not
        // set, it clears the body.
        // intent를 추가하여 body를 덮어쓰는 이메일 버그 해결, 설정하지 않으면 body를 지워 버림
        if (body != null) {
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        }

        return Intent.createChooser(emailIntent, resources.getString(R.string.email_picker_label));
    }

    /**
     * Example fake email addresses used as attendee emails are resources like conference rooms,
     * or another calendar, etc.  These all end in "calendar.google.com".
     * 참석자 이메일로 사용되는 가짜 이메일 주소는 회의실 또는 다른 캘린더 등과 같은 resource들임
     * 이 모든 것은 calendar.google.com으로 끝남
     */
    public static boolean isValidEmail(String email) {
        return email != null && !email.endsWith(MACHINE_GENERATED_ADDRESS);
    }

    /**
     * Returns true if:
     *   (1) the email is not a resource like a conference room or another calendar.
     *       Catch most of these by filtering out suffix calendar.google.com.
     *       이메일은 회의실이나 다른 캘린더 resource 등이 아님, 접미사 calendar.google.com을 필터링해서
     *       이 중 대부분을 파악함
     *   (2) the email is not equal to the sync account to prevent mailing himself.
     *       이메일이 동기화 계정과 동일하지 않아 직접 메일을 발송할 수 없음
     */
    public static boolean isEmailableFrom(String email, String syncAccountName) {
        return Utils.isValidEmail(email) && !email.equals(syncAccountName);
    }

    /**
     * Inserts a drawable with today's day into the today's icon in the option menu
     * 옵션 메뉴의 오늘의 아이콘에 오늘의 날짜 drawable을 삽입함
     * @param icon - today's icon from the options menu
     */
    public static void setTodayIcon(LayerDrawable icon, Context c, String timezone) {
        DayOfMonthDrawable today;

        // Reuse current drawable if possible
        Drawable currentDrawable = icon.findDrawableByLayerId(R.id.today_icon_day);
        if (currentDrawable != null && currentDrawable instanceof DayOfMonthDrawable) {
            today = (DayOfMonthDrawable)currentDrawable;
        } else {
            today = new DayOfMonthDrawable(c);
        }
        // Set the day and update the icon
        Time now =  new Time(timezone);
        now.setToNow();
        now.normalize(false);
        today.setDayOfMonth(now.monthDay);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.today_icon_day, today);
    }

    public static BroadcastReceiver setTimeChangesReceiver(Context c, Runnable callback) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);

        CalendarBroadcastReceiver r = new CalendarBroadcastReceiver(callback);
        c.registerReceiver(r, filter);
        return r;
    }


    public static void clearTimeChangesReceiver(Context c, BroadcastReceiver r) {
        c.unregisterReceiver(r);
    }

    /**
     * Get a list of quick responses used for emailing guests from the
     * SharedPreferences. If not are found, get the hard coded ones that shipped
     * with the app
     * SharedPreferences에서 참석자에게 이메일을 보내는 데 사용되는 빠른 응답(quick response) 목록을
     * 얻음, 발견되지 않으면, 앱과 함께 받은 하드 코드를 가져옴
     *
     * @param context
     * @return a list of quick responses.
     */
    public static String[] getQuickResponses(Context context) {
        String[] s = Utils.getSharedPreference(context, KEY_QUICK_RESPONSES, (String[]) null);

        if (s == null) {
            s = context.getResources().getStringArray(R.array.quick_response_defaults);
        }

        return s;
    }


    /**
     * Return the app version code.
     * 앱 버전 코드를 반환함
     */
    public static String getVersionCode(Context context) {
        if (sVersion == null) {
            try {
                sVersion = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                // Can't find version; just leave it blank.
                Log.e(TAG, "Error finding package " + context.getApplicationInfo().packageName);
            }
        }
        return sVersion;
    }

    /**
     * Checks the server for an updated list of Calendars (in the background).
     * 서버에서 업데이트된 캘린더 목록을 확인함(백그라운드에서)
     *
     * If a Calendar is added on the web (and it is selected and not
     * hidden) then it will be added to the list of calendars on the phone
     * (when this finishes).  When a new calendar from the
     * web is added to the phone, then the events for that calendar are also
     * downloaded from the web.
     * 캘린더를 웹에 추가하면(캘린더가 선택되고 숨겨지지 않음) 휴대폰의 캘린더 목록이 추가됨
     * 웹의 새로운 캘린더가 휴대폰에 추가되면, 그 캘린더에 대한 이벤트도 웹에서 다운로드됨
     *
     * This sync is done automatically in the background when the
     * SelectCalendars activity and fragment are started.
     * 이 동기화는 SelectCalendars activity와 fragment이 시작될 때 백그라운드에서 자동으로 수행됨
     *
     * @param account - The account to sync. May be null to sync all accounts.
     */
    public static void startCalendarMetafeedSync(Account account) {
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean("metafeedonly", true);
        ContentResolver.requestSync(account, Calendars.CONTENT_URI.getAuthority(), extras);
    }


    /**
     * Replaces stretches of text that look like addresses and phone numbers with clickable
     * links. If lastDitchGeo is true, then if no links are found in the textview, the entire
     * string will be converted to a single geo link. Any spans that may have previously been
     * in the text will be cleared out.
     * 주소와 전화번호처럼 보이는 텍스트의 span을 클릭 가능한 링크로 대체함
     * lastDitchGeo가 true고, TextView에서 링크가 발견되지 않으면, 전체 문자열이 단일 geo 링크로 변환됨
     * 이전 본문에 있었던 모든 span은 삭제될 것임
     * <p>
     * This is really just an enhanced version of Linkify.addLinks().
     *
     * @param text - The string to search for links.
     * @param lastDitchGeo - If no links are found, turn the entire string into one geo link.
     *                     link가 없다면, 전체 문자열을 하나의 geo link로 전환함
     * @return Spannable object containing the list of URL spans found.
     *          발견된 URL span 목록을 포함하는 Spannable 객체
     */
    public static Spannable extendedLinkify(String text, boolean lastDitchGeo) {
        // We use a copy of the string argument so it's available for later if necessary.
        // string argument의 사본을 사용하기 때문에 필요하다면 나중에 사용할 수 있음
        Spannable spanText = SpannableString.valueOf(text);

        /*
         * If the text includes a street address like "1600 Amphitheater Parkway, 94043",
         * the current Linkify code will identify "94043" as a phone number and invite
         * you to dial it (and not provide a map link for the address).  For outside US,
         * use Linkify result if it spans the entire text.  Otherwise send the user to maps.
         * 텍스트에 "1600 Amphitheater Parkway, 94043"와 같은 도로 주소가 포함된 경우,
         * 현재의 Linkify 코드는 "94043"을 전화번호로 인식하고, 전화를 걸도록 다이얼로 보냄
         * (주소에 대한 지도 링크를 제공하지 않음)
         * 미국 이외의 경우, 전체 텍스트에 걸쳐 있는 경우, Linkify result를 사용하기
         * 또는 유저를 maps로 보내기
         */
        String defaultPhoneRegion = System.getProperty("user.region", "US");
        if (!defaultPhoneRegion.equals("US")) {
            Linkify.addLinks(spanText, Linkify.ALL);

            // If Linkify links the entire text, use that result.
            URLSpan[] spans = spanText.getSpans(0, spanText.length(), URLSpan.class);
            if (spans.length == 1) {
                int linkStart = spanText.getSpanStart(spans[0]);
                int linkEnd = spanText.getSpanEnd(spans[0]);
                if (linkStart <= indexFirstNonWhitespaceChar(spanText) &&
                        linkEnd >= indexLastNonWhitespaceChar(spanText) + 1) {
                    return spanText;
                }
            }

            // Otherwise, to be cautious and to try to prevent false positives, reset the spannable.
            // 그렇지 않은 경우, 잘못된 결과를 방지하기 위해서 spannable 재설정함
            spanText = SpannableString.valueOf(text);
            // If lastDitchGeo is true, default the entire string to geo.
            if (lastDitchGeo && !text.isEmpty()) {
                Linkify.addLinks(spanText, mWildcardPattern, "geo:0,0?q=");
            }
            return spanText;
        }

        /*
         * For within US, we want to have better recognition of phone numbers without losing
         * any of the existing annotations.  Ideally this would be addressed by improving Linkify.
         * For now we manage it as a second pass over the text.
         * 미국 내에서, 기존의 주석을 하나도 잃지 않고 전화번호를 더 잘 인식하기를 원함
         * 이상적으로 이것은 Linkify를 개선하여 해결할 수 있을 것임
         * 일단은 텍스트에 대한 제2의 pass로 관리함
         *
         *
         * URIs and e-mail addresses are pretty easy to pick out of text.  Phone numbers
         * are a bit tricky because they have radically different formats in different
         * countries, in terms of both the digits and the way in which they are commonly
         * written or presented (e.g. the punctuation and spaces in "(650) 555-1212").
         * The expected format of a street address is defined in WebView.findAddress().  It's
         * pretty narrowly defined, so it won't often match.
         * URI와 이메일 주소는 텍스트에서 뽑아내기 매우 쉬움
         * 전화번호는 일반적으로 쓰거나 표시하는 숫자와 방식(예: "(650) 555-1212"의 문장 및 공백)에
         * 있어서 서로 다른 국가에서는 형식이 근본적으로 다르기 때문에 까다로움
         * 거리 주소의 예상 형식은 WebView.findAddress()에 정의되어 있음
         * 상당히 좁게 정의되어 있어서 일치하는 일이 많지는 않을 것임
         *
         * The RFC 3966 specification defines the format of a "tel:" URI.
         * RFC 3966 규격은 "tel:" URI 형식을 정의함
         *
         * Start by letting Linkify find anything that isn't a phone number.  We have to let it
         * run first because every invocation removes all previous URLSpan annotations.
         * Linkify가 전화번호가 아닌 것을 찾도록 함
         * 모든 호출?이 이전의 모든 URLSpan 주석을 제거하기 때문에 먼저 실행되도록 해야 함
         *
         * Ideally we'd use the external/libphonenumber routines, but those aren't available
         * to unbundled applications.
         * 이상적으로 외부/libphonenumber 루틴을 사용하지만, 그것들은 unbundled된 애플리케이션에서는 사용할 수 없음
         */
        boolean linkifyFoundLinks = Linkify.addLinks(spanText,
                Linkify.ALL & ~(Linkify.PHONE_NUMBERS));

        /*
         * Get a list of any spans created by Linkify, for the coordinate overlapping span check.
         * Linkify에서 생성한 모든 span 목록을 가져와서 좌표 중첩 span 검사를 함
         */
        URLSpan[] existingSpans = spanText.getSpans(0, spanText.length(), URLSpan.class);

        /*
         * Check for coordinates.
         * This must be done before phone numbers because longitude may look like a phone number.
         * 좌표 확인
         * 경도가 전화번호처럼 보일 수 있기 때문에, 전화번호보다 먼저 처리해야 함
         */
        Matcher coordMatcher = COORD_PATTERN.matcher(spanText);
        int coordCount = 0;
        while (coordMatcher.find()) {
            int start = coordMatcher.start();
            int end = coordMatcher.end();
            if (spanWillOverlap(spanText, existingSpans, start, end)) {
                continue;
            }

            URLSpan span = new URLSpan("geo:0,0?q=" + coordMatcher.group());
            spanText.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            coordCount++;
        }

        /*
         * Update the list of existing spans, for the phone number overlapping span check.
         * 전화번호와 겹치는 span 확인, 기존 span 목록 업데이트
         */
        existingSpans = spanText.getSpans(0, spanText.length(), URLSpan.class);

        /*
         * Search for phone numbers.
         * 휴대폰 번호 찾기
         *
         * Some URIs contain strings of digits that look like phone numbers.  If both the URI
         * scanner and the phone number scanner find them, we want the URI link to win.  Since
         * the URI scanner runs first, we just need to avoid creating overlapping spans.
         * 일부 URI에는 전화번호처럼 보이는 숫자의 string이 포함되어 있음
         * 만약 URI 스캐너와 전화번호 스캐너가 그걸 찾으면, URI를 우선으로 하고 싶음
         * URI 스캐너가 먼저 실행되기 때문에 우리는 중복되는 span을 만드는 걸 피할 필요가 있음
         */
        int[] phoneSequences = findNanpPhoneNumbers(text);

        /*
         * Insert spans for the numbers we found.  We generate "tel:" URIs.
         * 찾은 번호에 대한 span을 삽입함, "tel:" URI 생성함
         */
        int phoneCount = 0;
        for (int match = 0; match < phoneSequences.length / 2; match++) {
            int start = phoneSequences[match*2];
            int end = phoneSequences[match*2 + 1];

            if (spanWillOverlap(spanText, existingSpans, start, end)) {
                continue;
            }

            /*
             * The Linkify code takes the matching span and strips out everything that isn't a
             * digit or '+' sign.  We do the same here.  Extension numbers will get appended
             * without a separator, but the dialer wasn't doing anything useful with ";ext="
             * anyway.
             * Linkify 코드는 일치하는 범위를 취해서 숫자 또는 '+' 문자가 아닌 모든 것을 삭제함
             * 여기서도 마찬가지임, 확장 번호는 구분 기호 없이 추가될 것이지만, 다이얼러는
             * ";ext="로 유용한 걸 하고 있지 않음...
             */

            //String dialStr = phoneUtil.format(match.number(),
            //        PhoneNumberUtil.PhoneNumberFormat.RFC3966);
            StringBuilder dialBuilder = new StringBuilder();
            for (int i = start; i < end; i++) {
                char ch = spanText.charAt(i);
                if (ch == '+' || Character.isDigit(ch)) {
                    dialBuilder.append(ch);
                }
            }
            URLSpan span = new URLSpan("tel:" + dialBuilder.toString());

            spanText.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            phoneCount++;
        }

        /*
         * If lastDitchGeo, and no other links have been found, set the entire string as a geo link.
         * lastDitchGeo와 다른 링크가 발견되지 않은 경우, 전체 string을 geo 링크로 설정함
         */
        if (lastDitchGeo && !text.isEmpty() &&
                !linkifyFoundLinks && phoneCount == 0 && coordCount == 0) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "No linkification matches, using geo default");
            }
            Linkify.addLinks(spanText, mWildcardPattern, "geo:0,0?q=");
        }

        return spanText;
    }

    private static int indexFirstNonWhitespaceChar(CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int indexLastNonWhitespaceChar(CharSequence str) {
        for (int i = str.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Finds North American Numbering Plan (NANP) phone numbers in the input text.
     * 입력 텍스트에서 NANP 전화번호 찾음
     *
     * @param text The text to scan.
     * @return A list of [start, end) pairs indicating the positions of phone numbers in the input.
     */
    // @VisibleForTesting
    static int[] findNanpPhoneNumbers(CharSequence text) {
        ArrayList<Integer> list = new ArrayList<Integer>();

        int startPos = 0;
        int endPos = text.length() - NANP_MIN_DIGITS + 1;
        if (endPos < 0) {
            return new int[] {};
        }

        /*
         * We can't just strip the whitespace out and crunch it down, because the whitespace
         * is significant.  March through, trying to figure out where numbers start and end.
         * 공백은 중요하기 때문에, 공백을 없애고 쪼갤 수가 없음
         * 어디서부터 시작해서 어떻게 끝내는지 알아내려고 노력함...
         */
        while (startPos < endPos) {
            // skip whitespace
            // 공백 건너뛰기
            while (Character.isWhitespace(text.charAt(startPos)) && startPos < endPos) {
                startPos++;
            }
            if (startPos == endPos) {
                break;
            }

            // check for a match at this position
            // 이 위치에서 맞는지 체크함
            int matchEnd = findNanpMatchEnd(text, startPos);
            if (matchEnd > startPos) {
                list.add(startPos);
                list.add(matchEnd);
                startPos = matchEnd;    // skip past match, 이미 일치된 건 건너뜀
            } else {
                // skip to next whitespace char
                // 다음 공백 char로 건너뜀
                while (!Character.isWhitespace(text.charAt(startPos)) && startPos < endPos) {
                    startPos++;
                }
            }
        }

        int[] result = new int[list.size()];
        for (int i = list.size() - 1; i >= 0; i--) {
            result[i] = list.get(i);
        }
        return result;
    }


    /**
     * Checks to see if there is a valid phone number in the input, starting at the specified
     * offset.  If so, the index of the last character + 1 is returned.  The input is assumed
     * to begin with a non-whitespace character.
     * 지정된 오프셋으로 시작하는 유효한 전화번호가 있는지 확인함
     * 만약 그렇다면, 마지막 문자+1의 인덱스가 반환됨
     * 입력은 공백이 아닌 문자로 시작하는 것으로 가정함(맨앞에 공백 없다는 것 같음)
     *
     * @return Exclusive end position, or -1 if not a match.
     *          배타적 종료 위치, 또는 일치되지 않는 경우 -1
     */
    private static int findNanpMatchEnd(CharSequence text, int startPos) {
        /*
         * A few interesting cases:
         *   94043                              # too short, ignore
         *   123456789012                       # too long, ignore
         *   +1 (650) 555-1212                  # 11 digits, spaces
         *   (650) 555 5555                     # Second space, only when first is present.
         *   (650) 555-1212, (650) 555-1213     # two numbers, return first
         *   1-650-555-1212                     # 11 digits with leading '1'
         *   *#650.555.1212#*!                  # 10 digits, include #*, ignore trailing '!'
         *   555.1212                           # 7 digits
         *
         * For the most part we want to break on whitespace, but it's common to leave a space
         * between the initial '1' and/or after the area code.
         * 대부분의 경우 우리는 여백을 없애고 싶어하지만, 초기'1'과/또는 지역 코드 뒤에
         * 공백을 남겨두는 것이 일반적임
         */

        // Check for "tel:" URI prefix.
        // "tel:" URI 접두사
        if (text.length() > startPos+4
                && text.subSequence(startPos, startPos+4).toString().equalsIgnoreCase("tel:")) {
            startPos += 4;
        }

        int endPos = text.length();
        int curPos = startPos;
        int foundDigits = 0;
        char firstDigit = 'x';
        boolean foundWhiteSpaceAfterAreaCode = false;

        while (curPos <= endPos) {
            char ch;
            if (curPos < endPos) {
                ch = text.charAt(curPos);
            } else {
                ch = 27;    // fake invalid symbol at end to trigger loop break
                // 트리거 루프 중단 시 fake invalid symbol
            }

            if (Character.isDigit(ch)) {
                if (foundDigits == 0) {
                    firstDigit = ch;
                }
                foundDigits++;
                if (foundDigits > NANP_MAX_DIGITS) {
                    // too many digits, stop early
                    // 숫자가 너무 많음, 일찍 정지함
                    return -1;
                }
            } else if (Character.isWhitespace(ch)) {
                if ( (firstDigit == '1' && foundDigits == 4) ||
                        (foundDigits == 3)) {
                    foundWhiteSpaceAfterAreaCode = true;
                } else if (firstDigit == '1' && foundDigits == 1) {
                } else if (foundWhiteSpaceAfterAreaCode
                        && ( (firstDigit == '1' && (foundDigits == 7)) || (foundDigits == 6))) {
                } else {
                    break;
                }
            } else if (NANP_ALLOWED_SYMBOLS.indexOf(ch) == -1) {
                break;
            }
            // else it's an allowed symbol
            // 허용된 심볼임

            curPos++;
        }

        if ((firstDigit != '1' && (foundDigits == 7 || foundDigits == 10)) ||
                (firstDigit == '1' && foundDigits == 11)) {
            // match
            return curPos;
        }

        return -1;
    }


    /**
     * Determines whether a new span at [start,end) will overlap with any existing span.
     * [start,end)에서 새 span이 기존 span과 겹칠지(overlap) 결정함
     */
    private static boolean spanWillOverlap(Spannable spanText, URLSpan[] spanList, int start,
                                           int end) {
        if (start == end) {
            // empty span, ignore
            return false;
        }
        for (URLSpan span : spanList) {
            int existingStart = spanText.getSpanStart(span);
            int existingEnd = spanText.getSpanEnd(span);
            if ((start >= existingStart && start < existingEnd) ||
                    end > existingStart && end <= existingEnd) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    CharSequence seq = spanText.subSequence(start, end);
                    Log.v(TAG, "Not linkifying " + seq + " as phone number due to overlap");
                }
                return true;
            }
        }

        return false;
    }


    /**
     * @param bundle The incoming bundle that contains the reminder info.
     *               리마인더 정보를 포함하는 들어오는 bundle
     * @return ArrayList<ReminderEntry> of the reminder minutes and methods.
     *          리마인더 분과 메소드의 ArrayList<ReminderEntry>
     */
    public static ArrayList<ReminderEntry> readRemindersFromBundle(Bundle bundle) {
        ArrayList<ReminderEntry> reminders = null;

        ArrayList<Integer> reminderMinutes = bundle.getIntegerArrayList(
                EventInfoFragment.BUNDLE_KEY_REMINDER_MINUTES);
        ArrayList<Integer> reminderMethods = bundle.getIntegerArrayList(
                EventInfoFragment.BUNDLE_KEY_REMINDER_METHODS);
        if (reminderMinutes == null || reminderMethods == null) {
            if (reminderMinutes != null || reminderMethods != null) {
                String nullList = (reminderMinutes == null?
                        "reminderMinutes" : "reminderMethods");
                Log.d(TAG, String.format("Error resolving reminders: %s was null",
                        nullList));
            }
            return null;
        }

        int numReminders = reminderMinutes.size();
        if (numReminders == reminderMethods.size()) {
            // Only if the size of the reminder minutes we've read in is
            // the same as the size of the reminder methods. Otherwise,
            // something went wrong with bundling them.
            // 리마인드 분의 크기가 리마인더 메소드의 크기와 같아야 함
            // 그렇지 않으면, bundle하는 것이 잘못됐음
            reminders = new ArrayList<ReminderEntry>(numReminders);
            for (int reminder_i = 0; reminder_i < numReminders;
                 reminder_i++) {
                int minutes = reminderMinutes.get(reminder_i);
                int method = reminderMethods.get(reminder_i);
                reminders.add(ReminderEntry.valueOf(minutes, method));
            }
        } else {
            Log.d(TAG, String.format("Error resolving reminders." +
                            " Found %d reminderMinutes, but %d reminderMethods.",
                    numReminders, reminderMethods.size()));
        }

        return reminders;
    }


    // A single strand represents one color of events. Events are divided up by
    // color to make them convenient to draw. The black strand is special in
    // that it holds conflicting events as well as color settings for allday on
    // each day.
    // 하나의 strand는 이벤트의 색상 하나를 나타냄,,, 이벤트는 색상별로 구분되어 있어서 draw하기 편리함
    // 검은색 strand는 매일? 종일 이벤트의 색 설정뿐만 아니라 충돌하는 이벤트를 다룬다는..?연다는..? 점에서 특별함
    public static class DNAStrand {
        public float[] points;
        public int[] allDays; // color for the allday, 0 means no event
        public int color;
        int position;
        int count;
    }

    public static class DNADiary {
        public float[] points;
        public int color;
        int position;
        int count;
    }

    // A segment is a single continuous length of time occupied by a single
    // color. Segments should never span multiple days.
    // segment는 한 가지 색으로 칠해지는 단일 연속 시간임, 여러 날에 걸쳐서는 안 됨
    private static class DNASegment {
        int startMinute; // in minutes since the start of the week 주의 시작일 기준으로 분 단위
        int endMinute;
        int color; // Calendar color or black for conflicts 충돌에 대한 캘린더 색상 또는 검정색
        int day; // quick reference to the day this segment is on 이 세그먼트가 있는 날짜에 대한 빠른 참조
    }

    private static class DNASegmentD {
        int startMinute; // in minutes since the start of the week 주의 시작일 기준으로 분 단위
        int endMinute;
        int color;
        long day;
    }


    private static class CalendarBroadcastReceiver extends BroadcastReceiver {

        Runnable mCallBack;

        public CalendarBroadcastReceiver(Runnable callback) {
            super();
            mCallBack = callback;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_DATE_CHANGED) ||
                    intent.getAction().equals(Intent.ACTION_TIME_CHANGED) ||
                    intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED) ||
                    intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                if (mCallBack != null) {
                    mCallBack.run();
                }
            }
        }
    }

}