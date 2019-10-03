package com.android.nanal.query;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.nanal.CreateNanalCalendar;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.diary.Diary;
import com.android.nanal.event.Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GroupAsyncTask extends AsyncTask<String, String, String> {
    String sendMsg, receiveMsg;
    private Context mContext;

    public GroupAsyncTask(Context context) {
        mContext = context;
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
            그룹 일정은 ContentValues로 따로 캘린더 생성(??) / 그룹 정보, 그룹 일기 SQLiteㅊ
            일기 목록이랑 일정 목록 둘 다 반환해 줄 수가 없으니까/반환해도 또 따로 처리해야 하니까
            return 값은 OK ERROR 정도만 알려 주고 json 파싱하는 메소드 만들어서
            onPostExecute에서 직접 갱신해 주면 될 것 같음

            기본적으로 앱 켤 때마다 한 번씩 갱신
            그룹 상단에 싱크 버튼 만들어도 괜찮을 듯
             */

            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            Log.i("GroupAsyncTask", String[0]);
            sendMsg = "&user_id=" + String[0];
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
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject subObject = jsonArray.getJSONObject(i);
                String group_name = subObject.getString("group_name");
                int group_color = subObject.getInt("group_color");
                String account_id = subObject.getString("account_id");
                int group_id = subObject.getInt("group_id");
                if(AllInOneActivity.helper.checkGroup(group_id)) {
                    // 그룹이 있다면 업데이트해야 함
                    if(AllInOneActivity.helper.getGroupSync(group_id) != -1) {
                        // 버전이 옳게 들어간 경우에만 업데이트
                    } else {
                        // 버전이 이상한 경우 그냥 싹 밀고 다 저장하기
                    }
                } else {
                    // 그룹이 없다면 생성해야 함
                    AllInOneActivity.helper.addGroup(group_id, group_name, group_color, account_id);
                    CreateNanalCalendar.CreateCalendar(mContext, group_name, account_id);
                }

                // 다이어리
                String diary_formatting = subObject.getString("group_diary");
                JSONArray diaryArray = new JSONArray(diary_formatting);
                for(int j = 0; j <diaryArray.length(); i++) {
                    JSONObject diaryObject = diaryArray.getJSONObject(j);
                    Diary diary = new Diary();
                    diary.group_id = group_id;
                    diary.id = diaryObject.getInt("diary_id");
                    if(diaryObject.has("color")) { diary.color = diaryObject.getInt("color"); }
                    if(diaryObject.has("location")) { diary.location = diaryObject.getString("location"); }
                    String day = diaryObject.getString("day"); // 아마도 1999-09-09 이런 형식인 듯?
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date dateDay = dateFormat.parse(day, new ParsePosition(0));
                    diary.day = dateDay.getTime();
                    if(diaryObject.has("title")) { diary.title = diaryObject.getString("title"); }
                    diary.content = diaryObject.getString("content");
                    if(diaryObject.has("weather")) { diary.weather = diaryObject.getString("weather"); }
                    if(diaryObject.has("image")) { diary.img = diaryObject.getString("image"); }
                    AllInOneActivity.helper.addDiary(diary);
                }
                // 이벤트
                String event_formatting = subObject.getString("group_event");
                JSONArray eventArray = new JSONArray(event_formatting);
                for(int j = 0; j < eventArray.length(); j++) {
                    JSONObject eventObject = eventArray.getJSONObject(j);
                    Event event = new Event();
                    event.group_id = group_id;
                    event.allDay = eventObject.getBoolean("all_day");
                    //
                    // start_time time -> long(milli) 변환 필요
                    event.hasAlarm = eventObject.getBoolean("has_alarm");
                    event.isRepeating = eventObject.getBoolean("is_recurring");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
