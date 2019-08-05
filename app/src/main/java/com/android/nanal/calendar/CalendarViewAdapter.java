package com.android.nanal.calendar;

/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.content.Loader;
import android.content.Loader.OnLoadCompleteListener;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.nanal.calendar.CalendarController.ViewType;
import com.android.nanal.calendar.LunarUtils.LunarInfoLoader;
import com.android.nanal.R;
import com.android.nanal.event.Utils;

import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;




/*
 * The MenuSpinnerAdapter defines the look of the ActionBar's pull down menu
 * for small screen layouts. The pull down menu replaces the tabs uses for big screen layouts
 * MenuSpinnerAdapter는 작은 화면 레이아웃을 위한 ActionBar의 풀다운 메뉴의 모양을 정의함
 * 풀다운 메뉴는 대형 화면 레이아웃의 탭 사용을 대체함
 *
 * The MenuSpinnerAdapter responsible for creating the views used for in the pull down menu.
 * MenuSpinnerAdapter는 풀다운 메뉴에서 사용되는 view들을 만드는 역할을 함
 */

public class CalendarViewAdapter extends BaseAdapter {

    public static final int DAY_BUTTON_INDEX = 0;
    public static final int WEEK_BUTTON_INDEX = 1;
    public static final int MONTH_BUTTON_INDEX = 2;
    public static final int AGENDA_BUTTON_INDEX = 3;
    static final int VIEW_TYPE_NUM = 1;  // Increase this if you add more view types
    private static final String TAG = "MenuSpinnerAdapter";
    // Defines the types of view returned by this spinner
    private static final int BUTTON_VIEW_TYPE = 0;
    private final String mButtonNames [];           // Text on buttons
    private final LayoutInflater mInflater;
    private final Context mContext;
    private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    private final boolean mShowDate;   // Spinner mode indicator (view name or view name with date)
    // Used to define the look of the menu button according to the current view:
    // Day view: show day of the week + full date underneath
    // Week view: show the month + year
    // Month view: show the month + year
    // Agenda view: show day of the week + full date underneath
    // 스피너 모드 표시기 (view 이름 또는 날짜가 있는 view 이름)
    // 현재 view에 따라 메뉴 버튼의 모양을 정의하는 데 사용함:
    // 일 view: 요일 + 밑에? 전체 날짜 표시
    // 주 view: 월 + 연도 표시
    // 월 view: 월 + 연도 표시
    // Agenda view: 요일 + 전체 날짜 표시
    private int mCurrentMainView;
    // The current selected event's time, used to calculate the date and day of the week
    // for the buttons.
    // 현재 선택된 이벤트의 시간, 버튼에 대한 날짜 및 요일을 계산하는 데 사용됨
    private long mMilliTime;
    private String mTimeZone;
    private long mTodayJulianDay;
    private Handler mMidnightHandler = null; // Used to run a time update every midnight
    private LunarInfoLoader mLunarLoader = null;

    // Updates time specific variables (time-zone, today's Julian day).
    // 시간별 변수 업데이트(시간대, 오늘의 줄리안 데이)
    private final Runnable mTimeUpdater = new Runnable() {
        @Override
        public void run() {
            refresh(mContext);
        }
    };

    private OnLoadCompleteListener<Void> mLunarLoaderListener = new OnLoadCompleteListener<Void>() {
        @Override
        public void onLoadComplete(Loader<Void> loader, Void data) {
            notifyDataSetChanged();
        }
    };

    public CalendarViewAdapter(Context context, int viewType, boolean showDate) {
        super();

        mMidnightHandler = new Handler();
        mCurrentMainView = viewType;
        mContext = context;
        mShowDate = showDate;

        // Initialize 초기화
        mButtonNames = context.getResources().getStringArray(R.array.buttons_list);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

        // Sets time specific variables and starts a thread for midnight updates
        // 특정 시간 변수를 설정하고 자정 업데이트에 대한 스레드 시작
        if (showDate) {
            refresh(context);
        }

        mLunarLoader = new LunarInfoLoader(mContext);
        mLunarLoader.registerListener(0, mLunarLoaderListener);
    }

    @Override
    protected void finalize() throws Throwable {
        LunarUtils.clearInfo();
        if (mLunarLoader != null && mLunarLoaderListener != null) {
            mLunarLoader.unregisterListener(mLunarLoaderListener);
        }
        super.finalize();
    }

