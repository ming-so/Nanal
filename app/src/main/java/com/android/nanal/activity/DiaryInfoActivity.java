package com.android.nanal.activity;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.android.nanal.DynamicTheme;
import com.android.nanal.R;
import com.android.nanal.diary.DiaryInfoFragment;

import androidx.appcompat.app.AppCompatActivity;

public class DiaryInfoActivity extends AppCompatActivity {

    private static final String TAG = "DiaryInfoActivity";
    private DiaryInfoFragment mInfoFragment;
    // Create an observer so that we can update the views whenever a
    // Calendar event changes.
    // 캘린더 이벤트가 변경될 때마다 view를 업데이트할 수 있도록 관찰자(옵저버)를 생성함
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange) {
            if (selfChange) return;
            if (mInfoFragment != null) {
                //mInfoFragment.reloadDiaries();
            }
        }
    };
    private int mDiaryId;
    private long mLongId;
    private final DynamicTheme dynamicTheme = new DynamicTheme();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Get the info needed for the fragment
        // fragment에 필요한 정보 가져오기
        Intent intent = getIntent();
        mLongId = intent.getIntExtra("diary_id", -1);

        if (mLongId == -1) {
            Log.w(TAG, "No diary id");
            //Toast.makeText(this, "일기를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        Log.i(TAG, "mDiaryId: "+mLongId);

        setContentView(R.layout.simple_frame_layout);


        // Get the fragment if exists
        // 존재하는 경우 fragment 가져옴
        mInfoFragment = (DiaryInfoFragment)
                getFragmentManager().findFragmentById(R.id.main_frame);


        // Create a new fragment if none exists
        // 없는 경우 새 fragment 만듦
        if (mInfoFragment == null) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            mInfoFragment = new DiaryInfoFragment(this, mLongId);
            ft.replace(R.id.main_frame, mInfoFragment);
            ft.commit();
        }
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }
    @Override
    protected void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
