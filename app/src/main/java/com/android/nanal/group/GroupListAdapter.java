package com.android.nanal.group;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nanal.R;

import java.util.ArrayList;
import java.util.List;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.ViewHolder> {
    private List<Group> groupList;

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

    public GroupListAdapter(ArrayList<Group> groupList) {
        this.groupList = groupList;
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
        switch (group.group_color) {
            case 1:
            default:
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorPrimary));
                break;
            case 2:
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorOrangeAccent));
                break;
            case 3:
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorBluePrimary));
                break;
            case 4:
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorGreenAccent));
                break;
            case 5:
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorRedAccent));
                break;
            case 6:
                viewHolder.group_icon.setColorFilter(ContextCompat.getColor(
                        viewHolder.group_icon.getContext(), R.color.colorPurpleAccent));
                break;
        }
        viewHolder.group_wrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Toast.makeText(context, "선택 > "+group.group_id, Toast.LENGTH_LONG).show();
                //todo: groupId 가지고 그룹 상세 보기 화면으로 전환
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }
}