    // Sets the time zone and today's Julian day to be used by the adapter.
    // Also, notify listener on the change and resets the midnight update thread.
    // 어댑터가 사용할 시간대와 오늘의 줄리안 데이를 설정함
    // 또한, listener에게 변경 사항을 알리고, 자정 업데이트 스레드를 리셋함
    public void refresh(Context context) {
        mTimeZone = Utils.getTimeZone(context, mTimeUpdater);
        Time time = new Time(mTimeZone);
        long now = System.currentTimeMillis();
        time.set(now);
        mTodayJulianDay = Time.getJulianDay(now, time.gmtoff);
        notifyDataSetChanged();
        setMidnightHandler();
    }

    // Sets a thread to run 1 second after midnight and update the current date
    // This is used to display correctly the date of yesterday/today/tomorrow
    // 자정이 지난 후 1초 동안 실행되도록 스레드를 설정하고, 현재 날짜를 업데이트함
    // 어제/오늘/내일 날짜를 올바르게 표시하도록 사용함
    private void setMidnightHandler() {
        mMidnightHandler.removeCallbacks(mTimeUpdater);
        // Set the time updater to run at 1 second after midnight
        // 자정 이후 1초 동안 실행되도록 시간 업데이터 설정
        long now = System.currentTimeMillis();
        Time time = new Time(mTimeZone);
        time.set(now);
        long runInMillis = (24 * 3600 - time.hour * 3600 - time.minute * 60 -
                time.second + 1) * 1000;
        mMidnightHandler.postDelayed(mTimeUpdater, runInMillis);
    }

    // Stops the midnight update thread, called by the activity when it is paused.
    // activity가 일시정지될 때, 호출되는 자정 업데이트 스레드를 중지함
    public void onPause() {
        mMidnightHandler.removeCallbacks(mTimeUpdater);
    }

    // Returns the amount of buttons in the menu
    // 메뉴에 있는 버튼의 개수를 반환함
    @Override
    public int getCount() {
        return mButtonNames.length;
    }


