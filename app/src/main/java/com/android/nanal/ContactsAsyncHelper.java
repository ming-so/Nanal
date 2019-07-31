package com.android.nanal;

/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.nanal.event.EditEventHelper.AttendeeItem;

import java.io.InputStream;

/**
 * Helper class for async access of images.
 * 이미지의 비동기(async) 접근을 위한 헬퍼 클래스
 */
public class ContactsAsyncHelper extends Handler {

    private static final boolean DBG = false;
    private static final String LOG_TAG = "ContactsAsyncHelper";
    // constants
    private static final int EVENT_LOAD_IMAGE = 1;
    private static final int EVENT_LOAD_DRAWABLE = 2;
    private static final int DEFAULT_TOKEN = -1;
    private static ContactsAsyncHelper mInstance = null;
    // static objects
    private static Handler sThreadHandler;

    /**
     * Private constructor for static class
     * 정적 클래스를 위한 private 생성자
     */
    private ContactsAsyncHelper() {
        HandlerThread thread = new HandlerThread("ContactsAsyncWorker");
        thread.start();
        sThreadHandler = new WorkerHandler(thread.getLooper());
    }

    /**
     * Start an image load, attach the result to the specified CallerInfo object.
     * 이미지 로드를 시작하고, 결과를 지정된 CallerInfo 개체에 연결함
     * Note, when the query is started, we make the ImageView INVISIBLE if the
     * placeholderImageResource value is -1.  When we're given a valid (!= -1)
     * placeholderImageResource value, we make sure the image is visible.
     * 참고, 쿼리가 시작됐을 때, placeholderImageResource 값이 -1이면 ImaveView를 INVISIBLE로 함
     * 유효한 placeholderImageResource 값이 들어오면(!= -1), 이미지가 visible인지 확인함
     */
    public static final void updateImageViewWithContactPhotoAsync(Context context,
                                                                  ImageView imageView, Uri contact, int placeholderImageResource) {

        // in case the source caller info is null, the URI will be null as well.
        // just update using the placeholder image in this case.
        // caller 정보가 null인 경우, URL도 null이 됨
        // 이 경우, placeholder 이미지를 사용하여 업데이트함
        if (contact == null) {
            if (DBG) Log.d(LOG_TAG, "target image is null, just display placeholder.");
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(placeholderImageResource);
            return;
        }

        // Added additional Cookie field in the callee to handle arguments
        // sent to the callback function.
        // 콜백 함수로 전송된 인자들을 처리하기 위해 callee(caller?)에 추가적인 쿠키 필드 추가

        // setup arguments
        WorkerArgs args = new WorkerArgs();
        args.context = context;
        args.view = imageView;
        args.uri = contact;
        args.defaultResource = placeholderImageResource;

        if (mInstance == null) {
            mInstance = new ContactsAsyncHelper();
        }
        // setup message arguments
        Message msg = sThreadHandler.obtainMessage(DEFAULT_TOKEN);
        msg.arg1 = EVENT_LOAD_IMAGE;
        msg.obj = args;

        if (DBG) Log.d(LOG_TAG, "Begin loading image: " + args.uri +
                ", displaying default image for now.");

        // set the default image first, when the query is complete, we will
        // replace the image with the correct one.
        // 기본 이미지를 먼저 설정하고, 쿼리가 완료되면 이미지를 올바른 이미지로 바꿈
        if (placeholderImageResource != -1) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(placeholderImageResource);
        } else {
            imageView.setVisibility(View.INVISIBLE);
        }

