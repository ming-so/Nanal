package com.android.nanal;

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

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.ViewSwitcher;
import android.widget.ViewSwitcher.ViewFactory;

import com.android.nanal.event.Event;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarController.EventInfo;
import com.android.nanal.calendar.CalendarController.EventType;
import com.android.nanal.event.EventLoader;
import com.android.nanal.event.Utils;


/**
 * This is the base class for Day and Week Activities.
 * Day와 Week 액티비티의 기본 클래스
 */
public class DayFragment extends Fragment implements CalendarController.EventHandler, ViewFactory {
    protected static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    /**
     * The view id used for all the views we create. It's OK to have all child
     * views have the same ID. This ID is used to pick which view receives
     * focus when a view hierarchy is saved / restore
     * 생성하는 모든 view에 사용되는 view id
     * 모든 자식에게 동일한 id를 부여하는 것 허용함
     * 이 id는 view 계층이 저장/복원될 때 포커스를 받는 view를 선택하는 데 사용됨
     */
    private static final int VIEW_ID = 1;
    protected ProgressBar mProgressBar;
    protected ViewSwitcher mViewSwitcher;
    protected Animation mInAnimationForward;
    protected Animation mOutAnimationForward;
    protected Animation mInAnimationBackward;
    protected Animation mOutAnimationBackward;
    EventLoader mEventLoader;

