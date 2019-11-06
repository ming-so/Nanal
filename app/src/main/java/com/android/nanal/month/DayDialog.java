package com.android.nanal.month;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nanal.NanalDBHelper;
import com.android.nanal.R;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.diary.Diary;
import com.android.nanal.event.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

        rv_diary.setAdapter(new DiaryListAdapter(getContext(), mDay));
        rv_diary.setLayoutManager(new LinearLayoutManager(getContext()));

        rv_event.setAdapter(new EventListAdapter(getContext(), mDay));
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

    class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {
        private List<Event> eventList;
        private NanalDBHelper helper;
        private Context mContext;
        private CalendarController mController;

        public class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout event_wrapper;
            ImageView event_icon;
            TextView event_title;

            ViewHolder(View itemView) {
                super(itemView);
                event_wrapper = itemView.findViewById(R.id.ll_event_wrapper);
                event_title = itemView.findViewById(R.id.tv_event_list);
                event_icon = itemView.findViewById(R.id.iv_event_list);
            }
        }

        public EventListAdapter(Context context, String day) {
            //helper = AllInOneActivity.helper;
            mContext = context;
            mController = CalendarController.getInstance(context);
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Calendar c = Calendar.getInstance();
                c.setTime(mDate);
                c.add(Calendar.DATE, 1);
                Date next_date = c.getTime();

                ArrayList mDiaryList = new ArrayList<>();
                ContentResolver cr = mContext.getContentResolver();
                Uri uri = CalendarContract.Events.CONTENT_URI;
                long long_date_start = mDate.getTime() + 32400000;
                long long_date_end = next_date.getTime() + 32400000;
                Log.i("DayDialog", long_date_start + ", " + long_date_end);
                // 시작 시간이 오늘 자정보다 크고 내일 자정보다 작다
                // OR
                // 시작 시간이 오늘 자정보다 작고 종료 시간이 내일 자정보다 크고
                // OR
                // 시작 시간이 오늘 자정보다 작고 종료 시간이 내일 자정보다 작은 경우
                String selection = "(dtstart >= " + long_date_start + " and dtstart <" + long_date_end + ") OR ";
                selection += "(dtstart <" + long_date_start + " and dtend > " + long_date_end + ") OR ";
                selection += "(dtstart <" + long_date_start + " and dtend > "+ long_date_start +" and dtend <= " + long_date_end + ")";
                Cursor cur = cr.query(uri, null, selection, null, null);

                while (cur.moveToNext()) {
                    Log.i("DayDialog", cur.getString(73) + ", 시작: " + cur.getString(62) + ", 끝: " + cur.getString(106));
                    Log.i("DayDialog", "rdate: "+ cur.getString(22) + ", rrule: "+cur.getString(7));
                    Event e = new Event();
                    e.title = cur.getString(73);
                    e.id = cur.getInt(60);
                    e.startMillis = cur.getLong(62);
                    e.endMillis = cur.getLong(106);
                    mDiaryList.add(e);
                }
                eventList = mDiaryList;
            } catch (Exception e) {
                Log.wtf("DayDialog", e.getMessage());
                e.printStackTrace();
                Toast.makeText(mContext, "일정을 불러오는 데 오류가 발생했습니다. 다시 시도하세요.", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public EventListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            Context context = viewGroup.getContext();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View v = inflater.inflate(R.layout.eventlist_item, viewGroup, false);
            EventListAdapter.ViewHolder vh = new EventListAdapter.ViewHolder(v);
            return vh;
        }


        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {
            final Event e = eventList.get(i);
            viewHolder.event_title.setText(e.title);
            viewHolder.event_icon.setImageResource(R.drawable.ic_check_black_24dp);
//            if (helper.getDiaryIsInGroup(d)) {
//                viewHolder.diary_icon.setImageResource(R.drawable.ic_bookmark_border_black_24dp);
//            } else {
//                viewHolder.diary_icon.setImageResource(R.drawable.ic_bookmark_black_24dp);
//            }
            viewHolder.event_icon.setColorFilter(e.color);
/*
            String hexColor = String.format("#%06X", (0xFFFFFF & e.color));
            Log.i("DayDialog", hexColor);
            switch (hexColor) {
                case "#41C3B1":
                default:
                    viewHolder.event_icon.setColorFilter(ContextCompat.getColor(
                            viewHolder.event_icon.getContext(), R.color.colorPrimary));
                    break;
                case "#F1922D":
                    viewHolder.event_icon.setColorFilter(ContextCompat.getColor(
                            viewHolder.event_icon.getContext(), R.color.colorOrangeAccent));
                    break;
                case "#4B7BEA":
                    viewHolder.diary_icon.setColorFilter(ContextCompat.getColor(
                            viewHolder.diary_icon.getContext(), R.color.colorBluePrimary));
                    break;
                case "#3ABE3F":
                    viewHolder.diary_icon.setColorFilter(ContextCompat.getColor(
                            viewHolder.diary_icon.getContext(), R.color.colorGreenAccent));
                    break;
                case "#C72C14":
                    viewHolder.diary_icon.setColorFilter(ContextCompat.getColor(
                            viewHolder.diary_icon.getContext(), R.color.colorRedAccent));
                    break;
                case "#9C27B0":
                    viewHolder.diary_icon.setColorFilter(ContextCompat.getColor(
                            viewHolder.diary_icon.getContext(), R.color.colorPurpleAccent));
                    break;
            }*/
            viewHolder.event_wrapper.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //String hexColor = String.format("#%06X", (0xFFFFFF & d.color));
                    Context context = v.getContext();
                    Toast.makeText(context, "선택 > " + e.id, Toast.LENGTH_LONG).show();
                    mController.launchViewEvent(e.id, e.startMillis, e.endMillis, Attendees.ATTENDEE_STATUS_NONE);
                    DayDialog.this.dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            if(eventList != null) {
                return eventList.size();
            }
            return 0;
        }
    }
}

