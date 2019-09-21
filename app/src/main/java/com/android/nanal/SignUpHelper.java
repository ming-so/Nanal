package com.android.nanal;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SignUpHelper extends AsyncTask<String, Void, String> {
    private String msg;
    private String result = "a";

    @Override
    protected String doInBackground(String... strings) {
        try {
            String str;
            URL url = new URL("http://ci2019nanal.dongyangmirae.kr/SignUpHelper.jsp");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("POST");

            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            msg = "id=" + strings[0] + "&password=" + strings[1];
            osw.write(msg);
            osw.flush();
            osw.close();

            if(conn.getResponseCode() == conn.HTTP_OK) {
                InputStreamReader isr = new InputStreamReader(conn.getInputStream(), "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                StringBuffer buffer = new StringBuffer();
                while((str = reader.readLine()) != null) {
                    buffer.append(str);
                }
                result = buffer.toString();
                isr.close();
                reader.close();
            } else {
                Log.i("Result : ", conn.getResponseCode() + "error");
            }
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
