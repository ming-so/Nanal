package com.android.nanal.query;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.nanal.activity.AbstractCalendarActivity;
import com.android.nanal.diary.EditDiaryHelper;

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

    public EventAsyncTask(Context context, Activity activity) {
        mContext = context;
        mHelper = new EditDiaryHelper(mContext);
        mService = ((AbstractCalendarActivity) activity).getAsyncQueryService();
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
            로컬 ID가 이미 있기 때문에 서버에 저장돼 있는 아이디를 sync_data7로 처리할 예정

            그룹 일정은 GroupAsync에서 처리하므로 여기서는 다루지 않음

sync_data7 서버에 저장된 event_id
sync_data8 서버에 저장된 sync_time

user id, sync time 전송

서버 - 들어온 sync time보다 최근인 애들을 select
json print

if(sync time이 있는지?) {
selection 캘린더 이름=나날 and 캘린더 owner=유저아이디 and sync_data7 >= sync_time
} else {
selection 캘린더 이름=나날 and 캘린더 owner=유저아이디
}

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
            Log.i(TAG, String[0]);
            sendMsg = "&user_id=" + String[0] + "&sync_time=" + String[1];
            Log.i(TAG, "sync_time=" + String[1]);
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

    }
}
