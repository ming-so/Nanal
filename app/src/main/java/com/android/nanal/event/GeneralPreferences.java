package com.android.nanal.event;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarCache;
import android.provider.SearchRecentSuggestions;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.SparseIntArray;
import android.widget.Toast;

import com.android.nanal.CalendarRecentSuggestionsProvider;
import com.android.nanal.DynamicTheme;
import com.android.nanal.R;
import com.android.nanal.activity.SettingsActivity;
import com.android.nanal.alerts.AlertReceiver;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarUtils;
import com.android.nanal.color.ColorPickerDialog;
import com.android.nanal.color.ColorPickerSwatch;
import com.android.nanal.timezonepicker.TimeZoneInfo;
import com.android.nanal.timezonepicker.TimeZonePickerDialog;
import com.android.nanal.timezonepicker.TimeZonePickerDialog.OnTimeZoneSetListener;
import com.android.nanal.timezonepicker.TimeZonePickerUtils;

/*
 * Copyright (C) 2007 The Android Open Source Project
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

public class GeneralPreferences extends PreferenceFragment implements
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener, OnTimeZoneSetListener {
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

    public static final String KEY_DEFAULT_GROUP = "preference_defaultGroup";

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
    public static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";
    static final String SHARED_PREFS_NAME_NO_BACKUP = "com.android.calendar_preferences_no_backup";
    static final String KEY_HOME_TZ_ENABLED = "preferences_home_tz_enabled";
    static final String KEY_HOME_TZ = "preferences_home_tz";
    private static final String FRAG_TAG_TIME_ZONE_PICKER = "TimeZonePicker";

    CheckBoxPreference mAlert;
    Preference mNotification;
    CheckBoxPreference mVibrate;
    RingtonePreference mRingtone;
    CheckBoxPreference mPopup;
    CheckBoxPreference mUseHomeTZ;
    CheckBoxPreference mHideDeclined;
    Preference mHomeTZ;
    TimeZonePickerUtils mTzPickerUtils;
//    ListPreference mTheme;
    Preference mColor;
    ListPreference mWeekStart;
    ListPreference mDayWeek;
    ListPreference mDefaultReminder;
    ListPreference mDefaultEventDuration;
    ListPreference mSnoozeDelay;
    ListPreference mDefaultStart;

    private String mTimeZoneId;

    // Used to retrieve the color id from the color picker
    // 색상 선택기에서 색상 ID 검색에 사용함
    private SparseIntArray colorMap = new SparseIntArray();

    /** Return a properly configured SharedPreferences instance
     * 올바르게 구성된 SharedPreferences 인스턴스 반환 */
    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Set the default shared preferences in the proper context
     * 적절한 컨텍스트의 기본 공유 Preference을 설정 */
    public static void setDefaultValues(Context context) {
        if (Utils.isOreoOrLater()) {
            PreferenceManager.setDefaultValues(context, SHARED_PREFS_NAME, Context.MODE_PRIVATE,
                    R.xml.general_preferences_oreo_and_up, false);
        } else {
            PreferenceManager.setDefaultValues(context, SHARED_PREFS_NAME, Context.MODE_PRIVATE,
                    R.xml.general_preferences, false);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Activity activity = getActivity();

        initializeColorMap();

        // Make sure to always use the same preferences file regardless of the package name
        // we're running under
        // 실행 중인 패키지 이름과 관계없이 항상 동일한 Preference 파일을 사용함
        final PreferenceManager preferenceManager = getPreferenceManager();
        final SharedPreferences sharedPreferences = getSharedPreferences(activity);
        preferenceManager.setSharedPreferencesName(SHARED_PREFS_NAME);

        // Load the preferences from an XML resource
        // XML 리소스에서 Preference 로드
        if (Utils.isOreoOrLater()) {
            addPreferencesFromResource(R.xml.general_preferences_oreo_and_up);
        } else {
            addPreferencesFromResource(R.xml.general_preferences);
        }
        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        if (Utils.isOreoOrLater()) {
            mNotification = preferenceScreen.findPreference(KEY_NOTIFICATION);
        } else {
            mAlert = (CheckBoxPreference) preferenceScreen.findPreference(KEY_ALERTS);
            mVibrate = (CheckBoxPreference) preferenceScreen.findPreference(KEY_ALERTS_VIBRATE);
            Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) {
                PreferenceCategory mAlertGroup = (PreferenceCategory) preferenceScreen
                        .findPreference(KEY_ALERTS_CATEGORY);
                mAlertGroup.removePreference(mVibrate);
            }
            mRingtone = (RingtonePreference) preferenceScreen.findPreference(KEY_ALERTS_RINGTONE);
            String ringToneUri = Utils.getRingTonePreference(activity);

            // Set the ringToneUri to the backup-able shared pref only so that
            // the Ringtone dialog will open up with the correct value.
            // Ringtone 대화상자가 올바른 값으로 열리도록 ringToneUri를 백업 가능한
            // 공유 pref로 설정함
            final Editor editor = preferenceScreen.getEditor();
            editor.putString(GeneralPreferences.KEY_ALERTS_RINGTONE, ringToneUri).apply();

            String ringtoneDisplayString = getRingtoneTitleFromUri(activity, ringToneUri);
            mRingtone.setSummary(ringtoneDisplayString == null ? "" : ringtoneDisplayString);
        }

        mPopup = (CheckBoxPreference) preferenceScreen.findPreference(KEY_ALERTS_POPUP);
        mUseHomeTZ = (CheckBoxPreference) preferenceScreen.findPreference(KEY_HOME_TZ_ENABLED);
        //mTheme = (ListPreference) preferenceScreen.findPreference(KEY_THEME_PREF);
        mColor = preferenceScreen.findPreference(KEY_COLOR_PREF);
        mDefaultStart = (ListPreference) preferenceScreen.findPreference(KEY_DEFAULT_START);
        mHideDeclined = (CheckBoxPreference) preferenceScreen.findPreference(KEY_HIDE_DECLINED);
        mWeekStart = (ListPreference) preferenceScreen.findPreference(KEY_WEEK_START_DAY);
        mDayWeek = (ListPreference) preferenceScreen.findPreference(KEY_DAYS_PER_WEEK);
        mDefaultReminder = (ListPreference) preferenceScreen.findPreference(KEY_DEFAULT_REMINDER);
        mDefaultEventDuration = (ListPreference) preferenceScreen.findPreference(KEY_DEFAULT_EVENT_DURATION);
        mDefaultEventDuration.setSummary(mDefaultEventDuration.getEntry());
        mHomeTZ = preferenceScreen.findPreference(KEY_HOME_TZ);
        mSnoozeDelay = (ListPreference) preferenceScreen.findPreference(KEY_DEFAULT_SNOOZE_DELAY);
        buildSnoozeDelayEntries();
//        mTheme.setSummary(mTheme.getEntry());
        mWeekStart.setSummary(mWeekStart.getEntry());
        mDayWeek.setSummary(mDayWeek.getEntry());
        mDefaultReminder.setSummary(mDefaultReminder.getEntry());
        mSnoozeDelay.setSummary(mSnoozeDelay.getEntry());
        mDefaultStart.setSummary(mDefaultStart.getEntry());

        mColor.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showColorPickerDialog();
                return true;
            }
        });

        // This triggers an asynchronous call to the provider to refresh the data in shared pref
        // provider에 대한 비동기식 호출을 트리거하여 공유 사전 설정에서 데이터를 새로고침
        mTimeZoneId = Utils.getTimeZone(activity, null);

        SharedPreferences prefs = CalendarUtils.getSharedPreferences(activity,
                Utils.SHARED_PREFS_NAME);

        // Utils.getTimeZone will return the currentTimeZone instead of the one
        // in the shared_pref if home time zone is disabled. So if home tz is
        // off, we will explicitly read it.
        // Utils.getTimeZone은 홈 시간대가 비활성화된 경우, shared_pref에 있는 currentTimeZone을 반환함
        // 그래서 홈 시간대가 꺼져 있어도 읽을 수 있음
        if (!prefs.getBoolean(KEY_HOME_TZ_ENABLED, false)) {
            mTimeZoneId = prefs.getString(KEY_HOME_TZ, Time.getCurrentTimezone());
        }

        mHomeTZ.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showTimezoneDialog();
                return true;
            }
        });

        if (mTzPickerUtils == null) {
            mTzPickerUtils = new TimeZonePickerUtils(getActivity());
        }
        CharSequence timezoneName = mTzPickerUtils.getGmtDisplayName(getActivity(), mTimeZoneId,
                System.currentTimeMillis(), false);
        mHomeTZ.setSummary(timezoneName != null ? timezoneName : mTimeZoneId);

        TimeZonePickerDialog tzpd = (TimeZonePickerDialog) activity.getFragmentManager()
                .findFragmentByTag(FRAG_TAG_TIME_ZONE_PICKER);
        if (tzpd != null) {
            tzpd.setOnTimeZoneSetListener(this);
        }

        migrateOldPreferences(sharedPreferences);

    }

    private void showColorPickerDialog() {
        final ColorPickerDialog colorPickerDialog = new ColorPickerDialog();
        // Retrieve current color to show it as selected
        // 현재 색상을 검색하여 선택한 색으로 표시함
        String selectedColorName = Utils.getSharedPreference(getActivity(), KEY_COLOR_PREF,"teal");
        int selectedColor = getResources().getColor(DynamicTheme.getColorId(selectedColorName));

        colorPickerDialog.initialize(R.string.preferences_color_pick,
                new int[]{
                        getResources().getColor(R.color.colorPrimary),
                        getResources().getColor(R.color.colorBluePrimary),
                        getResources().getColor(R.color.colorPurplePrimary),
                        getResources().getColor(R.color.colorRedPrimary),
                        getResources().getColor(R.color.colorOrangePrimary),
                        getResources().getColor(R.color.colorGreenPrimary)
                },selectedColor,3,2);

        colorPickerDialog.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int colour) {
                Utils.setSharedPreference(getActivity(), KEY_COLOR_PREF, DynamicTheme.getColorName(colorMap.get(colour)));
            }
        });

        FragmentManager fm = this.getFragmentManager();
        colorPickerDialog.show(fm, "colorpicker");
    }

    private void initializeColorMap () {
        colorMap.put(getResources().getColor(R.color.colorPrimary),R.color.colorPrimary);
        colorMap.put(getResources().getColor(R.color.colorBluePrimary),R.color.colorBluePrimary);
        colorMap.put(getResources().getColor(R.color.colorOrangePrimary),R.color.colorOrangePrimary);
        colorMap.put(getResources().getColor(R.color.colorGreenPrimary),R.color.colorGreenPrimary);
        colorMap.put(getResources().getColor(R.color.colorRedPrimary),R.color.colorRedPrimary);
        colorMap.put(getResources().getColor(R.color.colorPurplePrimary),R.color.colorPurplePrimary);
    }

    private void showTimezoneDialog() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        Bundle b = new Bundle();
        b.putLong(TimeZonePickerDialog.BUNDLE_START_TIME_MILLIS, System.currentTimeMillis());
        b.putString(TimeZonePickerDialog.BUNDLE_TIME_ZONE, Utils.getTimeZone(activity, null));

        FragmentManager fm = getActivity().getFragmentManager();
        TimeZonePickerDialog tzpd = (TimeZonePickerDialog) fm
                .findFragmentByTag(FRAG_TAG_TIME_ZONE_PICKER);
        if (tzpd != null) {
            tzpd.dismiss();
        }
        tzpd = new TimeZonePickerDialog();
        tzpd.setArguments(b);
        tzpd.setOnTimeZoneSetListener(this);
        tzpd.show(fm, FRAG_TAG_TIME_ZONE_PICKER);
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        setPreferenceListeners(this);
    }

    /**
     * Sets up all the preference change listeners to use the specified
     * listener.
     * 지정된 listener를 사용하도록 모든 Preference 변경 listener를 설정함
     */
    private void setPreferenceListeners(OnPreferenceChangeListener listener) {
        mUseHomeTZ.setOnPreferenceChangeListener(listener);
        mHomeTZ.setOnPreferenceChangeListener(listener);
//        mTheme.setOnPreferenceChangeListener(listener);
        mColor.setOnPreferenceChangeListener(listener);
        mDefaultStart.setOnPreferenceChangeListener(listener);
        mWeekStart.setOnPreferenceChangeListener(listener);
        mDayWeek.setOnPreferenceChangeListener(listener);
        mDefaultReminder.setOnPreferenceChangeListener(listener);
        mSnoozeDelay.setOnPreferenceChangeListener(listener);
        mHideDeclined.setOnPreferenceChangeListener(listener);
        mDefaultEventDuration.setOnPreferenceChangeListener(listener);
        if (Utils.isOreoOrLater()) {
            mNotification.setOnPreferenceChangeListener(listener);
        } else {
            mVibrate.setOnPreferenceChangeListener(listener);
            mRingtone.setOnPreferenceChangeListener(listener);
        }
    }

    @Override
    public void onStop() {
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Activity a = getActivity();
        if (key.equals(KEY_ALERTS)) {

            if (a != null) {
                Intent intent = new Intent();
                intent.setClass(a, AlertReceiver.class);
                if (mAlert.isChecked()) {
                    intent.setAction(AlertReceiver.ACTION_DISMISS_OLD_REMINDERS);
                } else {
                    intent.setAction(AlertReceiver.EVENT_REMINDER_APP_ACTION);
                }
                a.sendBroadcast(intent);
            }
        }
        if (a != null) {
            BackupManager.dataChanged(a.getPackageName());
        }

        if (key.equals(KEY_THEME_PREF) || key.equals(KEY_COLOR_PREF)) {
            //((CalendarSettingsActivity)getActivity()).restartActivity();
            ((SettingsActivity)getActivity()).restartActivity();
        }
    }

    /**
     * Handles time zone preference changes
     * 시간대 Preference 변경 처리
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String tz;
        final Activity activity = getActivity();
        if (preference == mUseHomeTZ) {
            if ((Boolean)newValue) {
                tz = mTimeZoneId;
            } else {
                tz = CalendarCache.TIMEZONE_TYPE_AUTO;
            }
            Utils.setTimeZone(activity, tz);
            return true;
//        } else if (preference == mTheme) {
//            mTheme.setValue((String) newValue);
//            mTheme.setSummary(mTheme.getEntry());
        } else if (preference == mHideDeclined) {
            mHideDeclined.setChecked((Boolean) newValue);
            Intent intent = new Intent(Utils.getWidgetScheduledUpdateAction(activity));
            intent.setDataAndType(CalendarContract.CONTENT_URI, Utils.APPWIDGET_DATA_TYPE);
            activity.sendBroadcast(intent);
            return true;
        } else if (preference == mWeekStart) {
            mWeekStart.setValue((String) newValue);
            mWeekStart.setSummary(mWeekStart.getEntry());
        } else if (preference == mDayWeek) {
            mDayWeek.setValue((String) newValue);
            mDayWeek.setSummary(mDayWeek.getEntry());
        } else if (preference == mDefaultEventDuration) {
            mDefaultEventDuration.setValue((String) newValue);
            mDefaultEventDuration.setSummary(mDefaultEventDuration.getEntry());
        } else if (preference == mDefaultReminder) {
            mDefaultReminder.setValue((String) newValue);
            mDefaultReminder.setSummary(mDefaultReminder.getEntry());
        } else if (preference == mSnoozeDelay) {
            mSnoozeDelay.setValue((String) newValue);
            mSnoozeDelay.setSummary(mSnoozeDelay.getEntry());
        } else if (preference == mRingtone) {
            if (newValue instanceof String) {
                Utils.setRingTonePreference(activity, (String) newValue);
                String ringtone = getRingtoneTitleFromUri(activity, (String) newValue);
                mRingtone.setSummary(ringtone == null ? "" : ringtone);
            }
            return true;
        } else if (preference == mVibrate) {
            mVibrate.setChecked((Boolean) newValue);
            return true;
        } else if (preference == mDefaultStart) {
            int i = mDefaultStart.findIndexOfValue((String) newValue);
            mDefaultStart.setSummary(mDefaultStart.getEntries()[i]);
            return true;
        } else {
            return true;
        }
        return false;
    }

    public String getRingtoneTitleFromUri(Context context, String uri) {
        if (TextUtils.isEmpty(uri)) {
            return null;
        }

        Ringtone ring = RingtoneManager.getRingtone(getActivity(), Uri.parse(uri));
        if (ring != null) {
            return ring.getTitle(context);
        }
        return null;
    }

    /**
     * If necessary, upgrades previous versions of preferences to the current
     * set of keys and values.
     * 필요한 경우, 이전 버전의 Preference을 현재 키 및 값 set으로 업그레이드함
     * @param prefs the preferences to upgrade
     */
    private void migrateOldPreferences(SharedPreferences prefs) {
        // If needed, migrate vibration setting from a previous version
        // 필요한 경우, 이전 버전에서 진동 설정 마이그레이션
        if (!Utils.isOreoOrLater()) {
            mVibrate.setChecked(Utils.getDefaultVibrate(getActivity(), prefs));
        }
    }

    private void buildSnoozeDelayEntries() {
        final CharSequence[] values = mSnoozeDelay.getEntryValues();
        final int count = values.length;
        final CharSequence[] entries = new CharSequence[count];

        for (int i = 0; i < count; i++) {
            int value = Integer.parseInt(values[i].toString());
            entries[i] = EventViewUtils.constructReminderLabel(getActivity(), value, false);
        }

        mSnoozeDelay.setEntries(entries);
    }

    @Override
    public boolean onPreferenceTreeClick(
            PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();
        if (KEY_CLEAR_SEARCH_HISTORY.equals(key)) {
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                    Utils.getSearchAuthority(getActivity()),
                    CalendarRecentSuggestionsProvider.MODE);
            suggestions.clearHistory();
            Toast.makeText(getActivity(), R.string.search_history_cleared,
                    Toast.LENGTH_SHORT).show();
            return true;
        } else if (KEY_NOTIFICATION.equals(key)) {
            Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, "alert_channel_01");
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
            startActivity(intent);
            return true;
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }

    @Override
    public void onTimeZoneSet(TimeZoneInfo tzi) {
        if (mTzPickerUtils == null) {
            mTzPickerUtils = new TimeZonePickerUtils(getActivity());
        }

        final CharSequence timezoneName = mTzPickerUtils.getGmtDisplayName(
                getActivity(), tzi.mTzId, System.currentTimeMillis(), false);
        mHomeTZ.setSummary(timezoneName);
        Utils.setTimeZone(getActivity(), tzi.mTzId);
    }
}
