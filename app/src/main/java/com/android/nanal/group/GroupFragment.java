package com.android.nanal.group;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.nanal.R;
import com.android.nanal.calendar.CalendarController;

import java.util.ArrayList;

public class GroupFragment extends Fragment implements CalendarController.EventHandler {
    protected static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    private static final int VIEW_ID = 2;
    GroupListAdapter groupListAdapter;
    public static ArrayList<Group> groups = new ArrayList<>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Context context = getActivity();
        groups.add(new Group(1, "test", 2, "test"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.group_activity, null);
        RecyclerView recyclerView = v.findViewById(R.id.rv_group);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        groupListAdapter = new GroupListAdapter(groups);
        recyclerView.setAdapter(groupListAdapter);
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
}
