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

package com.android.nanal;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.nanal.AsyncQueryServiceHelper.OperationInfo;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A helper class that executes {@link ContentResolver} calls in a background
 * {@link android.app.Service}. This minimizes the chance of the call getting
 * lost because the caller ({@link android.app.Activity}) is killed. It is
 * designed for easy migration from {@link android.content.AsyncQueryHandler}
 * which calls the {@link ContentResolver} in a background thread. This supports
 * query/insert/update/delete and also batch mode i.e.
 * {@link ContentProviderOperation}. It also supports delay execution and cancel
 * which allows for time-limited undo. Note that there's one queue per
 * application which serializes all the calls.
 * 백그라운드 android.app.Service에서 ContentResolver 호출을 실행하는 헬퍼 클래스
 * caller(android.app.Activity)가 죽여져서 호출이 끊길 가능성을 최소화함
 * 백그라운드 스레드에서 ContentResolver를 호출하는 AsyncQueryHandler에서 쉽게 이동?할 수 있게 설계됨
 * 쿼리/삽입/업데이트/삭제를 지원하며 배치 모드? 즉, ContentProviderOperation도 지원함
 * 또한 지연 실행과 취소를 지원하며 시간 제한적 해제를 허용함
 * 애플리케이션당 모든 호출을 직렬화하는 대기열(queue)가 하나 있다는 점에 유의함
 */
public class AsyncQueryService extends Handler {
    static final boolean localLOGV = false;
    private static final String TAG = "AsyncQuery";
    // Used for generating unique tokens for calls to this service
    // 이 서비스에 대한 호출에 대한 고유한 토큰 생성에 사용
    private static AtomicInteger mUniqueToken = new AtomicInteger(0);

    private Context mContext;
    private Handler mHandler = this; // can be overridden for testing

    public AsyncQueryService(Context context) {
        mContext = context;
    }

    /**
     * returns a practically unique token for db operations
     * db 작업에 대해 실질적으로 고유한 토큰 반환
     */
    public final int getNextToken() {
        return mUniqueToken.getAndIncrement();
    }

    /**
     * Gets the last delayed operation. It is typically used for canceling.
     * 마지막 지연 작업을 가져옴, 일반적으로 취소에 사용됨
     *
     * @return Operation object which contains of the last cancelable operation
     *          마지막으로 취소할 수 있는 작업이 포함된 작업Operation 개체
     */
    public final Operation getLastCancelableOperation() {
        return AsyncQueryServiceHelper.getLastCancelableOperation();
    }

    /**
     * Attempts to cancel operation that has not already started. Note that
     * there is no guarantee that the operation will be canceled. They still may
     * result in a call to on[Query/Insert/Update/Delete/Batch]Complete after
     * this call has completed.
     * 아직 시작되지 않은 작업을 취소하려고 시도함
     * 작업이 취소된다는 보장은 없다는 점에 유의하기
     * 여전히 [쿼리/삽입/업데이트/삭제/배치]에 대한 호출이 발생할 수 있음
     *
     * @param token The token representing the operation to be canceled. If
     *            multiple operations have the same token they will all be
     *            canceled.
     *              작업을 취소할 것을 나타내는 토큰
     *              만약 여러 작업이 동일한 코든을 갖는 경우, 모두 취소됨
     */
    public final int cancelOperation(int token) {
        return AsyncQueryServiceHelper.cancelOperation(token);
    }

