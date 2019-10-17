package com.android.nanal.group;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import com.android.nanal.R;
import com.android.nanal.calendar.CalendarController;

public class GroupDetailFragment extends Fragment implements CalendarController.EventHandler {
    private int mGroupId;
    private Toolbar mToolbar;

    public GroupDetailFragment() {
        super();
    }

    @SuppressLint("ValidFragment")
    public GroupDetailFragment(int group_id) {
        mGroupId = group_id;
        Log.i("GroupDetailFragment", "전송받은 group_id="+mGroupId);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Context context = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.group_detail, null);
        return v;
    }

    private void goTo(int index) {

    }

    // 아래 세 개는 쓰지 마세용
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
