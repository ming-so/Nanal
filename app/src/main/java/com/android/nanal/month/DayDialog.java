package com.android.nanal.month;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
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

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DayDialog extends Dialog {
    TextView tv_day;
    RecyclerView rv_event, rv_diary;
    Time mTime;
    String mDay;

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

        setDayTitle();
    }

    public DayDialog(@NonNull Context context, Time day) {
        super(context);
        mTime = day;
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
        Log.i("DayDialog", mDay);
    }

    private void setDayTitle() {
        if (mDay.isEmpty() || mDay == null) {
            return;
        }
        tv_day.setText(mDay);
    }

    class DiaryListAdapter extends RecyclerView.Adapter<DiaryListAdapter.ViewHolder> {
        private List<Diary> diaryList;
        private NanalDBHelper helper;
        private Context mContext;
        private CalendarController mController;

        public class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout diary_wrapper;
            ImageView diary_icon;
            TextView diary_title;

            ViewHolder(View itemView) {
                super(itemView);
                diary_wrapper = itemView.findViewById(R.id.ll_diary_wrapper);
                diary_title = itemView.findViewById(R.id.tv_diary_list);
                diary_icon = itemView.findViewById(R.id.iv_diary_list);
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
            if(helper.getDiaryIsInGroup(d)) {
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
//                    AllInOneActivity.mGroupId = group.group_id;
//                    AllInOneActivity.mGroupName = group.group_name;
//                    mController.sendEvent(this, CalendarController.EventType.GO_TO, null, null, group.group_id, CalendarController.ViewType.GROUP_DETAIL);
                    Toast.makeText(context, "선택 > "+d.id, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return diaryList.size();
        }
    }
}
