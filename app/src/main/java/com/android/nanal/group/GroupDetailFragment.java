package com.android.nanal.group;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toolbar;

import com.android.nanal.NanalDBHelper;
import com.android.nanal.R;
import com.android.nanal.activity.AllInOneActivity;
import com.android.nanal.calendar.CalendarController;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GroupDetailFragment extends Fragment implements CalendarController.EventHandler {
    private int mGroupId;

    private NanalDBHelper helper;

    private Toolbar mToolbar;
    private ImageButton btn_join;
    private RecyclerView rv_gevent, rv_gdiary, rv_ghistory;
    private TextView tv_gname, tv_gemail, tv_allgevent, tv_allgdiary, tv_allghistory;

    public GroupDetailFragment() {
        super();
    }

    @SuppressLint("ValidFragment")
    public GroupDetailFragment(int group_id) {
        mGroupId = group_id;
        Log.i("GroupDetailFragment", "전송받은 group_id=" + mGroupId);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Context context = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.group_detail, null);
        final int groupId = AllInOneActivity.mGroupId;
        final String groupName, groupEmail;

        btn_join = (ImageButton) v.findViewById(R.id.btn_join);
        rv_gevent = (RecyclerView) v.findViewById(R.id.rv_gevent);
        rv_gdiary = (RecyclerView) v.findViewById(R.id.rv_gdiary);
        rv_ghistory = (RecyclerView) v.findViewById(R.id.rv_ghistory);
        tv_gname = (TextView) v.findViewById(R.id.tv_gname);
        tv_gemail = (TextView) v.findViewById(R.id.tv_gemail);
        tv_allgevent = (TextView) v.findViewById(R.id.tv_allgevent);
        tv_allgdiary = (TextView) v.findViewById(R.id.tv_allgdiary);
        tv_allghistory = (TextView) v.findViewById(R.id.tv_allghistory);

        btn_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onGroupInvitationClick(groupId);
            }
        });

        helper = AllInOneActivity.helper;
        groupName = helper.getGroupName(groupId);
        groupEmail = helper.getGroupEmail(groupId);

        tv_gname.setText(groupName);
        tv_gemail.setText(groupEmail);

        rv_gdiary.setAdapter(new GroupDiaryListAdapter(v.getContext(), groupId));
        rv_gdiary.setLayoutManager(new LinearLayoutManager(v.getContext()));
        return v;
    }

    private void goTo(int index) {

    }

    public Uri getGroupDeepLink(int groupId) {
        // 그룹 아이디 받아오게 하기
        return Uri.parse("https://nanal.com/nanal?groupId=" + groupId);
    }

    public void onGroupInvitationClick(int groupId) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(getGroupDeepLink(groupId))
                .setDomainUriPrefix("https://nanalcalendar.page.link")
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder(getActivity().getPackageName())
                                .setFallbackUrl(Uri.parse("http://ci2019nanal.dongyangmirae.kr/"))
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
                .buildShortDynamicLink()
                .addOnCompleteListener(getActivity(), new OnCompleteListener<ShortDynamicLink>() {
                    @Override
                    public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                        if (task.isSuccessful()) {
                            Uri shortLink = task.getResult().getShortLink();
                            try {
                                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                                sendIntent.putExtra(Intent.EXTRA_TEXT, shortLink.toString());
                                sendIntent.setType("text/plain");
                                startActivity(Intent.createChooser(sendIntent, "공유"));
                            } catch (ActivityNotFoundException ignored) {
                                ignored.printStackTrace();
                            }
                        } else {
                            Log.e("GroupDetailFragment : ", task.toString());
                        }
                    }
                });
    }

    // 아래 세 개는 쓰지 마세용
    @Override
    public long getSupportedEventTypes() {
        return 0;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {

    }

    @Override
    public void eventsChanged() {

    }
}
