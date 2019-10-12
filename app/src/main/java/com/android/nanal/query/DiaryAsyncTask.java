package com.android.nanal.query;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.android.nanal.activity.AbstractCalendarActivity;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.diary.Diary;
import com.android.nanal.diary.EditDiaryHelper;

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

public class DiaryAsyncTask extends AsyncTask<String, String, String> {
    String sendMsg, receiveMsg;
    EditDiaryHelper mHelper;
    private Context mContext;
    private AsyncQueryService mService;

    public DiaryAsyncTask(Context context, Activity activity) {
        mContext = context;
        mHelper = new EditDiaryHelper(mContext);
        mService = ((AbstractCalendarActivity) activity).getAsyncQueryService();
    }

    @Override
    protected String doInBackground(String... String) {
        try {
            String str;
            URL url = new URL("http://ci2019nanal.dongyangmirae.kr/DiaryAsync.jsp");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("POST");//데이터를 POST 방식으로 전송합니다.

            /*
            유저 ID를 보내면 서버에 저장돼 있는 일기 목록을 받음

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
            Log.i("DiaryAsyncTask", String[0]);
            sendMsg = "&user_id=" + String[0] + "&max_id=" + AllInOneActivity.helper.getDiaryLargestNumber();
            Log.i("DiaryAsyncTask", "max_id="+AllInOneActivity.helper.getDiaryLargestNumber());
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
                Log.i("DiaryAsyncTask", receiveMsg);
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
        } catch(ArrayIndexOutOfBoundsException e) {
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
            String group = jsonObject.getString("DIARY");
            JSONArray jsonArray = new JSONArray(group);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject diaryObject = jsonArray.getJSONObject(i);
                Diary diary = new Diary();
                diary.group_id = -1;
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
                diary.day = dateDay.getTime();
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
        } catch (JSONException e) {
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "동기화 중 문제가 발생했습니다.", Toast.LENGTH_LONG).show();
                }
            }, 0);
            e.printStackTrace();
        }
    }
}
