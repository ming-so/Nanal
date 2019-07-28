package com.android.nanal;

import android.content.ComponentName;
import android.content.Context;
import android.text.format.Time;
import android.util.Pair;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.WeakHashMap;

public class CalendarController {
    public static final String EVENT_EDIT_ON_LAUNCH = "editmode";
    public static final int MIN_CALENDAR_YEAR = 1970;
    public static final int MAX_CALENDAR_YEAR = 2036;
    public static final int MIN_CALENDAR_WEEK = 0;
    public static final int MAX_CALENDAR_WEEK = 3497; // weeks between 1/1/1970 and 1/1/2037

    // 종일 이벤트를 생성하려면 EventType.CREATE_EVENT에 대한 ExtraLong 매개변수 전달
    public static final long EXTRA_CREATE_ALL_DAY = 0x10;
    // 시간을 표시하려면 EventType.GO_TO에 대한 ExtraLong 매개변수 전달
    public static final long EXTRA_GOTO_DATE = 1;
    public static final long EXTRA_GOTO_TIME = 2;
    public static final long EXTRA_GOTO_BACK_TO_PREVIOUS = 4;
    public static final long EXTRA_GOTO_TODAY = 8;
    private static final boolean DEBUG = false;
    private static final String TAG = "CalendarController";
    private static WeakHashMap<Context, WeakReference<CalendarController>> instances =
            new WeakHashMap<Context, WeakReference<CalendarController>>();
    private final Context mContext;
    // LinkedHashMap을 사용하여 핸들러에 대한 참조(reference)를 찾을 수 있다고 보장할 수 없기 때문에
    // 확장되고 있는 view ID에 따라 fragment들을 교체할 수 있음
    private final LinkedHashMap<Integer,EventHandler> eventHandlers =
            new LinkedHashMap<Integer,EventHandler>(5);
    private final LinkedList<Integer> mToBeRemovedEventHandlers = new LinkedList<Integer>();
    private final LinkedHashMap<Integer, EventHandler> mToBeAddedEventHandlers = new LinkedHashMap<
            Integer, EventHandler>();
    private final WeakHashMap<Object, Long> filters = new WeakHashMap<Object, Long>(1);
    private final Time mTime = new Time();
    private final Runnable mUpdateTimezone = new Runnable() {
        @Override
        public void run() {
            mTime.switchTimezone(Utils.getTimeZone(mContext, this));
        }
    };
    private Pair<Integer, EventHandler> mFirstEventHandler;
    private Pair<Integer, EventHandler> mToBeAddedFirstEventHandler;
    private volatile int mDispatchInProgressCounter = 0;
    private int mViewType = -1;
    private int mDetailViewType = -1;
    private int mPreviousViewType = -1;
    private long mEventId = -1;
    private long mDiaryId = -1;
    private long mDateFlags = 0;

    private CalendarController(Context context) {
        mContext = context;
        mUpdateTimezone.run();
        mTime.setToNow();
        mDetailViewType = Utils.getSharedPreference(mContext,
                GeneralPreferences.KEY_DETAILED_VIEW,
                GeneralPreferences.DEFAULT_DETAILED_VIEW);
    }

    public static CalendarController getInstance(Context context) {
        // 제공된 context와 연결된 CalendarController 인스턴스 생성 및/또는 반환
        // 현재 Activity에서 전달하는 게 가장 좋음
        synchronized (instances) {
            CalendarController controller = null;
            WeakReference<CalendarController> weakController = instances.get(context);
            if (weakController != null) {
                controller = weakController.get();
            }

            if (controller == null) {
                controller = new CalendarController(context);
                instances.put(context, new WeakReference(controller));
            }
            return controller;
        }
    }

    public static void removeInstance(Context context) {
        /**
         * Removes an instance when it is no longer needed. This should be called in
         * an activity's onDestroy method.
         * 더 이상 필요하지 않을 때 인스턴스를 제거함
         * activity의 onDestroy 메소드로 호출되어야 함
         *
         * @param context The activity used to create the controller
         *                컨트롤러를 생성하는 데 사용되는 Activity
         */
        instances.remove(context);
    }

    public interface ViewType {
        // Agenda/일/주/월 view type 중 하나
        final int DETAIL = -1;
        final int CURRENT = 0;
        final int AGENDA = 1;
        final int DAY = 2;
        final int WEEK = 3;
        final int MONTH = 4;
        final int EDIT = 5;
        final int MAX_VALUE = 5;
    }

    public interface EventHandler {
        long getSupportedEventTypes();

        void handleEvent(EventInfo event);

        // 데이터베이스가 변경되었음을 핸들러에게 알리고, view를 업데이트해야 함
        void eventsChanged();
    }

    public static class EventInfo {

    }

    public static class DiaryInfo {
        public long id; // 일기 ID
        public long userId; // 유저 ID
        public long groupId;

        public int x;
        public int y;
        public Time createTime;
        public String query;
        public ComponentName componentName;

        public String img;
        public String contents;
        public String weather;
        public String location;

        public boolean isInGroup() {
            if (groupId <= 0) {
                return false;
            }
            return true;
        }
    }
}