class DiaryListAdapter extends RecyclerView.Adapter<DiaryListAdapter.ViewHolder> {
    private List<Diary> diaryList;
    private NanalDBHelper helper;
    private Context mContext;
    private CalendarController mController;

    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout diary_wrapper;
        ImageView diary_icon;
        TextView diary_title, diary_content;

        ViewHolder(View itemView) {
            super(itemView);
            diary_wrapper = itemView.findViewById(R.id.ll_diary_wrapper);
            diary_title = itemView.findViewById(R.id.tv_diary_list);
            diary_icon = itemView.findViewById(R.id.iv_diary_list);
            diary_content = itemView.findViewById(R.id.tv_diary_list2);
        }
    }

    public DiaryListAdapter(Context context, String day) {
        mContext = context;
        mController = CalendarController.getInstance(context);
        try {
            helper = AllInOneActivity.helper;
            diaryList = helper.getDiariesList(day);
        } catch (Exception e) {
            Toast.makeText(mContext, "일기를 불러오는 데 오류가 발생했습니다. 다시 시도하세요.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public DiaryListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.diarylist_item, viewGroup, false);
        DiaryListAdapter.ViewHolder vh = new DiaryListAdapter.ViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {
        final Diary d = diaryList.get(i);
        viewHolder.diary_title.setText(d.title);
        viewHolder.diary_content.setText(d.content);
        if (helper.getDiaryIsInGroup(d)) {
            viewHolder.diary_icon.setImageResource(R.drawable.ic_bookmark_border_black_24dp);
        } else {
            viewHolder.diary_icon.setImageResource(R.drawable.ic_bookmark_black_24dp);
        }

        String hexColor = String.format("#%06X", (0xFFFFFF & d.color));
        Log.i("DayDialog", hexColor);
        switch (hexColor) {
            case "#41C3B1":
            default:
                viewHolder.diary_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.diary_icon.getContext(), R.color.colorPrimary));
                break;
            case "#F1922D":
                viewHolder.diary_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.diary_icon.getContext(), R.color.colorOrangeAccent));
                break;
            case "#4B7BEA":
                viewHolder.diary_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.diary_icon.getContext(), R.color.colorBluePrimary));
                break;
            case "#3ABE3F":
                viewHolder.diary_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.diary_icon.getContext(), R.color.colorGreenAccent));
                break;
            case "#C72C14":
                viewHolder.diary_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.diary_icon.getContext(), R.color.colorRedAccent));
                break;
            case "#9C27B0":
                viewHolder.diary_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.diary_icon.getContext(), R.color.colorPurpleAccent));
                break;
        }
        viewHolder.diary_wrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hexColor = String.format("#%06X", (0xFFFFFF & d.color));
                Context context = v.getContext();
//                    mController.sendEvent(this, CalendarController.EventType.GO_TO, null, null, group.group_id, CalendarController.ViewType.GROUP_DETAIL);
                Toast.makeText(context, "선택 > " + d.id, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return diaryList.size();
    }
}
