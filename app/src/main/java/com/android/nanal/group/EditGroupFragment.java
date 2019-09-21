package com.android.nanal.group;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.nanal.R;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarController.GroupInfo;
import com.android.nanal.calendar.CalendarGroupModel;
import com.android.nanal.color.ColorPickerSwatch;
import com.android.nanal.color.HsvColorComparator;
import com.android.nanal.diary.DiaryColorCache;
import com.android.nanal.event.Utils;

import java.io.Serializable;

public class EditGroupFragment extends Fragment implements CalendarController.GroupHandler, ColorPickerSwatch.OnColorSelectedListener {
    private static final String TAG = "EditGroupActivity";
    private static final String COLOR_PICKER_DIALOG_TAG = "ColorPickerDialog";

    private static final String BUNDLE_KEY_MODEL = "key_model";
    private static final String BUNDLE_KEY_EDIT_STATE = "key_edit_state";
    private static final String BUNDLE_KEY_EVENT = "key_event";
    private static final String BUNDLE_KEY_READ_ONLY = "key_read_only";
    private static final String BUNDLE_KEY_EDIT_ON_LAUNCH = "key_edit_on_launch";
    private static final String BUNDLE_KEY_SHOW_COLOR_PALETTE = "show_color_palette";


    private static final String BUNDLE_KEY_DATE_BUTTON_CLICKED = "date_button_clicked";

    private static final boolean DEBUG = true;

    private static final int TOKEN_GROUP = 1;
    private static final int TOKEN_COLORS = 1 << 1;

    private static final int TOKEN_ALL = TOKEN_GROUP | TOKEN_COLORS;
    private static final int TOKEN_UNITIALIZED = 1 << 31;
    private final GroupInfo mGroup;
    private final Done mOnDone = new Done();
    private final Intent mIntent;
    public boolean mShowModifyDialogOnLaunch = false;
    EditGroupHelper mHelper;
    CalendarGroupModel mModel;
    CalendarGroupModel mOriginalModel;
    CalendarGroupModel mRestoreModel;
    EditGroupView mView;
    QueryHandler mHandler;
    int mModification = Utils.MODIFY_UNINITIALIZED;


