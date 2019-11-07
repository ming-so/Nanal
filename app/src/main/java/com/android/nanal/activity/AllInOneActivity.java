package com.android.nanal.activity;

import android.Manifest;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Events;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nanal.CreateNanalCalendar;
import com.android.nanal.DayFragment;
import com.android.nanal.DayOfMonthDrawable;
import com.android.nanal.DynamicTheme;
import com.android.nanal.ExtensionsFactory;
import com.android.nanal.GroupInvitation;
import com.android.nanal.LoginActivity;
import com.android.nanal.NanalDBHelper;
import com.android.nanal.PrefManager;
import com.android.nanal.R;
import com.android.nanal.TodayFragment;
import com.android.nanal.agenda.AgendaFragment;
import com.android.nanal.alerts.AlertService;
import com.android.nanal.alerts.NotificationMgr;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarController.EventHandler;
import com.android.nanal.calendar.CalendarController.EventInfo;
import com.android.nanal.calendar.CalendarController.EventType;
import com.android.nanal.calendar.CalendarController.ViewType;
import com.android.nanal.calendar.CalendarToolbarHandler;
import com.android.nanal.calendar.CalendarViewAdapter;
import com.android.nanal.calendar.OtherPreferences;
import com.android.nanal.calendar.SelectVisibleCalendarsFragment;
import com.android.nanal.datetimepicker.date.DatePickerDialog;
import com.android.nanal.event.EventInfoFragment;
import com.android.nanal.event.GeneralPreferences;
import com.android.nanal.event.Utils;
import com.android.nanal.group.Group;
import com.android.nanal.group.GroupDetailFragment;
import com.android.nanal.group.GroupFragment;
import com.android.nanal.interfaces.AllInOneMenuExtensionsInterface;
import com.android.nanal.month.MonthByWeekFragment;
import com.android.nanal.query.DiaryAsyncTask;
import com.android.nanal.query.EventAsyncTask;
import com.android.nanal.query.GroupAsyncTask;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS;
import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static com.android.nanal.alerts.AlertService.ALERT_CHANNEL_ID;

