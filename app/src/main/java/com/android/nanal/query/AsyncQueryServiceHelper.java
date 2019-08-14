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

package com.android.nanal.query;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.android.nanal.query.AsyncQueryService.Operation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;


public class AsyncQueryServiceHelper extends IntentService {
    /*
        IntentService: 액티비티와 프래그먼트 수명 주기에 의존하지 않고 백그라운드에서 처리할 때 사용함
                       오래 걸리지만 메인 스레드와 관련 없는 작업을 할 때 주로 이용함
                       메인 스레드와 관련된 작업을 하려면 메인 스레드의 Handler나 Broadcast Intent 이용
                       Intent 사용에 의해서 실행됨, 새로운 스레드가 생성되고, onHandleIntent() 호출됨
                       onHandleIntent() 내의 모든 동작이 수행되면 멈춤, 멈추는 메소드 호출 불필요
                       여러 번 실행되었을 때는 Queue로 처리됨(처리 중인 IntentService가 있다면 차례를 기다림)
                       Queue에 들어 있는 IntentService가 모두 종료되면 onDestroy() 호출됨
     */
    private static final String TAG = "AsyncQuery";

    private static final PriorityQueue<OperationInfo> sWorkQueue =
            new PriorityQueue<OperationInfo>();

    protected Class<AsyncQueryService> mService = AsyncQueryService.class;

    public AsyncQueryServiceHelper(String name) {
        super(name);
    }

    public AsyncQueryServiceHelper() {
        super("AsyncQueryServiceHelper");
    }

    /**
     * Queues the operation for execution
     * 실행을 위한 작업 큐
     *
     * @param context
     * @param args OperationInfo object describing the operation
     *             작업을 설명하는 OperationInfo
     */
    static public void queueOperation(Context context, OperationInfo args) {
        // Set the schedule time for execution based on the desired delay.
        // 원하는 지연(딜레이)에 따라 실행 일정 시간을 설정함
        args.calculateScheduledTime();

        synchronized (sWorkQueue) {
            sWorkQueue.add(args);
            // Object.notify(): 잠들어 있던 스레드 중 임의로 하나를 골라 깨움
            sWorkQueue.notify();
        }

        context.startService(new Intent(context, AsyncQueryServiceHelper.class));
    }

    /**
     * Gets the last delayed operation. It is typically used for canceling.
     * 가장 최근의(마지막) 작업을 가져옴, 일반적으로 취소에 사용됨
     *
     * @return Operation object which contains of the last cancelable operation
     * 마지막으로 취소할 수 있는 작업이 포함된 Operation 개체
     */
    static public Operation getLastCancelableOperation() {
        long lastScheduleTime = Long.MIN_VALUE;
        Operation op = null;

        synchronized (sWorkQueue) {
            // Unknown order even for a PriorityQueue
            // PriorityQueue에 대한 알 수 없는 요구...?
            Iterator<OperationInfo> it = sWorkQueue.iterator();
            while (it.hasNext()) {
                OperationInfo info = it.next();
                if (info.delayMillis > 0 && lastScheduleTime < info.mScheduledTimeMillis) {
                    if (op == null) {
                        op = new Operation();
                    }

                    op.token = info.token;
                    op.op = info.op;
                    op.scheduledExecutionTime = info.mScheduledTimeMillis;

                    lastScheduleTime = info.mScheduledTimeMillis;
                }
            }
        }

        if (AsyncQueryService.localLOGV) {
            Log.d(TAG, "getLastCancelableOperation -> Operation:" + Operation.opToChar(op.op)
                    + " token:" + op.token);
        }
        return op;
    }