    private int mOutstandingQueries = TOKEN_UNITIALIZED;
    private AlertDialog mModifyDialog;
    private EditGroupFragment.GroupBundle mGroupBundle;
    private int mGroupolor;
    private Uri mUri;
    private long mDay;
    private GroupColorPickerDialog mColorPickerDialog;
    private AppCompatActivity mContext;
    private boolean mSaveOnDetach = true;
    private boolean mIsReadOnly = false;
    private boolean mShowColorPalette = false;
    private boolean mTimeSelectedWasStartTime;
    private boolean mDateSelectedWasStartDate;
    private InputMethodManager mInputMethodManager;
    private final View.OnClickListener mActionBarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onActionBarItemSelected(v.getId());
        }
    };
    private boolean mUseCustomActionBar;
    private View.OnClickListener mOnColorPickerClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int[] colors = { getResources().getColor(R.color.colorPrimary),
                    getResources().getColor(R.color.colorBluePrimary),
                    getResources().getColor(R.color.colorPurplePrimary),
                    getResources().getColor(R.color.colorGreenPrimary),
                    getResources().getColor(R.color.colorOrangePrimary),
                    getResources().getColor(R.color.colorRedPrimary)
            };
                    mModel.getGroupColors();
            if (mColorPickerDialog == null) {
                mColorPickerDialog = GroupColorPickerDialog.newInstance(colors,
                        mModel.getGroupColor(), mView.mIsMultipane);
                mColorPickerDialog.setOnColorSelectedListener(EditGroupFragment.this);
            } else {
                mColorPickerDialog.setColors(colors, mModel.getGroupColor());
            }
            final android.app.FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.executePendingTransactions();
            if (!mColorPickerDialog.isAdded()) {
                mColorPickerDialog.show(fragmentManager, COLOR_PICKER_DIALOG_TAG);
            }
        }
    };

    public EditGroupFragment() {
        this(null, -1, false, null);
    }

    @SuppressLint("ValidFragment")
    public EditGroupFragment(GroupInfo group, int groupolor, boolean readOnly, Intent intent) {
        mGroup = group;
        mIsReadOnly = readOnly;
        mIntent = intent;

        setHasOptionsMenu(true);
    }


    private void setModelIfDone(int queryType) {
        synchronized (this) {
            mOutstandingQueries &= ~queryType;
            if (mOutstandingQueries == 0) {
                if (mRestoreModel != null) {
                    mModel = mRestoreModel;
                }
                if (mShowModifyDialogOnLaunch && mModification == Utils.MODIFY_UNINITIALIZED) {
                    mModification = Utils.MODIFY_ALL;
                }

            }
            mView.setModel(mModel);
            mView.setModification(mModification);
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mColorPickerDialog = (GroupColorPickerDialog) getActivity().getFragmentManager()
                .findFragmentByTag(COLOR_PICKER_DIALOG_TAG);
        if (mColorPickerDialog != null) {
            mColorPickerDialog.setOnColorSelectedListener(this);
        }
    }


    private void startQuery() {
        mUri = null;
        Uri uri = Uri.parse("content://" + "com.android.nanal" + "/group");
        if (mGroup != null) {
            if(mGroup.group_id != -1) {
                mModel.group_id = mGroup.group_id;
                mUri = ContentUris.withAppendedId(uri, mGroup.group_id);
            }
        } else if (mGroupBundle != null) {
            if (mGroupBundle.id != -1) {
                mGroup.group_id = mGroupBundle.id;
                mUri = ContentUris.withAppendedId(uri, mGroupBundle.id);
            }
        }

        // Kick off the query for the event
        boolean newGroup = mUri == null;
        if (!newGroup) {
            mOutstandingQueries = TOKEN_ALL;
            if (DEBUG) {
                Log.d(TAG, "startQuery: uri for event is " + mUri.toString());
            }
            mHandler.startQuery(TOKEN_GROUP, null, mUri, EditGroupHelper.GROUP_PROJECTION,
                    null /* selection */, null /* selection args */, null /* sort order */);
        } else {
            mOutstandingQueries = TOKEN_COLORS;
            if (DEBUG) {
                Log.d(TAG, "startQuery: Editing a new group.");
            }
//            String group_name = mGroup.group_name;
//            String group_color = Integer.toString(mGroup.group_color);
//            String account_id = AllInOneActivity.connectId;
//
//            CreateNewGroup.CreateGroup(new String[]{group_name, group_color, account_id});
            mHandler.startQuery(TOKEN_COLORS, null, CalendarContract.Colors.CONTENT_URI,
                    EditGroupHelper.COLORS_PROJECTION,
                    CalendarContract.Colors.COLOR_TYPE + "=" + CalendarContract.Colors.TYPE_EVENT, null, null);

            mModification = Utils.MODIFY_ALL;
            mView.setModification(mModification);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = (AppCompatActivity) activity;

        mHelper = new EditGroupHelper(activity);
        mHandler = new EditGroupFragment.QueryHandler(activity.getContentResolver());
        mModel = new CalendarGroupModel();
        mInputMethodManager = (InputMethodManager)
                activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        mUseCustomActionBar = !Utils.getConfigBool(mContext, R.bool.multiple_pane_config);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        mContext.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        View view;
        //todo:레이아웃 추가
        view = inflater.inflate(R.layout.edit_group, null);
        mView = new EditGroupView(mContext, view, mOnDone);

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            //If permission is not granted
            Toast.makeText(mContext, R.string.calendar_permission_not_granted, Toast.LENGTH_LONG).show();
        } else {
            startQuery();
        }


        if (mUseCustomActionBar) {
            View actionBarButtons = inflater.inflate(R.layout.edit_event_custom_actionbar,
                    new LinearLayout(mContext), false);
            View cancelActionView = actionBarButtons.findViewById(R.id.action_cancel);
            cancelActionView.setOnClickListener(mActionBarListener);
            View doneActionView = actionBarButtons.findViewById(R.id.action_done);
            doneActionView.setOnClickListener(mActionBarListener);
            ActionBar.LayoutParams layout = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
            mContext.getSupportActionBar().setCustomView(actionBarButtons, layout);
        }

        return view;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mUseCustomActionBar) {
            mContext.getSupportActionBar().setCustomView(null);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(EditGroupFragment.this.getActivity(),
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(EditGroupFragment.this.getActivity(), new String[]{Manifest.permission.READ_CONTACTS},
                    0);
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BUNDLE_KEY_MODEL)) {
                mRestoreModel = (CalendarGroupModel) savedInstanceState.getSerializable(
                        BUNDLE_KEY_MODEL);
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_EDIT_STATE)) {
                mModification = savedInstanceState.getInt(BUNDLE_KEY_EDIT_STATE);
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_EDIT_ON_LAUNCH)) {
                mShowModifyDialogOnLaunch = savedInstanceState
                        .getBoolean(BUNDLE_KEY_EDIT_ON_LAUNCH);
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_EVENT)) {
                mGroupBundle = (EditGroupFragment.GroupBundle) savedInstanceState.getSerializable(BUNDLE_KEY_EVENT);
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_READ_ONLY)) {
                mIsReadOnly = savedInstanceState.getBoolean(BUNDLE_KEY_READ_ONLY);
            }
            if (savedInstanceState.containsKey("EditEventView_timebuttonclicked")) {
                mTimeSelectedWasStartTime = savedInstanceState.getBoolean(
                        "EditEventView_timebuttonclicked");
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_DATE_BUTTON_CLICKED)) {
                mDateSelectedWasStartDate = savedInstanceState.getBoolean(
                        BUNDLE_KEY_DATE_BUTTON_CLICKED);
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_SHOW_COLOR_PALETTE)) {
                mShowColorPalette = savedInstanceState.getBoolean(BUNDLE_KEY_SHOW_COLOR_PALETTE);
            }

        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (!mUseCustomActionBar) {
            inflater.inflate(R.menu.edit_event_title_bar, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onActionBarItemSelected(item.getItemId());
    }

    private boolean onActionBarItemSelected(int itemId) {
        if (itemId == R.id.action_done) {
            // 상단 취소/완료 버튼 클릭했을 때 처리하는 곳
            if (mView != null && mView.prepareForSave()) {
                Log.d(TAG, "onActionBarItemSelected: 완료");
                if (mModification == Utils.MODIFY_UNINITIALIZED) {
                    mModification = Utils.MODIFY_ALL;
                }
                mOnDone.setDoneCode(Utils.DONE_SAVE | Utils.DONE_EXIT);
                mOnDone.run();
            } else {
                Log.d(TAG, "onActionBarItemSelected: 완료 else문");
                mOnDone.setDoneCode(Utils.DONE_REVERT);
                mOnDone.run();
            }
        } else if (itemId == R.id.action_cancel) {
            Log.d(TAG, "onActionBarItemSelected: 취소");
            mOnDone.setDoneCode(Utils.DONE_REVERT);
            mOnDone.run();
        }
        return true;
    }

    boolean isEmptyNewGroup() {
        if (mOriginalModel != null) {
            // Not new
            return false;
        }
        return mModel.isEmpty();
    }

    @Override
    public void onPause() {
        Activity act = getActivity();
        if (mSaveOnDetach && act != null && !mIsReadOnly && !act.isChangingConfigurations()
                && mView.prepareForSave()) {
            mOnDone.setDoneCode(Utils.DONE_SAVE);
            mOnDone.run();
        }
        if (act !=null && (Build.VERSION.SDK_INT < 23 ||
                ContextCompat.checkSelfPermission(EditGroupFragment.this.getActivity(),
                        Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED))
            act.finish();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mView != null) {
            mView.setModel(null);
        }
        if (mModifyDialog != null) {
            mModifyDialog.dismiss();
            mModifyDialog = null;
        }
        super.onDestroy();
    }

    @Override
    public void groupsChanged() {
        // TODO Requery to see if event has changed
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mView.prepareForSave();
        outState.putSerializable(BUNDLE_KEY_MODEL, mModel);
        outState.putInt(BUNDLE_KEY_EDIT_STATE, mModification);
        if (mGroupBundle == null && mGroup != null) {
            mGroupBundle = new EditGroupFragment.GroupBundle();
            mGroupBundle.id = mGroup.group_id;
        }
        outState.putBoolean(BUNDLE_KEY_EDIT_ON_LAUNCH, mShowModifyDialogOnLaunch);
        outState.putSerializable(BUNDLE_KEY_EVENT, mGroupBundle);
        outState.putBoolean(BUNDLE_KEY_READ_ONLY, mIsReadOnly);
        outState.putBoolean(BUNDLE_KEY_SHOW_COLOR_PALETTE, mView.isColorPaletteVisible());

        outState.putBoolean("EditGroupView_timebuttonclicked", mView.mTimeSelectedWasStartTime);
        outState.putBoolean(BUNDLE_KEY_DATE_BUTTON_CLICKED, mView.mDateSelectedWasStartDate);
    }

    @Override
    public long getSupportedGroupTypes() {
        return CalendarController.EventType.USER_HOME;
    }

    @Override
    public void handleEvent(GroupInfo group) {
        // It's currently unclear if we want to save the event or not when home
        // is pressed. When creating a new event we shouldn't save since we
        // can't get the id of the new event easily.
        if ((false && group.eventType == CalendarController.EventType.USER_HOME) || (group.eventType == CalendarController.EventType.GO_TO
                && mSaveOnDetach)) {
            if (mView != null && mView.prepareForSave()) {
                mOnDone.setDoneCode(Utils.DONE_SAVE);
                mOnDone.run();
            }
        }
    }

    @Override
    public void onColorSelected(int color) {
        if (mModel.getGroupColor() != color) {
            mModel.setGroupColor(color);
            mView.updateHeadlineColor(mModel, color);
            mView.mIvColor.setColorFilter(color);
        }
    }

    private static class GroupBundle implements Serializable {
        private static final long serialVersionUID = 1L;
        int id = -1;
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            // If the query didn't return a cursor for some reason return
            if (cursor == null) {
                return;
            }

            // If the Activity is finishing, then close the cursor.
            // Otherwise, use the new cursor in the adapter.
            final Activity activity = EditGroupFragment.this.getActivity();
            if (activity == null || activity.isFinishing()) {
                cursor.close();
                return;
            }
            int groupId;
            switch (token) {
                case TOKEN_GROUP:
                    if (cursor.getCount() == 0) {
                        // The cursor is empty. This can happen if the event
                        // was deleted.
                        cursor.close();
                        mOnDone.setDoneCode(Utils.DONE_EXIT);
                        mSaveOnDetach = false;
                        mOnDone.run();
                        return;
                    }
                    mOriginalModel = new CalendarGroupModel();
                    EditGroupHelper.setModelFromCursor(mOriginalModel, cursor);
                    EditGroupHelper.setModelFromCursor(mModel, cursor);
                    cursor.close();

                    mOriginalModel.mUri = mUri.toString();

                    mModel.mUri = mUri.toString();
                    groupId = mModel.group_id;

                    // TOKEN_COLORS
                    mHandler.startQuery(TOKEN_COLORS, null, CalendarContract.Colors.CONTENT_URI,
                            EditGroupHelper.COLORS_PROJECTION,
                            CalendarContract.Colors.COLOR_TYPE + "=" + CalendarContract.Colors.TYPE_EVENT, null, null);

                    setModelIfDone(TOKEN_GROUP);
                    break;
                case TOKEN_COLORS:
                    if (cursor.moveToFirst()) {
                        DiaryColorCache cache = new DiaryColorCache();
                        do {
                            int group_Id = cursor.getInt(EditGroupHelper.PROJECTION_GROUP_ID_INDEX);
                            String groupName = cursor.getString(EditGroupHelper.PROJECTION_GROUP_NAME_INDEX);
                            int groupColor = cursor.getInt(EditGroupHelper.PROJECTION_GROUP_COLOR_INDEX);
                            String colorKey = cursor.getString(EditGroupHelper.COLORS_INDEX_COLOR_KEY);
                            int rawColor = cursor.getInt(EditGroupHelper.COLORS_INDEX_COLOR);
                            int displayColor = Utils.getDisplayColorFromColor(rawColor);
                            String accountName = cursor
                                    .getString(EditGroupHelper.COLORS_INDEX_ACCOUNT_NAME);
                            String accountType = cursor
                                    .getString(EditGroupHelper.COLORS_INDEX_ACCOUNT_TYPE);
                            cache.insertColor(accountName, group_Id, colorKey);
                        } while (cursor.moveToNext());
                        cache.sortPalettes(new HsvColorComparator());

                        mView.mLlColor.setOnClickListener(mOnColorPickerClicked);
                    }
                    if (cursor != null) {
                        cursor.close();
                    }

                    // If the account name/type is null, the calendar event colors cannot be
                    // determined, so take the default/savedInstanceState value.

                    setModelIfDone(TOKEN_COLORS);
                    break;
                default:
                    cursor.close();
                    break;
            }
        }
    }

    class Done implements EditGroupHelper.EditDoneRunnable {
        private int mCode = -1;

        @Override
        public void setDoneCode(int code) {
            mCode = code;
        }

        @Override
        public void run() {
            // We only want this to get called once, either because the user
            // pressed back/home or one of the buttons on screen
            mSaveOnDetach = false;
            if (mModification == Utils.MODIFY_UNINITIALIZED) {
                // If this is uninitialized the user hit back, the only
                // changeable item is response to default to all events.
                mModification = Utils.MODIFY_ALL;
            }

            if ((mCode & Utils.DONE_SAVE) != 0 && mModel != null
                    && mView.prepareForSave()
                    && mHelper.saveGroup(mModel, mOriginalModel)) {
                int stringResource;
                if (mModel.mUri != null) {
                    stringResource = R.string.saving_group;
                } else {
                    stringResource = R.string.creating_group;
                }

                Toast.makeText(mContext, stringResource, Toast.LENGTH_SHORT).show();
            } else if ((mCode & Utils.DONE_SAVE) != 0 && mModel != null && isEmptyNewGroup()) {
                Toast.makeText(mContext, R.string.empty_group, Toast.LENGTH_SHORT).show();
            }

            if ((mCode & Utils.DONE_DELETE) != 0 && mOriginalModel != null) {
                DeleteGroupHelper deleteHelper = new DeleteGroupHelper(
                        mContext, mContext, !mIsReadOnly /* exitWhenDone */);
                deleteHelper.delete(mOriginalModel);
            }

            if ((mCode & Utils.DONE_EXIT) != 0) {
                // This will exit the edit event screen, should be called
                // when we want to return to the main calendar views
                if ((mCode & Utils.DONE_SAVE) != 0) {
                    if (mContext != null) {
//                        CalendarController.getInstance(mContext).launchViewDiary(-1, day, 0);
                        Toast.makeText(mContext, "테스트", Toast.LENGTH_LONG).show();
                    }
                }
                Activity a = EditGroupFragment.this.getActivity();
                if (a != null) {
                    a.finish();
                }
            }

            // Hide a software keyboard so that user won't see it even after this Fragment's
            // disappearing.
            final View focusedView = mContext.getCurrentFocus();
            if (focusedView != null) {
                mInputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
            }
        }
    }
}
