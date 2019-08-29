package com.android.nanal.query;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DiaryDBHelper extends SQLiteOpenHelper {

    public DiaryDBHelper(Context context) {
        super(context, "Diary", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE diary (" +
                "'diary_id' INTEGER PRIMARY KEY NOT NULL, " +
                "'account_id' VARCHAR(12) NOT NULL, " +
                "'connect_type' CHAR(1) NOT NULL, " +
                "'color' INTEGER, " +
                "'location' VARCHAR(20), " +
                "'day' DATE NOT NULL, " +
                "'title' VARCHAR(10), "+
                "'content' TEXT, "+
                "'weather' VARCHAR(10), "+
                "'image' VARCHAR(20), "+
                "'group_id' INTEGER"+
                ");";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS diary";
        db.execSQL(sql);

        onCreate(db);
    }
}