    Time mSelectedDay = new Time();

    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            if (!DayFragment.this.isAdded()) {
                return;
            }
            String tz = Utils.getTimeZone(getActivity(), mTZUpdater);
            mSelectedDay.timezone = tz;
            mSelectedDay.normalize(true);
        }
    };

    private int mNumDays;

    public DayFragment() {
        mSelectedDay.setToNow();
    }

    @SuppressLint("ValidFragment")
    public DayFragment(long timeMillis, int numOfDays) {
        mNumDays = numOfDays;
        if (timeMillis == 0) {
            mSelectedDay.setToNow();
        } else {
            mSelectedDay.set(timeMillis);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Context context = getActivity();

        mInAnimationForward = AnimationUtils.loadAnimation(context, R.anim.slide_left_in);
        mOutAnimationForward = AnimationUtils.loadAnimation(context, R.anim.slide_left_out);
        mInAnimationBackward = AnimationUtils.loadAnimation(context, R.anim.slide_right_in);
        mOutAnimationBackward = AnimationUtils.loadAnimation(context, R.anim.slide_right_out);

        mEventLoader = new EventLoader(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.day_activity, null);

        mViewSwitcher = (ViewSwitcher) v.findViewById(R.id.switcher);
        mViewSwitcher.setFactory(this);
        mViewSwitcher.getCurrentView().requestFocus();
        ((DayView) mViewSwitcher.getCurrentView()).updateTitle();

        return v;
    }

    public View makeView() {
        mTZUpdater.run();
        DayView view = new DayView(getActivity(), CalendarController
                .getInstance(getActivity()), mViewSwitcher, mEventLoader, mNumDays);
        view.setId(VIEW_ID);
        view.setLayoutParams(new ViewSwitcher.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        view.setSelected(mSelectedDay, false, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mEventLoader.startBackgroundThread();
        mTZUpdater.run();
        eventsChanged();
        DayView view = (DayView) mViewSwitcher.getCurrentView();
        view.handleOnResume();
        view.restartCurrentTimeUpdates();

        view = (DayView) mViewSwitcher.getNextView();
        view.handleOnResume();
        view.restartCurrentTimeUpdates();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        long time = getSelectedTimeInMillis();
        if (time != -1) {
            outState.putLong(BUNDLE_KEY_RESTORE_TIME, time);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        DayView view = (DayView) mViewSwitcher.getCurrentView();
        view.cleanup();
        view = (DayView) mViewSwitcher.getNextView();
        view.cleanup();
        mEventLoader.stopBackgroundThread();

        // Stop events cross-fade animation
        // 이벤트 크로스페이드 애니메이션 중지
        view.stopEventsAnimation();
        ((DayView) mViewSwitcher.getNextView()).stopEventsAnimation();
    }

    void startProgressSpinner() {
        // start the progress spinner
        // 프로그레스 스피너 시작
        mProgressBar.setVisibility(View.VISIBLE);
    }

    void stopProgressSpinner() {
        // stop the progress spinner
        // 프로그레스 스피너 멈춤
        mProgressBar.setVisibility(View.GONE);
    }

    private void goTo(Time goToTime, boolean ignoreTime, boolean animateToday) {
        if (mViewSwitcher == null) {
            // The view hasn't been set yet. Just save the time and use it later.
            // view가 set되지 않았음, 시간만 저장하고 나중에 사용함
            mSelectedDay.set(goToTime);
            return;
        }

        DayView currentView = (DayView) mViewSwitcher.getCurrentView();

        // How does goTo time compared to what's already displaying?
        // 표시된 시간과 비교했을 때 goTo는?
        int diff = currentView.compareToVisibleTimeRange(goToTime);

        if (diff == 0) {
            // In visible range. No need to switch view
            // 보이는 범위, view를 전환할 필요 없음
            currentView.setSelected(goToTime, ignoreTime, animateToday);
        } else {
            // Figure out which way to animate
            // 애니메이션을 만드는 방법 이해...?
            if (diff > 0) {
                mViewSwitcher.setInAnimation(mInAnimationForward);
                mViewSwitcher.setOutAnimation(mOutAnimationForward);
            } else {
                mViewSwitcher.setInAnimation(mInAnimationBackward);
                mViewSwitcher.setOutAnimation(mOutAnimationBackward);
            }

            DayView next = (DayView) mViewSwitcher.getNextView();
            if (ignoreTime) {
                next.setFirstVisibleHour(currentView.getFirstVisibleHour());
            }

            next.setSelected(goToTime, ignoreTime, animateToday);
            next.reloadEvents();
            mViewSwitcher.showNext();
            next.requestFocus();
            next.updateTitle();
            next.restartCurrentTimeUpdates();
        }
    }

    /**
     * Returns the selected time in milliseconds. The milliseconds are measured
     * in UTC milliseconds from the epoch and uniquely specifies any selectable
     * time.
     * 선택한 시간(밀리초 단위)을 반환함
     * 밀리초는 epoch로부터 UTC 밀리초로 측정되며, 선택 가능한 시간을 고유하게 지정함
     *
     * @return the selected time in milliseconds
     *          선택된 시간(밀리초)
     */
    public long getSelectedTimeInMillis() {
        if (mViewSwitcher == null) {
            return -1;
        }
        DayView view = (DayView) mViewSwitcher.getCurrentView();
        if (view == null) {
            return -1;
        }
        return view.getSelectedTimeInMillis();
    }

    public void eventsChanged() {
        if (mViewSwitcher == null) {
            return;
        }
        DayView view = (DayView) mViewSwitcher.getCurrentView();
        view.clearCachedEvents();
        view.reloadEvents();

        view = (DayView) mViewSwitcher.getNextView();
        view.clearCachedEvents();
    }

    Event getSelectedEvent() {
        DayView view = (DayView) mViewSwitcher.getCurrentView();
        return view.getSelectedEvent();
    }

    boolean isEventSelected() {
        DayView view = (DayView) mViewSwitcher.getCurrentView();
        return view.isEventSelected();
    }

    Event getNewEvent() {
        DayView view = (DayView) mViewSwitcher.getCurrentView();
        return view.getNewEvent();
    }

    public DayView getNextView() {
        return (DayView) mViewSwitcher.getNextView();
    }

    public long getSupportedEventTypes() {
        return EventType.GO_TO | EventType.EVENTS_CHANGED;
    }

    public void handleEvent(EventInfo msg) {
        if (msg.eventType == EventType.GO_TO) {
// TODO support a range of time
// TODO support event_id
// TODO support select message
            goTo(msg.selectedTime, (msg.extraLong & CalendarController.EXTRA_GOTO_DATE) != 0,
                    (msg.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0);
        } else if (msg.eventType == EventType.EVENTS_CHANGED) {
            eventsChanged();
        }
    }
}
