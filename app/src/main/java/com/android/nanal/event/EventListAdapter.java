package com.android.nanal.event;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nanal.NanalDBHelper;
import com.android.nanal.R;
import com.android.nanal.calendar.CalendarController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {
    private List<Event> eventList;
    private NanalDBHelper helper;
    private Context mContext;
    private Date mDate;
    private CalendarController mController;
    private boolean mNeedIcon;

    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout event_wrapper;
        ImageView event_icon;
        TextView event_title, event_content;

        ViewHolder(View itemView) {
            super(itemView);
            event_wrapper = itemView.findViewById(R.id.ll_event_wrapper);
            event_title = itemView.findViewById(R.id.tv_event_list);
            event_icon = itemView.findViewById(R.id.iv_event_list);
            event_content = itemView.findViewById(R.id.tv_event_list2);
        }
    }

    public EventListAdapter(Context context, Date day, boolean needIcon) {
        mDate = day;
        //helper = AllInOneActivity.helper;
        mContext = context;
        mController = CalendarController.getInstance(context);
        mNeedIcon = needIcon;
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
            long long_date_start_notall = mDate.getTime();
            long long_date_end_notall = next_date.getTime();
            Log.i("DayDialog", long_date_start + ", " + long_date_end);
            // 시작 시간이 오늘 자정보다 크고 내일 자정보다 작다
            // OR
            // 시작 시간이 오늘 자정보다 작고 종료 시간이 내일 자정보다 크고
            // OR
            // 시작 시간이 오늘 자정보다 작고 종료 시간이 내일 자정보다 작은 경우
            String selection = "(allDay > 0 AND ((dtstart >= " + long_date_start + " and dtstart <" + long_date_end + ") OR ";
            selection += "(dtstart <" + long_date_start + " and dtend > " + long_date_end + ") OR ";
            selection += "(dtstart <" + long_date_start + " and dtend > "+ long_date_start +" and dtend < " + long_date_end + "))) OR ";
            selection += "(allDay <= 0 AND ((dtstart >= " + long_date_start_notall + " and dtstart <" + long_date_end_notall + ") OR ";
            selection += "(dtstart <" + long_date_start_notall + " and dtend > " + long_date_end_notall + ") OR ";
            selection += "(dtstart <" + long_date_start_notall + " and dtend > "+ long_date_start_notall +" and dtend <= " + long_date_end_notall + ")))";
            Cursor cur = cr.query(uri, null, selection, null, null);

            while (cur.moveToNext()) {
                Log.i("DayDialog", cur.getString(73) + ", 시작: " + cur.getString(62) + ", 끝: " + cur.getString(106));
                Log.i("DayDialog", "rdate: "+ cur.getString(22) + ", rrule: "+cur.getString(7));
                Event e = new Event();
                e.title = cur.getString(73);
                e.id = cur.getInt(60);
                e.startMillis = cur.getLong(62);
                e.endMillis = cur.getLong(106);
                e.allDay = cur.getInt(43) > 0;
                e.color = cur.getInt(37);
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
        if(!mNeedIcon) {
            v.findViewById(R.id.iv_event_list).setVisibility(View.GONE);
        }
        EventListAdapter.ViewHolder vh = new EventListAdapter.ViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {
        final Event e = eventList.get(i);
        if(e.title.length() > 0) {
            viewHolder.event_title.setText(e.title);
        } else {
            viewHolder.event_title.setText("(제목 없음)");
        }
        if(e.allDay) {
            viewHolder.event_content.setText("하루 종일");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
            Date start = new Date(e.startMillis);
            Date end = new Date(e.endMillis);
            String str_start = sdf.format(start);
            String str_end = sdf.format(end);
            String str_d_start = sdf2.format(start);

            Calendar c = Calendar.getInstance();
            c.setTime(mDate);
            c.add(Calendar.DATE, 1);
            Date next_date = c.getTime();
            String str_next = sdf2.format(next_date);

            String text;
            if(end.compareTo(next_date) >= 0) {
                // 이벤트 끝나는 시간이 오늘 23시 59분을 넘어갈 때, 날짜 표시
                text = str_start + " ~ " + str_next + " " + str_end;
            } else if(start.compareTo(mDate) < 0) {
                // 이벤트 시작하는 시간이 오늘 0시 0분 전일 때, 날짜 표시
                text = str_d_start + " " + str_start + " ~ " + str_end;
            } else {
                text = str_start + " ~ " + str_end;
            }

            if(mNeedIcon) {
                viewHolder.event_content.setText(text);
            } else {
                viewHolder.event_content.setText(" > "+text);
            }
        }
        viewHolder.event_icon.setImageResource(R.drawable.ic_check_black_24dp);
//            if (helper.getDiaryIsInGroup(d)) {
//                viewHolder.diary_icon.setImageResource(R.drawable.ic_bookmark_border_black_24dp);
//            } else {
//                viewHolder.diary_icon.setImageResource(R.drawable.ic_bookmark_black_24dp);
//            }
        Log.wtf("DayDialog", "e.color: "+e.color);
        viewHolder.event_icon.setColorFilter(e.color);
        if(!mNeedIcon) {
            viewHolder.event_title.setTextColor(e.color);
        }
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
                //Toast.makeText(context, "선택 > " + e.id, Toast.LENGTH_LONG).show();
                mController.launchViewEvent(e.id, e.startMillis, e.endMillis, CalendarContract.Attendees.ATTENDEE_STATUS_NONE);
//                mContext.dismiss();
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
