package com.android.nanal.diary;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;

import com.android.nanal.R;
import com.android.nanal.calendar.CalendarDiaryModel;
import com.android.nanal.calendar.CalendarEventModel;
import com.android.nanal.event.DeleteEventHelper;
import com.android.nanal.event.Utils;
import com.android.nanal.query.AsyncQueryService;

import java.util.ArrayList;

public class DeleteDiaryHelper {

    /**
     * These are the corresponding indices into the array of strings
     * "R.array.delete_repeating_labels" in the resource file.
     * 리소스 파일의 스트링 배열 "R.array..._labels"에 해당하는 인덱스임
     */
    public static final int DELETE_SELECTED = 0;
    public static final int DELETE_ALL_FOLLOWING = 1;
    public static final int DELETE_ALL = 2;
    private final Activity mParent;
    private Context mContext;
    private long day;
    private CalendarDiaryModel mModel;
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

    private DeleteEventHelper.DeleteNotifyListener mDeleteStartedListener = null;
    /**
     * This callback is used when a normal event is deleted.
     * 이 콜백은 일반 이벤트가 삭제될 때 사용됨
     */
    private DialogInterface.OnClickListener mDeleteNormalDialogListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int button) {
                    deleteStarted();
                    int id = mModel.mDiaryId;
                    Uri m = Uri.parse("content://" + "com.android.nanal" + "/diary");
                    Uri uri = ContentUris.withAppendedId(m, id);
                    mService.startDelete(mService.getNextToken(), null, uri, null, null, Utils.UNDO_DELAY);
                    if (mCallback != null) {
                        mCallback.run();
                    }
                    if (mExitWhenDone) {
                        mParent.finish();
                    }
                }
            };

    /**
     * 그룹 일기 삭제 메소드
     */
    private DialogInterface.OnClickListener mDeleteGroupDialogListenr =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteStarted();
                    int id = mModel.mDiaryId;
                    Uri m = Uri.parse("content://" + "com.android.nanal" + "/diary");
                    Uri uri = ContentUris.withAppendedId(m, id);
                    mService.startDelete(mService.getNextToken(), 1, uri, null, null, Utils.UNDO_DELAY);
                    if (mCallback != null) {
                        mCallback.run();
                    }
                    if (mExitWhenDone) {
                        mParent.finish();
                    }
                }
            };


    public DeleteDiaryHelper(Context context, Activity parentActivity, boolean exitWhenDone) {
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
                CalendarDiaryModel mModel = new CalendarDiaryModel();
                EditDiaryHelper.setModelFromCursor(mModel, cursor);
                cursor.close();
                DeleteDiaryHelper.this.delete(day, mModel);
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
     * @param day 일기 작성된 날
     * @param eventId the event id
     * @param which one of the values DELETE_SELECTED,
     *  DELETE_ALL_FOLLOWING, DELETE_ALL, or -1
     */
    public void delete(long day, int eventId, int which) {
        Uri m = Uri.parse("content://" + "com.android.nanal" + "/diary");
        Uri uri = ContentUris.withAppendedId(m, eventId);
        mService.startQuery(mService.getNextToken(), null, uri, EditDiaryHelper.DIARY_PROJECTION,
                null, null, null);
        this.day = day;
    }

    public void delete(long day, int eventId, int which, Runnable callback) {
        delete(day, eventId, which);
        mCallback = callback;
    }


    /**
     * Does the required processing for deleting an event.  This method
     * takes a {@link CalendarEventModel} object, which must have a valid
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
     * @param day 일기 작성된 날
     * //@param cursor the database cursor containing the required fields
     */
    public void delete(long day, CalendarDiaryModel model) {
        this.day = day;
        mModel = model;

        if (!mModel.isInGroup()) {
            // 개인 일기
            AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setMessage(R.string.delete_this_diary_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setNegativeButton(android.R.string.cancel, null).create();

            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                        mContext.getText(android.R.string.ok),
                        mDeleteNormalDialogListener);
            dialog.setOnDismissListener(mDismissListener);
            dialog.show();
            mAlertDialog = dialog;
        } else {
            // 그룹 일기 삭제
            AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setMessage(R.string.delete_this_diary_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setNegativeButton(android.R.string.cancel, null).create();

            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    mContext.getText(android.R.string.ok),
                    mDeleteGroupDialogListenr);
            dialog.setOnDismissListener(mDismissListener);
            dialog.show();
            mAlertDialog = dialog;
        }
    }

    public void setDeleteNotificationListener(DeleteEventHelper.DeleteNotifyListener listener) {
        mDeleteStartedListener = listener;
    }

    private void deleteStarted() {
        if (mDeleteStartedListener != null) {
            mDeleteStartedListener.onDeleteStarted();
        }
    }

    public void setOnDismissListener(Dialog.OnDismissListener listener) {
        if (mAlertDialog != null) {
            mAlertDialog.setOnDismissListener(listener);
        }
        mDismissListener = listener;
    }

    public void dismissAlertDialog() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
    }

    public interface DeleteNotifyListener {
        public void onDeleteStarted();
    }
}
