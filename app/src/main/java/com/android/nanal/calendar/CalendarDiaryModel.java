package com.android.nanal.calendar;

import android.content.Context;
import android.content.Intent;

import com.android.nanal.diary.DiaryColorCache;

import java.io.Serializable;
import java.util.Calendar;

public class CalendarDiaryModel implements Serializable {
    private static final String TAG = "CalendarDiaryModel";
    public String mUri = null;

    public String mCalendarDisplayName = ""; // Make sure this is in sync with the mCalendarId, mCalendarId와 동기화되었는지 확인
    public String mCalendarAccountName;
    public String mCalendarAccountType;

    public int mDiaryId;
    public String mDiaryUserId;
    public String mConnectType;
    public int mDiaryColor;
    public String mDiaryLocation;
    public long mDiaryDay;
    public String mDiaryTitle;
    public String mDiaryWeather;
    public String mDiaryContent;
    public String mDiaryImg;
    public int mDiaryGroupId;

    public String mSyncId = null;
    public String mSyncAccount = null;
    public String mSyncAccountType = null;

    public String mGroupName;
    public int mGroupColor;

    public DiaryColorCache mDiaryColorCache;
    public boolean mModelUpdatedWithDiaryCursor;

    public boolean mDiaryColorInitialized = false;

    public CalendarDiaryModel(Context context){
        this();
        // Preference 불필요할 것 같음

    }
    public CalendarDiaryModel(Context context, Intent intent) {
        this(context);

        if(intent == null) return;

    }
    public CalendarDiaryModel() {

    }

    public boolean isValid() {
        // 유효성 검사 조건들 추가하기
        //if (mDiaryId == null) {
        //    return false;
        //}
        return true;
    }

    public boolean isEmpty() {
        if(mDiaryUserId != null && mDiaryUserId.trim().length() > 0) {
            return false;
        }
        if(mDiaryContent != null && mDiaryContent.trim().length() > 0) {
            return false;
        }
        if(mDiaryDay > 0) {
            return false;
        }
        return true;
    }

    public boolean isEmpty(String str) {
        // String 비었는지 확인하는 메소드
        return str == null || str.length() == 0;
    }

    public void clear() {
        mUri = null;

        mDiaryId = -1;
        mDiaryUserId = null;
        mConnectType = null;
        mDiaryColor = -1;
        mDiaryLocation = null;
        mDiaryDay = -1;
        mDiaryTitle = null;
        mDiaryContent = null;
        mDiaryWeather = null;
        mDiaryImg = null;
        mDiaryGroupId = -1;

        mSyncId = null;
        mSyncAccount = null;
        mSyncAccountType = null;

        mDiaryColorCache = null;
        mModelUpdatedWithDiaryCursor = false;
        mDiaryColorInitialized = false;

        mGroupName = null;
        mGroupColor = -1;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj == null) {
            return false;
        }
        if(!(obj instanceof Calendar)){
            return false;
        }

        CalendarDiaryModel other = (CalendarDiaryModel) obj;
        if(!checkOriginalModelFields(other)) {
            return false;
        }
        return true;
    }

    public boolean isUnchanged(CalendarDiaryModel originalModel) {
        if(this == originalModel) {
            return true;
        }
        if(originalModel == null) {
            return false;
        }
        if(!checkOriginalModelFields(originalModel)) {
            return false;
        }
        if(isEmpty(mDiaryWeather)) {
            if(!isEmpty(originalModel.mDiaryWeather)) {
                return false;
            }
        } else if(mDiaryWeather.equals(originalModel.mDiaryWeather)) {
            return false;
        }
        if(isEmpty(mDiaryLocation)) {
            if(!isEmpty(originalModel.mDiaryLocation)) {
                return false;
            }
        } else if(mDiaryLocation.equals(originalModel.mDiaryLocation)) {
            return false;
        }
        if(isEmpty(mDiaryImg)) {
            if(!isEmpty(originalModel.mDiaryImg)) {
                return false;
            }
        } else if(mDiaryImg.equals(originalModel.mDiaryImg)) {
            return false;
        }
        return true;
    }

    protected boolean checkOriginalModelFields(CalendarDiaryModel originalModel) {
        if (mDiaryId != originalModel.mDiaryId) {
            return false;
        }
        if(mDiaryUserId != originalModel.mDiaryUserId) {
            return false;
        }
        if(mDiaryGroupId != originalModel.mDiaryGroupId) {
            return false;
        }
        return true;
    }

    public int[] getGroupColors() {
        if(mDiaryColorCache != null) {
            return mDiaryColorCache.getColorArray(mGroupName, mDiaryGroupId);
        }
        return null;
    }

    public int getGroupColor() {
        if(mGroupName == null || mGroupColor == -1) {
          return mDiaryColor;
        }
        return mGroupColor;
    }

    public String getDiaryColorKey() {
        if (mDiaryColorCache != null) {
            if(isInGroup()) {
                return mDiaryColorCache.getColorKey(mGroupName, mDiaryGroupId, mGroupColor);
            }
            return mDiaryColorCache.getColorKey(mDiaryUserId, mDiaryColor);
        }
        return "";
    }

    public int getDiaryColor() {
        return mDiaryColor;
    }

    public void setDiaryColor(int color) {
        mDiaryColor = color;
        mDiaryColorInitialized = true;
    }

    public boolean isInGroup() {
        if(mDiaryGroupId > 0) {
            return true;
        }
        return false;
    }
}
