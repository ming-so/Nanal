package com.android.nanal.activity;
/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.android.nanal.DynamicTheme;
import com.android.nanal.R;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarEventModel.ReminderEntry;
import com.android.nanal.event.EventInfoFragment;
import com.android.nanal.event.Utils;

import java.util.ArrayList;
import java.util.List;

import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;

public class EventInfoActivity extends AppCompatActivity {

    private static final String TAG = "EventInfoActivity";
    private EventInfoFragment mInfoFragment;
    // Create an observer so that we can update the views whenever a
    // Calendar event changes.
    // 캘린더 이벤트가 변경될 때마다 view를 업데이트할 수 있도록 관찰자(옵저버)를 생성함
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange) {
            if (selfChange) return;
            if (mInfoFragment != null) {
                mInfoFragment.reloadEvents();
            }
        }
    };
    private long mStartMillis, mEndMillis;
    private long mEventId;
    private final DynamicTheme dynamicTheme = new DynamicTheme();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        dynamicTheme.onCreate(this);

        // Get the info needed for the fragment
        // fragment에 필요한 정보 가져오기
        Intent intent = getIntent();
        int attendeeResponse = 0;
        mEventId = -1;
        boolean isDialog = false;
        ArrayList<ReminderEntry> reminders = null;

        if (icicle != null) {
            mEventId = icicle.getLong(EventInfoFragment.BUNDLE_KEY_EVENT_ID);
            mStartMillis = icicle.getLong(EventInfoFragment.BUNDLE_KEY_START_MILLIS);
            mEndMillis = icicle.getLong(EventInfoFragment.BUNDLE_KEY_END_MILLIS);
            attendeeResponse = icicle.getInt(EventInfoFragment.BUNDLE_KEY_ATTENDEE_RESPONSE);
            isDialog = icicle.getBoolean(EventInfoFragment.BUNDLE_KEY_IS_DIALOG);

            reminders = Utils.readRemindersFromBundle(icicle);
        } else if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            mStartMillis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, 0);
            mEndMillis = intent.getLongExtra(EXTRA_EVENT_END_TIME, 0);
            attendeeResponse = intent.getIntExtra(ATTENDEE_STATUS,
                    Attendees.ATTENDEE_STATUS_NONE);
            Uri data = intent.getData();
            if (data != null) {
                try {
                    List<String> pathSegments = data.getPathSegments();
                    int size = pathSegments.size();
                    if (size > 2 && "EventTime".equals(pathSegments.get(2))) {
                        // Support non-standard VIEW intent format: 비표준 VIEW intent 포맷 지원
                        //dat = content://com.android.calendar/events/[id]/EventTime/[start]/[end]
                        mEventId = Long.parseLong(pathSegments.get(1));
                        if (size > 4) {
                            mStartMillis = Long.parseLong(pathSegments.get(3));
                            mEndMillis = Long.parseLong(pathSegments.get(4));
                        }
                    } else {
                        mEventId = Long.parseLong(data.getLastPathSegment());
                    }
                } catch (NumberFormatException e) {
                    if (mEventId == -1) {
                        // do nothing here , deal with it later
                        // 아무것도 하지 않고, 나중에 처리함
                    } else if (mStartMillis == 0 || mEndMillis ==0) {
                        // Parsing failed on the start or end time , make sure the times were not
                        // pulled from the intent's extras and reset them.
                        // 시작 또는 종료 시간에 분석을 실패하면, 의도된 추가 시간에서 시간을 끌어내지
                        // 않았는지 확인하고 다시 설정함...?
                        mStartMillis = 0;
                        mEndMillis = 0;
                    }
                }
            }
        }

        if (mEventId == -1) {
            Log.w(TAG, "No event id");
            Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }

        // If we do not support showing full screen event info in this configuration,
        // close the activity and show the event in AllInOne.
        // 이 구성(configuration)에서 전체 화면 이벤트 정보를 표시할 수 없는 경우,
        // activity를 종료하고 AllInOne에 이벤트를 표시함
        Resources res = getResources();
        if (!res.getBoolean(R.bool.agenda_show_event_info_full_screen)
                && !res.getBoolean(R.bool.show_event_info_full_screen)) {
            CalendarController.getInstance(this)
                    .launchViewEvent(mEventId, mStartMillis, mEndMillis, attendeeResponse);
            finish();
            return;
        }

        setContentView(R.layout.simple_frame_layout);

        // Get the fragment if exists
        // 존재하는 경우 fragment 가져옴
        mInfoFragment = (EventInfoFragment)
                getFragmentManager().findFragmentById(R.id.main_frame);


        // Create a new fragment if none exists
        // 없는 경우 새 fragment 만듦
        if (mInfoFragment == null) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            mInfoFragment = new EventInfoFragment(this, mEventId, mStartMillis, mEndMillis,
                    attendeeResponse, isDialog, (isDialog ?
                    EventInfoFragment.DIALOG_WINDOW_STYLE :
                    EventInfoFragment.FULL_WINDOW_STYLE),
                    reminders);
            ft.replace(R.id.main_frame, mInfoFragment);
            ft.commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // From the Android Dev Guide: "It's important to note that when
        // onNewIntent(Intent) is called, the Activity has not been restarted,
        // so the getIntent() method will still return the Intent that was first
        // received with onCreate(). This is why setIntent(Intent) is called
        // inside onNewIntent(Intent) (just in case you call getIntent() at a
        // later time)."
        // onNewIntent(Intent)가 호출될 때 Activity가 다시 시작되지 않았기 때문에,
        // getIntent() 메소드는 여전히 onCreate()로 처음 수신된 Intent를 반환함에 주의
        // 이게 setIntent(Intent)를 onNewIntent(Intent)에서 호출하는 이유임
        // 혹시 모르니까 나중에 getIntent()함
        setIntent(intent);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        getContentResolver().registerContentObserver(CalendarContract.Events.CONTENT_URI,
                true, mObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
