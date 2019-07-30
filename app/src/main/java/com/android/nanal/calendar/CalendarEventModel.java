package com.android.nanal.calendar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.text.util.Rfc822Token;

import com.android.nanal.event.GeneralPreferences;
import com.android.nanal.Rfc822Validator;
import com.android.nanal.event.Utils;
import com.android.nanal.event.EditEventHelper;
import com.android.nanal.event.EventColorCache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.TimeZone;

/**
 * Stores all the information needed to fill out an entry in the events table.
 * This is a convenient way for storing information needed by the UI to write to
 * the events table. Only fields that are important to the UI are included.
 * 이벤트 테이블에 항목을 작성하는 데 필요한 모든 정보를 저장함
 * 이벤트 테이블에 쓰기 위해 필요한 정보를 UI가 저장하는 편리한 방법임
 * UI에 중요한 필드만 포함됨
 */
public class CalendarEventModel implements Serializable {
    private static final String TAG = "CalendarEventModel";
    /**
     * The uri of the event in the db. This should only be null for new events.
     * db에서 이벤트의 url, 새로운 이벤트에 대해서만 null임
     */
    public String mUri = null;
    public long mId = -1;

    // TODO strip out fields that don't ever get used
    public long mCalendarId = -1;
    public String mCalendarDisplayName = ""; // Make sure this is in sync with the mCalendarId, mCalendarId와 동기화되었는지 확인
    public String mCalendarAccountName;
    public String mCalendarAccountType;
    public int mCalendarMaxReminders;
    public String mCalendarAllowedReminders;
    public String mCalendarAllowedAttendeeTypes;
    public String mCalendarAllowedAvailability;
    public String mSyncId = null;
    public String mSyncAccount = null;
    public String mSyncAccountType = null;
    public EventColorCache mEventColorCache;
    // PROVIDER_NOTES owner account comes from the calendars table
    // PROVIDER_NOTES 소유자 계정은 캘린더 테이블에서 제공됨
    public String mOwnerAccount = null;
    public String mTitle = null;
    public String mLocation = null;
    public String mDescription = null;
    public String mRrule = null;
    public String mOrganizer = null;
    public String mOrganizerDisplayName = null;
    /**
     * Read-Only - Derived from other fields
     * 읽기 전용 - 다른 필드에서 파생됨
     */
    public boolean mIsOrganizer = true;
    public boolean mIsFirstEventInSeries = true;
    // This should be set the same as mStart when created and is used for making changes to
    // recurring events. It should not be updated after it is initially set.
    // mStart와 동일하게 설정되어야 하며, 반복 이벤트를 변경하는 데 사용됨
    // 맨 처음 설정된 후에 업데이트되어서는 안 됨
    public long mOriginalStart = -1;
    public long mStart = -1;
    // This should be set the same as mEnd when created and is used for making changes to
    // recurring events. It should not be updated after it is initially set.
    // mEnd와 동일하게 설정되어야 하며, 반복 이벤트를 변경하는 데 사용됨
    // 맨 처음 설정된 후에 업데이트되어서는 안 됨
    public long mOriginalEnd = -1;
    public long mEnd = -1;
    public String mDuration = null;
    public String mTimezone = null;
    public String mTimezone2 = null;
    public boolean mAllDay = false;
    public boolean mHasAlarm = false;
    public int mAvailability = Events.AVAILABILITY_BUSY;
    // PROVIDER_NOTES How does an event not have attendee data? The owner is added
    // as an attendee by default.
    // PRORIVER_NOTES 이벤트에 참석자 데이터가 없는 경우? 소유자는 기본적으로 참석자로 추가됨
    public boolean mHasAttendeeData = true;
    public int mSelfAttendeeStatus = -1;
    public int mOwnerAttendeeId = -1;
    public String mOriginalSyncId = null;
    public long mOriginalId = -1;
    public Long mOriginalTime = null;
    public Boolean mOriginalAllDay = null;
    public boolean mGuestsCanModify = false;
    public boolean mGuestsCanInviteOthers = false;
    public boolean mGuestsCanSeeGuests = false;
    public boolean mOrganizerCanRespond = false;
    public int mCalendarAccessLevel = Calendars.CAL_ACCESS_CONTRIBUTOR;
    public int mEventStatus = Events.STATUS_CONFIRMED;
    // The model can't be updated with a calendar cursor until it has been
    // updated with an event cursor.
    // 모델은 이벤트 커서로 업데이트될 때까지 캘린더 커서로 업데이트할 수 없음
    public boolean mModelUpdatedWithEventCursor;
    public int mAccessLevel = 0;
    public ArrayList<ReminderEntry> mReminders;
    public ArrayList<ReminderEntry> mDefaultReminders;
    // PROVIDER_NOTES Using EditEventHelper the owner should not be included in this
    // list and will instead be added by saveEvent. Is this what we want?
    // EditEventHelper를 사용하면 소유자가 이 목록에 포함되지 않아야 하며, 대신 saveEvent에 의해 추가됨
    public LinkedHashMap<String, Attendee> mAttendeesList;
    private int mCalendarColor = -1;
    private boolean mCalendarColorInitialized = false;
    private int mEventColor = -1;
    private boolean mEventColorInitialized = false;

