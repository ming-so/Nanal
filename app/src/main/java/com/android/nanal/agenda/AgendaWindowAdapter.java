package com.android.nanal.agenda;

/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.TextView;

import com.android.nanal.R;
import com.android.nanal.StickyHeaderListView;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarController.EventType;
import com.android.nanal.calendar.CalendarController.ViewType;
import com.android.nanal.event.Utils;

import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
Bugs Bugs Bugs:
- At rotation and launch time, the initial position is not set properly. This code is calling
 listview.setSelection() in 2 rapid secessions but it dropped or didn't process the first one.
- Scroll using trackball isn't repositioning properly after a new adapter is added.
- Track ball clicks at the header/footer doesn't work.
- Potential ping pong effect if the prefetch window is big and data is limited
- Add index in calendar provider

ToDo ToDo ToDo:
Get design of header and footer from designer

Make scrolling smoother.
Test for correctness
Loading speed
Check for leaks and excessive allocations
 */

public class AgendaWindowAdapter extends BaseAdapter
        implements StickyHeaderListView.HeaderIndexer, StickyHeaderListView.HeaderHeightListener{

    public static final int INDEX_INSTANCE_ID = 0;
    public static final int INDEX_TITLE = 1;
    public static final int INDEX_EVENT_LOCATION = 2;
    public static final int INDEX_ALL_DAY = 3;
    public static final int INDEX_HAS_ALARM = 4;
    public static final int INDEX_COLOR = 5;
    public static final int INDEX_RRULE = 6;
    public static final int INDEX_BEGIN = 7;
    public static final int INDEX_END = 8;
    public static final int INDEX_EVENT_ID = 9;
    public static final int INDEX_START_DAY = 10;
    public static final int INDEX_END_DAY = 11;
    public static final int INDEX_SELF_ATTENDEE_STATUS = 12;
    public static final int INDEX_ORGANIZER = 13;
    public static final int INDEX_OWNER_ACCOUNT = 14;
    public static final int INDEX_CAN_ORGANIZER_RESPOND= 15;
    public static final int INDEX_TIME_ZONE = 16;
    static final boolean BASICLOG = false;
    static final boolean DEBUGLOG = false;
    private static final String TAG = "AgendaWindowAdapter";
    private static final String AGENDA_SORT_ORDER =
            CalendarContract.Instances.START_DAY + " ASC, " +
                    CalendarContract.Instances.BEGIN + " ASC, " +
                    CalendarContract.Events.TITLE + " ASC";
    private static final String[] PROJECTION = new String[] {
            Instances._ID, // 0
            Instances.TITLE, // 1
            Instances.EVENT_LOCATION, // 2
            Instances.ALL_DAY, // 3
            Instances.HAS_ALARM, // 4
            Instances.DISPLAY_COLOR, // 5
            Instances.RRULE, // 6
            Instances.BEGIN, // 7
            Instances.END, // 8
            Instances.EVENT_ID, // 9
            Instances.START_DAY, // 10 Julian start day
            Instances.END_DAY, // 11 Julian end day
            Instances.SELF_ATTENDEE_STATUS, // 12
            Instances.ORGANIZER, // 13
            Instances.OWNER_ACCOUNT, // 14
            Instances.CAN_ORGANIZER_RESPOND, // 15
            Instances.EVENT_TIMEZONE, // 16
    };
    // Listview may have a bug where the index/position is not consistent when there's a header.
    // 헤더가 있을 때 index/위치가 일치하지 않는 버그가 있을 수 있음
    // position == positionInListView - OFF_BY_ONE_BUG
    // TODO Need to look into this.
    private static final int OFF_BY_ONE_BUG = 1;
    private static final int MAX_NUM_OF_ADAPTERS = 5;
    private static final int IDEAL_NUM_OF_EVENTS = 50;
    private static final int MIN_QUERY_DURATION = 7; // days
    private static final int MAX_QUERY_DURATION = 60; // days
    private static final int PREFETCH_BOUNDARY = 1;
    /** Times to auto-expand/retry query after getting no data / 데이터를 얻지 못한 후, 쿼리를 자동 확장/재시도하는 시간 */
    private static final int RETRIES_ON_NO_DATA = 1;
    // Types of Query / 쿼리의 종류
    private static final int QUERY_TYPE_OLDER = 0; // Query for older events / 이전 이벤트를 위한 쿼리
    private static final int QUERY_TYPE_NEWER = 1; // Query for newer events / 최신 이벤트를 위한 쿼리
    private static final int QUERY_TYPE_CLEAN = 2; // Delete everything and query around a date / 모든 것을 삭제하고 날짜를 기준으로 쿼리


    private final Context mContext;
    private final Resources mResources;
    private final QueryHandler mQueryHandler;
    private final AgendaListView mAgendaListView;
    private final LinkedList<DayAdapterInfo> mAdapterInfos =
            new LinkedList<DayAdapterInfo>();
    private final ConcurrentLinkedQueue<QuerySpec> mQueryQueue =
            new ConcurrentLinkedQueue<QuerySpec>();
    private final TextView mHeaderView;
    private final TextView mFooterView;
    private final boolean mIsTabletConfig;
    // Note: Formatter is not thread safe. Fine for now as it is only used by the main thread.
    private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    // defines if to pop-up the current event when the agenda is first shown
    // 일정이 처음 표시될 때, 현재 이벤트를 팝업할지의 여부를 정의함
    private final boolean mShowEventOnStart;
    private final Handler mDataChangedHandler = new Handler();
    private final Runnable mDataChangedRunnable = new Runnable() {
        @Override
        public void run() {
            notifyDataSetChanged();
        }
    };
    private final int mSelectedItemBackgroundColor;
    private final int mSelectedItemTextColor;
    private final float mItemRightMargin;
    boolean mCleanQueryInitiated = false;
    // Used to stop a fling motion if the ListView is set to a specific position
    // ListView가 특정 위치로 설정된 경우, fling 동작을 중지하는 데 사용됨
    int mListViewScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    /**
     * The sum of the rows in all the adapters / 모든 어댑터의 행 합계
     */
    private int mRowCount;
    /**
     * The number of times we have queried and gotten no results back
     * 결과를 얻지 못한 횟수
     */
    private int mEmptyCursorCount;
    /**
     * Cached value of the last used adapter
     * 마지막으로 사용한 어댑터의 캐시 값
     */
    private DayAdapterInfo mLastUsedInfo;
    private boolean mDoneSettingUpHeaderFooter = false;
    private int mStickyHeaderSize = 44; // Initial size big enough for it to work
    // 작동하기에 충분한 초기 크기
    /**
     * When the user scrolled to the top, a query will be made for older events
     * and this will be incremented. Don't make more requests if
     * mOlderRequests > mOlderRequestsProcessed.
     * 사용자가 상단으로 스크롤하면, 이전 이벤트에 대한 쿼리가 수행되며 이 쿼리는 증가됨
     * mOlderRequests > mOlderRequestsProcessed인 경우에는 요청하지 않음
     */
    private int mOlderRequests;
    /** Number of "older" query that has been processed.
     * 처리된 "이전" 쿼리 갯수 */
    private int mOlderRequestsProcessed;
    /**
     * When the user scrolled to the bottom, a query will be made for newer
     * events and this will be incremented. Don't make more requests if
     * mNewerRequests > mNewerRequestsProcessed.
     * 사용자가 하단으로 스크롤하면, 최신 이벤트에 대한 쿼리가 수행되며 이 쿼리는 증가됨
     * mNewerRequests > mNewerRequestsProcessed인 경우에는 요청하지 않음
     */
    private int mNewerRequests;
    /** Number of "newer" query that has been processed.
     * 처리된 "최신" 쿼리 갯수 */
    private int mNewerRequestsProcessed;
    private String mTimeZone;
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(mContext, this);
            notifyDataSetChanged();
        }
    };
    private boolean mShuttingDown;
    private boolean mHideDeclined;
    /** The current search query, or null if none
     * 현재 검색 쿼리, 없으면 null */
    private String mSearchQuery;
    private long mSelectedInstanceId = -1;
    private AgendaAdapter.ViewHolder mSelectedVH = null;

    public AgendaWindowAdapter(Context context,
                               AgendaListView agendaListView, boolean showEventOnStart) {
        mContext = context;
        mResources = context.getResources();
        mSelectedItemBackgroundColor = mResources
                .getColor(R.color.agenda_selected_background_color);
        mSelectedItemTextColor = mResources.getColor(R.color.agenda_selected_text_color);
        mItemRightMargin = mResources.getDimension(R.dimen.agenda_item_right_margin);
        mIsTabletConfig = Utils.getConfigBool(mContext, R.bool.tablet_config);

        mTimeZone = Utils.getTimeZone(context, mTZUpdater);
        mAgendaListView = agendaListView;
        mQueryHandler = new QueryHandler(context.getContentResolver());

        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

        mShowEventOnStart = showEventOnStart;

        // Implies there is no sticky header
        // sticky 헤더가 없다는 걸 의미함
        if (!mShowEventOnStart) {
            mStickyHeaderSize = 0;
        }
        mSearchQuery = null;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mHeaderView = (TextView)inflater.inflate(R.layout.agenda_header_footer, null);
        mFooterView = (TextView)inflater.inflate(R.layout.agenda_header_footer, null);
        mHeaderView.setText(R.string.loading);
        mAgendaListView.addHeaderView(mHeaderView);
    }

    static String getViewTitle(View x) {
        String title = "";
        if (x != null) {
            Object yy = x.getTag();
            if (yy instanceof AgendaAdapter.ViewHolder) {
                TextView tv = ((AgendaAdapter.ViewHolder) yy).title;
                if (tv != null) {
                    title = (String) tv.getText();
                }
            } else if (yy != null) {
                TextView dateView = ((AgendaByDayAdapter.ViewHolder) yy).dateView;
                if (dateView != null) {
                    title = (String) dateView.getText();
                }
            }
        }
        return title;
    }

    // Method in Adapter
    // Adapter의 메소드
    @Override
    public int getViewTypeCount() {
        return AgendaByDayAdapter.TYPE_LAST;
    }

    // Method in BaseAdapter
    // BaseAdapter의 메소드
    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    // Method in Adapter
    // Adapter의 메소드
    @Override
    public int getItemViewType(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getItemViewType(position - info.offset);
        } else {
            return -1;
        }
    }

    // Method in BaseAdapter
    // BaseAdapter의 메소드
    @Override
    public boolean isEnabled(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.isEnabled(position - info.offset);
        } else {
            return false;
        }
    }

    // Abstract Method in BaseAdapter
    // BaseAdapter의 추상 메소드
    public int getCount() {
        return mRowCount;
    }

    // Abstract Method in BaseAdapter
    // BaseAdapter의 추상 메소드
    public Object getItem(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getItem(position - info.offset);
        } else {
            return null;
        }
    }

    // Method in BaseAdapter
    // BaseAdapter의 메소드
    @Override
    public boolean hasStableIds() {
        return true;
    }

    // Abstract Method in BaseAdapter
    // BaseAdapter의 추상 메소드
    @Override
    public long getItemId(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            int curPos = info.dayAdapter.getCursorPosition(position - info.offset);
            if (curPos == Integer.MIN_VALUE) {
                return -1;
            }
            // Regular event
            // 주기적인 이벤트
            if (curPos >= 0) {
                info.cursor.moveToPosition(curPos);
                return info.cursor.getLong(AgendaWindowAdapter.INDEX_EVENT_ID) << 20 +
                        info.cursor.getLong(AgendaWindowAdapter.INDEX_BEGIN);
            }
            // Day Header
            // 일 헤더
            return info.dayAdapter.findJulianDayFromPosition(position);

        } else {
            return -1;
        }
    }

    // Abstract Method in BaseAdapter
    // BaseAdapter의 추상 메소드
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= (mRowCount - PREFETCH_BOUNDARY)
                && mNewerRequests <= mNewerRequestsProcessed) {
            if (DEBUGLOG) Log.e(TAG, "queryForNewerEvents: ");
            mNewerRequests++;
            queueQuery(new QuerySpec(QUERY_TYPE_NEWER));
        }

        if (position < PREFETCH_BOUNDARY
                && mOlderRequests <= mOlderRequestsProcessed) {
            if (DEBUGLOG) Log.e(TAG, "queryForOlderEvents: ");
            mOlderRequests++;
            queueQuery(new QuerySpec(QUERY_TYPE_OLDER));
        }

        final View v;
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            int offset = position - info.offset;
            v = info.dayAdapter.getView(offset, convertView,
                    parent);

            // Turn on the past/present separator if the view is a day header
            // and it is the first day with events after yesterday.
            // view가 일 헤더고, 어제 이후 이벤트가 있는 첫 번째 날이라면 과거/현재 구분자를 켬
            if (info.dayAdapter.isDayHeaderView(offset)) {
                View simpleDivider = v.findViewById(R.id.top_divider_simple);
                View pastPresentDivider = v.findViewById(R.id.top_divider_past_present);
                if (info.dayAdapter.isFirstDayAfterYesterday(offset)) {
                    if (simpleDivider != null && pastPresentDivider != null) {
                        simpleDivider.setVisibility(View.GONE);
                        pastPresentDivider.setVisibility(View.VISIBLE);
                    }
                } else if (simpleDivider != null && pastPresentDivider != null) {
                    simpleDivider.setVisibility(View.VISIBLE);
                    pastPresentDivider.setVisibility(View.GONE);
                }
            }
        } else {
            // TODO
            Log.e(TAG, "BUG: getAdapterInfoByPosition returned null!!! " + position);
            TextView tv = new TextView(mContext);
            tv.setText("Bug! " + position);
            v = tv;
        }

        // If this is not a tablet config don't do selection highlighting
        // 태블릿 구성이 아닌 경우 선택 항목 강조 표시 안 함
        if (!mIsTabletConfig) {
            return v;
        }
        // Show selected marker if this is item is selected
        // 항목이 선택된 경우, 선택된 마커 표시
        boolean selected = false;
        Object yy = v.getTag();
        if (yy instanceof AgendaAdapter.ViewHolder) {
            AgendaAdapter.ViewHolder vh = (AgendaAdapter.ViewHolder) yy;
            selected = mSelectedInstanceId == vh.instanceId;
            vh.selectedMarker.setVisibility((selected && mShowEventOnStart) ?
                    View.VISIBLE : View.GONE);
            if (mShowEventOnStart) {
                GridLayout.LayoutParams lp =
                        (GridLayout.LayoutParams)vh.textContainer.getLayoutParams();
                if (selected) {
                    mSelectedVH = vh;
                    v.setBackgroundColor(mSelectedItemBackgroundColor);
                    vh.title.setTextColor(mSelectedItemTextColor);
                    vh.when.setTextColor(mSelectedItemTextColor);
                    vh.where.setTextColor(mSelectedItemTextColor);
                    lp.setMargins(0, 0, 0, 0);
                    vh.textContainer.setLayoutParams(lp);
                } else {
                    lp.setMargins(0, 0, (int)mItemRightMargin, 0);
                    vh.textContainer.setLayoutParams(lp);
                }
            }
        }

        if (DEBUGLOG) {
            Log.e(TAG, "getView " + position + " = " + getViewTitle(v));
        }
        return v;
    }

    private int findEventPositionNearestTime(Time time, long id) {
        DayAdapterInfo info = getAdapterInfoByTime(time);
        int pos = -1;
        if (info != null) {
            pos = info.offset + info.dayAdapter.findEventPositionNearestTime(time, id);
        }
        if (DEBUGLOG) Log.e(TAG, "findEventPositionNearestTime " + time + " id:" + id + " =" + pos);
        return pos;
    }

    protected DayAdapterInfo getAdapterInfoByPosition(int position) {
        synchronized (mAdapterInfos) {
            if (mLastUsedInfo != null && mLastUsedInfo.offset <= position
                    && position < (mLastUsedInfo.offset + mLastUsedInfo.size)) {
                return mLastUsedInfo;
            }
            for (DayAdapterInfo info : mAdapterInfos) {
                if (info.offset <= position
                        && position < (info.offset + info.size)) {
                    mLastUsedInfo = info;
                    return info;
                }
            }
        }
        return null;
    }

    private DayAdapterInfo getAdapterInfoByTime(Time time) {
        if (DEBUGLOG) Log.e(TAG, "getAdapterInfoByTime " + time.toString());

        Time tmpTime = new Time(time);
        long timeInMillis = tmpTime.normalize(true);
        int day = Time.getJulianDay(timeInMillis, tmpTime.gmtoff);
        synchronized (mAdapterInfos) {
            for (DayAdapterInfo info : mAdapterInfos) {
                if (info.start <= day && day <= info.end) {
                    return info;
                }
            }
        }
        return null;
    }

    public AgendaItem getAgendaItemByPosition(final int positionInListView) {
        return getAgendaItemByPosition(positionInListView, true);
    }

    /**
     * Return the event info for a given position in the adapter
     * 어댑터에서 지정된 위치에 대한 이벤트 정보 반환
     * @param positionInListView
     * @param returnEventStartDay If true, return actual event startday. Otherwise
     *        return agenda date-header date as the startDay.
     *        The two will differ for multi-day events after the first day.
     *        true: 실제 이벤트 시작일 반환
     *        false: agenda 일 헤더 날짜를 사용하는 듯?
     *        둘은 첫날 이후 여러 날에 걸친 이벤트에 대해서 다를 것임
     * @return
     */
    public AgendaItem getAgendaItemByPosition(final int positionInListView,
                                              boolean returnEventStartDay) {
        if (DEBUGLOG) Log.e(TAG, "getEventByPosition " + positionInListView);
        if (positionInListView < 0) {
            return null;
        }

        final int positionInAdapter = positionInListView - OFF_BY_ONE_BUG;
        DayAdapterInfo info = getAdapterInfoByPosition(positionInAdapter);
        if (info == null) {
            return null;
        }

        int cursorPosition = info.dayAdapter.getCursorPosition(positionInAdapter - info.offset);
        if (cursorPosition == Integer.MIN_VALUE) {
            return null;
        }

        boolean isDayHeader = false;
        if (cursorPosition < 0) {
            cursorPosition = -cursorPosition;
            isDayHeader = true;
        }

        if (cursorPosition < info.cursor.getCount()) {
            AgendaItem item = buildAgendaItemFromCursor(info.cursor, cursorPosition, isDayHeader);
            if (!returnEventStartDay && !isDayHeader) {
                item.startDay = info.dayAdapter.findJulianDayFromPosition(positionInAdapter -
                        info.offset);
            }
            return item;
        }
        return null;
    }

    private AgendaItem buildAgendaItemFromCursor(final Cursor cursor, int cursorPosition,
                                                 boolean isDayHeader) {
        if (cursorPosition == -1) {
            cursor.moveToFirst();
        } else {
            cursor.moveToPosition(cursorPosition);
        }
        AgendaItem agendaItem = new AgendaItem();
        agendaItem.begin = cursor.getLong(AgendaWindowAdapter.INDEX_BEGIN);
        agendaItem.end = cursor.getLong(AgendaWindowAdapter.INDEX_END);
        agendaItem.startDay = cursor.getInt(AgendaWindowAdapter.INDEX_START_DAY);
        agendaItem.allDay = cursor.getInt(AgendaWindowAdapter.INDEX_ALL_DAY) != 0;
        if (agendaItem.allDay) { // UTC to Local time conversion
            Time time = new Time(mTimeZone);
            time.setJulianDay(Time.getJulianDay(agendaItem.begin, 0));
            agendaItem.begin = time.toMillis(false /* use isDst */);
        } else if (isDayHeader) { // Trim to midnight.
            Time time = new Time(mTimeZone);
            time.set(agendaItem.begin);
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
            agendaItem.begin = time.toMillis(false /* use isDst */);
        }

        // If this is not a day header, then it's an event.
        // 일 헤더가 없다면 이벤트임
        if (!isDayHeader) {
            agendaItem.id = cursor.getLong(AgendaWindowAdapter.INDEX_EVENT_ID);
            if (agendaItem.allDay) {
                Time time = new Time(mTimeZone);
                time.setJulianDay(Time.getJulianDay(agendaItem.end, 0));
                agendaItem.end = time.toMillis(false /* use isDst */);
            }
        }
        return agendaItem;
    }

    /**
     * Ensures that any all day events are converted to UTC before a VIEW_EVENT command is sent.
     * VIEW_EVENT 명령을 전송하기 전에, 모든 일일 이벤트가 UTC로 변환되는지 확인함
     */
    private void sendViewEvent(AgendaItem item, long selectedTime) {
        long startTime;
        long endTime;
        if (item.allDay) {
            startTime = Utils.convertAlldayLocalToUTC(null, item.begin, mTimeZone);
            endTime = Utils.convertAlldayLocalToUTC(null, item.end, mTimeZone);
        } else {
            startTime = item.begin;
            endTime = item.end;
        }
        if (DEBUGLOG) {
            Log.d(TAG, "Sent (AgendaWindowAdapter): VIEW EVENT: " + new Date(startTime));
        }
        CalendarController.getInstance(mContext)
                .sendEventRelatedEventWithExtra(this, EventType.VIEW_EVENT,
                        item.id, startTime, endTime, 0,
                        0, CalendarController.EventInfo.buildViewExtraLong(
                                Attendees.ATTENDEE_STATUS_NONE,
                                item.allDay), selectedTime);
    }

    public void refresh(Time goToTime, long id, String searchQuery, boolean forced,
                        boolean refreshEventInfo) {
        if (searchQuery != null) {
            mSearchQuery = searchQuery;
        }

        if (DEBUGLOG) {
            Log.e(TAG, this + ": refresh " + goToTime.toString() + " id " + id
                    + ((searchQuery != null) ? searchQuery : "")
                    + (forced ? " forced" : " not forced")
                    + (refreshEventInfo ? " refresh event info" : ""));
        }

        int startDay = Time.getJulianDay(goToTime.toMillis(false), goToTime.gmtoff);

        if (!forced && isInRange(startDay, startDay)) {
            // No need to re-query
            if (!mAgendaListView.isAgendaItemVisible(goToTime, id)) {
                int gotoPosition = findEventPositionNearestTime(goToTime, id);
                if (gotoPosition > 0) {
                    mAgendaListView.setSelectionFromTop(gotoPosition +
                            OFF_BY_ONE_BUG, mStickyHeaderSize);
                    if (mListViewScrollState == OnScrollListener.SCROLL_STATE_FLING) {
                        mAgendaListView.smoothScrollBy(0, 0);
                    }
                    if (refreshEventInfo) {
                        long newInstanceId = findInstanceIdFromPosition(gotoPosition);
                        if (newInstanceId != getSelectedInstanceId()) {
                            setSelectedInstanceId(newInstanceId);
                            mDataChangedHandler.post(mDataChangedRunnable);
                            Cursor tempCursor = getCursorByPosition(gotoPosition);
                            if (tempCursor != null) {
                                int tempCursorPosition = getCursorPositionByPosition(gotoPosition);
                                AgendaItem item =
                                        buildAgendaItemFromCursor(tempCursor, tempCursorPosition,
                                                false);
                                mSelectedVH = new AgendaAdapter.ViewHolder();
                                mSelectedVH.allDay = item.allDay;
                                sendViewEvent(item, goToTime.toMillis(false));
                            }
                        }
                    }
                }

                Time actualTime = new Time(mTimeZone);
                actualTime.set(goToTime);
                CalendarController.getInstance(mContext).sendEvent(this, EventType.UPDATE_TITLE,
                        actualTime, actualTime, -1, ViewType.CURRENT);
            }
            return;
        }

        // If AllInOneActivity is sending a second GOTO event(in OnResume), ignore it.
        // AllInOneActivity가 두 번째 GOTE 이벤트(OnResume에서)를 전송하고 있는 경우 무시함
        if (!mCleanQueryInitiated || searchQuery != null) {
            // Query for a total of MIN_QUERY_DURATION days
            // 총 MIN_QUERY_DURATION 일수에 대한 쿼리문
            int endDay = startDay + MIN_QUERY_DURATION;

            mSelectedInstanceId = -1;
            mCleanQueryInitiated = true;
            queueQuery(startDay, endDay, goToTime, searchQuery, QUERY_TYPE_CLEAN, id);

            // Pre-fetch more data to overcome a race condition in AgendaListView.shiftSelection
            // Queuing more data with the goToTime set to the selected time skips the call to
            // shiftSelection on refresh.
            // AgendaListView.shiftSelection에서 race... 조건을 극복하기 위해 더 많은 데이터를 사전에 가져옴
            // 선택된 시간으로 설정된 goToTime 사용하여 더 많은 데이터를 가져오면 새로 고침 시 shiftSelection으로 호출이 건너뜀

            mOlderRequests++;
            queueQuery(0, 0, goToTime, searchQuery, QUERY_TYPE_OLDER, id);
            mNewerRequests++;
            queueQuery(0, 0, goToTime, searchQuery, QUERY_TYPE_NEWER, id);
        }
    }

    public void close() {
        mShuttingDown = true;
        pruneAdapterInfo(QUERY_TYPE_CLEAN);
        if (mQueryHandler != null) {
            mQueryHandler.cancelOperation(0);
        }
    }

    private DayAdapterInfo pruneAdapterInfo(int queryType) {
        synchronized (mAdapterInfos) {
            DayAdapterInfo recycleMe = null;
            if (!mAdapterInfos.isEmpty()) {
                if (mAdapterInfos.size() >= MAX_NUM_OF_ADAPTERS) {
                    if (queryType == QUERY_TYPE_NEWER) {
                        recycleMe = mAdapterInfos.removeFirst();
                    } else if (queryType == QUERY_TYPE_OLDER) {
                        recycleMe = mAdapterInfos.removeLast();
                        // Keep the size only if the oldest items are removed.
                        recycleMe.size = 0;
                    }
                    if (recycleMe != null) {
                        if (recycleMe.cursor != null) {
                            recycleMe.cursor.close();
                        }
                        return recycleMe;
                    }
                }

                if (mRowCount == 0 || queryType == QUERY_TYPE_CLEAN) {
                    mRowCount = 0;
                    int deletedRows = 0;
                    DayAdapterInfo info;
                    do {
                        info = mAdapterInfos.poll();
                        if (info != null) {
                            // TODO the following causes ANR's. Do this in a thread.
                            info.cursor.close();
                            deletedRows += info.size;
                            recycleMe = info;
                        }
                    } while (info != null);

                    if (recycleMe != null) {
                        recycleMe.cursor = null;
                        recycleMe.size = deletedRows;
                    }
                }
            }
            return recycleMe;
        }
    }

    private String buildQuerySelection() {
        // Respect the preference to show/hide declined events
        // 거부된 이벤트를 표시/숨기기 -> Preference을 따름

        if (mHideDeclined) {
            return Calendars.VISIBLE + "=1 AND "
                    + Instances.SELF_ATTENDEE_STATUS + "!="
                    + Attendees.ATTENDEE_STATUS_DECLINED;
        } else {
            return Calendars.VISIBLE + "=1";
        }
    }

    private Uri buildQueryUri(int start, int end, String searchQuery) {
        Uri rootUri = searchQuery == null ?
                Instances.CONTENT_BY_DAY_URI :
                Instances.CONTENT_SEARCH_BY_DAY_URI;
        Uri.Builder builder = rootUri.buildUpon();
        ContentUris.appendId(builder, start);
        ContentUris.appendId(builder, end);
        if (searchQuery != null) {
            builder.appendPath(searchQuery);
        }
        return builder.build();
    }

    private boolean isInRange(int start, int end) {
        synchronized (mAdapterInfos) {
            if (mAdapterInfos.isEmpty()) {
                return false;
            }
            return mAdapterInfos.getFirst().start <= start && end <= mAdapterInfos.getLast().end;
        }
    }

    private int calculateQueryDuration(int start, int end) {
        int queryDuration = MAX_QUERY_DURATION;
        if (mRowCount != 0) {
            queryDuration = IDEAL_NUM_OF_EVENTS * (end - start + 1) / mRowCount;
        }

        if (queryDuration > MAX_QUERY_DURATION) {
            queryDuration = MAX_QUERY_DURATION;
        } else if (queryDuration < MIN_QUERY_DURATION) {
            queryDuration = MIN_QUERY_DURATION;
        }

        return queryDuration;
    }

    private boolean queueQuery(int start, int end, Time goToTime,
                               String searchQuery, int queryType, long id) {
        QuerySpec queryData = new QuerySpec(queryType);
        queryData.goToTime = new Time(goToTime);    // Creates a new time reference per QuerySpec.
        queryData.start = start;
        queryData.end = end;
        queryData.searchQuery = searchQuery;
        queryData.id = id;
        return queueQuery(queryData);
    }

    private boolean queueQuery(QuerySpec queryData) {
        queryData.searchQuery = mSearchQuery;
        Boolean queuedQuery;
        synchronized (mQueryQueue) {
            queuedQuery = false;
            Boolean doQueryNow = mQueryQueue.isEmpty();
            mQueryQueue.add(queryData);
            queuedQuery = true;
            if (doQueryNow) {
                doQuery(queryData);
            }
        }
        return queuedQuery;
    }

    private void doQuery(QuerySpec queryData) {
        if (!mAdapterInfos.isEmpty()) {
            int start = mAdapterInfos.getFirst().start;
            int end = mAdapterInfos.getLast().end;
            int queryDuration = calculateQueryDuration(start, end);
            switch(queryData.queryType) {
                case QUERY_TYPE_OLDER:
                    queryData.end = start - 1;
                    queryData.start = queryData.end - queryDuration;
                    break;
                case QUERY_TYPE_NEWER:
                    queryData.start = end + 1;
                    queryData.end = queryData.start + queryDuration;
                    break;
            }

            // By "compacting" cursors, this fixes the disco/ping-pong problem
            // 커서를 "대조"하여 디스코/핑퐁 문제를 해결함
            // b/5311977
            if (mRowCount < 20 && queryData.queryType != QUERY_TYPE_CLEAN) {
                if (DEBUGLOG) {
                    Log.e(TAG, "Compacting cursor: mRowCount=" + mRowCount
                            + " totalStart:" + start
                            + " totalEnd:" + end
                            + " query.start:" + queryData.start
                            + " query.end:" + queryData.end);
                }

                queryData.queryType = QUERY_TYPE_CLEAN;

                if (queryData.start > start) {
                    queryData.start = start;
                }
                if (queryData.end < end) {
                    queryData.end = end;
                }
            }
        }

        if (BASICLOG) {
            Time time = new Time(mTimeZone);
            time.setJulianDay(queryData.start);
            Time time2 = new Time(mTimeZone);
            time2.setJulianDay(queryData.end);
            Log.v(TAG, "startQuery: " + time.toString() + " to "
                    + time2.toString() + " then go to " + queryData.goToTime);
        }

        mQueryHandler.cancelOperation(0);
        if (BASICLOG) queryData.queryStartMillis = System.nanoTime();

        Uri queryUri = buildQueryUri(
                queryData.start, queryData.end, queryData.searchQuery);
        mQueryHandler.startQuery(0, queryData, queryUri,
                PROJECTION, buildQuerySelection(), null,
                AGENDA_SORT_ORDER);
    }

    private String formatDateString(int julianDay) {
        Time time = new Time(mTimeZone);
        time.setJulianDay(julianDay);
        long millis = time.toMillis(false);
        mStringBuilder.setLength(0);
        return DateUtils.formatDateRange(mContext, mFormatter, millis, millis,
                DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_ABBREV_MONTH, mTimeZone).toString();
    }

    private void updateHeaderFooter(final int start, final int end) {
        mHeaderView.setText(mContext.getString(R.string.show_older_events,
                formatDateString(start)));
        mFooterView.setText(mContext.getString(R.string.show_newer_events,
                formatDateString(end)));
    }

    public void onResume() {
        mTZUpdater.run();
    }

    public void setHideDeclinedEvents(boolean hideDeclined) {
        mHideDeclined = hideDeclined;
    }

    public void setSelectedView(View v) {
        if (v != null) {
            Object vh = v.getTag();
            if (vh instanceof AgendaAdapter.ViewHolder) {
                mSelectedVH = (AgendaAdapter.ViewHolder) vh;
                if (mSelectedInstanceId != mSelectedVH.instanceId) {
                    mSelectedInstanceId = mSelectedVH.instanceId;
                    notifyDataSetChanged();
                }
            }
        }
    }

    public AgendaAdapter.ViewHolder getSelectedViewHolder() {
        return mSelectedVH;
    }

    public long getSelectedInstanceId() {
        return mSelectedInstanceId;
    }

    public void setSelectedInstanceId(long selectedInstanceId) {
        mSelectedInstanceId = selectedInstanceId;
        mSelectedVH = null;
    }

    private long findInstanceIdFromPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getInstanceId(position - info.offset);
        }
        return -1;
    }

    private long findStartTimeFromPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getStartTime(position - info.offset);
        }
        return -1;
    }

    private Cursor getCursorByPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.cursor;
        }
        return null;
    }

    private int getCursorPositionByPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getCursorPosition(position - info.offset);
        }
        return -1;
    }

    // Returns the location of the day header of a specific event specified in the position
    // in the adapter
    // 어댑터의 위치에 지정된 특정 이벤트의 일 헤더 위치를 반환함
    @Override
    public int getHeaderPositionFromItemPosition(int position) {

        // For phone configuration, return -1 so there will be no sticky header
        // 태블릿이 아니라 휴대폰 배치일 경우, -1을 반환해서 스티키 헤더가 생기지 않게 함
        if (!mIsTabletConfig) {
            return -1;
        }

        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            int pos = info.dayAdapter.getHeaderPosition(position - info.offset);
            return (pos != -1) ? (pos + info.offset) : -1;
        }
        return -1;
    }

    // Returns the number of events for a specific day header
    // 특정 일 헤더의 이벤트 개수 반환
    @Override
    public int getHeaderItemsNumber(int headerPosition) {
        if (headerPosition < 0 || !mIsTabletConfig) {
            return -1;
        }
        DayAdapterInfo info = getAdapterInfoByPosition(headerPosition);
        if (info != null) {
            return info.dayAdapter.getHeaderItemsCount(headerPosition - info.offset);
        }
        return -1;
    }

    @Override
    public void OnHeaderHeightChanged(int height) {
        mStickyHeaderSize = height;
    }

    public int getStickyHeaderHeight() {
        return mStickyHeaderSize;
    }

    // Implementation of HeaderIndexer interface for StickyHeeaderListView
    // StickyHeaderListView용 HeaderIndexer 인터페이스 구현

    public void setScrollState(int state) {
        mListViewScrollState = state;
    }

    private static class QuerySpec {
        long queryStartMillis;
        Time goToTime;
        int start;
        int end;
        String searchQuery;
        int queryType;
        long id;

        public QuerySpec(int queryType) {
            this.queryType = queryType;
            id = -1;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + end;
            result = prime * result + (int) (queryStartMillis ^ (queryStartMillis >>> 32));
            result = prime * result + queryType;
            result = prime * result + start;
            if (searchQuery != null) {
                result = prime * result + searchQuery.hashCode();
            }
            if (goToTime != null) {
                long goToTimeMillis = goToTime.toMillis(false);
                result = prime * result + (int) (goToTimeMillis ^ (goToTimeMillis >>> 32));
            }
            result = prime * result + (int) id;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            QuerySpec other = (QuerySpec) obj;
            if (end != other.end || queryStartMillis != other.queryStartMillis
                    || queryType != other.queryType || start != other.start
                    || Utils.equals(searchQuery, other.searchQuery) || id != other.id) {
                return false;
            }

            if (goToTime != null) {
                if (goToTime.toMillis(false) != other.goToTime.toMillis(false)) {
                    return false;
                }
            } else {
                if (other.goToTime != null) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Class representing a list item within the Agenda view.  Could be either an instance of an
     * event, or a header marking the specific day.
     * Agenda view에서 리스트 아이템을 나타내는 클래스
     * 이벤트의 인스턴스 또는 특정 날짜를 표시하는 헤더가 될 수 있음
     * <p/>
     * The begin and end times of an AgendaItem should always be in local time, even if the event
     * is all day.  buildAgendaItemFromCursor() converts each event to local time.
     * 이벤트가 하루 종일 진행되더라도 AdendaItem의 시작 시간과 종료 시간은 항상 로컬 시간(local time)이어야 함
     * buildAgendaItemFromCursor()는 각 이벤트를 로컬 시간으로 변환함
     */
    static class AgendaItem {
        long begin;
        long end;
        long id;
        int startDay;
        boolean allDay;
    }

    static class DayAdapterInfo {
        Cursor cursor;
        AgendaByDayAdapter dayAdapter;
        int start; // start day of the cursor's coverage, 커서 적용 범위의 시작일
        int end; // end day of the cursor's coverage, 커서 적용 범위의 종료일
        int offset; // offset in position in the list view, 리스트뷰에서 위치의 offset
        int size; // dayAdapter.getCount()

        public DayAdapterInfo(Context context) {
            dayAdapter = new AgendaByDayAdapter(context);
        }

        @Override
        public String toString() {
            // Static class, so the time in this toString will not reflect the
            // home tz settings. This should only affect debugging.
            // 정적 클래스이기 때문에 toString은 시간대 설정이 반영되지 않을 것임
            // 디버깅에만 영향을 미침
            Time time = new Time();
            StringBuilder sb = new StringBuilder();
            time.setJulianDay(start);
            time.normalize(false);
            sb.append("Start:").append(time.toString());
            time.setJulianDay(end);
            time.normalize(false);
            sb.append(" End:").append(time.toString());
            sb.append(" Offset:").append(offset);
            sb.append(" Size:").append(size);
            return sb.toString();
        }
    }

    private class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (DEBUGLOG) {
                Log.d(TAG, "(+)onQueryComplete");
            }
            QuerySpec data = (QuerySpec)cookie;

            if (cursor == null) {
                if (mAgendaListView != null && mAgendaListView.getContext() instanceof Activity) {
                    ((Activity) mAgendaListView.getContext()).finish();
                }
                return;
            }

            if (BASICLOG) {
                long queryEndMillis = System.nanoTime();
                Log.e(TAG, "Query time(ms): "
                        + (queryEndMillis - data.queryStartMillis) / 1000000
                        + " Count: " + cursor.getCount());
            }

            if (data.queryType == QUERY_TYPE_CLEAN) {
                mCleanQueryInitiated = false;
            }

            if (mShuttingDown) {
                cursor.close();
                return;
            }

            // Notify Listview of changes and update position
            // ListView의 변화 알림 및 position 업데이트
            int cursorSize = cursor.getCount();
            if (cursorSize > 0 || mAdapterInfos.isEmpty() || data.queryType == QUERY_TYPE_CLEAN) {
                final int listPositionOffset = processNewCursor(data, cursor);
                int newPosition = -1;
                if (data.goToTime == null) { // Typical Scrolling type query
                    notifyDataSetChanged();
                    if (listPositionOffset != 0) {
                        mAgendaListView.shiftSelection(listPositionOffset);
                    }
                } else { // refresh() called. Go to the designated position, 지정된 위치로 이동
                    final Time goToTime = data.goToTime;
                    notifyDataSetChanged();
                    newPosition = findEventPositionNearestTime(goToTime, data.id);
                    if (newPosition >= 0) {
                        if (mListViewScrollState == OnScrollListener.SCROLL_STATE_FLING) {
                            mAgendaListView.smoothScrollBy(0, 0);
                        }
                        mAgendaListView.setSelectionFromTop(newPosition + OFF_BY_ONE_BUG,
                                mStickyHeaderSize);
                        Time actualTime = new Time(mTimeZone);
                        actualTime.set(goToTime);
                        if (DEBUGLOG) {
                            Log.d(TAG, "onQueryComplete: Updating title...");
                        }
                        CalendarController.getInstance(mContext).sendEvent(this,
                                EventType.UPDATE_TITLE, actualTime, actualTime, -1,
                                ViewType.CURRENT);
                    }
                    if (DEBUGLOG) {
                        Log.e(TAG, "Setting listview to " +
                                "findEventPositionNearestTime: " + (newPosition + OFF_BY_ONE_BUG));
                    }
                }

                // Make sure we change the selected instance Id only on a clean query and we
                // do not have one set already
                // 선택한 인스턴스 ID만 새로 변경하고, 이미 가지고 있는지 확인함
                if (mSelectedInstanceId == -1 && newPosition != -1 &&
                        data.queryType == QUERY_TYPE_CLEAN) {
                    if (data.id != -1 || data.goToTime != null) {
                        mSelectedInstanceId = findInstanceIdFromPosition(newPosition);
                    }
                }

                // size == 1 means a fresh query. Possibly after the data changed.
                // Let's check whether mSelectedInstanceId is still valid.
                // size == 1: 새로운 쿼리, 데이터가 변경된 후일 수 있음
                // mSelectedInstanceId가 유효한지 체크함
                if (mAdapterInfos.size() == 1 && mSelectedInstanceId != -1) {
                    boolean found = false;
                    cursor.moveToPosition(-1);
                    while (cursor.moveToNext()) {
                        if (mSelectedInstanceId == cursor
                                .getLong(AgendaWindowAdapter.INDEX_INSTANCE_ID)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        mSelectedInstanceId = -1;
                    }
                }

                // Show the requested event, 요청된 이벤트 표시
                if (mShowEventOnStart && data.queryType == QUERY_TYPE_CLEAN) {
                    Cursor tempCursor = null;
                    int tempCursorPosition = -1;

                    // If no valid event is selected , just pick the first one
                    // 유효한 이벤트가 선택되지 않은 경우, 첫 번째 이벤트 선택
                    if (mSelectedInstanceId == -1) {
                        if (cursor.moveToFirst()) {
                            mSelectedInstanceId = cursor
                                    .getLong(AgendaWindowAdapter.INDEX_INSTANCE_ID);
                            // Set up a dummy view holder so we have the right all day
                            // info when the view is created.
                            // 더비 view holder를 설정하여 view가 생셩될 때 계속 옳은 정보를 얻을 수 있게(?)
                            // TODO determine the full set of what might be useful to
                            // know about the selected view and fill it in.
                            mSelectedVH = new AgendaAdapter.ViewHolder();
                            mSelectedVH.allDay =
                                    cursor.getInt(AgendaWindowAdapter.INDEX_ALL_DAY) != 0;
                            tempCursor = cursor;
                        }
                    } else if (newPosition != -1) {
                        tempCursor = getCursorByPosition(newPosition);
                        tempCursorPosition = getCursorPositionByPosition(newPosition);
                    }
                    if (tempCursor != null) {
                        AgendaItem item = buildAgendaItemFromCursor(tempCursor, tempCursorPosition,
                                false);
                        long selectedTime = findStartTimeFromPosition(newPosition);
                        if (DEBUGLOG) {
                            Log.d(TAG, "onQueryComplete: Sending View Event...");
                        }
                        sendViewEvent(item, selectedTime);
                    }
                }
            } else {
                cursor.close();
            }

            // Update header and footer
            // header와 footer 업데이트
            if (!mDoneSettingUpHeaderFooter) {
                OnClickListener headerFooterOnClickListener = new OnClickListener() {
                    public void onClick(View v) {
                        if (v == mHeaderView) {
                            queueQuery(new QuerySpec(QUERY_TYPE_OLDER));
                        } else {
                            queueQuery(new QuerySpec(QUERY_TYPE_NEWER));
                        }
                    }};
                mHeaderView.setOnClickListener(headerFooterOnClickListener);
                mFooterView.setOnClickListener(headerFooterOnClickListener);
                mAgendaListView.addFooterView(mFooterView);
                mDoneSettingUpHeaderFooter = true;
            }
            synchronized (mQueryQueue) {
                int totalAgendaRangeStart = -1;
                int totalAgendaRangeEnd = -1;

                if (cursorSize != 0) {
                    // Remove the query that just completed
                    // 방금 완료한 쿼리 제거
                    QuerySpec x = mQueryQueue.poll();
                    if (BASICLOG && !x.equals(data)) {
                        Log.e(TAG, "onQueryComplete - cookie != head of queue");
                    }
                    mEmptyCursorCount = 0;
                    if (data.queryType == QUERY_TYPE_NEWER) {
                        mNewerRequestsProcessed++;
                    } else if (data.queryType == QUERY_TYPE_OLDER) {
                        mOlderRequestsProcessed++;
                    }

                    totalAgendaRangeStart = mAdapterInfos.getFirst().start;
                    totalAgendaRangeEnd = mAdapterInfos.getLast().end;
                } else { // CursorSize == 0
                    QuerySpec querySpec = mQueryQueue.peek();

                    // Update Adapter Info with new start and end date range
                    // 어댑터 정보를 새로운 시작일 - 종료일 범위로 업데이트
                    if (!mAdapterInfos.isEmpty()) {
                        DayAdapterInfo first = mAdapterInfos.getFirst();
                        DayAdapterInfo last = mAdapterInfos.getLast();

                        if (first.start - 1 <= querySpec.end && querySpec.start < first.start) {
                            first.start = querySpec.start;
                        }

                        if (querySpec.start <= last.end + 1 && last.end < querySpec.end) {
                            last.end = querySpec.end;
                        }

                        totalAgendaRangeStart = first.start;
                        totalAgendaRangeEnd = last.end;
                    } else {
                        totalAgendaRangeStart = querySpec.start;
                        totalAgendaRangeEnd = querySpec.end;
                    }

                    // Update query specification with expanded search range
                    // and maybe rerun query
                    // 검색 범위가 확장되고 쿼리가 다시 실행될 수 있는 쿼리 사양? 업데이트
                    switch (querySpec.queryType) {
                        case QUERY_TYPE_OLDER:
                            totalAgendaRangeStart = querySpec.start;
                            querySpec.start -= MAX_QUERY_DURATION;
                            break;
                        case QUERY_TYPE_NEWER:
                            totalAgendaRangeEnd = querySpec.end;
                            querySpec.end += MAX_QUERY_DURATION;
                            break;
                        case QUERY_TYPE_CLEAN:
                            totalAgendaRangeStart = querySpec.start;
                            totalAgendaRangeEnd = querySpec.end;
                            querySpec.start -= MAX_QUERY_DURATION / 2;
                            querySpec.end += MAX_QUERY_DURATION / 2;
                            break;
                    }

                    if (++mEmptyCursorCount > RETRIES_ON_NO_DATA) {
                        // Nothing in the cursor again. Dropping query
                        // 커서에 아무것도 없다면 쿼리를 삭제
                        mQueryQueue.poll();
                    }
                }

                updateHeaderFooter(totalAgendaRangeStart, totalAgendaRangeEnd);

                // Go over the events and mark the first day after yesterday
                // that has events in it
                // 이벤트를 검토하면서 어제 이후로 이벤트가 있는 첫 번째 날짜를 표시하기
                // If the range of adapters doesn't include yesterday, skip marking it since it will
                // mark the first day in the adapters.
                // 어댑터 범위에 어제가 포함되지 않은 경우, 어댑터에서 첫날이 표시될 것이기 때문에 건너뜀
                synchronized (mAdapterInfos) {
                    DayAdapterInfo info = mAdapterInfos.getFirst();
                    Time time = new Time(mTimeZone);
                    long now = System.currentTimeMillis();
                    time.set(now);
                    int JulianToday = Time.getJulianDay(now, time.gmtoff);
                    if (info != null && JulianToday >= info.start && JulianToday
                            <= mAdapterInfos.getLast().end) {
                        Iterator<DayAdapterInfo> iter = mAdapterInfos.iterator();
                        boolean foundDay = false;
                        while (iter.hasNext() && !foundDay) {
                            info = iter.next();
                            for (int i = 0; i < info.size; i++) {
                                if (info.dayAdapter.findJulianDayFromPosition(i) >= JulianToday) {
                                    info.dayAdapter.setAsFirstDayAfterYesterday(i);
                                    foundDay = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                // Fire off the next query if any
                // 다음 쿼리가 있는 경우 종료
                Iterator<QuerySpec> it = mQueryQueue.iterator();
                while (it.hasNext()) {
                    QuerySpec queryData = it.next();
                    if (queryData.queryType == QUERY_TYPE_CLEAN
                            || !isInRange(queryData.start, queryData.end)) {
                        // Query accepted
                        if (DEBUGLOG) Log.e(TAG, "Query accepted. QueueSize:" + mQueryQueue.size());
                        doQuery(queryData);
                        break;
                    } else {
                        // Query rejected
                        it.remove();
                        if (DEBUGLOG) Log.e(TAG, "Query rejected. QueueSize:" + mQueryQueue.size());
                    }
                }
            }
            if (BASICLOG) {
                for (DayAdapterInfo info3 : mAdapterInfos) {
                    Log.e(TAG, "> " + info3.toString());
                }
            }
        }

        /*
         * Update the adapter info array with a the new cursor. Close out old
         * cursors as needed.
         * 새로운 커서로 어댑터 정보 배열을 업데이트함
         * 필요에 따라서 이전 커서를 닫음
         *
         * @return number of rows removed from the beginning
         *         처음부터 제거된 행의 수
         */
        private int processNewCursor(QuerySpec data, Cursor cursor) {
            synchronized (mAdapterInfos) {
                // Remove adapter info's from adapterInfos as needed
                // 필요에 따라 adapterInfos에서 어댑터 정보 제거
                DayAdapterInfo info = pruneAdapterInfo(data.queryType);
                int listPositionOffset = 0;
                if (info == null) {
                    info = new DayAdapterInfo(mContext);
                } else {
                    if (DEBUGLOG)
                        Log.e(TAG, "processNewCursor listPositionOffsetA="
                                + -info.size);
                    listPositionOffset = -info.size;
                }

                // Setup adapter info
                // 어댑터 정보 설정
                info.start = data.start;
                info.end = data.end;
                info.cursor = cursor;
                info.dayAdapter.changeCursor(info);
                info.size = info.dayAdapter.getCount();

                // Insert into adapterInfos
                if (mAdapterInfos.isEmpty()
                        || data.end <= mAdapterInfos.getFirst().start) {
                    mAdapterInfos.addFirst(info);
                    listPositionOffset += info.size;
                } else if (BASICLOG && data.start < mAdapterInfos.getLast().end) {
                    mAdapterInfos.addLast(info);
                    for (DayAdapterInfo info2 : mAdapterInfos) {
                        Log.e("========== BUG ==", info2.toString());
                    }
                } else {
                    mAdapterInfos.addLast(info);
                }

                // Update offsets in adapterInfos
                // 오프셋 업데이트
                mRowCount = 0;
                for (DayAdapterInfo info3 : mAdapterInfos) {
                    info3.offset = mRowCount;
                    mRowCount += info3.size;
                }
                mLastUsedInfo = null;

                return listPositionOffset;
            }
        }
    }
}
