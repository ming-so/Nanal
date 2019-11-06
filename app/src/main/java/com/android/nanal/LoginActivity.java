package com.android.nanal;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.event.GeneralPreferences;
import com.android.nanal.event.Utils;

import net.cryptobrewery.androidprocessingbutton.ProcessButton;

import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LoginActivity extends Activity {
    ConstraintLayout ll_login;
    TextView tv_inform, tv_pw;
    EditText et_email, et_pw;
    ProcessButton btn_login;

    private int mMorphCounter = 1;

    boolean isSignup = false;
    boolean isPass = false;

    String[] permissions = {
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        ll_login = findViewById(R.id.ll_login);
        tv_inform = findViewById(R.id.tv_login_inform);
        tv_pw = findViewById(R.id.tv_login_pw);
        btn_login = findViewById(R.id.btn_login);
        et_email = findViewById(R.id.et_email);
        et_pw = findViewById(R.id.et_pw);

        // 자동 로그인
        final SharedPreferences loginPref = getSharedPreferences("login_setting", MODE_PRIVATE);

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
                        if (et_email.getText().toString().length() == 0 || !et_email.getText().toString().contains("@") || !et_email.getText().toString().contains(".")) {
                            Toast.makeText(LoginActivity.this, R.string.email_error, Toast.LENGTH_LONG).show();
                            btn_login.setButtonState(ProcessButton.state.FAILURE);
                            btn_login.stopProgress();
                            return;
                        }
                        if (et_pw.getText().toString().length() < 8 && !isPass) {
                            Toast.makeText(LoginActivity.this, R.string.pw_error, Toast.LENGTH_LONG).show();
                            btn_login.setButtonState(ProcessButton.state.FAILURE);
                            btn_login.stopProgress();
                            return;
                        }
                        if (isSignup) {
                            // 회원가입 처리
                            String id = et_email.getText().toString();
                            String password = et_pw.getText().toString();
                            String result = "fail";

                            try {
                                SignUpHelper signUpHelper = new SignUpHelper();
                                result = (String) signUpHelper.execute(id, password).get();

                                if (result.equals("0")) {
                                    // 회원가입 성공했을 경우
                                    btn_login.setButtonState(ProcessButton.state.SUCCESS);
                                    btn_login.stopProgress();

                                    SharedPreferences.Editor editor = loginPref.edit();
                                    editor.putString("loginId", id);
                                    editor.putString("loginPw", password);
                                    editor.commit();
                                    if(hasPermission()) {
                                        goHome();
                                    } else {
                                        getPermission();
                                    }
                                } else if (result.equals("1")) {
                                    // 이미 존재하는 아이디인 경우
                                    Toast.makeText(LoginActivity.this, R.string.email_exist, Toast.LENGTH_LONG).show();
                                    btn_login.setButtonState(ProcessButton.state.FAILURE);
                                    btn_login.stopProgress();
                                } else {
                                    // 회원가입 실패했을 경우
                                    signupFail();
                                }
                            } catch (ExecutionException e) {
                                signupFail();
                            } catch (InterruptedException e) {
                                signupFail();
                            } catch (NullPointerException e) {
                                noInternet();
                            }
                        } else if (isPass) {
                            // 비밀번호 찾기 처리
                            String id = et_email.getText().toString();
                            String result = "fail";

                            try {
                                FindPasswordHelper findPasswordHelper = new FindPasswordHelper();
                                result = (String) findPasswordHelper.execute(id).get();

                                if (result.equals("0")) {
                                    // 비밀번호 찾기 성공했을 경우
                                    Toast.makeText(LoginActivity.this, R.string.pw_send, Toast.LENGTH_LONG).show();
                                    btn_login.setButtonState(ProcessButton.state.SUCCESS);
                                    btn_login.stopProgress();
                                } else if (result.equals("1")) {
                                    // 존재하지 않는 아이디인 경우
                                    Toast.makeText(LoginActivity.this, R.string.email_diff, Toast.LENGTH_LONG).show();
                                    btn_login.setButtonState(ProcessButton.state.FAILURE);
                                    btn_login.stopProgress();
                                } else if (result.equals("2")) {
                                    Toast.makeText(LoginActivity.this, R.string.pw_unvalid, Toast.LENGTH_LONG).show();
                                    btn_login.setButtonState(ProcessButton.state.FAILURE);
                                    btn_login.stopProgress();
                                } else {
                                    // 비밀번호 찾기 실패했을 경우
                                    pwdFail();
                                }
                            } catch (ExecutionException e) {
                                pwdFail();
                            } catch (InterruptedException e) {
                                pwdFail();
                            } catch (NullPointerException e) {
                                noInternet();
                            }
                        } else {
                            // 로그인 처리
                            // 로그인 처리 중 가끔 null 리턴하는 것 고치기
                            String id = et_email.getText().toString();
                            String password = et_pw.getText().toString();
                            String result = "fail";

                            try {
                                LoginHelper loginHelper = new LoginHelper();
                                result = (String) loginHelper.execute(id, password).get();

                                if (result.equals("0")) {
                                    // 로그인 성공했을 경우
                                    btn_login.setButtonState(ProcessButton.state.SUCCESS);
                                    btn_login.stopProgress();

                                    SharedPreferences.Editor editor = loginPref.edit();
                                    editor.putString("loginId", id);
                                    editor.putString("loginPw", password);
                                    editor.commit();

                                    if(hasPermission()) {
                                        goHome();
                                    } else {
                                        getPermission();
                                    }
                                } else if (result.equals("1")) {
                                    // 아이디 틀린 경우
                                    Toast.makeText(LoginActivity.this, R.string.email_diff, Toast.LENGTH_LONG).show();
                                    btn_login.setButtonState(ProcessButton.state.FAILURE);
                                    btn_login.stopProgress();
                                } else if (result.equals("3")) {
                                    // 비밀번호 틀린 경우
                                    Toast.makeText(LoginActivity.this, R.string.pw_diff, Toast.LENGTH_LONG).show();
                                    btn_login.setButtonState(ProcessButton.state.FAILURE);
                                    btn_login.stopProgress();
                                } else if (result.equals("2")) {
                                    // 로그인 실패 횟수가 5 이상인 경우
                                    showDialog();
                                    //Toast.makeText(LoginActivity.this, R.string.pw_out, Toast.LENGTH_LONG).show();
                                    btn_login.setButtonState(ProcessButton.state.FAILURE);
                                    btn_login.stopProgress();
                                } else {
                                    // 로그인 실패했을 경우
                                    loginFail();
                                }
                            } catch (ExecutionException e) {
                                loginFail();
                            } catch (InterruptedException e) {
                                loginFail();
                            } catch (NullPointerException e) {
                                noInternet();
                            }
                        }
                    }
                }, 1000);
            }
        });

    }

    private void loginFail() {
        Toast.makeText(LoginActivity.this, R.string.login_fail, Toast.LENGTH_LONG).show();
        btn_login.setButtonState(ProcessButton.state.FAILURE);
        btn_login.stopProgress();
    }

    private void pwdFail() {
        Toast.makeText(LoginActivity.this, R.string.pw_fail, Toast.LENGTH_LONG).show();
        btn_login.setButtonState(ProcessButton.state.FAILURE);
        btn_login.stopProgress();
    }

    private void signupFail() {
        Toast.makeText(LoginActivity.this, R.string.signup_fail, Toast.LENGTH_LONG).show();
        btn_login.setButtonState(ProcessButton.state.FAILURE);
        btn_login.stopProgress();
    }

    private void noInternet() {
        Toast.makeText(LoginActivity.this, R.string.no_internet, Toast.LENGTH_LONG).show();
        btn_login.setButtonState(ProcessButton.state.FAILURE);
        btn_login.stopProgress();
    }

    private void getPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                0);
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CALENDAR)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED)) {
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                // 요청이 취소되면 결과 배열이 비어 있음
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    // 퍼미션 받음!
                    goHome();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.user_rejected_calendar_write_permission, Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
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
        if (isSignup) {
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

    public void ModeSwitchPass(View v) {
        if (isPass) {
            // 비밀번호 찾기 화면을 보여 주고 있다면 로그인으로 전환
            tv_inform.setVisibility(View.VISIBLE);
            tv_pw.setText(getString(R.string.pw));
            et_pw.setVisibility(View.VISIBLE);
            btn_login.setBtnText(getString(R.string.button_login));
        } else {
            // 비밀번호 찾기 화면으로 전환
            tv_inform.setVisibility(View.GONE);
            tv_pw.setText(getString(R.string.pw_complete));
            et_email.setText("");
            et_pw.setText("");
            et_pw.setVisibility(View.GONE);
            btn_login.setBtnText(getString(R.string.pw));
        }
        isPass = !isPass;
    }

    public AlertDialog showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_message)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        return builder.show();
    }
}
