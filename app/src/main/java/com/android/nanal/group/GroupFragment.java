package com.android.nanal.group;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.android.nanal.R;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.calendar.CalendarController;

public class GroupFragment extends Fragment implements CalendarController.EventHandler {
    protected static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    private static final int VIEW_ID = 2;

    static RecyclerView recyclerView;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Context context = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.group_activity, null);
        recyclerView = v.findViewById(R.id.rv_group);

        recyclerView.setAdapter(AllInOneActivity.groupListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        return v;
    }

    private void goTo(int index) {

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

//    public static void addGroup(String groupId, String groupName, int groupColor) {
//        AllInOneActivity.groups.add(getNewGroup(Integer.parseInt(groupId), groupName, groupColor));
//        refreshGroups();
//    }
//
//    static Group getNewGroup(int groupId, String groupName, int groupColor) {
//        Group group = Group.newInstance();
//        group.group_id = groupId;
//        group.group_name = groupName;
//        group.group_color = groupColor;
//        return group;
//    }
}
