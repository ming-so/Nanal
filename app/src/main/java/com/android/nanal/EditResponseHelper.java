package com.android.nanal;

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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.widget.Button;

/**
 * A helper class for editing the response to an invitation when the invitation
 * is a repeating event.
 * 초대가 반복 이벤트일 때, 초대에 대한 응답을 편집하기 위한 헬퍼 클래스
 */
public class EditResponseHelper implements DialogInterface.OnClickListener, OnDismissListener {
    private final Activity mParent;
    private int mWhichEvents = -1;
    private AlertDialog mAlertDialog;
    private boolean mClickedOk = false;

    /**
     * This callback is passed in to this object when this object is created
     * and is invoked when the "Ok" button is selected.
     * 이 콜백은 이 객체가 생성될 때 이 객체에 전달되고, "Ok" 버튼을 선택하면 호출됨
     */
    private DialogInterface.OnClickListener mDialogListener;
    /**
     * This callback is used when a list item is selected
     * 이 콜백은 리스트 아이템을 선택할 때 사용됨
     */
    private DialogInterface.OnClickListener mListListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mWhichEvents = which;

                    // Enable the "ok" button now that the user has selected which
                    // events in the series to delete.
                    // 사용자가 series에서 삭제할 이벤트를 선택했으므로 "ok" 버튼을 활성화함
                    Button ok = mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    ok.setEnabled(true);
                }
            };
    private DialogInterface.OnDismissListener mDismissListener;

    public EditResponseHelper(Activity parent) {
        mParent = parent;
    }

    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        mDialogListener = listener;
    }

    /**
     * @return whichEvents, representing which events were selected on which to
     * apply the response:
     * -1 means no choice selected, or the dialog was canceled.
     *          선택 없음, dialog 취소
     * 0 means just the single event.
     *          이벤트
     * 1 means all events.
     *          모든 이벤트
     */
    public int getWhichEvents() {
        return mWhichEvents;
    }

    public void setWhichEvents(int which) {
        mWhichEvents = which;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        setClickedOk(true);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // If the click was not "OK", clear out whichEvents to represent
        // that the dialog was canceled.
        // 클릭이 "OK"되지 않은 경우, dialog가 취소되었음을 나타내는 whichEvents를 clear함
        if (!getClickedOk()) {
            setWhichEvents(-1);
        }
        setClickedOk(false);

        // Call the pre-set dismiss listener too.
        // pre-set dismiss listener도 호출
        if (mDismissListener != null) {
            mDismissListener.onDismiss(dialog);
        }

    }

    private boolean getClickedOk() {
        return mClickedOk;
    }

    private void setClickedOk(boolean clickedOk) {
        mClickedOk = clickedOk;
    }

    /**
     * Set the dismiss listener to be called when the dialog is ended. There,
     * use getWhichEvents() to see how the dialog was dismissed; if it returns
     * -1, the dialog was canceled out. If it is not -1, it's the index of
     * which events the user wants to respond to.
     * dialog가 종료될 때 호출할 dismiss listener 설정
     * 여기서 getWhichEvents()를 사용하여 dialog가 어떻게 해제되었는지 확인함
     * -1: dialog 취소 / 그 외: 사용자가 응답하고자 하는 이벤트의 인덱스 반환
     * @param onDismissListener
     */
    public void setDismissListener(OnDismissListener onDismissListener) {
        mDismissListener = onDismissListener;
    }

    public void showDialog(int whichEvents) {
        // We need to have a non-null listener, otherwise we get null when
        // we try to fetch the "Ok" button.
        // null이 아닌 listener가 있어야 함
        // 그렇지 않으면 "OK" 버튼을 가져오려고 할 때 null을 얻음
        if (mDialogListener == null) {
            mDialogListener = this;
        }
        AlertDialog dialog = new AlertDialog.Builder(mParent).setTitle(
                R.string.change_response_title).setIconAttribute(android.R.attr.alertDialogIcon)
                .setSingleChoiceItems(R.array.change_response_labels, whichEvents, mListListener)
                .setPositiveButton(android.R.string.ok, mDialogListener)
                .setNegativeButton(android.R.string.cancel, null).show();
        // The caller may set a dismiss listener to hear back when the dialog is
        // finished. Use getWhichEvents() to see how the dialog was dismissed.
        // caller는 dialog가 끝났을 때 다시 들을 수 있도록 dismiss listener를 설정할 수 있음
        // dialog가 어떻게 해제되었는지 확인하려면 getWhichEvents() 사용
        dialog.setOnDismissListener(this);
        mAlertDialog = dialog;

        if (whichEvents == -1) {
            // Disable the "Ok" button until the user selects which events to
            // delete.
            // 사용자가 삭제할 이벤트를 선택할 때까지 "OK" 버튼 비활성화
            Button ok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            ok.setEnabled(false);
        }
    }

    public void dismissAlertDialog() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
    }

}
