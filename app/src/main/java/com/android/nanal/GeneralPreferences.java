package com.android.nanal;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceFragment;

public class GeneralPreferences extends PreferenceFragment {
    // Preference keys
    // Preference 키
    public static final String KEY_THEME_PREF = "pref_theme";
    public static final String KEY_COLOR_PREF = "pref_color";
    public static final String KEY_DEFAULT_START = "preferences_default_start";
    public static final String KEY_HIDE_DECLINED = "preferences_hide_declined";
    public static final String KEY_WEEK_START_DAY = "preferences_week_start_day";
    public static final String KEY_SHOW_WEEK_NUM = "preferences_show_week_num";
    public static final String KEY_DAYS_PER_WEEK = "preferences_days_per_week";
    public static final String KEY_MDAYS_PER_WEEK = "preferences_mdays_per_week";
    public static final String KEY_SKIP_SETUP = "preferences_skip_setup";
    public static final String KEY_CLEAR_SEARCH_HISTORY = "preferences_clear_search_history";
    public static final String KEY_ALERTS_CATEGORY = "preferences_alerts_category";
    public static final String KEY_ALERTS = "preferences_alerts";
    public static final String KEY_NOTIFICATION = "preferences_notification";
    public static final String KEY_ALERTS_VIBRATE = "preferences_alerts_vibrate";
    public static final String KEY_ALERTS_RINGTONE = "preferences_alerts_ringtone";
    public static final String KEY_ALERTS_POPUP = "preferences_alerts_popup";
    public static final String KEY_SHOW_CONTROLS = "preferences_show_controls";
    public static final String KEY_DEFAULT_REMINDER = "preferences_default_reminder";
    public static final int NO_REMINDER = -1;
    public static final String NO_REMINDER_STRING = "-1";
    public static final int REMINDER_DEFAULT_TIME = 10; // in minutes
    public static final String KEY_USE_CUSTOM_SNOOZE_DELAY = "preferences_custom_snooze_delay";
    public static final String KEY_DEFAULT_SNOOZE_DELAY = "preferences_default_snooze_delay";
    public static final int SNOOZE_DELAY_DEFAULT_TIME = 5; // in minutes
    public static final String KEY_DEFAULT_CELL_HEIGHT = "preferences_default_cell_height";
    public static final String KEY_VERSION = "preferences_version";
    /** Key to SharePreference for default view (CalendarController.ViewType)
     * 기본 view에 대한 SharePreference 키(CalendarController.ViewType) */
    public static final String KEY_START_VIEW = "preferred_startView";
    /**
     *  Key to SharePreference for default detail view (CalendarController.ViewType)
     *  Typically used by widget
     *  기본 상세 view에 대한 SharePreference 키 (CalendarController.ViewType)
     *  일반적으로 위젯에서 사용
     */
    public static final String KEY_DETAILED_VIEW = "preferred_detailedView";
    public static final String KEY_DEFAULT_CALENDAR = "preference_defaultCalendar";

    /** Key to preference for default new event duration (if provider doesn't indicate one)
     * 기본 새 이벤트 지속 시간에 대한 Preference 키(제공자가 해당 이벤트를 표시하지 않는 경우) */
    public static final String KEY_DEFAULT_EVENT_DURATION = "preferences_default_event_duration";
    public static final String EVENT_DURATION_DEFAULT="60";

    // These must be in sync with the array preferences_week_start_day_values
    // 이것들은 반드시 배열 preferences_..._values와 동기화되어야 함
    public static final String WEEK_START_DEFAULT = "-1";
    public static final String WEEK_START_SATURDAY = "7";
    public static final String WEEK_START_SUNDAY = "1";
    public static final String WEEK_START_MONDAY = "2";
    // Default preference values
    // Preference 값
    public static final String DEFAULT_DEFAULT_START = "-2";
    public static final int DEFAULT_START_VIEW = CalendarController.ViewType.WEEK;
    public static final int DEFAULT_DETAILED_VIEW = CalendarController.ViewType.DAY;
    public static final boolean DEFAULT_SHOW_WEEK_NUM = false;
    // This should match the XML file.
    // XML 파일과 일치해야 함
    public static final String DEFAULT_RINGTONE = "content://settings/system/notification_sound";
    // The name of the shared preferences file. This name must be maintained for historical
    // reasons, as it's what PreferenceManager assigned the first time the file was created.
    // 공유 Preference 파일의 이름
    // 이 이름은 파일이 처음 만들어졌을 때 PreferenceManager가 할당했던 것이기 때문에 역사적인
    // 이유로 유지되어야 함
    static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";
    static final String SHARED_PREFS_NAME_NO_BACKUP = "com.android.calendar_preferences_no_backup";
    static final String KEY_HOME_TZ_ENABLED = "preferences_home_tz_enabled";
    static final String KEY_HOME_TZ = "preferences_home_tz";
    private static final String FRAG_TAG_TIME_ZONE_PICKER = "TimeZonePicker";

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }
}
