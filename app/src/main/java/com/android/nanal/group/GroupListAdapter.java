package com.android.nanal.group;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.nanal.R;

import java.util.ArrayList;
import java.util.List;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.ViewHolder> {
    private List<Group> groupList;

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView group_icon;
        TextView group_name;
        ViewHolder(View itemView) {
            super(itemView);
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
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Group group = groupList.get(i);
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
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }
}