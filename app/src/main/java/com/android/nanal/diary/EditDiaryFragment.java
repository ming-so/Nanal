package com.android.nanal.diary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
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

import com.android.nanal.NanalDBHelper;
import com.android.nanal.R;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarController.DiaryInfo;
import com.android.nanal.calendar.CalendarDiaryModel;
import com.android.nanal.color.ColorPickerSwatch;
import com.android.nanal.color.HsvColorComparator;
import com.android.nanal.event.Utils;

import java.io.Serializable;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//import android.support.v7.app.ActionBar;
//import android.support.v7.app.AppCompatActivity;

public class EditDiaryFragment extends Fragment implements CalendarController.DiaryHandler, ColorPickerSwatch.OnColorSelectedListener {
    private static final String TAG = "EditDiaryActivity";
    private static final String COLOR_PICKER_DIALOG_TAG = "ColorPickerDialog";

    private static final String BUNDLE_KEY_MODEL = "key_model";
    private static final String BUNDLE_KEY_EDIT_STATE = "key_edit_state";
    private static final String BUNDLE_KEY_EVENT = "key_event";
    private static final String BUNDLE_KEY_READ_ONLY = "key_read_only";
    private static final String BUNDLE_KEY_EDIT_ON_LAUNCH = "key_edit_on_launch";
    private static final String BUNDLE_KEY_SHOW_COLOR_PALETTE = "show_color_palette";

    private static final String BUNDLE_KEY_DATE_BUTTON_CLICKED = "date_button_clicked";

    private static final boolean DEBUG = false;

    private static final int TOKEN_DIARY = 1;
    private static final int TOKEN_COLORS = 1 << 1;

    private int mId = -1;


    private static final int TOKEN_ALL = TOKEN_DIARY | TOKEN_COLORS;
    private static final int TOKEN_UNITIALIZED = 1 << 31;
    private final DiaryInfo mDiary;
    private final Done mOnDone = new Done();
    private final Intent mIntent;
    public boolean mShowModifyDialogOnLaunch = false;
    EditDiaryHelper mHelper;
    CalendarDiaryModel mModel;
    CalendarDiaryModel mOriginalModel;
    CalendarDiaryModel mRestoreModel;
    EditDiaryView mView;
    QueryHandler mHandler;
    int mModification = Utils.MODIFY_UNINITIALIZED;

