package com.android.nanal.group;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nanal.R;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.query.GroupHistoryAsyncTask;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GroupHistoryListAdapter extends RecyclerView.Adapter<GroupHistoryListAdapter.ViewHolder>  {
    private List<String[]> historyList = new ArrayList<>();
    private Context mContext;
    private CalendarController mController;
    private GroupDetailFragment mFragment;

    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout ghistory_wrapper;
        TextView ghistory_name, ghistory_time;
        ViewHolder(View itemView) {
            super(itemView);
            ghistory_wrapper = itemView.findViewById(R.id.ll_ghistory_wrapper);
            ghistory_name = itemView.findViewById(R.id.tv_ghistory_list);
            ghistory_time = itemView.findViewById(R.id.tv_ghistory_list_time);
        }
    }

    public GroupHistoryListAdapter(Context context, GroupDetailFragment f, int groupid) {
        mContext = context;
        mFragment = f;
        mController = CalendarController.getInstance(context);
        try {
            GroupHistoryAsyncTask mTask = new GroupHistoryAsyncTask(mContext);
            historyList = mTask.execute(Integer.toString(groupid)).get();
            mFragment.refresh();
        } catch (Exception e) {
            Toast.makeText(mContext, "리스트를 불러오는 중 오류가 발생했습니다. 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    @NonNull
    @Override
    public GroupHistoryListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.ghistorylist_item, viewGroup, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Log.i("GroupHistoryListAdapter", "i: "+i+", getItemCount():"+getItemCount());
        final String[] history = historyList.get(i);
        viewHolder.ghistory_name.setText(history[2]);
        viewHolder.ghistory_time.setText(history[0]);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }
}
