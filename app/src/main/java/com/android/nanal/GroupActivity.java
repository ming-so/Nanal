package com.android.nanal;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.android.nanal.group.GroupListAdapter;

public class GroupActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_activity);

        RecyclerView recyclerView = findViewById(R.id.rv_group);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        GroupListAdapter groupListAdapter = new GroupListAdapter();
        recyclerView.setAdapter(groupListAdapter);
    }

    class GroupTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }
}
