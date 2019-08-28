package com.android.nanal.diary;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.CalendarContract;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.nanal.EmailAddressAdapter;
import com.android.nanal.R;
import com.android.nanal.Rfc822InputFilter;
import com.android.nanal.calendar.CalendarDiaryModel;
import com.android.nanal.chips.AccountSpecifier;
import com.android.nanal.diary.EditDiaryHelper.EditDoneRunnable;
import com.android.nanal.event.EventInfoFragment;
import com.android.nanal.event.GeneralPreferences;
import com.android.nanal.event.Utils;
import com.android.nanal.timezonepicker.TimeZoneInfo;
import com.android.nanal.timezonepicker.TimeZonePickerDialog;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

public class EditDiaryView implements DialogInterface.OnCancelListener,
        DialogInterface.OnClickListener, OnItemSelectedListener,
        TimeZonePickerDialog.OnTimeZoneSetListener {

    private static final String TAG = "EditDiary";
    private static final String PERIOD_SPACE = ". ";

    private static final String FRAG_TAG_DATE_PICKER = "datePickerDialogFragment";
    private static final String FRAG_TAG_TIME_PICKER = "timePickerDialogFragment";
    private static final String FRAG_TAG_TIME_ZONE_PICKER = "timeZonePickerDialogFragment";
    private static final String FRAG_TAG_RECUR_PICKER = "recurrencePickerDialogFragment";
    private static StringBuilder mSB = new StringBuilder(50);
    private static Formatter mF = new Formatter(mSB, Locale.getDefault());

    private static InputFilter[] sRecipientFilters = new InputFilter[]{new Rfc822InputFilter()};
    public boolean mIsMultipane;
    public boolean mTimeSelectedWasStartTime;
    public boolean mDateSelectedWasStartDate;
    ArrayList<View> mEditOnlyList = new ArrayList<View>();
    ArrayList<View> mEditViewList = new ArrayList<View>();
    ArrayList<View> mViewOnlyList = new ArrayList<View>();
    TextView mLoadingMessage;
    ScrollView mScrollView;
    TextView mTvDay, mTvColor, mTvPic, mTvLocation;
    EditText mEtTitle, mEtContent;
    ImageView mIvColor, mIvGroup, mIvPic, mIvWeather;
    Spinner mGroupSpinner;

    View mColorPickerNewEvent;
    View mColorPickerExistingEvent;
    View.OnClickListener mChangeColorOnClickListener;
    View mTimezoneRow;
    Spinner mCalendarsSpinner;
    Spinner mAvailabilitySpinner;
    Spinner mAccessLevelSpinner;
    RadioGroup mResponseRadioGroup;
    TextView mTitleTextView;
    AutoCompleteTextView mLocationTextView;
    View mCalendarSelectorGroup;
    private int[] mOriginalPadding = new int[4];
    private ProgressDialog mLoadingCalendarsDialog;
    private Activity mActivity;
    private EditDoneRunnable mDone;
    private View mView;
    private CalendarDiaryModel mModel;
    private Cursor mGroupsCursor;
    private AccountSpecifier mAddressAdapter;

    private boolean mSaveAfterQueryComplete = false;
    private String mStringDay;
    private long mLongDay;
    private Date mDay;
    private Time mTime;
    private String mTimezone;
    private int mModification = EditDiaryHelper.MODIFY_UNINITIALIZED;

    public String connectID = "test";


    public EditDiaryView(Activity activity, View view, EditDiaryHelper.EditDoneRunnable done,
                         boolean timeSelectedWasStartTime, boolean dateSelectedWasStartDate) {
        mActivity = activity;
        mView = view;
        mDone = done;

        mLoadingMessage = view.findViewById(R.id.d_loading_message);
        mScrollView = (ScrollView) view.findViewById(R.id.d_scroll_view);

        mTvDay = view.findViewById(R.id.tv_edit_diary_day);
        mTvColor = view.findViewById(R.id.tv_edit_diary_color);
        mTvPic = view.findViewById(R.id.tv_edit_diary_pic);
        mTvLocation = view.findViewById(R.id.tv_edit_diary_location);

        mEtTitle = view.findViewById(R.id.et_edit_diary_title);
        mEtContent = view.findViewById(R.id.et_edit_diary_content);

        mIvColor = view.findViewById(R.id.iv_edit_diary_color);
        mIvGroup = view.findViewById(R.id.iv_edit_diary_group);
        mIvPic = view.findViewById(R.id.iv_edit_diary_pic);
        mIvWeather = view.findViewById(R.id.iv_edit_diary_weather);

        mGroupSpinner = view.findViewById(R.id.sp_edit_diary_group);

        mColorPickerNewEvent = view.findViewById(R.id.tv_edit_diary_color);
        mColorPickerExistingEvent = view.findViewById(R.id.tv_edit_diary_color);

        mCalendarSelectorGroup = view.findViewById(R.id.ll_edit_diary_group);
        View mCalendarStaticGroup;
        View mLocationGroup;
        View mDescriptionGroup;

        mTimezone = Utils.getTimeZone(activity, null);
        mDay = new Date(System.currentTimeMillis());

        mStringDay = SetStringDay(mDay);
        SetLongDay(mDay);
        mTvDay.setText(mStringDay);

        setModel(null);
    }

    private String SetStringDay(Date day) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        return fmt.format(day);
    }

    private void SetLongDay(Date day) {
        mLongDay = day.getTime();
    }

    private static ArrayList<Integer> loadIntegerArray(Resources r, int resNum) {
        int[] vals = r.getIntArray(resNum);
        int size = vals.length;
        ArrayList<Integer> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            list.add(vals[i]);
        }

        return list;
    }

    private static ArrayList<String> loadStringArray(Resources r, int resNum) {
        String[] labels = r.getStringArray(resNum);
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(labels));
        return list;
    }

    @Override
    public void onTimeZoneSet(TimeZoneInfo tzi) {
        setTimezone(tzi.mTzId);
    }

    private void setTimezone(String timeZone) {
        mTimezone = timeZone;
        mTime.timezone = mTimezone;
    }

    public boolean prepareForSave() {
        if (mModel == null || (mGroupsCursor == null && mModel.mUri == null)) {
            return false;
        }
        return fillModelFromUI();
    }

    // This is called if the user cancels the "No calendars" dialog.
    // The "No calendars" dialog is shown if there are no syncable calendars.
    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == mLoadingCalendarsDialog) {
            mLoadingCalendarsDialog = null;
            mSaveAfterQueryComplete = false;
        }
