package com.android.nanal.group;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract.Colors;
import android.util.Log;

import com.android.nanal.activity.AbstractCalendarActivity;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.calendar.CalendarGroupModel;
import com.android.nanal.query.AsyncQueryService;

import java.util.concurrent.ExecutionException;


public class EditGroupHelper {
    private static final String TAG = "EditGroupHelper";
    private static final String NO_GROUP_COLOR = "";

    public static final String[] GROUP_PROJECTION = new String[]{
            "group_id",
            "group_name",
            "group_color",
            "account_id"
    };
    static final int PROJECTION_GROUP_ID_INDEX = 0;
    static final int PROJECTION_GROUP_NAME_INDEX = 1;
    static final int PROJECTION_GROUP_COLOR_INDEX = 2;
    private static final int PROJECTION_ACCOUNT_ID_INDEX = 3;

    private final AsyncQueryService mService;
    protected boolean mGroupOk = true;

    protected static final int MODIFY_UNINITIALIZED = 0;


    static final String[] COLORS_PROJECTION = new String[]{
            Colors._ID, // 0
            Colors.ACCOUNT_NAME,
            Colors.ACCOUNT_TYPE,
            Colors.COLOR, // 1
            Colors.COLOR_KEY // 2
    };

    static final String COLORS_WHERE = Colors.ACCOUNT_NAME + "=? AND " + Colors.ACCOUNT_TYPE +
            "=? AND " + Colors.COLOR_TYPE + "=" + Colors.TYPE_EVENT;

    static final int COLORS_INDEX_ACCOUNT_NAME = 1;
    static final int COLORS_INDEX_ACCOUNT_TYPE = 2;
    static final int COLORS_INDEX_COLOR = 3;
    static final int COLORS_INDEX_COLOR_KEY = 4;


    public EditGroupHelper(Context context) {
        mService = ((AbstractCalendarActivity) context).getAsyncQueryService();
    }


    public boolean saveGroup(CalendarGroupModel model, CalendarGroupModel originalModel) {
        Log.d(TAG, "saveGroup");
        if (!mGroupOk) {
            return false;
        }

        if (model == null) {
            Log.e(TAG, "Attempted to save null model.");
            return false;
        }

        if (originalModel != null) {
            Log.e(TAG, "Attempted to update existing event but models didn't refer to the same "
                    + "event.");
            return false;
        }
        if (originalModel != null && model.isUnchanged(originalModel)) {
            return false;
        }

//        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
//        int groupIdIndex = -1;

//        ContentValues values = getContentValuesFromModel(model);

//        if (model.mUri != null && originalModel == null) {
//            Log.e(TAG, "Existing event but no originalModel provided. Aborting save.");
//            return false;
//        }

//        Uri uri = null;
//        if (model.mUri != null) {
//            uri = Uri.parse(model.mUri);
//        }
//
//        groupIdIndex = ops.size();
//
//        if(uri == null) {
//            Uri CONTENT_URI = Uri.parse("content://" + "com.android.nanal" + "/group");
//            ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(CONTENT_URI).withValues(values);
//            ops.add(b.build());
//        } else {
//            ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(uri).withValues(values);
//            ops.add(b.build());
//        }
//
//        // New Event or New Exception to an existing event
//        boolean newGroup = (groupIdIndex != -1);
//
//        ContentProviderOperation.Builder b;
//
//        mService.startBatch(mService.getNextToken(), null, android.provider.CalendarContract.AUTHORITY, ops,
//                Utils.UNDO_DELAY);
        //todo: 생성/수정 따로 처리해야 함, jsp에서 처리하면 느려질까???
        CreateNewGroup mCreateGroupTask = new CreateNewGroup();
        final String receiveMsg;
        String group_id = "";
        try {
            receiveMsg = mCreateGroupTask.execute(model.group_name, Integer.toString(model.group_color), model.account_id).get();
            group_id = receiveMsg;
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        } catch (ExecutionException e) {
            Log.e(TAG, e.getMessage());
        }

        if(group_id == "") {
            Log.d(TAG, "receiveMsg 없음");
            return false;
        } else {
            final String GROUP_ID = group_id.trim();
            final String GROUP_NAME = model.group_name;
            final int GROUP_COLOR = model.group_color;
            final String ACCOUNT_ID = model.account_id;

            final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                        Log.d(TAG, "어댑터 갱신 시도");
                        AllInOneActivity.groupListAdapter.notifyDataSetChanged();
                        Log.d(TAG, "완료, 현재 갯수: " + AllInOneActivity.groupListAdapter.getItemCount());
                }
            };

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "groups에 추가 시도");
                    //todo:db 직접 연동으로 바꾸면 add문은 지워야 함
                    AllInOneActivity.groups.add(new Group(Integer.parseInt(GROUP_ID), GROUP_NAME, GROUP_COLOR, ACCOUNT_ID));

                    Message message = handler.obtainMessage();
                    handler.sendMessage(message);
                }
            }).start();
        }
        return true;
    }

    public static boolean isSameGroup(CalendarGroupModel model, CalendarGroupModel originalModel) {
        if (originalModel == null) {
            return true;
        }
        if (model.group_id != originalModel.group_id || (model.group_id != -1 && originalModel.group_id != -1)) {
            return false;
        }
        return true;
    }

    public static void setModelFromCursor(CalendarGroupModel model, Cursor cursor) {
        if (model == null || cursor == null || cursor.getCount() != 1) {
            Log.wtf(TAG, "Attempted to build non-existent model or from an incorrect query.");
            return;
        }
        model.clear();
        cursor.moveToFirst();
        model.group_id = cursor.getInt(PROJECTION_GROUP_ID_INDEX);
        model.group_name = cursor.getString(PROJECTION_GROUP_NAME_INDEX);
        model.group_color = cursor.getInt(PROJECTION_GROUP_COLOR_INDEX);
        model.account_id = cursor.getString(PROJECTION_ACCOUNT_ID_INDEX);
    }

    ContentValues getContentValuesFromModel(CalendarGroupModel model) {
        ContentValues values = new ContentValues();

        values.put("group_id", model.group_id);
        values.put("group_name", model.group_name);
        values.put("group_color", model.group_color);
        values.put("account_id", model.account_id);

        return values;
    }

    public interface EditDoneRunnable extends Runnable {
        void setDoneCode(int code);
    }
}
