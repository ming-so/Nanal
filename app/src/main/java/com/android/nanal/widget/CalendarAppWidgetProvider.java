package com.android.nanal.widget;

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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.nanal.R;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.activity.EventInfoActivity;
import com.android.nanal.event.Utils;

import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;

/**
 * Simple widget to show next upcoming calendar event.
 * 다가오는 달력 이벤트를 보여 주는 간단한 위젯
 */
public class CalendarAppWidgetProvider extends AppWidgetProvider {
    static final String TAG = "CalendarAppWidgetProvider";
    static final boolean LOGD = false;

    // TODO Move these to Calendar.java
    static final String EXTRA_EVENT_IDS = "com.android.calendar.EXTRA_EVENT_IDS";

    /**
     * Build {@link ComponentName} describing this specific
     * {@link AppWidgetProvider}
     */
    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, CalendarAppWidgetProvider.class);
    }

    /**
     * Build the {@link PendingIntent} used to trigger an update of all calendar
     * widgets. Uses {@link Utils#getWidgetScheduledUpdateAction(Context)} to
     * directly target all widgets instead of using
     * {@link AppWidgetManager#EXTRA_APPWIDGET_IDS}.
     *
     * 모든 일정 위젯의 업데이트를 트리거하는 데 사용되는 PendingIntent
     * AppWidgetManager 대신에 모든 위젯에 직접 타겟팅하기 위해 getWidgetScheduledUpdateAction을 이용함
     *
     *
     * @param context Context to use when building broadcast.
     *                 브로드캐스트 작성 시 사용할 context
     */
    static PendingIntent getUpdateIntent(Context context) {
        Intent intent = new Intent(Utils.getWidgetScheduledUpdateAction(context));
        intent.setDataAndType(CalendarContract.CONTENT_URI, Utils.APPWIDGET_DATA_TYPE);
        return PendingIntent.getBroadcast(context, 0 /* no requestCode */, intent,
                0 /* no flags */);
    }

    /**
     * Build a {@link PendingIntent} to launch the Calendar app. This should be used
     * in combination with {@link RemoteViews#setPendingIntentTemplate(int, PendingIntent)}.
     * 캘린더 앱을 작동하기 위한 PedingIntent
     * Remote...Template와 함께 사용되어야 함
     */
    static PendingIntent getLaunchPendingIntentTemplate(Context context) {
        Intent launchIntent = new Intent();
        launchIntent.setAction(Intent.ACTION_VIEW);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        launchIntent.setClass(context, AllInOneActivity.class);
        return PendingIntent.getActivity(context, 0 /* no requestCode */, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Build an {@link Intent} available as FillInIntent to launch the Calendar app.
     * This should be used in combination with
     * {@link RemoteViews#setOnClickFillInIntent(int, Intent)}.
     * If the go to time is 0, then calendar will be launched without a starting time.
     *
     * 캘린더 앱을 실행하려면 FillInIntent로 사용할 수 있는 Intent 사용
     * RemoteViews와 함께 사용되어야 함
     * 시간이 0이라면 시작 시간 없이 달력이 실행됨
     *
     *
     * @param //goToTime time that calendar should take the user to, or 0 to
     *                 indicate no specific start time.
     */
    static Intent getLaunchFillInIntent(Context context, long id, long start, long end,
                                        boolean allDay) {
        final Intent fillInIntent = new Intent();
        String dataString = "content://com.android.calendar/events";
        if (id != 0) {
            fillInIntent.putExtra(Utils.INTENT_KEY_DETAIL_VIEW, true);
            fillInIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_TASK_ON_HOME);

            dataString += "/" + id;
            // If we have an event id - start the event info activity
            // 이벤트 ID가 있는 경우 이벤트 정보 활동 시작
            fillInIntent.setClass(context, EventInfoActivity.class);
        } else {
            // If we do not have an event id - start AllInOne
            // 이벤트 ID가 없는 경우 AllInOne 시작
            fillInIntent.setClass(context, AllInOneActivity.class);
        }
        Uri data = Uri.parse(dataString);
        fillInIntent.setData(data);
        fillInIntent.putExtra(EXTRA_EVENT_BEGIN_TIME, start);
        fillInIntent.putExtra(EXTRA_EVENT_END_TIME, end);
        fillInIntent.putExtra(EXTRA_EVENT_ALL_DAY, allDay);

        return fillInIntent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle calendar-specific updates ourselves because they might be
        // coming in without extras, which AppWidgetProvider then blocks.
        // AppWidgetProvider에서 차단하는 추가 기능 없이 일정 관리별 업데이트를 직접 처리함
        final String action = intent.getAction();
        if (LOGD)
            Log.d(TAG, "AppWidgetProvider got the intent: " + intent.toString());
        if (Utils.getWidgetUpdateAction(context).equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            performUpdate(context, appWidgetManager,
                    appWidgetManager.getAppWidgetIds(getComponentName(context)),
                    null /* no eventIds */);
        } else if (action.equals(Intent.ACTION_PROVIDER_CHANGED)
                || action.equals(Intent.ACTION_TIME_CHANGED)
                || action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                || action.equals(Intent.ACTION_DATE_CHANGED)
                || action.equals(Utils.getWidgetScheduledUpdateAction(context))) {
            Intent service = new Intent(context, CalendarAppWidgetService.class);
            context.startService(service);
        } else {
            super.onReceive(context, intent);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisabled(Context context) {
        // Unsubscribe from all AlarmManager updates
        // 모든 AlarmManager 업데이트로부터 주소를 삭제
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingUpdate = getUpdateIntent(context);
        am.cancel(pendingUpdate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        performUpdate(context, appWidgetManager, appWidgetIds, null /* no eventIds */);
    }

    /**
     * Process and push out an update for the given appWidgetIds. This call
     * actually fires an intent to start {@link CalendarAppWidgetService} as a
     * background service which handles the actual update, to prevent ANR'ing
     * during database queries.
     * 지정된 appWidgetIds에 대한 업데이트를 처리하고 push out함
     * 이 호출은 실제로 데이터베이스 쿼리 중에 ANR를 방지하기 위해 실제 업데이트를 처리하는 백그라운드 서비스로
     * CalendarAppWidgetService를 시작함
     *
     * @param context Context to use when starting {@link CalendarAppWidgetService}.
     *                 CalendarAppWidgetService를 시작할 때 사용하는 context
     * @param appWidgetIds List of specific appWidgetIds to update, or null for
     *            all.
     *                       업데이트할 특정 appWidgetIds 목록 또는 모든 항목에 대해 null
     * @param changedEventIds Specific events known to be changed. If present,
     *            we use it to decide if an update is necessary.
     *                        변경되는 걸로 알려진 특정 이벤트
     *                        만약 있다면, 업데이트가 필요한지 결정하기 위해 사용함
     */
    private void performUpdate(Context context,
                               AppWidgetManager appWidgetManager, int[] appWidgetIds,
                               long[] changedEventIds) {
        // Launch over to service so it can perform update
        for (int appWidgetId : appWidgetIds) {
            if (LOGD) Log.d(TAG, "Building widget update...");
            Intent updateIntent = new Intent(context, CalendarAppWidgetService.class);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            if (changedEventIds != null) {
                updateIntent.putExtra(EXTRA_EVENT_IDS, changedEventIds);
            }
            updateIntent.setData(Uri.parse(updateIntent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget);
            // Calendar header
            Time time = new Time(Utils.getTimeZone(context, null));
            time.setToNow();
            long millis = time.toMillis(true);
            final String dayOfWeek = DateUtils.getDayOfWeekString(time.weekDay + 1,
                    DateUtils.LENGTH_MEDIUM);
            final String date = Utils.formatDateRange(context, millis, millis,
                    DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_DATE
                            | DateUtils.FORMAT_NO_YEAR);
            views.setTextViewText(R.id.day_of_week, dayOfWeek);
            views.setTextViewText(R.id.date, date);
            // Attach to list of events, 이벤트 목록에 첨부
            views.setRemoteAdapter(appWidgetId, R.id.events_list, updateIntent);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.events_list);


            // Launch calendar app when the user taps on the header
            // 사용자가 헤더에서 탭할 때 캘린더 앱 실행
            final Intent launchCalendarIntent = new Intent(Intent.ACTION_VIEW);
            launchCalendarIntent.setClass(context, AllInOneActivity.class);
            launchCalendarIntent
                    .setData(Uri.parse("content://com.android.calendar/time/" + millis));
            final PendingIntent launchCalendarPendingIntent = PendingIntent.getActivity(
                    context, 0 /* no requestCode */, launchCalendarIntent, 0 /* no flags */);
            views.setOnClickPendingIntent(R.id.header, launchCalendarPendingIntent);

            // Each list item will call setOnClickExtra() to let the list know
            // which item
            // is selected by a user.
            // 각 리스트 아이템은 사용자가 선택한 항목을 리스트에 알리기 위해 setOnClickExtra() 호출
            final PendingIntent updateEventIntent = getLaunchPendingIntentTemplate(context);
            views.setPendingIntentTemplate(R.id.events_list, updateEventIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

//    private static PendingIntent getNewEventPendingIntent(Context context) {
//        Intent newEventIntent = new Intent(Intent.ACTION_EDIT);
//        newEventIntent.setClass(context, EditEventActivity.class);
//        Builder builder = CalendarContract.CONTENT_URI.buildUpon();
//        builder.appendPath("events");
//        newEventIntent.setData(builder.build());
//        return PendingIntent.getActivity(context, 0, newEventIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//    }
}

