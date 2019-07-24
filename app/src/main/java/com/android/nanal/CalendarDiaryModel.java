package com.android.nanal;

import android.content.Context;
import android.content.Intent;

import java.io.Serializable;
import java.util.Calendar;

public class CalendarDiaryModel implements Serializable {
    private static final String TAG = "CalendarDiaryModel";
    public String mUri = null;
    public long mId = -1;

    public int mDiaryId;
    public String mDiaryUserId;
    public int mDiaryGroupId;
    public String mDiaryContents;
    public int mDiaryColor;
    public String mDiaryWeather;
    public String mDiaryLocation;
    public String mDiaryImg;

    public EventColorCache mDiaryColorCache;
    public String mTimezone = null;

    public CalendarDiaryModel(Context context){
        this();
        //mTimezone = Utils.getTimeZone(context, null);
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
        // 검사 조건 추가하기
        if(mDiaryContents != null && mDiaryContents.trim().length() > 0)
            return false;
        return true;
    }

    public boolean isEmpty(String str) {
        // String 비었는지 확인하는 메소드
        return str == null || str.length() == 0;
    }

    public void clear() {
        mUri = null;
        mId = -1;

        mDiaryId = -1;
        mDiaryUserId = null;
        mDiaryGroupId = -1;
        mDiaryContents = null;
        mDiaryColor = -1;
        mDiaryWeather = null;
        mDiaryLocation = null;
        mDiaryImg = null;
        mTimezone = null;
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
        // 조건 추가
        return true;
    }
}
