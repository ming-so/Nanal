package com.android.nanal;

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
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Adapter;
import android.widget.FrameLayout;
import android.widget.ListView;

/**
 * Implements a ListView class with a sticky header at the top. The header is
 * per section and it is pinned to the top as long as its section is at the top
 * of the view. If it is not, the header slides up or down (depending on the
 * scroll movement) and the header of the current section slides to the top.
 * 맨 위에 sticky 헤더가 있는 ListView 클래스 구현
 * 헤더는 섹션에 따라 있으며, 섹션이 뷰의 상단에 있는 한 상단에 고정되어 있음
 * 그렇지 안흥ㄴ 경우 헤더는 위로 또는 아래로 미끄러지며(방향은 스크롤 이동에 따라서),
 * 현재 섹션의 헤더는 위로 미끄러짐
 * Notes:
 * 1. The class uses the first available child ListView as the working
 *    ListView. If no ListView child exists, the class will create a default one.
 *    클래스는 사용 가능한 첫 번째 자식 ListView를 작업 ListView로 사용함
 *    만약 ListView 자식이 없는 경우, 클래스가 기본 하위 항목을 생성함
 * 2. The ListView's adapter must be passed to this class using the 'setAdapter'
 *    method. The adapter must implement the HeaderIndexer interface. If no adapter
 *    is specified, the class will try to extract it from the ListView
 *    ListView의 어댑터는 반드시 'setAdapter' 메소드를 이용하여 이 클래스로 전달되어야 함
 *    어댑터는 HeaderIndexer 인터페이스를 구현해야 함
 *    만약 어댑터가 지정되지 않은 경우, 클래스가 ListView에서 어댑터를 추출하려고 시도함
 * 3. The class registers itself as a listener to scroll events (OnScrollListener), if the
 *    ListView needs to receive scroll events, it must register its listener using
 *    this class' setOnScrollListener method.
 *    클래스가 이벤트를 스크롤하는 listener(OnScrollListener)를 자체 등록하며, 만약
 *    ListView가 스크롤 이벤트를 받아야 하는 경우,
 *    이 클래스의 setOnScrollListener 메소드를 사용해서 listener를 등록해야 함
 * 4. Headers for the list view must be added before using the StickyHeaderListView
 *    StickeyHeaderListView를 사용하기 전에 ListView에 대한 헤더를 추가해야 함
 * 5. The implementation should register to listen to dataset changes. Right now this is not done
 *    since a change the dataset in a listview forces a call to OnScroll. The needed code is
 *    commented out.
 *    구현은 데이터셋 변경사항을 listen하기 위해 등록해야 함,,, 뭐임
 *    ListView에서 데이터셋을 변경하면 OnScroll에 대한 호출이 강제되기 때문에 지금은 이 작업이 수행되지 않음
 *    필요한 코드가 설명되어 있음
 */
public class StickyHeaderListView extends FrameLayout implements OnScrollListener {

    private static final String TAG = "StickyHeaderListView";
    protected boolean mChildViewsCreated = false;
    protected boolean mDoHeaderReset = false;

    protected Context mContext = null;
    protected Adapter mAdapter = null;
    protected HeaderIndexer mIndexer = null;
    protected HeaderHeightListener mHeaderHeightListener = null;
    protected View mStickyHeader = null;
    protected View mDummyHeader = null; // A invisible header used when a section has no header
    protected ListView mListView = null;
    protected ListView.OnScrollListener mListener = null;
    protected int mCurrentSectionPos = -1; // Position of section that has its header on the
    // top of the view
    // view 상단에 헤더가 있는 섹션의 위치
    protected int mNextSectionPosition = -1; // Position of next section's header
    // 다음 섹션 헤더의 위치
    protected int mListViewHeadersCount = 0;

    // This code is needed only if dataset changes do not force a call to OnScroll
    // 이 코드는 데이터셋 변경으로 인해 OnScroll이 호출되지 않는 경우에만 필요함
    // protected DataSetObserver mListDataObserver = null;

    private int mSeparatorWidth;
    private View mSeparatorView;
    private int mLastStickyHeaderHeight = 0;

