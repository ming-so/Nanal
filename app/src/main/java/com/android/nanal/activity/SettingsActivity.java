package com.android.nanal.activity;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.android.nanal.DynamicTheme;
import com.android.nanal.QuickResponseSettings;
import com.android.nanal.R;
import com.android.nanal.ViewDetailsPreferences;
import com.android.nanal.calendar.OtherPreferences;
import com.android.nanal.calendar.SelectCalendarsSyncFragment;
import com.android.nanal.event.AboutPreferences;
import com.android.nanal.event.GeneralPreferences;
import com.android.nanal.event.Utils;

import java.util.List;


public class SettingsActivity extends PreferenceActivity {
    private boolean mHideMenuButtons = false;
    private final DynamicTheme dynamicTheme = new DynamicTheme();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dynamicTheme.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_activity);

        LinearLayout root = findViewById(R.id.ll_settings);

        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.app_bar, root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        bar.setTitle(getTitle());
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(0, 0);
            }
        });
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        // Allow super to try and create a view first
        // 부모가 먼저 view를 생성하도록 허용
        final View result = super.onCreateView(name, context, attrs);
        if (result != null) {
            return result;
        }
        return null;
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.calendar_settings_headers, target);

        if (Utils.getTardis() + DateUtils.MINUTE_IN_MILLIS > System.currentTimeMillis()) {
            Header tardisHeader = new Header();
            tardisHeader.title = getString(R.string.preferences_experimental_category);
            tardisHeader.fragment = "com.android.calendar.OtherPreferences";
            target.add(tardisHeader);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_add_account) {
            Intent nextIntent = new Intent(Settings.ACTION_ADD_ACCOUNT);
            final String[] array = { "com.android.nanal" };
            nextIntent.putExtra(Settings.EXTRA_AUTHORITIES, array);
            nextIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(nextIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mHideMenuButtons) {
            getMenuInflater().inflate(R.menu.settings_title_bar, menu);
        }
        if (getActionBar() != null){
            getActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    protected boolean isValidFragment(String fragmentName) {
        return GeneralPreferences.class.getName().equals(fragmentName)
                || SelectCalendarsSyncFragment.class.getName().equals(fragmentName)
                || OtherPreferences.class.getName().equals(fragmentName)
                || AboutPreferences.class.getName().equals(fragmentName)
                || QuickResponseSettings.class.getName().equals(fragmentName)
                || ViewDetailsPreferences.class.getName().equals(fragmentName);
    }

    public void hideMenuButtons() {
        mHideMenuButtons = true;
    }

    public void restartActivity() {
        recreate();
    }
}
