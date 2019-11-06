package com.android.nanal;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.nanal.calendar.CalendarController;

public class TodayFragment extends Fragment implements CalendarController.EventHandler {
    private boolean hasEvent = false;
    private boolean hasDiary = false;

    public TodayFragment() {
        super();
    }

    @SuppressLint("ValidFragment")
    public TodayFragment(long time) {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Context context = getActivity();

//        RecyclerView recyclerView = findViewById(R.id.rv_group);
//        recyclerView.setLayoutManager(new LinearLayoutManager(context));
//        groupListAdapter = new GroupListAdapter(groups);
//        recyclerView.setAdapter(groupListAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.today_activity, null);
        return v;
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
