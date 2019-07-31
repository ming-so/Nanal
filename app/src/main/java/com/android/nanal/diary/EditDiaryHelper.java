package com.android.nanal.diary;

import android.content.Context;

import com.android.nanal.activity.AbstractCalendarActivity;
import com.android.nanal.query.AsyncQueryService;

public class EditDiaryHelper {
    private static final String TAG = "EditDiaryHelper";
    private static final String NO_DIARY_COLOR = "";

    public static final String[] DIARY_PROJECTION = new String[]{
            "DIARY_ID",
            "DIARY_USER_ID",
            "DIARY_GROUP_ID",
            "DIARY_CONTENT",
            "DIARY_COLOR",
            "DIARY_WEATHER",
            "DIARY_LOCATION",
            "DIARY_IMG"
    };
    // DB: 일기번호, 클라아이디,그룹아이디,일기내용,일기표시색,날씨,위치,이미지
    protected static final int DIARY_INDEX_ID = 0;
    protected static final int DIARY_INDEX_USER_ID = 1;
    protected static final int DIARY_INDEX_GROUP_ID = 2;
    protected static final int DIARY_INDEX_CONTENT = 3;
    protected static final int DIARY_INDEX_COLOR = 4;
    protected static final int DIARY_INDEX_WEATHER = 5;
    protected static final int DIARY_INDEX_LOCATION = 6;
    protected static final int DIARY_INDEX_IMG = 7;

    private final AsyncQueryService mService;
    protected boolean mDiaryOk = true;

    static final String[] CALENDARS_PROJECTION = new String[]{
            // 캘린더에 표시될 때 필요한 것? 아직 잘 모르겠음
            // EditEventHelper.java 참고
    };
    // INDEX 똑같이 선언

    public EditDiaryHelper(Context context) {
        mService = ((AbstractCalendarActivity) context).getAsyncQueryService();
    }
/*
    public boolean saveDiary(CalendarDiaryModel model, CalendarDiaryModel originalModel, int modifyWhich) {
        if (!mDiaryOk) {
            return false;
        }

        if(model == null) {
            Log.e(TAG, "Attempted to save null model.");
            return false;
        }
        if(!model.isVaild())
        {
            Log.e(TAG, "Attempted to save invalid model.");
            return false;
        }

        if (originalModel != null && !isSameEvent(model, originalModel)) {
            Log.e(TAG, "Attempted to update existing event but models didn't refer to the same "
                    + "event.");
            return false;
        }
        if (originalModel != null && model.isUnchanged(originalModel)) {
            return false;
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        int diaryIdIndex = -1;

        ContentValues values = getContentValuesFromModel(model);

        if (model.mUri != null && originalModel == null) {
            Log.e(TAG, "Existing event but no originalModel provided. Aborting save.");
            return false;
        }
        Uri uri = null;
        if (model.mUri != null) {
            uri = Uri.parse(model.mUri);
        }
    }
    */
}
