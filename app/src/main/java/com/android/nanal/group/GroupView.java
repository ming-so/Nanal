package com.android.nanal.group;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.ViewSwitcher;

import com.android.nanal.R;
import com.android.nanal.calendar.CalendarController;

public class GroupView extends View {
    private static final int INVALID_GROUP_ID = -1;

    private final DismissPopup mDismissPopup = new DismissPopup();
    private Context mContext;

    protected final Resources mResources;
    private Handler mHandler;
    private PopupWindow mPopup;
    protected boolean mPaused = true;
    private int mLastPopupGroupID;

    static RecyclerView recyclerView;
    static GroupListAdapter groupListAdapter;

    public GroupView(Context context, CalendarController controller,
                     ViewSwitcher viewSwitcher) {
        super(context);
        mContext = context;
        mResources = getResources();

        recyclerView = findViewById(R.id.rv_group);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mHandler == null) {
            mHandler = getHandler();
        }
    }


    class DismissPopup implements Runnable {

        public void run() {
            // Protect against null-pointer exceptions
            if (mPopup != null) {
                mPopup.dismiss();
            }
        }
    }
}
