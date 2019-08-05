package com.android.nanal.agenda;

/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.database.Cursor;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.nanal.DynamicTheme;
import com.android.nanal.R;
import com.android.nanal.agenda.AgendaWindowAdapter.DayAdapterInfo;
import com.android.nanal.event.Utils;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;


public class AgendaByDayAdapter extends BaseAdapter {
    static final int TYPE_LAST = 2;
    private static final int TYPE_DAY = 0;
    private static final int TYPE_MEETING = 1;
    private final Context mContext;
    private final AgendaAdapter mAgendaAdapter;
    private final LayoutInflater mInflater;
    // Note: Formatter is not thread safe. Fine for now as it is only used by the main thread.
    private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;
    private ArrayList<RowInfo> mRowInfo;
    private int mTodayJulianDay;
    private Time mTmpTime;
    private String mTimeZone;
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(mContext, this);
            mTmpTime = new Time(mTimeZone);
            notifyDataSetChanged();
        }
    };

    public AgendaByDayAdapter(Context context) {
        mContext = context;
        mAgendaAdapter = new AgendaAdapter(context, R.layout.agenda_item);
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
        mTimeZone = Utils.getTimeZone(context, mTZUpdater);
        mTmpTime = new Time(mTimeZone);
    }

    public long getInstanceId(int position) {
        if (mRowInfo == null || position >= mRowInfo.size()) {
            return -1;
        }
        return mRowInfo.get(position).mInstanceId;
    }

    public long getStartTime(int position) {
        if (mRowInfo == null || position >= mRowInfo.size()) {
            return -1;
        }
        return mRowInfo.get(position).mEventStartTimeMilli;
    }

    // Returns the position of a header of a specific item
    // 특정 항목의 헤더 위치를 반환
    public int getHeaderPosition(int position) {
        if (mRowInfo == null || position >= mRowInfo.size()) {
            return -1;
        }

        for (int i = position; i >=0; i --) {
            RowInfo row = mRowInfo.get(i);
            if (row != null && row.mType == TYPE_DAY)
                return i;
        }
        return -1;
    }

    // Returns the number of items in a section defined by a specific header location
    // 특정 헤더 위치로 정의된 섹션의 아이템 수 반환
    public int getHeaderItemsCount(int position) {
        if (mRowInfo == null) {
            return -1;
        }
        int count = 0;
        for (int i = position +1; i < mRowInfo.size(); i++) {
            if (mRowInfo.get(i).mType != TYPE_MEETING) {
                return count;
            }
            count ++;
        }
        return count;
    }

    @Override
    public int getCount() {
        if (mRowInfo != null) {
            return mRowInfo.size();
        }
        return mAgendaAdapter.getCount();
    }

    @Override
    public Object getItem(int position) {
        if (mRowInfo != null) {
            RowInfo row = mRowInfo.get(position);
            if (row.mType == TYPE_DAY) {
                return row;
            } else {
                return mAgendaAdapter.getItem(row.mPosition);
            }
        }
        return mAgendaAdapter.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        if (mRowInfo != null) {
            RowInfo row = mRowInfo.get(position);
            if (row.mType == TYPE_DAY) {
                return -position;
            } else {
                return mAgendaAdapter.getItemId(row.mPosition);
            }
        }
        return mAgendaAdapter.getItemId(position);
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_LAST;
    }

    @Override
    public int getItemViewType(int position) {
        return mRowInfo != null && mRowInfo.size() > position ?
                mRowInfo.get(position).mType : TYPE_DAY;
    }

    public boolean isDayHeaderView(int position) {
        return (getItemViewType(position) == TYPE_DAY);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if ((mRowInfo == null) || (position > mRowInfo.size())) {
            // If we have no row info, mAgendaAdapter returns the view.
            // 행 정보가 없으면, mAgendaAdapter가 view를 반환함
            return mAgendaAdapter.getView(position, convertView, parent);
        }

        DynamicTheme theme = new DynamicTheme();
        RowInfo row = mRowInfo.get(position);
        if (row.mType == TYPE_DAY) {
            ViewHolder holder = null;
            View agendaDayView = null;
            if ((convertView != null) && (convertView.getTag() != null)) {
                // Listview may get confused and pass in a different type of
                // view since we keep shifting data around. Not a big problem.
                // 계속 데이터를 이동하기 때문에 ListView는 혼란스러워지고, 다른 타입의 view로 전달될 수 있음
                Object tag = convertView.getTag();
                if (tag instanceof ViewHolder) {
                    agendaDayView = convertView;
                    holder = (ViewHolder) tag;
                    holder.julianDay = row.mDay;
                }
            }

            if (holder == null) {
                // Create a new AgendaView with a ViewHolder for fast access to
                // views w/o calling findViewById()
                // findViewById()를 호출하는 뷰에 빠르게 접근하기 위한 ViewHolder를 통해 새로운 AgendaView 생성
                holder = new ViewHolder();
                agendaDayView = mInflater.inflate(R.layout.agenda_day, parent, false);
                holder.dayView = (TextView) agendaDayView.findViewById(R.id.day);
                holder.dateView = (TextView) agendaDayView.findViewById(R.id.date);
                holder.julianDay = row.mDay;
                holder.grayed = false;
                agendaDayView.setTag(holder);
            }

            // Re-use the member variable "mTime" which is set to the local
            // time zone.
            // It's difficult to find and update all these adapters when the
            // home tz changes so check it here and update if needed.
            // 로컬 표준시로 설정된 멤버 변수 "mTime"을 재사용
            // home tz가 변경되면 어댑터를 전부 찾아서 업데이트하는 게 어렵기 때문에 여기서 확인하고 필요한 경우 업데이트
            String tz = Utils.getTimeZone(mContext, mTZUpdater);
            if (!TextUtils.equals(tz, mTmpTime.timezone)) {
                mTimeZone = tz;
                mTmpTime = new Time(tz);
            }

            // Build the text for the day of the week.
            // Should be yesterday/today/tomorrow (if applicable) + day of the week
            // 요일에 대한 텍스트 작성
            // 어제/오늘/내일(해당하는 경우) + 요일

            Time date = mTmpTime;
            long millis = date.setJulianDay(row.mDay);
            int flags = DateUtils.FORMAT_SHOW_WEEKDAY;
            mStringBuilder.setLength(0);

            String dayViewText = Utils.getDayOfWeekString(row.mDay, mTodayJulianDay, millis,
                    mContext);

            // Build text for the date
            // Format should be month day
            // 날짜에 대한 텍스트 작성
            // 형식은 월 일

            mStringBuilder.setLength(0);
            flags = DateUtils.FORMAT_SHOW_DATE;
            String dateViewText = DateUtils.formatDateRange(mContext, mFormatter, millis, millis,
                    flags, mTimeZone).toString();

            if (AgendaWindowAdapter.BASICLOG) {
                dayViewText += " P:" + position;
                dateViewText += " P:" + position;
            }
            holder.dayView.setText(dayViewText);
            holder.dateView.setText(dateViewText);

            // Set the background of the view, it is grayed for day that are in the past and today
            // view의 백그라운드 설정, 과거와 현재라면 회색으로 표시함
            if (row.mDay > mTodayJulianDay) {
                agendaDayView.setBackgroundResource(theme.getDrawableId(mContext, "agenda_item_bg_primary"));
                holder.grayed = false;
            } else {
                agendaDayView.setBackgroundResource(theme.getDrawableId(mContext, "agenda_item_bg_secondary"));
                holder.grayed = true;
            }
            return agendaDayView;
        } else if (row.mType == TYPE_MEETING) {
            View itemView = mAgendaAdapter.getView(row.mPosition, convertView, parent);
            AgendaAdapter.ViewHolder holder = ((AgendaAdapter.ViewHolder) itemView.getTag());
            TextView title = holder.title;
            // The holder in the view stores information from the cursor, but the cursor has no
            // notion of multi-day event and the start time of each instance of a multi-day event
            // is the same.  RowInfo has the correct info , so take it from there.
            // view에 있는 홀더는 커서의 정보를 저장하지만, 커서에는 여러 날짜에 걸치는 이벤트의 개념이 없고,
            // 여러 날짜에 걸치는 이벤트의 각 인스턴스의 시작 시간은 동일함
            // RowInfo는 정확한 정보를 가지고 있으니 그걸 사용함
            holder.startTimeMilli = row.mEventStartTimeMilli;
            boolean allDay = holder.allDay;
            if (AgendaWindowAdapter.BASICLOG) {
                title.setText(title.getText() + " P:" + position);
            } else {
                title.setText(title.getText());
            }

            // if event in the past or started already, un-bold the title and set the background
            // 과거 이벤트거나 이미 시작했다면, 제목의 bold를 풀고 백그라운드 설정
            if ((!allDay && row.mEventStartTimeMilli <= System.currentTimeMillis()) ||
                    (allDay && row.mDay <= mTodayJulianDay)) {
                itemView.setBackgroundResource(theme.getDrawableId(mContext, "agenda_item_bg_secondary"));
                title.setTypeface(Typeface.DEFAULT);
                holder.grayed = true;
            } else {
                itemView.setBackgroundResource(theme.getDrawableId(mContext, "agenda_item_bg_primary"));
                title.setTypeface(Typeface.DEFAULT_BOLD);
                holder.grayed = false;
            }
            holder.julianDay = row.mDay;
            return itemView;
        } else {
            // Error
            throw new IllegalStateException("Unknown event type:" + row.mType);
        }
    }

    public void clearDayHeaderInfo() {
        mRowInfo = null;
    }

    public void changeCursor(DayAdapterInfo info) {
        calculateDays(info);
        mAgendaAdapter.changeCursor(info.cursor);
    }

    public void calculateDays(DayAdapterInfo dayAdapterInfo) {
        Cursor cursor = dayAdapterInfo.cursor;
        ArrayList<RowInfo> rowInfo = new ArrayList<RowInfo>();
        int prevStartDay = -1;

        Time tempTime = new Time(mTimeZone);
        long now = System.currentTimeMillis();
        tempTime.set(now);
        mTodayJulianDay = Time.getJulianDay(now, tempTime.gmtoff);

        LinkedList<MultipleDayInfo> multipleDayList = new LinkedList<MultipleDayInfo>();
        for (int position = 0; cursor.moveToNext(); position++) {
            int startDay = cursor.getInt(AgendaWindowAdapter.INDEX_START_DAY);
            long id = cursor.getLong(AgendaWindowAdapter.INDEX_EVENT_ID);
            long startTime =  cursor.getLong(AgendaWindowAdapter.INDEX_BEGIN);
            long endTime =  cursor.getLong(AgendaWindowAdapter.INDEX_END);
            long instanceId = cursor.getLong(AgendaWindowAdapter.INDEX_INSTANCE_ID);
            boolean allDay = cursor.getInt(AgendaWindowAdapter.INDEX_ALL_DAY) != 0;
            if (allDay) {
                startTime = Utils.convertAlldayUtcToLocal(tempTime, startTime, mTimeZone);
                endTime = Utils.convertAlldayUtcToLocal(tempTime, endTime, mTimeZone);
            }
            // Skip over the days outside of the adapter's range
            // 어댑터 범위를 벗어난 일수는 건너뛰기
            startDay = Math.max(startDay, dayAdapterInfo.start);
            // Make sure event's start time is not before the start of the day
            // (setJulianDay sets the time to 12:00am)
            // 이벤트 시작 시간이 하루 전인지 확인
            // (setJulianDay는 시간을 자정으로 설정)
            long adapterStartTime = tempTime.setJulianDay(startDay);
            startTime = Math.max(startTime, adapterStartTime);

            if (startDay != prevStartDay) {
                // Check if we skipped over any empty days
                // 비어 있는 날짜를 건너뛰었는지 확인
                if (prevStartDay == -1) {
                    rowInfo.add(new RowInfo(TYPE_DAY, startDay));
                } else {
                    // If there are any multiple-day events that span the empty
                    // range of days, then create day headers and events for
                    // those multiple-day events.
                    // 비어 있는 일 범위에 걸쳐 있는 여러 날짜 이벤트가 있는 경우,
                    // 해당 여러 날짜 이벤트에 대한 일별 헤더 및 이벤트 생성
                    boolean dayHeaderAdded = false;
                    for (int currentDay = prevStartDay + 1; currentDay <= startDay; currentDay++) {
                        dayHeaderAdded = false;
                        Iterator<MultipleDayInfo> iter = multipleDayList.iterator();
                        while (iter.hasNext()) {
                            MultipleDayInfo info = iter.next();
                            // If this event has ended then remove it from the
                            // list.
                            // 이벤트가 종료된 경우 목록에서 제거
                            if (info.mEndDay < currentDay) {
                                iter.remove();
                                continue;
                            }

                            // If this is the first event for the day, then
                            // insert a day header.
                            // 당일의 첫 번째 이벤트라면, 일별 헤더를 삽입
                            if (!dayHeaderAdded) {
                                rowInfo.add(new RowInfo(TYPE_DAY, currentDay));
                                dayHeaderAdded = true;
                            }
                            long nextMidnight = Utils.getNextMidnight(tempTime,
                                    info.mEventStartTimeMilli, mTimeZone);

                            long infoEndTime = (info.mEndDay == currentDay) ?
                                    info.mEventEndTimeMilli : nextMidnight;
                            rowInfo.add(new RowInfo(TYPE_MEETING, currentDay, info.mPosition,
                                    info.mEventId, info.mEventStartTimeMilli,
                                    infoEndTime, info.mInstanceId, info.mAllDay));

                            info.mEventStartTimeMilli = nextMidnight;
                        }
                    }

                    // If the day header was not added for the start day, then
                    // add it now.
                    // 시작일에 일 헤더를 추가하지 않은 경우 추가
                    if (!dayHeaderAdded) {
                        rowInfo.add(new RowInfo(TYPE_DAY, startDay));
                    }
                }
                prevStartDay = startDay;
            }

            // If this event spans multiple days, then add it to the multipleDay
            // list.
            // 이 이벤트가 여러 날에 걸치는 경우,  multipleDay 리스트에 추가
            int endDay = cursor.getInt(AgendaWindowAdapter.INDEX_END_DAY);

            // Skip over the days outside of the adapter's range
            // 어댑터 범위를 벗어난 일수 건너뛰기
            endDay = Math.min(endDay, dayAdapterInfo.end);
            if (endDay > startDay) {
                long nextMidnight = Utils.getNextMidnight(tempTime, startTime, mTimeZone);
                multipleDayList.add(new MultipleDayInfo(position, endDay, id, nextMidnight,
                        endTime, instanceId, allDay));
                // Add in the event for this cursor position - since it is the start of a multi-day
                // event, the end time is midnight
                // 이 커서 위치에 이벤트 추가 - 여러 날에 걸친 이벤트가 시작되기 때문에 종료 시간은 자정임
                rowInfo.add(new RowInfo(TYPE_MEETING, startDay, position, id, startTime,
                        nextMidnight, instanceId, allDay));
            } else {
                // Add in the event for this cursor position
                // 이 커서 위치에 이벤트 추가
                rowInfo.add(new RowInfo(TYPE_MEETING, startDay, position, id, startTime, endTime,
                        instanceId, allDay));
            }
        }

        // There are no more cursor events but we might still have multiple-day
        // events left.  So create day headers and events for those.
        // 커서 이벤트를 종료했지만, 여러 날짜 이벤트가 아직 남아 있을 수 있기 때문에
        // 그것들을 위한 일별 헤더와 이벤트를 만듦
        if (prevStartDay > 0) {
            for (int currentDay = prevStartDay + 1; currentDay <= dayAdapterInfo.end;
                 currentDay++) {
                boolean dayHeaderAdded = false;
                Iterator<MultipleDayInfo> iter = multipleDayList.iterator();
                while (iter.hasNext()) {
                    MultipleDayInfo info = iter.next();
                    // If this event has ended then remove it from the
                    // list.
                    // 이벤트가 종료된 경우 목록에서 제거
                    if (info.mEndDay < currentDay) {
                        iter.remove();
                        continue;
                    }

                    // If this is the first event for the day, then
                    // insert a day header.
                    // 당일 첫 번째 이벤트인 경우 일별 헤더 삽입
                    if (!dayHeaderAdded) {
                        rowInfo.add(new RowInfo(TYPE_DAY, currentDay));
                        dayHeaderAdded = true;
                    }
                    long nextMidnight = Utils.getNextMidnight(tempTime, info.mEventStartTimeMilli,
                            mTimeZone);
                    long infoEndTime =
                            (info.mEndDay == currentDay) ? info.mEventEndTimeMilli : nextMidnight;
                    rowInfo.add(new RowInfo(TYPE_MEETING, currentDay, info.mPosition,
                            info.mEventId, info.mEventStartTimeMilli, infoEndTime,
                            info.mInstanceId, info.mAllDay));

                    info.mEventStartTimeMilli = nextMidnight;
                }
            }
        }
        mRowInfo = rowInfo;
    }

    /**
     * Finds the position in the cursor of the event that best matches the time and Id.
     * It will try to find the event that has the specified id and start time, if such event
     * doesn't exist, it will return the event with a matching id that is closest to the start time.
     * If the id doesn't exist, it will return the event with start time closest to the specified
     * time.
     * 시간 및 ID와 가장 일치하는 이벤트 커서 위치를 찾음
     * 지정된 ID와 시작 시간을 가진 이벤트를 찾기를 시도할 것이며,
     * 그런 이벤트가 없을 경우, 시작 시간과 가장 가까운 ID를 가진 이벤트를 반환함
     * ID가 존재하지 않는 경우, 지정된 시간과 가장 가까운 시작 시간의 이벤트를 반환함
     *
     * @param time - start of event in milliseconds (or any arbitrary time if event id is unknown)
     *             이벤트 시작 시간, 밀리초 단위(이벤트 ID를 알 수 없는 경우에는 임의의 시간)
     * @param id - Event id (-1 if unknown).
     *           이벤트 ID (알 수 없는 경우 -1)
     * @return Position of event (if found) or position of nearest event according to the time.
     *         Zero if no event found
     *         이벤트의 위치(발견된 경우) 또는 시간에 가장 근접한 이벤트의 위치를 반환
     *         이벤트를 찾을 수 없는 경우는 0을 반환
     */
    public int findEventPositionNearestTime(Time time, long id) {
        if (mRowInfo == null) {
            return 0;
        }
        long millis = time.toMillis(false /* use isDst */);
        long minDistance =  Integer.MAX_VALUE;  // some big number
        long idFoundMinDistance =  Integer.MAX_VALUE;  // some big number
        int minIndex = 0;
        int idFoundMinIndex = 0;
        int eventInTimeIndex = -1;
        int allDayEventInTimeIndex = -1;
        int allDayEventDay = 0;
        int minDay = 0;
        boolean idFound = false;
        int len = mRowInfo.size();

        // Loop through the events and find the best match
        // 1. Event id and start time matches requested id and time
        // 2. Event id matches and closest time
        // 3. No event id match , time matches a all day event (midnight)
        // 4. No event id match , time is between event start and end
        // 5. No event id match , all day event
        // 6. The closest event to the requested time

        // 이벤트 루프를 통해 일치하는 최적의 항목 찾기
        // 1. 이벤트 ID 일치, 시작 시간 일치
        // 2. 이벤트 ID 일치, 가장 가까운 시간
        // 3. 이벤트 ID 불일치, 시간은 종일 이벤트와 일치(자정)
        // 4. 이벤트 ID 불일치, 시간이 이벤트의 시작과 종료 사이에 있음
        // 5. 이벤트 ID 불일치, 종일 이벤트
        // 6. 요청된 시간과 가장 가까운 이벤트

        for (int index = 0; index < len; index++) {
            RowInfo row = mRowInfo.get(index);
            if (row.mType == TYPE_DAY) {
                continue;
            }

            // Found exact match - done
            // 정확히 일치 - 완료
            if (row.mEventId == id) {
                if (row.mEventStartTimeMilli == millis) {
                    return index;
                }

                // Not an exact match, Save event index if it is the closest to time so far
                // 정확한 일치가 아님, 가장 가까운 시간이라면 이벤트 index 저장
                long distance = Math.abs(millis - row.mEventStartTimeMilli);
                if (distance < idFoundMinDistance) {
                    idFoundMinDistance = distance;
                    idFoundMinIndex = index;
                }
                idFound = true;
            }
            if (!idFound) {
                // Found an event that contains the requested time
                // 요청된 시간을 포함하는 이벤트 발견
                if (millis >= row.mEventStartTimeMilli && millis <= row.mEventEndTimeMilli) {
                    if (row.mAllDay) {
                        if (allDayEventInTimeIndex == -1) {
                            allDayEventInTimeIndex = index;
                            allDayEventDay = row.mDay;
                        }
                    } else if (eventInTimeIndex == -1){
                        eventInTimeIndex = index;
                    }
                } else if (eventInTimeIndex == -1){
                    // Save event index if it is the closest to time so far
                    // 가장 가까운 시간이라면 이벤트 index 저장
                    long distance = Math.abs(millis - row.mEventStartTimeMilli);
                    if (distance < minDistance) {
                        minDistance = distance;
                        minIndex = index;
                        minDay = row.mDay;
                    }
                }
            }
        }
        // We didn't find an exact match so take the best matching event
        // Closest event with the same id
        // 정확히 일치하는 걸 찾지 못했기 때문에 최선의 이벤트를 찾음
        // ID가 동일한 가장 가까운 이벤트
        if (idFound) {
            return idFoundMinIndex;
        }
        // Event which occurs at the searched time
        // 검색된 시간에 발생하는 이벤트
        if (eventInTimeIndex != -1) {
            return eventInTimeIndex;
            // All day event which occurs at the same day of the searched time as long as there is
            // no regular event at the same day
            // 같은 날 정기 행사가 없는 한... 검색된 시간과 동일한 날짜에 있는 모든 일 이벤트
        } else if (allDayEventInTimeIndex != -1 && minDay != allDayEventDay) {
            return allDayEventInTimeIndex;
        }
        // Closest event
        // 가장 가까운 이벤트
        return minIndex;
    }

    /**
     * Returns a flag indicating if this position is the first day after "yesterday" that has
     * events in it.
     * 이 위치가 어제 이후... 이벤트가 있는 첫 번째 날인지를 나타내는 플래그를 반환
     *
     * @return a flag indicating if this is the "first day after yesterday"
     *          어제 이후 첫날인지를 나타내는 플래그
     */
    public boolean isFirstDayAfterYesterday(int position) {
        int headerPos = getHeaderPosition(position);
        RowInfo row = mRowInfo.get(headerPos);
        if (row != null) {
            return row.mFirstDayAfterYesterday;
        }
        return false;
    }

    /**
     * Finds the Julian day containing the event at the given position.
     * 주어진 위치에서 이벤트가 포함된 줄리안 데이를 찾음
     *
     * @param position the list position of an event
     *                 이벤트의 리스트 위치
     * @return the Julian day containing that event
     *          그 이벤트를 포함하는 줄리안 데이
     */
    public int findJulianDayFromPosition(int position) {
        if (mRowInfo == null || position < 0) {
            return 0;
        }

        int len = mRowInfo.size();
        if (position >= len) return 0;  // no row info at this position
        // 이 위치에 행 정보가 없음

        for (int index = position; index >= 0; index--) {
            RowInfo row = mRowInfo.get(index);
            if (row.mType == TYPE_DAY) {
                return row.mDay;
            }
        }
        return 0;
    }

    /**
     * Marks the current row as the first day that has events after "yesterday".
     * Used to mark the separation between the past and the present/future
     * 현재 행을 어제 이후 이벤트가 있는 첫 번째 날로 표시함
     * 과거와 현재/미래를 분리하여 표시하기 위해서 사용됨
     *
     * @param position in the adapter
     */
    public void setAsFirstDayAfterYesterday(int position) {
        if (mRowInfo == null || position < 0 || position > mRowInfo.size()) {
            return;
        }
        RowInfo row = mRowInfo.get(position);
        row.mFirstDayAfterYesterday = true;
    }

    /**
     * Converts a list position to a cursor position.  The list contains
     * day headers as well as events.  The cursor contains only events.
     * 목록 위치를 커서 위치로 변환
     * 목록에는 이벤트뿐만이 아니라 일별 헤더도 포함되어 있음
     * 커서는 이벤트만 포함하고 있음
     *
     * @param listPos the list position of an event
     *                 이벤트의 리스트 위치
     * @return the corresponding cursor position of that event
     *         if the position point to day header , it will give the position of the next event
     *         negated.
     *         해당 위치의 커서가 일 헤더를 가리킬 경우, 다음 이벤트의 위치가 무시됨
     */
    public int getCursorPosition(int listPos) {
        if (mRowInfo != null && listPos >= 0) {
            RowInfo row = mRowInfo.get(listPos);
            if (row.mType == TYPE_MEETING) {
                return row.mPosition;
            } else {
                int nextPos = listPos + 1;
                if (nextPos < mRowInfo.size()) {
                    nextPos = getCursorPosition(nextPos);
                    if (nextPos >= 0) {
                        return -nextPos;
                    }
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        if (mRowInfo != null && position < mRowInfo.size()) {
            RowInfo row = mRowInfo.get(position);
            return row.mType == TYPE_MEETING;
        }
        return true;
    }

    static class ViewHolder {
        TextView dayView;
        TextView dateView;
        int julianDay;
        boolean grayed;
    }

    private static class RowInfo {
        // mType is either a day header (TYPE_DAY) or an event (TYPE_MEETING)
        // mType은 일 헤더(TYPE_DAY) 또는 이벤트(TYPE_MEETING)
        final int mType;

        final int mDay;          // Julian day 줄리안 데이
        final int mPosition;     // cursor position (not used for TYPE_DAY) 커서 위치(TYPE_DAY에서는 사용 X)
        final long mEventId;
        final long mEventStartTimeMilli;
        final long mEventEndTimeMilli;
        final long mInstanceId;
        final boolean mAllDay;
        // This is used to mark a day header as the first day with events that is "today"
        // or later. This flag is used by the adapter to create a view with a visual separator
        // between the past and the present/future
        // "오늘" 또는 그 이후의 이벤트가 있는 첫날에 일 헤더를 표시하기 위해 사용됨
        // 이 플래그는 어댑터가 과거와 현재/미래 사이에 시각적 구분 기호를 사용해 view를 생성하는 데 사용됨
        boolean mFirstDayAfterYesterday;

        RowInfo(int type, int julianDay, int position, long id, long startTime, long endTime,
                long instanceId, boolean allDay) {
            mType = type;
            mDay = julianDay;
            mPosition = position;
            mEventId = id;
            mEventStartTimeMilli = startTime;
            mEventEndTimeMilli = endTime;
            mFirstDayAfterYesterday = false;
            mInstanceId = instanceId;
            mAllDay = allDay;
        }

        RowInfo(int type, int julianDay) {
            mType = type;
            mDay = julianDay;
            mPosition = 0;
            mEventId = 0;
            mEventStartTimeMilli = 0;
            mEventEndTimeMilli = 0;
            mFirstDayAfterYesterday = false;
            mInstanceId = -1;
            mAllDay = false;
        }
    }

    private static class MultipleDayInfo {
        final int mPosition;
        final int mEndDay;
        final long mEventId;
        final long mInstanceId;
        final boolean mAllDay;
        long mEventStartTimeMilli;
        long mEventEndTimeMilli;

        MultipleDayInfo(int position, int endDay, long id, long startTime, long endTime,
                        long instanceId, boolean allDay) {
            mPosition = position;
            mEndDay = endDay;
            mEventId = id;
            mEventStartTimeMilli = startTime;
            mEventEndTimeMilli = endTime;
            mInstanceId = instanceId;
            mAllDay = allDay;
        }
    }
}
