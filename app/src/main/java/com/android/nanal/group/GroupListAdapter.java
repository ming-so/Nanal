package com.android.nanal.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nanal.NanalDBHelper;
import com.android.nanal.R;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.calendar.CalendarController;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.ViewHolder> {
    private List<Group> groupList;
    private NanalDBHelper helper;
    private Context mContext;
    private CalendarController mController;

    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout group_wrapper;
        ImageView group_icon;
        TextView group_name;
        ViewHolder(View itemView) {
            super(itemView);
            group_wrapper = itemView.findViewById(R.id.ll_group_wrapper);
            group_icon = itemView.findViewById(R.id.iv_group_list);
            group_name = itemView.findViewById(R.id.tv_group_list);
        }
    }

    public GroupListAdapter(Context context) {
        mContext = context;
        mController = CalendarController.getInstance(context);
        try {
            helper = AllInOneActivity.helper;
            groupList = helper.getGroupList();
        } catch(Exception e) {
            Toast.makeText(mContext, "그룹을 불러오는 데 오류가 발생했습니다. 다시 시도하세요.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public GroupListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.grouplist_item, viewGroup, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {
        final Group group = groupList.get(i);
        viewHolder.group_name.setText(group.group_name);
        String hexColor = String.format("#%06X", (0xFFFFFF & group.group_color));
        switch (hexColor) {
            case "#41C3B1":
            default:
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorPrimary));
                break;
            case "#F1922D":
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorOrangeAccent));
                break;
            case "#4B7BEA":
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorBluePrimary));
                break;
            case "#3ABE3F":
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorGreenAccent));
                break;
            case "#C72C14":
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorRedAccent));
                break;
            case "#9C27B0":
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorPurpleAccent));
                break;
        }
        viewHolder.group_wrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hexColor = String.format("#%06X", (0xFFFFFF & group.group_color));
                Context context = v.getContext();
                AllInOneActivity.mGroupId = group.group_id;
                AllInOneActivity.mGroupName = group.group_name;
                mController.sendEvent(this, CalendarController.EventType.GO_TO, null, null, group.group_id, CalendarController.ViewType.GROUP_DETAIL);
                //Toast.makeText(context, "선택 > "+group.group_id + ", hexColor > "+hexColor, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }
}
