package com.android.nanal.event;

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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Process;
import android.provider.CalendarContract;
import android.provider.CalendarContract.EventDays;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class EventLoader {

    private Context mContext;
    private Handler mHandler = new Handler();
    private AtomicInteger mSequenceNumber = new AtomicInteger();

    private LinkedBlockingQueue<LoadRequest> mLoaderQueue;
    private LoaderThread mLoaderThread;
    private ContentResolver mResolver;

    public EventLoader(Context context) {
        mContext = context;
        mLoaderQueue = new LinkedBlockingQueue<LoadRequest>();
        mResolver = context.getContentResolver();
    }

    /**
     * Call this from the activity's onResume()
     * activity의 onResume()에서 호출함
     */
    public void startBackgroundThread() {
        mLoaderThread = new LoaderThread(mLoaderQueue, this);
        mLoaderThread.start();
    }

    /**
     * Call this from the activity's onPause()
     * activity의 onPause()에서 호출함
     */
    public void stopBackgroundThread() {
        mLoaderThread.shutdown();
    }

    /**
     * Loads "numDays" days worth of events, starting at start, into events.
     * Posts uiCallback to the {@link Handler} for this view, which will run in the UI thread.
     * Reuses an existing background thread, if events were already being loaded in the background.
     * 처음부터 이벤트에 "numDays" 일 단위 이벤트를 로드함
     * UI 스레드에서 실행될 이 view에 대한 핸들러에 uiCallBack 게시함
     * 이벤트가 이미 백그라운드에서 로드된 경우, 기존 백그라운드 스레드를 재사용함
     *
     * NOTE: events and uiCallback are not used if an existing background thread gets reused --
     * the ones that were passed in on the call that results in the background thread getting
     * created are used, and the most recent call's worth of data is loaded into events and posted
     * via the uiCallback.
     * 참고: 기존 백그라운드 스레드를 재사용할 경우 이벤트와 uiCallback은 사용되지 않는다.
     * 백그라운드 스레드가 생성되는 콜에 전달된 스레드가 사용되며,
     * 가장 최근의 콜의 가치는 이벤트에 로드되어 uiCallback을 통해 게시된다.
     */
    public void loadEventsInBackground(final int numDays, final ArrayList<Event> events,
                                       int startDay, final Runnable successCallback, final Runnable cancelCallback) {

        // Increment the sequence number for requests.  We don't care if the
        // sequence numbers wrap around because we test for equality with the
        // latest one.
        // 요청에 대한 시퀀스 번호를 늘림
        // 최근의 것과 동등성을 테스트하기 때문에 스퀀스 번호가 wrap되어도 괜찮음...
        int id = mSequenceNumber.incrementAndGet();

        // Send the load request to the background thread
        // 로드 요청을 백그라운드 스레드로 전송
        LoadEventsRequest request = new LoadEventsRequest(id, startDay, numDays,
                events, successCallback, cancelCallback);

        try {
            mLoaderQueue.put(request);
        } catch (InterruptedException ex) {
            // The put() method fails with InterruptedException if the
            // queue is full. This should never happen because the queue
            // has no limit.
            // 큐(queue)가 가득 차면 InterruptedException과 함께 put() 메소드가 실패함
            // 큐에 제한이 없기 때문에 이런 일은 절대 있어서는 안 됨
            Log.e("Cal", "loadEventsInBackground() interrupted!");
        }
    }

    /**
     * Sends a request for the days with events to be marked. Loads "numDays"
     * worth of days, starting at start, and fills in eventDays to express which
     * days have events.
     * 이벤트를 표시할 날짜에 대한 요청을 전송함
     * 시작일로부터 "numDays" 값을 로드하고 이벤트 일수를 채워 이벤트가 있는 요일을 표현함
     *
     * @param startDay   First day to check for events
     *                   이벤트 확인할 첫 번째 날
     * @param numDays    Days following the start day to check
     *                   확인할 시작일 이후 일 수
     * @param eventDays  Whether or not an event exists on that day
     *                   해당 날짜에 이벤트가 존재하는지의 여부
     * @param uiCallback What to do when done (log data, redraw screen)
     *                   완료시 수행할 작업(로그 데이터, 화면 다시 그리기)
     */
    void loadEventDaysInBackground(int startDay, int numDays, boolean[] eventDays,
                                   final Runnable uiCallback) {
        // Send load request to the background thread
        // 백그라운드 스레드로 로드 요청 전송
        LoadEventDaysRequest request = new LoadEventDaysRequest(startDay, numDays,
                eventDays, uiCallback);
        try {
            mLoaderQueue.put(request);
        } catch (InterruptedException ex) {
            // The put() method fails with InterruptedException if the
            // queue is full. This should never happen because the queue
            // has no limit.
            // 큐(queue)가 가득 차면 InterruptedException과 함께 put() 메소드가 실패함
            // 큐에 제한이 없기 때문에 이런 일은 절대 있어서는 안 됨
            Log.e("Cal", "loadEventDaysInBackground() interrupted!");
        }
    }

    private static interface LoadRequest {
        public void processRequest(EventLoader eventLoader);
        public void skipRequest(EventLoader eventLoader);
    }

    private static class ShutdownRequest implements LoadRequest {
        public void processRequest(EventLoader eventLoader) {
        }

        public void skipRequest(EventLoader eventLoader) {
        }
    }

    /**
     *
     * Code for handling requests to get whether days have an event or not
     * and filling in the eventDays array.
     * 일마다 이벤트를 가지고 있는지 받아오기 및 eventDays 배열에 채우기 요청을 처리하는 코드
     *
     */
    private static class LoadEventDaysRequest implements LoadRequest {
        /**
         * The projection used by the EventDays query.
         * EventDays 쿼리에 사용되는 projection
         */
        private static final String[] PROJECTION = {
                CalendarContract.EventDays.STARTDAY, CalendarContract.EventDays.ENDDAY
        };
        public int startDay;
        public int numDays;
        public boolean[] eventDays;
        public Runnable uiCallback;

        public LoadEventDaysRequest(int startDay, int numDays, boolean[] eventDays,
                                    final Runnable uiCallback)
        {
            this.startDay = startDay;
            this.numDays = numDays;
            this.eventDays = eventDays;
            this.uiCallback = uiCallback;
        }

        @Override
        public void processRequest(EventLoader eventLoader)
        {
            final Handler handler = eventLoader.mHandler;
            ContentResolver cr = eventLoader.mResolver;

            // Clear the event days
            // eventDays 지우기
            Arrays.fill(eventDays, false);

            //query which days have events
            // 이벤트가 있는 날짜를 쿼리함
            Cursor cursor = EventDays.query(cr, startDay, numDays, PROJECTION);
            try {
                int startDayColumnIndex = cursor.getColumnIndexOrThrow(EventDays.STARTDAY);
                int endDayColumnIndex = cursor.getColumnIndexOrThrow(EventDays.ENDDAY);

                //Set all the days with events to true
                // 이벤트를 포함한 모든 날짜를 true로 설정
                while (cursor.moveToNext()) {
                    int firstDay = cursor.getInt(startDayColumnIndex);
                    int lastDay = cursor.getInt(endDayColumnIndex);
                    //we want the entire range the event occurs, but only within the month
                    int firstIndex = Math.max(firstDay - startDay, 0);
                    int lastIndex = Math.min(lastDay - startDay, 30);

                    for(int i = firstIndex; i <= lastIndex; i++) {
                        eventDays[i] = true;
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            handler.post(uiCallback);
        }

        @Override
        public void skipRequest(EventLoader eventLoader) {
        }
    }

    private static class LoadEventsRequest implements LoadRequest {

        public int id;
        public int startDay;
        public int numDays;
        public ArrayList<Event> events;
        public Runnable successCallback;
        public Runnable cancelCallback;

        public LoadEventsRequest(int id, int startDay, int numDays, ArrayList<Event> events,
                                 final Runnable successCallback, final Runnable cancelCallback) {
            this.id = id;
            this.startDay = startDay;
            this.numDays = numDays;
            this.events = events;
            this.successCallback = successCallback;
            this.cancelCallback = cancelCallback;
        }

        public void processRequest(EventLoader eventLoader) {
            Event.loadEvents(eventLoader.mContext, events, startDay,
                    numDays, id, eventLoader.mSequenceNumber);

            // Check if we are still the most recent request.
            // 여전히 가장 최근의 요청인지 체크하기
            if (id == eventLoader.mSequenceNumber.get()) {
                eventLoader.mHandler.post(successCallback);
            } else {
                eventLoader.mHandler.post(cancelCallback);
            }
        }

        public void skipRequest(EventLoader eventLoader) {
            eventLoader.mHandler.post(cancelCallback);
        }
    }

    private static class LoaderThread extends Thread {
        LinkedBlockingQueue<LoadRequest> mQueue;
        EventLoader mEventLoader;

        public LoaderThread(LinkedBlockingQueue<LoadRequest> queue, EventLoader eventLoader) {
            mQueue = queue;
            mEventLoader = eventLoader;
        }

        public void shutdown() {
            try {
                mQueue.put(new ShutdownRequest());
            } catch (InterruptedException ex) {
                // The put() method fails with InterruptedException if the
                // queue is full. This should never happen because the queue
                // has no limit.
                // 큐(queue)가 가득 차면 InterruptedException과 함께 put() 메소드가 실패함
                // 큐에 제한이 없기 때문에 이런 일은 절대 있어서는 안 됨
                Log.e("Cal", "LoaderThread.shutdown() interrupted!");
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            while (true) {
                try {
                    // Wait for the next request
                    // 다음 요청 기다리기
                    LoadRequest request = mQueue.take();

                    // If there are a bunch of requests already waiting, then
                    // skip all but the most recent request.
                    // 이미 여러 개의 요청이 대기 중인 경우, 가장 최근의 요청을 제외하고 모두 건너뜀
                    while (!mQueue.isEmpty()) {
                        // Let the request know that it was skipped
                        // 건너뛴 요청을 알림
                        request.skipRequest(mEventLoader);

                        // Skip to the next request
                        // 다음 요청으로 넘어감
                        request = mQueue.take();
                    }

                    if (request instanceof ShutdownRequest) {
                        return;
                    }
                    request.processRequest(mEventLoader);
                } catch (InterruptedException ex) {
                    Log.e("Cal", "background LoaderThread interrupted!");
                }
            }
        }
    }
}
