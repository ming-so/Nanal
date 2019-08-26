package com.android.nanal;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.Time;
import android.view.View;

import com.android.nanal.calendar.CalendarController;

public class TodayView extends View {
    private Time mCurrentTime;

    Time mBaseDate;

    private final String mCreateNewEventString, mNewEventHintString;

    protected Context mContext;
    protected final Resources mResources;

    public TodayView(Context context, CalendarController controller) {
        super(context);
        mContext = context;
        mResources = context.getResources();

        mCreateNewEventString = mResources.getString(R.string.event_create);
        mNewEventHintString = mResources.getString(R.string.day_view_new_event_hint);
    }

}
