package com.android.nanal.activity;

import com.android.nanal.DynamicTheme;
import com.android.nanal.group.EditGroupFragment;

public class EditGroupActivity extends AbstractCalendarActivity {
    public static final String EXTRA_GROUP_NAME = "group_name";
    public static final String EXTRA_GROUP_COLOR = "group_color";

    private static final String TAG = "EditGroupActivity";
    private static final boolean DEBUG = false;

    private static final String BUNDLE_KEY_DIARY_ID = "key_diary_id";

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private EditGroupFragment mEditFragment;


}