    public CalendarEventModel() {
        mReminders = new ArrayList<ReminderEntry>();
        mDefaultReminders = new ArrayList<ReminderEntry>();
        mAttendeesList = new LinkedHashMap<String, Attendee>();
        mTimezone = TimeZone.getDefault().getID();
    }

    public CalendarEventModel(Context context) {
        this();

        mTimezone = Utils.getTimeZone(context, null);
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);

        String defaultReminder = prefs.getString(
                GeneralPreferences.KEY_DEFAULT_REMINDER, GeneralPreferences.NO_REMINDER_STRING);
        int defaultReminderMins = Integer.parseInt(defaultReminder);
        if (defaultReminderMins != GeneralPreferences.NO_REMINDER) {
            // Assume all calendars allow at least one reminder.
            // 모든 캘린더가 적어도 한 가지 알림을 허용한다고 가정함
            mHasAlarm = true;
            mReminders.add(ReminderEntry.valueOf(defaultReminderMins));
            mDefaultReminders.add(ReminderEntry.valueOf(defaultReminderMins));
        }
    }

    public CalendarEventModel(Context context, Intent intent) {
        this(context);

        if (intent == null) {
            return;
        }

        String title = intent.getStringExtra(Events.TITLE);
        if (title != null) {
            mTitle = title;
        }

        String location = intent.getStringExtra(Events.EVENT_LOCATION);
        if (location != null) {
            mLocation = location;
        }

        String description = intent.getStringExtra(Events.DESCRIPTION);
        if (description != null) {
            mDescription = description;
        }

        int availability = intent.getIntExtra(Events.AVAILABILITY, -1);
        if (availability != -1) {
            mAvailability = availability;
        }

        int accessLevel = intent.getIntExtra(Events.ACCESS_LEVEL, -1);
        if (accessLevel != -1) {
            mAccessLevel = accessLevel;
        }

        String rrule = intent.getStringExtra(Events.RRULE);
        if (!TextUtils.isEmpty(rrule)) {
            mRrule = rrule;
        }

        String emails = intent.getStringExtra(Intent.EXTRA_EMAIL);
        if (!TextUtils.isEmpty(emails)) {
            String[] emailArray = emails.split("[ ,;]");
            for (String email : emailArray) {
                if (!TextUtils.isEmpty(email) && email.contains("@")) {
                    email = email.trim();
                    if (!mAttendeesList.containsKey(email)) {
                        mAttendeesList.put(email, new Attendee("", email));
                    }
                }
            }
        }
    }

    public boolean isValid() {
        if (mCalendarId == -1) {
            return false;
        }
        if (TextUtils.isEmpty(mOwnerAccount)) {
            return false;
        }
        return true;
    }

    public boolean isEmpty() {
        if (mTitle != null && mTitle.trim().length() > 0) {
            return false;
        }

        if (mLocation != null && mLocation.trim().length() > 0) {
            return false;
        }

        if (mDescription != null && mDescription.trim().length() > 0) {
            return false;
        }

        return true;
    }

    public void clear() {
        mUri = null;
        mId = -1;
        mCalendarId = -1;
        mCalendarColor = -1;
        mCalendarColorInitialized = false;

        mEventColorCache = null;
        mEventColor = -1;
        mEventColorInitialized = false;

        mSyncId = null;
        mSyncAccount = null;
        mSyncAccountType = null;
        mOwnerAccount = null;

        mTitle = null;
        mLocation = null;
        mDescription = null;
        mRrule = null;
        mOrganizer = null;
        mOrganizerDisplayName = null;
        mIsOrganizer = true;
        mIsFirstEventInSeries = true;

        mOriginalStart = -1;
        mStart = -1;
        mOriginalEnd = -1;
        mEnd = -1;
        mDuration = null;
        mTimezone = null;
        mTimezone2 = null;
        mAllDay = false;
        mHasAlarm = false;

        mHasAttendeeData = true;
        mSelfAttendeeStatus = -1;
        mOwnerAttendeeId = -1;
        mOriginalId = -1;
        mOriginalSyncId = null;
        mOriginalTime = null;
        mOriginalAllDay = null;

        mGuestsCanModify = false;
        mGuestsCanInviteOthers = false;
        mGuestsCanSeeGuests = false;
        mAccessLevel = 0;
        mEventStatus = Events.STATUS_CONFIRMED;
        mOrganizerCanRespond = false;
        mCalendarAccessLevel = Calendars.CAL_ACCESS_CONTRIBUTOR;
        mModelUpdatedWithEventCursor = false;
        mCalendarAllowedReminders = null;
        mCalendarAllowedAttendeeTypes = null;
        mCalendarAllowedAvailability = null;

        mReminders = new ArrayList<ReminderEntry>();
        mAttendeesList.clear();
    }

    public void addAttendee(Attendee attendee) {
        mAttendeesList.put(attendee.mEmail, attendee);
    }

    public void addAttendees(String attendees, Rfc822Validator validator) {
        final LinkedHashSet<Rfc822Token> addresses = EditEventHelper.getAddressesFromList(
                attendees, validator);
        synchronized (this) {
            for (final Rfc822Token address : addresses) {
                final Attendee attendee = new Attendee(address.getName(), address.getAddress());
                if (TextUtils.isEmpty(attendee.mName)) {
                    attendee.mName = attendee.mEmail;
                }
                addAttendee(attendee);
            }
        }
    }

    public void removeAttendee(Attendee attendee) {
        mAttendeesList.remove(attendee.mEmail);
    }

    public String getAttendeesString() {
        StringBuilder b = new StringBuilder();
        for (Attendee attendee : mAttendeesList.values()) {
            String name = attendee.mName;
            String email = attendee.mEmail;
            String status = Integer.toString(attendee.mStatus);
            b.append("name:").append(name);
            b.append(" email:").append(email);
            b.append(" status:").append(status);
        }
        return b.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mAllDay ? 1231 : 1237);
        result = prime * result + ((mAttendeesList == null) ? 0 : getAttendeesString().hashCode());
        result = prime * result + (int) (mCalendarId ^ (mCalendarId >>> 32));
        result = prime * result + ((mDescription == null) ? 0 : mDescription.hashCode());
        result = prime * result + ((mDuration == null) ? 0 : mDuration.hashCode());
        result = prime * result + (int) (mEnd ^ (mEnd >>> 32));
        result = prime * result + (mGuestsCanInviteOthers ? 1231 : 1237);
        result = prime * result + (mGuestsCanModify ? 1231 : 1237);
        result = prime * result + (mGuestsCanSeeGuests ? 1231 : 1237);
        result = prime * result + (mOrganizerCanRespond ? 1231 : 1237);
        result = prime * result + (mModelUpdatedWithEventCursor ? 1231 : 1237);
        result = prime * result + mCalendarAccessLevel;
        result = prime * result + (mHasAlarm ? 1231 : 1237);
        result = prime * result + (mHasAttendeeData ? 1231 : 1237);
        result = prime * result + (int) (mId ^ (mId >>> 32));
        result = prime * result + (mIsFirstEventInSeries ? 1231 : 1237);
        result = prime * result + (mIsOrganizer ? 1231 : 1237);
        result = prime * result + ((mLocation == null) ? 0 : mLocation.hashCode());
        result = prime * result + ((mOrganizer == null) ? 0 : mOrganizer.hashCode());
        result = prime * result + ((mOriginalAllDay == null) ? 0 : mOriginalAllDay.hashCode());
        result = prime * result + (int) (mOriginalEnd ^ (mOriginalEnd >>> 32));
        result = prime * result + ((mOriginalSyncId == null) ? 0 : mOriginalSyncId.hashCode());
        result = prime * result + (int) (mOriginalId ^ (mOriginalEnd >>> 32));
        result = prime * result + (int) (mOriginalStart ^ (mOriginalStart >>> 32));
        result = prime * result + ((mOriginalTime == null) ? 0 : mOriginalTime.hashCode());
        result = prime * result + ((mOwnerAccount == null) ? 0 : mOwnerAccount.hashCode());
        result = prime * result + ((mReminders == null) ? 0 : mReminders.hashCode());
        result = prime * result + ((mRrule == null) ? 0 : mRrule.hashCode());
        result = prime * result + mSelfAttendeeStatus;
        result = prime * result + mOwnerAttendeeId;
        result = prime * result + (int) (mStart ^ (mStart >>> 32));
        result = prime * result + ((mSyncAccount == null) ? 0 : mSyncAccount.hashCode());
        result = prime * result + ((mSyncAccountType == null) ? 0 : mSyncAccountType.hashCode());
        result = prime * result + ((mSyncId == null) ? 0 : mSyncId.hashCode());
        result = prime * result + ((mTimezone == null) ? 0 : mTimezone.hashCode());
        result = prime * result + ((mTimezone2 == null) ? 0 : mTimezone2.hashCode());
        result = prime * result + ((mTitle == null) ? 0 : mTitle.hashCode());
        result = prime * result + (mAvailability);
        result = prime * result + ((mUri == null) ? 0 : mUri.hashCode());
        result = prime * result + mAccessLevel;
        result = prime * result + mEventStatus;
        return result;
    }

    // Autogenerated equals method
    // 자동 생성 equals 메소드
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CalendarEventModel)) {
            return false;
        }

        CalendarEventModel other = (CalendarEventModel) obj;
        if (!checkOriginalModelFields(other)) {
            return false;
        }

        if (mLocation == null) {
            if (other.mLocation != null) {
                return false;
            }
        } else if (!mLocation.equals(other.mLocation)) {
            return false;
        }

        if (mTitle == null) {
            if (other.mTitle != null) {
                return false;
            }
        } else if (!mTitle.equals(other.mTitle)) {
            return false;
        }

        if (mDescription == null) {
            if (other.mDescription != null) {
                return false;
            }
        } else if (!mDescription.equals(other.mDescription)) {
            return false;
        }

        if (mDuration == null) {
            if (other.mDuration != null) {
                return false;
            }
        } else if (!mDuration.equals(other.mDuration)) {
            return false;
        }

        if (mEnd != other.mEnd) {
            return false;
        }
        if (mIsFirstEventInSeries != other.mIsFirstEventInSeries) {
            return false;
        }
        if (mOriginalEnd != other.mOriginalEnd) {
            return false;
        }

        if (mOriginalStart != other.mOriginalStart) {
            return false;
        }
        if (mStart != other.mStart) {
            return false;
        }

        if (mOriginalId != other.mOriginalId) {
            return false;
        }

        if (mOriginalSyncId == null) {
            if (other.mOriginalSyncId != null) {
                return false;
            }
        } else if (!mOriginalSyncId.equals(other.mOriginalSyncId)) {
            return false;
        }

        if (mRrule == null) {
            if (other.mRrule != null) {
                return false;
            }
        } else if (!mRrule.equals(other.mRrule)) {
            return false;
        }
        return true;
    }

    /**
     * Whether the event has been modified based on its original model.
     * 이벤트가 원래 모델(original model)에 따라 수정되었는지
     *
     * @param originalModel
     * @return true if the model is unchanged, false otherwise
     *          모델이 변경되지 않았으면 true, 그렇지 않으면 false
     */
    public boolean isUnchanged(CalendarEventModel originalModel) {
        if (this == originalModel) {
            return true;
        }
        if (originalModel == null) {
            return false;
        }

        if (!checkOriginalModelFields(originalModel)) {
            return false;
        }

        if (TextUtils.isEmpty(mLocation)) {
            if (!TextUtils.isEmpty(originalModel.mLocation)) {
                return false;
            }
        } else if (!mLocation.equals(originalModel.mLocation)) {
            return false;
        }

        if (TextUtils.isEmpty(mTitle)) {
            if (!TextUtils.isEmpty(originalModel.mTitle)) {
                return false;
            }
        } else if (!mTitle.equals(originalModel.mTitle)) {
            return false;
        }

        if (TextUtils.isEmpty(mDescription)) {
            if (!TextUtils.isEmpty(originalModel.mDescription)) {
                return false;
            }
        } else if (!mDescription.equals(originalModel.mDescription)) {
            return false;
        }

        if (TextUtils.isEmpty(mDuration)) {
            if (!TextUtils.isEmpty(originalModel.mDuration)) {
                return false;
            }
        } else if (!mDuration.equals(originalModel.mDuration)) {
            return false;
        }

        if (mEnd != mOriginalEnd) {
            return false;
        }
        if (mStart != mOriginalStart) {
            return false;
        }

        // If this changed the original id and it's not just an exception to the
        // original event
        // 이것이 original id를 변경했고 단순히 original event에 대한 예외가 아닌 경우
        if (mOriginalId != originalModel.mOriginalId && mOriginalId != originalModel.mId) {
            return false;
        }

        if (TextUtils.isEmpty(mRrule)) {
            // if the rrule is no longer empty check if this is an exception
            // rrule이 더 이상 비어 있지 않으면 이 예외가 아닌지 확인함
            if (!TextUtils.isEmpty(originalModel.mRrule)) {
                boolean syncIdNotReferenced = mOriginalSyncId == null
                        || !mOriginalSyncId.equals(originalModel.mSyncId);
                boolean localIdNotReferenced = mOriginalId == -1
                        || !(mOriginalId == originalModel.mId);
                if (syncIdNotReferenced && localIdNotReferenced) {
                    return false;
                }
            }
        } else if (!mRrule.equals(originalModel.mRrule)) {
            return false;
        }

        return true;
    }

    /**
     * Checks against an original model for changes to an event. This covers all
     * the fields that should remain consistent between an original event model
     * and the new one if nothing in the event was modified. This is also the
     * portion that overlaps with equality between two event models.
     * 이벤트에 대한 변경 사항을 원본 모델과 대조함
     * 이는 이벤트에서 아무것도 수정되지 않은 경우, 원래 이벤트 모델과 새 이벤트 모델 간
     * 일관성을 유지해야 하는 모든 필드를 포함함
     * 또한 두 이벤트 모델 간의 동일성...과 겹치는 부분
     *
     * @param originalModel
     * @return true if these fields are unchanged, false otherwise
     *          필드가 변경되지 않았으면 true, 그렇지 않으면 false
     */
    protected boolean checkOriginalModelFields(CalendarEventModel originalModel) {
        if (mAllDay != originalModel.mAllDay) {
            return false;
        }
        if (mAttendeesList == null) {
            if (originalModel.mAttendeesList != null) {
                return false;
            }
        } else if (!mAttendeesList.equals(originalModel.mAttendeesList)) {
            return false;
        }

        if (mCalendarId != originalModel.mCalendarId) {
            return false;
        }
        if (mCalendarColor != originalModel.mCalendarColor) {
            return false;
        }
        if (mCalendarColorInitialized != originalModel.mCalendarColorInitialized) {
            return false;
        }
        if (mGuestsCanInviteOthers != originalModel.mGuestsCanInviteOthers) {
            return false;
        }
        if (mGuestsCanModify != originalModel.mGuestsCanModify) {
            return false;
        }
        if (mGuestsCanSeeGuests != originalModel.mGuestsCanSeeGuests) {
            return false;
        }
        if (mOrganizerCanRespond != originalModel.mOrganizerCanRespond) {
            return false;
        }
        if (mCalendarAccessLevel != originalModel.mCalendarAccessLevel) {
            return false;
        }
        if (mModelUpdatedWithEventCursor != originalModel.mModelUpdatedWithEventCursor) {
            return false;
        }
        if (mHasAlarm != originalModel.mHasAlarm) {
            return false;
        }
        if (mHasAttendeeData != originalModel.mHasAttendeeData) {
            return false;
        }
        if (mId != originalModel.mId) {
            return false;
        }
        if (mIsOrganizer != originalModel.mIsOrganizer) {
            return false;
        }

        if (mOrganizer == null) {
            if (originalModel.mOrganizer != null) {
                return false;
            }
        } else if (!mOrganizer.equals(originalModel.mOrganizer)) {
            return false;
        }

        if (mOriginalAllDay == null) {
            if (originalModel.mOriginalAllDay != null) {
                return false;
            }
        } else if (!mOriginalAllDay.equals(originalModel.mOriginalAllDay)) {
            return false;
        }

        if (mOriginalTime == null) {
            if (originalModel.mOriginalTime != null) {
                return false;
            }
        } else if (!mOriginalTime.equals(originalModel.mOriginalTime)) {
            return false;
        }

        if (mOwnerAccount == null) {
            if (originalModel.mOwnerAccount != null) {
                return false;
            }
        } else if (!mOwnerAccount.equals(originalModel.mOwnerAccount)) {
            return false;
        }

        if (mReminders == null) {
            if (originalModel.mReminders != null) {
                return false;
            }
        } else if (!mReminders.equals(originalModel.mReminders)) {
            return false;
        }

        if (mSelfAttendeeStatus != originalModel.mSelfAttendeeStatus) {
            return false;
        }
        if (mOwnerAttendeeId != originalModel.mOwnerAttendeeId) {
            return false;
        }
        if (mSyncAccount == null) {
            if (originalModel.mSyncAccount != null) {
                return false;
            }
        } else if (!mSyncAccount.equals(originalModel.mSyncAccount)) {
            return false;
        }

        if (mSyncAccountType == null) {
            if (originalModel.mSyncAccountType != null) {
                return false;
            }
        } else if (!mSyncAccountType.equals(originalModel.mSyncAccountType)) {
            return false;
        }

        if (mSyncId == null) {
            if (originalModel.mSyncId != null) {
                return false;
            }
        } else if (!mSyncId.equals(originalModel.mSyncId)) {
            return false;
        }

        if (mTimezone == null) {
            if (originalModel.mTimezone != null) {
                return false;
            }
        } else if (!mTimezone.equals(originalModel.mTimezone)) {
            return false;
        }

        if (mTimezone2 == null) {
            if (originalModel.mTimezone2 != null) {
                return false;
            }
        } else if (!mTimezone2.equals(originalModel.mTimezone2)) {
            return false;
        }

        if (mAvailability != originalModel.mAvailability) {
            return false;
        }

        if (mUri == null) {
            if (originalModel.mUri != null) {
                return false;
            }
        } else if (!mUri.equals(originalModel.mUri)) {
            return false;
        }

        if (mAccessLevel != originalModel.mAccessLevel) {
            return false;
        }

        if (mEventStatus != originalModel.mEventStatus) {
            return false;
        }

        if (mEventColor != originalModel.mEventColor) {
            return false;
        }

        if (mEventColorInitialized != originalModel.mEventColorInitialized) {
            return false;
        }

        return true;
    }

    /**
     * Sort and uniquify mReminderMinutes.
     * mReminderMinutes 정렬 및 고유화(uniquify)
     *
     * @return true (for convenience of caller)
     *          true(caller의 편의를 위해)
     */
    public boolean normalizeReminders() {
        if (mReminders.size() <= 1) {
            return true;
        }

        // sort
        Collections.sort(mReminders);

        // remove duplicates
        // 중복된 것 제거함
        ReminderEntry prev = mReminders.get(mReminders.size()-1);
        for (int i = mReminders.size()-2; i >= 0; --i) {
            ReminderEntry cur = mReminders.get(i);
            if (prev.equals(cur)) {
                // match, remove later entry
                // 일치, 이후 항목은 제거
                mReminders.remove(i+1);
            }
            prev = cur;
        }

        return true;
    }

    public boolean isCalendarColorInitialized() {
        return mCalendarColorInitialized;
    }

    public boolean isEventColorInitialized() {
        return mEventColorInitialized;
    }

    public int getCalendarColor() {
        return mCalendarColor;
    }

    public void setCalendarColor(int color) {
        mCalendarColor = color;
        mCalendarColorInitialized = true;
    }

    public int getEventColor() {
        return mEventColor;
    }

    public void setEventColor(int color) {
        mEventColor = color;
        mEventColorInitialized = true;
    }

    public int[] getCalendarEventColors() {
        if (mEventColorCache != null) {
            return mEventColorCache.getColorArray(mCalendarAccountName, mCalendarAccountType);
        }
        return null;
    }

    public String getEventColorKey() {
        if (mEventColorCache != null) {
            return mEventColorCache.getColorKey(mCalendarAccountName, mCalendarAccountType,
                    mEventColor);
        }
        return "";
    }

    public static class Attendee implements Serializable {
        public String mName;
        public String mEmail;
        public int mStatus;
        public String mIdentity;
        public String mIdNamespace;

        public Attendee(String name, String email) {
            this(name, email, Attendees.ATTENDEE_STATUS_NONE, null, null);
        }

        public Attendee(String name, String email, int status, String identity,
                        String idNamespace) {
            mName = name;
            mEmail = email;
            mStatus = status;
            mIdentity = identity;
            mIdNamespace = idNamespace;
        }

        @Override
        public int hashCode() {
            return (mEmail == null) ? 0 : mEmail.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Attendee)) {
                return false;
            }
            Attendee other = (Attendee) obj;
            if (!TextUtils.equals(mEmail, other.mEmail)) {
                return false;
            }
            return true;
        }

        String getDisplayName() {
            if (TextUtils.isEmpty(mName)) {
                return mEmail;
            } else {
                return mName;
            }
        }
    }


    /**
     * A single reminder entry.
     * 싱글 리마인더
     * <p/>
     * Instances of the class are immutable.
     * 클래스의 인스턴스는 변경할 수 없음
     */
    public static class ReminderEntry implements Comparable<ReminderEntry>, Serializable {
        private final int mMinutes;
        private final int mMethod;

        /**
         * Constructs a new ReminderEntry.
         * 새 ReminderEntry 구성
         *
         * @param minutes Number of minutes before the start of the event that the alert will fire.
         *                이벤트가 시작되기 전 알림이 시작될 시간(분)
         * @param method Type of alert ({@link Reminders#METHOD_ALERT}, etc).
         *               알림의 유형
         */
        private ReminderEntry(int minutes, int method) {
            // TODO: error-check args
            mMinutes = minutes;
            mMethod = method;
        }

        /**
         * Returns a new ReminderEntry, with the specified minutes and method.
         * 지정된 분과 메소드를 사용하여 새 ReminderEntry를 반환함
         *
         * @param minutes Number of minutes before the start of the event that the alert will fire.
         *                이벤트가 시작되기 전 알림이 시작될 시간(분)
         * @param method Type of alert ({@link Reminders#METHOD_ALERT}, etc).
         *               알림의 유형
         */
        public static ReminderEntry valueOf(int minutes, int method) {
            // TODO: cache common instances
            return new ReminderEntry(minutes, method);
        }

        /**
         * Returns a ReminderEntry, with the specified number of minutes and a default alert method.
         * 지정된 시간(분)과 기본 알림 메소드를 사용하여 ReminderEntry를 반환함
         *
         * @param minutes Number of minutes before the start of the event that the alert will fire.
         *                이벤트가 시작되기 전 알림이 시작될 시간(분)
         */
        public static ReminderEntry valueOf(int minutes) {
            return valueOf(minutes, Reminders.METHOD_DEFAULT);
        }

        @Override
        public int hashCode() {
            return mMinutes * 10 + mMethod;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ReminderEntry)) {
                return false;
            }

            ReminderEntry re = (ReminderEntry) obj;

            if (re.mMinutes != mMinutes) {
                return false;
            }

            // Treat ALERT and DEFAULT as equivalent.  This is useful during the "has anything
            // "changed" test, so that if DEFAULT is present, but we don't change anything,
            // the internal conversion of DEFAULT to ALERT doesn't force a database update.
            // ALERT과 DEFAULT를 동등한 것으로 취급함
            // 이는 "변경된 것이 있다면" 테스트 중에 유용하게 사용되므로, DEFAULT가 존재하지만
            // 아무것도 변경하지 않은 경우, DEFAULT를 ALERT로 내부에서 변환해도 데이터베이스 업데이트를
            // 강제하지 않음
            return re.mMethod == mMethod ||
                    (re.mMethod == Reminders.METHOD_DEFAULT && mMethod == Reminders.METHOD_ALERT) ||
                    (re.mMethod == Reminders.METHOD_ALERT && mMethod == Reminders.METHOD_DEFAULT);
        }

        @Override
        public String toString() {
            return "ReminderEntry min=" + mMinutes + " meth=" + mMethod;
        }

        /**
         * Comparison function for a sort ordered primarily descending by minutes,
         * secondarily ascending by method type.
         * 분 단위로 내림차순으로 정렬하고, 두 번째로 메소드 타입별로 오름차순 정렬하는 비교 함수
         */
        @Override
        public int compareTo(ReminderEntry re) {
            if (re.mMinutes != mMinutes) {
                return re.mMinutes - mMinutes;
            }
            if (re.mMethod != mMethod) {
                return mMethod - re.mMethod;
            }
            return 0;
        }

        /** Returns the minutes. */
        public int getMinutes() {
            return mMinutes;
        }

        /** Returns the alert method. */
        public int getMethod() {
            return mMethod;
        }
    }
}
