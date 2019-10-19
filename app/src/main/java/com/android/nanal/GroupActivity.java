package com.android.nanal;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.android.nanal.group.GroupListAdapter;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GroupActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_activity);

        RecyclerView recyclerView = findViewById(R.id.rv_group);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        GroupListAdapter groupListAdapter = new GroupListAdapter(getApplicationContext());
        recyclerView.setAdapter(groupListAdapter);

        Log.i("GroupActivity", "");
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
