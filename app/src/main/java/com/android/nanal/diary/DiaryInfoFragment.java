package com.android.nanal.diary;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nanal.DynamicTheme;
import com.android.nanal.NanalDBHelper;
import com.android.nanal.R;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.activity.EditDiaryActivity;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.color.ColorPickerSwatch;
import com.android.nanal.event.DeleteEventHelper;
import com.android.nanal.event.Utils;
import com.android.nanal.query.AsyncQueryService;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import static com.android.nanal.calendar.CalendarController.EVENT_EDIT_ON_LAUNCH;
import static com.android.nanal.event.EventInfoFragment.COLOR_PICKER_DIALOG_TAG;

public class DiaryInfoFragment extends DialogFragment implements RadioGroup.OnCheckedChangeListener,
        CalendarController.EventHandler, View.OnClickListener, DeleteEventHelper.DeleteNotifyListener,
        ColorPickerSwatch.OnColorSelectedListener {

    private CalendarController mController;
    private Activity mActivity;
    private Context mContext;
    private long mDiaryId;
    private View mView;
    private ScrollView mScrollView;
    private TextView mTitle, mWhenDateTime, mContent;

    private DiaryColorPickerDialog mColorPickerDialog;
    private ObjectAnimator mAnimateAlpha;

    private NanalDBHelper helper;
    private Diary mDiary;
    private Menu mMenu = null;

    private QueryHandler mHandler;

    private int[] mColors;
    private int mOriginalColor = -1;
    private boolean mOriginalColorInitialized = false;
    private int mCalendarColor = -1;
    private boolean mCalendarColorInitialized = false;
    private int mCurrentColor = -1;
    private boolean mCurrentColorInitialized = false;
    private String mCurrentColorKey = "";
    private boolean mNoCrossFade = false;  // Used to prevent repeated cross-fade

    private View mHeadlines;

    private boolean mIsPaused = true;
    private boolean mDismissOnResume = false;
    private final Runnable onDeleteRunnable = new Runnable() {
        @Override
        public void run() {
            if (DiaryInfoFragment.this.mIsPaused) {
                mDismissOnResume = true;
                return;
            }
            if (DiaryInfoFragment.this.isVisible()) {
                DiaryInfoFragment.this.dismiss();
            }
        }
    };
    private boolean mDiaryDeletionStarted = false;
    private SparseArray<String> mDisplayColorKeyMap = new SparseArray<String>();


    public DiaryInfoFragment() {

    }

    @SuppressLint("ValidFragment")
    public DiaryInfoFragment(Context context, long diary_id) {
        mContext = context;
        mDiaryId = diary_id;
        helper = AllInOneActivity.helper;

        final Activity activity = getActivity();
        mContext = activity;
//        dynamicTheme.onCreate(activity);
    }

    private final DynamicTheme dynamicTheme = new DynamicTheme();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        mContext = activity;
        dynamicTheme.onCreate(activity);
        mColorPickerDialog = (DiaryColorPickerDialog) activity.getFragmentManager()
                .findFragmentByTag(COLOR_PICKER_DIALOG_TAG);
        if (mColorPickerDialog != null) {
            mColorPickerDialog.setOnColorSelectedListener(this);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mController.deregisterEventHandler(R.layout.diary_info);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        mController = CalendarController.getInstance(mActivity);
        mController.registerEventHandler(R.layout.event_info, this);

        mHandler = new QueryHandler(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.diary_info, container, false);

        Toolbar myToolbar = (Toolbar) mView.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity)getActivity();
        if (myToolbar != null && activity != null) {
            activity.setSupportActionBar(myToolbar);
            activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
            myToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        }

        mScrollView = mView.findViewById(R.id.diary_info_scroll_view);
        mTitle = mView.findViewById(R.id.title);
        mWhenDateTime =  mView.findViewById(R.id.when_datetime);
        mContent =  mView.findViewById(R.id.content);
        mHeadlines = mView.findViewById(R.id.diary_info_headline);

        mDiary = helper.getDiary((int)mDiaryId);

        View b = mView.findViewById(R.id.delete);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo:삭제 구현
//                if (!mCanModifyCalendar) {
//                    return;
//                }
//                mDeleteHelper =
//                        new DeleteEventHelper(mContext, mActivity, !mIsDialog && !mIsTabletConfig /* exitWhenDone */);
//                mDeleteHelper.setDeleteNotificationListener(EventInfoFragment.this);
//                mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
//                mDeleteDialogVisible = true;
//                mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
            }
        });

        b = mView.findViewById(R.id.change_color);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDiary.group_id <= 0) {
                    return;
                }
                showDiaryColorPickerDialog();
            }
        });
        return mView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Show color/edit/delete buttons only in non-dialog configuration
        // 대화상자가 아닌 구성에서만 컬러/편집/삭제 버튼을 표시함
            inflater.inflate(R.menu.diary_info_title_bar, menu);
            mMenu = menu;
            updateMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handles option menu selections:
        // Home button - close event info activity and start the main calendar
        // one
        // Edit button - start the event edit activity and close the info
        // activity
        // Delete button - start a delete query that calls a runnable that close
        // the info activity
        // 홈 버튼 - 이벤트 정보 activity를 닫고 메인 캘린더 시작
        // 편집 버튼 - 이벤트 편집 activity를 시작하고 정보 activity 닫음
        // 삭제 버튼 - 정보 activity를 닫는 runnable을 호출하는 삭제 쿼리를 시작함

        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Utils.returnToCalendarHome(mContext);
            mActivity.finish();
            return true;
        } else if (itemId == R.id.info_action_edit) {
            doEdit();
            mActivity.finish();
        } else if (itemId == R.id.info_action_delete) {
            //todo:삭제 처리
            /*
            mDeleteHelper =
                    new DeleteEventHelper(mActivity, mActivity, true /* exitWhenDone *///);
/*
            mDeleteHelper.setDeleteNotificationListener(DiaryInfoFragment.this);
            mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
            mDeleteDialogVisible = true;
            mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
            */
        } else if (itemId == R.id.info_action_change_color) {
            showDiaryColorPickerDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDiaryColorPickerDialog() {
        if (mColorPickerDialog == null) {
            mColorPickerDialog = DiaryColorPickerDialog.newInstance(mColors, mCurrentColor, false);
            mColorPickerDialog.setOnColorSelectedListener(this);
        }
        final FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.executePendingTransactions();
        if (!mColorPickerDialog.isAdded()) {
            mColorPickerDialog.show(fragmentManager, COLOR_PICKER_DIALOG_TAG);
        }
    }

    private boolean saveEventColor() {
        if (mCurrentColor == mOriginalColor) {
            return false;
        }
        if (mCurrentColor != mCalendarColor) {
            helper.setDiaryColor((int)mDiaryId, mCurrentColor);
        } else {
            helper.setDiaryColor((int)mDiaryId, 0);
        }
        return true;
    }

    @Override
    public void onStop() {
        Activity act = getActivity();
        if (act != null && !act.isChangingConfigurations()) {
            boolean eventColorSaved = saveEventColor();
            if (eventColorSaved) {
                Toast.makeText(getActivity(), R.string.saving_diary, Toast.LENGTH_SHORT).show();
            }
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void doEdit() {
        Context c = getActivity();
        // This ensures that we aren't in the process of closing and have been
        // unattached already
        // 우리가 닫는 중이 아니며, 이미 연결되어 있지 않음을 보장함
        if (c != null) {
//            Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setClass(mActivity, EditDiaryActivity.class);
            intent.putExtra("diary_id", mDiaryId);
//            intent.putExtra(EXTRA_EVENT_BEGIN_TIME, mStartMillis);
//            intent.putExtra(EXTRA_EVENT_END_TIME, mEndMillis);
//            intent.putExtra(EXTRA_EVENT_ALL_DAY, mAllDay);
//            intent.putExtra(EditEventActivity.EXTRA_EVENT_COLOR, mCurrentColor);
//            intent.putExtra(EditEventActivity.EXTRA_EVENT_REMINDERS, EventViewUtils
//                    .reminderItemsToReminders(mReminderViews, mReminderMinuteValues,
//                            mReminderMethodValues));
            intent.putExtra(EVENT_EDIT_ON_LAUNCH, true);
            startActivity(intent);
        }
    }

    private void updateMenu() {
        if (mMenu == null) {
            return;
        }
        MenuItem delete = mMenu.findItem(R.id.info_action_delete);
        MenuItem edit = mMenu.findItem(R.id.info_action_edit);
        MenuItem changeColor = mMenu.findItem(R.id.info_action_change_color);
        if (changeColor != null && mColors != null && mColors.length > 0) {
            changeColor.setVisible(mDiary.group_id > 0);
            changeColor.setEnabled(mDiary.group_id > 0);
        }
    }

    private void setTextCommon(View view, int id, CharSequence text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return;
        textView.setText(text);
    }

    private void setVisibilityCommon(View view, int id, int visibility) {
        View v = view.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
        return;
    }

    @Override
    public void onPause() {
        mIsPaused = true;
        mHandler.removeCallbacks(onDeleteRunnable);
        super.onPause();
        // Remove event deletion alert box since it is being rebuild in the OnResume
        // This is done to get the same behavior on OnResume since the AlertDialog is gone on
        // rotation but not if you press the HOME key
        /*
        if (mDeleteDialogVisible && mDeleteHelper != null) {
            mDeleteHelper.dismissAlertDialog();
            mDeleteHelper = null;
        }*/
    }


    @Override
    public void onResume() {
        super.onResume();
        mIsPaused = false;
        if (mDismissOnResume) {
            mHandler.post(onDeleteRunnable);
        }
        // Display the "delete confirmation" or "edit response helper" dialog if needed
        /*
        if (mDeleteDialogVisible) {
            mDeleteHelper = new DeleteEventHelper(
                    mContext, mActivity,
                    !mIsDialog && !mIsTabletConfig /* exitWhenDone *///);
/*
            mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
            mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
        } else if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
            int buttonId = findButtonIdForResponse(mTentativeUserSetResponse);
            mResponseRadioGroup.check(buttonId);
            mEditResponseHelper.showDialog(mEditResponseHelper.getWhichEvents());
        }
        */
    }

    @Override
    public void eventsChanged() {
    }

    @Override
    public long getSupportedEventTypes() {
        return CalendarController.EventType.EVENTS_CHANGED;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        reloadEvents();
    }

    public void reloadEvents() {
        mDiary = helper.getDiary((int)mDiaryId);
    }

    @Override
    public void onClick(View view) {

        // This must be a click on one of the "remove reminder" buttons
        LinearLayout reminderItem = (LinearLayout) view.getParent();
        LinearLayout parent = (LinearLayout) reminderItem.getParent();
        parent.removeView(reminderItem);
    }

    @Override
    public void onDeleteStarted() {
        mDiaryDeletionStarted = true;
    }

    private Dialog.OnDismissListener createDeleteOnDismissListener() {
        return new Dialog.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // Since OnPause will force the dialog to dismiss , do
                // not change the dialog status
                if (!mIsPaused) {
                    //mDeleteDialogVisible = false;
                }
            }
        };
    }

    public long getDiaryId() {
        return mDiaryId;
    }

/*
    private void setDialogSize(Resources r) {
        mDialogWidth = (int)r.getDimension(R.dimen.event_info_dialog_width);
        mDialogHeight = (int)r.getDimension(R.dimen.event_info_dialog_height);
    }
    */

    @Override
    public void onColorSelected(int color) {
        mCurrentColor = color;
        mCurrentColorKey = mDisplayColorKeyMap.get(color);
        mHeadlines.setBackgroundColor(color);
    }


    private class QueryHandler extends AsyncQueryService {
        public QueryHandler(Context context) {
            super(context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            // if the activity is finishing, then close the cursor and return
            final Activity activity = getActivity();
            if (activity == null || activity.isFinishing()) {
                if (cursor != null) {
                    cursor.close();
                }
                return;
            }
/*
            switch (token) {
                case TOKEN_QUERY_EVENT:
                    mEventCursor = Utils.matrixCursorFromCursor(cursor);
                    if (initEventCursor()) {
                        // The cursor is empty. This can happen if the event was
                        // deleted.
                        // FRAG_TODO we should no longer rely on Activity.finish()
                        activity.finish();
                        return;
                    }
                    if (!mCalendarColorInitialized) {
                        mCalendarColor = Utils.getDisplayColorFromColor(
                                mEventCursor.getInt(EVENT_INDEX_CALENDAR_COLOR));
                        mCalendarColorInitialized = true;
                    }

                    if (!mOriginalColorInitialized) {
                        mOriginalColor = mEventCursor.isNull(EVENT_INDEX_EVENT_COLOR)
                                ? mCalendarColor : Utils.getDisplayColorFromColor(
                                mEventCursor.getInt(EVENT_INDEX_EVENT_COLOR));
                        mOriginalColorInitialized = true;
                    }

                    if (!mCurrentColorInitialized) {
                        mCurrentColor = mOriginalColor;
                        mCurrentColorInitialized = true;
                    }

                    updateEvent(mView);
                    prepareReminders();

                    // start calendar query
                    Uri uri = Calendars.CONTENT_URI;
                    String[] args = new String[]{
                            Long.toString(mEventCursor.getLong(EVENT_INDEX_CALENDAR_ID))};
                    startQuery(TOKEN_QUERY_CALENDARS, null, uri, CALENDARS_PROJECTION,
                            CALENDARS_WHERE, args, null);
                    break;
                case TOKEN_QUERY_CALENDARS:
                    mCalendarsCursor = Utils.matrixCursorFromCursor(cursor);
                    updateCalendar(mView);
                    // FRAG_TODO fragments shouldn't set the title anymore
                    updateTitle();

                    args = new String[]{
                            mCalendarsCursor.getString(CALENDARS_INDEX_ACCOUNT_NAME),
                            mCalendarsCursor.getString(CALENDARS_INDEX_ACCOUNT_TYPE)};
                    uri = Colors.CONTENT_URI;
                    startQuery(TOKEN_QUERY_COLORS, null, uri, COLORS_PROJECTION, COLORS_WHERE, args,
                            null);

                    if (!mIsBusyFreeCalendar) {
                        args = new String[]{Long.toString(mEventId)};

                        // start attendees query
                        uri = Attendees.CONTENT_URI;
                        startQuery(TOKEN_QUERY_ATTENDEES, null, uri, ATTENDEES_PROJECTION,
                                ATTENDEES_WHERE, args, ATTENDEES_SORT_ORDER);
                    } else {
                        sendAccessibilityEventIfQueryDone(TOKEN_QUERY_ATTENDEES);
                    }
                    if (mHasAlarm) {
                        // start reminders query
                        args = new String[]{Long.toString(mEventId)};
                        uri = Reminders.CONTENT_URI;
                        startQuery(TOKEN_QUERY_REMINDERS, null, uri,
                                REMINDERS_PROJECTION, REMINDERS_WHERE, args, null);
                    } else {
                        sendAccessibilityEventIfQueryDone(TOKEN_QUERY_REMINDERS);
                    }
                    break;
                case TOKEN_QUERY_COLORS:
                    ArrayList<Integer> colors = new ArrayList<Integer>();
                    if (cursor.moveToFirst()) {
                        do {
                            String colorKey = cursor.getString(COLORS_INDEX_COLOR_KEY);
                            int rawColor = cursor.getInt(COLORS_INDEX_COLOR);
                            int displayColor = Utils.getDisplayColorFromColor(rawColor);
                            mDisplayColorKeyMap.put(displayColor, colorKey);
                            colors.add(displayColor);
                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                    Integer[] sortedColors = new Integer[colors.size()];
                    Arrays.sort(colors.toArray(sortedColors), new HsvColorComparator());
                    mColors = new int[sortedColors.length];
                    for (int i = 0; i < sortedColors.length; i++) {
                        mColors[i] = sortedColors[i].intValue();

                        float[] hsv = new float[3];
                        Color.colorToHSV(mColors[i], hsv);
                        if (DEBUG) {
                            Log.d("Color", "H:" + hsv[0] + ",S:" + hsv[1] + ",V:" + hsv[2]);
                        }
                    }
                    if (mCanModifyCalendar) {
                        View button = mView.findViewById(R.id.change_color);
                        if (button != null && mColors.length > 0) {
                            button.setEnabled(true);
                            button.setVisibility(View.VISIBLE);
                        }
                    }
                    updateMenu();
                    break;
                case TOKEN_QUERY_ATTENDEES:
                    mAttendeesCursor = Utils.matrixCursorFromCursor(cursor);
                    initAttendeesCursor(mView);
                    updateResponse(mView);
                    break;
                case TOKEN_QUERY_REMINDERS:
                    mRemindersCursor = Utils.matrixCursorFromCursor(cursor);
                    initReminders(mView, mRemindersCursor);
                    break;
                case TOKEN_QUERY_VISIBLE_CALENDARS:
                    if (cursor.getCount() > 1) {
                        // Start duplicate calendars query to detect whether to add the calendar
                        // email to the calendar owner display.
                        String displayName = mCalendarsCursor.getString(CALENDARS_INDEX_DISPLAY_NAME);
                        mHandler.startQuery(TOKEN_QUERY_DUPLICATE_CALENDARS, null,
                                Calendars.CONTENT_URI, CALENDARS_PROJECTION,
                                CALENDARS_DUPLICATE_NAME_WHERE, new String[]{displayName}, null);
                    } else {
                        // Don't need to display the calendar owner when there is only a single
                        // calendar.  Skip the duplicate calendars query.
                        setVisibilityCommon(mView, R.id.calendar_container, View.GONE);
                        mCurrentQuery |= TOKEN_QUERY_DUPLICATE_CALENDARS;
                    }
                    break;
                case TOKEN_QUERY_DUPLICATE_CALENDARS:
                    SpannableStringBuilder sb = new SpannableStringBuilder();

                    // Calendar display name
                    String calendarName = mCalendarsCursor.getString(CALENDARS_INDEX_DISPLAY_NAME);
                    sb.append(calendarName);

                    // Show email account if display name is not unique and
                    // display name != email
                    String email = mCalendarsCursor.getString(CALENDARS_INDEX_OWNER_ACCOUNT);
                    if (cursor.getCount() > 1 && !calendarName.equalsIgnoreCase(email) &&
                            Utils.isValidEmail(email)) {
                        sb.append(" (").append(email).append(")");
                    }

                    setVisibilityCommon(mView, R.id.calendar_container, View.VISIBLE);
                    setTextCommon(mView, R.id.calendar_name, sb);
                    break;
            }
            cursor.close();
            sendAccessibilityEventIfQueryDone(token);

            // All queries are done, show the view.
            if (mCurrentQuery == TOKEN_QUERY_ALL) {
                if (mLoadingMsgView.getAlpha() == 1) {
                    // Loading message is showing, let it stay a bit more (to prevent
                    // flashing) by adding a start delay to the event animation
                    long timeDiff = LOADING_MSG_MIN_DISPLAY_TIME - (System.currentTimeMillis() -
                            mLoadingMsgStartTime);
                    if (timeDiff > 0) {
                        mAnimateAlpha.setStartDelay(timeDiff);
                    }
                }
                if (!mAnimateAlpha.isRunning() && !mAnimateAlpha.isStarted() && !mNoCrossFade) {
                    mAnimateAlpha.start();
                } else {
                    mScrollView.setAlpha(1);
                    mLoadingMsgView.setVisibility(View.GONE);
                }

            }
            */
        }
    }
}