        // notify the thread to begin working
        // 일을 시작하라고 쓰레드에 통지함
        sThreadHandler.sendMessage(msg);
    }

    /**
     * Start an image load, attach the result to the specified CallerInfo object.
     * Note, when the query is started, we make the ImageView INVISIBLE if the
     * placeholderImageResource value is -1.  When we're given a valid (!= -1)
     * placeholderImageResource value, we make sure the image is visible.
     */
    public static final void retrieveContactPhotoAsync(Context context,
                                                       AttendeeItem item, Runnable run, Uri photoUri) {

        // in case the source caller info is null, the URI will be null as well.
        // just return as there's nothing to do.
        // caller 정보가 null인 경우, URL도 null이 됨
        // 할 일이 없으니 그냥 반환함
        if (photoUri == null) {
            return;
        }

        // Added additional Cookie field in the callee to handle arguments
        // sent to the callback function.
        // 콜백 함수로 전송된 인자들을 처리하기 위해 callee(caller?)에 추가적인 쿠키 필드 추가

        // setup arguments
        WorkerArgs args = new WorkerArgs();
        args.context = context;
        args.item = item;
        args.uri = photoUri;
        args.callback = run;

        if (mInstance == null) {
            mInstance = new ContactsAsyncHelper();
        }
        // setup message arguments
        Message msg = sThreadHandler.obtainMessage(DEFAULT_TOKEN);
        msg.arg1 = EVENT_LOAD_DRAWABLE;
        msg.obj = args;

        if (DBG) Log.d(LOG_TAG, "Begin loading drawable: " + args.uri);


        // notify the thread to begin working
        sThreadHandler.sendMessage(msg);
    }

    /**
     * Called when loading is done.
     * 로딩이 완료되면 호출됨
     */
    @Override
    public void handleMessage(Message msg) {
        WorkerArgs args = (WorkerArgs) msg.obj;
        switch (msg.arg1) {
            case EVENT_LOAD_IMAGE:
                // if the image has been loaded then display it, otherwise set default.
                // in either case, make sure the image is visible.
                // 이미지가 로드된 경우 이미지를 표시하고, 그렇지 않으면 기본값을 설정함
                // 두 경우 모두 이미지가 보이는지 확인함
                if (args.result != null) {
                    args.view.setVisibility(View.VISIBLE);
                    args.view.setImageDrawable((Drawable) args.result);
                } else if (args.defaultResource != -1) {
                    args.view.setVisibility(View.VISIBLE);
                    args.view.setImageResource(args.defaultResource);
                }
                break;
            case EVENT_LOAD_DRAWABLE:
                if (args.result != null) {
                    args.item.mBadge = (Drawable) args.result;
                    if (args.callback != null) {
                        args.callback.run();
                    }
                }
                break;
            default:
        }
    }

    /**
     * Interface for a WorkerHandler result return.
     * WorkerHandler 결과 반환에 대한 인터페이스
     */
    public interface OnImageLoadCompleteListener {
        /**
         * Called when the image load is complete.
         * 이미지 로드가 완료되면 호출됨
         *
         * @param imagePresent true if an image was found
         *                       이미지가 있으면 true
         */
        public void onImageLoadComplete(int token, Object cookie, ImageView iView,
                                        boolean imagePresent);
    }

    private static final class WorkerArgs {
        public Context context;
        public ImageView view;
        public Uri uri;
        public int defaultResource;
        public Object result;
        public AttendeeItem item;
        public Runnable callback;
    }

    /**
     * Thread worker class that handles the task of opening the stream and loading
     * the images.
     * 스트림을 열고 이미지를 로드하는 작업을 처리하는 쓰레드 워커 클래스
     */
    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            WorkerArgs args = (WorkerArgs) msg.obj;

            switch (msg.arg1) {
                case EVENT_LOAD_DRAWABLE:
                case EVENT_LOAD_IMAGE:
                    InputStream inputStream = null;
                    try {
                        inputStream = Contacts.openContactPhotoInputStream(
                                args.context.getContentResolver(), args.uri);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error opening photo input stream", e);
                    }

                    if (inputStream != null) {
                        args.result = Drawable.createFromStream(inputStream, args.uri.toString());

                        if (DBG) Log.d(LOG_TAG, "Loading image: " + msg.arg1 +
                                " token: " + msg.what + " image URI: " + args.uri);
                    } else {
                        args.result = null;
                        if (DBG) Log.d(LOG_TAG, "Problem with image: " + msg.arg1 +
                                " token: " + msg.what + " image URI: " + args.uri +
                                ", using default image.");
                    }
                    break;
                default:
            }

            // send the reply to the enclosing class.
            // enclosing 클래스로 답신을 보냄
            Message reply = ContactsAsyncHelper.this.obtainMessage(msg.what);
            reply.arg1 = msg.arg1;
            reply.obj = msg.obj;
            reply.sendToTarget();
        }
    }
}
