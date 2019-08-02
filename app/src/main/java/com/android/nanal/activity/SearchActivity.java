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

import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract.Events;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.nanal.CalendarRecentSuggestionsProvider;
import com.android.nanal.DynamicTheme;
import com.android.nanal.R;
import com.android.nanal.agenda.AgendaFragment;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarController.EventInfo;
import com.android.nanal.calendar.CalendarController.EventType;
import com.android.nanal.calendar.CalendarController.ViewType;
import com.android.nanal.event.DeleteEventHelper;
import com.android.nanal.event.EventInfoFragment;
import com.android.nanal.event.Utils;

import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;

public class SearchActivity extends AppCompatActivity implements CalendarController.EventHandler,
        SearchView.OnQueryTextListener, MenuItemCompat.OnActionExpandListener {


    protected static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    protected static final String BUNDLE_KEY_RESTORE_SEARCH_QUERY =
            "key_restore_search_query";
    private static final String TAG = SearchActivity.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final int HANDLER_KEY = 0;
    private static boolean mIsMultipane;
    // display event details to the side of the event list
    // 이벤트 세부 정보를 이벤트 리스트의 측면에 표시
    private boolean mShowEventDetailsWithAgenda;
    private CalendarController mController;
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            eventsChanged();
        }
    };
    private EventInfoFragment mEventInfoFragment;
    private long mCurrentEventId = -1;
    private String mQuery;
    private SearchView mSearchView;
    private DeleteEventHelper mDeleteEventHelper;
    private Handler mHandler;
    // runs when a timezone was changed and updates the today icon
    // 시간대가 변경되었을 때 실행되고, 오늘 아이콘 업데이트함
    private final Runnable mTimeChangesUpdater = new Runnable() {
        @Override
        public void run() {
            Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater,
                    Utils.getTimeZone(SearchActivity.this, mTimeChangesUpdater));
            SearchActivity.this.invalidateOptionsMenu();
        }
    };
    private BroadcastReceiver mTimeChangesReceiver;
    private ContentResolver mContentResolver;
    private final DynamicTheme dynamicTheme = new DynamicTheme();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        dynamicTheme.onCreate(this);
        // This needs to be created before setContentView
        // setContentView 전에 생성되어야 함
        mController = CalendarController.getInstance(this);
        mHandler = new Handler();

        mIsMultipane = Utils.getConfigBool(this, R.bool.multiple_pane_config);
        mShowEventDetailsWithAgenda =
                Utils.getConfigBool(this, R.bool.show_event_details_with_agenda);

        setContentView(R.layout.search);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        mContentResolver = getContentResolver();

        if (mIsMultipane) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME);
            }
        }

        // Must be the first to register because this activity can modify the
        // list of event handlers in it's handle method. This affects who the
        // rest of the handlers the controller dispatches to are.
        // 이 activity는 핸들 메소드에 있는 이벤트 핸들러의 리스트를 수정할 수 있기 때문에
        // 가장 먼저 등록되어야 함, 이것은 컨트롤러의 나머지 핸들러가 누구인지에 영향을 미침
        mController.registerEventHandler(HANDLER_KEY, this);

        mDeleteEventHelper = new DeleteEventHelper(this, this,
                false /* don't exit when done */);

        long millis = 0;
        if (icicle != null) {
            // Returns 0 if key not found
            // key를 못 찾았으면 0 반환
            millis = icicle.getLong(BUNDLE_KEY_RESTORE_TIME);
            if (DEBUG) {
                Log.v(TAG, "Restore value from icicle: " + millis);
            }
        }
        if (millis == 0) {
            // Didn't find a time in the bundle, look in intent or current time
            // 번들에서 시간을 찾지 못함, intent 또는 현재 시간을 확인함
            millis = Utils.timeFromIntentInMillis(getIntent());
        }

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query;
            if (icicle != null && icicle.containsKey(BUNDLE_KEY_RESTORE_SEARCH_QUERY)) {
                query = icicle.getString(BUNDLE_KEY_RESTORE_SEARCH_QUERY);
            } else {
                query = intent.getStringExtra(SearchManager.QUERY);
            }
            if ("TARDIS".equalsIgnoreCase(query)) {
                Utils.tardis();
            }
            initFragments(millis, query);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mController.deregisterAllEventHandlers();
        CalendarController.removeInstance(this);
    }

    private void initFragments(long timeMillis, String query) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        AgendaFragment searchResultsFragment = new AgendaFragment(timeMillis, true);
        ft.replace(R.id.search_results, searchResultsFragment);
        mController.registerEventHandler(R.id.search_results, searchResultsFragment);

        ft.commit();
        Time t = new Time();
        t.set(timeMillis);
        search(query, t);
    }

    private void showEventInfo(EventInfo event) {
        if (mShowEventDetailsWithAgenda) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();

            mEventInfoFragment = new EventInfoFragment(this, event.id,
                    event.startTime.toMillis(false), event.endTime.toMillis(false),
                    event.getResponse(), false, EventInfoFragment.DIALOG_WINDOW_STYLE,
                    null /* No reminders to explicitly pass in. */);
            ft.replace(R.id.agenda_event_info, mEventInfoFragment);
            ft.commit();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, event.id);
            intent.setData(eventUri);
            intent.setClass(this, EventInfoActivity.class);
            intent.putExtra(EXTRA_EVENT_BEGIN_TIME,
                    event.startTime != null ? event.startTime.toMillis(true) : -1);
            intent.putExtra(
                    EXTRA_EVENT_END_TIME, event.endTime != null ? event.endTime.toMillis(true) : -1);
            startActivity(intent);
        }
        mCurrentEventId = event.id;
    }

    private void search(String searchQuery, Time goToTime) {
        // save query in recent queries
        // 최근 쿼리에 쿼리를 저장함
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                Utils.getSearchAuthority(this),
                CalendarRecentSuggestionsProvider.MODE);
        suggestions.saveRecentQuery(searchQuery, null);


        EventInfo searchEventInfo = new EventInfo();
        searchEventInfo.eventType = EventType.SEARCH;
        searchEventInfo.query = searchQuery;
        searchEventInfo.viewType = ViewType.AGENDA;
        if (goToTime != null) {
            searchEventInfo.startTime = goToTime;
        }
        mController.sendEvent(this, searchEventInfo);
        mQuery = searchQuery;
        if (mSearchView != null) {
            mSearchView.setQuery(mQuery, false);
            mSearchView.clearFocus();
        }
    }

    private void deleteEvent(long eventId, long startMillis, long endMillis) {
        mDeleteEventHelper.delete(startMillis, endMillis, eventId, -1);
        if (mIsMultipane && mEventInfoFragment != null
                && eventId == mCurrentEventId) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.remove(mEventInfoFragment);
            ft.commit();
            mEventInfoFragment = null;
            mCurrentEventId = -1;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search_title_bar, menu);

        // replace the default top layer drawable of the today icon with a custom drawable
        // that shows the day of the month of today
        // 오늘 아이콘의 기본 탑 레이어 drawable을 오늘의 날짜?를 표시하는 커스텀 drawable로 교체함
        MenuItem menuItem = menu.findItem(R.id.action_today);
        LayerDrawable icon = (LayerDrawable) menuItem.getIcon();
        Utils.setTodayIcon(
                icon, this, Utils.getTimeZone(SearchActivity.this, mTimeChangesUpdater));

        MenuItem item = menu.findItem(R.id.action_search);

        MenuItemCompat.expandActionView(item);
        MenuItemCompat.setOnActionExpandListener(item, this);
        mSearchView = (SearchView) MenuItemCompat.getActionView(item);
        Utils.setUpSearchView(mSearchView, this);
        mSearchView.setQuery(mQuery, false);
        mSearchView.clearFocus();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Time t = null;
        final int itemId = item.getItemId();
        if (itemId == R.id.action_today) {
            t = new Time();
            t.setToNow();
            mController.sendEvent(this, EventType.GO_TO, t, null, -1, ViewType.CURRENT);
            return true;
        } else if (itemId == R.id.action_search) {
            return false;
        } else if (itemId == R.id.action_settings) {
            mController.sendEvent(this, EventType.LAUNCH_SETTINGS, null, null, 0, 0);
            return true;
        } else if (itemId == android.R.id.home) {
            Utils.returnToCalendarHome(this);
            return true;
        } else {
            return false;
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
        // onNewIntent(Intent)가 호출될 때 Activity가 다시 시작되지 않았으므로,
        // getIntent() 메소드는 여전히 onCreate()로 처음 받은 Intent를 반환한다는 점에 유의하기
        // 이것이 setIntent(Intent)를 onNewIntent(Intent) 내부에서 처리하는 이유임
        // (나중에 getIntent()를 호출할 경우에 대비)
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            search(query, null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(BUNDLE_KEY_RESTORE_TIME, mController.getTime());
        outState.putString(BUNDLE_KEY_RESTORE_SEARCH_QUERY, mQuery);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);

        Utils.setMidnightUpdater(
                mHandler, mTimeChangesUpdater, Utils.getTimeZone(this, mTimeChangesUpdater));
        // Make sure the today icon is up to date
        // 오늘 아이콘이 최신 상태인지 확인
        invalidateOptionsMenu();
        mTimeChangesReceiver = Utils.setTimeChangesReceiver(this, mTimeChangesUpdater);
        mContentResolver.registerContentObserver(Events.CONTENT_URI, true, mObserver);
        // We call this in case the user changed the time zone
        // 사용자가 표준 시간대를 변경한 경우 이를 호출함
        eventsChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.resetMidnightUpdater(mHandler, mTimeChangesUpdater);
        Utils.clearTimeChangesReceiver(this, mTimeChangesReceiver);
        mContentResolver.unregisterContentObserver(mObserver);
    }

    @Override
    public void eventsChanged() {
        mController.sendEvent(this, EventType.EVENTS_CHANGED, null, null, -1, ViewType.CURRENT);
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.VIEW_EVENT | EventType.DELETE_EVENT;
    }

    @Override
    public void handleEvent(EventInfo event) {
        long endTime = (event.endTime == null) ? -1 : event.endTime.toMillis(false);
        if (event.eventType == EventType.VIEW_EVENT) {
            showEventInfo(event);
        } else if (event.eventType == EventType.DELETE_EVENT) {
            deleteEvent(event.id, event.startTime.toMillis(false), endTime);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mQuery = query;
        mController.sendEvent(this, EventType.SEARCH, null, null, -1, ViewType.CURRENT, 0, query,
                getComponentName());
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        Utils.returnToCalendarHome(this);
        return false;
    }
}
