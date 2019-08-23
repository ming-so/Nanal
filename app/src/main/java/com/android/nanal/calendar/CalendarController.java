// 다 가져옴
package com.android.nanal.calendar;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;

import com.android.nanal.activity.AlertActivity;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.activity.CalendarSettingsActivity;
import com.android.nanal.activity.EditEventActivity;
import com.android.nanal.activity.SelectVisibleCalendarsActivity;
import com.android.nanal.activity.SettingsActivity;
import com.android.nanal.event.DeleteEventHelper;
import com.android.nanal.event.GeneralPreferences;
import com.android.nanal.event.Utils;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS;
import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;

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

    /**
     * Creates and/or returns an instance of CalendarController associated with
     * the supplied context. It is best to pass in the current Activity.
     * 제공된 context와 연결된 CalendarController 인스턴스 생성 및/또는 반환
     * 현재 Activity에서 전달하는 게 가장 좋음
     *
     * @param context The activity if at all possible.
     */
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

    /**
     * Removes an instance when it is no longer needed. This should be called in
     * an activity's onDestroy method.
     * 더 이상 필요하지 않을 때 인스턴스를 제거함
     * activity의 onDestroy 메소드로 호출되어야 함
     *
     * @param context The activity used to create the controller
     *                컨트롤러를 생성하는 데 사용되는 Activity
     */
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

    public void sendEventRelatedEvent(Object sender, long eventType, long eventId, long startMillis,
                                      long endMillis, int x, int y, long selectedMillis) {
        // TODO: pass the real allDay status or at least a status that says we don't know the
        // status and have the receiver query the data.
        // The current use of this method for VIEW_EVENT is by the day view to show an EventInfo
        // so currently the missing allDay status has no effect.
        sendEventRelatedEventWithExtra(sender, eventType, eventId, startMillis, endMillis, x, y,
                EventInfo.buildViewExtraLong(Attendees.ATTENDEE_STATUS_NONE, false),
                selectedMillis);
    }


    /**
     * Helper for sending New/View/Edit/Delete events
     * 등록/보기/편집/삭제 이벤트 전송 헬퍼
     *
     * @param sender object of the caller
     *               caller의 오브젝트
     * @param eventType one of {@link EventType}
     *                  EventType 중 하나
     * @param eventId event id
     *                이벤트 ID
     * @param startMillis start time
     *                    시작 시간
     * @param endMillis end time
     *                  종료 시간
     * @param x x coordinate in the activity space
     *          activity 공간의 x좌표
     * @param y y coordinate in the activity space
     *          activity 공간의 y좌표
     * @param extraLong default response value for the "simple event view" and all day indication.
     *        Use Attendees.ATTENDEE_STATUS_NONE for no response.
     *                  "심플 이벤트 뷰" 및 종일 표시?에 대한 기본 응답 값
     *                  응답 없음은 저거 사용
     * @param selectedMillis The time to specify as selected
     *                       선택한 대로 지정할 시간
     */
    public void sendEventRelatedEventWithExtra(Object sender, long eventType, long eventId,
                                               long startMillis, long endMillis, int x, int y, long extraLong, long selectedMillis) {
        sendEventRelatedEventWithExtraWithTitleWithCalendarId(sender, eventType, eventId,
                startMillis, endMillis, x, y, extraLong, selectedMillis, null, -1);
    }


    /**
     * Helper for sending New/View/Edit/Delete events
     *
     * @param sender object of the caller
     * @param eventType one of {@link EventType}
     * @param eventId event id
     * @param startMillis start time
     * @param endMillis end time
     * @param x x coordinate in the activity space
     * @param y y coordinate in the activity space
     * @param extraLong default response value for the "simple event view" and all day indication.
     *        Use Attendees.ATTENDEE_STATUS_NONE for no response.
     * @param selectedMillis The time to specify as selected
     * @param title The title of the event
     *              이벤트의 제목
     * @param calendarId The id of the calendar which the event belongs to
     *                   이벤트가 속한 캘린더 ID
     */
    public void sendEventRelatedEventWithExtraWithTitleWithCalendarId(Object sender, long eventType,
                                                                      long eventId, long startMillis, long endMillis, int x, int y, long extraLong,
                                                                      long selectedMillis, String title, long calendarId) {
        EventInfo info = new EventInfo();
        info.eventType = eventType;
        if (eventType == EventType.EDIT_EVENT || eventType == EventType.VIEW_EVENT_DETAILS) {
            info.viewType = ViewType.CURRENT;
        }

        info.id = eventId;
        info.startTime = new Time(Utils.getTimeZone(mContext, mUpdateTimezone));
        info.startTime.set(startMillis);
        if (selectedMillis != -1) {
            info.selectedTime = new Time(Utils.getTimeZone(mContext, mUpdateTimezone));
            info.selectedTime.set(selectedMillis);
        } else {
            info.selectedTime = info.startTime;
        }
        info.endTime = new Time(Utils.getTimeZone(mContext, mUpdateTimezone));
        info.endTime.set(endMillis);
        info.x = x;
        info.y = y;
        info.extraLong = extraLong;
        info.eventTitle = title;
        info.calendarId = calendarId;
        this.sendEvent(sender, info);
    }


    /**
     * Helper for sending non-calendar-event events
     *
     * @param sender    object of the caller
     * @param eventType one of {@link EventType}
     * @param start     start time
     * @param end       end time
     * @param eventId   event id
     * @param viewType  {@link ViewType}
     */
    public void sendEvent(Object sender, long eventType, Time start, Time end, long eventId,
                          int viewType) {
        sendEvent(sender, eventType, start, end, start, eventId, viewType, EXTRA_GOTO_TIME, null,
                null);
    }

    /**
     * sendEvent() variant with extraLong, search query, and search component name.
     * sendEvent() 변종..
     */
    public void sendEvent(Object sender, long eventType, Time start, Time end, long eventId,
                          int viewType, long extraLong, String query, ComponentName componentName) {
        sendEvent(sender, eventType, start, end, start, eventId, viewType, extraLong, query,
                componentName);
    }

    public void sendEvent(Object sender, long eventType, Time start, Time end, Time selected,
                          long eventId, int viewType, long extraLong, String query, ComponentName componentName) {
        EventInfo info = new EventInfo();
        info.eventType = eventType;
        info.startTime = start;
        info.selectedTime = selected;
        info.endTime = end;
        info.id = eventId;
        info.viewType = viewType;
        info.query = query;
        info.componentName = componentName;
        info.extraLong = extraLong;
        this.sendEvent(sender, info);
    }


    public void sendEvent(Object sender, final EventInfo event) {
        // TODO Throw exception on invalid events

        if (DEBUG) {
            Log.d(TAG, eventInfoToString(event));
        }

        Long filteredTypes = filters.get(sender);
        if (filteredTypes != null && (filteredTypes.longValue() & event.eventType) != 0) {
            // Suppress event per filter
            // 필터당 이벤트 억제
            if (DEBUG) {
                Log.d(TAG, "Event suppressed");
            }
            return;
        }

        mPreviousViewType = mViewType;

        // Fix up view if not specified
        // 지정되지 않은 경우 view 수정
        if (event.viewType == ViewType.DETAIL) {
            event.viewType = mDetailViewType;
            mViewType = mDetailViewType;
        } else if (event.viewType == ViewType.CURRENT) {
            event.viewType = mViewType;
        } else if (event.viewType != ViewType.EDIT) {
            mViewType = event.viewType;

            if (event.viewType == ViewType.AGENDA || event.viewType == ViewType.DAY
                    || (Utils.getAllowWeekForDetailView() && event.viewType == ViewType.WEEK)) {
                mDetailViewType = mViewType;
            }
        }

        if (DEBUG) {
            Log.d(TAG, "vvvvvvvvvvvvvvv");
            Log.d(TAG, "Start  " + (event.startTime == null ? "null" : event.startTime.toString()));
            Log.d(TAG, "End    " + (event.endTime == null ? "null" : event.endTime.toString()));
            Log.d(TAG, "Select " + (event.selectedTime == null ? "null" : event.selectedTime.toString()));
            Log.d(TAG, "mTime  " + (mTime == null ? "null" : mTime.toString()));
        }

        long startMillis = 0;
        if (event.startTime != null) {
            startMillis = event.startTime.toMillis(false);
        }

        // Set mTime if selectedTime is set
        // selectedTime이 설정되어 있다면 mTime 설정
        if (event.selectedTime != null && event.selectedTime.toMillis(false) != 0) {
            mTime.set(event.selectedTime);
        } else {
            if (startMillis != 0) {
                // selectedTime is not set so set mTime to startTime iff it is not
                // within start and end times
                // selectedTime이 설정되지 않았으므로 mTime이 시작 및 종료 시간 내에 있지 않은 경우,
                // mTime을 startTime으로 설정함
                long mtimeMillis = mTime.toMillis(false);
                if (mtimeMillis < startMillis
                        || (event.endTime != null && mtimeMillis > event.endTime.toMillis(false))) {
                    mTime.set(event.startTime);
                }
            }
            event.selectedTime = mTime;
        }
        // Store the formatting flags if this is an update to the title
        // 제목에 대한 업데이트인 경우, 형식 지정 flags를 저장함
        if (event.eventType == EventType.UPDATE_TITLE) {
            mDateFlags = event.extraLong;
        }

        // Fix up start time if not specified
        // 지정되지 않은 경우 시작 시간 수정
        if (startMillis == 0) {
            event.startTime = mTime;
        }
        if (DEBUG) {
            Log.d(TAG, "Start  " + (event.startTime == null ? "null" : event.startTime.toString()));
            Log.d(TAG, "End    " + (event.endTime == null ? "null" : event.endTime.toString()));
            Log.d(TAG, "Select " + (event.selectedTime == null ? "null" : event.selectedTime.toString()));
            Log.d(TAG, "mTime  " + (mTime == null ? "null" : mTime.toString()));
            Log.d(TAG, "^^^^^^^^^^^^^^^");
        }

        // Store the eventId if we're entering edit event
        // 편집 이벤트에 들어가면 eventId 저장
        if ((event.eventType
                & (EventType.CREATE_EVENT | EventType.EDIT_EVENT | EventType.VIEW_EVENT_DETAILS))
                != 0) {
            if (event.id > 0) {
                mEventId = event.id;
            } else {
                mEventId = -1;
            }
        }

        boolean handled = false;
        synchronized (this) {
            mDispatchInProgressCounter++;

            if (DEBUG) {
                Log.d(TAG, "sendEvent: Dispatching to " + eventHandlers.size() + " handlers");
            }
            // Dispatch to event handler(s)
            // 핸들러 전송?
            if (mFirstEventHandler != null) {
                // Handle the 'first' one before handling the others
                // "첫 번째" 것부터 다룸
                EventHandler handler = mFirstEventHandler.second;
                if (handler != null && (handler.getSupportedEventTypes() & event.eventType) != 0
                        && !mToBeRemovedEventHandlers.contains(mFirstEventHandler.first)) {
                    handler.handleEvent(event);
                    handled = true;
                }
            }
            for (Iterator<Entry<Integer, EventHandler>> handlers =
                 eventHandlers.entrySet().iterator(); handlers.hasNext(); ) {
                Entry<Integer, EventHandler> entry = handlers.next();
                int key = entry.getKey();
                if (mFirstEventHandler != null && key == mFirstEventHandler.first) {
                    // If this was the 'first' handler it was already handled
                    // 이것이 "첫 번째" 핸들러였다면 이미 처리됐음
                    continue;
                }
                EventHandler eventHandler = entry.getValue();
                if (eventHandler != null
                        && (eventHandler.getSupportedEventTypes() & event.eventType) != 0) {
                    if (mToBeRemovedEventHandlers.contains(key)) {
                        continue;
                    }
                    eventHandler.handleEvent(event);
                    handled = true;
                }
            }

            mDispatchInProgressCounter--;

            if (mDispatchInProgressCounter == 0) {

                // Deregister removed handlers
                // 제거된 핸들러 등록 취소
                if (mToBeRemovedEventHandlers.size() > 0) {
                    for (Integer zombie : mToBeRemovedEventHandlers) {
                        eventHandlers.remove(zombie);
                        if (mFirstEventHandler != null && zombie.equals(mFirstEventHandler.first)) {
                            mFirstEventHandler = null;
                        }
                    }
                    mToBeRemovedEventHandlers.clear();
                }
                // Add new handlers
                // 새로운 핸들러 추가
                if (mToBeAddedFirstEventHandler != null) {
                    mFirstEventHandler = mToBeAddedFirstEventHandler;
                    mToBeAddedFirstEventHandler = null;
                }
                if (mToBeAddedEventHandlers.size() > 0) {
                    for (Entry<Integer, EventHandler> food : mToBeAddedEventHandlers.entrySet()) {
                        eventHandlers.put(food.getKey(), food.getValue());
                    }
                }
            }
        }

        if (!handled) {
            // Launch Settings
            // Settings 실행
            if (event.eventType == EventType.LAUNCH_SETTINGS) {
                launchSettings();
                return;
            }

            if(event.eventType == EventType.LAUNCH_GROUP) {
                return;
            }

            if(event.eventType == EventType.LAUNCH_SETTINGS_DIRECT) {
                launchSettingsDirectly();
                return;
            }

            // Launch Calendar Visible Selector
            // Calendar Visible Selector 실행
            if (event.eventType == EventType.LAUNCH_SELECT_VISIBLE_CALENDARS) {
                launchSelectVisibleCalendars();
                return;
            }

            // Create/View/Edit/Delete Event
            // 이벤트 등록/보기/편집/삭제
            long endTime = (event.endTime == null) ? -1 : event.endTime.toMillis(false);
            if (event.eventType == EventType.CREATE_EVENT) {
                launchCreateEvent(event.startTime.toMillis(false), endTime,
                        event.extraLong == EXTRA_CREATE_ALL_DAY, event.eventTitle,
                        event.calendarId);
                return;
            } else if (event.eventType == EventType.VIEW_EVENT) {
                launchViewEvent(event.id, event.startTime.toMillis(false), endTime,
                        event.getResponse());
                return;
            } else if (event.eventType == EventType.EDIT_EVENT) {
                launchEditEvent(event.id, event.startTime.toMillis(false), endTime, true);
                return;
            } else if (event.eventType == EventType.VIEW_EVENT_DETAILS) {
                launchEditEvent(event.id, event.startTime.toMillis(false), endTime, false);
                return;
            } else if (event.eventType == EventType.DELETE_EVENT) {
                launchDeleteEvent(event.id, event.startTime.toMillis(false), endTime);
                return;
            } else if (event.eventType == EventType.SEARCH) {
                launchSearch(event.id, event.query, event.componentName);
                return;
            }
        }
    }


    /**
     * Adds or updates an event handler. This uses a LinkedHashMap so that we can
     * replace fragments based on the view id they are being expanded into.
     * 이벤트 핸들러를 추가하거나 업데이트함
     * LinkedMashMap을 사용하여 확장 중인 view ID에 따라 fragment를 교체할 수 있음
     *
     * @param key The view id or placeholder for this handler
     *             이 핸들러에 대한 view ID 또는 placeholder
     * @param eventHandler Typically a fragment or activity in the calendar app
     *                      일반적으로 캘린더 앱의 fragment 또는 activity
     */
    public void registerEventHandler(int key, EventHandler eventHandler) {
        synchronized (this) {
            if (mDispatchInProgressCounter > 0) {
                mToBeAddedEventHandlers.put(key, eventHandler);
            } else {
                eventHandlers.put(key, eventHandler);
            }
        }
    }

    public void registerFirstEventHandler(int key, EventHandler eventHandler) {
        synchronized (this) {
            registerEventHandler(key, eventHandler);
            if (mDispatchInProgressCounter > 0) {
                mToBeAddedFirstEventHandler = new Pair<Integer, EventHandler>(key, eventHandler);
            } else {
                mFirstEventHandler = new Pair<Integer, EventHandler>(key, eventHandler);
            }
        }
    }

    public void deregisterEventHandler(Integer key) {
        synchronized (this) {
            if (mDispatchInProgressCounter > 0) {
                // To avoid ConcurrencyException, stash away the event handler for now.
                // ConcurrencyException을 방지하려면, 이벤트 핸들러를 당분간 보관함
                mToBeRemovedEventHandlers.add(key);
            } else {
                eventHandlers.remove(key);
                if (mFirstEventHandler != null && mFirstEventHandler.first == key) {
                    mFirstEventHandler = null;
                }
            }
        }
    }

    public void deregisterAllEventHandlers() {
        synchronized (this) {
            if (mDispatchInProgressCounter > 0) {
                // To avoid ConcurrencyException, stash away the event handler for now.
                // ConcurrencyException을 방지하려면, 이벤트 핸들러를 당분간 보관함
                mToBeRemovedEventHandlers.addAll(eventHandlers.keySet());
            } else {
                eventHandlers.clear();
                mFirstEventHandler = null;
            }
        }
    }

    // FRAG_TODO doesn't work yet
    // FRAG_TODO 아직은 동작하지 않음
    public void filterBroadcasts(Object sender, long eventTypes) {
        filters.put(sender, eventTypes);
    }

    /**
     * @return the time that this controller is currently pointed at
     *          이 컨트롤러가 현재 가리키는 시간
     */
    public long getTime() {
        return mTime.toMillis(false);
    }

    /**
     * Set the time this controller is currently pointed at
     * 이 컨트롤러가 현재 가리키는 시간 설정
     *
     * @param millisTime Time since epoch in millis
     *                    epoch 이후의 시간(밀리세컨드)
     */
    public void setTime(long millisTime) {
        mTime.set(millisTime);
    }

    /**
     * @return the last set of date flags sent with
     *          UPDATE_TITLE과 함께 보낸 날짜 flags의 마지막 세트
     * {@link EventType#UPDATE_TITLE}
     */
    public long getDateFlags() {
        return mDateFlags;
    }

    /**
     * @return the last event ID the edit view was launched with
     *          편집 view가 시작된 마지막 이벤트 ID
     */
    public long getEventId() {
        return mEventId;
    }

    // Sets the eventId. Should only be used for initialization.
    // eventId 설정, 초기화에만 사용해야 함!
    public void setEventId(long eventId) {
        mEventId = eventId;
    }

    public int getViewType() {
        return mViewType;
    }

    // Forces the viewType. Should only be used for initialization.
    // viewType 적용, 초기화에만 사용해야 함!
    public void setViewType(int viewType) {
        mViewType = viewType;
    }

    public int getPreviousViewType() {
        return mPreviousViewType;
    }

    private void launchSelectVisibleCalendars() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(mContext, SelectVisibleCalendarsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mContext.startActivity(intent);
    }

    private void launchSettings() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //todo: GeneralPreferences(fragment)로 이동
        intent.setClass(mContext, CalendarSettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mContext.startActivity(intent);
    }

    private void launchSettingsDirectly() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //todo: GeneralPreferences(fragment)로 이동
        intent.setClass(mContext, SettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mContext.startActivity(intent);
    }

    private void launchCreateEvent(long startMillis, long endMillis, boolean allDayEvent,
                                   String title, long calendarId) {
        Intent intent = generateCreateEventIntent(startMillis, endMillis, allDayEvent, title,
                calendarId);
        mEventId = -1;
        mContext.startActivity(intent);
    }

    public Intent generateCreateEventIntent(long startMillis, long endMillis,
                                            boolean allDayEvent, String title, long calendarId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(mContext, EditEventActivity.class);
        intent.putExtra(EXTRA_EVENT_BEGIN_TIME, startMillis);
        intent.putExtra(EXTRA_EVENT_END_TIME, endMillis);
        intent.putExtra(EXTRA_EVENT_ALL_DAY, allDayEvent);
        intent.putExtra(Events.CALENDAR_ID, calendarId);
        intent.putExtra(Events.TITLE, title);
        return intent;
    }

    public void launchViewEvent(long eventId, long startMillis, long endMillis, int response) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
        intent.setData(eventUri);
        intent.setClass(mContext, AllInOneActivity.class);
        intent.putExtra(EXTRA_EVENT_BEGIN_TIME, startMillis);
        intent.putExtra(EXTRA_EVENT_END_TIME, endMillis);
        intent.putExtra(ATTENDEE_STATUS, response);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);
    }

    private void launchEditEvent(long eventId, long startMillis, long endMillis, boolean edit) {
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
        Intent intent = new Intent(Intent.ACTION_EDIT, uri);
        intent.putExtra(EXTRA_EVENT_BEGIN_TIME, startMillis);
        intent.putExtra(EXTRA_EVENT_END_TIME, endMillis);
        intent.setClass(mContext, EditEventActivity.class);
        intent.putExtra(EVENT_EDIT_ON_LAUNCH, edit);
        mEventId = eventId;
        mContext.startActivity(intent);
    }

    private void launchDeleteEvent(long eventId, long startMillis, long endMillis) {
        launchDeleteEventAndFinish(null, eventId, startMillis, endMillis, -1);
    }

    private void launchDeleteEventAndFinish(Activity parentActivity, long eventId, long startMillis,
                                            long endMillis, int deleteWhich) {
        DeleteEventHelper deleteEventHelper = new DeleteEventHelper(mContext, parentActivity,
                parentActivity != null /* exit when done */);
        deleteEventHelper.delete(startMillis, endMillis, eventId, deleteWhich);
    }

    private void launchAlerts() {
        Intent intent = new Intent();
        intent.setClass(mContext, AlertActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }


    private void launchSearch(long eventId, String query, ComponentName componentName) {
        final SearchManager searchManager =
                (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(componentName);
        final Intent intent = new Intent(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, query);
        intent.setComponent(searchableInfo.getSearchActivity());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mContext.startActivity(intent);
    }

    /**
     * Performs a manual refresh of calendars in all known accounts.
     * 알려진 모든 계정에서 달력을 수동으로 새로 고침
     */
    public void refreshCalendars() {
        Account[] accounts = AccountManager.get(mContext).getAccounts();
        Log.d(TAG, "Refreshing " + accounts.length + " accounts");

        String authority = Calendars.CONTENT_URI.getAuthority();
        for (int i = 0; i < accounts.length; i++) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Refreshing calendars for: " + accounts[i]);
            }
            Bundle extras = new Bundle();
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            ContentResolver.requestSync(accounts[i], authority, extras);
        }
    }

    private String eventInfoToString(EventInfo eventInfo) {
        String tmp = "Unknown";

        StringBuilder builder = new StringBuilder();
        if ((eventInfo.eventType & EventType.GO_TO) != 0) {
            tmp = "Go to time/event";
        } else if ((eventInfo.eventType & EventType.CREATE_EVENT) != 0) {
            tmp = "New event";
        } else if ((eventInfo.eventType & EventType.VIEW_EVENT) != 0) {
            tmp = "View event";
        } else if ((eventInfo.eventType & EventType.VIEW_EVENT_DETAILS) != 0) {
            tmp = "View details";
        } else if ((eventInfo.eventType & EventType.EDIT_EVENT) != 0) {
            tmp = "Edit event";
        } else if ((eventInfo.eventType & EventType.DELETE_EVENT) != 0) {
            tmp = "Delete event";
        } else if ((eventInfo.eventType & EventType.LAUNCH_SELECT_VISIBLE_CALENDARS) != 0) {
            tmp = "Launch select visible calendars";
        } else if ((eventInfo.eventType & EventType.LAUNCH_SETTINGS) != 0) {
            tmp = "Launch settings";
        } else if ((eventInfo.eventType & EventType.EVENTS_CHANGED) != 0) {
            tmp = "Refresh events";
        } else if ((eventInfo.eventType & EventType.SEARCH) != 0) {
            tmp = "Search";
        } else if ((eventInfo.eventType & EventType.USER_HOME) != 0) {
            tmp = "Gone home";
        } else if ((eventInfo.eventType & EventType.UPDATE_TITLE) != 0) {
            tmp = "Update title";
        }
        builder.append(tmp);
        builder.append(": id=");
        builder.append(eventInfo.id);
        builder.append(", selected=");
        builder.append(eventInfo.selectedTime);
        builder.append(", start=");
        builder.append(eventInfo.startTime);
        builder.append(", end=");
        builder.append(eventInfo.endTime);
        builder.append(", viewType=");
        builder.append(eventInfo.viewType);
        builder.append(", x=");
        builder.append(eventInfo.x);
        builder.append(", y=");
        builder.append(eventInfo.y);
        return builder.toString();
    }

    /**
     * One of the event types that are sent to or from the controller
     * 컨트롤러로 전송되거나 컨트롤러에서 전송되는 이벤트 타입 중 하나
     */
    public interface EventType {
        final long CREATE_EVENT = 1L;

        // Simple view of an event
        // 이벤트 심플 view
        final long VIEW_EVENT = 1L << 1;

        // Full detail view in read only mode
        // 읽기 전용 디테일 view
        final long VIEW_EVENT_DETAILS = 1L << 2;

        // full detail view in edit mode
        // 편집 가능한 디테일 view
        final long EDIT_EVENT = 1L << 3;

        final long DELETE_EVENT = 1L << 4;

        final long GO_TO = 1L << 5;

        final long LAUNCH_SETTINGS = 1L << 6;

        final long EVENTS_CHANGED = 1L << 7;

        final long SEARCH = 1L << 8;

        // User has pressed the home key
        // 사용자가 홈 키를 누름
        final long USER_HOME = 1L << 9;

        // date range has changed, update the title
        // 날짜 범위가 변경됨, 제목 업데이트
        final long UPDATE_TITLE = 1L << 10;

        // select which calendars to display
        // 표시할 캘린더 선택
        final long LAUNCH_SELECT_VISIBLE_CALENDARS = 1L << 11;

        final long LAUNCH_HOME = 1L << 12;

        final long LAUNCH_SETTINGS_DIRECT = 1L << 13;

        final long LAUNCH_GROUP = 1L << 14;

    }

    /**
     * One of the Agenda/Day/Week/Month view types
     * Agenda/일/주/월 view type 중 하나
     */
    public interface ViewType {
        final int DETAIL = -1;
        final int CURRENT = 0;
        final int AGENDA = 1;
        final int DAY = 2;
        final int WEEK = 3;
        final int MONTH = 4;
        final int EDIT = 5;
        final int TODAY = 6;
        final int MAX_VALUE = 6;

    }

    public interface EventHandler {
        long getSupportedEventTypes();

        void handleEvent(EventInfo event);

        /**
         * This notifies the handler that the database has changed and it should
         * update its view.
         * 데이터베이스가 변경되었음을 핸들러에게 알리고, view를 업데이트해야 함
         */
        void eventsChanged();
    }

    public static class EventInfo {

        private static final long ATTENTEE_STATUS_MASK = 0xFF;
        private static final long ALL_DAY_MASK = 0x100;
        private static final int ATTENDEE_STATUS_NONE_MASK = 0x01;
        private static final int ATTENDEE_STATUS_ACCEPTED_MASK = 0x02;
        private static final int ATTENDEE_STATUS_DECLINED_MASK = 0x04;
        private static final int ATTENDEE_STATUS_TENTATIVE_MASK = 0x08;

        public long eventType; // one of the EventType
        public int viewType; // one of the ViewType
        public long id; // event id
        public Time selectedTime; // the selected time in focus

        // Event start and end times.  All-day events are represented in:
        // - local time for GO_TO commands
        // - UTC time for VIEW_EVENT and other event-related commands
        // 이벤트 시작 및 종료 시간, 종일 이벤트는 다음과 같음:
        // - GO_TO 커맨드의 로컬 시간
        // - VIEW_EVENT 및 기타 이벤트 관련 커맨드의 UTC시간
        public Time startTime;
        public Time endTime;

        public int x; // x coordinate in the activity space x 좌표
        public int y; // y coordinate in the activity space y 좌표
        public String query; // query for a user search
        public ComponentName componentName;  // used in combination with query 쿼리와 함께 사용됨
        public String eventTitle;
        public long calendarId;

        /**
         * For EventType.VIEW_EVENT:
         * It is the default attendee response and an all day event indicator.
         * Set to Attendees.ATTENDEE_STATUS_NONE, Attendees.ATTENDEE_STATUS_ACCEPTED,
         * Attendees.ATTENDEE_STATUS_DECLINED, or Attendees.ATTENDEE_STATUS_TENTATIVE.
         * To signal the event is an all-day event, "or" ALL_DAY_MASK with the response.
         * Alternatively, use buildViewExtraLong(), getResponse(), and isAllDay().
         * EventType.VIEW_EVENT의 경우:
         * 기본 참석자 응답, 종일 이벤트 표시기
         * 참석자 설정은 Attendees.ATTENDEE_STATUS_NONE, Attendees.ATTENDEE_STATUS_ACCEPTED,
         *              Attendees.ATTENDEE_STATUS_DECLINED, or Attendees.ATTENDEE_STATUS_TENTATIVE
         * 이벤트 표시는 종일 이벤트 "또는" 응답과 함께 ALL_DAY_MASK
         * 또는 buildViewExtraLong(), getResponse(), isAllDay() 사용
         * <p/>
         * For EventType.CREATE_EVENT:
         * Set to {@link #EXTRA_CREATE_ALL_DAY} for creating an all-day event.
         * EventType.CREATE_EVENT의 경우:
         * 종일 이벤트 생성하려면 #EXTRA_CREATE_ALL_DAY 설정
         * <p/>
         * For EventType.GO_TO:
         * Set to {@link #EXTRA_GOTO_TIME} to go to the specified date/time.
         * Set to {@link #EXTRA_GOTO_DATE} to consider the date but ignore the time.
         * Set to {@link #EXTRA_GOTO_BACK_TO_PREVIOUS} if back should bring back previous view.
         * Set to {@link #EXTRA_GOTO_TODAY} if this is a user request to go to the current time.
         * EventType.GO_TO의 경우:
         * 지정된 날짜/시간으로 이동하려면 #EXTRA_GOTO_TIME 설정
         * 시간을 무시하고 날짜만 갖고 이동하려면 #EXTRA_GOTO_DATE 설정
         * 이전 view를 다시 가져오려면 #EXTRA_GOTO_BACK_TO_PREVIOUS 설정
         * 현재 시간으로 이동하려는 사용자 요청이 있으면 #EXTRA_GOTO_TODAY 설정
         * <p/>
         * For EventType.UPDATE_TITLE:
         * Set formatting flags for Utils.formatDateRange
         * EventType.UPDATE_TITLE의 경우:
         * Utils.formatDateRange에 대한 형식 flags 설정
         */
        public long extraLong;

        // Used to build the extra long for a VIEW event.
        // VIEW 이벤트를 위해 추가 시간을 구축하는 데 사용됨?
        public static long buildViewExtraLong(int response, boolean allDay) {
            long extra = allDay ? ALL_DAY_MASK : 0;

            switch (response) {
                case Attendees.ATTENDEE_STATUS_NONE:
                    extra |= ATTENDEE_STATUS_NONE_MASK;
                    break;
                case Attendees.ATTENDEE_STATUS_ACCEPTED:
                    extra |= ATTENDEE_STATUS_ACCEPTED_MASK;
                    break;
                case Attendees.ATTENDEE_STATUS_DECLINED:
                    extra |= ATTENDEE_STATUS_DECLINED_MASK;
                    break;
                case Attendees.ATTENDEE_STATUS_TENTATIVE:
                    extra |= ATTENDEE_STATUS_TENTATIVE_MASK;
                    break;
                default:
                    Log.wtf(TAG, "Unknown attendee response " + response);
                    extra |= ATTENDEE_STATUS_NONE_MASK;
                    break;
            }
            return extra;
        }

        public boolean isAllDay() {
            if (eventType != EventType.VIEW_EVENT) {
                Log.wtf(TAG, "illegal call to isAllDay , wrong event type " + eventType);
                return false;
            }
            return ((extraLong & ALL_DAY_MASK) != 0) ? true : false;
        }

        public int getResponse() {
            if (eventType != EventType.VIEW_EVENT) {
                Log.wtf(TAG, "illegal call to getResponse , wrong event type " + eventType);
                return Attendees.ATTENDEE_STATUS_NONE;
            }

            int response = (int) (extraLong & ATTENTEE_STATUS_MASK);
            switch (response) {
                case ATTENDEE_STATUS_NONE_MASK:
                    return Attendees.ATTENDEE_STATUS_NONE;
                case ATTENDEE_STATUS_ACCEPTED_MASK:
                    return Attendees.ATTENDEE_STATUS_ACCEPTED;
                case ATTENDEE_STATUS_DECLINED_MASK:
                    return Attendees.ATTENDEE_STATUS_DECLINED;
                case ATTENDEE_STATUS_TENTATIVE_MASK:
                    return Attendees.ATTENDEE_STATUS_TENTATIVE;
                default:
                    Log.wtf(TAG, "Unknown attendee response " + response);
            }
            return ATTENDEE_STATUS_NONE_MASK;
        }
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
