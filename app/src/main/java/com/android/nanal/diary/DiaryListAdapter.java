package com.android.nanal.diary;

import android.content.Context;
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
import com.android.nanal.TodayFragment;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.calendar.CalendarController;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class DiaryListAdapter extends RecyclerView.Adapter<DiaryListAdapter.ViewHolder> {
    private List<Diary> diaryList;
    private NanalDBHelper helper;
    private Context mContext;
    private CalendarController mController;
    private boolean mNeedIcon;
    private TodayFragment mFragment;

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

    public DiaryListAdapter(Context context, String day, boolean needIcon, TodayFragment f) {
        this(context, day, needIcon);
        mFragment = f;
    }

    public DiaryListAdapter(Context context, String day, boolean needIcon) {
        mContext = context;
        mController = CalendarController.getInstance(context);
        mNeedIcon = needIcon;

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
        if(!mNeedIcon) {
            v.findViewById(R.id.iv_diary_list).setVisibility(View.GONE);
        }
        DiaryListAdapter.ViewHolder vh = new DiaryListAdapter.ViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {
        final Diary d = diaryList.get(i);
        viewHolder.diary_title.setText(d.title);
        if(mNeedIcon) {
            viewHolder.diary_content.setText(d.content);
        } else {
            viewHolder.diary_content.setText(" > "+d.content);
        }
        if (helper.getDiaryIsInGroup(d)) {
            viewHolder.diary_icon.setImageResource(R.drawable.ic_bookmark_border_black_24dp);
        } else {
            viewHolder.diary_icon.setImageResource(R.drawable.ic_bookmark_black_24dp);
        }
        if(!mNeedIcon) {
            viewHolder.diary_title.setTextColor(d.color);
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
                //Toast.makeText(context, "선택 > " + d.id, Toast.LENGTH_LONG).show();
                if(mFragment == null) return;
                //mFragment.goDiaryEdit(d.id, d.day);
            }
        });
    }

    @Override
    public int getItemCount() {
        if(diaryList == null) {
            return 0;
        }
        return diaryList.size();
    }
}
