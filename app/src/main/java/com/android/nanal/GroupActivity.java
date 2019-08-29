package com.android.nanal;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.android.nanal.group.Group;
import com.android.nanal.group.GroupListAdapter;

import java.util.ArrayList;

public class GroupActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_activity);

        ArrayList<Group> groups = new ArrayList<>();
        groups.add(new Group(1, "test", 2, "test"));

        RecyclerView recyclerView = findViewById(R.id.rv_group);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        GroupListAdapter groupListAdapter = new GroupListAdapter(groups);
        recyclerView.setAdapter(groupListAdapter);
    }
}
