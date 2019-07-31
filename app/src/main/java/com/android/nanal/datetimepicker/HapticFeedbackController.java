package com.android.nanal.datetimepicker;

import android.app.Service;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;

/**
 * A simple utility class to handle haptic feedback.
 * 햅틱 피드백을 조정하기 위한 클래스
 *
 * @deprecated This module is deprecated. Do not use this class.
 */
@Deprecated
public class HapticFeedbackController {
    private static final int VIBRATE_DELAY_MS = 125;
    private static final int VIBRATE_LENGTH_MS = 5;

    private static boolean checkGlobalSetting(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) == 1;
    }

    private final Context mContext;
    private final ContentObserver mContentObserver;

    private Vibrator mVibrator;
    private boolean mIsGloballyEnabled;
    private long mLastVibrate;

    public HapticFeedbackController(Context context) {
        mContext = context;
        mContentObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                mIsGloballyEnabled = checkGlobalSetting(mContext);
            }
        };
    }

    /**
     * Call to setup the controller.
     * 컨트롤러 설정 호출
     */
    public void start() {
        mVibrator = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);

        // Setup a listener for changes in haptic feedback settings
        // 햅틱 피드백 설정의 변경을 위한 리스너 설정
        mIsGloballyEnabled = checkGlobalSetting(mContext);
        Uri uri = Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED);
        mContext.getContentResolver().registerContentObserver(uri, false, mContentObserver);
    }

    /**
     * Call this when you don't need the controller anymore.
     * 필요 없으면 stop() 호출
     */
    public void stop() {
        mVibrator = null;
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    /**
     * Try to vibrate. To prevent this becoming a single continuous vibration, nothing will
     * happen if we have vibrated very recently.
     * 진동하기
     * 연속적인 진동이 되는 것을 막기 위해서, 최근에 진동했다면 아무것도 하지 않음
     */
    public void tryVibrate() {
        if (mVibrator != null && mIsGloballyEnabled) {
            long now = SystemClock.uptimeMillis();
            // We want to try to vibrate each individual tick discretely.
            // 개별적으로 진동하기
            if (now - mLastVibrate >= VIBRATE_DELAY_MS) {
                mVibrator.vibrate(VIBRATE_LENGTH_MS);
                mLastVibrate = now;
            }
        }
    }
}
