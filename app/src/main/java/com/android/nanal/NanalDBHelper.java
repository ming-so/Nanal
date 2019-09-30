package com.android.nanal;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NanalDBHelper  extends SQLiteOpenHelper {


    public NanalDBHelper(Context context) {
        super(context, "nanal", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_DIARY = "CREATE TABLE IF NOT EXISTS diary (" +
                "'diary_id' INTEGER PRIMARY KEY NOT NULL, " +
                "'account_id' VARCHAR(320) NOT NULL, " +
                "'connect_type' TINYINT NOT NULL, " +
                "'color' INTEGER, " +
                "'location' VARCHAR(20), " +
                "'day' DATE NOT NULL, " +
                "'title' VARCHAR(10), "+
                "'content' TEXT, "+
                "'weather' VARCHAR(10), "+
                "'image' VARCHAR(20), "+
                "'group_id' INTEGER"+
                ")";
        db.execSQL(CREATE_TABLE_DIARY);
        String CREATE_TABLE_COMMUNITY = "CREATE TABLE IF NOT EXISTS community (" +
                "'group_id' INTEGER PRIMARY KEY NOT NULL, " +
                "'group_name' VARCHAR(15) NOT NULL, " +
                "'group_color' INTEGER NOT NULL, " +
                "'sync_version' INTEGER NOT NULL, " +
                "'account_id' VARCHAR(320) NOT NULL" +
                ")";
        db.execSQL(CREATE_TABLE_COMMUNITY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS diary";
        db.execSQL(sql);
        String sql2 = "DROP TABLE IF EXISTS community";
        db.execSQL(sql2);

        onCreate(db);
    }

    public void addDiary(int diary_id, String account_id, int connect_type, int color,
                        String location, Date day, String title, String content,
                         String weather, String image, int group_id) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // long을 넘겨야 할 경우 이거 사용하기
        // String dateString = new SimpleDateFormat("MM/dd/yyyy").format(new Date(TimeinMilliSeccond));
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("diary_id", diary_id);
        values.put("account_id", account_id);
        values.put("connect_type", connect_type);
        values.put("day", dateFormat.format(day));
        if(color != -1) values.put("color", color);
        if(location != null) values.put("location", location);
        if(title != null) values.put("title", title);
        if(content != null) values.put("content", content);
        if(weather != null) values.put("weather", weather);
        if(image != null) values.put("image", image);
        if(group_id != -1) values.put("group_id", group_id);
        db.insert("diary", null, values);
        db.close();
    }

    public void addGroup(int group_id, String group_name, int group_color, String account_id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("group_id", group_id);
        values.put("group_name", group_name);
        values.put("group_color", group_color);
        values.put("sync_version", 0);
        values.put("account_id", account_id);
        db.insert("group", null, values);
        db.close();
    }
}
