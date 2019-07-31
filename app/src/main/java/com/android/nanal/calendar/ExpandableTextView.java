package com.android.nanal.calendar;

/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.nanal.R;

public class ExpandableTextView extends LinearLayout implements OnClickListener {

    TextView mTv;
    ImageButton mButton; // Button to expand/collapse  확장/축소 버튼

    private boolean mRelayout = false;
    private boolean mCollapsed = true; // Show short version as default. 짧은 버전을 기본값으로
    private int mMaxCollapsedLines = 8; // The default number of lines; 기본 줄 수
    private Drawable mExpandDrawable;
    private Drawable mCollapseDrawable;

    public ExpandableTextView(Context context) {
        super(context);
        init();
    }

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        init();
    }

    public ExpandableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    void init() {
        mMaxCollapsedLines = getResources().getInteger((R.integer.event_info_desc_line_num));
        mExpandDrawable = getResources().getDrawable(R.drawable.ic_expand_small_holo_light);
        mCollapseDrawable = getResources().getDrawable(R.drawable.ic_collapse_small_holo_light);
    }

    @Override
    public void onClick(View v) {
        if (mButton.getVisibility() != View.VISIBLE) {
            return;
        }

        mCollapsed = !mCollapsed;
        mButton.setImageDrawable(mCollapsed ? mExpandDrawable : mCollapseDrawable);
        mTv.setMaxLines(mCollapsed ? mMaxCollapsedLines : Integer.MAX_VALUE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If no change, measure and return
        if (!mRelayout || getVisibility() == View.GONE) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        mRelayout = false;

        // Setup with optimistic case
        // 낙관적인 사례로 설정 (모든 게 들어맞음, 버튼 필요 없음)
        // i.e. Everything fits. No button needed
        mButton.setVisibility(View.GONE);
        mTv.setMaxLines(Integer.MAX_VALUE);

        // Measure
        // 측정함
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // If the text fits in collapsed mode, we are done.
        // 텍스트가 collapsed 모드에 맞으면 끝
        if (mTv.getLineCount() <= mMaxCollapsedLines) {
            return;
        }

        // Doesn't fit in collapsed mode. Collapse text view as needed. Show
        // button.
        // 맞지 않으면 Collapse 텍스트 view가 필요함, 버튼을 보임
        if (mCollapsed) {
            mTv.setMaxLines(mMaxCollapsedLines);
        }
        mButton.setVisibility(View.VISIBLE);

        // Re-measure with new setup
        // 새 설정으로 재측정함
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void findViews() {
        mTv = (TextView) findViewById(R.id.expandable_text);
        mTv.setOnClickListener(this);
        mButton = (ImageButton) findViewById(R.id.expand_collapse);
        mButton.setOnClickListener(this);
    }

    public CharSequence getText() {
        if (mTv == null) {
            return "";
        }
        return mTv.getText();
    }

    public void setText(String text) {
        mRelayout = true;
        if (mTv == null) {
            findViews();
        }
        String trimmedText = text.trim();
        mTv.setText(trimmedText);
        this.setVisibility(trimmedText.length() == 0 ? View.GONE : View.VISIBLE);
    }
}