    /**
     * This method begins an asynchronous query. When the query is done
     * {@link #onQueryComplete} is called.
     * 이 메소드는 비동기 쿼리를 시작함
     * 퀴리가 완료되면 #onQueryComplete가 호출됨
     *
     * @param token A token passed into {@link #onQueryComplete} to identify the
     *            query.
     *              쿼리를 식별하기 위해 #onQueryComplete에 전달된 토큰
     * @param cookie An object that gets passed into {@link #onQueryComplete}
     *               #onQueryComplete로 전달되는 오브젝트
     * @param uri The URI, using the content:// scheme, for the content to
     *            retrieve.
     *            content://를 사용하여 컨텐츠를 검색할 수 있는 URI
     * @param projection A list of which columns to return. Passing null will
     *            return all columns, which is discouraged to prevent reading
     *            data from storage that isn't going to be used.
     *                   반환할 컬럼(열)의 목록
     *                   null을 넘기면 모든 컬럼을 반환하는데,
     *                   스토리지로부터 사용되지 않을 데이터를 읽는 것을 방지하기 위해 사용하지 않음
     * @param selection A filter declaring which rows to return, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself). Passing null
     *            will return all rows for the given URI.
     *                  반환할 행을 선언하는 필터, SQL WHERE절(WHERE 자체는 제외함)으로 포맷됨
     *                  null을 넘기면 지정된 URI에 대한 모든 행이 반환됨
     * @param selectionArgs You may include ?s in selection, which will be
     *            replaced by the values from selectionArgs, in the order that
     *            they appear in the selection. The values will be bound as
     *            Strings.
     *                      ?s를 선택에 포함시킬 수 있으며, 이 값은 선택에 나타나는 순서대로 selectionArgs의
     *                      값으로 대체됨, 값은 Strings로 묶음
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     *            (excluding the ORDER BY itself). Passing null will use the
     *            default sort order, which may be unordered.
     *                SQL ORDER BY 절 형식으로 작성된 행의 정렬 방법(ORDER BY 자체는 제외함)
     *                null을 넘기면 기본 정렬 순서가 사용되며, 정렬되지 않았을 수도 있음
     */
    public void startQuery(int token, Object cookie, Uri uri, String[] projection,
                           String selection, String[] selectionArgs, String orderBy) {
        OperationInfo info = new OperationInfo();
        info.op = Operation.EVENT_ARG_QUERY;
        info.resolver = mContext.getContentResolver();

        info.handler = mHandler;
        info.token = token;
        info.cookie = cookie;
        info.uri = uri;
        info.projection = projection;
        info.selection = selection;
        info.selectionArgs = selectionArgs;
        info.orderBy = orderBy;

        AsyncQueryServiceHelper.queueOperation(mContext, info);
    }

    /**
     * This method begins an asynchronous insert. When the insert operation is
     * done {@link #onInsertComplete} is called.
     * 이 메소드는 비동기 삽입을 시작함, insert 작업이 완료되면 #onInsertComplete를 호출함
     *
     * @param token A token passed into {@link #onInsertComplete} to identify
     *            the insert operation.
     *              삽입 작업을 식별하기 위해 onInsertComplete에 전달된 토큰
     * @param cookie An object that gets passed into {@link #onInsertComplete}
     *               onInsertComplete에 전달되는 오브젝트
     * @param uri the Uri passed to the insert operation.
     *            삽입 작업으로 전달될 URI
     * @param initialValues the ContentValues parameter passed to the insert
     *            operation.
     *                      삽입 작업으로 전달될 ContentValues 매개변수
     * @param delayMillis delay in executing the operation. This operation will
     *            execute before the delayed time when another operation is
     *            added. Useful for implementing single level undo.
     *                    작업 수행의 지연 시간(딜레이)
     *                    이 작업은 다른 작업이 추가될 때 지연된 시간 전에 실행됨
     *                    싱글 레벨(단일 수준) 실행 취소 구현에 유용함
     */
    public void startInsert(int token, Object cookie, Uri uri, ContentValues initialValues,
                            long delayMillis) {
        OperationInfo info = new OperationInfo();
        info.op = Operation.EVENT_ARG_INSERT;
        info.resolver = mContext.getContentResolver();
        info.handler = mHandler;

        info.token = token;
        info.cookie = cookie;
        info.uri = uri;
        info.values = initialValues;
        info.delayMillis = delayMillis;

        AsyncQueryServiceHelper.queueOperation(mContext, info);
    }