    /**
     * Constructor
     *
     * @param context - application context.
     * @param attrs - layout attributes.
     */
    public StickyHeaderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        // This code is needed only if dataset changes do not force a call to OnScroll
        // 이 코드는 데이터셋 변경으로 인해 OnScroll이 호출되지 않은 경우에만 필요함
        // createDataListener();
    }

    /**
     * Sets the adapter to be used by the class to get views of headers
     * 클래스가 헤더의 view를 가져오는 데 사용할 어댑터 설정
     *
     * @param adapter - The adapter.
     */

    public void setAdapter(Adapter adapter) {

        // This code is needed only if dataset changes do not force a call to
        // OnScroll
        // 이 코드는 데이터셋 변경으로 인해 OnScroll이 호출되지 않은 경우에만 필요함
        // if (mAdapter != null && mListDataObserver != null) {
        // mAdapter.unregisterDataSetObserver(mListDataObserver);
        // }

        if (adapter != null) {
            mAdapter = adapter;
            // This code is needed only if dataset changes do not force a call
            // to OnScroll
            // 이 코드는 데이터셋 변경으로 인해 OnScroll이 호출되지 않은 경우에만 필요함
            // mAdapter.registerDataSetObserver(mListDataObserver);
        }
    }

    /**
     * Sets the indexer object (that implements the HeaderIndexer interface).
     * 인덱서 개체 설정(HeaderIndexer 인터페이스 구현)
     * @param indexer - The indexer.
     */

    public void setIndexer(HeaderIndexer indexer) {
        mIndexer = indexer;
    }

    /**
     * Sets the list view that is displayed
     * 표시되는 ListView 설정
     * @param lv - The list view.
     */

    public void setListView(ListView lv) {
        mListView = lv;
        mListView.setOnScrollListener(this);
        mListViewHeadersCount = mListView.getHeaderViewsCount();
    }

    /**
     * Sets an external OnScroll listener. Since the StickyHeaderListView sets
     * itself as the scroll events listener of the listview, this method allows
     * the user to register another listener that will be called after this
     * class listener is called.
     * 외부 OnScroll listener를 설정함
     * StickyHeaderListView가 ListView의 스크롤 이벤트 listener로 설정되므로,
     * 이 메소드는 사용자가 이 클래스 listener를 호출한 후, 호출할 다른 listener를 등록할 수 있게 함
     *
     * @param listener - The external listener.
     */
    public void setOnScrollListener(ListView.OnScrollListener listener) {
        mListener = listener;
    }

    public void setHeaderHeightListener(HeaderHeightListener listener) {
        mHeaderHeightListener = listener;
    }

    /**
     * Scroll status changes listener
     * 스크롤 상태 변경 listener
     *
     * @param view - the scrolled view
     * @param scrollState - new scroll state.
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mListener != null) {
            mListener.onScrollStateChanged(view, scrollState);
        }
    }

    // This code is needed only if dataset changes do not force a call to OnScroll
    // 이 코드는 데이터셋 변경으로 인해 OnScroll이 호출되지 않은 경우에만 필요함
    // protected void createDataListener() {
    //    mListDataObserver = new DataSetObserver() {
    //        @Override
    //        public void onChanged() {
    //            onDataChanged();
    //        }
    //    };
    // }

    /**
     * Scroll events listener
     * 스크롤 이벤트 listener
     *
     * @param view - the scrolled view
     * @param firstVisibleItem - the index (in the list's adapter) of the top
     *            visible item.
     *                         상단에 보이는 아이템의 인덱스(list의 어댑터에 있는)
     * @param visibleItemCount - the number of visible items in the list
     * @param totalItemCount - the total number items in the list
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                         int totalItemCount) {

        updateStickyHeader(firstVisibleItem);

        if (mListener != null) {
            mListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    /**
     * Sets a separator below the sticky header, which will be visible while the sticky header
     * is not scrolling up.
     * sticky header가 위로 스크롤되지 않는 동안, 보이는 stickey header의 아래에 구분 기호를 설정함
     * @param color - color of separator
     * @param width - width in pixels of separator
     */
    public void setHeaderSeparator(int color, int width) {
        mSeparatorView = new View(mContext);
        ViewGroup.LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                width, Gravity.TOP);
        mSeparatorView.setLayoutParams(params);
        mSeparatorView.setBackgroundColor(color);
        mSeparatorWidth = width;
        this.addView(mSeparatorView);
    }

    protected void updateStickyHeader(int firstVisibleItem) {

        // Try to make sure we have an adapter to work with (may not succeed).
        // 함께 사용할 어댑터가 있는지 확인(성공적이지 못할 수도 있음)
        if (mAdapter == null && mListView != null) {
            setAdapter(mListView.getAdapter());
        }

        firstVisibleItem -= mListViewHeadersCount;
        if (mAdapter != null && mIndexer != null && mDoHeaderReset) {

            // Get the section header position
            // 섹션 헤더 위치 가져오기
            int sectionSize = 0;
            int sectionPos = mIndexer.getHeaderPositionFromItemPosition(firstVisibleItem);

            // New section - set it in the header view
            // 새로운 섹션 - header view에서 설정
            boolean newView = false;
            if (sectionPos != mCurrentSectionPos) {

                // No header for current position , use the dummy invisible one, hide the separator
                // 현재 위치에 대한 헤더가 없음, 보이지 않는 더미 헤더를 사용해서 구분 기호 숨기기
                if (sectionPos == -1) {
                    sectionSize = 0;
                    this.removeView(mStickyHeader);
                    mStickyHeader = mDummyHeader;
                    if (mSeparatorView != null) {
                        mSeparatorView.setVisibility(View.GONE);
                    }
                    newView = true;
                } else {
                    // Create a copy of the header view to show on top
                    // 위에 표시할 헤더 view의 복사본 생성
                    sectionSize = mIndexer.getHeaderItemsNumber(sectionPos);
                    View v = mAdapter.getView(sectionPos + mListViewHeadersCount, null, mListView);
                    v.measure(MeasureSpec.makeMeasureSpec(mListView.getWidth(),
                            MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mListView.getHeight(),
                            MeasureSpec.AT_MOST));
                    this.removeView(mStickyHeader);
                    mStickyHeader = v;
                    newView = true;
                }
                mCurrentSectionPos = sectionPos;
                mNextSectionPosition = sectionSize + sectionPos + 1;
            }


            // Do transitions
            // If position of bottom of last item in a section is smaller than the height of the
            // sticky header - shift drawable of header.
            // 섹션의 마지막 항목의 하단 위치가 sticky header의 높이보다 작은 경우 - 헤더의 drawable 변경
            if (mStickyHeader != null) {
                int sectionLastItemPosition =  mNextSectionPosition - firstVisibleItem - 1;
                int stickyHeaderHeight = mStickyHeader.getHeight();
                if (stickyHeaderHeight == 0) {
                    stickyHeaderHeight = mStickyHeader.getMeasuredHeight();
                }

                // Update new header height
                // 새로운 헤더의 높이 업데이트함
                if (mHeaderHeightListener != null &&
                        mLastStickyHeaderHeight != stickyHeaderHeight) {
                    mLastStickyHeaderHeight = stickyHeaderHeight;
                    mHeaderHeightListener.OnHeaderHeightChanged(stickyHeaderHeight);
                }

                View SectionLastView = mListView.getChildAt(sectionLastItemPosition);
                if (SectionLastView != null && SectionLastView.getBottom() <= stickyHeaderHeight) {
                    int lastViewBottom = SectionLastView.getBottom();
                    mStickyHeader.setTranslationY(lastViewBottom - stickyHeaderHeight);
                    if (mSeparatorView != null) {
                        mSeparatorView.setVisibility(View.GONE);
                    }
                } else if (stickyHeaderHeight != 0) {
                    mStickyHeader.setTranslationY(0);
                    if (mSeparatorView != null && !mStickyHeader.equals(mDummyHeader)) {
                        mSeparatorView.setVisibility(View.VISIBLE);
                    }
                }
                if (newView) {
                    mStickyHeader.setVisibility(View.INVISIBLE);
                    this.addView(mStickyHeader);
                    if (mSeparatorView != null && !mStickyHeader.equals(mDummyHeader)){
                        FrameLayout.LayoutParams params =
                                new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                                        mSeparatorWidth);
                        params.setMargins(0, mStickyHeader.getMeasuredHeight(), 0, 0);
                        mSeparatorView.setLayoutParams(params);
                        mSeparatorView.setVisibility(View.VISIBLE);
                    }
                    mStickyHeader.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!mChildViewsCreated) {
            setChildViews();
        }
        mDoHeaderReset = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mChildViewsCreated) {
            setChildViews();
        }
        mDoHeaderReset = true;
    }

    private void setChildViews() {

        // Find a child ListView (if any)
        int iChildNum = getChildCount();
        for (int i = 0; i < iChildNum; i++) {
            Object v = getChildAt(i);
            if (v instanceof ListView) {
                setListView((ListView) v);
            }
        }

        // No child ListView - add one
        if (mListView == null) {
            setListView(new ListView(mContext));
        }

        // Create a dummy view , it will be used in case a section has no header
        // 더미 view 만듦, 섹션에 헤더가 없는 경우
        mDummyHeader = new View (mContext);
        ViewGroup.LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                1, Gravity.TOP);
        mDummyHeader.setLayoutParams(params);
        mDummyHeader.setBackgroundColor(Color.TRANSPARENT);

        mChildViewsCreated = true;
    }

    /**
     * Interface that must be implemented by the ListView adapter to provide headers locations
     * and number of items under each header.
     * 각 헤더 아래에 헤더 위치와 아이템의 수를 제공하기 위해 ListView 어댑터에 의해
     * 구현되어야 하는 인터페이스
     */
    public interface HeaderIndexer {
        /**
         * Calculates the position of the header of a specific item in the adapter's data set.
         * 어댑터의 데이터셋에 있는 특정 항목의 헤더 위치를 계산함
         * For example: Assuming you have a list with albums and songs names:
         * Album A, song 1, song 2, ...., song 10, Album B, song 1, ..., song 7. A call to
         * this method with the position of song 5 in Album B, should return  the position
         * of Album B.
         * 예:
         * 앨범 및 노래 이름이 있는 목록을 가지고 있다고 가정할 때:
         * 앨범 A, 노래 1, 노래 2, ...., 노래 10, 앨범 B, 노래 1, ..., 노래7
         * 앨범 B에 수록된 노래 5의 위치로 이 메소드를 호출하면, 앨범 B의 위치를 반환해야 함
         *
         * @param position - Position of the item in the ListView dataset
         *                 ListView 데이터셋에 있는 아이템의 위치
         * @return Position of header. -1 if the is no header
         *          헤더의 위치, 헤더가 없는 경우 -1
         */

        int getHeaderPositionFromItemPosition(int position);

        /**
         * Calculates the number of items in the section defined by the header (not including
         * the header).
         * 헤더(헤더 제외..)로 정의된 섹션에 있는 아이템의 수를 계산함
         * For example: A list with albums and songs, the method should return
         * the number of songs names (without the album name).
         * 예: 앨범과 노래가 있는 리스트, 메소드는 노래의 개수를 반환해야 함(앨범 이름 없이)
         *
         * @param headerPosition - the value returned by 'getHeaderPositionFromItemPosition'
         *                       'getHeaderPositionFromItemPosition'에 의해 반환된 값
         * @return Number of items. -1 on error.
         */
        int getHeaderItemsNumber(int headerPosition);
    }


    // Resets the sticky header when the adapter data set was changed
    // 어댑터 데이터셋이 변경되었을 때, stickey header를 재설정함
    // This code is needed only if dataset changes do not force a call to OnScroll
    // 이 코드는 데이터셋 변경으로 인해 OnScroll에 대한 호출이 강제되지 않은 경우에만 필요함
    // protected void onDataChanged() {
    // Should do a call to updateStickyHeader if needed
    // }

    /***
     * Interface that is used to update the sticky header's height
     * sticky header의 높이를 업데이트하는 데 사용되는 인터페이스
     */
    public interface HeaderHeightListener {

        /***
         * Updated a change in the sticky header's size
         * sticky header의 크기 변경 업데이트
         *
         * @param height - new height of sticky header sticky header의 새로운 높이
         */
        void OnHeaderHeightChanged(int height);
    }

}