public class AllInOneActivity extends AbstractCalendarActivity implements EventHandler, CalendarController.DiaryHandler,
        OnSharedPreferenceChangeListener, SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "AllInOneActivity";
    private static final boolean DEBUG = false;
    private static final String EVENT_INFO_FRAGMENT_TAG = "EventInfoFragment";
    private static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    private static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    private static final String BUNDLE_KEY_RESTORE_VIEW = "key_restore_view";
    //private static final String BUNDLE_KEY_CHECK_ACCOUNTS = "key_check_for_accounts";
    private static final int HANDLER_KEY = 0;
    private static final int PERMISSIONS_REQUEST_WRITE_CALENDAR = 0;

    // Indices of buttons for the drop down menu (tabs replacement)
    // 드롭다운 메뉴의 버튼의... 색인(탭 교체)
    // Must match the strings in the array buttons_list in arrays.xml and the
    // OnNavigationListener
    // arrays.xml의 buttons_list 배열 및 OnNavigationLister에 있는 문자열과 일치해야 함
    private static final int BUTTON_DAY_INDEX = 0;
    private static final int BUTTON_WEEK_INDEX = 1;
    private static final int BUTTON_MONTH_INDEX = 2;
    private static final int BUTTON_AGENDA_INDEX = 3;
    private static boolean mIsMultipane;
    private static boolean mIsTabletConfig;
    private static boolean mShowAgendaWithMonth;
    private static boolean mShowEventDetailsWithAgenda;
    DayOfMonthDrawable mDayOfMonthIcon;
    int mOrientation;
    BroadcastReceiver mCalIntentReceiver;
    private CalendarController mController;
    // Create an observer so that we can update the views whenever a
    // Calendar event changes.
    // 캘린더 이벤트가 변경될 때마다 뷰를 업데이트할 수 있도록 옵저버를 생성함
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            eventsChanged();
            diariesChanged();
        }
    };

    private boolean mOnSaveInstanceStateCalled = false;
    private boolean mBackToPreviousView = false;
    private ContentResolver mContentResolver;
    private int mPreviousView;
    private int mCurrentView;
    private boolean mPaused = true;
    private boolean mUpdateOnResume = false;
    private boolean mHideControls = false;
    private boolean mShowSideViews = true;
    private boolean mShowWeekNum = false;
    private TextView mHomeTime;
    private TextView mDateRange;
    private TextView mWeekTextView;
    private View mMiniMonth;
    private View mCalendarsList;
    private View mMiniMonthContainer;
    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final AnimatorListener mSlideAnimationDoneListener = new AnimatorListener() {

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationEnd(android.animation.Animator animation) {
            int visibility = mShowSideViews ? View.VISIBLE : View.GONE;
            mMiniMonth.setVisibility(visibility);
            mCalendarsList.setVisibility(visibility);
            mMiniMonthContainer.setVisibility(visibility);
        }

        @Override
        public void onAnimationRepeat(android.animation.Animator animation) {
        }

        @Override
        public void onAnimationStart(android.animation.Animator animation) {
        }
    };

    private View mSecondaryPane;
    private String mTimeZone;
    private boolean mShowCalendarControls;
    private boolean mShowEventInfoFullScreenAgenda;
    private boolean mShowEventInfoFullScreen;
    private int mWeekNum;
    private int mCalendarControlsAnimationTime;
    private int mControlsAnimateWidth;
    private int mControlsAnimateHeight;
    private long mViewEventId = -1;
    private long mIntentEventStartMillis = -1;
    private long mIntentEventEndMillis = -1;
    private int mIntentAttendeeResponse = Attendees.ATTENDEE_STATUS_NONE;
    private boolean mIntentAllDay = false;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private NavigationView mNavigationView;
    private int mCurrentMenuItem;
    private CalendarToolbarHandler mCalendarToolbarHandler;
    // Action bar
    private ActionBar mActionBar;
    private SearchView mSearchView;
    private MenuItem mSearchMenu;
    private MenuItem mControlsMenu;
    private MenuItem mViewSettings;
    private Menu mOptionsMenu;
    private QueryHandler mHandler;

    private BottomNavigationView mBottomNavi;
    private FloatingActionsMenu mFAB;
    private FloatingActionButton mAddCalendar, mAddDiary, mFABGroup;
    private ViewStub mViewStub;

    private final Runnable mHomeTimeUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(AllInOneActivity.this, mHomeTimeUpdater);
            updateSecondaryTitleFields(-1);
            AllInOneActivity.this.invalidateOptionsMenu();
            Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTimeZone);
        }
    };
    // runs every midnight/time changes and refreshes the today icon
    // 매일 자정에 변경 내용을 실행하고 오늘 아이콘 새로고침
    private final Runnable mTimeChangesUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(AllInOneActivity.this, mHomeTimeUpdater);
            AllInOneActivity.this.invalidateOptionsMenu();
            Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTimeZone);
        }
    };
    //private boolean mCheckForAccounts = true;
    private String mHideString;
    private String mShowString;
    // Params for animating the controls on the right
    // 우측에 있는 컨트롤의 애니메이션을 위한 매개변수
    private RelativeLayout.LayoutParams mControlsParams;
    private LinearLayout.LayoutParams mVerticalControlsParams;
    private AllInOneMenuExtensionsInterface mExtensions = ExtensionsFactory
            .getAllInOneMenuExtensions();

    //    public String connectID = "test";
    SharedPreferences prefs;
    public static String connectId;

    public int selectedMode = 1;

    public static NanalDBHelper helper = null;
    public static SQLiteDatabase nanalDB = null;

    public static ArrayList<Group> mGroups = new ArrayList<>();

    public static Activity mActivity;
    public static Context mContext;

    public static int mGroupId = -1;
    public static String mGroupName = "";

    private static boolean isRequiredMenu = true;  // default: 캘린더
    private static boolean isInGroupDetail = false;


    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (DEBUG)
            Log.d(TAG, "New intent received " + intent.toString());
        // Don't change the date if we're just returning to the app's home
        // 앱 홈으로 돌아가기만 하면 날짜 변경 안 함
        if (Intent.ACTION_VIEW.equals(action)
                && !intent.getBooleanExtra(Utils.INTENT_KEY_HOME, false)) {
            long millis = parseViewAction(intent);
            if (millis == -1) {
                millis = Utils.timeFromIntentInMillis(intent);
            }
            if (millis != -1 && mViewEventId == -1 && mController != null) {
                Time time = new Time(mTimeZone);
                time.set(millis);
                time.normalize(true);
                mController.sendEvent(this, EventType.GO_TO, time, time, -1, ViewType.CURRENT);
            }
        }
    }


    @Override
    protected void onCreate(Bundle icicle) {
        if (Utils.getSharedPreference(this, OtherPreferences.KEY_OTHER_1, false)) {
            setTheme(R.style.CalendarTheme_WithActionBarWallpaper);
        }
        super.onCreate(icicle);
        dynamicTheme.onCreate(this);

        mContext = AllInOneActivity.this;
        mActivity = AllInOneActivity.this;

        prefs = this.getApplicationContext().getSharedPreferences("login_setting", MODE_PRIVATE);
        connectId = prefs.getString("loginId", null);

        openDatabase();

        // This needs to be created before setContentView
        // setContentView 이전에 생성해야 함
        mController = CalendarController.getInstance(this);

        // Create notification channel
        // 알림 채널 생성
        NotificationMgr nm = new AlertService.NotificationMgrWrapper(
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        if (Utils.isOreoOrLater()) {
            String appName = this.getString(R.string.standalone_app_label);
            NotificationChannel channel = new NotificationChannel(ALERT_CHANNEL_ID, appName, NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        // Check and ask for most needed permissions
        // 퍼미션을 확인하고 요청함
        checkAppPermissions();

        // Get time from intent or icicle
        // 인텐트 또는 icicle에서 시간 가져옴
        long timeMillis = -1;
        int viewType = -1;
        final Intent intent = getIntent();
        if (icicle != null) {
            timeMillis = icicle.getLong(BUNDLE_KEY_RESTORE_TIME);
            viewType = icicle.getInt(BUNDLE_KEY_RESTORE_VIEW, -1);
        } else {
            String action = intent.getAction();
            if (Intent.ACTION_VIEW.equals(action)) {
                // Open EventInfo later
                timeMillis = parseViewAction(intent);
            }

            if (timeMillis == -1) {
                timeMillis = Utils.timeFromIntentInMillis(intent);
            }
        }

        if (viewType == -1 || viewType > ViewType.MAX_VALUE) {
            viewType = Utils.getViewTypeFromIntentAndSharedPref(this);
        }
        mTimeZone = Utils.getTimeZone(this, mHomeTimeUpdater);
        Time t = new Time(mTimeZone);
        t.set(timeMillis);

        if (DEBUG) {
            if (icicle != null && intent != null) {
                Log.d(TAG, "both, icicle:" + icicle.toString() + "  intent:" + intent.toString());
            } else {
                Log.d(TAG, "not both, icicle:" + icicle + " intent:" + intent);
            }
        }

        Resources res = getResources();
        mHideString = res.getString(R.string.hide_controls);
        mShowString = res.getString(R.string.show_controls);
        mOrientation = res.getConfiguration().orientation;
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mControlsAnimateWidth = (int) res.getDimension(R.dimen.calendar_controls_width);
            if (mControlsParams == null) {
                mControlsParams = new RelativeLayout.LayoutParams(mControlsAnimateWidth, 0);
            }
            mControlsParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        } else {
            // Make sure width is in between allowed min and max width values
            // 허용된 최소 폭과 최대 폭 값 사이에 width 값이 있는지 확인
            mControlsAnimateWidth = Math.max(res.getDisplayMetrics().widthPixels * 45 / 100,
                    (int) res.getDimension(R.dimen.min_portrait_calendar_controls_width));
            mControlsAnimateWidth = Math.min(mControlsAnimateWidth,
                    (int) res.getDimension(R.dimen.max_portrait_calendar_controls_width));
        }

        mControlsAnimateHeight = (int) res.getDimension(R.dimen.calendar_controls_height);

        mHideControls = !Utils.getSharedPreference(
                this, GeneralPreferences.KEY_SHOW_CONTROLS, true);
        mIsMultipane = Utils.getConfigBool(this, R.bool.multiple_pane_config);
        mIsTabletConfig = Utils.getConfigBool(this, R.bool.tablet_config);
        mShowAgendaWithMonth = Utils.getConfigBool(this, R.bool.show_agenda_with_month);
        mShowCalendarControls =
                Utils.getConfigBool(this, R.bool.show_calendar_controls);
        mShowEventDetailsWithAgenda =
                Utils.getConfigBool(this, R.bool.show_event_details_with_agenda);
        mShowEventInfoFullScreenAgenda =
                Utils.getConfigBool(this, R.bool.agenda_show_event_info_full_screen);
        mShowEventInfoFullScreen =
                Utils.getConfigBool(this, R.bool.show_event_info_full_screen);
        mCalendarControlsAnimationTime = res.getInteger(R.integer.calendar_controls_animation_time);
        Utils.setAllowWeekForDetailView(mIsMultipane);

        // setContentView must be called before configureActionBar
        // setContentView를 configureActionBar보다 먼저 호출해야 함
        setContentView(R.layout.all_in_one_material);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        mViewStub = findViewById(R.id.stub_import);
        mViewStub.setVisibility(View.VISIBLE);

        mBottomNavi = findViewById(R.id.bottom_navigation);

        MenuItem menuItem = mBottomNavi.getMenu().findItem(R.id.action_calendar);
        LayerDrawable icon = (LayerDrawable) menuItem.getIcon();
        Utils.setTodayIcon(icon, this, mTimeZone);

        mBottomNavi.setSelectedItemId(R.id.action_today);
        mBottomNavi.setSelectedItemId(R.id.action_calendar);

        mFAB = findViewById(R.id.floating_action_button2);
        mAddCalendar = findViewById(R.id.action_add_event);
        mAddDiary = findViewById(R.id.action_add_diary);
        mFABGroup = findViewById(R.id.floating_action_button_group);


        if (mIsTabletConfig) {
            mDateRange = (TextView) findViewById(R.id.date_bar);
            mWeekTextView = (TextView) findViewById(R.id.week_num);
        } else {
            mDateRange = (TextView) getLayoutInflater().inflate(R.layout.date_range_title, null);
        }


        setupToolbar(viewType);
        setupNavDrawer();
        setupFloatingActionButton();

        mHomeTime = (TextView) findViewById(R.id.home_time);
        mMiniMonth = findViewById(R.id.mini_month);
        if (mIsTabletConfig && mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            mMiniMonth.setLayoutParams(new RelativeLayout.LayoutParams(mControlsAnimateWidth,
                    mControlsAnimateHeight));
        }
        mCalendarsList = findViewById(R.id.calendar_list);
        mMiniMonthContainer = findViewById(R.id.mini_month_container);
        mSecondaryPane = findViewById(R.id.secondary_pane);

        // Must register as the first activity because this activity can modify
        // the list of event handlers in it's handle method. This affects who
        // the rest of the handlers the controller dispatches to are.
        // 이 activity는 핸들 메소드에 있는 이벤트 핸들러 리스트를 수정할 수 있기 때문에
        // 첫 번째 activity로 등록해야 함
        // 컨트롤러의 나머지 핸들러가 누구인지에 영향을 미침
        mController.registerFirstEventHandler(HANDLER_KEY, this);

        Log.i(TAG, "viewType:"+viewType);
        initFragments(timeMillis, 4, icicle);

        // Listen for changes that would require this to be refreshed
        // 새로 고치는 데 필요한 변경 내용 듣기(listen)
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        mContentResolver = getContentResolver();

        mBottomNavi.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_calendar:
                        // mode 1
                        if (selectedMode == 1) {
                            // 오늘 날짜로 갱신
                            Time t = new Time(mTimeZone);
                            t.setToNow();
                            int viewType = ViewType.CURRENT;
                            long extras = CalendarController.EXTRA_GOTO_TIME;
                            extras |= CalendarController.EXTRA_GOTO_TODAY;
                            mController.sendEvent(this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
                        } else {
                            // 캘린더 화면으로 이동
                            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
                            selectedMode = 1;
                        }
                        break;
                    case R.id.action_today:
                        // mode 2
                        mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.TODAY);
                        //mViewStub.setLayoutResource();
                        selectedMode = 2;
                        break;
                    case R.id.action_group:
                        // mode 3
                        mController.sendEvent(this, EventType.GO_TO, null, null, 0, ViewType.GROUP);
                        selectedMode = 3;
                        break;
                    case R.id.action_settings:
                        // mode 4
                        mController.sendEvent(this, EventType.LAUNCH_SETTINGS_DIRECT, null, null, 0, 0);
                        selectedMode = 4;
                        break;
                }
                return true;
            }
        });



        handleDynamicLink();
    }

    public void openDatabase() {
        PrefManager prefManager = new PrefManager(getApplicationContext());
        if (helper == null) {
            helper = new NanalDBHelper(getApplicationContext());
        }
        if (!prefManager.isDBCreated()) {
            // DB가 만들어지지 않았다면 새로 생성하기
            nanalDB = helper.getWritableDatabase();
            prefManager.setDBCreated(true);
            Toast.makeText(this, "DB 생성 완료", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "DB 이미 존재함", Toast.LENGTH_LONG).show();
            mGroups = helper.getGroupList();
            Log.i(TAG, "mGroups.size() " + mGroups.size());
        }
        try{
            mGroups = helper.getGroupList();
            Log.i(TAG, "mGroups.size() " + mGroups.size());
            try {
                Snackbar.make(getCurrentFocus(), "서버와 동기화를 진행합니다.", Snackbar.LENGTH_SHORT).show();
            } catch (IllegalArgumentException e) {

            }
            GroupSync();
            DiarySync();
        } catch (Exception e) {

        }
    }

    private void checkAppPermissions() {
        // Here, thisActivity is the current activity
        // thisActivity는 현재 activity
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CALENDAR)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED)) {

            // No explanation needed, we can request the permission.
            // 퍼미션 요청

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_CALENDAR);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_CALENDAR: {
                // If request is cancelled, the result arrays are empty.
                // 요청이 취소되면 결과 배열이 비어 있음
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    // 퍼미션 받음!
                    createLocalCalendar();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.user_rejected_calendar_write_permission, Toast.LENGTH_LONG).show();
                    checkAppPermissions();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
            // 필요에 따라 이 앱에서 요청할 다른 사용 권한을 위한 case 추가하셈
        }

        // Clean up cached ics and vcs files - in case onDestroy() didn't run the last time
        // 캐시된 ics, vcs 파일을 정리 - onDestory()가 마지막으로 실행되지 않은 경우
        cleanupCachedEventFiles();
    }


    private void setupToolbar(int viewType) {
        mToolbar = findViewById(R.id.toolbar);
        if (mToolbar == null) {
            if (DEBUG) {
                Log.d(TAG, "Didn't find a toolbar");
            }
            return;
        }

        if (!mIsTabletConfig) {
            mCalendarToolbarHandler = new CalendarToolbarHandler(this, mToolbar, viewType);
        } else {
            int titleResource;
            switch (viewType) {
                case ViewType.AGENDA:
                    titleResource = R.string.agenda_view;
                    break;
                case ViewType.DAY:
                    titleResource = R.string.day_view;
                    break;
                case ViewType.MONTH:
                    titleResource = R.string.month_view;
                    break;
                case ViewType.TODAY:
                    titleResource = R.string.today_view;
                    break;
                case ViewType.GROUP:
                    titleResource = R.string.group_view;
                    break;
                case ViewType.GROUP_DETAIL:
                    titleResource = R.string.group_detail;
                    break;
                case ViewType.WEEK:
                default:
                    titleResource = R.string.week_view;
                    break;
            }
            mToolbar.setTitle(titleResource);
        }
        mToolbar.setTitle(getTitle());
        mToolbar.setNavigationIcon(R.drawable.ic_menu_navigator);
        setSupportActionBar(mToolbar);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInGroupDetail) {
                    // 화면 전환
                } else {
                    AllInOneActivity.this.openDrawer();
                }
            }
        });
        mActionBar = getSupportActionBar();
        if (mActionBar == null) return;
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);
    }

    public void openDrawer() {
        mDrawerLayout.openDrawer(Gravity.LEFT);
    }

    public void setupNavDrawer() {
        if (mDrawerLayout == null) {
            if (DEBUG) {
                Log.d(TAG, "mDrawerLayout is null - Can not setup the NavDrawer! Have you set the android.support.v7.widget.DrawerLayout?");
            }
            return;
        }
        mNavigationView.setNavigationItemSelectedListener(this);
        showActionBar();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mFAB.isExpanded()) {
                Rect outRect = new Rect();
                mFAB.getGlobalVisibleRect(outRect);

                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY()))
                    mFAB.collapse();
            }
        }

        return super.dispatchTouchEvent(event);
    }

    public void setupFloatingActionButton() {
        mAddCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create new Event
                Time t = new Time();
                t.set(mController.getTime());
                t.second = 0;
                if (t.minute > 30) {
                    t.hour++;
                    t.minute = 0;
                } else if (t.minute > 0 && t.minute < 30) {
                    t.minute = 30;
                }
                if (createLocalCalendar()) {
                    mController.sendEventRelatedEvent(
                            this, EventType.CREATE_EVENT, -1, t.toMillis(true), 0, 0, 0, -1);
                }
            }
        });

        mAddDiary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GroupSync();
                DiarySync();
                Time t = new Time();
                t.set(mController.getTime());
                mController.sendEventRelatedEvent(
                        this, EventType.CREATE_DIARY, -1, t.toMillis(true), 0, 0, 0, -1);
            }
        });

        mFABGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.sendEventRelatedEvent(
                        this, EventType.CREATE_GROUP, -1, -1, 0, 0, 0, -1);
            }
        });
    }

    private boolean createLocalCalendar() {
        checkAppPermissions();
        try{
            PrefManager prefManager = new PrefManager(getApplicationContext());
            if (!prefManager.isCalendarCreated()) {
                // 로컬 캘린더가 만들어지지 않았다면
                try {
                    CreateNanalCalendar.CreateCalendar(AllInOneActivity.this.getApplicationContext(), "나날", connectId, false);
                    Toast.makeText(AllInOneActivity.this.getApplicationContext(), "캘린더를 생성했습니다.", Toast.LENGTH_LONG).show();
                    prefManager.setCalendarCreated(true);
                    return true;
                } catch (IllegalArgumentException e) {
                    Toast.makeText(AllInOneActivity.this.getApplicationContext(), "로컬 캘린더 생성에 문제가 발생했습니다.", Toast.LENGTH_LONG).show();
                    prefManager.setCalendarCreated(false);
                    return false;
                }
            } else {
                // 로컬 캘린더가 이미 있다면
                return true;
            }
        } catch (Exception e) {

        }
        // 권한 없음, 아무것도 하지 않음
        return false;
    }

    private void hideActionBar() {
        if (mActionBar == null) return;
        mActionBar.hide();
    }

    private void showActionBar() {
        if (mActionBar == null) return;
        mActionBar.show();
    }

    private long parseViewAction(final Intent intent) {
        long timeMillis = -1;
        Uri data = intent.getData();
        if (data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("events")) {
                try {
                    mViewEventId = Long.valueOf(data.getLastPathSegment());
                    if (mViewEventId != -1) {
                        mIntentEventStartMillis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, 0);
                        mIntentEventEndMillis = intent.getLongExtra(EXTRA_EVENT_END_TIME, 0);
                        mIntentAttendeeResponse = intent.getIntExtra(
                                ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_NONE);
                        mIntentAllDay = intent.getBooleanExtra(EXTRA_EVENT_ALL_DAY, false);
                        timeMillis = mIntentEventStartMillis;
                    }
                } catch (NumberFormatException e) {
                    // Ignore if mViewEventId can't be parsed
                    // mViewEventId를 parse할 수 없는 경우 무시함
                }
            }
        }
        return timeMillis;
    }

    // Clear buttons used in the agenda view
    // agenda view에 사용된 버튼 지우기
    private void clearOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }
        MenuItem cancelItem = mOptionsMenu.findItem(R.id.action_cancel);
        if (cancelItem != null) {
            cancelItem.setVisible(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);

        // Must register as the first activity because this activity can modify
        // the list of event handlers in it's handle method. This affects who
        // the rest of the handlers the controller dispatches to are.
        // 핸들러에 있는 핸들 메소드의 리스트를 수정할 수 있기 때문에 첫 번째 액티비티로 레지스터를 사용해야 함
        // 컨트롤러의 나머지 핸들러가 누구인지에 영향을 미침
        mController.registerFirstEventHandler(HANDLER_KEY, this);
        mOnSaveInstanceStateCalled = false;

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            //If permission is not granted then just return.
            // 퍼미션 못 받으면 그냥 반환
            Log.d(TAG, "Manifest.permission.READ_CALENDAR is not granted");
            return;
        }

        mContentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI,
                true, mObserver);
        if (mUpdateOnResume) {
            initFragments(mController.getTime(), mController.getViewType(), null);
            mUpdateOnResume = false;
        }
        Time t = new Time(mTimeZone);
        t.set(mController.getTime());
        mController.sendEvent(this, EventType.UPDATE_TITLE, t, t, -1, ViewType.CURRENT,
                mController.getDateFlags(), null, null);

        if (mControlsMenu != null) {
            mControlsMenu.setTitle(mHideControls ? mShowString : mHideString);
        }
        mPaused = false;

        if (mViewEventId != -1 && mIntentEventStartMillis != -1 && mIntentEventEndMillis != -1) {
            long currentMillis = System.currentTimeMillis();
            long selectedTime = -1;
            if (currentMillis > mIntentEventStartMillis && currentMillis < mIntentEventEndMillis) {
                selectedTime = currentMillis;
            }
            mController.sendEventRelatedEventWithExtra(this, EventType.VIEW_EVENT, mViewEventId,
                    mIntentEventStartMillis, mIntentEventEndMillis, -1, -1,
                    EventInfo.buildViewExtraLong(mIntentAttendeeResponse, mIntentAllDay),
                    selectedTime);
            mViewEventId = -1;
            mIntentEventStartMillis = -1;
            mIntentEventEndMillis = -1;
            mIntentAllDay = false;
        }
        Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTimeZone);
        // Make sure the today icon is up to date
        // 오늘 아이콘이 최신 상태인지 확인
        invalidateOptionsMenu();

        mCalIntentReceiver = Utils.setTimeChangesReceiver(this, mTimeChangesUpdater);

    }


    @Override
    protected void onPause() {
        super.onPause();

        mController.deregisterEventHandler(HANDLER_KEY);
        mPaused = true;
        mHomeTime.removeCallbacks(mHomeTimeUpdater);

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            //If permission is not granted then just return.
            Log.d(TAG, "Manifest.permission.WRITE_CALENDAR is not granted");
            return;
        }

        mContentResolver.unregisterContentObserver(mObserver);
        if (isFinishing()) {
            // Stop listening for changes that would require this to be refreshed
            // 새로 고쳐야 할 변경 사항은 듣지(listen) 않음
            SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        // FRAG_TODO save highlighted days of the week;
        // 강조 표시된 요일을 저장
        if (mController.getViewType() != ViewType.EDIT) {
            Utils.setDefaultView(this, mController.getViewType());
        }
        Utils.resetMidnightUpdater(mHandler, mTimeChangesUpdater);
        Utils.clearTimeChangesReceiver(this, mCalIntentReceiver);
    }


    @Override
    protected void onUserLeaveHint() {
        mController.sendEvent(this, EventType.USER_HOME, null, null, -1, ViewType.CURRENT);
        super.onUserLeaveHint();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mOnSaveInstanceStateCalled = true;
        super.onSaveInstanceState(outState);
        outState.putLong(BUNDLE_KEY_RESTORE_TIME, mController.getTime());
        outState.putInt(BUNDLE_KEY_RESTORE_VIEW, mCurrentView);
        if (mCurrentView == ViewType.EDIT) {
            outState.putLong(BUNDLE_KEY_EVENT_ID, mController.getEventId());
        } else if (mCurrentView == ViewType.AGENDA) {
            FragmentManager fm = getFragmentManager();
            Fragment f = fm.findFragmentById(R.id.main_pane);
            if (f instanceof AgendaFragment) {
                outState.putLong(BUNDLE_KEY_EVENT_ID, ((AgendaFragment) f).getLastShowEventId());
            }
        }
        //outState.putBoolean(BUNDLE_KEY_CHECK_ACCOUNTS, mCheckForAccounts);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        mController.deregisterAllEventHandlers();

        CalendarController.removeInstance(this);

        // Clean up cached ics and vcs files
        // 캐시된 ics 및 vcs 파일 정리
        cleanupCachedEventFiles();
    }


    /**
     * Cleans up the temporarily generated ics and vcs files in the cache directory
     * The files are of the format *.ics and *.vcs
     * 캐시 폴더에서 임시로 생성된 ics와 vcs 파일 정리
     * 파일 형식은 *.ics, *.vcs
     */
    private void cleanupCachedEventFiles() {
        if (!isExternalStorageWritable()) return;
        File cacheDir = getExternalCacheDir();
        File[] files = cacheDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            String filename = file.getName();
            if (filename.endsWith(".ics") || filename.endsWith(".vcs")) {
                file.delete();
            }
        }
    }

    /**
     * Checks if external storage is available for read and write
     * 외부 스토리지가 읽기 및 쓰기에 사용 가능한지 확인
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void initFragments(long timeMillis, int viewType, Bundle icicle) {
        if (DEBUG) {
            Log.d(TAG, "Initializing to " + timeMillis + " for view " + viewType);
        }
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if (mShowCalendarControls) {
            Fragment miniMonthFrag = new MonthByWeekFragment(timeMillis, true);
            ft.replace(R.id.mini_month, miniMonthFrag);
            mController.registerEventHandler(R.id.mini_month, (EventHandler) miniMonthFrag);

            Fragment selectCalendarsFrag = new SelectVisibleCalendarsFragment();
            ft.replace(R.id.calendar_list, selectCalendarsFrag);
            mController.registerEventHandler(
                    R.id.calendar_list, (EventHandler) selectCalendarsFrag);
        }
        if (!mShowCalendarControls || viewType == ViewType.EDIT) {
            mMiniMonth.setVisibility(View.GONE);
            mCalendarsList.setVisibility(View.GONE);
        }

        EventInfo info = null;
        if (viewType == ViewType.EDIT) {
            mPreviousView = GeneralPreferences.getSharedPreferences(this).getInt(
                    GeneralPreferences.KEY_START_VIEW, GeneralPreferences.DEFAULT_START_VIEW);

            long eventId = -1;
            Intent intent = getIntent();
            Uri data = intent.getData();
            if (data != null) {
                try {
                    eventId = Long.parseLong(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    if (DEBUG) {
                        Log.d(TAG, "Create new event");
                    }
                }
            } else if (icicle != null && icicle.containsKey(BUNDLE_KEY_EVENT_ID)) {
                eventId = icicle.getLong(BUNDLE_KEY_EVENT_ID);
            }

            long begin = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
            long end = intent.getLongExtra(EXTRA_EVENT_END_TIME, -1);
            info = new EventInfo();
            if (end != -1) {
                info.endTime = new Time();
                info.endTime.set(end);
            }
            if (begin != -1) {
                info.startTime = new Time();
                info.startTime.set(begin);
            }
            info.id = eventId;
            // We set the viewtype so if the user presses back when they are
            // done editing the controller knows we were in the Edit Event
            // screen. Likewise for eventId
            // 사용자의 편집이 완료되었을 때 back키를 누르면 이벤트 편집 화면에 있었다는 걸
            // 컨트롤러가 알게 viewtype 설정, eventId도 마찬가지임

            mController.setViewType(viewType);
            mController.setEventId(eventId);
        } else {
            mPreviousView = viewType;
        }

        setMainPane(ft, R.id.main_pane, viewType, timeMillis, true);
        ft.commit(); // this needs to be after setMainPane()  setMainPane() 이후여야 함

        Time t = new Time(mTimeZone);
        t.set(timeMillis);
        if (viewType == ViewType.AGENDA && icicle != null) {
            mController.sendEvent(this, EventType.GO_TO, t, null,
                    icicle.getLong(BUNDLE_KEY_EVENT_ID, -1), viewType);
        } else if (viewType != ViewType.EDIT) {
            mController.sendEvent(this, EventType.GO_TO, t, null, -1, viewType);
        }
    }


    @Override
    public void onBackPressed() {
        if (mCurrentView == ViewType.EDIT || mBackToPreviousView) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, mPreviousView);
        } else {
            super.onBackPressed();
        }
    }

    protected void updateViewSettingsVisibility() {
        if (mViewSettings != null) {
            boolean viewSettingsVisible = mController.getViewType() == ViewType.MONTH;
            mViewSettings.setVisible(viewSettingsVisible);
            mViewSettings.setEnabled(viewSettingsVisible);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.all_in_one_title_bar, menu);

        // Add additional options (if any).  추가적인 옵션 추가(있는 경우)
        Integer extensionMenuRes = mExtensions.getExtensionMenuResource(menu);
        if (extensionMenuRes != null) {
            getMenuInflater().inflate(extensionMenuRes, menu);
        }

        MenuItem item = menu.findItem(R.id.action_import);
        item.setVisible(ImportActivity.hasThingsToImport());

        mSearchMenu = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchMenu);
        if (mSearchView != null) {
            Utils.setUpSearchView(mSearchView, this);
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setOnSuggestionListener(this);
        }

        // Hide the "show/hide controls" button if this is a phone
        // or the view type is "Month" or "Agenda".
        // 휴대폰 또는 view 타입이 "Month" 또는 "Agenda"인 경우 "show/hide controls" 버튼을 숨김

        mControlsMenu = menu.findItem(R.id.action_hide_controls);
        if (!mShowCalendarControls) {
            if (mControlsMenu != null) {
                mControlsMenu.setVisible(false);
                mControlsMenu.setEnabled(false);
            }
        } else if (mControlsMenu != null && mController != null
                && (mController.getViewType() == ViewType.MONTH ||
                mController.getViewType() == ViewType.AGENDA)) {
            mControlsMenu.setVisible(false);
            mControlsMenu.setEnabled(false);
        } else if (mControlsMenu != null) {
            mControlsMenu.setTitle(mHideControls ? mShowString : mHideString);
        }

        mViewSettings = menu.findItem(R.id.action_view_settings);
        updateViewSettingsVisibility();
        setVisiblityMenuInGroup();

        return true;
    }

    public void setVisiblityMenuInGroup() {
        if (mOptionsMenu == null) {
            return;
        }
        Log.i(TAG, "setVisiblityMenuInGroup() 실행");
        MenuItem item1 = mOptionsMenu.findItem(R.id.action_goto);
        MenuItem item2 = mOptionsMenu.findItem(R.id.action_search);
        MenuItem item3 = mOptionsMenu.findItem(R.id.action_import);
        if (isInGroupDetail || !isRequiredMenu) {
            item1.setVisible(false);
            item1.setEnabled(false);
            item2.setVisible(false);
            item2.setEnabled(false);
            item3.setVisible(false);
            item3.setEnabled(false);
        } else {
            item1.setVisible(true);
            item1.setEnabled(true);
            item2.setVisible(true);
            item2.setEnabled(true);
            item3.setVisible(true);
            item3.setEnabled(true);
        }
    }

    public void GroupSync() {
        GroupAsyncTask mTask = new GroupAsyncTask(AllInOneActivity.this, AllInOneActivity.this);
        mTask.execute(connectId);
        mGroups = helper.getGroupList();
    }

    public void DiarySync() {
        DiaryAsyncTask mTask = new DiaryAsyncTask(AllInOneActivity.this, AllInOneActivity.this);
        mTask.execute(connectId);
    }

    public void EventSync() {
        EventAsyncTask mTask = new EventAsyncTask(AllInOneActivity.this, AllInOneActivity.this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Time t = null;
        int viewType = ViewType.CURRENT;
        long extras = CalendarController.EXTRA_GOTO_TIME;
        final int itemId = item.getItemId();
        if (itemId == R.id.action_refresh) {
            mGroups = helper.getGroupList();
            Log.i(TAG, "mGroups.size() " + mGroups.size());
            try {
                Snackbar.make(getCurrentFocus(), "서버와 동기화를 진행합니다.", Snackbar.LENGTH_SHORT).show();
            } catch (IllegalArgumentException e) {

            }
            GroupSync();
            DiarySync();
            mController.refreshCalendars();
            return true;
        } else if (itemId == R.id.action_today) {
            viewType = ViewType.CURRENT;
            t = new Time(mTimeZone);
            t.setToNow();
            extras |= CalendarController.EXTRA_GOTO_TODAY;
        } else if (itemId == R.id.action_goto) {
            Time todayTime;
            t = new Time(mTimeZone);
            t.set(mController.getTime());
            todayTime = new Time(mTimeZone);
            todayTime.setToNow();
            if (todayTime.month == t.month) {
                t = todayTime;
            }

            DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
                    Time selectedTime = new Time(mTimeZone);
                    selectedTime.year = year;
                    selectedTime.month = monthOfYear;
                    selectedTime.monthDay = dayOfMonth;
                    long extras = CalendarController.EXTRA_GOTO_TIME | CalendarController.EXTRA_GOTO_DATE;
                    mController.sendEvent(this, EventType.GO_TO, selectedTime, null, selectedTime, -1, ViewType.CURRENT, extras, null, null);
                }
            }, t.year, t.month, t.monthDay);
            datePickerDialog.show(getFragmentManager(), "datePickerDialog");

        } else if (itemId == R.id.action_hide_controls) {
            mHideControls = !mHideControls;
            Utils.setSharedPreference(
                    this, GeneralPreferences.KEY_SHOW_CONTROLS, !mHideControls);
            item.setTitle(mHideControls ? mShowString : mHideString);
            if (!mHideControls) {
                mMiniMonth.setVisibility(View.VISIBLE);
                mCalendarsList.setVisibility(View.VISIBLE);
                mMiniMonthContainer.setVisibility(View.VISIBLE);
            }
            final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this, "controlsOffset",
                    mHideControls ? 0 : mControlsAnimateWidth,
                    mHideControls ? mControlsAnimateWidth : 0);
            slideAnimation.setDuration(mCalendarControlsAnimationTime);
            ObjectAnimator.setFrameDelay(0);
            slideAnimation.start();
            return true;
        } else if (itemId == R.id.action_search) {
            return false;
        } else if (itemId == R.id.action_import) {
            ImportActivity.pickImportFile(this);
        } else {
            return mExtensions.handleItemSelected(item, this);
        }
        mController.sendEvent(this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
            case R.id.day_menu_item:
                if (mCurrentView != ViewType.DAY) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY);
                }
                break;
            case R.id.week_menu_item:
                if (mCurrentView != ViewType.WEEK) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.WEEK);
                }
                break;
            case R.id.month_menu_item:
                if (mCurrentView != ViewType.MONTH) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
                }
                break;
            case R.id.agenda_menu_item:
                if (mCurrentView != ViewType.AGENDA) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.AGENDA);
                }
                break;
            case R.id.action_select_visible_calendars:
                mController.sendEvent(this, EventType.LAUNCH_SELECT_VISIBLE_CALENDARS, null, null,
                        0, 0);
                break;
            case R.id.action_settings:
                mController.sendEvent(this, EventType.LAUNCH_SETTINGS, null, null, 0, 0);
                break;
            case R.id.test:
                SharedPreferences loginPref = getSharedPreferences("login_setting", MODE_PRIVATE);
                SharedPreferences.Editor editor = loginPref.edit();
                editor.remove("loginId");
                editor.remove("loginPw");
                editor.commit();

                Intent intent = new Intent(AllInOneActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.test2:
                CreateNanalCalendar.CreateCalendar(AllInOneActivity.this.getApplicationContext(), "나날", connectId, false);
                Toast.makeText(AllInOneActivity.this.getApplicationContext(), "생성", Toast.LENGTH_LONG).show();
                break;
            case R.id.test3:
                CreateNanalCalendar.DeleteCalendar(AllInOneActivity.this.getApplicationContext(), connectId);
                CreateNanalCalendar.DeleteColors(AllInOneActivity.this.getApplicationContext(), connectId);
                Toast.makeText(AllInOneActivity.this.getApplicationContext(), "삭제", Toast.LENGTH_LONG).show();
                break;
        }
        mDrawerLayout.closeDrawers();
        return false;
    }


    private void handleDynamicLink() {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        Uri deepLink = null;
                        if (pendingDynamicLinkData == null) {
                            Log.d(TAG, "No have dynamic link");
                            return;
                        } else {
                            deepLink = pendingDynamicLinkData.getLink();
                        }

                        Log.d(TAG, "deepLink: " + deepLink);

                        String segment = deepLink.getLastPathSegment();
                        switch (segment) {
                            case "nanal":
                                String code = deepLink.getQueryParameter("groupId");
                                showGroupDialog(code);
                                break;
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "getDynamicLink:onFailure", e);
                    }
                });
    }

    private void showGroupDialog(String code) {
        Intent intent = new Intent(this, GroupInvitation.class);
        intent.putExtra("groupId", code);
        startActivity(intent);
    }

    /**
     * Sets the offset of the controls on the right for animating them off/on
     * screen. ProGuard strips this if it's not in proguard.flags
     * off/on 스크린 애니메이팅하기 위하여 우측에 있는 컨트롤러의 오프셋을 설정함
     * proguard.flags가 아니라면, ProGuard가 이것을 벗겨냄 뭐임
     *
     * @param controlsOffset The current offset in pixels
     */
    public void setControlsOffset(int controlsOffset) {
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mMiniMonth.setTranslationX(controlsOffset);
            mCalendarsList.setTranslationX(controlsOffset);
            mControlsParams.width = Math.max(0, mControlsAnimateWidth - controlsOffset);
            mMiniMonthContainer.setLayoutParams(mControlsParams);
        } else {
            mMiniMonth.setTranslationY(controlsOffset);
            mCalendarsList.setTranslationY(controlsOffset);
            if (mVerticalControlsParams == null) {
                mVerticalControlsParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, mControlsAnimateHeight);
            }
            mVerticalControlsParams.height = Math.max(0, mControlsAnimateHeight - controlsOffset);
            mMiniMonthContainer.setLayoutParams(mVerticalControlsParams);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(GeneralPreferences.KEY_WEEK_START_DAY) || key.equals(GeneralPreferences.KEY_DAYS_PER_WEEK)) {
            if (mPaused) {
                mUpdateOnResume = true;
            } else {
                initFragments(mController.getTime(), mController.getViewType(), null);
            }
        }
    }

    public void setAgainFAB(boolean isGroupMenu) {
        if (!isGroupMenu) {
            mAddCalendar.setVisibility(View.VISIBLE);
            mAddDiary.setVisibility(View.VISIBLE);
        } else {
            mAddCalendar.setVisibility(View.GONE);
            mAddDiary.setVisibility(View.GONE);
        }
    }

    // 월-주-일 화면 전환 메소드인 듯!!
    private void setMainPane(
            FragmentTransaction ft, int viewId, int viewType, long timeMillis, boolean force) {
        if (mOnSaveInstanceStateCalled) {
            return;
        }
        if (!force && mCurrentView == viewType) {
            return;
        }

        // Remove this when transition to and from month view looks fine.
        // 월 view로 전환하거나 월 view에서 전환이 양호해 보이면 이 항목을 제거하기
        boolean doTransition = viewType != ViewType.MONTH && mCurrentView != ViewType.MONTH;
        FragmentManager fragmentManager = getFragmentManager();
        // Check if our previous view was an Agenda view
        // 이전 view가 Agenda view였는지 확인함
        // TODO remove this if framework ever supports nested fragments
        if (mCurrentView == ViewType.AGENDA) {
            // If it was, we need to do some cleanup on it to prevent the
            // edit/delete buttons from coming back on a rotation.
            // 맞다면, 편집/삭제 버튼이 교체되는? 것을 막기 위해 정리가 필요함
            Fragment oldFrag = fragmentManager.findFragmentById(viewId);
            if (oldFrag instanceof AgendaFragment) {
                ((AgendaFragment) oldFrag).removeFragments(fragmentManager);
            }
        }

        if (viewType != mCurrentView) {
            // The rules for this previous view are different than the
            // controller's and are used for intercepting the back button.
            // 이 previous view에 대한 규칙은 컨트롤러와 다르며, back 버튼을 막기 위해 사용함
            if (mCurrentView != ViewType.EDIT && mCurrentView > 0) {
                mPreviousView = mCurrentView;
            }
            mCurrentView = viewType;
        }
        // Create new fragment
        Fragment frag = null;
        Fragment secFrag = null;
        switch (viewType) {
            case ViewType.AGENDA:
                //mNavigationView.getMenu().findItem(R.id.agenda_menu_item).setChecked(true);
                isRequiredMenu = true;
                frag = new AgendaFragment(timeMillis, false);
                mToolbar.setNavigationIcon(R.drawable.ic_menu_navigator);
                if (mIsTabletConfig) {
                    mToolbar.setTitle(R.string.agenda_view);
                }
                setAgainFAB(false);
                mFAB.setVisibility(View.VISIBLE);
                mFABGroup.setVisibility(View.GONE);
                isInGroupDetail = false;
                break;
            case ViewType.DAY:
                //mNavigationView.getMenu().findItem(R.id.day_menu_item).setChecked(true);
                isRequiredMenu = true;
                frag = new DayFragment(timeMillis, 1);
                mToolbar.setNavigationIcon(R.drawable.ic_menu_navigator);
                if (mIsTabletConfig) {
                    mToolbar.setTitle(R.string.day_view);
                }
                setAgainFAB(false);
                mFAB.setVisibility(View.VISIBLE);
                mFABGroup.setVisibility(View.GONE);
                isInGroupDetail = false;
                break;
            case ViewType.MONTH:
                //mNavigationView.getMenu().findItem(R.id.month_menu_item).setChecked(true);
                isRequiredMenu = true;
                mToolbar.setNavigationIcon(R.drawable.ic_menu_navigator);
                frag = new MonthByWeekFragment(timeMillis, false);
                if (mShowAgendaWithMonth) {
                    secFrag = new AgendaFragment(timeMillis, false);
                }
                if (mIsTabletConfig) {
                    mToolbar.setTitle(R.string.month_view);
                }
                setAgainFAB(false);
                mFAB.setVisibility(View.VISIBLE);
                mFABGroup.setVisibility(View.GONE);
                isInGroupDetail = false;
                break;
            case ViewType.TODAY:
                isRequiredMenu = false;
                mToolbar.setNavigationIcon(R.drawable.ic_menu_navigator);
                mToolbar.setTitle(R.string.today_view);
                frag = new TodayFragment(timeMillis);
                mFAB.setVisibility(View.GONE);
                mFABGroup.setVisibility(View.GONE);
                isInGroupDetail = false;
                break;
            case ViewType.GROUP:
                isRequiredMenu = false;
                mToolbar.setTitle(R.string.group_view);
                mToolbar.setNavigationIcon(R.drawable.ic_menu_navigator);
                frag = new GroupFragment();
                setAgainFAB(true);
                mFAB.setVisibility(View.GONE);
                mFABGroup.setVisibility(View.VISIBLE);
                isInGroupDetail = false;
                break;
            case ViewType.GROUP_DETAIL:
                isRequiredMenu = false;
                if (mGroupId < 0 || mGroupName == "" || mGroupName.isEmpty()) {
                    return;
                }
                mToolbar.setTitle("그룹 > " + mGroupName);
                mToolbar.setNavigationIcon(R.drawable.ic_settings_black_24dp);
                frag = new GroupDetailFragment(mGroupId);
                setAgainFAB(false);
                mFAB.setVisibility(View.GONE);
                mFABGroup.setVisibility(View.GONE);
                isInGroupDetail = true;
                break;
            case ViewType.WEEK:
            default:
                isRequiredMenu = true;
                //mNavigationView.getMenu().findItem(R.id.week_menu_item).setChecked(true);
                setAgainFAB(false);
                mToolbar.setNavigationIcon(R.drawable.ic_menu_navigator);
                frag = new DayFragment(timeMillis, Utils.getDaysPerWeek(this));
                if (mIsTabletConfig) {
                    mToolbar.setTitle(R.string.week_view);
                }
                mFAB.setVisibility(View.VISIBLE);
                mFABGroup.setVisibility(View.GONE);
                isInGroupDetail = false;
                break;
        }
        // Update the current view so that the menu can update its look according to the
        // current view.
        // 메뉴가 현재 view에 따라 모양을 업데이트할 수 있도록 현재 view 업데이트함
        if (mCalendarToolbarHandler != null) {
            mCalendarToolbarHandler.setCurrentMainView(viewType);
        }

        if (!mIsTabletConfig) {
            refreshActionbarTitle(timeMillis);
        }

        setVisiblityMenuInGroup();

        // Show date only on tablet configurations in views different than Agenda
        // Agenda와 다른 view의 태블릿 구성에만 날짜 표시함
        if (!mIsTabletConfig) {
            mDateRange.setVisibility(View.GONE);
        } else if (viewType != ViewType.AGENDA) {
            mDateRange.setVisibility(View.VISIBLE);
        } else {
            mDateRange.setVisibility(View.GONE);
        }

        // Clear unnecessary buttons from the option menu when switching from the agenda view
        // Agenda view에서 전환할 때 옵션 메뉴의 불필요한 버튼 지움
        if (viewType != ViewType.AGENDA) {
            clearOptionsMenu();
        }

        boolean doCommit = false;
        if (ft == null) {
            doCommit = true;
            ft = fragmentManager.beginTransaction();
        }

        if (doTransition) {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }

        ft.replace(viewId, frag);
        if (mShowAgendaWithMonth) {

            // Show/hide secondary fragment

            if (secFrag != null) {
                ft.replace(R.id.secondary_pane, secFrag);
                mSecondaryPane.setVisibility(View.VISIBLE);
            } else {
                mSecondaryPane.setVisibility(View.GONE);
                Fragment f = fragmentManager.findFragmentById(R.id.secondary_pane);
                if (f != null) {
                    ft.remove(f);
                }
                mController.deregisterEventHandler(R.id.secondary_pane);
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Adding handler with viewId " + viewId + " and type " + viewType);
        }
        // If the key is already registered this will replace it
        // key가 이미 등록되어 있다면 이 키로 대체함
        mController.registerEventHandler(viewId, (EventHandler) frag);
        if (secFrag != null) {
            mController.registerEventHandler(viewId, (EventHandler) secFrag);
        }

        if (doCommit) {
            if (DEBUG) {
                Log.d(TAG, "setMainPane AllInOne=" + this + " finishing:" + this.isFinishing());
            }
            ft.commit();
        }
    }

    private void refreshActionbarTitle(long timeMillis) {
        if (mCalendarToolbarHandler != null) {
            mCalendarToolbarHandler.setTime(timeMillis);
        }
    }

    private void setTitleInActionBar(EventInfo event) {
        setVisiblityMenuInGroup();
        if (event.eventType != EventType.UPDATE_TITLE) {
            return;
        }

        final long start = event.startTime.toMillis(false /* use isDst */);
        final long end;
        if (event.endTime != null) {
            end = event.endTime.toMillis(false /* use isDst */);
        } else {
            end = start;
        }

        final String msg = Utils.formatDateRange(this, start, end, (int) event.extraLong);
        CharSequence oldDate = mDateRange.getText();
        mDateRange.setText(msg);
        updateSecondaryTitleFields(event.selectedTime != null ? event.selectedTime.toMillis(true)
                : start);
        if (!TextUtils.equals(oldDate, msg)) {
            mDateRange.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            if (mShowWeekNum && mWeekTextView != null) {
                mWeekTextView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        }
    }

    private void updateSecondaryTitleFields(long visibleMillisSinceEpoch) {
        mShowWeekNum = Utils.getShowWeekNumber(this);
        mTimeZone = Utils.getTimeZone(this, mHomeTimeUpdater);
        if (visibleMillisSinceEpoch != -1) {
            int weekNum = Utils.getWeekNumberFromTime(visibleMillisSinceEpoch, this);
            mWeekNum = weekNum;
        }

        if (mShowWeekNum && (mCurrentView == ViewType.WEEK) && mIsTabletConfig
                && mWeekTextView != null) {
            String weekString = getResources().getQuantityString(R.plurals.weekN, mWeekNum,
                    mWeekNum);
            mWeekTextView.setText(weekString);
            mWeekTextView.setVisibility(View.VISIBLE);
        } else if (visibleMillisSinceEpoch != -1 && mWeekTextView != null
                && mCurrentView == ViewType.DAY && mIsTabletConfig) {
            Time time = new Time(mTimeZone);
            time.set(visibleMillisSinceEpoch);
            int julianDay = Time.getJulianDay(visibleMillisSinceEpoch, time.gmtoff);
            time.setToNow();
            int todayJulianDay = Time.getJulianDay(time.toMillis(false), time.gmtoff);
            String dayString = Utils.getDayOfWeekString(julianDay, todayJulianDay,
                    visibleMillisSinceEpoch, this);
            mWeekTextView.setText(dayString);
            mWeekTextView.setVisibility(View.VISIBLE);
        } else if (mWeekTextView != null && (!mIsTabletConfig || mCurrentView != ViewType.DAY)) {
            mWeekTextView.setVisibility(View.GONE);
        }

        if (mHomeTime != null
                && (mCurrentView == ViewType.DAY || mCurrentView == ViewType.WEEK
                || mCurrentView == ViewType.AGENDA)
                && !TextUtils.equals(mTimeZone, Time.getCurrentTimezone())) {
            Time time = new Time(mTimeZone);
            time.setToNow();
            long millis = time.toMillis(true);
            boolean isDST = time.isDst != 0;
            int flags = DateUtils.FORMAT_SHOW_TIME;
            if (DateFormat.is24HourFormat(this)) {
                flags |= DateUtils.FORMAT_24HOUR;
            }
            // Formats the time as
            String timeString = (new StringBuilder(
                    Utils.formatDateRange(this, millis, millis, flags))).append(" ").append(
                    TimeZone.getTimeZone(mTimeZone).getDisplayName(
                            isDST, TimeZone.SHORT, Locale.getDefault())).toString();
            mHomeTime.setText(timeString);
            mHomeTime.setVisibility(View.VISIBLE);
            // Update when the minute changes
            mHomeTime.removeCallbacks(mHomeTimeUpdater);
            mHomeTime.postDelayed(
                    mHomeTimeUpdater,
                    DateUtils.MINUTE_IN_MILLIS - (millis % DateUtils.MINUTE_IN_MILLIS));
        } else if (mHomeTime != null) {
            mHomeTime.setVisibility(View.GONE);
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.GO_TO | EventType.VIEW_EVENT | EventType.UPDATE_TITLE;
    }

    @Override
    public long getSupportedDiaryTypes() {
        return EventType.GO_TO | EventType.VIEW_DIARY | EventType.UPDATE_TITLE;
    }

    @Override
    public void handleEvent(EventInfo event) {
        long displayTime = -1;
        if (event.eventType == EventType.GO_TO) {
            if ((event.extraLong & CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS) != 0) {
                mBackToPreviousView = true;
            } else if (event.viewType != mController.getPreviousViewType()
                    && event.viewType != ViewType.EDIT) {
                // Clear the flag is change to a different view type
                // 다른 view type으로 변경되었기 때문에 clear
                mBackToPreviousView = false;
            }

            setMainPane(
                    null, R.id.main_pane, event.viewType, event.startTime.toMillis(false), false);
            if (mSearchView != null) {
                mSearchView.clearFocus();
            }
            if (mShowCalendarControls) {
                int animationSize = (mOrientation == Configuration.ORIENTATION_LANDSCAPE) ?
                        mControlsAnimateWidth : mControlsAnimateHeight;
                boolean noControlsView = event.viewType == ViewType.MONTH || event.viewType == ViewType.AGENDA;
                if (mControlsMenu != null) {
                    mControlsMenu.setVisible(!noControlsView);
                    mControlsMenu.setEnabled(!noControlsView);
                }
                if (noControlsView || mHideControls) {
                    // hide minimonth and calendar frag
                    mShowSideViews = false;
                    if (!mHideControls) {
                        final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this,
                                "controlsOffset", 0, animationSize);
                        slideAnimation.addListener(mSlideAnimationDoneListener);
                        slideAnimation.setDuration(mCalendarControlsAnimationTime);
                        ObjectAnimator.setFrameDelay(0);
                        slideAnimation.start();
                    } else {
                        mMiniMonth.setVisibility(View.GONE);
                        mCalendarsList.setVisibility(View.GONE);
                        mMiniMonthContainer.setVisibility(View.GONE);
                    }
                } else {
                    // show minimonth and calendar frag
                    mShowSideViews = true;
                    mMiniMonth.setVisibility(View.VISIBLE);
                    mCalendarsList.setVisibility(View.VISIBLE);
                    mMiniMonthContainer.setVisibility(View.VISIBLE);
                    if (!mHideControls &&
                            (mController.getPreviousViewType() == ViewType.MONTH ||
                                    mController.getPreviousViewType() == ViewType.AGENDA)) {
                        final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this,
                                "controlsOffset", animationSize, 0);
                        slideAnimation.setDuration(mCalendarControlsAnimationTime);
                        ObjectAnimator.setFrameDelay(0);
                        slideAnimation.start();
                    }
                }
            }
            updateViewSettingsVisibility();
            setVisiblityMenuInGroup();
            displayTime = event.selectedTime != null ? event.selectedTime.toMillis(true)
                    : event.startTime.toMillis(true);
            if (!mIsTabletConfig) {
                refreshActionbarTitle(displayTime);
            }
        } else if (event.eventType == EventType.VIEW_EVENT) {

            // If in Agenda view and "show_event_details_with_agenda" is "true",
            // do not create the event info fragment here, it will be created by the Agenda
            // fragment
            // Agenda view에서 "show_..._agenda"가 "true"인 경우, 여기에 이벤트 정보 fragment 작성하지 않음
            // Agenda fragment에서 만들어질 것임

            if (mCurrentView == ViewType.AGENDA && mShowEventDetailsWithAgenda) {
                if (event.startTime != null && event.endTime != null) {
                    // Event is all day , adjust the goto time to local time
                    // 종일 이벤트이며, goto time을 로컬 시간으로 조정함
                    if (event.isAllDay()) {
                        Utils.convertAlldayUtcToLocal(
                                event.startTime, event.startTime.toMillis(false), mTimeZone);
                        Utils.convertAlldayUtcToLocal(
                                event.endTime, event.endTime.toMillis(false), mTimeZone);
                    }
                    mController.sendEvent(this, EventType.GO_TO, event.startTime, event.endTime,
                            event.selectedTime, event.id, ViewType.AGENDA,
                            CalendarController.EXTRA_GOTO_TIME, null, null);
                } else if (event.selectedTime != null) {
                    mController.sendEvent(this, EventType.GO_TO, event.selectedTime,
                            event.selectedTime, event.id, ViewType.AGENDA);
                }
            } else {
                // TODO Fix the temp hack below: && mCurrentView !=
                // ViewType.AGENDA
                if (event.selectedTime != null && mCurrentView != ViewType.AGENDA) {
                    mController.sendEvent(this, EventType.GO_TO, event.selectedTime,
                            event.selectedTime, -1, ViewType.CURRENT);
                }
                int response = event.getResponse();
                if ((mCurrentView == ViewType.AGENDA && mShowEventInfoFullScreenAgenda) ||
                        ((mCurrentView == ViewType.DAY || (mCurrentView == ViewType.WEEK) ||
                                mCurrentView == ViewType.MONTH) && mShowEventInfoFullScreen)) {
                    // start event info as activity
                    // activity로 이벤트 정보 시작
                    // *** 이벤트 상세 정보 ***
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, event.id);
                    intent.setData(eventUri);
                    intent.setClass(this, EventInfoActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(EXTRA_EVENT_BEGIN_TIME, event.startTime.toMillis(false));
                    intent.putExtra(EXTRA_EVENT_END_TIME, event.endTime.toMillis(false));
                    intent.putExtra(ATTENDEE_STATUS, response);
                    startActivity(intent);
                } else {
                    // start event info as a dialog
                    // dialog로 이벤트 정보 시작
                    EventInfoFragment fragment = new EventInfoFragment(this,
                            event.id, event.startTime.toMillis(false),
                            event.endTime.toMillis(false), response, true,
                            EventInfoFragment.DIALOG_WINDOW_STYLE,
                            null /* No reminders to explicitly pass in. */);
                    fragment.setDialogParams(event.x, event.y, mActionBar.getHeight());
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    // if we have an old popup replace it
                    // 예전 팝업이 있다면 교체함
                    Fragment fOld = fm.findFragmentByTag(EVENT_INFO_FRAGMENT_TAG);
                    if (fOld != null && fOld.isAdded()) {
                        ft.remove(fOld);
                    }
                    ft.add(fragment, EVENT_INFO_FRAGMENT_TAG);
                    ft.commit();
                }
            }
            displayTime = event.startTime.toMillis(true);
        } else if (event.eventType == EventType.UPDATE_TITLE) {
            setTitleInActionBar(event);
            setVisiblityMenuInGroup();
            if (!mIsTabletConfig) {
                refreshActionbarTitle(mController.getTime());
            }
        }
        updateSecondaryTitleFields(displayTime);
    }

    @Override
    public void handleEvent(CalendarController.DiaryInfo diary) {
        long displayTime = -1;
        if (diary.eventType == EventType.GO_TO) {
            if (diary.viewType != mController.getPreviousViewType()
                    && diary.viewType != ViewType.EDIT) {
                mBackToPreviousView = false;
            }

            setMainPane(null, R.id.main_pane, diary.viewType, diary.day, false);
            if (mSearchView != null) {
                mSearchView.clearFocus();
            }
            if (mShowCalendarControls) {
                int animationSize = (mOrientation == Configuration.ORIENTATION_LANDSCAPE) ?
                        mControlsAnimateWidth : mControlsAnimateHeight;
                boolean noControlsView = diary.viewType == ViewType.MONTH || diary.viewType == ViewType.AGENDA;
                if (mControlsMenu != null) {
                    mControlsMenu.setVisible(!noControlsView);
                    mControlsMenu.setEnabled(!noControlsView);
                }

                if (noControlsView || mHideControls) {
                    // hide minimonth and calendar frag
                    mShowSideViews = false;
                    if (!mHideControls) {
                        final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this,
                                "controlsOffset", 0, animationSize);
                        slideAnimation.addListener(mSlideAnimationDoneListener);
                        slideAnimation.setDuration(mCalendarControlsAnimationTime);
                        ObjectAnimator.setFrameDelay(0);
                        slideAnimation.start();
                    } else {
                        mMiniMonth.setVisibility(View.GONE);
                        mCalendarsList.setVisibility(View.GONE);
                        mMiniMonthContainer.setVisibility(View.GONE);
                    }
                } else {
                    // show minimonth and calendar frag
                    mShowSideViews = true;
                    mMiniMonth.setVisibility(View.VISIBLE);
                    mCalendarsList.setVisibility(View.VISIBLE);
                    mMiniMonthContainer.setVisibility(View.VISIBLE);
                    if (!mHideControls &&
                            (mController.getPreviousViewType() == ViewType.MONTH ||
                                    mController.getPreviousViewType() == ViewType.AGENDA)) {
                        final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this,
                                "controlsOffset", animationSize, 0);
                        slideAnimation.setDuration(mCalendarControlsAnimationTime);
                        ObjectAnimator.setFrameDelay(0);
                        slideAnimation.start();
                    }
                }
            }
            updateViewSettingsVisibility();
            setVisiblityMenuInGroup();
        } else if (diary.eventType == EventType.VIEW_DIARY) {
            if ((mCurrentView == ViewType.AGENDA && mShowEventInfoFullScreenAgenda) ||
                    ((mCurrentView == ViewType.DAY || (mCurrentView == ViewType.WEEK) ||
                            mCurrentView == ViewType.MONTH) && mShowEventInfoFullScreen)) {
                // start event info as activity
                // activity로 이벤트 정보 시작
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.putExtra("diary_id", diary.id);
                intent.setClass(this, EventInfoActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } else {
                // start event info as a dialog
                // dialog로 이벤트 정보 시작
//                EventInfoFragment fragment = new EventInfoFragment(this,
//                        diary.id, event.startTime.toMillis(false),
//                        event.endTime.toMillis(false), response, true,
//                        EventInfoFragment.DIALOG_WINDOW_STYLE,
//                        null /* No reminders to explicitly pass in. */);
//                fragment.setDialogParams(diary.x, diary.y, mActionBar.getHeight());
//                FragmentManager fm = getFragmentManager();
//                FragmentTransaction ft = fm.beginTransaction();
//                // if we have an old popup replace it
//                // 예전 팝업이 있다면 교체함
//                Fragment fOld = fm.findFragmentByTag(EVENT_INFO_FRAGMENT_TAG);
//                if (fOld != null && fOld.isAdded()) {
//                    ft.remove(fOld);
//                }
//                ft.add(fragment, EVENT_INFO_FRAGMENT_TAG);
//                ft.commit();
            }
        } else if (diary.eventType == EventType.UPDATE_TITLE) {
            setVisiblityMenuInGroup();
            if (!mIsTabletConfig) {
                refreshActionbarTitle(mController.getTime());
            }
        }
        updateSecondaryTitleFields(displayTime);
    }

    // Needs to be in proguard whitelist
    // Specified as listener via android:onClick in a layout xml
    // proguard whitelist에 있어야 함
    // xml 레이아웃의 android:onClick를 통해 listener로 지정됨
    public void handleSelectSyncedCalendarsClicked(View v) {
        mController.sendEvent(this, EventType.LAUNCH_SETTINGS, null, null, null, 0, 0,
                CalendarController.EXTRA_GOTO_TIME, null,
                null);
    }

    @Override
    public void eventsChanged() {
        mController.sendEvent(this, EventType.EVENTS_CHANGED, null, null, -1, ViewType.CURRENT);
    }

    @Override
    public void diariesChanged() {
        mController.sendEvent(this, EventType.DIARIES_CHANGED, null, null, -1, ViewType.CURRENT);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchMenu.collapseActionView();
        mController.sendEvent(this, EventType.SEARCH, null, null, -1, ViewType.CURRENT, 0, query,
                getComponentName());
        return true;
    }

    //@Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        switch (itemPosition) {
            case CalendarViewAdapter.DAY_BUTTON_INDEX:
                if (mCurrentView != ViewType.DAY) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY);
                }
                break;
            case CalendarViewAdapter.WEEK_BUTTON_INDEX:
                if (mCurrentView != ViewType.WEEK) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.WEEK);
                }
                break;
            case CalendarViewAdapter.MONTH_BUTTON_INDEX:
                if (mCurrentView != ViewType.MONTH) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
                }
                break;
            case CalendarViewAdapter.AGENDA_BUTTON_INDEX:
                if (mCurrentView != ViewType.AGENDA) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.AGENDA);
                }
                break;
            default:
                Log.w(TAG, "ItemSelected event from unknown button: " + itemPosition);
                /*Log.w(TAG, "CurrentView:" + mCurrentView + " Button:" + itemPosition +
                        " Day:" + mDayTab + " Week:" + mWeekTab + " Month:" + mMonthTab +
                        " Agenda:" + mAgendaTab);*/
                break;
        }
        return false;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        mSearchMenu.collapseActionView();
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        if (mSearchMenu != null) {
            mSearchMenu.expandActionView();
        }
        return false;
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            //mCheckForAccounts = false;
            try {
                // If the query didn't return a cursor for some reason return
                // 쿼리가 어떤 이유로 인해 커서를 반환하지 않은 경우
                if (cursor == null || cursor.getCount() > 0 || isFinishing()) {
                    return;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            Bundle options = new Bundle();
            options.putCharSequence("introMessage",
                    getResources().getString(R.string.create_an_account_desc));
            options.putBoolean("allowSkip", true);

            AccountManager am = AccountManager.get(AllInOneActivity.this);
            am.addAccount("com.google", CalendarContract.AUTHORITY, null, options,
                    AllInOneActivity.this,
                    new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> future) {
                            if (future.isCancelled()) {
                                return;
                            }
                            try {
                                Bundle result = future.getResult();
                                boolean setupSkipped = result.getBoolean("setupSkipped");

                                if (setupSkipped) {
                                    Utils.setSharedPreference(AllInOneActivity.this,
                                            GeneralPreferences.KEY_SKIP_SETUP, true);
                                }

                            } catch (OperationCanceledException | IOException | AuthenticatorException ignore) {
                                // The account creation process was canceled
                                // 계정 생성 프로세스가 취소됨
                            }
                        }
                    }, null);
        }
    }
}