//        else if (dialog == mNoAccountAccessDialog) {
//            mDone.setDoneCode(Utils.DONE_REVERT);
//            mDone.run();
//            return;
//        }
    }

    // This is called if the user clicks on a dialog button.
    @Override
    public void onClick(DialogInterface dialog, int which) {
//        if (dialog == mNoAccountAccessDialog) {
//            mDone.setDoneCode(Utils.DONE_REVERT);
//            mDone.run();
//            if (which == DialogInterface.BUTTON_POSITIVE) {
                //todo:인증받는? 인증 안내하는? 화면으로 이동
//                Intent nextIntent = new Intent(Settings.ACTION_ADD_ACCOUNT);
//                final String[] array = {"com.android.nanal"};
//                nextIntent.putExtra(Settings.EXTRA_AUTHORITIES, array);
//                nextIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                mActivity.startActivity(nextIntent);
//            }
//        }
    }

    // Goes through the UI elements and updates the model as necessary
    private boolean fillModelFromUI() {
        if (mModel == null) {
            return false;
        }
        mModel.mDiaryDay = mLongDay;
        mModel.mDiaryTitle = mEtTitle.getText().toString();
        mModel.mDiaryContent = mEtContent.getText().toString();
        //todo:색상 사진 위치 날씨 등 처리
        if(TextUtils.isEmpty(mModel.mDiaryTitle)) {
            mModel.mDiaryTitle = null;
        }
        if(TextUtils.isEmpty(mModel.mDiaryContent)) {
            mModel.mDiaryContent = null;
        }

        int status = EventInfoFragment.getResponseFromButtonId(mResponseRadioGroup
                .getCheckedRadioButtonId());

        // If this was a new event we need to fill in the Calendar information
        if (mModel.mUri == null) {

            mModel.mDiaryGroupId = (int) mGroupSpinner.getSelectedItemId();
            int groupCursorPosition = mGroupSpinner.getSelectedItemPosition();
            if(mGroupsCursor.moveToPosition(groupCursorPosition)) {
                String groupName = mGroupsCursor.getString(
                        EditDiaryHelper.GROUP_INDEX_NAME);
                //todo:기본(로컬) 캘린더 설정
                String defaultGroup = "Nanal";
                Utils.setSharedPreference(mActivity, GeneralPreferences.KEY_DEFAULT_GROUP, defaultGroup);
                mModel.mDiaryGroupId = mGroupsCursor.getInt(EditDiaryHelper.GROUP_INDEX_ID);
            }
        }
        mModel.mDiaryDay = mLongDay;
        mModel.mTimezone = mTimezone;
        return true;
    }

    /**
     * Fill in the view with the contents of the given event model. This allows
     * an edit view to be initialized before the event has been loaded. Passing
     * in null for the model will display a loading screen. A non-null model
     * will fill in the view's fields with the data contained in the model.
     *
     * @param model The event model to pull the data from
     */
    public void setModel(CalendarDiaryModel model) {
        mModel = model;

        // Need to close the autocomplete adapter to prevent leaking cursors.
        if (mAddressAdapter != null && mAddressAdapter instanceof EmailAddressAdapter) {
            ((EmailAddressAdapter)mAddressAdapter).close();
            mAddressAdapter = null;
        }

        if (model == null) {
            // Display loading screen
            mLoadingMessage.setVisibility(View.VISIBLE);
            mScrollView.setVisibility(View.GONE);
            return;
        }

        long day = model.mDiaryDay;
        mTimezone = model.mTimezone; // this will be UTC for all day events

        if (model.mDiaryTitle != null) {
            mTitleTextView.setTextKeepState(model.mDiaryTitle);
        }

        if (model.mDiaryLocation != null) {
            mLocationTextView.setTextKeepState(model.mDiaryLocation);
        }

        if (model.mDiaryContent != null) {
            mEtContent.setTextKeepState(model.mDiaryContent);
        }

        if (model.mUri != null) {
            // This is an existing event so hide the calendar spinner
            // since we can't change the calendar.
            View calendarGroup = mView.findViewById(R.id.sp_edit_diary_group);
            calendarGroup.setVisibility(View.GONE);
//            TextView tv = (TextView) mView.findViewById(R.id.calendar_textview);
//            tv.setText(model.mCalendarDisplayName);
//            tv = (TextView) mView.findViewById(R.id.calendar_textview_secondary);
        } else {
            View calendarGroup = mView.findViewById(R.id.sp_edit_diary_group);
            calendarGroup.setVisibility(View.GONE);
        }
        if (model.mDiaryColorInitialized) {
            updateHeadlineColor(model, model.getDiaryColor());
        }

        updateView();
        mScrollView.setVisibility(View.VISIBLE);
        mLoadingMessage.setVisibility(View.GONE);
        sendAccessibilityEvent();
    }

    public void updateHeadlineColor(CalendarDiaryModel model, int displayColor) {
        if (model.mUri != null) {
            if (mIsMultipane) {
                mView.findViewById(R.id.calendar_textview_with_colorpicker)
                        .setBackgroundColor(displayColor);
            } else {
                mView.findViewById(R.id.calendar_group).setBackgroundColor(displayColor);
            }
        } else {
            setSpinnerBackgroundColor(displayColor);
        }
    }

    private void setSpinnerBackgroundColor(int displayColor) {
        mCalendarSelectorGroup.setBackgroundColor(displayColor);
    }

    private void sendAccessibilityEvent() {
        AccessibilityManager am =
                (AccessibilityManager) mActivity.getSystemService(Service.ACCESSIBILITY_SERVICE);
        if (!am.isEnabled() || mModel == null) {
            return;
        }
        StringBuilder b = new StringBuilder();
        addFieldsRecursive(b, mView);
        CharSequence msg = b.toString();

        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        event.setClassName(getClass().getName());
        event.setPackageName(mActivity.getPackageName());
        event.getText().add(msg);
        event.setAddedCount(msg.length());

        am.sendAccessibilityEvent(event);
    }

    private void addFieldsRecursive(StringBuilder b, View v) {
        if (v == null || v.getVisibility() != View.VISIBLE) {
            return;
        }
        if (v instanceof TextView) {
            CharSequence tv = ((TextView) v).getText();
            if (!TextUtils.isEmpty(tv.toString().trim())) {
                b.append(tv + PERIOD_SPACE);
            }
        } else if (v instanceof RadioGroup) {
            RadioGroup rg = (RadioGroup) v;
            int id = rg.getCheckedRadioButtonId();
            if (id != View.NO_ID) {
                b.append(((RadioButton) (v.findViewById(id))).getText() + PERIOD_SPACE);
            }
        } else if (v instanceof Spinner) {
            Spinner s = (Spinner) v;
            if (s.getSelectedItem() instanceof String) {
                String str = ((String) (s.getSelectedItem())).trim();
                if (!TextUtils.isEmpty(str)) {
                    b.append(str + PERIOD_SPACE);
                }
            }
        } else if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int children = vg.getChildCount();
            for (int i = 0; i < children; i++) {
                addFieldsRecursive(b, vg.getChildAt(i));
            }
        }
    }
    /**
     * Configures the Calendars spinner.  This is only done for new events, because only new
     * events allow you to select a calendar while editing an event.
     * <p>
     * We tuck a reference to a Cursor with calendar database data into the spinner, so that
     * we can easily extract calendar-specific values when the value changes (the spinner's
     * onItemSelected callback is configured).
     */
    public void setGroupsCursor(Cursor cursor, boolean userVisible, int selectedGroupId) {
        mGroupsCursor = cursor;
        if(cursor == null || cursor.getCount() == 0) {
            if(mSaveAfterQueryComplete) {
                mLoadingCalendarsDialog.cancel();
            }
            if(!userVisible) {
                return;
            }
        }

        int selection;
        if (selectedGroupId != -1) {
            selection = findSelectedGroupPosition(cursor, selectedGroupId);
        } else {
            //todo:기본(로컬) 다이어리 설정을 해 줘야 함...
            selection = findSelectedGroupPosition(cursor, selectedGroupId);
        }

        // populate the calendars spinner
        GroupsAdapter gadapter = new GroupsAdapter(mActivity, R.layout.groups_spinner_item, cursor);
        mGroupSpinner.setAdapter(gadapter);
//        mGroupSpinner.setOnItemClickListener(this);
        mGroupSpinner.setSelection(selection);

        if (mSaveAfterQueryComplete) {
            mLoadingCalendarsDialog.cancel();
            if (prepareForSave() && fillModelFromUI()) {
                int exit = userVisible ? Utils.DONE_EXIT : 0;
                mDone.setDoneCode(Utils.DONE_SAVE | exit);
                mDone.run();
            } else if (userVisible) {
                mDone.setDoneCode(Utils.DONE_EXIT);
                mDone.run();
            } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "SetCalendarsCursor:Save failed and unable to exit view");
            }
            return;
        }
    }

    /**
     * Updates the view based on {@link #mModification} and {@link #mModel}
     */
    public void updateView() {
        if (mModel == null) {
            return;
        }
        setViewStates(mModification);
    }

    private void setViewStates(int mode) {
        // Extra canModify check just in case
        if (mode == Utils.MODIFY_UNINITIALIZED) {
//            setWhenString();

            for (View v : mViewOnlyList) {
                v.setVisibility(View.VISIBLE);
            }
            for (View v : mEditOnlyList) {
                v.setVisibility(View.GONE);
            }
            for (View v : mEditViewList) {
                v.setEnabled(false);
                v.setBackgroundDrawable(null);
            }
            mCalendarSelectorGroup.setVisibility(View.GONE);

            if (TextUtils.isEmpty(mLocationTextView.getText())) {
                mTvLocation.setVisibility(View.GONE);
            }
            if (TextUtils.isEmpty(mEtContent.getText())) {
                mEtContent.setVisibility(View.GONE);
            }
        } else {
            for (View v : mViewOnlyList) {
                v.setVisibility(View.GONE);
            }
            for (View v : mEditOnlyList) {
                v.setVisibility(View.VISIBLE);
            }
            for (View v : mEditViewList) {
                v.setEnabled(true);
                if (v.getTag() != null) {
                    v.setBackgroundDrawable((Drawable) v.getTag());
                    v.setPadding(mOriginalPadding[0], mOriginalPadding[1], mOriginalPadding[2],
                            mOriginalPadding[3]);
                }
            }
            if (mModel.mUri == null) {
                mCalendarSelectorGroup.setVisibility(View.VISIBLE);
            } else {
                mCalendarSelectorGroup.setVisibility(View.GONE);
            }
            mTvLocation.setVisibility(View.VISIBLE);
            mEtContent.setVisibility(View.VISIBLE);
        }
    }

    public void setModification(int modifyWhich) {
        mModification = modifyWhich;
        updateView();
    }

    private int findSelectedGroupPosition(Cursor diariesCursor, int groupId) {
        if(diariesCursor.getCount() <= 0) {
            return -1;
        }
        int diaryIdColumn = diariesCursor.getColumnIndexOrThrow("group_id");
        int position = 0;
        diariesCursor.moveToPosition(-1);
        while (diariesCursor.moveToNext()) {
            if(diariesCursor.getInt(diaryIdColumn) == groupId) {
                return position;
            }
            position++;
        }
        return 0;
    }

    public void setColorPickerButtonStates(int[] colorArray) {
        setColorPickerButtonStates(colorArray != null && colorArray.length > 0);
    }

    public void setColorPickerButtonStates(boolean showColorPalette) {
        if (showColorPalette) {
            mColorPickerNewEvent.setVisibility(View.VISIBLE);
            mColorPickerExistingEvent.setVisibility(View.VISIBLE);
        } else {
            mColorPickerNewEvent.setVisibility(View.INVISIBLE);
            mColorPickerExistingEvent.setVisibility(View.GONE);
        }
    }

    public boolean isColorPaletteVisible() {
        return mColorPickerNewEvent.getVisibility() == View.VISIBLE ||
                mColorPickerExistingEvent.getVisibility() == View.VISIBLE;
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // This is only used for the Calendar spinner in new events, and only fires when the
        // calendar selection changes or on screen rotation
        Cursor c = (Cursor) parent.getItemAtPosition(position);
        if(c == null) {
            Log.w(TAG, "커서에 그룹이 없음");
            return;
        }
        mModel.mGroupName = c.getString(EditDiaryHelper.GROUP_INDEX_NAME);
        setColorPickerButtonStates(mModel.getGroupColors());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public static class GroupsAdapter extends  ResourceCursorAdapter {
        public GroupsAdapter(Context context, int resourceId, Cursor c) {
            super(context, resourceId, c);
            setDropDownViewResource(R.layout.calendars_dropdown_item);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            View colorBar = view.findViewById(R.id.color);
            int nameColumn = cursor.getColumnIndexOrThrow("group_name");
            int colorColumn = cursor.getColumnIndexOrThrow("group_color");
            if(colorBar != null) {
                colorBar.setBackgroundColor(Utils.getDisplayColorFromColor(cursor.getInt(colorColumn)));
            }
            TextView name = (TextView) view.findViewById(R.id.calendar_name);
            if (name != null) {
                String displayName = cursor.getString(nameColumn);
                name.setText(displayName);
            }
        }
    }

    public static class CalendarsAdapter extends ResourceCursorAdapter {
        public CalendarsAdapter(Context context, int resourceId, Cursor c) {
            super(context, resourceId, c);
            setDropDownViewResource(R.layout.calendars_dropdown_item);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            View colorBar = view.findViewById(R.id.color);
            int colorColumn = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR);
            int nameColumn = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
            int ownerColumn = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT);
            if (colorBar != null) {
                colorBar.setBackgroundColor(Utils.getDisplayColorFromColor(cursor
                        .getInt(colorColumn)));
            }

            TextView name = (TextView) view.findViewById(R.id.calendar_name);
            if (name != null) {
                String displayName = cursor.getString(nameColumn);
                name.setText(displayName);

                TextView accountName = (TextView) view.findViewById(R.id.account_name);
                if (accountName != null) {
                    accountName.setText(cursor.getString(ownerColumn));
                    accountName.setVisibility(TextView.VISIBLE);
                }
            }
        }
    }

}