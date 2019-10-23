package com.android.nanal.group;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.util.Log;

import com.android.nanal.R;
import com.android.nanal.calendar.CalendarGroupModel;
import com.android.nanal.query.AsyncQueryService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class DeleteGroupHelper {

    /**
     * These are the corresponding indices into the array of strings
     * "R.array.delete_repeating_labels" in the resource file.
     * 리소스 파일의 스트링 배열 "R.array..._labels"에 해당하는 인덱스임
     */
    private final Activity mParent;
    private Context mContext;
    private CalendarGroupModel mModel;
    /**
     * If true, then call finish() on the parent activity when done.
     * true인 경우, 완료되면 부모 activity에 대한 finish()를 호출함
     */
    private boolean mExitWhenDone;
    // the runnable to execute when the delete is confirmed
    // 삭제가 확인되었을 때 실행할 runnable
    private Runnable mCallback;
    private AlertDialog mAlertDialog;
    private ArrayList<Integer> mWhichIndex;
    private Dialog.OnDismissListener mDismissListener;

    private String mSyncId;

    private AsyncQueryService mService;

    private DeleteGroupHelper.DeleteNotifyListener mDeleteStartedListener = null;

    /**
     * 그룹 일기 삭제 메소드
     */
    private DialogInterface.OnClickListener mDeleteGroupDialogListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteStarted();
                    int delete_id = mModel.group_id;
                    if (mCallback != null) {
                        mCallback.run();
                    }
                    if (mExitWhenDone) {
                        mParent.finish();
                    }
                }
            };


    public DeleteGroupHelper(Context context, Activity parentActivity, boolean exitWhenDone) {
        if (exitWhenDone && parentActivity == null) {
            throw new IllegalArgumentException("parentActivity is required to exit when done");
        }

        mContext = context;
        mParent = parentActivity;
        // TODO move the creation of this service out into the activity.
        mService = new AsyncQueryService(mContext) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor == null) {
                    return;
                }
                cursor.moveToFirst();
                CalendarGroupModel mModel = new CalendarGroupModel();
                EditGroupHelper.setModelFromCursor(mModel, cursor);
                cursor.close();
                DeleteGroupHelper.this.delete(mModel);
            }
        };
        mExitWhenDone = exitWhenDone;
    }

    public void setExitWhenDone(boolean exitWhenDone) {
        mExitWhenDone = exitWhenDone;
    }

    /**
     * Does the required processing for deleting an event, which includes
     * first popping up a dialog asking for confirmation (if the event is
     * a normal event) or a dialog asking which events to delete (if the
     * event is a repeating event).  The "which" parameter is used to check
     * the initial selection and is only used for repeating events.  Set
     * "which" to -1 to have nothing selected initially.
     * 확인을 요청하는 대화상자(이벤트가 일반 이벤트인 경우) 또는 삭제할 이벤트를 묻는 대화상자
     * (반복 이벤트인 경우)를 먼저 팝업하는 기능을 포함한 이벤트 삭제시 필요한 처리를 수행함
     * "which" 매개변수는 초기 선택을 확인하는 데 사용되며 반복 이벤트에만 사용됨
     * 처음에 아무것도 선택하지 않으려면 "which"를 -1로 설정하기
     *
     *  DELETE_ALL_FOLLOWING, DELETE_ALL, or -1
     */
    public String delete(int groupId) {
        String sendMsg, receiveMsg = "";
        try {
            String str;
            URL url = new URL("http://ci2019nanal.dongyangmirae.kr/android/GroupDelete.jsp");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("POST");//데이터를 POST 방식으로 전송합니다.

            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            sendMsg = "group_id=" + groupId;
            osw.write(sendMsg);
            osw.flush();
            osw.close();

            if (conn.getResponseCode() == conn.HTTP_OK) {
                InputStreamReader tmp = new InputStreamReader(conn.getInputStream(), "UTF-8");
                BufferedReader reader = new BufferedReader(tmp);
                StringBuffer buffer = new StringBuffer();
                while ((str = reader.readLine()) != null) {
                    buffer.append(str);
                }
                receiveMsg = buffer.toString();
                tmp.close();
                reader.close();
            } else {
                Log.i("통신 결과", conn.getResponseCode() + "에러");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return receiveMsg;
    }


    /**
     * Does the required processing for deleting an event.  This method
     * takes a {@link CalendarGroupModel} object, which must have a valid
     * uri for referencing the event in the database and have the required
     * fields listed below.
     * 이벤트를 삭제하는 데 필요한 처리 수행
     * 이 메소드는 데이터베이스에서 이벤트를 참조하는 데 유효한 uri가 있고,
     * 아래에 있는 필수 필드를 가지고 있는 CalendarEventModel 개체를 사용함
     * The required fields for a normal event are:
     * 일반 이벤트에 필요한 필드:
     *
     * <ul>
     *   <li> Events._ID </li>
     *   <li> Events.TITLE </li>
     *   <li> Events.RRULE </li>
     * </ul>
     *
     * The required fields for a repeating event include the above plus the
     * following fields:
     * 반복 이벤트에 필요한 필드는 위의 필드 + 다음 필드:
     *
     * <ul>
     *   <li> Events.ALL_DAY </li>
     *   <li> Events.CALENDAR_ID </li>
     *   <li> Events.DTSTART </li>
     *   <li> Events._SYNC_ID </li>
     *   <li> Events.EVENT_TIMEZONE </li>
     * </ul>
     *
     * If the event no longer exists in the db this will still prompt
     * the user but will return without modifying the db after the query
     * returns.
     * 이벤트가 db에 더 이상 존재하지 않는 경우, 여전히 사용자에게 메시지를 표시하지만
     * 쿼리가 반환된 후 db를 수정하지 않고 반환될 것임
     *
     * //@param cursor the database cursor containing the required fields
     */
    public void delete(CalendarGroupModel model) {
        mModel = model;
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setMessage(R.string.delete_this_group_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setNegativeButton(android.R.string.cancel, null).create();
        dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                mContext.getText(android.R.string.ok),
                mDeleteGroupDialogListener);
        dialog.setOnDismissListener(mDismissListener);
        dialog.show();
        mAlertDialog = dialog;
    }

    public void setDeleteNotificationListener(DeleteGroupHelper.DeleteNotifyListener listener) {
        mDeleteStartedListener = listener;
    }

    private void deleteStarted() {
        if (mDeleteStartedListener != null) {
            mDeleteStartedListener.onDeleteStarted();
        }
    }

    public interface DeleteNotifyListener {
        public void onDeleteStarted();
    }


}
