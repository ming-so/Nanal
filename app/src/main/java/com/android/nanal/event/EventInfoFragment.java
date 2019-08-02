package com.android.nanal.event;
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.QuickContact;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Rfc822Token;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nanal.BuildConfig;
import com.android.nanal.DateException;
import com.android.nanal.Duration;
import com.android.nanal.DynamicTheme;
import com.android.nanal.EditResponseHelper;
import com.android.nanal.R;
import com.android.nanal.activity.EditEventActivity;
import com.android.nanal.alerts.QuickResponseActivity;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarController.EventInfo;
import com.android.nanal.calendar.CalendarController.EventType;
import com.android.nanal.calendar.CalendarEventModel.Attendee;
import com.android.nanal.calendar.CalendarEventModel.ReminderEntry;
import com.android.nanal.calendar.ExpandableTextView;
import com.android.nanal.color.ColorPickerSwatch.OnColorSelectedListener;
import com.android.nanal.color.HsvColorComparator;
import com.android.nanal.icalendar.IcalendarUtils;
import com.android.nanal.icalendar.Organizer;
import com.android.nanal.icalendar.VCalendar;
import com.android.nanal.icalendar.VEvent;
import com.android.nanal.query.AsyncQueryService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static com.android.nanal.calendar.CalendarController.EVENT_EDIT_ON_LAUNCH;