    /**
     * This method begins an asynchronous update. When the update operation is
     * done {@link #onUpdateComplete} is called.
     * 이 메소드는 비동기 업데이트를 시작함, 업데이트 작업이 완료되면 #onUpdateComplete를 호출함
     *
     * @param token A token passed into {@link #onUpdateComplete} to identify
     *            the update operation.
     *              업데이트 작업을 식별하기 위해 onUpdateComplete에 전달된 토큰
     * @param cookie An object that gets passed into {@link #onUpdateComplete}
     *               onUpdateComplete에 전달되는 오브젝트
     * @param uri the Uri passed to the update operation.
     *            업데이트 작업으로 전달될 URI
     * @param values the ContentValues parameter passed to the update operation.
     *               업데이트 작업으로 전달될 ContentValues 매개변수
     * @param selection A filter declaring which rows to update, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself). Passing null
     *            will update all rows for the given URI.
     *                  업데이트할 행을 선언하는 필터, SQL WHERE절(WHERE 자체는 제외함)으로 포맷됨
     *                  null을 넘기면 지정된 URI에 대한 모든 행이 업데이트됨
     * @param selectionArgs You may include ?s in selection, which will be
     *            replaced by the values from selectionArgs, in the order that
     *            they appear in the selection. The values will be bound as
     *            Strings.
     *                      ?s를 선택에 포함시킬 수 있으며, 이 값은 선택에 나타나는 순서대로 selectionArgs의
     *                      값으로 대체됨, 값은 Strings로 묶음
     * @param delayMillis delay in executing the operation. This operation will
     *            execute before the delayed time when another operation is
     *            added. Useful for implementing single level undo.
     *                    작업 수행의 지연 시간(딜레이)
     *                    이 작업은 다른 작업이 추가될 때 지연된 시간 전에 실행됨
     *                    싱글 레벨(단일 수준) 실행 취소 구현에 유용함
     */
    public void startUpdate(int token, Object cookie, Uri uri, ContentValues values,
                            String selection, String[] selectionArgs, long delayMillis) {
        OperationInfo info = new OperationInfo();
        info.op = Operation.EVENT_ARG_UPDATE;
        info.resolver = mContext.getContentResolver();
        info.handler = mHandler;

        info.token = token;
        info.cookie = cookie;
        info.uri = uri;
        info.values = values;
        info.selection = selection;
        info.selectionArgs = selectionArgs;
        info.delayMillis = delayMillis;

        AsyncQueryServiceHelper.queueOperation(mContext, info);
    }

    /**
     * This method begins an asynchronous delete. When the delete operation is
     * done {@link #onDeleteComplete} is called.
     * 이 메소드는 비동기 삭제를 시작함, 삭제 작업이 완료되면 #onDeleteComplete를 호출함
     *
     * @param token A token passed into {@link #onDeleteComplete} to identify
     *            the delete operation.
     *              삭제 작업을 식별하기 위해 onDeleteComplete에 전달된 토큰
     * @param cookie An object that gets passed into {@link #onDeleteComplete}
     *               onDeleteComplete에 전달되는 오브젝트
     * @param uri the Uri passed to the delete operation.
     *            삭제 작업으로 전달될 URI
     * @param selection A filter declaring which rows to delete, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself). Passing null
     *            will delete all rows for the given URI.
     *                  삭제할 행을 선언하는 필터, SQL WHERE절(WHERE 자체는 제외함)으로 포맷됨
     *                  null을 넘기면 지정된 URI에 대한 모든 행이 삭제됨
     * @param selectionArgs You may include ?s in selection, which will be
     *            replaced by the values from selectionArgs, in the order that
     *            they appear in the selection. The values will be bound as
     *            Strings.
     *                      ?s를 선택에 포함시킬 수 있으며, 이 값은 선택에 나타나는 순서대로 selectionArgs의
     *                      값으로 대체됨, 값은 Strings로 묶음
     * @param delayMillis delay in executing the operation. This operation will
     *            execute before the delayed time when another operation is
     *            added. Useful for implementing single level undo.
     *                    작업 수행의 지연 시간(딜레이)
     *                    이 작업은 다른 작업이 추가될 때 지연된 시간 전에 실행됨
     *                    싱글 레벨(단일 수준) 실행 취소 구현에 유용함
     */
    public void startDelete(int token, Object cookie, Uri uri, String selection,
                            String[] selectionArgs, long delayMillis) {
        OperationInfo info = new OperationInfo();
        info.op = Operation.EVENT_ARG_DELETE;
        info.resolver = mContext.getContentResolver();
        info.handler = mHandler;

        info.token = token;
        info.cookie = cookie;
        info.uri = uri;
        info.selection = selection;
        info.selectionArgs = selectionArgs;
        info.delayMillis = delayMillis;

        AsyncQueryServiceHelper.queueOperation(mContext, info);
    }