    @Override
    public Object getItem(int position) {
        if (position < mButtonNames.length) {
            return mButtonNames[position];
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        // Item ID is its location in the list
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v;

        if (mShowDate) {
            // Check if can recycle the view
            // view를 재활용할 수 있는지 체크함
            if (convertView == null || ((Integer) convertView.getTag()).intValue()
                    != R.layout.actionbar_pulldown_menu_top_button) {
                v = mInflater.inflate(R.layout.actionbar_pulldown_menu_top_button, parent, false);
                // Set the tag to make sure you can recycle it when you get it
                // as a convert view
                // convert view로 가져올 때 재활용할 수 있도록 태그를 설정함
                v.setTag(R.layout.actionbar_pulldown_menu_top_button);
            } else {
                v = convertView;
            }
            TextView weekDay = (TextView) v.findViewById(R.id.top_button_weekday);
            TextView lunarInfo = (TextView) v.findViewById(R.id.top_button_lunar);
            TextView date = (TextView) v.findViewById(R.id.top_button_date);

            switch (mCurrentMainView) {
                case ViewType.DAY:
                    weekDay.setVisibility(View.VISIBLE);
                    weekDay.setText(buildDayOfWeek());
                    if (LunarUtils.showLunar(mContext)) {
                        lunarInfo.setVisibility(View.VISIBLE);
                        Time time = new Time(mTimeZone);
                        time.set(mMilliTime);
                        int flag = LunarUtils.FORMAT_LUNAR_LONG | LunarUtils.FORMAT_MULTI_FESTIVAL;
                        String lunar = LunarUtils.get(mContext, time.year, time.month,
                                time.monthDay, flag, false, null);
                        if (!TextUtils.isEmpty(lunar)) {
                            lunarInfo.setText(lunar);
                        }
                    } else {
                        lunarInfo.setVisibility(View.GONE);
                    }
                    date.setText(buildFullDate());
                    break;
                case ViewType.WEEK:
                    lunarInfo.setVisibility(View.GONE);
                    if (Utils.getShowWeekNumber(mContext)) {
                        weekDay.setVisibility(View.VISIBLE);
                        weekDay.setText(buildWeekNum());
                    } else {
                        weekDay.setVisibility(View.GONE);
                    }
                    date.setText(buildMonthYearDate());
                    break;
                case ViewType.MONTH:
                    weekDay.setVisibility(View.GONE);
                    lunarInfo.setVisibility(View.GONE);
                    date.setText(buildMonthYearDate());
                    break;
                case ViewType.AGENDA:
                    weekDay.setVisibility(View.VISIBLE);
                    lunarInfo.setVisibility(View.GONE);
                    weekDay.setText(buildDayOfWeek());
                    date.setText(buildFullDate());
                    break;
                default:
                    v = null;
                    break;
            }
        } else {
            if (convertView == null || ((Integer) convertView.getTag()).intValue()
                    != R.layout.actionbar_pulldown_menu_top_button_no_date) {
                v = mInflater.inflate(
                        R.layout.actionbar_pulldown_menu_top_button_no_date, parent, false);
                // Set the tag to make sure you can recycle it when you get it
                // as a convert view
                // convert view로 가져올 때 재활용할 수 있도록 태그를 설정함
                v.setTag(R.layout.actionbar_pulldown_menu_top_button_no_date);
            } else {
                v = convertView;
            }
            TextView title = (TextView) v;
            switch (mCurrentMainView) {
                case ViewType.DAY:
                    title.setText(mButtonNames [DAY_BUTTON_INDEX]);
                    break;
                case ViewType.WEEK:
                    title.setText(mButtonNames [WEEK_BUTTON_INDEX]);
                    break;
                case ViewType.MONTH:
                    title.setText(mButtonNames [MONTH_BUTTON_INDEX]);
                    break;
                case ViewType.AGENDA:
                    title.setText(mButtonNames [AGENDA_BUTTON_INDEX]);
                    break;
                default:
                    v = null;
                    break;
            }
        }
        return v;
    }

    @Override
    public int getItemViewType(int position) {
        // Only one kind of view is used
        // 사용되는 view는 하나임
        return BUTTON_VIEW_TYPE;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_NUM;
    }

    @Override
    public boolean isEmpty() {
        return (mButtonNames.length == 0);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = mInflater.inflate(R.layout.actionbar_pulldown_menu_button, parent, false);
        TextView viewType = (TextView)v.findViewById(R.id.button_view);
        TextView date = (TextView)v.findViewById(R.id.button_date);
        switch (position) {
            case DAY_BUTTON_INDEX:
                viewType.setText(mButtonNames [DAY_BUTTON_INDEX]);
                if (mShowDate) {
                    date.setText(buildMonthDayDate());
                }
                break;
            case WEEK_BUTTON_INDEX:
                viewType.setText(mButtonNames [WEEK_BUTTON_INDEX]);
                if (mShowDate) {
                    date.setText(buildWeekDate());
                }
                break;
            case MONTH_BUTTON_INDEX:
                viewType.setText(mButtonNames [MONTH_BUTTON_INDEX]);
                if (mShowDate) {
                    date.setText(buildMonthDate());
                }
                break;
            case AGENDA_BUTTON_INDEX:
                viewType.setText(mButtonNames [AGENDA_BUTTON_INDEX]);
                if (mShowDate) {
                    date.setText(buildMonthDayDate());
                }
                break;
            default:
                v = convertView;
                break;
        }
        return v;
    }

    // Updates the current viewType
    // Used to match the label on the menu button with the calendar view
    public void setMainView(int viewType) {
        mCurrentMainView = viewType;
        notifyDataSetChanged();
    }

    // Update the date that is displayed on buttons
    // Used when the user selects a new day/week/month to watch
    // 버튼에 표시되는 날짜를 업데이트함
    // 사용자가 볼 새 일/주/월을 선택할 때 사용됨
    public void setTime(long time) {
        mMilliTime = time;
        if (LunarUtils.showLunar(mContext)) {
            buildLunarInfo();
        }
        notifyDataSetChanged();
    }

    // Builds a string with the day of the week and the word yesterday/today/tomorrow
    // before it if applicable.
    // 해당되는 경우, 해당 요일 및 어제/오늘/내일로 문자열을 작성함
    private String buildDayOfWeek() {

        Time t = new Time(mTimeZone);
        t.set(mMilliTime);
        long julianDay = Time.getJulianDay(mMilliTime,t.gmtoff);
        String dayOfWeek = null;
        mStringBuilder.setLength(0);

        if (julianDay == mTodayJulianDay) {
            dayOfWeek = mContext.getString(R.string.agenda_today,
                    DateUtils.formatDateRange(mContext, mFormatter, mMilliTime, mMilliTime,
                            DateUtils.FORMAT_SHOW_WEEKDAY, mTimeZone).toString());
        } else if (julianDay == mTodayJulianDay - 1) {
            dayOfWeek = mContext.getString(R.string.agenda_yesterday,
                    DateUtils.formatDateRange(mContext, mFormatter, mMilliTime, mMilliTime,
                            DateUtils.FORMAT_SHOW_WEEKDAY, mTimeZone).toString());
        } else if (julianDay == mTodayJulianDay + 1) {
            dayOfWeek = mContext.getString(R.string.agenda_tomorrow,
                    DateUtils.formatDateRange(mContext, mFormatter, mMilliTime, mMilliTime,
                            DateUtils.FORMAT_SHOW_WEEKDAY, mTimeZone).toString());
        } else {
            dayOfWeek = DateUtils.formatDateRange(mContext, mFormatter, mMilliTime, mMilliTime,
                    DateUtils.FORMAT_SHOW_WEEKDAY, mTimeZone).toString();
        }
        return dayOfWeek.toUpperCase();
    }

    // Builds strings with different formats:
    // 다른 형식으로 문자열 작성:
    // Full date: Month,day Year
    // Month year
    // Month day
    // Month
    // Week:  month day-day or month day - month day
    private String buildFullDate() {
        mStringBuilder.setLength(0);
        String date = DateUtils.formatDateRange(mContext, mFormatter, mMilliTime, mMilliTime,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR, mTimeZone).toString();
        return date;
    }

    private String buildMonthYearDate() {
        mStringBuilder.setLength(0);
        String date = DateUtils.formatDateRange(
                mContext,
                mFormatter,
                mMilliTime,
                mMilliTime,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                        | DateUtils.FORMAT_SHOW_YEAR, mTimeZone).toString();
        return date;
    }

    private String buildMonthDayDate() {
        mStringBuilder.setLength(0);
        String date = DateUtils.formatDateRange(mContext, mFormatter, mMilliTime, mMilliTime,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR, mTimeZone).toString();
        return date;
    }

    private String buildMonthDate() {
        mStringBuilder.setLength(0);
        String date = DateUtils.formatDateRange(
                mContext,
                mFormatter,
                mMilliTime,
                mMilliTime,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR
                        | DateUtils.FORMAT_NO_MONTH_DAY, mTimeZone).toString();
        return date;
    }
    private String buildWeekDate() {


        // Calculate the start of the week, taking into account the "first day of the week"
        // setting.
        // "첫 번째 요일" 설정을 고려하여 주 계산을 시작함

        Time t = new Time(mTimeZone);
        t.set(mMilliTime);
        int firstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        int dayOfWeek = t.weekDay;
        int diff = dayOfWeek - firstDayOfWeek;
        if (diff != 0) {
            if (diff < 0) {
                diff += 7;
            }
            t.monthDay -= diff;
            t.normalize(true /* ignore isDst */);
        }

        long weekStartTime = t.toMillis(true);
        // The end of the week is 6 days after the start of the week
        // 한 주의 끝은 시작한 날의 6일 후임
        long weekEndTime = weekStartTime + DateUtils.WEEK_IN_MILLIS - DateUtils.DAY_IN_MILLIS;

        // If week start and end is in 2 different months, use short months names
        // 주의 시작과 끝이 다른 월인 경우, short months names 사용
        Time t1 = new Time(mTimeZone);
        t1.set(weekEndTime);
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        if (t.month != t1.month) {
            flags |= DateUtils.FORMAT_ABBREV_MONTH;
        }

        mStringBuilder.setLength(0);
        String date = DateUtils.formatDateRange(mContext, mFormatter, weekStartTime,
                weekEndTime, flags, mTimeZone).toString();
        return date;
    }

    private String buildWeekNum() {
        int week = Utils.getWeekNumberFromTime(mMilliTime, mContext);
        return mContext.getResources().getQuantityString(R.plurals.weekN, week, week);
    }

    private void buildLunarInfo() {
        if (mLunarLoader == null || TextUtils.isEmpty(mTimeZone)) return;

        Time time = new Time(mTimeZone);
        if (time != null) {
            // The the current month.
            // 현재 월
            time.set(mMilliTime);

            // As the first day of previous month;
            // 이전 달의 첫 날을 가져옴
            Calendar from = Calendar.getInstance();
            from.set(time.year, time.month - 1, 1);

            // Get the last day of next month.
            // 다음 달의 마지막 날을 가져옴
            Calendar to = Calendar.getInstance();
            to.set(Calendar.YEAR, time.year);
            to.set(Calendar.MONTH, time.month + 1);
            to.set(Calendar.DAY_OF_MONTH, to.getMaximum(Calendar.DAY_OF_MONTH));

            // Call LunarUtils to load the info.
            // 정보를 가져오기 위해 LunarUtils 호출
            mLunarLoader.load(from.get(Calendar.YEAR), from.get(Calendar.MONTH),
                    from.get(Calendar.DAY_OF_MONTH), to.get(Calendar.YEAR), to.get(Calendar.MONTH),
                    to.get(Calendar.DAY_OF_MONTH));
        }
    }

}

