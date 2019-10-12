package com.android.nanal.activity;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.android.nanal.DynamicTheme;
import com.android.nanal.R;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarController.DiaryInfo;
import com.android.nanal.diary.EditDiaryFragment;

import java.sql.Date;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

public class EditDiaryActivity extends AbstractCalendarActivity {
    public static final String EXTRA_DIARY_COLOR = "diary_color";
    public static final String EXTRA_READ_ONLY = "read_only";
    public static final String EXTRA_USER_ID = "user_id";

    public static final String EXTRA_IMG = "img";
    public static final String EXTRA_CONTENTS = "contents";
    public static final String EXTRA_WEATHER = "weather";
    public static final String EXTRA_LOCATION = "location";

    private static final String TAG = "EditDiaryActivity";
    private static final boolean DEBUG = false;

    private static final String BUNDLE_KEY_DIARY_ID = "key_diary_id";
    private static final String EXTRA_DIARY_TIME = "diary_time";

    private static boolean mIsMultipane;
    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private EditDiaryFragment mEditFragment;

    private int mDiaryColor;

    private DiaryInfo mDiaryInfo;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        dynamicTheme.onCreate(this);
        setContentView(R.layout.simple_frame_layout_material);
        mDiaryInfo = getmDiaryInfoFromIntent(icicle);
        mDiaryColor = getIntent().getIntExtra(EXTRA_DIARY_COLOR, -1);
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        mEditFragment = (EditDiaryFragment) getFragmentManager().findFragmentById(R.id.body_frame);

        if (mIsMultipane) {
            getSupportActionBar().setDisplayOptions(
                    ActionBar.DISPLAY_SHOW_TITLE,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME
                            | ActionBar.DISPLAY_SHOW_TITLE);
            getSupportActionBar().setTitle(
                    mDiaryInfo.id == -1 ? R.string.diary_create : R.string.diary_edit);
        } else {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME |
                            ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        if (mEditFragment == null) {
            Intent intent = null;
            boolean readOnly = false;
            if (mDiaryInfo.id == -1) {
                intent = getIntent();
                readOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false);
            }

            mEditFragment = new EditDiaryFragment(mDiaryInfo, mDiaryColor, readOnly, intent);

            mEditFragment.mShowModifyDialogOnLaunch = getIntent().getBooleanExtra(
                    CalendarController.EVENT_EDIT_ON_LAUNCH, false);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.body_frame, mEditFragment);
            ft.show(mEditFragment);
            ft.commit();
        }
    }

    private DiaryInfo getmDiaryInfoFromIntent(Bundle icicle) {
        DiaryInfo info = new DiaryInfo();
        int diaryId = -1;
        long qroupId = -1;
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            try {
                diaryId = Integer.parseInt(data.getLastPathSegment());
            } catch (NumberFormatException e) {
                if (DEBUG) {
                    Log.d(TAG, "새 일기 작성하기");
                }
            }
        } else if (icicle != null && icicle.containsKey(BUNDLE_KEY_DIARY_ID)) {
            diaryId = icicle.getInt(BUNDLE_KEY_DIARY_ID);
        }
        int userId = intent.getIntExtra(EXTRA_USER_ID, -1);
        long time = intent.getLongExtra(EXTRA_DIARY_TIME, -1);
        if (time != -1) {
            Date d = new Date(System.currentTimeMillis());
            info.day = d.getTime();
        }
        info.id = diaryId;
        info.userId = userId;
        info.color = intent.getIntExtra(EXTRA_DIARY_COLOR, -1);
        info.img = intent.getStringExtra(EXTRA_IMG);
        info.content = intent.getStringExtra(EXTRA_CONTENTS);
        info.weather = intent.getStringExtra(EXTRA_WEATHER);
        info.location = intent.getStringExtra(EXTRA_LOCATION);

        return info;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            //Utils.returnToCalendarHome(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
