package com.android.nanal;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.diary.DiaryListAdapter;
import com.android.nanal.event.EventListAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TodayFragment extends Fragment implements CalendarController.EventHandler {
    private boolean hasEvent = false;
    private boolean hasDiary = false;
    private NanalDBHelper helper;

    private RecyclerView rv_event, rv_diary;
    private TextView tv_date, tv_add_event, tv_add_diary, tv_email;

    private Date mDate;
    private String str_date;

    private CalendarController mController;

    private DiaryListAdapter mDiaryAdapter;

    public TodayFragment() {
        super();
    }

    @SuppressLint("ValidFragment")
    public TodayFragment(CalendarController controller) {
        super();
        mController = controller;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Context context = getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.today_activity, null);
        rv_event = v.findViewById(R.id.rv_day_event);
        rv_diary = v.findViewById(R.id.rv_day_diary);
        tv_date = v.findViewById(R.id.tv_date);
        tv_add_event = v.findViewById(R.id.tv_add_event);
        tv_add_diary = v.findViewById(R.id.tv_add_diary);
        tv_email = v.findViewById(R.id.tv_email);

        helper = AllInOneActivity.helper;

        mDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        str_date = sdf.format(mDate);

        tv_date.setText(str_date);
        tv_email.setText(AllInOneActivity.connectId);

        tv_add_diary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Time t = new Time();
                t.set(mController.getTime());
                mController.sendEventRelatedEvent(
                        this, CalendarController.EventType.CREATE_DIARY, -1, t.toMillis(true), 0, 0, 0, -1);
            }
        });

        tv_add_event.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create new Event
                Time t = new Time();
                t.set(mController.getTime());
                t.second = 0;
                if (t.minute > 30) {
                    t.hour++;
                    t.minute = 0;
                } else if (t.minute > 0 && t.minute < 30) {
                    t.minute = 30;
                }
                mController.sendEventRelatedEvent(
                        this, CalendarController.EventType.CREATE_EVENT, -1, t.toMillis(true), 0, 0, 0, -1);
            }
        });

        rv_diary.setAdapter(new DiaryListAdapter(getActivity().getApplicationContext(), str_date, false));
        rv_diary.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));

        rv_event.setAdapter(new EventListAdapter(getActivity().getApplicationContext(), mDate, false));
        rv_event.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));

        if(rv_event.getAdapter().getItemCount() <= 0) {
            tv_add_event.setVisibility(View.VISIBLE);
        }
        if(rv_diary.getAdapter().getItemCount() <= 0) {
            tv_add_diary.setVisibility(View.VISIBLE);
        }
        return v;
    }

    public void refreshList() {
        rv_event.getAdapter().notifyDataSetChanged();
        rv_diary.getAdapter().notifyDataSetChanged();
    }

    @Override
    public long getSupportedEventTypes() {
        return 0;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {

    }

    @Override
    public void eventsChanged() {

    }
}