    /**
     * This method begins an asynchronous {@link ContentProviderOperation}. When
     * the operation is done {@link #onBatchComplete} is called.
     * 이 메소드는 비동기 ContentProviderOperation를 시작함
     * 작업이 완료되면 onBatchComplete가 호출됨
     *
     * @param token A token passed into {@link #onDeleteComplete} to identify
     *            the delete operation.
     *              삭제 작업을 식별하기 위해 onDeleteComplete에 전달된 토큰
     * @param cookie An object that gets passed into {@link #onDeleteComplete}
     *               onDeleteComplete
     *               onDeleteComplete에 전달되는 오브젝트
     * @param authority the authority used for the
     *            {@link ContentProviderOperation}.
     *                  ContentProviderOperation에 사용되는 권한
     * @param cpo the {@link ContentProviderOperation} to be executed.
     *            실행할 ContentProviderOperation
     * @param delayMillis delay in executing the operation. This operation will
     *            execute before the delayed time when another operation is
     *            added. Useful for implementing single level undo.
     *                    작업 수행의 지연 시간(딜레이)
     *                    이 작업은 다른 작업이 추가될 때 지연된 시간 전에 실행됨
     *                    싱글 레벨(단일 수준) 실행 취소 구현에 유용함
     */
    public void startBatch(int token, Object cookie, String authority,
                           ArrayList<ContentProviderOperation> cpo, long delayMillis) {
        OperationInfo info = new OperationInfo();
        info.op = Operation.EVENT_ARG_BATCH;
        info.resolver = mContext.getContentResolver();
        info.handler = mHandler;

        info.token = token;
        info.cookie = cookie;
        info.authority = authority;
        info.cpo = cpo;
        info.delayMillis = delayMillis;

        AsyncQueryServiceHelper.queueOperation(mContext, info);
    }

