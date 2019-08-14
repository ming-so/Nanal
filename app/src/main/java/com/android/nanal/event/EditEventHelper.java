package com.android.nanal.event;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.View;

import com.android.nanal.DateException;
import com.android.nanal.RecurrenceProcessor;
import com.android.nanal.RecurrenceSet;
import com.android.nanal.Rfc822Validator;
import com.android.nanal.activity.AbstractCalendarActivity;
import com.android.nanal.calendar.CalendarEventModel;
import com.android.nanal.calendar.CalendarEventModel.Attendee;
import com.android.nanal.calendar.CalendarEventModel.ReminderEntry;
import com.android.nanal.query.AsyncQueryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TimeZone;

public class EditEventHelper {
    private static final String TAG = "EditEventHelper";
    private static final boolean DEBUG = false;

    // Used for parsing rrules for special cases.
    // 특수 케이스에 대한 rrules 파싱 시 사용됨
    private EventRecurrence mEventRecurrence = new EventRecurrence();

    private static final String NO_EVENT_COLOR = "";

    public static final String[] EVENT_PROJECTION = new String[] {
            Events._ID, // 0
            Events.TITLE, // 1
            Events.DESCRIPTION, // 2
            Events.EVENT_LOCATION, // 3
            Events.ALL_DAY, // 4
            Events.HAS_ALARM, // 5
            Events.CALENDAR_ID, // 6
            Events.DTSTART, // 7
            Events.DTEND, // 8
            Events.DURATION, // 9
            Events.EVENT_TIMEZONE, // 10
            Events.RRULE, // 11
            Events._SYNC_ID, // 12
            Events.AVAILABILITY, // 13
            Events.ACCESS_LEVEL, // 14
            Events.OWNER_ACCOUNT, // 15
            Events.HAS_ATTENDEE_DATA, // 16
            Events.ORIGINAL_SYNC_ID, // 17
            Events.ORGANIZER, // 18
            Events.GUESTS_CAN_MODIFY, // 19
            Events.ORIGINAL_ID, // 20
            Events.STATUS, // 21
            Events.CALENDAR_COLOR, // 22
            Events.EVENT_COLOR, // 23
            Events.EVENT_COLOR_KEY // 24
    };

        protected static final int EVENT_INDEX_ID = 0;
        protected static final int EVENT_INDEX_TITLE = 1;
        protected static final int EVENT_INDEX_DESCRIPTION = 2;
        protected static final int EVENT_INDEX_EVENT_LOCATION = 3;
        protected static final int EVENT_INDEX_ALL_DAY = 4;
        protected static final int EVENT_INDEX_HAS_ALARM = 5;
        protected static final int EVENT_INDEX_CALENDAR_ID = 6;
        protected static final int EVENT_INDEX_DTSTART = 7;
        protected static final int EVENT_INDEX_DTEND = 8;
        protected static final int EVENT_INDEX_DURATION = 9;
        protected static final int EVENT_INDEX_TIMEZONE = 10;
        protected static final int EVENT_INDEX_RRULE = 11;
        protected static final int EVENT_INDEX_SYNC_ID = 12;
        protected static final int EVENT_INDEX_AVAILABILITY = 13;
        protected static final int EVENT_INDEX_ACCESS_LEVEL = 14;
        protected static final int EVENT_INDEX_OWNER_ACCOUNT = 15;
        protected static final int EVENT_INDEX_HAS_ATTENDEE_DATA = 16;
        protected static final int EVENT_INDEX_ORIGINAL_SYNC_ID = 17;
        protected static final int EVENT_INDEX_ORGANIZER = 18;
        protected static final int EVENT_INDEX_GUESTS_CAN_MODIFY = 19;
        protected static final int EVENT_INDEX_ORIGINAL_ID = 20;
        protected static final int EVENT_INDEX_EVENT_STATUS = 21;
        protected static final int EVENT_INDEX_CALENDAR_COLOR = 22;
        protected static final int EVENT_INDEX_EVENT_COLOR = 23;
        protected static final int EVENT_INDEX_EVENT_COLOR_KEY = 24;

    public static final String[] REMINDERS_PROJECTION = new String[] {
            Reminders._ID, // 0
            Reminders.MINUTES, // 1
            Reminders.METHOD, // 2
    };
    public static final int REMINDERS_INDEX_MINUTES = 1;
    public static final int REMINDERS_INDEX_METHOD = 2;
    public static final String REMINDERS_WHERE = Reminders.EVENT_ID + "=?";

    // Visible for testing
    static final String ATTENDEES_DELETE_PREFIX = Attendees.EVENT_ID + "=? AND "
            + Attendees.ATTENDEE_EMAIL + " IN (";

    public static final int DOES_NOT_REPEAT = 0;
    public static final int REPEATS_DAILY = 1;
    public static final int REPEATS_EVERY_WEEKDAY = 2;
    public static final int REPEATS_WEEKLY_ON_DAY = 3;
    public static final int REPEATS_MONTHLY_ON_DAY_COUNT = 4;
    public static final int REPEATS_MONTHLY_ON_DAY = 5;
    public static final int REPEATS_YEARLY = 6;
    public static final int REPEATS_CUSTOM = 7;

    protected static final int MODIFY_UNINITIALIZED = 0;
    protected static final int MODIFY_SELECTED = 1;
    protected static final int MODIFY_ALL_FOLLOWING = 2;
    protected static final int MODIFY_ALL = 3;


    protected static final int DAY_IN_SECONDS = 24 * 60 * 60;

    private final AsyncQueryService mService;

    // This allows us to flag the event if something is wrong with it, right now
    // if an uri is provided for an event that doesn't exist in the db.
    // db에 존재하지 않는 이벤트에 대한 uri가 제공된 경우
    // 이벤트 플래그를 재정의함
    protected boolean mEventOk = true;

    public static final int ATTENDEE_ID_NONE = -1;
    public static final int[] ATTENDEE_VALUES = {
            Attendees.ATTENDEE_STATUS_NONE,
            Attendees.ATTENDEE_STATUS_ACCEPTED,
            Attendees.ATTENDEE_STATUS_TENTATIVE,
            Attendees.ATTENDEE_STATUS_DECLINED,
    };

    /**
     * This is the symbolic name for the key used to pass in the boolean for
     * creating all-day events that is part of the extra data of the intent.
     * This is used only for creating new events and is set to true if the
     * default for the new event should be an all-day event.
     *
     * intent의 추가 데이터의 일부인, 하루 종일 진행되는 이벤트를 생성하기 위해 boolean으로 전달하는 키의 이름.
     * 이는 새 이벤트를 생성하는 데에 사용되며, 새 이벤트의 기본값이 하루 종일 진행되는 이벤트여야 하는 경우 true로 설정함
     */
    public static final String EVENT_ALL_DAY = "allDay";

    static final String[] CALENDARS_PROJECTION = new String[] {
            Calendars._ID, // 0
            Calendars.CALENDAR_DISPLAY_NAME, // 1
            Calendars.OWNER_ACCOUNT, // 2
            Calendars.CALENDAR_COLOR, // 3
            Calendars.CAN_ORGANIZER_RESPOND, // 4
            Calendars.CALENDAR_ACCESS_LEVEL, // 5
            Calendars.VISIBLE, // 6
            Calendars.MAX_REMINDERS, // 7
            Calendars.ALLOWED_REMINDERS, // 8
            Calendars.ALLOWED_ATTENDEE_TYPES, // 9
            Calendars.ALLOWED_AVAILABILITY, // 10
            Calendars.ACCOUNT_NAME, // 11
            Calendars.ACCOUNT_TYPE, //12
    };
    static final int CALENDARS_INDEX_ID = 0;
    static final int CALENDARS_INDEX_DISPLAY_NAME = 1;
    static final int CALENDARS_INDEX_OWNER_ACCOUNT = 2;
    static final int CALENDARS_INDEX_COLOR = 3;
    static final int CALENDARS_INDEX_CAN_ORGANIZER_RESPOND = 4;
    static final int CALENDARS_INDEX_ACCESS_LEVEL = 5;
    static final int CALENDARS_INDEX_VISIBLE = 6;
    static final int CALENDARS_INDEX_MAX_REMINDERS = 7;
    static final int CALENDARS_INDEX_ALLOWED_REMINDERS = 8;
    static final int CALENDARS_INDEX_ALLOWED_ATTENDEE_TYPES = 9;
    static final int CALENDARS_INDEX_ALLOWED_AVAILABILITY = 10;
    static final int CALENDARS_INDEX_ACCOUNT_NAME = 11;
    static final int CALENDARS_INDEX_ACCOUNT_TYPE = 12;

