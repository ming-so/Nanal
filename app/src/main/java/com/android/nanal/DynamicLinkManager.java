package com.android.nanal;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;

import java.util.concurrent.Executor;

public class DynamicLinkManager {

    // 동적 링크 생성 메소드
    public Uri createDynamicLink() {
        DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("http://ci2019nanal.dongyangmirae.kr"))  // 앱에서 열리는 링크
                .setDomainUriPrefix("https://nanalcalendar.page.link")
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder("com.android.nanal")
                                .setFallbackUrl(Uri.parse("http://ci2019nanal.dongyangmirae.kr/"))
                                .build())
                .setIosParameters(
                        new DynamicLink.IosParameters.Builder("com.example.ios")
                                .setFallbackUrl(Uri.parse("http://ci2019nanal.dongyangmirae.kr/"))
                                .setIpadFallbackUrl(Uri.parse("http://ci2019nanal.dongyangmirae.kr/"))
                                .build())
                .setGoogleAnalyticsParameters(
                        new DynamicLink.GoogleAnalyticsParameters.Builder()
                                .setSource("orkut")
                                .setMedium("social")
                                .setCampaign("example-promo")
                                .build())
                .setSocialMetaTagParameters(
                        new DynamicLink.SocialMetaTagParameters.Builder()
                                .setTitle("나날")
                                .setDescription("그룹 캘린더의 초대장이 도착했습니다!")
                                .setImageUrl(Uri.parse("http://ci2019nanal.dongyangmirae.kr/images/nanal_logo.png"))
                                .build())
                .buildDynamicLink();

        Uri dynamicLinkUri = dynamicLink.getUri();
        return dynamicLinkUri;
    }
}
