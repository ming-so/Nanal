package com.android.nanal.activity;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.android.nanal.DynamicTheme;
import com.android.nanal.R;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarController.GroupInfo;
import com.android.nanal.group.EditGroupFragment;

public class EditGroupActivity extends AbstractCalendarActivity {
    public static final String EXTRA_GROUP_ID = "group_id";
    public static final String EXTRA_GROUP_NAME = "group_name";
    public static final String EXTRA_GROUP_COLOR = "group_color";
    public static final String EXTRA_GROUP_ACCOUNT = "account_id";
    public static final String EXTRA_READ_ONLY = "read_only";

    private static final String TAG = "EditGroupActivity";
    private static final boolean DEBUG = false;

    private static final String BUNDLE_KEY_GROUP_ID = "key_group_id";

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private EditGroupFragment mEditFragment;

    private static boolean mIsMultipane;
    private int mGroupColor;

    private CalendarController.GroupInfo mGroupInfo;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        dynamicTheme.onCreate(this);
        setContentView(R.layout.simple_frame_layout_material);
        mGroupInfo = getmGroupInfoFromIntent(icicle);
        mGroupColor = getIntent().getIntExtra(EXTRA_GROUP_COLOR, -1);
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        mEditFragment = (EditGroupFragment) getFragmentManager().findFragmentById(R.id.body_frame);

        if (mIsMultipane) {
            getSupportActionBar().setDisplayOptions(
                    ActionBar.DISPLAY_SHOW_TITLE,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME
                            | ActionBar.DISPLAY_SHOW_TITLE);
            getSupportActionBar().setTitle(
                    mGroupInfo.group_id == -1 ? R.string.group_create : R.string.group_edit);
        } else {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME |
                            ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        if (mEditFragment == null) {
            Intent intent = null;
            boolean readOnly = false;
            if (mGroupInfo.group_id == -1) {
                intent = getIntent();
                readOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false);
            }

            mEditFragment = new EditGroupFragment(mGroupInfo, mGroupColor, readOnly, intent);

            mEditFragment.mShowModifyDialogOnLaunch = getIntent().getBooleanExtra(
                    CalendarController.EVENT_EDIT_ON_LAUNCH, false);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.body_frame, mEditFragment);
            ft.show(mEditFragment);
            ft.commit();
        }
    }

    private GroupInfo getmGroupInfoFromIntent(Bundle icicle) {
        GroupInfo info = new GroupInfo();
        long qroupId = -1;
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            try {
                 qroupId = Integer.parseInt(data.getLastPathSegment());
            } catch (NumberFormatException e) {
                if (DEBUG) {
                    Log.d(TAG, "새 그룹 생성하기");
                }
            }
        } else if (icicle != null && icicle.containsKey(BUNDLE_KEY_GROUP_ID)) {
            qroupId = icicle.getInt(BUNDLE_KEY_GROUP_ID);
        }
        int groupId = intent.getIntExtra(EXTRA_GROUP_ID, -1);
        String groupName = intent.getStringExtra(EXTRA_GROUP_NAME);
        int groupColor = intent.getIntExtra(EXTRA_GROUP_COLOR, -1);
        String accountId = intent.getStringExtra(EXTRA_GROUP_ACCOUNT);

        info.group_id = groupId;
        info.group_name = groupName;
        info.group_color = groupColor;
        info.account_id = accountId;

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
