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

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.provider.CalendarContract.Attendees;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.android.nanal.R;
import com.android.nanal.agenda.AgendaAdapter.ViewHolder;
import com.android.nanal.agenda.AgendaWindowAdapter.AgendaItem;
import com.android.nanal.agenda.AgendaWindowAdapter.DayAdapterInfo;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarController.EventType;
import com.android.nanal.event.DeleteEventHelper;
import com.android.nanal.event.Utils;

public class AgendaListView extends ListView implements OnItemClickListener {

    private static final String TAG = "AgendaListView";
    private static final boolean DEBUG = false;
    private static final int EVENT_UPDATE_TIME = 300000;  // 5 minutes

    private AgendaWindowAdapter mWindowAdapter;
    private DeleteEventHelper mDeleteEventHelper;
    private Context mContext;
    private String mTimeZone;
    private Time mTime;
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(mContext, this);
            mTime.switchTimezone(mTimeZone);
        }
    };
    private boolean mShowEventDetailsWithAgenda;
    private Handler mHandler = null;
    // runs every midnight and refreshes the view in order to update the past/present
    // separator
    // 매일 자정에 실행하서 과거/현재 구분 기호를 업데이트하기 위해 view 새로 고침
    private final Runnable mMidnightUpdater = new Runnable() {
        @Override
        public void run() {
            refresh(true);
            Utils.setMidnightUpdater(mHandler, mMidnightUpdater, mTimeZone);
        }
    };

    // Runs every EVENT_UPDATE_TIME to gray out past events
    // 모든 EVENT_UPDATE_TIME을 실행하여, 이전 이벤트 회색으로 표시
    private final Runnable mPastEventUpdater = new Runnable() {
        @Override
        public void run() {
            if (updatePastEvents()) {
                refresh(true);
            }
            setPastEventsUpdater();
        }
    };

    public AgendaListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mContext = context;
        mTimeZone = Utils.getTimeZone(context, mTZUpdater);
        mTime = new Time(mTimeZone);
        setOnItemClickListener(this);
        setVerticalScrollBarEnabled(false);
        mWindowAdapter = new AgendaWindowAdapter(context, this,
                Utils.getConfigBool(context, R.bool.show_event_details_with_agenda));
        mWindowAdapter.setSelectedInstanceId(-1/* TODO:instanceId */);
        setAdapter(mWindowAdapter);
        setCacheColorHint(context.getResources().getColor(R.color.agenda_item_not_selected));
        mDeleteEventHelper =
                new DeleteEventHelper(context, null, false /* don't exit when done */);
        mShowEventDetailsWithAgenda = Utils.getConfigBool(mContext,
                R.bool.show_event_details_with_agenda);
        // Hide ListView dividers, they are done in the item views themselves
        // ListView 구분자 숨김, 아이템 뷰 자체에서 수행됨
        setDivider(null);
        setDividerHeight(0);

        mHandler = new Handler();
    }

    // Sets a thread to run every EVENT_UPDATE_TIME in order to update the list
    // with grayed out past events
    // 회색으로 표시된 과거 이벤트로 목록을 업데이트하기 위하여,
    // 모든 EVENT_UPDATE_TIME을 실행할 스레드를 설정
    private void setPastEventsUpdater() {

        // Run the thread in the nearest rounded EVENT_UPDATE_TIME
        // 가장 가까운 반올림 EVENT_UPDATE_TIME에서 스레드를 실행
        long now = System.currentTimeMillis();
        long roundedTime = (now / EVENT_UPDATE_TIME) * EVENT_UPDATE_TIME;
        mHandler.removeCallbacks(mPastEventUpdater);
        mHandler.postDelayed(mPastEventUpdater, EVENT_UPDATE_TIME - (now - roundedTime));
    }

    // Stop the past events thread
    // 과거 이벤트 스레드 중지
    private void resetPastEventsUpdater() {
        mHandler.removeCallbacks(mPastEventUpdater);
    }

    // Go over all visible views and checks if all past events are grayed out.
    // Returns true is there is at least one event that ended and it is not
    // grayed out.
    // 모든 보이는 뷰를 검토하고, 과거 이벤트가 모두 회색으로 표시되는지 확인함
    // true가 반환됐다는 건, 회색으로 표시되지 않은 종료된 이벤트가 하나 이상 있는 것임
    private boolean updatePastEvents() {

        int childCount = getChildCount();
        boolean needUpdate = false;
        long now = System.currentTimeMillis();
        Time time = new Time(mTimeZone);
        time.set(now);
        int todayJulianDay = Time.getJulianDay(now, time.gmtoff);

        // Go over views in list
        // 리스트에서 view 검토
        for (int i = 0; i < childCount; ++i) {
            View listItem = getChildAt(i);
            Object o = listItem.getTag();
            if (o instanceof AgendaByDayAdapter.ViewHolder) {
                // day view - check if day in the past and not grayed yet
                // 일 view - 과거 날짜인지, 그리고 회색으로 표시되지 않았는지 확인
                AgendaByDayAdapter.ViewHolder holder = (AgendaByDayAdapter.ViewHolder) o;
                if (holder.julianDay <= todayJulianDay && !holder.grayed) {
                    needUpdate = true;
                    break;
                }
            } else if (o instanceof AgendaAdapter.ViewHolder) {
                // meeting view - check if event in the past or started already and not grayed yet
                // All day meetings for a day are grayed out
                // 회의?일정? view - 이벤트가 과거에 시작되었거나, 이미 시작되었고 아직 회색으로 표시되지 않았는지 확인
                // 하루 종일 일정이 회색으로 됨
                AgendaAdapter.ViewHolder holder = (AgendaAdapter.ViewHolder) o;
                if (!holder.grayed && ((!holder.allDay && holder.startTimeMilli <= now) ||
                        (holder.allDay && holder.julianDay <= todayJulianDay))) {
                    needUpdate = true;
                    break;
                }
            }
        }
        return needUpdate;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWindowAdapter.close();
    }

    // Implementation of the interface OnItemClickListener
    // OnItemClickListenr 인터페이스 구현
    @Override
    public void onItemClick(AdapterView<?> a, View v, int position, long id) {
        if (id != -1) {
            // Switch to the EventInfo view
            AgendaItem item = mWindowAdapter.getAgendaItemByPosition(position);
            long oldInstanceId = mWindowAdapter.getSelectedInstanceId();
            mWindowAdapter.setSelectedView(v);

            // If events are shown to the side of the agenda list , do nothing
            // when the same event is selected , otherwise show the selected event.
            // 이벤트가 agenda 리스트의 측면에 표시되는 경우,
            // 동일한 이벤트를 선택할 때는 아무것도 하지 않고, 그렇지 않으면 선택된 이벤트를 표시함

            if (item != null && (oldInstanceId != mWindowAdapter.getSelectedInstanceId() ||
                    !mShowEventDetailsWithAgenda)) {
                long startTime = item.begin;
                long endTime = item.end;
                // Holder in view holds the start of the specific part of a multi-day event ,
                // use it for the goto
                // view의 홀더는 여러 날 걸친 이벤트의 특정한 시작을 유지함, goto에서 사용함
                long holderStartTime;
                Object holder = v.getTag();
                if (holder instanceof AgendaAdapter.ViewHolder) {
                    holderStartTime = ((AgendaAdapter.ViewHolder) holder).startTimeMilli;
                } else {
                    holderStartTime = startTime;
                }
                if (item.allDay) {
                    startTime = Utils.convertAlldayLocalToUTC(mTime, startTime, mTimeZone);
                    endTime = Utils.convertAlldayLocalToUTC(mTime, endTime, mTimeZone);
                }
                mTime.set(startTime);
                CalendarController controller = CalendarController.getInstance(mContext);
                controller.sendEventRelatedEventWithExtra(this, EventType.VIEW_EVENT, item.id,
                        startTime, endTime, 0, 0, CalendarController.EventInfo.buildViewExtraLong(
                                Attendees.ATTENDEE_STATUS_NONE, item.allDay), holderStartTime);
            }
        }
    }

    public void goTo(Time time, long id, String searchQuery, boolean forced,
                     boolean refreshEventInfo) {
        if (time == null) {
            time = mTime;
            long goToTime = getFirstVisibleTime(null);
            if (goToTime <= 0) {
                goToTime = System.currentTimeMillis();
            }
            time.set(goToTime);
        }
        mTime.set(time);
        mTime.switchTimezone(mTimeZone);
        mTime.normalize(true);
        if (DEBUG) {
            Log.d(TAG, "Goto with time " + mTime.toString());
        }
        mWindowAdapter.refresh(mTime, id, searchQuery, forced, refreshEventInfo);
    }

    public void refresh(boolean forced) {
        mWindowAdapter.refresh(mTime, -1, null, forced, false);
    }

    public void deleteSelectedEvent() {
        int position = getSelectedItemPosition();
        AgendaItem agendaItem = mWindowAdapter.getAgendaItemByPosition(position);
        if (agendaItem != null) {
            mDeleteEventHelper.delete(agendaItem.begin, agendaItem.end, agendaItem.id, -1);
        }
    }

    public View getFirstVisibleView() {
        Rect r = new Rect();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View listItem = getChildAt(i);
            listItem.getLocalVisibleRect(r);
            if (r.top >= 0) { // if visible
                return listItem;
            }
        }
        return null;
    }

    public long getSelectedTime() {
        int position = getSelectedItemPosition();
        if (position >= 0) {
            AgendaItem item = mWindowAdapter.getAgendaItemByPosition(position);
            if (item != null) {
                return item.begin;
            }
        }
        return getFirstVisibleTime(null);
    }

    public AgendaAdapter.ViewHolder getSelectedViewHolder() {
        return mWindowAdapter.getSelectedViewHolder();
    }

    public long getFirstVisibleTime(AgendaItem item) {
        AgendaItem agendaItem = item;
        if (item == null) {
            agendaItem = getFirstVisibleAgendaItem();
        }
        if (agendaItem != null) {
            Time t = new Time(mTimeZone);
            t.set(agendaItem.begin);
            // Save and restore the time since setJulianDay sets the time to 00:00:00
            // setJulianDay가 시간을 00:00:00으로 설정한 이후, 시간 저장 및 복원
            int hour = t.hour;
            int minute = t.minute;
            int second = t.second;
            t.setJulianDay(agendaItem.startDay);
            t.hour = hour;
            t.minute = minute;
            t.second = second;
            if (DEBUG) {
                t.normalize(true);
                Log.d(TAG, "first position had time " + t.toString());
            }
            return t.normalize(false);
        }
        return 0;
    }

    public AgendaItem getFirstVisibleAgendaItem() {
        int position = getFirstVisiblePosition();
        if (DEBUG) {
            Log.v(TAG, "getFirstVisiblePosition = " + position);
        }

        // mShowEventDetailsWithAgenda == true implies we have a sticky header. In that case
        // we may need to take the second visible position, since the first one maybe the one
        // under the sticky header.
        // mShowEventDetailsWithAgenda == true: sticky header를 가지고 있음을 의미함
        // StickyHeader: 리스트 중 하나의 아이템을 상단에 고정시키는 것 (https://black-jin0427.tistory.com/51)
        // 첫 번째는 아마 sticky header 아래에 있을 것이기 때문에, 두 번째 가시적 위치를 잡아야 함
        if (mShowEventDetailsWithAgenda) {
            View v = getFirstVisibleView ();
            if (v != null) {
                Rect r = new Rect ();
                v.getLocalVisibleRect(r);
                if (r.bottom - r.top <=  mWindowAdapter.getStickyHeaderHeight()) {
                    position ++;
                }
            }
        }

        return mWindowAdapter.getAgendaItemByPosition(position,
                false /* startDay = date separator date instead of actual event startday */);

    }

    public int getJulianDayFromPosition(int position) {
        DayAdapterInfo info = mWindowAdapter.getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.findJulianDayFromPosition(position - info.offset);
        }
        return 0;
    }

    // Finds is a specific event (defined by start time and id) is visible
    // Finds는 특정 이벤트(시작 시간과 아이디로 정의됨)를 표시함
    public boolean isAgendaItemVisible(Time startTime, long id) {

        if (id == -1 || startTime == null) {
            return false;
        }

        View child = getChildAt(0);
        // View not set yet, so not child - return
        if (child == null) {
            return false;
        }
        int start = getPositionForView(child);
        long milliTime = startTime.toMillis(true);
        int childCount = getChildCount();
        int eventsInAdapter = mWindowAdapter.getCount();

        for (int i = 0; i < childCount; i++) {
            if (i + start >= eventsInAdapter) {
                break;
            }
            AgendaItem agendaItem = mWindowAdapter.getAgendaItemByPosition(i + start);
            if (agendaItem == null) {
                continue;
            }
            if (agendaItem.id == id && agendaItem.begin == milliTime) {
                View listItem = getChildAt(i);
                if (listItem.getTop() <= getHeight() &&
                        listItem.getTop() >= mWindowAdapter.getStickyHeaderHeight()) {
                    return true;
                }
            }
        }
        return false;
    }

    public long getSelectedInstanceId() {
        return mWindowAdapter.getSelectedInstanceId();
    }

    public void setSelectedInstanceId(long id) {
        mWindowAdapter.setSelectedInstanceId(id);
    }

    // Move the currently selected or visible focus down by offset amount.
    // offset could be negative.
    // 현재 선택되었거나 가시적인 포ㅓ스를 오프셋만큼 아래로 이동시킴
    // 오프셋은 음수가 될 수 있음
    public void shiftSelection(int offset) {
        shiftPosition(offset);
        int position = getSelectedItemPosition();
        if (position != INVALID_POSITION) {
            setSelectionFromTop(position + offset, 0);
        }
    }

    private void shiftPosition(int offset) {
        if (DEBUG) {
            Log.v(TAG, "Shifting position " + offset);
        }

        View firstVisibleItem = getFirstVisibleView();

        if (firstVisibleItem != null) {
            Rect r = new Rect();
            firstVisibleItem.getLocalVisibleRect(r);
            // if r.top is < 0, getChildAt(0) and getFirstVisiblePosition() is
            // returning an item above the first visible item.
            // 만약 r.top이 < 0이면, getChildAt(0)과 getFirstVisiblePosition()은
            // 첫 번째 가시적 항목 위에 있는 아이템을 반환한다
            int position = getPositionForView(firstVisibleItem);
            setSelectionFromTop(position + offset, r.top > 0 ? -r.top : r.top);
            if (DEBUG) {
                if (firstVisibleItem.getTag() instanceof AgendaAdapter.ViewHolder) {
                    ViewHolder viewHolder = (AgendaAdapter.ViewHolder) firstVisibleItem.getTag();
                    Log.v(TAG, "Shifting from " + position + " by " + offset + ". Title "
                            + viewHolder.title.getText());
                } else if (firstVisibleItem.getTag() instanceof AgendaByDayAdapter.ViewHolder) {
                    AgendaByDayAdapter.ViewHolder viewHolder =
                            (AgendaByDayAdapter.ViewHolder) firstVisibleItem.getTag();
                    Log.v(TAG, "Shifting from " + position + " by " + offset + ". Date  "
                            + viewHolder.dateView.getText());
                } else if (firstVisibleItem instanceof TextView) {
                    Log.v(TAG, "Shifting: Looking at header here. " + getSelectedItemPosition());
                }
            }
        } else if (getSelectedItemPosition() >= 0) {
            if (DEBUG) {
                Log.v(TAG, "Shifting selection from " + getSelectedItemPosition() +
                        " by " + offset);
            }
            setSelection(getSelectedItemPosition() + offset);
        }
    }

    public void setHideDeclinedEvents(boolean hideDeclined) {
        mWindowAdapter.setHideDeclinedEvents(hideDeclined);
    }

    public void onResume() {
        mTZUpdater.run();
        Utils.setMidnightUpdater(mHandler, mMidnightUpdater, mTimeZone);
        setPastEventsUpdater();
        mWindowAdapter.onResume();
    }

    public void onPause() {
        Utils.resetMidnightUpdater(mHandler, mMidnightUpdater);
        resetPastEventsUpdater();
    }
}
