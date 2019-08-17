package com.android.nanal;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.event.GeneralPreferences;
import com.android.nanal.event.Utils;

import net.cryptobrewery.androidprocessingbutton.ProcessButton;


public class LoginActivity extends Activity {
    ConstraintLayout ll_login;
    TextView tv_inform, tv_pw;
    EditText et_email, et_pw;
    ProcessButton btn_login;

    private int mMorphCounter = 1;

    boolean isSignup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        ll_login = findViewById(R.id.ll_login);
        tv_inform = findViewById(R.id.tv_login_inform);
        tv_pw = findViewById(R.id.tv_login_pw);
        btn_login = findViewById(R.id.btn_login);
        et_email = findViewById(R.id.et_email);
        et_pw = findViewById(R.id.et_pw);

        tv_inform.setText(getString(R.string.sign));
        btn_login.setBtnText(getString(R.string.button_login));
        tv_inform.setPaintFlags(tv_inform.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        tv_pw.setVisibility(View.VISIBLE);

        // 백그라운드 테마 색상 설정
        String selectedColorName = Utils.getSharedPreference(this, GeneralPreferences.KEY_COLOR_PREF, "teal");
        ll_login.setBackgroundColor(getResources().getColor(DynamicTheme.getColorId(selectedColorName)));
        // 상단바 색상 변경
        getWindow().setStatusBarColor(getResources().getColor(DynamicTheme.getColorId(selectedColorName)));


        btn_login.setProgressActivated(false);
        btn_login.setIntepolator(ProcessButton.interpolators.INTERPOLATOR_ACCELERATEDECELERATE);
        btn_login.setMultipleProgressColors(new String[]{"#EF9812", "#A323E1", "#ABCDEF", "#9ABEF1"});
        btn_login.setBtnBackgroundColor(getResources().getString(DynamicTheme.getColorString(DynamicTheme.getColorId(selectedColorName))));
        btn_login.setBtnTextColor("#FFFFFF");
        btn_login.setFailureTxt(getResources().getString(R.string.error));
        btn_login.playReversed(false);
        btn_login.setIndeterminate(true);

        // 버튼
        btn_login.setOnBtnClickListener(new ProcessButton.onClickListener() {
            @Override
            public void onClick() {
                btn_login.playProgress();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (et_email.getText().toString().length() == 0) {
                            Toast.makeText(LoginActivity.this, R.string.email_error, Toast.LENGTH_LONG).show();
                            btn_login.setButtonState(ProcessButton.state.FAILURE);
                            btn_login.stopProgress();
                            return;
                        }
                        if (et_pw.getText().toString().length() < 8) {
                            Toast.makeText(LoginActivity.this, R.string.pw_error, Toast.LENGTH_LONG).show();
                            btn_login.setButtonState(ProcessButton.state.FAILURE);
                            btn_login.stopProgress();
                            return;
                        }
                        if (isSignup) {
                            // 회원가입 처리
                            // SignUpHelper 클래스에서 처리하고 switch문으로 return값 처리도 괜찮을 것 같음
                            btn_login.setButtonState(ProcessButton.state.SUCCESS);
                            btn_login.stopProgress();
                            goHome();
                        } else {
                            // 로그인 처리
                            btn_login.setButtonState(ProcessButton.state.SUCCESS);
                            btn_login.stopProgress();
                            goHome();
                        }
                    }
                }, 1000);
            }
        });
    }
    public void goHome() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(LoginActivity.this, AllInOneActivity.class);
                startActivity(intent);
                finish();
            }
        }, 500);
    }

    public void ModeSwitch(View v) {
        if(isSignup) {
            // 회원가입 화면을 보여 주고 있다면 로그인으로 전환
            tv_inform.setText(getString(R.string.sign));
            et_pw.setHint(R.string.pw_hint1);
            btn_login.setBtnText(getString(R.string.button_login));
            tv_pw.setVisibility(View.VISIBLE);
        } else {
            // 회원가입 화면으로 전환
            tv_inform.setText(getString(R.string.login));
            et_pw.setHint(R.string.pw_hint2);
            btn_login.setBtnText(getString(R.string.button_sign));
            tv_pw.setVisibility(View.GONE);
        }
        isSignup = !isSignup;
    }
}