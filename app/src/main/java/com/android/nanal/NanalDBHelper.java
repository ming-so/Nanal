package com.android.nanal;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.diary.Diary;
import com.android.nanal.group.Group;
import com.android.nanal.query.GroupAsyncTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class NanalDBHelper extends SQLiteOpenHelper {
    public static int DB_VERSION = 1;
    private Context mContext;
    public NanalDBHelper(Context context) {
        super(context, "nanal", null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_DIARY = "CREATE TABLE IF NOT EXISTS diary (" +
                "'diary_id' INTEGER PRIMARY KEY NOT NULL, " +
                "'account_id' VARCHAR(320) NOT NULL, " +
                "'group_id' INTEGER, "+
                "'color' INTEGER, " +
                "'location' VARCHAR(20), " +
                "'day' DATE NOT NULL, " +
                "'title' VARCHAR(10), "+
                "'content' TEXT NOT NULL, "+
                "'weather' VARCHAR(10), "+
                "'image' VARCHAR(20)"+
                ")";
        db.execSQL(CREATE_TABLE_DIARY);
        String CREATE_TABLE_COMMUNITY = "CREATE TABLE IF NOT EXISTS community (" +
                "'group_id' INTEGER PRIMARY KEY NOT NULL, " +
                "'group_name' VARCHAR(15) NOT NULL, " +
                "'group_color' INTEGER NOT NULL, " +
                "'sync_time' TIMESTAMP NOT NULL, " +
                "'account_id' VARCHAR(320) NOT NULL" +
                ")";
        db.execSQL(CREATE_TABLE_COMMUNITY);
        String CREATE_TABLE_EVENT_SYNC = "CREATE TABLE IF NOT EXISTS event_sync (" +
                "'event_id' INTEGER PRIMARY KEY NOT NULL, " +
                "'server_id' INTEGER NOT NULL, " +
                "'sync_time' TIMESTAMP NOT NULL" +
                ")";
        db.execSQL(CREATE_TABLE_EVENT_SYNC);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS diary";
        db.execSQL(sql);
        String sql2 = "DROP TABLE IF EXISTS community";
        db.execSQL(sql2);
        String sql3 = "DROP TABLE IF EXISTS event_sync";
        db.execSQL(sql3);

        onCreate(db);
    }

    public void addDiary(int diary_id, String account_id, int color,
                        String location, long day, String title, String content,
                         String weather, String image, String group_id) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        // long을 넘겨야 할 경우 이거 사용하기
        // String dateString = new SimpleDateFormat("MM/dd/yyyy").format(new Date(TimeinMilliSeccond));
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("diary_id", diary_id);
        values.put("account_id", account_id);
        values.put("day", dateFormat.format(day));
        if(color != -1) values.put("color", color);
        if(location != null) values.put("location", location);
        if(title != null) values.put("title", title);
        if(content != null) values.put("content", content);
        if(weather != null) values.put("weather", weather);
        if(image != null) values.put("image", image);
        if(group_id != null || group_id == "") values.put("group_id", group_id);
        db.insert("diary", null, values);
        db.close();
    }

    public void addDiary(Diary d) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("diary_id", d.id);
        values.put("account_id", d.account_id);
        values.put("day", d.day);
        values.put("content", d.content);
        if(d.color != -1) values.put("color", d.color);
        if(d.location != null) values.put("location", d.location);
        if(d.title != null) values.put("title", d.title);
        if(d.content != null) values.put("content", d.content);
        if(d.weather != null) values.put("weather", d.weather);
        if(d.img != null) values.put("image", d.img);
        if(d.group_id != -1) values.put("group_id", d.group_id);
        db.insert("diary", null, values);
        db.close();


    }

    public void addGroup(int group_id, String group_name, int group_color, String sync_time, String account_id) {
        Log.i("NanalDBHelper", "그룹 추가 시도 "+group_id+", "+group_name+", "+group_color+", "+account_id);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("group_id", group_id);
        values.put("group_name", group_name);
        values.put("group_color", group_color);
        values.put("sync_time", sync_time);
        values.put("account_id", account_id);
        db.insert("community", null, values);
        db.close();

        Log.i("NanalDBHelper", "AllInOneActivity Groups 갱신 시도");
        GroupAsyncTask groupAsyncTask = new GroupAsyncTask(AllInOneActivity.mContext, AllInOneActivity.mActivity);
        groupAsyncTask.execute(account_id);
    }

    public void addEventSync(int event_id, int server_id, String time) {
        Log.i("NanalDBHelper", "일정 추가 시도 "+event_id+", "+server_id+", "+time);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("event_id", event_id);
        values.put("server_id", server_id);
        values.put("sync_time", time);
        db.insert("event_sync", null, values);
        db.close();
        //todo:EventAsyncTask 갱신
    }

    public void addEventSync(String title, long start, long end, String server_id, String time) {
        Log.i("NanalDBHelper", "일정 추가 시도 "+title+", "+start+", "+end);
        Uri uri = CalendarContract.Events.CONTENT_URI;
        ContentResolver cr = mContext.getContentResolver();

        String selection = "title = '" + title +"' AND dtstart = " + start + " AND dtend = " + end;
        Cursor cur = cr.query(uri, null, selection, null, null);

        if (cur.moveToNext()) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("event_id", cur.getInt(60));
            values.put("server_id", Integer.parseInt(server_id.trim()));
            values.put("sync_time", time);
            db.insert("event_sync", null, values);
            db.close();
        }
    }

    public boolean checkGroup(int group_id) {
        Log.i("NanalDBHelper", "그룹 확인 "+group_id);
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM community WHERE group_id='"+group_id+"'";
        Cursor cursor = db.rawQuery(sql,null);
        if(cursor.getCount() > 0 && cursor.moveToFirst()) {
            Log.i("NanalDBHelper", cursor.getString(1));
            db.close();
            return true;
        }
        db.close();
        return false;
    }

    public void updateDiary(int diary_id, int color, String location, String title, String content, String weather, String image) {
        SQLiteDatabase db = getWritableDatabase();
        String sql = "UPDATE diary set color = '"+color+"'"
                + ", location = '"+location+"'"
                + ", title = '"+title+"'"
                + ", content = '"+content+"'"
                + ", weather = '"+weather+"'"
                + ", image = '"+image+"' "
                + "WHERE diary_id = '"+diary_id+"';";
        db.execSQL(sql);
        Log.i("NanalDBHelper","updateDiary 완료");
    }

    public void updateGroup(int group_id, String group_name, int group_color, String sync_time, String account_id) {
        SQLiteDatabase db = getWritableDatabase();
        String sql = "UPDATE community set group_name = '"+group_name+"'"
                + ", group_color = '"+group_color+"'"
                + ", sync_time = '"+sync_time+"'"
                + ", account_id = '"+account_id+"' "
                + "WHERE group_id = '"+group_id+"';";
        db.execSQL(sql);
        Log.i("NanalDBHelper", "updateGroup 완료");
    }

    public void updateEventSync(int event_id, int server_id, String time) {
        SQLiteDatabase db = getWritableDatabase();
        String sql = "UPDATE event_sync set event_id = '"+event_id+"'"
                + ", server_id = '"+server_id+"'"
                + ", sync_time = '"+time+"' "
                + "WHERE event_id = '"+event_id+"';";
        db.execSQL(sql);
    }

    public void deleteGroup(int group_id) {
        SQLiteDatabase db = getWritableDatabase();
        String sql = "DELETE FROM community WHERE group_id="+group_id;
        db.execSQL(sql);
        Log.i("NanalDBHelper", "deleteGroup 완료");
    }

    public void setGroupSync(int group_id, String time) {

    }

    public String getGroupSync(int group_id) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM community WHERE group_id='"+group_id+"'";
        Cursor cursor = db.rawQuery(sql, null);
        if(cursor.moveToNext()) {
            return cursor.getString(cursor.getColumnIndex("sync_time"));
        }
        return "";
    }

    public String getEventSync(int event_id) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM event_sync WHERE event_id='"+event_id+"'";
        Cursor cursor = db.rawQuery(sql, null);
        if(cursor.moveToNext()) {
            return cursor.getString(cursor.getColumnIndex("sync_time"));
        }
        return "";
    }

    public ArrayList<Group> getGroupList() {
        ArrayList<Group> mGroupList = new ArrayList<>();
        // 그룹 아이디, 그룹 이름
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM community";
        Cursor cursor = db.rawQuery(sql, null);
        while(cursor.moveToNext()) {
            Group g = new Group(cursor.getInt(cursor.getColumnIndex("group_id")),
                    cursor.getString(cursor.getColumnIndex("group_name")),
                    cursor.getInt(cursor.getColumnIndex("group_color")),
                    cursor.getString(cursor.getColumnIndex("account_id")));
            mGroupList.add(g);
        }
        return mGroupList;
    }

    public String convertJulian(int julianDay) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT DATE('"+julianDay+"', 'localtime');";
        Cursor cursor = db.rawQuery(sql, null);
        String date = "";
        if(cursor.moveToNext()) {
            date = cursor.getString(0);
            Log.i("NanalDBHelper", "date="+date);
        }
        if(date == "" || date.isEmpty()) {
            long now = System.currentTimeMillis();
            Date d = new Date(now);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            date = simpleDateFormat.format(d);
        }
        return date;
    }

    public ArrayList<Diary> getDiariesList(String day) {
        ArrayList<Diary> diaryList = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM diary WHERE day = '"+day+"'";
        Cursor cursor = db.rawQuery(sql, null);

        while(cursor.moveToNext()) {
            Log.i("NanalDBHelper", "커서 있음");
            Diary diary = new Diary();
            diary.id = cursor.getInt(cursor.getColumnIndex("diary_id"));
            diary.account_id = cursor.getString(cursor.getColumnIndex("account_id"));
            String str_day = cursor.getString(cursor.getColumnIndex("day"));
            try {
                Date d = dateFormat.parse(str_day);
                diary.day = d.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            diary.content = cursor.getString(cursor.getColumnIndex("content"));

            // 혹시나 모를 오류를 방지하기 위해 값이 유효하지 않은 경우에는 초기화를 하지 않고 초기값을 유지함
            int group_id = cursor.getInt(cursor.getColumnIndex("group_id"));
            if(group_id > 0) {
                diary.group_id = group_id;
            }
            int color = cursor.getInt(cursor.getColumnIndex("color"));
            if(color != -1) {
                diary.color = color;
            }
            String location = cursor.getString(cursor.getColumnIndex("location"));
            if(location != "" || !location.isEmpty()) {
                diary.location = location;
            }
            String title = cursor.getString(cursor.getColumnIndex("title"));
            if(title != "" || !title.isEmpty()) {
                diary.title = title;
            }
            String weather = cursor.getString(cursor.getColumnIndex("weather"));
            if(weather != "" || !weather.isEmpty()) {
                diary.weather = weather;
            }
            String image = cursor.getString(cursor.getColumnIndex("image"));
            if(image != "" || !image.isEmpty()) {
                diary.img = image;
            }
            diaryList.add(diary);
            Log.i("NanalDBHelper", "getDiariesList - 다이어리 추가 완료 diary_id="+diary.id+", content="+diary.content);
        }
        return diaryList;
    }

    public ArrayList<Diary> getDiariesList(int startDay, int endDay) {
        ArrayList<Diary> diaryList = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        String str_start_t = dateFormat.format(startDay);
//        String str_end_t = dateFormat.format(endDay);
        String str_start = convertJulian(startDay);
        String str_end = convertJulian(endDay);

        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM diary WHERE day BETWEEN '"+str_start+"' AND '"+str_end+"';";
        Cursor cursor = db.rawQuery(sql, null);
        while(cursor.moveToNext()) {
            Log.i("NanalDBHelper", "커서 있음");
            Diary diary = new Diary();
            diary.id = cursor.getInt(cursor.getColumnIndex("diary_id"));
            diary.account_id = cursor.getString(cursor.getColumnIndex("account_id"));
            String str_day = cursor.getString(cursor.getColumnIndex("day"));
            try {
                Date d = dateFormat.parse(str_day);
                diary.day = d.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            diary.content = cursor.getString(cursor.getColumnIndex("content"));

            // 혹시나 모를 오류를 방지하기 위해 값이 유효하지 않은 경우에는 초기화를 하지 않고 초기값을 유지함
            int group_id = cursor.getInt(cursor.getColumnIndex("group_id"));
            if(group_id > 0) {
                diary.group_id = group_id;
            }
            int color = cursor.getInt(cursor.getColumnIndex("color"));
            if(color != -1) {
                diary.color = color;
            }
            String location = cursor.getString(cursor.getColumnIndex("location"));
            if(location != "" || !location.isEmpty()) {
                diary.location = location;
            }
            String title = cursor.getString(cursor.getColumnIndex("title"));
            if(title != "" || !title.isEmpty()) {
                diary.title = title;
            }
            String weather = cursor.getString(cursor.getColumnIndex("weather"));
            if(weather != "" || !weather.isEmpty()) {
                diary.weather = weather;
            }
            String image = cursor.getString(cursor.getColumnIndex("image"));
            if(image != "" || !image.isEmpty()) {
                diary.img = image;
            }
            diaryList.add(diary);
            Log.i("NanalDBHelper", "getDiariesList - 다이어리 추가 완료 diary_id="+diary.id+", content="+diary.content);
        }
        return diaryList;
    }

    public int getDiaryLargestNumber() {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT MAX(diary_id) FROM diary";
        Cursor cursor = db.rawQuery(sql, null);
        if(cursor.moveToNext()) {
            return cursor.getInt(0);
        }
        return -1;
    }

    public int getGroupColor(int id) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT group_color FROM community WHERE group_id = "+id;
        Cursor cursor = db.rawQuery(sql, null);
        if(cursor.moveToNext()) {
            return cursor.getInt(0);
        }
        return -1;
    }

    public String getGroupName(int id) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT group_name FROM community WHERE group_id = "+id;
        Cursor cursor = db.rawQuery(sql, null);
        if(cursor.moveToNext()) {
            return cursor.getString(0);
        }
        return null;
    }

    public String getGroupEmail(int id) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT account_id FROM community WHERE group_id = "+id;
        Cursor cursor = db.rawQuery(sql, null);
        if(cursor.moveToNext()) {
            return cursor.getString(0);
        }
        return null;
    }

    public boolean getDiaryIsInGroup(Diary d) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT group_id FROM diary WHERE diary_id="+d.id;
        Cursor cursor = db.rawQuery(sql, null);
        if(cursor.moveToNext()) {
            if(cursor.getInt(0) != 0 && cursor.getInt(0) != -1) {
                Log.i("NanalDBHelper", "group_id="+cursor.getInt(0));
                return true;
            }
        }
        return false;
    }

    public void getAllDiaries() {
        Log.i("NanalDBHelper", "getAllDiaries 실행");
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM diary";
        Cursor cursor = db.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                Log.i("NanalDBHelper", "diary_id=" + cursor.getInt(cursor.getColumnIndex("diary_id"))
                        + ", account_id=" + cursor.getString(cursor.getColumnIndex("account_id")) +
                        ", day=" + cursor.getString(cursor.getColumnIndex("day")) + ", content=" +
                        cursor.getString(cursor.getColumnIndex("content")) + ", group_id=" +
                        cursor.getInt(cursor.getColumnIndex("group_id")));
            }
    }

    public Diary getTodayDiary(Date today) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String str_today = dateFormat.format(today);

        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM diary WHERE day='"+str_today+"'";
        Log.i("NanalDBHelper", sql);
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Log.i("NanalDBHelper", "있음! diary_id=" + cursor.getInt(cursor.getColumnIndex("diary_id"))
                    + ", account_id=" + cursor.getString(cursor.getColumnIndex("account_id")) +
                    ", day=" + cursor.getString(cursor.getColumnIndex("day")) + ", content=" +
                    cursor.getString(cursor.getColumnIndex("content"))+", group_id="+cursor.getInt(cursor.getColumnIndex("group_id")));
            if(cursor.getInt(cursor.getColumnIndex("group_id")) > 0) continue;
            Diary d = new Diary();
            d.id = cursor.getInt(cursor.getColumnIndex("diary_id"));
            d.account_id = cursor.getString(cursor.getColumnIndex("account_id"));
            d.day = today.getTime();
            d.title = cursor.getString(cursor.getColumnIndex("title"));
            d.content = cursor.getString(cursor.getColumnIndex("content"));
            d.color = cursor.getInt(cursor.getColumnIndex("color"));
            return d;
        }
        return null;
    }

    public ArrayList<Diary> getGroupDiariesList(int groupid) {
        ArrayList<Diary> diaryList = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM diary WHERE group_id = '"+groupid+"'";
        Cursor cursor = db.rawQuery(sql, null);

        while(cursor.moveToNext()) {
            Log.i("NanalDBHelper", "커서 있음");
            Diary diary = new Diary();
            diary.id = cursor.getInt(cursor.getColumnIndex("diary_id"));
            diary.account_id = cursor.getString(cursor.getColumnIndex("account_id"));
            String str_day = cursor.getString(cursor.getColumnIndex("day"));
            Log.wtf("NanalDBHelper", str_day);
            try {
                //Date d = new Date(str_day);
                Date d = dateFormat.parse(str_day);
                diary.day = d.getTime();
            } catch (Exception e) {
                e.printStackTrace();
            }
            diary.content = cursor.getString(cursor.getColumnIndex("content"));

            // 혹시나 모를 오류를 방지하기 위해 값이 유효하지 않은 경우에는 초기화를 하지 않고 초기값을 유지함
            int group_id = cursor.getInt(cursor.getColumnIndex("group_id"));
            if(group_id > 0) {
                diary.group_id = group_id;
            }
            int color = cursor.getInt(cursor.getColumnIndex("color"));
            if(color != -1) {
                diary.color = color;
            }
            String location = cursor.getString(cursor.getColumnIndex("location"));
            if(location != "" || !location.isEmpty()) {
                diary.location = location;
            }
            String title = cursor.getString(cursor.getColumnIndex("title"));
            if(title != "" || !title.isEmpty()) {
                diary.title = title;
            }
            String weather = cursor.getString(cursor.getColumnIndex("weather"));
            if(weather != "" || !weather.isEmpty()) {
                diary.weather = weather;
            }
            String image = cursor.getString(cursor.getColumnIndex("image"));
            if(image != "" || !image.isEmpty()) {
                diary.img = image;
            }
            diaryList.add(diary);
            Log.i("NanalDBHelper", "getDiariesList - 다이어리 추가 완료 diary_id="+diary.id+", content="+diary.content);
        }
        return diaryList;
    }
}