    /**
     * Attempts to cancel operation that has not already started. Note that
     * there is no guarantee that the operation will be canceled. They still may
     * result in a call to on[Query/Insert/Update/Delete/Batch]Complete after
     * this call has completed.
     * 아직 시작되지 않은 작업을 취소하려고 시도함, 작업이 취소된다는 보장은 없음
     * 이 호출이 완료된 후에도 작업들은 여전히 on[쿼리/삽입/업데이트/삭제/배치]Complete에 대한 호출이 발생할 수 있음
     *
     * @param token The token representing the operation to be canceled. If
     *            multiple operations have the same token they will all be
     *            canceled.
     *              취소할 작업을 나타내는 토큰
     *              여러 작업이 동일한 토큰을 가진 경우 모두 취소됨
     */
    static public int cancelOperation(int token) {
        int canceled = 0;
        synchronized (sWorkQueue) {
            Iterator<OperationInfo> it = sWorkQueue.iterator();
            while (it.hasNext()) {
                if (it.next().token == token) {
                    it.remove();
                    ++canceled;
                }
            }
        }

        if (AsyncQueryService.localLOGV) {
            Log.d(TAG, "cancelOperation(" + token + ") -> " + canceled);
        }
        return canceled;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        OperationInfo args;

        if (AsyncQueryService.localLOGV) {
            Log.d(TAG, "onHandleIntent: queue size=" + sWorkQueue.size());
        }
        synchronized (sWorkQueue) {
            while (true) {
                /*
                 * This method can be called with no work because of
                 * cancellations
                 * 이 메소드는 취소cancellations로 인해 작업work 없이 호출될 수 있음
                 */
                if (sWorkQueue.size() == 0) {
                    return;
                } else if (sWorkQueue.size() == 1) {
                    OperationInfo first = sWorkQueue.peek();
                    // SystemClock.elapsedRealtime() sleep 시간을 포함한 부팅 이후 시간(밀리초 단위) 리턴
                    long waitTime = first.mScheduledTimeMillis - SystemClock.elapsedRealtime();
                    if (waitTime > 0) {
                        try {
                            // 아직 남았다면 기다림
                            sWorkQueue.wait(waitTime);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                // poll(): Queue에서 값을 꺼냄
                args = sWorkQueue.poll();
                if (args != null) {
                    // Got work to do. Break out of waiting loop
                    // 할 것이 있음, 대기 루프를 벗어남
                    break;
                }
            }
        }

        if (AsyncQueryService.localLOGV) {
            Log.d(TAG, "onHandleIntent: " + args);
        }

        ContentResolver resolver = args.resolver;
        if (resolver != null) {

            switch (args.op) {
                case Operation.EVENT_ARG_QUERY:
                    Cursor cursor;
                    try {
                        cursor = resolver.query(args.uri, args.projection, args.selection,
                                args.selectionArgs, args.orderBy);
                        /*
                         * Calling getCount() causes the cursor window to be
                         * filled, which will make the first access on the main
                         * thread a lot faster
                         * getCount()를 호출하면 커서 창이 채워져, 메인 스레드에 대한 첫 번째
                         * 접근(액세스)이 훨씬 빨라짐
                         */
                        if (cursor != null) {
                            cursor.getCount();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, e.toString());
                        cursor = null;
                    }

                    args.result = cursor;
                    break;

                case Operation.EVENT_ARG_INSERT:
                    args.result = resolver.insert(args.uri, args.values);
                    break;

                case Operation.EVENT_ARG_UPDATE:
                    args.result = resolver.update(args.uri, args.values, args.selection,
                            args.selectionArgs);
                    break;

                case Operation.EVENT_ARG_DELETE:
                    try {
                        args.result = resolver.delete(args.uri, args.selection, args.selectionArgs);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Delete failed.");
                        Log.w(TAG, e.toString());
                        args.result = 0;
                    }

                    break;

                case Operation.EVENT_ARG_BATCH:
                    try {
                        args.result = resolver.applyBatch(args.authority, args.cpo);
                    } catch (RemoteException e) {
                        Log.e(TAG, e.toString());
                        args.result = null;
                    } catch (OperationApplicationException e) {
                        Log.e(TAG, e.toString());
                        args.result = null;
                    }
                    break;
            }

            /*
             * passing the original token value back to the caller on top of the
             * event values in arg1.
             * 원래 토큰 값을 arg1의 이벤트 값 위에.. 있는 caller에게 다시 전달함
             */
            Message reply = args.handler.obtainMessage(args.token);
            reply.obj = args;
            reply.arg1 = args.op;

            if (AsyncQueryService.localLOGV) {
                Log.d(TAG, "onHandleIntent: op=" + Operation.opToChar(args.op) + ", token="
                        + reply.what);
            }

            reply.sendToTarget();
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (AsyncQueryService.localLOGV) {
            Log.d(TAG, "onStart startId=" + startId);
        }
        super.onStart(intent, startId);
    }

    @Override
    public void onCreate() {
        if (AsyncQueryService.localLOGV) {
            Log.d(TAG, "onCreate");
        }
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (AsyncQueryService.localLOGV) {
            Log.d(TAG, "onDestroy");
        }
        super.onDestroy();
    }

    protected static class OperationInfo implements Delayed {
        public int token; // Used for cancel
        public int op;
        public ContentResolver resolver;
        public Uri uri;
        public String authority;
        public Handler handler;
        public String[] projection;
        public String selection;
        public String[] selectionArgs;
        public String orderBy;
        public Object result;
        public Object cookie;
        public ContentValues values;
        public ArrayList<ContentProviderOperation> cpo;

        /**
         * delayMillis is relative time e.g. 10,000 milliseconds
         * delayMillis는 상대적인 시간임(예: 10,000 milliseconds)
         */
        public long delayMillis;

        /**
         * scheduleTimeMillis is the time scheduled for this to be processed.
         * scheduleTimeMillis는 이 스케줄...작업?을 처리하기 위해 예약된 시간임
         * e.g. SystemClock.elapsedRealtime() + 10,000 milliseconds Based on
         * {@link android.os.SystemClock#elapsedRealtime }
         */
        private long mScheduledTimeMillis = 0;

        // @VisibleForTesting
        void calculateScheduledTime() {
            mScheduledTimeMillis = SystemClock.elapsedRealtime() + delayMillis;
        }

        // @Override // Uncomment with Java6
        public long getDelay(TimeUnit unit) {
            return unit.convert(mScheduledTimeMillis - SystemClock.elapsedRealtime(),
                    TimeUnit.MILLISECONDS);
        }

        // @Override // Uncomment with Java6
        public int compareTo(Delayed another) {
            OperationInfo anotherArgs = (OperationInfo) another;
            if (this.mScheduledTimeMillis == anotherArgs.mScheduledTimeMillis) {
                return 0;
            } else if (this.mScheduledTimeMillis < anotherArgs.mScheduledTimeMillis) {
                return -1;
            } else {
                return 1;
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("OperationInfo [\n\t token= ");
            builder.append(token);
            builder.append(",\n\t op= ");
            builder.append(Operation.opToChar(op));
            builder.append(",\n\t uri= ");
            builder.append(uri);
            builder.append(",\n\t authority= ");
            builder.append(authority);
            builder.append(",\n\t delayMillis= ");
            builder.append(delayMillis);
            builder.append(",\n\t mScheduledTimeMillis= ");
            builder.append(mScheduledTimeMillis);
            builder.append(",\n\t resolver= ");
            builder.append(resolver);
            builder.append(",\n\t handler= ");
            builder.append(handler);
            builder.append(",\n\t projection= ");
            builder.append(Arrays.toString(projection));
            builder.append(",\n\t selection= ");
            builder.append(selection);
            builder.append(",\n\t selectionArgs= ");
            builder.append(Arrays.toString(selectionArgs));
            builder.append(",\n\t orderBy= ");
            builder.append(orderBy);
            builder.append(",\n\t result= ");
            builder.append(result);
            builder.append(",\n\t cookie= ");
            builder.append(cookie);
            builder.append(",\n\t values= ");
            builder.append(values);
            builder.append(",\n\t cpo= ");
            builder.append(cpo);
            builder.append("\n]");
            return builder.toString();
        }

        /**
         * Compares an user-visible operation to this private OperationInfo
         * object
         * 사용자가 볼 수 있는 작업을 이 private OperationInfo 오브젝트와 비교함
         *
         * @param o operation to be compared
         *          비교될 작업
         * @return true if logically equivalent
         *          논리적으로 동등하다면 true
         */
        public boolean equivalent(Operation o) {
            return o.token == this.token && o.op == this.op;
        }
    }
}
