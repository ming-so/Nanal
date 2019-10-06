package com.android.nanal.group;

import android.app.Activity;
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
import com.android.nanal.query.GroupAsyncTask;

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

    private Context mContext;
    private Activity mActivity;


    public EditGroupHelper(Context context) {
        mService = ((AbstractCalendarActivity) context).getAsyncQueryService();
        mContext = context;
        mActivity = (AbstractCalendarActivity) context;
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

        if (model.group_name.trim().length() <= 0) {
            Log.e(TAG, "빈 그룹 생성");
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

        CreateNewGroup mCreateGroupTask = new CreateNewGroup();
        String receiveMsg;
        String group_id = "";
        try {
            Log.d(TAG, "서버 DB에 데이터 전송하여 insert하고 그룹 아이디 받아오기");
            receiveMsg = mCreateGroupTask.execute(model.group_name, Integer.toString(model.group_color), model.account_id).get();
            group_id = receiveMsg.trim();
            AllInOneActivity.helper.addGroup(Integer.parseInt(group_id), model.group_name, model.group_color, model.account_id);
            Log.d(TAG, "DB에 그룹 추가! group_id="+group_id+", group_name="+model.group_name);
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
                    Log.d(TAG, "groups에 추가 시도, "+ACCOUNT_ID);
                    GroupAsyncTask groupAsyncTask = new GroupAsyncTask(mContext, mActivity);
                    groupAsyncTask.execute(ACCOUNT_ID);
                    Message message = handler.obtainMessage();
                    handler.sendMessage(message);
                }
            }).start();
        }
        mCreateGroupTask.cancel(true);
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
