package com.android.nanal;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;

import java.util.TimeZone;

public class CreateNanalCalendar {
    static int[] colors = {
            0xffd50000, 0xffFF0000, 0xffff4444, 0xfff4511e,
            0xffFF7F00, 0xffff8800, 0xffFFFF00, 0xfff6bf26,
            0xff00FF00, 0xff0b8043, 0xff33b679, 0xff00FFFF,
            0xff33b5e5, 0xff039be5, 0xff007FFF, 0xff0000FF,
            0xff3f51b5, 0xff7F00FF, 0xff7986cb, 0xff8e24aa,
            0xffFF007F, 0xffFF00FF, 0xffe67c73, 0xff616161
    };

    public static Uri CreateCalendar(Context context, String name, String accountName) {
        Uri target = Uri.parse(CalendarContract.Calendars.CONTENT_URI.toString());

        ContentValues values = new ContentValues();
        values.put(Calendars.ACCOUNT_NAME, accountName);
        values.put(Calendars.ACCOUNT_TYPE, "com.google");
        values.put(Calendars.NAME, name);
        values.put(Calendars.CALENDAR_DISPLAY_NAME, name);
        values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
        values.put(Calendars.OWNER_ACCOUNT, accountName);
        values.put(Calendars.VISIBLE, 1);
        values.put(Calendars.SYNC_EVENTS, 1);
        values.put(Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().getID());
        values.put(Calendars.CAN_PARTIALLY_UPDATE, 1);
        values.put(Calendars.DIRTY, 1);
        values.put(Calendars.CALENDAR_COLOR, 0xff616161);

        target = target.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, "com.google").build();

        Uri newCalendar = context.getContentResolver().insert(target, values);

//        CalendarColorCache calendarColorCache = new CalendarColorCache(context, null);
//        eventColorCache.insertColor(accountName, "com.android.nanal", R.color.colorPrimary, "테스트1");
//        eventColorCache.insertColor(accountName, "com.android.nanal", R.color.colorRedPrimary, "테스트2");
        CreateCalendarColors(context, accountName, colors);
        CreateColors(context, accountName, colors);
        return newCalendar;
    }

    public static void CreateCalendarColors(Context context, String accountName, int[] colors) {
        Uri target = Uri.parse(CalendarContract.Colors.CONTENT_URI.toString());
        ContentValues values = new ContentValues();
        int i = 1;
        while (i <= colors.length) {
            values.put(CalendarContract.Colors.ACCOUNT_NAME, accountName);
            values.put(CalendarContract.Colors.ACCOUNT_TYPE, "com.google");
            values.put(CalendarContract.Colors.COLOR_KEY, Integer.toString(i));
            values.put(CalendarContract.Colors.COLOR_TYPE, CalendarContract.Colors.TYPE_CALENDAR);
            values.put(CalendarContract.Colors.COLOR, colors[i-1]);

            target = target.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Colors.ACCOUNT_NAME, accountName)
                    .appendQueryParameter(CalendarContract.Colors.ACCOUNT_TYPE, "com.google")
                    .build();

            context.getContentResolver().insert(target, values);
            i++;
        }
    }

    public static void CreateColors(Context context, String accountName, int[] colors) {
        Uri target = Uri.parse(CalendarContract.Colors.CONTENT_URI.toString());
        ContentValues values = new ContentValues();
        int i = 1;
        while (i <= colors.length) {
            values.put(CalendarContract.Colors.ACCOUNT_NAME, accountName);
            values.put(CalendarContract.Colors.ACCOUNT_TYPE, "com.google");
            values.put(CalendarContract.Colors.COLOR_KEY, Integer.toString(i));
            values.put(CalendarContract.Colors.COLOR_TYPE, CalendarContract.Colors.TYPE_EVENT);
            values.put(CalendarContract.Colors.COLOR, colors[i-1]);

            target = target.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Colors.ACCOUNT_NAME, accountName)
                    .appendQueryParameter(CalendarContract.Colors.ACCOUNT_TYPE, "com.google")
                    .build();

            context.getContentResolver().insert(target, values);
            i++;
        }
    }

    public static void DeleteColors(Context context, String accountName) {
        Uri target = Uri.parse(CalendarContract.Colors.CONTENT_URI.toString());
        target = target.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, "com.google").build();
        context.getContentResolver().delete(target, null, null);
    }

    public static void DeleteCalendar(Context context, String name, String accountName) {
        Uri target = Uri.parse(CalendarContract.Calendars.CONTENT_URI.toString());
        target = target.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, "com.google").build();

        context.getContentResolver().delete(target, null, null);
    }
}
