package com.android.nanal;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Application;

import com.android.nanal.event.GeneralPreferences;
import com.android.nanal.event.Utils;

public class CalendarApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        /*
         * Ensure the default values are set for any receiver, activity,
         * service, etc. of Calendar
         * 캘린더의 receiver, activity service 등에 대한 기본값이 설정되어 있는지 확인
         */
        GeneralPreferences.setDefaultValues(this);

        // Save the version number, for upcoming 'What's new' screen.  This will be later be
        // moved to that implementation.
        // 다가오는 'What's new' 화면에 버전 번호를 저장함
        // 나중에 실행 implemetation으로 옮겨질 것임(?)
        Utils.setSharedPreference(this, GeneralPreferences.KEY_VERSION,
                Utils.getVersionCode(this));

        // Initialize the registry mapping some custom behavior.
        // 레지스트리 매핑 일부 사용자 지정 동작을 초기화함
        ExtensionsFactory.init(getAssets());
    }
}