    static final String CALENDARS_WHERE_WRITEABLE_VISIBLE = Calendars.CALENDAR_ACCESS_LEVEL + ">="
            + Calendars.CAL_ACCESS_CONTRIBUTOR + " AND " + Calendars.VISIBLE + "=1";

    static final String CALENDARS_WHERE = Calendars._ID + "=?";

    static final String[] COLORS_PROJECTION = new String[] {
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

    static final String[] ATTENDEES_PROJECTION = new String[] {
            Attendees._ID, // 0
            Attendees.ATTENDEE_NAME, // 1
            Attendees.ATTENDEE_EMAIL, // 2
            Attendees.ATTENDEE_RELATIONSHIP, // 3
            Attendees.ATTENDEE_STATUS, // 4
    };
    static final int ATTENDEES_INDEX_ID = 0;
    static final int ATTENDEES_INDEX_NAME = 1;
    static final int ATTENDEES_INDEX_EMAIL = 2;
    static final int ATTENDEES_INDEX_RELATIONSHIP = 3;
    static final int ATTENDEES_INDEX_STATUS = 4;
    static final String ATTENDEES_WHERE = Attendees.EVENT_ID + "=? AND attendeeEmail IS NOT NULL";

    public static class AttendeeItem {
        public boolean mRemoved;
        public Attendee mAttendee;
        public Drawable mBadge;
        public int mUpdateCounts;
        public View mView;
        public Uri mContactLookupUri;

        public AttendeeItem(Attendee attendee, Drawable badge) {
            mAttendee = attendee;
            mBadge = badge;
        }
    }

    public EditEventHelper(Context context) {
        mService = ((AbstractCalendarActivity)context).getAsyncQueryService();
    }

    public EditEventHelper(Context context, CalendarEventModel model) {
        this(context);
        // TODO: Remove unnecessary constructor.
    }

    /**
     * Saves the event. Returns true if the event was successfully saved, false
     * otherwise.
     *
     * @param model The event model to save
     * @param originalModel A model of the original event if it exists
     * @param modifyWhich For recurring events which type of series modification to use
     * @return true if the event was successfully queued for saving
     *
     * 이벤트를 저장. 이벤트가 성공적으로 저장되면 true 반환, 그렇지 않으면 false 반환
     *
     * model -> 저장할 이벤트 모델
     * originalModel -> 본래 이벤트의 모델 (존재하는 경우)
     * modifyWhich -> 사용할 수정 시리즈의 타입인 반복 이벤트 ...?
     */
    public boolean saveEvent(CalendarEventModel model, CalendarEventModel originalModel,
                             int modifyWhich) {
        boolean forceSaveReminders = false;

        if (DEBUG) {
            Log.d(TAG, "Saving event model: " + model);
        }

        if (!mEventOk) {
            if (DEBUG) {
                Log.w(TAG, "Event no longer exists. Event was not saved.");
            }
            return false;
        }

        // It's a problem if we try to save a non-existent or invalid model or if we're
        // modifying an existing event and we have the wrong original model
        // 존재하지 않거나 잘못된 모델을 저장하려고 하거나
        // 기존 이벤트를 수정했는데 잘못된 원래 모델이 존재하는 경우 문제가 발생함
        if (model == null) {
            Log.e(TAG, "Attempted to save null model.");
            return false;
        }
        if (!model.isValid()) {
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
        int eventIdIndex = -1;

        ContentValues values = getContentValuesFromModel(model);

        if (model.mUri != null && originalModel == null) {
            Log.e(TAG, "Existing event but no originalModel provided. Aborting save.");
            return false;
        }
        Uri uri = null;
        if (model.mUri != null) {
            uri = Uri.parse(model.mUri);
        }

        // Update the "hasAlarm" field for the event
        // 이벤트에 대한 "hasAlarm" 필드 업데이트
        ArrayList<ReminderEntry> reminders = model.mReminders;
        int len = reminders.size();
        values.put(Events.HAS_ALARM, (len > 0) ? 1 : 0);

        if (uri == null) {
            // Add hasAttendeeData for a new event
            // 새 이벤트에 대한 hasAttendeeData 추가
            values.put(Events.HAS_ATTENDEE_DATA, 1);
            values.put(Events.STATUS, Events.STATUS_CONFIRMED);
            eventIdIndex = ops.size();
            ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(
                    Events.CONTENT_URI).withValues(values);
            ops.add(b.build());
            forceSaveReminders = true;

        } else if (TextUtils.isEmpty(model.mRrule) && TextUtils.isEmpty(originalModel.mRrule)) {
            // Simple update to a non-recurring event
            // 반복되지 않는 이벤트에 대한 간단한 업데이트
            checkTimeDependentFields(originalModel, model, values, modifyWhich);
            ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());

        } else if (TextUtils.isEmpty(originalModel.mRrule)) {
            // This event was changed from a non-repeating event to a
            // repeating event.
            // 이 이벤트는 반복되지 않는 이벤트에서 반복 이벤트로 변경됨
            ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());

        } else if (modifyWhich == MODIFY_SELECTED) {
            // Modify contents of the current instance of repeating event
            // Create a recurrence exception
            // 현재 반복 이벤트 인스턴스의 내용 수정
            // 반복 예외 생성
            long begin = model.mOriginalStart;
            values.put(Events.ORIGINAL_SYNC_ID, originalModel.mSyncId);
            values.put(Events.ORIGINAL_INSTANCE_TIME, begin);
            boolean allDay = originalModel.mAllDay;
            values.put(Events.ORIGINAL_ALL_DAY, allDay ? 1 : 0);
            values.put(Events.STATUS, originalModel.mEventStatus);

            eventIdIndex = ops.size();
            ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(
                    Events.CONTENT_URI).withValues(values);
            ops.add(b.build());
            forceSaveReminders = true;

        } else if (modifyWhich == MODIFY_ALL_FOLLOWING) {

            if (TextUtils.isEmpty(model.mRrule)) {
                // We've changed a recurring event to a non-recurring event.
                // If the event we are editing is the first in the series,
                // then delete the whole series. Otherwise, update the series
                // to end at the new start time.
                // 반복 이벤트를 반복되지 않는 이벤트로 변경한 경우
                // 편집 중인 이벤트가 그 시리즈의 첫번째 이벤트인 경우 전체 시리즈를 삭제
                // 그렇지 않으면 새 시작 시간에 종료되도록 그 시리즈를 업데이트함
                if (isFirstEventInSeries(model, originalModel)) {
                    ops.add(ContentProviderOperation.newDelete(uri).build());
                } else {
                    // Update the current repeating event to end at the new start time.  We
                    // ignore the RRULE returned because the exception event doesn't want one.
                    // 새 시작 시간에 종료되도록 현재 반복 이벤트를 업데이트
                    // 예외 이벤트는 RRULE를 원하지 않기 때문에 RRULE 무시
                    updatePastEvents(ops, originalModel, model.mOriginalStart);
                }
                eventIdIndex = ops.size();
                values.put(Events.STATUS, originalModel.mEventStatus);
                ops.add(ContentProviderOperation.newInsert(Events.CONTENT_URI).withValues(values)
                        .build());
            } else {
                if (isFirstEventInSeries(model, originalModel)) {
                    checkTimeDependentFields(originalModel, model, values, modifyWhich);
                    ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(uri)
                            .withValues(values);
                    ops.add(b.build());
                } else {
                    // We need to update the existing recurrence to end before the exception
                    // event starts.  If the recurrence rule has a COUNT, we need to adjust
                    // that in the original and in the exception.  This call rewrites the
                    // original event's recurrence rule (in "ops"), and returns a new rule
                    // for the exception.  If the exception explicitly set a new rule, however,
                    // we don't want to overwrite it.
                    // 예외 이벤트가 시작되기 전에 기존 반복을 종료하도록 업데이트 해야 함
                    // 만약 반복 규정에 COUNT가 있다면 그것을 조정할 필요가 있음
                    // 이 호출은 원래 이벤트의 반복 규칙 (in "ops")을 다시 쓰고 예외에 대한 새 규칙을 반환함
                    // 그러나 예외가 명시적으로 새로운 규칙을 설정한 경우, 우리는 이를 덮어쓰기를 원하지 않음
                    String newRrule = updatePastEvents(ops, originalModel, model.mOriginalStart);
                    if (model.mRrule.equals(originalModel.mRrule)) {
                        values.put(Events.RRULE, newRrule);
                    }

                    // Create a new event with the user-modified fields
                    // 사용자 수정 필드를 사용하여 새 이벤트 생성
                    eventIdIndex = ops.size();
                    values.put(Events.STATUS, originalModel.mEventStatus);
                    ops.add(ContentProviderOperation.newInsert(Events.CONTENT_URI).withValues(
                            values).build());
                }
            }
            forceSaveReminders = true;

        } else if (modifyWhich == MODIFY_ALL) {

            // Modify all instances of repeating event
            // 이벤트의 모든 인스턴스 수정
            if (TextUtils.isEmpty(model.mRrule)) {
                // We've changed a recurring event to a non-recurring event.
                // Delete the whole series and replace it with a new
                // non-recurring event.
                // 반복 이벤트를 반복되지 않는 이벤트로 변경한 경우
                // 전체 시리즈를 삭제하고 반복하지 않는 새 이벤트로 교체하기
                ops.add(ContentProviderOperation.newDelete(uri).build());

                eventIdIndex = ops.size();
                ops.add(ContentProviderOperation.newInsert(Events.CONTENT_URI).withValues(values)
                        .build());
                forceSaveReminders = true;
            } else {
                checkTimeDependentFields(originalModel, model, values, modifyWhich);
                ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
            }
        }

        // New Event or New Exception to an existing event
        // 기존 이벤트에 대한 새 이벤트 또는 새 예외
        boolean newEvent = (eventIdIndex != -1);
        ArrayList<ReminderEntry> originalReminders;
        if (originalModel != null) {
            originalReminders = originalModel.mReminders;
        } else {
            originalReminders = new ArrayList<ReminderEntry>();
        }

        if (newEvent) {
            saveRemindersWithBackRef(ops, eventIdIndex, reminders, originalReminders,
                    forceSaveReminders);
        } else if (uri != null) {
            long eventId = ContentUris.parseId(uri);
            saveReminders(ops, eventId, reminders, originalReminders, forceSaveReminders);
        }

        ContentProviderOperation.Builder b;
        boolean hasAttendeeData = model.mHasAttendeeData;

        if (hasAttendeeData && model.mOwnerAttendeeId == -1) {
            // Organizer is not an attendee
            // 주최자가 참석자가 아님 ..?

            String ownerEmail = model.mOwnerAccount;
            if (model.mAttendeesList.size() != 0 && Utils.isValidEmail(ownerEmail)) {
                // Add organizer as attendee since we got some attendees
                // 참석자를 몇 명 확보했으므로 주최자를 참석자로 추가 ..?

                values.clear();
                values.put(Attendees.ATTENDEE_EMAIL, ownerEmail);
                values.put(Attendees.ATTENDEE_RELATIONSHIP, Attendees.RELATIONSHIP_ORGANIZER);
                values.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_REQUIRED);
                values.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_ACCEPTED);

                if (newEvent) {
                    b = ContentProviderOperation.newInsert(Attendees.CONTENT_URI)
                            .withValues(values);
                    b.withValueBackReference(Attendees.EVENT_ID, eventIdIndex);
                } else {
                    values.put(Attendees.EVENT_ID, model.mId);
                    b = ContentProviderOperation.newInsert(Attendees.CONTENT_URI)
                            .withValues(values);
                }
                ops.add(b.build());
            }
        } else if (hasAttendeeData &&
                model.mSelfAttendeeStatus != originalModel.mSelfAttendeeStatus &&
                model.mOwnerAttendeeId != -1) {
            if (DEBUG) {
                Log.d(TAG, "Setting attendee status to " + model.mSelfAttendeeStatus);
            }
            Uri attUri = ContentUris.withAppendedId(Attendees.CONTENT_URI, model.mOwnerAttendeeId);

            values.clear();
            values.put(Attendees.ATTENDEE_STATUS, model.mSelfAttendeeStatus);
            values.put(Attendees.EVENT_ID, model.mId);
            b = ContentProviderOperation.newUpdate(attUri).withValues(values);
            ops.add(b.build());
        }

        // TODO: is this the right test? this currently checks if this is
        // a new event or an existing event. or is this a paranoia check?
        if (hasAttendeeData && (newEvent || uri != null)) {
            String attendees = model.getAttendeesString();
            String originalAttendeesString;
            if (originalModel != null) {
                originalAttendeesString = originalModel.getAttendeesString();
            } else {
                originalAttendeesString = "";
            }
            // Hit the content provider only if this is a new event or the user
            // has changed it
            // 이 이벤트가 새 이벤트이거나 사용자가 변경한 경우에만 컨텐츠 공급자 기록
            if (newEvent || !TextUtils.equals(originalAttendeesString, attendees)) {
                // figure out which attendees need to be added and which ones
                // need to be deleted. use a linked hash set, so we maintain
                // order (but also remove duplicates).
                // 어떤 참석자를 추가하고 어떤 참석자를 삭제해야 하는지 파악
                // 링크된 해시셋을 사용하므로 순서를 유지 (중복도 제거)
                HashMap<String, Attendee> newAttendees = model.mAttendeesList;
                LinkedList<String> removedAttendees = new LinkedList<String>();

                // the eventId is only used if eventIdIndex is -1.
                // eventId는 eventIndex가 -1인 경우에만 사용됨
                // TODO: clean up this code.
                long eventId = uri != null ? ContentUris.parseId(uri) : -1;

                // only compute deltas if this is an existing event.
                // new events (being inserted into the Events table) won't
                // have any existing attendees.
                // 기존 이벤트인 경우에만 델타 계산 (compute deltas)
                // 새로운 이벤트(이벤트 테이블에 삽입됨)에는 기존 참석자가 없음
                if (!newEvent) {
                    removedAttendees.clear();
                    HashMap<String, Attendee> originalAttendees = originalModel.mAttendeesList;
                    for (String originalEmail : originalAttendees.keySet()) {
                        if (newAttendees.containsKey(originalEmail)) {
                            // existing attendee. remove from new attendees set.
                            // 기존 참석자. 새 참석자 셋으로부터 제거
                            newAttendees.remove(originalEmail);
                        } else {
                            // no longer in attendees. mark as removed.
                            // 더 이상 참석자가 없음. 제거된 것으로 표시
                            removedAttendees.add(originalEmail);
                        }
                    }

                    // delete removed attendees if necessary
                    // 필요한 경우 제거된 참석자 삭제
                    if (removedAttendees.size() > 0) {
                        b = ContentProviderOperation.newDelete(Attendees.CONTENT_URI);

                        String[] args = new String[removedAttendees.size() + 1];
                        args[0] = Long.toString(eventId);
                        int i = 1;
                        StringBuilder deleteWhere = new StringBuilder(ATTENDEES_DELETE_PREFIX);
                        for (String removedAttendee : removedAttendees) {
                            if (i > 1) {
                                deleteWhere.append(",");
                            }
                            deleteWhere.append("?");
                            args[i++] = removedAttendee;
                        }
                        deleteWhere.append(")");
                        b.withSelection(deleteWhere.toString(), args);
                        ops.add(b.build());
                    }
                }

                if (newAttendees.size() > 0) {
                    // Insert the new attendees
                    // 새 참석자 삽입
                    for (Attendee attendee : newAttendees.values()) {
                        values.clear();
                        values.put(Attendees.ATTENDEE_NAME, attendee.mName);
                        values.put(Attendees.ATTENDEE_EMAIL, attendee.mEmail);
                        values.put(Attendees.ATTENDEE_RELATIONSHIP,
                                Attendees.RELATIONSHIP_ATTENDEE);
                        values.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_REQUIRED);
                        values.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_NONE);

                        if (newEvent) {
                            b = ContentProviderOperation.newInsert(Attendees.CONTENT_URI)
                                    .withValues(values);
                            b.withValueBackReference(Attendees.EVENT_ID, eventIdIndex);
                        } else {
                            values.put(Attendees.EVENT_ID, eventId);
                            b = ContentProviderOperation.newInsert(Attendees.CONTENT_URI)
                                    .withValues(values);
                        }
                        ops.add(b.build());
                    }
                }
            }
        }


        mService.startBatch(mService.getNextToken(), null, android.provider.CalendarContract.AUTHORITY, ops,
                Utils.UNDO_DELAY);

        return true;
    }

    public static LinkedHashSet<Rfc822Token> getAddressesFromList(String list, Rfc822Validator validator) {
        LinkedHashSet<Rfc822Token> addresses = new LinkedHashSet<Rfc822Token>();
        Rfc822Tokenizer.tokenize(list, addresses);
        if (validator == null) {
            return addresses;
        }

        // validate the emails, out of paranoia. they should already be
        // validated on input, but drop any invalid emails just to be safe.
        // out of paranoia..? 편집증에서 나온..? 이메일 확인하기
        // 그것들은 이미 입력에 대해 확인되어야 하지만 단지 안전하기 위해 잘못된 이메일을 삭제..?
        Iterator<Rfc822Token> addressIterator = addresses.iterator();
        while (addressIterator.hasNext()) {
            Rfc822Token address = addressIterator.next();
            if (!validator.isValid(address.getAddress())) {
                Log.v(TAG, "Dropping invalid attendee email address: " + address.getAddress());
                addressIterator.remove();
            }
        }
        return addresses;
    }

    /**
     * When we aren't given an explicit start time, we default to the next
     * upcoming half hour. So, for example, 5:01 -> 5:30, 5:30 -> 6:00, etc.
     *
     * @return a UTC time in milliseconds representing the next upcoming half
     * hour
     *
     * 명시적인 시작 시간이 주어지지 않으면 다음 30분 이내로 기본 설정됨
     * 예를 들어 5:01 -> 5:30, 5:30 -> 6:00 등
     *
     * return -> 다음 30분을 나타내는 UTC 시간 (밀리초)
     */
    protected long constructDefaultStartTime(long now) {
        Time defaultStart = new Time();
        defaultStart.set(now);
        defaultStart.second = 0;
        defaultStart.minute = 30;
        long defaultStartMillis = defaultStart.toMillis(false);
        if (now < defaultStartMillis) {
            return defaultStartMillis;
        } else {
            return defaultStartMillis + 30 * DateUtils.MINUTE_IN_MILLIS;
        }
    }

    /**
     * When we aren't given an explicit end time, we calculate according to user preference.
     * @param startTime the start time
     * @param context a {@link Context} with which to look up user preference
     * @return a default end time
     *
     * 명시적인 종료 시간이 주어지지 않은 경우 사용자 선호도에 따라 계산
     * startTime -> 시작 시간
     * context -> 사용자 선호도를 검색하는 {@link Context}
     * return -> 기본 종료 시간
     */
    protected long constructDefaultEndTime(long startTime, Context context) {
        return startTime + Utils.getDefaultEventDurationInMillis(context);
    }

    // TODO think about how useful this is. Probably check if our event has
    // changed early on and either update all or nothing. Should still do the if
    // MODIFY_ALL bit.
    void checkTimeDependentFields(CalendarEventModel originalModel, CalendarEventModel model,
                                  ContentValues values, int modifyWhich) {
        long oldBegin = model.mOriginalStart;
        long oldEnd = model.mOriginalEnd;
        boolean oldAllDay = originalModel.mAllDay;
        String oldRrule = originalModel.mRrule;
        String oldTimezone = originalModel.mTimezone;

        long newBegin = model.mStart;
        long newEnd = model.mEnd;
        boolean newAllDay = model.mAllDay;
        String newRrule = model.mRrule;
        String newTimezone = model.mTimezone;

        // If none of the time-dependent fields changed, then remove them.
        // 시간에 따라 달라지는 필드가 없으면 제거함
        if (oldBegin == newBegin && oldEnd == newEnd && oldAllDay == newAllDay
                && TextUtils.equals(oldRrule, newRrule)
                && TextUtils.equals(oldTimezone, newTimezone)) {
            values.remove(Events.DTSTART);
            values.remove(Events.DTEND);
            values.remove(Events.DURATION);
            values.remove(Events.ALL_DAY);
            values.remove(Events.RRULE);
            values.remove(Events.EVENT_TIMEZONE);
            return;
        }

        if (TextUtils.isEmpty(oldRrule) || TextUtils.isEmpty(newRrule)) {
            return;
        }

        // If we are modifying all events then we need to set DTSTART to the
        // start time of the first event in the series, not the current
        // date and time. If the start time of the event was changed
        // (from, say, 3pm to 4pm), then we want to add the time difference
        // to the start time of the first event in the series (the DTSTART
        // value). If we are modifying one instance or all following instances,
        // then we leave the DTSTART field alone.
        // 모든 이벤트를 수정하는 경우 현재 날짜와 시간이 아닌 그 시리즈에서늬 첫번째 이벤트의 시작 시간으로 DTSTART 설정
        // 이벤트의 시작 시간(예: 오후 3시에서 오후 4시로)이 변경되었다면,
        // 시리즈에서 첫번째 이벤트의 시작 시간(DTSTART 값)에 시간 차이를 추가하고 함
        // 인스턴스 하나 또는 다음 인스턴스를 모두 수정하는 경우 DTSTART 필드를 그대로 둠
        if (modifyWhich == MODIFY_ALL) {
            long oldStartMillis = originalModel.mStart;
            if (oldBegin != newBegin) {
                // The user changed the start time of this event
                // 사용자가 이벤트의 시작 시간을 변경
                long offset = newBegin - oldBegin;
                oldStartMillis += offset;
            }
            if (newAllDay) {
                Time time = new Time(Time.TIMEZONE_UTC);
                time.set(oldStartMillis);
                time.hour = 0;
                time.minute = 0;
                time.second = 0;
                oldStartMillis = time.toMillis(false);
            }
            values.put(Events.DTSTART, oldStartMillis);
        }
    }


    /**
     * Prepares an update to the original event so it stops where the new series
     * begins. When we update 'this and all following' events we need to change
     * the original event to end before a new series starts. This creates an
     * update to the old event's rrule to do that.
     *<p>
     * If the event's recurrence rule has a COUNT, we also need to reduce the count in the
     * RRULE for the exception event.
     *
     * @param ops The list of operations to add the update to
     * @param originalModel The original event that we're updating
     * @param endTimeMillis The time before which the event must end (i.e. the start time of the
     *        exception event instance).
     * @return A replacement exception recurrence rule.
     *
     * 원래 이벤트에 대한 업데이트를 준비하여 새 시리즈가 시작되는 곳에서 중지함
     * 'this and all follwing' 이벤트를 업데이트할 때 새 시리즈가 시작되기 전에 원래 이벤트를 종료하도록 변경해야 함
     * 이는 그것을 하기 위해 이전 이벤트의 rrule을 업데이트함
     *
     * 이벤트의 반복 규칙에 COUNT가 있는 경우, 예외 이벤트에 대한 RRULE의 count도 줄여야 함
     *
     * ops -> 업데이트를 추가할 작업 리스트
     * originalModel -> 업데이트할 원래 이벤트
     * endTimeMillis -> 이벤트가 종료되는 시간 (예외 이벤트 인스턴스의 시작 시간)
     * return -> 예외 반복 규칙 대체
     */
    public String updatePastEvents(ArrayList<ContentProviderOperation> ops,
                                   CalendarEventModel originalModel, long endTimeMillis) {
        boolean origAllDay = originalModel.mAllDay;
        String origRrule = originalModel.mRrule;
        String newRrule = origRrule;

        EventRecurrence origRecurrence = new EventRecurrence();
        origRecurrence.parse(origRrule);

        // Get the start time of the first instance in the original recurrence.
        // 원래 반복의 첫번째 인스턴스의 시작 시간 구하기
        long startTimeMillis = originalModel.mStart;
        Time dtstart = new Time();
        dtstart.timezone = originalModel.mTimezone;
        dtstart.set(startTimeMillis);

        ContentValues updateValues = new ContentValues();

        if (origRecurrence.count > 0) {
            /*
             * Generate the full set of instances for this recurrence, from the first to the
             * one just before endTimeMillis.  The list should never be empty, because this method
             * should not be called for the first instance.  All we're really interested in is
             * the *number* of instances found.
             *
             * TODO: the model assumes RRULE and ignores RDATE, EXRULE, and EXDATE.  For the
             * current environment this is reasonable, but that may not hold in the future.
             *
             * TODO: if COUNT is 1, should we convert the event to non-recurring?  e.g. we
             * do an "edit this and all future events" on the 2nd instances.
             *
             * endTimeMillis 바로 앞의 첫번째 인스턴스에서 이 반복에 대한 전체 인스턴스 셋을 생성
             * 이 메소드는 첫번째 인스턴스에 대해 호출되어서는 안되므로 리스트가 비어있으면 안됨
             * 정말로 관심 있는 것은 발견된 인스턴스의 수..?
             *
             * 모델은 RRULE를 가정하고 RDATE, EXRULE 및 EDATE를 무시함. 현재의 환경에서는 이것이 합리적이지만
             * 이는 미래에 유지되지 않을 수 있음
             *
             * COUNT가 1인 경우 이벤트를 반복되지 않게 해야하는가? 예를 들어 두번째 인스턴스에 대해
             * "이 이벤트와 모든 향후 이벤트 편집"을 수행해야 하나
             */
            RecurrenceSet recurSet = new RecurrenceSet(originalModel.mRrule, null, null, null);
            RecurrenceProcessor recurProc = new RecurrenceProcessor();
            long[] recurrences;
            try {
                recurrences = recurProc.expand(dtstart, recurSet, startTimeMillis, endTimeMillis);
            } catch (DateException de) {
                throw new RuntimeException(de);
            }

            if (recurrences.length == 0) {
                throw new RuntimeException("can't use this method on first instance");
            }

            EventRecurrence excepRecurrence = new EventRecurrence();
            excepRecurrence.parse(origRrule);  // TODO: add+use a copy constructor instead
            excepRecurrence.count -= recurrences.length;
            newRrule = excepRecurrence.toString();

            origRecurrence.count = recurrences.length;

        } else {
            // The "until" time must be in UTC time in order for Google calendar
            // to display it properly. For all-day events, the "until" time string
            // must include just the date field, and not the time field. The
            // repeating events repeat up to and including the "until" time.
            // 구글 달력이 제대로 표시되려면 "until" 시간이 UTV 시간 내에 있어야 함
            // 하루 종일 진행되는 이벤트의 경우 "until" 시간 문자열에는 시간 필드가 아니라 날짜 필드만 포함
            // 반복 이벤트에는 "until" 시간까지 반복됨
            Time untilTime = new Time();
            untilTime.timezone = Time.TIMEZONE_UTC;

            // Subtract one second from the old begin time to get the new
            // "until" time.
            // 새로운 "until" 시간을 얻기 위해 이전 시작 시간에서 1초를 뺌
            untilTime.set(endTimeMillis - 1000); // subtract one second (1000 millis)
            if (origAllDay) {
                untilTime.hour = 0;
                untilTime.minute = 0;
                untilTime.second = 0;
                untilTime.allDay = true;
                untilTime.normalize(false);

                // This should no longer be necessary -- DTSTART should already be in the correct
                // format for an all-day event.
                // 더 이상 필요하지 않아야 함
                // -- DTSTART는 이미 하루 종일 진행되는 이벤트에 올바른 형식이어야 함
                dtstart.hour = 0;
                dtstart.minute = 0;
                dtstart.second = 0;
                dtstart.allDay = true;
                dtstart.timezone = Time.TIMEZONE_UTC;
            }
            origRecurrence.until = untilTime.format2445();
        }

        updateValues.put(Events.RRULE, origRecurrence.toString());
        updateValues.put(Events.DTSTART, dtstart.normalize(true));
        ContentProviderOperation.Builder b =
                ContentProviderOperation.newUpdate(Uri.parse(originalModel.mUri))
                        .withValues(updateValues);
        ops.add(b.build());

        return newRrule;
    }

    /**
     * Compares two models to ensure that they refer to the same event. This is
     * a safety check to make sure an updated event model refers to the same
     * event as the original model. If the original model is null then this is a
     * new event or we're forcing an overwrite so we return true in that case.
     * The important identifiers are the Calendar Id and the Event Id.
     *
     * @return
     *
     * 두 모델을 비교하여 동일한 이벤트를 참조하는지 확인
     * 이는 업데이트된 이벤트 모델이 원래 모델인 같은 이벤트를 참조하는지 확인하기 위한 안전 점검
     * 만약 원래 모델이 null이라면 이는 새로운 이벤트이거나 오버라이딩을 강요하고 있는 것. 그래서 그 경우에는 true 반환
     * 중요한 식별자는 캘린더 ID와 이벤트 ID
     */
    public static boolean isSameEvent(CalendarEventModel model, CalendarEventModel originalModel) {
        if (originalModel == null) {
            return true;
        }

        if (model.mCalendarId != originalModel.mCalendarId) {
            return false;
        }
        if (model.mId != originalModel.mId) {
            return false;
        }

        return true;
    }

    /**
     * Saves the reminders, if they changed. Returns true if operations to
     * update the database were added.
     *
     * @param ops the array of ContentProviderOperations
     * @param eventId the id of the event whose reminders are being updated
     * @param reminders the array of reminders set by the user
     * @param originalReminders the original array of reminders
     * @param forceSave if true, then save the reminders even if they didn't change
     * @return true if operations to update the database were added
     *
     * 변경된 경우, 리마인더를 저장.
     * 데이터베이스를 업데이트하는 작업이 추가된 경우 true 반환
     *
     * ops -> ContentProviderOperations 배열
     * eventId -> 알림 메시지가 업데이트되는 이벤트의 ID
     * reminders -> 사용자가 설정한 알림의 배열
     * originalReminders -> 리마인더의 원래 배열
     * forceSave -> 만약 true일 경우 변경되지 않았더라도 미리 알림 저장
     * return -> 데이터베이스 업데이트 작업이 추가된 경우 true
     */
    public static boolean saveReminders(ArrayList<ContentProviderOperation> ops, long eventId,
                                        ArrayList<ReminderEntry> reminders, ArrayList<ReminderEntry> originalReminders,
                                        boolean forceSave) {
        // If the reminders have not changed, then don't update the database
        if (reminders.equals(originalReminders) && !forceSave) {
            return false;
        }

        // Delete all the existing reminders for this event
        // 이 이벤트에 대한 기존 알림 삭제
        String where = Reminders.EVENT_ID + "=?";
        String[] args = new String[] {Long.toString(eventId)};
        ContentProviderOperation.Builder b = ContentProviderOperation
                .newDelete(Reminders.CONTENT_URI);
        b.withSelection(where, args);
        ops.add(b.build());

        ContentValues values = new ContentValues();
        int len = reminders.size();

        // Insert the new reminders, if any
        // 새 알림 삽입 (있는 경우)
        for (int i = 0; i < len; i++) {
            ReminderEntry re = reminders.get(i);

            values.clear();
            values.put(Reminders.MINUTES, re.getMinutes());
            values.put(Reminders.METHOD, re.getMethod());
            values.put(Reminders.EVENT_ID, eventId);
            b = ContentProviderOperation.newInsert(Reminders.CONTENT_URI).withValues(values);
            ops.add(b.build());
        }
        return true;
    }



    /**
     * Saves the reminders, if they changed. Returns true if operations to
     * update the database were added. Uses a reference id since an id isn't
     * created until the row is added.
     *
     * @param ops the array of ContentProviderOperations
     * @param eventIdIndex the id of the event whose reminders are being updated
     * @param reminders the array of reminders set by the user
     * @param originalReminders the original array of reminders
     * @param forceSave if true, then save the reminders even if they didn't change
     * @return true if operations to update the database were added
     *
     * 변경된 경우, 리마인더를 저장
     * 데이터베이스를 업데이트하는 작업이 추가된 경우 true 반환
     * 행이 추가될 때까지 ID가 생성되지 않으므로 참조 ID 사용
     *
     * ops -> ContentProviderOperations 배열
     * eventId -> 알림 메시지가 업데이트되는 이벤트의 ID
     * reminderMinutes -> 사용자가 설정한 알림의 배열
     * originalReminders -> 리마인더의 원래 배열
     * forceSave -> 만약 true일 경우 변경되지 않았더라도 미리 알림 저장
     * return -> 데이터베이스 업데이트 작업이 추가된 경우 true
     */
    public static boolean saveRemindersWithBackRef(ArrayList<ContentProviderOperation> ops,
                                                   int eventIdIndex, ArrayList<ReminderEntry> reminders,
                                                   ArrayList<ReminderEntry> originalReminders, boolean forceSave) {
        // If the reminders have not changed, then don't update the database
        // 알림 메시지가 변경되지 않은 경우 데이터베이스를 업데이트하지 않음
        if (reminders.equals(originalReminders) && !forceSave) {
            return false;
        }

        // Delete all the existing reminders for this event
        // 이 이벤트에 대한 기존 알림 삭제
        ContentProviderOperation.Builder b = ContentProviderOperation
                .newDelete(Reminders.CONTENT_URI);
        b.withSelection(Reminders.EVENT_ID + "=?", new String[1]);
        b.withSelectionBackReference(0, eventIdIndex);
        ops.add(b.build());

        ContentValues values = new ContentValues();
        int len = reminders.size();

        // Insert the new reminders, if any
        // 새 알림 삽입 (있는 경우)
        for (int i = 0; i < len; i++) {
            ReminderEntry re = reminders.get(i);

            values.clear();
            values.put(Reminders.MINUTES, re.getMinutes());
            values.put(Reminders.METHOD, re.getMethod());
            b = ContentProviderOperation.newInsert(Reminders.CONTENT_URI).withValues(values);
            b.withValueBackReference(Reminders.EVENT_ID, eventIdIndex);
            ops.add(b.build());
        }
        return true;
    }


    // It's the first event in the series if the start time before being
    // modified is the same as the original event's start time
    // 수정 전 시작 시간이 원래 이벤트의 시작 시간과 같을 경우 그 시리즈의 첫번째 이벤트임
    static boolean isFirstEventInSeries(CalendarEventModel model,
                                        CalendarEventModel originalModel) {
        return model.mOriginalStart == originalModel.mStart;
    }



    // Adds an rRule and duration to a set of content values
    // content 값 집합에 rRULE 및 기간 추가
    void addRecurrenceRule(ContentValues values, CalendarEventModel model) {
        String rrule = model.mRrule;

        values.put(Events.RRULE, rrule);
        long end = model.mEnd;
        long start = model.mStart;
        String duration = model.mDuration;

        boolean isAllDay = model.mAllDay;
        if (end >= start) {
            if (isAllDay) {
                // if it's all day compute the duration in days
                // 하루 종일 지속 시간(일) 계산
                long days = (end - start + DateUtils.DAY_IN_MILLIS - 1)
                        / DateUtils.DAY_IN_MILLIS;
                duration = "P" + days + "D";
            } else {
                // otherwise compute the duration in seconds
                // 그렇지 않은 경우 지속 시간(초) 계산
                long seconds = (end - start) / DateUtils.SECOND_IN_MILLIS;
                duration = "P" + seconds + "S";
            }
        } else if (TextUtils.isEmpty(duration)) {

            // If no good duration info exists assume the default
            // 양호한 지속 시간 정보가 없는 경우 기본값을 가정
            if (isAllDay) {
                duration = "P1D";
            } else {
                duration = "P3600S";
            }
        }
        // recurring events should have a duration and dtend set to null
        // 반복 이벤트의 지속 시간 및 dtend가 null로 설정되어야 함
        values.put(Events.DURATION, duration);
        values.put(Events.DTEND, (Long) null);
    }


    /**
     * Uses the recurrence selection and the model data to build an rrule and
     * write it to the model.
     *
     * @param selection the type of rrule
     * @param model The event to update
     * @param weekStart the week start day, specified as java.util.Calendar
     * constants
     *
     * 반복 선택 및 모델 데이터를 사용하여 규칙 작성 및 모델에 기록
     *
     * selection -> rrule의 타입
     * model -> 업데이트할 이벤트
     * weekStart -> java.util.Calendar 상수로 지정된 한 주의 시작일
     */
    static void updateRecurrenceRule(int selection, CalendarEventModel model,
                                     int weekStart) {
        // Make sure we don't have any leftover data from the previous setting
        // 이전 설정에서 남은 데이터가 없는지 확인
        EventRecurrence eventRecurrence = new EventRecurrence();

        if (selection == DOES_NOT_REPEAT) {
            model.mRrule = null;
            return;
        } else if (selection == REPEATS_CUSTOM) {
            // Keep custom recurrence as before.
            // 이전과 같이 사용자 정의 반복 유지
            return;
        } else if (selection == REPEATS_DAILY) {
            eventRecurrence.freq = EventRecurrence.DAILY;
        } else if (selection == REPEATS_EVERY_WEEKDAY) {
            eventRecurrence.freq = EventRecurrence.WEEKLY;
            int dayCount = 5;
            int[] byday = new int[dayCount];
            int[] bydayNum = new int[dayCount];

            byday[0] = EventRecurrence.MO;
            byday[1] = EventRecurrence.TU;
            byday[2] = EventRecurrence.WE;
            byday[3] = EventRecurrence.TH;
            byday[4] = EventRecurrence.FR;
            for (int day = 0; day < dayCount; day++) {
                bydayNum[day] = 0;
            }

            eventRecurrence.byday = byday;
            eventRecurrence.bydayNum = bydayNum;
            eventRecurrence.bydayCount = dayCount;
        } else if (selection == REPEATS_WEEKLY_ON_DAY) {
            eventRecurrence.freq = EventRecurrence.WEEKLY;
            int[] days = new int[1];
            int dayCount = 1;
            int[] dayNum = new int[dayCount];
            Time startTime = new Time(model.mTimezone);
            startTime.set(model.mStart);

            days[0] = EventRecurrence.timeDay2Day(startTime.weekDay);
            // not sure why this needs to be zero, but set it for now.
            dayNum[0] = 0;

            eventRecurrence.byday = days;
            eventRecurrence.bydayNum = dayNum;
            eventRecurrence.bydayCount = dayCount;
        } else if (selection == REPEATS_MONTHLY_ON_DAY) {
            eventRecurrence.freq = EventRecurrence.MONTHLY;
            eventRecurrence.bydayCount = 0;
            eventRecurrence.bymonthdayCount = 1;
            int[] bymonthday = new int[1];
            Time startTime = new Time(model.mTimezone);
            startTime.set(model.mStart);
            bymonthday[0] = startTime.monthDay;
            eventRecurrence.bymonthday = bymonthday;
        } else if (selection == REPEATS_MONTHLY_ON_DAY_COUNT) {
            eventRecurrence.freq = EventRecurrence.MONTHLY;
            eventRecurrence.bydayCount = 1;
            eventRecurrence.bymonthdayCount = 0;

            int[] byday = new int[1];
            int[] bydayNum = new int[1];
            Time startTime = new Time(model.mTimezone);
            startTime.set(model.mStart);
            // Compute the week number (for example, the "2nd" Monday)
            // 주 번호 계산 (예: "2번째" 월요일)
            int dayCount = 1 + ((startTime.monthDay - 1) / 7);
            if (dayCount == 5) {
                dayCount = -1;
            }
            bydayNum[0] = dayCount;
            byday[0] = EventRecurrence.timeDay2Day(startTime.weekDay);
            eventRecurrence.byday = byday;
            eventRecurrence.bydayNum = bydayNum;
        } else if (selection == REPEATS_YEARLY) {
            eventRecurrence.freq = EventRecurrence.YEARLY;
        }

        // Set the week start day.
        // 주 시작일을 설정
        eventRecurrence.wkst = EventRecurrence.calendarDay2Day(weekStart);
        model.mRrule = eventRecurrence.toString();
    }


    /**
     * Uses an event cursor to fill in the given model This method assumes the
     * cursor used {@link #EVENT_PROJECTION} as it's query projection. It uses
     * the cursor to fill in the given model with all the information available.
     *
     * @param model The model to fill in
     * @param cursor An event cursor that used {@link #EVENT_PROJECTION} for the query
     *
     * 지정된 모델을 채우기 위해 이벤트 커서 사용
     * 이 메소드는 {@link #EVENT_PROJECTION}을 쿼리 예상으로 사용하는 커서를 가정함
     * 그것은 커서를 사용하여 주어진 모델을 이용할 수 있는 모든 정보로 채움
     */
    public static void setModelFromCursor(CalendarEventModel model, Cursor cursor) {
        if (model == null || cursor == null || cursor.getCount() != 1) {
            Log.wtf(TAG, "Attempted to build non-existent model or from an incorrect query.");
            return;
        }

        model.clear();
        cursor.moveToFirst();

        model.mId = cursor.getInt(EVENT_INDEX_ID);
        model.mTitle = cursor.getString(EVENT_INDEX_TITLE);
        model.mDescription = cursor.getString(EVENT_INDEX_DESCRIPTION);
        model.mLocation = cursor.getString(EVENT_INDEX_EVENT_LOCATION);
        model.mAllDay = cursor.getInt(EVENT_INDEX_ALL_DAY) != 0;
        model.mHasAlarm = cursor.getInt(EVENT_INDEX_HAS_ALARM) != 0;
        model.mCalendarId = cursor.getInt(EVENT_INDEX_CALENDAR_ID);
        model.mStart = cursor.getLong(EVENT_INDEX_DTSTART);
        String tz = cursor.getString(EVENT_INDEX_TIMEZONE);
        if (TextUtils.isEmpty(tz)) {
            Log.w(TAG, "Query did not return a timezone for the event.");
            model.mTimezone = TimeZone.getDefault().getID();
        } else {
            model.mTimezone = tz;
        }
        String rRule = cursor.getString(EVENT_INDEX_RRULE);
        model.mRrule = rRule;
        model.mSyncId = cursor.getString(EVENT_INDEX_SYNC_ID);
        model.mAvailability = cursor.getInt(EVENT_INDEX_AVAILABILITY);
        int accessLevel = cursor.getInt(EVENT_INDEX_ACCESS_LEVEL);
        model.mOwnerAccount = cursor.getString(EVENT_INDEX_OWNER_ACCOUNT);
        model.mHasAttendeeData = cursor.getInt(EVENT_INDEX_HAS_ATTENDEE_DATA) != 0;
        model.mOriginalSyncId = cursor.getString(EVENT_INDEX_ORIGINAL_SYNC_ID);
        model.mOriginalId = cursor.getLong(EVENT_INDEX_ORIGINAL_ID);
        model.mOrganizer = cursor.getString(EVENT_INDEX_ORGANIZER);
        model.mIsOrganizer = model.mOwnerAccount.equalsIgnoreCase(model.mOrganizer);
        model.mGuestsCanModify = cursor.getInt(EVENT_INDEX_GUESTS_CAN_MODIFY) != 0;

        int rawEventColor;
        if (cursor.isNull(EVENT_INDEX_EVENT_COLOR)) {
            rawEventColor = cursor.getInt(EVENT_INDEX_CALENDAR_COLOR);
        } else {
            rawEventColor = cursor.getInt(EVENT_INDEX_EVENT_COLOR);
        }
        model.setEventColor(Utils.getDisplayColorFromColor(rawEventColor));

        model.mAccessLevel = accessLevel;
        model.mEventStatus = cursor.getInt(EVENT_INDEX_EVENT_STATUS);

        boolean hasRRule = !TextUtils.isEmpty(rRule);

        // We expect only one of these, so ignore the other
        // 이 중 하나만 기대되므로 나머지는 무시
        if (hasRRule) {
            model.mDuration = cursor.getString(EVENT_INDEX_DURATION);
        } else {
            model.mEnd = cursor.getLong(EVENT_INDEX_DTEND);
        }

        model.mModelUpdatedWithEventCursor = true;
    }


    /**
     * Uses a calendar cursor to fill in the given model This method assumes the
     * cursor used {@link #CALENDARS_PROJECTION} as it's query projection. It uses
     * the cursor to fill in the given model with all the information available.
     *
     * @param model The model to fill in
     * @param cursor An event cursor that used {@link #CALENDARS_PROJECTION} for the query
     * @return returns true if model was updated with the info in the cursor.
     *
     * 지정된 모델을 작성하기 위해 캘린더 커서 사용
     * 이 메소드는 쿼리 예상으로 사용되는 {@link #CALENDARS_PROJECTION} 커서를 가정
     * 그것은 커서를 사용하여 주어진 모델을 이용할 수 있는 모든 정보로 채움
     *
     * model -> 채울 모델
     * cursor -> 쿼리에 {@link #CALENDARS_PROJECTION}을 사용한 이벤트 커서
     * return -> 커서의 정보로 모델이 업데이트된 경우 true
     */
    public static boolean setModelFromCalendarCursor(CalendarEventModel model, Cursor cursor) {
        if (model == null || cursor == null) {
            Log.wtf(TAG, "Attempted to build non-existent model or from an incorrect query.");
            return false;
        }

        if (model.mCalendarId == -1) {
            return false;
        }

        if (!model.mModelUpdatedWithEventCursor) {
            Log.wtf(TAG,
                    "Can't update model with a Calendar cursor until it has seen an Event cursor.");
            return false;
        }

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            if (model.mCalendarId != cursor.getInt(CALENDARS_INDEX_ID)) {
                continue;
            }

            model.mOrganizerCanRespond = cursor.getInt(CALENDARS_INDEX_CAN_ORGANIZER_RESPOND) != 0;

            model.mCalendarAccessLevel = cursor.getInt(CALENDARS_INDEX_ACCESS_LEVEL);
            model.mCalendarDisplayName = cursor.getString(CALENDARS_INDEX_DISPLAY_NAME);
            model.setCalendarColor(Utils.getDisplayColorFromColor(
                    cursor.getInt(CALENDARS_INDEX_COLOR)));

            model.mCalendarAccountName = cursor.getString(CALENDARS_INDEX_ACCOUNT_NAME);
            model.mCalendarAccountType = cursor.getString(CALENDARS_INDEX_ACCOUNT_TYPE);

            model.mCalendarMaxReminders = cursor.getInt(CALENDARS_INDEX_MAX_REMINDERS);
            model.mCalendarAllowedReminders = cursor.getString(CALENDARS_INDEX_ALLOWED_REMINDERS);
            model.mCalendarAllowedAttendeeTypes = cursor
                    .getString(CALENDARS_INDEX_ALLOWED_ATTENDEE_TYPES);
            model.mCalendarAllowedAvailability = cursor
                    .getString(CALENDARS_INDEX_ALLOWED_AVAILABILITY);

            return true;
        }
        return false;
    }


    public static boolean canModifyEvent(CalendarEventModel model) {
        return canModifyCalendar(model)
                && (model.mIsOrganizer || model.mGuestsCanModify);
    }

    public static boolean canModifyCalendar(CalendarEventModel model) {
        return model.mCalendarAccessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR
                || model.mCalendarId == -1;
    }

    public static boolean canAddReminders(CalendarEventModel model) {
        return model.mCalendarAccessLevel >= Calendars.CAL_ACCESS_READ;
    }

    public static boolean canRespond(CalendarEventModel model) {
        // For non-organizers, write permission to the calendar is sufficient.
        // For organizers, the user needs a) write permission to the calendar
        // AND b) ownerCanRespond == true AND c) attendee data exist
        // (this means num of attendees > 1, the calendar owner's and others).
        // Note that mAttendeeList omits the organizer.

        // (there are more cases involved to be 100% accurate, such as
        // paying attention to whether or not an attendee status was
        // included in the feed, but we're currently omitting those corner cases
        // for simplicity).

        // 비조직의 경우 캘린더에 대한 쓰기 권한으로 충분
        // 주최자의 경우 사용자는 a) 캘린더 및 b) ownerCanRespond == true 또는 c) 참석자 데이터가 존재
        // (이는 참석자 수 > 1, 캘린더 소유자 등을 의미)
        // mAttendeeList는 주최자를 생략

        // (참석자 상태가 피드에 포함되었는지 여부를 주의를 기울이는 등 100% 정확해야하는 케이스가 더 있지만
        // 현재 단순성을 위해 그러한 코너 케이스를 생략하고 있음)

        if (!canModifyCalendar(model)) {
            return false;
        }

        if (!model.mIsOrganizer) {
            return true;
        }

        if (!model.mOrganizerCanRespond) {
            return false;
        }

        // This means we don't have the attendees data so we can't send
        // the list of attendees and the status back to the server
        // 참석자 데이터가 없어 참석자 명단과 상태를 다시 서버로 보낼 수 없다는 것을 의미
        if (model.mHasAttendeeData && model.mAttendeesList.size() == 0) {
            return false;
        }

        return true;
    }



    /**
     * Goes through an event model and fills in content values for saving. This
     * method will perform the initial collection of values from the model and
     * put them into a set of ContentValues. It performs some basic work such as
     * fixing the time on allDay events and choosing whether to use an rrule or
     * dtend.
     *
     * @param model The complete model of the event you want to save
     * @return values
     *
     * 이벤트 모델을 살펴보고 저장하기 위해 content value 입력
     * 이 메소드는 모델에서 초기 값의 수집을 수행하여 Contextalues 집합에 넣음
     * 그것은 하루 종일 진행되는 이벤트에 대한 시간을 고정하고 rule 또는 dtend를 사용할지 선택하는 것과 같은
     * 몇가지 기본적인 작업을 수행함
     *
     * model -> 저장할 이벤트의 전체 모델
     * return -> values
     */
    ContentValues getContentValuesFromModel(CalendarEventModel model) {
        String title = model.mTitle;
        boolean isAllDay = model.mAllDay;
        String rrule = model.mRrule;
        String timezone = model.mTimezone;
        if (timezone == null) {
            timezone = TimeZone.getDefault().getID();
        }
        Time startTime = new Time(timezone);
        Time endTime = new Time(timezone);

        startTime.set(model.mStart);
        endTime.set(model.mEnd);
        offsetStartTimeIfNecessary(startTime, endTime, rrule, model);

        ContentValues values = new ContentValues();

        long startMillis;
        long endMillis;
        long calendarId = model.mCalendarId;
        if (isAllDay) {
            // Reset start and end time, ensure at least 1 day duration, and set
            // the timezone to UTC, as required for all-day events.
            // 시작 및 종료 시간을 재설정하고, 최소 1일 이상의 지속 시간을 보장하며
            // 하루 종일 진행되는 이벤트에 필요한 시간대를 UTC로 설정하기
            timezone = Time.TIMEZONE_UTC;
            startTime.hour = 0;
            startTime.minute = 0;
            startTime.second = 0;
            startTime.timezone = timezone;
            startMillis = startTime.normalize(true);

            endTime.hour = 0;
            endTime.minute = 0;
            endTime.second = 0;
            endTime.timezone = timezone;
            endMillis = endTime.normalize(true);
            if (endMillis < startMillis + DateUtils.DAY_IN_MILLIS) {
                // EditEventView#fillModelFromUI() should treat this case, but we want to ensure
                // the condition anyway.
                endMillis = startMillis + DateUtils.DAY_IN_MILLIS;
            }
        } else {
            startMillis = startTime.toMillis(true);
            endMillis = endTime.toMillis(true);
        }

        values.put(Events.CALENDAR_ID, calendarId);
        values.put(Events.EVENT_TIMEZONE, timezone);
        values.put(Events.TITLE, title);
        values.put(Events.ALL_DAY, isAllDay ? 1 : 0);
        values.put(Events.DTSTART, startMillis);
        values.put(Events.RRULE, rrule);
        if (!TextUtils.isEmpty(rrule)) {
            addRecurrenceRule(values, model);
        } else {
            values.put(Events.DURATION, (String) null);
            values.put(Events.DTEND, endMillis);
        }
        if (model.mDescription != null) {
            values.put(Events.DESCRIPTION, model.mDescription.trim());
        } else {
            values.put(Events.DESCRIPTION, (String) null);
        }
        if (model.mLocation != null) {
            values.put(Events.EVENT_LOCATION, model.mLocation.trim());
        } else {
            values.put(Events.EVENT_LOCATION, (String) null);
        }
        values.put(Events.AVAILABILITY, model.mAvailability);
        values.put(Events.HAS_ATTENDEE_DATA, model.mHasAttendeeData ? 1 : 0);

        int accessLevel = model.mAccessLevel;
        values.put(Events.ACCESS_LEVEL, accessLevel);
        values.put(Events.STATUS, model.mEventStatus);
        if (model.isEventColorInitialized()) {
            if (model.getEventColor() == model.getCalendarColor()) {
                values.put(Events.EVENT_COLOR_KEY, NO_EVENT_COLOR);
            } else {
                values.put(Events.EVENT_COLOR_KEY, model.getEventColorKey());
            }
        }
        return values;
    }


    /**
     * If the recurrence rule is such that the event start date doesn't actually fall in one of the
     * recurrences, then push the start date up to the first actual instance of the event.
     *
     * 이벤트 시작 날짜가 실제로 반복 중 하나에 해당되지 않는 경우
     * 시작 날짜를 이벤트의 실제 첫번째 인스턴스로 push
     */
    private void offsetStartTimeIfNecessary(Time startTime, Time endTime, String rrule,
                                            CalendarEventModel model) {
        if (rrule == null || rrule.isEmpty()) {
            // No need to waste any time with the parsing if the rule is empty.
            // rule이 비어 있는 경우 파싱으로 시간을 낭비할 필요가 없음
            return;
        }

        mEventRecurrence.parse(rrule);
        // Check if we meet the specific special case. It has to:
        //  * be weekly
        //  * not recur on the same day of the week that the startTime falls on
        // In this case, we'll need to push the start time to fall on the first day of the week
        // that is part of the recurrence.
        // 특정 특수 케이스를 충족하는지 확인
        // * 매주
        // * 시작 시간이 경과한 요일에 반복되지 않음
        // 이 경우 반복의 일부인 주의 첫번째 날에 시작 시간을 push해야 할 것
        if (mEventRecurrence.freq != EventRecurrence.WEEKLY) {
            // Not weekly so nothing to worry about.
            return;
        }
        if (mEventRecurrence.byday == null ||
                mEventRecurrence.byday.length > mEventRecurrence.bydayCount) {
            // This shouldn't happen, but just in case something is weird about the recurrence.
            // 반복에 이상한 일이 생길 경우를 대비
            return;
        }

        // Start to figure out what the nearest weekday is.
        // 가장 가까운 평일이 무엇인지 알아내기 시작
        int closestWeekday = Integer.MAX_VALUE;
        int weekstart = EventRecurrence.day2TimeDay(mEventRecurrence.wkst);
        int startDay = startTime.weekDay;
        for (int i = 0; i < mEventRecurrence.bydayCount; i++) {
            int day = EventRecurrence.day2TimeDay(mEventRecurrence.byday[i]);
            if (day == startDay) {
                // Our start day is one of the recurring days, so we're good.
                return;
            }

            if (day < weekstart) {
                // Let's not make any assumptions about what weekstart can be.
                day += 7;
            }
            // We either want the earliest day that is later in the week than startDay ...
            // 시작일보다 더 빠른 요일을 원하거나...
            if (day > startDay && (day < closestWeekday || closestWeekday < startDay)) {
                closestWeekday = day;
            }
            // ... or if there are no days later than startDay, we want the earliest day that is
            // earlier in the week than startDay.
            // ... 또는 startDay보다 며칠 늦은 시간이 없는 경우, startDay보다 이른 요일을 원함
            if (closestWeekday == Integer.MAX_VALUE || closestWeekday < startDay) {
                // We haven't found a day that's later in the week than startDay yet.
                if (day < closestWeekday) {
                    closestWeekday = day;
                }
            }
        }

        // We're here, so unfortunately our event's start day is not included in the days of
        // the week of the recurrence. To save this event correctly we'll need to push the start
        // date to the closest weekday that *is* part of the recurrence.
        // 여기 왔기 때문에 이벤트 시작일은 반복한 요일에 포함되지 않음
        // 이 이벤트를 올바르게 저장하려면 시작 날짜를 *반복*의 일부인 가장 가까운 평일로 미룰 필요가 있음
        if (closestWeekday < startDay) {
            closestWeekday += 7;
        }
        int daysOffset = closestWeekday - startDay;
        startTime.monthDay += daysOffset;
        endTime.monthDay += daysOffset;
        long newStartTime = startTime.normalize(true);
        long newEndTime = endTime.normalize(true);

        // Later we'll actually be using the values from the model rather than the startTime
        // and endTime themselves, so we need to make these changes to the model as well.
        // 나중에 실제로 startTime과 endTime 자체보다는 모델에서 얻은 값을 사용할 것이므로
        // 모델에서도 이러한 값을 변경해야 함
        model.mStart = newStartTime;
        model.mEnd = newEndTime;
    }


    /**
     * Takes an e-mail address and returns the domain (everything after the last @)
     * 이메일 주소를 가져오고 도메인을 반환 (마지막 @ 이후 모든 항목)
     */
    public static String extractDomain(String email) {
        int separator = email.lastIndexOf('@');
        if (separator != -1 && ++separator < email.length()) {
            return email.substring(separator);
        }
        return null;
    }

    public interface EditDoneRunnable extends Runnable {
        public void setDoneCode(int code);
    }

}
