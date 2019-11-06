package com.android.nanal.query;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class GroupHistoryAsyncTask extends AsyncTask<String, String, ArrayList<String[]>> {
    String sendMsg, receiveMsg;
    private Context mContext;

    public GroupHistoryAsyncTask(Context context) {
        mContext = context;
    }

    @Override
    protected ArrayList<String[]> doInBackground(String... String) {
        ArrayList<String[]> a = new ArrayList<>();
        try {
            String str;
            URL url = new URL("http://ci2019nanal.dongyangmirae.kr/android/GroupHistoryAsync.jsp");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("POST");
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            Log.i("GroupHistoryAsyncTask", String[0]);
            sendMsg = "&group_id=" + String[0];
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
                Log.i("GroupHistoryAsyncTask", receiveMsg);
                a = parseJSON(receiveMsg);
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
        return a;
    }

    protected ArrayList<String[]> parseJSON(String msg) {
        ArrayList<String[]> arrayList = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(msg);
            String arr = jsonObject.getString("arr");
            JSONArray jsonArray = new JSONArray(arr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject subObject = jsonArray.getJSONObject(i);
                String[] strs = new String[4];
                strs[0] = subObject.getString("time");
                strs[1] = subObject.getString("type");
                strs[2] = subObject.getString("text");
                strs[3] = subObject.getString("account");
                arrayList.add(strs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
       return arrayList;
    }
}
