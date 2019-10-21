package com.android.nanal.group;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.CalendarContract;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.nanal.R;
import com.android.nanal.Rfc822InputFilter;
import com.android.nanal.calendar.CalendarGroupModel;
import com.android.nanal.event.Utils;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class EditGroupView implements DialogInterface.OnCancelListener {
    private static final String TAG = "EditGroup";
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
    //    ArrayList<Group>
    ScrollView mScrollView;
    EditText mEtName;
    ImageView mIvColor;
    TextView mTvColor, mLoadingMessage;
    LinearLayout mLlColor;

    View.OnClickListener mChangeColorOnClickListener;

    private int[] mOriginalPadding = new int[4];
    private ProgressDialog mLoadingCalendarsDialog;
    private Activity mActivity;
    private EditGroupHelper.EditDoneRunnable mDone;
    private View mView;
    private CalendarGroupModel mModel;
    private Cursor mGroupsCursor;

    private boolean mSaveAfterQueryComplete = false;
    private int mModification = EditGroupHelper.MODIFY_UNINITIALIZED;
    SharedPreferences prefs;
    String account_id;


    public EditGroupView(Activity activity, View view, EditGroupHelper.EditDoneRunnable done) {
        Context mContext = view.getContext();
        mActivity = activity;
        mView = view;
        mDone = done;

        mScrollView = (ScrollView) view.findViewById(R.id.g_scroll_view);
        mEtName = view.findViewById(R.id.et_edit_group_name);

        mTvColor = view.findViewById(R.id.tv_edit_group_color);
        mIvColor = view.findViewById(R.id.iv_edit_group_color);
        mLoadingMessage = view.findViewById(R.id.loading_message);
        mLlColor = view.findViewById(R.id.ll_edit_group_color);

        prefs = mContext.getSharedPreferences("login_setting", MODE_PRIVATE);
        account_id = prefs.getString("loginId", null);

        setModel(null);
    }


    public boolean prepareForSave() {
        return fillModelFromUI();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == mLoadingCalendarsDialog) {
            mLoadingCalendarsDialog = null;
            mSaveAfterQueryComplete = false;
        }
    }

    private boolean fillModelFromUI() {
        if (mModel == null) {
            return false;
        }
        mModel.group_id = 1;
        mModel.group_name = mEtName.getText().toString();
        mModel.account_id = account_id;
        return true;
    }

    public void setModel(CalendarGroupModel model) {
        mModel = model;

        if (model == null) {
            // Display loading screen
            mScrollView.setVisibility(View.GONE);
            return;
        }
        updateHeadlineColor(model, model.getGroupColor());

        updateView();
        mScrollView.setVisibility(View.VISIBLE);
        mLoadingMessage.setVisibility(View.GONE);
        sendAccessibilityEvent();
    }

    public void updateHeadlineColor(CalendarGroupModel model, int displayColor) {
        if (model.mUri != null) {
            if (mIsMultipane) {
                mView.findViewById(R.id.ll_edit_group_color)
                        .setBackgroundColor(displayColor);
            } else {
                mView.findViewById(R.id.calendar_group).setBackgroundColor(displayColor);
            }
        } else {
            setSpinnerBackgroundColor(displayColor);
        }
    }

    private void setSpinnerBackgroundColor(int displayColor) {
//        mCalendarSelectorGroup.setBackgroundColor(displayColor);
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

        int selection = findSelectedGroupPosition(cursor, selectedGroupId);

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
//            mCalendarSelectorGroup.setVisibility(View.GONE);

//            if (TextUtils.isEmpty(mLocationTextView.getText())) {
//                mTvLocation.setVisibility(View.GONE);
//            }
//            if (TextUtils.isEmpty(mEtContent.getText())) {
//                mEtContent.setVisibility(View.GONE);
//            }
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
//                mCalendarSelectorGroup.setVisibility(View.VISIBLE);
            } else {
//                mCalendarSelectorGroup.setVisibility(View.GONE);
            }
//            mTvLocation.setVisibility(View.VISIBLE);
//            mEtContent.setVisibility(View.VISIBLE);
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


    public boolean isColorPaletteVisible() {
        return mLlColor.getVisibility() == View.VISIBLE;
    }

    public static class GroupsAdapter extends ResourceCursorAdapter {
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
