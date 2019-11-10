package com.android.nanal.event;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class CreateNewEvent extends AsyncTask<String, String, String> {
    @Override
    protected String doInBackground(String... strings) {
        String str, sendMsg, receiveMsg = "";
        try {
            URL url = new URL("http://ci2019nanal.dongyangmirae.kr/EventCreate.jsp");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("POST");//데이터를 POST 방식으로 전송합니다.
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            sendMsg = "event_name=" + strings[0] + "&account_id=" + strings[1] +
                    "&group_name=" + strings[2] + "&group_account=" + strings[3] + "&color=" + strings[4] +
                    "&memo=" + strings[5] + "&location=" + strings[6] +
                    "&start=" + strings[7] + "&end=" + strings[8] +
                    "&all_day=" + strings[9] + "&has_alarm=" + strings[10] +
                    "&is_recurring=" + strings[11];
            Log.i("CreateNewEvent", "sendMsg: "+sendMsg);
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
                tmp.close();
                reader.close();
                Log.d("CreateNewDiary", receiveMsg);
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
    }
}
