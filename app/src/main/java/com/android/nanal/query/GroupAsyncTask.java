package com.android.nanal.query;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GroupAsyncTask extends AsyncTask<String, String, String> {
    String sendMsg, receiveMsg;

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
}
