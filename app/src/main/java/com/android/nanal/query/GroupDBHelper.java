package com.android.nanal.query;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class GroupDBHelper extends SQLiteOpenHelper {

    public GroupDBHelper(Context context) {
        super(context, "community", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE community (" +
                "'group_id' INTEGER PRIMARY KEY NOT NULL, " +
                "'group_name' VARCHAR(15) NOT NULL, " +
                "'group_color' INTEGER NOT NULL, " +
                "'account_id' VARCHAR(12) NOT NULL " +
                ");";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS group";
        db.execSQL(sql);

        onCreate(db);
    }
}