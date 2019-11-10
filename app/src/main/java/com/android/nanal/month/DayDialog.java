package com.android.nanal.month;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.nanal.R;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.diary.DiaryListAdapter;
import com.android.nanal.event.EventListAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DayDialog extends Dialog {
    TextView tv_day;
    RecyclerView rv_event, rv_diary;
    Time mTime;
    Date mDate;
    String mDay;
    Context mContext;

    ArrayList[] events;

    protected CalendarController mController;
    private static final String DISPLAY_AS_ALLDAY = "dispAllday";
    public static final String[] EVENT_PROJECTION = new String[]{
            Instances.TITLE,                 // 0
            Instances.EVENT_LOCATION,        // 1
            Instances.ALL_DAY,               // 2
            Instances.DISPLAY_COLOR,         // 3
            Instances.EVENT_TIMEZONE,        // 4
            Instances.EVENT_ID,              // 5
            Instances.BEGIN,                 // 6
            Instances.END,                   // 7
            Instances._ID,                   // 8
            Instances.START_DAY,             // 9
            Instances.END_DAY,               // 10
            Instances.START_MINUTE,          // 11
            Instances.END_MINUTE,            // 12
            Instances.HAS_ALARM,             // 13
            Instances.RRULE,                 // 14
            Instances.RDATE,                 // 15
            Instances.SELF_ATTENDEE_STATUS,  // 16
            Events.ORGANIZER,                // 17
            Events.GUESTS_CAN_MODIFY,        // 18
            Instances.ALL_DAY + "=1 OR (" + Instances.END + "-" + Instances.BEGIN + ")>="
                    + DateUtils.DAY_IN_MILLIS + " AS " + DISPLAY_AS_ALLDAY, // 19
            "event_id",                      // 20
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //다이얼로그 밖의 화면은 흐리게 만들어줌
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);

        setContentView(R.layout.day_dialog);
        tv_day = findViewById(R.id.tv_day_day);
        rv_event = findViewById(R.id.rv_day_event);
        rv_diary = findViewById(R.id.rv_day_diary);

        rv_diary.setAdapter(new DiaryListAdapter(getContext(), mDay, true));
        rv_diary.setLayoutManager(new LinearLayoutManager(getContext()));

        rv_event.setAdapter(new EventListAdapter(getContext(), mDate, true));
        rv_event.setLayoutManager(new LinearLayoutManager(getContext()));

        setDayTitle();
    }

    public void refreshList() {
        rv_event.getAdapter().notifyDataSetChanged();
        rv_diary.getAdapter().notifyDataSetChanged();
    }


    public DayDialog(@NonNull Context context, Time day) {
        super(context);
        mContext = context;
        mTime = day;
        String tz_day;
        if (Integer.toString(day.month + 1).trim().length() == 1) {
            mDay = day.year + "-0" + (day.month + 1) + "-";
        } else {
            mDay = day.year + "-" + (day.month + 1) + "-";
        }
        if (Integer.toString(day.monthDay).trim().length() == 1) {
            mDay += "0" + day.monthDay;
        } else {
            mDay += day.monthDay;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            mDate = sdf.parse(mDay);
        } catch (ParseException e) {

        }
        Log.i("DayDialog", "mDay:" + mDay + ", mDate:" + mDate);
    }

    private void setDayTitle() {
        if (mDay.isEmpty() || mDay == null) {
            return;
        }
        tv_day.setText(mDay);
    }
}
