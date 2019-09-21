package com.android.nanal.calendar;

import com.android.nanal.diary.DiaryColorCache;

import java.io.Serializable;

public class CalendarGroupModel implements Serializable {
    private static final String TAG = "CalendarGroupModel";
    public String mUri = null;

    public int group_id;
    public String group_name;
    public int group_color;
    public String account_id;

    public DiaryColorCache mColorCache;

    public boolean isCreated;

    public boolean isValid() {
        if(group_id == -1) {
            return false;
        }
        if(group_name == null) {
            return false;
        }
        if(group_color == -1) {
            return false;
        }
        if(account_id == null) {
            return false;
        }
        return true;
    }

    public void clear() {
        group_id = -1;
        group_name = null;
        group_color = -1;
        account_id = null;
        isCreated = false;
    }

    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj == null) {
            return false;
        }

        CalendarGroupModel other = (CalendarGroupModel) obj;
        if(!checkOriginalModelFields(other)) {
            return false;
        }
        return true;
    }

    public boolean isEmpty() {
        if(group_id > 0) {
            return false;
        }
        if(group_name != null && group_name.trim().length() > 0) {
            return false;
        }
        if(account_id != null && account_id.trim().length() > 0) {
            return false;
        }
        if(group_color > 0) {
            return false;
        }
        return true;
    }


    public boolean isUnchanged(CalendarGroupModel originalModel) {
        if(this == originalModel) {
            return true;
        }
        if(originalModel == null) {
            return false;
        }
        if(!checkOriginalModelFields(originalModel)) {
            return false;
        }
        if(!group_name.equals(originalModel.group_name)) {
            return false;
        }
        if(group_color != originalModel.group_color) {
            return false;
        }
        if(!account_id.equals(originalModel.account_id)) {
            return false;
        }
        return true;
    }

    protected boolean checkOriginalModelFields(CalendarGroupModel originalModel) {
        if (group_id != originalModel.group_id) {
            return false;
        }
        return true;
    }

    public int[] getGroupColors() {
        if(mColorCache != null) {
            return mColorCache.getColorArray(group_name, group_id);
        }
        return null;
    }

    public String getGroupColorKey() {
        if (mColorCache != null) {
            return mColorCache.getColorKey(group_name, group_id, group_color);
        }
        return "";
    }

    public int getGroupColor() {
        return group_color;
    }

    public void setGroupColor(int color) {
        group_color = color;
    }
}
