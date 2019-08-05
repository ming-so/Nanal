package com.android.nanal.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;

import com.android.nanal.LoginActivity;
import com.android.nanal.R;

public class MainActivity extends Activity {

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_search:
                        Intent a = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(a);
                        break;
                    case R.id.action_settings:
                        Intent b = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(b);
                        break;
                    case R.id.action_navigation:
                        Intent c = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(c);
                        break;
                }
                return false;
            }
        });
    }

}
