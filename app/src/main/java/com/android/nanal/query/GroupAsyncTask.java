package com.android.nanal.query;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.util.Log;
import android.widget.Toast;

import com.android.nanal.CreateNanalCalendar;
import com.android.nanal.activity.AbstractCalendarActivity;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.calendar.CalendarController;
import com.android.nanal.calendar.CalendarEventModel;
import com.android.nanal.diary.Diary;
import com.android.nanal.event.EditEventFragment;
import com.android.nanal.event.EditEventHelper;
import com.android.nanal.event.EventRecurrence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class GroupAsyncTask extends AsyncTask<String, String, String> {
    String sendMsg, receiveMsg;
    EditEventHelper mHelper;
    private Context mContext;
    private EventRecurrence mEventRecurrence = new EventRecurrence();
    private AsyncQueryService mService;
    EditEventFragment.QueryHandler mHandler;

    private static final int TOKEN_EVENT = 1;
    private static final int TOKEN_ATTENDEES = 1 << 1;
    private static final int TOKEN_REMINDERS = 1 << 2;
    private static final int TOKEN_CALENDARS = 1 << 3;
    private static final int TOKEN_COLORS = 1 << 4;

    private static final int TOKEN_ALL = TOKEN_EVENT | TOKEN_ATTENDEES | TOKEN_REMINDERS
            | TOKEN_CALENDARS | TOKEN_COLORS;
    private static final int TOKEN_UNITIALIZED = 1 << 31;


    public GroupAsyncTask(Context context, Activity activity) {
        mContext = context;
        mHelper = new EditEventHelper(mContext);
        mService = ((AbstractCalendarActivity) activity).getAsyncQueryService();
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                mHandler = new EditEventFragment().new QueryHandler(mContext.getContentResolver());
            }
        }, 0);
    }

    @Override
    protected String doInBackground(String... String) {
        try {
            String str;
            URL url = new URL("http://ci2019nanal.dongyangmirae.kr/GroupAsync.jsp");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("POST");//데이터를 POST 방식으로 전송합니다.

            /*
            유저 ID를 보내면 가입되어 있는 그룹 목록을 받음(jsp 파일 수정 필요함!!)
            그룹 아이디, 그룹 이름, 그룹 색상, 그룹장 아이디, 일정 전체, 일기 전체
            그룹 일정은 ContentValues로 따로 캘린더 생성(??) / 그룹 정보, 그룹 일기 SQLite
            일기 목록이랑 일정 목록 둘 다 반환해 줄 수가 없으니까/반환해도 또 따로 처리해야 하니까
            return 값은 OK ERROR 정도만 알려 주고 json 파싱하는 메소드 만들어서
            onPostExecute에서 직접 갱신해 주면 될 것 같음

            기본적으로 앱 켤 때마다 한 번씩 갱신
            그룹 상단에 싱크 버튼 만들어도 괜찮을 듯
             */

            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            sendMsg = "user_id=" + String[0];
            Log.i("GroupAsyncTask", "user_id=" + String[0]);
            osw.write(sendMsg);
            osw.flush();
            osw.close();

            if (conn.getResponseCode() == conn.HTTP_OK) {
                InputStreamReader tmp = new InputStreamReader(conn.getInputStream(), "UTF-8");
                BufferedReader reader = new BufferedReader(tmp);
                StringBuffer buffer = new StringBuffer();
                while ((str = reader.readLine()) != null) {
                    buffer.append(str);
                }
                receiveMsg = buffer.toString();
                Log.i("GroupAsyncTask", receiveMsg);
                parseJSON(receiveMsg);
                tmp.close();
                reader.close();
            } else {
                Log.i("통신 결과", conn.getResponseCode() + "에러");
            }
        } catch (MalformedURLException e) {
            Log.i("통신 결과", e.getMessage() + "에러");
        } catch (IOException e) {
            Log.i("통신 결과", e.getMessage() + "에러");
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.i("통신 결과", e.getMessage() + "에러");
        }
        return receiveMsg;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        // UI 작업
    }

    protected void parseJSON(String msg) {
        try {
            JSONObject jsonObject = new JSONObject(msg);
            String group = jsonObject.getString("GROUP");
            JSONArray jsonArray = new JSONArray(group);
            for (int i = 0; i < jsonArray.length(); i++) {
                Log.i("GroupAsyncTask", "jsonArray.length(): " + jsonArray.length());
                JSONObject subObject = jsonArray.getJSONObject(i);
                String group_name = subObject.getString("group_name");
                int group_color = subObject.getInt("group_color");
                String account_id = subObject.getString("account_id");
                String str_sync_time = subObject.getString("sync_time");

                int group_id = subObject.getInt("group_id");
                if (AllInOneActivity.helper.checkGroup(group_id)) {
                    // 그룹이 있다면 업데이트해야 함
                    if (AllInOneActivity.helper.getGroupSync(group_id) != null &&
                            !AllInOneActivity.helper.getGroupSync(group_id).isEmpty()) {
                        // 버전이 옳게 들어간 경우에만 업데이트 후 return
                        Log.i("GroupAsyncTask: ", "그룹 존재, 업데이트 진행");
                        try {
                            /*
                            각각
                            쿼리타입이 insert인 경우 insert 메소드로 전송
                            쿼리타입이 update인 경우 update 메소드로 전송
                            쿼리타입이 delete인 경우 delete 메소드로 전송
                             */
                            String str;
                            URL url = new URL("http://ci2019nanal.dongyangmirae.kr/GroupAsyncUpdate.jsp");

                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                            conn.setRequestMethod("POST");//데이터를 POST 방식으로 전송합니다.

                            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
                            Log.i("GroupAsyncTask Update Group", Integer.toString(group_id).trim() + ", " + AllInOneActivity.helper.getGroupSync(group_id));
                            sendMsg = "&group_id=" + Integer.toString(group_id).trim() + "&sync_time=" + AllInOneActivity.helper.getGroupSync(group_id) + "";
                            osw.write(sendMsg);
                            osw.flush();
                            osw.close();

                            if (conn.getResponseCode() == conn.HTTP_OK) {
                                InputStreamReader tmp = new InputStreamReader(conn.getInputStream(), "UTF-8");
                                BufferedReader reader = new BufferedReader(tmp);
                                StringBuffer buffer = new StringBuffer();
                                while ((str = reader.readLine()) != null) {
                                    buffer.append(str);
                                }
                                receiveMsg = buffer.toString();
                                Log.i("GroupAsyncTask Update Group", receiveMsg);
                                parseJSONUpdate(receiveMsg);
                                tmp.close();
                                reader.close();
                            } else {
                                Log.i("GroupAsyncTask 통신 결과: ", conn.getResponseCode() + "에러");
                            }
                        } catch (MalformedURLException e) {
                            Log.i("GroupAsyncTask: ", "에러! " + e.getMessage());
                        } catch (IOException e) {
                            Log.i("GroupAsyncTask: ", "에러! " + e.getMessage());
                        }
                        continue;
                    } else {
                        // 버전이 이상한 경우 그냥 싹 밀고 다 저장하기
                        Log.i("GroupAsyncTask: ", "그룹 존재, 버전 오류");
                        AllInOneActivity.helper.onUpgrade(AllInOneActivity.helper.getWritableDatabase(),
                                AllInOneActivity.helper.DB_VERSION, AllInOneActivity.helper.DB_VERSION + 1);
                    }
                } else {
                    Log.i("GroupAsyncTask: ", "그룹 없음, 테이블 생성 group_id=" + group_id);
                    // 그룹이 없다면 생성해야 함
                    AllInOneActivity.helper.addGroup(group_id, group_name, group_color, str_sync_time, account_id);

                    ContentResolver cr = mContext.getContentResolver();
                    Uri uri = CalendarContract.Calendars.CONTENT_URI;

                    String selection = "(ownerAccount = '+" + account_id + "' AND account_name = '" + group_name + "')";
                    Cursor cur = cr.query(uri, null, selection, null, null);
                    if (!cur.moveToFirst()) {
                        CreateNanalCalendar.CreateCalendar(mContext, group_name, account_id, true);
                    }
                }
                // 그룹이 존재하지 않거나 버전이 옳지 않은 경우에는 받아온 다이어리와 이벤트를 모두 저장한다
                // 다이어리
                String diary_formatting = subObject.getString("group_diary");
                JSONArray diaryArray = new JSONArray(diary_formatting);
                for (int j = 0; j < diaryArray.length(); j++) {
                    JSONObject diaryObject = diaryArray.getJSONObject(j);
                    Diary diary = new Diary();
                    diary.group_id = group_id;
                    diary.account_id = diaryObject.getString("account_id");
                    diary.id = diaryObject.getInt("diary_id");
                    if (diaryObject.has("color")) {
                        diary.color = diaryObject.getInt("color");
                    }
                    if (diaryObject.has("location")) {
                        diary.location = diaryObject.getString("location");
                    }
                    String day = diaryObject.getString("day"); // 아마도 1999-09-09 이런 형식인 듯?
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date dateDay = dateFormat.parse(day, new ParsePosition(0));
                    diary.day = dateDay.getTime() + 32400000;
                    if (diaryObject.has("title")) {
                        diary.title = diaryObject.getString("title");
                    }
                    diary.content = diaryObject.getString("content");
                    if (diaryObject.has("weather")) {
                        diary.weather = diaryObject.getString("weather");
                    }
                    if (diaryObject.has("image")) {
                        diary.img = diaryObject.getString("image");
                    }
                    AllInOneActivity.helper.addDiary(diary);
                    Log.i("GroupAsyncTask: ", "다이어리 추가 완료 diary_id=" + diary.id);
                }
                Log.i("GroupAsyncTask: ", diaryArray.length() + "개의 다이어리 작업 완료");
                // 이벤트
                String event_formatting = subObject.getString("group_event");
                JSONArray eventArray = new JSONArray(event_formatting);
                int eventIdIndex = -1;
                for (int j = 0; j < eventArray.length(); j++) {
                    JSONObject eventObject = eventArray.getJSONObject(j);

                    CalendarEventModel model = new CalendarEventModel();
                    model.mCalendarId = group_id;
                    model.mAllDay = eventObject.getBoolean("all_day");
                    model.mHasAlarm = eventObject.getBoolean("has_alarm");
                    // start_time time -> long(milli) 변환 필요

                    mHelper.saveEvent(model, null, 3);
                }
                try {
                    Toast.makeText(mContext, "동기화를 성공적으로 마쳤습니다.", Toast.LENGTH_LONG).show();
                } catch (RuntimeException ex) {

                }
                Log.i("GroupAsyncTask: ", eventArray.length() + "개의 이벤트 작업 완료");
            }
        } catch (JSONException e) {
            try {
                //Toast.makeText(mContext, "동기화 중 문제가 발생했습니다.", Toast.LENGTH_LONG).show();
            } catch (RuntimeException ex) {

            }
            e.printStackTrace();
        }
    }

    protected void parseJSONUpdate(String msg) {
        try {
            JSONObject jsonObject = new JSONObject(msg);
            String group = jsonObject.getString("GROUP");
            String diary = jsonObject.getString("DIARY");
            String event = jsonObject.getString("EVENT");
            String sync = jsonObject.getString("SYNC_TIME");
            JSONArray groupArray = new JSONArray(group);
            JSONArray diaryArray = new JSONArray(diary);
            JSONArray eventArray = new JSONArray(event);

            for (int i = 0; i < diaryArray.length(); i++) {
                JSONObject subObject = diaryArray.getJSONObject(i);
                Diary d = new Diary();
                d.id = subObject.getInt("diary_id");
                d.group_id = subObject.getInt("group_id");
                d.account_id = subObject.getString("query_account_id");
                if (subObject.has("color")) {
                    d.color = subObject.getInt("color");
                }
                if (subObject.has("location")) {
                    d.location = subObject.getString("location");
                }
                String day = subObject.getString("day");
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date dateDay = dateFormat.parse(day, new ParsePosition(0));
                d.day = dateDay.getTime() + 32400000;
                if (subObject.has("title")) {
                    d.title = subObject.getString("title");
                }
                d.content = subObject.getString("content");
                if (subObject.has("weather")) {
                    d.weather = subObject.getString("weather");
                }
                if (subObject.has("image")) {
                    d.img = subObject.getString("image");
                }

                String query_type = subObject.getString("query_type");

                switch (query_type) {
                    case "I":
                    case "i":
                        AllInOneActivity.helper.addDiary(d);
                        break;
                    case "U":
                    case "u":
                        AllInOneActivity.helper.updateDiary(d);
                        break;
                    case "D":
                    case "d":
                        AllInOneActivity.helper.deleteDiary(d.id);
                        break;
                    default:
                        Log.wtf("GroupAsyncTask", "query_type 문제! > " + query_type);
                        return;
                }
                AllInOneActivity.helper.addDiary(d);
                Log.i("GroupAsyncTask Update: ", "다이어리 추가 완료 diary_id=" + d.id);
            }

            for (int i = 0; i < eventArray.length(); i++) {

            }

            for (int i = 0; i < groupArray.length(); i++) {
                JSONObject subObject = groupArray.getJSONObject(i);
                String group_name = subObject.getString("group_name");
                int group_color = subObject.getInt("group_color");
                String account_id = subObject.getString("account_id");
                String str_sync_time = subObject.getString("sync_time");
                int group_id = subObject.getInt("group_id");
                String query_type = subObject.getString("query_type");

                switch (query_type) {
                    case "I":
                    case "i":
                        AllInOneActivity.helper.addGroup(group_id, group_name, group_color, str_sync_time, account_id);
                        break;
                    case "U":
                    case "u":
                        AllInOneActivity.helper.updateGroup(group_id, group_name, group_color, str_sync_time, account_id);
                        break;
                    case "D":
                    case "d":
                        AllInOneActivity.helper.deleteGroup(group_id);
                        break;
                    default:
                        Log.wtf("GroupAsyncTask", "query_type 문제! > " + query_type);
                        return;
                }
                AllInOneActivity.helper.setGroupSync(group_id, sync);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void startQuery(CalendarController.EventInfo mEvent) {
        Uri mUri = null;
        long mBegin = -1;
        long mEnd = -1;
        long mCalendarId = -1;
        ArrayList<CalendarEventModel.ReminderEntry> mReminders = new ArrayList<>();
        boolean mEventColorInitialized = false;
        int mEventColor = -1;
        int mOutstandingQueries = TOKEN_UNITIALIZED;

        CalendarEventModel mModel = new CalendarEventModel();

        if (mEvent != null) {
            if (mEvent.id != -1) {
                mModel.mId = mEvent.id;
                mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEvent.id);
            } else {
                // New event. All day?
                mModel.mAllDay = mEvent.extraLong == CalendarController.EXTRA_CREATE_ALL_DAY;
            }
            if (mEvent.startTime != null) {
                mBegin = mEvent.startTime.toMillis(true);
            }
            if (mEvent.endTime != null) {
                mEnd = mEvent.endTime.toMillis(true);
            }
            if (mEvent.calendarId != -1) {
                mCalendarId = mEvent.calendarId;
            }
        }

        if (mReminders != null) {
            mModel.mReminders = mReminders;
        }

        if (mEventColorInitialized) {
            mModel.setEventColor(mEventColor);
        }

        if (mBegin <= 0) {
            // use a default value instead
            mBegin = mHelper.constructDefaultStartTime(System.currentTimeMillis());
        }
        if (mEnd < mBegin) {
            // use a default value instead
            mEnd = mHelper.constructDefaultEndTime(mBegin, mContext);
        }
        mOutstandingQueries = TOKEN_CALENDARS | TOKEN_COLORS;

        mModel.mOriginalStart = mBegin;
        mModel.mOriginalEnd = mEnd;
        mModel.mStart = mBegin;
        mModel.mEnd = mEnd;
        mModel.mCalendarId = mCalendarId;
        mModel.mSelfAttendeeStatus = Attendees.ATTENDEE_STATUS_ACCEPTED;

        // Start a query in the background to read the list of calendars and colors
        mHandler.startQuery(TOKEN_CALENDARS, null, Calendars.CONTENT_URI,
                mHelper.CALENDARS_PROJECTION,
                mHelper.CALENDARS_WHERE_WRITEABLE_VISIBLE, null /* selection args */,
                null /* sort order */);

        mHandler.startQuery(TOKEN_COLORS, null, Colors.CONTENT_URI,
                mHelper.COLORS_PROJECTION,
                Colors.COLOR_TYPE + "=" + Colors.TYPE_EVENT, null, null);

//        mModification = Utils.MODIFY_ALL;
//        mView.setModification(mModification);
    }

    private static class EventBundle implements Serializable {
        private static final long serialVersionUID = 1L;
        long id = -1;
        long start = -1;
        long end = -1;
    }
}