    private int mOutstandingQueries = TOKEN_UNITIALIZED;
    private AlertDialog mModifyDialog;
    private DiaryBundle mDiaryBundle;
    private int mDiaryColor;
    private long mDay;
    private DiaryColorPickerDialog mColorPickerDialog;
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
            if (mColorPickerDialog == null) {
                mColorPickerDialog = DiaryColorPickerDialog.newInstance(colors,
                        mModel.getDiaryColor(), mView.mIsMultipane);
                mColorPickerDialog.setOnColorSelectedListener(EditDiaryFragment.this);
            } else {
                mColorPickerDialog.setColors(colors, mModel.getDiaryColor());
            }
            final android.app.FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.executePendingTransactions();
            if (!mColorPickerDialog.isAdded()) {
                mColorPickerDialog.show(fragmentManager, COLOR_PICKER_DIALOG_TAG);
            }
        }
    };

    public EditDiaryFragment() {
        this(null, -1, false, null);
    }

    @SuppressLint("ValidFragment")
    public EditDiaryFragment(DiaryInfo diary, int diaryColor, boolean readOnly, Intent intent) {
        mDiary = diary;
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
        mColorPickerDialog = (DiaryColorPickerDialog) getActivity().getFragmentManager()
                .findFragmentByTag(COLOR_PICKER_DIALOG_TAG);
        if (mColorPickerDialog != null) {
            mColorPickerDialog.setOnColorSelectedListener(this);
        }
    }


    private void startQuery() {
        int diary_id = -1;
        mDay = -1;
        NanalDBHelper helper = AllInOneActivity.helper;
        if(mDiary != null) {
            if(mDiary.id != -1) {
                mModel.mDiaryId = mDiary.id;
            }
            if(mDiary.day > 0) {
                mDay = mDiary.day;
            }
        }
        if(mDay <= 0) {
            mDay = mHelper.constructDefaultStartTime(System.currentTimeMillis());
        }

        // Kick off the query for the event
        boolean newDiary = mDiary.id == -1;

        if (!newDiary) {
            // 수정해야 하는 경우
            mOutstandingQueries = TOKEN_ALL;
            mHandler.startQuery(TOKEN_DIARY, null, null, EditDiaryHelper.DIARY_PROJECTION,
                    null /* selection */, null /* selection args */, null /* sort order */);
        } else {
            mModel.mDiaryUserId = AllInOneActivity.connectId;
            mOutstandingQueries = TOKEN_COLORS;
            if (DEBUG) {
                Log.d(TAG, "startQuery: Editing a new event.");
            }
            mModel.mDiaryDay = mDay;
            mModel.mDiaryColor = mDiaryColor;
            mHandler.startQuery(TOKEN_COLORS, null, CalendarContract.Colors.CONTENT_URI,
                    EditDiaryHelper.COLORS_PROJECTION,
                    CalendarContract.Colors.COLOR_TYPE + "=" + CalendarContract.Colors.TYPE_EVENT, null, null);

            mModification = Utils.MODIFY_ALL;
            mView.setModification(mModification);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = (AppCompatActivity) activity;

        mHelper = new EditDiaryHelper(activity);
        mHandler = new EditDiaryFragment.QueryHandler(activity.getContentResolver());
        mModel = new CalendarDiaryModel(activity, mIntent);
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
        view = inflater.inflate(R.layout.edit_diary, null);
        mView = new EditDiaryView(mContext, view, mOnDone, mTimeSelectedWasStartTime,
                mDateSelectedWasStartDate);

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

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(EditDiaryFragment.this.getActivity(),
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(EditDiaryFragment.this.getActivity(), new String[]{Manifest.permission.READ_CONTACTS},
                    0);
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BUNDLE_KEY_MODEL)) {
                mRestoreModel = (CalendarDiaryModel) savedInstanceState.getSerializable(
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
                mDiaryBundle = (EditDiaryFragment.DiaryBundle) savedInstanceState.getSerializable(BUNDLE_KEY_EVENT);
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

    boolean isEmptyNewDiary() {
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
                ContextCompat.checkSelfPermission(EditDiaryFragment.this.getActivity(),
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
    public void diariesChanged() {
        // TODO Requery to see if event has changed
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mView.prepareForSave();
        outState.putSerializable(BUNDLE_KEY_MODEL, mModel);
        outState.putInt(BUNDLE_KEY_EDIT_STATE, mModification);
        if (mDiaryBundle == null && mDiary != null) {
            mDiaryBundle = new EditDiaryFragment.DiaryBundle();
            mDiaryBundle.id = mDiary.id;
            if (mDiary.day <= 0) {
                mDiaryBundle.day = mDiary.day;
            }
        }
        outState.putBoolean(BUNDLE_KEY_EDIT_ON_LAUNCH, mShowModifyDialogOnLaunch);
        outState.putSerializable(BUNDLE_KEY_EVENT, mDiaryBundle);
        outState.putBoolean(BUNDLE_KEY_READ_ONLY, mIsReadOnly);
        outState.putBoolean(BUNDLE_KEY_SHOW_COLOR_PALETTE, mView.isColorPaletteVisible());

        outState.putBoolean("EditDiaryView_timebuttonclicked", mView.mTimeSelectedWasStartTime);
        outState.putBoolean(BUNDLE_KEY_DATE_BUTTON_CLICKED, mView.mDateSelectedWasStartDate);
    }

    @Override
    public long getSupportedDiaryTypes() {
        return CalendarController.EventType.USER_HOME;
    }

    @Override
    public void handleEvent(DiaryInfo event) {
        // It's currently unclear if we want to save the event or not when home
        // is pressed. When creating a new event we shouldn't save since we
        // can't get the id of the new event easily.
        if ((false && event.eventType == CalendarController.EventType.USER_HOME) || (event.eventType == CalendarController.EventType.GO_TO
                && mSaveOnDetach)) {
            if (mView != null && mView.prepareForSave()) {
                mOnDone.setDoneCode(Utils.DONE_SAVE);
                mOnDone.run();
            }
        }
    }

    @Override
    public void onColorSelected(int color) {
        if (mModel.isInGroup()) {
            Toast.makeText(getActivity(), "그룹에 있음", Toast.LENGTH_LONG).show();
            mModel.setDiaryColor(AllInOneActivity.helper.getGroupColor(mModel.mDiaryGroupId));
            return;
        }
        if (mModel.getDiaryColor() != color) {
            Toast.makeText(getActivity(), "색상 선택="+color , Toast.LENGTH_LONG).show();
            mModel.setDiaryColor(color);
            mDiaryColor = color;
            mView.updateHeadlineColor(mModel, color);
            mView.mIvColor.setColorFilter(color);
        }
    }

    private static class DiaryBundle implements Serializable {
        private static final long serialVersionUID = 1L;
        int id = -1;
        long day = -1;
    }


    // TODO turn this into a helper function in EditEventHelper for building the
    // model
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
            final Activity activity = EditDiaryFragment.this.getActivity();
            if (activity == null || activity.isFinishing()) {
                cursor.close();
                return;
            }
            int diaryId;
            switch (token) {
                case TOKEN_DIARY:
                    if (cursor.getCount() == 0) {
                        // The cursor is empty. This can happen if the event
                        // was deleted.
                        cursor.close();
                        mOnDone.setDoneCode(Utils.DONE_EXIT);
                        mSaveOnDetach = false;
                        mOnDone.run();
                        return;
                    }
                    mOriginalModel = new CalendarDiaryModel();
                    EditDiaryHelper.setModelFromCursor(mOriginalModel, cursor);
                    EditDiaryHelper.setModelFromCursor(mModel, cursor);
                    cursor.close();

                    mOriginalModel.mDiaryId = mId;
                    mModel.mDiaryId = mId;

                    mModel.mDiaryDay = mDay;

                    // TOKEN_COLORS
                    mHandler.startQuery(TOKEN_COLORS, null, CalendarContract.Colors.CONTENT_URI,
                            EditDiaryHelper.COLORS_PROJECTION,
                            CalendarContract.Colors.COLOR_TYPE + "=" + CalendarContract.Colors.TYPE_EVENT, null, null);

                    setModelIfDone(TOKEN_DIARY);
                    break;
                case TOKEN_COLORS:
                    if (cursor.moveToFirst()) {
                        DiaryColorCache cache = new DiaryColorCache();
                        do {
                            String userId = cursor.getString(EditDiaryHelper.PROJECTION_ACCOUNT_ID_INDEX);
                            int groupId = cursor.getInt(EditDiaryHelper.GROUP_INDEX_ID);
                            String colorKey = cursor.getString(EditDiaryHelper.COLORS_INDEX_COLOR_KEY);
                            int rawColor = cursor.getInt(EditDiaryHelper.COLORS_INDEX_COLOR);
                            int displayColor = Utils.getDisplayColorFromColor(rawColor);
                            String accountName = cursor
                                    .getString(EditDiaryHelper.COLORS_INDEX_ACCOUNT_NAME);
                            String accountType = cursor
                                    .getString(EditDiaryHelper.COLORS_INDEX_ACCOUNT_TYPE);
                            cache.insertColor(userId, groupId, colorKey);
                        } while (cursor.moveToNext());
                        cache.sortPalettes(new HsvColorComparator());

                        mView.mLlColor.setOnClickListener(mOnColorPickerClicked);
                    }
                    if (cursor != null) {
                        cursor.close();
                    }

                    // If the account name/type is null, the calendar event colors cannot be
                    // determined, so take the default/savedInstanceState value.
                    if (mModel.mDiaryGroupId == -1) {
                        //mView.setColorPickerButtonStates(mShowColorPalette);
                    } else {
                        //mView.setColorPickerButtonStates(mModel.getGroupColors());
                    }
                    setModelIfDone(TOKEN_COLORS);
                    break;
                default:
                    cursor.close();
                    break;
            }
        }
    }

    class Done implements EditDiaryHelper.EditDoneRunnable {
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
                    && mHelper.saveDiary(mModel, mOriginalModel)) {
                int stringResource;
                if (mModel.mDiaryId != -1) {
                    stringResource = R.string.saving_diary;
                } else {
                    stringResource = R.string.creating_diary;
                }

                Toast.makeText(mContext, stringResource, Toast.LENGTH_SHORT).show();
            } else if ((mCode & Utils.DONE_SAVE) != 0 && mModel != null && isEmptyNewDiary()) {
                Toast.makeText(mContext, R.string.empty_diary, Toast.LENGTH_SHORT).show();
            }

            if ((mCode & Utils.DONE_DELETE) != 0 && mOriginalModel != null) {
                long day = mModel.mDiaryDay;
                DeleteDiaryHelper deleteHelper = new DeleteDiaryHelper(
                        mContext, mContext, !mIsReadOnly /* exitWhenDone */);
                deleteHelper.delete(day, mOriginalModel);
            }

            if ((mCode & Utils.DONE_EXIT) != 0) {
                // This will exit the edit event screen, should be called
                // when we want to return to the main calendar views
                if ((mCode & Utils.DONE_SAVE) != 0) {
                    if (mContext != null) {
                        long day = mModel.mDiaryDay;
                        CalendarController.getInstance(mContext).launchViewDiary(-1, day, 0);
                    }
                }
                Activity a = EditDiaryFragment.this.getActivity();
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