public class EventInfoFragment extends DialogFragment implements OnCheckedChangeListener,
        CalendarController.EventHandler, OnClickListener, DeleteEventHelper.DeleteNotifyListener,
        OnColorSelectedListener {

    public static final boolean DEBUG = false;

    public static final String TAG = "EventInfoFragment";
    public static final String COLOR_PICKER_DIALOG_TAG = "EventColorPickerDialog";
    // Style of view
    public static final int FULL_WINDOW_STYLE = 0;
    public static final int DIALOG_WINDOW_STYLE = 1;
    public static final int COLORS_INDEX_COLOR = 1;
    public static final int COLORS_INDEX_COLOR_KEY = 2;
    public static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    public static final String BUNDLE_KEY_START_MILLIS = "key_start_millis";
    public static final String BUNDLE_KEY_END_MILLIS = "key_end_millis";
    public static final String BUNDLE_KEY_IS_DIALOG = "key_fragment_is_dialog";
    protected static final String BUNDLE_KEY_DELETE_DIALOG_VISIBLE = "key_delete_dialog_visible";
    protected static final String BUNDLE_KEY_WINDOW_STYLE = "key_window_style";
    protected static final String BUNDLE_KEY_CALENDAR_COLOR = "key_calendar_color";
    protected static final String BUNDLE_KEY_CALENDAR_COLOR_INIT = "key_calendar_color_init";
    protected static final String BUNDLE_KEY_CURRENT_COLOR = "key_current_color";
    protected static final String BUNDLE_KEY_CURRENT_COLOR_KEY = "key_current_color_key";
    protected static final String BUNDLE_KEY_CURRENT_COLOR_INIT = "key_current_color_init";
    protected static final String BUNDLE_KEY_ORIGINAL_COLOR = "key_original_color";
    protected static final String BUNDLE_KEY_ORIGINAL_COLOR_INIT = "key_original_color_init";
    public static final String BUNDLE_KEY_ATTENDEE_RESPONSE = "key_attendee_response";
    protected static final String BUNDLE_KEY_USER_SET_ATTENDEE_RESPONSE =
            "key_user_set_attendee_response";
    protected static final String BUNDLE_KEY_TENTATIVE_USER_RESPONSE =
            "key_tentative_user_response";
    protected static final String BUNDLE_KEY_RESPONSE_WHICH_EVENTS = "key_response_which_events";
    protected static final String BUNDLE_KEY_REMINDER_MINUTES = "key_reminder_minutes";
    protected static final String BUNDLE_KEY_REMINDER_METHODS = "key_reminder_methods";
    /**
     * These are the corresponding indices into the array of strings
     * "R.array.change_response_labels" in the resource file.
     * 리소스 파일의 string 배열 "R.array.change_response_labels"에 해당하는 indices
     */
    static final int UPDATE_SINGLE = 0;
    static final int UPDATE_ALL = 1;
    static final String[] CALENDARS_PROJECTION = new String[]{
            Calendars._ID,           // 0
            Calendars.CALENDAR_DISPLAY_NAME,  // 1
            Calendars.OWNER_ACCOUNT, // 2
            Calendars.CAN_ORGANIZER_RESPOND, // 3
            Calendars.ACCOUNT_NAME, // 4
            Calendars.ACCOUNT_TYPE  // 5
    };
    static final int CALENDARS_INDEX_DISPLAY_NAME = 1;
    static final int CALENDARS_INDEX_OWNER_ACCOUNT = 2;
    static final int CALENDARS_INDEX_OWNER_CAN_RESPOND = 3;
    static final int CALENDARS_INDEX_ACCOUNT_NAME = 4;
    static final int CALENDARS_INDEX_ACCOUNT_TYPE = 5;
    static final String CALENDARS_WHERE = Calendars._ID + "=?";
    static final String CALENDARS_DUPLICATE_NAME_WHERE = Calendars.CALENDAR_DISPLAY_NAME + "=?";
    static final String CALENDARS_VISIBLE_WHERE = Calendars.VISIBLE + "=?";
    static final String[] COLORS_PROJECTION = new String[]{
            Colors._ID, // 0
            Colors.COLOR, // 1
            Colors.COLOR_KEY // 2
    };
    static final String COLORS_WHERE = Colors.ACCOUNT_NAME + "=? AND " + Colors.ACCOUNT_TYPE +
            "=? AND " + Colors.COLOR_TYPE + "=" + Colors.TYPE_EVENT;
    private static final int REQUEST_CODE_COLOR_PICKER = 0;
    private static final String PERIOD_SPACE = ". ";
    private static final String NO_EVENT_COLOR = "";
    // Query tokens for QueryHandler
    // QueryHandler를 위한 Query tokens
    private static final int TOKEN_QUERY_EVENT = 1;
    private static final int TOKEN_QUERY_CALENDARS = 1 << 1;
    private static final int TOKEN_QUERY_ATTENDEES = 1 << 2;
    private static final int TOKEN_QUERY_DUPLICATE_CALENDARS = 1 << 3;
    private static final int TOKEN_QUERY_REMINDERS = 1 << 4;
    private static final int TOKEN_QUERY_VISIBLE_CALENDARS = 1 << 5;
    private static final int TOKEN_QUERY_COLORS = 1 << 6;
    private static final int TOKEN_QUERY_ALL = TOKEN_QUERY_DUPLICATE_CALENDARS
            | TOKEN_QUERY_ATTENDEES | TOKEN_QUERY_CALENDARS | TOKEN_QUERY_EVENT
            | TOKEN_QUERY_REMINDERS | TOKEN_QUERY_VISIBLE_CALENDARS | TOKEN_QUERY_COLORS;

    public static final File EXPORT_SDCARD_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(), "CalendarEvents");

    private enum ShareType {
        SDCARD,
        INTENT
    }

    private static final String[] EVENT_PROJECTION = new String[] {
            Events._ID,                  // 0  do not remove; used in DeleteEventHelper
            Events.TITLE,                // 1  do not remove; used in DeleteEventHelper
            Events.RRULE,                // 2  do not remove; used in DeleteEventHelper
            Events.ALL_DAY,              // 3  do not remove; used in DeleteEventHelper
            Events.CALENDAR_ID,          // 4  do not remove; used in DeleteEventHelper
            Events.DTSTART,              // 5  do not remove; used in DeleteEventHelper
            Events._SYNC_ID,             // 6  do not remove; used in DeleteEventHelper
            Events.EVENT_TIMEZONE,       // 7  do not remove; used in DeleteEventHelper
            Events.DESCRIPTION,          // 8
            Events.EVENT_LOCATION,       // 9
            Calendars.CALENDAR_ACCESS_LEVEL, // 10
            Events.CALENDAR_COLOR,       // 11
            Events.EVENT_COLOR,          // 12
            Events.HAS_ATTENDEE_DATA,    // 13
            Events.ORGANIZER,            // 14
            Events.HAS_ALARM,            // 15
            Calendars.MAX_REMINDERS,     // 16
            Calendars.ALLOWED_REMINDERS, // 17
            Events.CUSTOM_APP_PACKAGE,   // 18
            Events.CUSTOM_APP_URI,       // 19
            Events.DTEND,                // 20
            Events.DURATION,             // 21
            Events.ORIGINAL_SYNC_ID      // 22 do not remove; used in DeleteEventHelper
    };
    private static final int EVENT_INDEX_ID = 0;
    private static final int EVENT_INDEX_TITLE = 1;
    private static final int EVENT_INDEX_RRULE = 2;
    private static final int EVENT_INDEX_ALL_DAY = 3;
    private static final int EVENT_INDEX_CALENDAR_ID = 4;
    private static final int EVENT_INDEX_DTSTART = 5;
    private static final int EVENT_INDEX_SYNC_ID = 6;
    private static final int EVENT_INDEX_EVENT_TIMEZONE = 7;
    private static final int EVENT_INDEX_DESCRIPTION = 8;
    private static final int EVENT_INDEX_EVENT_LOCATION = 9;
    private static final int EVENT_INDEX_ACCESS_LEVEL = 10;
    private static final int EVENT_INDEX_CALENDAR_COLOR = 11;
    private static final int EVENT_INDEX_EVENT_COLOR = 12;
    private static final int EVENT_INDEX_HAS_ATTENDEE_DATA = 13;
    private static final int EVENT_INDEX_ORGANIZER = 14;
    private static final int EVENT_INDEX_HAS_ALARM = 15;
    private static final int EVENT_INDEX_MAX_REMINDERS = 16;
    private static final int EVENT_INDEX_ALLOWED_REMINDERS = 17;
    private static final int EVENT_INDEX_CUSTOM_APP_PACKAGE = 18;
    private static final int EVENT_INDEX_CUSTOM_APP_URI = 19;
    private static final int EVENT_INDEX_DTEND = 20;
    private static final int EVENT_INDEX_DURATION = 21;
    private static final String[] ATTENDEES_PROJECTION = new String[] {
            Attendees._ID,                      // 0
            Attendees.ATTENDEE_NAME,            // 1
            Attendees.ATTENDEE_EMAIL,           // 2
            Attendees.ATTENDEE_RELATIONSHIP,    // 3
            Attendees.ATTENDEE_STATUS,          // 4
            Attendees.ATTENDEE_IDENTITY,        // 5
            Attendees.ATTENDEE_ID_NAMESPACE     // 6
    };
    private static final int ATTENDEES_INDEX_ID = 0;
    private static final int ATTENDEES_INDEX_NAME = 1;
    private static final int ATTENDEES_INDEX_EMAIL = 2;
    private static final int ATTENDEES_INDEX_RELATIONSHIP = 3;
    private static final int ATTENDEES_INDEX_STATUS = 4;
    private static final int ATTENDEES_INDEX_IDENTITY = 5;
    private static final int ATTENDEES_INDEX_ID_NAMESPACE = 6;
    private static final String ATTENDEES_WHERE = Attendees.EVENT_ID + "=?";
    private static final String ATTENDEES_SORT_ORDER = Attendees.ATTENDEE_NAME + " ASC, "
            + Attendees.ATTENDEE_EMAIL + " ASC";
    private static final String[] REMINDERS_PROJECTION = new String[] {
            Reminders._ID,                      // 0
            Reminders.MINUTES,            // 1
            Reminders.METHOD           // 2
    };
    private static final int REMINDERS_INDEX_ID = 0;
    private static final int REMINDERS_MINUTES_ID = 1;
    private static final int REMINDERS_METHOD_ID = 2;
    private static final String REMINDERS_WHERE = Reminders.EVENT_ID + "=?";
    private static final int FADE_IN_TIME = 300;   // in milliseconds
    private static final int LOADING_MSG_DELAY = 600;   // in milliseconds
    private static final int LOADING_MSG_MIN_DISPLAY_TIME = 600;
    private static float mScale = 0; // Used for supporting different screen densities
    // 다양한 화면 밀도를 지원하는 데 사용함
    private static int mCustomAppIconSize = 32;
    private static int mDialogWidth = 500;
    private static int mDialogHeight = 600;
    private static int DIALOG_TOP_MARGIN = 8;


    private final ArrayList<LinearLayout> mReminderViews = new ArrayList<LinearLayout>(0);
    public ArrayList<ReminderEntry> mReminders;
    public ArrayList<ReminderEntry> mOriginalReminders = new ArrayList<ReminderEntry>();
    public ArrayList<ReminderEntry> mUnsupportedReminders = new ArrayList<ReminderEntry>();
    ArrayList<Attendee> mAcceptedAttendees = new ArrayList<Attendee>();
    ArrayList<Attendee> mDeclinedAttendees = new ArrayList<Attendee>();
    ArrayList<Attendee> mTentativeAttendees = new ArrayList<Attendee>();
    ArrayList<Attendee> mNoResponseAttendees = new ArrayList<Attendee>();
    ArrayList<String> mToEmails = new ArrayList<String>();
    ArrayList<String> mCcEmails = new ArrayList<String>();
    private int mWindowStyle = DIALOG_WINDOW_STYLE;
    private int mCurrentQuery = 0;
    private View mView;
    private Uri mUri;
    private long mEventId;
    private Cursor mEventCursor;
    private Cursor mAttendeesCursor;
    private Cursor mCalendarsCursor;
    private Cursor mRemindersCursor;
    private long mStartMillis;
    private long mEndMillis;
    private boolean mAllDay;
    private boolean mHasAttendeeData;
    private String mEventOrganizerEmail;
    private String mEventOrganizerDisplayName = "";
    private boolean mIsOrganizer;
    private long mCalendarOwnerAttendeeId = EditEventHelper.ATTENDEE_ID_NONE;
    private boolean mOwnerCanRespond;
    private String mSyncAccountName;
    private String mCalendarOwnerAccount;
    private boolean mCanModifyCalendar;
    private boolean mCanModifyEvent;
    private boolean mIsBusyFreeCalendar;
    private int mNumOfAttendees;
    private EditResponseHelper mEditResponseHelper;
    private boolean mDeleteDialogVisible = false;
    private DeleteEventHelper mDeleteHelper;
    private int mOriginalAttendeeResponse;
    private int mAttendeeResponseFromIntent = Attendees.ATTENDEE_STATUS_NONE;
    private int mUserSetResponse = Attendees.ATTENDEE_STATUS_NONE;
    private int mWhichEvents = -1;
    // Used as the temporary response until the dialog is confirmed. It is also
    // able to be used as a state marker for configuration changes.
    // 대화상자가 확인될 때까지 임시 응답으로 사용됨, 또한 구성 변경을 위한 상태 표시state marker로도 사용할 수 있음
    private int mTentativeUserSetResponse = Attendees.ATTENDEE_STATUS_NONE;
    private boolean mIsRepeating;
    private boolean mHasAlarm;
    private int mMaxReminders;
    private String mCalendarAllowedReminders;
    // Used to prevent saving changes in event if it is being deleted.
    // 삭제 중인 경우, 변경 사항을 저장하지 않도록 하기 위해 사용됨
    private boolean mEventDeletionStarted = false;
    private TextView mTitle;
    private TextView mWhenDateTime;
    private TextView mWhere;
    private ExpandableTextView mDesc;
    private AttendeesView mLongAttendees;
    private Button emailAttendeesButton;
    private Menu mMenu = null;
    private View mHeadlines;
    private ScrollView mScrollView;
    private View mLoadingMsgView;
    private ObjectAnimator mAnimateAlpha;
    private long mLoadingMsgStartTime;
    private final Runnable mLoadingMsgAlphaUpdater = new Runnable() {
        @Override
        public void run() {
            // Since this is run after a delay, make sure to only show the message
            // if the event's data is not shown yet.
            // 지연된 후에 실행되기 때문에, 이벤트 데이터가 아직 표시되지 않은 경우에만 메시지를 표시함
            if (!mAnimateAlpha.isRunning() && mScrollView.getAlpha() == 0) {
                mLoadingMsgStartTime = System.currentTimeMillis();
                mLoadingMsgView.setAlpha(1);
            }
        }
    };
    private EventColorPickerDialog mColorPickerDialog;
    private SparseArray<String> mDisplayColorKeyMap = new SparseArray<String>();
    private int[] mColors;
    private int mOriginalColor = -1;
    private boolean mOriginalColorInitialized = false;
    private int mCalendarColor = -1;
    private boolean mCalendarColorInitialized = false;
    private int mCurrentColor = -1;
    private boolean mCurrentColorInitialized = false;
    private String mCurrentColorKey = NO_EVENT_COLOR;
    private boolean mNoCrossFade = false;  // Used to prevent repeated cross-fade
    private RadioGroup mResponseRadioGroup;
    private int mDefaultReminderMinutes;
    private boolean mUserModifiedReminders = false;
    /**
     * Contents of the "minutes" spinner.  This has default values from the XML file, augmented
     * with any additional values that were already associated with the event.
     * "분" 스피너의 내용물, 이 값은 이벤트에 이미 연결되어 있는 추가적인 값을 가진 XML 파일로부터? 기본값을 가져옴
     */
    private ArrayList<Integer> mReminderMinuteValues;
    private ArrayList<String> mReminderMinuteLabels;
    /**
     * Contents of the "methods" spinner.  The "values" list specifies the method constant
     * (e.g. {@link Reminders#METHOD_ALERT}) associated with the labels.  Any methods that
     * aren't allowed by the Calendar will be removed.
     * "메소드" 스피너의 내용물, "values" 리스트는 label과 관련된 메소드 상수(예 {})를 지정함
     * 캘린더에서 허용하지 않는 모든 메소드는 제거됨
     */
    private ArrayList<Integer> mReminderMethodValues;
    private ArrayList<String> mReminderMethodLabels;
    private QueryHandler mHandler;
    private OnItemSelectedListener mReminderChangeListener;
    private boolean mIsDialog = false;
    private boolean mIsPaused = true;
    private boolean mDismissOnResume = false;
    private final Runnable onDeleteRunnable = new Runnable() {
        @Override
        public void run() {
            if (EventInfoFragment.this.mIsPaused) {
                mDismissOnResume = true;
                return;
            }
            if (EventInfoFragment.this.isVisible()) {
                EventInfoFragment.this.dismiss();
            }
        }
    };
    private int mX = -1;
    private int mY = -1;
    private int mMinTop;         // Dialog cannot be above this location, 대화상자는 이 위치에 있을 수 없음
    private boolean mIsTabletConfig;
    private Activity mActivity;
    private Context mContext;
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            updateEvent(mView);
        }
    };
    private CalendarController mController;

    @SuppressLint("ValidFragment")
    public EventInfoFragment(Context context, Uri uri, long startMillis, long endMillis,
                             int attendeeResponse, boolean isDialog, int windowStyle,
                             ArrayList<ReminderEntry> reminders) {

        Resources r = context.getResources();
        if (mScale == 0) {
            mScale = context.getResources().getDisplayMetrics().density;
            if (mScale != 1) {
                mCustomAppIconSize *= mScale;
                if (isDialog) {
                    DIALOG_TOP_MARGIN *= mScale;
                }
            }
        }
        if (isDialog) {
            setDialogSize(r);
        }
        mIsDialog = isDialog;

        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        mUri = uri;
        mStartMillis = startMillis;
        mEndMillis = endMillis;
        mAttendeeResponseFromIntent = attendeeResponse;
        mWindowStyle = windowStyle;

        // Pass in null if no reminders are being specified.
        // This may be used to explicitly show certain reminders already known
        // about, such as during configuration changes.
        // 리마인더를 지정하지 않을 경우, null 전달하기
        // 이는 구성 변경 중과 같이 이미 알려진 특정 알림을 명시적으로 보여 주는 데 사용될 수 있음
        mReminders = reminders;
    }

    // This is currently required by the fragment manager.
    // 현재 fragment manager가 요구하고 있음
    public EventInfoFragment() {
    }

    @SuppressLint("ValidFragment")
    public EventInfoFragment(Context context, long eventId, long startMillis, long endMillis,
                             int attendeeResponse, boolean isDialog, int windowStyle,
                             ArrayList<ReminderEntry> reminders) {
        this(context, ContentUris.withAppendedId(Events.CONTENT_URI, eventId), startMillis,
                endMillis, attendeeResponse, isDialog, windowStyle, reminders);
        mEventId = eventId;
    }

    public static int getResponseFromButtonId(int buttonId) {
        int response;
        if (buttonId == R.id.response_yes) {
            response = Attendees.ATTENDEE_STATUS_ACCEPTED;
        } else if (buttonId == R.id.response_maybe) {
            response = Attendees.ATTENDEE_STATUS_TENTATIVE;
        } else if (buttonId == R.id.response_no) {
            response = Attendees.ATTENDEE_STATUS_DECLINED;
        } else {
            response = Attendees.ATTENDEE_STATUS_NONE;
        }
        return response;
    }

    public static int findButtonIdForResponse(int response) {
        int buttonId;
        switch (response) {
            case Attendees.ATTENDEE_STATUS_ACCEPTED:
                buttonId = R.id.response_yes;
                break;
            case Attendees.ATTENDEE_STATUS_TENTATIVE:
                buttonId = R.id.response_maybe;
                break;
            case Attendees.ATTENDEE_STATUS_DECLINED:
                buttonId = R.id.response_no;
                break;
            default:
                buttonId = -1;
        }
        return buttonId;
    }

    /**
     * Loads an integer array asset into a list.
     * integer 배열 asset을 리스트에 로드함
     */
    private static ArrayList<Integer> loadIntegerArray(Resources r, int resNum) {
        int[] vals = r.getIntArray(resNum);
        int size = vals.length;
        ArrayList<Integer> list = new ArrayList<Integer>(size);

        for (int i = 0; i < size; i++) {
            list.add(vals[i]);
        }

        return list;
    }

    /**
     * Loads a String array asset into a list.
     * String 배열 asset을 리스트에 로드함
     */
    private static ArrayList<String> loadStringArray(Resources r, int resNum) {
        String[] labels = r.getStringArray(resNum);
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(labels));
        return list;
    }

    private void sendAccessibilityEventIfQueryDone(int token) {
        mCurrentQuery |= token;
        if (mCurrentQuery == TOKEN_QUERY_ALL) {
            sendAccessibilityEvent();
        }
    }

    private final DynamicTheme dynamicTheme = new DynamicTheme();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mReminderChangeListener = new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Integer prevValue = (Integer) parent.getTag();
                if (prevValue == null || prevValue != position) {
                    parent.setTag(position);
                    mUserModifiedReminders = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }

        };

        if (savedInstanceState != null) {
            mIsDialog = savedInstanceState.getBoolean(BUNDLE_KEY_IS_DIALOG, false);
            mWindowStyle = savedInstanceState.getInt(BUNDLE_KEY_WINDOW_STYLE,
                    DIALOG_WINDOW_STYLE);
        }

        if (mIsDialog) {
            applyDialogParams();
        }

        final Activity activity = getActivity();
        mContext = activity;
        dynamicTheme.onCreate(activity);
        mColorPickerDialog = (EventColorPickerDialog) activity.getFragmentManager()
                .findFragmentByTag(COLOR_PICKER_DIALOG_TAG);
        if (mColorPickerDialog != null) {
            mColorPickerDialog.setOnColorSelectedListener(this);
        }
    }

    private void applyDialogParams() {
        Dialog dialog = getDialog();
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams a = window.getAttributes();
        a.dimAmount = .4f;

        a.width = mDialogWidth;
        a.height = mDialogHeight;


        // On tablets , do smart positioning of dialog
        // On phones , use the whole screen
        // 태블릿에서 대화상자의 스마트 위치 지정
        // 휴대폰에서 전체 화면 사용

        if (mX != -1 || mY != -1) {
            a.x = mX - mDialogWidth / 2;
            a.y = mY - mDialogHeight / 2;
            if (a.y < mMinTop) {
                a.y = mMinTop + DIALOG_TOP_MARGIN;
            }
            a.gravity = Gravity.LEFT | Gravity.TOP;
        }
        window.setAttributes(a);
    }

    public void setDialogParams(int x, int y, int minTop) {
        mX = x;
        mY = y;
        mMinTop = minTop;
    }

    // Implements OnCheckedChangeListener
    // OnCheckedChangeListener 구현
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // If we haven't finished the return from the dialog yet, don't display.
        // 대화상자에서 아직 반환을 완료하지 않았으면 표시하지 않음
        if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
            return;
        }

        // If this is not a repeating event, then don't display the dialog
        // asking which events to change.
        // 반복 이벤트가 아니라면, 변경할 이벤트를 묻는 대화상자를 표시하지 않음
        int response = getResponseFromButtonId(checkedId);
        if (!mIsRepeating) {
            mUserSetResponse = response;
            return;
        }

        // If the selection is the same as the original, then don't display the
        // dialog asking which events to change.
        // 선택 항목이 원본과 같다면 변경할 이벤트를 묻는 대화상자를 표시하지 않음
        if (checkedId == findButtonIdForResponse(mOriginalAttendeeResponse)) {
            mUserSetResponse = response;
            return;
        }

        // This is a repeating event. We need to ask the user if they mean to
        // change just this one instance or all instances.
        // 반복되는 이벤트임, 사용자에게 하나의 인스턴스만 바꾸려는 것인지 아니면 모든 인스턴스를
        // 바꾸려는 것인지 물어볼 필요가 있음
        mTentativeUserSetResponse = response;
        mEditResponseHelper.showDialog(mWhichEvents);
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mController.deregisterEventHandler(R.layout.event_info);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        // Ensure that mIsTabletConfig is set before creating the menu.
        // 메뉴를 만들기 전에 mIsTabletConfig가 설정되어 있는지 확인함
        mIsTabletConfig = Utils.getConfigBool(mActivity, R.bool.tablet_config);
        mController = CalendarController.getInstance(mActivity);
        mController.registerEventHandler(R.layout.event_info, this);
        mEditResponseHelper = new EditResponseHelper(activity);
        mEditResponseHelper.setDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // If the user dismisses the dialog (without hitting OK),
                        // then we want to revert the selection that opened the dialog.
                        // 사용자가 대화상자를 닫은 경우(OK 누르지 않고), 대화상자를 열었던 선택항목을 되돌림
                        if (mEditResponseHelper.getWhichEvents() != -1) {
                            mUserSetResponse = mTentativeUserSetResponse;
                            mWhichEvents = mEditResponseHelper.getWhichEvents();
                        } else {
                            // Revert the attending response radio selection to whatever
                            // was selected prior to this selection (possibly nothing).
                            // 이 선택 전에 선택된 모든 항목으로,,, 참석 응답 radio 선택...을 되돌림 (아무것도 없을 수 있음)
                            int oldResponse;
                            if (mUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
                                oldResponse = mUserSetResponse;
                            } else {
                                oldResponse = mOriginalAttendeeResponse;
                            }
                            int buttonToCheck = findButtonIdForResponse(oldResponse);

                            if (mResponseRadioGroup != null) {
                                mResponseRadioGroup.check(buttonToCheck);
                            }

                            // If the radio group is being cleared, also clear the
                            // dialog's selection of which events should be included
                            // in this response.
                            // 라디오 그룹을 삭제하는 경우, 이 응답response에 포함할 이벤트를
                            // 대화상자에서 선택 해제함
                            if (buttonToCheck == -1) {
                                mEditResponseHelper.setWhichEvents(-1);
                            }
                        }

                        // Since OnPause will force the dialog to dismiss, do
                        // not change the dialog status
                        // OnPause는 대화상자를 강제로 해제하므로 대화상자 상태를 변경하지 말기
                        if (!mIsPaused) {
                            mTentativeUserSetResponse = Attendees.ATTENDEE_STATUS_NONE;
                        }
                    }
                });

        if (mAttendeeResponseFromIntent != Attendees.ATTENDEE_STATUS_NONE) {
            mEditResponseHelper.setWhichEvents(UPDATE_ALL);
            mWhichEvents = mEditResponseHelper.getWhichEvents();
        }
        mHandler = new QueryHandler(activity);
        if (!mIsDialog) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            mIsDialog = savedInstanceState.getBoolean(BUNDLE_KEY_IS_DIALOG, false);
            mWindowStyle = savedInstanceState.getInt(BUNDLE_KEY_WINDOW_STYLE,
                    DIALOG_WINDOW_STYLE);
            mDeleteDialogVisible =
                    savedInstanceState.getBoolean(BUNDLE_KEY_DELETE_DIALOG_VISIBLE,false);
            mCalendarColor = savedInstanceState.getInt(BUNDLE_KEY_CALENDAR_COLOR);
            mCalendarColorInitialized =
                    savedInstanceState.getBoolean(BUNDLE_KEY_CALENDAR_COLOR_INIT);
            mOriginalColor = savedInstanceState.getInt(BUNDLE_KEY_ORIGINAL_COLOR);
            mOriginalColorInitialized = savedInstanceState.getBoolean(
                    BUNDLE_KEY_ORIGINAL_COLOR_INIT);
            mCurrentColor = savedInstanceState.getInt(BUNDLE_KEY_CURRENT_COLOR);
            mCurrentColorInitialized = savedInstanceState.getBoolean(
                    BUNDLE_KEY_CURRENT_COLOR_INIT);
            mCurrentColorKey = savedInstanceState.getString(BUNDLE_KEY_CURRENT_COLOR_KEY);

            mTentativeUserSetResponse = savedInstanceState.getInt(
                    BUNDLE_KEY_TENTATIVE_USER_RESPONSE,
                    Attendees.ATTENDEE_STATUS_NONE);
            if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE &&
                    mEditResponseHelper != null) {
                // If the edit response helper dialog is open, we'll need to
                // know if either of the choices were selected.
                // 응답 헬퍼,, 편집 대화상자가 열려 있는 경우, 선택항목 중 하나가 선택되었는지를 알아야 함
                mEditResponseHelper.setWhichEvents(savedInstanceState.getInt(
                        BUNDLE_KEY_RESPONSE_WHICH_EVENTS, -1));
            }
            mUserSetResponse = savedInstanceState.getInt(
                    BUNDLE_KEY_USER_SET_ATTENDEE_RESPONSE,
                    Attendees.ATTENDEE_STATUS_NONE);
            if (mUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
                // If the response was set by the user before a configuration
                // change, we'll need to know which choice was selected.
                // 구성이 변경되기 전에 사용자가 응답을 설정한 경우,
                // 어떤 선택항목이 선택되었는지 알아야 함
                mWhichEvents = savedInstanceState.getInt(
                        BUNDLE_KEY_RESPONSE_WHICH_EVENTS, -1);
            }

            mReminders = Utils.readRemindersFromBundle(savedInstanceState);
        }

        if (mWindowStyle == DIALOG_WINDOW_STYLE) {
            mView = inflater.inflate(R.layout.event_info_dialog, container, false);
        } else {
            mView = inflater.inflate(R.layout.event_info, container, false);
        }

        Toolbar myToolbar = (Toolbar) mView.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity)getActivity();
        if (myToolbar != null && activity != null) {
            activity.setSupportActionBar(myToolbar);
            activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
            myToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        }

        mScrollView = (ScrollView) mView.findViewById(R.id.event_info_scroll_view);
        mLoadingMsgView = mView.findViewById(R.id.event_info_loading_msg);
        mTitle = (TextView) mView.findViewById(R.id.title);
        mWhenDateTime = (TextView) mView.findViewById(R.id.when_datetime);
        mWhere = (TextView) mView.findViewById(R.id.where);
        mDesc = (ExpandableTextView) mView.findViewById(R.id.description);
        mHeadlines = mView.findViewById(R.id.event_info_headline);
        mLongAttendees = (AttendeesView) mView.findViewById(R.id.long_attendee_list);

        mResponseRadioGroup = (RadioGroup) mView.findViewById(R.id.response_value);

        if (mUri == null) {
            // restore event ID from bundle
            // 번들로부터 이벤트 ID 복원
            mEventId = savedInstanceState.getLong(BUNDLE_KEY_EVENT_ID);
            mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
            mStartMillis = savedInstanceState.getLong(BUNDLE_KEY_START_MILLIS);
            mEndMillis = savedInstanceState.getLong(BUNDLE_KEY_END_MILLIS);
        }

        mAnimateAlpha = ObjectAnimator.ofFloat(mScrollView, "Alpha", 0, 1);
        mAnimateAlpha.setDuration(FADE_IN_TIME);
        mAnimateAlpha.addListener(new AnimatorListenerAdapter() {
            int defLayerType;

            @Override
            public void onAnimationStart(Animator animation) {
                // Use hardware layer for better performance during animation
                // 애니메이션 도중에 성능 향상을 위해서 하드웨어 계층 사용
                defLayerType = mScrollView.getLayerType();
                mScrollView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                // Ensure that the loading message is gone before showing the
                // event info
                // 이벤트 정보를 표시하기 전, 로딩 메시지가 사라졌는지 확인함
                mLoadingMsgView.removeCallbacks(mLoadingMsgAlphaUpdater);
                mLoadingMsgView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mScrollView.setLayerType(defLayerType, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mScrollView.setLayerType(defLayerType, null);
                // Do not cross fade after the first time
                mNoCrossFade = true;
            }
        });

        mLoadingMsgView.setAlpha(0);
        mScrollView.setAlpha(0);
        mLoadingMsgView.postDelayed(mLoadingMsgAlphaUpdater, LOADING_MSG_DELAY);

        // start loading the data
        // 데이터 로딩 시작

        mHandler.startQuery(TOKEN_QUERY_EVENT, null, mUri, EVENT_PROJECTION,
                null, null, null);

        View b = mView.findViewById(R.id.delete);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mCanModifyCalendar) {
                    return;
                }
                mDeleteHelper =
                        new DeleteEventHelper(mContext, mActivity, !mIsDialog && !mIsTabletConfig /* exitWhenDone */);
                mDeleteHelper.setDeleteNotificationListener(EventInfoFragment.this);
                mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
                mDeleteDialogVisible = true;
                mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
            }
        });

        b = mView.findViewById(R.id.change_color);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mCanModifyCalendar) {
                    return;
                }
                showEventColorPickerDialog();
            }
        });

        // Hide Edit/Delete buttons if in full screen mode on a phone
        // 휴대폰이 전체 화면 모드인 경우, 편집/삭제 버튼 숨기기
        if (!mIsDialog && !mIsTabletConfig || mWindowStyle == EventInfoFragment.FULL_WINDOW_STYLE) {
            mView.findViewById(R.id.event_info_buttons_container).setVisibility(View.GONE);
        }

        // Create a listener for the email guests button
        // 이메일 게스트 버튼을 위한 listener 만들기
        emailAttendeesButton = (Button) mView.findViewById(R.id.email_attendees_button);
        if (emailAttendeesButton != null) {
            emailAttendeesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    emailAttendees();
                }
            });
        }

        // Create a listener for the add reminder button
        // 리마인더 추가 버튼을 위한 listener 만들기
        View reminderAddButton = mView.findViewById(R.id.reminder_add);
        View.OnClickListener addReminderOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addReminder();
                mUserModifiedReminders = true;
            }
        };
        reminderAddButton.setOnClickListener(addReminderOnClickListener);

        // Set reminders variables
        // 리마인더 변수 설정

        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(mActivity);
        String defaultReminderString = prefs.getString(
                GeneralPreferences.KEY_DEFAULT_REMINDER, GeneralPreferences.NO_REMINDER_STRING);
        mDefaultReminderMinutes = Integer.parseInt(defaultReminderString);
        prepareReminders();

        return mView;
    }

    private void updateTitle() {
        Resources res = getActivity().getResources();
        if (mCanModifyCalendar && !mIsOrganizer) {
            getActivity().setTitle(res.getString(R.string.event_info_title_invite));
        } else {
            getActivity().setTitle(res.getString(R.string.event_info_title));
        }
    }

    /**
     * Initializes the event cursor, which is expected to point to the first
     * (and only) result from a query.
     * 쿼리의 첫 번째(및 유일한) 결과를 가리킬 것으로 예상되는 이벤트 커서를 초기화함
     * @return true if the cursor is empty.
     *          커서가 비어 있는 경우 true 반환
     */
    private boolean initEventCursor() {
        if ((mEventCursor == null) || (mEventCursor.getCount() == 0)) {
            return true;
        }
        mEventCursor.moveToFirst();
        mEventId = mEventCursor.getInt(EVENT_INDEX_ID);
        String rRule = mEventCursor.getString(EVENT_INDEX_RRULE);
        mIsRepeating = !TextUtils.isEmpty(rRule);
        // mHasAlarm will be true if it was saved in the event already, or if
        // we've explicitly been provided reminders (e.g. during rotation).
        // mHasAlarm이 이미 이벤트에서 저장되었거나, 명시적으로 알림 메시지가 제공된 경우
        // (예: 로테이션?회전? 중)에는 true가 될 것임
        mHasAlarm = (mEventCursor.getInt(EVENT_INDEX_HAS_ALARM) == 1)? true :
                (mReminders != null && mReminders.size() > 0);
        mMaxReminders = mEventCursor.getInt(EVENT_INDEX_MAX_REMINDERS);
        mCalendarAllowedReminders =  mEventCursor.getString(EVENT_INDEX_ALLOWED_REMINDERS);
        return false;
    }

    @SuppressWarnings("fallthrough")
    private void initAttendeesCursor(View view) {
        mOriginalAttendeeResponse = Attendees.ATTENDEE_STATUS_NONE;
        mCalendarOwnerAttendeeId = EditEventHelper.ATTENDEE_ID_NONE;
        mNumOfAttendees = 0;
        if (mAttendeesCursor != null) {
            mNumOfAttendees = mAttendeesCursor.getCount();
            if (mAttendeesCursor.moveToFirst()) {
                mAcceptedAttendees.clear();
                mDeclinedAttendees.clear();
                mTentativeAttendees.clear();
                mNoResponseAttendees.clear();

                do {
                    int status = mAttendeesCursor.getInt(ATTENDEES_INDEX_STATUS);
                    String name = mAttendeesCursor.getString(ATTENDEES_INDEX_NAME);
                    String email = mAttendeesCursor.getString(ATTENDEES_INDEX_EMAIL);

                    if (mAttendeesCursor.getInt(ATTENDEES_INDEX_RELATIONSHIP) ==
                            Attendees.RELATIONSHIP_ORGANIZER) {

                        // Overwrites the one from Event table if available
                        // 가능한 경우 이벤트 테이블에서 덮어씀
                        if (!TextUtils.isEmpty(name)) {
                            mEventOrganizerDisplayName = name;
                            if (!mIsOrganizer) {
                                setVisibilityCommon(view, R.id.organizer_container, View.VISIBLE);
                                setTextCommon(view, R.id.organizer, mEventOrganizerDisplayName);
                            }
                        }
                    }

                    if (mCalendarOwnerAttendeeId == EditEventHelper.ATTENDEE_ID_NONE &&
                            mCalendarOwnerAccount.equalsIgnoreCase(email)) {
                        mCalendarOwnerAttendeeId = mAttendeesCursor.getInt(ATTENDEES_INDEX_ID);
                        mOriginalAttendeeResponse = mAttendeesCursor.getInt(ATTENDEES_INDEX_STATUS);
                    } else {
                        String identity = mAttendeesCursor.getString(ATTENDEES_INDEX_IDENTITY);
                        String idNamespace = mAttendeesCursor.getString(ATTENDEES_INDEX_ID_NAMESPACE);

                        // Don't show your own status in the list because:
                        //  1) it doesn't make sense for event without other guests.
                        //  2) there's a spinner for that for events with guests.
                        // 다음과 같은 이유로 리스트에 자신의 상태를 표시하지 말기:
                        //  1) 다른 참석자(게스트)가 없는 이벤트는 말이 안 됨
                        //  2) 참석자와의 행사에는 그것을 위한 스피너가 있음
                        switch(status) {
                            case Attendees.ATTENDEE_STATUS_ACCEPTED:
                                mAcceptedAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_ACCEPTED, identity,
                                        idNamespace));
                                break;
                            case Attendees.ATTENDEE_STATUS_DECLINED:
                                mDeclinedAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_DECLINED, identity,
                                        idNamespace));
                                break;
                            case Attendees.ATTENDEE_STATUS_TENTATIVE:
                                mTentativeAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_TENTATIVE, identity,
                                        idNamespace));
                                break;
                            default:
                                mNoResponseAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_NONE, identity,
                                        idNamespace));
                        }
                    }
                } while (mAttendeesCursor.moveToNext());
                mAttendeesCursor.moveToFirst();

                updateAttendees(view);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(BUNDLE_KEY_EVENT_ID, mEventId);
        outState.putLong(BUNDLE_KEY_START_MILLIS, mStartMillis);
        outState.putLong(BUNDLE_KEY_END_MILLIS, mEndMillis);
        outState.putBoolean(BUNDLE_KEY_IS_DIALOG, mIsDialog);
        outState.putInt(BUNDLE_KEY_WINDOW_STYLE, mWindowStyle);
        outState.putBoolean(BUNDLE_KEY_DELETE_DIALOG_VISIBLE, mDeleteDialogVisible);
        outState.putInt(BUNDLE_KEY_CALENDAR_COLOR, mCalendarColor);
        outState.putBoolean(BUNDLE_KEY_CALENDAR_COLOR_INIT, mCalendarColorInitialized);
        outState.putInt(BUNDLE_KEY_ORIGINAL_COLOR, mOriginalColor);
        outState.putBoolean(BUNDLE_KEY_ORIGINAL_COLOR_INIT, mOriginalColorInitialized);
        outState.putInt(BUNDLE_KEY_CURRENT_COLOR, mCurrentColor);
        outState.putBoolean(BUNDLE_KEY_CURRENT_COLOR_INIT, mCurrentColorInitialized);
        outState.putString(BUNDLE_KEY_CURRENT_COLOR_KEY, mCurrentColorKey);

        // We'll need the temporary response for configuration changes.
        // 구성 변경에 대한 임시 대응이 필요할 것임
        outState.putInt(BUNDLE_KEY_TENTATIVE_USER_RESPONSE, mTentativeUserSetResponse);
        if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE &&
                mEditResponseHelper != null) {
            outState.putInt(BUNDLE_KEY_RESPONSE_WHICH_EVENTS,
                    mEditResponseHelper.getWhichEvents());
        }

        // Save the current response.
        // 현재 응답 저장
        int response;
        if (mAttendeeResponseFromIntent != Attendees.ATTENDEE_STATUS_NONE) {
            response = mAttendeeResponseFromIntent;
        } else {
            response = mOriginalAttendeeResponse;
        }
        outState.putInt(BUNDLE_KEY_ATTENDEE_RESPONSE, response);
        if (mUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
            response = mUserSetResponse;
            outState.putInt(BUNDLE_KEY_USER_SET_ATTENDEE_RESPONSE, response);
            outState.putInt(BUNDLE_KEY_RESPONSE_WHICH_EVENTS, mWhichEvents);
        }

        // Save the reminders.
        // 리마인더 저장
        mReminders = EventViewUtils.reminderItemsToReminders(mReminderViews,
                mReminderMinuteValues, mReminderMethodValues);
        int numReminders = mReminders.size();
        ArrayList<Integer> reminderMinutes =
                new ArrayList<Integer>(numReminders);
        ArrayList<Integer> reminderMethods =
                new ArrayList<Integer>(numReminders);
        for (ReminderEntry reminder : mReminders) {
            reminderMinutes.add(reminder.getMinutes());
            reminderMethods.add(reminder.getMethod());
        }
        outState.putIntegerArrayList(
                BUNDLE_KEY_REMINDER_MINUTES, reminderMinutes);
        outState.putIntegerArrayList(
                BUNDLE_KEY_REMINDER_METHODS, reminderMethods);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Show color/edit/delete buttons only in non-dialog configuration
        // 대화상자가 아닌 구성에서만 컬러/편집/삭제 버튼을 표시함
        if (!mIsDialog && !mIsTabletConfig || mWindowStyle == EventInfoFragment.FULL_WINDOW_STYLE) {
            inflater.inflate(R.menu.event_info_title_bar, menu);
            mMenu = menu;
            updateMenu();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // If we're a dialog we don't want to handle menu buttons
        // 대화 상자인데 메뉴 버튼을 처리하지 않을 경우
        if (mIsDialog) {
            return false;
        }
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
            mDeleteHelper =
                    new DeleteEventHelper(mActivity, mActivity, true /* exitWhenDone */);
            mDeleteHelper.setDeleteNotificationListener(EventInfoFragment.this);
            mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
            mDeleteDialogVisible = true;
            mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
        } else if (itemId == R.id.info_action_change_color) {
            showEventColorPickerDialog();
        } else if (itemId == R.id.info_action_share_event) {
            shareEvent(ShareType.INTENT);
        } else if (itemId == R.id.info_action_export) {
            shareEvent(ShareType.SDCARD);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Generates an .ics formatted file with the event info and launches intent chooser to
     * share said file
     * 이벤트 정보가 포함된 .ics 형식의 파일을 생성하고 해당 파일을 공유할 intent 선택기 실행
     */
    private void shareEvent(ShareType type) {
        // Create the respective ICalendar objects from the event info
        // 이벤트 정보로부터 각 ICalendar 객체 생성
        VCalendar calendar = new VCalendar();
        calendar.addProperty(VCalendar.VERSION, "2.0");
        calendar.addProperty(VCalendar.PRODID, VCalendar.PRODUCT_IDENTIFIER);
        calendar.addProperty(VCalendar.CALSCALE, "GREGORIAN");
        calendar.addProperty(VCalendar.METHOD, "REQUEST");

        VEvent event = new VEvent();
        mEventCursor.moveToFirst();
        // Add event start and end datetime
        // 이벤트 시작 및 종료 datetime 추가
        if (!mAllDay) {
            String eventTimeZone = mEventCursor.getString(EVENT_INDEX_EVENT_TIMEZONE);
            event.addEventStart(mStartMillis, eventTimeZone);
            event.addEventEnd(mEndMillis, eventTimeZone);
        } else {
            // All-day events' start and end time are stored as UTC.
            // Treat the event start and end time as being in the local time zone and convert them
            // to the corresponding UTC datetime. If the UTC time is used as is, the ical recipients
            // will report the wrong start and end time (+/- 1 day) for the event as they will
            // convert the UTC time to their respective local time-zones
            // 종일 이벤트 시작 및 종료 시간은 UTC로 저장됨
            // 이벤트 시작 및 종료 시간을 로컬 표준 시간대에 있는 것으로 처리하고
            // 해당 UTC 데이터 시간으로 변환함
            // UTC 시간을 그대로 사용할 경우, ical recipient(수신자?)는 UTC 시간을 각 지역 시간대로
            // 변환하기 때문에, 이벤트에 대한 잘못된 시작 및 종료 시간 (+/- 1일)을 보고함
            String localTimeZone = Utils.getTimeZone(mActivity, mTZUpdater);
            long eventStart = IcalendarUtils.convertTimeToUtc(mStartMillis, localTimeZone);
            long eventEnd = IcalendarUtils.convertTimeToUtc(mEndMillis, localTimeZone);
            event.addEventStart(eventStart, "UTC");
            event.addEventEnd(eventEnd, "UTC");
        }

        event.addProperty(VEvent.LOCATION, mEventCursor.getString(EVENT_INDEX_EVENT_LOCATION));
        event.addProperty(VEvent.DESCRIPTION, mEventCursor.getString(EVENT_INDEX_DESCRIPTION));
        event.addProperty(VEvent.SUMMARY, mEventCursor.getString(EVENT_INDEX_TITLE));
        event.addOrganizer(new Organizer(mEventOrganizerDisplayName, mEventOrganizerEmail));

        // Add Attendees to event
        // 이벤트에 참석자 추가
        for (Attendee attendee : mAcceptedAttendees) {
            IcalendarUtils.addAttendeeToEvent(attendee, event);
        }

        for (Attendee attendee : mDeclinedAttendees) {
            IcalendarUtils.addAttendeeToEvent(attendee, event);
        }

        for (Attendee attendee : mTentativeAttendees) {
            IcalendarUtils.addAttendeeToEvent(attendee, event);
        }

        for (Attendee attendee : mNoResponseAttendees) {
            IcalendarUtils.addAttendeeToEvent(attendee, event);
        }

        // Compose all of the ICalendar objects
        // 모든 ICalender 개체 구성
        calendar.addEvent(event);

        // Create and share ics file
        // ics 파일 만들고 공유
        boolean isShareSuccessful = false;
        try {
            // Event title serves as the file name prefix
            // 이벤트 제목이 파일 이름 접두사로 사용됨
            String filePrefix = event.getProperty(VEvent.SUMMARY);
            if (filePrefix == null || filePrefix.length() < 3) {
                // Default to a generic filename if event title doesn't qualify
                // Prefix length constraint is imposed by File#createTempFile
                // 이벤트 제목이 적합하지 않을 경우, 일반 파일이름으로 기본 설정됨
                // 접두사 길이 조건은 File#createTempFile에 의해 정해짐
                filePrefix = "invite";
            }

            filePrefix = filePrefix.replaceAll("\\W+", " ");

            if (!filePrefix.endsWith(" ")) {
                filePrefix += " ";
            }

            File dir;
            if (type == ShareType.SDCARD) {
                dir = EXPORT_SDCARD_DIRECTORY;
                if (!dir.exists()) {
                    dir.mkdir();
                }
            } else {
                dir = mActivity.getExternalCacheDir();
            }

            File inviteFile = IcalendarUtils.createTempFile(filePrefix, ".ics",
                    dir);

            if (IcalendarUtils.writeCalendarToFile(calendar, inviteFile)) {
                if (type == ShareType.INTENT) {
                    inviteFile.setReadable(true, false);     // Set world-readable
                    Uri icsFile = FileProvider.getUriForFile(getActivity(),
                            BuildConfig.APPLICATION_ID + ".provider", inviteFile);
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, icsFile);
                    // The ics file is sent as an extra, the receiving application decides whether
                    // to parse the file to extract calendar events or treat it as a regular file
                    // ics 파일은 추가 파일로 전송되며, 받은 애플리케이션은 캘린더 이벤트를 추출하기 위해
                    // 파일을 분석할지 아니면 일반 파일로 처리할지를 결정함
                    shareIntent.setType("application/octet-stream");

                    Intent chooserIntent = Intent.createChooser(shareIntent,
                            getResources().getString(R.string.cal_share_intent_title));

                    // The MMS app only responds to "text/x-vcalendar" so we create a chooser intent
                    // that includes the targeted mms intent + any that respond to the above general
                    // purpose "application/octet-stream" intent.
                    // MMS 앱은 "text/x-vcalendar"에만 응답하기 때문에,
                    // 대상 mms intent + 일반적인 목적 "application/octet-stream" intent에 대응하는
                    // 모든 것을 포함하는 chooser intent를 만듦
                    File vcsInviteFile = File.createTempFile(filePrefix, ".vcs",
                            mActivity.getExternalCacheDir());

                    // For now, we are duplicating ics file and using that as the vcs file
                    // 현재는 ics 파일을 복제하여 vcs 파일로 사용하고 있음
                    // TODO: revisit above
                    if (IcalendarUtils.copyFile(inviteFile, vcsInviteFile)) {
                        Uri vcsFile = FileProvider.getUriForFile(getActivity(),
                                BuildConfig.APPLICATION_ID + ".provider", vcsInviteFile);
                        Intent mmsShareIntent = new Intent();
                        mmsShareIntent.setAction(Intent.ACTION_SEND);
                        mmsShareIntent.setPackage("com.android.mms");
                        mmsShareIntent.putExtra(Intent.EXTRA_STREAM, vcsFile);
                        mmsShareIntent.setType("text/x-vcalendar");
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                                new Intent[]{mmsShareIntent});
                    }
                    startActivity(chooserIntent);
                } else {
                    String msg = getString(R.string.cal_export_succ_msg);
                    Toast.makeText(mActivity, String.format(msg, inviteFile),
                            Toast.LENGTH_SHORT).show();
                }
                isShareSuccessful = true;

            } else {
                // Error writing event info to file
                // 파일에 이벤트 정보를 쓰는 중 오류
                isShareSuccessful = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            isShareSuccessful = false;
        }

        if (!isShareSuccessful) {
            Log.e(TAG, "Couldn't generate ics file");
            Toast.makeText(mActivity, R.string.error_generating_ics, Toast.LENGTH_SHORT).show();
        }
    }

    private void showEventColorPickerDialog() {
        if (mColorPickerDialog == null) {
            mColorPickerDialog = EventColorPickerDialog.newInstance(mColors, mCurrentColor,
                    mCalendarColor, mIsTabletConfig);
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

        ContentValues values = new ContentValues();
        if (mCurrentColor != mCalendarColor) {
            values.put(Events.EVENT_COLOR_KEY, mCurrentColorKey);
        } else {
            values.put(Events.EVENT_COLOR_KEY, NO_EVENT_COLOR);
        }
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
        mHandler.startUpdate(mHandler.getNextToken(), null, uri, values,
                null, null, Utils.UNDO_DELAY);
        return true;
    }

    @Override
    public void onStop() {
        Activity act = getActivity();
        if (!mEventDeletionStarted && act != null && !act.isChangingConfigurations()) {

            boolean responseSaved = saveResponse();
            boolean eventColorSaved = saveEventColor();
            if (saveReminders() || responseSaved || eventColorSaved) {
                Toast.makeText(getActivity(), R.string.saving_event, Toast.LENGTH_SHORT).show();
            }
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mEventCursor != null) {
            mEventCursor.close();
        }
        if (mCalendarsCursor != null) {
            mCalendarsCursor.close();
        }
        if (mAttendeesCursor != null) {
            mAttendeesCursor.close();
        }
        super.onDestroy();
    }

    /**
     * Asynchronously saves the response to an invitation if the user changed
     * the response. Returns true if the database will be updated.
     * 사용자가 응답을 변경한 경우, 초대에 대한 응답을 비동기식으로 저장함
     * 데이터베이스가 업데이트될 경우 true를 반환함
     *
     * @return true if the database will be changed
     */
    private boolean saveResponse() {
        if (mAttendeesCursor == null || mEventCursor == null) {
            return false;
        }

        int status = getResponseFromButtonId(
                mResponseRadioGroup.getCheckedRadioButtonId());
        if (status == Attendees.ATTENDEE_STATUS_NONE) {
            return false;
        }

        // If the status has not changed, then don't update the database
        // 상태가 변경되지 않은 경우, 데이터베이스를 업데이트하지 않음
        if (status == mOriginalAttendeeResponse) {
            return false;
        }

        // If we never got an owner attendee id we can't set the status
        // 소유주 참석자 ID가 없으면 상태를 설정할 수 없음
        if (mCalendarOwnerAttendeeId == EditEventHelper.ATTENDEE_ID_NONE) {
            return false;
        }

        if (!mIsRepeating) {
            // This is a non-repeating event
            // 반복되지 않는 이벤트
            updateResponse(mEventId, mCalendarOwnerAttendeeId, status);
            mOriginalAttendeeResponse = status;
            return true;
        }

        if (DEBUG) {
            Log.d(TAG, "Repeating event: mWhichEvents=" + mWhichEvents);
        }
        // This is a repeating event
        // 반복 이벤트
        switch (mWhichEvents) {
            case -1:
                return false;
            case UPDATE_SINGLE:
                createExceptionResponse(mEventId, status);
                mOriginalAttendeeResponse = status;
                return true;
            case UPDATE_ALL:
                updateResponse(mEventId, mCalendarOwnerAttendeeId, status);
                mOriginalAttendeeResponse = status;
                return true;
            default:
                Log.e(TAG, "Unexpected choice for updating invitation response");
                break;
        }
        return false;
    }

    private void updateResponse(long eventId, long attendeeId, int status) {
        // Update the attendee status in the attendees table. the provider
        // takes care of updating the self attendance status.
        // 참석자 테이블에서 참석자 상태를 업데이트함
        // 제공자는 본인의 출석 상태 업데이트를 관리함
        ContentValues values = new ContentValues();

        if (!TextUtils.isEmpty(mCalendarOwnerAccount)) {
            values.put(Attendees.ATTENDEE_EMAIL, mCalendarOwnerAccount);
        }
        values.put(Attendees.ATTENDEE_STATUS, status);
        values.put(Attendees.EVENT_ID, eventId);

        Uri uri = ContentUris.withAppendedId(Attendees.CONTENT_URI, attendeeId);

        mHandler.startUpdate(mHandler.getNextToken(), null, uri, values,
                null, null, Utils.UNDO_DELAY);
    }

    /**
     * Creates an exception to a recurring event.  The only change we're making is to the
     * "self attendee status" value.  The provider will take care of updating the corresponding
     * Attendees.attendeeStatus entry.
     * 반복..? 이벤트에 대한 예외를 생성함 , "본인 참석자 상태"의 값에 대한 변화만 하고 있음
     * 제공자는 해당하는 Attendees.attendeeStatue 항목을 업데이트하는 작업을 처리할 것임
     *
     * @param eventId The recurring event.
     * @param status The new value for selfAttendeeStatus.
     */
    private void createExceptionResponse(long eventId, int status) {
        ContentValues values = new ContentValues();
        values.put(Events.ORIGINAL_INSTANCE_TIME, mStartMillis);
        values.put(Events.SELF_ATTENDEE_STATUS, status);
        values.put(Events.STATUS, Events.STATUS_CONFIRMED);

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        Uri exceptionUri = Uri.withAppendedPath(Events.CONTENT_EXCEPTION_URI,
                String.valueOf(eventId));
        ops.add(ContentProviderOperation.newInsert(exceptionUri).withValues(values).build());

        mHandler.startBatch(mHandler.getNextToken(), null, CalendarContract.AUTHORITY, ops,
                Utils.UNDO_DELAY);
    }

    private void doEdit() {
        Context c = getActivity();
        // This ensures that we aren't in the process of closing and have been
        // unattached already
        // 우리가 닫는 중이 아니며, 이미 연결되어 있지 않음을 보장함
        if (c != null) {
            Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
            Intent intent = new Intent(Intent.ACTION_EDIT, uri);
            intent.setClass(mActivity, EditEventActivity.class);
            intent.putExtra(EXTRA_EVENT_BEGIN_TIME, mStartMillis);
            intent.putExtra(EXTRA_EVENT_END_TIME, mEndMillis);
            intent.putExtra(EXTRA_EVENT_ALL_DAY, mAllDay);
            intent.putExtra(EditEventActivity.EXTRA_EVENT_COLOR, mCurrentColor);
            intent.putExtra(EditEventActivity.EXTRA_EVENT_REMINDERS, EventViewUtils
                    .reminderItemsToReminders(mReminderViews, mReminderMinuteValues,
                            mReminderMethodValues));
            intent.putExtra(EVENT_EDIT_ON_LAUNCH, true);
            startActivity(intent);
        }
    }

    private void updateEvent(View view) {
        if (mEventCursor == null || view == null) {
            return;
        }

        Context context = view.getContext();
        if (context == null) {
            return;
        }

        String eventName = mEventCursor.getString(EVENT_INDEX_TITLE);
        if (eventName == null || eventName.length() == 0) {
            eventName = getActivity().getString(R.string.no_title_label);
        }

        // 3rd parties might not have specified the start/end time when firing the
        // Events.CONTENT_URI intent.  Update these with values read from the db.
        // 제3자가 CONTENT_URI intent를 실행할 때, 시작/종료 시간을 지정하지 않았을 수 있음
        // db에서 읽은 값으로 업데이트함
        if (mStartMillis == 0 && mEndMillis == 0) {
            mStartMillis = mEventCursor.getLong(EVENT_INDEX_DTSTART);
            mEndMillis = mEventCursor.getLong(EVENT_INDEX_DTEND);
            if (mEndMillis == 0) {
                String duration = mEventCursor.getString(EVENT_INDEX_DURATION);
                if (!TextUtils.isEmpty(duration)) {
                    try {
                        Duration d = new Duration();
                        d.parse(duration);
                        long endMillis = mStartMillis + d.getMillis();
                        if (endMillis >= mStartMillis) {
                            mEndMillis = endMillis;
                        } else {
                            Log.d(TAG, "Invalid duration string: " + duration);
                        }
                    } catch (DateException e) {
                        Log.d(TAG, "Error parsing duration string " + duration, e);
                    }
                }
                if (mEndMillis == 0) {
                    mEndMillis = mStartMillis;
                }
            }
        }

        mAllDay = mEventCursor.getInt(EVENT_INDEX_ALL_DAY) != 0;
        String location = mEventCursor.getString(EVENT_INDEX_EVENT_LOCATION);
        String description = mEventCursor.getString(EVENT_INDEX_DESCRIPTION);
        String rRule = mEventCursor.getString(EVENT_INDEX_RRULE);
        String eventTimezone = mEventCursor.getString(EVENT_INDEX_EVENT_TIMEZONE);

        mHeadlines.setBackgroundColor(mCurrentColor);

        // What
        if (eventName != null) {
            setTextCommon(view, R.id.title, eventName);
        }

        // When
        // Set the date and repeats (if any)
        // 날짜 및 반복(있는 경우) 설정
        String localTimezone = Utils.getTimeZone(mActivity, mTZUpdater);

        Resources resources = context.getResources();
        String displayedDatetime = Utils.getDisplayedDatetime(mStartMillis, mEndMillis,
                System.currentTimeMillis(), localTimezone, mAllDay, context);

        String displayedTimezone = null;
        if (!mAllDay) {
            displayedTimezone = Utils.getDisplayedTimezone(mStartMillis, localTimezone,
                    eventTimezone);
        }
        // Display the datetime.  Make the timezone (if any) transparent.
        // 시간 표시, 시간대(있는 경우)를 투명하게 함
        if (displayedTimezone == null) {
            setTextCommon(view, R.id.when_datetime, displayedDatetime);
        } else {
            int timezoneIndex = displayedDatetime.length();
            displayedDatetime += "  " + displayedTimezone;
            SpannableStringBuilder sb = new SpannableStringBuilder(displayedDatetime);
            ForegroundColorSpan transparentColorSpan = new ForegroundColorSpan(
                    resources.getColor(R.color.event_info_headline_transparent_color));
            sb.setSpan(transparentColorSpan, timezoneIndex, displayedDatetime.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            setTextCommon(view, R.id.when_datetime, sb);
        }

        // Display the repeat string (if any)
        // 반복 문자열(있는 경우) 표시
        String repeatString = null;
        if (!TextUtils.isEmpty(rRule)) {
            EventRecurrence eventRecurrence = new EventRecurrence();
            eventRecurrence.parse(rRule);
            Time date = new Time(localTimezone);
            date.set(mStartMillis);
            if (mAllDay) {
                date.timezone = Time.TIMEZONE_UTC;
            }
            eventRecurrence.setStartDate(date);
            repeatString = EventRecurrenceFormatter.getRepeatString(mContext, resources,
                    eventRecurrence, true);
        }
        if (repeatString == null) {
            view.findViewById(R.id.when_repeat).setVisibility(View.GONE);
        } else {
            setTextCommon(view, R.id.when_repeat, repeatString);
        }

        // Organizer view is setup in the updateCalendar method
        // Organizer view가 updateCalendar 메소드에서 설정됨


        // Where
        if (location == null || location.trim().length() == 0) {
            setVisibilityCommon(view, R.id.where, View.GONE);
        } else {
            final TextView textView = mWhere;
            if (textView != null) {
                textView.setAutoLinkMask(0);
                textView.setText(location.trim());
                try {
                    textView.setText(Utils.extendedLinkify(textView.getText().toString(), true));

                    // Linkify.addLinks() sets the TextView movement method if it finds any links.
                    // We must do the same here, in case linkify by itself did not find any.
                    // (This is cloned from Linkify.addLinkMovementMethod().)
                    // Linkify.addLinks()는 링크를 찾을 셩우 TextView 이동 방법을 설정함
                    // 만약 그 자체로 linkify...가 아무것도 찾지 못했을 경우를 대비해서 똑같이 해야 함
                    MovementMethod mm = textView.getMovementMethod();
                    if ((mm == null) || !(mm instanceof LinkMovementMethod)) {
                        if (textView.getLinksClickable()) {
                            textView.setMovementMethod(LinkMovementMethod.getInstance());
                        }
                    }
                } catch (Exception ex) {
                    // unexpected
                    Log.e(TAG, "Linkification failed", ex);
                }

                textView.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        try {
                            return v.onTouchEvent(event);
                        } catch (ActivityNotFoundException e) {
                            // ignore
                            return true;
                        }
                    }
                });
            }
        }

        // Description
        if (description != null && description.length() != 0) {
            mDesc.setText(description);
        }

        // Launch Custom App
        updateCustomAppButton();
    }

    private void updateCustomAppButton() {
        buttonSetup: {
            final Button launchButton = (Button) mView.findViewById(R.id.launch_custom_app_button);
            if (launchButton == null)
                break buttonSetup;

            final String customAppPackage = mEventCursor.getString(EVENT_INDEX_CUSTOM_APP_PACKAGE);
            final String customAppUri = mEventCursor.getString(EVENT_INDEX_CUSTOM_APP_URI);

            if (TextUtils.isEmpty(customAppPackage) || TextUtils.isEmpty(customAppUri))
                break buttonSetup;

            PackageManager pm = mContext.getPackageManager();
            if (pm == null)
                break buttonSetup;

            ApplicationInfo info;
            try {
                info = pm.getApplicationInfo(customAppPackage, 0);
                if (info == null)
                    break buttonSetup;
            } catch (NameNotFoundException e) {
                break buttonSetup;
            }

            Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
            final Intent intent = new Intent(CalendarContract.ACTION_HANDLE_CUSTOM_EVENT, uri);
            intent.setPackage(customAppPackage);
            intent.putExtra(CalendarContract.EXTRA_CUSTOM_APP_URI, customAppUri);
            intent.putExtra(EXTRA_EVENT_BEGIN_TIME, mStartMillis);

            // See if we have a taker for our intent
            // intent를 위한 taker가 있는지 보기
            if (pm.resolveActivity(intent, 0) == null)
                break buttonSetup;

            Drawable icon = pm.getApplicationIcon(info);
            if (icon != null) {

                Drawable[] d = launchButton.getCompoundDrawables();
                icon.setBounds(0, 0, mCustomAppIconSize, mCustomAppIconSize);
                launchButton.setCompoundDrawables(icon, d[1], d[2], d[3]);
            }

            CharSequence label = pm.getApplicationLabel(info);
            if (label != null && label.length() != 0) {
                launchButton.setText(label);
            } else if (icon == null) {
                // No icon && no label. Hide button?
                break buttonSetup;
            }

            // Launch custom app
            launchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivityForResult(intent, 0);
                    } catch (ActivityNotFoundException e) {
                        // Shouldn't happen as we checked it already
                        setVisibilityCommon(mView, R.id.launch_custom_app_container, View.GONE);
                    }
                }
            });

            setVisibilityCommon(mView, R.id.launch_custom_app_container, View.VISIBLE);
            return;

        }

        setVisibilityCommon(mView, R.id.launch_custom_app_container, View.GONE);
        return;
    }

    private void sendAccessibilityEvent() {
        AccessibilityManager am =
                (AccessibilityManager) getActivity().getSystemService(Service.ACCESSIBILITY_SERVICE);
        if (!am.isEnabled()) {
            return;
        }

        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        event.setClassName(getClass().getName());
        event.setPackageName(getActivity().getPackageName());
        List<CharSequence> text = event.getText();

        addFieldToAccessibilityEvent(text, mTitle, null);
        addFieldToAccessibilityEvent(text, mWhenDateTime, null);
        addFieldToAccessibilityEvent(text, mWhere, null);
        addFieldToAccessibilityEvent(text, null, mDesc);

        if (mResponseRadioGroup.getVisibility() == View.VISIBLE) {
            int id = mResponseRadioGroup.getCheckedRadioButtonId();
            if (id != View.NO_ID) {
                text.add(((TextView) getView().findViewById(R.id.response_label)).getText());
                text.add((((RadioButton) (mResponseRadioGroup.findViewById(id)))
                        .getText() + PERIOD_SPACE));
            }
        }

        am.sendAccessibilityEvent(event);
    }

    private void addFieldToAccessibilityEvent(List<CharSequence> text, TextView tv,
                                              ExpandableTextView etv) {
        CharSequence cs;
        if (tv != null) {
            cs = tv.getText();
        } else if (etv != null) {
            cs = etv.getText();
        } else {
            return;
        }

        if (!TextUtils.isEmpty(cs)) {
            cs = cs.toString().trim();
            if (cs.length() > 0) {
                text.add(cs);
                text.add(PERIOD_SPACE);
            }
        }
    }

    private void updateCalendar(View view) {

        mCalendarOwnerAccount = "";
        if (mCalendarsCursor != null && mEventCursor != null) {
            mCalendarsCursor.moveToFirst();
            String tempAccount = mCalendarsCursor.getString(CALENDARS_INDEX_OWNER_ACCOUNT);
            mCalendarOwnerAccount = (tempAccount == null) ? "" : tempAccount;
            mOwnerCanRespond = mCalendarsCursor.getInt(CALENDARS_INDEX_OWNER_CAN_RESPOND) != 0;
            mSyncAccountName = mCalendarsCursor.getString(CALENDARS_INDEX_ACCOUNT_NAME);

            // start visible calendars query
            // 표시되는 캘린더 쿼리 시작
            mHandler.startQuery(TOKEN_QUERY_VISIBLE_CALENDARS, null, Calendars.CONTENT_URI,
                    CALENDARS_PROJECTION, CALENDARS_VISIBLE_WHERE, new String[] {"1"}, null);

            mEventOrganizerEmail = mEventCursor.getString(EVENT_INDEX_ORGANIZER);
            mIsOrganizer = mCalendarOwnerAccount.equalsIgnoreCase(mEventOrganizerEmail);

            if (!TextUtils.isEmpty(mEventOrganizerEmail) &&
                    !mEventOrganizerEmail.endsWith(Utils.MACHINE_GENERATED_ADDRESS)) {
                mEventOrganizerDisplayName = mEventOrganizerEmail;
            }

            if (!mIsOrganizer && !TextUtils.isEmpty(mEventOrganizerDisplayName)) {
                setTextCommon(view, R.id.organizer, mEventOrganizerDisplayName);
                setVisibilityCommon(view, R.id.organizer_container, View.VISIBLE);
            } else {
                setVisibilityCommon(view, R.id.organizer_container, View.GONE);
            }
            mHasAttendeeData = mEventCursor.getInt(EVENT_INDEX_HAS_ATTENDEE_DATA) != 0;
            mCanModifyCalendar = mEventCursor.getInt(EVENT_INDEX_ACCESS_LEVEL)
                    >= Calendars.CAL_ACCESS_CONTRIBUTOR;
            // TODO add "|| guestCanModify" after b/1299071 is fixed
            mCanModifyEvent = mCanModifyCalendar && mIsOrganizer;
            mIsBusyFreeCalendar =
                    mEventCursor.getInt(EVENT_INDEX_ACCESS_LEVEL) == Calendars.CAL_ACCESS_FREEBUSY;

            if (!mIsBusyFreeCalendar) {

                View b = mView.findViewById(R.id.edit);
                b.setEnabled(true);
                b.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doEdit();
                        // For dialogs, just close the fragment
                        // For full screen, close activity on phone, leave it for tablet
                        if (mIsDialog) {
                            EventInfoFragment.this.dismiss();
                        }
                        else if (!mIsTabletConfig){
                            getActivity().finish();
                        }
                    }
                });
            }
            View button;
            if (mCanModifyCalendar) {
                button = mView.findViewById(R.id.delete);
                if (button != null) {
                    button.setEnabled(true);
                    button.setVisibility(View.VISIBLE);
                }
            }
            if (mCanModifyEvent) {
                button = mView.findViewById(R.id.edit);
                if (button != null) {
                    button.setEnabled(true);
                    button.setVisibility(View.VISIBLE);
                }
            }
            if ((!mIsDialog && !mIsTabletConfig ||
                    mWindowStyle == EventInfoFragment.FULL_WINDOW_STYLE) && mMenu != null) {
                mActivity.invalidateOptionsMenu();
            }
        } else {
            setVisibilityCommon(view, R.id.calendar, View.GONE);
            sendAccessibilityEventIfQueryDone(TOKEN_QUERY_DUPLICATE_CALENDARS);
        }
    }

    /**
     *
     */
    private void updateMenu() {
        if (mMenu == null) {
            return;
        }
        MenuItem delete = mMenu.findItem(R.id.info_action_delete);
        MenuItem edit = mMenu.findItem(R.id.info_action_edit);
        MenuItem changeColor = mMenu.findItem(R.id.info_action_change_color);
        if (delete != null) {
            delete.setVisible(mCanModifyCalendar);
            delete.setEnabled(mCanModifyCalendar);
        }
        if (edit != null) {
            edit.setVisible(mCanModifyEvent);
            edit.setEnabled(mCanModifyEvent);
        }
        if (changeColor != null && mColors != null && mColors.length > 0) {
            changeColor.setVisible(mCanModifyCalendar);
            changeColor.setEnabled(mCanModifyCalendar);
        }
    }

    private void updateAttendees(View view) {
        if (mAcceptedAttendees.size() + mDeclinedAttendees.size() +
                mTentativeAttendees.size() + mNoResponseAttendees.size() > 0) {
            mLongAttendees.clearAttendees();
            (mLongAttendees).addAttendees(mAcceptedAttendees);
            (mLongAttendees).addAttendees(mDeclinedAttendees);
            (mLongAttendees).addAttendees(mTentativeAttendees);
            (mLongAttendees).addAttendees(mNoResponseAttendees);
            mLongAttendees.setEnabled(false);
            mLongAttendees.setVisibility(View.VISIBLE);
        } else {
            mLongAttendees.setVisibility(View.GONE);
        }

        if (hasEmailableAttendees()) {
            setVisibilityCommon(mView, R.id.email_attendees_container, View.VISIBLE);
            if (emailAttendeesButton != null) {
                emailAttendeesButton.setText(R.string.email_guests_label);
            }
        } else if (hasEmailableOrganizer()) {
            setVisibilityCommon(mView, R.id.email_attendees_container, View.VISIBLE);
            if (emailAttendeesButton != null) {
                emailAttendeesButton.setText(R.string.email_organizer_label);
            }
        } else {
            setVisibilityCommon(mView, R.id.email_attendees_container, View.GONE);
        }
    }

    /**
     * Returns true if there is at least 1 attendee that is not the viewer.
     * viewer가 아닌 참가자가 적어도 한 명 이상인 경우 true 반환
     */
    private boolean hasEmailableAttendees() {
        for (Attendee attendee : mAcceptedAttendees) {
            if (Utils.isEmailableFrom(attendee.mEmail, mSyncAccountName)) {
                return true;
            }
        }
        for (Attendee attendee : mTentativeAttendees) {
            if (Utils.isEmailableFrom(attendee.mEmail, mSyncAccountName)) {
                return true;
            }
        }
        for (Attendee attendee : mNoResponseAttendees) {
            if (Utils.isEmailableFrom(attendee.mEmail, mSyncAccountName)) {
                return true;
            }
        }
        for (Attendee attendee : mDeclinedAttendees) {
            if (Utils.isEmailableFrom(attendee.mEmail, mSyncAccountName)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEmailableOrganizer() {
        return mEventOrganizerEmail != null &&
                Utils.isEmailableFrom(mEventOrganizerEmail, mSyncAccountName);
    }

    public void initReminders(View view, Cursor cursor) {

        // Add reminders
        // 리마인더 추가
        mOriginalReminders.clear();
        mUnsupportedReminders.clear();
        while (cursor.moveToNext()) {
            int minutes = cursor.getInt(EditEventHelper.REMINDERS_INDEX_MINUTES);
            int method = cursor.getInt(EditEventHelper.REMINDERS_INDEX_METHOD);

            if (method != Reminders.METHOD_DEFAULT && !mReminderMethodValues.contains(method)) {
                // Stash unsupported reminder types separately so we don't alter
                // them in the UI
                // 지원되지 않는 알림 유형을 별도로 저장하여 UI에서 변경하지 않도록 함
                mUnsupportedReminders.add(ReminderEntry.valueOf(minutes, method));
            } else {
                mOriginalReminders.add(ReminderEntry.valueOf(minutes, method));
            }
        }
        // Sort appropriately for display (by time, then type)
        // 디스플레이?표시?에 맞게 정렬(시간별, 유형별)
        Collections.sort(mOriginalReminders);

        if (mUserModifiedReminders) {
            // If the user has changed the list of reminders don't change what's
            // shown.
            // 사용자가 리마인더 목록을 변경한 경우, 표시되는 내용을 변경하지 말기
            return;
        }

        LinearLayout parent = (LinearLayout) mScrollView
                .findViewById(R.id.reminder_items_container);
        if (parent != null) {
            parent.removeAllViews();
        }
        if (mReminderViews != null) {
            mReminderViews.clear();
        }

        if (mHasAlarm) {
            ArrayList<ReminderEntry> reminders;
            // If applicable, use reminders saved in the bundle.
            // 해당되는 경우, 번들에 저장된 미리 리마인더 사용함
            if (mReminders != null) {
                reminders = mReminders;
            } else {
                reminders = mOriginalReminders;
            }
            // Insert any minute values that aren't represented in the minutes list.
            for (ReminderEntry re : reminders) {
                EventViewUtils.addMinutesToList(
                        mActivity, mReminderMinuteValues, mReminderMinuteLabels, re.getMinutes());
            }
            // Create a UI element for each reminder.  We display all of the reminders we get
            // from the provider, even if the count exceeds the calendar maximum.  (Also, for
            // a new event, we won't have a maxReminders value available.)
            for (ReminderEntry re : reminders) {
                EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderViews,
                        mReminderMinuteValues, mReminderMinuteLabels, mReminderMethodValues,
                        mReminderMethodLabels, re, Integer.MAX_VALUE, mReminderChangeListener);
            }
            EventViewUtils.updateAddReminderButton(mView, mReminderViews, mMaxReminders);
            // TODO show unsupported reminder types in some fashion.
        }
    }

    void updateResponse(View view) {
        // we only let the user accept/reject/etc. a meeting if:
        // a) you can edit the event's containing calendar AND
        // b) you're not the organizer and only attendee AND
        // c) organizerCanRespond is enabled for the calendar
        // (if the attendee data has been hidden, the visible number of attendees
        // will be 1 -- the calendar owner's).
        // (there are more cases involved to be 100% accurate, such as
        // paying attention to whether or not an attendee status was
        // included in the feed, but we're currently omitting those corner cases
        // for simplicity).

        // TODO Switch to EditEventHelper.canRespond when this class uses CalendarEventModel.
        if (!mCanModifyCalendar || (mHasAttendeeData && mIsOrganizer && mNumOfAttendees <= 1) ||
                (mIsOrganizer && !mOwnerCanRespond)) {
            setVisibilityCommon(view, R.id.response_container, View.GONE);
            return;
        }

        setVisibilityCommon(view, R.id.response_container, View.VISIBLE);


        int response;
        if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
            response = mTentativeUserSetResponse;
        } else if (mUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
            response = mUserSetResponse;
        } else if (mAttendeeResponseFromIntent != Attendees.ATTENDEE_STATUS_NONE) {
            response = mAttendeeResponseFromIntent;
        } else {
            response = mOriginalAttendeeResponse;
        }

        int buttonToCheck = findButtonIdForResponse(response);
        mResponseRadioGroup.check(buttonToCheck); // -1 clear all radio buttons
        mResponseRadioGroup.setOnCheckedChangeListener(this);
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

    /**
     * Taken from com.google.android.gm.HtmlConversationActivity
     *
     * Send the intent that shows the Contact info corresponding to the email address.
     */
    public void showContactInfo(Attendee attendee, Rect rect) {
        // First perform lookup query to find existing contact
        final ContentResolver resolver = getActivity().getContentResolver();
        final String address = attendee.mEmail;
        final Uri dataUri = Uri.withAppendedPath(CommonDataKinds.Email.CONTENT_FILTER_URI,
                Uri.encode(address));
        final Uri lookupUri = ContactsContract.Data.getContactLookupUri(resolver, dataUri);

        if (lookupUri != null) {
            // Found matching contact, trigger QuickContact
            QuickContact.showQuickContact(getActivity(), rect, lookupUri,
                    QuickContact.MODE_MEDIUM, null);
        } else {
            // No matching contact, ask user to create one
            final Uri mailUri = Uri.fromParts("mailto", address, null);
            final Intent intent = new Intent(Intents.SHOW_OR_CREATE_CONTACT, mailUri);

            // Pass along full E-mail string for possible create dialog
            Rfc822Token sender = new Rfc822Token(attendee.mName, attendee.mEmail, null);
            intent.putExtra(Intents.EXTRA_CREATE_DESCRIPTION, sender.toString());

            // Only provide personal name hint if we have one
            final String senderPersonal = attendee.mName;
            if (!TextUtils.isEmpty(senderPersonal)) {
                intent.putExtra(Intents.Insert.NAME, senderPersonal);
            }

            startActivity(intent);
        }
    }

    @Override
    public void onPause() {
        mIsPaused = true;
        mHandler.removeCallbacks(onDeleteRunnable);
        super.onPause();
        // Remove event deletion alert box since it is being rebuild in the OnResume
        // This is done to get the same behavior on OnResume since the AlertDialog is gone on
        // rotation but not if you press the HOME key
        if (mDeleteDialogVisible && mDeleteHelper != null) {
            mDeleteHelper.dismissAlertDialog();
            mDeleteHelper = null;
        }
        if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE
                && mEditResponseHelper != null) {
            mEditResponseHelper.dismissAlertDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsDialog) {
            setDialogSize(getActivity().getResources());
            applyDialogParams();
        }
        mIsPaused = false;
        if (mDismissOnResume) {
            mHandler.post(onDeleteRunnable);
        }
        // Display the "delete confirmation" or "edit response helper" dialog if needed
        if (mDeleteDialogVisible) {
            mDeleteHelper = new DeleteEventHelper(
                    mContext, mActivity,
                    !mIsDialog && !mIsTabletConfig /* exitWhenDone */);
            mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
            mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
        } else if (mTentativeUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
            int buttonId = findButtonIdForResponse(mTentativeUserSetResponse);
            mResponseRadioGroup.check(buttonId);
            mEditResponseHelper.showDialog(mEditResponseHelper.getWhichEvents());
        }
    }

    @Override
    public void eventsChanged() {
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.EVENTS_CHANGED;
    }

    @Override
    public void handleEvent(EventInfo event) {
        reloadEvents();
    }

    public void reloadEvents() {
        if (mHandler != null) {
            mHandler.startQuery(TOKEN_QUERY_EVENT, null, mUri, EVENT_PROJECTION,
                    null, null, null);
        }
    }

    @Override
    public void onClick(View view) {

        // This must be a click on one of the "remove reminder" buttons
        LinearLayout reminderItem = (LinearLayout) view.getParent();
        LinearLayout parent = (LinearLayout) reminderItem.getParent();
        parent.removeView(reminderItem);
        mReminderViews.remove(reminderItem);
        mUserModifiedReminders = true;
        EventViewUtils.updateAddReminderButton(mView, mReminderViews, mMaxReminders);
    }

    /**
     * Add a new reminder when the user hits the "add reminder" button.  We use the default
     * reminder time and method.
     */
    private void addReminder() {
        // TODO: when adding a new reminder, make it different from the
        // last one in the list (if any).
        if (mDefaultReminderMinutes == GeneralPreferences.NO_REMINDER) {
            EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderViews,
                    mReminderMinuteValues, mReminderMinuteLabels, mReminderMethodValues,
                    mReminderMethodLabels,
                    ReminderEntry.valueOf(GeneralPreferences.REMINDER_DEFAULT_TIME), mMaxReminders,
                    mReminderChangeListener);
        } else {
            EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderViews,
                    mReminderMinuteValues, mReminderMinuteLabels, mReminderMethodValues,
                    mReminderMethodLabels, ReminderEntry.valueOf(mDefaultReminderMinutes),
                    mMaxReminders, mReminderChangeListener);
        }

        EventViewUtils.updateAddReminderButton(mView, mReminderViews, mMaxReminders);
    }

    synchronized private void prepareReminders() {
        // Nothing to do if we've already built these lists _and_ we aren't
        // removing not allowed methods
        if (mReminderMinuteValues != null && mReminderMinuteLabels != null
                && mReminderMethodValues != null && mReminderMethodLabels != null
                && mCalendarAllowedReminders == null) {
            return;
        }
        // Load the labels and corresponding numeric values for the minutes and methods lists
        // from the assets.  If we're switching calendars, we need to clear and re-populate the
        // lists (which may have elements added and removed based on calendar properties).  This
        // is mostly relevant for "methods", since we shouldn't have any "minutes" values in a
        // new event that aren't in the default set.
        Resources r = mActivity.getResources();
        mReminderMinuteValues = loadIntegerArray(r, R.array.reminder_minutes_values);
        mReminderMinuteLabels = loadStringArray(r, R.array.reminder_minutes_labels);
        mReminderMethodValues = loadIntegerArray(r, R.array.reminder_methods_values);
        mReminderMethodLabels = loadStringArray(r, R.array.reminder_methods_labels);

        // Remove any reminder methods that aren't allowed for this calendar.  If this is
        // a new event, mCalendarAllowedReminders may not be set the first time we're called.
        if (mCalendarAllowedReminders != null) {
            EventViewUtils.reduceMethodList(mReminderMethodValues, mReminderMethodLabels,
                    mCalendarAllowedReminders);
        }
        if (mView != null) {
            mView.invalidate();
        }
    }

    private boolean saveReminders() {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(3);

        // Read reminders from UI
        mReminders = EventViewUtils.reminderItemsToReminders(mReminderViews,
                mReminderMinuteValues, mReminderMethodValues);
        mOriginalReminders.addAll(mUnsupportedReminders);
        Collections.sort(mOriginalReminders);
        mReminders.addAll(mUnsupportedReminders);
        Collections.sort(mReminders);

        // Check if there are any changes in the reminder
        boolean changed = EditEventHelper.saveReminders(ops, mEventId, mReminders,
                mOriginalReminders, false /* no force save */);

        if (!changed) {
            return false;
        }

        // save new reminders
        AsyncQueryService service = new AsyncQueryService(getActivity());
        service.startBatch(0, null, Calendars.CONTENT_URI.getAuthority(), ops, 0);
        mOriginalReminders = mReminders;
        // Update the "hasAlarm" field for the event
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
        int len = mReminders.size();
        boolean hasAlarm = len > 0;
        if (hasAlarm != mHasAlarm) {
            ContentValues values = new ContentValues();
            values.put(Events.HAS_ALARM, hasAlarm ? 1 : 0);
            service.startUpdate(0, null, uri, values, null, null, 0);
        }
        return true;
    }

    /**
     * Email all the attendees of the event, except for the viewer (so as to not email
     * himself) and resources like conference rooms.
     */
    private void emailAttendees() {
        Intent i = new Intent(getActivity(), QuickResponseActivity.class);
        i.putExtra(QuickResponseActivity.EXTRA_EVENT_ID, mEventId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    @Override
    public void onDeleteStarted() {
        mEventDeletionStarted = true;
    }

    private Dialog.OnDismissListener createDeleteOnDismissListener() {
        return new Dialog.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // Since OnPause will force the dialog to dismiss , do
                // not change the dialog status
                if (!mIsPaused) {
                    mDeleteDialogVisible = false;
                }
            }
        };
    }

    public long getEventId() {
        return mEventId;
    }

    public long getStartMillis() {
        return mStartMillis;
    }

    public long getEndMillis() {
        return mEndMillis;
    }

    private void setDialogSize(Resources r) {
        mDialogWidth = (int)r.getDimension(R.dimen.event_info_dialog_width);
        mDialogHeight = (int)r.getDimension(R.dimen.event_info_dialog_height);
    }

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
        }
    }
}
