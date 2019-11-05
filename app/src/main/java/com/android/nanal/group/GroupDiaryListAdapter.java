package com.android.nanal.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nanal.NanalDBHelper;
import com.android.nanal.R;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.diary.Diary;

import org.w3c.dom.Text;

import java.util.List;

public class GroupDiaryListAdapter extends RecyclerView.Adapter<GroupDiaryListAdapter.ViewHolder> {
    private List<Diary> diaryList;
    private NanalDBHelper helper;
    private Context mContext;
    private CalendarController mController;

    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout gdiary_wrapper;
        TextView gdiary_name;
        ViewHolder(View itemView) {
          super(itemView);
          gdiary_wrapper = itemView.findViewById(R.id.ll_gdiary_wrapper);
          gdiary_name = itemView.findViewById(R.id.tv_gdiary_list);
        }
    }

    public GroupDiaryListAdapter(Context context, int groupid) {
        mContext = context;
        mController = CalendarController.getInstance(context);
        try {
            helper = AllInOneActivity.helper;
            diaryList = helper.getGroupDiariesList(groupid);
        } catch (Exception e) {
            Toast.makeText(mContext, "리스트를 불러오는 중 오류가 발생했습니다. 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    @Override
    public GroupDiaryListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.gdiarylist_item, viewGroup, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        final Diary diary = diaryList.get(i);
        viewHolder.gdiary_name.setText(diary.title);
    }

    @Override
    public int getItemCount() {
        return diaryList.size();
    }
}
