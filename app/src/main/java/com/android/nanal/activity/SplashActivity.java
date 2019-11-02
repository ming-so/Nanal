package com.android.nanal.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import com.android.nanal.DynamicTheme;
import com.android.nanal.LoginActivity;
import com.android.nanal.LoginHelper;
import com.android.nanal.PrefManager;
import com.android.nanal.R;
import com.android.nanal.WelcomeActivity;
import com.android.nanal.event.GeneralPreferences;
import com.android.nanal.event.Utils;

import java.util.concurrent.ExecutionException;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        // 백그라운드 테마 색상 설정
        String selectedColorName = Utils.getSharedPreference(this, GeneralPreferences.KEY_COLOR_PREF, "teal");
        findViewById(R.id.cl_splash).setBackgroundColor(getResources().getColor(DynamicTheme.getColorId(selectedColorName)));

        startLoading();
    }

    private void startLoading() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 처음 켜는 건지?
                PrefManager prefManager = new PrefManager(getApplicationContext());
                if (!prefManager.isFirstTimeLaunch()) {
                    // 처음 켜는 게 아니라면... 로그인 확인
                    final SharedPreferences loginPref = getSharedPreferences("login_setting", MODE_PRIVATE);

                    String loginId = loginPref.getString("loginId", null);
                    String loginPw = loginPref.getString("loginPw", null);

                    if (loginId != null && loginPw != null) {
                        LoginHelper loginHelper = new LoginHelper();
                        String result = null;
                        try {
                            result = (String) loginHelper.execute(loginId, loginPw).get();

                            if (result.equals("0")) {
                                // 로그인 성공했을 경우
                                Intent intent = new Intent(SplashActivity.this, AllInOneActivity.class);
                                overridePendingTransition(R.anim.fadein, R.anim.hold);
                                startActivity(intent);
                                finish();
                            }
                        } catch (ExecutionException e) {
                            // 에러가 뜨면 그냥 LoginActivity로 이동
                            e.printStackTrace();
                            goLogin();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            goLogin();
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                            goLogin();
                        }
                    } else {
                        // 로그인 안 돼 있다면 LoginActivity로 이동
                        goLogin();
                    }
                } else {
                    // 처음 켜는 거라면 WelcomeActivity로 이동
                    startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
                    overridePendingTransition(R.anim.fadein, R.anim.hold);
                    finish();
                }
            }
        }, 1500);
    }

    private void goLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.hold);
        finish();
    }
}