    /**
     * Called when an asynchronous query is completed.
     * 비동기 쿼리가 완료되면 호출됨
     *
     * @param token the token to identify the query, passed in from
     *            {@link #startQuery}.
     *              startQuery에서 전달된 쿼리를 식별하는 토큰
     * @param cookie the cookie object passed in from {@link #startQuery}.
     *               startQuery에서 전달된 쿠키 오브젝트
     * @param cursor The cursor holding the results from the query.
     *               쿼리의 결과를 보관하는 커서
     */
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (localLOGV) {
            Log.d(TAG, "########## default onQueryComplete");
        }
    }

    /**
     * Called when an asynchronous insert is completed.
     * 비동기 삽입이 완료되면 호출됨
     *
     * @param token the token to identify the query, passed in from
     *            {@link #startInsert}.
     *              startInsert에서 전달된 쿼리를 식별하는 토큰
     * @param cookie the cookie object that's passed in from
     *            {@link #startInsert}.
     *               startInsert에서 전달된 쿠키 오브젝트
     * @param uri the uri returned from the insert operation.
     *            삽입 실행에서 반환된 uri
     */
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
        if (localLOGV) {
            Log.d(TAG, "########## default onInsertComplete");
        }
    }

    /**
     * Called when an asynchronous update is completed.
     * 비동기 업데이트가 완료되면 호출됨
     *
     * @param token the token to identify the query, passed in from
     *            {@link #startUpdate}.
     *              startUpdate에서 전달된 쿼리를 식별하는 토큰
     * @param cookie the cookie object that's passed in from
     *            {@link #startUpdate}.
     *               startUpdate에서 전달된 쿠키 오브젝트
     * @param result the result returned from the update operation
     *               업데이트 실행에서 반환된 result
     */
    protected void onUpdateComplete(int token, Object cookie, int result) {
        if (localLOGV) {
            Log.d(TAG, "########## default onUpdateComplete");
        }
    }

    /**
     * Called when an asynchronous delete is completed.
     * 비동기 삭제가 완료되면 호출됨
     *
     * @param token the token to identify the query, passed in from
     *            {@link #startDelete}.
     *              startDelete에서 전달된 쿼리를 식별하는 토큰
     * @param cookie the cookie object that's passed in from
     *            {@link #startDelete}.
     *               startDelete에서 전달된 쿠키 오브젝트
     * @param result the result returned from the delete operation
     *               삭제 실행에서 반환된 result
     */
    protected void onDeleteComplete(int token, Object cookie, int result) {
        if (localLOGV) {
            Log.d(TAG, "########## default onDeleteComplete");
        }
    }

    /**
     * Called when an asynchronous {@link ContentProviderOperation} is
     * completed.
     * 비동기 ContentProviderOperation이 완료되면 호출됨
     *
     * @param token the token to identify the query, passed in from
     *            {@link #startDelete}.
     *              startDelete에서 전달된 쿼리를 식별하는 토큰
     * @param cookie the cookie object that's passed in from
     *            {@link #startDelete}.
     *              startDelete에서 전달된 쿠키 오브젝트
     * @param results the result returned from executing the
     *            {@link ContentProviderOperation}
     *                ContentProviderOperation 실행에서 반환된 result
     */
    protected void onBatchComplete(int token, Object cookie, ContentProviderResult[] results) {
        if (localLOGV) {
            Log.d(TAG, "########## default onBatchComplete");
        }
    }

    @Override
    public void handleMessage(Message msg) {
        OperationInfo info = (OperationInfo) msg.obj;

        int token = msg.what;
        int op = msg.arg1;

        if (localLOGV) {
            Log.d(TAG, "AsyncQueryService.handleMessage: token=" + token + ", op=" + op
                    + ", result=" + info.result);
        }

        // pass token back to caller on each callback.
        // 토큰을 각 콜백에 있는 caller에게 돌려줌
        switch (op) {
            case Operation.EVENT_ARG_QUERY:
                onQueryComplete(token, info.cookie, (Cursor) info.result);
                break;

            case Operation.EVENT_ARG_INSERT:
                onInsertComplete(token, info.cookie, (Uri) info.result);
                break;

            case Operation.EVENT_ARG_UPDATE:
                onUpdateComplete(token, info.cookie, (Integer) info.result);
                break;

            case Operation.EVENT_ARG_DELETE:
                onDeleteComplete(token, info.cookie, (Integer) info.result);
                break;

            case Operation.EVENT_ARG_BATCH:
                onBatchComplete(token, info.cookie, (ContentProviderResult[]) info.result);
                break;
        }
    }

    //    @VisibleForTesting
    protected void setTestHandler(Handler handler) {
        mHandler = handler;
    }

    /**
     * Data class which holds into info of the queued operation
     * 대기 중인 작업에 대한 정보를 포함하는 데이터 클래스
     */
    public static class Operation {
        static final int EVENT_ARG_QUERY = 1;
        static final int EVENT_ARG_INSERT = 2;
        static final int EVENT_ARG_UPDATE = 3;
        static final int EVENT_ARG_DELETE = 4;
        static final int EVENT_ARG_BATCH = 5;

        /**
         * unique identify for cancellation purpose
         * 취소 목적의 고유 식별자(토큰)
         */
        public int token;

        /**
         * One of the EVENT_ARG_ constants in the class describing the operation
         * 작업을 설명하는 클래스의 EVENT_ART_ 상수 중 하나
         */
        public int op;

        /**
         * {@link SystemClock.elapsedRealtime()} based
         */
        public long scheduledExecutionTime;

        protected static char opToChar(int op) {
            switch (op) {
                case Operation.EVENT_ARG_QUERY:
                    return 'Q';
                case Operation.EVENT_ARG_INSERT:
                    return 'I';
                case Operation.EVENT_ARG_UPDATE:
                    return 'U';
                case Operation.EVENT_ARG_DELETE:
                    return 'D';
                case Operation.EVENT_ARG_BATCH:
                    return 'B';
                default:
                    return '?';
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Operation [op=");
            builder.append(op);
            builder.append(", token=");
            builder.append(token);
            builder.append(", scheduledExecutionTime=");
            builder.append(scheduledExecutionTime);
            builder.append("]");
            return builder.toString();
        }
    }
}
