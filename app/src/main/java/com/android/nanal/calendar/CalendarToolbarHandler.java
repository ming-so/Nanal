package com.android.nanal.calendar;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.LayoutInflater;

import com.android.nanal.R;
import com.android.nanal.event.Utils;

import java.util.Formatter;
import java.util.Locale;

/**
 * Created by xsoh64 on 7/21/15.
 */
public class CalendarToolbarHandler {

    private final LayoutInflater mInflater;
    private final StringBuilder mStringBuilder;
    private final Formatter mFormatter;
    private AppCompatActivity mContext;
    private Toolbar mToolbar;
    private int mCurrentViewType;
    // The current selected event's time, used to calculate the date and day of the week
    // for the buttons.
    // 버튼에 대한 날짜 및 요일을 계산하는 데 사용되는 현재 선택된 이벤트의 시간
    private long mMilliTime;
    private String mTimeZone;
    private long mTodayJulianDay;
    private Handler mMidnightHandler = null; // Used to run a time update every midnight
    // 매일 밤 시간 업데이트를 하는 데 사용됨

    private final Runnable mTimeUpdater = new Runnable() {
        @Override
        public void run() {
            refresh(mContext);
        }
    };


    public CalendarToolbarHandler(AppCompatActivity context, Toolbar toolbar, int defaultViewType) {
        mContext = context;
        mToolbar = toolbar;
        mCurrentViewType = defaultViewType;

        mMidnightHandler = new Handler();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

        refresh(mContext);
    }

    // Sets the time zone and today's Julian day to be used by the adapter.
    // Also, update the change and resets the midnight update thread.
    // 어댑터가 사용할 시간대와 오늘의 줄리안 날짜를 설정함
    // 또한, 변경 사항을 업데이트하고 자정 업데이트 쓰레드를 리셋함
    public void refresh(Context context) {
        mTimeZone = Utils.getTimeZone(context, mTimeUpdater);
        Time time = new Time(mTimeZone);
        long now = System.currentTimeMillis();
        time.set(now);
        mTodayJulianDay = Time.getJulianDay(now, time.gmtoff);
        updateTitle();
        setMidnightHandler();
    }

    public void setCurrentMainView(int viewType) {
        mCurrentViewType = viewType;
        updateTitle();
    }

    // Update the date that is displayed on buttons
    // Used when the user selects a new day/week/month to watch
    // 버튼에 표시되는 날짜 업데이트
    // 사용자가 볼 새로운 일/주/월을 선택할 때 사용함
    public void setTime(long time) {
        mMilliTime = time;
        updateTitle();
    }

    private void updateTitle() {
        switch (mCurrentViewType) {
            case CalendarController.ViewType.DAY:
                mToolbar.setSubtitle(buildDayOfWeek());
                mToolbar.setTitle(buildFullDate());
                break;
            case CalendarController.ViewType.WEEK:
                if (Utils.getShowWeekNumber(mContext)) {
                    mToolbar.setSubtitle(buildWeekNum());
                } else {
                    mToolbar.setSubtitle("");
                }
                mToolbar.setTitle(buildMonthYearDate());
                break;
            case CalendarController.ViewType.MONTH:
                mToolbar.setSubtitle("");
                mToolbar.setTitle(buildMonthYearDate());
                break;
            case CalendarController.ViewType.AGENDA:
                mToolbar.setSubtitle(buildDayOfWeek());
                mToolbar.setTitle(buildFullDate());
                break;
        }
    }


    // Sets a thread to run 1 second after midnight and update the current date
    // This is used to display correctly the date of yesterday/today/tomorrow
    // 자정이 지나고 1초 후 실행되도록 스레드를 설정하고 현재 날짜를 업데이ㅡ함
    // 어제/오늘/내일 날짜를 정확하게 표시하기 위해 사용됨
    private void setMidnightHandler() {
        mMidnightHandler.removeCallbacks(mTimeUpdater);
        // Set the time updater to run at 1 second after midnight
        long now = System.currentTimeMillis();
        Time time = new Time(mTimeZone);
        time.set(now);
        long runInMillis = (24 * 3600 - time.hour * 3600 - time.minute * 60 -
                time.second + 1) * 1000;
        mMidnightHandler.postDelayed(mTimeUpdater, runInMillis);
    }

    // Builds a string with the day of the week and the word yesterday/today/tomorrow
    // before it if applicable.
    // 해당되는 경우, 요일 및 어제/오늘/내일이라는 단어가 포함된 문자열을 작성함
    private String buildDayOfWeek() {

        Time t = new Time(mTimeZone);
        t.set(mMilliTime);
        long julianDay = Time.getJulianDay(mMilliTime, t.gmtoff);
        String dayOfWeek;
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
        return dayOfWeek;
    }

    // Builds strings with different formats:
    // Full date: Month,day Year
    // Month year
    // Month day
    // Month
    // Week:  month day-day or month day - month day
    // 다른 형식으로 문자열 작성:
    // 전체 날짜: 월,일 년
    // 월 년
    // 월 일
    // 월
    // 주: 월 일-일 또는 월 일 - 월 일
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
        // "첫 번째 요일" 설정을 고려하여 주의 시작일을 계산함

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
        // 주의 마지막은 주의 첫 날의 6일 후임
        long weekEndTime = weekStartTime + DateUtils.WEEK_IN_MILLIS - DateUtils.DAY_IN_MILLIS;

        // If week start and end is in 2 different months, use short months names
        // 주 시작일과 종료일이 다른 월에 있는 경우? 짧은 달 이름? 사용
        Time t1 = new Time(mTimeZone);
        t.set(weekEndTime);
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
}
