package com.android.nanal.query;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import com.android.nanal.activity.AbstractCalendarActivity;
import com.android.nanal.diary.EditDiaryHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class EventAsyncTask extends AsyncTask<String, String, String> {
    String TAG = "EventAsyncTask";
    String sendMsg, receiveMsg;
    EditDiaryHelper mHelper;
    private Context mContext;
    private AsyncQueryService mService;
    String mStrDay, mUserId;

    public EventAsyncTask(Context context, Activity activity) {
        mContext = context;
        mHelper = new EditDiaryHelper(mContext);
        mService = ((AbstractCalendarActivity) activity).getAsyncQueryService();

        SharedPreferences eventPref = mContext.getSharedPreferences("event_sync", 0);
        mStrDay = eventPref.getString("event_sync", "");
    }

    @Override
    protected String doInBackground(String... String) {
        try {
            String str;
            URL url = new URL("http://ci2019nanal.dongyangmirae.kr/android/EventAsync.jsp");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("POST");//데이터를 POST 방식으로 전송합니다.

            /*
            유저 ID를 보내면 서버에 저장돼 있는 일정 목록을 받음
            로컬 ID가 이미 있기 때문에 서버에 저장돼 있는 아이디를 따로 내부 DB에서 처리할 예정

            그룹 일정은 GroupAsync에서 처리하므로 여기서는 다루지 않음

            서버에 저장된 event_id, 서버에 저장된 sync_time 내부 DB로 다룸

            커서 검색
            while(커서next) {
                cursor 값 가져오기
                if(cursor의 id 값이 있다면?) {
                    수정이 가능하면 수정 처리,
                    수정 불가능하면 삭제 후 등록 처리
                } else {
                    등록 처리
                }
                if (sync_time이 비어 있거나 커서의 sync_time보다 작은 경우) 	{
                    지금 sync_time에 커서 sync_time 저장
                }
            }
             */
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            mUserId = String[0];
            Log.i(TAG, mUserId);
            sendMsg = "&user_id=" + mUserId + "&sync_time=" + mStrDay;
            Log.i(TAG, "sync_time=" + mStrDay);
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
                Log.i(TAG, receiveMsg);
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
            String group = jsonObject.getString("EVENT");
            JSONArray jsonArray = new JSONArray(group);

            for (int i = 0; i < jsonArray.length(); i++) {
//                JSONObject eventObject = jsonArray.getJSONObject(i);
//                Event event = new Event();
//                event.id = eventObject.getInt()
//                Diary diary = new Diary();
//                diary.group_id = -1;
//                diary.account_id = diaryObject.getString("account_id");
//                diary.id = diaryObject.getInt("diary_id");
//                if (diaryObject.has("color")) {
//                    diary.color = diaryObject.getInt("color");
//                }
//                if (diaryObject.has("location")) {
//                    diary.location = diaryObject.getString("location");
//                }
//                String day = diaryObject.getString("day"); // 아마도 1999-09-09 이런 형식인 듯?
//                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//                Date dateDay = dateFormat.parse(day, new ParsePosition(0));
//                diary.day = dateDay.getTime();
//                if (diaryObject.has("title")) {
//                    diary.title = diaryObject.getString("title");
//                }
//                diary.content = diaryObject.getString("content");
//                if (diaryObject.has("weather")) {
//                    diary.weather = diaryObject.getString("weather");
//                }
//                if (diaryObject.has("image")) {
//                    diary.img = diaryObject.getString("image");
//                }
//                AllInOneActivity.helper.addDiary(diary);
//                Log.i("GroupAsyncTask: ", "다이어리 추가 완료 diary_id=" + diary.id);
            }
        } catch (Exception e) {
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "동기화 중 문제가 발생했습니다.", Toast.LENGTH_LONG).show();
                }
            }, 0);
            e.printStackTrace();
        }
        String selection;
        ContentResolver cr = mContext.getContentResolver();
        Uri uri = CalendarContract.Events.CONTENT_URI;

        if (!(mStrDay == "" || mStrDay.isEmpty() || mStrDay == null)) {
            selection = "(account_name is '나날' AND name = '" + mUserId + "' AND sync_data7 > '" + mStrDay + "')";
        } else {
            selection = "(account_name is '나날' AND name = '" + mUserId + "')";
        }
        Cursor cur = cr.query(uri, null, selection, null, null);
        while (cur.moveToNext()) {

        }
    }
}
