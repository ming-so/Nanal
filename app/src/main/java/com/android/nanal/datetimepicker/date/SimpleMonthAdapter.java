package com.android.nanal.datetimepicker.date;

import android.content.Context;

/**
 * An adapter for a list of {@link SimpleMonthView} items.
 * SimpleMonthView 리스트의 어댑터
 */
class SimpleMonthAdapter extends MonthAdapter {

    public SimpleMonthAdapter(Context context, DatePickerController controller) {
        super(context, controller);
    }

    @Override
    public MonthView createMonthView(Context context) {
        final MonthView monthView = new SimpleMonthView(context);
        monthView.setDatePickerController(mController);
        return monthView;
    }
}